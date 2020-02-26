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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * An instance of an operation call to an AWS service;
 * has request body, headers and query parameters (attributes);
 * is able to unmarshal a response from a response stream using a specified AwsResponseUnmarshaller.
 */
public abstract class AwsOperation<T> {

    private final Map<String, String> attributes = new HashMap<String, String>();
    private final Map<String, String> headers = new HashMap<String, String>();
    private String body = "";
    private AwsResponseUnmarshaller<T> unmarshaller;

    public AwsOperation(AwsResponseUnmarshaller<T> unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    protected void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public T unmarshalResponse(InputStream stream) {
        return unmarshaller.unmarshal(stream);
    }
}
