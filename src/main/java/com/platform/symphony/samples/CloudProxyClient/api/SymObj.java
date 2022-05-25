/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.symphony.samples.CloudProxyClient.constant.RestClientConstant;
import com.platform.symphony.samples.CloudProxyClient.loadbalance.ETCDLoadBalancer;
import com.platform.symphony.samples.CloudProxyClient.loadbalance.LoadBalanceStrategy;
import com.platform.symphony.samples.CloudProxyClient.loadbalance.RandomLoadBalancer;
import com.platform.symphony.samples.CloudProxyClient.model.Proxy;
import com.platform.symphony.samples.CloudProxyClient.model.RestClientConfig;
import com.platform.symphony.samples.CloudProxyClient.rest.RESTRequestExecutor;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientMessage;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientUtil;

/**
 * Symphony client related Objects.
 *
 */
public class SymObj {
    private static Logger log = Logger.getLogger(SymObj.class);

    public static void printError(Map<String, String> result, String action) {
        if (StringUtils.isNotBlank(result.get("result"))) {
            log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.error1", action, result.get("result").toString(), result.get("status")));
        } else {
            log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.error2", action,  result.get("status")));
        }
    }

    public static class SoamFactory {
        // HTTP Client wrapper for sending HTTP REST request
        private static RESTRequestExecutor executor = RESTRequestExecutor.getInstance();

        public static void initialize() {
            RestClientUtil.initLogger();
            // load config
            RestClientConfig.getInstance();
        }

        public static void uninitialize() {
            RestClientUtil.clean();
        }

        public static Connection connect(String appName) {
            LoadBalanceStrategy strategy;
            if (RestClientConfig.getInstance().getPolicy().equals(RestClientConstant.FAILOVER_POLICY_ETCD)) {
                strategy = ETCDLoadBalancer.getInstance();
            } else {
                strategy = RandomLoadBalancer.getInstance();
            }

            Proxy proxy;
            proxy = strategy.choiceOne();
            if (proxy == null) {
                throw new RuntimeException("All proxies are invalid for connecting.");
            }

            return new Connection(executor, appName, strategy, proxy);
        }

        public static Connection connect(String appName, String clusterId) {
            LoadBalanceStrategy strategy;
            if (RestClientConfig.getInstance().getPolicy().equals(RestClientConstant.FAILOVER_POLICY_ETCD)) {
                strategy = ETCDLoadBalancer.getInstance();
            } else {
                strategy = RandomLoadBalancer.getInstance();
            }

            Proxy proxy;
            proxy = strategy.choiceOne();
            if (proxy == null) {
                throw new RuntimeException("All proxies are invalid for connecting.");
            }

            return new Connection(executor, appName, clusterId, strategy, proxy);
        }
    }

    public static class Connection implements Serializable {
        private static final long serialVersionUID = -1L;
        private RESTRequestExecutor executor; // HTTP Client wrapper for sending HTTP REST request
        private LoadBalanceStrategy strategy; // Load Balancer
        private Proxy proxy; // Proxy entity
        private String appName = "";
        private String clusterId = "";
        private String baseUrl;

        public Connection(RESTRequestExecutor executor, String appName, LoadBalanceStrategy strategy, Proxy proxy) {
            this.executor = executor;
            this.strategy = strategy;
            this.proxy = proxy;
            this.appName = appName;
            authenticate();
        }

        public Connection(RESTRequestExecutor executor, String appName, String clusterId, LoadBalanceStrategy strategy, Proxy proxy) {
            this(executor, appName, strategy, proxy);
            this.clusterId = clusterId;
        }

