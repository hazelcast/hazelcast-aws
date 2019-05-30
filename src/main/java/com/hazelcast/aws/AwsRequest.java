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

package com.hazelcast.aws;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract request to an AWS service;
 * has a body, headers and query parameters (attributes);
 * is able to unmarshal a response from a response stream using a specified AwsResponseUnmarshaller.
 */
public abstract class AwsRequest<T> {

    private final Map<String, String> attributes = new HashMap<String, String>();
    private final Map<String, String> headers = new HashMap<String, String>();
    private String body = "";
    private AwsResponseUnmarshaller<T> unmarshaller;

    public AwsRequest(AwsResponseUnmarshaller<T> unmarshaller) {
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
