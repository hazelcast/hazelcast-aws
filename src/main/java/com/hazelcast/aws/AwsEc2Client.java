package com.hazelcast.aws;

import java.util.Map;

class AwsEc2Client implements AwsClient {
    private final AwsEc2Api awsEc2Api;
    private final AwsMetadataApi awsMetadataApi;
    private final AwsCredentialsProvider awsCredentialsProvider;

    AwsEc2Client(AwsEc2Api awsEc2Api, AwsMetadataApi awsMetadataApi, AwsCredentialsProvider awsCredentialsProvider) {
        this.awsEc2Api = awsEc2Api;
        this.awsMetadataApi = awsMetadataApi;
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