        public Session createSession(SessionCreationAttributes attr) {
            Session session = null;
            do {
                try {
                    session = this.createSessionImpl(attr);
                } catch (Exception ex) {
                    log.error("Failed to create session, prepare to failover to another proxy to re-create session", ex);
                    if (null != session) {
                        try {
                            session.closeSession();
                            log.info("Session " + session.getSessionId() + " is closed");
                        } catch (Exception e) {
                            log.error("Close session error.", e);
                        }
                    }
                    // Fail over to another proxy for re-create new session
                    // remove the unavailable proxy from cache
                    this.strategy.removeOne(this.proxy);
                    log.debug("Delete the unavailable proxy from cache "+this.proxy);
                    // choose a new proxy
                    this.proxy = this.strategy.choiceOne();
                    log.debug("Choose a new proxy "+this.proxy);
                }
            } while (session == null && this.proxy != null);

            return session;
        }

        private Session createSessionImpl(SessionCreationAttributes attr) throws Exception {
            log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.createSession", appName));
            StringBuilder url = new StringBuilder(50);
            url.append(proxy.getPath());
            if (proxy.getPath().endsWith("/")) {
                url.append(RestClientConstant.REST_SERVER_URI_WITHOUT_PREFIX);
            } else {
                url.append(RestClientConstant.REST_SERVER_URI);
            }
            this.baseUrl = url.toString();
            url.append(RestClientConstant.CREATE_SESSION_PATH_INFO);
            url.append("?");
            url.append(RestClientConstant.APP_NAME);
            url.append("=");
            url.append(appName);
            url.append("&");
            url.append(RestClientConstant.CLUSTER_ID);
            url.append("=");
            url.append(clusterId);

            Map<String, Object> param = new HashMap<String, Object>();
            param.put(RestClientConstant.PARAMTER_JSON, RestClientUtil.toJson(attr));

            Map<String, String> result = executor
                    .base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_POST).url(url.toString())
                    .headerWithJson().param(param).execute();

            Session session = null;
            if (null != result && StringUtils.isNotBlank(result.get("result")) && RestClientUtil.success(result)) {
                session = RestClientUtil.toObject(result.get("result"), new TypeReference<Session>() {});
                if (session != null) {
                    log.info(RestClientMessage.getMessage("com.ibm.specturm.SymObj.createSessionSucess", session.getSessionId(), session.getApplicationName()));
                    log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.response", result.get("result")));
                    session.setConnection(this);
                } else {
                    log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionFailed", "Create session"));
                    throw new Exception();
                }
            } else if (result != null) {
                printError(result, "Create Session");
                throw new Exception();
            } else {
                log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionFailed", "Create session"));
                throw new Exception();
            }
            return session;
        }

        public RESTRequestExecutor getConn() {
            return executor;
        }

        public String getBaseUrl() {
            return this.baseUrl;
        }

        private void authenticate() {
            Map<String, Object> authInfo = new HashMap<String, Object>();
            authInfo.put("username", "Admin");
            authInfo.put("passowrd", "Admin");
            executor.base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_POST).authBasic(authInfo,
                    proxy);
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Session implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("sessionId")
        private String sessionId = "";

        @JsonProperty("applicationName")
        private String applicationName = "";

        @JsonProperty("clusterId")
        private String clusterId;

        private Connection connection;

        private TaskSubmissionAttributes taskAttr;

        public TaskInputHandle sendTaskInput() throws Exception {
            log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.submitTask", sessionId, applicationName));
            StringBuilder url = new StringBuilder(50);
            url.append(connection.getBaseUrl());
            url.append(RestClientConstant.SUBMIT_TASK_PATH_INFO);
            url.append("?");
            url.append(RestClientConstant.APP_NAME);
            url.append("=");
            url.append(applicationName);
            url.append("&");
            url.append(RestClientConstant.SESSION_ID);
            url.append("=");
            url.append(sessionId);
            url.append("&");
            url.append(RestClientConstant.CLUSTER_ID);
            url.append("=");
            url.append(clusterId);

            Map<String, Object> param = new HashMap<String, Object>();
            param.put(RestClientConstant.PARAMTER_JSON, RestClientUtil.toJson(taskAttr));

            Map<String, String> result = connection.getConn()
                    .base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_PUT).url(url.toString())
                    .headerWithJson().param(param).execute();

