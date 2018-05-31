package org.apache.tomcat.util.net.jsse;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;

public class ExposeJSSESSLContext extends JSSESSLContext {

    private javax.net.ssl.SSLContext context;

    public ExposeJSSESSLContext(String protocol) throws NoSuchAlgorithmException {
        super(protocol);
        this.context = javax.net.ssl.SSLContext.getInstance(protocol);
    }

    public ExposeJSSESSLContext(String protocol, Provider provider) throws NoSuchAlgorithmException {
        super(protocol);
        this.context = javax.net.ssl.SSLContext.getInstance(protocol, provider);
    }

    @Override
    public void init(KeyManager[] kms, TrustManager[] tms, SecureRandom sr) throws KeyManagementException {
        if (sr == null) {
            context.init(kms, tms, new SecureRandom());
            super.init(kms, tms, new SecureRandom());
        } else {
            context.init(kms, tms, sr);
            super.init(kms, tms, sr);
        }
    }

    @Override
    public SSLSessionContext getServerSessionContext() {
        return context.getServerSessionContext();
    }

    @Override
    public SSLEngine createSSLEngine() {
        return context.createSSLEngine();
    }

    @Override
    public SSLServerSocketFactory getServerSocketFactory() {
        return context.getServerSocketFactory();
    }

    @Override
    public SSLParameters getSupportedSSLParameters() {
        return context.getSupportedSSLParameters();
    }

}
