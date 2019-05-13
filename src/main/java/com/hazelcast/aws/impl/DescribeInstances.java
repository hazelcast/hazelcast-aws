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

import static com.hazelcast.aws.impl.Constants.GET;
import static com.hazelcast.aws.impl.Constants.HTTPS;
import static com.hazelcast.aws.utility.StringUtils.isNotEmpty;

/**
 * EC2 DescribeInstances operation.
 * See http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html
 * for AWS API details.
 */
public class DescribeInstances extends Ec2Operation<Map<String, String>> {

    public DescribeInstances(AwsConfig awsConfig, URL endpointURL) {
        super(awsConfig, endpointURL, GET);
        attributes.put("Action", this.getClass().getSimpleName());
        attributes.put("Version", this.docVersion);
    }

    // Visible for testing
    public DescribeInstances(AwsConfig awsConfig, String endpoint) throws MalformedURLException {
        this(awsConfig, new URL(HTTPS, endpoint, -1, "/"));
    }

    // Visible for testing
    DescribeInstances(AwsConfig awsConfig) throws MalformedURLException {
        this(awsConfig, awsConfig.getHostHeader());
    }

    // Visible for testing
    @Override
    Map<String, String> unmarshal(InputStream stream) {
        return Ec2XmlUtils.unmarshalDescribeInstancesResponse(stream);
    }

    /**
     * Add available filters to narrow down the scope of the query
     */
    @Override
    public void prepareHttpRequest(Object... args) {
        Filter filter = new Filter();
        if (isNotEmpty(awsConfig.getTagKey())) {
            if (isNotEmpty(awsConfig.getTagValue())) {
                filter.addFilter("tag:" + awsConfig.getTagKey(), awsConfig.getTagValue());
            } else {
                filter.addFilter("tag-key", awsConfig.getTagKey());
            }
        } else if (isNotEmpty(awsConfig.getTagValue())) {
            filter.addFilter("tag-value", awsConfig.getTagValue());
        }

        if (isNotEmpty(awsConfig.getSecurityGroupName())) {
            filter.addFilter("instance.group-name", awsConfig.getSecurityGroupName());
        }

        filter.addFilter("instance-state-name", "running");
        attributes.putAll(filter.getFilters());
    }
}
