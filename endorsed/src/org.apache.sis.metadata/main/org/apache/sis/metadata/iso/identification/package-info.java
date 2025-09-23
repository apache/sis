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
 * Information to uniquely identify the data or service.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.identification OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <h2>Overview</h2>
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO 19115 metadata}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.AbstractIdentification        Identification} «abstract»<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.identification.DefaultDataIdentification     Data identification}<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.identification.DefaultServiceIdentification  Service identification}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultResolution             Resolution}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic          Browse graphic}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultKeywords               Keywords}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultUsage                  Usage}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultAggregateInformation   Aggregate information}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultCoupledResource        Coupled resource}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultOperationMetadata      Operation metadata}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultOperationChainMetadata Operation chain metadata}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.Progress        Progress}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.KeywordType     Keyword type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.AssociationType Association type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.InitiativeType  Initiative type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.TopicCategory   Topic category}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.CouplingType    Coupling type}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.identification.DistributedComputingPlatform Distributed computing platform}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *                 {@linkplain org.apache.sis.metadata.iso.identification.AbstractIdentification        Identification} «abstract»<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultResolution             Resolution}<br>
 * {@code  ├─}     {@linkplain org.opengis.metadata.identification.TopicCategory                        Topic category} «code list»<br>
 * {@code  ├─}     {@linkplain org.opengis.metadata.identification.Progress                             Progress} «code list»<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic          Browse graphic}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultKeywords               Keywords}<br>
 * {@code  │   └─} {@linkplain org.opengis.metadata.identification.KeywordType                          Keyword type} «code list»<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultUsage                  Usage}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultAssociatedResource     Associated resource}<br>
 * {@code      ├─} {@linkplain org.opengis.metadata.identification.AssociationType                      Association type} «code list»<br>
 * {@code      └─} {@linkplain org.opengis.metadata.identification.InitiativeType                       Initiative type} «code list»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.identification.DefaultDataIdentification     Data identification}<br>
 *                 {@linkplain org.apache.sis.metadata.iso.identification.DefaultServiceIdentification  Service identification}<br>
 * {@code  ├─}     {@linkplain org.opengis.metadata.identification.CouplingType                         Coupling type} «code list»<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultCoupledResource        Coupled resource}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultOperationMetadata      Operation metadata}<br>
 * {@code  │   ├─} {@linkplain org.opengis.metadata.identification.DistributedComputingPlatform         Distributed computing platform} «code list»<br>
 * {@code  │   └─} {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor                      Parameter descriptor}<br>
 * {@code  │       └─} {@linkplain org.opengis.parameter.ParameterDirection                             Parameter direction} «enum»<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultOperationChainMetadata Operation chain metadata}<br>
 * </td></tr></table>
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction#setScale(double)}
 *       for computing the denominator from a scale value.</li>
 * </ul>
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
@XmlSchema(location="https://schemas.isotc211.org/19115/-3/mri/1.0/mri.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.MRI,
           xmlns = {
                @XmlNs(prefix = "mri",  namespaceURI = Namespaces.MRI),        // Metadata for Resource Identification
                @XmlNs(prefix = "srv",  namespaceURI = Namespaces.SRV),        // Metadata for Services 2.0
                @XmlNs(prefix = "lan",  namespaceURI = Namespaces.LAN),        // Language localization
                @XmlNs(prefix = "mcc",  namespaceURI = Namespaces.MCC),        // Metadata Common Classes
                @XmlNs(prefix = "gco",  namespaceURI = Namespaces.GCO),        // Geographic Common
                @XmlNs(prefix = "gmd",  namespaceURI = LegacyNamespaces.GMD),  // Metadata ISO 19139:2007
                @XmlNs(prefix = "srv1", namespaceURI = LegacyNamespaces.SRV)   // Metadata for Services 1.0
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(CI_Responsibility.class),
    @XmlJavaTypeAdapter(DCPList.class),
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(GO_DateTime.class),
    @XmlJavaTypeAdapter(GO_GenericName.class),
    @XmlJavaTypeAdapter(MD_AggregateInformation.class),
    @XmlJavaTypeAdapter(MD_AssociatedResource.class),
    @XmlJavaTypeAdapter(MD_BrowseGraphic.class),
    @XmlJavaTypeAdapter(MD_CharacterSetCode.class),
    @XmlJavaTypeAdapter(MD_Constraints.class),
    @XmlJavaTypeAdapter(MD_DataIdentification.class),
    @XmlJavaTypeAdapter(MD_Format.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(MD_Keywords.class),
    @XmlJavaTypeAdapter(MD_KeywordTypeCode.class),
    @XmlJavaTypeAdapter(MD_MaintenanceInformation.class),
    @XmlJavaTypeAdapter(MD_ProgressCode.class),
    @XmlJavaTypeAdapter(MD_RepresentativeFraction.class),
    @XmlJavaTypeAdapter(MD_Resolution.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentationTypeCode.class),
    @XmlJavaTypeAdapter(MD_StandardOrderProcess.class),
    @XmlJavaTypeAdapter(MD_TopicCategoryCode.class),
    @XmlJavaTypeAdapter(MD_Usage.class),
    @XmlJavaTypeAdapter(SV_CoupledResource.class),
    @XmlJavaTypeAdapter(SV_CouplingType.class),
    @XmlJavaTypeAdapter(SV_OperationMetadata.class),
    @XmlJavaTypeAdapter(SV_OperationChainMetadata.class),
    @XmlJavaTypeAdapter(SV_Parameter.class),
    @XmlJavaTypeAdapter(TM_Duration.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(URIAdapter.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(LocaleAdapter.class)
})
package org.apache.sis.metadata.iso.identification;

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
import org.apache.sis.xml.bind.gts.*;
import org.apache.sis.xml.bind.metadata.*;
import org.apache.sis.xml.bind.metadata.code.*;