            TaskInputHandle taskInputHandle = null;
            if (null != result && StringUtils.isNotBlank(result.get("result")) && RestClientUtil.success(result)) {
                taskInputHandle = RestClientUtil.toObject(result.get("result"), new TypeReference<TaskInputHandle>() {});
                if (taskInputHandle == null) {
                    log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionFailed", "Submit task"));
                    throw new Exception();
                } else {
                    log.info(RestClientMessage.getMessage("com.ibm.specturm.SymObj.submitTaskSuccess", sessionId, taskInputHandle.getTaskIds()));
                    log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.response", result.get("result")));
                }
            } else if (result != null) {
                printError(result, "Submit task");
                throw new Exception();
            } else {
                log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionFailed", "Submit task"));
                throw new Exception();
            }
            return taskInputHandle;
        }

        public TaskOutputHandle fetchTaskOutput(int count, String filter, TaskOutputFormat taskOutputFormat)
                throws Exception {
            log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.fetchResult", sessionId, applicationName));
            StringBuilder url = new StringBuilder(50);
            url.append(connection.getBaseUrl());
            url.append(RestClientConstant.FETCH_RESULT_PATH_INFO);
            url.append("?");
            url.append(RestClientConstant.APP_NAME);
            url.append("=");
            url.append(applicationName);
            url.append("&");
            url.append(RestClientConstant.SESSION_ID);
            url.append("=");
            url.append(sessionId);
            url.append("&");
            url.append(RestClientConstant.CLUSTER_ID);
            url.append("=");
            url.append(clusterId);
            url.append("&");
            url.append(RestClientConstant.COUNT_MAX);
            url.append("=");
            url.append(count);
            url.append("&");
            url.append(RestClientConstant.FILTER);
            url.append("=");
            url.append(filter);

            Map<String, Object> param = new HashMap<String, Object>();
            param.put(RestClientConstant.PARAMTER_JSON, RestClientUtil.toJson(taskOutputFormat));

            Map<String, String> result = connection.getConn()
                    .base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_PUT).url(url.toString())
                    .headerWithJson().param(param).execute();

            TaskOutputHandle taskOuputHandle = null;
            if (null != result && StringUtils.isNotBlank(result.get("result")) && RestClientUtil.success(result)) {
                taskOuputHandle = RestClientUtil.toObject(result.get("result"), new TypeReference<TaskOutputHandle>() {});
                if (taskOuputHandle == null) {
                    log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionFailed", "Fetch task result"));
                    throw new Exception();
                } else {
                    log.info(RestClientMessage.getMessage("com.ibm.specturm.SymObj.fetchTaskResultSuccess", sessionId));
                    log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.response", result.get("result")));
                }
            } else if (result != null) {
                printError(result, "Fetch task result");
                throw new Exception();
            } else {
                log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionFailed", "Fetch task result"));
                throw new Exception();
            }
            return taskOuputHandle;
        }

        public TaskOutputHandle fetchTaskOutput(int count, TaskOutputFormat taskOutputFormat) throws Exception {
            return fetchTaskOutput(count, "", taskOutputFormat);
        }

        public void closeSession(String closeFlag)  {
            log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.closeSession", sessionId));
            StringBuilder url = new StringBuilder(50);
            url.append(connection.getBaseUrl());
            url.append(RestClientConstant.DELETE_SESSION_PATH_INFO);
            url.append("?");
            url.append(RestClientConstant.APP_NAME);
            url.append("=");
            url.append(applicationName);
            url.append("&");
            url.append(RestClientConstant.SESSION_ID);
            url.append("=");
            url.append(sessionId);
            url.append("&");
            url.append(RestClientConstant.CLUSTER_ID);
            url.append("=");
            url.append(clusterId);
            url.append("&");
            url.append(RestClientConstant.CLOSE_FLAG);
            url.append("=");
            url.append(closeFlag);

            try {
                connection.getConn().base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_DELETE)
                        .url(url.toString()).headerWithJson().execute();
                log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.closeSessionSuccess", sessionId));
            } catch (Exception e) {
                log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionFailed", "Close session"));
            }
        }

        public void closeSession() {
            closeSession("");
        }

        public void addTaskInput(TaskSubmissionAttributes input) {
            this.taskAttr = input;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public String getClusterId() {
            return clusterId;
        }

        public void setClusterId(String clusterId) {
            this.clusterId = clusterId;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionCreationAttributes implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("sessionName")
        private String sessionName;

        @JsonProperty("sessionType")
        private String sessionType;

        public String getSessionName() {
            return sessionName;
        }

        public void setSessionName(String sessionName) {
            this.sessionName = sessionName;
        }

        public String getSessionType() {
            return sessionType;
        }

        public void setSessionType(String sessionType) {
            this.sessionType = sessionType;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskSubmissionAttributes implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("taskTag")
        private String taskTag;

        @JsonProperty("taskPriority")
        private Integer taskPriority;

        @JsonProperty("inputData")
        private List<String[]> taskInput;

        @JsonProperty("taskInputList")
        private List<TaskSubmissionAttributes> taskAttrlist;

        public List<TaskSubmissionAttributes> getTaskAttrlist() {
            return taskAttrlist;
        }

        public void setTaskAttrlist(List<TaskSubmissionAttributes> taskAttrlist) {
            this.taskAttrlist = taskAttrlist;
        }

        public void setTaskInput(List<String[]> taskInput) {
            this.taskInput = taskInput;
        }

        public String getTaskTag() {
            return taskTag;
        }

        public void setTaskTag(String taskTag) {
            this.taskTag = taskTag;
        }

        public Integer getTaskPriority() {
            return taskPriority;
        }

        public void setTaskPriority(Integer taskPriority) {
            this.taskPriority = taskPriority;
        }

    }

    static abstract class Message {
        public String[] onSerialize(Object o) {
            if (o instanceof Integer) {
                return new String[] { "SOAM_INT32", o.toString() };
            } else if (o instanceof Long) {
                return new String[] { "SOAM_UINT64", o.toString() };
            } else if (o instanceof Boolean) {
                return new String[] { "SOAM_BOOL", Boolean.parseBoolean(o.toString()) ? "true" : "false" };
            } else if (o instanceof Byte[]) {
                return new String[] { "SOAM_DATA_BLOCK", "" };
            } else {
                return null;
            }
        }

        public Object onDeserialize(String type, Object value) {
            return null;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskInputHandle implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("taskId")
        private String taskId;

        @JsonProperty("errorMessage")
        private String errorMessage;

        @JsonProperty("taskIds")
        private List<TaskInputHandle> taskIds;

        public String getTaskId() {
            return taskId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public List<TaskInputHandle> getTaskIds() {
            return taskIds;
        }

        @Override
        public String toString() {
            return "taskId=" + taskId + ", errorMessage=" + errorMessage;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskOutputHandle implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("taskOutputList")
        private List<TaskOutputHandle> outputList;

        @JsonProperty("taskTag")
        public String taskTag;

        @JsonProperty("lastTaskRecoveryId")
        public String lastTaskRecoveryId;

        @JsonProperty("outputData")
        public List<String> outputData;

        @JsonProperty("taskId")
        private String taskId;

        private String errorMessage;

        private boolean isSuccsss;

        public boolean isSuccsss() {
            return isSuccsss;
        }

        @JsonProperty("errorMessage")
        public void setErrorMessage(String message) {
            this.errorMessage = message;
            if (StringUtils.isBlank(errorMessage)) {
                isSuccsss = true;
            } else {
                isSuccsss = false;
            }
        }

        public void setSuccsss(boolean isSuccsss) {
            this.isSuccsss = isSuccsss;
        }

        public List<TaskOutputHandle> getOutputList() {
            return outputList;
        }

        public String getTaskTag() {
            return taskTag;
        }

        public String getLastTaskRecoveryId() {
            return lastTaskRecoveryId;
        }

        public List<String> getOutputData() {
            return outputData;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskOutputFormat implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("taskOutputFormat")
        private List<String> format = new LinkedList<String>();

        public void addFormat(String s) {
            format.add(s);
        }
    }

}
