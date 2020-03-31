package com.hazelcast.aws;

import java.util.Objects;
import java.util.Optional;

public final class AwsCredentials {
    private String accessKey;
    private String secretKey;
    private Optional<String> token;

    private AwsCredentials(String accessKey, String secretKey, Optional<String> token) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.token = token;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public Optional<String> getToken() {
        return token;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessKey;
        private String secretKey;
        private String token;

        public Builder setAccessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        public Builder setSecretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public AwsCredentials build() {
            return new AwsCredentials(accessKey, secretKey, Optional.ofNullable(token));
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
