package com.hazelcast.aws;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.time.Clock;

import static com.hazelcast.aws.RegionValidator.validateRegion;
import static com.hazelcast.aws.StringUtils.isNotEmpty;

/**
 * Responsible for creating the correct {@code AwsClient} implementation.
 * <p>
 * It also creates and injects all dependencies.
 */
class AwsClientConfigurator {
    private static final ILogger LOGGER = Logger.getLogger(AwsClientConfigurator.class);

    private static final String DEFAULT_EC2_HOST_HEADER = "ec2.amazonaws.com";
    private static final String DEFAULT_ECS_HOST_HEADER = "ecs.amazonaws.com";

    private static final String EC2_SERVICE = "ec2";
    private static final String ECS_SERVICE = "ecs";

    private static final Environment environment = resolveEnvironment();

    static AwsClient createAwsClient(AwsConfig awsConfig) {
        String region = resolveRegion(awsConfig);
        validateRegion(region);

        String ec2Endpoint = resolveEc2Endpoint(awsConfig, region);
        AwsRequestSigner ec2RequestSigner = new AwsRequestSigner(region, ec2Endpoint, EC2_SERVICE);
        AwsEc2Api awsEc2Api = new AwsEc2Api(ec2Endpoint, awsConfig, ec2RequestSigner, Clock.systemUTC());
        AwsEc2MetadataApi ec2MetadataApi = new AwsEc2MetadataApi(awsConfig);
        AwsAuthenticator awsAuthenticator = new AwsAuthenticator(ec2MetadataApi, awsConfig, new com.hazelcast.aws.Environment());

        // EC2 Discovery
        if (Environment.EC2.equals(environment) || explicitlyEc2Configured(awsConfig)) {
            return new AwsEc2Client(ec2MetadataApi, awsEc2Api, awsAuthenticator);
        }

        // ECS Discovery
        String ecsEndpoint = resolveEcsEndpoint(awsConfig, region);
        AwsRequestSigner ecsRequestSigner = new AwsRequestSigner(region, ecsEndpoint, ECS_SERVICE);
        AwsEcsMetadataApi awsEcsMetadataApi = new AwsEcsMetadataApi(awsConfig);
        AwsEcsApi awsEcsApi = new AwsEcsApi(ecsEndpoint, awsConfig, ecsRequestSigner, Clock.systemUTC());
        return new AwsEcsClient(awsEcsMetadataApi, awsEcsApi, awsEc2Api, awsAuthenticator);
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
        return isNotEmpty(awsConfig.getHostHeader()) && awsConfig.getHostHeader().startsWith("ec2.");
    }

    /**
     * Visibility for testing.
     */
    static String resolveRegion(AwsConfig awsConfig) {
        if (isNotEmpty(awsConfig.getRegion())) {
            return awsConfig.getRegion();
        }

        if (Environment.ECS.equals(environment)) {
            return System.getenv("AWS_REGION");
        }

        AwsEc2MetadataApi metadataApi = new AwsEc2MetadataApi(awsConfig);
        String availabilityZone = metadataApi.availabilityZone();
        String region = availabilityZone.substring(0, availabilityZone.length() - 1);
        return region;
    }

    /**
     * Visibility for testing.
     */
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

    /**
     * Visibility for testing.
     */
    static String resolveEcsEndpoint(AwsConfig awsConfig, String region) {
        String ecsHostHeader = awsConfig.getHostHeader();
        if (StringUtils.isEmpty(ecsHostHeader) ||
            ecsHostHeader.equals("ecs")
        ) {
            ecsHostHeader = DEFAULT_ECS_HOST_HEADER;
        }
        return ecsHostHeader.replace("ecs.", "ecs." + region + ".");
    }

    private static Environment resolveEnvironment() {
        if (isRunningOn("ECS")) {
            return Environment.ECS;
        }
        return Environment.EC2;
    }

    private static boolean isRunningOn(String system) {
        String execEnv = new com.hazelcast.aws.Environment().getEnv("AWS_EXECUTION_ENV");
        return isNotEmpty(execEnv) && execEnv.contains(system);
    }

    private enum Environment {
        EC2, ECS;
    }
}
