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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.hazelcast.aws.AwsEc2RequestSigner.SIGNATURE_METHOD_V4;
import static com.hazelcast.aws.CloudyUtility.getCanonicalizedQueryString;
import static com.hazelcast.aws.StringUtil.isNotEmpty;

/**
 * Responsible for connecting to AWS EC2 Describe Instances API.
 *
 * @see <a href="http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html">EC2 Describe Instances</a>
 */
class AwsDescribeInstancesApi {

    private final AwsConfig awsConfig;
    private final AwsEc2RequestSigner requestSigner;
    private final Calendar calendar;

    AwsDescribeInstancesApi(AwsConfig awsConfig, AwsEc2RequestSigner requestSigner, Calendar calendar) {
        this.awsConfig = awsConfig;
        this.requestSigner = requestSigner;
        this.calendar = calendar;
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
        String response = callServiceWithRetries(attributes, endpoint);
        return CloudyUtility.parseResponse(response);
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

        addFilters(attributes);
        attributes.put("X-Amz-Algorithm", SIGNATURE_METHOD_V4);
        attributes.put("X-Amz-Signature", requestSigner.sign(attributes, region, endpoint, credentials, timestamp));

        return attributes;
    }

    private static String formatCredentials(String region, AwsCredentials credentials, String timestamp) {
        return String.format("%s/%s/%s/ec2/aws4_request",
            credentials.getAccessKey(),
            timestamp.substring(0, 8),
            region);
    }


    private String formatCurrentTimestamp() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = calendar.getTime();
        // TODO: Change to custom time provider, date should be retrieved here, not at the beginning!
        return df.format(date);
    }

    /**
     * Add available filters to narrow down the scope of the query
     */
    private void addFilters(Map<String, String> attributes) {
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
        attributes.putAll(filter.getFilters());
    }

    private String callServiceWithRetries(Map<String, String> attributes, String endpoint) {
        return RetryUtils.retry(() -> callService(attributes, endpoint),
            awsConfig.getConnectionRetries());
    }

    private String callService(Map<String, String> attributes, String endpoint) {
        String query = getCanonicalizedQueryString(attributes);
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
}
