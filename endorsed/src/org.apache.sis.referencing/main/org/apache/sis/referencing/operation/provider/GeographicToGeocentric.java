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
package org.apache.sis.referencing.operation.provider;

import java.util.OptionalInt;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;


/**
 * The provider for <q>Geographic/geocentric conversions</q> (EPSG:9602).
 * This provider creates transforms from geographic to geocentric coordinate reference systems.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see GeocentricToGeographic
 */
public final class GeographicToGeocentric extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5690807111952562344L;

    /**
     * The OGC name used for this operation method. The OGC name is preferred to the EPSG name in Apache SIS
     * implementation because it allows to distinguish between the forward and the inverse conversion.
     */
    public static final String NAME = "Ellipsoid_To_Geocentric";

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
     * Creates a new provider.
     */
    public GeographicToGeocentric() {
        super(Conversion.class, PARAMETERS,
              EllipsoidalCS.class, true,
              CartesianCS.class, false,     // Type expected in WKT, but not the only type accepted by this operation.
              (byte) 2);
    }

    /**
     * If the user asked for the <q>Geographic/geocentric conversions</q> operation but the parameter types
     * suggest that (s)he intended to convert in the opposite direction, returns the name of operation method to use.
     * We need this check because EPSG defines a single operation method for both {@code "Ellipsoid_To_Geocentric"}
     * and {@code "Geocentric_To_Ellipsoid"} methods.
     *
     * <p><b>Note:</b> we do not define similar method in {@link GeocentricToGeographic} class because the only
     * way to obtain that operation method is to ask explicitly for {@code "Geocentric_To_Ellipsoid"} operation.
     * The ambiguity that we try to resolve here exists only if the user asked for the EPSG:9602 operation,
     * which is defined only in this class.</p>
     *
     * @return {@code "Geocentric_To_Ellipsoid"} if the user apparently wanted to get the inverse of this
     *         {@code "Ellipsoid_To_Geocentric"} operation, or {@code null} if none.
     */
    @Override
    public String resolveAmbiguity(final Context context) {
        if (CartesianCS.class.isAssignableFrom(context.getSourceCSType()) &&
            EllipsoidalCS.class.isAssignableFrom(context.getTargetCSType()))
        {
            return GeocentricToGeographic.NAME;
        }
        return super.resolveAmbiguity(context);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     *
     * @param  context  the parameter values together with its context.
     * @return the conversion from geographic to geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws FactoryException {
        return create(context, context.getSourceDimensions(), context.getTargetCSType());
    }

    /**
     * Implementation of {@link #createMathTransform(Context)} shared with {@link GeocentricToGeographic}.
     * This method creates the "geographic to geocentric" operation. Callers is responsible to invert the
     * transform if the "geocentric to geographic" operation is desired.
     *
     * @param  context     the parameter values together with its context.
     * @param  dimension   the number of dimension of the geographic <abbr>CRS</abbr>.
     * @param  geocentric  the coordinate system type of the geocentric <abbr>CRS</abbr>.
     * @return the conversion from geographic to geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    static MathTransform create(final Context context, final OptionalInt dimension,
                                final Class<? extends CoordinateSystem> geocentric)
            throws FactoryException
    {
        final Parameters values = Parameters.castOrWrap(context.getCompletedParameters());
        final EllipsoidToCentricTransform.TargetType type;
        if (geocentric == CoordinateSystem.class) {
            type = EllipsoidToCentricTransform.TargetType.CARTESIAN;    // Default value.
        } else try {
            type = EllipsoidToCentricTransform.TargetType.of(geocentric);
        } catch (IllegalArgumentException e) {
            throw new InvalidGeodeticParameterException(
                    Resources.format(Resources.Keys.IncompatibleCoordinateSystemTypes), e);
        }
        return EllipsoidToCentricTransform.createGeodeticConversion(context.getFactory(),
                MapProjection.getEllipsoid(values, context), dimension.orElse(3) >= 3, type);
    }
}
