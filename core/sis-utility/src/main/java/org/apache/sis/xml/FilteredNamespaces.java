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
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.XMLConstants;


/**
 * In the associations between prefixes and namespaces, substitutes the namespaces used in JAXB annotations by the
 * namespaces used in the XML document at marshalling time. This class is used internally by {@link FilteredReader}
 * and {@link FilteredWriter} only.
 *
 * <div class="section">The problem</div>
 * When the XML schemas of an international standard is updated, the URL of the namespace is often modified.
 * For example when GML has been updated from version 3.1 to 3.2, the URL mandated by the international standard
 * changed from {@code "http://www.opengis.net/gml"} to {@code "http://www.opengis.net/gml/3.2"}
 * (XML namespaces usually have a version number or publication year - GML before 3.2 were an exception).
 *
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
 * as "micro-transformers".
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-152">SIS-152</a>
 *
 * @since 0.4
 * @module
 */
class FilteredNamespaces implements NamespaceContext {
    /**
     * Given the context for namespaces used in our JAXB annotations, returns a context working with namespaces
     * used in XML document. The returned context converts namespace arguments from XML to JAXB namespaces, and
     * converts returned namespaces from JAXB to XML.
     *
     * <div class="note"><b>Example:</b>
     * for a {@code "http://www.isotc211.org/2005/gmd"} namespace (legacy ISO 19139:2007) given in argument to
     * {@link #getPrefixes(String)}, the context converts that namespace to all possible ISO 19115-3 namespaces
     * (there is many) and returns the associated prefixes: {@code "mdb"}, {@code "cit"}, <i>etc.</i>
     * Conversely given a {@code "mdb"}, {@code "cit"}, <i>etc.</i>, prefix, {@link #getNamespaceURI(String)}
     * method returns the above-cited legacy GMD namespace.</div>
     */
    static NamespaceContext asXML(NamespaceContext context, final FilterVersion version) {
        if (context != null) {
            if (context instanceof Inverse && ((Inverse) context).version == version) {
                context = ((Inverse) context).context;
            } else {
                context = new FilteredNamespaces(context, version);
            }
        }
        return context;
    }

    /**
     * Given a context for namespaces used in XML document, returns a context working with the namespaces used
     * in our JAXB annotations.  The returned context converts namespace arguments from JAXB to XML namespaces
     * before to delegate to the wrapped context, and converts returned namespaces from XML to JAXB.
     *
     * <p>This can be used when a {@link javax.xml.stream.XMLEventWriter} has been created for writing a legacy
     * XML document and we want to expose a {@link FilteredWriter} view to give to JAXB.</p>
     */
    static NamespaceContext asJAXB(NamespaceContext context, final FilterVersion version) {
        if (context != null) {
            if (context.getClass() == FilteredNamespaces.class && ((FilteredNamespaces) context).version == version) {
                context = ((FilteredNamespaces) context).context;
            } else {
                context = new FilteredNamespaces.Inverse(context, version);
            }
        }
        return context;
    }

    /**
     * The context to wrap, given by {@link FilteredReader} or {@link FilteredWriter}.
     *
     * @see javax.xml.stream.XMLEventWriter#getNamespaceContext()
     */
    final NamespaceContext context;

    /**
     * The URI replacements to apply when exporting from the JAXB annotations to the XML documents.
     */
    final FilterVersion version;

    /**
     * Creates a new namespaces filter for the given target version.
     */
    private FilteredNamespaces(final NamespaceContext context, final FilterVersion version) {
        this.context = context;
        this.version = version;
    }

    /**
     * Substitutes the XML namespaces used in XML documents by namespaces used in JAXB annotations.
     * This is used at marshalling time for exporting legacy documents, performing the reverse of
     * {@link FilteredNamespaces}. The <i>namespace → prefix</i> mapping is simple because various
     * ISO 19115-3 namespaces are mapped to the same legacy {@code "gmd"} prefix, but the reverse
     * operation (<i>prefix → namespace</i> mapping) can often not be resolved.
     */
    private static final class Inverse extends FilteredNamespaces {
        /** Creates a new namespaces filter for the given source version. */
        Inverse(final NamespaceContext context, final FilterVersion version) {
            super(context, version);
        }

        /**
         * Returns the namespace used in JAXB annotations for the given prefix in XML document.
         * If no unique namespace can be mapped (for example if asking the namespace of legacy
         * {@code "gmd"} prefix), returns {@link XMLConstants#NULL_NS_URI}.
         *
         * <p>Except for {@code NULL_NS_URI}, this is usually an <cite>injective</cite> function:
         * each namespace can be created from at most one prefix.</p>
         *
         * @see FilteredEvent.Start#getNamespaceURI(String)
         */
        @Override public String getNamespaceURI(final String prefix) {
            return version.importNS(context.getNamespaceURI(prefix));
        }

