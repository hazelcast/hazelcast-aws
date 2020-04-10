package com.hazelcast.aws;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hazelcast.aws.AwsUrlUtils.canonicalQueryString;
import static com.hazelcast.aws.AwsUrlUtils.formatCurrentTimestamp;
import static com.hazelcast.aws.AwsUrlUtils.hostFor;
import static java.util.Collections.emptyMap;

class AwsDescribeNetworkInterfacesApi {

    private final String endpoint;
    private final AwsConfig awsConfig;
    private final AwsEc2RequestSigner requestSigner;
    private final Clock clock;


    AwsDescribeNetworkInterfacesApi(String endpoint, AwsConfig awsConfig, AwsEc2RequestSigner requestSigner,
                                    Clock clock) {
        this.endpoint = endpoint;
        this.awsConfig = awsConfig;
        this.requestSigner = requestSigner;
        this.clock = clock;
    }

    Map<String, String> publicAddresses(List<String> privateAddresses, String region, AwsCredentials credentials) {
        Map<String, String> attributes = createAttributes(privateAddresses);
        Map<String, String> headers = createHeaders(attributes, region, credentials);
        String response = callServiceWithRetries(attributes, headers);
        return parse(response);
    }

    private Map<String, String> createAttributes(List<String> privateAddresses) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Action", "DescribeNetworkInterfaces");
        attributes.put("Version", "2016-11-15");

        Filter filter = new Filter();
        filter.addMultiValuedFilter("addresses.private-ip-address", privateAddresses);
        attributes.putAll(filter.getFilterAttributes());

        return attributes;
    }

    private Map<String, String> createHeaders(Map<String, String> attributes, String region, AwsCredentials credentials) {
        Map<String, String> headers = new HashMap<>();
        if (credentials.getToken() != null) {
            headers.put("X-Amz-Security-Token", credentials.getToken());
        }

        String timestamp = formatCurrentTimestamp(clock);
        headers.put("X-Amz-Date", timestamp);
        // TODO: Is it needed?
        headers.put("Host", hostFor(endpoint));
        headers.put("Authorization", requestSigner.authenticationHeader(attributes, headers, region, endpoint,
            credentials, timestamp, "", "GET"));

        return headers;
    }

    private String callServiceWithRetries(Map<String, String> attributes, Map<String, String> headers) {
        return RetryUtils.retry(() -> callService(attributes, headers), awsConfig.getConnectionRetries());
    }

    private String callService(Map<String, String> attributes, Map<String, String> headers) {
        String query = canonicalQueryString(attributes);
        return RestClient.create(urlFor(endpoint, query))
            .withHeaders(headers)
            .get();
    }

    private static String urlFor(String endpoint, String query) {
        if (endpoint.startsWith("http")) {
            return endpoint + "/?" + query;
        }
        return "https://" + endpoint + "/?" + query;
    }

    private static Map<String, String> parse(String xmlResponse) {
        try {
            return tryParse(xmlResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> tryParse(String xmlResponse) throws Exception {
        return XmlNode.create(xmlResponse)
            .getSubNodes("networkinterfaceset").stream()
            .flatMap(e1 -> e1.getSubNodes("item").stream())
            .collect(Collectors.toMap(
                e -> e.getValue("privateipaddress"),
                e ->
                    e.getSubNodes("association").stream()
                        .map(a -> a.getValue("publicip"))
                        .findFirst()
                        .orElse(null)
            ));
    }
}
