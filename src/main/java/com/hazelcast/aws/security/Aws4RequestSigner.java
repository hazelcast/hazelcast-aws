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

package com.hazelcast.aws.security;

import java.util.Map;

/**
 * AWS request signer implementing signature version 4 algorithm
 * See https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
 */
public interface Aws4RequestSigner {

    /**
     * Signs a GET request with empty body
     * @param attributes query parameters
     * @param headers request headers
     * @return signature
     */
    String sign(Map<String, String> attributes, Map<String, String> headers);

    /**
     * Signs a request
     * @param attributes query parameters
     * @param headers request headers
     * @param body request body
     * @param httpMethod HTTP method, e.g., "GET" or "POST"
     * @return signature
     */
    String sign(Map<String, String> attributes, Map<String, String> headers, String body, String httpMethod);

    /**
     * @return the complete <code>Authorization</code> header for the generated signature
     */
    String getAuthorizationHeader();

    /**
     * @return the request timestamp
     */
    String getTimestamp();
}
