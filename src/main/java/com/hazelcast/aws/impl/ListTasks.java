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

package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.aws.impl.Constants.DOC_VERSION;
import static com.hazelcast.aws.impl.Constants.ECS_DOC_VERSION;

/**
 * See http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html
 * for AWS API details.
 */
public class ListTasks extends AwsOperation<Collection<String>> {

    public ListTasks(AwsConfig awsConfig, String endpoint) {
        super(awsConfig, endpoint, "ecs", ECS_DOC_VERSION);
    }

    //Just for testing purposes
    ListTasks(AwsConfig awsConfig) {
        this(awsConfig, null);
    }

    // visible for testing
    @Override
    InputStream callService()
            throws Exception {
        String query = getRequestSigner().getCanonicalizedQueryString(attributes);
        URL url = new URL("https", endpoint, -1, "/?" + query);

        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        httpConnection.setRequestMethod(Constants.POST);
        httpConnection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(awsConfig.getConnectionTimeoutSeconds()));
        httpConnection.setDoOutput(true); // FIXME
        httpConnection.setRequestProperty("Content-Type", "application/x-amz-json-1.1");
        httpConnection.setRequestProperty("X-Amz-Target", "AmazonEC2ContainerServiceV20141113.ListTasks");
        httpConnection.setRequestProperty("Accept-Encoding", "identity");

        httpConnection.connect();
        OutputStream outputStream = httpConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        writer.write("{}");
        writer.flush();
        writer.close();
        checkNoAwsErrors(httpConnection);

        return httpConnection.getInputStream();
    }

    @Override
    Collection<String> unmarshal(InputStream stream) {
        // TODO
        return null;
    }

}
