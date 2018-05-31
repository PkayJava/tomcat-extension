package org.apache.tomcat.util.net.openssl;

public class ExposeOpenSSLKeyManager extends OpenSSLKeyManager {

    public ExposeOpenSSLKeyManager(String certChainFile, String keyFile) {
        super(certChainFile, keyFile);
    }

}
