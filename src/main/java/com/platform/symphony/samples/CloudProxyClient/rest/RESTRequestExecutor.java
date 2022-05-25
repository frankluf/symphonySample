/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.platform.symphony.samples.CloudProxyClient.constant.RestClientConstant;
import com.platform.symphony.samples.CloudProxyClient.loadbalance.ETCDLoadBalancer;
import com.platform.symphony.samples.CloudProxyClient.model.Proxy;
import com.platform.symphony.samples.CloudProxyClient.model.RestClientConfig;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientMessage;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientUtil;

/**
 * This class is for build http request and send request to server.
 *
 */
public class RESTRequestExecutor {
    private static Logger log = Logger.getLogger(RESTRequestExecutor.class);
    private static RESTRequestExecutor instance; // singleton

    /**
     * Generate HttpClient object and HttpMethod Object
     * @param restServer  which restServer be connected
     * @param method  which HTTP method be used
     * @return
     */
    public Builder base(String restServer, String method) {
        log.debug("The connected rest server type is: ["+restServer+"]");
        log.debug("The HTTP method is : ["+method+"]");

        // validate rest server name
        if (!(restServer.equals(RestClientConstant.ETCD_SERVER_NAME) || restServer.equals(RestClientConstant.PRXOY_SERVER_NAME))) {
            throw new IllegalArgumentException(RestClientMessage.getMessage("com.ibm.spectrum.RESTRequestExecutor.restServerTypeInvalid", restServer));
        }

        HttpRequestBase httpRequest = null;
        if (method.equals(RestClientConstant.HTTP_GET)) {
            httpRequest = new HttpGet();
        } else if (method.equals(RestClientConstant.HTTP_POST)) {
            httpRequest = new HttpPost();
        } else if (method.equals(RestClientConstant.HTTP_DELETE)) {
            httpRequest = new HttpDelete();
        } else if (method.equals(RestClientConstant.HTTP_PUT)) {
            httpRequest = new HttpPut();
        }else {
            throw new IllegalArgumentException(RestClientMessage.getMessage("com.ibm.spectrum.RESTRequestExecutor.unsupportHttpMethod", method));
        }

        // The HttpClient holder will cache httpClient/httpContext object for each server.
        ImmutablePair<CloseableHttpClient, HttpClientContext> pair = HttpClientHolder.build(restServer);
        return new Builder(pair.left, pair.right, httpRequest);
    }

    /**
     * Close all connection of rest serever
     */
    public void clean() {
        log.debug("Starting to clean up...");
        HttpClientHolder.close(RestClientConstant.PRXOY_SERVER_NAME);
        HttpClientHolder.close(RestClientConstant.ETCD_SERVER_NAME);
        if (RestClientConfig.getInstance().getPolicy().equals(RestClientConstant.FAILOVER_POLICY_ETCD)) {
            ETCDLoadBalancer.getInstance().shutdownNow();
        }
        log.debug("End for clean up...");
    }

    public static RESTRequestExecutor getInstance() {
        if (instance == null) {
            instance = new RESTRequestExecutor();
        }
        return instance;
    }

    /**
     * Build the rest reuqest
     *
     *
     */
    public class Builder {
        private HttpRequestBase httpRequest;
        private CloseableHttpClient httpClient;
        private HttpClientContext httpContext;
        private ResponseHandler<Map<String, String>> handler = new SYMResponseHandler();

        public Builder(CloseableHttpClient httpClient, HttpClientContext httpContext, HttpRequestBase httpRequest) {
            this.httpRequest = httpRequest;
            this.httpClient = httpClient;
            this.httpContext = httpContext;
        }

        public Builder url(String url) throws URISyntaxException {
            log.info(RestClientMessage.getMessage("com.ibm.spectrum.RESTRequestExecutor.url", url));
            httpRequest.setURI(new URI(url));
            return this;
        }

        /**
         * Fill the http request parameter
         * @param param
         * @return
         * @throws ParseException
         * @throws IOException
         * @throws URISyntaxException
         */
        public Builder param(Map<String, Object> param) throws ParseException, IOException, URISyntaxException {
            if (httpRequest instanceof HttpGet || httpRequest instanceof HttpDelete) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                for (Entry<String, Object> entry : param.entrySet()) {
                    log.debug("The request parameter is: "+ entry.getKey() +" , "+entry.getValue());
                    params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
                }
                String paramString = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
                log.debug("The encoded request parameter is: "+paramString);
                httpRequest.setURI(new URI(httpRequest.getURI().toString()+"?"+paramString));
            }

            if (httpRequest instanceof HttpPost || httpRequest instanceof HttpPut) {
                if (param.get(RestClientConstant.PARAMTER_JSON) != null) {
                    String inputJson = param.get(RestClientConstant.PARAMTER_JSON).toString();
                    StringEntity inputJsonEntitiy = new StringEntity(inputJson);
                    log.debug("The HTTP request entity is: \n"+RestClientUtil.prettyJson(inputJson)+"\nContent-Type is: "+inputJsonEntitiy);
                    if (httpRequest instanceof HttpPost) {
                        HttpPost post = (HttpPost)httpRequest;
                        post.setEntity(inputJsonEntitiy);
                    } else {
                        HttpPut put = (HttpPut)httpRequest;
                        put.setEntity(inputJsonEntitiy);
                    }
                } else {
                    log.debug("No HTTP entity is set");
                }
            }
            return this;
        }


        /**
         * fill the header
         * @param header
         * @return
         */
        public Builder header(Map<String, String> header) {
            for (Entry<String, String> entry : header.entrySet()) {
                httpRequest.addHeader(entry.getKey(), entry.getValue());
            }
            log.debug("The HTTP request header is: \n"+ArrayUtils.toString(httpRequest.getAllHeaders()));
            return this;
        }

        public Builder headerWithJson() {
            httpRequest.addHeader("Content-Type", RestClientConstant.JSON_TYPE);
            log.debug("The HTTP request header is: \n"+ArrayUtils.toString(httpRequest.getAllHeaders()));
            return this;
        }

        public Builder authBasic(Map<String, Object> authInfo, Proxy proxy) {
            log.debug("Authenticate with Basic schema");
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(authInfo.get("username")+"", authInfo.get("passowrd")+""));

            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();

            String fullpath = proxy.getPath();
            String afterHostPath = fullpath.substring(fullpath.lastIndexOf(":")+1);
            int idx = afterHostPath.indexOf("/")+fullpath.lastIndexOf(":")+1;
            HttpHost targetHost = HttpHost.create(fullpath.substring(0, idx));
            authCache.put(targetHost, basicAuth);

            httpContext.setCredentialsProvider(credsProvider);
            httpContext.setAuthCache(authCache);

            return this;
        }


        /**
         * start to send request.
         * @return
         * @throws ClientProtocolException
         * @throws IOException
         */
        public Map<String, String> execute() throws ClientProtocolException, IOException  {
            log.debug("Start to send HTTP request...");
            Map<String, String> result = httpClient.execute(httpRequest,handler,httpContext);
            log.debug("End for HTTP request...");
            return result;
        }

    }
}