        /**
         * Returns an arbitrary prefix for the given namespace. For example given the
         * {@code "http://standards.iso.org/iso/19115/-3/mdb/1.0"} namespace from ISO 19115-3,
         * this method returns {@code "gmd"} which was the prefix used in legacy ISO 19139:2007.
         *
         * <p>This is a <cite>surjective</cite> function:
         * many prefixes can be created from the same namespace.</p>
         */
        @Override
        public String getPrefix(final String namespaceURI) {
            return context.getPrefix(version.exportNS(namespaceURI));
        }

        /**
         * Returns all prefixes for the given namespace. There is usually only one, contrarily
         * to {@link FilteredNamespaces#getPrefixes(String)} which have many.
         *
         * <p>This is a <cite>surjective</cite> function:
         * many prefixes can be created from the same namespace.</p>
         */
        @Override
        @SuppressWarnings("unchecked")      // TODO: remove with JDK9
        public Iterator<String> getPrefixes(final String namespaceURI) {
            return context.getPrefixes(version.exportNS(namespaceURI));
        }
    }

    /**
     * Returns the namespace for the given prefix. The same URI may be returned for many prefixes.
     * For example when exporting from ISO 19115-3:2016 to legacy ISO 19139:2007, the {@code "mdb"},
     * {@code "cit"} and many other prefixes are all mapped to {@code "http://www.isotc211.org/2005/gmd"}.
     * This is legal according {@link NamespaceContext} javadoc.
     */
    @Override
    public String getNamespaceURI(final String prefix) {
        return version.exportNS(context.getNamespaceURI(prefix));
    }

    /**
     * Returns an arbitrary prefix for the given namespace. More than one prefix may be bounded to a namespace,
     * in which case this method returns an arbitrary prefix (which may differ between different JVM executions).
     */
    @Override
    public String getPrefix(final String namespaceURI) {
        String p = context.getPrefix(version.importNS(namespaceURI));
        if (p != null) return p;
        /*
         * We can not use the 'imports' map when the same namespace (e.g. "http://www.isotc211.org/2005/gmd" from
         * legacy ISO 19139:2007) is mapped to multiple namespaces in the new ISO 19115-3:2016 or other standard.
         * In such case, we have to iterate over 'exports' entries until we find an inverse mapping.
         */
        final Iterator<Map.Entry<String, FilterVersion.Replacement>> it = version.exports();
        while (it.hasNext()) {
            final Map.Entry<String, FilterVersion.Replacement> e = it.next();
            if (namespaceURI.equals(e.getValue().namespace)) {
                p = context.getPrefix(e.getKey());
                if (p != null) return p;
            }
        }
        return context.getPrefix(namespaceURI);
    }

    /**
     * Returns all prefixes for the given namespace. For example given the {@code "http://www.isotc211.org/2005/gmd"}
     * namespace from legacy ISO 19139:2007, this method returns {@code "mdb"}, {@code "cit"} and all other prefixes
     * from the new ISO 19115-3:2016 specification which are used in replacement of the legacy {@code "gmd"} prefix.
     */
    @Override
    public Iterator<String> getPrefixes(final String namespaceURI) {
        return new Prefixes(context, version.exports(), namespaceURI);
    }

    /**
     * Iterator for the prefixes to be returned by {@link FilteredNamespaces#getPrefixes(String)}.
     * Each prefix is fetched only when first needed.
     */
    private static final class Prefixes implements Iterator<String> {
        /** The namespace for which prefixes are desired. */
        private final String namespaceURI;

        /** The {@link FilteredNamespaces#context} reference. */
        private final NamespaceContext context;

        /** Iterator over the namespace replacements. */
        private final Iterator<Map.Entry<String, FilterVersion.Replacement>> exports;

        /** Iterator over some (not all) prefixes, or {@code null} if a new iterator needs to be fetched. */
        private Iterator<String> prefixes;

        /** The next value to be returned by {@link #next()}, or {@code null} if not yet fetched. */
        private String next;

        /** Creates a new iterator for the prefixes associated to the given namespace URI. */
        Prefixes(final NamespaceContext context, final Iterator<Map.Entry<String, FilterVersion.Replacement>> exports,
                 final String namespaceURI)
        {
            this.context      = context;
            this.exports      = exports;
            this.namespaceURI = namespaceURI;
        }

        /**
         * Returns {@code true} if there is at least one more prefix to return.
         * Invoking this method fetched at most one prefix from the wrapped context.
         */
        @Override
        @SuppressWarnings("unchecked")          // TODO: remove on JDK9
        public boolean hasNext() {
            while (next == null) {
                while (prefixes == null) {
                    if (!exports.hasNext()) {
                        return false;
                    }
                    final Map.Entry<String, FilterVersion.Replacement> e = exports.next();
                    if (namespaceURI.equals(e.getValue().namespace)) {
                        prefixes = context.getPrefixes(e.getKey());
                    }
                }
                if (prefixes.hasNext()) {
                    next = prefixes.next();
                } else {
                    prefixes = null;
                }
            }
            return true;
        }

        /**
         * Returns the next prefix.
         */
        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final String n = next;
            next = null;
            return n;
        }
    }
}
