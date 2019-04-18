package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.ECS_DOC_VERSION;

/**
 *
 */
public class DescribeTasks extends AwsOperation<Map<String, String>> {
    private Collection<String> taskArns;

    public DescribeTasks(AwsConfig awsConfig, String endpoint) {
        super(awsConfig, endpoint, "ecs", ECS_DOC_VERSION);
    }

    public Map<String, String> execute(Collection<String> taskArns) throws Exception {
        this.taskArns = taskArns;
        return super.execute();
    }

    @Override
    InputStream callService() throws Exception {
        // TODO
        return null;
    }

    @Override
    Map<String, String> unmarshal(InputStream stream) {
        // TODO
        return null;
    }
}
