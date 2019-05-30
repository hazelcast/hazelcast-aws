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

package com.hazelcast.aws;

import com.hazelcast.aws.impl.Constants;
import com.hazelcast.aws.impl.DescribeNetworkInterfacesRequest;
import com.hazelcast.aws.impl.DescribeTasksRequest;
import com.hazelcast.aws.impl.Ec2OperationClient;
import com.hazelcast.aws.impl.EcsOperationClient;
import com.hazelcast.aws.impl.ListTasksRequest;
import com.hazelcast.aws.utility.Environment;
import com.hazelcast.aws.utility.MetadataUtils;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.AWS_EXECUTION_ENV_VAR_NAME;
import static com.hazelcast.aws.impl.Constants.EC2_PREFIX;
import static com.hazelcast.aws.utility.StringUtils.isNotEmpty;

/**
 *
 */
class EcsClientStrategy extends AwsClientStrategy {

    private static final String UPPER_ECS = "ECS";

    private static final ILogger LOGGER = Logger.getLogger(AwsClientStrategy.class);

    private String metadataClusterName;
    private String metadataFamilyName;
    private String endpointDomain;

    EcsClientStrategy(AwsConfig awsConfig, String endpoint) {
        super(awsConfig, endpoint);
        this.endpointDomain = endpoint.substring(Constants.HOSTNAME_PREFIX_LENGTH);
    }

    @Override
    public Collection<String> getPrivateIpAddresses() throws Exception {
        retrieveAndParseMetadata();
        EcsOperationClient listTasks = new EcsOperationClient(awsConfig, endpoint);
        Collection<String> taskArns = listTasks.execute(new ListTasksRequest(metadataClusterName, metadataFamilyName));
        if (!taskArns.isEmpty()) {
            EcsOperationClient describeTasks = new EcsOperationClient(awsConfig, endpoint);
            Collection<String> taskAddresses =
                    describeTasks.execute(new DescribeTasksRequest(taskArns, metadataClusterName));
            return taskAddresses;
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Map<String, String> getAddresses() throws Exception {
        retrieveAndParseMetadata();
        EcsOperationClient listTasks = new EcsOperationClient(awsConfig, endpoint);
        Collection<String> taskArns = listTasks.execute(new ListTasksRequest(metadataClusterName, metadataFamilyName));
        if (!taskArns.isEmpty()) {
            Collection<String> taskAddresses = new EcsOperationClient(awsConfig, endpoint)
                    .execute(new DescribeTasksRequest(taskArns, metadataClusterName));
            Ec2OperationClient describeNetworkInterfaces =
                    new Ec2OperationClient(awsConfig, EC2_PREFIX + endpointDomain);
            Map<String, String> privateAndPublicAddresses =
                    describeNetworkInterfaces.execute(new DescribeNetworkInterfacesRequest(taskAddresses));
            LOGGER.fine(String.format("Found privateAndPublicAddresses: %s", privateAndPublicAddresses));
            return privateAndPublicAddresses;
        }
        return Collections.EMPTY_MAP;
    }

    private void retrieveAndParseMetadata() {
        if (runningOnEcs()) {
            Map<String, String> metadata = MetadataUtils.retrieveContainerMetadataFromEnv(
                    getEnvironment(),
                    awsConfig.getConnectionTimeoutSeconds(),
                    awsConfig.getConnectionRetries());
            metadataClusterName = metadata.get("clusterName");
            metadataFamilyName = metadata.get("familyName");
        }
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
