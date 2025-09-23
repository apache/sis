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
package org.apache.sis.xml.internal.shared;

import org.apache.sis.util.Version;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ISO_NAMESPACE;


/**
 * Legacy XML namespaces, and {@link Version} constants for identifying when those namespaces were used.
 *
 * @author  Guilhem Legal  (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 *
 * @see org.apache.sis.xml.Namespaces
 */
public final class LegacyNamespaces {
    /**
     * Miscellaneous version constants used for ISO standards.
     */
    public static final Version VERSION_2007 = new Version("2007"),
                                VERSION_2014 = new Version("2014"),
                                VERSION_2016 = new Version("2016");

    /**
     * Miscellaneous version constants used for GML versions.
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
     * The <code>{@value}</code> URL as an alias for {@link #GMI}.
     * Was used in some XML files before a more official URL was set.
     */
    public static final String GMI_ALIAS = "http://www.isotc211.org/2005/gmi";

    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "gmi"}.
     */
    public static final String GMI = ISO_NAMESPACE + "19115/-2/gmi/1.0";

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
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "csw"}.
     */
    public static final String CSW = "http://www.opengis.net/cat/csw/2.0.2";

    /**
     * Do not allow instantiation of this class.
     */
    private LegacyNamespaces() {
    }
}
