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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

public class AwsRequestSignerTest {
    private final static String REQUEST_DATE = "20141106T111126Z";

    private final static String TEST_REGION = "eu-central-1";
    private final static String TEST_HOST = "ecs.eu-central-1.amazonaws.com";
    private final static String TEST_SERVICE = "ecs";
    private final static String TEST_ACCESS_KEY = "AKIDEXAMPLE";
    private final static String TEST_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private final static String TEST_REQUEST_DATE = "20141106T111126Z";
    private final static String TEST_SIGNATURE_EXPECTED = "272804941fb77a8c7def35d26bd97a704da4ea9e65cb18296213f840395d646c";
    private final static String TEST_AUTHENTICATION_HEADER = "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20141106/eu"
        + "-central-1/ecs/aws4_request, SignedHeaders=host;x-amz-date, Signature=" + TEST_SIGNATURE_EXPECTED;

    @Test
    public void signEcs() {
        // given
        AwsConfig awsConfig = AwsConfig.builder()
            .setRegion(TEST_REGION)
            .setHostHeader(TEST_HOST)
            .setAccessKey(TEST_ACCESS_KEY)
            .setSecretKey(TEST_SECRET_KEY)
            .build();
        AwsCredentials awsCredentials = AwsCredentials.builder()
            .setAccessKey(TEST_ACCESS_KEY)
            .setSecretKey(TEST_SECRET_KEY)
            .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Amz-Date", TEST_REQUEST_DATE);
        headers.put("Host", TEST_HOST);

        AwsRequestSigner requestSigner = new AwsRequestSigner(TEST_REGION, TEST_HOST, "ecs");

        // when
        String signature = requestSigner.authHeader(emptyMap(), headers, "", awsCredentials, TEST_REQUEST_DATE, "GET");

        // then
        assertEquals(TEST_AUTHENTICATION_HEADER, signature);
    }

