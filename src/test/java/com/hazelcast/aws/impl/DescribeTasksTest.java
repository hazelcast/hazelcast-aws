package com.hazelcast.aws.impl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.hazelcast.aws.AwsConfig;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 *
 */
public class DescribeTasksTest {

    private static final String ENDPOINT = "localhost";
    private final static String TEST_REGION = "eu-central-1";
    private final static String TEST_HOST = "ecs.eu-central-1.amazonaws.com";
    private final static String TEST_ACCESS_KEY = "AKIDEXAMPLE";
    private final static String TEST_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
    private DescribeTasks describeTasks;

    @Before
    public void setUp() throws Exception {
        String testEndpoint = String.format("http://%s:%d", ENDPOINT, wireMockRule.port());

        AwsConfig awsConfig = AwsConfig.builder()
                .setHostHeader(TEST_HOST)
                .setRegion(TEST_REGION)
                .setAccessKey(TEST_ACCESS_KEY)
                .setSecretKey(TEST_SECRET_KEY)
                .build();
        describeTasks = new DescribeTasks(awsConfig, new URL(testEndpoint));
//        stubFor(post(urlMatching("^/.*"))
//                .atPriority(5).willReturn(aResponse().withStatus(401).withBody("\"reason\":\"Forbidden\"")));

    }

    @Test
    public void describeTasks() throws Exception {
        // given
        stubDescribeTasks("/.*", describeTasksResponse());

        // when
        Map<String, String> intfaces = describeTasks.execute();

        // then
        MatcherAssert.assertThat("list of 2 tasks", intfaces.size() == 2);
    }

    private void stubDescribeTasks(String urlRegex, String response) {
        stubFor(post(urlMatching(urlRegex))
                .withHeader("X-Amz-Target", new EqualToPattern("AmazonEC2ContainerServiceV20141113.DescribeTasks"))
                .willReturn(aResponse().withStatus(200).withBody(response)));
    }

