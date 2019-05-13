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

package com.hazelcast.aws.impl;

/**
 * AWS constants
 */
public final class Constants {

    public static final String DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    public static final String EC2_DOC_VERSION = "2016-11-15";
    public static final String ECS_DOC_VERSION = "2014-11-13";
    public static final String SIGNATURE_METHOD_V4 = "AWS4-HMAC-SHA256";
    public static final String HTTPS = "https";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String ECS_CONTAINER_CREDENTIALS_ENV_VAR_NAME = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
    public static final String EC2_PREFIX = "ec2.";
    public static final String ECS_PREFIX = "ecs.";
    public static final String EC2 = "ec2";
    public static final String ECS = "ecs";
    public static final String AWS_EXECUTION_ENV_VAR_NAME = "AWS_EXECUTION_ENV";

    public static final int HOSTNAME_PREFIX_LENGTH = 4;

    private Constants() {
    }
}
