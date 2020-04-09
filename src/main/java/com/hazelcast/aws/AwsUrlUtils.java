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

import com.hazelcast.core.HazelcastException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Utility class to for AWS Requests.
 */
final class AwsUrlUtils {

    private AwsUrlUtils() {
    }

    static String formatCurrentTimestamp(Clock clock) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(Instant.now(clock).toEpochMilli());
    }

    static String callAwsService(String url, AwsConfig awsConfig) {
        return RetryUtils.retry(() -> RestClient.create(url)
                .withConnectTimeoutSeconds(awsConfig.getConnectionTimeoutSeconds())
                .withReadTimeoutSeconds(awsConfig.getReadTimeoutSeconds())
                .get()
            , awsConfig.getConnectionRetries());
    }

    static String canonicalQueryString(Map<String, String> attributes) {
        List<String> components = getListOfEntries(attributes);
        Collections.sort(components);
        return canonicalQueryString(components);
    }

    private static List<String> getListOfEntries(Map<String, String> entries) {
        List<String> components = new ArrayList<>();
        for (String key : entries.keySet()) {
            addComponents(components, entries, key);
        }
        return components;
    }

    private static String canonicalQueryString(List<String> list) {
        Iterator<String> it = list.iterator();
        StringBuilder result = new StringBuilder(it.next());
        while (it.hasNext()) {
            result.append('&').append(it.next());
        }
        return result.toString();
    }

    private static void addComponents(List<String> components, Map<String, String> attributes, String key) {
        components.add(urlEncode(key) + '=' + urlEncode(attributes.get(key)));
    }

    private static String urlEncode(String string) {
        String encoded;
        try {
            encoded = URLEncoder.encode(string, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new HazelcastException(e);
        }
        return encoded;
    }
}
