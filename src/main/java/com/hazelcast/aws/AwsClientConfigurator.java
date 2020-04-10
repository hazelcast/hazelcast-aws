package com.hazelcast.aws;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.time.Clock;

import static com.hazelcast.aws.RegionValidator.validateRegion;
import static com.hazelcast.aws.StringUtils.isNotEmpty;

/**
 * Responsible for creating the right {@code AwsClient}.
 * <p>
 * It also create inject all dependencies.
 */
class AwsClientConfigurator {
    private static final ILogger LOGGER = Logger.getLogger(AwsClientConfigurator.class);

    static AwsClient createAwsClient(AwsConfig awsConfig) {
        String region = resolveRegion(awsConfig);
        String ec2Endpoint = resolveEc2Endpoint(awsConfig, region);
        AwsRequestSigner ec2RequestSigner = new AwsRequestSigner(region, ec2Endpoint, "ec2");
        AwsEc2Api awsEc2Api = new AwsEc2Api(ec2Endpoint, awsConfig, ec2RequestSigner, Clock.systemUTC());
        AwsMetadataApi awsMetadataApi = new AwsMetadataApi(awsConfig);
        AwsAuthenticator awsAuthenticator = new AwsAuthenticator(awsMetadataApi, awsConfig, new Environment());

        if (isRunningOnEcs()) {
            String ecsEndpoint = resolveEcsEndpoint(awsConfig, region);
            AwsRequestSigner ecsRequestSigner = new AwsRequestSigner(region, ecsEndpoint, "ecs");
            AwsEcsMetadataApi awsEcsMetadataApi = new AwsEcsMetadataApi(awsConfig);
            AwsEcsApi awsEcsApi = new AwsEcsApi(ecsEndpoint, awsConfig, ecsRequestSigner,
                Clock.systemUTC());
            return new AwsEcsClient(awsEcsMetadataApi, awsEcsApi, awsEc2Api, awsAuthenticator);
        }

        return new AwsEc2Client(awsMetadataApi, awsEc2Api, awsAuthenticator);
    }


    /**
     * Visibility for testing.
     */
    static String resolveRegion(AwsConfig awsConfig) {
        if (isNotEmpty(awsConfig.getRegion())) {
            return awsConfig.getRegion();
        }

        if (isRunningOnEcs()) {
            return System.getenv("AWS_REGION");
        }

        AwsMetadataApi metadataApi = new AwsMetadataApi(awsConfig);
        String availabilityZone = metadataApi.availabilityZone();
        String region = availabilityZone.substring(0, availabilityZone.length() - 1);
        validateRegion(region);
        return region;
    }

    /**
     * Visibility for testing.
     */
    static String resolveEc2Endpoint(AwsConfig awsConfig, String region) {
        // TODO
//        if (isNotEmpty(awsConfig.getHostHeader())) {
//            if (!awsConfig.getHostHeader().startsWith("ec2.") && !awsConfig.getHostHeader().startsWith("ecs.")) {
//                throw new InvalidConfigurationException("HostHeader should start with \"ec2.\" or \"ecs\" prefix");
//            }
//            return awsConfig.getHostHeader()
//                .replace("ec2.", "ec2." + region + ".")
//                .replace("ecs.", "ecs." + region + ".");
//        }
//        if (isRunningOnEcs()) {
//            return "ec2." + region + "amazonaws.com";
//        }
//        return "ecs." + region + "amazonaws.com";
        return "ec2." + region + ".amazonaws.com";
    }

    static String resolveEcsEndpoint(AwsConfig awsConfig, String region) {
        // TODO
//        if (isNotEmpty(awsConfig.getHostHeader())) {
//            if (!awsConfig.getHostHeader().startsWith("ec2.") && !awsConfig.getHostHeader().startsWith("ecs.")) {
//                throw new InvalidConfigurationException("HostHeader should start with \"ec2.\" or \"ecs\" prefix");
//            }
//            return awsConfig.getHostHeader()
//                .replace("ec2.", "ec2." + region + ".")
//                .replace("ecs.", "ecs." + region + ".");
//        }
//        if (isRunningOnEcs()) {
//            return "ec2." + region + "amazonaws.com";
//        }
//        return "ecs." + region + "amazonaws.com";
        return "ecs." + region + ".amazonaws.com";
    }

    private static boolean isRunningOnEc2() {
        return isRunningOn("ECS");
    }

    private static boolean isRunningOnEcs() {
        return isRunningOn("ECS");
    }

    private static boolean isRunningOn(String system) {
        String execEnv = new Environment().getEnv("AWS_EXECUTION_ENV");
        return isNotEmpty(execEnv) && execEnv.contains(system);
    }
}
