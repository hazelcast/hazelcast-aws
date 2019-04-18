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

import com.hazelcast.config.InvalidConfigurationException;

import java.util.Collection;
import java.util.Map;

import static com.hazelcast.aws.utility.MetadataUtil.AVAILABILITY_ZONE_URI;
import static com.hazelcast.aws.utility.MetadataUtil.INSTANCE_METADATA_URI;
import static com.hazelcast.aws.utility.MetadataUtil.retrieveMetadataFromURI;

public class AWSClient {

    private final AwsConfig awsConfig;
    private final AwsClientStrategy clientStrategy;
    private String endpoint;

    public AWSClient(AwsConfig awsConfig) {
        if (awsConfig == null) {
            throw new IllegalArgumentException("AwsConfig is required!");
        }
        this.awsConfig = awsConfig;
        String hostHeader = awsConfig.getHostHeader();
        this.endpoint = hostHeader;
        if (awsConfig.getRegion() != null && awsConfig.getRegion().length() > 0) {
            if (!(hostHeader.startsWith("ec2.") || hostHeader.startsWith("ecs."))) {
                throw new InvalidConfigurationException("HostHeader should start with \"ec2.\" or \"ecs.\" prefix, found: " + hostHeader);
            }
            String host = hostHeader.substring(0, 4);
            this.endpoint = hostHeader.replace(host, host + awsConfig.getRegion() + ".");
        }
        clientStrategy = AwsClientStrategy.create(awsConfig, endpoint);
    }

    public Collection<String> getPrivateIpAddresses() throws Exception {
        return clientStrategy.getPrivateIpAddresses();
    }

    public Map<String, String> getAddresses() throws Exception {
        return clientStrategy.getAddresses();
    }

    public String getAvailabilityZone() {
        String uri = INSTANCE_METADATA_URI.concat(AVAILABILITY_ZONE_URI);
        return retrieveMetadataFromURI(uri, awsConfig.getConnectionTimeoutSeconds(), awsConfig.getConnectionRetries());
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    @Deprecated
    public void setEndpoint(String s) {
        this.endpoint = s;
    }
}
