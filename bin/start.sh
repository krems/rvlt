#!/usr/bin/env bash
java -Dlog4j.configurationFile=conf/log4j.xml -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -jar target/revolut-1.0-SNAPSHOT-jar-with-dependencies.jar
