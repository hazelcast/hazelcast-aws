package com.hazelcast.aws;

import com.hazelcast.aws.AwsEcsMetadataApi.EcsMetadata;

import java.util.Map;

public class AwsEcsClient {
    private final AwsEcsMetadataApi awsEcsMetadataApi;
    private final String clusterArn;
    private final String familyName;

    public AwsEcsClient(AwsEcsMetadataApi awsEcsMetadataApi, AwsClient awsClient) {
        this.awsEcsMetadataApi = awsEcsMetadataApi;

        // TODO: Add config parameters
        EcsMetadata metadata = awsEcsMetadataApi.metadata();
        this.clusterArn = metadata.getClusterArn();
        this.familyName = metadata.getFamilyName();
    }

    Map<String, String> getAddresses() {

        // TODO
        return null;
    }

    String getAvailabilityZone() {
        // TODO: Return fetching availability zone
        return "unknown";
    }
}
