package com.hazelcast.aws;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.time.Clock;

/**
 * Responsible for creating the right {@code AwsClient}.
 * <p>
 * It also create inject all dependencies.
 */
class AwsClientConfigurator {
    private static final ILogger LOGGER = Logger.getLogger(AwsClientConfigurator.class);

    static AwsClient createAwsClient(AwsConfig awsConfig) {


//        AwsMetadataApi awsMetadataApi = new AwsMetadataApi(awsConfig);
//        AwsEc2Api awsEc2Api = new AwsEc2Api("ec2.eu-central-1.amazonaws"
//            + ".com", awsConfig,
//            new AwsEc2RequestSigner("ec2"), Clock.systemUTC());
//        return new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, new Environment());

//        AwsMetadataApi metadataApi = new AwsMetadataApi(awsConfig);
//        String region = resolveRegion(metadataApi, awsConfig);

        String endMetadataEndpoint = new Environment().getEnv("ECS_CONTAINER_METADATA_URI");
        LOGGER.info("ECS Metadata Endpoint: " + endMetadataEndpoint);
        AwsEcsMetadataApi awsEcsMetadataApi = new AwsEcsMetadataApi(endMetadataEndpoint, awsConfig);
        AwsEc2Api awsEc2Api = new AwsEc2Api("ec2.eu-central-1.amazonaws.com", awsConfig, new AwsEc2RequestSigner("ec2"),
            Clock.systemUTC());
        AwsEcsApi awsEcsApi = new AwsEcsApi("ecs.eu-central-1.amazonaws.com", awsConfig, new AwsEc2RequestSigner("ecs"),
            Clock.systemUTC());
        return new AwsEcsClient(awsEcsMetadataApi, awsEcsApi, awsEc2Api, awsConfig);
    }

    /**
     * Visibility for testing.
     */
    static String resolveRegion(AwsMetadataApi metadataApi, AwsConfig awsConfig) {
        if (StringUtils.isNotEmpty(awsConfig.getRegion())) {
            return awsConfig.getRegion();
        }

        String availabilityZone = metadataApi.availabilityZone();
        return availabilityZone.substring(0, availabilityZone.length() - 1);
    }
}
