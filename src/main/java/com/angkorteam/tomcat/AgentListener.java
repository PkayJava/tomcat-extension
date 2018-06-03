package com.angkorteam.tomcat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.core.StandardServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.angkorteam.tomcat.message.AgentMessage;
import com.google.gson.Gson;

import io.github.openunirest.http.HttpResponse;
import io.github.openunirest.http.Unirest;
import io.github.openunirest.request.GetRequest;

public class AgentListener implements LifecycleListener, MessageListener {

    protected String tomcat;

    protected String instanceId;

    protected String activemqServer;

    protected String activemqChannel;

    protected Process process;

    protected int processId = -1;

    protected LifecycleState state;

    private Connection activemqConnection;

    private Session activemqSession;

    private MessageConsumer activemqConsumer;

    private Gson gson = new Gson();

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getSource() != null) {
            if (event.getSource() instanceof StandardServer) {
                if (event.getLifecycle().getState() == LifecycleState.INITIALIZING) {
                    File setenv = new File(tomcat + "/bin/setenv.sh");
                    List<String> lines = null;
                    if (setenv.exists()) {
                        try {
                            lines = FileUtils.readLines(setenv, "UTF-8");
                        } catch (IOException e) {
                        }
                    } else {
                        lines = new ArrayList<>();
                        lines.add("#!/bin/sh");
                        lines.add("");
                    }

                    Map<String, String> params = new HashMap<>();
                    params.put("-DinstanceId=", String.format("JAVA_OPTS=\"$JAVA_OPTS -DinstanceId=%s\"", this.instanceId));
                    params.put("-DactivemqServer=", String.format("JAVA_OPTS=\"$JAVA_OPTS -DactivemqServer=%s\"", this.activemqServer));
                    params.put("-DactivemqChannel=", String.format("JAVA_OPTS=\"$JAVA_OPTS -DactivemqChannel=%s\"", this.activemqChannel));

                    List<String> newLines = new ArrayList<>();
                    for (String line : lines) {
                        boolean replaced = false;
                        for (Entry<String, String> param : params.entrySet()) {
                            if (line.contains(param.getKey())) {
                                newLines.add(param.getValue());
                                replaced = true;
                                break;
                            }
                        }
                        if (!replaced) {
                            newLines.add(line);
                        }
                    }

                    try {
                        FileUtils.writeLines(setenv, newLines);
                    } catch (IOException e) {
                    }

                    this.state = LifecycleState.INITIALIZING;

                    start();

                    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(activemqServer);

                    try {
                        // Create a Connection
                        activemqConnection = connectionFactory.createConnection();
                        activemqConnection.start();

                        // Create a Session
                        activemqSession = activemqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                        // Create the destination (Topic or Queue)
                        Destination destination = activemqSession.createQueue(this.instanceId);

                        // Create a MessageConsumer from the Session to the Topic or Queue
                        activemqConsumer = activemqSession.createConsumer(destination);
                        activemqConsumer.setMessageListener(this);
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    if (event.getLifecycle().getState() == LifecycleState.DESTROYING) {
                        if (this.process != null && this.process.isAlive()) {
                            this.process.destroy();
                        }
                    } else if (event.getLifecycle().getState() == LifecycleState.DESTROYED) {
                        try {
                            activemqConsumer.close();
                            activemqSession.close();
                            activemqConnection.close();
                        } catch (JMSException e) {
                        }
                    }
                }
            }
        }
    }

    protected void start() {
        Runtime runtime = Runtime.getRuntime();
        try {
            process = runtime.exec(tomcat + "/bin/catalina.sh run");
            processId = getProcessId(process);
        } catch (IOException e) {
        }
    }

    protected void stop() {
        Runtime runtime = Runtime.getRuntime();
        try {
            process = runtime.exec(tomcat + "/bin/catalina.sh stop");
            processId = getProcessId(process);
        } catch (IOException e) {
        }
    }

    protected void kill() {
        if (process != null && process.isAlive()) {
            process.destroy();
            process = null;
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                AgentMessage m = gson.fromJson(textMessage.getText(), AgentMessage.class);
                if ("stop".equals(m.getCommand())) {
                    stop();
                } else if ("start".equals(m.getCommand())) {
                    start();
                } else if ("kill".equals(m.getCommand())) {
                    kill();
                } else if ("deploy".equals(m.getCommand())) {
                    if ("war".equals(m.getType()) && m.getWebapps() != null && !"".equals(m.getWebapps())) {
                        File webapps = new File(m.getWebapps());
                        if (webapps.exists() && webapps.isDirectory()) {
                            deployWar(m);
                        }
                    } else if ("jar".equals(m.getType())) {
                        if (m.getLibraryName() != null && !"".equals(m.getLibraryName()) && m.getLibraryVersion() != null && !"".equals(m.getLibraryVersion())) {
                            deployJar(m);
                        }
                    }
                } else if ("undeploy".equals(m.getCommand())) {
                    if ("war".equals(m.getType()) && m.getWebapps() != null && !"".equals(m.getWebapps())) {
                        File webapps = new File(m.getWebapps());
                        if (webapps.exists() && webapps.isDirectory()) {
                            undeployWar(m);
                        }
                    } else if ("jar".equals(m.getType())) {
                        if (m.getFile() != null && !"".equals(m.getFile())) {
                            undeployJar(m);
                        }
                    }
                } else if ("restart".equals(m.getCommand())) {
                    restart();
                }
            }
        } catch (JMSException e) {
        }
    }

    protected void restart() {
        kill();
        start();
    }

    protected void deployWar(AgentMessage message) {
        GetRequest request = Unirest.get(message.getFile());
        HttpResponse<InputStream> response = request.asBinary();
        File tmp = new File(FileUtils.getTempDirectory(), System.currentTimeMillis() + ".war");
        try (FileOutputStream stream = FileUtils.openOutputStream(tmp)) {
            try (InputStream war = response.getBody()) {
                IOUtils.copy(war, stream);
                kill();
                File webapps = new File(message.getWebapps());
                if (message.getContext() == null && "/".equals(message.getContext())) {
                    FileUtils.deleteQuietly(new File(webapps, "ROOT.war"));
                    FileUtils.deleteDirectory(new File(webapps, "ROOT"));
                    FileUtils.moveFile(tmp, new File(webapps, "ROOT.war"));
                } else {
                    FileUtils.deleteQuietly(new File(webapps, message.getContext().substring(1) + ".war"));
                    FileUtils.deleteDirectory(new File(webapps, message.getContext().substring(1)));
                    FileUtils.moveFile(tmp, new File(webapps, message.getContext().substring(1) + ".war"));
                }
                start();
            }
        } catch (IOException e) {
        }
    }

    protected void deployJar(AgentMessage message) {
        GetRequest request = Unirest.get(message.getFile());
        HttpResponse<InputStream> response = request.asBinary();
        File tmp = new File(FileUtils.getTempDirectory(), System.currentTimeMillis() + ".jar");
        try (FileOutputStream stream = FileUtils.openOutputStream(tmp)) {
            try (InputStream jar = response.getBody()) {
                IOUtils.copy(jar, stream);
                kill();
                File library = new File(new File(tomcat, "lib"), message.getLibraryName() + "v" + message.getLibraryVersion() + ".jar");
                FileUtils.moveFile(tmp, library);
                start();
            }
        } catch (IOException e) {
        }
    }

    protected void undeployWar(AgentMessage message) {
        File webapps = new File(message.getWebapps());
        try {
            File war = null;
            File folder = null;
            kill();
            if (message.getContext() == null && "/".equals(message.getContext())) {
                war = new File(webapps, "ROOT.war");
                folder = new File(webapps, "ROOT");
            } else {
                war = new File(webapps, message.getContext().substring(1) + ".war");
                folder = new File(webapps, message.getContext().substring(1));
            }
            if (war.exists() || folder.exists()) {
                kill();
                FileUtils.deleteQuietly(new File(webapps, message.getContext().substring(1) + ".war"));
                FileUtils.deleteDirectory(new File(webapps, message.getContext().substring(1)));
                start();
            }

        } catch (IOException e) {
        }
    }

    protected void undeployJar(AgentMessage message) {
        File jar = new File(new File(tomcat, "lib"), message.getFile());
        if (jar.exists()) {
            kill();
            FileUtils.deleteQuietly(jar);
            start();
        }
    }

    protected int getProcessId(Process process) {
        int processId = -1;
        if (process != null && process.isAlive()) {
            if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                /* get the PID on unix/linux systems */
                try {
                    Field field = process.getClass().getDeclaredField("pid");
                    field.setAccessible(true);
                    processId = field.getInt(process);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                }
            }
        }
        return processId;
    }

    public String getTomcat() {
        return tomcat;
    }

    public void setTomcat(String tomcat) {
        this.tomcat = tomcat;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getActivemqServer() {
        return activemqServer;
    }

    public void setActivemqServer(String activemqServer) {
        this.activemqServer = activemqServer;
    }

    public String getActivemqChannel() {
        return activemqChannel;
    }

    public void setActivemqChannel(String activemqChannel) {
        this.activemqChannel = activemqChannel;
    }

}
