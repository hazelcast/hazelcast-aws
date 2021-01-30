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

import com.hazelcast.aws.Tags.Tag;

import java.util.ArrayList;
import java.util.Collection;

import static com.hazelcast.aws.StringUtils.isNotEmpty;

final class TagFilters {
    private TagFilters() {
    }

    static Collection<TagFilter> from(Tags tags) {
        Collection<TagFilter> values = new ArrayList<>();
        for (Tag tag : tags) {
            if (isNotEmpty(tag.getKey()) && isNotEmpty(tag.getValue())) {
                values.add(new TagFilter("tag:" + tag.getKey(), tag.getValue()));
                continue;
            }
            if (isNotEmpty(tag.getKey())) {
                values.add(new TagFilter("tag-key", tag.getKey()));
                continue;
            }
            if (isNotEmpty(tag.getValue())) {
                values.add(new TagFilter("tag-value", tag.getValue()));
            }
        }
        return values;
    }

    static class TagFilter {
        private final String name;
        private final String value;

        private TagFilter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        String getName() {
            return name;
        }

        String getValue() {
            return value;
        }
    }
}
