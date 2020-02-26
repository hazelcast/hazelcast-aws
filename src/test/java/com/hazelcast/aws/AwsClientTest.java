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

import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class AwsClientTest {

    public static final String DEFAULT_REGION = "us-east-1";
    public static final String EC2_HOST_HEADER = "ec2.amazonaws.com";
    public static final String ECS_HOST_HEADER = "ecs.amazonaws.com";

    private static AwsConfig.Builder predefinedEc2ConfigBuilder() {
        return AwsConfig.builder().setHostHeader(EC2_HOST_HEADER).setRegion(DEFAULT_REGION).setConnectionTimeoutSeconds(5);
    }

    private static AwsConfig.Builder predefinedEcsConfigBuilder() {
        return AwsConfig.builder().setHostHeader(ECS_HOST_HEADER).setRegion(DEFAULT_REGION).setConnectionTimeoutSeconds(5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAwsClient_whenNoAwsConfig() {
        new AwsClient(null);
    }

    @Test
    public void testAwsClient_getEndPoint() {
        AwsConfig awsConfig = predefinedEc2ConfigBuilder().setIamRole("test").build();
        AwsClient awsClient = new AwsClient(awsConfig);
        assertEquals("ec2.us-east-1.amazonaws.com", awsClient.getEndpoint());
    }

    @Test
    public void testAwsClient_withDifferentHostHeader() {
        AwsConfig awsConfig = predefinedEc2ConfigBuilder().setIamRole("test")
          .setHostHeader("ec2.amazonaws.com.cn")
          .setRegion("cn-north-1").build();
        AwsClient awsClient = new AwsClient(awsConfig);
        assertEquals("ec2.cn-north-1.amazonaws.com.cn", awsClient.getEndpoint());
    }

    @Test(expected = InvalidConfigurationException.class)
    public void testAwsClient_withInvalidHostHeader() {
        AwsConfig awsConfig = predefinedEc2ConfigBuilder().setIamRole("test").setHostHeader("ec3.amazonaws.com.cn").build();
        new AwsClient(awsConfig);
    }

    @Test
    public void testEcsAwsClient_getEndPoint() {
        AwsConfig awsConfig = predefinedEcsConfigBuilder().setIamRole("test").build();
        AwsClient awsClient = new AwsClient(awsConfig);
        assertEquals("ecs.us-east-1.amazonaws.com", awsClient.getEndpoint());
    }

    @Test
    public void testEcsAwsClient_withDifferentHostHeader() {
        AwsConfig awsConfig = predefinedEcsConfigBuilder().setIamRole("test").setHostHeader("ecs.amazonaws.com.cn")
          .setRegion("cn-north-1").build();
        AwsClient awsClient = new AwsClient(awsConfig);
        assertEquals("ecs.cn-north-1.amazonaws.com.cn", awsClient.getEndpoint());
    }

    @Test(expected = InvalidConfigurationException.class)
    public void testEcsAwsClient_withInvalidHostHeader() {
        AwsConfig awsConfig = predefinedEcsConfigBuilder().setIamRole("test").setHostHeader("eks.amazonaws.com.cn").build();
        new AwsClient(awsConfig);
    }
}
