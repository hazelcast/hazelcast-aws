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
import com.hazelcast.internal.json.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import static com.hazelcast.aws.impl.Constants.POST;

/**
 * ECS ListTasks operation.
 * See https://docs.aws.amazon.com/AmazonECS/latest/APIReference/API_ListTasks.html
 * for AWS API details.
 */
public class ListTasks extends EcsOperation<Collection<String>> {

    public ListTasks(AwsConfig awsConfig, URL endpointURL) {
        super(awsConfig, endpointURL, POST);
    }

    @Override
    protected void prepareHttpRequest(Object... args) {
        headers.put("X-Amz-Target", "AmazonEC2ContainerServiceV20141113.ListTasks");
        headers.put("Content-Type", "application/x-amz-json-1.1");
        headers.put("Accept-Encoding", "identity");
//        TODO:
//          String clusterName = (String) args[0];
//          String familyName = (String) args[1];
        body = "{}";
    }

    @Override
    Collection<String> unmarshal(InputStream stream) {
        ArrayList<String> response = new ArrayList<String>();

        try {
            JsonArray jsonValues = Json.parse(new InputStreamReader(stream, UTF8_ENCODING)).asObject().get("taskArns").asArray();
            for (JsonValue value : jsonValues) {
                response.add(value.asString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Malformed response", e);
        }

        return response;
    }
}
