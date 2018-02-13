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
import java.util.Iterator;
import java.util.Collections;
import javax.xml.namespace.QName;
import org.apache.sis.util.Debug;
import org.apache.sis.internal.jaxb.LegacyNamespaces;


/**
 * The target version of standards for {@link FilteredNamespaces}.
 *
 * See {@link FilteredNamespaces} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.4
 * @module
 */
final class FilterVersion {
    /**
     * Metadata using the legacy ISO 19139:2007 schema (replaced by ISO 19115-3).
     */
    static final FilterVersion ISO19139 = new FilterVersion(42, 2);
    static {
        ISO19139.addSurjectives(new String[] {
                Namespaces.CAT,
                Namespaces.CIT,
                Namespaces.DQC,
                Namespaces.FCC,
                Namespaces.GEX,
                Namespaces.GMW,
                Namespaces.LAN,
                Namespaces.MAC,
                Namespaces.MAS,
                Namespaces.MCC,
                Namespaces.MCO,
                Namespaces.MD1,
                Namespaces.MD2,
                Namespaces.MDA,
                Namespaces.MDB,
                Namespaces.MDQ,
                Namespaces.MDS,
                Namespaces.MDT,
                Namespaces.MEX,
                Namespaces.MMI,
                Namespaces.MPC,
                Namespaces.MRC,
                Namespaces.MRD,
                Namespaces.MRI,
                Namespaces.MRL,
                Namespaces.MRS,
                Namespaces.MSR,
                Namespaces.RCE
            }, LegacyNamespaces.GMD);
        /*
         * SV_OperationMetadata has two properties that are identical except for the name
         * between ISO 19115-3:2016 and legacy ISO 19139:2007. Instead than complicating
         * the class with JAXB annotations, we perform the renaming in this package.
         */
        Replacement r = new Replacement(LegacyNamespaces.SRV, 4);
        r.addProperty("distributedComputingPlatform", "DCP");
        r.addProperty("parameter", "parameters");
        ISO19139.exports.put(Namespaces.SRV, r);
        /*
         * For the way back from legacy ISO 19139:2007 to new ISO 19115-3:2016, we must rely on
         * FilteredReader (do NOT declare entries in 'imports', because some namespaces must be
         * left unchanged). An exception to this rule is the "gco" namespace because our reader
         * renames only element namespaces while we need to rename also attributes in "gco"
         * namespace (e.g. "gco:nilReason").
         */
        ISO19139.addSurjective(Namespaces.GCX, LegacyNamespaces.GMX);
        ISO19139.addBijective (Namespaces.GCO, LegacyNamespaces.GCO);
    }

    /**
     * GML using the legacy {@code "http://www.opengis.net/gml"} namespace.
     * Note that the use of GML 3.2 implies the use of ISO 19139:2007.
     */
    static final FilterVersion GML31 = new FilterVersion(ISO19139);
    static {
        GML31.addBijective(Namespaces.GML, LegacyNamespaces.GML);
    }

    /**
     * Apply all known namespace replacements. This can be used only at unmarshalling time,
     * for replacing all namespaces by the namespaces declared in Apache SIS JAXB annotations.
     */
    static final FilterVersion ALL = GML31;

    /**
     * The exported namespace used in the XML file instead then the namespaces used by JAXB annotations.
     * Contains also the properties renamed in that exported namespaces if any.
     */
    static final class Replacement {
        /**
         * The exported namespace (used in XML document).
         */
        final String namespace;

        /**
         * The properties having a different name in the exported namespace. Keys are property names
         * as declared in JAXB annotations, and values are property names used in the XML document.
         */
        private final Map<String,String> exports;

        /**
         * Constructs a replacement for a namespace only (no renamed property).
         */
        Replacement(final String namespace) {
            this.namespace  = namespace;
            this.exports = Collections.emptyMap();
        }

        /**
         * Constructs a replacement for a namespace, together with the map of properties
         * that need to be renamed for that exported namespace.
         */
        Replacement(final String namespace, final int capacity) {
            this.namespace  = namespace;
            this.exports = new HashMap<>(capacity);
        }

        /**
         * Adds a two-directional association between a property used in JAXB annotation and a property
         * used in XML document. A bijective association means that the renaming is reversible.
         */
        final void addProperty(final String jaxb, final String xml) {
            exports.put(jaxb, xml);
        }

        /**
         * Converts a property name used in JAXB annotations to a property name used in XML document.
         */
        final String exportProperty(final String localPart) {
            return exports.getOrDefault(localPart, localPart);
        }

        /**
         * Returns the namespace for debugging purpose.
         */
        @Override @Debug
        public String toString() {
            return namespace;
        }
    }

