package com.hazelcast.aws;

import java.util.Objects;

final class AwsCredentials {
    private String accessKey;
    private String secretKey;
    private String token;

    private AwsCredentials(String accessKey, String secretKey, String token) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.token = token;
    }

    String getAccessKey() {
        return accessKey;
    }

    String getSecretKey() {
        return secretKey;
    }

    String getToken() {
        return token;
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String accessKey;
        private String secretKey;
        private String token;

        Builder setAccessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        Builder setSecretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        Builder setToken(String token) {
            this.token = token;
            return this;
        }

        AwsCredentials build() {
            return new AwsCredentials(accessKey, secretKey, token);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AwsCredentials that = (AwsCredentials) o;
        return Objects.equals(accessKey, that.accessKey) &&
            Objects.equals(secretKey, that.secretKey) &&
            Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessKey, secretKey, token);
    }
}
