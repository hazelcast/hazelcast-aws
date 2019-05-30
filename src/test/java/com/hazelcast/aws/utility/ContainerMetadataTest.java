package com.hazelcast.aws.utility;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hazelcast.aws.AwsConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.hazelcast.aws.utility.MetadataUtils.ECS_CONTAINER_METADATA_URI_VAR_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ContainerMetadataTest {

    private static final String ENDPOINT = "localhost";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
    private String testEndpoint;

    @Before
    public void setUp() {
        testEndpoint = String.format("http://%s:%d", ENDPOINT, wireMockRule.port());

        AwsConfig awsConfig = AwsConfig.builder()
                .setHostHeader("ecs.localhost")
                .build();
        stubFor(get(urlMatching("/")).atPriority(1)
                .willReturn(aResponse().withStatus(200).withBody(metadata())));
        stubFor(get(urlMatching("/.*")).atPriority(5)
                .willReturn(aResponse().withStatus(401).withBody("\"reason\":\"Forbidden\"")));

    }

    @Test
    public void testExecute() {
        Environment mockedEnv = mock(Environment.class);
        when(mockedEnv.getEnvVar(ECS_CONTAINER_METADATA_URI_VAR_NAME)).thenReturn(testEndpoint);

        Map<String, String> taskMetadata = MetadataUtils.retrieveContainerMetadataFromEnv(mockedEnv, 15, 2);
        assertEquals(taskMetadata.get("clusterName"), "arn:aws:ecs:eu-central-1:665466731577:cluster/default");
        assertEquals(taskMetadata.get("familyName"), "g-test-shell");
    }

    private String metadata() {
        return "{\n" +
                "    \"DockerId\": \"7957bda89bb6308f7287691685f3ed674b2b780de931ec8b535da41f0864ea11\",\n" +
                "    \"Name\": \"my-bb\",\n" +
                "    \"DockerName\": \"ecs-g-test-shell-15-my-bb-b8d48f93e1fde0b75900\",\n" +
                "    \"Image\": \"busybox:latest\",\n" +
                "    \"ImageID\": \"sha256:af2f74c517aac1d26793a6ed05ff45b299a037e1a9eefeae5eacda133e70a825\",\n" +
                "    \"Labels\": {\n" +
                "        \"com.amazonaws.ecs.cluster\": \"arn:aws:ecs:eu-central-1:665466731577:cluster/default\",\n" +
                "        \"com.amazonaws.ecs.container-name\": \"my-bb\",\n" +
                "        \"com.amazonaws.ecs.task-arn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/0dcf990c3ef3436c84e0c7430d14a3d4\",\n" +
                "        \"com.amazonaws.ecs.task-definition-family\": \"g-test-shell\",\n" +
                "        \"com.amazonaws.ecs.task-definition-version\": \"15\"\n" +
                "    },\n" +
                "    \"DesiredStatus\": \"RUNNING\",\n" +
                "    \"KnownStatus\": \"CREATED\",\n" +
                "    \"Limits\": {\n" +
                "        \"CPU\": 0,\n" +
                "        \"Memory\": 0\n" +
                "    },\n" +
                "    \"CreatedAt\": \"2019-04-04T15:46:26.351467103Z\",\n" +
                "    \"Type\": \"NORMAL\",\n" +
                "    \"Networks\": [\n" +
                "        {\n" +
                "            \"NetworkMode\": \"awsvpc\",\n" +
                "            \"IPv4Addresses\": [\n" +
                "                \"10.0.1.174\"\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
    }
}