package com.hazelcast.aws;

import com.hazelcast.aws.AwsEcsMetadataApi.EcsMetadata;
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.util.List;
import java.util.Map;

import static com.hazelcast.aws.AwsUrlUtils.callAwsService;
import static java.util.Collections.emptyMap;

class AwsEcsClient implements AwsClient {
    private static final ILogger LOGGER = Logger.getLogger(AwsClient.class);

    private final AwsEcsMetadataApi awsEcsMetadataApi;
    private final AwsEcsApi awsEcsApi;
    private final AwsEc2Api awsEc2Api;
    private final String clusterArn;
    private final String familyName;
    private final AwsAuthenticator awsAuthenticator;

    AwsEcsClient(AwsEcsMetadataApi awsEcsMetadataApi, AwsEcsApi awsEcsApi,
                 AwsEc2Api awsEc2Api, AwsAuthenticator awsAuthenticator) {
        this.awsEcsMetadataApi = awsEcsMetadataApi;
        this.awsEcsApi = awsEcsApi;
        this.awsEc2Api = awsEc2Api;
        this.awsAuthenticator = awsAuthenticator;

        // TODO: Add config parameters
        LOGGER.info("Retrieving data from ECS Metadata service");
        EcsMetadata metadata = awsEcsMetadataApi.metadata();
        this.clusterArn = metadata.getClusterArn();
        this.familyName = metadata.getFamilyName();

        LOGGER.info(String.format("AWS ECS Discovery: {cluster : '%s', family : '%s'}", clusterArn, familyName));
    }

    @Override
    public Map<String, String> getAddresses() {
        LOGGER.info("Discovering Addresses from ECS");

        LOGGER.info("Retrieving AWS Credentials from ECS");
        AwsCredentials credentials = awsAuthenticator.credentials();

        LOGGER.info(String.format("Listing tasks from {cluster: '%s', family: '%s'}", clusterArn, familyName));
        List<String> tasks = awsEcsApi.listTasks(clusterArn, familyName, credentials);
        LOGGER.info(String.format("Found the following tasks: %s", tasks));

        if (!tasks.isEmpty()) {
            List<String> privateAddresses = awsEcsApi.describeTasks(clusterArn, tasks, credentials);
            LOGGER.info(String.format("Found the following private describeInstances: %s", privateAddresses));

            Map<String, String> privateToPublicAddresses = fetchPublicAddresses(privateAddresses, credentials);
            LOGGER.info(String.format("The following (private, public) describeInstances found: %s", privateToPublicAddresses));
            return privateToPublicAddresses;
        }
        return emptyMap();
    }

    /**
     * Fetches private describeInstances for the tasks.
     * <p>
     * Note that this is done as best-effort and does not fail if no public describeInstances are not found, because:
     * <ul>
     * <li>Task may not have public IP describeInstances</li>
     * <li>Task may not have access rights to query for public describeInstances</li>
     * </ul>
     */
    private Map<String, String> fetchPublicAddresses(List<String> privateAddresses, AwsCredentials credentials) {
        return awsEc2Api.describeNetworkInterfaces(privateAddresses, credentials);
    }

    @Override
    public String getAvailabilityZone() {
        // TODO: Return fetching availability zone
        return "unknown";
    }
}
