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

package com.hazelcast.aws.utility;

import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonArray;
import com.hazelcast.internal.json.JsonValue;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.UTF8_ENCODING;
import static com.hazelcast.internal.config.DomConfigHelper.childElements;
import static com.hazelcast.internal.config.DomConfigHelper.cleanNodeName;
import static java.lang.String.format;

public final class MarshallingUtils {

    private static final String NODE_ITEM = "item";
    private static final String NODE_VALUE = "value";
    private static final String NODE_KEY = "key";

    private static final ILogger LOGGER = Logger.getLogger(MarshallingUtils.class);

    private MarshallingUtils() {
    }

    /**
     * Unmarshal the response from DescribeInstances and return the discovered node map.
     * The map contains mappings from private to public IP and all contained nodes match the filtering rules defined by
     * the {@code awsConfig}.
     * If there is an exception while unmarshalling the response, returns an empty map.
     *
     * @param stream the response XML stream
     * @return map from private to public IP or empty map in case of exceptions
     */
    public static Map<String, String> unmarshalDescribeInstancesResponse(InputStream stream) {
        DocumentBuilder builder;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(stream);

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

            if (LOGGER.isFinestEnabled()) {
                LOGGER.finest("Returned document:");
                LOGGER.finest(getNicelyFormattedXMLDocument(doc));
            }
            return addresses;
        } catch (Exception e) {
            LOGGER.warning(e);
        }
        return new LinkedHashMap<String, String>();
    }

    /**
     * Unmarshal the response from DescribeNetworkInterfaces and return the discovered node map.
     * The map contains mappings from private to public IP and all contained nodes match the filtering rules defined by
     * the {@code awsConfig}.
     * If there is an exception while unmarshalling the response, returns an empty map.
     *
     * @param stream the response XML stream
     * @return map from private to public IP or empty map in case of exceptions
     */
    public static Map<String, String> unmarshalDescribeNetworkInterfacesResponse(InputStream stream) {
        DocumentBuilder builder;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(stream);

            Map<String, String> addresses = new LinkedHashMap<String, String>();
            Element element = doc.getDocumentElement();

            NodeHolder elementNodeHolder = new NodeHolder(element);
            List<NodeHolder> networkInterfaceSet = elementNodeHolder.getSubNodes("networkinterfaceset");
            for (NodeHolder networkInterface : networkInterfaceSet) {
                List<NodeHolder> items = networkInterface.getSubNodes(NODE_ITEM);
                for (NodeHolder item : items) {
                    try {
                        String privateIp = item.getSubNodes("privateipaddress").get(0)
                                .getNode().getFirstChild().getNodeValue();
                        String publicIp =
                                item.getSubNodes("association").get(0).getSubNodes("publicip").get(0)
                                        .getNode().getFirstChild().getNodeValue();
                        addresses.put(privateIp, publicIp);
                    } catch (DOMException e) {
                        // ignore nonconforming interfaces
                        LOGGER.fine(e);
                    }
                }
            }

            if (LOGGER.isFinestEnabled()) {
                LOGGER.finest("Returned document:");
                LOGGER.finest(getNicelyFormattedXMLDocument(doc));
            }
            return addresses;
        } catch (Exception e) {
            LOGGER.warning(e);
        }
        return new LinkedHashMap<String, String>();
    }

    /**
     * Unmarshal the response from DescribeTasks and return the discovered tasks' private IPs.
     *
     * @param stream the response XML stream
     * @return a collection containing the private IPs of the discovered tasks
     */
    public static Collection<String> unmarshalDescribeTasksResponse(InputStream stream) {
        Collection<String> response = new ArrayList<String>();

        try {
            JsonArray jsonValues = Json.parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).asObject()
                    .get("tasks").asArray();
            for (JsonValue task : jsonValues) {
                for (JsonValue container : task.asObject().get("containers").asArray()) {
                    for (JsonValue value : container.asObject().get("networkInterfaces").asArray()) {
                        String privateIpv4Address = value.asObject().get("privateIpv4Address").asString();
                        response.add(privateIpv4Address);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Malformed response", e);
        }

        return response;
    }

    /**
     * Unmarshal the response from ListTasks and return the discovered task collection.
     * The collection contains the IDs (taskArns) of the discovered tasks.
     *
     * @param stream the response XML stream
     * @return a collection containing the taskArns of the discovered tasks
     */
    public static Collection<String> unmarshalListTasksResponse(InputStream stream) {
        ArrayList<String> response = new ArrayList<String>();

        try {
            JsonArray jsonValues = Json.parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .asObject().get("taskArns").asArray();
            for (JsonValue value : jsonValues) {
                response.add(value.asString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Malformed response", e);
        }

        return response;
    }

    private static String getNicelyFormattedXMLDocument(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        Writer stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(new DOMSource(doc), streamResult);

        return stringWriter.toString();
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
            List<NodeHolder> result = new ArrayList<>();
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
         * Unmarshal the response from the DescribeInstances service and return the map from private to public IP.
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
}
