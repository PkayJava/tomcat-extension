package com.angkorteam.tomcat.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Resource {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "auth")
    private String auth;

    @XmlAttribute(name = "type")
    private String type;

    @XmlAttribute(name = "description")
    private String description;

    @XmlAttribute(name = "factory")
    private String factory;

    @XmlAttribute(name = "pathname")
    private String pathname;

    public String getName() {
        return name;
    }

    public String getAuth() {
        return auth;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getFactory() {
        return factory;
    }

    public String getPathname() {
        return pathname;
    }

}
