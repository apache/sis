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
package org.apache.sis.xml;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.InvalidPropertiesFormatException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * Base class of XML reader or writer replacing the namespaces used in JAXB annotations by namespaces used in
 * the XML document, or conversely (depending on the direction of the I/O operation).  The {@code Transform*}
 * classes in this package perform a work similar to XSLT transformers, but using a lighter implementation at
 * the expense of less transformation capabilities. This transformer supports:
 *
 * <ul>
 *   <li>Renaming namespaces, which may depend on the parent element.</li>
 *   <li>Renaming elements (classes or properties).</li>
 *   <li>Moving elements provided that the old and new locations are in the same parent element.</li>
 * </ul>
 *
 * If a more complex transformation is needed, it should be handled by JAXB methods. This {@code Transformer}
 * is not expected to transform fully a XML document by itself. It is rather designed to complete JAXB: some
 * transformations are better handled with Java methods annotated with JAXB (see for example the deprecated
 * methods in {@link org.apache.sis.metadata.iso} packages for legacy ISO 19115:2003 properties), and some
 * transformations, in particular namespace changes, are better handled by this {@code Transformer}.
 *
 * <div class="section">Why using {@code Transformer}</div>
 * When the XML schemas of an international standard is updated, the URL of the namespace is often modified.
 * For example when GML has been updated from version 3.1 to 3.2, the URL mandated by the international standard
 * changed from {@code "http://www.opengis.net/gml"} to {@code "http://www.opengis.net/gml/3.2"}
 * (XML namespaces usually have a version number or publication year - GML before 3.2 were an exception).
 * The problem is that namespaces in JAXB annotations are static. The straightforward solution is
 * to generate complete new set of classes for every GML version using the {@code xjc} compiler.
 * But this approach has many inconvenient:
 *
 * <ul>
 *   <li>Massive code duplication (hundreds of classes, many of them strictly identical except for the namespace).</li>
 *   <li>Handling of above-cited classes duplication requires either a bunch of {@code if (x instanceof Y)} in every
 *       SIS corners, or to modify the {@code xjc} output in order to give to generated classes a common parent class
 *       or interface. In the later case, the auto-generated classes require significant work anyways.</li>
 *   <li>The namespaces of all versions appear in the {@code xmlns} attributes of the root element (we can not always
 *       create separated JAXB contexts), which is confusing and prevent usage of usual prefixes for all versions
 *       except one.</li>
 * </ul>
 *
 * An alternative is to support only one version of each standard, and transform XML documents before unmarshalling
 * or after marshalling if they use different versions of standards. We could use XSLT for that, but this is heavy.
 * A lighter approach is to use {@link javax.xml.stream.XMLEventReader} and {@link javax.xml.stream.XMLEventWriter}
 * as "micro-transformers". One advantage is that they can transform on-the-fly (no need to load and transform the
 * document in memory). It also avoid the need to detect in advance which schemas a XML document is using, and can
 * handle the cases where mixed versions are used.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 *
 * @see javax.xml.transform.Transformer
 *
 * @since 1.0
 * @module
 */
abstract class Transformer {
    /**
     * Character used for separating an old name from the new name. For example in {@code SV_OperationMetadata},
     * {@code "DCP"} in ISO 19139:2007 has been renamed {@code "distributedComputingPlatform"} in ISO 19115-3.
     * This is encoded in {@value TransformingReader#FILENAME} file as {@code "DCP/distributedComputingPlatform"}.
     */
    private static final char RENAME_SEPARATOR = '/';

    /**
     * A sentinel value in files loaded by {@link #load(String)} meaning that the identifier stands for the type
     * name instead than for a property name. This sentinel value is used in {@value TransformingReader#FILENAME}
     * and {@value TransformingWriter#FILENAME} resource files. For example in the following:
     *
     * {@preformat text
     *  http://standards.iso.org/iso/19115/-3/cit/1.0
     *   CI_Citation
     *    <type>
     *    title
     *    alternateTitle
     * }
     *
     * The {@code <type>} sentinel value is substituted by {@code CI_Citation}, which means that the {@code Citation}
     * class itself is in the {@code cit} namespace. This needs to be specified explicitely because the properties in
     * a class are not necessarily in the same namespace than the enclosing class.
     */
    static final String TYPE_KEY = "<type>";

    /**
     * The external XML format version to (un)marshal from.
     */
    final TransformVersion version;

    /**
     * List of encountered XML tags, used for backtracking. Elements are removed from this list when they are closed.
     * Names should be the ones we get after conversion from namespaces used in XML document to namespaces used in
     * JAXB annotations. For example given the following XML, this list should contains {@code cit:CI_Citation},
     * {@code cit:date} and {@code cit:CI_Date} (in that order) when the (un)marshalling reaches the "…" location.
     *
     * {@preformat xml
     *   <cit:CI_Citation>
     *     <cit:date>
     *       <cit:CI_Date>
     *         …
     *       </cit:CI_Date>
     *     </cit:date>
     *   </cit:CI_Citation>
     * }
     */
    private final List<QName> outerElements;

