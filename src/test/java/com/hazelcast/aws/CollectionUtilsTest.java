package com.hazelcast.aws;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class CollectionUtilsTest {
    @Test
    public void isNotEmpty() {
        assertTrue(CollectionUtils.isNotEmpty(Collections.singletonList(new Object())));
    }
}
