package com.hazelcast.aws;

import com.hazelcast.aws.impl.DescribeInstances;

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

    private static class EcsClientStrategy extends AwsClientStrategy {
        private EcsClientStrategy(AwsConfig awsConfig, String endpoint) {
            super(awsConfig, endpoint);
        }

        @Override
        public Collection<String> getPrivateIpAddresses() throws Exception {
            return null;
        }

        @Override
        public Map<String, String> getAddresses() throws Exception {
            return null;
        }
    }

    private static class Ec2ClientStrategy extends AwsClientStrategy {

        public Ec2ClientStrategy(AwsConfig awsConfig, String endpoint) {
            super(awsConfig, endpoint);
        }

        @Override
        public Collection<String> getPrivateIpAddresses() throws Exception {
            Map<String, String> result = new DescribeInstances(awsConfig, endpoint).execute();
            return result.keySet();
        }

        public Map<String, String> getAddresses() throws Exception {
            return new DescribeInstances(awsConfig, endpoint).execute();
        }
    }
}
