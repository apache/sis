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

import java.util.Map;
import java.util.Collections;
import javax.measure.converter.ConversionException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CylindricalCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of conversions between coordinate systems.
 * Each subclasses should have a singleton instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
abstract class CoordinateSystemTransform extends AbstractMathTransform {
    /**
     * Number of input and output dimensions.
     */
    private final int dimension;

    /**
     * An empty contextual parameter, used only for representing conversion from degrees to radians.
     */
    final transient ContextualParameters context;

    /**
     * The complete transform, including conversion between degrees and radians.
     *
     * @see #completeTransform(MathTransformFactory)
     */
    private transient volatile MathTransform complete;

    /**
     * The {@link #complete} transform in a {@link PassThroughTransform} with a 1 trailing ordinate.
     * This is used for supporting the cylindrical case on top the polar case.
     *
     * @see #passthrough(MathTransformFactory)
     */
    private transient volatile MathTransform passthrough;

    /**
     * Creates a new conversion between two types of coordinate system.
     * Subclasses may need to invoke {@link ContextualParameters#normalizeGeographicInputs(double)}
     * or {@link ContextualParameters#denormalizeGeographicOutputs(double)} after this constructor.
     */
    CoordinateSystemTransform(final String method, final int dimension) {
        this.dimension = dimension;
        final Map<String,?> properties = Collections.singletonMap(DefaultOperationMethod.NAME_KEY,
                new ImmutableIdentifier(Citations.SIS, Constants.SIS, method));
        context = new ContextualParameters(new DefaultOperationMethod(properties, dimension, dimension,
                new DefaultParameterDescriptorGroup(properties, 1, 1)));
    }

    /**
     * Returns the complete transform, including conversion between degrees and radians units.
     */
    final MathTransform completeTransform(final MathTransformFactory factory) throws FactoryException {
        MathTransform tr = complete;
        if (tr == null) {
            tr = context.completeTransform(factory, this);
            if (DefaultFactories.isDefaultInstance(MathTransformFactory.class, factory)) {
                // No need to synchronize since DefaultMathTransformFactory returns unique instances.
                complete = tr;
            }
        }
        return tr;
    }

    /**
     * Returns the cylindrical, including conversion between degrees and radians units.
     * This method is legal only for {@link PolarToCartesian} or {@link CartesianToPolar}.
     */
    final MathTransform passthrough(final MathTransformFactory factory) throws FactoryException {
        MathTransform tr = passthrough;
        if (tr == null) {
            tr = factory.createPassThroughTransform(0, completeTransform(factory), 1);
            if (DefaultFactories.isDefaultInstance(MathTransformFactory.class, factory)) {
                // No need to synchronize since DefaultMathTransformFactory returns unique instances.
                passthrough = tr;
            }
        }
        return tr;
    }

    /**
     * Returns the number of dimensions in the source coordinate points.
     * Shall be equals to {@code getSourceCS().getDimension()}.
     */
    @Override
    public final int getSourceDimensions() {
        return dimension;
    }

    /**
     * Returns the number of dimensions in the target coordinate points.
     * Shall be equals to {@code getTargetCS().getDimension()}.
     */
    @Override
    public final int getTargetDimensions() {
        return dimension;
    }

    /**
     * Returns the empty set of parameter values.
     */
    @Override
    public final ParameterValueGroup getParameterValues() {
        return context;
    }

    /**
     * Returns the contextual parameters. This is used for telling to the Well Known Text (WKT) formatter that this
     * {@code CoordinateSystemTransform} transform is usually preceeded or followed by a conversion between degree
     * and radian units of measurement.
     */
    @Override
    protected final ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Implementation of {@link DefaultMathTransformFactory#createCoordinateSystemChange(CoordinateSystem, CoordinateSystem)},
     * defined here for reducing the {@code DefaultMathTransformFactory} weight in the common case where the conversions
     * handled by this class are not needed.
     */
    static MathTransform create(final MathTransformFactory factory, final CoordinateSystem source,
            final CoordinateSystem target) throws FactoryException
    {
        int passthrough = 0;
        CoordinateSystemTransform kernel = null;
        if (source instanceof CartesianCS) {
            if (target instanceof SphericalCS) {
                kernel = CartesianToSpherical.INSTANCE;
            } else if (target instanceof PolarCS) {
                kernel = CartesianToPolar.INSTANCE;
            } else if (target instanceof CylindricalCS) {
                kernel = CartesianToPolar.INSTANCE;
                passthrough = 1;
            }
        } else if (target instanceof CartesianCS) {
            if (source instanceof SphericalCS) {
                kernel = SphericalToCartesian.INSTANCE;
            } else if (source instanceof PolarCS) {
                kernel = PolarToCartesian.INSTANCE;
            } else if (source instanceof CylindricalCS) {
                kernel = PolarToCartesian.INSTANCE;
                passthrough = 1;
            }
        }
        Exception cause = null;
        try {
            if (kernel == null) {
                return factory.createAffineTransform(CoordinateSystems.swapAndScaleAxes(source, target));
            } else if (source.getDimension() == kernel.getSourceDimensions() + passthrough &&
                       target.getDimension() == kernel.getTargetDimensions() + passthrough)
            {
                final MathTransform tr = (passthrough == 0)
                        ? kernel.completeTransform(factory)
                        : kernel.passthrough(factory);
                final MathTransform before = factory.createAffineTransform(
                        CoordinateSystems.swapAndScaleAxes(source,
                        CoordinateSystems.replaceAxes(source, AxesConvention.NORMALIZED)));
                final MathTransform after = factory.createAffineTransform(
                        CoordinateSystems.swapAndScaleAxes(
                        CoordinateSystems.replaceAxes(target, AxesConvention.NORMALIZED), target));
                return factory.createConcatenatedTransform(before,
                       factory.createConcatenatedTransform(tr, after));
            }
        } catch (IllegalArgumentException e) {
            cause = e;
        } catch (ConversionException e) {
            cause = e;
        }
        throw new OperationNotFoundException(Errors.format(Errors.Keys.CoordinateOperationNotFound_2,
                WKTUtilities.toType(CoordinateSystem.class, source.getClass()),
                WKTUtilities.toType(CoordinateSystem.class, target.getClass())), cause);
    }
}