    /**
     * Properties of the last outer elements, or {@code null} if not yet determined.
     * If non-empty, this is one of the values got from the map given in argument to {@link #open(QName, Map)}.
     */
    private Map<String,String> outerElementProperties;

    /**
     * Temporary list of attributes after their namespace change.
     * This list is recycled for each XML element to be read or written.
     */
    final List<Attribute> renamedAttributes;

    /**
     * Creates a new XML reader or writer.
     */
    Transformer(final TransformVersion version) {
        this.version           = version;
        outerElements          = new ArrayList<>();
        renamedAttributes      = new ArrayList<>();
        outerElementProperties = Collections.emptyMap();
    }

    /**
     * Removes the trailing slash in given URI, if any. It is caller's responsibility
     * to ensure that the URI is not null and not empty before to invoke this method.
     */
    static String removeTrailingSlash(String uri) {
        final int end = uri.length() - 1;
        if (uri.charAt(end) == '/') {
            uri = uri.substring(0, end);
        }
        return uri;
    }

    /**
     * Returns {@code true} if the given string is a namespace URI, or {@code false} if it is a property name.
     * This method implements a very fast check based on the presence of {@code ':'} in {@code "http://foo.bar"}.
     * It assumes that all namespaces declared in files loaded by {@link #load(String)} use the {@code "http"}
     * protocol and no property name use the {@code ':'} character.
     */
    static boolean isNamespace(final String candidate) {
        return (candidate.length() > 4) && (candidate.charAt(4) == ':');
    }

