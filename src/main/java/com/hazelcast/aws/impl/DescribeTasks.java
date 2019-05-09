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
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonArray;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.internal.json.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.ECS;
import static com.hazelcast.aws.impl.Constants.ECS_DOC_VERSION;
import static com.hazelcast.aws.impl.Constants.POST;

/**
 *
 */
public class DescribeTasks extends EcsOperation<Map<String, String>> {
//    private Collection<String> taskArns;

    public DescribeTasks(AwsConfig awsConfig, URL endpointURL) {
        super(awsConfig, endpointURL, ECS, ECS_DOC_VERSION, POST);
    }

    public Map<String, String> execute(Collection<String> taskArns) throws Exception {
//        this.taskArns = taskArns;
        return super.execute(taskArns);
    }

    @Override
    protected void prepareHttpRequest(Object... args) {
        headers.put("X-Amz-Target", "AmazonEC2ContainerServiceV20141113.DescribeTasks");
        headers.put("Content-Type", "application/x-amz-json-1.1");
        headers.put("Accept-Encoding", "identity");
        JsonArray jsonArray = new JsonArray();
        if (args.length > 0) {
            Collection<String> taskArns = (Collection<String>) args[0];
//            TODO:
//              String clusterName = (String) args[1];
//              String familyName = (String) args[2];
            for (Object arg : taskArns) {
                jsonArray.add(Json.value(String.valueOf(arg)));
            }
        }
        body = new JsonObject().add("tasks", jsonArray).toString();
    }

    @Override
    Map<String, String> unmarshal(InputStream stream) {
        Map<String, String> response = new HashMap<String, String>();

        try {
            JsonArray jsonValues = Json.parse(new InputStreamReader(stream, UTF8_ENCODING)).asObject()
                    .get("tasks").asArray();
            for (JsonValue task : jsonValues) {
                for (JsonValue container : task.asObject().get("containers").asArray()) {
                    for (JsonValue intface : container.asObject().get("networkInterfaces").asArray()) {
                        String privateIpv4Address = intface.asObject().get("privateIpv4Address").asString();
                        response.put(privateIpv4Address, privateIpv4Address);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Malformed response", e);
        }

        return response;
    }
}
