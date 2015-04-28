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
package org.apache.sis.internal.referencing;

import java.util.Collections;
import java.io.ObjectStreamException;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Messages;


/**
 * Pre-defined positional accuracy resulting from some coordinate operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see org.opengis.referencing.operation.Transformation#getCoordinateOperationAccuracy()
 */
@XmlTransient
public final class PositionalAccuracyConstant extends DefaultAbsoluteExternalPositionalAccuracy {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2554090935254116470L;

    /**
     * Indicates that a {@linkplain org.opengis.referencing.operation.Transformation transformation}
     * requires a datum shift and some method has been applied. Datum shift methods often use
     * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa Wolf parameters},
     * but other kind of method may have been applied as well.
     */
    public static final PositionalAccuracy DATUM_SHIFT_APPLIED;

    /**
     * Indicates that a {@linkplain org.opengis.referencing.operation.Transformation transformation}
     * requires a datum shift, but no method has been found applicable. This usually means that no
     * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa Wolf parameters} have
     * been found. Such datum shifts are approximative and may have 1 kilometer error.
     */
    public static final PositionalAccuracy DATUM_SHIFT_OMITTED;
    static {
        final InternationalString desc = Vocabulary.formatInternational(Vocabulary.Keys.TransformationAccuracy);
        final InternationalString eval = Messages  .formatInternational(Messages.Keys.ConformanceMeansDatumShift);
        DATUM_SHIFT_APPLIED = new PositionalAccuracyConstant(desc, eval, true);
        DATUM_SHIFT_OMITTED = new PositionalAccuracyConstant(desc, eval, false);
    }

    /**
     * Creates an positional accuracy initialized to the given result.
     */
    private PositionalAccuracyConstant(final InternationalString measureDescription,
            final InternationalString evaluationMethodDescription, final boolean pass)
    {
        DefaultConformanceResult result = new DefaultConformanceResult(Citations.SIS, evaluationMethodDescription, pass);
        result.freeze();
        setResults(Collections.singleton(result));
        setMeasureDescription(measureDescription);
        setEvaluationMethodDescription(evaluationMethodDescription);
        setEvaluationMethodType(EvaluationMethodType.DIRECT_INTERNAL);
        freeze();
    }

    /**
     * Invoked on deserialization. Replace this instance by one of the constants, if applicable.
     */
    private Object readResolve() throws ObjectStreamException {
        if (equals(DATUM_SHIFT_APPLIED)) return DATUM_SHIFT_APPLIED;
        if (equals(DATUM_SHIFT_OMITTED)) return DATUM_SHIFT_OMITTED;
        return this;
    }
}
