package com.hazelcast.aws;

import com.hazelcast.aws.impl.DescribeInstances;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
class Ec2ClientStrategy extends AwsClientStrategy {

    public Ec2ClientStrategy(AwsConfig awsConfig, String endpoint) {
        super(awsConfig, endpoint);
    }

    @Override
    public Collection<String> getPrivateIpAddresses() throws Exception {
        return getAddresses().keySet();
    }

    public Map<String, String> getAddresses() throws Exception {
        return new DescribeInstances(awsConfig, new URL("https", endpoint, -1, "/")).execute();
    }
}
