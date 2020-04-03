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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.hazelcast.internal.config.DomConfigHelper.childElements;
import static com.hazelcast.internal.config.DomConfigHelper.cleanNodeName;
import static java.lang.String.format;

final class CloudyUtility {

    private static final ILogger LOGGER = Logger.getLogger(CloudyUtility.class);

    private CloudyUtility() {
    }

    /**
     * Parses the response from DescribeInstances API.
     *
     * @param xmlResponse the response XML stream
     * @return map from private to public IP or empty map in case of exceptions
     */
    static Map<String, String> parse(String xmlResponse) {
        DocumentBuilder builder;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes()));

            Element element = doc.getDocumentElement();
            NodeHolder elementNodeHolder = new NodeHolder(element);
            Map<String, String> addresses = new LinkedHashMap<>();
            List<NodeHolder> reservationSet = elementNodeHolder.getSubNodes("reservationset");
            for (NodeHolder reservation : reservationSet) {
                List<NodeHolder> items = reservation.getSubNodes("item");
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
            for (NodeHolder itemHolder : tagSetHolder.getSubNodes("item")) {
                Node keyNode = itemHolder.getFirstSubNode("key").getNode();
                if (keyNode == null || keyNode.getFirstChild() == null) {
                    continue;
                }
                String nodeValue = keyNode.getFirstChild().getNodeValue();
                if (!"Name".equals(nodeValue)) {
                    continue;
                }

                Node valueNode = itemHolder.getFirstSubNode("value").getNode();
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

            for (NodeHolder childHolder : getSubNodes("item")) {
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
}
