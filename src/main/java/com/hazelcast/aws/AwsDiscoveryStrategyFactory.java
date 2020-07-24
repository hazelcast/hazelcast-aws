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

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.internal.nio.IOUtil;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Factory class which returns {@link AwsDiscoveryStrategy} to Discovery SPI
 */
public class AwsDiscoveryStrategyFactory
        implements DiscoveryStrategyFactory {
    @Override
    public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
        return AwsDiscoveryStrategy.class;
    }

    @Override
    public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode, ILogger logger,
                                                  Map<String, Comparable> properties) {
        return new AwsDiscoveryStrategy(properties);
    }

    @Override
    public Collection<PropertyDefinition> getConfigurationProperties() {
        final AwsProperties[] props = AwsProperties.values();
        final ArrayList<PropertyDefinition> definitions = new ArrayList<>(props.length);
        for (AwsProperties prop : props) {
            definitions.add(prop.getDefinition());
        }
        return definitions;
    }

    /**
     * To check if Hazelcast is running on AWS, we first check machine uuid which should start with "ec2" or "EC2". There is
     * a small chance that a non-AWS machine has uuid starting from the mentioned prefix. That is why, to be sure, we make
     * an API call to a local, non-routable address http://169.254.169.254/latest/dynamic/instance-identity/.
     *
     * @return true if running in the AWS (EC2, ECS/EC2, ECS/Fargate) environment
     * @see https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/identify_ec2_instances.html
     */
    @Override
    public boolean isAutoDetectionApplicable() {
        return uuidContainsEc2() && instanceIdentityExists();

    }

    private boolean uuidContainsEc2() {
        String uuidPath = "/sys/hypervisor/uuid";
        if (new File(uuidPath).exists()) {
            String uuid = readFileContents(uuidPath);
            return uuid.startsWith("ec2") || uuid.startsWith("EC2");
        }
        return false;
    }

    private boolean instanceIdentityExists() {
        String metadataUrl = "http://169.254.169.254/latest/dynamic/instance-identity/";
        int timeoutInSeconds = 1;
        try {
            return !RestClient.create(metadataUrl)
                    .withConnectTimeoutSeconds(timeoutInSeconds)
                    .withReadTimeoutSeconds(timeoutInSeconds)
                    .withRetries(0)
                    .get()
                    .isEmpty();
        } catch (Exception e) {
            // any exception means that we're not running on AWS
            return false;
        }
    }

    @Override
    public DiscoveryStrategyLevel discoveryStrategyLevel() {
        return DiscoveryStrategyLevel.CLOUD_VM;
    }

    static String readFileContents(String fileName) {
        InputStream is = null;
        try {
            File file = new File(fileName);
            byte[] data = new byte[(int) file.length()];
            is = new FileInputStream(file);
            is.read(data);
            return new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not get " + fileName, e);
        } finally {
            IOUtil.closeResource(is);
        }
    }
}
