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
 * {@section Overview}
 * For a global overview of metadata in SIS, see the
 * <a href="{@docRoot}/../sis-metadata/index.html">Metadata page on the project web site</a>.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td width="50%" nowrap>
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.AbstractIdentification       Identification} «abstract»<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.identification.DefaultDataIdentification    Data identification}<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.identification.DefaultServiceIdentification Service identification}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultResolution            Resolution}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic         Browse graphic}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultKeywords              Keywords}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultUsage                 Usage}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultAggregateInformation  Aggregate information}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.Progress        Progress}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.KeywordType     Keyword type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.AssociationType Association type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.InitiativeType  Initiative type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.identification.TopicCategory   Topic category}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.identification.CharacterSet    Character set}<br>
 * </td><td class="sep" width="50%" nowrap>
 *                 {@linkplain org.apache.sis.metadata.iso.identification.AbstractIdentification       Identification} «abstract»<br>
 * {@code  ├─}     {@linkplain org.opengis.metadata.identification.Progress                            Progress} «code list»<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic         Browse graphic}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultKeywords              Keywords}<br>
 * {@code  │   └─} {@linkplain org.opengis.metadata.identification.KeywordType                         Keyword type} «code list»<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultUsage                 Usage}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultAggregateInformation  Aggregate information}<br>
 * {@code      ├─} {@linkplain org.opengis.metadata.identification.AssociationType                     Association type} «code list»<br>
 * {@code      └─} {@linkplain org.opengis.metadata.identification.InitiativeType                      Initiative type} «code list»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.identification.DefaultDataIdentification    Data identification}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.identification.DefaultResolution            Resolution}<br>
 * {@code  └─}     {@linkplain org.opengis.metadata.identification.TopicCategory                       Topic category} «code list»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.identification.DefaultServiceIdentification Service identification}<br>
 *                 {@linkplain org.opengis.metadata.identification.CharacterSet                        Character set} «code list»<br>
 * </td></tr></table>
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction#setScale(double)}
 *       for computing the denominator from a scale value.</li>
 * </ul>
 *
 * {@section Null values, nil objects and collections}
 * All constructors (except the <cite>copy constructors</cite>) and setter methods accept {@code null} arguments.
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
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMD, xmlns = {
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "srv", namespaceURI = Namespaces.SRV),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    @XmlJavaTypeAdapter(DS_AssociationTypeCode.class),
    @XmlJavaTypeAdapter(DS_InitiativeTypeCode.class),
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(MD_AggregateInformation.class),
    @XmlJavaTypeAdapter(MD_BrowseGraphic.class),
    @XmlJavaTypeAdapter(MD_CharacterSetCode.class),
    @XmlJavaTypeAdapter(MD_Constraints.class),
    @XmlJavaTypeAdapter(MD_Format.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(MD_Keywords.class),
    @XmlJavaTypeAdapter(MD_KeywordTypeCode.class),
    @XmlJavaTypeAdapter(MD_MaintenanceInformation.class),
    @XmlJavaTypeAdapter(MD_ProgressCode.class),
    @XmlJavaTypeAdapter(MD_RepresentativeFraction.class),
    @XmlJavaTypeAdapter(MD_Resolution.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentationTypeCode.class),
    @XmlJavaTypeAdapter(MD_TopicCategoryCode.class),
    @XmlJavaTypeAdapter(MD_Usage.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(URIAdapter.class),
    @XmlJavaTypeAdapter(LocaleAdapter.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(GO_DateTime.class)
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
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.gmd.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
