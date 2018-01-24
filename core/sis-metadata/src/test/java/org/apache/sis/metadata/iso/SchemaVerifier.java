/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.metadata.iso;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.util.Map;
import java.util.Deque;
import java.util.HashMap;
import java.util.ArrayDeque;
import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
import org.apache.sis.util.StringBuilders;



/**
 * Compares the {@link XmlElement} against the ISO 19115 schema. This test requires a connection
 * to <a href="http://standards.iso.org/iso/19115/-3/">http://standards.iso.org/iso/19115/-3/</a>.
 * All classes in a given directory are scanned.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class SchemaVerifier {
    /**
     * The suffix of XML type names for classes.
     * This is used by convention in OGC/ISO standards (but not necessarily in other XSD).
     */
    private static final String TYPE_SUFFIX = "_Type";

    /**
     * The suffix of XML property type names in a given class.
     * This is used by convention in OGC/ISO standards (but not necessarily in other XSD).
     */
    private static final String PROPERTY_TYPE_SUFFIX = "_PropertyType";

    /**
     * XML type to ignore because of key collisions in {@link #typeDefinitions}.
     * Those collisions occur because code lists are defined as links to the same file,
     * with only different anchor positions.
     */
    private static final String TYPE_TO_IGNORE = "gco:CodeListValue_Type";

    /**
     * Separator between XML prefix and the actual name.
     */
    private static final char PREFIX_SEPARATOR = ':';

    /**
     * Root directory from which to search for classes.
     */
    private final Path rootDirectory;

    /**
     * A temporary buffer for miscellaneous string operations.
     * Valid only in a local scope since the content may change at any time.
     */
    private final StringBuilder buffer;

    /**
     * The DOM factory used for reading XSD schemas.
     */
    private final DocumentBuilderFactory factory;

    /**
     * URL of schemas loaded, for avoiding loading the same schema many time.
     * The last element on the queue is the schema in process of being loaded,
     * used for resolving relative paths in {@code <xs:include>} elements.
     */
    private final Deque<String> schemaLocations;

    /**
     * Definition of XML types for each classes. In OGC/ISO schemas, those definitions have the {@value #TYPE_SUFFIX}
     * suffix in their name (which is omitted). The value is another map, where keys are property names and values are
     * their types, having the {@link #PROPERTY_TYPE_SUFFIX} suffix in their name (which is omitted).
     */
    private final Map<String, Map<String,String>> typeDefinitions;

    /**
     * The properties of the XML type under examination, or {@code null} if none.
     * If non-null, this is one of the values in the {@link #typeDefinitions} map.
     */
    private transient Map<String,String> currentProperties;

    /**
     * A single property type under examination, or {@code null} if none.
     * If non-null, this is a value ending with the {@value #PROPERTY_TYPE_SUFFIX} suffix.
     */
    private transient String currentPropertyType;

    /**
     * Creates a new verifier for classes under the given directory. The given directory shall be the
     * root of {@code "*.class"} files. For example if the {@code mypackage.MyClass} class is compiled
     * in the {@code "MyProject/target/classes/mypackage/MyClass.class"} file, then the root directory
     * shall be {@code "MyProject/target/classes/"}.
     *
     * @param  rootDirectory  the root of compiled class files.
     */
    public SchemaVerifier(final Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        buffer = new StringBuilder(100);
        typeDefinitions = new HashMap<>();
        schemaLocations = new ArrayDeque<>();
    }

    /**
     * Verifies {@link XmlElement} annotations on all {@code *.class} files in the given directory and sub-directories.
     * The given directory must be a sub-directory of the root directory given at construction time.
     * This method will invoke itself for scanning sub-directories.
     *
     * @param  directory  the directory to scan for classes.
     * @throws IOException if an error occurred while reading files or schemas.
     * @throws ClassNotFoundException if an error occurred while loading a {@code "*.class"} file.
     * @throws ParserConfigurationException if {@link javax.xml.parsers.DocumentBuilder} can not be created.
     * @throws SAXException if an error occurred while parsing the XSD file.
     */
    public void verify(final Path directory) throws IOException, ClassNotFoundException, ParserConfigurationException, SAXException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                final String filename = path.getFileName().toString();
                if (!filename.startsWith(".")) {
                    if (Files.isDirectory(path)) {
                        verify(path);
                    } else if (filename.endsWith(".class")) {
                        path = rootDirectory.relativize(path);
                        buffer.setLength(0);
                        buffer.append(path.toString()).setLength(buffer.length() - 6);      // Remove ".class" suffix.
                        StringBuilders.replace(buffer, '/', '.');
                        verify(Class.forName(buffer.toString()));
                    }
                }
            }
        } catch (DirectoryIteratorException e) {
            throw e.getCause();
        }
    }

    /**
     * Loads the XSD file at the given URL. Definitions are stored in the {@link #typeDefinitions} map.
     * Only information of interest are stored, and we assume that the XSD follows OGC/ISO conventions.
     * This method may be invoked recursively if the XSD contains {@code <xs:include>} elements.
     *
     * @param  location  URL to the XSD file to load.
     */
    private void loadSchema(final String location) throws IOException, ParserConfigurationException, SAXException {
        if (!schemaLocations.contains(location)) {
            final Document doc;
            try (final InputStream in = new URL(location).openStream()) {
                doc = factory.newDocumentBuilder().parse(in);
            }
            schemaLocations.addLast(location);
            storeClassDefinition(doc);
        }
    }

    /**
     * Stores information about classes in the given node and children. This method invokes itself
     * for scanning children, until we reach sub-nodes about properties (in which case we continue
     * with {@link #storePropertyDefinition(Node)}).
     */
    private void storeClassDefinition(final Node node) throws IOException, ParserConfigurationException, SAXException {
        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) {
            switch (node.getNodeName()) {
                /*
                 * <xs:include schemaLocation="(…).xsd">
                 * Load the schema at the given URL, which is assumed relative.
                 */
                case "include": {
                    final String location = schemaLocations.getLast();
                    buffer.setLength(0);
                    buffer.append(location, 0, location.lastIndexOf('/') + 1).append(getMandatoryAttribute(node, "schemaLocation"));
                    loadSchema(buffer.toString());
                    return;                             // Skip children (normally, there is none).
                }
                /*
                 * <xs:element name="(…)" type="(…)_Type">
                 * There is actually nothing to store here;
                 * we just verify that the names comply with our assumptions.
                 */
                case "element": {
                    final String type = getMandatoryAttribute(node, "type");
                    if (!TYPE_TO_IGNORE.equals(type)) {
                        verifyNamingConvention(getMandatoryAttribute(node, "name"), type, TYPE_SUFFIX);
                    }
                    return;                             // Ignore children (they are about documentation).
                }
                /*
                 * <xs:complexType name="(…)_Type">
                 * <xs:complexType name="(…)_PropertyType">
                 */
                case "complexType": {
                    final String name = getMandatoryAttribute(node, "name");
                    if (name.endsWith(PROPERTY_TYPE_SUFFIX)) {
                        currentPropertyType = name;
                        verifyPropertyType(node);
                        currentPropertyType = null;
                    } else {
                        currentProperties = typeDefinitions.computeIfAbsent(trim(name, TYPE_SUFFIX).intern(), (k) -> new HashMap<>());
                        storePropertyDefinition(node);
                        currentProperties = null;
                    }
                    return;                             // Skip children since they have already been examined.
                }
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            storeClassDefinition(child);
        }
    }

    /**
     * Stores information about properties in the current class. The {@link #currentProperties} field must be
     * set to the map of properties for the class defined by the enclosing {@code <xs:complexType>} element.
     * This method parses elements of the following form:
     *
     * {@preformat xml
     *   <xs:element name="(…)" type="(…)_PropertyType" minOccurs="(…)" maxOccurs="(…)">
     * }
     */
    private void storePropertyDefinition(final Node node) throws SAXException {
        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) {
            if ("element".equals(node.getNodeName())) {
                final String type = trim(getMandatoryAttribute(node, "type"), PROPERTY_TYPE_SUFFIX).intern();
                final String name = getMandatoryAttribute(node, "name").intern();
                final String old = currentProperties.put(name, type);
                if (old != null && !old.equals(type)) {
                    throw new SchemaException("\"" + name + "\" is already associated to \"" + old + "\". Can not associated to \"" + type + "\".");
                }
                return;
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            storePropertyDefinition(child);
        }
    }

    /**
     * Verifies the naming convention of property defined by the given node. The {@link #currentPropertyType}
     * field must be set to the type of the property defined by the enclosing {@code <xs:complexType>} element.
     * This method parses elements of the following form:
     *
     * {@preformat xml
     *   <xs:element ref="(…)">
     * }
     */
    private void verifyPropertyType(final Node node) throws SAXException {
        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) {
            if ("element".equals(node.getNodeName())) {
                verifyNamingConvention(getMandatoryAttribute(node, "ref"), currentPropertyType, PROPERTY_TYPE_SUFFIX);
                return;
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            verifyPropertyType(child);
        }
    }

    /**
     * Verifies that the relationship between the name of the given entity and its type are consistent with
     * OGC/ISO conventions. This method ignore the prefix (e.g. {@code "mdb:"} in {@code "mdb:MD_Metadata"}).
     *
     * @param  name    the class or property name. Example: {@code "MD_Metadata"}, {@code "citation"}.
     * @param  type    the type of the above named object. Example: {@code "MD_Metadata_Type"}, {@code "CI_Citation_PropertyType"}.
     * @param  suffix  the expected suffix at the end of {@code type}.
     * @throws SchemaException if the given {@code name} and {@code type} are not compliant with expected convention.
     */
    private static void verifyNamingConvention(final String name, final String type, final String suffix) throws SchemaException {
        if (type.endsWith(suffix)) {
            final int nameStart = name.indexOf(PREFIX_SEPARATOR) + 1;        // Skip "mdb:" or similar prefix.
            final int typeStart = type.indexOf(PREFIX_SEPARATOR) + 1;
            final int length    = name.length() - nameStart;
            if (type.length() - typeStart - suffix.length() == length &&
                    type.regionMatches(typeStart, name, nameStart, length))
            {
                return;
            }
        }
        throw new SchemaException("<element name=\"" + name + "\" type=\"" + type + "\">: "
                + "'type' should be 'name' with \"" + suffix + "\" suffix.");
    }

    /**
     * Removes leading and trailing spaces if any, then the prefix and the suffix in the given name.
     * The prefix is anything before the first {@value #PREFIX_SEPARATOR} character.
     * The suffix must be the given string, otherwise an exception is thrown.
     *
     * @param  name     the name from which to remove prefix and suffix.
     * @param  suffix   the suffix to remove.
     * @return the given name without prefix and suffix.
     * @throws SchemaException if the given name does not end with the given suffix.
     */
    private static String trim(String name, final String suffix) throws SchemaException {
        name = name.trim();
        if (name.endsWith(suffix)) {
            return name.substring(name.indexOf(PREFIX_SEPARATOR) + 1, name.length() - suffix.length());
        }
        throw new SchemaException("Expected a name ending with \"" + suffix + "\" but got \"" + name + "\".");
    }

    /**
     * Returns the attribute of the given name in the given node,
     * or throws an exception if the attribute is not present.
     */
    private static String getMandatoryAttribute(final Node node, final String name) throws SAXException {
        final NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            final Node attr = attributes.getNamedItem(name);
            if (attr != null) {
                final String value = attr.getNodeValue();
                if (value != null) {
                    return value;
                }
            }
        }
        throw new SchemaException("Node " + node.getNodeName() + " should have an attribute '" + name + "'.");
    }

    /**
     * Verifies the {@link XmlElement} annotation on the given class.
     *
     * @param  type  the class on which to verify annotations.
     */
    private void verify(final Class<?> type) throws IOException, ParserConfigurationException, SAXException {
        final Package pkg = type.getPackage();
        final XmlSchema schema = pkg.getAnnotation(XmlSchema.class);
        if (schema != null) {
            final String location = schema.location();
            if (XmlSchema.NO_LOCATION.equals(location)) {
                throw new SchemaException("Package " + pkg.getName() + " does not specify XML schema location.");
            }
            loadSchema(location);
        }
        // TODO
    }
}
