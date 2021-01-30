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

import com.hazelcast.config.InvalidConfigurationException;

import static com.hazelcast.aws.StringUtils.isEmpty;
import static com.hazelcast.aws.StringUtils.isNotEmpty;

/**
 * AWS Discovery Strategy configuration that corresponds to the properties passed in the Hazelcast configuration and listed in
 * {@link AwsProperties}.
 * <p>
 * This class is immutable.
 */
final class AwsConfig {
    private final String region;
    private final String hostHeader;
    private final String securityGroupName;
    private final String tagKey;
    private final String tagValue;
    private final Tags tags;
    private final int connectionTimeoutSeconds;
    private final int connectionRetries;
    private final int readTimeoutSeconds;
    private final PortRange hzPort;
    private final String accessKey;
    private final String secretKey;
    private final String iamRole;
    private final String cluster;
    private final String family;
    private final String serviceName;

    @SuppressWarnings("checkstyle:parameternumber")
    // Constructor has a lot of parameters, but it's private.
    private AwsConfig(String accessKey, String secretKey, String region, String iamRole, String hostHeader,
                      String securityGroupName, String tagKey, String tagValue, int connectionTimeoutSeconds,
                      int connectionRetries, int readTimeoutSeconds, PortRange hzPort, String cluster, String family,
                      String serviceName) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
        this.iamRole = iamRole;
        this.hostHeader = hostHeader;
        this.securityGroupName = securityGroupName;
        this.tagKey = tagKey;
        this.tagValue = tagValue;
        this.tags = Tags.from(tagKey, tagValue);
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.connectionRetries = connectionRetries;
        this.readTimeoutSeconds = readTimeoutSeconds;
        this.hzPort = hzPort;
        this.cluster = cluster;
        this.family = family;
        this.serviceName = serviceName;

        validateConfig();
    }

    private void validateConfig() {
        if (anyOfEc2PropertiesConfigured() && anyOfEcsPropertiesConfigured()) {
            throw new InvalidConfigurationException(
                "You have to configure either EC2 properties ('iam-role', 'security-group-name', 'tag-key', 'tag-value')"
                    + " or ECS properties ('cluster', 'family', 'service-name'). You cannot define both of them"
            );
        }
        if (isNotEmpty(family) && isNotEmpty(serviceName)) {
            throw new InvalidConfigurationException(
                "You cannot configure ECS discovery with both 'family' and 'service-name', these filters are mutually"
                    + " exclusive"
            );
        }
        if (isNotEmpty(iamRole) && (isNotEmpty(accessKey) || isNotEmpty(secretKey))) {
            throw new InvalidConfigurationException(
                "You cannot define both 'iam-role' and 'access-key'/'secret-key'. Choose how you want to authenticate"
                    + " with AWS API, either with IAM Role or with hardcoded AWS Credentials");
        }
        if ((isEmpty(accessKey) && isNotEmpty(secretKey)) || (isNotEmpty(accessKey) && isEmpty(secretKey))) {
            throw new InvalidConfigurationException(
                "You have to either define both ('access-key', 'secret-key') or none of them");
        }
    }

    private boolean anyOfEc2PropertiesConfigured() {
        return isNotEmpty(iamRole) || isNotEmpty(securityGroupName) || tags.hasTags();
    }

    private boolean anyOfEcsPropertiesConfigured() {
        return isNotEmpty(cluster) || isNotEmpty(family) || isNotEmpty(serviceName);
    }

    static Builder builder() {
        return new Builder();
    }

    String getAccessKey() {
        return accessKey;
    }

    String getSecretKey() {
        return secretKey;
    }

    String getRegion() {
        return region;
    }

    String getIamRole() {
        return iamRole;
    }

    String getHostHeader() {
        return hostHeader;
    }

    String getSecurityGroupName() {
        return securityGroupName;
    }

    String getTagKey() {
        return tagKey;
    }

    String getTagValue() {
        return tagValue;
    }

    Tags getTags() {
        return tags;
    }

    int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    int getConnectionRetries() {
        return connectionRetries;
    }

    int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    PortRange getHzPort() {
        return hzPort;
    }

    String getCluster() {
        return cluster;
    }

    String getFamily() {
        return family;
    }

    String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return "AwsConfig{"
            + "accessKey='***'"
            + ", secretKey='***'"
            + ", iamRole='" + iamRole + '\''
            + ", region='" + region + '\''
            + ", hostHeader='" + hostHeader + '\''
            + ", securityGroupName='" + securityGroupName + '\''
            + ", tagKey='" + tagKey + '\''
            + ", tagValue='" + tagValue + '\''
            + ", tags='" + tags + '\''
            + ", hzPort=" + hzPort
            + ", cluster='" + cluster + '\''
            + ", family='" + family + '\''
            + ", serviceName='" + serviceName + '\''
            + ", connectionTimeoutSeconds=" + connectionTimeoutSeconds
            + ", connectionRetries=" + connectionRetries
            + ", readTimeoutSeconds=" + readTimeoutSeconds
            + '}';
    }

    static class Builder {

        private String accessKey;
        private String secretKey;
        private String region;
        private String iamRole;
        private String hostHeader;
        private String securityGroupName;
        private String tagKey;
        private String tagValue;
        private int connectionTimeoutSeconds;
        private int connectionRetries;
        private int readTimeoutSeconds;
        private PortRange hzPort;
        private String cluster;
        private String family;
        private String serviceName;

        Builder setAccessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        Builder setSecretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        Builder setIamRole(String iamRole) {
            this.iamRole = iamRole;
            return this;
        }

        Builder setHostHeader(String hostHeader) {
            this.hostHeader = hostHeader;
            return this;
        }

        Builder setSecurityGroupName(String securityGroupName) {
            this.securityGroupName = securityGroupName;
            return this;
        }

        Builder setTagKey(String tagKey) {
            this.tagKey = tagKey;
            return this;
        }

        Builder setTagValue(String tagValue) {
            this.tagValue = tagValue;
            return this;
        }

        Builder setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
            this.connectionTimeoutSeconds = connectionTimeoutSeconds;
            return this;
        }

        Builder setConnectionRetries(int connectionRetries) {
            this.connectionRetries = connectionRetries;
            return this;
        }

        Builder setReadTimeoutSeconds(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
            return this;
        }

        Builder setHzPort(PortRange hzPort) {
            this.hzPort = hzPort;
            return this;
        }

        Builder setCluster(String cluster) {
            this.cluster = cluster;
            return this;
        }

        Builder setFamily(String family) {
            this.family = family;
            return this;
        }

        Builder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        AwsConfig build() {
            return new AwsConfig(accessKey, secretKey, region, iamRole, hostHeader, securityGroupName, tagKey, tagValue,
                connectionTimeoutSeconds, connectionRetries, readTimeoutSeconds, hzPort, cluster, family, serviceName);
        }
    }
}
