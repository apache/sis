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

import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Constants;


/**
 * The provider for <cite>"Geographic/geocentric conversions"</cite> (EPSG:9602).
 * This provider creates transforms from geographic to geocentric coordinate reference systems.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 *
 * @see GeocentricToGeographic
 *
 * @since 0.7
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
                .createGroupForMapProjection();
                // Not really a map projection, but we leverage the same axis parameters.
    }

    /**
     * Constructs a provider for the 3-dimensional case.
     */
    public GeographicToGeocentric() {
        super(3, 3, PARAMETERS);
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
     * If the user asked for the <cite>"Geographic/geocentric conversions"</cite> operation but the parameter types
     * suggest that (s)he intended to convert in the opposite direction, return the name of operation method to use.
     * We need this check because EPSG defines a single operation method for both {@code "Ellipsoid_To_Geocentric"}
     * and {@code "Geocentric_To_Ellipsoid"} methods.
     *
     * <p><b>Note:</b>  we do not define similar method in {@link GeocentricToGeographic} class because the only
     * way to obtain that operation method is to ask explicitely for {@code "Geocentric_To_Ellipsoid"} operation.
     * The ambiguity that we try to resolve here exists only if the user asked for the EPSG:9602 operation, which
     * is defined only in this class.</p>
     *
     * @return {@code "Geocentric_To_Ellipsoid"} if the user apparently wanted to get the inverse of this
     *         {@code "Ellipsoid_To_Geocentric"} operation, or {@code null} if none.
     */
    @Override
    public String resolveAmbiguity(final DefaultMathTransformFactory.Context context) {
        if (context.getSourceCS() instanceof CartesianCS && context.getTargetCS() instanceof EllipsoidalCS) {
            return GeocentricToGeographic.NAME;
        }
        return super.resolveAmbiguity(context);
    }

    /**
     * Notifies {@code DefaultMathTransformFactory} that Geographic/geocentric conversions
     * require values for the {@code "semi_major"} and {@code "semi_minor"} parameters.
     *
     * @return 1, meaning that the operation requires a source ellipsoid.
     */
    @Override
    public int getEllipsoidsMask() {
        return 1;
    }

    /**
     * Creates a transform from the specified group of parameter values.
     *
     * @param  factory  the factory to use for creating the transform.
     * @param  values   the parameter values that define the transform to create.
     * @return the conversion from geographic to geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        return create(factory, values);
    }

    /**
     * Implementation of {@link #createMathTransform(MathTransformFactory, ParameterValueGroup)}
     * shared with {@link GeocentricToGeographic}.
     */
    static MathTransform create(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        final ParameterValue<?> semiMajor = values.parameter(Constants.SEMI_MAJOR);
        final Unit<Length> unit = semiMajor.getUnit().asType(Length.class);
        return EllipsoidToCentricTransform.createGeodeticConversion(factory, semiMajor.doubleValue(),
                values.parameter(Constants.SEMI_MINOR).doubleValue(unit), unit, true,
                EllipsoidToCentricTransform.TargetType.CARTESIAN);
    }
}
