/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.loadbalance;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.platform.symphony.samples.CloudProxyClient.constant.RestClientConstant;
import com.platform.symphony.samples.CloudProxyClient.model.ETCDResponse;
import com.platform.symphony.samples.CloudProxyClient.model.Proxy;
import com.platform.symphony.samples.CloudProxyClient.model.RestClientConfig;
import com.platform.symphony.samples.CloudProxyClient.rest.RESTRequestExecutor;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientMessage;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientUtil;

/**
 * ETCD load balancer
 * It's a backend thread to pull the proxy list from ETCD Server at regular intervals
 */
public class ETCDLoadBalancer extends Thread implements LoadBalanceStrategy {
    private static ETCDLoadBalancer instance; // singleton
    private static Logger log = Logger.getLogger(ETCDLoadBalancer.class);
    private final static int PULL_INTERVAL = 5;
    private final static int PULL_FIRST_DELAY = -1;
    private final static TimeUnit TIME_UNIT = TimeUnit.MINUTES;
    private List<Proxy> proxyList = new LinkedList<Proxy>();
    private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);


    private ETCDLoadBalancer() {
        // get the proxy list immediately
        refreshProxyInfo();
        // schedule fixed rate to get the refresh list from ETCD Server
        exec.scheduleAtFixedRate(this, PULL_FIRST_DELAY, PULL_INTERVAL, TIME_UNIT);
    }

    @Override
    public void run() {
        refreshProxyInfo();
    }

    /* (non-Javadoc)
     * @see com.ibm.spectrum.loadbalance.LoadBalanceStrategy#choiceOne()
     */
    @Override
    public Proxy choiceOne() {
        // find the min load proxy via load info
        return Collections.min(proxyList, new Comparator<Proxy>() {
            @Override
            public int compare(Proxy proxy1, Proxy proxy2) {
                if (proxy1 != null && proxy2 != null) {
                    return new Float(proxy1.getLoad()).compareTo(new Float(proxy2.getLoad()));
                }
                return 0;
            }
        });
    }

    /* (non-Javadoc)
     * @see com.ibm.spectrum.loadbalance.LoadBalanceStrategy#removeOne(com.ibm.spectrum.model.Proxy)
     */
    @Override
    public void removeOne(Proxy p) {
        proxyList.remove(p);
    }

    public static ETCDLoadBalancer getInstance() {
        if (instance == null) {
            instance = new ETCDLoadBalancer();
        }
        return instance;
    }

    public void shutdownNow() {
        if (this.exec != null && !this.exec.isTerminated())
            this.exec.shutdownNow();
    }


    private synchronized void refreshProxyInfo() {
        try {
            // Rest request for pull the proxy list from ETCD Server
            log.debug("Start to send request to ETCD Server to get Proxy List. Url is: "+ RestClientConfig.getInstance().getUrl());
            Map<String, String> etcdResponse = RESTRequestExecutor.getInstance()
                 .base(RestClientConstant.ETCD_SERVER_NAME, RestClientConstant.HTTP_GET)
                 .url(RestClientConfig.getInstance().getUrl()+RestClientConstant.ETCD_SERVER_URI)
                 .execute();
            log.debug("Finish to get Proxy List. Response is: "+ etcdResponse);
            if (RestClientUtil.success(etcdResponse)) {
                ETCDResponse etcd = RestClientUtil.toObject(etcdResponse.get("result"), ETCDResponse.class);
                // remove the old list
                proxyList.clear();
                List<LinkedHashMap<String, Object>> list = etcd.getNode().getItem();
                String pathAndLoad = null;
                String path = null;
                float load = 0.0f;
                for (LinkedHashMap<String, Object> map : list) {
                    if (map.get("value") != null) {
                        pathAndLoad = map.get("value").toString();
                        path = pathAndLoad.split(RestClientConstant.SEPARATOR)[0];
                        load = Float.parseFloat(pathAndLoad.split(RestClientConstant.SEPARATOR)[1]);
                        // load is -1 means the proxy is down or busy.
                        if (load != -1)
                            proxyList.add(new Proxy(path,load));
                    }
                }
            }
            log.info(RestClientMessage.getMessage("com.ibm.spectrum.ETCDLoadBalancer.proxyList", proxyList));
        } catch (Exception e) {
            log.error(RestClientMessage.getMessage("com.ibm.spectrum.ETCDLoadBalancer.getProxyListFailed", RestClientConfig.getInstance().getUrl()), e);
            exec.shutdownNow();
        }
    }


}