    /**
     * Loads a file listing types and properties contained in namespaces.
     * The file location is relative to the {@code Transformer} class.
     * The file format is a tree structured with indentation as below:
     *
     * <ul>
     *   <li>Lines with zero-space indentation are namespace URIs.</li>
     *   <li>Lines with one-space  indentation are classes within the last namespace URIs found so far.</li>
     *   <li>Lines with two-spaces indentation are properties within the last class found so far.</li>
     *   <li>All other indentations are illegal and cause an {@link InvalidPropertiesFormatException} to be thrown.
     *       This exception type is not really appropriate since the file format is not a {@code .properties} file,
     *       but it is the closest we could find in existing exceptions and we don't want to define a new exception
     *       type since this error should never happen.</li>
     * </ul>
     *
     * The returned map is structured as below:
     *
     * <ul>
     *   <li>Keys are XML names of types, ignoring {@code "_TYPE"} suffix (e.g. {@code "CI_Citation"})</li>
     *   <li>Values are maps where:<ul>
     *     <li>Keys are XML names of properties (e.g. {@code "title"})</li>
     *     <li>Values are either:<ul>
     *       <li>Namespace URI if {@link #isNamespace(String)} returns {@code true} for that value.</li>
     *       <li>New name of the element otherwise. In such case, the map must be queried again with
     *           that new name for obtaining the namespace.</li>
     *     </ul></li>
     *   </ul></li>
     * </ul>
     */
    static Map<String, Map<String,String>> load(final String filename) {
        final Map<String, Map<String,String>> m = new HashMap<>(250);
        try (LineNumberReader in = new LineNumberReader(new InputStreamReader(
                TransformingReader.class.getResourceAsStream(filename), "UTF-8")))
        {
            Map<String,String> attributes = null;               // All attributes for a given type.
            String namespace = null;                            // Value to store in 'attributes' map.
            String type = null;                                 // Class or code list containing attributes.
            String line;
            while ((line = in.readLine()) != null) {
                final int length = line.length();
                final int start = CharSequences.skipLeadingWhitespaces(line, 0, length);
                if (start < length && line.charAt(start) != '#') {
                    String element = line.substring(start).trim();
                    switch (start) {
                        case 0: {                                                   // New namespace URI.
                            if (!isNamespace(element)) break;                       // Report illegal format.
                            namespace  = element.intern();
                            attributes = null;
                            type       = null;
                            continue;
                        }
                        case 1: {                                                   // New type in above namespace URI.
                            type = element.intern();
                            attributes = m.computeIfAbsent(type, (k) -> new HashMap<>());
                            continue;
                        }
                        case 2: {                                                   // New attribute in above type.
                            if (attributes == null || namespace == null) break;     // Report illegal format.
                            final int s = element.indexOf(RENAME_SEPARATOR);
                            if (s >= 0) {
                                final String old = element.substring(0, s).trim().intern();
                                element = element.substring(s+1).trim().intern();
                                attributes.put(old, element);
                            } else {
                                element = element.equals(TYPE_KEY) ? type : element.intern();
                            }
                            attributes.put(element, namespace);
                            continue;
                        }
                    }
                    throw new InvalidPropertiesFormatException(Errors.format(       // See FILE javadoc.
                            Errors.Keys.ErrorInFileAtLine_2, filename, in.getLineNumber()));
                }
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        /*
         * At this point we finished computing the map values. Many values are maps with only 1 entry.
         * Save a little bit of space by replacing maps of 1 element by Collections.singletonMap(…).
         */
        m.replaceAll((k, v) -> CollectionsExt.compact(v));
        return m;
    }

    /**
     * Returns a snapshot of {@link #renamedAttributes} list and clears the later.
     */
    final List<Attribute> attributes() {
        final List<Attribute> attributes;
        switch (renamedAttributes.size()) {
            case 0:  attributes = Collections.emptyList(); break;      // Avoid object creation for this common case.
            case 1:  attributes = Collections.singletonList(renamedAttributes.remove(0)); break;
            default: attributes = Arrays.asList(renamedAttributes.toArray(new Attribute[renamedAttributes.size()]));
                     renamedAttributes.clear();
                     break;
        }
        return attributes;
    }

    /**
     * Returns {@code true} if an element with the given name is an OGC/ISO type (as opposed to property).
     * For example given the following XML, this method returns {@code true} for {@code cit:CI_Date} but
     * {@code false} for {@code cit:date}:
     *
     * {@preformat xml
     *   <cit:CI_Citation>
     *     <cit:date>
     *       <cit:CI_Date>
     *         …
     *       </cit:CI_Date>
     *     </cit:date>
     *   </cit:CI_Citation>
     * }
     *
     * This method is based on simple heuristic applicable to OGC/ISO conventions, and may change in any future SIS
     * version depending on new formats to support.
     */
    private static boolean isTypeElement(final String localPart) {
        if (localPart.length() < 4) return false;
        final char c = localPart.charAt(0);
        return (c >= 'A' && c <= 'Z');
    }

    /**
     * Notifies that we are opening an element of the given name.
     *
     * @param  name        element name as declared in JAXB annotations.
     * @param  namespaces  namespaces map loaded by {@link #load(String)}.
     */
    final void open(final QName name, final Map<String, Map<String,String>> namespaces) {
        final String localPart = name.getLocalPart();
        if (isTypeElement(localPart)) {
            outerElements.add(name);
            outerElementProperties = namespaces.getOrDefault(localPart, Collections.emptyMap());
        }
    }

    /**
     * Notifies that we are closing an element of the given name. This method closes the last start element
     * with a matching name. It should be the last element on the list in a well-formed XML, but we loop in
     * the list anyway as a safety.
     *
     * @param  name        element name as declared in JAXB annotations.
     * @param  namespaces  namespaces map loaded by {@link #load(String)}.
     */
    final void close(final QName name, final Map<String, Map<String,String>> namespaces) {
        if (isTypeElement(name.getLocalPart())) {
            outerElementProperties = null;
            for (int i=outerElements.size(); --i >= 0;) {
                if (name.equals(outerElements.get(i))) {
                    outerElements.remove(i);
                    final String parent = (--i >= 0) ? outerElements.get(i).getLocalPart() : null;
                    outerElementProperties = namespaces.getOrDefault(parent, Collections.emptyMap());
                    break;
                }
            }
        }
    }

    /**
     * Frees any resources associated with this reader.
     */
    public void close() throws XMLStreamException {
        outerElementProperties = null;
        outerElements.clear();
    }

    /**
     * Renames en element using the namespaces map give to the {@code open(…)} and {@code close(…)} methods.
     * When unmarshalling, this imports a name read from the XML document to the name to give to JAXB.
     * When marshalling, this exports a name used in JAXB annotation to the name to use in XML document.
     * The new namespace depends on both the old namespace and the element name.
     * The prefix is computed by {@link #prefixReplacement(String, String)}.
     *
     * @param   name   the name of the element or attribute currently being read or written.
     * @return  a name with potentially the namespace and the local part replaced.
     */
    final QName convert(QName name) throws XMLStreamException {
        String localPart = name.getLocalPart();
        String namespace = outerElementProperties.get(localPart);
        if (namespace != null && !isNamespace(namespace)) {
            localPart = namespace;
            namespace = outerElementProperties.get(localPart);
        }
        /*
         * If above code found no namespace by looking in our dictionary of special cases,
         * maybe there is a simple one-to-one relationship between the old and new namespaces.
         * Otherwise we leave the namespace unchanged.
         */
        final String oldNS = name.getNamespaceURI();
        if (namespace == null) {
            namespace = version.importNS(oldNS);
        }
        if (!namespace.equals(oldNS) || !localPart.equals(name.getLocalPart())) {
            name = new QName(namespace, localPart, prefixReplacement(name.getPrefix(), namespace));
        }
        return name;
    }

    /**
     * Returns the prefix to use for a name in a new namespace.
     *
     * @param  previous   the prefix associated to old namespace.
     * @param  namespace  the new namespace URI.
     * @return prefix to use for the new namespace.
     * @throws XMLStreamException if an error occurred while fetching the prefix.
     */
    abstract String prefixReplacement(String previous, String namespace) throws XMLStreamException;
}
