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
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.util.Map;
import java.util.HashMap;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.opengis.geoapi.Departures;
import org.opengis.geoapi.DocumentationStyle;
import org.opengis.geoapi.SchemaInformation;
import org.opengis.geoapi.SchemaException;
import org.apache.sis.util.StringBuilders;


/**
 * Compares JAXB annotations against the ISO 19115 schemas. This test requires a connection to
 * <a href="https://standards.iso.org/iso/19115/-3/">https://standards.iso.org/iso/19115/-3/</a>.
 * All classes in a given directory are scanned.
 *
 * <h2>Limitations</h2>
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
public final strictfp class SchemaCompliance extends SchemaInformation {
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
     * Separator between XML prefix and the actual name.
     */
    private static final char PREFIX_SEPARATOR = ':';

    /**
     * Root directory from which to search for classes.
     */
    private final Path classRootDirectory;

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
        super(schemaRootDirectory, new Departures(), DocumentationStyle.NONE);
        this.classRootDirectory = classRootDirectory;
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
        final StringBuilder buffer = new StringBuilder();
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
     * Verifies that the relationship between the name of the given entity and its type are consistent with
     * OGC/ISO conventions. This method ignores the prefix (e.g. {@code "mdb:"} in {@code "mdb:MD_Metadata"}).
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
            if (name.startsWith(ABSTRACT_PREFIX, nameStart)) nameStart += ABSTRACT_PREFIX.length();
            if (type.startsWith(ABSTRACT_PREFIX, typeStart)) typeStart += ABSTRACT_PREFIX.length();
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
}
