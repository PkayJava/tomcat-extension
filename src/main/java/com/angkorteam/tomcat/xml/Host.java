package com.angkorteam.tomcat.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Host {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "appBase")
    private String appBase;

    @XmlAttribute(name = "unpackWARs")
    private Boolean unpackWARs;

    @XmlAttribute(name = "autoDeploy")
    private Boolean autoDeploy;

    @XmlElement(name = "Valve")
    private List<Valve> valves = new ArrayList<>();

    public String getName() {
        return name;
    }

    public String getAppBase() {
        return appBase;
    }

    public Boolean getUnpackWARs() {
        return unpackWARs;
    }

    public Boolean getAutoDeploy() {
        return autoDeploy;
    }

    public List<Valve> getValves() {
        return valves;
    }

}
