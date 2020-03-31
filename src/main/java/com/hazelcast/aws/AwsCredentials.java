package com.hazelcast.aws;

public final class AwsCredentials {
    private String accessKey;
    private String secretKey;
    private String token;

    private AwsCredentials(String accessKey, String secretKey, String token) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.token = token;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
            return new AwsCredentials(accessKey, secretKey, token);
        }
    }

    @Override
    public String toString() {
        return "AwsCredentials{" +
            "accessKey='" + accessKey + '\'' +
            ", secretKey='" + secretKey + '\'' +
            ", token='" + token + '\'' +
            '}';
    }
}
