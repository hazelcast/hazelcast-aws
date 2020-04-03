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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.hazelcast.aws.AwsEc2RequestSigner.SIGNATURE_METHOD_V4;
import static com.hazelcast.aws.AwsUrlUtils.canonicalQueryString;
import static com.hazelcast.aws.StringUtil.isNotEmpty;
import static java.lang.String.format;

/**
 * Responsible for connecting to AWS EC2 Describe Instances API.
 *
 * @see <a href="http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html">EC2 Describe Instances</a>
 */
class AwsDescribeInstancesApi {
    private static final ILogger LOGGER = Logger.getLogger(AwsDescribeInstancesApi.class);

    private final AwsConfig awsConfig;
    private final AwsEc2RequestSigner requestSigner;
    private final Environment environment;

    AwsDescribeInstancesApi(AwsConfig awsConfig, AwsEc2RequestSigner requestSigner, Environment environment) {
        this.awsConfig = awsConfig;
        this.requestSigner = requestSigner;
        this.environment = environment;
    }

    /**
     * Invoke the service to describe the instances, unmarshal the response and return the discovered node map.
     * The map contains mappings from private to public IP and all contained nodes match the filtering rules defined by
     * the {@link AwsConfig}.
     *
     * @return map from private to public IP or empty map in case of failed response unmarshalling
     */
    Map<String, String> addresses(String region, String endpoint, AwsCredentials credentials) {
        Map<String, String> attributes = createAttributes(region, endpoint, credentials);
        String response = callServiceWithRetries(endpoint, attributes);
        return parse(response);
    }

    private Map<String, String> createAttributes(String region, String endpoint, AwsCredentials credentials) {
        Map<String, String> attributes = new HashMap<>();

        if (credentials.getToken() != null) {
            attributes.put("X-Amz-Security-Token", credentials.getToken());
        }
        attributes.put("Action", "DescribeInstances");
        attributes.put("Version", "2016-11-15");
        attributes.put("X-Amz-SignedHeaders", "host");
        attributes.put("X-Amz-Expires", "30");

        String timestamp = formatCurrentTimestamp();
        attributes.put("X-Amz-Date", timestamp);
        attributes.put("X-Amz-Credential", formatCredentials(region, credentials, timestamp));

        attributes.putAll(filterAttributes());
        attributes.put("X-Amz-Algorithm", SIGNATURE_METHOD_V4);
        attributes.put("X-Amz-Signature", requestSigner.sign(attributes, region, endpoint, credentials, timestamp));

        return attributes;
    }

    private String formatCurrentTimestamp() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(environment.date());
    }

    private static String formatCredentials(String region, AwsCredentials credentials, String timestamp) {
        return String.format("%s/%s/%s/ec2/aws4_request",
            credentials.getAccessKey(),
            timestamp.substring(0, 8),
            region);
    }

    private Map<String, String> filterAttributes() {
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

    private String callServiceWithRetries(String endpoint, Map<String, String> attributes) {
        return RetryUtils.retry(() -> callService(endpoint, attributes),
            awsConfig.getConnectionRetries());
    }

    private String callService(String endpoint, Map<String, String> attributes) {
        String query = canonicalQueryString(attributes);
        return RestClient.create(urlFor(endpoint, query))
            .withConnectTimeoutSeconds(awsConfig.getConnectionTimeoutSeconds())
            .withReadTimeoutSeconds(awsConfig.getReadTimeoutSeconds())
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
            LOGGER.warning(e);
        }
        return new HashMap<>();
    }

    private static Map<String, String> tryParse(String xmlResponse) throws Exception {
        XmlNode root = XmlNode.create(xmlResponse);

        Map<String, String> addresses = new HashMap<>();
        List<XmlNode> reservationSet = root.getSubNodes("reservationset");
        for (XmlNode reservation : reservationSet) {
            List<XmlNode> items = reservation.getSubNodes("item");
            for (XmlNode item : items) {
                XmlNode instancesSet = item.getFirstSubNode("instancesset");
                addresses.putAll(parseAddresses(instancesSet));
            }
        }
        return addresses;

    }

    private static Map<String, String> parseAddresses(XmlNode instancesSet) {
        Map<String, String> addresses = new HashMap<>();
        if (instancesSet == null) {
            return addresses;
        }

        for (XmlNode item : instancesSet.getSubNodes("item")) {
            String privateIp = item.getValue("privateipaddress");
            String publicIp = item.getValue("ipaddress");
            String instanceName = parseInstanceName(item);

            if (privateIp != null) {
                addresses.put(privateIp, publicIp);
                LOGGER.finest(format("Accepting EC2 instance [%s][%s]", instanceName, privateIp));
            }
        }
        return addresses;
    }

    private static String parseInstanceName(XmlNode nodeHolder) {
        XmlNode tagSetHolder = nodeHolder.getFirstSubNode("tagset");
        if (tagSetHolder.getNode() == null) {
            return null;
        }
        for (XmlNode itemHolder : tagSetHolder.getSubNodes("item")) {
            Node keyNode = itemHolder.getFirstSubNode("key").getNode();
            if (keyNode == null || keyNode.getFirstChild() == null) {
                continue;
            }
            String nodeValue = keyNode.getFirstChild().getNodeValue();
            if (!"Name".equals(nodeValue)) {
                continue;
            }

            Node valueNode = itemHolder.getFirstSubNode("value").getNode();
            if (valueNode == null || valueNode.getFirstChild() == null) {
                continue;
            }
            return valueNode.getFirstChild().getNodeValue();
        }
        return null;
    }
}
