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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class RestClientTest {
    private static final String API_ENDPOINT = "/some/endpoint";
    private static final String BODY_REQUEST = "some body request";
    private static final String BODY_RESPONSE = "some body response";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private String address;

    @Before
    public void setUp() {
        address = String.format("http://localhost:%s", wireMockRule.port());
    }

    @Test
    public void getSuccess() {
        // given
        stubFor(get(urlEqualTo(API_ENDPOINT))
            .willReturn(aResponse().withStatus(200).withBody(BODY_RESPONSE)));

        // when
        String result = RestClient.create(String.format("%s%s", address, API_ENDPOINT)).get();

        // then
        assertEquals(BODY_RESPONSE, result);
    }

    @Test
    public void getWithHeadersSuccess() {
        // given
        String headerKey = "Metadata-Flavor";
        String headerValue = "Google";
        stubFor(get(urlEqualTo(API_ENDPOINT))
            .withHeader(headerKey, equalTo(headerValue))
            .willReturn(aResponse().withStatus(200).withBody(BODY_RESPONSE)));

        // when
        String result = RestClient.create(String.format("%s%s", address, API_ENDPOINT))
            .withHeaders(singletonMap(headerKey, headerValue))
            .get();

        // then
        assertEquals(BODY_RESPONSE, result);
    }

    @Test
    public void getWithRetries() {
        // given
        stubFor(get(urlEqualTo(API_ENDPOINT))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(500).withBody("Internal error"))
            .willSetStateTo("Second Try"));
        stubFor(get(urlEqualTo(API_ENDPOINT))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Second Try")
            .willReturn(aResponse().withStatus(200).withBody(BODY_RESPONSE)));

        // when
        String result = RestClient.create(String.format("%s%s", address, API_ENDPOINT))
            .withReadTimeoutSeconds(1200)
            .withConnectTimeoutSeconds(1200)
            .withRetries(1)
            .get();

        // then
        assertEquals(BODY_RESPONSE, result);
    }

    @Test(expected = Exception.class)
    public void getFailure() {
        // given
        stubFor(get(urlEqualTo(API_ENDPOINT))
            .willReturn(aResponse().withStatus(500).withBody("Internal error")));

        // when
        RestClient.create(String.format("%s%s", address, API_ENDPOINT)).get();

        // then
        // throw exception
    }

    @Test
    public void postSuccess() {
        // given
        stubFor(post(urlEqualTo(API_ENDPOINT))
            .withRequestBody(equalTo(BODY_REQUEST))
            .willReturn(aResponse().withStatus(200).withBody(BODY_RESPONSE)));

        // when
        String result = RestClient.create(String.format("%s%s", address, API_ENDPOINT))
            .withBody(BODY_REQUEST)
            .post();

        // then
        assertEquals(BODY_RESPONSE, result);
    }
}