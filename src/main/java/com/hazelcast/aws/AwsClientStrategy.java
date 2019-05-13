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

import java.util.Collection;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.ECS_PREFIX;

/**
 * Abstract AWS client strategy
 */
abstract class AwsClientStrategy {

    final AwsConfig awsConfig;
    final String endpoint;

    AwsClientStrategy(AwsConfig awsConfig, String endpoint) {
        this.awsConfig = awsConfig;
        this.endpoint = endpoint;
    }

    /**
     * Creates an AWS client strategy
     * @param awsConfig configuration
     * @param endpoint endpoint
     * @return the appropriate AWS client strategy
     */
    static AwsClientStrategy create(AwsConfig awsConfig, String endpoint) {
        if (endpoint.toLowerCase().startsWith(ECS_PREFIX)) {
            return new EcsClientStrategy(awsConfig, endpoint);
        } else {
            return new Ec2ClientStrategy(awsConfig, endpoint);
        }
    }

    abstract Collection<String> getPrivateIpAddresses() throws Exception;

    abstract Map<String, String> getAddresses() throws Exception;

    abstract String getAvailabilityZone();
}
