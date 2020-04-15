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

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import org.w3c.dom.Node;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hazelcast.aws.AwsUrlUtils.canonicalQueryString;
import static com.hazelcast.aws.AwsUrlUtils.createRestClient;
import static com.hazelcast.aws.AwsUrlUtils.currentTimestamp;
import static com.hazelcast.aws.StringUtils.isNotEmpty;

/**
 * Responsible for connecting to AWS EC2 API.
 *
 * @see <a href="https://docs.aws.amazon.com/AWSEC2/latest/APIReference/Welcome.html">AWS EC2 API</a>
 */
class AwsEc2Api {
    private static final ILogger LOGGER = Logger.getLogger(AwsEc2Api.class);

    private final String endpoint;
    private final AwsConfig awsConfig;
    private final AwsRequestSigner requestSigner;
    private final Clock clock;

    AwsEc2Api(String endpoint, AwsConfig awsConfig, AwsRequestSigner requestSigner, Clock clock) {
        this.endpoint = endpoint;
        this.awsConfig = awsConfig;
        this.requestSigner = requestSigner;
        this.clock = clock;
    }

    /**
     * Calls AWS EC2 Describe Instances API, parses the response, and returns mapping from private to public IPs.
     * <p>
     * Note that if EC2 Instance does not have a public IP, then an entry (private-ip, null) is returned.
     *
     * @return map from private to public IP
     * @see <a href="http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html">EC2 Describe Instances</a>
     */
    Map<String, String> describeInstances(AwsCredentials credentials) {
        Map<String, String> attributes = createAttributesDescribeInstances();
        Map<String, String> headers = createHeaders(attributes, credentials);
        String response = callAwsService(attributes, headers);
        return parseDescribeInstances(response);
    }

    private Map<String, String> createAttributesDescribeInstances() {
        Map<String, String> attributes = createSharedAttributes();
        attributes.put("Action", "DescribeInstances");
        attributes.putAll(filterAttributesDescribeInstances());
        return attributes;
    }

    private Map<String, String> filterAttributesDescribeInstances() {
        Filter filter = new Filter();
        if (isNotEmpty(awsConfig.getTagKey())) {
            if (isNotEmpty(awsConfig.getTagValue())) {
                filter.addFilter("tag:" + awsConfig.getTagKey(), awsConfig.getTagValue());
            } else {
                filter.addFilter("tag-key", awsConfig.getTagKey());
            }
        } else if (isNotEmpty(awsConfig.getTagValue())) {
            filter.addFilter("tag-value", awsConfig.getTagValue());
        }

        if (isNotEmpty(awsConfig.getSecurityGroupName())) {
            filter.addFilter("instance.group-name", awsConfig.getSecurityGroupName());
        }

        filter.addFilter("instance-state-name", "running");
        return filter.getFilterAttributes();
    }

    private static Map<String, String> parseDescribeInstances(String xmlResponse) {
        Map<String, String> result = new HashMap<>();
        XmlNode.create(xmlResponse)
            .getSubNodes("reservationset").stream()
            .flatMap(e -> e.getSubNodes("item").stream())
            .flatMap(e -> e.getSubNodes("instancesset").stream())
            .flatMap(e -> e.getSubNodes("item").stream())
            .filter(e -> e.getValue("privateipaddress") != null)
            .peek(AwsEc2Api::logInstanceName)
            .forEach(e -> result.put(e.getValue("privateipaddress"), e.getValue("ipaddress")));
        return result;
    }

    private static void logInstanceName(XmlNode item) {
        LOGGER.fine(String.format("Accepting EC2 instance [%s][%s]",
            parseInstanceName(item).orElse("<unknown>"),
            item.getValue("privateipaddress")));
    }

    private static Optional<String> parseInstanceName(XmlNode nodeHolder) {
        return nodeHolder.getSubNodes("tagset").stream()
            .flatMap(e -> e.getSubNodes("item").stream())
            .filter(AwsEc2Api::isNameField)
            .flatMap(e -> e.getSubNodes("value").stream())
            .map(XmlNode::getNode)
            .map(Node::getFirstChild)
            .map(Node::getNodeValue)
            .findFirst();
    }

    private static boolean isNameField(XmlNode item) {
        return item.getSubNodes("key").stream()
            .map(XmlNode::getNode)
            .map(Node::getFirstChild)
            .map(Node::getNodeValue)
            .map("Name"::equals)
            .findFirst()
            .orElse(false);
    }

    /**
     * Calls AWS EC2 Describe Network Interfaces API, parses the response, and returns mapping from private to public
     * IPs.
     * <p>
     * Note that if the given private IP does not have a public IP association, then an entry (private-ip, null)
     * is returned.
     *
     * @return map from private to public IP
     * @see <a href="http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeNetworkInterfaces.html">EC2 Describe Network Interfaces</a>
     */
    Map<String, String> describeNetworkInterfaces(List<String> privateAddresses, AwsCredentials credentials) {
        Map<String, String> attributes = createAttributesDescribeNetworkInterfaces(privateAddresses);
        Map<String, String> headers = createHeaders(attributes, credentials);
        String response = callAwsService(attributes, headers);
        return parseDescribeNetworkInterfaces(response);
    }

    private Map<String, String> createAttributesDescribeNetworkInterfaces(List<String> privateAddresses) {
        Map<String, String> attributes = createSharedAttributes();
        attributes.put("Action", "DescribeNetworkInterfaces");
        attributes.putAll(filterAttributesDescribeNetworkInterfaces(privateAddresses));
        return attributes;
    }

    private Map<String, String> filterAttributesDescribeNetworkInterfaces(List<String> privateAddresses) {
        Filter filter = new Filter();
        filter.addMultiValuedFilter("addresses.private-ip-address", privateAddresses);
        return filter.getFilterAttributes();
    }

    private static Map<String, String> parseDescribeNetworkInterfaces(String xmlResponse) {
        Map<String, String> result = new HashMap<>();
        XmlNode.create(xmlResponse)
            .getSubNodes("networkinterfaceset").stream()
            .flatMap(e -> e.getSubNodes("item").stream())
            .filter(e -> e.getValue("privateipaddress") != null)
            .forEach(e -> result.put(
                e.getValue("privateipaddress"),
                e.getSubNodes("association").stream()
                    .map(a -> a.getValue("publicip"))
                    .findFirst()
                    .orElse(null)
            ));
        return result;
    }

    private static Map<String, String> createSharedAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Version", "2016-11-15");
        return attributes;
    }

    private Map<String, String> createHeaders(Map<String, String> attributes, AwsCredentials credentials) {
        Map<String, String> headers = new HashMap<>();

        if (credentials.getToken() != null) {
            headers.put("X-Amz-Security-Token", credentials.getToken());
        }
        String timestamp = currentTimestamp(clock);
        headers.put("X-Amz-Date", timestamp);
        headers.put("Authorization", requestSigner.authHeader(attributes, headers, "", credentials, timestamp, "GET"));

        return headers;
    }

    private String callAwsService(Map<String, String> attributes, Map<String, String> headers) {
        String query = canonicalQueryString(attributes);
        return createRestClient(urlFor(endpoint, query), awsConfig)
            .withHeaders(headers)
            .get();
    }

    private static String urlFor(String endpoint, String query) {
        return AwsUrlUtils.urlFor(endpoint) + "/?" + query;
    }
}
