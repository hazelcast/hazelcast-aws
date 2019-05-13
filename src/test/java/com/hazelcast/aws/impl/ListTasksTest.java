package com.hazelcast.aws.impl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.hazelcast.aws.AwsConfig;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;
import java.util.Collection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 *
 */
public class ListTasksTest {

    private static final String ENDPOINT = "localhost";
    private final static String TEST_REGION = "eu-central-1";
    private final static String TEST_HOST = "ecs.eu-central-1.amazonaws.com";
    private final static String TEST_ACCESS_KEY = "AKIDEXAMPLE";
    private final static String TEST_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
    private ListTasks listTasks;

    @Before
    public void setUp() throws Exception {
        //disableSSLCertValidation();

        String testEndpoint = String.format("http://%s:%d", ENDPOINT, wireMockRule.port());

        AwsConfig awsConfig = AwsConfig.builder()
                .setHostHeader(TEST_HOST)
                .setRegion(TEST_REGION)
                .setAccessKey(TEST_ACCESS_KEY)
                .setSecretKey(TEST_SECRET_KEY)
                .build();
        listTasks = new ListTasks(awsConfig, new URL(testEndpoint));
        stubFor(post(urlMatching("^/.*"))
                .atPriority(5).willReturn(aResponse().withStatus(401).withBody("\"reason\":\"Forbidden\"")));

    }

    @Test
    public void listTasksEmpty() throws Exception {
        // given
        stubListTasks("/.*", listTaskEmptyResponse());

        // when
        Collection<String> taskArns = listTasks.execute();

        // then
        MatcherAssert.assertThat("empty list", taskArns.size() == 0);
    }

    @Test
    public void listTasks() throws Exception {
        // given
        stubListTasks("/.*", listTasksResponse());

        // when
        Collection<String> taskArns = listTasks.execute();

        // then
        MatcherAssert.assertThat("list of 2 tasks", taskArns.size() == 2);
    }

    private void stubListTasks(String urlRegex, String response) {
        stubFor(post(urlMatching(urlRegex))
                .withHeader("X-Amz-Target", new EqualToPattern("AmazonEC2ContainerServiceV20141113.ListTasks"))
                .willReturn(aResponse().withStatus(200).withBody(response)));
    }

    private String listTaskEmptyResponse() {
        //language=JSON
        return "{\n"
                + "    \"taskArns\": []\n"
                + "}\n";
    }

    private String listTasksResponse() {
        return "{\n" +
                "    \"taskArns\": [\n" +
                "        \"arn:aws:ecs:eu-central-1:665466731577:task/default/40c003cf5d504c65b2d5418c9e9deb72\", \n" +
                "        \"arn:aws:ecs:eu-central-1:665466731577:task/default/bf2687a5187b46ac8edf541bac799d52\"\n" +
                "    ]\n" +
                "}\n";
    }
}