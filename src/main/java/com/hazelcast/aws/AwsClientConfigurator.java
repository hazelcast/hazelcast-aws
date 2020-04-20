/*
 * Copyright 2020 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.hazelcast.aws;

import com.hazelcast.aws.AwsMetadataApi.EcsMetadata;
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
final class AwsClientConfigurator {
    private static final ILogger LOGGER = Logger.getLogger(AwsClientConfigurator.class);

    private static final String DEFAULT_EC2_HOST_HEADER = "ec2.amazonaws.com";
    private static final String DEFAULT_ECS_HOST_HEADER = "ecs.amazonaws.com";

    private static final String EC2_SERVICE_NAME = "ec2";
    private static final String ECS_SERVICE_NAME = "ecs";

    private AwsClientConfigurator() {
    }

    static AwsClient createAwsClient(AwsConfig awsConfig) {
        Environment environment = new Environment();
        AwsMetadataApi metadataApi = new AwsMetadataApi(awsConfig);

        String region = resolveRegion(awsConfig, metadataApi, environment);
        validateRegion(region);

        AwsCredentialsProvider awsCredentialsProvider = new AwsCredentialsProvider(awsConfig, metadataApi, environment);
        AwsEc2Api ec2Api = createEc2Api(awsConfig, region);

        // EC2 Discovery
        if ((!environment.isRunningOnEcs() && !explicitlyEcsConfigured(awsConfig)) || explicitlyEc2Configured(awsConfig)) {
            logEc2Environment(awsConfig, region);
            return new AwsEc2Client(ec2Api, metadataApi, awsCredentialsProvider);
        }

        // ECS Discovery
        EcsMetadata metadata = tryToFetchEcsMetadata(metadataApi);
        String taskArn = metadata.getTaskArn();
        String cluster = resolveCluster(awsConfig, metadata);
        AwsEcsApi ecsApi = createEcsApi(awsConfig, region);
        logEcsEnvironment(awsConfig, region, cluster);
        return new AwsEcsClient(taskArn, cluster, ecsApi, ec2Api, awsCredentialsProvider);
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
        if (StringUtils.isEmpty(ec2HostHeader)
            || ec2HostHeader.startsWith("ecs")
            || ec2HostHeader.equals("ec2")
        ) {
            ec2HostHeader = DEFAULT_EC2_HOST_HEADER;
        }
        return ec2HostHeader.replace("ec2.", "ec2." + region + ".");
    }

    static String resolveEcsEndpoint(AwsConfig awsConfig, String region) {
        String ecsHostHeader = awsConfig.getHostHeader();
        if (StringUtils.isEmpty(ecsHostHeader)
            || ecsHostHeader.equals("ecs")
        ) {
            ecsHostHeader = DEFAULT_ECS_HOST_HEADER;
        }
        return ecsHostHeader.replace("ecs.", "ecs." + region + ".");
    }

    static boolean explicitlyEcsConfigured(AwsConfig awsConfig) {
        return isNotEmpty(awsConfig.getCluster())
            || (isNotEmpty(awsConfig.getHostHeader()) && awsConfig.getHostHeader().startsWith("ecs"));
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

    private static EcsMetadata tryToFetchEcsMetadata(AwsMetadataApi metadataApi) {
        try {
            return metadataApi.metadataEcs();
        } catch (Exception e) {
            LOGGER.fine(e);
            // if no access to metadata, just return an empty EcsMetadata
            return new EcsMetadata(null, null);
        }
    }

    static String resolveCluster(AwsConfig awsConfig, EcsMetadata metadata) {
        if (isNotEmpty(awsConfig.getCluster())) {
            return awsConfig.getCluster();
        }
        LOGGER.info("No ECS cluster defined, using current cluster: " + metadata.getClusterArn());
        return metadata.getClusterArn();
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

    private static void logEcsEnvironment(AwsConfig awsConfig, String region, String cluster) {
        Map<String, String> filters = new HashMap<>();
        filters.put("hz-port", awsConfig.getHzPort().toString());

        LOGGER.info(String.format(
            "AWS plugin performing discovery in ECS environment for region: '%s' for cluster: '%s' filtered by: '%s'",
            region, cluster, logFilters(filters))
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
