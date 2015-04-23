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
package org.apache.sis.referencing.operation;

import java.util.Map;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import java.util.Objects;


/**
 * Shared implementation for {@link DefaultConversion} and {@link DefaultTransformation}.
 * Does not need to be public, as users should handle only conversions or transformations.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
class AbstractSingleOperation extends AbstractCoordinateOperation implements SingleOperation, Parameterized {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2635450075620911309L;

    /**
     * The operation method.
     */
    private final OperationMethod method;

    /**
     * The parameter values, or {@code null} for inferring it from the math transform.
     */
    private final ParameterValueGroup parameters;

    /**
     * Creates a coordinate operation from the given properties.
     */
    public AbstractSingleOperation(final Map<String,?>             properties,
                                   final CoordinateReferenceSystem sourceCRS,
                                   final CoordinateReferenceSystem targetCRS,
                                   final CoordinateReferenceSystem interpolationCRS,
                                   final OperationMethod           method,
                                   final MathTransform             transform)
    {
        super(properties, sourceCRS, targetCRS, interpolationCRS, transform);
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        ArgumentChecks.ensureNonNull("method",    method);
        ArgumentChecks.ensureNonNull("transform", transform);
        checkDimensions(method, transform, properties);
        this.method = method;
        /*
         * Undocumented property, because SIS usually infers the parameters from the MathTransform.
         * However there is a few cases, for example the Molodenski transform, where we can not infer the
         * parameters easily because the operation is implemented by a concatenation of math transforms.
         */
        parameters = Containers.property(properties, OperationMethods.PARAMETERS_KEY, ParameterValueGroup.class);
        // No clone since this is a SIS internal property and SIS does not modify those values after construction.
    }

    /**
     * Creates a defining conversion. This is for {@link DefaultConversion} constructor only.
     */
    AbstractSingleOperation(final Map<String,?>   properties,
                            final OperationMethod method,
                            final MathTransform   transform)
    {
        super(properties, null, null, null, transform);
        ArgumentChecks.ensureNonNull("method",    method);
        ArgumentChecks.ensureNonNull("transform", transform);
        checkDimensions(method, transform, properties);
        this.method = method;
        parameters = Containers.property(properties, OperationMethods.PARAMETERS_KEY, ParameterValueGroup.class);
    }

    /**
     * Constructs a new operation with the same values than the specified one, together with the
     * specified source and target CRS. While the source operation can be an arbitrary one, it is
     * typically a defining conversion.
     */
    AbstractSingleOperation(final SingleOperation           definition,
                            final CoordinateReferenceSystem sourceCRS,
                            final CoordinateReferenceSystem targetCRS)
    {
        super(definition, sourceCRS, targetCRS);
        method = definition.getMethod();
        parameters = (definition instanceof AbstractSingleOperation) ?
                ((AbstractSingleOperation) definition).parameters : definition.getParameterValues();
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
    static void checkDimensions(final OperationMethod method, MathTransform transform,
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
     * Returns the operation method.
     *
     * @return The operation method.
     */
    @Override
    public OperationMethod getMethod() {
        return method;
    }

    /**
     * Returns a description of the parameters. The default implementation tries to infer the
     * description from the {@linkplain #getMathTransform() math transform} itself before to
     * fallback on the {@linkplain DefaultOperationMethod#getParameters() method parameters}.
     *
     * <div class="note"><b>Note:</b>
     * the two parameter descriptions (from the {@code MathTransform} or from the {@code OperationMethod})
     * should be very similar. If they differ, it should be only in minor details like remarks, default
     * values or units of measurement.</div>
     *
     * @return A description of the parameters.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return (parameters != null) ? parameters.getDescriptor() : super.getParameterDescriptors();
    }

    /**
     * Returns the parameter values. The default implementation infers the parameter values from the
     * {@linkplain #getMathTransform() math transform}, if possible.
     *
     * @return The parameter values.
     * @throws UnsupportedOperationException if the parameter values can not be determined
     *         for the current math transform implementation.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return (parameters != null) ? parameters.clone() : super.getParameterValues();
    }

    /**
     * Compares this coordinate operation with the specified object for equality. If the {@code mode} argument
     * is {@link ComparisonMode#STRICT} or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available
     * properties are compared including the {@linkplain #getDomainOfValidity() domain of validity} and the
     * {@linkplain #getScope() scope}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;   // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final AbstractSingleOperation that = (AbstractSingleOperation) object;
                return Objects.equals(method,     that.method) &&
                       Objects.equals(parameters, that.parameters);
            }
            case BY_CONTRACT: {
                final SingleOperation that = (SingleOperation) object;
                return deepEquals(getMethod(),          that.getMethod(),          mode) &&
                       deepEquals(getParameterValues(), that.getParameterValues(), mode);
            }
        }
        /*
         * We consider the operation method as metadata. One could argue that OperationMethod's 'sourceDimension' and
         * 'targetDimension' are not metadata, but their values should be identical to the 'sourceCRS' and 'targetCRS'
         * dimensions, already checked below. We could also argue that 'OperationMethod.parameters' are not metadata,
         * but their values should have been taken in account for the MathTransform creation, compared below.
         *
         * Comparing the MathTransforms instead of parameters avoid the problem of implicit parameters. For example in
         * a ProjectedCRS, the "semiMajor" and "semiMinor" axis lengths are sometime provided as explicit parameters,
         * and sometime inferred from the geodetic datum. The two cases would be different set of parameters from the
         * OperationMethod's point of view, but still result in the creation of identical MathTransforms.
         *
         * An other rational for treating OperationMethod as metadata is that SIS's MathTransform providers extend
         * DefaultOperationMethod. Consequently there is a wide range of subclasses, which make the comparisons more
         * difficult. For example Mercator1SP and Mercator2SP providers are two different ways to describe the same
         * projection. The SQL-backed EPSG factory uses yet an other implementation.
         *
         * NOTE: A previous Geotk implementation made this final check:
         *
         *     return nameMatches(this.method, that.method);
         *
         * but it was not strictly necessary since it was redundant with the comparisons of MathTransforms.
         * Actually it was preventing to detect that two CRS were equivalent despite different method names
         * (e.g. "Mercator (1SP)" and "Mercator (2SP)" when the parameters are properly chosen).
         */
        return true;
    }
}
