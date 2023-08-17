/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.aws.utility;

import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public final class MetadataUtil {

    /**
     * This IP is only accessible inside AWS and is used to fetch metadata of running EC2 Instance.
     * Outside connection is only possible with the keys.
     * See details at http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html.
     */
    public static final String INSTANCE_METADATA_URI = "http://169.254.169.254/latest/meta-data/";
    public static final String INSTANCE_METADATA_TOKEN_URI = "http://169.254.169.254/latest/api/token";
    private static final Long TOKEN_LIFETIME_SECONDS = 21600L;

    /**
     * Post-fix URI to fetch IAM role details
     */
    public static final String IAM_SECURITY_CREDENTIALS_URI = "iam/security-credentials/";

    /**
     * Post-fix URI to fetch availability-zone info.
     */
    public static final String AVAILABILITY_ZONE_URI = "placement/availability-zone/";

    private static final ILogger LOGGER = Logger.getLogger(MetadataUtil.class);
    private static String imdsV2Token = null;
    private static long tokenLastFetchMillis = 0;    

    private MetadataUtil() {
    }


    public static String retrieveMetadataFromURI(String uri, int timeoutInSeconds) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(timeoutInSeconds));
            connection.setRequestProperty("X-aws-ec2-metadata-token", getImdsV2Token(timeoutInSeconds));

            return retrieveFromConnection(connection);
        } catch (IOException io) {
            throw new InvalidConfigurationException("Unable to lookup role in URI: " + uri, io);
        }
    }

    /**
     * Performs the HTTP request to retrieve AWS Instance Metadata from the given URI.
     *
     * @param uri              the full URI where a `GET` request will retrieve the metadata information, represented as JSON.
     * @param timeoutInSeconds timeout for the AWS service call
     * @return The content of the HTTP response, as a String. NOTE: This is NEVER null.
     */
    public static String retrieveFromConnection(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();

        InputStreamReader is = null;
        BufferedReader reader = null;
        try {
            is = new InputStreamReader(connection.getInputStream(), "UTF-8");
            reader = new BufferedReader(is);
            String resp;
            while ((resp = reader.readLine()) != null) {
                response = response.append(resp);
            }
            return response.toString();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.warning(e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.warning(e);
                }
            }
        }
    }

    private static synchronized String getImdsV2Token(int timeoutInSeconds) {
        if (imdsV2Token == null
            || System.currentTimeMillis() - tokenLastFetchMillis >= TOKEN_LIFETIME_SECONDS * 1000) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(INSTANCE_METADATA_TOKEN_URI).openConnection();
                    connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(timeoutInSeconds));
                    connection.setRequestMethod("PUT");
                    connection.setRequestProperty(
                        "X-aws-ec2-metadata-token-ttl-seconds", TOKEN_LIFETIME_SECONDS.toString());

                    imdsV2Token = retrieveFromConnection(connection);
                    tokenLastFetchMillis = System.currentTimeMillis();
                } catch (IOException io) {
                    throw new InvalidConfigurationException("Unable get new token for IMDSv2", io);
                }
        }
        return imdsV2Token;
    }

    /**
     * Performs the HTTP request to retrieve AWS Instance Metadata from the given URI.
     *
     * @param uri              the full URI where a `GET` request will retrieve the metadata information, represented as JSON.
     * @param timeoutInSeconds timeout for the AWS service call
     * @param retries          number of retries in case the AWS request fails
     * @return The content of the HTTP response, as a String. NOTE: This is NEVER null.
     */
    public static String retrieveMetadataFromURI(final String uri, final int timeoutInSeconds, int retries) {
        return RetryUtils.retry(new Callable<String>() {
            @Override
            public String call() {
                return retrieveMetadataFromURI(uri, timeoutInSeconds);
            }
        }, retries);
    }
}
