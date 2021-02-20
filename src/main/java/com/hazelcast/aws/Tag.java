package com.hazelcast.aws;

class Tag {
    private final String key;
    private final String value;

    Tag(String key, String value) {
        if (key == null && value == null) {
            throw new IllegalArgumentException("Tag requires at least key or value");
        }
        this.key = key;
        this.value = value;
    }

    String getKey() {
        return key;
    }

    String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("(key=%s, value=%s)", key, value);
    }
}
