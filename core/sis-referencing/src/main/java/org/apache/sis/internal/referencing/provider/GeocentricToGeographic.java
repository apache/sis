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


/**
 * The provider for the <strong>inverse</strong> of "<cite>Geographic/geocentric conversions</cite>" (EPSG:9602).
 * This provider creates transforms from geocentric to geographic coordinate reference systems.
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
                .createGroupForMapProjection();
                // Not really a map projection, but we leverage the same axis parameters.
    }

    /**
     * Constructs a provider for the 3-dimensional case.
     */
    public GeocentricToGeographic() {
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
        MathTransform tr = GeographicToGeocentric.create(factory, values);
        try {
            tr = tr.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                  // Should never happen with SIS implementation.
        }
        return tr;
    }
}
