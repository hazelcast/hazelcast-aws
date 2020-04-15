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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class AwsEc2ApiTest {
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(1585909518929L), ZoneId.systemDefault());
    private static final String SIGNATURE = "032264a26b2b3bd7c021603233dd7822b571750e174fb1e47b8e0784dd160fb6";
    private static final AwsConfig AWS_CONFIG = AwsConfig.builder()
        .setSecurityGroupName("hazelcast")
        .setTagKey("aws-test-cluster")
        .setTagValue("cluster1")
        .build();
    private static final String REGION = "eu-central-1";
    private static final String AUTHORIZATION_HEADER = "authorization-header";
    private static final String TOKEN =
        "IQoJb3JpZ2luX2VjEFIaDGV1LWNlbnRyYWwtMSJGMEQCIGNqWOCTxslYFGiTqX2smgm5wANL67R4PE1HPpisXiQxAiBwtbamKgJR8FAcbOOEEMm1nTCsarvIqDGip5SE55ZNsSq6AwhbEAAaDDY2NTQ2NjczMTU3NyIM345eTegAGRnGjFHaKpcD/E8DRZLAQeDobXIgX1/oezU1Q6ZOv/M3tk6maifeh+UQIpRFLntzpPjadt5LiJTngti4KQkXb8XQKKHjIp+zN4rrRYhqUqhAe+BP8Qm7L2NczwRhnSVfoTJjZOx5CNw/tQf1n3CdNWKgZcgTSVwF1lLPyKK0bpoj3AkQvOjfSIo0ix9xHj1FnezO1QVzdFjJK70oMU806bAPzQ48KAVfh2L5gihaZo3KUDydOUpPcRbKYlrflOuifsxO25OAEqxhTLfFQAggApZ1a8ZGG278f+40Quh5XBAySU+SUgm3kDZ5ufWBePXVdfS8MD/WnO1sSRUKJMEFPgVHQ5DwcK8I+k0T4GhSIFxHjtUg8upKviSw1PR3OXI9AxLFpbHNcTXz9Q06sPj59VgnXvIdUwdZ/usL3YOhWI10ouPQQVG6KLdDMZT/gjWlrARN1rXHhuWOzyG5l8HfaYBMczGqgA1H1Oqjc767GaojiJ2N6cQbmmdYZMzG3EuBwKedIloDL0/2hYtiivwoOIycFOPMZcYzBPr8IbxGkVUwkIWc9AU67AHTKRcXVecgSjGOWuhoLz0gd8kSvBCqzvJdAdh0gVxsgTRmsh2BFEmEkqJHckIgpVZC8yEp/UZMAm8yu8RSeIcoxlEZfLKKqqQbWs9iHDBSGFwD5FLi7rHAMmYG2k6zGew2Vse3qI5uXquJDJlyzurZdnxu6O9BFSN0LBgO4e9OGHrLnwPMjYHwCqcsleS3mM7+v8a7i3HPE+wBIjfh9X96Dl25k1OBhvy8Xuzr+cERGqsMWLr5m5eck3V23Y+/pbS6FiFfaYMjc4ewjtPGT3/51wcvOvUTbl5B52uHKwMqIszO/qXTmqm0roC/OA==";
    private static final AwsCredentials CREDENTIALS = AwsCredentials.builder()
        .setAccessKey("AKIDEXAMPLE")
        .setSecretKey("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY")
        .setToken(TOKEN)
        .build();
    private String endpoint;

    @Mock
    private AwsRequestSigner requestSigner;

    private AwsEc2Api awsEc2Api;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        given(requestSigner.authenticationHeader(any(), any(), any(), any(), any(), any())).willReturn(AUTHORIZATION_HEADER);
        endpoint = String.format("http://localhost:%s", wireMockRule.port());
        awsEc2Api = new AwsEc2Api(endpoint, AWS_CONFIG, requestSigner, CLOCK);
    }

    @Test
    public void describeInstances() {
        // given
        String requestUrl = "/?Action=DescribeInstances"
            + "&Filter.1.Name=tag%3Aaws-test-cluster"
            + "&Filter.1.Value.1=cluster1"
            + "&Filter.2.Name=instance.group-name"
            + "&Filter.2.Value.1=hazelcast"
            + "&Filter.3.Name=instance-state-name&Filter.3.Value.1=running"
            + "&Version=2016-11-15";

        //language=XML
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<DescribeInstancesResponse xmlns=\"http://ec2.amazonaws.com/doc/2016-11-15/\">\n"
            + "    <reservationSet>\n"
            + "        <item>\n"
            + "            <instancesSet>\n"
            + "                <item>\n"
            + "                    <privateIpAddress>10.0.1.25</privateIpAddress>\n"
            + "                    <ipAddress>54.93.121.213</ipAddress>\n"
            + "                    <tagSet>\n"
            + "                        <item>\n"
            + "                            <key>kubernetes.io/cluster/openshift-cluster</key>\n"
            + "                            <value>openshift-cluster-eu-central-1</value>\n"
            + "                        </item>\n"
            + "                        <item>\n"
            + "                            <key>Name</key>\n"
            + "                            <value>* OpenShift Node 1</value>\n"
            + "                        </item>\n"
            + "                    </tagSet>\n"
            + "                </item>\n"
            + "            </instancesSet>\n"
            + "        </item>\n"
            + "        <item>\n"
            + "            <instancesSet>\n"
            + "                <item>\n"
            + "                    <privateIpAddress>172.31.14.42</privateIpAddress>\n"
            + "                    <ipAddress>18.196.228.248</ipAddress>\n"
            + "                    <tagSet>\n"
            + "                        <item>\n"
            + "                            <key>Name</key>\n"
            + "                            <value>rafal-ubuntu-2</value>\n"
            + "                        </item>\n"
            + "                    </tagSet>\n"
            + "                </item>\n"
            + "            </instancesSet>\n"
            + "        </item>\n"
            + "    </reservationSet>\n"
            + "</DescribeInstancesResponse>";

        stubFor(get(urlEqualTo(requestUrl))
            .withHeader("X-Amz-Date", equalTo("20200403T102518Z"))
            .withHeader("Authorization", equalTo(AUTHORIZATION_HEADER))
            .withHeader("X-Amz-Security-Token", equalTo(TOKEN))
            .willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        Map<String, String> result = awsEc2Api.describeInstances(CREDENTIALS);

        // then
        assertEquals(2, result.size());
        assertEquals("54.93.121.213", result.get("10.0.1.25"));
        assertEquals("18.196.228.248", result.get("172.31.14.42"));
    }

    @Test
    public void describeInstancesNoPublicIpNoInstanceName() {
        // given
        String requestUrl = "/?Action=DescribeInstances"
            + "&Filter.1.Name=tag%3Aaws-test-cluster"
            + "&Filter.1.Value.1=cluster1"
            + "&Filter.2.Name=instance.group-name"
            + "&Filter.2.Value.1=hazelcast"
            + "&Filter.3.Name=instance-state-name&Filter.3.Value.1=running"
            + "&Version=2016-11-15";

        //language=XML
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<DescribeInstancesResponse xmlns=\"http://ec2.amazonaws.com/doc/2016-11-15/\">\n"
            + "    <reservationSet>\n"
            + "        <item>\n"
            + "            <instancesSet>\n"
            + "                <item>\n"
            + "                    <privateIpAddress>10.0.1.25</privateIpAddress>\n"
            + "                </item>\n"
            + "            </instancesSet>\n"
            + "        </item>\n"
            + "        <item>\n"
            + "            <instancesSet>\n"
            + "                <item>\n"
            + "                    <privateIpAddress>172.31.14.42</privateIpAddress>\n"
            + "                </item>\n"
            + "            </instancesSet>\n"
            + "        </item>\n"
            + "    </reservationSet>\n"
            + "</DescribeInstancesResponse>";

        stubFor(get(urlEqualTo(requestUrl))
            .withHeader("X-Amz-Date", equalTo("20200403T102518Z"))
            .withHeader("Authorization", equalTo(AUTHORIZATION_HEADER))
            .withHeader("X-Amz-Security-Token", equalTo(TOKEN))
            .willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        Map<String, String> result = awsEc2Api.describeInstances(CREDENTIALS);

        // then
        assertEquals(2, result.size());
        assertEquals(null, result.get("10.0.1.25"));
        assertEquals(null, result.get("172.31.14.42"));
    }

    @Test
    public void describeNetworkInterfaces() {
        // given
        List<String> privateAddresses = Arrays.asList("10.0.1.207", "10.0.1.82");

        String requestUrl = "/?Action=DescribeNetworkInterfaces"
            + "&Filter.1.Name=addresses.private-ip-address"
            + "&Filter.1.Value.1=10.0.1.207"
            + "&Filter.1.Value.2=10.0.1.82"
            + "&Version=2016-11-15";

        //language=XML
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<DescribeNetworkInterfacesResponse xmlns=\"http://ec2.amazonaws.com/doc/2016-11-15/\">\n"
            + "    <requestId>21bc9f93-2196-4107-87a3-9e5b2b3f29d9</requestId>\n"
            + "    <networkInterfaceSet>\n"
            + "        <item>\n"
            + "            <availabilityZone>eu-central-1a</availabilityZone>\n"
            + "            <privateIpAddress>10.0.1.207</privateIpAddress>\n"
            + "            <association>\n"
            + "                <publicIp>54.93.217.194</publicIp>\n"
            + "            </association>\n"
            + "        </item>\n"
            + "        <item>\n"
            + "            <availabilityZone>eu-central-1a</availabilityZone>\n"
            + "            <privateIpAddress>10.0.1.82</privateIpAddress>\n"
            + "            <association>\n"
            + "                <publicIp>35.156.192.128</publicIp>\n"
            + "            </association>\n"
            + "        </item>\n"
            + "    </networkInterfaceSet>\n"
            + "</DescribeNetworkInterfacesResponse>";

        stubFor(get(urlEqualTo(requestUrl))
            .withHeader("X-Amz-Date", equalTo("20200403T102518Z"))
            .withHeader("Authorization", equalTo(AUTHORIZATION_HEADER))
            .withHeader("X-Amz-Security-Token", equalTo(TOKEN))
            .willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        Map<String, String> result = awsEc2Api.describeNetworkInterfaces(privateAddresses, CREDENTIALS);

        // then
        assertEquals(2, result.size());
        assertEquals("54.93.217.194", result.get("10.0.1.207"));
        assertEquals("35.156.192.128", result.get("10.0.1.82"));
    }

    @Test
    public void describeNetworkInterfacesNoPublicIp() {
        // given
        List<String> privateAddresses = Arrays.asList("10.0.1.207", "10.0.1.82");

        String requestUrl = "/?Action=DescribeNetworkInterfaces"
            + "&Filter.1.Name=addresses.private-ip-address"
            + "&Filter.1.Value.1=10.0.1.207"
            + "&Filter.1.Value.2=10.0.1.82"
            + "&Version=2016-11-15";

        //language=XML
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<DescribeNetworkInterfacesResponse xmlns=\"http://ec2.amazonaws.com/doc/2016-11-15/\">\n"
            + "    <networkInterfaceSet>\n"
            + "        <item>\n"
            + "            <privateIpAddress>10.0.1.207</privateIpAddress>\n"
            + "        </item>\n"
            + "        <item>\n"
            + "            <privateIpAddress>10.0.1.82</privateIpAddress>\n"
            + "        </item>\n"
            + "    </networkInterfaceSet>\n"
            + "</DescribeNetworkInterfacesResponse>";

        stubFor(get(urlEqualTo(requestUrl))
            .withHeader("X-Amz-Date", equalTo("20200403T102518Z"))
            .withHeader("Authorization", equalTo(AUTHORIZATION_HEADER))
            .withHeader("X-Amz-Security-Token", equalTo(TOKEN))
            .willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        Map<String, String> result = awsEc2Api.describeNetworkInterfaces(privateAddresses, CREDENTIALS);

        // then
        assertEquals(2, result.size());
        assertEquals(null, result.get("10.0.1.207"));
        assertEquals(null, result.get("10.0.1.82"));
    }

    @Test
    public void awsError() {
        // given
        int errorCode = 401;
        String errorMessage = "Error message retrieved from AWS";
        stubFor(get(urlMatching("/.*"))
            .willReturn(aResponse().withStatus(errorCode).withBody(errorMessage)));

        // when
        RestClientException exception = assertThrows(RestClientException.class,
            () -> awsEc2Api.describeInstances(CREDENTIALS));

        // then
        assertTrue(exception.getMessage().contains(Integer.toString(errorCode)));
        assertTrue(exception.getMessage().contains(errorMessage));
    }
}
