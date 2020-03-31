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

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(HazelcastParallelClassRunner.class)
@Category( {QuickTest.class, ParallelJVMTest.class})
public class AwsClientTest {

//    @Test
//    public void useCurrentRegion() {
//        // given
//        AwsDiscoveryStrategy awsDiscoveryStrategy = spy(new AwsDiscoveryStrategy(Collections.emptyMap(), null, mockClient));
//        doReturn("us-east-1").when(awsDiscoveryStrategy).getCurrentRegion(10, 3, 10);
//        // when
//        AwsConfig awsConfig = awsDiscoveryStrategy.getAwsConfig();
//
//        // then
//        assertEquals("us-east-1", awsConfig.getRegion());
//    }

//    @Test
//    public void validateValidRegion() {
//        awsDiscoveryStrategy.validateRegion("us-west-1");
//        awsDiscoveryStrategy.validateRegion("us-gov-east-1");
//    }
//
//    @Test
//    public void validateInvalidRegion() {
//        // given
//        String region = "us-wrong-1";
//        String expectedMessage = String.format("The provided region %s is not a valid AWS region.", region);
//
//        //when
//        Runnable validateRegion = () -> awsDiscoveryStrategy.validateRegion(region);
//
//        //then
//        InvalidConfigurationException thrownEx = assertThrows(InvalidConfigurationException.class, validateRegion);
//        assertEquals(expectedMessage, thrownEx.getMessage());
//    }
//
//    @Test
//    public void validateInvalidGovRegion() {
//        // given
//        String region = "us-gov-wrong-1";
//        String expectedMessage = String.format("The provided region %s is not a valid AWS region.", region);
//
//        // when
//        Runnable validateRegion = () -> awsDiscoveryStrategy.validateRegion(region);
//
//        //then
//        InvalidConfigurationException thrownEx = assertThrows(InvalidConfigurationException.class, validateRegion);
//        assertEquals(expectedMessage, thrownEx.getMessage());
//    }

    @Test
    public void resolveEndpointEmpty() {
        // given
        AwsConfig awsConfig = AwsConfig.builder()
            .setHostHeader("ec2.amazonaws.com")
            .setRegion("us-east-1").build();

        // when
        String endpoint = new AwsClient(null, null, awsConfig).resolveEndpoint();

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
        new AwsClient(null, null, awsConfig).resolveEndpoint();

        // then
        // throws exception
    }

//    @Test
//    public void resolveEndpointEmptyRegion() {
//        // given
//        String hostHeader = "ec2.amazonaws";
//        AwsConfig awsConfig = AwsConfig.builder()
//            .setHostHeader(hostHeader)
//            .setRegion(null).build();
//
//        // when
//        String endpoint = new AwsClient(null, awsConfig).resolveEndpoint();
//
//        // then
//        assertEquals(hostHeader, endpoint);
//    }
}
