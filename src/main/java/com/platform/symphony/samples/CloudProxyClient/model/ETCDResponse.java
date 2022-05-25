/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.model;

import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ETCDResponse {
    @JsonProperty("action")
    private String action;

    @JsonProperty("node")
    private Node node;


    public String getAction() {
        return action;
    }

    public Node getNode() {
        return node;
    }


    @Override
    public String toString() {
        return "Test [action=" + action + ", node=" + node + "]";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Node {
        @JsonProperty("nodes")
        private List<LinkedHashMap<String, Object>> item;

        @Override
        public String toString() {
            return "Node [item=" + item + "]";
        }

        public List<LinkedHashMap<String, Object>> getItem() {
            return item;
        }
    }


}
