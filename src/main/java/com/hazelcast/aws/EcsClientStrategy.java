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

import com.hazelcast.aws.impl.Constants;
import com.hazelcast.aws.impl.DescribeNetworkInterfaces;
import com.hazelcast.aws.impl.DescribeTasks;
import com.hazelcast.aws.impl.ListTasks;
import com.hazelcast.aws.utility.Environment;
import com.hazelcast.aws.utility.MetadataUtils;
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.AWS_EXECUTION_ENV_VAR_NAME;
import static com.hazelcast.aws.impl.Constants.EC2_PREFIX;
import static com.hazelcast.aws.impl.Constants.HTTPS;
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
        ListTasks listTasks = new ListTasks(awsConfig, new URL(HTTPS, endpoint, -1, "/"));
        Collection<String> taskArns = listTasks.execute(metadataClusterName, metadataFamilyName);
        if (!taskArns.isEmpty()) {
            DescribeTasks describeTasks = new DescribeTasks(awsConfig, new URL(HTTPS, endpoint, -1, "/"));
            Collection<String> taskAddresses = describeTasks.execute(taskArns, metadataClusterName, metadataFamilyName);
            return taskAddresses;
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Map<String, String> getAddresses() throws Exception {
        retrieveAndParseMetadata();
        ListTasks listTasks = new ListTasks(awsConfig, new URL(HTTPS, endpoint, -1, "/"));
        Collection<String> taskArns = listTasks.execute(metadataClusterName, metadataFamilyName);
        if (!taskArns.isEmpty()) {
            DescribeTasks describeTasks = new DescribeTasks(awsConfig, new URL(HTTPS, endpoint, -1, "/"));
            Collection<String> taskAddresses = describeTasks.execute(taskArns, metadataClusterName, metadataFamilyName);
            DescribeNetworkInterfaces describeNetworkInterfaces =
                    new DescribeNetworkInterfaces(awsConfig, new URL(HTTPS, EC2_PREFIX + endpointDomain, -1, "/"));
            Map<String, String> privateAndPublicAddresses = describeNetworkInterfaces.execute(taskAddresses);
            LOGGER.fine(String.format("Found privateAndPublicAddresses: %s", privateAndPublicAddresses));
            return privateAndPublicAddresses;
        }
        return Collections.EMPTY_MAP;
    }

    private void retrieveAndParseMetadata() {
        if (runningOnEcs()) {
            String json = MetadataUtils.retrieveContainerMetadata(awsConfig.getConnectionTimeoutSeconds(),
                    awsConfig.getConnectionRetries());
            parseContainerMetadata(json);
        }
    }

    private void parseContainerMetadata(String json) {
        JsonObject containerAsJson = Json.parse(json).asObject();
        JsonObject labels = containerAsJson.get("Labels").asObject();
        metadataClusterName = labels.getString("com.amazonaws.ecs.cluster", null);
        metadataFamilyName = labels.getString("com.amazonaws.ecs.task-definition-family", null);
    }

    @Override
    public String getAvailabilityZone() {
        return UPPER_ECS;
    }

    private boolean runningOnEcs() {
        String execEnv = new Environment().getEnvVar(AWS_EXECUTION_ENV_VAR_NAME);
        return isNotEmpty(execEnv) && execEnv.contains(UPPER_ECS);
    }
}
