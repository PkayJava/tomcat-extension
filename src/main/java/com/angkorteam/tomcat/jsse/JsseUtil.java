package com.angkorteam.tomcat.jsse;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.compat.JreVendor;
import org.apache.tomcat.util.net.Constants;
import org.apache.tomcat.util.net.AbstractSSLUtilBase;
import org.apache.tomcat.util.net.SSLContext;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.jsse.ExposeJSSESSLContext;
import org.apache.tomcat.util.res.StringManager;

public class JsseUtil extends AbstractSSLUtilBase {

    private static final Log log = LogFactory.getLog(JsseUtil.class);
    private static final StringManager sm = StringManager.getManager(JsseUtil.class);

    private static final Set<String> implementedProtocols;
    private static final Set<String> implementedCiphers;

    static {
        SSLContext context;
        try {
            context = new ExposeJSSESSLContext(Constants.SSL_PROTO_TLS);
            context.init(null, null, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // This is fatal for the connector so throw an exception to prevent
            // it from starting
            throw new IllegalArgumentException(e);
        }

        String[] implementedProtocolsArray = context.getSupportedSSLParameters().getProtocols();
        implementedProtocols = new HashSet<>(implementedProtocolsArray.length);

        // Filter out SSLv2 from the list of implemented protocols (just in case
        // we are running on a JVM that supports it) since it is no longer
        // considered secure but allow SSLv2Hello.
        // Note SSLv3 is allowed despite known insecurities because some users
        // still have a requirement for it.
        for (String protocol : implementedProtocolsArray) {
            String protocolUpper = protocol.toUpperCase(Locale.ENGLISH);
            if (!"SSLV2HELLO".equals(protocolUpper) && !"SSLV3".equals(protocolUpper)) {
                if (protocolUpper.contains("SSL")) {
                    log.debug(sm.getString("jsse.excludeProtocol", protocol));
                    continue;
                }
            }
            implementedProtocols.add(protocol);
        }

        if (implementedProtocols.size() == 0) {
            log.warn(sm.getString("jsse.noDefaultProtocols"));
        }

        String[] implementedCipherSuiteArray = context.getSupportedSSLParameters().getCipherSuites();
        // The IBM JRE will accept cipher suites names SSL_xxx or TLS_xxx but
        // only returns the SSL_xxx form for supported cipher suites. Therefore
        // need to filter the requested cipher suites using both forms with an
        // IBM JRE.
        if (JreVendor.IS_IBM_JVM) {
            implementedCiphers = new HashSet<>(implementedCipherSuiteArray.length * 2);
            for (String name : implementedCipherSuiteArray) {
                implementedCiphers.add(name);
                if (name.startsWith("SSL")) {
                    implementedCiphers.add("TLS" + name.substring(3));
                }
            }
        } else {
            implementedCiphers = new HashSet<>(implementedCipherSuiteArray.length);
            implementedCiphers.addAll(Arrays.asList(implementedCipherSuiteArray));
        }
    }

    public JsseUtil(SSLHostConfigCertificate certificate) {
        super(certificate);
    }

    @Override
    protected Log getLog() {
        return log;
    }

    @Override
    protected Set<String> getImplementedProtocols() {
        return implementedProtocols;
    }

    @Override
    protected Set<String> getImplementedCiphers() {
        return implementedCiphers;
    }

    @Override
    public SSLContext createSSLContext(List<String> negotiableProtocols) throws NoSuchAlgorithmException {
        return new ExposeJSSESSLContext(sslHostConfig.getSslProtocol());
    }

}
