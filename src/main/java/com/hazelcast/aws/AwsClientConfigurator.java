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
//        AwsDescribeInstancesApi awsDescribeInstancesApi = new AwsDescribeInstancesApi(awsConfig,
//            new AwsEc2RequestSigner(), Clock.systemUTC());
        String endMetadataEndpoint = new Environment().getEnv("ECS_CONTAINER_METADATA_URI");
        LOGGER.info("ECS Metadata Endpoint: " + endMetadataEndpoint);
        AwsEcsMetadataApi awsEcsMetadataApi = new AwsEcsMetadataApi(endMetadataEndpoint, awsConfig);
        AwsDescribeNetworkInterfacesApi awsDescribeNetworkInterfacesApi =
            new AwsDescribeNetworkInterfacesApi("ec2.eu-central-1.amazonaws.com", awsConfig, new AwsEc2RequestSigner("ec2"), Clock.systemUTC());

//        this.awsClient = new AwsClient(awsMetadataApi, awsDescribeInstancesApi, awsConfig, new Environment());
        AwsEcsApi awsEcsApi = new AwsEcsApi("ecs.eu-central-1.amazonaws.com", awsConfig, new AwsEc2RequestSigner("ecs"),
            Clock.systemUTC());
        return new AwsEcsClient(awsEcsMetadataApi, awsEcsApi, awsDescribeNetworkInterfacesApi, awsConfig);
    }
}
