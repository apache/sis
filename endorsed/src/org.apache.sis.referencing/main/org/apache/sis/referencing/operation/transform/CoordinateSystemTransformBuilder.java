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
package org.apache.sis.referencing.operation.transform;

import java.util.List;
import javax.measure.IncommensurableException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CylindricalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.referencing.operation.provider.Spherical2Dto3D;
import org.apache.sis.referencing.operation.provider.Spherical3Dto2D;
import org.apache.sis.referencing.operation.provider.Geographic2Dto3D;
import org.apache.sis.referencing.operation.provider.Geographic3Dto2D;
import org.apache.sis.referencing.operation.provider.GeocentricToGeographic;
import org.apache.sis.referencing.operation.provider.GeographicToGeocentric;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.util.resources.Errors;


/**
 * Builder of transforms between coordinate systems.
 * This class performs only axis swapping, unit conversions and change of coordinate system type.
 * This class does not handle datum shifts. All <abbr>CRS</abbr> associated to the <abbr>CS</abbr>
 * must use the same datum.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CoordinateSystemTransformBuilder extends MathTransformBuilder {
    /**
     * The source and target coordinate systems.
     */
    private CoordinateSystem source, target;

    /**
     * The ellipsoid of the source and/or the target. Usually, only one of the source or target
     * is associated to an ellipsoid. If an ellipsoid is specified for both source and target,
     * then it must be the same ellipsoid because this builder is not for datum change.
     */
    private Ellipsoid ellipsoid;

    /**
     * The parameters used for creating the transform, or {@code null} if unspecified.
     *
     * @see #parameters()
     * @see #setParameters(MathTransform, OperationMethod, ParameterValueGroup)
     */
    private ParameterValueGroup parameters;

    /**
     * The {@link MathTransform} to use as the source of parameters, or {@code null} if none.
     * This is used as a fallback if {@link #parameters} is null.
     *
     * @see #parameters()
     * @see #setParameters(MathTransform, OperationMethod, ParameterValueGroup)
     */
    private Parameterized parameterized;

    /**
     * A code identifying the type of parameters. Values can be:
     *
     * <ul>
     *   <li>0: parameters are not set.</li>
     *   <li>1: identity transform.</li>
     *   <li>2: linear transform.</li>
     *   <li>3: non-linear transform.</li>
     * </ul>
     *
     * THe {@link #provider} and {@link #parameters} fields should be updated together
     * and only with strictly increasing {@code parameterType} values. If parameters
     * are specified twice for the same type, the first occurrence prevails.
     *
     * @see #parameters()
     * @see #setParameters(MathTransform, OperationMethod, ParameterValueGroup)
     */
    private byte parametersType;

    /**
     * Values for the {@link #parametersType} field.
     */
    private static final byte IDENTITY = 1, LINEAR = 2, CONVERSION = 3;

    /**
     * Creates a new builder.
     *
     * @param  factory  the factory to use for building the transform.
     */
    CoordinateSystemTransformBuilder(final MathTransformFactory factory) {
        super(factory);
    }

    /**
     * Sets the source coordinate system.
     * The ellipsoid shall be either null or the same as the target.
     *
     * @throws IllegalStateException if more than one ellipsoid is specified.
     */
    @Override
    public void setSourceAxes(CoordinateSystem cs, Ellipsoid ellipsoid) {
        setEllipsoid(ellipsoid);
        source = cs;
    }

    /**
     * Sets the target coordinate system.
     * The ellipsoid shall be either null or the same as the source.
     *
     * @throws IllegalStateException if more than one ellipsoid is specified.
     */
    @Override
    public void setTargetAxes(CoordinateSystem cs, Ellipsoid ellipsoid) {
        setEllipsoid(ellipsoid);
        target = cs;
    }

    /**
     * Sets the unique ellipsoid.
     *
     * @param  value  the ellipsoid, or {@code null} if unspecified.
     * @throws IllegalStateException if more than one ellipsoid is specified.
     */
    private void setEllipsoid(final Ellipsoid value) {
        if (value != null) {
            if (ellipsoid != null && ellipsoid != value) {
                throw new IllegalStateException(Errors.format(Errors.Keys.AlreadyInitialized_1, "ellipsoid"));
            }
            ellipsoid = value;
        }
    }

    /**
     * Returns the parameters for creating the transform. If possible, the parameter values will be set
     * to the values actually used, but this is not guaranteed (this is not required by method contract).
     * This information is valid after the {@link #create()} method has been invoked.
     */
    @Override
    public ParameterValueGroup parameters() {
        if (parameters == null) {
            if (parameterized != null) {
                parameters = parameterized.getParameterValues();
            }
            if (parameters == null) {
                OperationMethod method = provider;
                if (method == null) {
                    method = Affine.provider();
                }
                /*
                 * This is either a parameterless conversion (for example, from spherical to Cartesian coordinate system),
                 * in which case there is no parameters to set, or an affine transform. In the latter case, the default is
                 * the identity transform, which is often correct. But even if not exact, an identity affine is still okay
                 * because this method contract is to provide an initial set of parameters to be filled by the user.
                 */
                parameters = method.getParameters().createValue();
            }
        }
        return parameters;
    }

    /**
     * Creates the change of coordinate system.
     *
     * @todo Handle the case where coordinate system components are not in the same order.
     *
     * @return the transform from the given source CS to the given target CS.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform create() throws FactoryException {
        if (source == null || target == null) {
            throw new IllegalStateException(Errors.format(
                    Errors.Keys.MissingValueForProperty_1,
                    (source == null) ? "source" : "target"));
        }
        final List<CoordinateSystem> sources = CoordinateSystems.getSingleComponents(source);
        final List<CoordinateSystem> targets = CoordinateSystems.getSingleComponents(target);
        final int count = sources.size();
        /*
         * Current implementation expects the same number of components, in the same order
         * and with the same number of dimensions in each component. A future version will
         * need to improve on that.
         */
        MathTransform result = null;
        if (count == targets.size()) {
            final int dimension = source.getDimension();
            int firstAffectedCoordinate = 0;
            for (int i=0; i<count; i++) {
                final CoordinateSystem stepSource = sources.get(i);
                final CoordinateSystem stepTarget = targets.get(i);
                final int sourceDim = stepSource.getDimension();
                if (stepTarget.getDimension() != sourceDim) {
                    result = null;
                    break;
                }
                MathTransform step;
                try {
                    step = single(stepSource, stepTarget);
                } catch (IllegalArgumentException | IncommensurableException e) {
                    throw new OperationNotFoundException(operationNotFound(stepSource, stepTarget), e);
                }
                final int numTrailingCoordinates = dimension - (firstAffectedCoordinate + sourceDim);
                step = factory.createPassThroughTransform(firstAffectedCoordinate, step, numTrailingCoordinates);
                if (result == null) {
                    result = step;
                } else {
                    result = factory.createConcatenatedTransform(result, step);
                }
                firstAffectedCoordinate += sourceDim;
            }
        }
        /*
         * If we couldn't process components separately, try with the CS as a whole.
         * It may be a `CompoundCS` (in which case we can still apply axis swapping)
         * or it may be standard CS with different number of dimensions.
         */
        if (result == null) try {
            result = single(source, target);
        } catch (IllegalArgumentException | IncommensurableException e) {
            throw new OperationNotFoundException(operationNotFound(source, target), e);
        }
        return unique(result);
    }

    /**
     * Implementation of {@code create(…)} for a single component.
     * This implementation can handle changes of coordinate system type between {@link EllipsoidalCS},
     * {@link CartesianCS}, {@link SphericalCS}, {@link CylindricalCS} and {@link PolarCS}.
     *
     * @param  stepSource  source coordinate system of the step to build.
     * @param  stepTarget  target coordinate system of the step to build.
     * @return transform between the given coordinate systems (never null in current implementation).
     * @throws IllegalArgumentException if the <abbr>CS</abbr> are not compatible, or axes do not match.
     * @throws IncommensurableException if the units are not compatible, or the conversion is non-linear.
     * @throws FactoryException if a factory method failed.
     */
    private MathTransform single(final CoordinateSystem stepSource,
                                 final CoordinateSystem stepTarget)
            throws FactoryException, IncommensurableException
    {
        /*
         * Cases that require an ellipsoid. All those cases are delegated to another operation method
         * in the transform factory. The check for axis order and unit of measurement will be done by
         * public methods of the factory, which may invoke this `CoordinateSystemTransformBuilder`
         * recursively but with a different pair of coordinate systems.
         */
        if (ellipsoid != null) {
            if (stepSource instanceof EllipsoidalCS) {
                if (stepTarget instanceof EllipsoidalCS) {
                    return addOrRemoveVertical(stepSource, stepTarget, Geographic2Dto3D.NAME, Geographic3Dto2D.NAME);
                }
                if ((stepTarget instanceof CartesianCS || stepTarget instanceof SphericalCS)) {
                    final var context = factory.builder(GeographicToGeocentric.NAME);
                    context.setSourceAxes(stepSource, ellipsoid);
                    context.setTargetAxes(stepTarget, null);
                    return delegate(context);
                }
            } else if (stepTarget instanceof EllipsoidalCS) {
                if ((stepSource instanceof CartesianCS || stepSource instanceof SphericalCS)) {
                    final var context = factory.builder(GeocentricToGeographic.NAME);
                    context.setSourceAxes(stepSource, null);
                    context.setTargetAxes(stepTarget, ellipsoid);
                    return delegate(context);
                }
            } else if (stepSource instanceof SphericalCS && stepTarget instanceof SphericalCS) {
                return addOrRemoveVertical(stepSource, stepTarget, Spherical2Dto3D.NAME, Spherical3Dto2D.NAME);
            }
        }
        /*
         * Cases that can be done without ellipsoid. Change of axis order and unit of measurement
         * needs to be done here. There is no `CoordinateSystemTransformBuilder` recursive calls.
         */
        int passthrough = 0;
        CoordinateSystemTransform kernel = null;
        if (stepSource instanceof CartesianCS) {
            if (stepTarget instanceof SphericalCS) {
                kernel = CartesianToSpherical.INSTANCE;
            } else if (stepTarget instanceof PolarCS) {
                kernel = CartesianToPolar.INSTANCE;
            } else if (stepTarget instanceof CylindricalCS) {
                kernel = CartesianToPolar.INSTANCE;
                passthrough = 1;
            }
        } else if (stepTarget instanceof CartesianCS) {
            if (stepSource instanceof SphericalCS) {
                kernel = SphericalToCartesian.INSTANCE;
            } else if (stepSource instanceof PolarCS) {
                kernel = PolarToCartesian.INSTANCE;
            } else if (stepSource instanceof CylindricalCS) {
                kernel = PolarToCartesian.INSTANCE;
                passthrough = 1;
            }
        }
        final MathTransform normalized, result;
        final OperationMethod method;
        if (kernel == null) {
            method = Affine.provider();
            result = factory.createAffineTransform(CoordinateSystems.swapAndScaleAxes(stepSource, stepTarget));
            normalized = result;
        } else {
            if (stepSource.getDimension() != kernel.getSourceDimensions() + passthrough ||
                stepTarget.getDimension() != kernel.getTargetDimensions() + passthrough)
            {
                throw new OperationNotFoundException(operationNotFound(stepSource, stepTarget));
            }
            final MathTransform before, after;
            if (passthrough == 0) {
                method     = kernel.method;
                normalized = kernel.completeTransform(factory);
            } else {
                method     = kernel.method3D;
                normalized = kernel.passthrough(factory);
            }
            /*
             * Adjust for axis order an units of measurement.
             */
            before = factory.createAffineTransform(
                    CoordinateSystems.swapAndScaleAxes(stepSource,
                    CoordinateSystems.replaceAxes(stepSource, AxesConvention.NORMALIZED)));
            after  = factory.createAffineTransform(
                    CoordinateSystems.swapAndScaleAxes(
                    CoordinateSystems.replaceAxes(stepTarget, AxesConvention.NORMALIZED), stepTarget));
            result = factory.createConcatenatedTransform(before,
                     factory.createConcatenatedTransform(normalized, after));
        }
        setParameters(normalized, method, null);
        return result;
    }

    /**
     * Adds or removes the ellipsoidal height or spherical radius dimension.
     *
     * @param  stepSource  source coordinate system of the step to build.
     * @param  stepTarget  target coordinate system of the step to build.
     * @param  add         the operation method for adding the vertical dimension.
     * @param  remove      the operation method for removing the vertical dimension.
     * @return transform adding or removing a vertical coordinate.
     * @throws IllegalArgumentException if the <abbr>CS</abbr> are not compatible, or axes do not match.
     * @throws IncommensurableException if the units are not compatible, or the conversion is non-linear.
     * @throws FactoryException if a factory method failed.
     *
     * @see org.apache.sis.referencing.internal.ParameterizedTransformBuilder#addOrRemoveVertical
     */
    private MathTransform addOrRemoveVertical(final CoordinateSystem stepSource,
                                              final CoordinateSystem stepTarget,
                                              final String add, final String remove)
            throws FactoryException, IncommensurableException
    {
        final int change = stepTarget.getDimension() - stepSource.getDimension();
        if (change != 0) {
            final String method = change < 0 ? remove : add;
            final var context = factory.builder(method);
            context.setSourceAxes(stepSource, ellipsoid);
            context.setTargetAxes(stepTarget, ellipsoid);
            return delegate(context);
        }
        // No change in the number of dimensions. Maybe there is axis swapping and unit conversions.
        MathTransform step = factory.createAffineTransform(CoordinateSystems.swapAndScaleAxes(stepSource, stepTarget));
        setParameters(step, Affine.provider(), null);
        return step;
    }

    /**
     * Delegates the transform creation to another builder, then remember the operation method which was used.
     *
     * @param  context  an initialized context on which to invoke the {@code create()} method.
     * @return result of {@code context.create()}.
     * @throws FactoryException if the given context cannot create the transform.
     */
    private MathTransform delegate(final MathTransform.Builder context) throws FactoryException {
        final MathTransform step = context.create();
        setParameters(step, context.getMethod().orElse(null), context.parameters());
        return step;
    }

    /**
     * Remembers the operation method and parameters for the given transform.
     *
     * @param  result  the transform that has been created.
     * @param  method  the method, or {@code null} if unspecified.
     * @param  values  the parameter values, or {@code null} for inferring from the method.
     */
    private void setParameters(final MathTransform result, final OperationMethod method, final ParameterValueGroup values) {
        final byte type;
        if (result.isIdentity()) {
            type = IDENTITY;
        } else if (MathTransforms.isLinear(result)) {
            type = LINEAR;
        } else {
            type = CONVERSION;
        }
        if (parametersType < type) {
            parametersType = type;
            provider = method;
            parameters= values;
            if (result instanceof Parameterized) {
                parameterized = (Parameterized) result;
            }
        }
    }

    /**
     * Returns the error message for an operation not found between the coordinate systems.
     */
    private static String operationNotFound(final CoordinateSystem stepSource,
                                            final CoordinateSystem stepTarget)
    {
        return Resources.format(Resources.Keys.CoordinateOperationNotFound_2,
                WKTUtilities.toType(CoordinateSystem.class, stepSource.getClass()),
                WKTUtilities.toType(CoordinateSystem.class, stepTarget.getClass()));
    }
}
