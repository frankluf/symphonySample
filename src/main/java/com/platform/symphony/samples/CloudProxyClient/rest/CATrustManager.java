/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.rest;

import java.io.FileInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import com.platform.symphony.samples.CloudProxyClient.constant.RestClientConstant;
import com.platform.symphony.samples.CloudProxyClient.model.RestClientConfig;
import com.platform.symphony.samples.CloudProxyClient.util.RestClientMessage;

/**
 * This class is for server CA certificate verification
 *
 */
class CATrustManager implements X509TrustManager {
    private static Logger log = Logger.getLogger(CATrustManager.class);
    private X509Certificate caCertificate;
    private String restServer;

    public CATrustManager(String restServer) {
        this.restServer = restServer;
        init();
    }

    /**
     * Init the caCertificate
     */
    private void init() {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(RestClientConstant.CA_CERT_TYPE);
            FileInputStream fis = null;
            if (restServer.equals(RestClientConstant.ETCD_SERVER_NAME)) {
                fis = new FileInputStream(RestClientConfig.getInstance().getCafileForETCD());
                log.debug("The certificate file for ETCD is in the disk: "+RestClientConfig.getInstance().getCafileForETCD());
            } else if(restServer.equals(RestClientConstant.PRXOY_SERVER_NAME)) {
                fis = new FileInputStream(RestClientConfig.getInstance().getCafileForProxy());
                log.debug("The certificate file for Proxy is in the disk: "+RestClientConfig.getInstance().getCafileForProxy());
            }
            caCertificate = (X509Certificate) factory.generateCertificate(fis);
        } catch (Exception e) {
        }
    }


    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    @Override
    public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
            throws CertificateException {
    }

    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        if (!(chain.length > 0) || chain == null) {
            throw new IllegalArgumentException(RestClientMessage.getMessage("com.ibm.spectrum.CATrustManager.certificateFailed1"));
        }
        for (X509Certificate cert : chain) {
            if (!cert.equals(caCertificate)) {
                try {
                    cert.verify(caCertificate.getPublicKey());
                } catch (Exception e) {
                    log.error(RestClientMessage.getMessage("com.ibm.spectrum.CATrustManager.certificateFailed2"),e);
                    throw new CertificateException(e);
                }
            }
            try {
                cert.checkValidity();
            } catch(Exception ex) {
                if (ex instanceof CertificateExpiredException) {
                    log.error(RestClientMessage.getMessage("com.ibm.spectrum.CATrustManager.certificateFailed3"));
                } else {
                    log.error(RestClientMessage.getMessage("com.ibm.spectrum.CATrustManager.certificateFailed4"));
                }
                throw new CertificateException(ex);
            }
        }

    }

    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

}
