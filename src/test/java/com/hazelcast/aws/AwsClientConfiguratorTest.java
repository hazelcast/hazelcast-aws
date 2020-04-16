package com.hazelcast.aws;

import org.junit.Test;

import static com.hazelcast.aws.AwsClientConfigurator.resolveEc2Endpoint;
import static com.hazelcast.aws.AwsClientConfigurator.resolveEcsEndpoint;
import static com.hazelcast.aws.AwsClientConfigurator.resolveRegion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class AwsClientConfiguratorTest {

    @Test
    public void resolveRegionAwsConfig() {
        // given
        String region = "us-east-1";
        AwsConfig awsConfig = AwsConfig.builder().setRegion(region).build();
        AwsMetadataApi awsMetadataApi = mock(AwsMetadataApi.class);
        Environment environment = mock(Environment.class);


        // when
        String result = resolveRegion(awsConfig, awsMetadataApi, environment);

        // then
        assertEquals(region, result);
    }

    @Test
    public void resolveRegionEcsConfig() {
        // given
        String region = "us-east-1";
        AwsConfig awsConfig = AwsConfig.builder().build();
        AwsMetadataApi awsMetadataApi = mock(AwsMetadataApi.class);
        Environment environment = mock(Environment.class);
        given(environment.getEnv("AWS_REGION")).willReturn(region);
        given(environment.isRunningOnEcs()).willReturn(true);

        // when
        String result = resolveRegion(awsConfig, awsMetadataApi, environment);

        // then
        assertEquals(region, result);
    }

    @Test
    public void resolveRegionEc2Metadata() {
        // given
        AwsConfig awsConfig = AwsConfig.builder().build();
        AwsMetadataApi awsMetadataApi = mock(AwsMetadataApi.class);
        Environment environment = mock(Environment.class);
        given(awsMetadataApi.availabilityZoneEc2()).willReturn("us-east-1a");

        // when
        String result = resolveRegion(awsConfig, awsMetadataApi, environment);

        // then
        assertEquals("us-east-1", result);
    }

    @Test
    public void resolveEc2Endpoints() {
        assertEquals("ec2.us-east-1.amazonaws.com", resolveEc2Endpoint(AwsConfig.builder().build(), "us-east-1"));
        assertEquals("ec2.us-east-1.amazonaws.com", resolveEc2Endpoint(AwsConfig.builder().setHostHeader("ecs").build(), "us-east-1"));
        assertEquals("ec2.us-east-1.amazonaws.com", resolveEc2Endpoint(AwsConfig.builder().setHostHeader("ec2").build(), "us-east-1"));
        assertEquals("ec2.us-east-1.something",
            resolveEc2Endpoint(AwsConfig.builder().setHostHeader("ec2.something").build(), "us-east-1"));
    }

    @Test
    public void resolveEcsEndpoints() {
        assertEquals("ecs.us-east-1.amazonaws.com", resolveEcsEndpoint(AwsConfig.builder().build(), "us-east-1"));
        assertEquals("ecs.us-east-1.amazonaws.com",
            resolveEcsEndpoint(AwsConfig.builder().setHostHeader("ecs").build(), "us-east-1"));
        assertEquals("ecs.us-east-1.something",
            resolveEcsEndpoint(AwsConfig.builder().setHostHeader("ecs.something").build(), "us-east-1"));
    }

    @Test
    public void explicitlyEc2Configured() {
        assertTrue(AwsClientConfigurator.explicitlyEc2Configured(AwsConfig.builder().setHostHeader("ec2").build()));
        assertTrue(AwsClientConfigurator.explicitlyEc2Configured(
            AwsConfig.builder().setHostHeader("ec2.us-east-1.amazonaws.com").build()));
        assertFalse(AwsClientConfigurator.explicitlyEc2Configured(
            AwsConfig.builder().setHostHeader("ecs.us-east-1.amazonaws.com").build()));
        assertFalse(AwsClientConfigurator.explicitlyEc2Configured(AwsConfig.builder().build()));
    }

}