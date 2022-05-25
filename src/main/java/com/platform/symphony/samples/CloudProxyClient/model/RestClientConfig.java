/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.model;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.symphony.samples.CloudProxyClient.constant.RestClientConstant;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientMessage;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientUtil;

/**
 * The entity for symrest_client.json
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestClientConfig {


    @JsonProperty("loadBalancer")
    private LoadBalancer loadBalancer;
    private static RestClientConfig instance; // singleton
    private static Logger log = Logger.getLogger(RestClientConfig.class);

    private RestClientConfig() {

    }



    @Override
    public String toString() {
        return "Config [loadBalancer=" + loadBalancer + "]";
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }


    public String getUrl() {
        return loadBalancer.getUrlMap().get("url");
    }

    public String getCafileForETCD() {
        return loadBalancer.getCafileForETCD();
    }

    public String getCafileForProxy() {
        return loadBalancer.getCafileForProxy();
    }

    public String[] getProxies() {
        return loadBalancer.getProxyList().get("proxies");
    }


    public String getPolicy() {
        return loadBalancer.policy;
    }



    static class LoadBalancer {

        @Override
        public String toString() {
            return "LoadBalancer [policy=" + policy + ", cafileForETCD=" + cafileForETCD + ", cafileForProxy="
                    + cafileForProxy + ", urlMap=" + urlMap + ", proxyList=" + proxyList
                    + "]";
        }

        @JsonProperty("policy")
        private String policy;

        @JsonProperty("cafileForETCD")
        private String cafileForETCD;

        @JsonProperty("cafileForProxy")
        private String cafileForProxy;

        @JsonProperty("ETCD")
        private Map<String, String> urlMap;

        @JsonProperty("clientRandom")
        private Map<String, String[]> proxyList;

        public String getPolicy() {
            return policy;
        }

        public String getCafileForETCD() {
            return cafileForETCD;
        }

        public String getCafileForProxy() {
            return cafileForProxy;
        }

        public Map<String, String> getUrlMap() {
            return urlMap;
        }

        public Map<String, String[]> getProxyList() {
            return proxyList;
        }


    }




    /**
     * RestClientConfig builder
     * @return
     */
    public static RestClientConfig getInstance() {
        if (instance == null) {
            String configFilePath = "";
            // Get config file path from property
            if (StringUtils.isNotBlank(System.getProperty("CONF_DIR"))) {
                configFilePath = System.getProperty("CONF_DIR");
            }
            String symrestClientConfigPath = configFilePath+File.separator+RestClientConstant.SYMREST_CLIENT_CONFIG_NAME;

            log.debug("The configuration file(symrest_client.json) path is: "+symrestClientConfigPath);
            instance =RestClientUtil.toObject(new File(symrestClientConfigPath), RestClientConfig.class);
            if (instance == null) {
                System.out.println(RestClientMessage.getMessage("com.ibm.spectrum.RestClientConfig.initConfigFailed"));
                System.exit(1);
            }

            // check policy
            if (StringUtils.isBlank(instance.getPolicy())) {
                System.out.println(RestClientMessage.getMessage("com.ibm.spectrum.RestClientConfig.policyNotExists"));
                System.exit(1);
            }

            // print the configuration
            log.info(RestClientMessage.getMessage("com.ibm.spectrum.RestClientConfig.policy", instance.getPolicy().equals(RestClientConstant.FAILOVER_POLICY_ETCD)?"ETCD":"Random Client"));
            if (instance.getPolicy().equals(RestClientConstant.FAILOVER_POLICY_ETCD)) {
                log.info(RestClientMessage.getMessage("com.ibm.spectrum.RestClientConfig.url", instance.getUrl()));
            } else if (instance.getPolicy().equals(RestClientConstant.FAILOVER_POLICY_RANDOM)) {
                log.info(RestClientMessage.getMessage("com.ibm.spectrum.RestClientConfig.proxyList", ArrayUtils.toString(instance.getProxies())));
            } else {
                System.out.println(RestClientMessage.getMessage("com.ibm.spectrum.RestClientConfig.policyWrong", instance.getPolicy()));
                System.exit(1);
            }
            log.info(RestClientMessage.getMessage("com.ibm.spectrum.RestClientConfig.caFileForETCD", instance.getCafileForETCD()));
            log.info(RestClientMessage.getMessage("com.ibm.spectrum.RestClientConfig.cafileForProxy", instance.getCafileForProxy()));
        }
        return instance;
    }
}
