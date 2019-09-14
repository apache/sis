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
package org.apache.sis.internal.metadata;

import java.util.Collections;
import org.opengis.util.RecordType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.iso.DefaultRecord;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.Static;


/**
 * Creates a record reporting coordinate transformation accuracy.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 */
public final class TransformationAccuracy extends Static {
    /**
     * The name for the transformation accuracy metadata.
     */
    private static final InternationalString TRANSFORMATION_ACCURACY =
            Vocabulary.formatInternational(Vocabulary.Keys.TransformationAccuracy);

    /**
     * Cache the positional accuracies. Most coordinate operation use a small set of accuracy values.
     */
    private static final WeakValueHashMap<Double,PositionalAccuracy> CACHE = new WeakValueHashMap<>(Double.class);

    /**
     * Do not allow instantiation of this class.
     */
    private TransformationAccuracy() {
    }

    /**
     * Creates a positional accuracy for the given value, in metres.
     * This method may return a cached value.
     *
     * @param  accuracy  the accuracy in metres.
     * @return a positional accuracy with the given value.
     */
    public static PositionalAccuracy create(final Double accuracy) {
        PositionalAccuracy p = CACHE.get(accuracy);
        if (p == null) {
            final RecordType type = RecordSchemaSIS.REAL;
            final DefaultRecord record = new DefaultRecord(type);
            record.setAll(accuracy);

            final DefaultQuantitativeResult result = new DefaultQuantitativeResult();
            result.setValues(Collections.singletonList(record));
            result.setValueUnit(Units.METRE);              // In metres by definition in the EPSG database.
            result.setValueType(type);

            final DefaultAbsoluteExternalPositionalAccuracy element =
                    new DefaultAbsoluteExternalPositionalAccuracy(result);
            element.setNamesOfMeasure(Collections.singleton(TRANSFORMATION_ACCURACY));
            element.setEvaluationMethodType(EvaluationMethodType.DIRECT_EXTERNAL);
            element.transitionTo(DefaultAbsoluteExternalPositionalAccuracy.State.FINAL);

            p = CACHE.putIfAbsent(accuracy, element);
            if (p == null) {
                p = element;
            }
        }
        return p;
    }
}
