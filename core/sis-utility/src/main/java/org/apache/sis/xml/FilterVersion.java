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
import org.apache.sis.internal.jaxb.LegacyNamespaces;

import static java.util.Collections.singletonMap;


/**
 * The target version of standards for {@link FilteredNamespaces}.
 *
 * See {@link FilteredNamespaces} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
enum FilterVersion {
    /**
     * GML using the legacy {@code "http://www.opengis.net/gml"} namespace.
     */
    GML31(Namespaces.GML, LegacyNamespaces.GML);

    /**
     * Apply all known namespace replacements. This can be used only at unmarshalling time,
     * for replacing all namespaces by the namespaces declared in Apache SIS JAXB annotations.
     */
    static FilterVersion ALL = GML31;

    /**
     * The URI replacements to apply when going from the "real" data producer (JAXB marshaller)
     * to the filtered reader/writer. Keys are the actual URIs as declared in SIS implementation,
     * and values are the URIs read or to write instead of the actual ones.
     *
     * @see FilteredNamespaces#toView
     */
    final Map<String,String> toView;

    /**
     * The URI replacements to apply when going from the filtered reader/writer to the "real"
     * data consumer (JAXB unmarshaller). This map is the converse of {@link #toView}.
     *
     * @see FilteredNamespaces#toImpl
     */
    final Map<String,String> toImpl;

    /**
     * Creates a new enum for replacing only one namespace.
     */
    private FilterVersion(final String impl, final String view) {
        this.toView = singletonMap(impl, view);
        this.toImpl = singletonMap(view, impl);
    }
}
