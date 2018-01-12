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


/**
 * Constants for URL to schema directories or definition files.
 * Constants in this class are organized in three groups:
 *
 * <ul>
 *   <li>Constants with the {@code _ROOT} suffix are {@code "http://"} URL to a root directory.</li>
 *   <li>Constants with the {@code _PATH} suffix are relative paths to concatenate to a {@code _ROOT}
 *       constant in order to get the full path to a file.</li>
 *   <li>Constants with the {@code _XSD} suffix are {@code "http://"} URL to a the XSD definition file.</li>
 * </ul>
 *
 * <div class="section">Note on multi-lingual files</div>
 * Some files are available in two variants: with and without {@code "ML_"} prefix, which stands for "Multi Lingual".
 * Some examples are {@code "[ML_]gmxCodelists.xml"} and {@code "[ML_]gmxUom.xml"}. The following assumptions hold:
 *
 * <ul>
 *   <li>All code lists defined in a {@code ML_foo.xml} file exist also in {@code foo.xml}.</li>
 *   <li>The converse of above point is not necessarily true:
 *       the {@code ML_foo.xml} file may contain only a subset of {@code foo.xml}.</li>
 *   <li>All English descriptions in {@code ML_foo.xml} file are strictly identical to the ones in {@code foo.xml}.</li>
 *   <li>Descriptions in other languages than English exist only in {@code ML_foo.xml}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.4
 * @module
 */
public final class Schemas {
    /**
     * The XSD definition for Geographic Markup Language (GML) objects.
     */
    public static final String GML_XSD = "http://schemas.opengis.net/gml/3.2.1/gml.xsd";

    /**
     * The XSD definition for the root of ISO 19115-3 metadata objects.
     * Most metadata XSD paths start with {@value #METADATA_ROOT}, but not all.
     */
    public static final String METADATA_XSD_BASE           = "http://standards.iso.org/iso/19115/-3/mdb/1.0/mdb.xsd",
                               METADATA_XSD_SPATIAL        = "http://standards.iso.org/iso/19115/-3/msr/1.0/msr.xsd",
                               METADATA_XSD_QUALITY        = "http://standards.iso.org/iso/19157/-2/dqc/1.0/dqc.xsd",
                               METADATA_XSD_MAINTENANCE    = "http://standards.iso.org/iso/19115/-3/mmi/1.0/mmi.xsd",
                               METADATA_XSD_LINEAGE        = "http://standards.iso.org/iso/19115/-3/mrl/1.0/mrl.xsd",
                               METADATA_XSD_IDENTIFICATION = "http://standards.iso.org/iso/19115/-3/mri/1.0/mri.xsd",
                               METADATA_XSD_EXTENT         = "http://standards.iso.org/iso/19115/-3/gex/1.0/gex.xsd",
                               METADATA_XSD_DISTRIBUTION   = "http://standards.iso.org/iso/19115/-3/mrd/1.0/mrd.xsd",
                               METADATA_XSD_CONTENT        = "http://standards.iso.org/iso/19115/-3/mrc/1.0/mrc.xsd",
                               METADATA_XSD_CONSTRAINT     = "http://standards.iso.org/iso/19115/-3/mrc/1.0/mrc.xsd",
                               METADATA_XSD_CITATION       = "http://standards.iso.org/iso/19115/-3/cit/1.0/cit.xsd",
                               METADATA_XSD_ACQUISITION    = "http://standards.iso.org/iso/19115/-3/mac/1.0/mac.xsd";

    /**
     * The root directory of ISO 19115 metadata schemas.
     * This is the schema used by default in Apache SIS.
     */
    public static final String METADATA_ROOT = "http://standards.iso.org/iso/19115/";

    /**
     * The root directory of OGC metadata schemas.
     * This is the schema used by default in Apache SIS.
     * Some alternatives to this URL are:
     *
     * <ul>
     *   <li>http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/</li>
     *   <li>http://www.isotc211.org/2005/</li>
     * </ul>
     */
    public static final String METADATA_ROOT_LEGACY = "http://schemas.opengis.net/iso/19139/20070417/";

    /**
     * The string to append to {@link #METADATA_ROOT} for obtaining the path to the definitions of code lists.
     */
    public static final String CODELISTS_PATH = "resources/Codelist/cat/codelists.xml";

    /**
     * The string to append to {@link #METADATA_ROOT_LEGACY} or one of its alternative for obtaining the path
     * to the definitions of code lists.
     *
     * <p>A localized version of this file exists also with the {@code "ML_gmxCodelists.xml"} filename
     * instead of {@code "gmxCodelists.xml"}</p>
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-154">SIS-154</a>
     */
    public static final String CODELISTS_PATH_LEGACY = "resources/Codelist/gmxCodelists.xml";

    /**
     * The string to append to {@link #METADATA_ROOT} or one of its alternative for obtaining the path
     * to the definitions of units of measurement.
     *
     * <p>A localized version of this file exists also with the {@code "ML_gmxUom.xml"} filename
     * instead of {@code "gmxUom.xml"}</p>
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-154">SIS-154</a>
     */
    public static final String UOM_PATH = "resources/uom/gmxUom.xml";

    /**
     * Do not allow instantiation of this class.
     */
    private Schemas() {
    }
}
