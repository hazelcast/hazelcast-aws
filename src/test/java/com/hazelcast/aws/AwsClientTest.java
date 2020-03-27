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
@Category( {QuickTest.class, ParallelJVMTest.class})
public class AwsClientTest {

    private static AwsConfig.Builder predefinedAwsConfigBuilder() {
        return AwsConfig.builder().setHostHeader("ec2.amazonaws.com").setRegion("us-east-1").setConnectionTimeoutSeconds(5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAwsClient_whenNoAwsConfig() {
        new AwsClient(null, null);
    }

    @Test
    public void resolveEndpointEmpty() {
        // given
        AwsConfig awsConfig = AwsConfig.builder()
            .setHostHeader("ec2.amazonaws.com")
            .setRegion("us-east-1").build();

        // when
        String endpoint = AwsClient.resolveEndpoint(awsConfig);

        // then
        assertEquals("ec2.us-east-1.amazonaws.com", endpoint);
    }

    @Test(expected = InvalidConfigurationException.class)
    public void resolveEndpointInvalidHostHeader() {
        // given
        AwsConfig awsConfig = AwsConfig.builder()
            .setHostHeader("invalid.host.header")
            .setRegion("us-east-1").build();

        // when
        AwsClient.resolveEndpoint(awsConfig);

        // then
        // throws exception
    }

    @Test
    public void resolveEndpointEmptyRegion() {
        // given
        String hostHeader = "ec2.amazonaws";
        AwsConfig awsConfig = AwsConfig.builder()
            .setHostHeader(hostHeader)
            .setRegion(null).build();

        // when
        String endpoint = AwsClient.resolveEndpoint(awsConfig);

        // then
        assertEquals(hostHeader, endpoint);
    }
}
