package com.angkorteam.tomcat.bouncy;

import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLUtil;
import org.apache.tomcat.util.net.jsse.JSSEImplementation;

public class BouncySSLImplementation extends JSSEImplementation {

    @Override
    public SSLUtil getSSLUtil(SSLHostConfigCertificate certificate) {
        return new BouncySSLUtil(certificate);
    }

}
