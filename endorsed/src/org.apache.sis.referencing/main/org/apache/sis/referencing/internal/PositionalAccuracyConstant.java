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
package org.apache.sis.referencing.internal;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.io.ObjectStreamException;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
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
import org.apache.sis.metadata.iso.quality.DefaultMeasureReference;
import org.apache.sis.metadata.iso.quality.DefaultEvaluationMethod;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.metadata.privy.RecordSchemaSIS;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.DefaultRecord;
import org.apache.sis.system.Configuration;


/**
 * Predefined positional accuracy resulting from some coordinate operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
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
    @Configuration
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
    @Configuration
    public static final double DATUM_SHIFT_ACCURACY = 25;

    /**
     * Default accuracy of datum shifts when using an intermediate datum (typically WGS 84).
     * Since this is a concatenation of two datum shifts, we use twice {@link #DATUM_SHIFT_ACCURACY}.
     * The result is multiplied by 2 again as a margin because we have no guarantees that the domain
     * of validity of the two datum are close enough for making this concatenation valid.
     */
    @Configuration
    public static final double INDIRECT_SHIFT_ACCURACY = 100;

    /**
     * Indicates that a {@linkplain org.opengis.referencing.operation.Transformation transformation}
     * requires a datum shift and some method has been applied. Datum shift methods often use
     * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa Wolf parameters},
     * but other kinds of method may have been applied as well.
     *
     * @todo Should use the accuracy defined in {@code BoundCRS} instead.
     */
    public static final PositionalAccuracy DATUM_SHIFT_APPLIED;

    /**
     * Indicates that a {@linkplain org.opengis.referencing.operation.Transformation transformation}
     * requires a datum shift, but no method has been found applicable. This usually means that no
     * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa Wolf parameters} have
     * been found. Such datum shifts are approximations and may have 1 kilometer error.
     */
    public static final PositionalAccuracy DATUM_SHIFT_OMITTED;

    /**
     * Indicates that a {@linkplain org.opengis.referencing.operation.Transformation transformation}
     * requires a datum shift, but only an indirect method has been found. The indirect method uses
     * an intermediate datum, typically WGS 84.
     */
    public static final PositionalAccuracy INDIRECT_SHIFT_APPLIED;

    /**
     * Name for accuracy metadata of datum ensemble.
     */
    private static final DefaultMeasureReference ENSEMBLE_REFERENCE =
            new DefaultMeasureReference(Vocabulary.formatInternational(Vocabulary.Keys.EnsembleAccuracy));

    /**
     * Name for accuracy metadata of coordinate transformations.
     */
    private static final DefaultMeasureReference TRANSFORMATION_REFERENCE =
            new DefaultMeasureReference(Vocabulary.formatInternational(Vocabulary.Keys.TransformationAccuracy));

    /**
     * The evaluation method for coordinate transformations when the accuracy is specified in the EPSG database.
     * Those evaluation method are considered "external" on the assumption that the operation results have been
     * compared by the database maintainers against some results taken as true. By contrast, the accuracies that
     * we have set to conservative values are considered "direct internal".
     */
    private static final DefaultEvaluationMethod EVALUATION_METHOD =
            new DefaultEvaluationMethod(EvaluationMethodType.DIRECT_EXTERNAL,
                    Resources.formatInternational(Resources.Keys.AccuracyFromGeodeticDatase));

    static {
        ENSEMBLE_REFERENCE      .transitionTo(DefaultMeasureReference.State.FINAL);
        TRANSFORMATION_REFERENCE.transitionTo(DefaultMeasureReference.State.FINAL);
        EVALUATION_METHOD       .transitionTo(DefaultEvaluationMethod.State.FINAL);
        final var desc   = Resources.formatInternational(Resources.Keys.ConformanceMeansDatumShift);
        final var method = new DefaultEvaluationMethod(EvaluationMethodType.DIRECT_INTERNAL, desc);
        final var pass   = new DefaultConformanceResult(Citations.SIS, desc, true);
        final var fail   = new DefaultConformanceResult(Citations.SIS, desc, false);

        DATUM_SHIFT_APPLIED    = new PositionalAccuracyConstant(TRANSFORMATION_REFERENCE, method, pass, DATUM_SHIFT_ACCURACY);
        DATUM_SHIFT_OMITTED    = new PositionalAccuracyConstant(TRANSFORMATION_REFERENCE, method, fail, UNKNOWN_ACCURACY);
        INDIRECT_SHIFT_APPLIED = new PositionalAccuracyConstant(TRANSFORMATION_REFERENCE, method, pass, INDIRECT_SHIFT_ACCURACY);
    }

    /**
     * Creates a positional accuracy initialized to the given result.
     *
     * @param  reference  description of the positional accuracy.
     * @param  method     method used for accuracy measurement, or {@code null}.
     * @param  result     qualitative result, or {@code null} if none.
     * @param  accuracy   the linear accuracy in metres, or {@code null} if none.
     */
    private PositionalAccuracyConstant(final DefaultMeasureReference  reference,
                                       final DefaultEvaluationMethod  method,
                                       final DefaultConformanceResult result,
                                       final Double accuracy)
    {
        setMeasureReference(reference);
        setEvaluationMethod(method);
        final var results = new ArrayList<Result>(2);
        if (result != null) {
            results.add(result);
        }
        if (accuracy != null) {
            final RecordType type = RecordSchemaSIS.REAL;
            final var record = new DefaultRecord(type);
            record.setAll(accuracy);

            final var r = new DefaultQuantitativeResult();
            r.setValues(List.of(record));
            r.setValueUnit(Units.METRE);        // In metres by definition in the EPSG database.
            r.setValueType(type);
            results.add(r);
        }
        setResults(results);
        transitionTo(State.FINAL);
    }

    /**
     * Creates a transformation accuracy for the given value, in metres.
     * This method may return a cached value.
     *
     * @param  accuracy  the accuracy in metres.
     * @return a positional accuracy with the given value, or {@code null} if the value is not positive.
     */
    public static PositionalAccuracy transformation(final double accuracy) {
        if (accuracy >= 0) {
            return CACHE.computeIfAbsent(Math.abs(accuracy),    // For making sure that we have positive zero.
                    (key) -> new PositionalAccuracyConstant(TRANSFORMATION_REFERENCE, EVALUATION_METHOD, null, key));
        }
        return null;
    }

    /**
     * Creates a datum ensemble accuracy for the given value, in metres.
     * This method may return a cached value.
     *
     * @param  accuracy  the accuracy in metres.
     * @return a positional accuracy with the given value, or {@code null} if the value is not positive.
     */
    public static PositionalAccuracy ensemble(final double accuracy) {
        if (accuracy >= 0) {
            return CACHE.computeIfAbsent(-Math.abs(accuracy),    // For making sure that we have negative zero.
                    (key) -> new PositionalAccuracyConstant(ENSEMBLE_REFERENCE, EVALUATION_METHOD, null, -key));
        }
        return null;
    }

    /**
     * Cache of positional accuracies of coordinate transformations (positive) or datum ensembles (negative).
     * The sign is used for differentiating whether the cache value is for an ensemble or a transformation.
     * Most coordinate operations use a small set of accuracy values.
     */
    private static final WeakValueHashMap<Double,PositionalAccuracy> CACHE = new WeakValueHashMap<>(Double.class);

    /**
     * Invoked on deserialization. Replace this instance by one of the constants, if applicable.
     *
     * @return the object to use after deserialization.
     * @throws ObjectStreamException if the serialized object defines an unknown data type.
     */
    private Object readResolve() throws ObjectStreamException {
        if (equals(DATUM_SHIFT_APPLIED))    return DATUM_SHIFT_APPLIED;
        if (equals(DATUM_SHIFT_OMITTED))    return DATUM_SHIFT_OMITTED;
        if (equals(INDIRECT_SHIFT_APPLIED)) return INDIRECT_SHIFT_APPLIED;
        return this;
    }

    /**
     * Convenience method returning the accuracy in meters for the specified operation.
     * This method tries each of the following procedures and returns the first successful one:
     *
     * <ul>
     *   <li>If at least one {@link QuantitativeResult} is found with a linear unit, then the largest
     *       accuracy estimate is converted to {@linkplain Units#METRE metres} and returned.</li>
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
     * @param  operation  the operation to inspect for accuracy.
     * @return the accuracy estimate (always in meters), or NaN if unknown.
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
                                        v = unitOfLength.getConverterTo(Units.METRE).convert(v);
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
