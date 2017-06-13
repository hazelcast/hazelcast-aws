/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.aws;

public class Configuration {
    private static final int CONNECTION_TIMEOUT = 10;
    private boolean enabled;
    private String accessKey;
    private String secretKey;
    private String region = "us-east-1";
    private String securityGroupName;
    private String tagKey;
    private String tagValue;
    private String hostHeader = "ec2.amazonaws.com";
    private String iamRole;
    private int connectionTimeoutSeconds = CONNECTION_TIMEOUT;

    public Configuration() {
    }

    public String getAccessKey() {
        return this.accessKey;
    }

    public Configuration setAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public Configuration setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public String getRegion() {
        return this.region;
    }

    public Configuration setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getHostHeader() {
        return this.hostHeader;
    }

    public Configuration setHostHeader(String hostHeader) {
        this.hostHeader = hostHeader;
        return this;
    }

    public Configuration setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Configuration setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
        return this;
    }

    public String getSecurityGroupName() {
        return this.securityGroupName;
    }

    public Configuration setTagKey(String tagKey) {
        this.tagKey = tagKey;
        return this;
    }

    public Configuration setTagValue(String tagValue) {
        this.tagValue = tagValue;
        return this;
    }

    public String getTagKey() {
        return this.tagKey;
    }

    public String getTagValue() {
        return this.tagValue;
    }

    public int getConnectionTimeoutSeconds() {
        return this.connectionTimeoutSeconds;
    }

    public Configuration setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        if (connectionTimeoutSeconds < 0) {
            throw new IllegalArgumentException("connection timeout can\'t be smaller than 0");
        } else {
            this.connectionTimeoutSeconds = connectionTimeoutSeconds;
            return this;
        }
    }

    public String getIamRole() {
        return this.iamRole;
    }

    public Configuration setIamRole(String iamRole) {
        this.iamRole = iamRole;
        return this;
    }

    public String toString() {
        return "Configuration{enabled=" + this.enabled + ", region=\'" + this.region + '\'' + ", securityGroupName=\'" +
                this.securityGroupName + '\'' + ", tagKey=\'" + this.tagKey + '\'' + ", tagValue=\'" + this.tagValue +
                '\'' + ", hostHeader=\'" + this.hostHeader + '\'' + ", iamRole=\'" + this.iamRole + '\'' +
                ", connectionTimeoutSeconds=" + this.connectionTimeoutSeconds + '}';
    }
}
