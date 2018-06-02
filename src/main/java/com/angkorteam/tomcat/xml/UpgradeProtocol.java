package com.angkorteam.tomcat.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradeProtocol {

    @XmlAttribute(name = "className")
    private String className;

    public String getClassName() {
        return className;
    }

}
