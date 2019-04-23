package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;

import java.io.InputStream;
import java.net.URL;

/**
 *
 */
public class TaskMetadata extends AwsOperation<String> {

    public TaskMetadata(AwsConfig awsConfig, URL endpointURL) {
        // TODO
        super(awsConfig, endpointURL, "", "");
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