    private String describeTasksResponse() {
        return "{\n" +
                "    \"failures\": [], \n" +
                "    \"tasks\": [\n" +
                "        {\n" +
                "            \"launchType\": \"FARGATE\", \n" +
                "            \"attachments\": [\n" +
                "                {\n" +
                "                    \"status\": \"DELETED\", \n" +
                "                    \"type\": \"ElasticNetworkInterface\", \n" +
                "                    \"id\": \"4419a868-8d29-467e-806e-910c3dda8e5e\", \n" +
                "                    \"details\": [\n" +
                "                        {\n" +
                "                            \"name\": \"subnetId\", \n" +
                "                            \"value\": \"subnet-03e33b2ef078a5155\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"networkInterfaceId\", \n" +
                "                            \"value\": \"eni-e8a88cc5\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"macAddress\", \n" +
                "                            \"value\": \"06:dc:f9:99:aa:b4\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"privateIPv4Address\", \n" +
                "                            \"value\": \"10.0.1.41\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ], \n" +
                "            \"stoppingAt\": 1555668264.118, \n" +
                "            \"clusterArn\": \"arn:aws:ecs:eu-central-1:665466731577:cluster/default\", \n" +
                "            \"desiredStatus\": \"STOPPED\", \n" +
                "            \"createdAt\": 1555668204.287, \n" +
                "            \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/40c003cf5d504c65b2d5418c9e9deb72\", \n" +
                "            \"group\": \"family:g-aws-test\", \n" +
                "            \"pullStartedAt\": 1555668226.844, \n" +
                "            \"version\": 5, \n" +
                "            \"stopCode\": \"EssentialContainerExited\", \n" +
                "            \"connectivityAt\": 1555668209.209, \n" +
                "            \"startedAt\": 1555668240.844, \n" +
                "            \"taskDefinitionArn\": \"arn:aws:ecs:eu-central-1:665466731577:task-definition/g-aws-test:6\", \n" +
                "            \"containers\": [\n" +
                "                {\n" +
                "                    \"containerArn\": \"arn:aws:ecs:eu-central-1:665466731577:container/ff414717-8dcd-4171-9d39-3f41443c36ca\", \n" +
                "                    \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/40c003cf5d504c65b2d5418c9e9deb72\", \n" +
                "                    \"name\": \"g-aws-test-container\", \n" +
                "                    \"networkBindings\": [], \n" +
                "                    \"lastStatus\": \"STOPPED\", \n" +
                "                    \"healthStatus\": \"UNKNOWN\", \n" +
                "                    \"networkInterfaces\": [\n" +
                "                        {\n" +
                "                            \"privateIpv4Address\": \"10.0.1.41\", \n" +
                "                            \"attachmentId\": \"4419a868-8d29-467e-806e-910c3dda8e5e\"\n" +
                "                        }\n" +
                "                    ], \n" +
                "                    \"cpu\": \"0\", \n" +
                "                    \"exitCode\": 0\n" +
                "                }\n" +
                "            ], \n" +
                "            \"tags\": [], \n" +
                "            \"executionStoppedAt\": 1555668263.0, \n" +
                "            \"memory\": \"512\", \n" +
                "            \"lastStatus\": \"STOPPED\", \n" +
                "            \"connectivity\": \"CONNECTED\", \n" +
                "            \"healthStatus\": \"UNKNOWN\", \n" +
                "            \"platformVersion\": \"1.3.0\", \n" +
                "            \"overrides\": {\n" +
                "                \"containerOverrides\": [\n" +
                "                    {\n" +
                "                        \"name\": \"g-aws-test-container\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }, \n" +
                "            \"pullStoppedAt\": 1555668239.844, \n" +
                "            \"stoppedAt\": 1555668287.208, \n" +
                "            \"stoppedReason\": \"Essential container in task exited\", \n" +
                "            \"cpu\": \"256\"\n" +
                "        }, \n" +
                "        {\n" +
                "            \"launchType\": \"FARGATE\", \n" +
                "            \"attachments\": [\n" +
                "                {\n" +
                "                    \"status\": \"DELETED\", \n" +
                "                    \"type\": \"ElasticNetworkInterface\", \n" +
                "                    \"id\": \"27b75830-adf5-4e32-8b08-2c56a0f50ea5\", \n" +
                "                    \"details\": [\n" +
                "                        {\n" +
                "                            \"name\": \"subnetId\", \n" +
                "                            \"value\": \"subnet-03e33b2ef078a5155\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"networkInterfaceId\", \n" +
                "                            \"value\": \"eni-e9a88cc4\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"macAddress\", \n" +
                "                            \"value\": \"06:d1:92:e2:03:da\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"privateIPv4Address\", \n" +
                "                            \"value\": \"10.0.1.162\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ], \n" +
                "            \"stoppingAt\": 1555668266.788, \n" +
                "            \"clusterArn\": \"arn:aws:ecs:eu-central-1:665466731577:cluster/default\", \n" +
                "            \"desiredStatus\": \"STOPPED\", \n" +
                "            \"createdAt\": 1555668204.287, \n" +
                "            \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/bf2687a5187b46ac8edf541bac799d52\", \n" +
                "            \"group\": \"family:g-aws-test\", \n" +
                "            \"pullStartedAt\": 1555668230.188, \n" +
                "            \"version\": 5, \n" +
                "            \"stopCode\": \"EssentialContainerExited\", \n" +
                "            \"connectivityAt\": 1555668209.427, \n" +
                "            \"startedAt\": 1555668244.188, \n" +
                "            \"taskDefinitionArn\": \"arn:aws:ecs:eu-central-1:665466731577:task-definition/g-aws-test:6\", \n" +
                "            \"containers\": [\n" +
                "                {\n" +
                "                    \"containerArn\": \"arn:aws:ecs:eu-central-1:665466731577:container/de5d6fca-4ce0-4ab5-90a5-53693c7c11d1\", \n" +
                "                    \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/bf2687a5187b46ac8edf541bac799d52\", \n" +
                "                    \"name\": \"g-aws-test-container\", \n" +
                "                    \"networkBindings\": [], \n" +
                "                    \"lastStatus\": \"STOPPED\", \n" +
                "                    \"healthStatus\": \"UNKNOWN\", \n" +
                "                    \"networkInterfaces\": [\n" +
                "                        {\n" +
                "                            \"privateIpv4Address\": \"10.0.1.162\", \n" +
                "                            \"attachmentId\": \"27b75830-adf5-4e32-8b08-2c56a0f50ea5\"\n" +
                "                        }\n" +
                "                    ], \n" +
                "                    \"cpu\": \"0\", \n" +
                "                    \"exitCode\": 0\n" +
                "                }\n" +
                "            ], \n" +
                "            \"tags\": [], \n" +
                "            \"executionStoppedAt\": 1555668266.0, \n" +
                "            \"memory\": \"512\", \n" +
                "            \"lastStatus\": \"STOPPED\", \n" +
                "            \"connectivity\": \"CONNECTED\", \n" +
                "            \"healthStatus\": \"UNKNOWN\", \n" +
                "            \"platformVersion\": \"1.3.0\", \n" +
                "            \"overrides\": {\n" +
                "                \"containerOverrides\": [\n" +
                "                    {\n" +
                "                        \"name\": \"g-aws-test-container\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }, \n" +
                "            \"pullStoppedAt\": 1555668242.188, \n" +
                "            \"stoppedAt\": 1555668279.388, \n" +
                "            \"stoppedReason\": \"Essential container in task exited\", \n" +
                "            \"cpu\": \"256\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
    }
}