package com.angkorteam.tomcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.coyote.UpgradeProtocol;
import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http11.Http11Nio2Protocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.ContextEnvironment;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;

import com.angkorteam.tomcat.message.InstanceMessage;
import com.angkorteam.tomcat.xml.Certificate;
import com.angkorteam.tomcat.xml.Engine;
import com.angkorteam.tomcat.xml.Environment;
import com.angkorteam.tomcat.xml.GlobalNamingResources;
import com.angkorteam.tomcat.xml.Host;
import com.angkorteam.tomcat.xml.Resource;
import com.angkorteam.tomcat.xml.Server;
import com.angkorteam.tomcat.xml.Valve;
import com.google.gson.Gson;

public class InstanceListener implements LifecycleListener {

    protected Server server;

    protected String configuration;

    protected String instanceId;

    protected String activemqServer;

    protected String activemqChannel;

    private Connection activemqConnection;

    private Session activemqSession;

    private MessageProducer activemqProducer;

    private Gson gson = new Gson();

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getSource() != null) {
            if (event.getSource() instanceof StandardServer) {
                StandardServer server = (StandardServer) event.getSource();
                if (event.getLifecycle().getState() == LifecycleState.INITIALIZING) {
                    this.instanceId = System.getProperty("instanceId");
                    this.activemqServer = System.getProperty("activemqServer");
                    this.activemqChannel = System.getProperty("activemqChannel");

                    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(activemqServer);

                    try {
                        // Create a Connection
                        activemqConnection = connectionFactory.createConnection();
                        activemqConnection.start();

                        // Create a Session
                        activemqSession = activemqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                        // Create the destination (Topic or Queue)
                        Destination destination = activemqSession.createQueue(this.activemqChannel);

                        // Create a MessageProducer from the Session to the Topic or Queue
                        activemqProducer = activemqSession.createProducer(destination);
                        activemqProducer.setTimeToLive(1000 * 5);
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        TextMessage message = activemqSession.createTextMessage(gson.toJson(buildMessage(event.getLifecycle().getStateName(), server.getCatalinaHome())));
                        activemqProducer.send(message);
                    } catch (JMSException e) {
                    }
                    if (this.configuration != null && !"".equals(this.configuration)) {
                        initStandardServer(server);
                    }
                } else {
                    try {
                        TextMessage message = activemqSession.createTextMessage(gson.toJson(buildMessage(event.getLifecycle().getStateName(), server.getCatalinaHome())));
                        activemqProducer.send(message);
                    } catch (JMSException e) {
                    }
                    if (event.getLifecycle().getState() == LifecycleState.DESTROYED) {
                        try {
                            activemqProducer.close();
                            activemqSession.close();
                            activemqConnection.close();
                        } catch (JMSException e) {
                        }
                    }
                }
            } else if (event.getSource() instanceof StandardService) {
                StandardService service = (StandardService) event.getSource();
                if (event.getLifecycle().getState() == LifecycleState.INITIALIZING) {
                    if (this.server != null) {
                        initStandardService(service);
                    }
                }
            } else if (event.getSource() instanceof StandardEngine) {
                if (event.getLifecycle().getState() == LifecycleState.INITIALIZING) {
                    if (this.server != null) {
                        initStandardEngine((StandardEngine) event.getSource());
                    }
                }
            }
        }
    }

    protected InstanceMessage buildMessage(String event, File catalinaHome) {
        InstanceMessage message = new InstanceMessage();
        message.setInstanceId(instanceId);
        message.setEvent(event);
        message.setServerInfo(ServerInfo.getServerInfo());
        message.setServerNumber(ServerInfo.getServerNumber());
        message.setOsVersion(System.getProperty("os.version"));
        message.setOsName(System.getProperty("os.name"));
        message.setOsArch(System.getProperty("os.arch"));
        message.setJvmVendor(System.getProperty("java.vm.vendor"));
        message.setJvmVersion(System.getProperty("java.runtime.version"));
        message.setOsArch(System.getProperty("os.arch"));
        message.setCatalinaHome(catalinaHome.getAbsolutePath());
        File library = new File(catalinaHome, "lib");
        for (String lib : library.list()) {
            message.getLibrary().add(lib);
        }
        return message;
    }

    protected void initStandardEngine(StandardEngine engine) {
        for (Host host : this.server.getService().getEngine().getHosts()) {
            StandardHost h = new StandardHost();
            h.setAppBase(host.getAppBase());
            if (host.getUnpackWARs() != null) {
                h.setUnpackWARs(host.getUnpackWARs());
            }
            if (host.getAutoDeploy()) {
                h.setAutoDeploy(host.getAutoDeploy());
            }
            h.setName(host.getName());
            for (Valve valve : host.getValves()) {
                if (AccessLogValve.class.getName().equals(valve.getClassName())) {
                    AccessLogValve v = new AccessLogValve();
                    v.setDirectory(valve.getDirectory());
                    v.setPrefix(valve.getPrefix());
                    v.setSuffix(valve.getSuffix());
                    v.setPattern(valve.getPattern());
                    h.getPipeline().addValve(v);
                }
            }
            engine.addChild(h);

            HostConfig hostConfig = new HostConfig(this.gson, this.instanceId, this.activemqSession, this.activemqProducer);
            h.addLifecycleListener(hostConfig);
        }
    }

    protected void initStandardService(StandardService service) {
        for (com.angkorteam.tomcat.xml.Connector connector : this.server.getService().getConnectors()) {
            Connector con = new Connector(connector.getProtocol());
            con.setPort(connector.getPort());
            con.setRedirectPort(connector.getRedirectPort());
            if (connector.getUriEncoding() != null && !"".equals(connector.getUriEncoding())) {
                con.setURIEncoding(connector.getUriEncoding());
            }
            try {
                if (connector.getUpgradeProtocol() != null && connector.getUpgradeProtocol().getClassName() != null && !"".equals(connector.getUpgradeProtocol().getClassName())) {
                    con.addUpgradeProtocol((UpgradeProtocol) Class.forName(connector.getUpgradeProtocol().getClassName()).newInstance());
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            }
            if (connector.getSslEnabled() != null) {
                if (con.getProtocolHandler() instanceof Http11NioProtocol) {
                    Http11NioProtocol protocol = (Http11NioProtocol) con.getProtocolHandler();
                    protocol.setSSLEnabled(connector.getSslEnabled());
                    protocol.setSslImplementationName(connector.getSslImplementationName());
                } else if (con.getProtocolHandler() instanceof Http11Nio2Protocol) {
                    Http11Nio2Protocol protocol = (Http11Nio2Protocol) con.getProtocolHandler();
                    protocol.setSSLEnabled(connector.getSslEnabled());
                    protocol.setSslImplementationName(connector.getSslImplementationName());
                } else if (con.getProtocolHandler() instanceof Http11AprProtocol) {
                    Http11AprProtocol protocol = (Http11AprProtocol) con.getProtocolHandler();
                    protocol.setSSLEnabled(connector.getSslEnabled());
                }
            }

            for (com.angkorteam.tomcat.xml.SSLHostConfig hostConfig : connector.getSslHostConfigs()) {
                SSLHostConfig config = new SSLHostConfig();
                config.setProtocols(hostConfig.getProtocols());
                config.setCaCertificateFile(hostConfig.getCaCertificateFile());
                config.setCertificateVerification(hostConfig.getCertificateVerification());
                config.setCiphers(hostConfig.getCiphers());
                config.setHostName(hostConfig.getHostName());
                config.setTruststoreFile(hostConfig.getTruststoreFile());
                config.setTruststorePassword(hostConfig.getTruststorePassword());

                Certificate certificate = hostConfig.getCertificate();

                SSLHostConfigCertificate cert = new SSLHostConfigCertificate(config, Type.valueOf(certificate.getType()));
                cert.setCertificateChainFile(certificate.getCertificateChainFile());
                cert.setCertificateFile(certificate.getCertificateFile());
                cert.setCertificateKeyAlias(certificate.getCertificateKeyAlias());
                cert.setCertificateKeyFile(certificate.getCertificateKeyFile());
                cert.setCertificateKeyPassword(certificate.getCertificateKeyPassword());
                cert.setCertificateKeystoreFile(certificate.getCertificateKeystoreFile());
                cert.setCertificateKeystorePassword(certificate.getCertificateKeystorePassword());
                config.addCertificate(cert);
                con.addSslHostConfig(config);

            }

            for (Entry<QName, String> attribute : connector.getExtension().entrySet()) {
                String key = attribute.getKey().getLocalPart();
                String value = attribute.getValue();
                if (value != null && !"".equals(value)) {
                    con.setProperty(key, value);
                }
            }

            service.addConnector(con);
        }
        service.getContainer().addLifecycleListener(this);
    }

    protected void initStandardServer(StandardServer server) {
        this.server = new Server();
        File configurationFolder = new File(this.configuration);
        for (File configurationFile : configurationFolder.listFiles()) {
            try {
                JAXBContext jaxb = JAXBContext.newInstance(com.angkorteam.tomcat.xml.Connector.class, Engine.class, GlobalNamingResources.class, Host.class, Resource.class, Server.class, com.angkorteam.tomcat.xml.Service.class, Valve.class);
                Unmarshaller unmarshaller = jaxb.createUnmarshaller();
                try (InputStream stream = new FileInputStream(configurationFile)) {
                    Server tempServer = (com.angkorteam.tomcat.xml.Server) unmarshaller.unmarshal(stream);
                    if (tempServer != null) {
                        if (tempServer.getGlobalNamingResources() != null && tempServer.getGlobalNamingResources().getResources() != null && !tempServer.getGlobalNamingResources().getResources().isEmpty()) {
                            this.server.getGlobalNamingResources().getResources().addAll(tempServer.getGlobalNamingResources().getResources());
                        }
                        if (tempServer.getGlobalNamingResources() != null && tempServer.getGlobalNamingResources().getEnvironments() != null && !tempServer.getGlobalNamingResources().getEnvironments().isEmpty()) {
                            this.server.getGlobalNamingResources().getEnvironments().addAll(tempServer.getGlobalNamingResources().getEnvironments());
                        }
                        if (tempServer.getService() != null && tempServer.getService().getConnectors() != null && !tempServer.getService().getConnectors().isEmpty()) {
                            this.server.getService().getConnectors().addAll(tempServer.getService().getConnectors());
                        }
                        if (tempServer.getService() != null && tempServer.getService().getEngine() != null && tempServer.getService().getEngine().getHosts() != null && !tempServer.getService().getEngine().getHosts().isEmpty()) {
                            this.server.getService().getEngine().getHosts().addAll(tempServer.getService().getEngine().getHosts());
                        }
                    }
                }
            } catch (JAXBException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (Service service : server.findServices()) {
            service.addLifecycleListener(this);
        }

        for (Resource resource : this.server.getGlobalNamingResources().getResources()) {
            ContextResource res = new ContextResource();
            res.setAuth(resource.getAuth());
            res.setType(resource.getType());
            res.setDescription(resource.getDescription());
            res.setName(resource.getName());
            for (Entry<QName, String> attribute : resource.getExtension().entrySet()) {
                String key = attribute.getKey().getLocalPart();
                String value = attribute.getValue();
                if (value != null && !"".equals(value)) {
                    res.setProperty(key, value);
                }
            }
            server.getGlobalNamingResources().addResource(res);
        }

        for (Environment environment : this.server.getGlobalNamingResources().getEnvironments()) {
            ContextEnvironment env = new ContextEnvironment();
            env.setDescription(environment.getDescription());
            env.setName(environment.getName());
            env.setType(environment.getType());
            env.setValue(environment.getValue());
            if (environment.getOverride() != null) {
                env.setOverride(environment.getOverride());
            }
            server.getGlobalNamingResources().addEnvironment(env);
        }
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

}
