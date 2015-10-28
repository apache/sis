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
package org.apache.sis.internal.referencing.provider;

import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.referencing.operation.transform.EllipsoidalToCartesianTransform;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;


/**
 * The provider for <cite>"Geographic/geocentric conversions"</cite> (EPSG:9602).
 * This provider creates transforms from geographic to geocentric coordinate reference systems.
 *
 * <p>By default, this provider creates a transform from a three-dimensional ellipsoidal coordinate system,
 * which is the behavior implied in OGC's WKT. However a SIS-specific {@code "dim"} parameter allows to transform
 * from a two-dimensional ellipsoidal coordinate system instead.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class GeographicToGeocentric extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5690807111952562344L;

    /**
     * The OGC name used for this operation method. The OGC name is preferred to the EPSG name
     * because it allows to distinguish between the forward and the inverse conversion.
     */
    static final String NAME = "Ellipsoid_To_Geocentric";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
            .addIdentifier("9602")
            .addName("Geographic/geocentric conversions")
            .addName(Citations.OGC, NAME)
            .createGroupForMapProjection(AbridgedMolodensky.DIMENSION);
            // Not really a map projection, but we leverage the same axis parameters.
    }

    /**
     * The provider for the other number of dimensions (2D or 3D).
     */
    private final GeographicToGeocentric complement;

    /**
     * Constructs a provider for the 3-dimensional case.
     */
    public GeographicToGeocentric() {
        super(3, 3, PARAMETERS);
        complement = new GeographicToGeocentric(this);
    }

    /**
     * Constructs a provider for the 2-dimensional case.
     *
     * @param complement The three-dimensional case.
     */
    private GeographicToGeocentric(final GeographicToGeocentric complement) {
        super(2, 3, PARAMETERS);
        this.complement = complement;
    }

    /**
     * Returns the operation type.
     *
     * @return {@code Conversion.class}.
     */
    @Override
    public Class<Conversion> getOperationType() {
        return Conversion.class;
    }

    /**
     * Creates a transform from the specified group of parameter values.
     *
     * @param  factory The factory to use for creating the transform.
     * @param  values The parameter values that define the transform to create.
     * @return The conversion from geographic to geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        return createMathTransform(GeographicToGeocentric.class, factory, values);
    }

    /**
     * Implementation of {@link #createMathTransform(MathTransformFactory, ParameterValueGroup)}
     * shared with {@link GeocentricToGeographic}.
     */
    static MathTransform createMathTransform(final Class<?> caller, final MathTransformFactory factory,
            final ParameterValueGroup values) throws FactoryException
    {
        boolean is3D = true;
        try {
            /*
             * Set 'is3D' to false if the given parameter group contains a "DIM" parameter having value 2.
             * If the parameter value is 3 or if there is no parameter value, then 'is3D' is set to true,
             * which is consistent with the default value.
             */
            final int dimension = values.parameter("dim").intValue();
            switch (dimension) {
                case 2:  is3D = false;      break;
                case 3:  /* already true */ break;
                default: throw new InvalidParameterValueException(Errors.format(Errors.Keys.
                            IllegalArgumentValue_2, "dim", dimension), "dim", dimension);
            }
        } catch (ParameterNotFoundException e) {
            /*
             * Should never happen with the parameter descriptors provided by SIS, but could happen
             * if the user provided its own descriptor. Default to three-dimensional case.
             */
            Logging.recoverableException(Logging.getLogger(Loggers.COORDINATE_OPERATION), caller, "createMathTransform", e);
        }
        final ParameterValue<?> semiMajor = values.parameter(Constants.SEMI_MAJOR);
        final Unit<Length> unit = semiMajor.getUnit().asType(Length.class);
        return new EllipsoidalToCartesianTransform(semiMajor.doubleValue(),
                values.parameter(Constants.SEMI_MINOR).doubleValue(unit), unit, is3D)
                .createGeodeticConversion(factory);
    }

    /**
     * Returns the same operation method, but for different number of dimensions.
     *
     * @param  sourceDimensions The desired number of input dimensions.
     * @param  targetDimensions The desired number of output dimensions.
     * @return The redimensioned operation method, or {@code this} if no change is needed.
     */
    @Override
    public OperationMethod redimension(final int sourceDimensions, final int targetDimensions) {
        ArgumentChecks.ensureBetween("sourceDimensions", 2, 3, sourceDimensions);
        ArgumentChecks.ensureBetween("targetDimensions", 3, 3, targetDimensions);
        return (sourceDimensions == getSourceDimensions()) ? this : complement;
    }
}
