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
 * {@section Overview}
 * For a global overview of metadata in SIS, see the
 * <a href="{@docRoot}/../sis-metadata/index.html">Metadata page on the project web site</a>.
 * For some explanation about how to use various ISO 19115 elements for scientific dataset, the
 * <a href="https://geo-ide.noaa.gov/wiki/index.php?title=Category:ISO_19115">NOAA wiki page</a>
 * is a good source of information.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td width="50%" nowrap>
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultMetadata                     Metadata}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultPortrayalCatalogueReference  Portrayal catalogue reference}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultApplicationSchemaInformation Application schema information}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultMetadataExtensionInformation Metadata extension information}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultExtendedElementInformation   Extended element information}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.DefaultFeatureTypeList              Feature type list}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.DefaultIdentifier                   Identifier}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.Datatype   Data type}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.Obligation Obligation}<br>
 * </td><td class="sep" width="50%" nowrap>
 *                     {@linkplain org.apache.sis.metadata.iso.DefaultMetadata                     Metadata}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.DefaultPortrayalCatalogueReference  Portrayal catalogue reference}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.DefaultApplicationSchemaInformation Application schema information}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.DefaultMetadataExtensionInformation Metadata extension information}<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.DefaultExtendedElementInformation   Extended element information}<br>
 * {@code  │       ├─} {@linkplain org.opengis.metadata.Datatype                                   Data type} «code list»<br>
 * {@code  │       └─} {@linkplain org.opengis.metadata.Obligation                                 Obligation} «code list»<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.DefaultFeatureTypeList              Feature type list}<br>
 * {@code  └─}         {@linkplain org.apache.sis.metadata.iso.DefaultIdentifier                   Identifier}<br>
 * </td></tr></table>
 *
 * {@section Localization}
 * When a metadata object is marshalled as an ISO 19139 compliant XML document, the marshaller
 * {@link org.apache.sis.xml.XML#LOCALE} property will be used for the localization of every
 * {@link org.opengis.util.InternationalString} and {@link org.opengis.util.CodeList} instances,
 * <strong>except</strong> if the object to be marshalled is an instance of
 * {@link org.apache.sis.metadata.iso.DefaultMetadata}, in which case the value given to the
 * {@link org.apache.sis.metadata.iso.DefaultMetadata#setLanguage setLanguage(Locale)} method
 * will have precedence. The later behavior is compliant with INSPIRE rules.
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
    @XmlNs(prefix = "gmi", namespaceURI = Namespaces.GMI),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    @XmlJavaTypeAdapter(DQ_DataQuality.class),
    @XmlJavaTypeAdapter(MD_ApplicationSchemaInformation.class),
    @XmlJavaTypeAdapter(MD_CharacterSetCode.class),
    @XmlJavaTypeAdapter(MD_Constraints.class),
    @XmlJavaTypeAdapter(MD_ContentInformation.class),
    @XmlJavaTypeAdapter(MD_DatatypeCode.class),
    @XmlJavaTypeAdapter(MD_Distribution.class),
    @XmlJavaTypeAdapter(MD_ExtendedElementInformation.class),
    @XmlJavaTypeAdapter(MD_FeatureTypeList.class),
    @XmlJavaTypeAdapter(MD_Identification.class),
    @XmlJavaTypeAdapter(MD_MaintenanceInformation.class),
    @XmlJavaTypeAdapter(MD_MetadataExtensionInformation.class),
    @XmlJavaTypeAdapter(MD_ObligationCode.class),
    @XmlJavaTypeAdapter(MD_PortrayalCatalogueReference.class),
    @XmlJavaTypeAdapter(MD_ScopeCode.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentation.class),
    @XmlJavaTypeAdapter(MI_AcquisitionInformation.class),
    @XmlJavaTypeAdapter(RS_ReferenceSystem.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(LocaleAdapter.class),
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(GO_DateTime.class),
    @XmlJavaTypeAdapter(GO_Boolean.class), @XmlJavaTypeAdapter(type=boolean.class, value=GO_Boolean.class),
    @XmlJavaTypeAdapter(GO_Integer.class), @XmlJavaTypeAdapter(type=int.class,     value=GO_Integer.class)
})
package org.apache.sis.metadata.iso;

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
