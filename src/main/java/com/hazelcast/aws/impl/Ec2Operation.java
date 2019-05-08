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

package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.utility.MetadataUtil;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.IOException;
import java.net.URL;

import static com.hazelcast.aws.utility.StringUtil.isEmpty;
import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;

/**
 *
 */
public abstract class Ec2Operation<E> extends AwsOperation<E> {

    private static final ILogger LOGGER = Logger.getLogger(Ec2Operation.class);

    protected Ec2Operation(AwsConfig awsConfig, URL endpointURL, String service, String docVersion, String httpMethod) {
        super(awsConfig, endpointURL, service, docVersion, httpMethod);
    }

    @Override
    protected void retrieveCredentials() throws IOException {
        retrieveIamRole();
        if (isNotEmpty(awsCredentials.getIamRole())) {
            parseAndStoreRoleCreds(retrieveIamRoleCredentials());
        } else {
            // legacy ECS mode
            retrieveContainerCredentials(getEnvironment());
        }
    }

    // Visible for testing
    protected String retrieveIamRoleCredentials() {
        return MetadataUtil.retrieveIamRoleCredentials(
                awsCredentials.getIamRole(), awsConfig.getConnectionTimeoutSeconds(), awsConfig.getConnectionRetries());
    }

    // Visible for testing
    protected String getDefaultIamRole() {
        return MetadataUtil.getDefaultIamRole(awsConfig.getConnectionTimeoutSeconds(), awsConfig.getConnectionRetries());
    }

    private void retrieveIamRole() {
        if (isEmpty(awsCredentials.getIamRole()) || "DEFAULT".equals(awsCredentials.getIamRole())) {
            String defaultIAMRole = null;
            try {
                defaultIAMRole = getDefaultIamRole();
            } catch (Throwable e) {
                LOGGER.finest("Cannot get DEFAULT IAM role. CONTINUING. Exception was: " + e.getMessage());
            }
            awsCredentials.setIamRole(defaultIAMRole);
        }
    }

}
