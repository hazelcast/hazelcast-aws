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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class AwsEc2ClientTest {
//    private static final String ECS_CREDENTIALS_ENV_VAR_NAME = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
//    private static final String REGION = "us-east-1";
//    private static final String ENDPOINT = "ec2.us-east-1.amazonaws.com";
//    private static final AwsCredentials CREDENTIALS =
//        AwsCredentials.builder()
//            .setAccessKey("access-key")
//            .setSecretKey("secret-key")
//            .build();
//    private static final String IAM_ROLE = "iam-role";
//    private static final Map<String, String> ADDRESSES = Collections.singletonMap("192.168.1.2", "12.56.345.2");
//
//    @Mock
//    private AwsMetadataApi awsMetadataApi;
//
//    @Mock
//    private AwsEc2Api awsEc2Api;
//
//    @Mock
//    private Environment environment;
//
//    @Before
//    public void setUp() {
//        given(awsEc2Api.describeInstances(CREDENTIALS)).willReturn(ADDRESSES);
//        given(awsMetadataApi.credentialsEc2(IAM_ROLE)).willReturn(CREDENTIALS);
//    }
//
//    @Test(expected = InvalidConfigurationException.class)
//    public void newInvalidRegion() {
//        // given
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setRegion("invalid-region")
//            .build();
//
//        // when
////        new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // then
//        // throws exception
//    }
//
//    @Test(expected = InvalidConfigurationException.class)
//    public void newInvalidHostHeader() {
//        // given
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setHostHeader("invalid-host-header")
//            .build();
//
//        // when
//        new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // then
//        // throws exception
//    }
//
//    @Test
//    public void getAddresses() {
//        // given
//        AwsConfig awsConfig = predefinedAwsConfig().build();
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // when
//        Map<String, String> result = awsClient.getAddresses();
//
//        // then
//        assertEquals(ADDRESSES, result);
//    }
//
//    @Test
//    public void getAddressesNoRegionConfigured() {
//        // given
//        given(awsMetadataApi.availabilityZoneEc2()).willReturn("us-east-1a");
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setRegion("")
//            .build();
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // when
//        Map<String, String> result = awsClient.getAddresses();
//
//        // then
//        assertEquals(ADDRESSES, result);
//    }
//
//    @Test
//    public void getAddressesNoAccessKey() {
//        // given
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setAccessKey("")
//            .setSecretKey("")
//            .setIamRole(IAM_ROLE)
//            .build();
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // when
//        Map<String, String> result = awsClient.getAddresses();
//
//        // then
//        assertEquals(ADDRESSES, result);
//    }
//
//    @Test
//    public void getAddressesNoAccessKeyNoIamRole() {
//        // given
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setAccessKey("")
//            .setSecretKey("")
//            .setIamRole("")
//            .build();
//        given(awsMetadataApi.defaultIamRoleEc2()).willReturn(IAM_ROLE);
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // when
//        Map<String, String> result = awsClient.getAddresses();
//
//        // then
//        assertEquals(ADDRESSES, result);
//    }
//
//    @Test(expected = InvalidConfigurationException.class)
//    public void getAddressesInvalidIamRole() {
//        // given
//        String iamRole = "invalid-iam-role";
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setAccessKey("")
//            .setSecretKey("")
//            .setIamRole(iamRole)
//            .build();
//        given(awsMetadataApi.credentialsEc2(iamRole)).willThrow(new RuntimeException("Invalid IAM Role"));
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // when
//        awsClient.getAddresses();
//
//        // then
//        // throws exception
//    }
//
//    @Test
//    public void getAddressesEcs() {
//        // given
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setAccessKey("")
//            .setSecretKey("")
//            .setIamRole("")
//            .build();
//        String relativePath = "/some/relative/path";
//        given(environment.getEnv(ECS_CREDENTIALS_ENV_VAR_NAME)).willReturn(relativePath);
//        given(awsMetadataApi.credentialsEcs(relativePath)).willReturn(CREDENTIALS);
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // when
//        Map<String, String> result = awsClient.getAddresses();
//
//        // then
//        assertEquals(ADDRESSES, result);
//    }
//
//    @Test(expected = InvalidConfigurationException.class)
//    public void getAddressesEcsInvalidRelativePath() {
//        // given
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setAccessKey("")
//            .setSecretKey("")
//            .setIamRole("")
//            .build();
//        String invalidPath = "/some/relative/path";
//        given(environment.getEnv(ECS_CREDENTIALS_ENV_VAR_NAME)).willReturn(invalidPath);
//        given(awsMetadataApi.credentialsEcs(invalidPath)).willThrow(new RuntimeException("Invalid ECS Metadata"));
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // when
//        awsClient.getAddresses();
//
//        // then
//        // throws exception
//
//    }
//
//    @Test(expected = InvalidConfigurationException.class)
//    public void getAddressesNoCorrectConfiguration() {
//        // given
//        AwsConfig awsConfig = predefinedAwsConfig()
//            .setAccessKey("")
//            .setSecretKey("")
//            .setIamRole("")
//            .build();
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, awsConfig, environment);
//
//        // when
//        awsClient.getAddresses();
//
//        // then
//        // throws exception
//    }
//
//    @Test
//    public void getAvailabilityZone() {
//        // given
//        String availabilityZoneEc2 = "us-east-1a";
//        given(awsMetadataApi.availabilityZoneEc2()).willReturn(availabilityZoneEc2);
//        AwsClient awsClient = new AwsEc2Client(awsMetadataApi, awsEc2Api, predefinedAwsConfig().build(),
//            environment);
//
//        // when
//        String result = awsClient.getAvailabilityZone();
//
//        // then
//        assertEquals(availabilityZoneEc2, result);
//    }
//
//    private static AwsConfig.Builder predefinedAwsConfig() {
//        return AwsConfig.builder()
//            .setHostHeader("ec2.amazonaws.com")
//            .setAccessKey("access-key")
//            .setSecretKey("secret-key")
//            .setRegion("us-east-1");
//    }
}
