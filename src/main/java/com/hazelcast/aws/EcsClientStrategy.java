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

import com.hazelcast.aws.impl.Constants;
import com.hazelcast.aws.impl.DescribeNetworkInterfacesOperation;
import com.hazelcast.aws.impl.DescribeTasksOperation;
import com.hazelcast.aws.impl.Ec2OperationClient;
import com.hazelcast.aws.impl.EcsOperationClient;
import com.hazelcast.aws.impl.ListTasksOperation;
import com.hazelcast.aws.utility.Environment;
import com.hazelcast.aws.utility.MetadataUtils;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.AWS_EXECUTION_ENV_VAR_NAME;
import static com.hazelcast.aws.impl.Constants.EC2_PREFIX;
import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;
import static java.lang.String.format;

/**
 * Strategy for discovery of Hazelcast instances running under ECS / Fargate
 */
class EcsClientStrategy extends AwsClientStrategy {

    private static final String UPPER_ECS = "ECS";

    private static final ILogger LOGGER = Logger.getLogger(EcsClientStrategy.class);

    private final String endpointDomain;

    EcsClientStrategy(AwsConfig awsConfig, String endpoint) {
        super(awsConfig, endpoint);
        this.endpointDomain = endpoint.substring(Constants.HOSTNAME_PREFIX_LENGTH);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Map<String, String> getAddresses() throws Exception {
        Map<String, String> metadata = retrieveAndParseMetadata();
        String metadataClusterName = metadata.get("clusterName");
        String metadataFamilyName = metadata.get("familyName");
        EcsOperationClient ecsOperationClient = new EcsOperationClient(awsConfig, endpoint);
        Ec2OperationClient ec2OperationClient = new Ec2OperationClient(awsConfig, EC2_PREFIX + endpointDomain);
        Collection<String> taskArns = ecsOperationClient.execute(new ListTasksOperation(metadataClusterName, metadataFamilyName));
        if (!taskArns.isEmpty()) {
            Collection<String> taskAddresses = new EcsOperationClient(awsConfig, endpoint).execute(
                    new DescribeTasksOperation(taskArns, metadataClusterName));
            ec2OperationClient.synchronizeCredentials(ecsOperationClient);
            Map<String, String> privateAndPublicAddresses = ec2OperationClient.execute(
                    new DescribeNetworkInterfacesOperation(taskAddresses));
            LOGGER.fine(format("The following (private, public) addresses found: %s", privateAndPublicAddresses));
            return privateAndPublicAddresses;
        }
        return Collections.EMPTY_MAP;
    }

    private Map<String, String> retrieveAndParseMetadata() {
        if (runningOnEcs()) {
            return MetadataUtils.retrieveContainerMetadataFromEnv(
                    getEnvironment(),
                    awsConfig.getConnectionTimeoutSeconds(),
                    awsConfig.getConnectionRetries());
        }

        return Collections.emptyMap();
    }


    @Override
    public String getAvailabilityZone() {
        return UPPER_ECS;
    }

    private boolean runningOnEcs() {
        String execEnv = getEnvironment().getEnvVar(AWS_EXECUTION_ENV_VAR_NAME);
        return isNotEmpty(execEnv) && execEnv.contains(UPPER_ECS);
    }

    private Environment getEnvironment() {
        return new Environment();
    }
}
