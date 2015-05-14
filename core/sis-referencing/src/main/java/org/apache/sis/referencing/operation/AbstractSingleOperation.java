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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.internal.util.Constants;
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
@XmlType(name="AbstractSingleOperationType", propOrder = {
//  "method",   // TODO
//  "parameters"
})
@XmlRootElement(name = "AbstractSingleOperation")
@XmlSeeAlso({
    DefaultConversion.class
})
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractSingleOperation() {
        method = null;
        parameters = null;
    }

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
        ArgumentChecks.ensureNonNull("method",    method);
        ArgumentChecks.ensureNonNull("transform", transform);
        checkDimensions(method, ReferencingUtilities.getDimension(interpolationCRS), transform, properties);
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
     * Creates a defining conversion from the given transform and/or parameters.
     * See {@link DefaultConversion#DefaultConversion(Map, OperationMethod, MathTransform, ParameterValueGroup)}
     * for more information.
     */
    AbstractSingleOperation(final Map<String,?>       properties,
                            final OperationMethod     method,
                            final MathTransform       transform,
                            final ParameterValueGroup parameters)
    {
        super(properties, null, null, null, transform);
        ArgumentChecks.ensureNonNull("method", method);
        if (transform != null) {
            checkDimensions(method, 0, transform, properties);
        } else if (parameters == null) {
            throw new IllegalArgumentException(Errors.getResources(properties)
                    .getString(Errors.Keys.UnspecifiedParameterValues));
        }
        this.method = method;
        this.parameters = (parameters != null) ? parameters.clone() : null;
    }

    /**
     * Creates a new coordinate operation with the same values than the specified defining conversion,
     * except for the source CRS, target CRS and the math transform which are set the given values.
     *
     * <p>This constructor is for {@link DefaultConversion} usage only,
     * in order to create a "real" conversion from a defining conversion.</p>
     */
    AbstractSingleOperation(final SingleOperation definition,
                            final CoordinateReferenceSystem sourceCRS,
                            final CoordinateReferenceSystem targetCRS,
                            final MathTransform transform)
    {
        super(definition, sourceCRS, targetCRS, transform);
        method = definition.getMethod();
        parameters = getParameterValues(definition);
    }

    /**
     * Creates a new coordinate operation with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param operation The coordinate operation to copy.
     */
    protected AbstractSingleOperation(final SingleOperation operation) {
        super(operation);
        method = operation.getMethod();
        parameters = getParameterValues(operation);
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
     * @param  interpDim  The number of interpolation dimension, or 0 if none.
     * @param  transform  The math transform to compare to the operation method.
     * @param  properties Properties of the caller object being constructed, used only for formatting error message.
     * @throws IllegalArgumentException if the number of dimensions are incompatible.
     */
    static void checkDimensions(final OperationMethod method, final int interpDim, MathTransform transform,
            final Map<String,?> properties) throws IllegalArgumentException
    {
        int actual = transform.getSourceDimensions();
        Integer expected = method.getSourceDimensions();
        if (expected != null && actual > expected + interpDim) {
            /*
             * The given MathTransform uses more dimensions than the OperationMethod.
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
         * ignoring null java.lang.Integer instances.  We do not specify whether the method
         * dimensions should include the interpolation dimensions or not, so we accept both.
         */
        int isTarget = 0;   // 0 == false: the wrong dimension is the source one.
        if (expected == null || (actual == expected) || (actual == expected + interpDim)) {
            actual = transform.getTargetDimensions();
            expected = method.getTargetDimensions();
            if (expected == null || (actual == expected) || (actual == expected + interpDim)) {
                return;
            }
            isTarget = 1;   // 1 == true: the wrong dimension is the target one.
        }
        /*
         * At least one dimension does not match.  In principle this is an error, but we make an exception for the
         * "Affine parametric transformation" (EPSG:9624). The reason is that while OGC define that transformation
         * as two-dimensional, it can easily be extended to any number of dimensions. Note that Apache SIS already
         * has special handling for this operation (a TensorParameters dedicated class, etc.)
         */
        if (!IdentifiedObjects.isHeuristicMatchForName(method, Constants.AFFINE)) {
            throw new IllegalArgumentException(Errors.getResources(properties).getString(
                    Errors.Keys.MismatchedTransformDimension_3, isTarget, expected, actual));
        }
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
     * Returns a description of the operation method, including a list of expected parameter names.
     * The returned object does not contains any parameter value.
     *
     * @return A description of the operation method.
     */
    @Override
    public OperationMethod getMethod() {
        return method;
    }

    /**
     * Returns a description of the parameters. The default implementation performs the following choice:
     *
     * <ul>
     *   <li>If parameter values were specified explicitely at construction time,
     *       then the descriptor of those parameters is returned.</li>
     *   <li>Otherwise if this method can infer the parameter descriptor from the
     *       {@linkplain #getMathTransform() math transform}, then that descriptor is returned.</li>
     *   <li>Otherwise fallback on the {@linkplain DefaultOperationMethod#getParameters() method parameters}.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * the two parameter descriptions (from the {@code MathTransform} or from the {@code OperationMethod})
     * should be very similar. If they differ, it should be only in minor details like remarks, default
     * values or units of measurement.</div>
     *
     * @return A description of the parameters.
     *
     * @see DefaultOperationMethod#getParameters()
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterDescriptors()
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return (parameters != null) ? parameters.getDescriptor() : super.getParameterDescriptors();
    }

    /**
     * Returns the parameter values. The default implementation performs the following choice:
     *
     * <ul>
     *   <li>If parameter values were specified explicitely at construction time, then a
     *       {@linkplain org.apache.sis.parameter.DefaultParameterValueGroup#clone() clone}
     *       of those parameters is returned.</li>
     *   <li>Otherwise if this method can infer the parameter values from the
     *       {@linkplain #getMathTransform() math transform}, then those parameters are returned.</li>
     *   <li>Otherwise throw {@link org.apache.sis.util.UnsupportedImplementationException}.</li>
     * </ul>
     *
     * @return The parameter values.
     * @throws UnsupportedOperationException if the parameter values can not be determined
     *         for the current math transform implementation.
     *
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterValues()
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return (parameters != null) ? parameters.clone() : super.getParameterValues();
    }

    /**
     * Gets the parameter values of the given operation without computing and without cloning them (if possible).
     * If the parameters are automatically inferred from the math transform, do not compute them and instead return
     * {@code null} (in conformance with {@link #parameters} contract).
     */
    private static ParameterValueGroup getParameterValues(final SingleOperation operation) {
        return (operation instanceof AbstractSingleOperation) ?
                ((AbstractSingleOperation) operation).parameters : operation.getParameterValues();
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
