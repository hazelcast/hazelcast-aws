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

import com.hazelcast.config.InvalidConfigurationException;

import java.util.Map;

import static com.hazelcast.aws.impl.Constants.EC2_PREFIX;
import static com.hazelcast.aws.impl.Constants.ECS_PREFIX;
import static com.hazelcast.aws.impl.Constants.HOSTNAME_PREFIX_LENGTH;
import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;

/**
 * AWS client used to discover IP addresses. Delegates to an {@link AwsClientStrategy}
 */
public class AwsClient {

    private final AwsClientStrategy clientStrategy;

    /**
     * Creates an AwsClient
     * @param awsConfig configuration
     */
    public AwsClient(AwsConfig awsConfig) {
        if (awsConfig == null) {
            throw new IllegalArgumentException("AwsConfig is required!");
        }
        String hostHeader = awsConfig.getHostHeader();
        if (isNotEmpty(awsConfig.getRegion())) {
            hostHeader = createEndpoint(awsConfig, hostHeader);
        }
        clientStrategy = AwsClientStrategy.create(awsConfig, hostHeader);
    }

    private String createEndpoint(AwsConfig awsConfig, String hostHeader) {
        if (!(hostHeader.startsWith(EC2_PREFIX) || hostHeader.startsWith(ECS_PREFIX))) {
            throw new InvalidConfigurationException(
              String.format("HostHeader should start with \"%s\" or \"%s\" prefix, found: %s",
                EC2_PREFIX, ECS_PREFIX, hostHeader));
        }
        String hostName = hostHeader.substring(0, HOSTNAME_PREFIX_LENGTH);
        return hostName + awsConfig.getRegion() + "." + hostHeader.substring(HOSTNAME_PREFIX_LENGTH);
    }

    /**
     * @return a map of private IP -> public IP addresses
     * @throws Exception
     */
    Map<String, String> getAddresses() throws Exception {
        return clientStrategy.getAddresses();
    }

    /**
     * @return a string representing the current AZ
     */
    String getAvailabilityZone() {
        return clientStrategy.getAvailabilityZone();
    }

    // Visible for testing
    String getEndpoint() {
        return clientStrategy.endpoint;
    }
}
