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
import java.util.Collections;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.util.InvalidPropertiesFormatException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * A {@code StreamReader} which uses a more complex algorithm backed by a dictionary for identifying the XML namespaces
 * expected by JAXB implementation. This is used when a single namespace in a legacy schema has been splitted into many
 * namespaces in a newer schema. This happen for example in the upgrade from ISO 19139 to ISO 19115-3. In such cases,
 * we need to check which attribute is being mapped in order to determine the new namespace.
 *
 * @author  Cullen Rombach (Image Matters)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class FilteredStreamResolver extends FilteredStreamReader {
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
    private static final String TYPE_KEY = "<type>";

    /**
     * The mapping from (<var>type</var>, <var>attribute</var>) pairs to namespaces.
     *
     * <ul>
     *   <li>Keys are XML names of types (e.g. {@code "CI_Citation"})</li>
     *   <li>Values are maps where:<ul>
     *     <li>Keys are XML names of attributes (e.g. {@code "title"}) or {@value #TYPE_KEY}</li>
     *     <li>Values are namespace URI</li>
     *   </ul></li>
     * </ul>
     *
     * This map is initialized only once and should not be modified after that point.
     * The file format is described in {@link #FILENAME}.
     */
    private static final Map<String, Map<String,String>> NAMESPACES;
    static {
        final Map<String, Map<String,String>> m = new HashMap<>(250);
        try (final LineNumberReader in = new LineNumberReader(new InputStreamReader(
                FilteredStreamResolver.class.getResourceAsStream(FILENAME), "UTF-8")))
        {
            Map<String,String> attributes = null;   // All attributes for a given type.
            String namespace = null;                // Value to store in 'attributes' map.
            String line;
            while ((line = in.readLine()) != null) {
                final int length = line.length();
                final int start = CharSequences.skipLeadingWhitespaces(line, 0, length);
                if (start < length && line.charAt(start) != '#') {
                    final String element = line.substring(start).trim().intern();
                    switch (start) {
                        case 0: {                       // New namespace URI.
                            namespace  = element;
                            attributes = null;
                            continue;
                        }
                        case 2: {                       // New type in above namespace URI.
                            attributes = m.computeIfAbsent(element, (k) -> new HashMap<>());
                            continue;
                        }
                        case 4: {                       // New attribute in above type.
                            if (attributes != null && namespace != null) {
                                attributes.put(element, namespace);
                                continue;
                            }
                            // Fall through for reporting illegal format.
                        }
                    }
                    throw new InvalidPropertiesFormatException(Errors.format(      // See FILE javadoc.
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
     */
    private static final Map<String, Set<String>> DECLARING_TYPES;
    static {
        final Map<String, Set<String>> m = new HashMap<>(500);
        for (final Map.Entry<String, Map<String,String>> e : NAMESPACES.entrySet()) {
            final String type = e.getKey();
            for (final String attribute : e.getValue().keySet()) {
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
     * Elements that need to have their name changed.
     * For example {@code gmd:URL} needs to be changed to {@code gco:CharacterString} for reading ISO 19139.
     *
     * <ul>
     *   <li>key:   old element name</li>
     *   <li>value: new element name</li>
     * </ul>
     */
    private static final Map<String,String> NAME_CHANGES = Collections.singletonMap("URL", "CharacterString");

    /**
     * List of encountered XML tags, in order. Used for backtracking.
     * Elements are removed from this list when they are closed.
     */
    private final List<String> outerElements;

    /**
     * Creates a new filter for the given version of the standards.
     */
    FilteredStreamResolver(final XMLStreamReader in, final FilterVersion version) {
        super(in, version);
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
     * Return the namespace used by implementation (the SIS classes with JAXB annotations)
     * in the context of the current part of the XML document being read.
     *
     * @param   name       the element or attribute name currently being read.
     * @param   namespace  the namespace declared in the document being read (e.g. an ISO 19139 namespace).
     * @return  the namespace URI for the element or attribute in the current context (e.g. an ISO 19115-3 namespace).
     */
    private String toImplNamespace(final String name, final String namespace) {
        final Set<String> declaringTypes = DECLARING_TYPES.get(name);
        if (declaringTypes == null) {
            /*
             * If the element is a root element, return the associated namespace.
             */
            final Map<String,String> attributes = NAMESPACES.get(name);
            if (attributes != null) {
                return attributes.getOrDefault(TYPE_KEY, namespace);
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
                    return NAMESPACES.get(parent).getOrDefault(name, namespace);
                }
            }
        }
        return toImpl(namespace);
    }

    /**
     * If the given text seems to be a qualified name and its prefix is recognized, replaces its namespace URI.
     * Otherwise returns the text unchanged. This method is invoked for transforming attribute values.
     */
    private String toImplName(String text) {
        int length = CharSequences.skipTrailingWhitespaces(text, 0, text.length());
        int start  = CharSequences.skipLeadingWhitespaces (text, 0, length);
        for (int sep=start; sep < length; sep++) {
            if (text.charAt(sep) == ':') {
                if (sep != start) {
                    // Prefix exists, check everything
                    String prefix = text.substring(start, sep);
                    String uri = getParent().getNamespaceContext().getNamespaceURI(prefix);
                    if (uri != null && !uri.isEmpty()) {
                        String name = text.substring(sep+1, length);
                        uri = toImplNamespace(name, uri);
                        prefix = Namespaces.getPreferredPrefix(uri, prefix);
                        text = prefix + ':' + name;
                    }
                }
                break;
            }
        }
        return text;
    }

    /**
     * Converts a name read from the XML document to the name to give to JAXB.
     * The new namespace depends on both the old namespace and the element name.
     */
    @Override
    final QName toImpl(QName name) {
        final String namespaceURI = name.getNamespaceURI();
        final String oldLocal = name.getLocalPart();
        final String newLocal = NAME_CHANGES.getOrDefault(oldLocal, oldLocal);
        final String replacement = toImplNamespace(newLocal, namespaceURI);
        if (replacement != namespaceURI || newLocal != oldLocal) {               // Identity checks are okay here.
            name = new QName(replacement, oldLocal, name.getPrefix());
        }
        return name;
    }

    /**
     * Forwards the call, then replaces the local name if needed.
     */
    @Override
    public String getLocalName() {
        final String name = super.getLocalName();
        return NAME_CHANGES.getOrDefault(name, name);
    }

    /**
     * Returns the namespace of current element, after replacement by the URI used by SIS.
     * This replacement depends on the current local name in addition of current namespace.
     */
    @Override
    public String getNamespaceURI() {
        return toImplNamespace(getLocalName(), getParent().getNamespaceURI());
    }

    /**
     * Replaces the given URI if needed, then forwards the call.
     * If the attribute value seems to be a qualified name, replaces the namespace URI there too.
     */
    @Override
    public String getAttributeValue(final String namespaceUri, final String localName) {
        return toImplName(super.getAttributeValue(namespaceUri, localName));
    }

    /**
     * Forwards the call.
     * If the attribute value seems to be a qualified name, replaces the namespace URI there too.
     */
    @Override
    public String getAttributeValue(int index) {
        return toImplName(super.getAttributeValue(index));
    }
}