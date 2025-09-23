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
import java.util.Locale;
import java.util.Objects;
import javax.xml.XMLConstants;
import org.apache.sis.util.Static;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ISO_NAMESPACE;


/**
 * Lists some namespaces URLs used by JAXB when (un)marshalling.
 *
 * <p><strong>Warning: string constants in this class may change in any SIS version.</strong>
 * Those constants are made available for applications who wish to use the same URLs as SIS
 * in their own JAXB annotations. Note that applications using those constants will have their
 * URLs determined by the SIS version available at compile-time, not runtime, because the
 * {@code javac} compiler inlines string constants.</p>
 *
 * <p>The following table lists some URLs, their usual prefix, and the SIS versions when each URL changed.</p>
 * <table class="sis">
 *   <caption>Namespaces and change log</caption>
 *   <tr><th>Prefix</th> <th>Meaning</th>                                    <th>XML Namespace</th>   <th>Changes history</th></tr>
 *   <tr><td>lan</td>    <td>Language localization</td>                      <td>{@value #LAN}</td>   <td></td></tr>
 *   <tr><td>gco</td>    <td>Geographic COmmon</td>                          <td>{@value #GCO}</td>   <td>SIS 1.0</td></tr>
 *   <tr><td>mcc</td>    <td>Metadata Common Classes</td>                    <td>{@value #MCC}</td>   <td></td></tr>
 *   <tr><td>fcc</td>    <td>Feature Catalog Common</td>                     <td>{@value #FCC}</td>   <td></td></tr>
 *   <tr><td>gfc</td>    <td>General Feature Catalog</td>                    <td>{@value #GFC}</td>   <td>SIS 1.0</td></tr>
 *   <tr><td>cat</td>    <td>CATalogue</td>                                  <td>{@value #CAT}</td>   <td></td></tr>
 *   <tr><td>mdb</td>    <td>Metadata Base</td>                              <td>{@value #MDB}</td>   <td></td></tr>
 *   <tr><td>cit</td>    <td>Citation and responsible party information</td> <td>{@value #CIT}</td>   <td></td></tr>
 *   <tr><td>gex</td>    <td>Geospatial EXtent</td>                          <td>{@value #GEX}</td>   <td></td></tr>
 *   <tr><td>rce</td>    <td>Referencing By Coordinates Common</td>          <td>{@value #RCE}</td>   <td></td></tr>
 *   <tr><td>mrs</td>    <td>Metadata for Reference System</td>              <td>{@value #MRS}</td>   <td></td></tr>
 *   <tr><td>msr</td>    <td>Metadata for Spatial Representation</td>        <td>{@value #MSR}</td>   <td></td></tr>
 *   <tr><td>mrc</td>    <td>Metadata for Resource Content</td>              <td>{@value #MRC}</td>   <td></td></tr>
 *   <tr><td>mri</td>    <td>Metadata for Resource Identification</td>       <td>{@value #MRI}</td>   <td></td></tr>
 *   <tr><td>mrd</td>    <td>Metadata for Resource Distribution</td>         <td>{@value #MRD}</td>   <td></td></tr>
 *   <tr><td>mdt</td>    <td>Metadata for Data Transfer</td>                 <td>{@value #MDT}</td>   <td></td></tr>
 *   <tr><td>mco</td>    <td>Metadata for Constraints</td>                   <td>{@value #MCO}</td>   <td></td></tr>
 *   <tr><td>mac</td>    <td>Metadata for Acquisition</td>                   <td>{@value #MAC}</td>   <td></td></tr>
 *   <tr><td>mrl</td>    <td>Metadata for Resource Lineage</td>              <td>{@value #MRL}</td>   <td></td></tr>
 *   <tr><td>mmi</td>    <td>Metadata for Maintenance Information</td>       <td>{@value #MMI}</td>   <td></td></tr>
 *   <tr><td>dqc</td>    <td>Data Quality Common Classes</td>                <td>{@value #DQC}</td>   <td></td></tr>
 *   <tr><td>mdq</td>    <td>Metadata for Data Quality</td>                  <td>{@value #MDQ}</td>   <td></td></tr>
 *   <tr><td>dqm</td>    <td>Data Quality Measures</td>                      <td>{@value #DQM}</td>   <td></td></tr>
 *   <tr><td>mds</td>    <td>Metadata for Data and Services</td>             <td>{@value #MDS}</td>   <td></td></tr>
 *   <tr><td>srv</td>    <td>Metadata for Services</td>                      <td>{@value #SRV}</td>   <td>SIS 1.0</td></tr>
 *   <tr><td>mpc</td>    <td>Metadata for Portrayal Catalog</td>             <td>{@value #MPC}</td>   <td></td></tr>
 *   <tr><td>mda</td>    <td>MetaData Application</td>                       <td>{@value #MDA}</td>   <td></td></tr>
 *   <tr><td>mas</td>    <td>Metadata for Application Schema</td>            <td>{@value #MAS}</td>   <td></td></tr>
 *   <tr><td>mex</td>    <td>Metadata with Schema Extensions</td>            <td>{@value #MEX}</td>   <td></td></tr>
 *   <tr><td>gcx</td>    <td>Geospatial Common eXtension</td>                <td>{@value #GCX}</td>   <td></td></tr>
 *   <tr><td>gmw</td>    <td>Geographic Markup Wrappers</td>                 <td>{@value #GMW}</td>   <td></td></tr>
 *   <tr><td>gml</td>    <td>Geographic Markup Language</td>                 <td>{@value #GML}</td>   <td>SIS 0.4</td></tr>
 *   <tr><td>csw</td>    <td>Catalog Service for the Web</td>                <td>{@value #CSW}</td>   <td></td></tr>
 *   <tr><td>xsi</td>    <td>XML Schema Instance information</td>            <td>{@value #XSI}</td>   <td></td></tr>
 *   <tr><td>xlink</td>  <td>Link</td>                                       <td>{@value #XLINK}</td> <td></td></tr>
 * </table>
 *
 * <h2>Profiles</h2>
 * Some countries or organizations define profiles of international standards, which may contain
 * country-specific extensions. The namespace of such extensions are usually defined in a separated
 * class dedicated to the profile. Some of them are listed below:
 *
 * <ul>
 *   <li>{@value org.apache.sis.profile.france.FrenchProfile#NAMESPACE}</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Quentin Boileau (Geomatys)
 * @author  Guilhem Legal   (Geomatys)
 * @author  Cullen Rombach  (Image Matters)
 * @author  Alexis Gaillard (Geomatys)
 * @version 1.4
 * @since   0.3
 */
