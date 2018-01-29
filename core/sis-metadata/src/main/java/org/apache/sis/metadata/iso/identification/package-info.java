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
 * <div class="section">Overview</div>
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<br>
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
 * <div class="section">Null values, nil objects and collections</div>
 * All constructors and setter methods accept {@code null} arguments.
 * A null argument value means that the metadata element can not be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if the type is neither a collection or an array.
 * Note that non-null values may be {@link org.apache.sis.xml.NilObject}s.</p>
 *
 * <p>Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <cite>live</cite>: adding new elements in the collection
 * modify directly the underlying metadata object.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.3
 * @module
 */
@XmlSchema(location=Schemas.METADATA_XSD_IDENTIFICATION, elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.MRI, xmlns = {
    @XmlNs(prefix = "mri", namespaceURI = Namespaces.MRI),      // Metadata for Resource Identification
    @XmlNs(prefix = "srv", namespaceURI = Namespaces.SRV),      // Metadata for Services
    @XmlNs(prefix = "cit", namespaceURI = Namespaces.CIT),      // Citation and responsible party information
    @XmlNs(prefix = "lan", namespaceURI = Namespaces.LAN),      // Language localization
    @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC),      // Metadata Common Classes
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),      // Geographic Common
    @XmlNs(prefix = "gmw", namespaceURI = Namespaces.GMW),      // Geographic Markup Wrappers
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI),      // XML schema instance
    @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(CI_Responsibility.class),
    @XmlJavaTypeAdapter(DCPList.class),
    @XmlJavaTypeAdapter(DS_AssociationTypeCode.class),
    @XmlJavaTypeAdapter(DS_InitiativeTypeCode.class),
    @XmlJavaTypeAdapter(EX_Extent.class),
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
    @XmlJavaTypeAdapter(PT_Locale.class),
    @XmlJavaTypeAdapter(SV_CoupledResource.class),
    @XmlJavaTypeAdapter(SV_CouplingType.class),
    @XmlJavaTypeAdapter(SV_OperationMetadata.class),
    @XmlJavaTypeAdapter(SV_OperationChainMetadata.class),
    @XmlJavaTypeAdapter(SV_Parameter.class),
    @XmlJavaTypeAdapter(SV_ParameterDirection.class),
    @XmlJavaTypeAdapter(TM_Duration.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(URIAdapter.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(GO_DateTime.class),
    @XmlJavaTypeAdapter(GO_GenericName.class),
    @XmlJavaTypeAdapter(GO_ScopedName.class),
    @XmlJavaTypeAdapter(GO_Boolean.class), @XmlJavaTypeAdapter(type=boolean.class, value=GO_Boolean.class)
})
package org.apache.sis.metadata.iso.identification;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.internal.jaxb.Schemas;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.gts.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
