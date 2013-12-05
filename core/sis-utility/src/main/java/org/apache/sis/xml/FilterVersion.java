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
     * The URIs to replace. Keys are the old URIs, and values are the new URIs to use instead of the old one.
     * This map must be immutable.
     */
    final Map<String,String> replacements;

    /**
     * The converse of {@link #replacements}. Keys are the new URIs, and values are the old URIs which are
     * replaced by the new ones. This map is inferred from {@link #replacements} and must be immutable.
     */
    final Map<String,String> toDelegate;

    /**
     * Creates a new enum for replacing only one namespace.
     */
    private FilterVersion(final String from, final String to) {
        this.replacements = singletonMap(from, to);
        this.toDelegate   = singletonMap(to, from);
    }
}
