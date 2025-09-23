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

/**
 * Root package for ISO 19115 metadata about resources (data or services).
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <h2>Overview</h2>
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 * For some explanation about how to use various ISO 19115 elements for scientific dataset, the
 * <a href="https://geo-ide.noaa.gov/wiki/index.php?title=Category:ISO_19115">NOAA wiki page</a>
 * is a good source of information.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO 19115 metadata}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultMetadata                     Metadata}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultMetadataScope                Metadata scope}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultPortrayalCatalogueReference  Portrayal catalogue reference}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultApplicationSchemaInformation Application schema information}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultMetadataExtensionInformation Metadata extension information}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultExtendedElementInformation   Extended element information}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.DefaultIdentifier                   Identifier}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.Datatype     Data type}<br>
 * {@code  └─} {@linkplain org.opengis.annotation.Obligation Obligation}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *                     {@linkplain org.apache.sis.metadata.iso.DefaultMetadata                     Metadata}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.DefaultMetadataScope                Metadata scope}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.DefaultPortrayalCatalogueReference  Portrayal catalogue reference}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.DefaultApplicationSchemaInformation Application schema information}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.DefaultMetadataExtensionInformation Metadata extension information}<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.DefaultExtendedElementInformation   Extended element information}<br>
 * {@code  │       ├─} {@linkplain org.opengis.metadata.Datatype                                   Data type} «code list»<br>
 * {@code  │       └─} {@linkplain org.opengis.annotation.Obligation                               Obligation} «code list»<br>
 * {@code  └─}         {@linkplain org.apache.sis.metadata.iso.DefaultIdentifier                   Identifier}<br>
 * </td></tr></table>
 *
 * <h2>Localization</h2>
 * When a metadata object is marshalled as an ISO 19115-3 compliant XML document, the marshaller
 * {@link org.apache.sis.xml.XML#LOCALE} property will be used for the localization of every
 * {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList} instances,
 * <strong>except</strong> if the object to be marshalled is an instance of
 * {@link org.apache.sis.metadata.iso.DefaultMetadata}, in which case the value given to the
 * {@link org.apache.sis.metadata.iso.DefaultMetadata#setLanguage setLanguage(Locale)} method
 * will have precedence. The latter behavior is compliant with INSPIRE rules.
 *
 * <h2>Null values, nil objects and collections</h2>
 * All constructors and setter methods accept {@code null} arguments.
 * A null argument value means that the metadata element cannot be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if the type is neither a collection or an array.
 * Note that non-null values may be {@link org.apache.sis.xml.NilObject}s.</p>
 *
 * <p>Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <em>live</em>: adding new elements in the collection
 * modify directly the underlying metadata object.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.5
 * @since   0.3
 */
@XmlSchema(location="https://schemas.isotc211.org/19115/-3/mdb/1.0/mdb.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.MDB,
           xmlns = {
                @XmlNs(prefix = "mdb", namespaceURI = Namespaces.MDB),      // Metadata Base
                @XmlNs(prefix = "mpc", namespaceURI = Namespaces.MPC),      // Metadata for Portrayal Catalog
                @XmlNs(prefix = "mas", namespaceURI = Namespaces.MAS),      // Metadata for Application Schema
                @XmlNs(prefix = "mex", namespaceURI = Namespaces.MEX),      // Metadata with Schema Extensions
                @XmlNs(prefix = "lan", namespaceURI = Namespaces.LAN),      // Language localization
                @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC),      // Metadata Common Classes
           //   @XmlNs(prefix = "dqc", namespaceURI = Namespaces.DQC),      // Data Quality Common Classes
                @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_Date.class),
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    @XmlJavaTypeAdapter(DQ_DataQuality.class),
    @XmlJavaTypeAdapter(GO_DateTime.class),
    @XmlJavaTypeAdapter(GO_Integer.class),
    @XmlJavaTypeAdapter(LI_Lineage.class),
    @XmlJavaTypeAdapter(MD_ApplicationSchemaInformation.class),
    @XmlJavaTypeAdapter(MD_CharacterSetCode.class),
    @XmlJavaTypeAdapter(MD_Constraints.class),
    @XmlJavaTypeAdapter(MD_ContentInformation.class),
    @XmlJavaTypeAdapter(MD_DatatypeCode.class),
    @XmlJavaTypeAdapter(MD_Distribution.class),
    @XmlJavaTypeAdapter(MD_ExtendedElementInformation.class),
    @XmlJavaTypeAdapter(MD_Identification.class),
    @XmlJavaTypeAdapter(MD_MaintenanceInformation.class),
    @XmlJavaTypeAdapter(MD_MetadataExtensionInformation.class),
    @XmlJavaTypeAdapter(MD_MetadataScope.class),
    @XmlJavaTypeAdapter(MD_ObligationCode.class),
    @XmlJavaTypeAdapter(MD_PortrayalCatalogueReference.class),
    @XmlJavaTypeAdapter(MD_ScopeCode.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentation.class),
    @XmlJavaTypeAdapter(MI_AcquisitionInformation.class),
    @XmlJavaTypeAdapter(RS_ReferenceSystem.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(LocaleAdapter.class)
})
package org.apache.sis.metadata.iso;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.lan.LocaleAdapter;
import org.apache.sis.xml.bind.gco.*;
import org.apache.sis.xml.bind.metadata.*;
import org.apache.sis.xml.bind.metadata.code.*;
