package com.hazelcast.aws.impl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.hazelcast.aws.AwsConfig;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;
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
        Map<String, String> values = describeTasks.execute();

        // then
        MatcherAssert.assertThat("list of 3 tasks", values.size() == 3);
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
                "                    \"status\": \"ATTACHED\", \n" +
                "                    \"type\": \"ElasticNetworkInterface\", \n" +
                "                    \"id\": \"4a6decb5-cfaa-4a7c-8f1a-adc3522d0241\", \n" +
                "                    \"details\": [\n" +
                "                        {\n" +
                "                            \"name\": \"subnetId\", \n" +
                "                            \"value\": \"subnet-03e33b2ef078a5155\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"networkInterfaceId\", \n" +
                "                            \"value\": \"eni-cddaf7e0\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"macAddress\", \n" +
                "                            \"value\": \"06:fe:ac:7a:f9:b8\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"privateIPv4Address\", \n" +
                "                            \"value\": \"10.0.1.16\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ], \n" +
                "            \"clusterArn\": \"arn:aws:ecs:eu-central-1:665466731577:cluster/default\", \n" +
                "            \"desiredStatus\": \"RUNNING\", \n" +
                "            \"createdAt\": 1556639867.784, \n" +
                "            \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/203153e347194427bffd9e248ad36142\", \n" +
                "            \"group\": \"family:g-aws-test\", \n" +
                "            \"pullStartedAt\": 1556639886.903, \n" +
                "            \"version\": 3, \n" +
                "            \"memory\": \"512\", \n" +
                "            \"connectivityAt\": 1556639878.177, \n" +
                "            \"startedAt\": 1556639900.903, \n" +
                "            \"taskDefinitionArn\": \"arn:aws:ecs:eu-central-1:665466731577:task-definition/g-aws-test:6\", \n" +
                "            \"containers\": [\n" +
                "                {\n" +
                "                    \"containerArn\": \"arn:aws:ecs:eu-central-1:665466731577:container/c4df80dd-8bb2-4b68-88a8-b952fbd085cb\", \n" +
                "                    \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/203153e347194427bffd9e248ad36142\", \n" +
                "                    \"name\": \"g-aws-test-container\", \n" +
                "                    \"networkBindings\": [], \n" +
                "                    \"lastStatus\": \"RUNNING\", \n" +
                "                    \"healthStatus\": \"UNKNOWN\", \n" +
                "                    \"cpu\": \"0\", \n" +
                "                    \"networkInterfaces\": [\n" +
                "                        {\n" +
                "                            \"privateIpv4Address\": \"10.0.1.16\", \n" +
                "                            \"attachmentId\": \"4a6decb5-cfaa-4a7c-8f1a-adc3522d0241\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ], \n" +
                "            \"tags\": [], \n" +
                "            \"lastStatus\": \"RUNNING\", \n" +
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
                "            \"pullStoppedAt\": 1556639899.903, \n" +
                "            \"cpu\": \"256\"\n" +
                "        }, \n" +
                "        {\n" +
                "            \"launchType\": \"FARGATE\", \n" +
                "            \"attachments\": [\n" +
                "                {\n" +
                "                    \"status\": \"ATTACHED\", \n" +
                "                    \"type\": \"ElasticNetworkInterface\", \n" +
                "                    \"id\": \"b69a4813-8de6-4c99-9afd-32a8744a65fe\", \n" +
                "                    \"details\": [\n" +
                "                        {\n" +
                "                            \"name\": \"subnetId\", \n" +
                "                            \"value\": \"subnet-03e33b2ef078a5155\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"networkInterfaceId\", \n" +
                "                            \"value\": \"eni-f2daf7df\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"macAddress\", \n" +
                "                            \"value\": \"06:bd:1c:29:a6:4e\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"privateIPv4Address\", \n" +
                "                            \"value\": \"10.0.1.219\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ], \n" +
                "            \"clusterArn\": \"arn:aws:ecs:eu-central-1:665466731577:cluster/default\", \n" +
                "            \"desiredStatus\": \"RUNNING\", \n" +
                "            \"createdAt\": 1556639867.784, \n" +
                "            \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/80913c890830447d8f64c137fcc9a17b\", \n" +
                "            \"group\": \"family:g-aws-test\", \n" +
                "            \"pullStartedAt\": 1556639887.666, \n" +
                "            \"version\": 3, \n" +
                "            \"memory\": \"512\", \n" +
                "            \"connectivityAt\": 1556639873.628, \n" +
                "            \"startedAt\": 1556639902.666, \n" +
                "            \"taskDefinitionArn\": \"arn:aws:ecs:eu-central-1:665466731577:task-definition/g-aws-test:6\", \n" +
                "            \"containers\": [\n" +
                "                {\n" +
                "                    \"containerArn\": \"arn:aws:ecs:eu-central-1:665466731577:container/7def58a6-73cf-4e94-b336-2137d0c84ac2\", \n" +
                "                    \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/80913c890830447d8f64c137fcc9a17b\", \n" +
                "                    \"name\": \"g-aws-test-container\", \n" +
                "                    \"networkBindings\": [], \n" +
                "                    \"lastStatus\": \"RUNNING\", \n" +
                "                    \"healthStatus\": \"UNKNOWN\", \n" +
                "                    \"cpu\": \"0\", \n" +
                "                    \"networkInterfaces\": [\n" +
                "                        {\n" +
                "                            \"privateIpv4Address\": \"10.0.1.219\", \n" +
                "                            \"attachmentId\": \"b69a4813-8de6-4c99-9afd-32a8744a65fe\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ], \n" +
                "            \"tags\": [], \n" +
                "            \"lastStatus\": \"RUNNING\", \n" +
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
                "            \"pullStoppedAt\": 1556639900.666, \n" +
                "            \"cpu\": \"256\"\n" +
                "        }, \n" +
                "        {\n" +
                "            \"launchType\": \"FARGATE\", \n" +
                "            \"attachments\": [\n" +
                "                {\n" +
                "                    \"status\": \"ATTACHED\", \n" +
                "                    \"type\": \"ElasticNetworkInterface\", \n" +
                "                    \"id\": \"29e29922-3a89-4bfb-b454-6007db30b564\", \n" +
                "                    \"details\": [\n" +
                "                        {\n" +
                "                            \"name\": \"subnetId\", \n" +
                "                            \"value\": \"subnet-03e33b2ef078a5155\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"networkInterfaceId\", \n" +
                "                            \"value\": \"eni-28b49905\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"macAddress\", \n" +
                "                            \"value\": \"06:0d:d8:66:a3:9c\"\n" +
                "                        }, \n" +
                "                        {\n" +
                "                            \"name\": \"privateIPv4Address\", \n" +
                "                            \"value\": \"10.0.1.161\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ], \n" +
                "            \"clusterArn\": \"arn:aws:ecs:eu-central-1:665466731577:cluster/default\", \n" +
                "            \"desiredStatus\": \"RUNNING\", \n" +
                "            \"createdAt\": 1556639867.784, \n" +
                "            \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/c3585245b78b4f1baf967e71d97c836b\", \n" +
                "            \"group\": \"family:g-aws-test\", \n" +
                "            \"pullStartedAt\": 1556639890.044, \n" +
                "            \"version\": 3, \n" +
                "            \"memory\": \"512\", \n" +
                "            \"connectivityAt\": 1556639880.44, \n" +
                "            \"startedAt\": 1556639904.044, \n" +
                "            \"taskDefinitionArn\": \"arn:aws:ecs:eu-central-1:665466731577:task-definition/g-aws-test:6\", \n" +
                "            \"containers\": [\n" +
                "                {\n" +
                "                    \"containerArn\": \"arn:aws:ecs:eu-central-1:665466731577:container/be5971c1-1ada-402c-81d5-8352592435a9\", \n" +
                "                    \"taskArn\": \"arn:aws:ecs:eu-central-1:665466731577:task/default/c3585245b78b4f1baf967e71d97c836b\", \n" +
                "                    \"name\": \"g-aws-test-container\", \n" +
                "                    \"networkBindings\": [], \n" +
                "                    \"lastStatus\": \"RUNNING\", \n" +
                "                    \"healthStatus\": \"UNKNOWN\", \n" +
                "                    \"cpu\": \"0\", \n" +
                "                    \"networkInterfaces\": [\n" +
                "                        {\n" +
                "                            \"privateIpv4Address\": \"10.0.1.161\", \n" +
                "                            \"attachmentId\": \"29e29922-3a89-4bfb-b454-6007db30b564\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ], \n" +
                "            \"tags\": [], \n" +
                "            \"lastStatus\": \"RUNNING\", \n" +
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
                "            \"pullStoppedAt\": 1556639903.044, \n" +
                "            \"cpu\": \"256\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }
}