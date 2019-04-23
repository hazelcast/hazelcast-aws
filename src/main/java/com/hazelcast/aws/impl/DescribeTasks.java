package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonArray;
import com.hazelcast.internal.json.JsonValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.aws.impl.Constants.ECS_DOC_VERSION;

/**
 *
 */
public class DescribeTasks extends AwsOperation<Map<String, String>> {
    private Collection<String> taskArns;

    public DescribeTasks(AwsConfig awsConfig, URL endpointURL) {
        super(awsConfig, endpointURL, "ecs", ECS_DOC_VERSION);
    }

    public Map<String, String> execute(Collection<String> taskArns) throws Exception {
        this.taskArns = taskArns;
        return super.execute();
    }

    @Override
    InputStream callService() throws Exception {
        String query = getRequestSigner().getCanonicalizedQueryString(attributes);
        URL url = new URL(endpointURL, "/?" + query);

        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        httpConnection.setRequestMethod(Constants.POST);
        httpConnection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(awsConfig.getConnectionTimeoutSeconds()));
        httpConnection.setDoOutput(true);
        httpConnection.setRequestProperty("Content-Type", "application/x-amz-json-1.1");
        httpConnection.setRequestProperty("X-Amz-Target", "AmazonEC2ContainerServiceV20141113.DescribeTasks");
        httpConnection.setRequestProperty("Accept-Encoding", "identity");

        httpConnection.connect();
        OutputStream outputStream = httpConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        writer.write("{}");
        writer.flush();
        writer.close();
        checkNoAwsErrors(httpConnection);

        return httpConnection.getInputStream();
    }

    @Override
    Map<String, String> unmarshal(InputStream stream) {
        Map<String, String> response = new HashMap<String, String>();

        try {
            JsonArray jsonValues = Json.parse(new InputStreamReader(stream)).asObject()
                    .get("tasks").asArray();
            for (JsonValue task : jsonValues) {
                for (JsonValue container : task.asObject().get("containers").asArray()) {
                    for (JsonValue intface : container.asObject().get("networkInterfaces").asArray()) {
                        String privateIpv4Address = intface.asObject().get("privateIpv4Address").asString();
                        response.put(privateIpv4Address, privateIpv4Address);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Malformed response", e);
        }

        return response;
    }
}
