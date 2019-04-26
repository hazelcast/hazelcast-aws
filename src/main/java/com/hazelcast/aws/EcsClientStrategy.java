package com.hazelcast.aws;

import com.hazelcast.aws.impl.DescribeTasks;
import com.hazelcast.aws.impl.ListTasks;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
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
        // FIXME taskMetadata URL
        // String clusterName = new TaskMetadata(awsConfig, new URL("https", endpoint, -1, "/")).execute();
        ListTasks listTasks = new ListTasks(awsConfig, new URL("https", endpoint, -1, "/"));
        Collection<String> taskArns = listTasks.execute(/*clusterName*/);
        if (!taskArns.isEmpty()) {
            DescribeTasks describeTasks = new DescribeTasks(awsConfig, new URL("https", endpoint, -1, "/"));
//            describeTasks.setSecurityToken(listTasks.getSecurityToken());
            return describeTasks.execute(taskArns);
        }
        return Collections.EMPTY_MAP;
    }
}
