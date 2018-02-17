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
import java.util.Iterator;
import java.util.Collections;
import java.util.InvalidPropertiesFormatException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CollectionsExt;

import static javax.xml.stream.XMLStreamConstants.*;


/**
 * A XML reader replacing the namespaces found in XML documents by the namespaces expected by SIS at unmarshalling time.
 * This class forwards every method calls to the wrapped {@link XMLEventReader}, but with some {@code namespaceURI}
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
final class TransformingReader extends Transformer implements XMLEventReader {
    /**
     * Location of the file listing types and properties contained in namespaces.
     * The file location is relative to this {@code TransformingReader} class.
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
     */
    static final String FILENAME = "NamespaceContent.lst";

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
     *   <li>Keys are XML names of types, ignoring {@code "_TYPE"} suffix (e.g. {@code "CI_Citation"})</li>
     *   <li>Values are maps where:<ul>
     *     <li>Keys are XML names of properties (e.g. {@code "title"}) or {@value #TYPE_KEY}</li>
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
                TransformingReader.class.getResourceAsStream(FILENAME), "UTF-8")))
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
                        case 1: {                                                   // New type in above namespace URI.
                            attributes = m.computeIfAbsent(element.intern(), (k) -> new HashMap<>());
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
     * Returns the namespace for the given ISO type, or {@code null} if unknown.
     * This is the namespace used in JAXB annotations.
     *
     * @param  type  a class name defined by ISO 19115 or related standards (e.g. {@code "CI_Citation"}.
     * @return a namespace for the given type, or {@code null} if unknown.
     */
    static String namespace(final String type) {
        final Map<String,String> attributes = NAMESPACES.get(type);
        return (attributes != null) ? attributes.get(TYPE_KEY) : null;
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
     * The reader from which to read events.
     */
    private final XMLEventReader in;

    /**
     * List of encountered XML tags, in order. Used for backtracking. Elements are removed from this list
     * when they are closed. Names should be the ones we get after conversion from namespaces used in XML
     * document to namespaces used in JAXB annotations.
     */
    private final List<QName> outerElements;

    /**
     * The prefixes for namespace URIs. Keys are URIs used in JAXB annotations and values are prefixes
     * computed by {@link Namespaces#getPreferredPrefix(String, String)} or any other means. We store
     * the prefix both for performance reasons and for improving the guarantees that the URI → prefix
     * mapping is stable.
     */
    private final Map<String,String> prefixes;

    /**
     * The next event to return after a call to {@link #peek()}. This is used for avoiding to recompute
     * the same object many times when {@link #peek()} is invoked before a call to {@link #nextEvent()}.
     * This is also required for avoiding to duplicate additions and removals of elements in the
     * {@link #outerElements} list.
     */
    private XMLEvent nextEvent;

    /**
     * Creates a new reader for the given version of the standards.
     */
    TransformingReader(final XMLEventReader in, final TransformVersion version) {
        super(version);
        this.in = in;
        outerElements = new ArrayList<>();
        prefixes = new HashMap<>();
    }

    /**
     * Returns {@code true} if the given {@code wrapper} is a wrapper for the given {@code event}.
     * This method is used for assertions only.
     */
    private static boolean isWrapper(final XMLEvent event, final XMLEvent wrapper) {
        return (event == wrapper) || (wrapper instanceof TransformedEvent && ((TransformedEvent) wrapper).event == event);
    }

    /**
     * Checks if there are more events.
     */
    @Override
    public boolean hasNext() {
        return (nextEvent != null) || in.hasNext();
    }

    /**
     * Checks the next {@code XMLEvent} without removing it from the stream.
     */
    @Override
    public XMLEvent peek() throws XMLStreamException {
        if (nextEvent == null) {
            final XMLEvent event = in.peek();
            if (event != null) {
                nextEvent = convert(event);
            }
        }
        return nextEvent;
    }

    /**
     * Returns the next element. Use {@link #nextEvent()} instead.
     */
    @Override
    public Object next() {
        final XMLEvent event = (XMLEvent) in.next();
        final XMLEvent next  = nextEvent;
        if (next != null) {
            nextEvent = null;
            assert isWrapper(event, next) : event;
            return next;
        }
        return convert(event);
    }

    /**
     * Forwards the call and keep trace of the XML elements opened up to this point.
     */
    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        final XMLEvent event = in.nextEvent();
        final XMLEvent next  = nextEvent;
        if (next != null) {
            nextEvent = null;
            assert isWrapper(event, next) : event;
            return next;
        }
        return convert(event);
    }

    /**
     * Forwards the call and keep trace of the XML elements opened up to this point.
     */
    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        final XMLEvent event = in.nextTag();
        final XMLEvent next  = nextEvent;
        if (next != null) {
            nextEvent = null;
            switch (event.getEventType()) {
                case START_ELEMENT:
                case END_ELEMENT: {
                    assert isWrapper(event, next) : event;
                    return event;
                }
            }
        }
        return convert(event);
    }

    /**
     * Keeps trace of XML elements opened up to this point and imports the given event.
     * This method replaces the namespaces used in XML document by the namespace used by JAXB annotations.
     * It is caller's responsibility to ensure that this method is invoked exactly once for each element,
     * or at least for each {@code START_ELEMENT} and {@code END_ELEMENT}.
     *
     * @param  event  the event read from the underlying event reader.
     * @return the converted event (may be the same instance).
     */
    @SuppressWarnings("unchecked")      // TODO: remove on JDK9
    private XMLEvent convert(XMLEvent event) {
        switch (event.getEventType()) {
            case ATTRIBUTE: {
                event = convert((Attribute) event);
                break;
            }
            case NAMESPACE: {
                event = importNS((Namespace) event, null, null);
                break;
            }
            case START_ELEMENT: {
                final StartElement e = event.asStartElement();
                final QName originalName = e.getName();
                final QName name = convert(originalName);
                boolean changed = name != originalName;
                for (final Iterator<Attribute> it = e.getAttributes(); it.hasNext();) {
                    final Attribute a = it.next();
                    final Attribute ae = convert(a);
                    changed |= (a != ae);
                    renamedAttributes.add(ae);
                }
                final List<Namespace> namespaces = importNS(e.getNamespaces(),
                        originalName.getNamespaceURI(), name.getNamespaceURI(), changed);
                if (namespaces != null) {
                    event = new TransformedEvent.Start(e, name, namespaces, attributes(), version);
                } else {
                    renamedAttributes.clear();
                }
                outerElements.add(name);
                break;
            }
            case END_ELEMENT: {
                final EndElement e = event.asEndElement();
                final QName originalName = e.getName();
                final QName name = convert(originalName);
                final List<Namespace> namespaces = importNS(e.getNamespaces(),
                        originalName.getNamespaceURI(), name.getNamespaceURI(), name != originalName);
                if (namespaces != null) {
                    event = new TransformedEvent.End(e, name, namespaces);
                }
                /*
                 * Close the last start element with a matching name. It should be the last element
                 * on the list in a well-formed XML, but we loop in the list anyway as a safety.
                 */
                for (int i = outerElements.size(); --i >= 0;) {
                    if (name.equals(outerElements.get(i))) {
                        outerElements.remove(i);
                        break;
                    }
                }
                break;
            }
        }
        return event;
    }

    /**
     * Imports a name read from the XML document to the name to give to JAXB.
     * The new namespace depends on both the old namespace and the element name.
     * The prefix is left unchanged since it can be arbitrary (even if confusing for
     * human reader used to ISO/TC211 prefixes, it is non-ambiguous to the computer).
     *
     * @param   name   the name of the element or attribute currently being read.
     * @return  the namespace URI for the element or attribute in the current context (e.g. an ISO 19115-3 namespace),
     *          or {@code null} if the given name is unknown.
     */
    private QName convert(final QName name) {
        String namespace = null;                        // In this method, null means no change to given name.
        String localPart = name.getLocalPart();
        final Set<String> declaringTypes = DECLARING_TYPES.get(localPart);
        if (declaringTypes == null) {
            /*
             * If the element is a root element, return the associated namespace.
             * We do not need to check if the value associated to TYPE_KEY is for
             * a renaming since it is never the case for that key.
             */
            final Map<String,String> attributes = NAMESPACES.get(localPart);
            if (attributes != null) {
                namespace = attributes.get(TYPE_KEY);           // May be null.
            }
        } else {
            /*
             * If the element is not a root element, we need to backtrack until we find the latest
             * possible parent element. Then, we use the namespace associated with that parent.
             */
            for (int i = outerElements.size(); --i >= 0;) {
                final String parent = outerElements.get(i).getLocalPart();
                if (declaringTypes.contains(parent)) {
                    /*
                     * A NullPointerException below would be a bug in our algorithm because
                     * we constructed DECLARING_TYPES from NAMESPACES keys only.
                     */
                    final Map<String,String> attributes = NAMESPACES.get(parent);
                    namespace = attributes.get(localPart);
                    if (!isNamespace(namespace)) {
                        localPart = namespace;
                        namespace = attributes.get(namespace);
                    }
                    break;
                }
            }
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
        if (namespace.equals(oldNS) && localPart.equals(name.getLocalPart())) {
            return name;
        }
        /*
         * Build a new name if any component (URI or local part) changed. The prefix should have
         * been specified (indirectly) by a previous call to 'importNS(Namespace)', for example
         * as a result of a NAMESPACE event. If not, we compute it now using the same method.
         */
        final String prefix = prefixes.computeIfAbsent(namespace,
                (ns) -> Namespaces.getPreferredPrefix(ns, name.getPrefix()));
        return new QName(namespace, localPart, prefix);
    }

    /**
     * Imports an attribute read from the XML document.
     * If there is no name change, then this method returns the given instance as-is.
     */
    private Attribute convert(Attribute attribute) {
        final QName originalName = attribute.getName();
        final QName name = convert(originalName);
        if (name != originalName) {
            attribute = new TransformedEvent.Attr(attribute, name);
        }
        return attribute;
    }

    /**
     * Converts a namespace read from the XML document to the namespace used by JAXB annotations.
     * This methods can convert the namespace for which there is a bijective mapping, for example
     * {@code "http://www.isotc211.org/2005/gco"} to {@code "http://standards.iso.org/iso/19115/-3/gco/1.0"}.
     * However some namespaces like {@code "http://www.isotc211.org/2005/gmd"} may be left unchanged,
     * because that namespace from legacy ISO 19139:2007 can be mapped to many different namespaces
     * in newer ISO 19115-3:2016 standard. However in some cases the context allows us to determines
     * which newer namespace is used. In such case, that mapping is specified by the
     * ({@code oldURI}, {@code newURI}) pair.
     *
     * @param  namespace  the namespace to import.
     * @param  oldURI     an old URI which has been renamed as {@code newURI}, or {@code null} if none.
     * @param  newURI     the new URI for {@code oldURI}, or {@code null} if {@code newURI} is null.
     */
    private Namespace importNS(final Namespace namespace, final String oldURI, final String newURI) {
        String uri = namespace.getNamespaceURI();
        if (uri != null && !uri.isEmpty()) {
            uri = removeTrailingSlash(uri);
            final String imported = uri.equals(oldURI) ? newURI : version.importNS(uri);
            if (imported != uri) {
                final String prefix = prefixes.computeIfAbsent(imported,
                        (ns) -> Namespaces.getPreferredPrefix(ns, namespace.getPrefix()));
                return new TransformedEvent.NS(namespace, prefix, imported);
            }
        }
        return namespace;
    }

    /**
     * Imports the namespaces read from the XML document.
     *
     * @param  namespaces  the namespaces to transform.
     * @param  oldURI      an old URI which has been renamed as {@code newURI}, or {@code null} if none.
     * @param  newURI      the new URI for {@code oldURI}, or {@code null} if {@code newURI} is null.
     * @param  changed     whether to unconditionally pretend that there is a change.
     * @return the updated namespaces, or {@code null} if there is no changes.
     */
    private List<Namespace> importNS(final Iterator<Namespace> namespaces,
            final String oldURI, final String newURI, boolean changed)
    {
        if (!namespaces.hasNext()) {
            return changed ? Collections.emptyList() : null;
        }
        final List<Namespace> modified = new ArrayList<>();
        do {
            Namespace namespace = namespaces.next();
            changed |= (namespace != (namespace = importNS(namespace, oldURI, newURI)));
            modified.add(namespace);
        } while (namespaces.hasNext());
        return changed ? modified : null;
    }

    /**
     * Reads the content of a text-only element. Forwards from the underlying reader as-is.
     *
     * @todo Untested. In particular, it is not clear how to update {@link #outerElements}.
     *       By change, JAXB does not seem to invoke this method.
     */
    @Override
    public String getElementText() throws XMLStreamException {
        return in.getElementText();
    }

    /**
     * Get the value of a feature/property from the underlying implementation.
     */
    @Override
    public Object getProperty​(final String name) {
        return in.getProperty(name);
    }

    /**
     * Frees any resources associated with this reader.
     * This method does not close the underlying input source.
     */
    @Override
    public void close() throws XMLStreamException {
        outerElements.clear();
        in.close();
    }
}
