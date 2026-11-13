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

import org.apache.sis.metadata.iso.quality.AbstractElement;
import org.apache.sis.metadata.iso.quality.DefaultEvaluationMethod;
import org.apache.sis.metadata.iso.quality.DefaultNonQuantitativeAttributeAccuracy;
import org.opengis.metadata.quality.EvaluationMethodType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JSONMDQualityElementType} and {@link JSONMDEvaluationMethodKind}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class JSONMDQualityElementTypeTest {

    @Test
    public void testElementTypes() {
        for (final JSONMDQualityElementType type : JSONMDQualityElementType.values()) {
            final AbstractElement element = type.newElement();
            assertInstanceOf(type.getType(), element);
            assertSame(type, JSONMDQualityElementType.of(element), type.getName());
            assertSame(type, JSONMDQualityElementType.of(type.getName()), type.getName());
        }
        assertNull(JSONMDQualityElementType.of("Completeness"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLegacyElement() {
        assertSame(JSONMDQualityElementType.NON_QUANTITATIVE_ATTRIBUTE_CORRECTNESS,
                   JSONMDQualityElementType.of(new DefaultNonQuantitativeAttributeAccuracy()));
    }

    @Test
    public void testMetaquality() {
        assertTrue (JSONMDQualityElementType.CONFIDENCE.isMetaquality());
        assertFalse(JSONMDQualityElementType.COMMISSION.isMetaquality());
    }

    @Test
    public void testEvaluationMethodKinds() {
        for (final JSONMDEvaluationMethodKind kind : JSONMDEvaluationMethodKind.values()) {
            assertSame(kind, JSONMDEvaluationMethodKind.of(kind.newMethod()), kind.getName());
            assertSame(kind, JSONMDEvaluationMethodKind.of(kind.getName()), kind.getName());
        }
        final DefaultEvaluationMethod legacy = new DefaultEvaluationMethod();
        assertSame(JSONMDEvaluationMethodKind.FULL_INSPECTION, JSONMDEvaluationMethodKind.of(legacy));
        legacy.setEvaluationMethodType(EvaluationMethodType.INDIRECT);
        assertSame(JSONMDEvaluationMethodKind.INDIRECT_EVALUATION, JSONMDEvaluationMethodKind.of(legacy));
    }
}
