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

package com.hazelcast.aws.security;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.impl.DescribeInstances;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import uk.co.lucasweb.aws.v4.signer.HttpRequest;
import uk.co.lucasweb.aws.v4.signer.Signer;
import uk.co.lucasweb.aws.v4.signer.credentials.AwsCredentials;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;
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
    private final static String TEST_SIGNATURE_EXPECTED = "79f7a4d346ee69ca22ba5f9bc3dd1efc13ac7509936afc5ec21cac37de071eef";

    @Test
    public void deriveSigningKeyTest()
            throws Exception {
        // this is from http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html
        AwsConfig awsConfig = AwsConfig.builder().setRegion(TEST_REGION).
                setHostHeader(TEST_HOST).
                                               setAccessKey(TEST_ACCESS_KEY).
                                               setSecretKey(TEST_SECRET_KEY).build();

        DescribeInstances di = new DescribeInstances(awsConfig, TEST_HOST);
        // Override the attributes map. We need to change values. Not pretty, but
        // no real alternative, and in this case : testing only

        Field field = di.getClass().getSuperclass().getDeclaredField("attributes");
        field.setAccessible(true);
        Map<String, String> attributes = (Map<String, String>) field.get(di);
        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);
        field.set(di, attributes);

        // Override private method
        Aws4RequestSignerImpl rs = new Aws4RequestSignerImpl(awsConfig, TEST_REQUEST_DATE, TEST_SERVICE, TEST_HOST);

        Method method = rs.getClass().getDeclaredMethod("deriveSigningKey");
        method.setAccessible(true);
        byte[] derivedKey = (byte[]) method.invoke(rs);

        assertEquals(TEST_DERIVED_EXPECTED, bytesToHex(derivedKey));
    }

    @Test
    public void testSigning()
            throws NoSuchFieldException, IllegalAccessException, IOException, URISyntaxException {
        AwsConfig awsConfig = AwsConfig.builder()
                .setRegion(TEST_REGION)
                .setHostHeader(TEST_HOST)
                .setAccessKey(TEST_ACCESS_KEY)
                .setSecretKey(TEST_SECRET_KEY)
                .build();

        DescribeInstances di = new DescribeInstances(awsConfig, TEST_HOST);
        Aws4RequestSigner requestSigner = di.getRequestSigner();

        Field attributesField = di.getClass().getSuperclass().getDeclaredField("attributes");
        attributesField.setAccessible(true);
        Map<String, String> attributes = (Map<String, String>) attributesField.get(di);
//        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);

        Field headersField = di.getClass().getSuperclass().getDeclaredField("headers");
        headersField.setAccessible(true);
        Map<String, String> headers = (Map<String, String>) headersField.get(di);
//        headers.put("Host", TEST_HOST);
//        headers.put("X-Amz-Date", TEST_REQUEST_DATE);

//        Aws4RequestSignerImpl actual = new Aws4RequestSignerImpl(awsConfig, TEST_REQUEST_DATE, TEST_SERVICE, TEST_HOST);
//        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);
//        attributes.put("X-Amz-Credential", actual.createFormattedCredential());
//        attributes.put("X-Amz-SignedHeaders", "host");
//        String signature = actual.sign(attributes, headers);

//        Aws4RequestSignerReference signer2 = new Aws4RequestSignerReference(awsConfig, TEST_REQUEST_DATE, TEST_SERVICE, TEST_HOST);
//        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);
//        attributes.put("X-Amz-Credential", signer2.createFormattedCredential());
//        String signature = signer2.sign(attributes, headers);
//        attributes.put("X-Amz-SignedHeaders", signer2.getSignedHeaders());

//        attributes.put("X-Amz-Credential", requestSigner.createFormattedCredential());
//        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);
//        String signature = requestSigner.sign(attributes, headers);
//        attributes.put("X-Amz-SignedHeaders", requestSigner.getSignedHeaders());

        Aws4RequestSigner actual = new Aws4RequestSignerImpl(awsConfig, TEST_REQUEST_DATE, TEST_SERVICE, TEST_HOST);
        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);
        headers.put("Host", TEST_HOST);
        attributes.put("X-Amz-Credential", actual.createFormattedCredential());
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
