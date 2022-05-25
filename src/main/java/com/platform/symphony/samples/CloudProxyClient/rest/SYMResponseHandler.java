/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

/**
 * Handle restful response.
 *
 */
public class SYMResponseHandler implements ResponseHandler<Map<String, String>> {
    /**
     * only handle the content-type is  application/json,
     * the other content-type will be consider as failed.
     */
    @Override
    public Map<String, String> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity();
        Map<String, String> result = new HashMap<String, String>();
        if (entity != null) {
            result.put("status", response.getStatusLine().getStatusCode()+"");
            result.put("result", EntityUtils.toString(entity));
        } else {
            result.put("status", "404");
            result.put("result", "");
        }
        return result;
    }

}
