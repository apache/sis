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
 * @since   0.4
 * @version 0.7
 * @module
 */
public final class Schemas {
    /**
     * The XSD definition for Geographic Markup Language (GML) objects.
     */
    public static final String GML_XSD = "http://schemas.opengis.net/gml/3.2.1/gml.xsd";

    /**
     * The XSD definition for metadata objects.
     */
    public static final String METADATA_XSD = "http://schemas.opengis.net/iso/19139/20070417/gmd/gmd.xsd";

    /**
     * The root directory of OGC metadata schemas.
     * This is the schema used by default in Apache SIS.
     */
    public static final String METADATA_ROOT = "http://schemas.opengis.net/iso/19139/20070417/";

    /**
     * The root directory of ISO metadata schemas.
     * This is sometime used as an alternative to {@link #METADATA_ROOT}.
     */
    public static final String ISO_19139_ROOT = "http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/";

    /**
     * The string to append to {@link #METADATA_ROOT} or {@link #ISO_19139_ROOT} for obtaining the path
     * to the definitions of code lists.
     *
     * <p>A localized version of this file exists also with the {@code "ML_gmxCodelists.xml"} filename
     * instead of {@code "gmxCodelists.xml"}</p>
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-154">SIS-154</a>
     */
    public static final String CODELISTS_PATH = "resources/Codelist/gmxCodelists.xml";

    /**
     * The string to append to {@link #METADATA_ROOT} or {@link #ISO_19139_ROOT} for obtaining the path
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
