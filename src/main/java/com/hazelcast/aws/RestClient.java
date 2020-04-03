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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for making REST calls.
 */
final class RestClient {
    private static final ILogger LOGGER = Logger.getLogger(RestClient.class);

    private static final int HTTP_OK = 200;

    private final String url;
    private final List<Parameter> headers = new ArrayList<>();
    private final List<Parameter> queryParams = new ArrayList<>();
    private String body;
    private int readTimeoutSeconds = 0; // infinite timeout
    private int connectTimeoutSeconds = 0; // infinite timeout

    private RestClient(String url) {
        this.url = url;
    }

    static RestClient create(String url) {
        return new RestClient(url);
    }

    RestClient withHeader(String key, String value) {
        headers.add(new Parameter(key, value));
        return this;
    }

    RestClient withQueryParam(String key, String value) {
        return this;
    }

    RestClient withBody(String body) {
        this.body = body;
        return this;
    }

    RestClient withReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
        return this;
    }

    RestClient withConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        return this;
    }

    String get() {
        return call("GET");
    }

    String post() {
        return call("POST");
    }

    private String call(String method) {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        try {
            URL urlToConnect = new URL(url);
            connection = (HttpURLConnection) urlToConnect.openConnection();
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(readTimeoutSeconds));
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(connectTimeoutSeconds));
            connection.setRequestMethod(method);
            for (Parameter header : headers) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (body != null) {
                byte[] bodyData = body.getBytes(StandardCharsets.UTF_8);

                connection.setDoOutput(true);
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Content-Length", Integer.toString(bodyData.length));

                outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.write(bodyData);
                outputStream.flush();
            }

            if (connection.getResponseCode() != HTTP_OK) {
                throw new RestClientException(String.format("Failure executing: %s at: %s. HTTP Response Code: %s, "
                        + "Message: \"%s\",", method, url, connection.getResponseCode(),
                    read(connection.getErrorStream())));
            }
            return read(connection.getInputStream());
        } catch (Exception e) {
            throw new RestClientException("Failure in executing REST call", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LOGGER.finest("Error while closing HTTP output stream", e);
                }
            }
        }
    }

    private static String read(InputStream stream) {
        if (stream == null) {
            return "";
        }
        Scanner scanner = new Scanner(stream, "UTF-8");
        scanner.useDelimiter("\\Z");
        return scanner.next();
    }

    private static final class Parameter {
        private final String key;
        private final String value;

        private Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        private String getKey() {
            return key;
        }

        private String getValue() {
            return value;
        }
    }

}