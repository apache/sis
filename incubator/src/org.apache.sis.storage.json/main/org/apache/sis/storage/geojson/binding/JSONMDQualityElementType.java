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
package org.apache.sis.storage.geojson.binding;

import java.util.function.Supplier;
import org.apache.sis.metadata.iso.quality.AbstractElement;
import org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy;
import org.apache.sis.metadata.iso.quality.DefaultAccuracyOfATimeMeasurement;
import org.apache.sis.metadata.iso.quality.DefaultCompletenessCommission;
import org.apache.sis.metadata.iso.quality.DefaultCompletenessOmission;
import org.apache.sis.metadata.iso.quality.DefaultConceptualConsistency;
import org.apache.sis.metadata.iso.quality.DefaultConfidence;
import org.apache.sis.metadata.iso.quality.DefaultDomainConsistency;
import org.apache.sis.metadata.iso.quality.DefaultFormatConsistency;
import org.apache.sis.metadata.iso.quality.DefaultGriddedDataPositionalAccuracy;
import org.apache.sis.metadata.iso.quality.DefaultHomogeneity;
import org.apache.sis.metadata.iso.quality.DefaultNonQuantitativeAttributeCorrectness;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeAttributeAccuracy;
import org.apache.sis.metadata.iso.quality.DefaultRelativeInternalPositionalAccuracy;
import org.apache.sis.metadata.iso.quality.DefaultRepresentativity;
import org.apache.sis.metadata.iso.quality.DefaultTemporalConsistency;
import org.apache.sis.metadata.iso.quality.DefaultTemporalValidity;
import org.apache.sis.metadata.iso.quality.DefaultThematicClassificationCorrectness;
import org.apache.sis.metadata.iso.quality.DefaultTopologicalConsistency;
import org.opengis.metadata.quality.AbsoluteExternalPositionalAccuracy;
import org.opengis.metadata.quality.AccuracyOfATimeMeasurement;
import org.opengis.metadata.quality.CompletenessCommission;
import org.opengis.metadata.quality.CompletenessOmission;
import org.opengis.metadata.quality.ConceptualConsistency;
import org.opengis.metadata.quality.Confidence;
import org.opengis.metadata.quality.DomainConsistency;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.FormatConsistency;
import org.opengis.metadata.quality.GriddedDataPositionalAccuracy;
import org.opengis.metadata.quality.Homogeneity;
import org.opengis.metadata.quality.Metaquality;
import org.opengis.metadata.quality.NonQuantitativeAttributeCorrectness;
import org.opengis.metadata.quality.QuantitativeAttributeAccuracy;
import org.opengis.metadata.quality.RelativeInternalPositionalAccuracy;
import org.opengis.metadata.quality.Representativity;
import org.opengis.metadata.quality.TemporalConsistency;
import org.opengis.metadata.quality.TemporalValidity;
import org.opengis.metadata.quality.ThematicClassificationCorrectness;
import org.opengis.metadata.quality.TopologicalConsistency;

/**
 * Concrete data quality element types of the {@code QualityElementUnion} JSON type.
 *
 * <p>The JSON names are the ISO 19157-1:2023 class names, which differ from the ISO 19157:2013
 * names used by GeoAPI and SIS for some types (e.g. {@code Commission} is {@code CompletenessCommission}).
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 a)"
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#QualityElementUnion</a>
 */
public enum JSONMDQualityElementType {
    ABSOLUTE_POSITIONAL_ACCURACY("AbsolutePositionalAccuracy",
            AbsoluteExternalPositionalAccuracy.class, DefaultAbsoluteExternalPositionalAccuracy::new),
    ACCURACY_OF_A_TIME_MEASUREMENT("AccuracyOfATimeMeasurement",
            AccuracyOfATimeMeasurement.class, DefaultAccuracyOfATimeMeasurement::new),
    COMMISSION("Commission",
            CompletenessCommission.class, DefaultCompletenessCommission::new),
    CONCEPTUAL_CONSISTENCY("ConceptualConsistency",
            ConceptualConsistency.class, DefaultConceptualConsistency::new),
    CONFIDENCE("Confidence",
            Confidence.class, DefaultConfidence::new),
    DOMAIN_CONSISTENCY("DomainConsistency",
            DomainConsistency.class, DefaultDomainConsistency::new),
    FORMAT_CONSISTENCY("FormatConsistency",
            FormatConsistency.class, DefaultFormatConsistency::new),
    GRIDDED_DATA_POSITIONAL_ACCURACY("GriddedDataPositionalAccuracy",
            GriddedDataPositionalAccuracy.class, DefaultGriddedDataPositionalAccuracy::new),
    HOMOGENEITY("Homogeneity",
            Homogeneity.class, DefaultHomogeneity::new),
    NON_QUANTITATIVE_ATTRIBUTE_CORRECTNESS("NonQuantitativeAttributeCorrectness",
            NonQuantitativeAttributeCorrectness.class, DefaultNonQuantitativeAttributeCorrectness::new),
    OMISSION("Omission",
            CompletenessOmission.class, DefaultCompletenessOmission::new),
    QUANTITATIVE_ATTRIBUTE_ACCURACY("QuantitativeAttributeAccuracy",
            QuantitativeAttributeAccuracy.class, DefaultQuantitativeAttributeAccuracy::new),
    RELATIVE_POSITIONAL_ACCURACY("RelativePositionalAccuracy",
            RelativeInternalPositionalAccuracy.class, DefaultRelativeInternalPositionalAccuracy::new),
    REPRESENTATIVITY("Representativity",
            Representativity.class, DefaultRepresentativity::new),
    TEMPORAL_CONSISTENCY("TemporalConsistency",
            TemporalConsistency.class, DefaultTemporalConsistency::new),
    TEMPORAL_VALIDITY("TemporalValidity",
            TemporalValidity.class, DefaultTemporalValidity::new),
    THEMATIC_CLASSIFICATION_CORRECTNESS("ThematicClassificationCorrectness",
            ThematicClassificationCorrectness.class, DefaultThematicClassificationCorrectness::new),
    TOPOLOGICAL_CONSISTENCY("TopologicalConsistency",
            TopologicalConsistency.class, DefaultTopologicalConsistency::new);

    private final String jsonName;
    private final Class<? extends Element> type;
    private final Supplier<? extends AbstractElement> factory;

    private JSONMDQualityElementType(final String name, final Class<? extends Element> type,
                               final Supplier<? extends AbstractElement> factory)
    {
        this.jsonName = name;
        this.type    = type;
        this.factory = factory;
    }

    /**
     * Returns the type name used as {@code type} member value in JSON.
     */
    public String getName() {
        return jsonName;
    }

    /**
     * Returns the GeoAPI interface implemented by elements of this type.
     */
    public Class<? extends Element> getType() {
        return type;
    }

    /**
     * Returns whether this type is a metaquality element, which may have a {@code relatedQualityElement}.
     */
    public boolean isMetaquality() {
        return Metaquality.class.isAssignableFrom(type);
    }

    /**
     * Creates a new SIS element of this type.
     */
    public AbstractElement newElement() {
        return factory.get();
    }

    /**
     * Returns the type for the given JSON name, or {@code null} if unknown.
     */
    public static JSONMDQualityElementType of(final String name) {
        for (final JSONMDQualityElementType candidate : values()) {
            if (candidate.jsonName.equals(name)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Returns the type of the given element, or {@code null} if the element is not of a concrete type.
     */
    public static JSONMDQualityElementType of(final Element element) {
        for (final JSONMDQualityElementType candidate : values()) {
            if (candidate.type.isInstance(element)) {
                return candidate;
            }
        }
        return null;
    }
}
