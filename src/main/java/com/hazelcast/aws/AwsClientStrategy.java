package com.hazelcast.aws;

import java.util.Collection;
import java.util.Map;

/**
 *
 */
public abstract class AwsClientStrategy {

    final protected AwsConfig awsConfig;
    final protected String endpoint;

    protected AwsClientStrategy(AwsConfig awsConfig, String endpoint) {
        this.awsConfig = awsConfig;
        this.endpoint = endpoint;
    }

    public static AwsClientStrategy create(AwsConfig awsConfig, String endpoint) {
        if (endpoint.startsWith("ecs.")) {
            return new EcsClientStrategy(awsConfig, endpoint);
        } else {
            return new Ec2ClientStrategy(awsConfig, endpoint);
        }
    }

    public abstract Collection<String> getPrivateIpAddresses() throws Exception;

    public abstract Map<String, String> getAddresses() throws Exception;

}
