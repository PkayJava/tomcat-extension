package com.angkorteam.tomcat.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Server")
@XmlAccessorType(XmlAccessType.FIELD)
public class Server {

    @XmlElement(name = "GlobalNamingResources")
    private GlobalNamingResources globalNamingResources = new GlobalNamingResources();

    @XmlElement(name = "Service")
    private Service service = new Service();

    public GlobalNamingResources getGlobalNamingResources() {
        return this.globalNamingResources;
    }

    public Service getService() {
        return this.service;
    }

}
