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
import java.util.regex.Pattern;

public class AwsClient {
    private static final Pattern AWS_REGION_PATTERN =
        Pattern.compile("\\w{2}(-gov-|-)(north|northeast|east|southeast|south|southwest|west|northwest|central)-\\d(?!.+)");

    private final AwsMetadataApi awsMetadataApi;
    private final AwsConfig awsConfig;

    private final String region;
    private final String endpoint;

    AwsClient(AwsMetadataApi awsMetadataApi, AwsConfig awsConfig) {
        this.awsMetadataApi = awsMetadataApi;
        this.awsConfig = awsConfig;

        this.region = regionFromConfigOrMetadataApi();
        this.endpoint = resolveEndpoint();

        validateRegion(region);
    }

    String resolveEndpoint() {
        if (!awsConfig.getHostHeader().startsWith("ec2.")) {
            throw new InvalidConfigurationException("HostHeader should start with \"ec2.\" prefix");
        }
        if (StringUtil.isNotEmpty(region)) {
            return awsConfig.getHostHeader().replace("ec2.", "ec2." + region + ".");
        }
        return awsConfig.getHostHeader();
    }

    private String regionFromConfigOrMetadataApi() {
        if (StringUtil.isNotEmpty(awsConfig.getRegion())) {
            return awsConfig.getRegion();
        }

        String availabilityZone = awsMetadataApi.availabilityZone();
        return availabilityZone.substring(0, availabilityZone.length() - 1);
    }

    static void validateRegion(String region) {
        if (!AWS_REGION_PATTERN.matcher(region).matches()) {
            String message = String.format("The provided region %s is not a valid AWS region.", region);
            throw new InvalidConfigurationException(message);
        }
    }

    Map<String, String> getAddresses() throws IOException {
        return new DescribeInstances(awsConfig, region, endpoint).execute();
    }

    String getAvailabilityZone() {
        return awsMetadataApi.availabilityZone();
    }

}
