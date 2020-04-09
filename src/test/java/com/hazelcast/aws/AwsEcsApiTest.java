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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class AwsEcsApiTest {
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(1585909518929L), ZoneId.systemDefault());
    private static final String AUTHORIZATION_HEADER = "authorization-header";
    private static final String REGION = "eu-central-1";
    private static final AwsConfig AWS_CONFIG = AwsConfig.builder().build();
    private static final String TOKEN =
        "IQoJb3JpZ2luX2VjEFIaDGV1LWNlbnRyYWwtMSJGMEQCIGNqWOCTxslYFGiTqX2smgm5wANL67R4PE1HPpisXiQxAiBwtbamKgJR8FAcbOOEEMm1nTCsarvIqDGip5SE55ZNsSq6AwhbEAAaDDY2NTQ2NjczMTU3NyIM345eTegAGRnGjFHaKpcD/E8DRZLAQeDobXIgX1/oezU1Q6ZOv/M3tk6maifeh+UQIpRFLntzpPjadt5LiJTngti4KQkXb8XQKKHjIp+zN4rrRYhqUqhAe+BP8Qm7L2NczwRhnSVfoTJjZOx5CNw/tQf1n3CdNWKgZcgTSVwF1lLPyKK0bpoj3AkQvOjfSIo0ix9xHj1FnezO1QVzdFjJK70oMU806bAPzQ48KAVfh2L5gihaZo3KUDydOUpPcRbKYlrflOuifsxO25OAEqxhTLfFQAggApZ1a8ZGG278f+40Quh5XBAySU+SUgm3kDZ5ufWBePXVdfS8MD/WnO1sSRUKJMEFPgVHQ5DwcK8I+k0T4GhSIFxHjtUg8upKviSw1PR3OXI9AxLFpbHNcTXz9Q06sPj59VgnXvIdUwdZ/usL3YOhWI10ouPQQVG6KLdDMZT/gjWlrARN1rXHhuWOzyG5l8HfaYBMczGqgA1H1Oqjc767GaojiJ2N6cQbmmdYZMzG3EuBwKedIloDL0/2hYtiivwoOIycFOPMZcYzBPr8IbxGkVUwkIWc9AU67AHTKRcXVecgSjGOWuhoLz0gd8kSvBCqzvJdAdh0gVxsgTRmsh2BFEmEkqJHckIgpVZC8yEp/UZMAm8yu8RSeIcoxlEZfLKKqqQbWs9iHDBSGFwD5FLi7rHAMmYG2k6zGew2Vse3qI5uXquJDJlyzurZdnxu6O9BFSN0LBgO4e9OGHrLnwPMjYHwCqcsleS3mM7+v8a7i3HPE+wBIjfh9X96Dl25k1OBhvy8Xuzr+cERGqsMWLr5m5eck3V23Y+/pbS6FiFfaYMjc4ewjtPGT3/51wcvOvUTbl5B52uHKwMqIszO/qXTmqm0roC/OA==";
    private static final AwsCredentials CREDENTIALS = AwsCredentials.builder()
        .setAccessKey("AKIDEXAMPLE")
        .setSecretKey("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY")
        .setToken(TOKEN)
        .build();

    @Mock
    private AwsEc2RequestSigner requestSigner;

    private AwsEcsApi awsEcsApi;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private String endpoint;

    @Before
    public void setUp() {
        given(requestSigner.authenticationHeader(any(), any(), any(), any(), any(), any(), any(), any())).willReturn(AUTHORIZATION_HEADER);
        endpoint = String.format("http://localhost:%s", wireMockRule.port());
        awsEcsApi = new AwsEcsApi(endpoint, AWS_CONFIG, requestSigner, CLOCK);
    }

    @Test
    public void listTasks() throws Exception {
        // given
        //language=JSON
        String requestBody = "{\n"
            + "  \"cluster\" : \"cluster-arn\",\n"
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
        List<String> tasks = awsEcsApi.listTasks("cluster-arn", "family-name", REGION, CREDENTIALS);

        // then
        assertThat(tasks, hasItems(
            "arn:aws:ecs:us-east-1:012345678910:task/0b69d5c0-d655-4695-98cd-5d2d526d9d5a",
            "arn:aws:ecs:us-east-1:012345678910:task/51a01bdf-d00e-487e-ab14-7645330b6207"
            )
        );
    }
}