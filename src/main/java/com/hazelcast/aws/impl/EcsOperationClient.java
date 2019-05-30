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

import java.net.MalformedURLException;
import java.net.URL;

import static com.hazelcast.aws.impl.Constants.ECS;
import static com.hazelcast.aws.impl.Constants.HTTPS;
import static com.hazelcast.aws.impl.Constants.POST;

/**
 * AWS ECS service client.
 * Used by EcsClientStrategy for calling ECS service endpoints.
 */
public class EcsOperationClient extends AwsOperationClient {

    public EcsOperationClient(AwsConfig awsConfig, String endpoint) throws MalformedURLException {
        this(awsConfig, new URL(HTTPS, endpoint, -1, "/"));
    }

    // Visible for testing
    EcsOperationClient(AwsConfig awsConfig, URL endpointURL) {
        super(awsConfig, endpointURL, ECS, POST);
    }

    @Override
    protected void retrieveCredentials() {
        retrieveContainerCredentials();
    }
}
