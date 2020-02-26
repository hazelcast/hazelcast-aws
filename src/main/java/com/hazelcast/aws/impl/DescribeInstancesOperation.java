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

package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsOperation;
import com.hazelcast.aws.utility.MarshallingUtils;

import java.util.Map;

import static com.hazelcast.aws.impl.Constants.EC2_DOC_VERSION;
import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;

/**
 * EC2 DescribeInstances operation.
 * See <a href="https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html">EC2 documentation</a>.
 */
public class DescribeInstancesOperation extends AwsOperation<Map<String, String>> {

    public DescribeInstancesOperation(String tagKey, String tagValue, String securityGroupName) {
        super(MarshallingUtils::unmarshalDescribeInstancesResponse);

        getAttributes().put("Action", "DescribeInstances");
        getAttributes().put("Version", EC2_DOC_VERSION);

        /* Prepares the request using filters from config to narrow down the scope of the query */
        Filter filter = new Filter();
        if (isNotEmpty(tagKey)) {
            if (isNotEmpty(tagValue)) {
                filter.addFilter("tag:" + tagKey, tagValue);
            } else {
                filter.addFilter("tag-key", tagKey);
            }
        } else if (isNotEmpty(tagValue)) {
            filter.addFilter("tag-value", tagValue);
        }

        if (isNotEmpty(securityGroupName)) {
            filter.addFilter("instance.group-name", securityGroupName);
        }
        filter.addFilter("instance-state-name", "running");

        getAttributes().putAll(filter.getFilters());
    }
}
