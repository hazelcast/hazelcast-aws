/*
 * Copyright 2020 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.hazelcast.aws;

import com.hazelcast.aws.TagFilters.TagFilter;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class TagFiltersTest {
    @Test
    public void keyOnlyFilter() {
        // given
        Tags tags = Tags.from("KeyA", null);

        // when
        List<TagFilter> filters = asList(TagFilters.from(tags));

        // then
        assertEquals(1, filters.size());
        assertEquals("tag-key", filters.get(0).getName());
        assertEquals("KeyA", filters.get(0).getValue());
    }

    @Test
    public void valueOnlyFilter() {
        // given
        Tags tags = Tags.from(null, "ValueA");

        // when
        List<TagFilter> filters = asList(TagFilters.from(tags));

        // then
        assertEquals(1, filters.size());
        assertEquals("tag-value", filters.get(0).getName());
        assertEquals("ValueA", filters.get(0).getValue());
    }

    @Test
    public void keyAndValueFilter() {
        // given
        Tags tags = Tags.from("KeyA", "ValueA");

        // when
        List<TagFilter> filters = asList(TagFilters.from(tags));

        // then
        assertEquals(1, filters.size());
        assertEquals("tag:KeyA", filters.get(0).getName());
        assertEquals("ValueA", filters.get(0).getValue());
    }

    private static <T> List<T> asList(Collection<T> collection) {
        return (List<T>) collection;
    }
}