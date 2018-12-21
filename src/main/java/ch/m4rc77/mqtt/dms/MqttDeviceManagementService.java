package ch.m4rc77.mqtt.dms;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttDeviceManagementService {

    private static final Logger log = LoggerFactory.getLogger(MqttDeviceManagementService.class.getName());

    /**
     * Runs the mqtt-device-management-service
     *
     * @param args no need for arguments
     */
    public static void main(String[] args) {
        log.info("Start ... ");

        Properties config = loadProperties();

        final DeviceManager dm = new DeviceManager(config);

        final MqttDeviceConnector mqttConnector = new MqttDeviceConnector(config, dm);
        mqttConnector.setupMqtt();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    log.info("MqttDeviceManagementService Shutdown ...");
                    // Disconnect the client from the server
                    mqttConnector.disconnect();
                    dm.shutdown();

                    log.info("MqttDeviceManagementService Shutdown ... done");
                    log.info("BYE BYE from MqttDeviceManagementService");
                    Runtime.getRuntime().halt(0);
                } catch (Exception e) {
                    log.error("Exception while shutting down MqttDeviceManagementService" + e);
                    Runtime.getRuntime().halt(-1);
                }
            }
        });
    }

    private static Properties loadProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream("dms.properties");
            prop.load(input);
        } catch (Exception ex) {
            log.error("Error loading properties: " + ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }

}
