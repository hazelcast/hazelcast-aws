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

package com.hazelcast.aws.security;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.impl.Constants;
import com.hazelcast.aws.impl.Filter;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class Ec2RequestSignerTest {

    private final static String TEST_REGION = "eu-central-1";
    private final static String TEST_HOST = "ec2.eu-central-1.amazonaws.com";
    private final static String TEST_SERVICE = "ec2";
    private final static String TEST_ACCESS_KEY = "AKIDEXAMPLE";
    private final static String TEST_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private final static String TEST_REQUEST_DATE = "20141106T111126Z";
    private final static String TEST_DERIVED_EXPECTED = "7038265e40236063ebcd2e201908ad6e9f64e533439bfa7a5faa07ba419329bc";
    private final static String TEST_SIGNATURE_EXPECTED = "8e4f83fe919390f53fa71ea0ea8a25a09e7d10e1740b238fc6969a1410e06c57";

    @Test
    public void deriveSigningKeyTest()
            throws Exception {
        // this is from http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html
        AwsConfig awsConfig = AwsConfig.builder()
                .setRegion(TEST_REGION)
                .setHostHeader(TEST_HOST)
                .setAccessKey(TEST_ACCESS_KEY)
                .setSecretKey(TEST_SECRET_KEY)
                .build();
        AwsCredentials awsCredentials = new AwsCredentials(awsConfig);

        Aws4RequestSignerImpl rs = new Aws4RequestSignerImpl(awsConfig, awsCredentials, TEST_REQUEST_DATE, TEST_SERVICE, TEST_HOST);

        byte[] derivedKey = rs.deriveSigningKey();

        assertEquals(TEST_DERIVED_EXPECTED, bytesToHex(derivedKey));
    }

    @Test
    public void testSigning() {
        AwsConfig awsConfig = AwsConfig.builder()
                .setRegion(TEST_REGION)
                .setHostHeader(TEST_HOST)
                .setAccessKey(TEST_ACCESS_KEY)
                .setSecretKey(TEST_SECRET_KEY)
                .build();
        AwsCredentials awsCredentials = new AwsCredentials(awsConfig);

        Map<String, String> attributes = new HashMap<>();
        Filter filter = new Filter();
        filter.addFilter("instance-state-name", "running");
        attributes.putAll(filter.getFilters());
        attributes.put("Action", "DescribeInstances");
        attributes.put("Version", Constants.EC2_DOC_VERSION);

        Map<String, String> headers = new HashMap<>();
        headers.put("Host", TEST_HOST);
        headers.put("X-Amz-Date", TEST_REQUEST_DATE);

        Aws4RequestSigner actual =
                new Aws4RequestSignerImpl(awsConfig, awsCredentials, TEST_REQUEST_DATE, TEST_SERVICE, TEST_HOST);
        String signature = actual.sign(attributes, headers);

        assertEquals(TEST_SIGNATURE_EXPECTED, signature);
    }

    private String bytesToHex(byte[] in) {
        char[] hexArray = "0123456789abcdef".toCharArray();

        char[] hexChars = new char[in.length * 2];
        for (int j = 0; j < in.length; j++) {
            int v = in[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
