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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.cs.AbstractCS;
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
     * An empty contextual parameter, used only for representing conversion from degrees to radians.
     */
    final transient ContextualParameters context;

    /**
     * The complete transform, including conversion between degrees and radians.
     */
    private volatile MathTransform complete;

    /**
     * Creates a new conversion between two types of coordinate system.
     * Subclasses may need to invoke {@link ContextualParameters#normalizeGeographicInputs(double)}
     * or {@link ContextualParameters#denormalizeGeographicOutputs(double)} after this constructor.
     */
    CoordinateSystemTransform(final String method) {
        final Map<String,?> properties = Collections.singletonMap(DefaultOperationMethod.NAME_KEY,
                new ImmutableIdentifier(Citations.SIS, Constants.SIS, method));
        context = new ContextualParameters(new DefaultOperationMethod(properties, 3, 3,
                new DefaultParameterDescriptorGroup(properties, 1, 1)));
    }

    /**
     * Returns the complete transform, including conversion between degrees and radians units.
     */
    final MathTransform completeTransform() throws FactoryException {
        MathTransform tr = complete;
        if (tr == null) {
            // No need to synchronize since DefaultMathTransformFactory returns unique instances.
            complete = tr = context.completeTransform(DefaultFactories.forBuildin(MathTransformFactory.class), this);
        }
        return tr;
    }

    /**
     * Returns the number of dimensions in the source coordinate points.
     * Shall be equals to {@code getSourceCS().getDimension()}.
     */
    @Override
    public final int getSourceDimensions() {
        return 3;
    }

    /**
     * Returns the number of dimensions in the target coordinate points.
     * Shall be equals to {@code getTargetCS().getDimension()}.
     */
    @Override
    public final int getTargetDimensions() {
        return 3;
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
        CoordinateSystemTransform tr = null;
        if (source instanceof CartesianCS) {
            if (target instanceof SphericalCS) {
                tr = CartesianToSpherical.INSTANCE;
            } else if (target instanceof CylindricalCS) {
                tr = CartesianToCylindrical.INSTANCE;
            }
        } else if (source instanceof SphericalCS) {
            if (target instanceof CartesianCS) {
                tr = SphericalToCartesian.INSTANCE;
            }
        } else if (source instanceof CylindricalCS) {
            if (target instanceof CartesianCS) {
                tr = CylindricalToCartesian.INSTANCE;
            }
        }
        Exception cause = null;
        try {
            if (tr == null) {
                return factory.createAffineTransform(CoordinateSystems.swapAndScaleAxes(source, target));
            } else if (tr.getSourceDimensions() == source.getDimension() &&
                       tr.getTargetDimensions() == target.getDimension())
            {
                return factory.createConcatenatedTransform(normalize(factory, source, false),
                       factory.createConcatenatedTransform(tr.completeTransform(), normalize(factory, target, true)));
            }
        } catch (IllegalArgumentException | ConversionException e) {
            cause = e;
        }
        throw new OperationNotFoundException(Errors.format(Errors.Keys.CoordinateOperationNotFound_2,
                WKTUtilities.toType(CoordinateSystem.class, source.getClass()),
                WKTUtilities.toType(CoordinateSystem.class, target.getClass())), cause);
    }

    /**
     * Returns the conversion between the given coordinate system and its normalized form.
     */
    private static MathTransform normalize(final MathTransformFactory factory, final CoordinateSystem cs,
            final boolean inverse) throws FactoryException, ConversionException
    {
        AbstractCS source = AbstractCS.castOrCopy(cs);
        AbstractCS target = source.forConvention(AxesConvention.NORMALIZED);
        if (inverse) {
            AbstractCS tmp = source;
            source = target;
            target = tmp;
        }
        return factory.createAffineTransform(CoordinateSystems.swapAndScaleAxes(source, target));
    }
}
