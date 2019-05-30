/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.utility.MetadataUtils;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.GET;
import static com.hazelcast.aws.impl.Constants.HTTPS;
import static com.hazelcast.aws.utility.StringUtils.isEmpty;
import static com.hazelcast.aws.utility.StringUtils.isNotEmpty;

/**
 * AWS EC2 service client.
 * Used by AwsClientStrategy implementations for calling EC2 service endpoints.
 */
public class Ec2OperationClient extends AwsOperationClient {

    private static final ILogger LOGGER = Logger.getLogger(Ec2OperationClient.class);

    public Ec2OperationClient(AwsConfig awsConfig, String endpoint) throws MalformedURLException {
        this(awsConfig, new URL(HTTPS, endpoint, -1, "/"));
    }

    // Visible for testing
    Ec2OperationClient(AwsConfig awsConfig, URL endpointURL) {
        super(awsConfig, endpointURL, Constants.EC2, GET);
    }

    @Override
    InputStream callService(Map<String, String> attributes, Map<String, String> headers, String body) throws Exception {
        Map<String, String> enrichedAttributes = new HashMap<>();
        enrichedAttributes.putAll(attributes);
        enrichedAttributes.put("Version", Constants.EC2_DOC_VERSION);
        return super.callService(enrichedAttributes, headers, body);
    }

    @Override
    protected void retrieveCredentials() {
        retrieveIamRole();
        if (isNotEmpty(getAwsCredentials().getIamRole())) {
            parseAndStoreRoleCreds(retrieveIamRoleCredentials());
        } else {
            // legacy ECS mode
            retrieveContainerCredentials();
        }
    }

    // Visible for testing
    String retrieveIamRoleCredentials() {
        return MetadataUtils.retrieveIamRoleCredentials(
                getAwsCredentials().getIamRole(),
                getAwsConfig().getConnectionTimeoutSeconds(),
                getAwsConfig().getConnectionRetries());
    }

    // Visible for testing
    String getDefaultIamRole() {
        return MetadataUtils.getDefaultIamRole(
                getAwsConfig().getConnectionTimeoutSeconds(),
                getAwsConfig().getConnectionRetries());
    }

    private void retrieveIamRole() {
        if (isEmpty(getAwsCredentials().getIamRole()) || "DEFAULT".equals(getAwsCredentials().getIamRole())) {
            String defaultIAMRole = null;
            try {
                defaultIAMRole = getDefaultIamRole();
            } catch (Throwable e) {
                LOGGER.finest("Cannot get DEFAULT IAM role. CONTINUING. Exception was: " + e.getMessage());
            }
            getAwsCredentials().setIamRole(defaultIAMRole);
        }
    }
}
