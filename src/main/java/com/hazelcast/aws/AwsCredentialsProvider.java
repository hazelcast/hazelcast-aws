package com.hazelcast.aws;

import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

class AwsCredentialsProvider {
    private static final ILogger LOGGER = Logger.getLogger(AwsCredentialsProvider.class);

    private final AwsConfig awsConfig;
    private final AwsMetadataApi awsMetadataApi;
    private final Environment environment;
    private final String ec2IamRole;

    AwsCredentialsProvider(AwsConfig awsConfig, AwsMetadataApi awsMetadataApi, Environment environment) {
        this.awsConfig = awsConfig;
        this.awsMetadataApi = awsMetadataApi;
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

        if (environment.isRunningOnEcs()) {
            // ECS has only one role assigned and no need to resolve it here
            LOGGER.info("Using IAM Task Role attached to ECS Task");
            return null;
        }

        String ec2IamRole = awsMetadataApi.defaultIamRoleEc2();
        LOGGER.info(String.format("Using IAM Role attached to EC2 Instance: '%s'", ec2IamRole));
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
            return awsMetadataApi.credentialsEc2(ec2IamRole);
        } catch (Exception e) {
            throw new InvalidConfigurationException(String.format("Unable to retrieve credentials from IAM Role: "
                + "'%s', please make sure it's attached to your EC2 Instance", awsConfig.getIamRole()), e);
        }
    }

    private AwsCredentials fetchCredentialsFromEcs() {
        LOGGER.fine("Fetching AWS Credentials from ECS IAM Task Role");

        try {
            return awsMetadataApi.credentialsEcs();
        } catch (Exception e) {
            throw new InvalidConfigurationException("Unable to retrieve credentials from IAM Role attached to ECS Task,"
                + " please check your configuration");
        }
    }
}
