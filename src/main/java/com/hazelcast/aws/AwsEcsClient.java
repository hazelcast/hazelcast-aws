package com.hazelcast.aws;

import com.hazelcast.aws.AwsMetadataApi.EcsMetadata;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

class AwsEcsClient implements AwsClient {
    private static final ILogger LOGGER = Logger.getLogger(AwsClient.class);

    private final AwsEcsApi awsEcsApi;
    private final AwsEc2Api awsEc2Api;
    private final AwsCredentialsProvider awsCredentialsProvider;
    private final String clusterArn;
    private final String familyName;

    AwsEcsClient(AwsEcsApi awsEcsApi, AwsEc2Api awsEc2Api, AwsMetadataApi awsMetadataApi, AwsCredentialsProvider awsCredentialsProvider) {
        this.awsEcsApi = awsEcsApi;
        this.awsEc2Api = awsEc2Api;
        this.awsCredentialsProvider = awsCredentialsProvider;

        EcsMetadata metadata = awsMetadataApi.metadataEcs();
        this.clusterArn = metadata.getClusterArn();
        this.familyName = metadata.getFamilyName();
    }

    @Override
    public Map<String, String> getAddresses() {
        AwsCredentials credentials = awsCredentialsProvider.credentials();

        LOGGER.fine(String.format("Listing tasks from {cluster: '%s', family: '%s'}", clusterArn, familyName));
        List<String> tasks = awsEcsApi.listTasks(clusterArn, familyName, credentials);
        LOGGER.fine(String.format("AWS ECS ListTasks found the following tasks: %s", tasks));

        if (!tasks.isEmpty()) {
            List<String> privateAddresses = awsEcsApi.describeTasks(clusterArn, tasks, credentials);
            LOGGER.fine(String.format("AWS ECS DescribeTasks found the following addresses: %s", privateAddresses));

            Map<String, String> privateToPublicAddresses = fetchPublicAddresses(privateAddresses, credentials);
            LOGGER.fine(String.format("AWS EC2 DescribeNetworkInterfaces found the following (private, public) addresses: %s", privateToPublicAddresses));
            return privateToPublicAddresses;
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
            LOGGER.info("Cannot fetch public IPs of ECS Tasks, only private addresses are used. If you need to access"
                + " Hazelcast with public IP, please check if your Task has IAM role which allows querying EC2 API");
            LOGGER.fine(e);

            Map<String, String> map = new HashMap<>();
            privateAddresses.forEach(k -> map.put(k, null));
            return map;
        }
    }

    @Override
    public String getAvailabilityZone() {
        // TODO: Return fetching availability zone
        return "unknown";
    }
}
