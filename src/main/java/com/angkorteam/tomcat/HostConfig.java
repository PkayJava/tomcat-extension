package com.angkorteam.tomcat;

import java.io.File;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.catalina.util.ContextName;

import com.angkorteam.tomcat.message.ApplicationMessage;
import com.google.gson.Gson;

public class HostConfig extends org.apache.catalina.startup.HostConfig {

    protected Session activemqSession;

    protected MessageProducer activemqProducer;

    protected String instanceId;

    protected Gson gson;

    public HostConfig(Gson gson, String instanceId, Session activemqSession, MessageProducer activemqProducer) {
        super();
        this.gson = gson;
        this.activemqSession = activemqSession;
        this.activemqProducer = activemqProducer;
        this.instanceId = instanceId;
    }

    @Override
    protected void deployDirectory(ContextName cn, File dir) {
        super.deployDirectory(cn, dir);
        try {
            TextMessage message = this.activemqSession.createTextMessage(gson.toJson(buildMessage("Directory", cn, dir)));
            activemqProducer.send(message);
        } catch (JMSException e) {
        }
    }

    @Override
    protected void deployDescriptor(ContextName cn, File contextXml) {
        super.deployDescriptor(cn, contextXml);
        try {
            TextMessage message = this.activemqSession.createTextMessage(gson.toJson(buildMessage("Descriptor", cn, contextXml)));
            activemqProducer.send(message);
        } catch (JMSException e) {
        }
    }

    @Override
    protected void deployWAR(ContextName cn, File war) {
        super.deployWAR(cn, war);
        try {
            TextMessage message = this.activemqSession.createTextMessage(gson.toJson(buildMessage("WAR", cn, war)));
            activemqProducer.send(message);
        } catch (JMSException e) {
        }
    }

    protected ApplicationMessage buildMessage(String type, ContextName cn, File resource) {
        ApplicationMessage message = new ApplicationMessage();
        message.setInstanceId(instanceId);
        message.setBaseName(cn.getBaseName());
        message.setDisplayName(cn.getDisplayName());
        message.setName(cn.getName());
        message.setPath(cn.getPath());
        message.setVersion(cn.getVersion());
        message.setEvent("Deployed");
        message.setResource(resource.getAbsolutePath());
        message.setType(type);
        return message;
    }

}
