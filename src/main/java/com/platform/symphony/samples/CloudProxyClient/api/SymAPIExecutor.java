/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

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
 * Sym API for create/submit/fetch result/close
 *
 */
public class SymAPIExecutor {
    private static Logger log = Logger.getLogger(SymAPIExecutor.class);
    private Proxy proxy; // Proxy entity
    private RESTRequestExecutor executor; // Execute REST request
    private LoadBalanceStrategy strategy; // Load Balancer
    public final static int RETRY_TIME = 3; // retry times when failed
    private  String baseUrl;

    public SymAPIExecutor() {
        if (RestClientConfig.getInstance().getPolicy().toLowerCase().equals(RestClientConstant.FAILOVER_POLICY_ETCD)) {
            this.strategy = ETCDLoadBalancer.getInstance();
        } else {
            this.strategy = RandomLoadBalancer.getInstance();
        }
        this.proxy = this.strategy.choiceOne();
        this.executor = RESTRequestExecutor.getInstance();

        StringBuilder url = new StringBuilder(10);
        url.append(proxy.getPath());
        if (proxy.getPath().endsWith("/")) {
            url.append(RestClientConstant.REST_SERVER_URI_WITHOUT_PREFIX);
        } else {
            url.append(RestClientConstant.REST_SERVER_URI);
        }
        baseUrl = url.toString();
    }




    public Map<String, String> execute(Map<String, Object> param, Map<String, String> header) {
        authenticate();

        String action = param.get(RestClientConstant.ACTION_PARAM_NAME).toString();
        Map<String, String> result = null;
        StringBuilder url = new StringBuilder(10);
        try {
            if (action.equals("createSession")) {
                String applicationName = param.get(RestClientConstant.APP_NAME).toString();
                String clusterId = param.get(RestClientConstant.CLUSTER_ID).toString();
                log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.createSession", applicationName));

                url.append(baseUrl);
                url.append(RestClientConstant.CREATE_SESSION_PATH_INFO);
                url.append("?");
                url.append(RestClientConstant.APP_NAME);
                url.append("=");
                url.append(applicationName);
                url.append("&");
                url.append(RestClientConstant.CLUSTER_ID);
                url.append("=");
                url.append(clusterId);

                result = executor.base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_POST)
                        .url(url.toString())
                        .header(header)
                        .param(param)
                        .execute();

            } else if (action.equals("submitTask")) {
                String applicationName = param.get(RestClientConstant.APP_NAME).toString();
                String sessionId = param.get(RestClientConstant.SESSION_ID).toString();
                String clusterId = param.get(RestClientConstant.CLUSTER_ID).toString();
                log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.submitTask", sessionId, applicationName, clusterId));

                url.append(baseUrl);
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

                result = executor.base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_PUT)
                        .url(url.toString())
                        .header(header)
                        .param(param)
                        .execute();

            } else if (action.equals("fetchResult")) {
                String applicationName = param.get(RestClientConstant.APP_NAME).toString();
                String sessionId = param.get(RestClientConstant.SESSION_ID).toString();
                String clusterId = param.get(RestClientConstant.CLUSTER_ID).toString();
                String countMax = param.get(RestClientConstant.COUNT_MAX).toString();
                String filter = param.get(RestClientConstant.FILTER).toString();
                log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.fetchResult", sessionId, applicationName, clusterId));

                url.append(baseUrl);
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
                url.append(countMax);
                url.append("&");
                url.append(RestClientConstant.FILTER);
                url.append("=");
                url.append(filter);

                result = executor.base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_PUT)
                        .url(url.toString())
                        .header(header)
                        .param(param)
                        .execute();

            } else if (action.equals("closeSession")) {
                String applicationName = param.get(RestClientConstant.APP_NAME).toString();
                String clusterId = param.get(RestClientConstant.CLUSTER_ID).toString();
                String sessionId = param.get(RestClientConstant.SESSION_ID).toString();
                String closeFlag = param.get(RestClientConstant.CLOSE_FLAG).toString();
                log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.closeSession", sessionId, applicationName, clusterId));

                url.append(baseUrl);
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

                result = executor.base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_DELETE)
                        .url(url.toString())
                        .header(header)
                        .execute();
            } else {
                System.out.println(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.noSuchAction", action));
                System.exit(1);
            }
        } catch (Exception ex) {
            log.error(action+" failed.",ex);
        }
        if (null != result && RestClientUtil.success(result)) {
            System.out.println(RestClientMessage.getMessage("com.ibm.specturm.SymObj.closeSessionSuccess", param.get(RestClientConstant.SESSION_ID).toString()));
            log.info(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionSucces", action));
        } else {
            if (result != null && StringUtils.isNotBlank(result.get("result"))) {
                log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.error1", action, result.get("result").toString(), result.get("status")));
            } else {
                log.error(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.error2", action,  result.get("status")));
            }
            System.out.println(RestClientMessage.getMessage("com.ibm.specturm.SymAPIExecutor.actionFailed", action));
        }
        return result;
    }


    private void authenticate() {
        Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put("username", "Admin");
        authInfo.put("passowrd", "Admin");
     // Cache authentication info and it will be added to HTTP request automatically as authentication
        executor.base(RestClientConstant.PRXOY_SERVER_NAME, RestClientConstant.HTTP_POST).authBasic(authInfo,
                proxy);

    }
}
