package com.hazelcast.aws.impl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.hazelcast.aws.AwsConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 *
 */
public class DescribeNetworkInterfacesTest {

    private static final String ENDPOINT = "localhost";
    private final static String TEST_REGION = "eu-central-1";
    private final static String TEST_HOST = "ecs.eu-central-1.amazonaws.com";
    private final static String TEST_ACCESS_KEY = "AKIDEXAMPLE";
    private final static String TEST_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
    private DescribeNetworkInterfaces describeNetworkInterfaces;

    @Before
    public void setUp() throws Exception {
        String testEndpoint = String.format("http://%s:%d", ENDPOINT, wireMockRule.port());

        AwsConfig awsConfig = AwsConfig.builder()
                .setHostHeader(TEST_HOST)
                .setRegion(TEST_REGION)
                .setAccessKey(TEST_ACCESS_KEY)
                .setSecretKey(TEST_SECRET_KEY)
                .build();
        describeNetworkInterfaces = new DescribeNetworkInterfaces(awsConfig, new URL(testEndpoint));

    }

    @Test
    public void execute() throws Exception {
        stubDescribeTasks("/.*", describeNetworkInterfacesResponse());

        // when
        Map<String, String> intfaces = describeNetworkInterfaces.execute();

        // then
        // TODO
        //MatcherAssert.assertThat("list of 3 interfaces", intfaces.size(), new IsEqual<Integer>(2));
    }

    private void stubDescribeTasks(String urlRegex, String response) {
        stubFor(post(urlMatching(urlRegex))
                .withQueryParam("Action", new EqualToPattern("DescribeNetworkInterfaces"))
                .willReturn(aResponse().withStatus(200).withBody(response)));

    }

