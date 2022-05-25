/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.loadbalance;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.platform.symphony.samples.CloudProxyClient.model.Proxy;
import com.platform.symphony.samples.CloudProxyClient.model.RestClientConfig;

/**
 *
 *
 */
public class RandomLoadBalancer implements LoadBalanceStrategy {
    private static RandomLoadBalancer instance; // singleton
    private static List<Proxy> proxyList = new LinkedList<Proxy>();

    private RandomLoadBalancer() {
        String[] proxies = RestClientConfig.getInstance().getProxies();
        for (int i=0 ; i<proxies.length ; i++) {
            proxyList.add(new Proxy(proxies[i]));
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.spectrum.loadbalance.LoadBalanceStrategy#choiceOne()
     */
    @Override
    public Proxy choiceOne() {
        if (proxyList != null && proxyList.size() != 0) {
            Random r = new Random();
            return proxyList.get(r.nextInt(proxyList.size()));
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.spectrum.loadbalance.LoadBalanceStrategy#removeOne(com.ibm.spectrum.model.Proxy)
     */
    @Override
    public void removeOne(Proxy p) {
        proxyList.remove(p);
    }


    public static RandomLoadBalancer getInstance() {
        if (instance == null) {
            instance = new RandomLoadBalancer();
        }
        return instance;
    }


}
