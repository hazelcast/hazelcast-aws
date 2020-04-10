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

public class AwsEcsClient {
    private static final ILogger LOGGER = Logger.getLogger(AwsClient.class);

    private final AwsEcsMetadataApi awsEcsMetadataApi;
    private final AwsEcsApi awsEcsApi;
    private final AwsDescribeNetworkInterfacesApi awsDescribeNetworkInterfacesApi;
    private final String clusterArn;
    private final String familyName;
    private final String region;
    private final AwsConfig awsConfig;

    public AwsEcsClient(AwsEcsMetadataApi awsEcsMetadataApi, AwsEcsApi awsEcsApi,
                        AwsDescribeNetworkInterfacesApi awsDescribeNetworkInterfacesApi, AwsConfig awsConfig) {
        this.awsEcsMetadataApi = awsEcsMetadataApi;
        this.awsEcsApi = awsEcsApi;
        this.awsDescribeNetworkInterfacesApi = awsDescribeNetworkInterfacesApi;
        this.awsConfig = awsConfig;

        // TODO: Add config parameters
        LOGGER.info("Retrieving data from ECS Metadata service");
        EcsMetadata metadata = awsEcsMetadataApi.metadata();
        this.clusterArn = metadata.getClusterArn();
        this.familyName = metadata.getFamilyName();
        this.region = retrieveRegion();

        LOGGER.info(String.format("AWS ECS Discovery: {cluster : '%s', family : '%s'}", clusterArn, familyName));
    }

    private String retrieveRegion() {
        // TODO: Use Metadata service to retrieve region
        return "eu-central-1";
    }

    Map<String, String> getAddresses() {
        LOGGER.info("Discovering Addresses from ECS");

        LOGGER.info("Retrieving AWS Credentials from ECS");
        AwsCredentials credentials = retrieveCredentials();

        LOGGER.info(String.format("Listing tasks from {cluster: '%s', family: '%s'}", clusterArn, familyName));
        List<String> tasks = awsEcsApi.listTasks(clusterArn, familyName, region, credentials);
        LOGGER.info(String.format("Found the following tasks: %s", tasks));

        if (!tasks.isEmpty()) {
            List<String> privateAddresses = awsEcsApi.describeTasks(clusterArn, tasks, region, credentials);
            LOGGER.info(String.format("Found the following private addresses: %s", privateAddresses));

            Map<String, String> privateToPublicAddresses = fetchPublicAddresses(privateAddresses, credentials);
            LOGGER.info(String.format("The following (private, public) addresses found: %s", privateToPublicAddresses));
            return privateToPublicAddresses;
        }
        return emptyMap();
    }

    /**
     * Fetches private addresses for the tasks.
     * <p>
     * Note that this is done as best-effort and does not fail if no public addresses are not found, because:
     * <ul>
     * <li>Task may not have public IP addresses</li>
     * <li>Task may not have access rights to query for public addresses</li>
     * </ul>
     */
    private Map<String, String> fetchPublicAddresses(List<String> privateAddresses, AwsCredentials credentials) {
        return awsDescribeNetworkInterfacesApi.publicAddresses(privateAddresses, region, credentials);
    }

    // TODO: Improve in the context of AwsMetadataApi
    private AwsCredentials retrieveCredentials() {
        String uri = "http://169.254.170.2" + System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI");
        String response = callAwsService(uri, awsConfig);
        return parseCredentials(response);
    }

    private static AwsCredentials parseCredentials(String response) {
        JsonObject role = Json.parse(response).asObject();
        return AwsCredentials.builder()
            .setAccessKey(role.getString("AccessKeyId", null))
            .setSecretKey(role.getString("SecretAccessKey", null))
            .setToken(role.getString("Token", null))
            .build();
    }

    String getAvailabilityZone() {
        // TODO: Return fetching availability zone
        return "unknown";
    }
}
