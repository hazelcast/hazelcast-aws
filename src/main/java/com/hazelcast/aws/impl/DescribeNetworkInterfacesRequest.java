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

import java.util.Collection;
import java.util.Map;

/**
 * EC2 DescribeNetworkInterfaces operation request.
 * See <a href="https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeNetworkInterfaces.html">EC2 documentation</a>.
 */
public class DescribeNetworkInterfacesRequest extends AwsRequest<Map<String, String>> {

    /**
     * @param taskAddresses private IP addresses as <code>Collection&lt;String&gt;
     */
    public DescribeNetworkInterfacesRequest(Collection<String> taskAddresses) {
        super(MarshallingUtils::unmarshalDescribeNetworkInterfacesResponse);

        getAttributes().put("Action", "DescribeNetworkInterfaces");

        if (taskAddresses != null) {
            Filter filter = new Filter();
            filter.addMultiValuedFilter("addresses.private-ip-address", taskAddresses);
            getAttributes().putAll(filter.getFilters());
        }
    }
}
