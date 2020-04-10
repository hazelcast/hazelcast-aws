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

import static com.hazelcast.aws.AwsUrlUtils.formatCurrentTimestamp;
import static com.hazelcast.aws.AwsUrlUtils.hostFor;
import static com.hazelcast.aws.AwsUrlUtils.urlFor;
import static java.util.Collections.emptyMap;

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
        String response = callServiceWithRetries(body, headers);
        return parseListTasks(response);
    }

    private Map<String, String> createHeadersListTasks(String body, AwsCredentials credentials) {
        return createHeaders(body, credentials, "ListTasks");
    }

    private String createBodyListTasks(String clusterArn, String familyName) {
        JsonObject body = new JsonObject();
        body.add("cluster", clusterArn);
        body.add("family", familyName);
        return body.toString();
    }

    private List<String> parseListTasks(String response) {
        return toStream(toJson(response).get("taskArns"))
            .map(JsonValue::asString)
            .collect(Collectors.toList());
    }

    List<String> describeTasks(String cluster, List<String> tasks, AwsCredentials credentials) {
        String body = createBodyDescribeTasks(cluster, tasks);
        Map<String, String> headers = createHeadersDescribeTasks(body, credentials);
        String response = callServiceWithRetries(body, headers);
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
        String timestamp = formatCurrentTimestamp(clock);
        headers.put("X-Amz-Date", timestamp);
        // TODO: Is it needed?
        headers.put("Host", hostFor(endpoint));
        headers.put("Authorization", requestSigner.authenticationHeader(emptyMap(), headers, credentials, timestamp,
            body, "POST"));

        return headers;
    }

    private String callServiceWithRetries(String body, Map<String, String> headers) {
        return RetryUtils.retry(() -> callService(body, headers), awsConfig.getConnectionRetries());
    }

    private String callService(String body, Map<String, String> headers) {
        return RestClient.create(urlFor(endpoint))
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
