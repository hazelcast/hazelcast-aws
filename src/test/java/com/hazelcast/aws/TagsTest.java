package com.hazelcast.aws;

import com.hazelcast.aws.Tags.Tag;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class TagsTest {

    @Test
    public void singleTagKeyAndValue() {
        // given
        String tagKey = "KeyA";
        String tagValue = "ValueA";

        // when
        Tags tags = Tags.from(tagKey, tagValue);

        // then
        Tag tag = tags.iterator().next();
        assertEquals(tagKey, tag.getKey());
        assertEquals(tagValue, tag.getValue());
    }

    @Test
    public void multipleTagKeysAndValues() {
        // given
        String tagKey = "KeyA,KeyB";
        String tagValue = "ValueA,ValueB";

        // when
        Tags tags = Tags.from(tagKey, tagValue);

        // then
        Iterator<Tag> iterator = tags.iterator();
        Tag tag = iterator.next();
        assertEquals("KeyA", tag.getKey());
        assertEquals("ValueA", tag.getValue());

        // and
        tag = iterator.next();
        assertEquals("KeyB", tag.getKey());
        assertEquals("ValueB", tag.getValue());
    }

    @Test
    public void multipleTagKeys() {
        // given
        String tagKey = "KeyA,KeyB";

        // when
        Tags tags = Tags.from(tagKey, null);

        // then
        Iterator<Tag> iterator = tags.iterator();
        Tag tag = iterator.next();
        assertEquals("KeyA", tag.getKey());
        assertNull(tag.getValue());

        // and
        tag = iterator.next();
        assertEquals("KeyB", tag.getKey());
        assertNull(tag.getValue());
    }

    @Test
    public void multipleTagValues() {
        // given
        String tagValue = "ValueA,ValueB";

        // when
        Tags tags = Tags.from(null, tagValue);

        // then
        Iterator<Tag> iterator = tags.iterator();
        Tag tag = iterator.next();
        assertNull(tag.getKey());
        assertEquals("ValueA", tag.getValue());

        // and
        tag = iterator.next();
        assertNull(tag.getKey());
        assertEquals("ValueB", tag.getValue());
    }

    @Test
    public void singleTagKeyAndMultipleTagValues() {
        // given
        String tagKey = "KeyA";
        String tagValue = "ValueA,ValueB";

        // when
        Tags tags = Tags.from(tagKey, tagValue);

        // then
        Iterator<Tag> iterator = tags.iterator();
        Tag tag = iterator.next();
        assertEquals("KeyA", tag.getKey());
        assertEquals("ValueA", tag.getValue());

        // and
        tag = iterator.next();
        assertNull(tag.getKey());
        assertEquals("ValueB", tag.getValue());
    }

    @Test
    public void emptyTagKeyAndValue() {
        // given
        String tagKey = "";
        String tagValue = "";

        // when
        Tags tags = Tags.from(tagKey, tagValue);

        // then
        assertFalse(tags.hasTags());
    }

    @Test
    public void noTagKeyAndValue() {
        // given
        String tagKey = null;
        String tagValue = null;

        // when
        Tags tags = Tags.from(tagKey, tagValue);

        // then
        assertFalse(tags.hasTags());
    }

    @Test
    public void restructureTags() {
        // given
        String tagKey = "KeyA,KeyB";
        String tagValue = "ValueA,ValueB";

        // when
        Tags tags = Tags.from(tagKey, tagValue);

        // then
        assertEquals(tagKey, tags.getKeys());
        assertEquals(tagValue, tags.getValues());
    }
}
