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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.hazelcast.internal.config.DomConfigHelper.childElements;
import static com.hazelcast.internal.config.DomConfigHelper.cleanNodeName;

/**
 * Helper class for parsing XML strings
 */
final class XmlNode {
    private Node node;

    private XmlNode(Node node) {
        this.node = node;
    }

    static XmlNode create(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlString.getBytes()));

        return new XmlNode(doc.getDocumentElement());
    }

    Node getNode() {
        return node;
    }

    XmlNode getFirstSubNode(String name) {
        if (node == null) {
            return new XmlNode(null);
        }
        for (Node child : childElements(node)) {
            if (name.equals(cleanNodeName(child))) {
                return new XmlNode(child);
            }
        }
        return new XmlNode(null);
    }

    List<XmlNode> getSubNodes(String name) {
        List<XmlNode> result = new ArrayList<>();
        if (node == null) {
            return result;
        }
        for (Node child : childElements(node)) {
            if (name.equals(cleanNodeName(child))) {
                result.add(new XmlNode(child));
            }
        }
        return result;
    }

    String getValue(String name) {
        Node child = getFirstSubNode(name).getNode();
        return (child == null ? null : child.getFirstChild().getNodeValue());
    }

}
