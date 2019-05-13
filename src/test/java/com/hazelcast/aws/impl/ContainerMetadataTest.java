package com.hazelcast.aws.impl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hazelcast.aws.AwsConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 *
 */
public class ContainerMetadataTest {

    private static final String ENDPOINT = "localhost";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        String testEndpoint = String.format("http://%s:%d", ENDPOINT, wireMockRule.port());

        AwsConfig awsConfig = AwsConfig.builder()
                .setHostHeader("ecs.localhost")
                .build();
        stubFor(get(urlMatching("/.*")).atPriority(5)
                .willReturn(aResponse().withStatus(401).withBody("\"reason\":\"Forbidden\"")));

    }

    @Test
    public void testExecute() {
        // TODO stub Environment
        // taskMetadata = MetadataUtil.retrieveContainerMetadata();
    }
}