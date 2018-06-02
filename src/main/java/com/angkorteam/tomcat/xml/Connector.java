package com.angkorteam.tomcat.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Connector {

    @XmlAttribute(name = "port")
    private Integer port;

    @XmlAttribute(name = "connectionTimeout")
    private Integer connectionTimeout;

    @XmlAttribute(name = "redirectPort")
    private Integer redirectPort;

    @XmlAttribute(name = "protocol")
    private String protocol;

    @XmlAttribute(name = "URIEncoding")
    private String uriEncoding;

    @XmlAttribute(name = "maxThreads")
    private Integer maxThreads;

    @XmlAttribute(name = "SSLEnabled")
    private Boolean sslEnabled;

    @XmlAttribute(name = "sslImplementationName")
    private String sslImplementationName;

    @XmlElement(name = "UpgradeProtocol")
    private UpgradeProtocol upgradeProtocol;

    @XmlElement(name = "SSLHostConfig")
    private List<SSLHostConfig> sslHostConfigs = new ArrayList<>();

    public Integer getPort() {
        return port;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Integer getRedirectPort() {
        return redirectPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUriEncoding() {
        return uriEncoding;
    }

    public Integer getMaxThreads() {
        return maxThreads;
    }

    public Boolean getSslEnabled() {
        return sslEnabled;
    }

    public String getSslImplementationName() {
        return sslImplementationName;
    }

    public UpgradeProtocol getUpgradeProtocol() {
        return upgradeProtocol;
    }

    public List<SSLHostConfig> getSslHostConfigs() {
        return sslHostConfigs;
    }

}
