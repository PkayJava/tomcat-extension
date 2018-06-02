package com.angkorteam.tomcat.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class SSLHostConfig {

    @XmlAttribute(name = "protocols")
    private String protocols;

    @XmlAttribute(name = "hostName")
    private String hostName;

    @XmlAttribute(name = "caCertificateFile")
    private String caCertificateFile;

    @XmlAttribute(name = "certificateVerification")
    private String certificateVerification;

    @XmlAttribute(name = "ciphers")
    private String ciphers;

    @XmlAttribute(name = "truststoreFile")
    private String truststoreFile;

    @XmlAttribute(name = "truststorePassword")
    private String truststorePassword;

    @XmlElement(name = "Certificate")
    private Certificate certificate;

    public String getProtocols() {
        return protocols;
    }

    public String getHostName() {
        return hostName;
    }

    public String getCaCertificateFile() {
        return caCertificateFile;
    }

    public String getCertificateVerification() {
        return certificateVerification;
    }

    public String getCiphers() {
        return ciphers;
    }

    public String getTruststoreFile() {
        return truststoreFile;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public Certificate getCertificate() {
        return certificate;
    }

}
