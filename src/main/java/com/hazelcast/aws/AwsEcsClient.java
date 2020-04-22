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

import com.hazelcast.aws.AwsEcsApi.Task;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

class AwsEcsClient implements AwsClient {
    private static final ILogger LOGGER = Logger.getLogger(AwsClient.class);

    private final AwsEcsApi awsEcsApi;
    private final AwsEc2Api awsEc2Api;
    private final AwsMetadataApi awsMetadataApi;
    private final AwsCredentialsProvider awsCredentialsProvider;
    private final String cluster;

    AwsEcsClient(String cluster, AwsEcsApi awsEcsApi, AwsEc2Api awsEc2Api, AwsMetadataApi awsMetadataApi,
                 AwsCredentialsProvider awsCredentialsProvider) {
        this.cluster = cluster;
        this.awsEcsApi = awsEcsApi;
        this.awsEc2Api = awsEc2Api;
        this.awsMetadataApi = awsMetadataApi;
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    @Override
    public Map<String, String> getAddresses() {
        AwsCredentials credentials = awsCredentialsProvider.credentials();

        LOGGER.fine(String.format("Listing tasks from cluster: '%s'", cluster));
        List<String> taskArns = awsEcsApi.listTasks(cluster, credentials);
        LOGGER.fine(String.format("AWS ECS ListTasks found the following tasks: %s", taskArns));

        if (!taskArns.isEmpty()) {
            List<Task> tasks = awsEcsApi.describeTasks(cluster, taskArns, credentials);
            List<String> taskAddresses = tasks.stream().map(Task::getPrivateAddress).collect(Collectors.toList());
            LOGGER.fine(String.format("AWS ECS DescribeTasks found the following addresses: %s", taskAddresses));

            return fetchPublicAddresses(taskAddresses, credentials);
        }
        return emptyMap();
    }

    /**
     * Fetches private addresses for the tasks.
     * <p>
     * Note that this is done as best-effort and does not fail if no public describeInstances are not found, because:
     * <ul>
     * <li>Task may not have public IP addresses</li>
     * <li>Task may not have access rights to query for public addresses</li>
     * </ul>
     */
    private Map<String, String> fetchPublicAddresses(List<String> privateAddresses, AwsCredentials credentials) {
        try {
            return awsEc2Api.describeNetworkInterfaces(privateAddresses, credentials);
        } catch (Exception e) {
            LOGGER.fine("Cannot fetch public IPs of ECS Tasks, only private addresses are used. If you need to access"
                + " Hazelcast with public IP, please check if your Task has IAM role which allows querying EC2 API", e);

            Map<String, String> map = new HashMap<>();
            privateAddresses.forEach(k -> map.put(k, null));
            return map;
        }
    }

    @Override
    public String getAvailabilityZone() {
        String taskArn = awsMetadataApi.metadataEcs().getTaskArn();
        AwsCredentials credentials = awsCredentialsProvider.credentials();
        List<Task> tasks = awsEcsApi.describeTasks(cluster, singletonList(taskArn), credentials);
        return tasks.stream()
            .map(Task::getAvailabilityZone)
            .findFirst()
            .orElse("unknown");
    }
}
