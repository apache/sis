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
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.Record;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.referencing.operation.*;
import org.apache.sis.util.Static;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.iso.DefaultNameSpace;


/**
 * Utility methods related to {@link OperationMethod} and {@link MathTransform} instances.
 * Not in public API because the contract of those methods is not clear or is disputable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public final class OperationMethods extends Static {
    /**
     * The key for specifying explicitely the value to be returned by
     * {@link org.apache.sis.referencing.operation.DefaultConversion#getParameterValues()}.
     * It is usually not necessary to specify those parameters because they are inferred either from
     * the {@link MathTransform}, or specified explicitely in a {@code DefiningConversion}. However
     * there is a few cases, for example the Molodenski transform, where none of the above can apply,
     * because SIS implements those operations as a concatenation of math transforms, and such
     * concatenations do not have {@link org.opengis.parameter.ParameterValueGroup}.
     */
    public static final String PARAMETERS_KEY = "parameters";

    /**
     * The key for specifying a {@linkplain org.opengis.referencing.operation.MathTransformFactory}
     * instance to use for the construction of a geodetic object. This is usually not needed for CRS
     * construction, except in the special case of a derived CRS created from a defining conversion.
     */
    public static final String MT_FACTORY = "mtFactory";

    /**
     * The separator character between an identifier and its namespace in the argument given to
     * {@link #getOperationMethod(String)}. For example this is the separator in {@code "EPSG:9807"}.
     *
     * This is defined as a constant for now, but we may make it configurable in a future version.
     */
    private static final char IDENTIFIER_SEPARATOR = DefaultNameSpace.DEFAULT_SEPARATOR;

    /**
     * Do not allow instantiation of this class.
     */
    private OperationMethods() {
    }

    /**
     * Returns the operation method for the specified name or identifier. The given argument shall be either a
     * method name (e.g. <cite>"Transverse Mercator"</cite>) or one of its identifiers (e.g. {@code "EPSG:9807"}).
     *
     * @param  methods The method candidates.
     * @param  identifier The name or identifier of the operation method to search.
     * @return The coordinate operation method for the given name or identifier, or {@code null} if none.
     *
     * @see org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#getOperationMethod(String)
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#getOperationMethod(String)
     */
    public static OperationMethod getOperationMethod(final Iterable<? extends OperationMethod> methods, final String identifier) {
        OperationMethod fallback = null;
        for (final OperationMethod method : methods) {
            if (matches(method, identifier)) {
                /*
                 * Stop the iteration at the first non-deprecated method.
                 * If we find only deprecated methods, take the first one.
                 */
                if (!(method instanceof Deprecable) || !((Deprecable) method).isDeprecated()) {
                    return method;
                }
                if (fallback == null) {
                    fallback = method;
                }
            }
        }
        return fallback;
    }

    /**
     * Returns {@code true} if the name or an identifier of the given method matches the given {@code identifier}.
     *
     * @param  method     The method to test for a match.
     * @param  identifier The name or identifier of the operation method to search.
     * @return {@code true} if the given method is a match for the given identifier.
     */
    private static boolean matches(final OperationMethod method, final String identifier) {
        if (IdentifiedObjects.isHeuristicMatchForName(method, identifier)) {
            return true;
        }
        for (int s = identifier.indexOf(IDENTIFIER_SEPARATOR); s >= 0;
                 s = identifier.indexOf(IDENTIFIER_SEPARATOR, s))
        {
            final String codespace = identifier.substring(0, s).trim();
            final String code = identifier.substring(++s).trim();
            for (final Identifier id : method.getIdentifiers()) {
                if (codespace.equalsIgnoreCase(id.getCodeSpace()) && code.equalsIgnoreCase(id.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convenience method returning the accuracy in meters for the specified operation.
     * This method tries each of the following procedures and returns the first successful one:
     *
     * <ul>
     *   <li>If a {@link QuantitativeResult} is found with a linear unit, then this accuracy estimate
     *       is converted to {@linkplain SI#METRE metres} and returned.</li>
     *   <li>Otherwise, if the operation is a {@link Conversion}, then returns 0 since a conversion
     *       is by definition accurate up to rounding errors.</li>
     *   <li>Otherwise, if the operation is a {@link Transformation}, then checks if the datum shift
     *       were applied with the help of Bursa-Wolf parameters. This procedure looks for SIS-specific
     *       {@link PositionalAccuracyConstant#DATUM_SHIFT_APPLIED} and
     *       {@link PositionalAccuracyConstant#DATUM_SHIFT_OMITTED DATUM_SHIFT_OMITTED} constants.</li>
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
        final Collection<PositionalAccuracy> accuracies = operation.getCoordinateOperationAccuracy();
        for (final PositionalAccuracy accuracy : accuracies) {
            for (final Result result : accuracy.getResults()) {
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
                                        return v;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
            if (accuracies.contains(PositionalAccuracyConstant.DATUM_SHIFT_APPLIED)) {
                return PositionalAccuracyConstant.DATUM_SHIFT_ACCURACY;
            }
            if (accuracies.contains(PositionalAccuracyConstant.DATUM_SHIFT_OMITTED)) {
                return PositionalAccuracyConstant.UNKNOWN_ACCURACY;
            }
        }
        /*
         * If the coordinate operation is a compound of other coordinate operations, returns the sum of their accuracy,
         * skipping unknown ones. Making the sum is a conservative approach (not exactly the "worst case" scenario,
         * since it could be worst if the transforms are highly non-linear).
         */
        double accuracy = Double.NaN;
        if (operation instanceof ConcatenatedOperation) {
            for (final SingleOperation op : ((ConcatenatedOperation) operation).getOperations()) {
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
        return accuracy;
    }
}
