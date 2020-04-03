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

class XmlNode {
    private Node node;

    static XmlNode create(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlString.getBytes()));

        return new XmlNode(doc.getDocumentElement());
    }

    private XmlNode(Node node) {
        this.node = node;
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
