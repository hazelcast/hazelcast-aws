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

import com.hazelcast.aws.impl.DescribeTasks;
import com.hazelcast.aws.impl.ListTasks;
import com.hazelcast.aws.utility.Environment;
import com.hazelcast.aws.utility.MetadataUtil;
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonObject;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;

/**
 *
 */
class EcsClientStrategy extends AwsClientStrategy {

    private String metadataClusterName;
    private String metadataFamilyName;

    EcsClientStrategy(AwsConfig awsConfig, String endpoint) {
        super(awsConfig, endpoint);
    }

    @Override
    public Collection<String> getPrivateIpAddresses() throws Exception {
        return getAddresses().keySet();
    }

    @Override
    public Map<String, String> getAddresses() throws Exception {
        // FIXME taskMetadata URL
        retrieveAndParseMetadata();
        ListTasks listTasks = new ListTasks(awsConfig, new URL("https", endpoint, -1, "/"));
        Collection<String> taskArns = listTasks.execute(metadataClusterName, metadataFamilyName);
        if (!taskArns.isEmpty()) {
            DescribeTasks describeTasks = new DescribeTasks(awsConfig, new URL("https", endpoint, -1, "/"));
            return describeTasks.execute(taskArns, metadataClusterName, metadataFamilyName);
        }
        return Collections.EMPTY_MAP;
    }

    private void retrieveAndParseMetadata() {
        if (runningOnEcs()) {
            String json = MetadataUtil.retrieveContainerMetadata(awsConfig.getConnectionTimeoutSeconds(),
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

    private boolean runningOnEcs() {
        String execEnv = new Environment().getEnvVar(AWS_EXECUTION_ENV_VAR_NAME);
        return isNotEmpty(execEnv) && execEnv.contains(ECS);
    }

    @Override
    public String getAvailabilityZone() {
        return ECS;
    }
}
