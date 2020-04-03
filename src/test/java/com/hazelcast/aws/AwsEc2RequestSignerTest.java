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

import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.hazelcast.aws.AwsEc2RequestSigner.SIGNATURE_METHOD_V4;
import static org.junit.Assert.assertEquals;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class AwsEc2RequestSignerTest {

    private final static String TEST_REGION = "eu-central-1";
    private final static String TEST_HOST = "ec2.eu-central-1.amazonaws.com";
    private final static String TEST_SERVICE = "ec2";
    private final static String TEST_ACCESS_KEY = "AKIDEXAMPLE";
    private final static String TEST_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private final static String TEST_REQUEST_DATE = "20141106T111126Z";
    private final static String TEST_DERIVED_EXPECTED = "7038265e40236063ebcd2e201908ad6e9f64e533439bfa7a5faa07ba419329bc";
    private final static String TEST_SIGNATURE_EXPECTED = "79f7a4d346ee69ca22ba5f9bc3dd1efc13ac7509936afc5ec21cac37de071eef";

    @Test
    public void deriveSigningKeyTest()
        throws Exception {
        // this is from http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html
        AwsCredentials credentials = AwsCredentials.builder()
            .setAccessKey(TEST_ACCESS_KEY)
            .setSecretKey(TEST_SECRET_KEY)
            .build();

        // Override private method
        AwsEc2RequestSigner rs = new AwsEc2RequestSigner();

        Method method = rs.getClass().getDeclaredMethod("deriveSigningKey", null);
        method.setAccessible(true);
        byte[] derivedKey = (byte[]) method.invoke(rs);

        assertEquals(TEST_DERIVED_EXPECTED, bytesToHex(derivedKey));
    }

    @Test
    public void testSigning()
        throws NoSuchFieldException, IllegalAccessException, IOException {
        AwsConfig awsConfig = AwsConfig.builder()
            .setRegion(TEST_REGION)
            .setHostHeader(TEST_HOST).build();
        AwsCredentials credentials = AwsCredentials.builder()
            .setAccessKey(TEST_ACCESS_KEY)
            .setSecretKey(TEST_SECRET_KEY)
            .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);
        attributes.put("Action", "DescribeInstances");
        attributes.put("Version", "2016-11-15");
        attributes.put("X-Amz-Algorithm", SIGNATURE_METHOD_V4);
        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);
        attributes.put("X-Amz-SignedHeaders", "host");
        attributes.put("X-Amz-Expires", "30");
        Filter filter = new Filter();
        filter.addFilter("instance-state-name", "running");
        attributes.putAll(filter.getFilters());

        AwsEc2RequestSigner actual = new AwsEc2RequestSigner();
//        attributes.put("X-Amz-Credential", createFormattedCredential(credentials, TEST_REQUEST_DATE, TEST_REGION));
        String signature = actual.sign(attributes, TEST_REGION, TEST_HOST, credentials, TEST_REQUEST_DATE);

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
