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

import com.hazelcast.cluster.Address;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.hazelcast.spi.partitiongroup.PartitionGroupMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hazelcast.aws.AwsProperties.ACCESS_KEY;
import static com.hazelcast.aws.AwsProperties.CLUSTER;
import static com.hazelcast.aws.AwsProperties.CONNECTION_RETRIES;
import static com.hazelcast.aws.AwsProperties.CONNECTION_TIMEOUT_SECONDS;
import static com.hazelcast.aws.AwsProperties.FAMILY;
import static com.hazelcast.aws.AwsProperties.HOST_HEADER;
import static com.hazelcast.aws.AwsProperties.IAM_ROLE;
import static com.hazelcast.aws.AwsProperties.PORT;
import static com.hazelcast.aws.AwsProperties.READ_TIMEOUT_SECONDS;
import static com.hazelcast.aws.AwsProperties.REGION;
import static com.hazelcast.aws.AwsProperties.SECRET_KEY;
import static com.hazelcast.aws.AwsProperties.SECURITY_GROUP_NAME;
import static com.hazelcast.aws.AwsProperties.SERVICE_NAME;
import static com.hazelcast.aws.AwsProperties.TAG_KEY;
import static com.hazelcast.aws.AwsProperties.TAG_VALUE;

/**
 * AWS implementation of {@link DiscoveryStrategy}.
 *
 * @see AwsClient
 */
public class AwsDiscoveryStrategy
        extends AbstractDiscoveryStrategy {
    private static final ILogger LOGGER = Logger.getLogger(AwsDiscoveryStrategy.class);

    private static final String DEFAULT_PORT_RANGE = "5701-5708";
    private static final Integer DEFAULT_CONNECTION_RETRIES = 3;
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 10;

    private final AwsClient awsClient;
    private final PortRange portRange;

    private final Map<String, String> memberMetadata = new HashMap<>();

    private boolean isKnownExceptionAlreadyLogged;

    AwsDiscoveryStrategy(Map<String, Comparable> properties) {
        super(LOGGER, properties);

        AwsConfig awsConfig = createAwsConfig();
        LOGGER.info("Using AWS discovery plugin with configuration: " + awsConfig);

        this.awsClient = AwsClientConfigurator.createAwsClient(awsConfig);
        this.portRange = awsConfig.getHzPort();
    }

    /**
     * For test purposes only.
     */
    AwsDiscoveryStrategy(Map<String, Comparable> properties, AwsClient client) {
        super(LOGGER, properties);
        this.awsClient = client;
        this.portRange = createAwsConfig().getHzPort();
    }

    private AwsConfig createAwsConfig() {
        try {
            return AwsConfig.builder()
                .setAccessKey(getOrNull(ACCESS_KEY)).setSecretKey(getOrNull(SECRET_KEY))
                .setRegion(getOrDefault(REGION.getDefinition(), null))
                .setIamRole(getOrNull(IAM_ROLE))
                .setHostHeader(getOrNull(HOST_HEADER.getDefinition()))
                .setSecurityGroupName(getOrNull(SECURITY_GROUP_NAME)).setTagKey(getOrNull(TAG_KEY))
                .setTagValue(getOrNull(TAG_VALUE))
                .setConnectionTimeoutSeconds(getOrDefault(CONNECTION_TIMEOUT_SECONDS.getDefinition(),
                    DEFAULT_CONNECTION_TIMEOUT_SECONDS))
                .setConnectionRetries(getOrDefault(CONNECTION_RETRIES.getDefinition(), DEFAULT_CONNECTION_RETRIES))
                .setReadTimeoutSeconds(getOrDefault(READ_TIMEOUT_SECONDS.getDefinition(), DEFAULT_READ_TIMEOUT_SECONDS))
                .setHzPort(new PortRange(getPortRange()))
                .setCluster(getOrNull(CLUSTER))
                .setFamily(getOrNull(FAMILY))
                .setServiceName(getOrNull(SERVICE_NAME))
                .build();
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException("AWS configuration is not valid", e);
        }
    }

    /**
     * Returns port range from properties or default value if the property does not exist.
     * <p>
     * Note that {@link AbstractDiscoveryStrategy#getOrDefault(PropertyDefinition, Comparable)} cannot be reused, since
     * the "hz-port" property can be either {@code String} or {@code Integer}.
     */
    private String getPortRange() {
        Object portRange = getOrNull(PORT.getDefinition());
        if (portRange == null) {
            return DEFAULT_PORT_RANGE;
        }
        return portRange.toString();
    }

    @Override
    public Map<String, String> discoverLocalMetadata() {
        if (memberMetadata.isEmpty()) {
            String availabilityZone = awsClient.getAvailabilityZone();
            LOGGER.info(String.format("Availability zone found: '%s'", availabilityZone));
            memberMetadata.put(PartitionGroupMetaData.PARTITION_GROUP_ZONE, availabilityZone);
        }
        return memberMetadata;
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        try {
            Map<String, String> addresses = awsClient.getAddresses();
            logResult(addresses);

            List<DiscoveryNode> result = new ArrayList<>();
            for (Map.Entry<String, String> entry : addresses.entrySet()) {
                for (int port = portRange.getFromPort(); port <= portRange.getToPort(); port++) {
                    Address privateAddress = new Address(entry.getKey(), port);
                    Address publicAddress = new Address(entry.getValue(), port);
                    result.add(new SimpleDiscoveryNode(privateAddress, publicAddress));
                }
            }
            return result;
        } catch (NoCredentialsException e) {
            if (!isKnownExceptionAlreadyLogged) {
                LOGGER.warning("No AWS credentials found! Starting standalone. To use Hazelcast AWS discovery, configure"
                        + " properties (access-key, secret-key) or assign the required IAM Role to your EC2 instance");
                LOGGER.finest(e);
                isKnownExceptionAlreadyLogged = true;
            }
        } catch (RestClientException e) {
            if (e.getHttpErrorCode() == 403) {
                if (!isKnownExceptionAlreadyLogged) {
                    LOGGER.warning("AWS IAM Role Policy is missing 'ec2:DescribeInstances' Action! Starting standalone.");
                    isKnownExceptionAlreadyLogged = true;
                }
                LOGGER.finest(e);
            } else {
                LOGGER.warning("Cannot discover nodes. Starting standalone.", e);
            }
        } catch (Exception e) {
            LOGGER.warning("Cannot discover nodes. Starting standalone.", e);
        }
        return Collections.emptyList();
    }

    private static void logResult(Map<String, String> addresses) {
        if (addresses.isEmpty()) {
            LOGGER.warning("No IP addresses found! Starting standalone.");
        }

        LOGGER.fine(String.format("Found the following (private => public) addresses: %s", addresses));
    }

    private String getOrNull(AwsProperties awsProperties) {
        return getOrNull(awsProperties.getDefinition());
    }
}
