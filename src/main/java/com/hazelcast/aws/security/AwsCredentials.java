package com.hazelcast.aws.security;

import com.hazelcast.aws.AwsConfig;

/**
 * Effective Credentials to be used for AWS operations
 */
public class AwsCredentials {
    private String iamRole = null;
    private String accessKey = null;
    private String secretKey = null;
    private String securityToken = null;

    public AwsCredentials() {
    }

    public AwsCredentials(AwsConfig config) {
        this.iamRole = config.getIamRole();
        this.accessKey = config.getAccessKey();
        this.secretKey = config.getSecretKey();
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getIamRole() {
        return iamRole;
    }

    public void setIamRole(String iamRole) {
        this.iamRole = iamRole;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    @Override
    public String toString() {
        return "AwsCredentials{" +
                "accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", securityToken='" + securityToken + '\'' +
                '}';
    }
}
