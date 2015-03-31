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
import javax.xml.namespace.NamespaceContext;


/**
 * Substitutes at (un)marshalling time the XML namespaces used by SIS by the namespaces used in the XML document.
 * This class is used internally by {@link FilteredStreamReader} and {@link FilteredStreamWriter} only.
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
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see <a href="http://issues.apache.org/jira/browse/SIS-152">SIS-152</a>
 */
final class FilteredNamespaces implements NamespaceContext {
    /**
     * The context to wrap, given by {@link FilteredStreamReader} or {@link FilteredStreamWriter}.
     *
     * @see javax.xml.stream.XMLStreamReader#getNamespaceContext()
     * @see javax.xml.stream.XMLStreamWriter#getNamespaceContext()
     */
    private final NamespaceContext context;

    /**
     * The URI replacements to apply when going from the wrapped context to the filtered context.
     *
     * @see FilterVersion#toView
     */
    private final Map<String,String> toView;

    /**
     * The URI replacements to apply when going from the filtered context to the wrapped context.
     * This map is the converse of {@link #toView}.
     *
     * @see FilterVersion#toImpl
     */
    private final Map<String,String> toImpl;

    /**
     * Creates a new namespaces filter for the given target version.
     */
    FilteredNamespaces(final NamespaceContext context, final FilterVersion version, final boolean inverse) {
        this.context = context;
        if (!inverse) {
            toView = version.toView;
            toImpl = version.toImpl;
        } else {
            toView = version.toImpl;
            toImpl = version.toView;
        }
    }

    /**
     * Wraps this {@code FilteredNamespaces} in a new instance performing the inverse of the replacements
     * specified by the given version.
     */
    NamespaceContext inverse(final FilterVersion version) {
        if (toView == version.toView && toImpl == version.toImpl) {
            return this;
        }
        return new FilteredNamespaces(this, version, true);
    }

    /**
     * Returns the URI to make visible to the user of this filter.
     */
    private String toView(final String uri) {
        final String replacement = toView.get(uri);
        return (replacement != null) ? replacement : uri;
    }

    /**
     * Returns the URI used by the {@linkplain #context}.
     */
    private String toImpl(final String uri) {
        final String replacement = toImpl.get(uri);
        return (replacement != null) ? replacement : uri;
    }

    /**
     * Returns the namespace for the given prefix.
     */
    @Override
    public String getNamespaceURI(final String prefix) {
        return toView(context.getNamespaceURI(prefix));
    }

    /**
     * Returns the prefix for the given namespace.
     */
    @Override
    public String getPrefix(final String namespaceURI) {
        return context.getPrefix(toImpl(namespaceURI));
    }

    /**
     * Returns all prefixes for the given namespace.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<String> getPrefixes(final String namespaceURI) {
        return context.getPrefixes(toImpl(namespaceURI));
    }
}
