package com.angkorteam.tomcat.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Environment {

    @XmlAttribute(name = "description")
    private String description;

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "override")
    private Boolean override;

    @XmlAttribute(name = "type")
    private String type;

    @XmlAttribute(name = "value")
    private String value;

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public Boolean getOverride() {
        return override;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

}
