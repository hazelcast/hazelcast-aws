package com.hazelcast.aws;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class AwsEc2MetadataApiTest {

    private AwsEc2MetadataApi awsEc2MetadataApi;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        AwsConfig awsConfig = AwsConfig.builder().build();
        String endpoint = String.format("http://localhost:%s", wireMockRule.port());
        awsEc2MetadataApi = new AwsEc2MetadataApi(endpoint, endpoint, awsConfig);
    }

    @Test
    public void availabilityZone() {
        // given
        String availabilityZone = "eu-central-1b";
        stubFor(get(urlEqualTo("/placement/availability-zone/"))
            .willReturn(aResponse().withStatus(200).withBody(availabilityZone)));

        // when
        String result = awsEc2MetadataApi.availabilityZone();

        // then
        assertEquals(availabilityZone, result);
    }

    @Test
    public void defaultIamRole() {
        // given
        String defaultIamRole = "default-role-name";
        stubFor(get(urlEqualTo("/iam/security-credentials/"))
            .willReturn(aResponse().withStatus(200).withBody(defaultIamRole)));

        // when
        String result = awsEc2MetadataApi.defaultIamRole();

        // then
        assertEquals(defaultIamRole, result);
    }

    @Test
    public void credentials() {
        // given
        String iamRole = "some-iam-role";
        String response = "{\n"
            + "  \"Code\": \"Success\",\n"
            + "  \"AccessKeyId\": \"Access1234\",\n"
            + "  \"SecretAccessKey\": \"Secret1234\",\n"
            + "  \"Token\": \"Token1234\",\n"
            + "  \"Expiration\": \"2020-03-27T21:01:33Z\"\n"
            + "}";
        stubFor(get(urlEqualTo(String.format("/iam/security-credentials/%s", iamRole)))
            .willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        AwsCredentials result = awsEc2MetadataApi.credentials(iamRole);

        // then
        assertEquals("Access1234", result.getAccessKey());
        assertEquals("Secret1234", result.getSecretKey());
        assertEquals("Token1234", result.getToken());
    }

    @Test
    public void credentialsFromEcs() {
        // given
        String relativePath = "/some-path";
        String response = "{\n"
            + "  \"Code\": \"Success\",\n"
            + "  \"AccessKeyId\": \"Access1234\",\n"
            + "  \"SecretAccessKey\": \"Secret1234\",\n"
            + "  \"Token\": \"Token1234\",\n"
            + "  \"Expiration\": \"2020-03-27T21:01:33Z\"\n"
            + "}";
        stubFor(get(urlEqualTo(relativePath))
            .willReturn(aResponse().withStatus(200).withBody(response)));

        // when
        AwsCredentials result = awsEc2MetadataApi.credentialsFromEcs(relativePath);

        // then
        assertEquals("Access1234", result.getAccessKey());
        assertEquals("Secret1234", result.getSecretKey());
        assertEquals("Token1234", result.getToken());
    }
}