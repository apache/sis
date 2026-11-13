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
import org.apache.sis.metadata.iso.quality.DefaultAggregationDerivation;
import org.apache.sis.metadata.iso.quality.DefaultEvaluationMethod;
import org.apache.sis.metadata.iso.quality.DefaultFullInspection;
import org.apache.sis.metadata.iso.quality.DefaultIndirectEvaluation;
import org.apache.sis.metadata.iso.quality.DefaultSampleBasedInspection;
import org.opengis.metadata.quality.AggregationDerivation;
import org.opengis.metadata.quality.EvaluationMethod;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.opengis.metadata.quality.FullInspection;
import org.opengis.metadata.quality.IndirectEvaluation;
import org.opengis.metadata.quality.SampleBasedInspection;

/**
 * Concrete evaluation method types of the {@code EvaluationMethodUnion} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 a)"
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#EvaluationMethodUnion</a>
 */
public enum JSONMDEvaluationMethodKind {
    AGGREGATION_DERIVATION("AggregationDerivation", AggregationDerivation.class, DefaultAggregationDerivation::new),
    FULL_INSPECTION("FullInspection", FullInspection.class, DefaultFullInspection::new),
    INDIRECT_EVALUATION("IndirectEvaluation", IndirectEvaluation.class, DefaultIndirectEvaluation::new),
    SAMPLE_BASED_INSPECTION("SampleBasedInspection", SampleBasedInspection.class, DefaultSampleBasedInspection::new);

    private final String jsonName;
    private final Class<? extends EvaluationMethod> type;
    private final Supplier<? extends DefaultEvaluationMethod> factory;

    private JSONMDEvaluationMethodKind(final String name, final Class<? extends EvaluationMethod> type,
                                 final Supplier<? extends DefaultEvaluationMethod> factory)
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
     * Creates a new SIS evaluation method of this kind.
     */
    public DefaultEvaluationMethod newMethod() {
        return factory.get();
    }

    /**
     * Returns the kind for the given JSON name, or {@code null} if unknown.
     */
    public static JSONMDEvaluationMethodKind of(final String name) {
        for (final JSONMDEvaluationMethodKind candidate : values()) {
            if (candidate.jsonName.equals(name)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Returns the kind of the given evaluation method.
     *
     * <p>A method which is not of a concrete type, as created from ISO 19115:2003 metadata,
     * is an indirect evaluation if its evaluation method type is {@code indirect}, or a full inspection otherwise.
     */
    public static JSONMDEvaluationMethodKind of(final EvaluationMethod method) {
        for (final JSONMDEvaluationMethodKind candidate : values()) {
            if (candidate.type.isInstance(method)) {
                return candidate;
            }
        }
        return (method.getEvaluationMethodType() == EvaluationMethodType.INDIRECT) ? INDIRECT_EVALUATION : FULL_INSPECTION;
    }
}
