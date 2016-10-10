/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.aws.utility.Environment;
import com.hazelcast.config.AwsConfig;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class DescribeInstancesTest {

    @Test(expected = IllegalArgumentException.class)
    public void test_whenAwsConfigIsNull() throws IOException {
        new DescribeInstances(null, "endpoint");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_whenAccessKey_And_IamRole_And_IamTaskRoleEnvVar_Null() throws IOException {
        Environment mockedEnv = mock(Environment.class);
        when(mockedEnv.getEnvVar(Constants.ECS_CREDENTIALS_ENV_VAR_NAME)).thenReturn(null);

        DescribeInstances descriptor = new DescribeInstances(new AwsConfig());
        descriptor.checkKeysFromIamRoles(mockedEnv);
    }

    @Test
    public void test_whenAccessKeyExistsInConfig() throws IOException {
        AwsConfig awsConfig = new AwsConfig();
        awsConfig.setAccessKey("accesskey");
        awsConfig.setSecretKey("secretkey");
        new DescribeInstances(awsConfig, "endpoint");
    }
}
