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
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Arrays;
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
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;


/**
 * A writer replacing the namespaces used by JAXB by other namespaces to be used in the XML document
 * at marshalling time. This class forwards every method calls to the wrapped {@link XMLEventWriter},
 * with all {@code namespaceURI} arguments transformed before to be delegated.
 *
 * See {@link Transformer} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
final class TransformingWriter extends Transformer implements XMLEventWriter {
    /**
     * Location of the file listing types and their properties contained in legacy namespaces.
     * This is used for mapping new ISO 19115-3:2016 namespaces to legacy ISO 19139:2007 ones,
     * where the same {@code "http://standards.iso.org/iso/19115/-3/…"} URI is used in places
     * where legacy schema had two distinct URIs:  {@code "http://www.isotc211.org/2005/gmd"}
     * and {@code "http://standards.iso.org/iso/19115/-2/gmi/1.0"}.
     */
    private static final String FILENAME = "RenameOnExport.lst";

    /**
     * The mapping from (<var>type</var>, <var>attribute</var>) pairs to legacy namespaces.
     *
     * <ul>
     *   <li>Keys are XML names of types, ignoring {@code "_TYPE"} suffix (e.g. {@code "MI_Georectified"})</li>
     *   <li>Values are maps where:<ul>
     *     <li>Keys are XML names of properties (e.g. {@code "checkPoint"})</li>
     *     <li>Values are either:<ul>
     *       <li>Namespace URI if {@link #isNamespace(String)} returns {@code true} for that value.</li>
     *       <li>New name of the element otherwise. In such case, the map must be queried again with
     *           that new name for obtaining the namespace.</li>
     *     </ul></li>
     *   </ul></li>
     * </ul>
     *
     * This map is initialized only once and should not be modified after that point.
     */
    private static final Map<String, Map<String,String>> NAMESPACES = load(true, FILENAME, Set.of(), 60);

    /**
     * Elements that appear in different order in ISO 19139:2007 (or other legacy standards) compared
     * to ISO 19115-3:2016 (or other newer standards). Key are names of elements to reorder.
     * Values are the elements to skip before to write the element to reorder.
     *
     * <p>While this map is used for reordering elements according legacy standards, the {@link QName} keys and values
     * use the namespaces of newer standards. This is because newer standards like ISO 19115-3 uses many namespaces
     * where legacy ISO 19139:2007 used only one namespace, so using the newer names reduce the risk of confusion.</p>
     *
     * <h4>Example</h4>
     * In {@code SV_ServiceIdentification}, {@code <srv:couplingType>} appears before {@code <srv:coupledResource>}
     * according ISO 19115-3:2016. But in ISO 19139:2007, it was the reverse order. Since Apache SIS writes elements
     * in the order defined by the newer standard, {@code <couplingType>} is encountered first. The set associated
     * to that key tells us that, when writing legacy ISO 19139 document, we should skip {@code <coupledResource>}
     * before to write {@code <srv:couplingType>}.
     *
     * @see #toSkip
     * @see #deferred
     *
     * @todo Hard-coded for now. Should move to a resource file in a future version.
     */
    private static final Map<QName, Set<QName>> ELEMENTS_TO_REORDER;
    static {
        final Map<QName, Set<QName>> m = new HashMap<>(4);
        m.put(new QName(Namespaces.SRV, "couplingType",  "srv"), Set.of(new QName(Namespaces.SRV, "coupledResource", "srv")));
        m.put(new QName(Namespaces.SRV, "connectPoint",  "srv"), Set.of(new QName(Namespaces.SRV, "parameter",       "srv")));
        /*
         * ISO 19139:2997 declared `topicCategory` and `extent` in MD_DataIdentification subclass, while ISO 19115-3
         * moves them to the MD_Identification parent class. In order to write topicCategory at location expected by
         * legacy metadata, we need to skip all properties declared after `topicCategory` in that parent class.
         */
        QName first;
        HashSet<QName> toSkip = new HashSet<>(Arrays.asList(
            first = new QName(      Namespaces.MRI, "extent",                  "mri"),
                    new QName(      Namespaces.MRI, "additionalDocumentation", "mri"),
                    new QName(      Namespaces.MRI, "processingLevel",         "mri"),
                    new QName(      Namespaces.MRI, "resourceMaintenance",     "mri"),
                    new QName(      Namespaces.MRI, "graphicOverview",         "mri"),
                    new QName(      Namespaces.MRI, "resourceFormat",          "mri"),
                    new QName(      Namespaces.MRI, "descriptiveKeywords",     "mri"),
                    new QName(      Namespaces.MRI, "resourceSpecificUsage",   "mri"),
                    new QName(      Namespaces.MRI, "resourceConstraints",     "mri"),
                    new QName(      Namespaces.MRI, "associatedResource",      "mri"),
                    new QName(LegacyNamespaces.GMD, "aggregationInfo",         "gmd"),
                    new QName(LegacyNamespaces.GMD, "language",                "gmd"),
                    new QName(LegacyNamespaces.GMD, "characterSet",            "gmd"),
                    new QName(      Namespaces.MRI, "defaultLocale",           "mri"),
                    new QName(      Namespaces.MRI, "otherLocale",             "mri")));
        /*
         * The `extent` element is right after `topicCategory` in ISO 19115-3:2016, but there was an
         * `environmentDescription` between them in legacy ISO 19139:2007. So we add the latter in the
         * list of elements to skip for `extent`.
         */
        m.put(new QName(Namespaces.MRI, "topicCategory", "mri"), CollectionsExt.clone(toSkip));
        toSkip.remove(first);
        toSkip.add(new QName(Namespaces.MRI, "environmentDescription", "mri"));
        m.put(first, toSkip);                                                     // For <mri:extent>
        ELEMENTS_TO_REORDER = m;
    }

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
     * {@code true} if events should be sent to {@link #deferred} instead of to {@link #out}.
     * This is set to {@code true} when we see a {@link StartElement} having one of the names
     * contained in the {@link #ELEMENTS_TO_REORDER} keys set.
     */
    private boolean isDeferring;

    /**
     * Events for which writing is deferred as long as there is elements {@linkplain #toSkip to skip}.
     * The intent is to reorder elements that appear in a different order in legacy standards compared
     * to newer standards. This is a FIFO (First-In-First-Out) queue. Namespaces are the exported ones
     * (the ones after conversions from JAXB to the XML document to write).
     *
     * <p>Elements are instance of {@link XMLEvent} or {@link NewDeferred}.</p>
     *
     * @see #ELEMENTS_TO_REORDER
     */
    private final Queue<Object> deferred;

    /**
     * If non-null, elements to skip before we can write the {@linkplain #deferred} events.
     * Should be the {@link #ELEMENTS_TO_REORDER} value associated to the element to defer.
     * A null value means that events can be written immediately to {@link #out}.
     */
    private Set<QName> toSkip;

    /**
     * Name of the root element of a sub-tree to handle in a special way, or {@code null} if none.
     * At first, this is the name of the {@link StartElement} of a sub-tree to defer (i.e. one of the
     * keys in the {@link #ELEMENTS_TO_REORDER} map). Later, it becomes the names of sub-trees to skip
     * (i.e. the {@link #toSkip} values).
     */
    private QName subtreeRootName;

    /**
     * Number of times that {@link #subtreeRootName} has been found. A value of 1 means that we started
     * receiving events for that subtree. A value of 0 means that we finished receiving events for that
     * subtree. A value greater than 1 means that there is nested sub-trees (should not happen).
     */
    private int subtreeNesting;

    /**
     * Creates a new writer for the given version of the standards.
     */
    TransformingWriter(final XMLEventWriter out, final TransformVersion version) {
        super(version);
        this.out = out;
        uniqueNamespaces = new LinkedHashMap<>();
        deferred = new ArrayDeque<>();
    }

    /**
     * Returns the map loaded by {@link #load(boolean, String, Set, int)}.
     *
     * @param  namespace  the namespace URI for which to get the substitution map.
     * @return the substitution map for the given namespace.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Map<String, Map<String,String>> renamingMap(final String namespace) {
        return NAMESPACES;
    }

    /**
     * Returns the old namespace for elements (types and properties) in the given namespace.
     * This method is used only for default relocations, i.e. the fallback to apply when no
     * explicit rule has been found.
     */
    @Override
    final String relocate(final String namespace) {
        return version.exportNS(namespace);
    }

    /**
     * Returns the prefix to use for a name in a new namespace.
     *
     * @param  previous   the prefix associated to old namespace.
     * @param  namespace  the new namespace URI.
     * @return prefix to use for the new namespace.
     * @throws XMLStreamException if an error occurred while fetching the prefix.
     */
    @Override
    final String prefixReplacement(final String previous, final String namespace) throws XMLStreamException {
        /*
         * The wrapped XMLEventWriter maintains a mapping from prefixes to namespace URIs.
         * Arguments are exported URIs (e.g. from legacy ISO 19139:2007) and return values
         * are prefixes computed by `Namespaces.getPreferredPrefix(…)` or any other means.
         * We fetch those prefixes for performance reasons and for improving the guarantees
         * that the URI → prefix mapping is stable, since JAXB seems to require them for
         * writing namespaces in XML.
         */
        String prefix = out.getPrefix(namespace);
        if (prefix == null) {
            prefix = Namespaces.getPreferredPrefix(namespace, previous);
            out.setPrefix(prefix, namespace);
            /*
             * The above call for `getPreferredPrefix` above is required: JAXB seems to need the prefixes
             * for recognizing namespaces. The prefix shall be computed in the same way as `exportIfNew`.
             * We enter in this block only for the root element, before to parse `xmlns` attributes. For
             * all other elements after the root elements, above call to `out.getPrefix(uri)` should succeed.
             */
        }
        return prefix;
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
        notify(namespace);
        String uri = namespace.getNamespaceURI();
        if (uri != null && !uri.isEmpty()) {
            /*
             * Ignore trailing slash when checking if there is a namespace change.
             * But if we do not find a namespace change, keep the trailing slash as
             * given in the Namespace since that slash may be intentional.
             */
            final String trimed = removeTrailingSlash(uri);
            final String exported = relocate(trimed);
            if (exported != trimed) {
                return uniqueNamespaces.computeIfAbsent(exported, (k) -> {
                    return new TransformedEvent.NS(namespace, Namespaces.getPreferredPrefix(k, namespace.getPrefix()), k);
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
     * @param  namespaces  the namespaces to transform.
     * @param  changed     whether to unconditionally pretend that there is a change.
     * @return the updated namespaces, or {@code null} if there is no change.
     */
    private List<Namespace> export(final Iterator<Namespace> namespaces, boolean changed) {
        if (!namespaces.hasNext()) {
            return changed ? List.of() : null;
        }
        do {
            final Namespace namespace = namespaces.next();
            changed |= (namespace != exportIfNew(namespace));
        } while (namespaces.hasNext());
        if (changed) {
            assert !uniqueNamespaces.isEmpty();
            final var exported = uniqueNamespaces.values().toArray(Namespace[]::new);
            uniqueNamespaces.clear();
            return Arrays.asList(exported);
        } else {
            uniqueNamespaces.clear();
            return null;
        }
    }

    /**
     * Converts an event from the namespaces used in JAXB annotations to the namespaces used in the XML document
     * to write. This method may wrap the given event into another event for changing the namespace and prefix,
     * or use the event as-is if no change is needed.
     *
     * @param  event  the event using JAXB namespaces.
     */
    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        switch (event.getEventType()) {
            case ATTRIBUTE: {
                event = convert((Attribute) event);
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
            case END_ELEMENT: {
                final EndElement e = event.asEndElement();
                final QName originalName = e.getName();
                final QName name = convert(originalName);
                final List<Namespace> namespaces = export(e.getNamespaces(), name != originalName);
                if (namespaces != null) {
                    event = new TransformedEvent.End(e, name, namespaces);
                }
                if (toSkip != null) {                           // Check if a previous START_ELEMENT found a need to reorder.
                    if (originalName.equals(subtreeRootName)) {
                        subtreeNesting--;                       // May reach 0 but be followed by another element to skip.
                    } else if (subtreeNesting == 0) {
                        writeDeferred(null);                    // About to exit the parent element containing deferred element.
                    }
                }
                close(originalName);                            // Must be invoked only after `convert(QName)`
                break;
            }
            case START_ELEMENT: {
                uniqueNamespaces.clear();                       // Discard entries created by NAMESPACE events.
                final StartElement e = event.asStartElement();
                final QName originalName = e.getName();
                open(originalName);                             // Must be invoked before `convert(QName)`.
                final QName name = convert(originalName);
                boolean changed = name != originalName;
                for (final Iterator<Attribute> it = e.getAttributes(); it.hasNext();) {
                    final Attribute a = it.next();
                    final Attribute ae = convert(a);
                    changed |= (a != ae);
                    renamedAttributes.add(ae);
                }
                final List<Namespace> namespaces = export(e.getNamespaces(), changed);
                if (namespaces != null) {
                    event = new Event(e, name, namespaces, attributes(), version);
                } else {
                    renamedAttributes.clear();          // Note: above call to attributes() also cleared that list.
                }
                /*
                 * At this point, we finished to export the event (i.e. to convert namespaces to the URI
                 * used in the XML documents). The remaining code in this block is for handling the case
                 * where the elements should not be written immediately, but after some other events.
                 * This happen when legacy standards ordered some elements differently.
                 */
                if (toSkip == null) {
                    toSkip = ELEMENTS_TO_REORDER.get(originalName);
                    if (toSkip != null) {
                        subtreeRootName = originalName;                 // Found a new element to defer.
                        subtreeNesting = 1;
                        isDeferring = true;
                    }
                } else if (subtreeNesting == 0) {
                    /*
                     * If the current element should not be skipped (toSkip.contains(…) = false), then
                     * we need to write all deferred elements. Usually `writeDeferred(…)` returns false,
                     * so the block inside the `if` is executed only if `toSkip.contains(…)` is true.
                     * But in few cases, we still need to be in "skipping" state after `writeDeferred`.
                     */
                    if (toSkip.contains(originalName) || writeDeferred(originalName)) {
                        /*
                         * Found an element to skip. That element will be written immediately (except if
                         * it is another element which needs reordering), and the elements currently in
                         * the `deferred` list will continue to be deferred at least until we reached the
                         * end of the current element. It may happen that the current element is itself
                         * an element that needs reordering (i.e. we may have nested reordered elements),
                         * in which case we reset the `isDeferring` flag to `true`.
                         */
                        subtreeRootName = originalName;
                        subtreeNesting = 1;
                        final Set<QName> interleaved = ELEMENTS_TO_REORDER.get(originalName);
                        if (interleaved != null) {
                            isDeferring = true;
                            deferred.add(new NewDeferred(interleaved));
                        }
                    }
                } else if (originalName.equals(subtreeRootName)) {
                    subtreeNesting++;
                }
                break;
            }
        }
        if (isDeferring) {
            deferred.add(event);
            isDeferring = (subtreeNesting != 0);
        } else {
            out.add(event);
        }
    }

    /**
     * A sentinel value in the {@link TransformingWriter#deferred} queue meaning that after reaching this point,
     * we need to reevaluate if the remaining elements should be written immediately of deferred again.
     * This happen when some elements to move are interleaved. For example, in {@code MD_DataIdentification}:
     *
     * <ol>
     *   <li>{@code topicCategory} needs to move before {@code environmentDescription}</li>
     *   <li>{@code extent} needs to move before {@code supplementalInformation}</li>
     *   <li>{@code graphicOverviews}</li>
     *   <li>{@code resourceFormats}</li>
     *   <li><i>etc.</i>
     *   <li>{@code environmentDescription}</li>
     *   <li>{@code supplementalInformation}</li>
     * </ol>
     *
     * This class is for handling the {@code extent} case in such scenario.
     */
    private static final class NewDeferred {
        /** The value to assign to {@link TransformingWriter#deferred} after we reach this point. */
        final Set<QName> toSkip;

        /** Creates a new sentinel value for a reevaluation point. */
        NewDeferred(final Set<QName> toSkip) {
            this.toSkip = toSkip;
        }
    }

    /**
     * Writes immediately all elements that were deferred. This happen because the next {@link StartElement}
     * to write should be after the deferred element, or because we are about to exit the parent element that
     * contains the deferred element, or because {@link #flush()} has been invoked.
     *
     * @param  element  the {@link StartElement} element name, or {@code null} for other events.
     * @return {@code true} if the given element starts a new subtree to skip.
     *
     * @see #ELEMENTS_TO_REORDER
     */
    private boolean writeDeferred(final QName element) throws XMLStreamException {
        subtreeRootName = null;
        toSkip = null;
        Object v;
        while ((v = deferred.poll()) != null) {
            if (!(v instanceof NewDeferred)) {
                out.add((XMLEvent) v);
            } else if (element != null) {
                final Set<QName> s = ((NewDeferred) v).toSkip;
                if (s.contains(element)) {
                    toSkip = s;
                    return true;
                }
            }
        }
        return false;
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
     * Note that it may result in the same URI to be bound to many prefixes. For example, ISO 19115-3:2016 has many
     * URIs, each with a different prefix ({@code "mdb"}, {@code "cit"}, <i>etc.</i>). But all those URIs may be
     * replaced by the unique URI used in legacy ISO 19139:2007. Since this method does not replace the prefix
     * (it was {@code "gmd"} in ISO 19139:2007), the various ISO 19115-3:2016 prefixes are all bound to the same
     * legacy ISO 19139:2007 URI. This is confusing, but not ambiguous for XML parsers.
     *
     * <p>Implemented as a matter of principle, but JAXB did not invoked this method in our tests.</p>
     */
    @Override
    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        out.setPrefix(prefix, relocate(uri));
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
        out.setDefaultNamespace(relocate(uri));
    }

    /**
     * Sets the current namespace context for prefix and URI bindings.
     * This method unwraps the original context and forwards the call.
     *
     * <p>Implemented as a matter of principle, but JAXB did not invoked this method in our tests.</p>
     */
    @Override
    public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
        out.setNamespaceContext(TransformingNamespaces.asXML(context, version));
    }

    /**
     * Returns a naming context suitable for consumption by JAXB marshallers.
     * The {@link XMLEventWriter} wrapped by this {@code TransformingWriter} has been created for writing in a file.
     * Consequently, its naming context manages namespaces used in the XML document. But the JAXB marshaller using
     * this {@code TransformingWriter} facade expects the namespaces declared in JAXB annotations. Consequently, this
     * method returns an adapter that converts namespaces on the fly.
     *
     * @see Event#getNamespaceContext()
     */
    @Override
    public NamespaceContext getNamespaceContext() {
        return TransformingNamespaces.asJAXB(out.getNamespaceContext(), version);
    }

    /**
     * Wraps the {@link StartElement} produced by JAXB for using the namespaces used in the XML document.
     */
    private static final class Event extends TransformedEvent.Start {
        /** Wraps the given event with potentially different name, namespaces and attributes. */
        Event(StartElement event, QName name, List<Namespace> namespaces, List<Attribute> attributes, TransformVersion version) {
            super(event, name, namespaces, attributes, version);
        }

        /**
         * Gets the URI used in the XML document for the given prefix used in JAXB annotations.
         * At marshalling time, events are created by JAXB using namespaces used in JAXB annotations.
         * {@link TransformingWriter} wraps those events for converting those namespaces to the ones used
         * in the XML document.
         *
         * <h4>Example</h4>
         * The {@code "cit"} prefix from ISO 19115-3:2016 standard
         * represents the {@code "http://standards.iso.org/iso/19115/-3/mdb/1.0"} namespace, which is
         * mapped to {@code "http://www.isotc211.org/2005/gmd"} in the legacy ISO 19139:2007 standard.
         * That later URI is returned.
         */
        @Override
        public String getNamespaceURI(final String prefix) {
            return version.exportNS(event.getNamespaceURI(prefix));
        }

        /**
         * Returns a context mapping prefixes used in JAXB annotations to namespaces used in XML document.
         * The {@link TransformingNamespaces#getNamespaceURI(String)} method in that context shall do the same
         * work than {@link #getNamespaceURI(String)} in this event.
         *
         * @see TransformingNamespaces#getNamespaceURI(String)
         */
        @Override
        public NamespaceContext getNamespaceContext() {
            return TransformingNamespaces.asXML(event.getNamespaceContext(), version);
        }
    }

    /**
     * Writes any cached events to the underlying output mechanism.
     */
    @Override
    public void flush() throws XMLStreamException {
        writeDeferred(null);
        out.flush();
    }

    /**
     * Frees any resources associated with this writer.
     */
    @Override
    public void close() throws XMLStreamException {
        uniqueNamespaces.clear();
        super.close();
        out.close();
    }
}
