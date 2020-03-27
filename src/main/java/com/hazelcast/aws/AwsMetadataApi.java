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

import com.hazelcast.aws.utility.RetryUtils;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for connecting to AWS EC2 Instance Metadata API.
 *
 * @see <a href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html">EC2 Instance Metatadata</a>
 */
public final class AwsMetadataApi {
    public static final String METADATA_ENDPOINT = "http://169.254.169.254/latest/meta-data/";

    private final String endpoint;

    /**
     * Post-fix URI to fetch IAM role details
     */
    public static final String IAM_SECURITY_CREDENTIALS_URI = "iam/security-credentials/";

    /**
     * Post-fix URI to fetch availability-zone info.
     */
    private static final String AVAILABILITY_ZONE_URI = "placement/availability-zone/";

    private static final ILogger LOGGER = Logger.getLogger(AwsMetadataApi.class);

    public AwsMetadataApi() {
        this.endpoint = METADATA_ENDPOINT;
    }

    /**
     * For test purposes only.
     */
    AwsMetadataApi(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Performs the HTTP request to retrieve AWS Instance Metadata from the given URI.
     *
     * @param uri                     the full URI where a `GET` request will retrieve the metadata information, represented as JSON.
     * @param connectTimeoutInSeconds connect timeout for the AWS service call
     * @param readTimeoutSeconds      read timeout for the AWS service call
     * @return The content of the HTTP response, as a String. NOTE: This is NEVER null.
     */
    private String retrieveMetadataFromURI(String uri, int connectTimeoutInSeconds, int readTimeoutSeconds) {
        StringBuilder response = new StringBuilder();

        InputStreamReader is = null;
        BufferedReader reader = null;
        try {
            URLConnection url = new URL(uri).openConnection();
            url.setReadTimeout((int) TimeUnit.SECONDS.toMillis(readTimeoutSeconds));
            url.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(connectTimeoutInSeconds));
            is = new InputStreamReader(url.getInputStream(), "UTF-8");
            reader = new BufferedReader(is);
            String resp;
            while ((resp = reader.readLine()) != null) {
                response = response.append(resp);
            }
            return response.toString();
        } catch (IOException io) {
            throw new InvalidConfigurationException("Unable to lookup role in URI: " + uri, io);
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

    /**
     * Performs the HTTP request to retrieve AWS Instance Metadata from the given URI.
     *
     * @param uri                     the full URI where a `GET` request will retrieve the metadata information, represented as JSON.
     * @param connectTimeoutInSeconds connect timeout for the AWS service call
     * @param retries                 number of retries in case the AWS request fails
     * @param readTimeoutInSeconds    read timeout for the AWS service call
     * @return The content of the HTTP response, as a String. NOTE: This is NEVER null.
     */
    public String retrieveMetadataFromURI(final String uri, final int connectTimeoutInSeconds,
                                          final int retries, final int readTimeoutInSeconds) {
        return RetryUtils.retry(() -> retrieveMetadataFromURI(uri, connectTimeoutInSeconds, readTimeoutInSeconds), retries);
    }

    String getAvailabilityZone(int connectionTimeoutSeconds, int connectionRetries, int readTimeoutSeconds) {
        String uri = endpoint.concat(AVAILABILITY_ZONE_URI);
        return retrieveMetadataFromURI(uri, connectionTimeoutSeconds, connectionRetries, readTimeoutSeconds);
    }
}
