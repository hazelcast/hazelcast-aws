package com.hazelcast.aws;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.time.Clock;

import static com.hazelcast.aws.RegionValidator.validateRegion;
import static com.hazelcast.aws.StringUtils.isNotEmpty;

/**
 * Responsible for creating the correct {@code AwsClient} implementation.
 * <p>
 * Note that it also creates and injects all dependencies.
 */
class AwsClientConfigurator {
    private static final ILogger LOGGER = Logger.getLogger(AwsClientConfigurator.class);

    private static final String DEFAULT_EC2_HOST_HEADER = "ec2.amazonaws.com";
    private static final String DEFAULT_ECS_HOST_HEADER = "ecs.amazonaws.com";

    private static final String EC2_SERVICE_NAME = "ec2";
    private static final String ECS_SERVICE_NAME = "ecs";

    static AwsClient createAwsClient(AwsConfig awsConfig) {
        Environment environment = new Environment();

        String region = resolveRegion(awsConfig, environment);
        validateRegion(region);

        String ec2Endpoint = resolveEc2Endpoint(awsConfig, region);
        AwsRequestSigner ec2RequestSigner = new AwsRequestSigner(region, EC2_SERVICE_NAME);
        AwsEc2Api ec2Api = new AwsEc2Api(ec2Endpoint, awsConfig, ec2RequestSigner, Clock.systemUTC());
        AwsMetadataApi metadataApi = new AwsMetadataApi(awsConfig);
        AwsCredentialsProvider awsCredentialsProvider = new AwsCredentialsProvider(awsConfig, metadataApi, environment);

        // EC2 Discovery
        if (!environment.isRunningOnEcs() || explicitlyEc2Configured(awsConfig)) {
            LOGGER.info("Using AWS discovery for EC2 environment");
            return new AwsEc2Client(ec2Api, metadataApi, awsCredentialsProvider);
        }

        // ECS Discovery
        String ecsEndpoint = resolveEcsEndpoint(awsConfig, region);
        AwsRequestSigner ecsRequestSigner = new AwsRequestSigner(region, ECS_SERVICE_NAME);
        AwsEcsApi ecsApi = new AwsEcsApi(ecsEndpoint, awsConfig, ecsRequestSigner, Clock.systemUTC());
        LOGGER.info("Using AWS discovery for ECS environment");
        return new AwsEcsClient(ecsApi, ec2Api, metadataApi, awsCredentialsProvider);
    }

    static String resolveRegion(AwsConfig awsConfig, Environment environment) {
        if (isNotEmpty(awsConfig.getRegion())) {
            return awsConfig.getRegion();
        }

        if (environment.isRunningOnEcs()) {
            return System.getenv("AWS_REGION");
        }

        AwsMetadataApi metadataApi = new AwsMetadataApi(awsConfig);
        String availabilityZone = metadataApi.availabilityZoneEc2();
        return availabilityZone.substring(0, availabilityZone.length() - 1);
    }

    static String resolveEc2Endpoint(AwsConfig awsConfig, String region) {
        String ec2HostHeader = awsConfig.getHostHeader();
        if (StringUtils.isEmpty(ec2HostHeader) ||
            ec2HostHeader.startsWith("ecs") ||
            ec2HostHeader.equals("ec2")
        ) {
            ec2HostHeader = DEFAULT_EC2_HOST_HEADER;
        }
        return ec2HostHeader.replace("ec2.", "ec2." + region + ".");
    }

    static String resolveEcsEndpoint(AwsConfig awsConfig, String region) {
        String ecsHostHeader = awsConfig.getHostHeader();
        if (StringUtils.isEmpty(ecsHostHeader) ||
            ecsHostHeader.equals("ecs")
        ) {
            ecsHostHeader = DEFAULT_ECS_HOST_HEADER;
        }
        return ecsHostHeader.replace("ecs.", "ecs." + region + ".");
    }

    /**
     * Checks if EC2 environment was explicitly configured in the Hazelcast configuration.
     * <p>
     * Hazelcast may run inside ECS, but use EC2 discovery when:
     * <ul>
     * <li>EC2 cluster uses EC2 mode (not Fargate)</li>
     * <li>Containers are run in "host" network mode</li>
     * </ul>
     */
    private static boolean explicitlyEc2Configured(AwsConfig awsConfig) {
        return isNotEmpty(awsConfig.getHostHeader()) && awsConfig.getHostHeader().startsWith("ec2");
    }
}
