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

import java.util.Map;
import java.util.Collection;
import org.opengis.util.Record;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.referencing.operation.*;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Static;
import org.apache.sis.measure.Units;


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
     * {@link org.apache.sis.referencing.operation.DefaultSingleOperation#getParameterValues()}.
     * It is usually not necessary to specify those parameters because they are inferred either from
     * the {@link MathTransform}, or specified explicitely in a {@code DefiningConversion}. However
     * there is a few cases, for example the Molodenski transform, where none of the above can apply,
     * because SIS implements those operations as a concatenation of math transforms, and such
     * concatenations do not have {@link org.opengis.parameter.ParameterValueGroup}.
     */
    public static final String PARAMETERS_KEY = "parameters";

    /**
     * Do not allow instantiation of this class.
     */
    private OperationMethods() {
    }

    /**
     * Returns the most specific {@link CoordinateOperation} interface implemented by the specified operation.
     * Special cases:
     *
     * <ul>
     *   <li>If the operation implements the {@link Transformation} interface,
     *       then this method returns {@code Transformation.class}. Transformation
     *       has precedence over any other interface implemented by the operation.</li>
     *   <li>Otherwise if the operation implements the {@link Conversion} interface,
     *       then this method returns the most specific {@code Conversion} sub-interface.</li>
     *   <li>Otherwise if the operation implements the {@link SingleOperation} interface,
     *       then this method returns {@code SingleOperation.class}.</li>
     *   <li>Otherwise if the operation implements the {@link ConcatenatedOperation} interface,
     *       then this method returns {@code ConcatenatedOperation.class}.</li>
     *   <li>Otherwise this method returns {@code CoordinateOperation.class}.</li>
     * </ul>
     *
     * @param  operation A coordinate operation.
     * @return The most specific GeoAPI interface implemented by the given operation.
     */
    public static Class<? extends CoordinateOperation> getType(final CoordinateOperation operation) {
        if (operation instanceof        Transformation) return        Transformation.class;
        if (operation instanceof       ConicProjection) return       ConicProjection.class;
        if (operation instanceof CylindricalProjection) return CylindricalProjection.class;
        if (operation instanceof      PlanarProjection) return      PlanarProjection.class;
        if (operation instanceof            Projection) return            Projection.class;
        if (operation instanceof            Conversion) return            Conversion.class;
        if (operation instanceof       SingleOperation) return       SingleOperation.class;
        if (operation instanceof ConcatenatedOperation) return ConcatenatedOperation.class;
        return CoordinateOperation.class;
    }

    /**
     * Determines whether a match or mismatch is found between the two given collections of identifiers.
     * If any of the given collections is {@code null} or empty, this method returns {@code null}.
     *
     * <p>According ISO 19162 (<cite>Well known text representation of coordinate reference systems</cite>),
     * {@linkplain AbstractIdentifiedObject#getIdentifier() identifiers} should have precedence over
     * {@linkplain AbstractIdentifiedObject#getName() name} for identifying {@code IdentifiedObject}s,
     * at least in the case of {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod
     * operation methods} and {@linkplain org.apache.sis.parameter.AbstractParameterDescriptor parameters}.</p>
     *
     * @param  id1 The first collection of identifiers, or {@code null}.
     * @param  id2 The second collection of identifiers, or {@code null}.
     * @return {@code TRUE} or {@code FALSE} on match or mismatch respectively, or {@code null} if this method
     *         can not determine if there is a match or mismatch.
     */
    public static Boolean hasCommonIdentifier(final Iterable<? extends Identifier> id1,
                                              final Iterable<? extends Identifier> id2)
    {
        if (id1 != null && id2 != null) {
            boolean hasFound = false;
            for (final Identifier identifier : id1) {
                final Citation authority = identifier.getAuthority();
                final String   codeSpace = identifier.getCodeSpace();
                for (final Identifier other : id2) {
                    if (authorityMatches(identifier, authority, codeSpace)) {
                        if (CharSequences.equalsFiltered(identifier.getCode(), other.getCode(), Characters.Filter.UNICODE_IDENTIFIER, true)) {
                            return Boolean.TRUE;
                        }
                        hasFound = true;
                    }
                }
            }
            if (hasFound) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the given identifier authority matches the given {@code authority}.
     * If one of the authority is null, then the comparison fallback on the given {@code codeSpace}.
     * If the code spaces are also null, then this method conservatively returns {@code false}.
     *
     * @param  identifier The identifier to compare.
     * @param  authority  The desired authority, or {@code null}.
     * @param  codeSpace  The desired code space or {@code null}, used as a fallback if an authority is null.
     * @return {@code true} if the authority or code space (as a fallback only) matches.
     */
    private static boolean authorityMatches(final Identifier identifier, final Citation authority, final String codeSpace) {
        if (authority != null) {
            final Citation other = identifier.getAuthority();
            if (other != null) {
                return Citations.identifierMatches(authority, other);
            }
        }
        if (codeSpace != null) {
            final String other = identifier.getCodeSpace();
            if (other != null) {
                return CharSequences.equalsFiltered(codeSpace, other, Characters.Filter.UNICODE_IDENTIFIER, true);
            }
        }
        return false;
    }

    /**
     * Checks if an operation method and a math transform have a compatible number of source and target dimensions.
     * In the particular case of a {@linkplain PassThroughTransform pass through transform} with more dimensions
     * than what we would expect from the given method, the check will rather be performed against the
     * {@linkplain PassThroughTransform#getSubTransform() sub transform}.
     *
     * <p>The intend is to allow creation of a three-dimensional {@code ProjectedCRS} with a two-dimensional
     * {@code OperationMethod}, where the third-dimension just pass through. This is not a recommended approach
     * and we do not document that as a supported feature, but we do not prevent it neither.</p>
     *
     * <p>This method tries to locates what seems to be the "core" of the given math transform. The definition
     * of "core" is imprecise and may be adjusted in future SIS versions. The current algorithm is as below:</p>
     *
     * <ul>
     *   <li>If the given transform can be decomposed in {@linkplain MathTransforms#getSteps(MathTransform) steps},
     *       then the steps for {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis
     *       swapping and scaling} are ignored.</li>
     *   <li>If the given transform or its non-ignorable step is a {@link PassThroughTransform}, then its sub-transform
     *       is taken. Only one non-ignorable step may exist, otherwise we do not try to select any of them.</li>
     * </ul>
     *
     * @param  method     The operation method to compare to the math transform.
     * @param  transform  The math transform to compare to the operation method.
     * @param  properties Properties of the caller object being constructed, used only for formatting error message.
     * @throws IllegalArgumentException if the number of dimensions are incompatible.
     */
    public static void checkDimensions(final OperationMethod method, MathTransform transform,
            final Map<String,?> properties) throws IllegalArgumentException
    {
        int actual = transform.getSourceDimensions();
        Integer expected = method.getSourceDimensions();
        if (expected != null && actual > expected) {
            /*
             * The given MathTransform use more dimensions than the OperationMethod.
             * Try to locate one and only one sub-transform, ignoring axis swapping and scaling.
             */
            MathTransform subTransform = null;
            for (final MathTransform step : MathTransforms.getSteps(transform)) {
                if (!isIgnorable(step)) {
                    if (subTransform == null && step instanceof PassThroughTransform) {
                        subTransform = ((PassThroughTransform) step).getSubTransform();
                    } else {
                        subTransform = null;
                        break;
                    }
                }
            }
            if (subTransform != null) {
                transform = subTransform;
                actual = transform.getSourceDimensions();
            }
        }
        /*
         * Now verify if the MathTransform dimensions are equal to the OperationMethod ones,
         * ignoring null java.lang.Integer instances.
         */
        byte isTarget = 0; // false: wrong dimension is the source one.
        if (expected == null || actual == expected) {
            actual = transform.getTargetDimensions();
            expected = method.getTargetDimensions();
            if (expected == null || actual == expected) {
                return;
            }
            isTarget = 1; // true: wrong dimension is the target one.
        }
        throw new IllegalArgumentException(Errors.getResources(properties).getString(
                Errors.Keys.MismatchedTransformDimension_3, isTarget, expected, actual));
    }

    /**
     * Returns {@code true} if the specified transform is likely to exists only for axis swapping
     * and/or unit conversions. The heuristic rule checks if the transform is backed by a square
     * matrix with exactly one non-null value in each row and each column.
     */
    private static boolean isIgnorable(final MathTransform transform) {
        final Matrix matrix = MathTransforms.getMatrix(transform);
        if (matrix != null) {
            final int size = matrix.getNumRow();
            if (matrix.getNumCol() == size) {
                for (int j=0; j<size; j++) {
                    int n1=0, n2=0;
                    for (int i=0; i<size; i++) {
                        if (matrix.getElement(j,i) != 0) n1++;
                        if (matrix.getElement(i,j) != 0) n2++;
                    }
                    if (n1 != 1 || n2 != 1) {
                        return false;
                    }
                }
                return true;
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
     *
     *   <li>Otherwise, if the operation is a {@link Conversion}, then returns 0 since a conversion
     *       is by definition accurate up to rounding errors.</li>
     *
     *   <li>Otherwise, if the operation is a {@link Transformation}, then checks if the datum shift
     *       were applied with the help of Bursa-Wolf parameters. This procedure looks for SIS-specific
     *       {@link PositionalAccuracyConstant#DATUM_SHIFT_APPLIED} and
     *       {@link PositionalAccuracyConstant#DATUM_SHIFT_OMITTED DATUM_SHIFT_OMITTED} constants.
     *       If a datum shift has been applied, returns 25 meters.
     *       If a datum shift should have been applied but has been omitted, returns 3000 meters.</li>
     *
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
         * were available for the datum shift. This is SIS-specific. See method javadoc for a rational
         * about the return values chosen.
         */
        if (operation instanceof Transformation) {
            if (!accuracies.contains(PositionalAccuracyConstant.DATUM_SHIFT_OMITTED)) {
                if (accuracies.contains(PositionalAccuracyConstant.DATUM_SHIFT_APPLIED)) {
                    return 25;
                }
            }
            return 3000;
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
