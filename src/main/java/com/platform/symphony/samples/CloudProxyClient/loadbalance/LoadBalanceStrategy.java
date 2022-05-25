/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.loadbalance;

import com.platform.symphony.samples.CloudProxyClient.model.Proxy;

/**
 * Load Balance strategy
 *
 */
public interface LoadBalanceStrategy {
    // find a new proxy.
    public Proxy choiceOne();

    // remove a unavailable proxy
    public void removeOne(Proxy p);
}
