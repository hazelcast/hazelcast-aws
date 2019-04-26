package com.hazelcast.aws.security;

import java.util.Map;

/**
 *
 */
public interface Aws4RequestSigner {
    String sign(Map<String, String> attributes, Map<String, String> headers);

    String sign(Map<String, String> attributes, Map<String, String> headers, String body, String httpMethod);

    String getSignedHeaders();

    String getAuthorizationHeader();

    String createFormattedCredential();
}
