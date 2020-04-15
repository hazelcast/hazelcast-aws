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
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class AwsEcsApiTest {
    private static final String AUTHORIZATION_HEADER = "authorization-header";
    private static final String TOKEN = "IQoJb3JpZ2luX2VjEFIaDGV1LWNlbnRyYWwtMSJGM==";
    private static final AwsCredentials CREDENTIALS = AwsCredentials.builder()
        .setAccessKey("AKIDEXAMPLE")
        .setSecretKey("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY")
        .setToken(TOKEN)
        .build();

    @Mock
    private AwsRequestSigner requestSigner;

    private AwsEcsApi awsEcsApi;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        given(requestSigner.authHeader(any(), any(), any(), any(), any(), any())).willReturn(AUTHORIZATION_HEADER);

        String endpoint = String.format("http://localhost:%s", wireMockRule.port());
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1585909518929L), ZoneId.systemDefault());
        AwsConfig awsConfig = AwsConfig.builder().build();
        awsEcsApi = new AwsEcsApi(endpoint, awsConfig, requestSigner, clock);
    }

    @Test
    public void listTasks() {
        // given
        String cluster = "arn:aws:ecs:eu-central-1:665466731577:cluster/rafal-test-cluster";
        String family = "family-name";

        //language=JSON
        String requestBody = "{\n"
            + "  \"cluster\" : \"arn:aws:ecs:eu-central-1:665466731577:cluster/rafal-test-cluster\",\n"
            + "  \"family\" : \"family-name\"\n"
            + "}";

        //language=JSON
        String response = "{\n"
            + "  \"taskArns\": [\n"
            + "    \"arn:aws:ecs:us-east-1:012345678910:task/0b69d5c0-d655-4695-98cd-5d2d526d9d5a\",\n"
            + "    \"arn:aws:ecs:us-east-1:012345678910:task/51a01bdf-d00e-487e-ab14-7645330b6207\"\n"
            + "  ]\n"
            + "}";

        stubFor(post("/")
            .withHeader("X-Amz-Date", equalTo("20200403T102518Z"))
            .withHeader("Authorization", equalTo(AUTHORIZATION_HEADER))
            .withHeader("X-Amz-Target", equalTo("AmazonEC2ContainerServiceV20141113.ListTasks"))
            .withHeader("Content-Type", equalTo("application/x-amz-json-1.1"))
            .withHeader("Accept-Encoding", equalTo("identity"))
            .withHeader("X-Amz-Security-Token", equalTo(TOKEN))
            .withRequestBody(equalToJson(requestBody))
            .willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        List<String> tasks = awsEcsApi.listTasks(cluster, family, CREDENTIALS);

        // then
        assertThat(tasks, hasItems(
            "arn:aws:ecs:us-east-1:012345678910:task/0b69d5c0-d655-4695-98cd-5d2d526d9d5a",
            "arn:aws:ecs:us-east-1:012345678910:task/51a01bdf-d00e-487e-ab14-7645330b6207"
            )
        );
    }

    @Test
    public void describeTasks() {
        // given
        String cluster = "arn:aws:ecs:eu-central-1:665466731577:cluster/rafal-test-cluster";
        List<String> tasks = asList(
            "arn:aws:ecs:eu-central-1-east-1:012345678910:task/0b69d5c0-d655-4695-98cd-5d2d526d9d5a",
            "arn:aws:ecs:eu-central-1:012345678910:task/51a01bdf-d00e-487e-ab14-7645330b6207"
        );

        //language=JSON
        String requestBody = "{\n"
            + "  \"cluster\" : \"arn:aws:ecs:eu-central-1:665466731577:cluster/rafal-test-cluster\",\n"
            + "  \"tasks\": [\n"
            + "    \"arn:aws:ecs:eu-central-1-east-1:012345678910:task/0b69d5c0-d655-4695-98cd-5d2d526d9d5a\",\n"
            + "    \"arn:aws:ecs:eu-central-1:012345678910:task/51a01bdf-d00e-487e-ab14-7645330b6207\"\n"
            + "  ]\n"
            + "}";

        //language=JSON
        String response = "{\n"
            + "  \"tasks\": [\n"
            + "    {\n"
            + "      \"taskArn\": \"arn:aws:ecs:eu-central-1-east-1:012345678910:task/0b69d5c0-d655-4695-98cd-5d2d526d9d5a\",\n"
            + "      \"containers\": [\n"
            + "        {\n"
            + "          \"taskArn\": \"arn:aws:ecs:eu-central-1-east-1:012345678910:task/0b69d5c0-d655-4695-98cd-5d2d526d9d5a\",\n"
            + "          \"networkInterfaces\": [\n"
            + "            {\n"
            + "              \"privateIpv4Address\": \"10.0.1.16\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      ]\n"
            + "    },\n"
            + "    {\n"
            + "      \"taskArn\": \"arn:aws:ecs:eu-central-1:012345678910:task/51a01bdf-d00e-487e-ab14-7645330b6207\",\n"
            + "      \"containers\": [\n"
            + "        {\n"
            + "          \"taskArn\": \"arn:aws:ecs:eu-central-1:012345678910:task/51a01bdf-d00e-487e-ab14-7645330b6207\",\n"
            + "          \"networkInterfaces\": [\n"
            + "            {\n"
            + "              \"privateIpv4Address\": \"10.0.1.219\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        stubFor(post("/")
            .withHeader("X-Amz-Date", equalTo("20200403T102518Z"))
            .withHeader("Authorization", equalTo(AUTHORIZATION_HEADER))
            .withHeader("X-Amz-Target", equalTo("AmazonEC2ContainerServiceV20141113.DescribeTasks"))
            .withHeader("Content-Type", equalTo("application/x-amz-json-1.1"))
            .withHeader("Accept-Encoding", equalTo("identity"))
            .withHeader("X-Amz-Security-Token", equalTo(TOKEN))
            .withRequestBody(equalToJson(requestBody))
            .willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        List<String> result = awsEcsApi.describeTasks(cluster, tasks, CREDENTIALS);

        // then
        assertThat(result, hasItems("10.0.1.16", "10.0.1.219"));
    }

    @Test
    public void awsError() {
        // given
        int errorCode = 401;
        String errorMessage = "Error message retrieved from AWS";
        stubFor(post(urlMatching("/.*"))
            .willReturn(aResponse().withStatus(errorCode).withBody(errorMessage)));

        // when
        RestClientException exception = assertThrows(RestClientException.class,
            () -> awsEcsApi.listTasks("cluster-arn", "family-name", CREDENTIALS));

        // then
        assertTrue(exception.getMessage().contains(Integer.toString(errorCode)));
        assertTrue(exception.getMessage().contains(errorMessage));
    }
}