    /**
     * The URI replacements to apply when going from the model implemented by Apache SIS
     * to the filtered reader/writer. Keys are the URIs as declared in JAXB annotations,
     * and values are the URIs to write instead of the actual ones.
     *
     * <p>This map shall not be modified after construction.
     * We do not wrap in {@link Collections#unmodifiableMap(Map)} for efficiency.</p>
     *
     * @see #exports()
     */
    private final Map<String, Replacement> exports;

    /**
     * The URI replacements to apply when going from the filtered reader/writer to the
     * model implemented by Apache SIS. This map is the converse of {@link #exports}.
     * It does not contain the map of properties to rename because that map is handled
     * by {@link FilteredReader} instead, as part of {@code NamespaceContent.txt} file.
     *
     * <p>This map shall not be modified after construction.
     * We do not wrap in {@link Collections#unmodifiableMap(Map)} for efficiency.</p>
     */
    private final Map<String, String> imports;

    /**
     * Creates a new enumeration initialized to the given capacity.
     *
     * @param  ec  exports capacity.
     * @param  ic  imports capacity.
     */
    private FilterVersion(final int ec, final int ic) {
        exports = new HashMap<>(ec);
        imports = new HashMap<>(ic);
    }

    /**
     * Creates an enumeration initialized to a copy of the given enumeration.
     * This construction should be followed by calls to {@code add(…)} methods.
     */
    private FilterVersion(final FilterVersion first) {
        exports = new HashMap<>(first.exports);
        imports = new HashMap<>(first.imports);
    }

    /**
     * Adds a two-directional association between a namespace used in JAXB annotation and a namespace
     * used in XML document. A bijective association means that the renaming is reversible.
     */
    private void addBijective(final String jaxb, final String xml) {
        exports.put(jaxb, new Replacement(xml));
        imports.put(xml, jaxb);
    }

    /**
     * Adds a one-way association from JAXB namespace to XML namespace. Many JAXB namespaces may map
     * to the same XML namespace. For example most ISO 19115-3:2016 namespaces map to the same legacy
     * ISO 19139:2007 namespace. Consequently this association is not easily reversible.
     */
    private void addSurjective(final String jaxb, final String xml) {
        exports.put(jaxb, new Replacement(xml));
    }

    /**
     * Adds one-way associations from JAXB namespaces to a single XML namespace.
     * This method is used when the legacy schema (the {@code xml} one) was one large monolithic schema,
     * and the new schema (represented by {@code jaxb}) has been separated in many smaller modules.
     */
    private void addSurjectives(final String[] jaxb, final String xml) {
        final Replacement r = new Replacement(xml);
        for (final String e : jaxb) {
            exports.put(e, r);
        }
    }

    /**
     * Returns the name (prefix, namespace and local part) to write in the XML document.
     * This method may replace the namespace, and in some case the name local part too.
     * If there is no name change, then this method returns the given instance as-is.
     */
    final QName export(QName name) {
        String uri = name.getNamespaceURI();
        if (uri != null && !uri.isEmpty()) {                                // Optimization for a common case.
            final Replacement r = exports.get(uri);
            if (r != null) {
                uri = r.namespace;
                name = new QName(uri, r.exportProperty(name.getLocalPart()),
                        Namespaces.getPreferredPrefix(uri, name.getPrefix()));
                /*
                 * Note: the call for 'getPreferredPrefix' above is required:
                 * JAXB seems to need the prefixes for recognizing namespaces.
                 */
            }
        }
        return name;
    }

    /**
     * Converts a namespace used in JAXB annotation to the namespace used in XML document.
     * Returns the same URI if there is no replacement.
     */
    final String exportNS(final String uri) {
        final FilterVersion.Replacement r = exports.get(uri);
        return (r != null) ? r.namespace : uri;
    }

    /**
     * Converts a namespace used in XML document to the namespace used in JAXB annotation.
     * Returns the same URI if there is no replacement.
     */
    final String importNS(final String uri) {
        return imports.getOrDefault(uri, uri);
    }

    /**
     * Converts a namespace used in JAXB annotation to the namespace used in XML document,
     * together with a map of properties to rename.
     * Returns {@code null} if there is no replacement.
     */
    final Replacement export(final String uri) {
        return exports.get(uri);
    }

    /**
     * Returns the URI replacements to apply when going from the model implemented by Apache SIS to the
     * filtered reader/writer. Used only for more sophisticated work than what {@link #exportNS(String)} does.
     * Returned as an iterator for avoiding to expose modifiable map; do not invoke {@link Iterator#remove()}.
     */
    final Iterator<Map.Entry<String, Replacement>> exports() {
        return exports.entrySet().iterator();
    }
}
