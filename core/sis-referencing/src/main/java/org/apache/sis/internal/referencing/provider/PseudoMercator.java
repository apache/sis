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

import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.operation.projection.Mercator;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * The provider for "<cite>Popular Visualisation Pseudo Mercator</cite>" projection (EPSG:1024).
 * This is also known as the "Google projection", defined by popular demand but not considered
 * a valid projection method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class PseudoMercator extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8126827491349984471L;

    /**
     * The name of this operation method.
     */
    public static final String NAME = "Popular Visualisation Pseudo Mercator";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        final ParameterDescriptor<?> latitudeOfOrigin = createLatitude(
                onlyEPSG(Mercator1SP.LATITUDE_OF_ORIGIN, builder), false);

        final ParameterDescriptor<?> centralMeridian = createLongitude(
                onlyEPSG(Mercator1SP.CENTRAL_MERIDIAN, builder));

        final ParameterDescriptor<?> falseEasting = createShift(
                onlyEPSG(Mercator1SP.FALSE_EASTING, builder));

        final ParameterDescriptor<?> falseNorthing = createShift(
                onlyEPSG(Mercator1SP.FALSE_NORTHING, builder));
        /*
         * The scale factor is not formally a parameter of the "Popular Visualisation Pseudo Mercator" projection
         * according EPSG. But we declare it as an optional parameters because it is sometime used.
         */
        final InternationalString remarks = notFormalParameter(Mercator1SP.NAME, "Pseudo Mercator");
        final ParameterDescriptor<?> scaleFactor = createScale(onlyEPSG(Mercator1SP.SCALE_FACTOR, builder)
                .setRemarks(remarks).setRequired(false));

        PARAMETERS = builder
            .addIdentifier("1024")
            .addName(NAME)
            .createGroupForMapProjection(
                    latitudeOfOrigin,
                    centralMeridian,
                    scaleFactor, // Not an official parameter, provided for compatibility with those who still use it.
                    falseEasting,
                    falseNorthing);
    }

    /**
     * Constructs a new provider.
     */
    public PseudoMercator() {
        super(PARAMETERS);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code CylindricalProjection.class}
     */
    @Override
    public Class<CylindricalProjection> getOperationType() {
        return CylindricalProjection.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new Mercator(this, parameters);
    }
}
