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

import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonArray;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.internal.json.JsonValue;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.hazelcast.aws.AwsUrlUtils.createRestClient;
import static com.hazelcast.aws.AwsUrlUtils.currentTimestamp;
import static com.hazelcast.aws.AwsUrlUtils.urlFor;
import static java.util.Collections.emptyMap;

/**
 * Responsible for connecting to AWS ECS API.
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonECS/latest/APIReference/Welcome.html">AWS ECS API</a>
 */
class AwsEcsApi {
    private final String endpoint;
    private final AwsConfig awsConfig;
    private final AwsRequestSigner requestSigner;
    private final Clock clock;

    AwsEcsApi(String endpoint, AwsConfig awsConfig, AwsRequestSigner requestSigner, Clock clock) {
        this.endpoint = endpoint;
        this.awsConfig = awsConfig;
        this.requestSigner = requestSigner;
        this.clock = clock;
    }

    List<String> listTasks(String clusterArn, String familyName, AwsCredentials credentials) {
        String body = createBodyListTasks(clusterArn, familyName);
        Map<String, String> headers = createHeadersListTasks(body, credentials);
        String response = callAwsService(body, headers);
        return parseListTasks(response);
    }

    private String createBodyListTasks(String clusterArn, String familyName) {
        JsonObject body = new JsonObject();
        body.add("cluster", clusterArn);
        body.add("family", familyName);
        return body.toString();
    }

    private Map<String, String> createHeadersListTasks(String body, AwsCredentials credentials) {
        return createHeaders(body, credentials, "ListTasks");
    }

    private List<String> parseListTasks(String response) {
        return toStream(toJson(response).get("taskArns"))
            .map(JsonValue::asString)
            .collect(Collectors.toList());
    }

    List<String> describeTasks(String clusterArn, List<String> tasks, AwsCredentials credentials) {
        String body = createBodyDescribeTasks(clusterArn, tasks);
        Map<String, String> headers = createHeadersDescribeTasks(body, credentials);
        String response = callAwsService(body, headers);
        return parseDescribeTasks(response);
    }

    private String createBodyDescribeTasks(String cluster, List<String> tasks) {
        JsonArray jsonArray = new JsonArray();
        tasks.stream().map(Json::value).forEach(jsonArray::add);
        return new JsonObject()
            .add("tasks", jsonArray)
            .add("cluster", cluster)
            .toString();
    }

    private Map<String, String> createHeadersDescribeTasks(String body, AwsCredentials credentials) {
        return createHeaders(body, credentials, "DescribeTasks");
    }

    private List<String> parseDescribeTasks(String response) {
        return toStream(toJson(response).get("tasks"))
            .flatMap(e -> toStream(e.asObject().get("containers")))
            .flatMap(e -> toStream(e.asObject().get("networkInterfaces")))
            .map(e -> e.asObject().get("privateIpv4Address").asString())
            .collect(Collectors.toList());
    }

    private Map<String, String> createHeaders(String body, AwsCredentials credentials,
                                              String awsTargetAction) {
        Map<String, String> headers = new HashMap<>();

        if (credentials.getToken() != null) {
            headers.put("X-Amz-Security-Token", credentials.getToken());
        }
        headers.put("X-Amz-Target", String.format("AmazonEC2ContainerServiceV20141113.%s", awsTargetAction));
        headers.put("Content-Type", "application/x-amz-json-1.1");
        headers.put("Accept-Encoding", "identity");
        String timestamp = currentTimestamp(clock);
        headers.put("X-Amz-Date", timestamp);
        headers.put("Authorization", requestSigner.authHeader(emptyMap(), headers, credentials, timestamp, body, "POST"));

        return headers;
    }

    private String callAwsService(String body, Map<String, String> headers) {
        return createRestClient(urlFor(endpoint), awsConfig)
            .withHeaders(headers)
            .withBody(body)
            .post();
    }

    private static JsonObject toJson(String jsonString) {
        return Json.parse(jsonString).asObject();
    }

    private static Stream<JsonValue> toStream(JsonValue json) {
        return StreamSupport.stream(json.asArray().spliterator(), false);
    }
}
