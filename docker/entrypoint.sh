#!/bin/bash -e
java $JAVA_OPTS -Xms$JAVA_MEM_XMS -Xmx$JAVA_MEM_XMX -jar app.jar
