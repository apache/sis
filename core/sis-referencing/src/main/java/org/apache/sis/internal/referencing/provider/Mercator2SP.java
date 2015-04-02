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

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.projection.Mercator;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * The provider for "<cite>Mercator (variant B)</cite>" projection (EPSG:9805).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/mercator_2sp.html">Mercator 2SP on RemoteSensing.org</a>
 */
public final class Mercator2SP extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6356028352681135786L;

    /**
     * The operation parameter descriptor for the <cite>Latitude of 1st standard parallel</cite> (φ₁) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL;

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     *
     * <p>This parameter is taken from {@link Mercator1SP}. It is not formally a parameter from the {@code Mercator2SP}
     * projection. Nevertheless we take is as an optional parameter because it is sometime used in Well Known Text.</p>
     *
     * <p>In theory, this parameter should not be used and its value should be 0 in all cases.
     * This parameter is included in the EPSG dataset for completeness in CRS labelling only.</p>
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the <cite>Scale factor at natural origin</cite> (k₀) parameter value.
     * Valid values range is (0 … ∞) and default value is 1.
     *
     * <p>This parameter is taken from {@link Mercator1SP}. It is not formally a parameter from the {@code Mercator2SP}
     * projection. Nevertheless we take is as an optional parameter because it is sometime used in Well Known Text.</p>
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING = EquidistantCylindrical.FALSE_EASTING;

    /**
     * The operation parameter descriptor for the <cite>False northing</cite> (FN) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING = EquidistantCylindrical.FALSE_NORTHING;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        STANDARD_PARALLEL = createLatitude(builder.addNamesAndIdentifiers(EquidistantCylindrical.STANDARD_PARALLEL)
                .replaceNames(Citations.GEOTIFF, "StdParallel1")
                .replaceNames(Citations.PROJ4,   "lat_1"), false);

        CENTRAL_MERIDIAN = createLongitude(builder.addNamesAndIdentifiers(EquidistantCylindrical.CENTRAL_MERIDIAN)
                .replaceNames(Citations.GEOTIFF, "NatOriginLong"));

        /*
         * "Latitude of natural origin" and "Scale factor" are not formally parameters
         * of the "Mercator (variant B)" projection according EPSG. But we declare them
         * as optional parameters because they are sometime used.
         */
        builder.setRequired(false); // Will apply to all remaining parameters.
        LATITUDE_OF_ORIGIN = createConstant(builder.addNamesAndIdentifiers(EquidistantCylindrical.LATITUDE_OF_ORIGIN)
                .replaceNames(Citations.GEOTIFF, "NatOriginLat")
                .setRemarks(EquidistantCylindrical.LATITUDE_OF_ORIGIN.getRemarks()), 0.0);

        SCALE_FACTOR = createScale(builder
                .addIdentifier("8805")
                .addName("Scale factor at natural origin")
                .addName(Citations.OGC,     "scale_factor")
                .addName(Citations.ESRI,    "Scale_Factor")
                .addName(Citations.NETCDF,  "scale_factor_at_projection_origin")
                .addName(Citations.GEOTIFF, "ScaleAtNatOrigin")
                .addName(Citations.PROJ4,   "k")
                .setRemarks(notFormalParameter(Mercator1SP.NAME, "Mercator (variant B)")));

        PARAMETERS = builder
            .addIdentifier(             "9805")
            .addName(                   "Mercator (variant B)")     // Starting from EPSG version 7.6
            .addName(                   "Mercator (2SP)")           // Prior to EPSG version 7.6
            .addName(Citations.OGC,     "Mercator_2SP")
            .addName(Citations.ESRI,    "Mercator")
            .addName(Citations.NETCDF,  "Mercator")
            .addName(Citations.GEOTIFF, "CT_Mercator")
            .addName(Citations.PROJ4,   "merc")
            .addIdentifier(Citations.MAP_INFO, "26")    // MapInfo names this projection "Regional Mercator".
            .addIdentifier(Citations.S57,       "8")
            .createGroupForMapProjection(
                    STANDARD_PARALLEL,
                    LATITUDE_OF_ORIGIN,
                    CENTRAL_MERIDIAN,
                    SCALE_FACTOR,
                    FALSE_EASTING,
                    FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public Mercator2SP() {
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
