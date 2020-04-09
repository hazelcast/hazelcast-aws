package com.hazelcast.aws;

import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.internal.json.JsonValue;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.hazelcast.aws.AwsUrlUtils.formatCurrentTimestamp;
import static java.util.Collections.emptyMap;

class AwsEcsApi {
    private final String endpoint;
    private final AwsConfig awsConfig;
    private final AwsEc2RequestSigner requestSigner;
    private final Clock clock;

    AwsEcsApi(String endpoint, AwsConfig awsConfig, AwsEc2RequestSigner requestSigner, Clock clock) {
        this.endpoint = endpoint;
        this.awsConfig = awsConfig;
        this.requestSigner = requestSigner;
        this.clock = clock;
    }

    List<String> listTasks(String clusterArn, String familyName, String region, AwsCredentials credentials) {
        Map<String, String> headers = createHeaders(region, credentials);
        String body = createBody(clusterArn, familyName);
        String response = callServiceWithRetries(headers, body);
        return parse(response);
    }

    private Map<String, String> createHeaders(String region, AwsCredentials credentials) {
        Map<String, String> headers = new HashMap<>();

        headers.put("X-Amz-Target", "AmazonEC2ContainerServiceV20141113.ListTasks");
        headers.put("Content-Type", "application/x-amz-json-1.1");
        headers.put("Accept-Encoding", "identity");
        String timestamp = formatCurrentTimestamp(clock);
        headers.put("X-Amz-Date", timestamp);
        // TODO: Is it needed?
        headers.put("Host", host());
        headers.put("Authorization", requestSigner.authenticationHeader(emptyMap(), headers, region, endpoint,
            credentials, timestamp));

        // TODO: Shouldn't it be at the beginning of this method?
        if (credentials.getToken() != null) {
            headers.put("X-Amz-Security-Token", credentials.getToken());
        }

        return headers;
    }

    private String host() {
        try {
            return new URL(endpoint).getHost();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(String.format("Wrong endpoint: %s", endpoint), e);
        }
    }

    private String createBody(String clusterArn, String familyName) {
        JsonObject body = new JsonObject();
        body.add("cluster", clusterArn);
        body.add("family", familyName);
        return body.toString();
    }

    private String callServiceWithRetries(Map<String, String> headers, String body) {
        return RetryUtils.retry(() -> callService(headers, body), awsConfig.getConnectionRetries());
    }

    private String callService(Map<String, String> headers, String body) {
        return RestClient.create(endpoint)
            .withHeaders(headers)
            .withBody(body)
            .post();
    }

    private List<String> parse(String response) {
        JsonObject tasks = Json.parse(response).asObject();
        return StreamSupport.stream(tasks.get("taskArns").asArray().spliterator(), false)
            .map(JsonValue::asString)
            .collect(Collectors.toList());
    }
}