    @Test
    public void signEcsSecond() {
        // given
        AwsConfig awsConfig = AwsConfig.builder()
            .setRegion("eu-central-1")
            .setHostHeader("ecs.eu-central-1.amazonaws.com")
            .setAccessKey(TEST_ACCESS_KEY)
            .setSecretKey(TEST_SECRET_KEY)
            .build();
        AwsCredentials awsCredentials = AwsCredentials.builder()
            .setAccessKey(TEST_ACCESS_KEY)
            .setSecretKey(TEST_SECRET_KEY)
            .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Amz-Date", "20200409T144619Z");
        headers.put("X-Amz-Security-Token", "IQoJb3JpZ2luX2VjEOf//////////wEaDGV1LWNlbnRyYWwtMSJHMEUCIA198PQif6eefjZREzH85wqGm/82zWQn4xB7faJ7RMVzAiEA6yvQk1jaSXkzfRhxn3wGYfR8Gl5sM7VLcP+iCwhKznYqgQQI8P//////////ARAAGgw2NjU0NjY3MzE1NzciDAOipuj8pwuNkEWTzirVA0rUn8aM2uunfc8fPzD3LnoQkCOak2pBT9V3D4jrO3fvK0Nrdipzyl6PKKXzBD9STJp75Y/XCP4fNdfepxt+d1VOFXVLtNGC/aA8oa3N9RaNAcZIYHcVQqO8K+KylpP6kpNGoFAY0nu3R8BMBJZQqtrMLsUTNwU/Tx13qZmQI5YURsdHYs8ZtlZc7rHECHHYBDp1fVhg+80IHHe+K8hmePHTav6GjNyZRh20ENB89VNauF03sD8boZNx+NMFJlb9vTV4AjxYVkZ2v5DxXjZWT/qYVorhHFFnUDIOFG74VnHqB/2V1G5MW2cTnqzr7kQSu23ebj4t+yXxhRx344tnGLyG9/iny/p5MLfrde77CX5w979q4oZANp+eImRkoR9t0Pp49LjzfxBHr8Hsyigqie3hDfsCDWDGorM+WZ8ZyyN9NetsSSvMnytIknoGN9T6LVBbkmsRc6sgvMR51s9xvCLsthgpZjQTCBBQwJ7SiEm745AgsnEH0ctLyPDwHPw75edmdAfB3RLKaqDqeiRZIsIyHhZeJnfC7Jjf6X6YetXRhsJiNuYD5SFSTjNGs3iZV5iYRl9c9+id+GbibRL3xCDCNe1rTDOffdGXKVqofqFQRKe8ja0wneq89AU67wHbRiEEKflex4E0pNDAvDAF9V7IPwbwqjjBO6H6St9NX21nX0VrWtHgypAvASW8X0CLn4FNG27i7EhsOS7H8xzUdZVDz8aYdY6A2yIGAh9wvPZr+bwlHp6pfpPw4UnajbUT1V2HD+IOIQ7J2KiJw2rHQ+rzCVyspB1WkPRHf9F9TvAtQgDwRtXw6OY1V3q7Gc6DqQyW/b8Ua2P2T6QEza2FeB/xUTtMNbS+rKC0sjv6qZSEC/+9jOnh5nmeyKvaUtg6echbvXNiYxryFV6GDKe63uY+m6hO/gOVsWAOyT3xz9nyXfyiCxfZP9yRoMwG7Q==");
        headers.put("Host", "ecs.eu-central-1.amazonaws.com");
        headers.put("Accept-Encoding", "identity");
        headers.put("X-Amz-Target", "AmazonEC2ContainerServiceV20141113.ListTasks");
        headers.put("Content-Type", "application/x-amz-json-1.1");

        String body = "{\"cluster\":\"arn:aws:ecs:eu-central-1:665466731577:cluster/rafal-test-cluster\","
            + "\"family\":\"rafal-test-aws-ecs\"}";

        AwsRequestSigner requestSigner = new AwsRequestSigner("eu-central-1", "ecs.eu-central-1.amazonaws.com",
            "ecs");

        // when
        String signature = requestSigner.authHeader(emptyMap(), headers, body, awsCredentials, "20200409T144619Z", "POST");

        // then
        assertEquals("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20200409/eu-central-1/ecs/aws4_request, SignedHeaders=accept-encoding;content-type;host;x-amz-date;x-amz-security-token;x-amz-target, Signature=5bc9912aa2fd47e19e2c80c903a4e4d5ca63dbe73af19fed6887c1d3610554bb", signature);
    }

    private static final String TEST_SIGNATURE_EXPECTED_EC2 = "8e4f83fe919390f53fa71ea0ea8a25a09e7d10e1740b238fc6969a1410e06c57";

    @Test
    public void testSigningEc2() {
//        AwsConfig awsConfig = AwsConfig.builder()
//            .setRegion("eu-central-1")
//            .setHostHeader("ec2.eu-central-1.amazonaws.com")
//            .setAccessKey(TEST_ACCESS_KEY)
//            .setSecretKey(TEST_SECRET_KEY)
//            .build();
//        AwsCredentials awsCredentials = AwsCredentials.builder()
//            .setAccessKey(TEST_ACCESS_KEY)
//            .setSecretKey(TEST_SECRET_KEY)
//            .build();
//
//        Map<String, String> attributes = new HashMap<>();
//        Filter filter = new Filter();
//        filter.addFilter("instance-state-name", "running");
//        attributes.putAll(filter.getFilterAttributes());
//        attributes.put("Action", "DescribeInstances");
//        attributes.put("Version", "2016-11-15");
//
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Host", TEST_HOST);
//        headers.put("X-Amz-Date", TEST_REQUEST_DATE);
//
//        AwsRequestSigner awsRequestSigner = new AwsRequestSigner("eu-central-1", "ec2.eu-central-1"
//            + ".amazonaws.com", "ec2");
//        String signature = awsRequestSigner.sign(attributes, headers, "", awsCredentials, TEST_REQUEST_DATE, "GET");
//
//        assertEquals(TEST_SIGNATURE_EXPECTED_EC2, signature);
    }


}