public final class Namespaces extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Namespaces() {
    }

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/lan/1.0/index.html">Language localization (LAN) version 1.0</a>.
     * The usual prefix for this namespace is {@code "lan"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String LAN = ISO_NAMESPACE + "19115/-3/lan/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/gco/1.0/index.html">Geographic COmmon (GCO) version 1.0</a>.
     * The usual prefix for this namespace is {@code "gco"}.
     *
     * <p>History</p>
     * <table class="sis">
     *   <caption>Change log</caption>
     *   <tr><th>SIS version</th> <th>URL</th></tr>
     *   <tr><td>0.3 to 0.8</td>  <td>http://www.isotc211.org/2005/gco</td></tr>
     *   <tr><td>Since 1.0</td>   <td>http://standards.iso.org/iso/19115/-3/gco/1.0</td></tr>
     * </table>
     *
     * @category ISO
     */
    public static final String GCO = ISO_NAMESPACE + "19115/-3/gco/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mcc/1.0/index.html">Metadata Common Classes (MCC) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mcc"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MCC = ISO_NAMESPACE + "19115/-3/mcc/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19110/fcc/1.0/index.html">Feature Catalog Common (FCC) version 1.0</a>.
     * The usual prefix for this namespace is {@code "fcc"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String FCC = ISO_NAMESPACE + "19110/fcc/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19110/gfc/1.1/index.html">General Feature Catalog (GFC) version 1.1</a>.
     * The usual prefix for this namespace is {@code "gfc"}.
     *
     * <p>History</p>
     * <table class="sis">
     *   <caption>Change log</caption>
     *   <tr><th>SIS version</th> <th>URL</th></tr>
     *   <tr><td>0.3 to 0.8</td>  <td>http://www.isotc211.org/2005/gfc</td></tr>
     *   <tr><td>Since 1.0</td>   <td>http://standards.iso.org/iso/19110/gfc/1.1</td></tr>
     * </table>
     *
     * @category ISO
     */
    public static final String GFC = ISO_NAMESPACE + "19110/gfc/1.1";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/cat/1.0/index.html">CATalogue (CAT) version 1.0</a>.
     * The usual prefix for this namespace is {@code "cat"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String CAT = ISO_NAMESPACE + "19115/-3/cat/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mdb/1.0/index.html">Metadata Base (MDB) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mdb"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MDB = ISO_NAMESPACE + "19115/-3/mdb/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/cit/1.0/index.html">Citation and responsible party information (CIT) version 1.0</a>.
     * The usual prefix for this namespace is {@code "cit"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String CIT = ISO_NAMESPACE + "19115/-3/cit/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/gex/1.0/index.html">Geospatial EXtent (GEX) version 1.0</a>.
     * The usual prefix for this namespace is {@code "gex"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String GEX = ISO_NAMESPACE + "19115/-3/gex/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mrs/1.0/index.html">Metadata for Reference System (MRS) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mrs"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MRS = ISO_NAMESPACE + "19115/-3/mrs/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19111/rce/1.0/index.html">Referencing By Coordinates Common (RCE) version 1.0</a>.
     * The usual prefix for this namespace is {@code "rce"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String RCE = ISO_NAMESPACE + "19111/rce/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/msr/1.0/index.html">Metadata for Spatial Representation (MSR) version 1.0</a>.
     * The usual prefix for this namespace is {@code "msr"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MSR = ISO_NAMESPACE + "19115/-3/msr/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mrc/1.0/index.html">Metadata for Resource Content (MRC) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mrc"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MRC = ISO_NAMESPACE + "19115/-3/mrc/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mri/1.0/index.html">Metadata for Resource Identification (MRI) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mri"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MRI = ISO_NAMESPACE + "19115/-3/mri/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mrd/1.0/index.html">Metadata for Resource Distribution (MRD) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mrd"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MRD = ISO_NAMESPACE + "19115/-3/mrd/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mdt/1.0/index.html">Metadata for Data Transfer (MDT) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mdt"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MDT = ISO_NAMESPACE + "19115/-3/mdt/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mco/1.0/index.html">Metadata for Constraints (MCO) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mco"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MCO = ISO_NAMESPACE + "19115/-3/mco/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mac/1.0/index.html">Metadata for Acquisition (MAC) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mac"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MAC = ISO_NAMESPACE + "19115/-3/mac/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mrl/1.0/index.html">Metadata for Resource Lineage (MRL) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mrl"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MRL = ISO_NAMESPACE + "19115/-3/mrl/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mmi/1.0/index.html">Metadata for Maintenance Information (MMI) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mmi"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MMI = ISO_NAMESPACE + "19115/-3/mmi/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19157/-2/dqc/1.0/index.html">Data Quality Common Classes (DQC) version 1.0</a>.
     * The usual prefix for this namespace is {@code "dqc"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String DQC = ISO_NAMESPACE + "19157/-2/dqc/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19157/-2/mdq/1.0/index.html">Metadata for Data Quality (MDQ) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mdq"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MDQ = ISO_NAMESPACE + "19157/-2/mdq/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19157/-2/dqm/1.0/index.html">Data Quality Measures (DQM) version 1.0</a>.
     * The usual prefix for this namespace is {@code "dqm"}.
     *
     * @category ISO
     * @since 1.3
     */
    public static final String DQM = ISO_NAMESPACE + "19157/-2/dqm/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mds/1.0/index.html">Metadata for Data and Services (MDS) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mds"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MDS = ISO_NAMESPACE + "19115/-3/mds/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/srv/2.0/index.html">Metadata for Services (SRV) version 2.0</a>.
     * The usual prefix for this namespace is {@code "srv"}.
     *
     * <p>History</p>
     * <table class="sis">
     *   <caption>Change log</caption>
     *   <tr><th>SIS version</th> <th>URL</th></tr>
     *   <tr><td>0.3 to 0.8</td>  <td>http://www.isotc211.org/2005/srv</td></tr>
     *   <tr><td>Since 1.0</td>   <td>http://standards.iso.org/iso/19115/-3/srv/2.0</td></tr>
     * </table>
     *
     * @category ISO
     * @since 1.0
     */
    public static final String SRV = ISO_NAMESPACE + "19115/-3/srv/2.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mpc/1.0/index.html">Metadata for Portrayal Catalog (MPC) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mpc"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MPC = ISO_NAMESPACE + "19115/-3/mpc/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mda/1.0/index.html">MetaData Application (MDA) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mda"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MDA = ISO_NAMESPACE + "19115/-3/mda/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mas/1.0/index.html">Metadata for Application Schema (MAS) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mas"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MAS = ISO_NAMESPACE + "19115/-3/mas/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/mex/1.0/index.html">Metadata with Schema Extensions (MEX) version 1.0</a>.
     * The usual prefix for this namespace is {@code "mex"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MEX = ISO_NAMESPACE + "19115/-3/mex/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/gcx/1.0/index.html">Geospatial Common eXtension (GCX) version 1.0</a>.
     * The usual prefix for this namespace is {@code "gcx"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String GCX = ISO_NAMESPACE + "19115/-3/gcx/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/md1/1.0/index.html">Metadata for Data and Services with Geospatial Common Extensions (MD1) version 1.0</a>.
     * The usual prefix for this namespace is {@code "md1"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MD1 = ISO_NAMESPACE + "19115/-3/md1/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/md2/1.0/index.html">Metadata for Data and Services with Geospatial Common Extensions (MD2) version 1.0</a>.
     * The usual prefix for this namespace is {@code "md2"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String MD2 = ISO_NAMESPACE + "19115/-3/md2/1.0";

    /**
     * The <code>{@value}</code> URL. This is the namespace of
     * <a href="https://schemas.isotc211.org/19115/-3/gmw/1.0/index.html">Geographic Markup Wrappers (GMW) version 1.0</a>.
     * The usual prefix for this namespace is {@code "gmw"}.
     *
     * @category ISO
     * @since 1.0
     */
    public static final String GMW = ISO_NAMESPACE + "19115/-3/gmw/1.0";

    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "gmd"}.
     * This is a legacy namespace, but still in wide use.
     *
     * @category ISO
     *
     * @deprecated as of ISO 19115-3, split in many different namespaces.
     */
    @Deprecated(since="1.0")
    public static final String GMD = LegacyNamespaces.GMD;

    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "gml"}.
     * The 3.2 version is equivalent to ISO 19136.
     *
     * <p>History</p>
     * <table class="sis">
     *   <caption>Change log</caption>
     *   <tr><th>SIS version</th> <th>URL</th></tr>
     *   <tr><td>0.3</td>         <td>http://www.opengis.net/gml</td></tr>
     *   <tr><td>Since 0.4</td>   <td>http://www.opengis.net/gml/3.2</td></tr>
     * </table>
     *
     * @category OGC
     */
    public static final String GML = "http://www.opengis.net/gml/3.2";

    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "csw"}.
     *
     * <p>History</p>
     * <table class="sis">
     *   <caption>Change log</caption>
     *   <tr><th>SIS version</th> <th>URL</th></tr>
     *   <tr><td>0.3</td>         <td>http://www.opengis.net/cat/csw/2.0.2</td></tr>
     *   <tr><td>Since 1.0</td>   <td>http://www.opengis.net/cat/csw/3.0</td></tr>
     * </table>
     *
     * @category OGC
     */
    public static final String CSW = "http://www.opengis.net/cat/csw/3.0";

    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "se"}.
     *
     * <p>History</p>
     * <table class="sis">
     *   <caption>Change log</caption>
     *   <tr><th>SIS version</th> <th>URL</th></tr>
     *   <tr><td>1.4</td>         <td>http://www.opengis.net/se</td></tr>
     * </table>
     *
     * @category OGC
     * @since 1.4
     */
    public static final String SE = "http://www.opengis.net/se";

    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "xsi"}.
     * This is also defined by {@link XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI}.
     *
     * @category W3C
     * @see XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI
     */
    public static final String XSI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

    /**
     * The <code>{@value}</code> URL.
     * The usual prefix for this namespace is {@code "xlink"}.
     *
     * @category W3C
     */
    public static final String XLINK = "http://www.w3.org/1999/xlink";

    /**
     * URLs for which the XML prefix to use directly follows an URL starts given in this array.
     */
    private static final String[] GENERIC_URLS = {
        ISO_NAMESPACE + "19115/-3/",
        ISO_NAMESPACE + "19115/-2/",
        ISO_NAMESPACE + "19157/-2/",
        ISO_NAMESPACE + "19111/",
        ISO_NAMESPACE + "19110/",
        "http://www.isotc211.org/2005/",
        "http://www.opengis.net/",
        "http://www.w3.org/1999/",
        "http://www.cnig.gouv.fr/2005/"
    };

    /**
     * A map of (<var>URLs</var>, <var>prefix</var>). Stores URLs for which
     * the prefix to use cannot be easily inferred from the URL itself.
     */
    private static final Map<String,String> SPECIFIC_URLS = Map.ofEntries(
            Map.entry(XMLConstants.W3C_XML_SCHEMA_NS_URI,                          "xs"),
            Map.entry(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,                "xsi"),
            Map.entry("http://www.w3.org/2004/02/skos/core#",                    "skos"),
            Map.entry("http://www.w3.org/1999/02/22-rdf-syntax-ns#",              "rdf"),
            Map.entry("http://www.w3.org/1998/Math/MathML",                       "mml"),
            Map.entry("http://www.opengis.net/sensorML/1.0",                     "sml1"),
            Map.entry("http://www.opengis.net/sensorML/1.0.1",                    "sml"),
            Map.entry("http://www.opengis.net/swe/1.0",                          "swe1"),
            Map.entry("http://www.opengis.net/cat/csw/3.0",                       "csw"),
            Map.entry("http://www.opengis.net/cat/csw/2.0.2",                    "csw2"),
            Map.entry("http://www.opengis.net/ows/2.0",                           "ows"),
            Map.entry("http://www.opengis.net/cat/wrs/1.0",                       "wrs"),
            Map.entry("http://www.opengis.net/cat/wrs",                         "wrs09"),
            Map.entry("http://www.opengis.net/ows-6/utds/0.3",                   "utds"),
            Map.entry("http://www.opengis.net/citygml/1.0",                      "core"),
            Map.entry("http://www.opengis.net/citygml/building/1.0",            "build"),
            Map.entry("http://www.opengis.net/citygml/cityfurniture/1.0",   "furniture"),
            Map.entry("http://www.opengis.net/citygml/transportation/1.0",         "tr"),
            Map.entry("http://www.isotc211.org/2005/gco",                        "gcol"),   // "l" for "legacy" (prior version 1).
            Map.entry("http://www.isotc211.org/2005/srv",                        "srv1"),
            Map.entry("http://www.purl.org/dc/elements/1.1/",                     "dc2"),
            Map.entry("http://www.purl.org/dc/terms/",                           "dct2"),
            Map.entry("http://purl.org/dc/terms/",                                "dct"),
            Map.entry("http://www.inspire.org",                                   "ins"),
            Map.entry("http://inspira.europa.eu/networkservice/view/1.0",  "inspire_vs"),
            Map.entry("urn:oasis:names:tc:ciq:xsdschema:xAL:2.0",                 "xal"),
            Map.entry("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0",              "rim"),
            Map.entry("urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.5",            "rim25"),
            Map.entry("urn:oasis:names:tc:xacml:2.0:context:schema:os", "xacml-context"),
            Map.entry("urn:oasis:names:tc:xacml:2.0:policy:schema:os",   "xacml-policy"),
            Map.entry("urn:us:gov:ic:ism:v2",                                   "icism"));

    /**
     * Returns the preferred prefix for the given namespace URI.
     *
     * @param  namespace     the namespace URI for which the prefix needs to be found. Cannot be {@code null}.
     * @param  defaultValue  the default prefix to return if the given {@code namespace} is not recognized,
     *                       or {@code null}.
     * @return the prefix inferred from the namespace URI, or {@code null} if the given namespace is unrecognized
     *         and the {@code defaultValue} is null.
     */
    public static String getPreferredPrefix(String namespace, final String defaultValue) {
        String prefix = SPECIFIC_URLS.get(Objects.requireNonNull(namespace));
        if (prefix != null) {
            return prefix;
        }
        namespace = namespace.toLowerCase(Locale.ROOT);
        for (final String baseURL : GENERIC_URLS) {
            if (namespace.startsWith(baseURL)) {
                final int startAt = baseURL.length();
                final int endAt = namespace.indexOf('/', startAt);
                if (endAt >= 0) {
                    prefix = namespace.substring(startAt, endAt);
                } else {
                    prefix = namespace.substring(startAt);
                }
                return prefix;
            }
        }
        return defaultValue;
    }

    /**
     * Guesses the namespace for a type of the given ISO name. The argument given to this method
     * must be a class name defined by ISO 19115 or other standards to be added in the future.
     * Those ISO class names usually start with a two letter prefix, e.g. {@code "CI"}
     * in {@link org.apache.sis.metadata.iso.citation.DefaultCitation CI_Citation}.
     *
     * <p>This method uses heuristic rules, first looking at the prefix, then the rest of the name in case of ambiguity.
     * A namespace is returned on a <em>best effort</em> basis only; this method may or may not check the full name, and
     * values returned by this method may change in future SIS versions (e.g. when new standards become supported by SIS
     * or when existing standards are upgraded). This method should be used in last resort only, when this information
     * cannot be obtained easily in a more reliable way.</p>
     *
     * @param  type  a class name defined by ISO 19115 or related standards (e.g. {@code "CI_Citation"}.
     * @return a <em>possible</em> namespace for the given type, or {@code null} if unknown.
     *
     * @since 1.0
     */
    public static String guessForType(final String type) {
        /*
         * Implementation note: we could invoke TransformingReader.namespace(type) unconditionally,
         * but that method may be removed in a future SIS version if we replace TransformingReader
         * by XSD (https://issues.apache.org/jira/projects/SIS/issues/SIS-381). By using a switch now,
         * we reduce the behavioral change is SIS-381 is applied. It can also reduce classes loading.
         */
        if (type != null && type.length() >= 3) {
            if (type.charAt(2) == '_') {
                switch ((type.charAt(0) << Character.SIZE) | type.charAt(1)) {
                    case ('C' << Character.SIZE) | 'I': return CIT;
                    case ('E' << Character.SIZE) | 'X': return GEX;
                    case ('F' << Character.SIZE) | 'C': return GFC;
                    case ('L' << Character.SIZE) | 'E':
                    case ('L' << Character.SIZE) | 'I': return MRL;
                    case ('D' << Character.SIZE) | 'S': // Usually MDA except for DS_InitiativeTypeCode
                    case ('M' << Character.SIZE) | 'D':
                    case ('M' << Character.SIZE) | 'I': return TransformingReader.namespace(type);
                    case ('M' << Character.SIZE) | 'X': return MDT;
                    case ('P' << Character.SIZE) | 'T': return LAN;
                    case ('S' << Character.SIZE) | 'V': return SRV;
                    case ('C' << Character.SIZE) | 'S':
                    case ('C' << Character.SIZE) | 'D':
                    case ('S' << Character.SIZE) | 'C': return GML;
                }
            } else {
                // Needs to handle at least DCPList
                return TransformingReader.namespace(type);
            }
        }
        return null;
    }
}
