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
import org.opengis.referencing.operation.OperationNotFoundException;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.provider.GeocentricToGeographic;
import org.apache.sis.referencing.operation.provider.GeographicToGeocentric;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.util.resources.Errors;


/**
 * Builder of transforms between coordinate systems.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CoordinateSystemTransformBuilder extends MathTransformBuilder {
    /**
     * The source and target coordinate systems.
     */
    private CoordinateSystem source, target;

    /**
     * The ellipsoid of the source or the target.
     * Only one of the source or target should have an ellipsoid,
     * because this builder is not for datum change.
     */
    private Ellipsoid ellipsoid;

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
     */
    @Override
    public void setSourceAxes(CoordinateSystem cs, Ellipsoid ellipsoid) {
        setEllipsoid(ellipsoid);
        source = cs;
    }

    /**
     * Sets the target coordinate system.
     */
    @Override
    public void setTargetAxes(CoordinateSystem cs, Ellipsoid ellipsoid) {
        setEllipsoid(ellipsoid);
        target = cs;
    }

    /**
     * Sets the ellipsoid if it was not already set.
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
     * Unsupported operation because this builder has no parameters.
     */
    @Override
    public ParameterValueGroup parameters() {
        throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForProperty_1, "method"));
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
        if (ellipsoid != null && (count | targets.size()) == 1) {
            final boolean isEllipsoidalSource = (source instanceof EllipsoidalCS);
            if (isEllipsoidalSource != (target instanceof EllipsoidalCS)) {
                final var context = factory.builder(isEllipsoidalSource ? GeographicToGeocentric.NAME
                                                                        : GeocentricToGeographic.NAME);
                context.setSourceAxes(source, isEllipsoidalSource? ellipsoid : null);
                context.setTargetAxes(target, isEllipsoidalSource ? null : ellipsoid);
                return context.create();
            }
        }
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
                final CoordinateSystem s = sources.get(i);
                final CoordinateSystem t = targets.get(i);
                final int sd = s.getDimension();
                if (t.getDimension() != sd) {
                    result = null;
                    break;
                }
                final MathTransform subTransform = factory.createPassThroughTransform(
                        firstAffectedCoordinate,
                        single(s, t),
                        dimension - (firstAffectedCoordinate + sd));
                if (result == null) {
                    result = subTransform;
                } else {
                    result = factory.createConcatenatedTransform(result, subTransform);
                }
                firstAffectedCoordinate += sd;
            }
        }
        // If we couldn't process components separately, try with the compound CS as a whole.
        if (result == null) {
            result = single(source, target);
        }
        return unique(result);
    }

    /**
     * Implementation of {@code create(…)} for a single component.
     * This implementation can handle changes of coordinate system type between
     * {@link CartesianCS}, {@link SphericalCS}, {@link CylindricalCS} and {@link PolarCS}.
     */
    private MathTransform single(final CoordinateSystem stepSource,
                                 final CoordinateSystem stepTarget) throws FactoryException
    {
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
        Exception cause = null;
        try {
            if (kernel == null) {
                return factory.createAffineTransform(CoordinateSystems.swapAndScaleAxes(stepSource, stepTarget));
            } else if (stepSource.getDimension() == kernel.getSourceDimensions() + passthrough &&
                       stepTarget.getDimension() == kernel.getTargetDimensions() + passthrough)
            {
                final MathTransform tr = (passthrough == 0)
                        ? kernel.completeTransform(factory)
                        : kernel.passthrough(factory);
                final MathTransform before = factory.createAffineTransform(
                        CoordinateSystems.swapAndScaleAxes(stepSource,
                        CoordinateSystems.replaceAxes(stepSource, AxesConvention.NORMALIZED)));
                final MathTransform after  = factory.createAffineTransform(
                        CoordinateSystems.swapAndScaleAxes(
                        CoordinateSystems.replaceAxes(stepTarget, AxesConvention.NORMALIZED), stepTarget));
                final MathTransform result = factory.createConcatenatedTransform(before,
                                             factory.createConcatenatedTransform(tr, after));
                provider = (passthrough == 0 ? kernel.method : kernel.method3D);
                return result;
            }
        } catch (IllegalArgumentException | IncommensurableException e) {
            cause = e;
        }
        throw new OperationNotFoundException(Resources.format(Resources.Keys.CoordinateOperationNotFound_2,
                WKTUtilities.toType(CoordinateSystem.class, stepSource.getClass()),
                WKTUtilities.toType(CoordinateSystem.class, stepTarget.getClass())), cause);
    }
}
