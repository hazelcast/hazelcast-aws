package com.hazelcast.aws;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class FilterTest {

    @Test
    public void getFilterAttributes() {
        // given
        Filter filter = new Filter();

        // when
        filter.addFilter("key", "value");
        filter.addFilter("second-key", "second-value");
        Map<String, String> result = filter.getFilterAttributes();

        // then
        assertEquals(4, result.size());
        assertEquals("key", result.get("Filter.1.Name"));
        assertEquals("value", result.get("Filter.1.Value.1"));
        assertEquals("second-key", result.get("Filter.2.Name"));
        assertEquals("second-value", result.get("Filter.2.Value.1"));
    }

}