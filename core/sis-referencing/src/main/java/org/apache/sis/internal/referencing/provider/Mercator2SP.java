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
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for "<cite>Mercator (variant B)</cite>" projection (EPSG:9805).
 *
 * <p>This provider reuses some of the parameters defined in {@link Mercator2SP}.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/mercator_2sp.html">Mercator 2SP on RemoteSensing.org</a>
 */
public final class Mercator2SP extends AbstractMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6356028352681135786L;

    /*
     * ACCESS POLICY: Only formal EPSG parameters shall be public.
     * Parameters that we add ourselves should be package-privated.
     */

    /**
     * The operation parameter descriptor for the <cite>Latitude of 1st standard parallel</cite> (φ₁) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL = Equirectangular.STANDARD_PARALLEL;

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * In theory, this parameter should not be used and its value should be 0 in all cases.
     * This parameter is included in the EPSG dataset for completeness in CRS labelling only.
     *
     * <p>This parameter is used by {@link Mercator1SP} and is not formally a parameter of {@link Mercator2SP}
     * projections. Nevertheless we declare it is as an optional parameter because it is sometime used in Well
     * Known Text (WKT).</p>
     */
    static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN = Mercator1SP.CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the <cite>Scale factor</cite> (not necessarily at natural origin)
     * parameter value. Valid values range is (0 … ∞) and default value is 1.
     *
     * <p>This parameter is used by {@link Mercator1SP} and is not formally a parameter of {@link Mercator2SP}
     * projections. Nevertheless we declare it is as an optional parameter because it is sometime used in Well
     * Known Text (WKT). However it shall be interpreted as a <cite>Scale factor at the standard parallel</cite>
     * rather than at the natural origin.</p>
     */
    static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        /*
         * "Latitude of natural origin" and "Scale factor" are not formally parameters of the "Mercator (variant B)"
         * projection according EPSG. But we declare them as optional parameters because they are sometime used.
         * However we remove the EPSG name and identifier at least for the scale factor, because its meaning does
         * not fit well in this context. The EPSG name is "Scale factor at natural origin" while actually the scale
         * factor applied here would rather at the standard parallel.
         */
        builder.setRequired(false); // Will apply to all remaining parameters.
        LATITUDE_OF_ORIGIN = createConstant(exceptEPSG(Mercator1SP.LATITUDE_OF_ORIGIN, builder)
                .setRemarks(Mercator1SP.LATITUDE_OF_ORIGIN.getRemarks()), 0.0);

        SCALE_FACTOR = createScale(exceptEPSG(Mercator1SP.SCALE_FACTOR, builder)
                .setRemarks(notFormalParameter("Mercator (variant A)")));

        PARAMETERS = builder
            .addIdentifier(             "9805")
            .addName(                   "Mercator (variant B)")     // Starting from EPSG version 7.6
            .addName(                   "Mercator (2SP)")           // Prior to EPSG version 7.6
            .addName(Citations.OGC,     "Mercator_2SP")
            .addName(Citations.ESRI,    "Mercator")
            .addName(Citations.NETCDF,  "Mercator")
            .addName(sameNameAs(Citations.PROJ4, Mercator1SP.PARAMETERS))
            .addIdentifier(Citations.MAP_INFO, "26")    // MapInfo names this projection "Regional Mercator".
            .addIdentifier(Citations.S57,       "8")
            .createGroupForMapProjection(
                    STANDARD_PARALLEL,
                    LATITUDE_OF_ORIGIN,     // Not formally a Mercator2SP parameter.
                    CENTRAL_MERIDIAN,
                    SCALE_FACTOR,           // Not formally a Mercator2SP parameter.
                    FALSE_EASTING,
                    FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public Mercator2SP() {
        super(PARAMETERS);
    }
}
