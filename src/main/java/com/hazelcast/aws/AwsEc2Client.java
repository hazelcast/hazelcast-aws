package com.hazelcast.aws;

import java.util.Map;

class AwsEc2Client implements AwsClient {
    private final AwsEc2MetadataApi awsEc2MetadataApi;
    private final AwsEc2Api awsEc2Api;
    private final AwsCredentialsProvider awsCredentialsProvider;

    AwsEc2Client(AwsEc2MetadataApi awsEc2MetadataApi, AwsEc2Api awsEc2Api, AwsCredentialsProvider awsCredentialsProvider) {
        this.awsEc2MetadataApi = awsEc2MetadataApi;
        this.awsEc2Api = awsEc2Api;
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    @Override
    public Map<String, String> getAddresses() {
        return awsEc2Api.describeInstances(awsCredentialsProvider.credentials());
    }

    @Override
    public String getAvailabilityZone() {
        return awsEc2MetadataApi.availabilityZone();
    }
}
