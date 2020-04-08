package com.hazelcast.aws;

import com.hazelcast.aws.AwsEcsMetadataApi.EcsMetadata;

import java.util.Map;

public class AwsEcsClient {
    private final AwsEcsMetadataApi awsEcsMetadataApi;

    public AwsEcsClient(AwsEcsMetadataApi awsEcsMetadataApi) {
        this.awsEcsMetadataApi = awsEcsMetadataApi;
    }

    Map<String, String> getAddresses() {
        EcsMetadata metadata = awsEcsMetadataApi.metadata();
        // TODO
        return null;
    }

    String getAvailabilityZone() {
        // TODO: Return fetching availability zone
        return "unknown";
    }
}
