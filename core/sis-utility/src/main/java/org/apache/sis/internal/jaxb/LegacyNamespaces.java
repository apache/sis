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
 * @author  Guilhem Legal  (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 *
 * @see org.apache.sis.xml.Namespaces
 *
 * @since 0.4
 * @module
 */
public final class LegacyNamespaces {
    /**
     * @deprecated to be replaced by {@code VERSION_1_0}, which is the version declared in XML schemas.
     */
    @Deprecated
    public static final Version ISO_19115_3 = new Version("2014"),
                                ISO_19139   = new Version("2003");

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
     * The <code>{@value}</code> URL, used in ISO 19139:2007.
     * The usual prefix for this namespace is {@code "gmd"}.
     */
    public static final String GMD = "http://www.isotc211.org/2005/gmd";

    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "gmi"}.
     */
    public static final String GMI = "http://www.isotc211.org/2005/gmi";

    /**
     * The <code>{@value}</code> URL, used in ISO 19139:2007.
     * The usual prefix for this namespace is {@code "gmx"}.
     */
    public static final String GMX = "http://www.isotc211.org/2005/gmx";

    /**
     * The <code>{@value}</code> URL, used in ISO 19139:2007.
     * The usual prefix for this namespace is {@code "gco"}.
     * Replaced by {@link org.apache.sis.xml.Namespaces#GCO}.
     */
    public static final String GCO = "http://www.isotc211.org/2005/gco";

    /**
     * The <code>{@value}</code> URL, used in ISO 19139:2007.
     * The usual prefix for this namespace is {@code "srv"}.
     * Replaced by {@link org.apache.sis.xml.Namespaces#SRV}.
     */
    public static final String SRV = "http://www.isotc211.org/2005/srv";

    /**
     * The <code>{@value}</code> URL, used in ISO 19110.
     * The usual prefix for this namespace is {@code "gfc"}.
     * Replaced by {@link org.apache.sis.xml.Namespaces#GFC}.
     */
    public static final String GFC = "http://www.isotc211.org/2005/gfc";

    /**
     * The <code>{@value}</code> URL, used in ISO 19139:2007.
     * The usual prefix for this namespace is {@code "gts"}.
     */
    public static final String GTS = "http://www.isotc211.org/2005/gts";

    /**
     * Do not allow instantiation of this class.
     */
    private LegacyNamespaces() {
    }
}
