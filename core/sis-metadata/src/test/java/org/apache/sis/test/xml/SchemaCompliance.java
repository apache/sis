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
package org.apache.sis.test.xml;

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
import java.util.Objects;
import java.util.Collections;
import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
import org.apache.sis.util.StringBuilders;


/**
 * Compares JAXB annotations against the ISO 19115 schema. This test requires a connection to
 * <a href="http://standards.iso.org/iso/19115/-3/">http://standards.iso.org/iso/19115/-3/</a>.
 * All classes in a given directory are scanned.
 *
 * <div class="section">Limitations</div>
 * Current implementation ignores the XML prefix (e.g. {@code "cit:"} in {@code "cit:CI_Citation"}).
 * We assume that there is no name collision, especially given that {@code "CI_"} prefix in front of
 * most OGC/ISO class names have the effect of a namespace. If a collision nevertheless happen, then
 * an exception will be thrown.
 *
 * <p>Current implementation assumes that XML element name, type name, property name and property type
 * name follow some naming convention. For example type names are suffixed with {@code "_Type"} in OGC
 * schemas, while property type names are suffixed with {@code "_PropertyType"}.  This class throws an
 * exception if a type does not follow the expected naming convention. This requirement makes
 * implementation easier, by reducing the amount of {@link Map}s that we need to manage.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class SchemaCompliance {
    /**
     * The root of ISO schemas. May be replaced by {@link #schemaRootDirectory} if a local copy
     * is available for faster tests.
     */
    private static final String SCHEMA_ROOT_DIRECTORY = "http://standards.iso.org/iso/";

    /**
     * ISO 19115-2 classes to merge with ISO 19115-1 classes. For example ISO 19115-2 defines {@code MI_Band}
     * as an extension of ISO 19115-1 {@code MD_Band}, but GeoAPI and Apache SIS merges those two types in a
     * single class for simplicity. Consequently when reading the schema, we rename some {@code MI_*} types
     * as {@code MD_*} in order to store properties together.
     */
    private static final Map<String,String> TYPES_TO_MERGE;
    static {
        final Map<String,String> m = new HashMap<>();
        // ………Merge what…………………………………………………………Into……………………………………………
        m.put("MI_Band_Type",                 "MD_Band_Type");
        m.put("MI_CoverageDescription_Type",  "MD_CoverageDescription_Type");
        m.put("MI_Georectified_Type",         "MD_Georectified_Type");
        m.put("MI_Georeferenceable_Type",     "MD_Georeferenceable_Type");
        m.put("LE_ProcessStep_Type",          "LI_ProcessStep_Type");
        m.put("AbstractMX_File_Type",         "MX_DataFile_Type");
        m.put("Abstract_DataQuality_Type",    "DQ_DataQuality_Type");
        m.put("Abstract_QualityElement_Type", "AbstractDQ_Element_Type");
        TYPES_TO_MERGE = Collections.unmodifiableMap(m);
    }

    /**
     * The prefix of XML type names for properties. In ISO/OGC schemas, this prefix does not appear
     * in the definition of class types but may appear in the definition of property types.
     */
    private static final String ABSTRACT_PREFIX = "Abstract_";

    /**
     * The suffix of XML type names for classes.
     * This is used by convention in OGC/ISO standards (but not necessarily in other XSD).
     */
    static final String TYPE_SUFFIX = "_Type";

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
    private static final String CODELIST_TYPE = "gco:CodeListValue_Type";

    /**
     * Separator between XML prefix and the actual name.
     */
    private static final char PREFIX_SEPARATOR = ':';

    /**
     * If the computer contains a local copy of ISO schemas, path to that directory. Otherwise {@code null}.
     * If non-null, the {@code "http://standards.iso.org/iso/"} prefix in URL will be replaced by that path.
     * This field is usually {@code null}, but can be set to a non-null value for making tests faster.
     */
    private final Path schemaRootDirectory;

    /**
     * Root directory from which to search for classes.
     */
    private final Path classRootDirectory;

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
     * The type and namespace of a property or class. Used in {@link #typeDefinitions} map.
     */
    static final class Info {
        final String  typeName;
        final String  namespace;
        final boolean isRequired;
        final boolean isCollection;

        Info(final String typeName, final String namespace, final boolean isRequired, final boolean isCollection) {
            this.typeName     = typeName;
            this.namespace    = namespace;
            this.isRequired   = isRequired;
            this.isCollection = isCollection;
        }

        boolean equal(final Info other) {
            return Objects.equals(typeName,     other.typeName)
                && Objects.equals(namespace,    other.namespace)
                && isRequired   == other.isRequired
                && isCollection == other.isCollection;
        }

        @Override public String toString() {
            return typeName;
        }
    }

    /**
     * Definitions of XML type for each class. In OGC/ISO schemas, those definitions have the {@value #TYPE_SUFFIX}
     * suffix in their name (which is omitted). The value is another map, where keys are property names and values
     * are their types, having the {@link #PROPERTY_TYPE_SUFFIX} suffix in their name (which is omitted).
     */
    private final Map<String, Map<String,Info>> typeDefinitions;

    /**
     * Notifies that we are about to define the XML type for each property. In OGC/ISO schemas, those definitions
     * have the {@value #PROPERTY_TYPE_SUFFIX} suffix in their name (which is omitted). After this method call,
     * properties can be defined by calls to {@link #addProperty(String, String, boolean, boolean)}.
     */
    private void preparePropertyDefinitions(final String type) throws SchemaException {
        currentProperties = typeDefinitions.computeIfAbsent(trim(type, TYPE_SUFFIX).intern(), (k) -> new HashMap<>());
    }

    /**
     * The properties of the XML type under examination, or {@code null} if none.
     * If non-null, this is one of the values in the {@link #typeDefinitions} map.
     * By convention, the {@code null} key is associated to information about the class.
     */
    private Map<String,Info> currentProperties;

    /**
     * A single property type under examination, or {@code null} if none.
     * If non-null, this is a value ending with the {@value #PROPERTY_TYPE_SUFFIX} suffix.
     */
    private String currentPropertyType;

    /**
     * Default value for the {@code required} attribute of {@link XmlElement}. This default value should
     * be {@code true} for properties declared inside a {@code <sequence>} element, and {@code false} for
     * properties declared inside a {@code <choice>} element.
     */
    private boolean requiredByDefault;

    /**
     * Namespace of the type or properties being defined.
     * This is specified by {@code <xs:schema targetNamespace="(…)">}.
     */
    private String targetNamespace;

    /**
     * The namespaces associated to prefixes, as declared by JAXB {@link XmlNs} annotations.
     * Used for verifying that no prefix is defined twice for different namespaces.
     *
     * <p>This field is not really related to schema loading process. But we keep it in this class for
     * {@link PackageVerifier} convenience, as a way to share a single map for all verifier instances.</p>
     */
    final Map<String,String> allXmlNS;

    /**
     * Creates a new verifier for classes under the given directory. The given directory shall be the
     * root of {@code "*.class"} files. For example if the {@code mypackage.MyClass} class is compiled
     * in the {@code "MyProject/target/classes/mypackage/MyClass.class"} file, then the root directory
     * shall be {@code "MyProject/target/classes/"}.
     *
     * @param  classRootDirectory   the root of compiled class files.
     * @param  schemaRootDirectory  if the computer contains a local copy of ISO schemas, path to that directory.
     *                              Otherwise {@code null}. This is only for making tests faster.
     */
    public SchemaCompliance(final Path classRootDirectory, final Path schemaRootDirectory) {
        this.classRootDirectory  = classRootDirectory;
        this.schemaRootDirectory = schemaRootDirectory;
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        buffer = new StringBuilder(100);
        typeDefinitions = new HashMap<>();
        schemaLocations = new ArrayDeque<>();
        allXmlNS = new HashMap<>();
    }

    /**
     * Verifies {@link XmlElement} annotations on all {@code *.class} files in the given directory and sub-directories.
     * The given directory must be a sub-directory of the root directory given at construction time.
     * This method will invoke itself for scanning sub-directories.
     *
     * @param  directory  the directory to scan for classes, relative to class root directory.
     * @throws IOException if an error occurred while reading files or schemas.
     * @throws ClassNotFoundException if an error occurred while loading a {@code "*.class"} file.
     * @throws ParserConfigurationException if {@link javax.xml.parsers.DocumentBuilder} can not be created.
     * @throws SAXException if an error occurred while parsing the XSD file.
     * @throws SchemaException if a XSD file does not comply with our assumptions,
     *         or a JAXB annotation failed a compliance check.
     */
    public void verify(final Path directory)
            throws IOException, ClassNotFoundException, ParserConfigurationException, SAXException, SchemaException
    {
        PackageVerifier verifier = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(classRootDirectory.resolve(directory))) {
            for (Path path : stream) {
                final String filename = path.getFileName().toString();
                if (!filename.startsWith(".")) {
                    if (Files.isDirectory(path)) {
                        verify(path);
                    } else if (filename.endsWith(".class")) {
                        path = classRootDirectory.relativize(path);
                        buffer.setLength(0);
                        buffer.append(path.toString()).setLength(buffer.length() - 6);      // Remove ".class" suffix.
                        StringBuilders.replace(buffer, '/', '.');
                        final Class<?> c = Class.forName(buffer.toString());
                        if (verifier == null) {
                            verifier = new PackageVerifier(this, c.getPackage());
                        }
                        verifier.verify(c);
                    }
                }
            }
        } catch (DirectoryIteratorException e) {
            throw e.getCause();
        }
        if (verifier != null) {
            verifier.reportUnused();
        }
    }

    /**
     * Loads the XSD file at the given URL. Definitions are stored in the {@link #typeDefinitions} map.
     * Only information of interest are stored, and we assume that the XSD follows OGC/ISO conventions.
     * This method may be invoked recursively if the XSD contains {@code <xs:include>} elements.
     *
     * @param  location  URL to the XSD file to load.
     */
    final void loadSchema(String location)
            throws IOException, ParserConfigurationException, SAXException, SchemaException
    {
        if (schemaRootDirectory != null && location.startsWith(SCHEMA_ROOT_DIRECTORY)) {
            location = schemaRootDirectory.resolve(location.substring(SCHEMA_ROOT_DIRECTORY.length())).toUri().toString();
        }
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
    private void storeClassDefinition(final Node node)
            throws IOException, ParserConfigurationException, SAXException, SchemaException
    {
        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) {
            switch (node.getNodeName()) {
                case "schema": {
                    targetNamespace = getMandatoryAttribute(node, "targetNamespace").intern();
                    break;
                }
                /*
                 * <xs:include schemaLocation="(…).xsd">
                 * Load the schema at the given URL, which is assumed relative.
                 */
                case "include": {
                    final String oldTarget = targetNamespace;
                    final String location = schemaLocations.getLast();
                    buffer.setLength(0);
                    buffer.append(location, 0, location.lastIndexOf('/') + 1).append(getMandatoryAttribute(node, "schemaLocation"));
                    loadSchema(buffer.toString());
                    targetNamespace = oldTarget;
                    return;                             // Skip children (normally, there is none).
                }
                /*
                 * <xs:element name="(…)" type="(…)_Type">
                 * Verify that the names comply with our assumptions.
                 */
                case "element": {
                    final String name = getMandatoryAttribute(node, "name");
                    final String type = getMandatoryAttribute(node, "type");
                    if (CODELIST_TYPE.equals(type)) {
                        final Map<String,Info> properties = new HashMap<>(4);
                        final Info info = new Info(null, targetNamespace, false, false);
                        properties.put(null, info);     // Remember namespace of the code list.
                        properties.put(name, info);     // Pseudo-property used in our CodeList adapters.
                        if (typeDefinitions.put(name, properties) != null) {
                            throw new SchemaException(String.format("Code list \"%s\" is defined twice.", name));
                        }
                    } else {
                        verifyNamingConvention(schemaLocations.getLast(), name, type, TYPE_SUFFIX);
                        preparePropertyDefinitions(type);
                        addProperty(null, type, false, false);
                        currentProperties = null;
                    }
                    return;                             // Ignore children (they are about documentation).
                }
                /*
                 * <xs:complexType name="(…)_Type">
                 * <xs:complexType name="(…)_PropertyType">
                 */
                case "complexType": {
                    String name = getMandatoryAttribute(node, "name");
                    if (name.endsWith(PROPERTY_TYPE_SUFFIX)) {
                        currentPropertyType = name;
                        verifyPropertyType(node);
                        currentPropertyType = null;
                    } else {
                        /*
                         * In the case of "(…)_PropertyType", replace some ISO 19115-2 types by ISO 19115-1 types.
                         * For example "MI_Band_Type" is renamed as "MD_Band_Type". We do that because we use only
                         * one class for representing those two distincts ISO types. Note that not all ISO 19115-2
                         * types extend an ISO 19115-1 type, so we need to apply a case-by-case approach.
                         */
                        requiredByDefault = true;
                        name = TYPES_TO_MERGE.getOrDefault(name, name);
                        preparePropertyDefinitions(name);
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
    private void storePropertyDefinition(final Node node) throws SchemaException {
        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) {
            switch (node.getNodeName()) {
                case "sequence": {
                    requiredByDefault = true;
                    break;
                }
                case "choice": {
                    requiredByDefault = false;
                    break;
                }
                case "element": {
                    boolean isRequired = requiredByDefault;
                    boolean isCollection = false;
                    final NamedNodeMap attributes = node.getAttributes();
                    if (attributes != null) {
                        Node attr = attributes.getNamedItem("minOccurs");
                        if (attr != null) {
                            final String value = attr.getNodeValue();
                            if (value != null) {
                                isRequired = Integer.parseInt(value) > 0;
                            }
                        }
                        attr = attributes.getNamedItem("maxOccurs");
                        if (attr != null) {
                            final String value = attr.getNodeValue();
                            if (value != null) {
                                isCollection = value.equals("unbounded") || Integer.parseInt(value) >  1;
                            }
                        }
                    }
                    addProperty(getMandatoryAttribute(node, "name").intern(),
                           trim(getMandatoryAttribute(node, "type"), PROPERTY_TYPE_SUFFIX).intern(), isRequired, isCollection);
                    return;
                }
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
    private void verifyPropertyType(final Node node) throws SchemaException {
        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) {
            if ("element".equals(node.getNodeName())) {
                verifyNamingConvention(schemaLocations.getLast(),
                        getMandatoryAttribute(node, "ref"), currentPropertyType, PROPERTY_TYPE_SUFFIX);
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
     * @param  enclosing  schema or other container where the error happened.
     * @param  name       the class or property name. Example: {@code "MD_Metadata"}, {@code "citation"}.
     * @param  type       the type of the above named object. Example: {@code "MD_Metadata_Type"}, {@code "CI_Citation_PropertyType"}.
     * @param  suffix     the expected suffix at the end of {@code type}.
     * @throws SchemaException if the given {@code name} and {@code type} are not compliant with expected convention.
     */
    static void verifyNamingConvention(final String enclosing,
            final String name, final String type, final String suffix) throws SchemaException
    {
        if (type.endsWith(suffix)) {
            int nameStart = name.indexOf(PREFIX_SEPARATOR) + 1;        // Skip "mdb:" or similar prefix.
            int typeStart = type.indexOf(PREFIX_SEPARATOR) + 1;
            final int plg = ABSTRACT_PREFIX.length();
            if (name.regionMatches(nameStart, ABSTRACT_PREFIX, 0, plg)) nameStart += plg;
            if (type.regionMatches(typeStart, ABSTRACT_PREFIX, 0, plg)) typeStart += plg;
            final int length = name.length() - nameStart;
            if (type.length() - typeStart - suffix.length() == length &&
                    type.regionMatches(typeStart, name, nameStart, length))
            {
                return;
            }
        }
        throw new SchemaException(String.format("Error in %s:%n" +
                "The type name should be the name with \"%s\" suffix, but found name=\"%s\" and type=\"%s\">.",
                enclosing, suffix, name, type));
    }

    /**
     * Adds a property of the current name and type. This method is invoked during schema parsing.
     * The property namespace is assumed to be {@link #targetNamespace}.
     */
    private void addProperty(final String name, final String type, final boolean isRequired, final boolean isCollection) throws SchemaException {
        final Info info = new Info(type, targetNamespace, isRequired, isCollection);
        final Info old = currentProperties.put(name, info);
        if (old != null && !old.equal(info)) {
            throw new SchemaException(String.format("Error while parsing %s:%n" +
                    "Property \"%s\" is associated to type \"%s\", but that property was already associated to \"%s\".",
                    schemaLocations.getLast(), name, type, old));
        }
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
    static String trim(String name, final String suffix) throws SchemaException {
        name = name.trim();
        if (name.endsWith(suffix)) {
            return name.substring(name.indexOf(PREFIX_SEPARATOR) + 1, name.length() - suffix.length());
        }
        throw new SchemaException(String.format("Expected a name ending with \"%s\" but got \"%s\".", suffix, name));
    }

    /**
     * Returns the attribute of the given name in the given node,
     * or throws an exception if the attribute is not present.
     */
    private static String getMandatoryAttribute(final Node node, final String name) throws SchemaException {
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
        throw new SchemaException(String.format("Node \"%s\" should have a '%s' attribute.", node.getNodeName(), name));
    }

    /**
     * Returns the type definitions for a class of the given name.
     *
     * @param  className  ISO identifier of a class (e.g. {@code "MD_Metadata"}).
     */
    final Map<String, SchemaCompliance.Info> typeDefinition(final String className) {
        return typeDefinitions.get(className);
    }
}
