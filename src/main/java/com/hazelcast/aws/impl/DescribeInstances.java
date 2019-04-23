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
import com.hazelcast.aws.utility.Ec2XmlUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.aws.impl.Constants.EC2_DOC_VERSION;

/**
 * See http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html
 * for AWS API details.
 */
public class DescribeInstances extends AwsOperation<Map<String, String>> {

    public DescribeInstances(AwsConfig awsConfig, URL endpointURL) {
        super(awsConfig, endpointURL, "ec2", EC2_DOC_VERSION);
    }

    //Just for testing purposes
    public DescribeInstances(AwsConfig awsConfig, String endpoint) throws MalformedURLException {
        super(awsConfig, new URL("https", endpoint, -1, "/"), "ec2", EC2_DOC_VERSION);
    }

    //Just for testing purposes
    DescribeInstances(AwsConfig awsConfig) {
        this(awsConfig, (URL) null);
    }

    // visible for testing
    @Override
    InputStream callService() throws Exception {
        String query = getRequestSigner().getCanonicalizedQueryString(attributes);
        URL url = new URL(endpointURL, "/?" + query);

        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        httpConnection.setRequestMethod(Constants.GET);
        httpConnection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(awsConfig.getConnectionTimeoutSeconds()));
        httpConnection.setDoOutput(false);
        httpConnection.connect();

        checkNoAwsErrors(httpConnection);

        return httpConnection.getInputStream();
    }

    @Override
    Map<String, String> unmarshal(InputStream stream) {
        return Ec2XmlUtils.unmarshalTheResponse(stream);
    }

}
