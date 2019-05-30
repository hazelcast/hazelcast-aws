/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.AwsRequest;
import com.hazelcast.aws.exception.AwsConnectionException;
import com.hazelcast.aws.security.Aws4RequestSigner;
import com.hazelcast.aws.security.Aws4RequestSignerImpl;
import com.hazelcast.aws.security.AwsCredentials;
import com.hazelcast.aws.utility.Aws4RequestSignerUtils;
import com.hazelcast.aws.utility.Environment;
import com.hazelcast.aws.utility.MetadataUtils;
import com.hazelcast.aws.utility.RetryUtils;
import com.hazelcast.aws.utility.StringUtils;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.aws.impl.Constants.UTF8_ENCODING;
import static com.hazelcast.aws.utility.StringUtils.isEmpty;
import static com.hazelcast.aws.utility.StringUtils.isNotEmpty;
import static com.hazelcast.nio.IOUtil.closeResource;

/**
 * Abstract base class for invoking an AWS service.
 * Used by AwsClientStrategy implementations for calling AWS service endpoints.
 */
public abstract class AwsOperationClient {

    /**
     * URI to fetch container credentials (when IAM role is enabled)
     * <p>
     * see http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html
     */
    static final String IAM_TASK_ROLE_ENDPOINT = "http://169.254.170.2";

    private static final ILogger LOGGER = Logger.getLogger(AwsOperationClient.class);
    private static final int MIN_HTTP_CODE_FOR_AWS_ERROR = 400;
    private static final int MAX_HTTP_CODE_FOR_AWS_ERROR = 600;

    private final AwsConfig awsConfig;
    private final AwsCredentials awsCredentials;

    private final URL endpointURL;
    private final String service;
    private final String httpMethod;

    /**
     * Creates an AwsOperationClient
     * @param awsConfig configuration
     * @param endpointURL endpoint URL
     * @param service AWS service name, e.g., "ec2"
     * @param httpMethod HTTP method name to use for this operation, e.g., "POST"
     */
    AwsOperationClient(AwsConfig awsConfig, URL endpointURL, String service, String httpMethod) {
        this.awsConfig = awsConfig;
        this.awsCredentials = new AwsCredentials(awsConfig);
        this.endpointURL = endpointURL;
        this.service = service;
        this.httpMethod = httpMethod;
    }

    /**
     * Sumbits a service request with retry logic. In case of success, unmarshals the response and returns it.
     *
     * @param awsRequest service request
     * @return the response
     */
    public <R> R execute(AwsRequest<R> awsRequest) {
        // authorization
        final Map<String, String> enrichedHeaders = authorizeRequest(awsRequest);

        // service invocation
        InputStream stream = null;
        try {
            LOGGER.finest("Calling service at " + endpointURL.toExternalForm());
            stream = callServiceWithRetries(awsRequest.getAttributes(), enrichedHeaders, awsRequest.getBody());
            return awsRequest.unmarshalResponse(stream);
        } finally {
            closeResource(stream);
        }
    }

    /**
     * Retrieve awsCredentials from configuration or from default IAM role in the current environment
     */
    protected abstract void retrieveCredentials();

    private <R> Map<String, String> authorizeRequest(AwsRequest<R> awsRequest) {
        if (isNotEmpty(awsCredentials.getIamRole()) || isEmpty(awsCredentials.getAccessKey())) {
            retrieveCredentials();
        }

        final Map<String, String> enrichedHeaders = new HashMap<String, String>();
        enrichedHeaders.putAll(awsRequest.getHeaders());

        Aws4RequestSigner requestSigner = getRequestSigner();
        requestSigner.sign(awsRequest.getAttributes(), enrichedHeaders, awsRequest.getBody(), httpMethod);
        enrichedHeaders.put("Authorization", requestSigner.getAuthorizationHeader());
        enrichedHeaders.put("X-Amz-Date", requestSigner.getTimestamp());
        enrichedHeaders.put("Host", endpointURL.getHost());


        String securityToken = awsCredentials.getSecurityToken();
        if (StringUtils.isNotEmpty(securityToken)) {
            enrichedHeaders.put("X-Amz-Security-Token", securityToken);
        }
        return enrichedHeaders;
    }

    // Visible for testing
    AwsCredentials getAwsCredentials() {
        return awsCredentials;
    }

    /**
     * AWS response codes for client and server errors are specified here:
     * {@see http://docs.aws.amazon.com/AWSEC2/latest/APIReference/errors-overview.html}.
     */
    private static boolean isAwsError(int responseCode) {
        return responseCode >= MIN_HTTP_CODE_FOR_AWS_ERROR && responseCode < MAX_HTTP_CODE_FOR_AWS_ERROR;
    }

    protected AwsConfig getAwsConfig() {
        return awsConfig;
    }

