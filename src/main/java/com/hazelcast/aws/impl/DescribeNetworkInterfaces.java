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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.EC2;
import static com.hazelcast.aws.impl.Constants.EC2_DOC_VERSION;
import static com.hazelcast.aws.impl.Constants.GET;
import static com.hazelcast.aws.impl.Constants.HTTPS;

/**
 * See http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html
 * for AWS API details.
 */
public class DescribeNetworkInterfaces extends Ec2Operation<Map<String, String>> {

    public DescribeNetworkInterfaces(AwsConfig awsConfig, URL endpointURL) {
        super(awsConfig, endpointURL, EC2, EC2_DOC_VERSION, GET);
        attributes.put("Action", this.getClass().getSimpleName());
        attributes.put("Version", EC2_DOC_VERSION);
    }

    //Just for testing purposes
    public DescribeNetworkInterfaces(AwsConfig awsConfig, String endpoint) throws MalformedURLException {
        this(awsConfig, new URL(HTTPS, endpoint, -1, "/"));
    }

    //Just for testing purposes
    // FIXME REMOVE
    DescribeNetworkInterfaces(AwsConfig awsConfig) throws MalformedURLException {
        this(awsConfig, awsConfig.getHostHeader());
    }

    // visible for testing
    @Override
    Map<String, String> unmarshal(InputStream stream) {
        return Ec2XmlUtils.unmarshalDescribeNetworkInterfacesResponse(stream);
    }

    /**
     * TODO
     */
    @Override
    public void prepareHttpRequest(Object... args) {
        Filter filter = new Filter();
        if (args.length > 0) {
            Map<String, String> taskAddresses = (Map<String, String>) args[0];
            filter.addFilter("addresses.private-ip-address", taskAddresses.keySet());
        }
        attributes.putAll(filter.getFilters());
    }
}