    private String describeNetworkInterfacesResponse() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<DescribeNetworkInterfacesResponse xmlns=\"http://ec2.amazonaws.com/doc/2016-11-15/\">\n" +
                "    <requestId>21bc9f93-2196-4107-87a3-9e5b2b3f29d9</requestId>\n" +
                "    <networkInterfaceSet>\n" +
                "        <item>\n" +
                "            <networkInterfaceId>eni-072205748f23b9db8</networkInterfaceId>\n" +
                "            <subnetId>subnet-0f042c997bad8e2b9</subnetId>\n" +
                "            <vpcId>vpc-0681043d6f49b039a</vpcId>\n" +
                "            <availabilityZone>eu-central-1a</availabilityZone>\n" +
                "            <description/>\n" +
                "            <requesterManaged>false</requesterManaged>\n" +
                "            <status>in-use</status>\n" +
                "            <macAddress>02:5b:f7:d1:86:b8</macAddress>\n" +
                "            <privateIpAddress>10.0.1.207</privateIpAddress>\n" +
                "            <privateDnsName>ip-10-0-1-207.eu-central-1.compute.internal</privateDnsName>\n" +
                "            <sourceDestCheck>true</sourceDestCheck>\n" +
                "            <attachment>\n" +
                "                <attachmentId>eni-attach-07d12c551700e5873</attachmentId>\n" +
                "                <deviceIndex>0</deviceIndex>\n" +
                "                <status>attached</status>\n" +
                "                <attachTime>2019-04-18T14:53:25.000Z</attachTime>\n" +
                "                <deleteOnTermination>true</deleteOnTermination>\n" +
                "            </attachment>\n" +
                "            <association>\n" +
                "                <publicIp>54.93.217.194</publicIp>\n" +
                "                <publicDnsName>ec2-54-93-217-194.eu-central-1.compute.amazonaws.com</publicDnsName>\n" +
                "                <ipOwnerId>665466731577</ipOwnerId>\n" +
                "                <allocationId>eipalloc-0c2758a281484b7c5</allocationId>\n" +
                "                <associationId>eipassoc-084d96d1516fbc707</associationId>\n" +
                "                <natEnabled>true</natEnabled>\n" +
                "            </association>\n" +
                "            <tagSet/>\n" +
                "            <privateIpAddressesSet>\n" +
                "                <item>\n" +
                "                    <privateIpAddress>10.0.1.207</privateIpAddress>\n" +
                "                    <privateDnsName>ip-10-0-1-207.eu-central-1.compute.internal</privateDnsName>\n" +
                "                    <primary>true</primary>\n" +
                "                    <association>\n" +
                "                        <publicIp>54.93.217.194</publicIp>\n" +
                "                        <publicDnsName>ec2-54-93-217-194.eu-central-1.compute.amazonaws.com</publicDnsName>\n" +
                "                        <ipOwnerId>665466731577</ipOwnerId>\n" +
                "                        <allocationId>eipalloc-0c2758a281484b7c5</allocationId>\n" +
                "                        <associationId>eipassoc-084d96d1516fbc707</associationId>\n" +
                "                        <natEnabled>true</natEnabled>\n" +
                "                    </association>\n" +
                "                </item>\n" +
                "            </privateIpAddressesSet>\n" +
                "            <ipv6AddressesSet/>\n" +
                "            <interfaceType>interface</interfaceType>\n" +
                "        </item>\n" +
                "        <item>\n" +
                "            <networkInterfaceId>eni-00920151bbce229d6</networkInterfaceId>\n" +
                "            <subnetId>subnet-0f042c997bad8e2b9</subnetId>\n" +
                "            <vpcId>vpc-0681043d6f49b039a</vpcId>\n" +
                "            <availabilityZone>eu-central-1a</availabilityZone>\n" +
                "            <description/>\n" +
                "            <requesterManaged>false</requesterManaged>\n" +
                "            <status>in-use</status>\n" +
                "            <macAddress>02:37:de:81:dc:b0</macAddress>\n" +
                "            <privateIpAddress>10.0.1.82</privateIpAddress>\n" +
                "            <privateDnsName>ip-10-0-1-82.eu-central-1.compute.internal</privateDnsName>\n" +
                "            <sourceDestCheck>true</sourceDestCheck>\n" +
                "            <attachment>\n" +
                "                <attachmentId>eni-attach-0ab014c78885ef88d</attachmentId>\n" +
                "                <deviceIndex>0</deviceIndex>\n" +
                "                <status>attached</status>\n" +
                "                <attachTime>2019-04-18T14:53:26.000Z</attachTime>\n" +
                "                <deleteOnTermination>true</deleteOnTermination>\n" +
                "            </attachment>\n" +
                "            <association>\n" +
                "                <publicIp>35.156.192.128</publicIp>\n" +
                "                <publicDnsName>ec2-35-156-192-128.eu-central-1.compute.amazonaws.com</publicDnsName>\n" +
                "                <ipOwnerId>665466731577</ipOwnerId>\n" +
                "                <allocationId>eipalloc-0bb825da9116cdfe5</allocationId>\n" +
                "                <associationId>eipassoc-0aa7bcbcd781b67eb</associationId>\n" +
                "                <natEnabled>true</natEnabled>\n" +
                "            </association>\n" +
                "            <tagSet/>\n" +
                "            <privateIpAddressesSet>\n" +
                "                <item>\n" +
                "                    <privateIpAddress>10.0.1.82</privateIpAddress>\n" +
                "                    <privateDnsName>ip-10-0-1-82.eu-central-1.compute.internal</privateDnsName>\n" +
                "                    <primary>true</primary>\n" +
                "                    <association>\n" +
                "                        <publicIp>35.156.192.128</publicIp>\n" +
                "                        <publicDnsName>ec2-35-156-192-128.eu-central-1.compute.amazonaws.com</publicDnsName>\n" +
                "                        <ipOwnerId>665466731577</ipOwnerId>\n" +
                "                        <allocationId>eipalloc-0bb825da9116cdfe5</allocationId>\n" +
                "                        <associationId>eipassoc-0aa7bcbcd781b67eb</associationId>\n" +
                "                        <natEnabled>true</natEnabled>\n" +
                "                    </association>\n" +
                "                </item>\n" +
                "            </privateIpAddressesSet>\n" +
                "            <ipv6AddressesSet/>\n" +
                "            <interfaceType>interface</interfaceType>\n" +
                "        </item>\n" +
                "    </networkInterfaceSet>\n" +
                "</DescribeNetworkInterfacesResponse>\n";
    }
}