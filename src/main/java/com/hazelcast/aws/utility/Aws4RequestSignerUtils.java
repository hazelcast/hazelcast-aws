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

package com.hazelcast.aws.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.SIGNATURE_METHOD_V4;

/**
 * Static utility class for Aws signature version 4
 */
public final class Aws4RequestSignerUtils {

    private Aws4RequestSignerUtils() {
    }

    private static String getCanonicalizedQueryString(List<String> list) {
        Iterator<String> it = list.iterator();
        StringBuilder result = new StringBuilder();
        if (it.hasNext()) {
            result.append(it.next());
        }
        while (it.hasNext()) {
            result.append('&').append(it.next());
        }
        return result.toString();
    }

    private static void addComponents(List<String> components, Map<String, String> attributes, String key) {
        components.add(AwsURLEncoder.urlEncode(key) + '=' + AwsURLEncoder.urlEncode(attributes.get(key)));
    }

    private static List<String> getListOfEntries(Map<String, String> entries) {
        List<String> components = new ArrayList<String>();
        for (String key : entries.keySet()) {
            addComponents(components, entries, key);
        }
        return components;
    }

    public static String getCanonicalizedQueryString(Map<String, String> attributes) {
        List<String> components = getListOfEntries(attributes);
        Collections.sort(components);
        return getCanonicalizedQueryString(components);
    }

    public static String buildAuthHeader(String accessKey, String credentialScope, String signedHeaders, String signature) {
        return SIGNATURE_METHOD_V4 + " " + "Credential=" + accessKey + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
    }
}
