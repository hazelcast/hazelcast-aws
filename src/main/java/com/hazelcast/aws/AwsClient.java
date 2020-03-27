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

import com.hazelcast.aws.impl.DescribeInstances;
import com.hazelcast.aws.utility.StringUtil;
import com.hazelcast.config.InvalidConfigurationException;

import java.io.IOException;
import java.util.Map;

public class AwsClient {

    private final AwsConfig awsConfig;
    private final String endpoint;
    private final AwsMetadataApi awsMetadataApi;

    AwsClient(AwsMetadataApi awsMetadataApi, AwsConfig awsConfig) {
        this.awsMetadataApi = awsMetadataApi;

        if (awsConfig == null) {
            throw new IllegalArgumentException("AwsConfig is required!");
        }
        this.awsConfig = awsConfig;
        this.endpoint = resolveEndpoint(awsConfig);
    }

    static String resolveEndpoint(AwsConfig awsConfig) {
        if (!awsConfig.getHostHeader().startsWith("ec2.")) {
            throw new InvalidConfigurationException("HostHeader should start with \"ec2.\" prefix");
        }
        if (StringUtil.isNotEmpty(awsConfig.getRegion())) {
            return awsConfig.getHostHeader().replace("ec2.", "ec2." + awsConfig.getRegion() + ".");
        }
        return awsConfig.getHostHeader();
    }

    Map<String, String> getAddresses() throws IOException {
        return new DescribeInstances(awsConfig, endpoint).execute();
    }

    String getAvailabilityZone() {
        return awsMetadataApi.availabilityZone();
    }

}
