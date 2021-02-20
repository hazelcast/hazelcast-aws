package com.hazelcast.aws;

import java.util.Collection;

class CollectionUtils {
    private CollectionUtils() {
    }

    static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }
}
