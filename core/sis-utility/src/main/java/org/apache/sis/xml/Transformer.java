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

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import javax.xml.stream.events.Attribute;


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
     * The external XML format version to (un)marshal from.
     */
    final TransformVersion version;

    /**
     * Temporary list of attributes after their namespace change.
     * This list is recycled for each XML element to be read or written.
     */
    final List<Attribute> renamedAttributes;

    /**
     * Creates a new XML reader or writer.
     */
    Transformer(final TransformVersion version) {
        this.version = version;
        renamedAttributes = new ArrayList<>();
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
}
