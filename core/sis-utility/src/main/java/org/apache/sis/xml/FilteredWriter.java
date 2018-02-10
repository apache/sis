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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Namespace;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import static javax.xml.stream.XMLStreamConstants.*;


/**
 * A filter replacing the namespaces used by JAXB by other namespaces to be used in the XML document
 * at marshalling time. This class forwards every method calls to the wrapped {@link XMLEventWriter},
 * with all {@code namespaceURI} arguments filtered before to be delegated.
 *
 * See {@link FilteredNamespaces} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class FilteredWriter implements XMLEventWriter {
    /**
     * Where events are sent.
     */
    private final XMLEventWriter out;

    /**
     * The other version to marshal to.
     */
    private final FilterVersion version;

    /**
     * Keep track of namespace URIs that have already been declared so they don't get duplicated.
     * This map is recycled in two different contexts:
     *
     * <ul>
     *   <li>In a sequence of {@code NAMESPACE} events.</li>
     *   <li>In the namespaces of a start element.</li>
     * </ul>
     */
    private final Map<String, Namespace> uniqueNamespaces;

    /**
     * Temporary list of attributes after their namespace change.
     * This list is recycled for each XML element to be read.
     */
    private final List<Attribute> exportedAttributes;

    /**
     * Creates a new filter for the given version of the standards.
     */
    FilteredWriter(final XMLEventWriter out, final FilterVersion version) {
        this.out = out;
        this.version = version;
        uniqueNamespaces = new LinkedHashMap<>();
        exportedAttributes = new ArrayList<>();
    }

    /**
     * Returns the URI to write in the XML document.
     * If there is no namespace change, then this method returns the given instance as-is.
     */
    private String export(final String uri) {
        return version.exports.getOrDefault(uri, uri);
    }

    /**
     * Returns the name (prefix, namespace and local part) to write in the XML document.
     * If there is no name change, then this method returns the given instance as-is.
     */
    private QName export(QName name) {
        String uri = name.getNamespaceURI();
        if (uri != null && !uri.isEmpty()) {                                // Optimization for a common case.
            uri = version.exports.get(uri);
            if (uri != null) {
                name = new QName(uri, name.getLocalPart(), Namespaces.getPreferredPrefix(uri, name.getPrefix()));
            }
        }
        return name;
    }

    /**
     * Returns the attribute to write in the XML document.
     * If there is no name change, then this method returns the given instance as-is.
     */
    private Attribute export(Attribute attribute) {
        final QName originalName = attribute.getName();
        final QName name = export(originalName);
        if (name != originalName) {
            attribute = new FilteredEvent.Attr(attribute, name);
        }
        return attribute;
    }

    /**
     * Returns the namespace to write in the XML document. This may imply a prefix change.
     * If there is no namespace change, then this method returns the given instance as-is.
     * To test if the returned namespace is a new one, callers should check if the size of
     * {@link #uniqueNamespaces} changed.
     *
     * @param  namespace  the namespace to export.
     */
    private Namespace exportIfNew(final Namespace namespace) {
        String uri = namespace.getNamespaceURI();
        if (uri != null && !uri.isEmpty()) {
            final int end = uri.length() - 1;
            if (uri.charAt(end) == '/') {
                uri = uri.substring(0, end);                            // Trim trailing '/' in URI.
            }
            final String exported = version.exports.get(uri);
            if (exported != null) {
                return uniqueNamespaces.computeIfAbsent(exported, (k) -> {
                    return new FilteredEvent.NS(namespace, Namespaces.getPreferredPrefix(k, namespace.getPrefix()), k);
                });
            }
            final Namespace c = uniqueNamespaces.put(uri, namespace);   // No namespace change needed. Overwrite wrapper if any.
            if (c != null) return c;
        }
        return namespace;
    }

    /**
     * Returns the namespaces to write in the XML document, or {@code null} if there is no change.
     * If non-null, the result may contain less namespaces because duplicated entries are omitted
     * (duplication may occur as a result of replacing various ISO 19115-3 namespaces by the legacy
     * ISO 19139:2007 {@code "gmd"} unique namespace).
     *
     * @param  namespaces  the namespaces to filter.
     * @param  changed     whether to unconditionally pretend that there is a change.
     * @return the updated namespaces, or {@code null} if there is no changes.
     */
    private List<Namespace> export(final Iterator<Namespace> namespaces, boolean changed) {
        if (!namespaces.hasNext()) {
            return changed ? Collections.emptyList() : null;
        }
        do {
            final Namespace namespace = namespaces.next();
            changed |= (namespace != exportIfNew(namespace));
        } while (namespaces.hasNext());
        if (changed) {
            assert !uniqueNamespaces.isEmpty();
            final Namespace[] exported = uniqueNamespaces.values().toArray(new Namespace[uniqueNamespaces.size()]);
            uniqueNamespaces.clear();
            return Arrays.asList(exported);
        } else {
            uniqueNamespaces.clear();
            return null;
        }
    }

    /**
     * Adds an event to the output stream. This method may wrap the given event into another event
     * for changing the namespace and/or the prefix.
     */
    @Override
    @SuppressWarnings("unchecked")      // TODO: remove on JDK9
    public void add(XMLEvent event) throws XMLStreamException {
        switch (event.getEventType()) {
            case ATTRIBUTE: {
                event = export((Attribute) event);
                break;
            }
            case NAMESPACE: {
                final int n = uniqueNamespaces.size();
                event = exportIfNew((Namespace) event);
                if (uniqueNamespaces.size() == n) {
                    return;                                     // An event has already been created for that namespace.
                }
                break;
            }
            case START_ELEMENT: {
                uniqueNamespaces.clear();                       // Discard entries created by NAMESPACE events.
                final StartElement e = event.asStartElement();
                final QName originalName = e.getName();
                final QName name = export(originalName);
                boolean changed = name != originalName;
                for (final Iterator<Attribute> it = e.getAttributes(); it.hasNext();) {
                    final Attribute a = it.next();
                    final Attribute ae = export(a);
                    changed |= (a != ae);
                    exportedAttributes.add(ae);
                }
                final List<Namespace> namespaces = export(e.getNamespaces(), changed);
                if (namespaces != null) {
                    final List<Attribute> attributes;
                    switch (exportedAttributes.size()) {
                        case 0:  attributes = Collections.emptyList(); break;      // Avoid object creation for this common case.
                        case 1:  attributes = Collections.singletonList(exportedAttributes.remove(0)); break;
                        default: attributes = Arrays.asList(exportedAttributes.toArray(new Attribute[exportedAttributes.size()]));
                                 exportedAttributes.clear();
                                 break;
                    }
                    event = new FilteredEvent.Start(e, name, namespaces, attributes, version);
                } else {
                    exportedAttributes.clear();
                }
                break;
            }
            case END_ELEMENT: {
                final EndElement e = event.asEndElement();
                final QName originalName = e.getName();
                final QName name = export(originalName);
                final List<Namespace> namespaces = export(e.getNamespaces(), name != originalName);
                if (namespaces != null) {
                    event = new FilteredEvent.End(e, name, namespaces);
                }
                break;
            }
        }
        out.add(event);
    }

    /**
     * Adds an entire stream to an output stream.
     */
    @Override
    public void add(final XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            add(reader.nextEvent());
        }
    }

    /**
     * Gets the prefix the URI is bound to.
     * This method replaces the given URI if needed, then forwards the call.
     */
    @Override
    public String getPrefix(final String uri) throws XMLStreamException {
        return out.getPrefix(export(uri));
    }

    /**
     * Sets the prefix the URI is bound to.
     * This method replaces the given URI if needed, then forwards the call.
     */
    @Override
    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        out.setPrefix(prefix, export(uri));
    }

    /**
     * Binds a URI to the default namespace.
     * Replaces the given URI if needed, then forwards the call.
     */
    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        out.setDefaultNamespace(export(uri));
    }

    /**
     * Sets the current namespace context for prefix and URI bindings.
     * Unwraps the original context and forwards the call.
     */
    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        if (context instanceof FilteredNamespaces) {
            context = ((FilteredNamespaces) context).inverse(version);
        } else {
            context = new FilteredNamespaces(context, version, true);
        }
        out.setNamespaceContext(context);
    }

    /**
     * Returns the context of the underlying writer wrapped in a filter that convert the namespaces on the fly.
     */
    @Override
    public NamespaceContext getNamespaceContext() {
        return new FilteredNamespaces(out.getNamespaceContext(), version, false);
    }

    /**
     * Writes any cached events to the underlying output mechanism.
     */
    @Override
    public void flush() throws XMLStreamException {
        out.flush();
    }

    /**
     * Frees any resources associated with this writer.
     */
    @Override
    public void close() throws XMLStreamException {
        uniqueNamespaces.clear();
        out.close();
    }
}
