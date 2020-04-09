package com.hazelcast.aws;

import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonObject;

class AwsEcsMetadataApi {
    private final String endpoint;
    private final AwsConfig awsConfig;

    AwsEcsMetadataApi(String endpoint, AwsConfig awsConfig) {
        this.endpoint = endpoint;
        this.awsConfig = awsConfig;
    }

    EcsMetadata metadata() {
        String response = AwsUrlUtils.callAwsService(endpoint, awsConfig);
        return parse(response);
    }

    private EcsMetadata parse(String response) {
        JsonObject metadata = Json.parse(response).asObject();
        JsonObject labels = metadata.get("Labels").asObject();
        String clusterArn = labels.getString("com.amazonaws.ecs.cluster", null);
        String familyName = labels.getString("com.amazonaws.ecs.task-definition-family", null);
        return new EcsMetadata(clusterArn, familyName);
    }

    static class EcsMetadata {
        private final String clusterArn;
        private final String familyName;

        private EcsMetadata(String clusterArn, String familyName) {
            this.clusterArn = clusterArn;
            this.familyName = familyName;
        }

        String getClusterArn() {
            return clusterArn;
        }

        String getFamilyName() {
            return familyName;
        }
    }
}
