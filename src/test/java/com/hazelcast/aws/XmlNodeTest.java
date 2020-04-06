package com.hazelcast.aws;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

public class XmlNodeTest {

    @Test
    public void parse() throws IOException, SAXException, ParserConfigurationException {
        // given
        //language=XML
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<root xmlns=\"http://ec2.amazonaws.com/doc/2016-11-15/\">\n"
            + "    <parent>\n"
            + "        <item>\n"
            + "            <key>value</key>\n"
            + "        </item>\n"
            + "        <item>\n"
            + "            <key>second-value</key>\n"
            + "        </item>\n"
            + "    </parent>\n"
            + "</root>";

        // when
        List<String> itemValues = XmlNode.create(xml)
            .getFirstSubNode("parent")
            .getSubNodes("item")
            .stream()
            .map(item -> item.getValue("key"))
            .collect(Collectors.toList());

        // then
        assertThat(itemValues, hasItems("value", "second-value"));
    }

}