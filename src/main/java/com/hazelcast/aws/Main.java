package com.hazelcast.aws;

import com.hazelcast.logging.ILogger;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class Main {

    public static void main(String[] args) {

        Logger parent = Logger.getLogger("com.hazelcast");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINEST);
        parent.addHandler(handler);
        parent.setLevel(Level.FINEST);

        ILogger LOGGER = com.hazelcast.logging.Logger.getLogger(Main.class);
        LOGGER.finest("Version 22");

        System.setProperty("java.net.preferIPv4Stack","true");

        AwsConfig awsConfig = AwsConfig.builder()
                .setRegion("eu-central-1")
                .setHostHeader("ecs.amazonaws.com")
                .setHzPort(new PortRange("5701-5709")).build();

        LOGGER.finest(awsConfig.toString());

        AWSClient awsClient = new AWSClient(awsConfig);
        try {
            LOGGER.finest("addresses: " + awsClient.getAddresses());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
