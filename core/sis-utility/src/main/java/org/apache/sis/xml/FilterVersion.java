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
import java.util.Collections;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.jaxb.LegacyNamespaces;

import static java.util.Collections.singletonMap;


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
enum FilterVersion {
    /**
     * Metadata using the legacy ISO 19139:2007 schema (replaced by ISO 19115-3).
     */
    ISO19139(new String[] {
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
        }, LegacyNamespaces.GMD,
        new String[] {
            Namespaces.GCX, LegacyNamespaces.GMX,
            Namespaces.GCO, LegacyNamespaces.GCO,
            Namespaces.SRV, LegacyNamespaces.SRV},
        Collections.singletonMap(LegacyNamespaces.GCO, Namespaces.GCO)),
        /*
         * For the way back from legacy ISO 19139:2007 to new ISO 19115-3:2016, we must rely on
         * FilteredStreamResolver (do NOT declare entries in 'imports', because some namespaces
         * must be left unchanged). An exception to this rule is the "gco" namespace because
         * FilteredStreamResolver renames only element namespaces while we need to rename also
         * attributes in "gco" namespace (e.g. "gco:nilReason").
         */

    /**
     * GML using the legacy {@code "http://www.opengis.net/gml"} namespace.
     * Note that the use of GML 3.2 may imply the use of ISO 19139:2007,
     * which requires the use of {@link #ALL}.
     */
    GML31(Namespaces.GML, LegacyNamespaces.GML),

    /**
     * Apply all known namespace replacements. This can be used only at unmarshalling time,
     * for replacing all namespaces by the namespaces declared in Apache SIS JAXB annotations.
     */
    ALL(ISO19139, GML31);

    /**
     * The URI replacements to apply when going from the model implemented by Apache SIS
     * to the filtered reader/writer. Keys are the URIs as declared in JAXB annotations,
     * and values are the URIs to write instead of the actual ones.
     *
     * @see FilteredNamespaces#exports
     */
    final Map<String,String> exports;

    /**
     * The URI replacements to apply when going from the filtered reader/writer to the
     * model implemented by Apache SIS. This map is the converse of {@link #exports}.
     *
     * @see FilteredNamespaces#imports
     */
    final Map<String,String> imports;

    /**
     * {@code true} if applying {@link #exports} replacements results in many namespaces collapsed into
     * a single namespace. In those case, {@link #imports} is not sufficient for performing the reverse
     * operation; we need {@link FilteredStreamResolver}.
     */
    final boolean manyToOne;

    /**
     * Creates a new enum for replacing only one namespace.
     *
     * @param  impl  the namespace used in JAXB annotations (should be latest schema).
     * @param  view  the namespace used in the XML file to (un)marshall (older schema).
     */
    private FilterVersion(final String impl, final String view) {
        this.exports = singletonMap(impl, view);
        this.imports = singletonMap(view, impl);
        manyToOne = false;
    }

    /**
     * Creates a new enum for replacing many namespaces by a single one.
     * This constructor is used when the legacy schema (the "view") was one large monolithic schema,
     * and the new schema (the "impl") has been separated in many smaller modules.
     *
     * @param  impl        the namespaces used in JAXB annotations (should be most recent schema).
     * @param  view        the single namespace used in the XML file to (un)marshall (older schema).
     * @param  additional  additional (<var>impl</var>, <var>view</var>) mapping for a few namespaces
     *                     having different {@code view} values.
     */
    private FilterVersion(final String[] impl, final String view, final String[] additional,
            final Map<String,String> imports)
    {
        exports = new HashMap<>(Containers.hashMapCapacity(impl.length));
        this.imports = imports;
        for (final String e : impl) {
            exports.put(e, view);
        }
        for (int i=0; i<additional.length;) {
            exports.put(additional[i++],
                       additional[i++]);
        }
        manyToOne = true;
    }

    /**
     * Creates the {@link #ALL} enumeration.
     */
    private FilterVersion(final FilterVersion first, final FilterVersion more) {
        exports = new HashMap<>(first.exports);
        imports = new HashMap<>(first.imports);
        exports.putAll(more.exports);
        imports.putAll(more.imports);
        manyToOne = first.manyToOne | more.manyToOne;
    }
}