    private static String readFrom(InputStream stream) {
        Scanner scanner = new Scanner(stream, UTF8_ENCODING).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * Helper method for retrieving IAM task role credentials when running on ECS
     */
    void retrieveContainerCredentials() {
        String uri = getEnvironment().getEnvVar(Constants.ECS_CONTAINER_CREDENTIALS_ENV_VAR_NAME);
        if (uri == null) {
            throw new IllegalArgumentException("Could not acquire credentials! "
                    + "Did not find declared AWS access key or IAM Role, and could not discover IAM Task Role or default role.");
        }
        uri = IAM_TASK_ROLE_ENDPOINT + uri;

        String json = null;
        try {
            json = retrieveRoleFromURI(uri);
            parseAndStoreRoleCreds(json);
        } catch (Exception io) {
            throw new InvalidConfigurationException(
                    "Unable to retrieve credentials from IAM Task Role. " + "URI: " + uri
                            + ". \nHTTP Response content: " + json, io);
        }
    }

    /**
     * This is a helper method that simply performs the HTTP request to retrieve the role, from a given URI.
     * (It allows us to cleanly separate the network calls out of our main code logic, so we can mock in our UT.)
     *
     * Visible for testing.
     *
     * @param uri the full URI where a `GET` request will retrieve the role information, represented as JSON.
     * @return The content of the HTTP response, as a String. NOTE: This is NEVER null.
     */
    String retrieveRoleFromURI(String uri) {
        return MetadataUtils
                .retrieveMetadataFromURI(uri, awsConfig.getConnectionTimeoutSeconds(), awsConfig.getConnectionRetries());
    }

    /**
     * This helper method is responsible for just parsing the content of the HTTP response and
     * storing the access keys and token it finds there.
     *
     * @param json The JSON representation of the IAM (Task) Role.
     */
    void parseAndStoreRoleCreds(String json) {
        JsonObject roleAsJson = Json.parse(json).asObject();
        awsCredentials.setAccessKey(roleAsJson.getString("AccessKeyId", null));
        awsCredentials.setSecretKey(roleAsJson.getString("SecretAccessKey", null));
        awsCredentials.setSecurityToken(roleAsJson.getString("Token", null));
        if (LOGGER.isFinestEnabled()) {
            LOGGER.finest("In parseAndStoreRoleCreds: credentials are: " + awsCredentials.toString());
        }
    }

    /**
     * Calls service with retry-logic
     *
     * @return the response <code>InputStream</code>
     * @param attributes
     * @param headers
     * @param body
     */
    private InputStream callServiceWithRetries(Map<String, String> attributes, Map<String, String> headers, String body) {
        return RetryUtils.retry(new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
                return callService(attributes, headers, body);
            }
        }, awsConfig.getConnectionRetries());
    }

    /**
     * Issues the actual service call and checks for error response codes.
     * Visible for testing.
     *
     * @return input stream for server response
     * @throws Exception in case of networking or service errors
     * @param attributes
     * @param headers
     * @param body
     */
    InputStream callService(Map<String, String> attributes, Map<String, String> headers, String body) throws Exception {
        String query = Aws4RequestSignerUtils.getCanonicalizedQueryString(attributes);
        String spec = "/" + (isNotEmpty(query) ? "?" + query : "");
        URL url = new URL(endpointURL, spec);

        LOGGER.finest(String.format("URL:%s", url.toExternalForm()));
        LOGGER.finest(String.format("Attributes:%s", attributes.toString()));
        LOGGER.finest(String.format("Headers:%s", headers.toString()));
        LOGGER.finest(String.format("Body:%s", body));
        LOGGER.finest(String.format("Body-length:%d", body.length()));

        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        httpConnection.setRequestMethod(httpMethod);
        httpConnection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(awsConfig.getConnectionTimeoutSeconds()));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        if (isNotEmpty(body)) {
            httpConnection.setDoOutput(true);
        }

        httpConnection.connect();

        if (isNotEmpty(body)) {
            OutputStream outputStream = httpConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, UTF8_ENCODING));
            writer.write(body);
            writer.flush();
            writer.close();
        }
        checkNoAwsErrors(httpConnection);

        return httpConnection.getInputStream();
    }

    // Visible for testing
    void checkNoAwsErrors(HttpURLConnection httpConnection)
            throws IOException {
        int responseCode = httpConnection.getResponseCode();
        if (isAwsError(responseCode)) {
            InputStream errorStream = httpConnection.getErrorStream();
            String errorMessage = (errorStream == null) ? "" : readFrom(errorStream);
            throw new AwsConnectionException(responseCode, errorMessage);
        }
    }

    // Added for testing (mocking) purposes.
    // Visible for testing
    Environment getEnvironment() {
        return new Environment();
    }

    private Aws4RequestSigner getRequestSigner() {
        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeStamp = df.format(new Date());
        Aws4RequestSigner rs = new Aws4RequestSignerImpl(awsConfig, awsCredentials, timeStamp, service, endpointURL.getHost());
        return rs;
    }
}
