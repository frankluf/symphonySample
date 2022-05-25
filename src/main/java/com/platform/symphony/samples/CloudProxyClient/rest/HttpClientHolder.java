/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.rest;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import com.platform.symphony.samples.CloudProxyClient.constant.RestClientConstant;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientMessage;

/**
 * This class is the implement of http communicator.
 *
 */
final class HttpClientHolder {
    private static Logger log = Logger.getLogger(HttpClientHolder.class);
    private static Map<String, ImmutablePair<CloseableHttpClient, HttpClientContext>> httpClientMap = new HashMap<String, ImmutablePair<CloseableHttpClient, HttpClientContext>>();

    private HttpClientHolder() {

    }

    /**
     * Create a new HttpClient with custom TrustManager. Support to cache the old httpClient
     *
     * @param restServer
     *            ETCD rest server or Proxy rest server
     * @return
     */
    public static ImmutablePair<CloseableHttpClient, HttpClientContext> build(String restServer) {
        ImmutablePair<CloseableHttpClient, HttpClientContext> pair = null;
        try {
            if (restServer.equals(RestClientConstant.ETCD_SERVER_NAME)) {
                pair = httpClientMap.get(RestClientConstant.ETCD_SERVER_NAME);
            } else if (restServer.equals(RestClientConstant.PRXOY_SERVER_NAME)) {
                pair = httpClientMap.get(RestClientConstant.PRXOY_SERVER_NAME);
            }
            if (pair == null) {
                HttpClientBuilder builder = HttpClientBuilder.create();

                SSLContext ctx = SSLContext.getInstance(RestClientConstant.SSL_PROTOCOL);
                // Custom TrustManager
                X509TrustManager tm = new CATrustManager(restServer);
                ctx.init(null, new TrustManager[] { tm }, new SecureRandom());
                LayeredConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(ctx);

                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                        .<ConnectionSocketFactory> create().register("https", sslSocketFactory).register("http", PlainConnectionSocketFactory.INSTANCE).build();
                PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(
                        socketFactoryRegistry);

                // cookie strategy
                RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
                // for persist cookie
                CookieStore cookieStore = new BasicCookieStore();
                // create context
                HttpClientContext httpContext = HttpClientContext.create();
                httpContext.setCookieStore(cookieStore);

                builder.setConnectionManager(connMgr);
                builder.setDefaultCookieStore(cookieStore);
                builder.setDefaultRequestConfig(requestConfig);
                builder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
                builder.disableContentCompression();
                builder.disableAuthCaching();

                pair = new ImmutablePair<CloseableHttpClient, HttpClientContext>(builder.build(), httpContext);
                httpClientMap.put(restServer, pair);
            }
        } catch (Exception e) {
            log.error(RestClientMessage.getMessage("com.ibm.spectrum.HttpClientHolder.createHttpClientFailed"), e);
        }

        return pair;
    }

    public static void close(String restServer) {
        try {
            if (null != httpClientMap.get(restServer)) {
                // close the HttpClient
                httpClientMap.get(restServer).left.close();
                log.debug("Close the connection with "+restServer+" successfully");
            }
        } catch (IOException e) {
            log.debug("Close the connection with "+restServer+" failed.");
        }
        // Remove the ImmutablePair from cache
        if (httpClientMap.containsKey(restServer)) {
            log.debug("Remove the HTTPClient Object for "+restServer+" from the cache");
            httpClientMap.remove(restServer);
        }
    }
}
