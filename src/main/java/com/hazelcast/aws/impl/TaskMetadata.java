package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;

import java.io.InputStream;

/**
 *
 */
public class TaskMetadata extends AwsOperation<String> {
    public TaskMetadata(AwsConfig awsConfig, String endpoint) {
        // TODO
        super(awsConfig, endpoint, "", "");
    }

    @Override
    InputStream callService() throws Exception {
        // TODO
        return null;
    }

    @Override
    String unmarshal(InputStream stream) {
        // TODO
        return null;
    }
}
