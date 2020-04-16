package com.hazelcast.aws;

import com.hazelcast.aws.AwsMetadataApi.EcsMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class AwsEcsClientTest {
    private static final String CLUSTER = "cluster-arn";
    private static final String FAMILY = "family-name";
    private static final AwsCredentials CREDENTIALS = AwsCredentials.builder()
        .setAccessKey("access-key")
        .setSecretKey("secret-key")
        .setToken("token")
        .build();

    @Mock
    private AwsEcsApi awsEcsApi;

    @Mock
    private AwsEc2Api awsEc2Api;

    @Mock
    private AwsMetadataApi awsMetadataApi;

    @Mock
    private AwsCredentialsProvider awsCredentialsProvider;

    private AwsEcsClient awsEcsClient;

    @Before
    public void setUp() {
        EcsMetadata ecsMetadata = mock(EcsMetadata.class);
        given(ecsMetadata.getClusterArn()).willReturn(CLUSTER);
        given(ecsMetadata.getFamilyName()).willReturn(FAMILY);
        given(awsMetadataApi.metadataEcs()).willReturn(ecsMetadata);
        given(awsCredentialsProvider.credentials()).willReturn(CREDENTIALS);

        awsEcsClient = new AwsEcsClient(awsEcsApi, awsEc2Api, awsMetadataApi, awsCredentialsProvider);
    }

    @Test
    public void getAddresses() {
        // given
        List<String> tasks = singletonList("task-arn");
        List<String> privateIps = singletonList("123.12.1.0");
        Map<String, String> expectedResult = singletonMap("123.12.1.0", "1.4.6.2");
        given(awsEcsApi.listTasks(CLUSTER, FAMILY, CREDENTIALS)).willReturn(tasks);
        given(awsEcsApi.describeTasks(CLUSTER, tasks, CREDENTIALS)).willReturn(privateIps);
        given(awsEc2Api.describeNetworkInterfaces(privateIps, CREDENTIALS)).willReturn(expectedResult);

        // when
        Map<String, String> result = awsEcsClient.getAddresses();

        // then
        assertEquals(expectedResult, result);
    }

    @Test
    public void getAddressesNoPublicAddresses() {
        // given
        List<String> tasks = singletonList("task-arn");
        List<String> privateIps = singletonList("123.12.1.0");
        given(awsEcsApi.listTasks(CLUSTER, FAMILY, CREDENTIALS)).willReturn(tasks);
        given(awsEcsApi.describeTasks(CLUSTER, tasks, CREDENTIALS)).willReturn(privateIps);
        given(awsEc2Api.describeNetworkInterfaces(privateIps, CREDENTIALS)).willThrow(new RuntimeException());

        // when
        Map<String, String> result = awsEcsClient.getAddresses();

        // then
        assertEquals(singletonMap("123.12.1.0", null), result);
    }

    @Test
    public void getAddressesNoTasks() {
        // given
        List<String> tasks = emptyList();
        given(awsEcsApi.listTasks(CLUSTER, FAMILY, CREDENTIALS)).willReturn(tasks);

        // when
        Map<String, String> result = awsEcsClient.getAddresses();

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAvailabilityZone() {
        assertEquals("unknown", awsEcsClient.getAvailabilityZone());
    }
}