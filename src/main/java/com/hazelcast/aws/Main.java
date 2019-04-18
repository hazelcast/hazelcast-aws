package com.hazelcast.aws;

/**
 *
 */
public class Main {


    public static void main(String[] args) {

        System.setProperty("java.net.preferIPv4Stack","true");

        AwsConfig awsConfig = AwsConfig.builder()
                .setRegion("eu-central-1")
                .setHostHeader("ecs.amazonaws.com")
                .setHzPort(new PortRange("5701-5709")).build();

        System.out.println("Version 21");
        System.out.println(awsConfig.toString());

        AWSClient awsClient = new AWSClient(awsConfig);
        try {
            System.out.println("addresses: " + awsClient.getAddresses());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
