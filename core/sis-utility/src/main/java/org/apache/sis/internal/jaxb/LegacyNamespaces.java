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
package org.apache.sis.internal.jaxb;

import org.apache.sis.util.Version;


/**
 * Legacy XML namespaces, and {@link Version} constants for identifying when those namespaces were used.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.xml.Namespaces
 */
public final class LegacyNamespaces {
    /**
     * Miscellaneous version constants.
     */
    public static final Version VERSION_3_0 = new Version("3.0"),
                                VERSION_3_2 = new Version("3.2");

    /**
     * The {@value} URL, which was used for all GML versions before 3.2.
     * This URL should not be used in JAXB annotations, even if the annotated element is really for that
     * legacy GML version. Instead, namespace replacements are applied on-the-fly at marshalling time.
     */
    public static final String GML = "http://www.opengis.net/gml";

    /**
     * A copy of {@link #GML} used only in JAXB annotations.
     * We use a separated constant in order to make easier to remove every uses of this namespace in
     * all JAXB annotations if we can find a way to share the same Java classes between different versions.
     * If such better way is found, then every classes, methods and fields using this constant should be deleted.
     */
    @Deprecated
    public static final String GML_IN_JAXB = GML;

    /**
     * A non-public (un)marshaller property for disabling usage of {@link org.apache.sis.xml.FilteredNamespaces}.
     */
    public static final String DISABLE_NAMESPACE_REPLACEMENTS = "org.apache.sis.xml.disableNamespaceReplacements";

    /**
     * Do not allow instantiation of this class.
     */
    private LegacyNamespaces() {
    }
}
