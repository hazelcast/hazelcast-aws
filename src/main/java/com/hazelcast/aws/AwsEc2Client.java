package com.hazelcast.aws;

import java.util.Map;

class AwsEc2Client implements AwsClient {
    private final AwsMetadataApi awsMetadataApi;
    private final AwsEc2Api awsEc2Api;
    private final AwsCredentialsProvider awsCredentialsProvider;

    AwsEc2Client(AwsMetadataApi awsMetadataApi, AwsEc2Api awsEc2Api, AwsCredentialsProvider awsCredentialsProvider) {
        this.awsMetadataApi = awsMetadataApi;
        this.awsEc2Api = awsEc2Api;
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    @Override
    public Map<String, String> getAddresses() {
        return awsEc2Api.describeInstances(awsCredentialsProvider.credentials());
    }

    @Override
    public String getAvailabilityZone() {
        return awsMetadataApi.availabilityZoneEc2();
    }
}
