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
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.util.FactoryException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for the <strong>inverse</strong> of "<cite>Geographic/geocentric conversions</cite>" (EPSG:9602).
 * This provider creates transforms from geocentric to geographic coordinate reference systems.
 *
 * <p>By default, this provider creates a transform to a three-dimensional ellipsoidal coordinate system,
 * which is the behavior implied in OGC's WKT. However a SIS-specific {@code "dim"} parameter allows to
 * transform to a two-dimensional ellipsoidal coordinate system instead.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see GeographicToGeocentric
 */
public final class GeocentricToGeographic extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8459294628751497567L;

    /**
     * The OGC name used for this operation method.
     */
    static final String NAME = "Geocentric_To_Ellipsoid";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addName(Citations.OGC, NAME)
                .createGroupForMapProjection(GeocentricAffineBetweenGeographic.DIMENSION);
                // Not really a map projection, but we leverage the same axis parameters.
    }

    /**
     * The provider for the other number of dimensions (2D or 3D).
     */
    private final GeocentricToGeographic redimensioned;

    /**
     * Constructs a provider for the 3-dimensional case.
     */
    public GeocentricToGeographic() {
        super(3, 3, PARAMETERS);
        redimensioned = new GeocentricToGeographic(this);
    }

    /**
     * Constructs a provider for the 2-dimensional case.
     *
     * @param redimensioned The three-dimensional case.
     */
    private GeocentricToGeographic(final GeocentricToGeographic redimensioned) {
        super(3, 2, PARAMETERS);
        this.redimensioned = redimensioned;
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
     * @return The conversion from geocentric to geographic coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        MathTransform tr = GeographicToGeocentric.createMathTransform(GeocentricToGeographic.class, factory, values);
        try {
            tr = tr.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);
        }
        return tr;
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
        ArgumentChecks.ensureBetween("sourceDimensions", 3, 3, sourceDimensions);
        ArgumentChecks.ensureBetween("targetDimensions", 2, 3, targetDimensions);
        return (targetDimensions == getTargetDimensions()) ? this : redimensioned;
    }
}
