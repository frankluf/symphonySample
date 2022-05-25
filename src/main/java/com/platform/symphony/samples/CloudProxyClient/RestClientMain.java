/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.platform.symphony.samples.CloudProxyClient;

import java.util.Map;

import com.platform.symphony.samples.CloudProxyClient.api.sympingSample;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientUtil;

/**
 * The entrance of this program
 *
 *
 */
public class RestClientMain {
    public static void main(String[] args) {
        // Parse command line parameter
        Map<String, String> commandLineParameter = RestClientUtil.parseArgs(args);

        if (!commandLineParameter.isEmpty()) {
            //  seperate rest request via comand line
            RestClientUtil.invokeSymAPI(commandLineParameter);
        } else {
            // run symping application as sample
            sympingSample symping = new sympingSample();
            symping.run();
        }
    }
}