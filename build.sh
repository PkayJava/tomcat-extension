#!/bin/sh

mvn clean install
cp target/tomcat-extension.jar /opt/tomcat-agent/lib
cp target/tomcat-extension.jar /opt/tomcat-instance/lib
cp target/tomcat-extension.jar /opt/tomcat-enterprise/lib