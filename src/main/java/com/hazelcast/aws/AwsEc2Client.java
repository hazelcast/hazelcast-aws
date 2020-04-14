package com.hazelcast.aws;

import java.util.Map;

class AwsEc2Client implements AwsClient {
    private final AwsEc2MetadataApi awsEc2MetadataApi;
    private final AwsEc2Api awsEc2Api;
    private final AwsAuthenticator awsAuthenticator;

    AwsEc2Client(AwsEc2MetadataApi awsEc2MetadataApi, AwsEc2Api awsEc2Api, AwsAuthenticator awsAuthenticator) {
        this.awsEc2MetadataApi = awsEc2MetadataApi;
        this.awsEc2Api = awsEc2Api;
        this.awsAuthenticator = awsAuthenticator;
    }

    @Override
    public Map<String, String> getAddresses() {
        return awsEc2Api.describeInstances(awsAuthenticator.credentials());
    }

    @Override
    public String getAvailabilityZone() {
        return awsEc2MetadataApi.availabilityZone();
    }
}
