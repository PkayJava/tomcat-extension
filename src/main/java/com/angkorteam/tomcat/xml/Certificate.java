package com.angkorteam.tomcat.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Certificate {

    @XmlAttribute(name = "certificateChainFile")
    private String certificateChainFile;

    @XmlAttribute(name = "certificateFile")
    private String certificateFile;

    @XmlAttribute(name = "certificateKeyFile")
    private String certificateKeyFile;

    @XmlAttribute(name = "certificateKeyPassword")
    private String certificateKeyPassword;

    @XmlAttribute(name = "certificateKeystoreFile")
    private String certificateKeystoreFile;

    @XmlAttribute(name = "certificateKeystorePassword")
    private String certificateKeystorePassword;

    @XmlAttribute(name = "certificateKeyAlias")
    private String certificateKeyAlias;

    @XmlAttribute(name = "type")
    private String type;

    public String getCertificateChainFile() {
        return certificateChainFile;
    }

    public String getCertificateFile() {
        return certificateFile;
    }

    public String getCertificateKeyFile() {
        return certificateKeyFile;
    }

    public String getCertificateKeyPassword() {
        return certificateKeyPassword;
    }

    public String getCertificateKeystoreFile() {
        return certificateKeystoreFile;
    }

    public String getCertificateKeystorePassword() {
        return certificateKeystorePassword;
    }

    public String getCertificateKeyAlias() {
        return certificateKeyAlias;
    }

    public String getType() {
        return type;
    }

}
