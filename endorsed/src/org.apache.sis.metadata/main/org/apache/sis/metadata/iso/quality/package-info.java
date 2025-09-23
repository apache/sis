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
 * Information about data quality, accuracy and consistency of a dataset.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.quality OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
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
 * @author  Guilhem Legal (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @author  Alexis Gaillard (Geomatys)
 * @version 1.5
 * @since   0.3
 */
@XmlSchema(location="https://schemas.isotc211.org/19157/-2/mdq/1.0/mdq.xsd",
           elementFormDefault=XmlNsForm.QUALIFIED, namespace=Namespaces.MDQ,
           xmlns = {
                @XmlNs(prefix = "mdq", namespaceURI = Namespaces.MDQ),      // Metadata for Data Quality
                @XmlNs(prefix = "mrd", namespaceURI = Namespaces.MRD),      // Metadata for Resource Distribution
                @XmlNs(prefix = "dqm", namespaceURI = Namespaces.DQM),      // Data Quality Measures
                @XmlNs(prefix = "mcc", namespaceURI = Namespaces.MCC),      // Metadata Common Classes
                @XmlNs(prefix = "gmd", namespaceURI = LegacyNamespaces.GMD),
                @XmlNs(prefix = "gmi", namespaceURI = LegacyNamespaces.GMI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(DQ_Element.class),
    @XmlJavaTypeAdapter(DQ_EvaluationMethod.class),
    @XmlJavaTypeAdapter(DQ_EvaluationMethodTypeCode.class),
    @XmlJavaTypeAdapter(DQ_MeasureReference.class),
    @XmlJavaTypeAdapter(DQ_Result.class),
    @XmlJavaTypeAdapter(DQ_StandaloneQualityReportInformation.class),
    @XmlJavaTypeAdapter(DQM_BasicMeasure.class),
    @XmlJavaTypeAdapter(DQM_Description.class),
//  @XmlJavaTypeAdapter(DQM_Measure.class),             // Not directly referenced, but a "weak" association exists.
    @XmlJavaTypeAdapter(DQM_Parameter.class),
    @XmlJavaTypeAdapter(DQM_SourceReference.class),
    @XmlJavaTypeAdapter(DQM_ValueStructure.class),
    @XmlJavaTypeAdapter(GO_Temporal.class),
    @XmlJavaTypeAdapter(GO_DateTime.class),
    @XmlJavaTypeAdapter(GO_GenericName.class),
    @XmlJavaTypeAdapter(GO_Record.class),
    @XmlJavaTypeAdapter(GO_RecordType.class),
    @XmlJavaTypeAdapter(LI_Lineage.class),
    @XmlJavaTypeAdapter(MD_BrowseGraphic.class),
    @XmlJavaTypeAdapter(MD_ContentInformation.class),
    @XmlJavaTypeAdapter(MD_Format.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
//  @XmlJavaTypeAdapter(MD_RangeDimension.class),       // Pending new ISO 19157 revision.
    @XmlJavaTypeAdapter(MD_Scope.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentation.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentationTypeCode.class),
    @XmlJavaTypeAdapter(MX_DataFile.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(UnitAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class)
})
package org.apache.sis.metadata.iso.quality;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.gco.*;
import org.apache.sis.xml.bind.metadata.*;
import org.apache.sis.xml.bind.metadata.code.*;
