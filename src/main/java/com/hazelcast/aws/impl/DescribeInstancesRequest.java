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

package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsRequest;
import com.hazelcast.aws.utility.MarshallingUtils;

import java.util.Map;

import static com.hazelcast.aws.utility.StringUtils.isNotEmpty;

/**
 * EC2 DescribeInstances operation request.
 * See <a href="https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html">EC2 documentation</a>.
 */
public class DescribeInstancesRequest extends AwsRequest<Map<String, String>> {

    public DescribeInstancesRequest(String tagKey, String tagValue, String securityGroupName) {
        super(MarshallingUtils::unmarshalDescribeInstancesResponse);

        getAttributes().put("Action", "DescribeInstances");

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
