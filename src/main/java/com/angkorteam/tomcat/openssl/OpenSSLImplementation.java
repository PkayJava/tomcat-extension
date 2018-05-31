package com.angkorteam.tomcat.openssl;

import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLUtil;

public class OpenSSLImplementation extends org.apache.tomcat.util.net.openssl.OpenSSLImplementation {

    @Override
    public SSLUtil getSSLUtil(SSLHostConfigCertificate certificate) {
        return new OpenSSLUtil(certificate);
    }

}
