package com.angkorteam.tomcat.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Valve {

    @XmlAttribute(name = "className")
    private String className;

    @XmlAttribute(name = "directory")
    private String directory;

    @XmlAttribute(name = "prefix")
    private String prefix;

    @XmlAttribute(name = "suffix")
    private String suffix;

    @XmlAttribute(name = "pattern")
    private String pattern;

    public String getClassName() {
        return className;
    }

    public String getDirectory() {
        return directory;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getPattern() {
        return pattern;
    }

}
