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
import org.apache.sis.util.resources.Errors;

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
final class FilteredWriter extends FilteredXML implements XMLEventWriter {
    /**
     * Where events are sent.
     */
    private final XMLEventWriter out;

    /**
     * Keep track of namespace URIs that have already been declared so they don't get duplicated.
     * This map is recycled in two different contexts:
     *
     * <ul>
     *   <li>In a sequence of {@code NAMESPACE} events.</li>
     *   <li>In the namespaces of a start element.</li>
     * </ul>
     */
    private final Map<String,Namespace> uniqueNamespaces;

    /**
     * Creates a new filter for the given version of the standards.
     */
    FilteredWriter(final XMLEventWriter out, final FilterVersion version) {
        super(version);
        this.out = out;
        uniqueNamespaces = new LinkedHashMap<>();
    }

    /**
     * Returns the name (prefix, namespace and local part) to write in the XML document.
     * This method may replace the namespace, and in some case the name local part too.
     * If there is no name change, then this method returns the given instance as-is.
     */
    private QName export(QName name) throws XMLStreamException {
        String uri = name.getNamespaceURI();
        if (uri != null && !uri.isEmpty()) {                                // Optimization for a common case.
            final FilterVersion.Replacement r = version.export(uri);
            if (r != null) {
                uri = r.namespace;
                /*
                 * The wrapped XMLEventWriter maintains a mapping from prefixes to namespace URIs.
                 * Arguments are exported URIs (e.g. from legacy ISO 19139:2007) and return values
                 * are prefixes computed by 'Namespaces.getPreferredPrefix(…)' or any other means.
                 * We fetch those prefixes for performance reasons and for improving the guarantees
                 * that the URI → prefix mapping is stable, since JAXB seems to require them for
                 * writing namespaces in XML.
                 */
                String prefix = out.getPrefix(uri);
                if (prefix == null) {
                    prefix = Namespaces.getPreferredPrefix(uri, name.getPrefix());
                    out.setPrefix(prefix, uri);
                    /*
                     * The above call for 'getPreferredPrefix' above is required: JAXB seems to need the prefixes
                     * for recognizing namespaces. The prefix shall be computed in the same way than 'exportIfNew'.
                     * We enter in this block only for the root element, before to parse 'xmlns' attributes. For
                     * all other elements after the root elements, above call to 'out.getPrefix(uri)' should succeed.
                     */
                }
                name = new QName(uri, r.exportProperty(name.getLocalPart()), prefix);
            }
        }
        return name;
    }

