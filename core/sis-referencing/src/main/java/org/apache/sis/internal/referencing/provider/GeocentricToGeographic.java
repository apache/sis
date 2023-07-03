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

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.util.FactoryException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;


/**
 * The provider for the <strong>inverse</strong> of <cite>"Geographic/geocentric conversions"</cite> (EPSG:9602).
 * This provider creates transforms from geocentric to geographic coordinate reference systems.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 *
 * @see GeographicToGeocentric
 *
 * @since 0.7
 */
public final class GeocentricToGeographic extends GeodeticOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8459294628751497567L;

    /**
     * The OGC name used for this operation method.
     */
    public static final String NAME = "Geocentric_To_Ellipsoid";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addName(Citations.OGC, NAME)
                .createGroupForMapProjection(GeographicToGeocentric.DIMENSION);
                // Not really a map projection, but we leverage the same axis parameters.
    }

    /**
     * The providers for all combinations between 2D and 3D cases.
     */
    private static final GeocentricToGeographic[] REDIMENSIONED = new GeocentricToGeographic[4];
    static {
        REDIMENSIONED[2] = new GeocentricToGeographic(2);     // 3D to 2D.
        REDIMENSIONED[3] = new GeocentricToGeographic(3);
    }

    /**
     * Returns the provider for the specified combination of source and target dimensions.
     */
    @Override
    final GeodeticOperation redimensioned(int indexOfDim) {
        return REDIMENSIONED[indexOfDim];
    }

    /**
     * Creates a copy of this provider.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    public GeocentricToGeographic() {
        super(REDIMENSIONED[INDEX_OF_3D]);
    }

    /**
     * Constructs a provider for the given dimensions.
     *
     * @param indexOfDim  number of dimensions as the index in {@link #redimensioned} array.
     */
    private GeocentricToGeographic(final int indexOfDim) {
        super(Conversion.class, PARAMETERS, indexOfDim,
              CartesianCS.class,   false,
              EllipsoidalCS.class, true);
    }

    /**
     * Specifies that the inverse of this operation is a different kind of operation.
     *
     * @return {@code null}.
     */
    @Override
    public AbstractProvider inverse() {
        return null;
    }

    /**
     * Creates a transform from the specified group of parameter values.
     *
     * @param  factory  the factory to use for creating the transform.
     * @param  values   the parameter values that define the transform to create.
     * @return the conversion from geocentric to geographic coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        MathTransform tr = GeographicToGeocentric.create(factory, Parameters.castOrWrap(values));
        try {
            tr = tr.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                  // Should never happen with SIS implementation.
        }
        return tr;
    }
}
