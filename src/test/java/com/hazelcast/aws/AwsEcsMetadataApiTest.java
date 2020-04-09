package com.hazelcast.aws;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class AwsEcsMetadataApiTest {

    private AwsEcsMetadataApi awsEcsMetadataApi;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        AwsConfig awsConfig = AwsConfig.builder().build();
        String endpoint = String.format("http://localhost:%s", wireMockRule.port());
        awsEcsMetadataApi = new AwsEcsMetadataApi(endpoint, awsConfig);
    }

    @Test
    public void metadata() {
        // given
        //language=JSON
        String response = "{\n"
            + "  \"Name\": \"container-name\",\n"
            + "  \"Labels\": {\n"
            + "    \"com.amazonaws.ecs.cluster\": \"arn:aws:ecs:eu-central-1:665466731577:cluster/default\",\n"
            + "    \"com.amazonaws.ecs.container-name\": \"container-name\",\n"
            + "    \"com.amazonaws.ecs.task-arn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/0dcf990c3ef3436c84e0c7430d14a3d4\",\n"
            + "    \"com.amazonaws.ecs.task-definition-family\": \"family-name\"\n"
            + "  },\n"
            + "  \"Networks\": [\n"
            + "    {\n"
            + "      \"NetworkMode\": \"awsvpc\",\n"
            + "      \"IPv4Addresses\": [\n"
            + "        \"10.0.1.174\"\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        stubFor(get("/").willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        AwsEcsMetadataApi.EcsMetadata result = awsEcsMetadataApi.metadata();

        // then
        assertEquals("arn:aws:ecs:eu-central-1:665466731577:cluster/default", result.getClusterArn());
        assertEquals("family-name", result.getFamilyName());
    }
}