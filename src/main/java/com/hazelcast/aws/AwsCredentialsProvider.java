package com.hazelcast.aws;

import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import static com.hazelcast.aws.Environment.isRunningOnEcs;

class AwsCredentialsProvider {
    private static final ILogger LOGGER = Logger.getLogger(AwsCredentialsProvider.class);

    private static final String ECS_CREDENTIALS_ENV_VAR_NAME = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";

    private final AwsEc2MetadataApi awsEc2MetadataApi;
    private final AwsConfig awsConfig;
    private final Environment environment;
    private final String ec2IamRole;

    AwsCredentialsProvider(AwsEc2MetadataApi awsEc2MetadataApi, AwsConfig awsConfig, Environment environment) {
        this.awsEc2MetadataApi = awsEc2MetadataApi;
        this.awsConfig = awsConfig;
        this.environment = environment;
        this.ec2IamRole = resolveEc2IamRole();

    }

    private String resolveEc2IamRole() {
        if (StringUtils.isNotEmpty(awsConfig.getAccessKey())) {
            // no need to resolve IAM Role, since using hardcoded Access/Secret keys takes precedence
            return null;
        }

        if (StringUtils.isNotEmpty(awsConfig.getIamRole()) && !"DEFAULT".equals(awsConfig.getIamRole())) {
            return awsConfig.getIamRole();
        }

        if (isRunningOnEcs()) {
            // ECS has only one role assigned and no need to resolve it here
            return null;
        }

        String ec2IamRole = awsEc2MetadataApi.defaultIamRole();
        LOGGER.info(String.format("Using IAM Role attached to EC2 Instance: '%s'", ec2IamRole));
        // TODO: what if Role is not assigned?
        return ec2IamRole;
    }

    AwsCredentials credentials() {
        if (StringUtils.isNotEmpty(awsConfig.getAccessKey())) {
            return AwsCredentials.builder()
                .setAccessKey(awsConfig.getAccessKey())
                .setSecretKey(awsConfig.getSecretKey())
                .build();
        }

        if (StringUtils.isNotEmpty(ec2IamRole)) {
            return fetchCredentialsFromEc2();
        }

        return fetchCredentialsFromEcs();
    }

    private AwsCredentials fetchCredentialsFromEc2() {
        LOGGER.fine(String.format("Fetching AWS Credentials using EC2 IAM Role: %s", ec2IamRole));

        try {
            return awsEc2MetadataApi.credentials(ec2IamRole);
        } catch (Exception e) {
            throw new InvalidConfigurationException(String.format("Unable to retrieve credentials from IAM Role: "
                + "'%s', please make sure it's attached to your EC2 Instance", awsConfig.getIamRole()), e);
        }
    }

    private AwsCredentials fetchCredentialsFromEcs() {
        LOGGER.fine("Fetching AWS Credentials from ECS");

        String relativePath = environment.getEnv(ECS_CREDENTIALS_ENV_VAR_NAME);
        if (relativePath == null) {
            throw new InvalidConfigurationException("Could not acquire credentials! "
                + "Did not find declared AWS access key or IAM Role, and could not discover IAM Task Role or default role.");
        }
        try {
            return awsEc2MetadataApi.credentialsFromEcs(relativePath);
        } catch (Exception e) {
            throw new InvalidConfigurationException(String.format("Unable to retrieve credentials from IAM Task Role."
                + " URI: %s", relativePath));
        }
    }
}
