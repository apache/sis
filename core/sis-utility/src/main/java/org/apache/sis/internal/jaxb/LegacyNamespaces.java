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
     * First GML version of the new {@code xmlns}.
     * GML 3.2.0 schemas are defined in the namespace {@code http://www.opengis.net/gml} whereas
     * GML 3.2.1 schemas are defined in the namespace {@code http://www.opengis.net/gml/3.2}.
     */
    public static final Version VERSION_3_2_1 = new Version("3.2.1");

    /**
     * The {@value} URL, which was used for all GML versions before 3.2.
     * This URL should not be used in JAXB annotations, even if the annotated element is really for that
     * legacy GML version. Instead, namespace replacements are applied on-the-fly at marshalling time.
     */
    public static final String GML = "http://www.opengis.net/gml";

    /**
     * A non-public (un)marshaller property for controlling usage of {@code org.apache.sis.xml.FilteredNamespaces}.
     * Values can be:
     *
     * <ul>
     *   <li>{@link Boolean#FALSE} for disabling namespace replacements. XML (un)marshalling will use the namespaces URI
     *       supported natively by SIS as declared in JAXB annotations. This is sometime useful for debugging purpose.</li>
     *   <li>{@link Boolean#TRUE} for forcing namespace replacements at unmarshalling time. This is useful for reading a
     *       XML document of unknown GML version.</li>
     *   <li>{@code null} or missing for the default behavior, which is apply namespace replacements only if the
     *       {@link org.apache.sis.xml.XML#GML_VERSION} property is set to an older value than the one supported
     *       natively by SIS.</li>
     * </ul>
     *
     * This property can be given to {@link org.apache.sis.xml.MarshallerPool} constructor, or set directly on
     * a {@code Marshaller} or {@code Unmarshaller} instance created by {@code MarshallerPool} by invoking its
     * {@code setProperty(String, Object)} method.
     */
    public static final String APPLY_NAMESPACE_REPLACEMENTS = "org.apache.sis.xml.applyNamespaceReplacements";

    /**
     * Do not allow instantiation of this class.
     */
    private LegacyNamespaces() {
    }
}
