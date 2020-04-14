package com.hazelcast.aws;

import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import static com.hazelcast.aws.Environment.isRunningOnEcs;

class AwsAuthenticator {
    private static final ILogger LOGGER = Logger.getLogger(AwsAuthenticator.class);

    private static final String ECS_CREDENTIALS_ENV_VAR_NAME = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";

    private final AwsEc2MetadataApi awsEc2MetadataApi;
    private final AwsConfig awsConfig;
    private final Environment environment;
    private final String iamRole;

    AwsAuthenticator(AwsEc2MetadataApi awsEc2MetadataApi, AwsConfig awsConfig, Environment environment) {
        this.awsEc2MetadataApi = awsEc2MetadataApi;
        this.awsConfig = awsConfig;
        this.environment = environment;
        this.iamRole = resolveIamRole();

    }

    private String resolveIamRole() {
        if (StringUtils.isNotEmpty(awsConfig.getAccessKey())) {
            // no need to resolve IAM Role, since using hardcoded Access/Secret keys takes precedence
            return null;
        }
        if (isRunningOnEcs()) {
            // ECS has only one role assigned, so no need to resolve it here
            return null;
        }
        if (StringUtils.isNotEmpty(awsConfig.getIamRole()) && !"DEFAULT".equals(awsConfig.getIamRole())) {
            return awsConfig.getIamRole();
        }

        String iamRole = awsEc2MetadataApi.defaultIamRole();
        LOGGER.info(String.format("Using IAM Role attached to EC2 Instance: %s", iamRole));
        return iamRole;
    }

    AwsCredentials credentials() {
        if (StringUtils.isNotEmpty(awsConfig.getAccessKey())) {
            // authenticate using access key and secret key from the configuration
            return AwsCredentials.builder()
                .setAccessKey(awsConfig.getAccessKey())
                .setSecretKey(awsConfig.getSecretKey())
                .build();
        }

        if (StringUtils.isNotEmpty(iamRole)) {
            // authenticate using IAM Role
            LOGGER.fine(String.format("Fetching credentials using IAM Role: %s", iamRole));
            try {
                return awsEc2MetadataApi.credentials(iamRole);
            } catch (Exception e) {
                throw new InvalidConfigurationException("Unable to retrieve credentials from IAM Role: "
                    + awsConfig.getIamRole(), e);
            }
        }

        return fetchCredentialsFromEcs();
    }

    private AwsCredentials fetchCredentialsFromEcs() {
        // before giving up, attempt to discover whether we're running in an ECS Container,
        // in which case, AWS_CONTAINER_CREDENTIALS_RELATIVE_URI will exist as an env var.
        String relativePath = environment.getEnv(ECS_CREDENTIALS_ENV_VAR_NAME);
        if (relativePath == null) {
            throw new InvalidConfigurationException("Could not acquire credentials! "
                + "Did not find declared AWS access key or IAM Role, and could not discover IAM Task Role or default role.");
        }
        try {
            return awsEc2MetadataApi.credentialsFromEcs(relativePath);
        } catch (Exception e) {
            throw new InvalidConfigurationException(
                "Unable to retrieve credentials from IAM Task Role. " + "URI: " + relativePath);
        }
    }
}
