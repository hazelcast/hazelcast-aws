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

import com.hazelcast.aws.impl.DescribeInstances;
import com.hazelcast.aws.utility.Environment;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.AWS_EXECUTION_ENV_VAR_NAME;
import static com.hazelcast.aws.impl.Constants.HTTPS;
import static com.hazelcast.aws.utility.MetadataUtils.getEc2AvailabilityZone;
import static com.hazelcast.aws.utility.StringUtils.isNotEmpty;

/**
 *
 */
class Ec2ClientStrategy extends AwsClientStrategy {

    private static final String UPPER_EC2 = "EC2";

    public Ec2ClientStrategy(AwsConfig awsConfig, String endpoint) {
        super(awsConfig, endpoint);
    }

    @Override
    public Collection<String> getPrivateIpAddresses() throws Exception {
        return getAddresses().keySet();
    }

    public Map<String, String> getAddresses() throws Exception {
        return new DescribeInstances(awsConfig, new URL(HTTPS, endpoint, -1, "/")).execute();
    }

    @Override
    public String getAvailabilityZone() {
        if (runningOnEc2()) {
            return getEc2AvailabilityZone(awsConfig.getConnectionTimeoutSeconds(), awsConfig.getConnectionRetries());
        }
        return UPPER_EC2;
    }

    private boolean runningOnEc2() {
        String execEnv = new Environment().getEnvVar(AWS_EXECUTION_ENV_VAR_NAME);
        return isNotEmpty(execEnv) && execEnv.contains(UPPER_EC2);
    }
}
