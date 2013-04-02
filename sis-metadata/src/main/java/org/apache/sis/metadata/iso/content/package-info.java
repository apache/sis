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
 * {@linkplain org.apache.sis.metadata.iso.content.AbstractContentInformation Content information} implementation.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.content OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * {@section Overview}
 * For a global overview of metadata in SIS, see the
 * <a href="{@docRoot}/../sis-metadata/index.html">Metadata page on the project web site</a>.
 *
 * <table class="sis"><tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td>
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.content.AbstractContentInformation Content information}<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription Feature catalogue description}<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.content.DefaultCoverageDescription Coverage description}<br>
 * {@code  │       └─} {@linkplain org.apache.sis.metadata.iso.content.DefaultImageDescription Image description}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeDimension Range dimension}<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.content.DefaultBand Band}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeElementDescription Range element description}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.BandDefinition Band definition}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.CoverageContentType Coverage content type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.ImagingCondition Imaging condition}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.PolarizationOrientation Polarization orientation}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.content.TransferFunctionType Transfer function type}<br>
 * </td><td class="sep">
 * {@linkplain org.apache.sis.metadata.iso.content.AbstractContentInformation Content information}<br>
 * {@linkplain org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription Feature catalogue description}<br>
 * {@linkplain org.apache.sis.metadata.iso.content.DefaultCoverageDescription Coverage description}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.CoverageContentType Coverage content type} (a code list)<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeDimension Range dimension}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeElementDescription Range element description}<br>
 * {@linkplain org.apache.sis.metadata.iso.content.DefaultBand Band}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.BandDefinition Band definition} (a code list)<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.PolarizationOrientation Polarization orientation} (a code list)<br>
 * {@code  └─} {@linkplain org.opengis.metadata.content.TransferFunctionType Transfer function type}} (a code list)<br>
 * {@linkplain org.apache.sis.metadata.iso.content.DefaultImageDescription Image description}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.content.ImagingCondition Imaging condition} (a code list)<br>
 * </td></tr></table>
 *
 * {@section Bands in gridded data}
 * ISO 19115 defines a {@link org.opengis.metadata.content.Band} interface
 * which expresses the range of wavelengths in the electromagnetic spectrum.
 * For the needs of Image I/O, an additional interface has been defined with a subset
 * of the {@code Band} API and the restriction to electromagnetic spectrum removed.
 * That interface is named {@link org.apache.sis.image.io.metadata.SampleDimension}.
 * Both {@code Band} and {@code SampleDimension} interfaces extend the same parent,
 * {@link org.opengis.metadata.content.RangeDimension}.
 *
 * {@section Collections and null values}
 * Unless otherwise noted in the Javadoc, all constructors and setter methods accept {@code null} argument.
 * A null argument value means that the metadata element can not be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if there is no value. More specifically:</p>
 * <ul>
 *   <li>If the return type is a collection, the method may return an empty collection (never {@code null}).</li>
 *   <li>If the return type is an array, the method may return an empty array (never {@code null}).</li>
 *   <li>Otherwise the method may return {@code null}.</li>
 * </ul>
 *
 * Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <cite>live</cite>: adding new elements in the collection
 * modify directly the underlying metadata object.
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
    @XmlJavaTypeAdapter(MD_CoverageContentTypeCode.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(MD_ImagingConditionCode.class),
    @XmlJavaTypeAdapter(MD_RangeDimension.class),
    @XmlJavaTypeAdapter(MI_BandDefinition.class),
    @XmlJavaTypeAdapter(MI_PolarizationOrientationCode.class),
    @XmlJavaTypeAdapter(MI_RangeElementDescription.class),
    @XmlJavaTypeAdapter(MI_TransferFunctionTypeCode.class),

    // Java types, primitive types and basic OGC types handling
//  @XmlJavaTypeAdapter(UnitAdapter.class), // TODO
    @XmlJavaTypeAdapter(LocaleAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(GO_GenericName.class),
//  @XmlJavaTypeAdapter(GO_RecordType.class), // TODO
    @XmlJavaTypeAdapter(GO_Boolean.class),        @XmlJavaTypeAdapter(type=boolean.class, value=GO_Boolean.class),
    @XmlJavaTypeAdapter(GO_Decimal.class),        @XmlJavaTypeAdapter(type=double.class,  value=GO_Decimal.class),
    @XmlJavaTypeAdapter(GO_Integer.class),        @XmlJavaTypeAdapter(type=int.class,     value=GO_Integer.class),
    @XmlJavaTypeAdapter(GO_Integer.AsLong.class), @XmlJavaTypeAdapter(type=long.class,    value=GO_Integer.AsLong.class)
})
package org.apache.sis.metadata.iso.content;

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
