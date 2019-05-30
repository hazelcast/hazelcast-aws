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

import com.hazelcast.aws.AwsRequest;
import com.hazelcast.aws.utility.MarshallingUtils;
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonArray;
import com.hazelcast.internal.json.JsonObject;

import java.util.Collection;

/**
 * ECS DescribeTasks operation request.
 * See <a href="https://docs.aws.amazon.com/AmazonECS/latest/APIReference/API_DescribeTasks.html">EC2 documentation</a>.
 */
public class DescribeTasksRequest extends AwsRequest<Collection<String>> {

    public DescribeTasksRequest() {
        this(null, null);
    }

    public DescribeTasksRequest(Collection<String> taskArns, String clusterName) {
        super(MarshallingUtils::unmarshalDescribeTasksResponse);

        getHeaders().put("X-Amz-Target", "AmazonEC2ContainerServiceV20141113.DescribeTasks");
        getHeaders().put("Content-Type", "application/x-amz-json-1.1");
        getHeaders().put("Accept-Encoding", "identity");

        JsonObject body = new JsonObject();

        if (clusterName != null) {
            body.add("cluster", clusterName);
        }

        JsonArray jsonArray = new JsonArray();
        if (taskArns != null && taskArns.size() > 0) {
            for (Object arg : taskArns) {
                jsonArray.add(Json.value(String.valueOf(arg)));
            }
        }
        body.add("tasks", jsonArray);

        setBody(body.toString());
    }
}
