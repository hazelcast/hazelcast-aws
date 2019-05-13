/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.aws;

import com.hazelcast.aws.utility.StringUtils;
import com.hazelcast.config.InvalidConfigurationException;

import java.util.Collection;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.EC2_PREFIX;
import static com.hazelcast.aws.impl.Constants.ECS_PREFIX;
import static com.hazelcast.aws.impl.Constants.HOSTNAME_PREFIX_LENGTH;

/**
 * AWS client used to retrieve IP addresses. Delegates to an {@link AwsClientStrategy}
 */
class AwsClient {

    private final AwsClientStrategy clientStrategy;
    private String endpoint;

    /**
     * Creates an AwsClient
     * @param awsConfig configuration
     */
    AwsClient(AwsConfig awsConfig) {
        if (awsConfig == null) {
            throw new IllegalArgumentException("AwsConfig is required!");
        }
        String hostHeader = awsConfig.getHostHeader();
        this.endpoint = hostHeader;
        if (StringUtils.isNotEmpty(awsConfig.getRegion())) {
            if (!(hostHeader.startsWith(EC2_PREFIX) || hostHeader.startsWith(ECS_PREFIX))) {
                throw new InvalidConfigurationException(
                        String.format("HostHeader should start with \"%s\" or \"%s\" prefix, found: %s",
                                EC2_PREFIX, ECS_PREFIX, hostHeader));
            }
            String host = hostHeader.substring(0, HOSTNAME_PREFIX_LENGTH);
            this.endpoint = host + awsConfig.getRegion() + "." + hostHeader.substring(HOSTNAME_PREFIX_LENGTH);
        }
        clientStrategy = AwsClientStrategy.create(awsConfig, endpoint);
    }

    /**
     * @return the private IPs for Hazelcast members
     * @throws Exception
     */
    Collection<String> getPrivateIpAddresses() throws Exception {
        return clientStrategy.getPrivateIpAddresses();
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

    String getEndpoint() {
        return this.endpoint;
    }
}
