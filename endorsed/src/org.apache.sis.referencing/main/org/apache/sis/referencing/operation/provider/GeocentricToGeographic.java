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

import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.util.FactoryException;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for the <strong>inverse</strong> of <q>Geographic/geocentric conversions</q> (EPSG:9602).
 * This provider creates transforms from geocentric to geographic coordinate reference systems.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
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
    public static final String NAME = "Geocentric_To_Ellipsoid";

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
     * Creates a new provider.
     */
    public GeocentricToGeographic() {
        super(Conversion.class, PARAMETERS,
              CartesianCS.class,  false,    // Type expected in WKT, but not the only type accepted by this operation.
              EllipsoidalCS.class, true,
              (byte) 3);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     *
     * @param  context  the parameter values together with its context.
     * @return the conversion from geocentric to geographic coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws FactoryException {
        MathTransform tr = GeographicToGeocentric.create(context, context.getTargetDimensions(), context.getSourceCSType());
        try {
            tr = tr.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                  // Should never happen with SIS implementation.
        }
        return tr;
    }
}
