package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;

import java.io.InputStream;
import java.net.URL;

import static com.hazelcast.aws.impl.Constants.GET;

/**
 *
 */
public class TaskMetadata extends AwsOperation<String> {

    public TaskMetadata(AwsConfig awsConfig, URL endpointURL) {
        // TODO
        super(awsConfig, endpointURL, "", "", GET);
    }

    @Override
    String unmarshal(InputStream stream) {
        // TODO
        return null;
    }
}
