package com.hazelcast.aws.impl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hazelcast.aws.AwsConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 *
 */
public class TaskMetadataTest {

    private static final String ENDPOINT = "localhost";

    private TaskMetadata taskMetadata;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() throws Exception {
        String testEndpoint = String.format("http://%s:%d", ENDPOINT, wireMockRule.port());

        AwsConfig awsConfig = AwsConfig.builder()
                .setHostHeader("ecs.localhost")
                .build();
        taskMetadata = new TaskMetadata(awsConfig, new URL(testEndpoint));
        stubFor(get(urlMatching("/.*")).atPriority(5)
                .willReturn(aResponse().withStatus(401).withBody("\"reason\":\"Forbidden\"")));

    }

    @Ignore
    @Test
    public void execute() throws Exception {
        String execute = taskMetadata.execute();
    }
}