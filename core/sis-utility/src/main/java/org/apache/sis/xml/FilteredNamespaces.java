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


/**
 * Substitutes at (un)marshalling time the XML namespaces used by SIS by the namespaces used in the XML document.
 * This class is used internally by {@link FilteredStreamReader} and {@link FilteredWriter} only.
 * Current {@code FilteredNamespaces} implementation takes care of XML prefixes only;
 * the stream reader and writer do the rest of the work.
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
 *       SIS corners (unconceivable), or to modify the {@code xjc} output in order to give to generated classes a
 *       common parent class or interface. In the later case, the auto-generated classes require significant work
 *       anyways.</li>
 *   <li>The namespaces of all versions appear in the {@code xmlns} attributes of the root element (we can not always
 *       create separated JAXB contexts), which is confusing and prevent usage of usual prefixes for all versions
 *       except one.</li>
 * </ul>
 *
 * An alternative is to support only one version of each standard, and transform XML documents before unmarshalling
 * or after marshalling if they use different versions of standards. We could use XSLT for that, but this is heavy.
 * A lighter approach is to use {@link javax.xml.stream.XMLStreamReader} and {@link javax.xml.stream.XMLStreamWriter}
 * as "micro-transformers".
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-152">SIS-152</a>
 *
 * @since 0.4
 * @module
 */
final class FilteredNamespaces implements NamespaceContext {
    /**
     * The context to wrap, given by {@link FilteredStreamReader} or {@link FilteredWriter}.
     *
     * @see javax.xml.stream.XMLStreamReader#getNamespaceContext()
     * @see javax.xml.stream.XMLStreamWriter#getNamespaceContext()
     */
    private final NamespaceContext context;

    /**
     * The URI replacements to apply when going from the wrapped context to the filtered context.
     *
     * @see FilterVersion#exports
     */
    private final Map<String,String> exports;

    /**
     * The URI replacements to apply when going from the filtered context to the wrapped context.
     * This map is the converse of {@link #exports}.
     *
     * @see FilterVersion#imports
     */
    private final Map<String,String> imports;

    /**
     * Creates a new namespaces filter for the given target version.
     */
    FilteredNamespaces(final NamespaceContext context, final FilterVersion version, final boolean inverse) {
        this.context = context;
        if (!inverse) {
            exports = version.exports;
            imports = version.imports;
        } else {
            exports = version.imports;
            imports = version.exports;
        }
    }

    /**
     * Wraps this {@code FilteredNamespaces} in a new instance performing the inverse of the replacements
     * specified by the given version.
     */
    NamespaceContext inverse(final FilterVersion version) {
        if (exports == version.exports && imports == version.imports) {
            return this;
        }
        return new FilteredNamespaces(this, version, true);
    }

    /**
     * Returns the namespace for the given prefix. The same URI may be returned for many prefixes.
     * For example when exporting from ISO 19115-3:2016 to legacy ISO 19139:2007, the {@code "mdb"},
     * {@code "cit"} and many other prefixes are all mapped to {@code "http://www.isotc211.org/2005/gmd"}.
     * This is legal according {@link NamespaceContext} javadoc.
     */
    @Override
    public String getNamespaceURI(final String prefix) {
        final String uri = context.getNamespaceURI(prefix);
        return exports.getOrDefault(uri, uri);
    }

    /**
     * Returns an arbitrary prefix for the given namespace. More than one prefix may be bounded to a namespace,
     * in which case this method returns an arbitrary prefix (which may differ between different JVM executions).
     */
    @Override
    public String getPrefix(final String namespaceURI) {
        final String ns = imports.get(namespaceURI);
        if (ns != null) {
            final String p = context.getPrefix(ns);
            if (p != null) return p;
        }
        /*
         * We can not use the 'imports' map when the same namespace (e.g. "http://www.isotc211.org/2005/gmd" from
         * legacy ISO 19139:2007) is mapped to multiple namespaces in the new ISO 19115-3:2016 or other standard.
         * In such case, we have to iterate over map 'exports' entries until we find an inverse mapping.
         */
        for (final Map.Entry<String,String> e : exports.entrySet()) {
            if (namespaceURI.equals(e.getValue())) {
                final String p = context.getPrefix(e.getKey());
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
        return new Prefixes(context, exports, namespaceURI);
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

        /** Iterator over the {@link FilteredNamespaces#exports} entries. */
        private final Iterator<Map.Entry<String,String>> exports;

        /** Iterator over some (not all) prefixes, or {@code null} if a new iterator needs to be fetched. */
        private Iterator<String> prefixes;

        /** The next value to be returned by {@link #next()}, or {@code null} if not yet fetched. */
        private String next;

        /** Creates a new iterator for the prefixes associated to the given namespace URI. */
        Prefixes(final NamespaceContext context, final Map<String,String> exports, final String namespaceURI) {
            this.context      = context;
            this.exports      = exports.entrySet().iterator();
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
                    final Map.Entry<String,String> e = exports.next();
                    if (namespaceURI.equals(e.getValue())) {
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
