package com.hazelcast.aws;

import java.util.Map;

class AwsEc2Client implements AwsClient {
    private final AwsMetadataApi awsMetadataApi;
    private final AwsEc2Api awsEc2Api;
    private final AwsAuthenticator awsAuthenticator;

    AwsEc2Client(AwsMetadataApi awsMetadataApi, AwsEc2Api awsEc2Api, AwsAuthenticator awsAuthenticator) {
        this.awsMetadataApi = awsMetadataApi;
        this.awsEc2Api = awsEc2Api;
        this.awsAuthenticator = awsAuthenticator;
    }

    @Override
    public Map<String, String> getAddresses() {
        return awsEc2Api.describeInstances(awsAuthenticator.credentials());
    }

    @Override
    public String getAvailabilityZone() {
        return awsMetadataApi.availabilityZone();
    }
}
