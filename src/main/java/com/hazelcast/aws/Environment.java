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

import static com.hazelcast.aws.StringUtils.isNotEmpty;

/**
 * This class is introduced to lookup system parameters.
 * <p>
 * It is introduced to isolate and mock system-related stuff.
 */
class Environment {
    private static final boolean IS_RUNNING_ON_ECS = isRunningOn("ECS");

    String getEnv(String name) {
        return System.getenv(name);
    }

    boolean isRunningOnEcs() {
        return IS_RUNNING_ON_ECS;
    }

    private static boolean isRunningOn(String system) {
        String execEnv = System.getenv("AWS_EXECUTION_ENV");
        return isNotEmpty(execEnv) && execEnv.contains(system);
    }
}
