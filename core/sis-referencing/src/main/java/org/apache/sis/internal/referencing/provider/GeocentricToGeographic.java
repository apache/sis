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
import org.opengis.util.FactoryException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;


/**
 * The provider for the <strong>inverse</strong> of "<cite>Geographic/geocentric conversions</cite>" (EPSG:9602).
 * This provider creates transforms from geocentric to geographic coordinate reference systems.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 *
 * @see GeographicToGeocentric
 *
 * @since 0.7
 * @module
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
     * Constructs a provider for the 3-dimensional case.
     */
    public GeocentricToGeographic() {
        this(3, new GeocentricToGeographic[4]);
        redimensioned[2] = new GeocentricToGeographic(2, redimensioned);
        redimensioned[3] = this;
    }

    /**
     * Constructs a provider for the given dimensions.
     *
     * @param targetDimensions  number of dimensions in the target CRS of this operation method.
     * @param redimensioned     providers for all combinations between 2D and 3D cases.
     */
    private GeocentricToGeographic(int targetDimensions, GeodeticOperation[] redimensioned) {
        super(3, targetDimensions, PARAMETERS, redimensioned);
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
     * Notifies {@code DefaultMathTransformFactory} that Geographic/geocentric conversions
     * require values for the {@code "semi_major"} and {@code "semi_minor"} parameters.
     *
     * @return 2, meaning that the operation requires a target ellipsoid.
     */
    @Override
    public int getEllipsoidsMask() {
        return 2;
    }

    /**
     * Specifies that the inverse of this operation is a different kind of operation.
     *
     * @return {@code false}.
     */
    @Override
    public boolean isInvertible() {
        return false;
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
