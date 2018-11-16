package ch.m4rc77.mqtt.dms;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttDeviceConnector  implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttDeviceConnector.class.getName());

    private static final String SUBSCRIBE_TO_TOPIC_PREFIX = "subscribe.to.";

    private static final String CONFIG_BROKER = "mqtt.broker";
    private static final String CONFIG_QOS = "mqtt.qos";
    private static final String CONFIG_TOPIC_STATUS = "topic.status";

    private static final int TEN_SECONDS = 10;

    private String mqttBroker;
    private int mqttQos;
    private String statusTopic;

    private Set<String> topics;

    private MqttClient client;

    private DeviceManager dm;

    MqttDeviceConnector(Properties config, DeviceManager dm) {
        this.dm = dm;

        mqttBroker = config.getProperty(CONFIG_BROKER).trim();
        mqttQos = Integer.parseInt(config.getProperty(CONFIG_QOS, "0").trim());
        statusTopic = config.getProperty(CONFIG_TOPIC_STATUS);

        topics = new TreeSet<>();
        for (String key: config.stringPropertyNames()) {
            if (key.startsWith(SUBSCRIBE_TO_TOPIC_PREFIX)) {
                String topic = key.replace(SUBSCRIBE_TO_TOPIC_PREFIX, "");
                topic = topic.replace(DeviceManager.EXEC_CMD, "");
                topic = topic.replace(DeviceManager.KILL_CMD, "");
                topic = topic.replace(DeviceManager.START_CMD, "");
                topics.add(topic);
            }
        }
    }

    void setupMqtt() {
        log.info("setupMqtt() ... start");

        boolean connected = false;

        long tries = 1;
        do {
            try {
                String clientId = "MqttDeviceManagementService[" + UUID.randomUUID() + "]";
                MemoryPersistence persistence = new MemoryPersistence();
                client = new MqttClient(mqttBroker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                // auto reconnect does not work ... see https://github.com/eclipse/paho.mqtt.android/issues/116
                connOpts.setAutomaticReconnect(false);
                connOpts.setKeepAliveInterval(TEN_SECONDS);
                connOpts.setConnectionTimeout(TEN_SECONDS);
                log.info("Set last will to 0 (retained, QoS=" + mqttQos + ") on topic " + statusTopic);
                connOpts.setWill(statusTopic, "0".getBytes(), mqttQos, true);

                log.info("Connecting to broker: " + mqttBroker);
                client.setCallback(this);
                client.connect(connOpts);
                log.info("Connected to " + mqttBroker + " with client ID " + client.getClientId());


                log.info("Subscribe to topics ...");
                for (String topic: topics) {
                    log.info("Subscribed to " + topic);
                    client.subscribe(topic, mqttQos);
                }
                log.info("Subscribe to topics ... done");


                // Update status ...
                log.info("Publish 0 (retained, QoS=" + mqttQos + ") on topic " + statusTopic);
                client.publish(statusTopic, "1".getBytes(), mqttQos, true);

                log.info("setupMqtt() ... done");
                connected = true;
            } catch (MqttException me) {
                log.info("\rError on connecting to server " + me);
                log.info("Retry in " + (2 * tries) + " seconds!");
                sleep(2 * tries);
                tries++;
            }
        } while (!connected);

        log.info("Connection to " + mqttBroker + " established!");
    }

    void disconnect() {
        log.info("Disconnect from " + mqttBroker);

        if (client != null) {
            try {
                client.publish(statusTopic, "0".getBytes(), mqttQos, true);
            } catch (MqttException e) {
                log.warn("Exception while sending bye bye mqtt-message: " + e);
            }
            client.setCallback(null);
            try {
                client.disconnect();
                client.close();
            } catch (MqttException e) {
                log.warn("Exception while disconnect: " + e);
            }
            client = null;
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        // Called when the connection to the server has been lost.
        log.info("Connection to " + mqttBroker + " lost! Error: " + cause);
        client.setCallback(null);
        setupMqtt();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Called when a message arrives from the server that matches any subscription made by the client
        log.info("Message on topic: " + topic + "; Message:" + new String(message.getPayload()) + ": QoS:" + message.getQos());

        if (dm.executeCommand(topic, SUBSCRIBE_TO_TOPIC_PREFIX + topic)) {
            log.debug("Executed command for " + topic);
        } else {
            log.warn("Failed to executed command for " + topic);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Called when a message has been delivered to the
        // server. The token passed in here is the same one
        // that was passed to or returned from the original
        // call to publish.
        // This allows applications to perform asynchronous
        // delivery without blocking until delivery completes.
    }

//    private static void mqttOut(int val) {
//        try {
//            log.info("\rMQTT Send: " + val);
//            MqttMessage message = new MqttMessage(("" + val).getBytes());
//            message.setQos(QOS);
//            message.setRetained(false);
//            client.publish(topicPrefix + TOPIC, message);
//            log.info("\rMessage published to " + topicPrefix + TOPIC);
//        } catch(MqttException me) {
//            log.info("\rError on publishing message " + me );
//        }
//    }

    private void sleep(long sec) {
        try {
            Thread.sleep(sec * 1000);
        } catch (InterruptedException e1) {
            // ignore
        }
    }

}
