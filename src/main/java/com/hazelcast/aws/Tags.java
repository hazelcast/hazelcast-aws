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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.hazelcast.aws.StringUtils.isEmpty;

final class Tags implements Iterable<Tags.Tag> {
    private static final String SEPARATOR = ",";
    private final Collection<Tag> tags = new ArrayList<>();

    private Tags(Collection<Tag> tags) {
        this.tags.addAll(tags);
    }

    static Tags from(String key, String value) {
        Iterator<String> keys = splitValue(key).iterator();
        Iterator<String> values = splitValue(value).iterator();

        Collection<Tag> tags = new ArrayList<>();

        while (keys.hasNext() || values.hasNext()) {
            if (keys.hasNext() && values.hasNext()) {
                tags.add(new Tag(keys.next(), values.next()));
                continue;
            }
            if (keys.hasNext()) {
                tags.add(new Tag(keys.next(), null));
                continue;
            }
            tags.add(new Tag(null, values.next()));
        }

        return new Tags(tags);
    }

    private static Collection<String> splitValue(String value) {
        return isEmpty(value) ? Collections.emptyList() : Arrays.asList(value.split(SEPARATOR));
    }

    @Override
    public Iterator<Tag> iterator() {
        return new ArrayList<>(tags).iterator();
    }

    boolean hasTags() {
        return !tags.isEmpty();
    }

    String getKeys() {
        return tags.stream()
                .map(Tag::getKey)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(SEPARATOR));
    }

    String getValues() {
        return tags.stream()
                .map(Tag::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public String toString() {
        return "Tags{" +
                "tagKey='" + getKeys() + '\''
                + ", tagValue='" + getValues() + '\''
                + '}';
    }

    static class Tag {
        private final String key;
        private final String value;

        private Tag(String key, String value) {
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
    }
}
