# mqtt-device-management-service

## Description
Simple Service that connects to a MQTT-Broker and subscribes 
to a list of configured topics. If messages do arrive on the
topics the mqtt-device-management-service will start the configured
commands e.g. the browser.

This service can be used to build simple remote controlled devices e.g. 
info screens or similar.

## Configuration

See `dms.properties.sample` how to configure the service. In order to get
the service working, a file called `dms.properties` must be present in the
current working directory.

## Build

`mvn clean install`

## Run
`java -jar target/mqtt-device-management-service.jar`

## Important

This code is build for **Java 7**. This is intentionally. It was necessary due 
to limitation on the destination system. With Java 8 some code could be
simplified and optimized. 

This code runs only on systems where **bash** is available. Tested on **Ubuntu 18.04** and 
**Mac OS 10.8**. 



