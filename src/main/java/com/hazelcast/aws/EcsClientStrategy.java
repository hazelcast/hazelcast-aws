package com.hazelcast.aws;

import com.hazelcast.aws.impl.DescribeTasks;
import com.hazelcast.aws.impl.ListTasks;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
class EcsClientStrategy extends AwsClientStrategy {
    EcsClientStrategy(AwsConfig awsConfig, String endpoint) {
        super(awsConfig, endpoint);
    }

    @Override
    public Collection<String> getPrivateIpAddresses() throws Exception {
        return getAddresses().keySet();
    }

    @Override
    public Map<String, String> getAddresses() throws Exception {
        // FIXME taskMetada URL
        // String clusterName = new TaskMetadata(awsConfig, new URL("https", endpoint, -1, "/")).execute();
        Collection<String> taskArns = new ListTasks(awsConfig, new URL("https", endpoint, -1, "/")).execute(/*clusterName*/);
        Map<String, String> addresses = new DescribeTasks(awsConfig, new URL("https", endpoint, -1, "/")).execute(taskArns);
        return addresses;
    }
}
