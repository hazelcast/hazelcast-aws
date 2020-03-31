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

import com.hazelcast.aws.exception.AwsConnectionException;
import com.hazelcast.aws.utility.CloudyUtility;
import com.hazelcast.aws.utility.EC2RequestSigner;
import com.hazelcast.aws.utility.RetryUtils;
import com.hazelcast.aws.utility.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hazelcast.aws.Constants.DOC_VERSION;
import static com.hazelcast.aws.Constants.SIGNATURE_METHOD_V4;
import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;
import static com.hazelcast.internal.nio.IOUtil.closeResource;

/**
 * Responsible for connecting to AWS EC2 Describe Instances API.
 *
 * @see <a href="http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html">EC2 Describe Instances</a>
 */
public class AwsDescribeInstancesApi {
    /**
     * URI to fetch container credentials (when IAM role is enabled)
     * <p>
     * see http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html
     */
    private static final int MIN_HTTP_CODE_FOR_AWS_ERROR = 400;
    private static final int MAX_HTTP_CODE_FOR_AWS_ERROR = 600;
    private static final String UTF8_ENCODING = "UTF-8";

    private AwsConfig awsConfig;

    public AwsDescribeInstancesApi(AwsConfig awsConfig) {
        this.awsConfig = awsConfig;
    }

    /**
     * AWS response codes for client and server errors are specified here:
     * {@see http://docs.aws.amazon.com/AWSEC2/latest/APIReference/errors-overview.html}.
     */
    private static boolean isAwsError(int responseCode) {
        return responseCode >= MIN_HTTP_CODE_FOR_AWS_ERROR && responseCode < MAX_HTTP_CODE_FOR_AWS_ERROR;
    }

    private static String extractErrorMessage(HttpURLConnection httpConnection) {
        InputStream errorStream = httpConnection.getErrorStream();
        if (errorStream == null) {
            return "";
        }
        return readFrom(errorStream);
    }

    private static String readFrom(InputStream stream) {
        Scanner scanner = new Scanner(stream, UTF8_ENCODING).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * @param reader The reader that gives access to the JSON-formatted content that includes all the role information.
     * @return A map with all the parsed keys and values from the JSON content.
     * @throws IOException In case the input from reader cannot be correctly parsed.
     * @deprecated Since we moved JSON parsing from manual pattern matching to using
     * `com.hazelcast.com.eclipsesource.json.JsonObject`, this method should be deprecated.
     */
    @Deprecated
    public Map<String, String> parseIamRole(BufferedReader reader)
        throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        Pattern keyPattern = Pattern.compile("\"(.*?)\" : ");
        Pattern valuePattern = Pattern.compile(" : \"(.*?)\",");
        String line;
        for (line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.contains(":")) {
                Matcher keyMatcher = keyPattern.matcher(line);
                Matcher valueMatcher = valuePattern.matcher(line);
                if (keyMatcher.find() && valueMatcher.find()) {
                    String key = keyMatcher.group(1);
                    String value = valueMatcher.group(1);
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private String getFormattedTimestamp() {
        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(new Date());
    }

    /**
     * Add available filters to narrow down the scope of the query
     *
     * @param attributes
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

    /**
     * Invoke the service to describe the instances, unmarshal the response and return the discovered node map.
     * The map contains mappings from private to public IP and all contained nodes match the filtering rules defined by
     * the {@link #awsConfig}.
     *
     * @return map from private to public IP or empty map in case of failed response unmarshalling
     * @throws Exception if there is an exception invoking the service
     */
    public Map<String, String> addresses(String region, String endpoint, AwsCredentials credentials) {
        Map<String, String> attributes = new HashMap<>();
        fillAttributesWithTemporaryToken(attributes, credentials);

        String signature = getRequestSigner(attributes, region, endpoint, credentials).sign("ec2", attributes);
        Map<String, String> response;
        InputStream stream = null;
        attributes.put("X-Amz-Signature", signature);
        try {
            stream = callServiceWithRetries(attributes, region, endpoint, credentials);
            response = CloudyUtility.unmarshalTheResponse(stream);
            return response;
        } finally {
            closeResource(stream);
        }
    }

    private void fillAttributesWithTemporaryToken(Map<String, String> attributes, AwsCredentials credentials) {
        if (StringUtil.isNotEmpty(credentials.getToken())) {
            attributes.put("X-Amz-Security-Token", credentials.getToken());
        }
    }

    private InputStream callServiceWithRetries(Map<String, String> attributes, String region, String endpoint,
                                               AwsCredentials credentials) {
        return RetryUtils.retry(() -> callService(attributes, region, endpoint, credentials),
            awsConfig.getConnectionRetries());
    }

    // visible for testing
    InputStream callService(Map<String, String> attributes, String region, String endpoint, AwsCredentials credentials)
        throws Exception {
        String query =
            getRequestSigner(attributes, region, endpoint, credentials).getCanonicalizedQueryString(attributes);
        URL url = new URL("https", endpoint, -1, "/?" + query);

        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        httpConnection.setRequestMethod(Constants.GET);
        httpConnection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(awsConfig.getReadTimeoutSeconds()));
        httpConnection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(awsConfig.getConnectionTimeoutSeconds()));
        httpConnection.setDoOutput(false);
        httpConnection.connect();

        checkNoAwsErrors(httpConnection);

        return httpConnection.getInputStream();
    }

    // visible for testing
    void checkNoAwsErrors(HttpURLConnection httpConnection)
        throws IOException {
        int responseCode = httpConnection.getResponseCode();
        if (isAwsError(responseCode)) {
            String errorMessage = extractErrorMessage(httpConnection);
            throw new AwsConnectionException(responseCode, errorMessage);
        }
    }

    private EC2RequestSigner getRequestSigner(Map<String, String> attributes, String region, String endpoint, AwsCredentials credentials) {
        String timeStamp = getFormattedTimestamp();
        EC2RequestSigner rs = new EC2RequestSigner(timeStamp, region, endpoint, credentials);
        attributes.put("Action", "DescribeInstances");
        attributes.put("Version", DOC_VERSION);
        attributes.put("X-Amz-Algorithm", SIGNATURE_METHOD_V4);
        attributes.put("X-Amz-Credential", rs.createFormattedCredential());
        attributes.put("X-Amz-Date", timeStamp);
        attributes.put("X-Amz-SignedHeaders", "host");
        attributes.put("X-Amz-Expires", "30");
        addFilters(attributes);
        return rs;
    }
}
