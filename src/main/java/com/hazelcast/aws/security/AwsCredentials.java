/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.aws.security;

import com.hazelcast.aws.AwsConfig;

/**
 * Holder of Effective Credentials to be used for AWS operations
 */
public class AwsCredentials {
    private String iamRole;
    private String accessKey;
    private String secretKey;
    private String securityToken;

    /**
     * Initialize credentials from config
     * @param config configuration
     */
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
        return "AwsCredentials{"
                + "accessKey='" + accessKey + '\''
                + ", secretKey='" + secretKey + '\''
                + ", securityToken='" + securityToken + '\''
                + '}';
    }
}
