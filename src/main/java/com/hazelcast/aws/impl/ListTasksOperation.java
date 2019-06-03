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

import com.hazelcast.aws.AwsOperation;
import com.hazelcast.aws.utility.MarshallingUtils;
import com.hazelcast.internal.json.JsonObject;

import java.util.Collection;

/**
 * ECS ListTasks operation request.
 * See <a href="https://docs.aws.amazon.com/AmazonECS/latest/APIReference/API_ListTasks.html">EC2 documentation</a>.
 */
public class ListTasksOperation extends AwsOperation<Collection<String>> {

    public ListTasksOperation() {
        this(null, null);
    }

    public ListTasksOperation(String clusterName, String familyName) {
        super(MarshallingUtils::unmarshalListTasksResponse);

        getHeaders().put("X-Amz-Target", "AmazonEC2ContainerServiceV20141113.ListTasks");
        getHeaders().put("Content-Type", "application/x-amz-json-1.1");
        getHeaders().put("Accept-Encoding", "identity");

        JsonObject body = new JsonObject();

        if (clusterName != null) {
            body.add("cluster", clusterName);
        }

        if (familyName != null) {
            body.add("family", familyName);
        }

        setBody(body.toString());
    }
}
