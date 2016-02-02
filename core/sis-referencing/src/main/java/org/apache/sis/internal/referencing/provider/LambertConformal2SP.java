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

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for <cite>"Lambert Conic Conformal (2SP)"</cite> projection (EPSG:9802).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/lambert_conic_conformal_2sp.html">Lambert Conic Conformal 2SP on RemoteSensing.org</a>
 */
@XmlTransient
public final class LambertConformal2SP extends AbstractLambert {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3240860802816724947L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9802";

    /**
     * The operation parameter descriptor for the <cite>Latitude of false origin</cite> (φf) parameter value.
     * Valid values range is [-90 … 90]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_FALSE_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of false origin</cite> (λf) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_FALSE_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Latitude of 1st standard parallel</cite> parameter value.
     * Valid values range is [-90 … 90]° and default value is the value given to the {@link #LATITUDE_OF_FALSE_ORIGIN}
     * parameter.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL_1;

    /**
     * The operation parameter descriptor for the <cite>Latitude of 2nd standard parallel</cite> parameter value.
     * Valid values range is [-90 … 90]° and default value is the value given to the {@link #STANDARD_PARALLEL_2}
     * parameter.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL_2;

    /**
     * The operation parameter descriptor for the <cite>Easting at false origin</cite> (Ef) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> EASTING_AT_FALSE_ORIGIN = RegionalMercator.EASTING_AT_FALSE_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Northing at false origin</cite> (Nf) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> NORTHING_AT_FALSE_ORIGIN = RegionalMercator.NORTHING_AT_FALSE_ORIGIN;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        /*
         * EPSG:    Latitude of false origin
         * OGC:     latitude_of_origin
         * ESRI:    Latitude_Of_Origin
         * NetCDF:  latitude_of_projection_origin
         * GeoTIFF: FalseOriginLat
         */
        LATITUDE_OF_FALSE_ORIGIN = createLatitude(builder
                .addNamesAndIdentifiers(RegionalMercator.LATITUDE_OF_FALSE_ORIGIN), true);
        /*
         * EPSG:    Longitude of false origin
         * OGC:     central_meridian
         * ESRI:    Central_Meridian
         * NetCDF:  longitude_of_central_meridian
         * GeoTIFF: FalseOriginLong
         */
        LONGITUDE_OF_FALSE_ORIGIN = createLongitude(exceptEPSG(LambertConformal1SP.LONGITUDE_OF_ORIGIN, builder
                .addIdentifier("8822")
                .addName("Longitude of false origin"))
                .rename(Citations.NETCDF, "longitude_of_central_meridian")
                .rename(Citations.GEOTIFF, "FalseOriginLong"));
        /*
         * EPSG:    Latitude of 1st standard parallel
         * OGC:     standard_parallel_1
         * ESRI:    Standard_Parallel_1
         * NetCDF:  standard_parallel
         * GeoTIFF: StdParallel1
         *
         * Special case: default value shall be the value of LATITUDE_OF_FALSE_ORIGIN.
         */
        STANDARD_PARALLEL_1 = createMandatoryLatitude(builder
                .addNamesAndIdentifiers(Mercator2SP.STANDARD_PARALLEL));
        /*
         * EPSG:    Latitude of 2nd standard parallel
         * OGC:     standard_parallel_2
         * ESRI:    Standard_Parallel_2
         * NetCDF:  standard_parallel
         * GeoTIFF: StdParallel2
         *
         * Special case: default value shall be the value of STANDARD_PARALLEL_1.
         */
        STANDARD_PARALLEL_2 = createMandatoryLatitude(builder
                .addIdentifier("8824")
                .addName("Latitude of 2nd standard parallel")
                .addName(Citations.OGC,     Constants.STANDARD_PARALLEL_2)
                .addName(Citations.ESRI,    "Standard_Parallel_2")
                .addName(Citations.GEOTIFF, "StdParallel2")
                .addName(Citations.PROJ4,   "lat_2"));
        /*
         * The scale factor is used by LambertConformal1SP and is not formally a parameter of LambertConformal2SP.
         * Nevertheless we declare it is as an optional parameter because it is sometime used in Well Known Text,
         * but we omit the EPSG name and identifier because its meaning does not fit well in this context.
         * The EPSG name is "Scale factor at natural origin" while actually the scale factor applied here
         * would rather be at the standard parallels.
         */
        final ParameterDescriptor<Double> scaleFactor = createScale(builder
                .addNamesAndIdentifiers(Mercator2SP.SCALE_FACTOR)
                .setRemarks(notFormalParameter("Lambert Conic Conformal (1SP)"))
                .setRequired(false).setDeprecated(true));

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER)
                .addName(                    "Lambert Conic Conformal (2SP)")
                .addName(Citations.OGC,      "Lambert_Conformal_Conic_2SP")
                .addName(Citations.ESRI,     "Lambert_Conformal_Conic")
                .addName(Citations.NETCDF,   "LambertConformal")
                .addName(Citations.GEOTIFF,  "CT_LambertConfConic_2SP")
                .addName(Citations.GEOTIFF,  "CT_LambertConfConic")
                .addName(Citations.PROJ4,    "lcc")
                .addIdentifier(Citations.GEOTIFF,  "8")
                .addIdentifier(Citations.MAP_INFO, "3")
                .addIdentifier(Citations.S57,      "6")
                .createGroupForMapProjection(
                        LATITUDE_OF_FALSE_ORIGIN,
                        LONGITUDE_OF_FALSE_ORIGIN,
                        STANDARD_PARALLEL_1,
                        STANDARD_PARALLEL_2,
                        scaleFactor,           // Not formally a LambertConformal2SP parameter.
                        EASTING_AT_FALSE_ORIGIN,
                        NORTHING_AT_FALSE_ORIGIN);
    }

    /**
     * Constructs a new provider.
     */
    public LambertConformal2SP() {
        super(PARAMETERS);
    }
}
