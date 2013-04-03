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
 * {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality Data quality} implementation.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.quality OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * {@section Overview}
 * For a global overview of metadata in SIS, see the
 * <a href="{@docRoot}/../sis-metadata/index.html">Metadata page on the project web site</a>.
 *
 * <table class="sis"><tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td width="50%" nowrap>
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality                        Data quality}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.quality.AbstractElement                           Element} «abstract»<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.quality.AbstractCompleteness                      Completeness} «abstract»<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultCompletenessCommission             Completeness commission}<br>
 * {@code  │   │   └─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultCompletenessOmission               Completeness omission}<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.quality.AbstractLogicalConsistency                Logical consistency} «abstract»<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultConceptualConsistency              Conceptual consistency}<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultDomainConsistency                  Domain consistency}<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultFormatConsistency                  Format consistency}<br>
 * {@code  │   │   └─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultTopologicalConsistency             Topological consistency}<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.quality.AbstractPositionalAccuracy                Positional accuracy} «abstract»<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy Absolute external positional accuracy}<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultRelativeInternalPositionalAccuracy Relative internal positional accuracy}<br>
 * {@code  │   │   └─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultGriddedDataPositionalAccuracy      Gridded data positional accuracy}<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.quality.AbstractTemporalAccuracy                  Temporal accuracy} «abstract»<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultAccuracyOfATimeMeasurement         Accuracy of a time measurement}<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultTemporalConsistency                Temporal consistency}<br>
 * {@code  │   │   └─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultTemporalValidity                   Temporal validity}<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.quality.AbstractThematicAccuracy                  Thematic accuracy} «abstract»<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultQuantitativeAttributeAccuracy      Quantitative attribute accuracy}<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultNonQuantitativeAttributeAccuracy   Non quantitative attribute accuracy}<br>
 * {@code  │   │   └─} {@linkplain org.apache.sis.metadata.iso.quality.DefaultThematicClassificationCorrectness  Thematic classification correctness}<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.quality.DefaultUsability                          Usability}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.quality.AbstractResult                            Result} «abstract»<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult                  Conformance result}<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult                 Quantitative result}<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.quality.DefaultCoverageResult                     Coverage result}<br>
 * {@code  └─}         {@linkplain org.apache.sis.metadata.iso.quality.DefaultScope                              Scope}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.quality.EvaluationMethodType Evaluation method type}<br>
 * </td><td class="sep" width="50%" nowrap>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality                        Data quality}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.quality.DefaultScope                              Scope}<br>
 * {@code  └─}         {@linkplain org.apache.sis.metadata.iso.quality.AbstractElement                           Element} «abstract»<br>
 * {@code      ├─}     {@linkplain org.opengis.metadata.quality.EvaluationMethodType                             Evaluation method type} «code list»<br>
 * {@code      └─}     {@linkplain org.apache.sis.metadata.iso.quality.AbstractResult                            Result} «abstract»<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.AbstractCompleteness                      Completeness} «abstract»<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultCompletenessCommission             Completeness commission}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultCompletenessOmission               Completeness omission}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.AbstractLogicalConsistency                Logical consistency} «abstract»<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultConceptualConsistency              Conceptual consistency}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultDomainConsistency                  Domain consistency}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultFormatConsistency                  Format consistency}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultTopologicalConsistency             Topological consistency}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.AbstractPositionalAccuracy                Positional accuracy} «abstract»<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy Absolute external positional accuracy}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultRelativeInternalPositionalAccuracy Relative internal positional accuracy}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultGriddedDataPositionalAccuracy      Gridded data positional accuracy}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.AbstractTemporalAccuracy                  Temporal accuracy} «abstract»<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultAccuracyOfATimeMeasurement         Accuracy of a time measurement}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultTemporalConsistency                Temporal consistency}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultTemporalValidity                   Temporal validity}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.AbstractThematicAccuracy                  Thematic accuracy} «abstract»<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultQuantitativeAttributeAccuracy      Quantitative attribute accuracy}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultNonQuantitativeAttributeAccuracy   Non quantitative attribute accuracy}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultThematicClassificationCorrectness  Thematic classification correctness}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultUsability                          Usability}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult                  Conformance result}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult                 Quantitative result}<br>
 *                     {@linkplain org.apache.sis.metadata.iso.quality.DefaultCoverageResult                     Coverage result}<br>
 * </td></tr></table>
 *
 * {@section Collections and null values}
 * All constructors (except the <cite>copy constructor</cite>) and setter methods accept {@code null} arguments.
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
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMD, xmlns = {
    @XmlNs(prefix = "gmx", namespaceURI = Namespaces.GMX),
    @XmlNs(prefix = "gmi", namespaceURI = Namespaces.GMI),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(DQ_Element.class),
    @XmlJavaTypeAdapter(DQ_EvaluationMethodTypeCode.class),
    @XmlJavaTypeAdapter(DQ_Result.class),
    @XmlJavaTypeAdapter(DQ_Scope.class),
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(LI_Lineage.class),
    @XmlJavaTypeAdapter(MD_ContentInformation.class),
    @XmlJavaTypeAdapter(MD_Format.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(MD_ScopeCode.class),
    @XmlJavaTypeAdapter(MD_ScopeDescription.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentation.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentationTypeCode.class),
    @XmlJavaTypeAdapter(MX_DataFile.class),

    // Java types, primitive types and basic OGC types handling
//    @XmlJavaTypeAdapter(UnitAdapter.class), // TODO
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
//    @XmlJavaTypeAdapter(GO_DateTime.class), // TODO
//    @XmlJavaTypeAdapter(GO_RecordType.class), // TODO
    @XmlJavaTypeAdapter(GO_Boolean.class), @XmlJavaTypeAdapter(type=boolean.class, value=GO_Boolean.class)
})
package org.apache.sis.metadata.iso.quality;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