    /**
     * Returns the attribute to write in the XML document.
     * If there is no name change, then this method returns the given instance as-is.
     */
    private Attribute export(Attribute attribute) throws XMLStreamException {
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
            final String exported = version.exportNS(removeTrailingSlash(uri));
            if (exported != uri) {
                return uniqueNamespaces.computeIfAbsent(exported, (k) -> {
                    return new FilteredEvent.NS(namespace, Namespaces.getPreferredPrefix(k, namespace.getPrefix()), k);
                    /*
                     * The new prefix selected by above line will be saved by out.add(Namespace)
                     * after this method has been invoked by the NAMESPACE case of add(XMLEvent).
                     */
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
                    renamedAttributes.add(ae);
                }
                final List<Namespace> namespaces = export(e.getNamespaces(), changed);
                if (namespaces != null) {
                    event = new Event(e, name, namespaces, attributes(), version);
                } else {
                    renamedAttributes.clear();
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
     * Gets the prefix the URI is bound to. Since our (imported URI) ⟶ (exported URI) transformation
     * is not bijective, implementing this method could potentially result in the same prefix for different URIs,
     * which is illegal for a XML document and potentially dangerous. Thankfully JAXB seems to never invoke this
     * method in our tests.
     */
    @Override
    public String getPrefix(final String uri) throws XMLStreamException {
        throw new XMLStreamException(Errors.format(Errors.Keys.UnsupportedOperation_1, "getPrefix"));
    }

    /**
     * Sets the prefix the URI is bound to. This method replaces the given URI if needed, then forwards the call.
     * Note that it may result in the same URI to be bound to many prefixes. For example ISO 19115-3:2016 has many
     * URIs, each with a different prefix ({@code "mdb"}, {@code "cit"}, <i>etc.</i>). But all those URIs may be
     * replaced by the unique URI used in legacy ISO 19139:2007. Since this method does not replace the prefix
     * (it was {@code "gmd"} in ISO 19139:2007), the various ISO 19115-3:2016 prefixes are all bound to the same
     * legacy ISO 19139:2007 URI. This is confusing, but not ambiguous for XML parsers.
     *
     * <p>Implemented as a matter of principle, but JAXB did not invoked this method in our tests.</p>
     */
    @Override
    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        out.setPrefix(prefix, version.exportNS(uri));
    }

    /**
     * Binds a URI to the default namespace. Current implementation replaces the given URI
     * (e.g. an ISO 19115-3:2016 one) by the exported URI (e.g. legacy ISO 19139:2007 one),
     * then forwards the call.
     *
     * <p>Implemented as a matter of principle, but JAXB did not invoked this method in our tests.</p>
     */
    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        out.setDefaultNamespace(version.exportNS(uri));
    }

    /**
     * Sets the current namespace context for prefix and URI bindings.
     * This method unwraps the original context and forwards the call.
     *
     * <p>Implemented as a matter of principle, but JAXB did not invoked this method in our tests.</p>
     */
    @Override
    public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
        out.setNamespaceContext(FilteredNamespaces.asXML(context, version));
    }

    /**
     * Returns a naming context suitable for consumption by JAXB marshallers.
     * The {@link XMLEventWriter} wrapped by this {@code FilteredWriter} has been created for writing in a file.
     * Consequently its naming context manages namespaces used in the XML document. But the JAXB marshaller using
     * this {@code FilteredWriter} facade expects the namespaces declared in JAXB annotations. Consequently this
     * method returns an adapter that converts namespaces on the fly.
     *
     * @see Event#getNamespaceContext()
     */
    @Override
    public NamespaceContext getNamespaceContext() {
        return FilteredNamespaces.asJAXB(out.getNamespaceContext(), version);
    }

    /**
     * Wraps the {@link StartElement} produced by JAXB for using the namespaces used in the XML document.
     */
    private static final class Event extends FilteredEvent.Start {
        /** Wraps the given event with potentially different name, namespaces and attributes. */
        Event(StartElement event, QName name, List<Namespace> namespaces, List<Attribute> attributes, FilterVersion version) {
            super(event, name, namespaces, attributes, version);
        }

        /**
         * Gets the URI used in the XML document for the given prefix used in JAXB annotations.
         * At marshalling time, events are created by JAXB using namespaces used in JAXB annotations.
         * {@link FilteredWriter} wraps those events for converting those namespaces to the ones used
         * in the XML document.
         *
         * <div class="note"><b>Example:</b> the {@code "cit"} prefix from ISO 19115-3:2016 standard
         * represents the {@code "http://standards.iso.org/iso/19115/-3/mdb/1.0"} namespace, which is
         * mapped to {@code "http://www.isotc211.org/2005/gmd"} in the legacy ISO 19139:2007 standard.
         * That later URI is returned.</div>
         */
        @Override
        public String getNamespaceURI(final String prefix) {
            return version.exportNS(event.getNamespaceURI(prefix));
        }

        /**
         * Returns a context mapping prefixes used in JAXB annotations to namespaces used in XML document.
         * The {@link FilteredNamespaces#getNamespaceURI(String)} method in that context shall do the same
         * work than {@link #getNamespaceURI(String)} in this event.
         *
         * @see FilteredNamespaces#getNamespaceURI(String)
         */
        @Override
        public NamespaceContext getNamespaceContext() {
            return FilteredNamespaces.asXML(event.getNamespaceContext(), version);
        }
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
