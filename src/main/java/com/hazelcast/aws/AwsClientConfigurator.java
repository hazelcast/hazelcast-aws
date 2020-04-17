package com.hazelcast.aws;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
        AwsMetadataApi metadataApi = new AwsMetadataApi(awsConfig);

        String region = resolveRegion(awsConfig, metadataApi, environment);
        validateRegion(region);

        AwsCredentialsProvider awsCredentialsProvider = new AwsCredentialsProvider(awsConfig, metadataApi, environment);
        AwsEc2Api ec2Api = createEc2Api(awsConfig, region);

        // EC2 Discovery
        if (!environment.isRunningOnEcs() || explicitlyEc2Configured(awsConfig)) {
            logEc2Environment(awsConfig, region);
            return new AwsEc2Client(ec2Api, metadataApi, awsCredentialsProvider);
        }

        // ECS Discovery
        AwsEcsApi ecsApi = createEcsApi(awsConfig, region);
        logEcsEnvironment(awsConfig, region);
        return new AwsEcsClient(ecsApi, ec2Api, metadataApi, awsCredentialsProvider);
    }

    static String resolveRegion(AwsConfig awsConfig, AwsMetadataApi metadataApi, Environment environment) {
        if (isNotEmpty(awsConfig.getRegion())) {
            return awsConfig.getRegion();
        }

        if (environment.isRunningOnEcs()) {
            return environment.getAwsRegionOnEcs();
        }

        String availabilityZone = metadataApi.availabilityZoneEc2();
        return availabilityZone.substring(0, availabilityZone.length() - 1);
    }

    private static AwsEc2Api createEc2Api(AwsConfig awsConfig, String region) {
        String ec2Endpoint = resolveEc2Endpoint(awsConfig, region);
        AwsRequestSigner ec2RequestSigner = new AwsRequestSigner(region, EC2_SERVICE_NAME);
        return new AwsEc2Api(ec2Endpoint, awsConfig, ec2RequestSigner, Clock.systemUTC());
    }

    private static AwsEcsApi createEcsApi(AwsConfig awsConfig, String region) {
        String ecsEndpoint = resolveEcsEndpoint(awsConfig, region);
        AwsRequestSigner ecsRequestSigner = new AwsRequestSigner(region, ECS_SERVICE_NAME);
        return new AwsEcsApi(ecsEndpoint, awsConfig, ecsRequestSigner, Clock.systemUTC());
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
    static boolean explicitlyEc2Configured(AwsConfig awsConfig) {
        return isNotEmpty(awsConfig.getHostHeader()) && awsConfig.getHostHeader().startsWith("ec2");
    }

    private static void logEc2Environment(AwsConfig awsConfig, String region) {
        Map<String, String> filters = new HashMap<>();
        filters.put("tag-key", awsConfig.getTagKey());
        filters.put("tag-value", awsConfig.getTagValue());
        filters.put("security-group-name", awsConfig.getSecurityGroupName());
        filters.put("hz-port", awsConfig.getHzPort().toString());

        LOGGER.info(String.format(
            "AWS plugin performing discovery in EC2 environment for region: '%s' filtered by: '%s'",
            region, logFilters(filters))
        );
    }

    private static void logEcsEnvironment(AwsConfig awsConfig, String region) {
        Map<String, String> filters = new HashMap<>();
        filters.put("hz-port", awsConfig.getHzPort().toString());

        LOGGER.info(String.format(
            "AWS plugin performing discovery in ECS environment for region: '%s' filtered by: '%s'",
            region, logFilters(filters))
        );
    }

    private static String logFilters(Map<String, String> parameters) {
        return parameters.entrySet().stream()
            .filter(e -> e.getValue() != null)
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(e -> String.format("%s:%s", e.getKey(), e.getValue()))
            .collect(Collectors.joining(", "));
    }
}
