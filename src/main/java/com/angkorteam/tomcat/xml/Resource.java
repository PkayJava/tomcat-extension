package com.angkorteam.tomcat.xml;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;

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

    @XmlAnyAttribute
    private Map<QName, String> extension = new HashMap<>();

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

    public Map<QName, String> getExtension() {
        return extension;
    }

}
