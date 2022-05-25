/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.model;

/**
 * Rest Server(Proxy) entity
 *
 */
public class Proxy {
    private String path;
    private float load;

    public Proxy() {
    }

    public Proxy(String path) {
        this.path = path;
    }

    public Proxy(String path, float load) {
        this.path = path;
        this.load = load;
    }

    public String getPath() {
        return path;
    }

    public float getLoad() {
        return load;
    }

    @Override
    public String toString() {
        return "Proxy [path=" + path + ", load=" + load + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Proxy other = (Proxy) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }



}
