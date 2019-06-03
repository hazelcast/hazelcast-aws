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

import com.hazelcast.aws.AwsOperation;
import com.hazelcast.aws.utility.MarshallingUtils;

import java.util.Collection;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.EC2_DOC_VERSION;

/**
 * EC2 DescribeNetworkInterfaces operation.
 * See <a href="https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeNetworkInterfaces.html">EC2 documentation</a>.
 */
public class DescribeNetworkInterfacesOperation extends AwsOperation<Map<String, String>> {

    /**
     * @param taskAddresses private IP addresses as <code>Collection&lt;String&gt;
     */
    public DescribeNetworkInterfacesOperation(Collection<String> taskAddresses) {
        super(MarshallingUtils::unmarshalDescribeNetworkInterfacesResponse);

        getAttributes().put("Action", "DescribeNetworkInterfaces");
        getAttributes().put("Version", EC2_DOC_VERSION);

        if (taskAddresses != null) {
            Filter filter = new Filter();
            filter.addMultiValuedFilter("addresses.private-ip-address", taskAddresses);
            getAttributes().putAll(filter.getFilters());
        }
    }
}
