package com.angkorteam.tomcat;

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

public class CollectorListener implements LifecycleListener, MessageListener {

    protected String activemqServer;

    protected String activemqChannel;

    private Connection activemqConnection;

    private Session activemqSession;

    private MessageConsumer activemqConsumer;

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getSource() != null) {
            if (event.getSource() instanceof StandardServer) {
                if (event.getLifecycle().getState() == LifecycleState.INITIALIZING) {
                    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(activemqServer);

                    try {
                        // Create a Connection
                        activemqConnection = connectionFactory.createConnection();
                        activemqConnection.start();

                        // Create a Session
                        activemqSession = activemqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                        // Create the destination (Topic or Queue)
                        Destination destination = activemqSession.createQueue(this.activemqChannel);

                        // Create a MessageConsumer from the Session to the Topic or Queue
                        activemqConsumer = activemqSession.createConsumer(destination);
                        activemqConsumer.setMessageListener(this);
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
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

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                System.out.println("Received: " + text);
            }
        } catch (JMSException e) {
        }
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
