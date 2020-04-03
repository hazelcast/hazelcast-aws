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

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.hazelcast.internal.config.DomConfigHelper.childElements;
import static com.hazelcast.internal.config.DomConfigHelper.cleanNodeName;
import static java.lang.String.format;

final class CloudyUtility {

    private static final String NODE_ITEM = "item";
    private static final String NODE_VALUE = "value";
    private static final String NODE_KEY = "key";
    private static final int LAST_INDEX = 8;

    private static final ILogger LOGGER = Logger.getLogger(CloudyUtility.class);

    private CloudyUtility() {
    }

    /**
     * Unmarshal the response from {@link com.hazelcast.aws.AwsDescribeInstancesApi} and return the discovered node map.
     * The map contains mappings from private to public IP and all contained nodes match the filtering rules defined by
     * the {@code awsConfig}.
     * If there is an exception while unmarshalling the response, returns an empty map.
     *
     * @param stream the response XML stream
     * @return map from private to public IP or empty map in case of exceptions
     */
    static Map<String, String> parseResponse(String response) {
        DocumentBuilder builder;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(response.getBytes()));

            Element element = doc.getDocumentElement();
            NodeHolder elementNodeHolder = new NodeHolder(element);
            Map<String, String> addresses = new LinkedHashMap<String, String>();
            List<NodeHolder> reservationSet = elementNodeHolder.getSubNodes("reservationset");
            for (NodeHolder reservation : reservationSet) {
                List<NodeHolder> items = reservation.getSubNodes(NODE_ITEM);
                for (NodeHolder item : items) {
                    NodeHolder instancesSet = item.getFirstSubNode("instancesset");
                    addresses.putAll(instancesSet.getAddresses());
                }
            }
            return addresses;
        } catch (Exception e) {
            LOGGER.warning(e);
        }
        return new LinkedHashMap<>();
    }

    private static class NodeHolder {

        private final Node node;

        NodeHolder(Node node) {
            this.node = node;
        }

        private static String getInstanceName(NodeHolder nodeHolder) {
            NodeHolder tagSetHolder = nodeHolder.getFirstSubNode("tagset");
            if (tagSetHolder.getNode() == null) {
                return null;
            }
            for (NodeHolder itemHolder : tagSetHolder.getSubNodes(NODE_ITEM)) {
                Node keyNode = itemHolder.getFirstSubNode(NODE_KEY).getNode();
                if (keyNode == null || keyNode.getFirstChild() == null) {
                    continue;
                }
                String nodeValue = keyNode.getFirstChild().getNodeValue();
                if (!"Name".equals(nodeValue)) {
                    continue;
                }

                Node valueNode = itemHolder.getFirstSubNode(NODE_VALUE).getNode();
                if (valueNode == null || valueNode.getFirstChild() == null) {
                    continue;
                }
                return valueNode.getFirstChild().getNodeValue();
            }
            return null;
        }

        private static String getIp(String name, NodeHolder nodeHolder) {
            Node child = nodeHolder.getFirstSubNode(name).getNode();
            return (child == null ? null : child.getFirstChild().getNodeValue());
        }

        Node getNode() {
            return node;
        }

        NodeHolder getFirstSubNode(String name) {
            if (node == null) {
                return new NodeHolder(null);
            }
            for (Node child : childElements(node)) {
                if (name.equals(cleanNodeName(child))) {
                    return new NodeHolder(child);
                }
            }
            return new NodeHolder(null);
        }

        List<NodeHolder> getSubNodes(String name) {
            List<NodeHolder> result = new ArrayList<NodeHolder>();
            if (node == null) {
                return result;
            }
            for (Node child : childElements(node)) {
                if (name.equals(cleanNodeName(child))) {
                    result.add(new NodeHolder(child));
                }
            }
            return result;
        }

        /**
         * Unmarshal the response from the {@link com.hazelcast.aws.AwsDescribeInstancesApi} service and
         * return the map from private to public IP.
         * This method expects that the DOM containing the XML has been positioned at the node containing the addresses.
         *
         * @return map from private to public IP
         * @see #getFirstSubNode(String)
         */
        Map<String, String> getAddresses() {
            Map<String, String> privatePublicPairs = new LinkedHashMap<String, String>();
            if (node == null) {
                return privatePublicPairs;
            }

            for (NodeHolder childHolder : getSubNodes(NODE_ITEM)) {
                String privateIp = getIp("privateipaddress", childHolder);
                String publicIp = getIp("ipaddress", childHolder);
                String instanceName = getInstanceName(childHolder);

                if (privateIp != null) {
                    privatePublicPairs.put(privateIp, publicIp);
                    LOGGER.finest(format("Accepting EC2 instance [%s][%s]", instanceName, privateIp));
                }

            }
            return privatePublicPairs;
        }
    }

    static String getCanonicalizedQueryString(Map<String, String> attributes) {
        List<String> components = getListOfEntries(attributes);
        Collections.sort(components);
        return getCanonicalizedQueryString(components);
    }

    private static String getCanonicalizedQueryString(List<String> list) {
        Iterator<String> it = list.iterator();
        StringBuilder result = new StringBuilder(it.next());
        while (it.hasNext()) {
            result.append('&').append(it.next());
        }
        return result.toString();
    }

    private static void addComponents(List<String> components, Map<String, String> attributes, String key) {
        components.add(AwsURLEncoder.urlEncode(key) + '=' + AwsURLEncoder.urlEncode(attributes.get(key)));
    }

    private static List<String> getListOfEntries(Map<String, String> entries) {
        List<String> components = new ArrayList<String>();
        for (String key : entries.keySet()) {
            addComponents(components, entries, key);
        }
        return components;
    }
}
