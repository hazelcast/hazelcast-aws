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

import java.util.Map;

import static com.hazelcast.aws.impl.Constants.ECS_PREFIX;

abstract class AwsClientStrategy {

    protected final AwsConfig awsConfig;
    protected final String endpoint;

    AwsClientStrategy(AwsConfig awsConfig, String endpoint) {
        this.awsConfig = awsConfig;
        this.endpoint = endpoint;
    }

    /**
     * Static factory method returning a new AWS client strategy depending on the configured endpoint.
     * It will return EcsClientStrategy or Ec2ClientStrategy.
     * @param awsConfig configuration
     * @param endpoint endpoint
     * @return the appropriate AWS client strategy (EcsClientStrategy or Ec2ClientStrategy)
     */
    static AwsClientStrategy create(AwsConfig awsConfig, String endpoint) {
        return endpoint.toLowerCase().startsWith(ECS_PREFIX)
          ? new EcsClientStrategy(awsConfig, endpoint)
          : new Ec2ClientStrategy(awsConfig, endpoint);
    }

    abstract Map<String, String> getAddresses() throws Exception;

    abstract String getAvailabilityZone();
}
