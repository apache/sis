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

import java.util.Collection;
import java.util.Collections;
import java.io.ObjectStreamException;
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.opengis.util.Record;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.metadata.quality.Result;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Transformation;
import org.apache.sis.measure.Units;
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
 * @version 0.6
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
     * Presumed worst case error when no datum shift information was found.
     * The highest value found in the EPSG database 6.7 is 999 metres (worst datum shift), so this error
     * should be yet higher. I have seen 3 kilometres mentioned in some documentation somewhere.
     *
     * <p>If this value is modified, please update {@code getLinearAccuracy()} public javadoc accordingly.</p>
     *
     * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation#getLinearAccuracy()
     */
    public static final double UNKNOWN_ACCURACY = 3000;

    /**
     * Default accuracy of datum shift, if not explicitly provided in the EPSG database.
     * The 25 meters value is the next highest value (after 999 metres) found in the EPSG
     * database version 6.7 for a significant number of transformations.
     *
     * <p>If this value is modified, please update {@code getLinearAccuracy()} public javadoc accordingly.</p>
     *
     * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation#getLinearAccuracy()
     */
    private static final double DATUM_SHIFT_ACCURACY = 25;

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

    /**
     * Convenience method returning the accuracy in meters for the specified operation.
     * This method tries each of the following procedures and returns the first successful one:
     *
     * <ul>
     *   <li>If at least one {@link QuantitativeResult} is found with a linear unit, then the largest
     *       accuracy estimate is converted to {@linkplain SI#METRE metres} and returned.</li>
     *   <li>Otherwise, if the operation is a {@link Conversion}, then returns 0 since a conversion
     *       is by definition accurate up to rounding errors.</li>
     *   <li>Otherwise, if the operation is a {@link Transformation}, then checks if the datum shift
     *       were applied with the help of Bursa-Wolf parameters. This procedure looks for SIS-specific
     *       {@link #DATUM_SHIFT_APPLIED} and {@link #DATUM_SHIFT_OMITTED DATUM_SHIFT_OMITTED} constants.</li>
     *   <li>Otherwise, if the operation is a {@link ConcatenatedOperation}, returns the sum of the accuracy
     *       of all components. This is a conservative scenario where we assume that errors cumulate linearly.
     *       Note that this is not necessarily the "worst case" scenario since the accuracy could be worst
     *       if the math transforms are highly non-linear.</li>
     * </ul>
     *
     * If the above is modified, please update {@code AbstractCoordinateOperation.getLinearAccuracy()} javadoc.
     *
     * @param  operation The operation to inspect for accuracy.
     * @return The accuracy estimate (always in meters), or NaN if unknown.
     *
     * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation#getLinearAccuracy()
     */
    public static double getLinearAccuracy(final CoordinateOperation operation) {
        double accuracy = Double.NaN;
        final Collection<PositionalAccuracy> accuracies = operation.getCoordinateOperationAccuracy();
        for (final PositionalAccuracy metadata : accuracies) {
            for (final Result result : metadata.getResults()) {
                if (result instanceof QuantitativeResult) {
                    final QuantitativeResult quantity = (QuantitativeResult) result;
                    final Collection<? extends Record> records = quantity.getValues();
                    if (records != null) {
                        final Unit<?> unit = quantity.getValueUnit();
                        if (Units.isLinear(unit)) {
                            final Unit<Length> unitOfLength = unit.asType(Length.class);
                            for (final Record record : records) {
                                for (final Object value : record.getAttributes().values()) {
                                    if (value instanceof Number) {
                                        double v = ((Number) value).doubleValue();
                                        v = unitOfLength.getConverterTo(SI.METRE).convert(v);
                                        if (v >= 0 && !(v <= accuracy)) {       // '!' is for replacing the NaN value.
                                            accuracy = v;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (Double.isNaN(accuracy)) {
            /*
             * No quantitative (linear) accuracy were found. If the coordinate operation is actually
             * a conversion, the accuracy is up to rounding error (i.e. conceptually 0) by definition.
             */
            if (operation instanceof Conversion) {
                return 0;
            }
            /*
             * If the coordinate operation is actually a transformation, checks if Bursa-Wolf parameters
             * were available for the datum shift. This is SIS-specific. See field javadoc for a rational
             * about the return values chosen.
             */
            if (operation instanceof Transformation) {
                if (accuracies.contains(DATUM_SHIFT_APPLIED)) {
                    return DATUM_SHIFT_ACCURACY;
                }
                if (accuracies.contains(DATUM_SHIFT_OMITTED)) {
                    return UNKNOWN_ACCURACY;
                }
            }
            /*
             * If the coordinate operation is a compound of other coordinate operations, returns the sum of their accuracy,
             * skipping unknown ones. Making the sum is a conservative approach (not exactly the "worst case" scenario,
             * since it could be worst if the transforms are highly non-linear).
             */
            if (operation instanceof ConcatenatedOperation) {
                for (final CoordinateOperation op : ((ConcatenatedOperation) operation).getOperations()) {
                    final double candidate = Math.abs(getLinearAccuracy(op));
                    if (!Double.isNaN(candidate)) {
                        if (Double.isNaN(accuracy)) {
                            accuracy = candidate;
                        } else {
                            accuracy += candidate;
                        }
                    }
                }
            }
        }
        return accuracy;
    }
}
