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

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.util.InvalidPropertiesFormatException;
import javax.xml.namespace.QName;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * A filter replacing the namespaces found in XML documents by the namespaces expected by SIS at unmarshalling time.
 * This class forwards every method calls to the wrapped {@link XMLStreamReader}, but with some {@code namespaceURI}
 * modified before being transfered. This class uses a dictionary for identifying the XML namespaces expected by JAXB
 * implementation. This is needed when a single namespace in a legacy schema has been splitted into many namespaces
 * in the newer schema. This happen for example in the upgrade from ISO 19139:2007 to ISO 19115-3.
 * In such cases, we need to check which attribute is being mapped in order to determine the new namespace.
 *
 * @author  Cullen Rombach (Image Matters)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class FilteredReader extends StreamReaderDelegate {
    /**
     * Location of the file listing types and attributes contained in namespaces.
     * The file location is relative to this {@code NamespaceContent} class.
     * The file format is a tree structured with indentation as below:
     *
     * <ul>
     *   <li>Lines with zero-spaces indentation are namespace URIs.</li>
     *   <li>Lines with two-spaces  indentation are classes within the last namespace URIs found so far.</li>
     *   <li>Lines with four-spaces indentation are attributes within the last class found so far.</li>
     *   <li>All other indentations are illegal and cause an {@link InvalidPropertiesFormatException} to be thrown.
     *       This exception type is not really appropriate since the file format is not a {@code .properties} file,
     *       but it is the closest we could find in existing exceptions and we don't want to define a new exception
     *       type since this error should never happen.</li>
     * </ul>
     */
    private static final String FILENAME = "NamespaceContent.txt";

    /**
     * A key in {@link #NAMESPACES} sub-map meaning that the value (a namespace URI) is for the type instead
     * than for an attribute. Shall be the same string than the one used in {@value #FILENAME} resource file.
     */
    static final String TYPE_KEY = "<type>";

    /**
     * Character used for separating a old name for the new name. For example in {@code SV_OperationMetadata},
     * {@code "DCP"} in ISO 19139:2007 has been renamed {@code "distributedComputingPlatform"} in ISO 19115-3.
     * This is encoded in {@value #FILENAME} file as {@code "DCP/distributedComputingPlatform"}.
     */
    private static final char RENAME_SEPARATOR = '/';

    /**
     * Returns {@code true} if the given string is a namespace URI, or {@code false} if it is a property name.
     * This method implements a very fast check based on the presence of {@code ':'} in {@code "http://foo.bar"}.
     * It assumes that all namespaces declared in the {@value #FILENAME} file use the {@code "http"} protocol and
     * no property name use the {@code ':'} character.
     */
    private static boolean isNamespace(final String candidate) {
        return (candidate.length() > 4) && (candidate.charAt(4) == ':');
    }

    /**
     * The mapping from (<var>type</var>, <var>attribute</var>) pairs to namespaces.
     *
     * <ul>
     *   <li>Keys are XML names of types (e.g. {@code "CI_Citation"})</li>
     *   <li>Values are maps where:<ul>
     *     <li>Keys are XML names of attributes (e.g. {@code "title"}) or {@value #TYPE_KEY}</li>
     *     <li>Values are either:<ul>
     *       <li>Namespace URI if {@link #isNamespace(String)} returns {@code true} for that value.</li>
     *       <li>New name of the element otherwise. In such case, the map must be queried again with
     *           that new name for obtaining the namespace.</li>
     *     </ul></li>
     *   </ul></li>
     * </ul>
     *
     * This map is initialized only once and should not be modified after that point.
     * The file format is described in {@link #FILENAME}.
     */
    private static final Map<String, Map<String,String>> NAMESPACES;
    static {
        final Map<String, Map<String,String>> m = new HashMap<>(250);
        try (LineNumberReader in = new LineNumberReader(new InputStreamReader(
                FilteredReader.class.getResourceAsStream(FILENAME), "UTF-8")))
        {
            Map<String,String> attributes = null;               // All attributes for a given type.
            String namespace = null;                            // Value to store in 'attributes' map.
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
                            continue;
                        }
                        case 2: {                                                   // New type in above namespace URI.
                            attributes = m.computeIfAbsent(element.intern(), (k) -> new HashMap<>());
                            continue;
                        }
                        case 4: {                                                   // New attribute in above type.
                            if (attributes == null || namespace == null) break;     // Report illegal format.
                            final int s = element.indexOf(RENAME_SEPARATOR);
                            if (s >= 0) {
                                final String old = element.substring(0, s).trim().intern();
                                element = element.substring(s+1).trim().intern();
                                attributes.put(old, element);
                            } else {
                                element = element.intern();
                            }
                            attributes.put(element, namespace);
                            continue;
                        }
                    }
                    throw new InvalidPropertiesFormatException(Errors.format(       // See FILE javadoc.
                            Errors.Keys.ErrorInFileAtLine_2, FILENAME, in.getLineNumber()));
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
        NAMESPACES = m;
    }

    /**
     * The mapping from attribute names to types where such attribute is declared.
     * An attribute of the same name may be declared in many types.
     *
     * <p>This method does not look at namespaces. Consequently the renaming encoding
     * (e.g. {@code "DCP/distributedComputingPlatform"}) is not of concern here.</p>
     */
    private static final Map<String, Set<String>> DECLARING_TYPES;
    static {
        final Map<String, Set<String>> m = new HashMap<>(500);
        for (final Map.Entry<String, Map<String,String>> forTypes : NAMESPACES.entrySet()) {
            final String type = forTypes.getKey();
            for (final String attribute : forTypes.getValue().keySet()) {
                m.computeIfAbsent(attribute, (k) -> new HashSet<>()).add(type);
            }
        }
        /*
         * At this point we finished computing the map values. Many values are the same sets.
         * For example the {CI_Citation} singleton set occurs often. Following code replaces
         * duplicated sets by shared instances in order to save a little bit of space.
         */
        final Map<Set<String>, Set<String>> unique = new HashMap<>(200);
        m.replaceAll((k, v) -> {
            v = CollectionsExt.compact(v);
            final Set<String> r = unique.putIfAbsent(v, v);
            return (r != null) ? r : v;
        });
        DECLARING_TYPES = m;
    }

    /**
     * The external XML format version to unmarshal from.
     */
    private final FilterVersion version;

    /**
     * List of encountered XML tags, in order. Used for backtracking.
     * Elements are removed from this list when they are closed.
     */
    private final List<String> outerElements;

    /**
     * The {@code localPart} argument given to {@link #namespaceOf(String)}, potentially renamed.
     * For example given the {@code "DCP/distributedComputingPlatform"} entry in {@value #FILENAME} file,
     * a call to {@code namespaceOf("DCP")} will set this field to {@code "distributedComputingPlatform"}.
     */
    private String renamed;

    /**
     * Creates a new filter for the given version of the standards.
     */
    FilteredReader(final XMLStreamReader in, final FilterVersion version) {
        super(in);
        this.version = version;
        outerElements = new ArrayList<>();
    }

    /**
     * Forwards the call and keep trace of the XML element opened up to this point.
     */
    @Override
    public int next() throws XMLStreamException {
        return traceElements(super.next());
    }

    /**
     * Forwards the call and keep trace of the XML element opened up to this point.
     */
    @Override
    public int nextTag() throws XMLStreamException {
        return traceElements(super.nextTag());
    }

    /**
     * Keeps trace of XML elements opened up to this point.
     *
     * @param  type  value of {@link #getEventType()}.
     * @return {@code type}, returned for convenience.
     */
    private int traceElements(final int type) {
        switch (type) {
            case START_ELEMENT: {
                outerElements.add(getLocalName());
                break;
            }
            case END_ELEMENT: {
                /*
                 * If this is an end element, close the last open one with a matching name.
                 * It should be the last list element in a well-formed XML, but we loop in
                 * the list anyway as a safety.
                 */
                final String name = getLocalName();
                for (int i = outerElements.size(); --i >= 0;) {
                    if (name.equals(outerElements.get(i))) {
                        outerElements.remove(i);
                        break;
                    }
                }
                break;
            }
        }
        return type;
    }

    /**
     * Returns the namespace of the given ISO type, or {@code null} if unknown.
     * This is the namespace used in JAXB annotations.
     *
     * @param  type  a class name defined by ISO 19115 or related standards (e.g. {@code "CI_Citation"}.
     * @return a namespace for the given type, or {@code null} if unknown.
     */
    static String namespace(final String type) {
        /*
         * Same implementation than namespaceOf(type) but without DECLARING_TYPES.get(…)
         * since that value should alway be null for class names.
         */
        final Map<String,String> attributes = NAMESPACES.get(type);
        return (attributes != null) ? attributes.get(TYPE_KEY) : null;
    }

    /**
     * Return the namespace used by implementation (the SIS classes with JAXB annotations)
     * in the context of the current part of the XML document being read.
     *
     * @param   localPart   the local name of the element or attribute currently being read.
     * @return  the namespace URI for the element or attribute in the current context (e.g. an ISO 19115-3 namespace),
     *          or {@code null} if the given name is unknown.
     */
    private String namespaceOf(final String localPart) {
        renamed = localPart;
        final Set<String> declaringTypes = DECLARING_TYPES.get(localPart);
        if (declaringTypes == null) {
            /*
             * If the element is a root element, return the associated namespace.
             * We do not need to check if the value associated to TYPE_KEY is for
             * a renaming since it is never the case for that key.
             */
            final Map<String,String> attributes = NAMESPACES.get(localPart);
            if (attributes != null) {
                return attributes.get(TYPE_KEY);
            }
        } else {
            /*
             * If the element is not a root element, we need to backtrack until we find the latest
             * possible parent element. Then, we use the namespace associated with that parent.
             */
            for (int i = outerElements.size(); --i >= 0;) {
                final String parent = outerElements.get(i);
                if (declaringTypes.contains(parent)) {
                    /*
                     * A NullPointerException below would be a bug in our algorithm because
                     * we constructed DECLARING_TYPES from NAMESPACES keys only.
                     */
                    final Map<String,String> attributes = NAMESPACES.get(parent);
                    String uri = attributes.get(localPart);
                    if (!isNamespace(uri)) {
                        renamed = uri;
                        uri = attributes.get(uri);
                    }
                    return uri;
                }
            }
        }
        return null;
    }

    /**
     * Converts a name read from the XML document to the name to give to JAXB.
     * The new namespace depends on both the old namespace and the element name.
     */
    private QName importNS(QName name) {
        final String namespaceURI = name.getNamespaceURI();
        final String localPart = name.getLocalPart();
        String replacement = namespaceOf(localPart);
        if (replacement == null) {
            replacement = version.importNS(namespaceURI);
        }
        if (!replacement.equals(namespaceURI) || !localPart.equals(renamed)) {
            name = new QName(replacement, renamed, name.getPrefix());
        }
        return name;
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public void require(final int type, final String namespaceURI, final String localName) throws XMLStreamException {
        super.require(type, version.exportNS(namespaceURI), localName);
    }

    /** Returns the context of the underlying reader wrapped in a filter that converts the namespaces on the fly. */
    @Override
    public NamespaceContext getNamespaceContext() {
        return FilteredNamespaces.importNS(super.getNamespaceContext(), version);
    }

    /** Forwards the call, then replaces the namespace URI if needed. */
    @Override
    public QName getName() {
        return importNS(super.getName());
    }

    /** Forwards the call, then replaces the namespace URI if needed. */
    @Override
    public QName getAttributeName(final int index) {
        return importNS(super.getAttributeName(index));
    }

    /**
     * Returns the namespace of current element, after replacement by the URI used by SIS.
     * This replacement depends on the current local name in addition of current namespace.
     */
    @Override
    public String getNamespaceURI() {
        String namespace = namespaceOf(getLocalName());
        if (namespace == null) {
            namespace = version.importNS(super.getNamespaceURI());
        }
        return namespace;
    }

    /**
     * Forwards the call, then replaces the returned URI if needed.
     *
     * <b>Note:</b> the index passed to this method is the index of a namespace declaration on the root element.
     * This should not matter as long as each <em>element</em> has the proper namespace URI.
     */
    @Override
    public String getNamespaceURI(int index) {
        return version.importNS(super.getNamespaceURI(index));
    }

    /** Forwards the call, then replaces the returned URI if needed. */
    @Override
    public String getNamespaceURI(final String prefix) {
        return version.importNS(super.getNamespaceURI(prefix));
    }

    /** Forwards the call, then replaces the returned URI if needed. */
    @Override
    public String getAttributeNamespace(final int index) {
        return version.importNS(super.getAttributeNamespace(index));
    }

    /** Replaces the given URI if needed, then forwards the call. */
    @Override
    public String getAttributeValue(final String namespaceUri, final String localName) {
        return super.getAttributeValue(version.exportNS(namespaceUri), localName);
    }
}
