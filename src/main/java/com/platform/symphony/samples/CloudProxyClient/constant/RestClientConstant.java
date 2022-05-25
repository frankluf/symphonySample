/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.constant;


public final class RestClientConstant {
    public static final String SYMREST_CLIENT_CONFIG_NAME = "symrest_client.json";
    public static final String CA_CERT_TYPE = "X.509";
    public static final String SSL_PROTOCOL = "TLSv1.2";
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_DELETE = "DELETE";
    public static final String HTTP_PUT = "PUT";
    public static final String ETCD_SERVER_NAME = "ECTD";
    public static final String PRXOY_SERVER_NAME = "PROXY";
    public static final String JSON_TYPE = "application/json";
    public static final String REST_SERVER_URI = "/platform/rest/symrest/v1/clientapi";
    public static final String REST_SERVER_URI_WITHOUT_PREFIX = "v1/clientapi";
    public static final String CREATE_SESSION_PATH_INFO = "/session";
    public static final String SUBMIT_TASK_PATH_INFO = "/session/tasks";
    public static final String FETCH_RESULT_PATH_INFO = "/session/taskresults";
    public static final String DELETE_SESSION_PATH_INFO = "/session";
    public static final String ETCD_SERVER_URI = "/v2/keys/proxies";
    public static final String SEPARATOR = " ";
    public static final String FAILOVER_POLICY_ETCD = "etcd";
    public static final String FAILOVER_POLICY_RANDOM = "random";
    public static final String ACTION_PARAM_NAME = "action";
    public static final String APP_NAME = "applicationName";
    public static final String SESSION_ID = "sessionId";
    public static final String CLUSTER_ID = "clusterId";
    public static final String TASK_ID = "taskId";
    public static final String COUNT_MAX = "countMax";
    public static final String FILTER = "filter";
    public static final String CLOSE_FLAG = "destroyOnCloseFlag";
    public static final String ERROR_MSG = "errorMessage";
    public static final String PARAMTER_JSON = "source";
}
