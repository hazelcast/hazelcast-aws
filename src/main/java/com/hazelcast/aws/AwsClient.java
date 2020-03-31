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

import com.hazelcast.aws.utility.Environment;
import com.hazelcast.aws.utility.StringUtil;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.hazelcast.aws.utility.StringUtil.isEmpty;

public class AwsClient {
    private static final ILogger LOGGER = Logger.getLogger(AwsClient.class);
    private static final Pattern AWS_REGION_PATTERN =
        Pattern.compile("\\w{2}(-gov-|-)(north|northeast|east|southeast|south|southwest|west|northwest|central)-\\d(?!.+)");

    private final AwsMetadataApi awsMetadataApi;
    private final AwsDescribeInstancesApi awsDescribeInstancesApi;
    private final AwsConfig awsConfig;

    private final String region;
    private final String endpoint;
    private String iamRole;

    AwsClient(AwsMetadataApi awsMetadataApi, AwsDescribeInstancesApi awsDescribeInstancesApi, AwsConfig awsConfig) {
        this.awsMetadataApi = awsMetadataApi;
        this.awsDescribeInstancesApi = awsDescribeInstancesApi;
        this.awsConfig = awsConfig;

        this.region = regionFromConfigOrMetadataApi();
        this.endpoint = resolveEndpoint();
        this.iamRole = resolveIamRole();

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
        return awsDescribeInstancesApi.addresses(region, endpoint, prepareCredentials());
    }

    private AwsCredentials prepareCredentials() {
        if (StringUtil.isNotEmpty(awsConfig.getAccessKey())) {
            // authenticate using access key and secret key from the configuration
            return AwsCredentials.builder()
                .setAccessKey(awsConfig.getAccessKey())
                .setSecretKey(awsConfig.getSecretKey())
                .build();
        }

        if (iamRole == null) {
            iamRole = resolveIamRole();
        }

        if (StringUtil.isNotEmpty(iamRole)) {
            // authenticate using IAM Role
            LOGGER.info(String.format("Fetching credentials using IAM Role: %s", iamRole));
            try {
                return awsMetadataApi.credentials(iamRole);
            } catch (Exception io) {
                throw new InvalidConfigurationException("Unable to retrieve credentials from IAM Role: " + awsConfig.getIamRole(),
                    io);
            }
        }

        // authenticate using ECS Endpoint
        return fetchCredentialsFromEcs();
    }

    private AwsCredentials fetchCredentialsFromEcs() {
        // before giving up, attempt to discover whether we're running in an ECS Container,
        // in which case, AWS_CONTAINER_CREDENTIALS_RELATIVE_URI will exist as an env var.
        String relativePath = getEnvironment().getEnvVar(Constants.ECS_CREDENTIALS_ENV_VAR_NAME);
        if (relativePath == null) {
            throw new InvalidConfigurationException("Could not acquire credentials! "
                + "Did not find declared AWS access key or IAM Role, and could not discover IAM Task Role or default role.");
        }
        try {
            return awsMetadataApi.credentialsFromEcs(relativePath);
        } catch (Exception io) {
            throw new InvalidConfigurationException(
                "Unable to retrieve credentials from IAM Task Role. " + "URI: " + relativePath);
        }
    }

    //Added for testing (mocking) purposes.
    Environment getEnvironment() {
        return new Environment();
    }

    private String resolveIamRole() {
        if (StringUtil.isNotEmpty(awsConfig.getIamRole()) && !"DEFAULT".equals(awsConfig.getIamRole())) {
            return awsConfig.getIamRole();
        }
        return awsMetadataApi.defaultIamRole();
    }

    String getAvailabilityZone() {
        return awsMetadataApi.availabilityZone();
    }

}
