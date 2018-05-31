package com.angkorteam.tomcat.openssl;

import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.AbstractSSLUtilBase;
import org.apache.tomcat.util.net.SSLContext;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.openssl.ExposeOpenSSLKeyManager;
import org.apache.tomcat.util.net.openssl.OpenSSLContext;
import org.apache.tomcat.util.net.openssl.OpenSSLEngine;

import com.angkorteam.tomcat.jsse.JsseUtil;

public class OpenSSLUtil extends AbstractSSLUtilBase {

    private static final Log log = LogFactory.getLog(OpenSSLUtil.class);

    private final JsseUtil jsseUtil;

    public OpenSSLUtil(SSLHostConfigCertificate certificate) {
        super(certificate);

        if (certificate.getCertificateFile() == null) {
            // Using JSSE configuration for keystore and truststore
            jsseUtil = new JsseUtil(certificate);
        } else {
            // Use OpenSSL configuration for certificates
            jsseUtil = null;
        }
    }

    @Override
    protected Log getLog() {
        return log;
    }

    @Override
    protected Set<String> getImplementedProtocols() {
        return OpenSSLEngine.IMPLEMENTED_PROTOCOLS_SET;
    }

    @Override
    protected Set<String> getImplementedCiphers() {
        return OpenSSLEngine.AVAILABLE_CIPHER_SUITES;
    }

    @Override
    public SSLContext createSSLContext(List<String> negotiableProtocols) throws Exception {
        return new OpenSSLContext(certificate, negotiableProtocols);
    }

    @Override
    public KeyManager[] getKeyManagers() throws Exception {
        if (jsseUtil != null) {
            return jsseUtil.getKeyManagers();
        } else {
            // Return something although it is not actually used
            KeyManager[] managers = { new ExposeOpenSSLKeyManager(SSLHostConfig.adjustRelativePath(certificate.getCertificateFile()), SSLHostConfig.adjustRelativePath(certificate.getCertificateKeyFile())) };
            return managers;
        }
    }

    @Override
    public TrustManager[] getTrustManagers() throws Exception {
        if (jsseUtil != null) {
            return jsseUtil.getTrustManagers();
        } else {
            return null;
        }
    }

    @Override
    public void configureSessionContext(SSLSessionContext sslSessionContext) {
        if (jsseUtil != null) {
            jsseUtil.configureSessionContext(sslSessionContext);
        }
    }

}
