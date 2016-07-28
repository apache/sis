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
import org.opengis.referencing.operation.ConicProjection;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * The provider for "<cite>Albers Equal Area</cite>" projection (EPSG:9822).
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/albers_equal_area_conic.html">Albers Equal-Area Conic on RemoteSensing.org</a>
 */
@XmlTransient
public final class AlbersEqualArea extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7489679528438418778L;

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
    public static final ParameterDescriptor<Double> EASTING_AT_FALSE_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Northing at false origin</cite> (Nf) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> NORTHING_AT_FALSE_ORIGIN;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        /*
         * EPSG:    Latitude of false origin
         * OGC:     latitude_of_center
         * ESRI:    Latitude_Of_Origin
         * NetCDF:  latitude_of_projection_origin
         * GeoTIFF: NatOriginLat
         */
        LATITUDE_OF_FALSE_ORIGIN = createLatitude(
                renameAlias(LambertConformal2SP.LATITUDE_OF_FALSE_ORIGIN, Citations.GEOTIFF, Mercator1SP.LATITUDE_OF_ORIGIN, builder)
                .rename(Citations.OGC, "latitude_of_center"), true);
        /*
         * EPSG:    Longitude of false origin
         * OGC:     longitude_of_center
         * ESRI:    Central_Meridian
         * NetCDF:  longitude_of_central_meridian
         * GeoTIFF: NatOriginLong
         */
        LONGITUDE_OF_FALSE_ORIGIN = createLongitude(
                renameAlias(LambertConformal2SP.LONGITUDE_OF_FALSE_ORIGIN, Citations.GEOTIFF, Mercator1SP.LONGITUDE_OF_ORIGIN, builder)
                .rename(Citations.OGC, "longitude_of_center"));
        /*
         * EPSG:    Latitude of 1st standard parallel
         * OGC:     standard_parallel_1
         * ESRI:    Standard_Parallel_1
         * NetCDF:  standard_parallel
         * GeoTIFF: StdParallel1
         *
         * Special case: default value shall be the value of LATITUDE_OF_FALSE_ORIGIN.
         */
        STANDARD_PARALLEL_1 = LambertConformal2SP.STANDARD_PARALLEL_1;
        /*
         * EPSG:    Latitude of 2nd standard parallel
         * OGC:     standard_parallel_2
         * ESRI:    Standard_Parallel_2
         * NetCDF:  standard_parallel
         * GeoTIFF: StdParallel2
         *
         * Special case: default value shall be the value of STANDARD_PARALLEL_1.
         */
        STANDARD_PARALLEL_2 = LambertConformal2SP.STANDARD_PARALLEL_2;
        /*
         * EPSG:    Easting at false origin
         * OGC:     false_easting
         * ESRI:    False_Easting
         * NetCDF:  false_easting
         * GeoTIFF: FalseEasting
         */
        EASTING_AT_FALSE_ORIGIN = createShift(
                renameAlias(LambertConformal2SP.EASTING_AT_FALSE_ORIGIN, Citations.GEOTIFF, LambertConformal1SP.FALSE_EASTING, builder));
        /*
         * EPSG:    Northing at false origin
         * OGC:     false_northing
         * ESRI:    False_Northing
         * NetCDF:  false_northing
         * GeoTIFF: FalseNorthing
         */
        NORTHING_AT_FALSE_ORIGIN = createShift(
                renameAlias(LambertConformal2SP.NORTHING_AT_FALSE_ORIGIN, Citations.GEOTIFF, LambertConformal1SP.FALSE_NORTHING, builder));

        PARAMETERS = builder
                .addIdentifier(             "9822")
                .addName(                   "Albers Equal Area")
                .addName(Citations.OGC,     "Albers_Conic_Equal_Area")
                .addName(Citations.ESRI,    "Albers_Equal_Area_Conic")
                .addName(Citations.ESRI,    "Albers")
                .addName(Citations.NETCDF,  "AlbersEqualArea")
                .addName(Citations.GEOTIFF, "CT_AlbersEqualArea")
                .addName(Citations.PROJ4,   "aea")
                .addIdentifier(Citations.GEOTIFF, "11")
                .addIdentifier(Citations.MAP_INFO, "9")
                .addIdentifier(Citations.S57,      "1")
                .createGroupForMapProjection(
                        LATITUDE_OF_FALSE_ORIGIN,
                        LONGITUDE_OF_FALSE_ORIGIN,
                        STANDARD_PARALLEL_1,
                        STANDARD_PARALLEL_2,
                        EASTING_AT_FALSE_ORIGIN,
                        NORTHING_AT_FALSE_ORIGIN);
    }

    /**
     * Constructs a new provider.
     */
    public AlbersEqualArea() {
        super(PARAMETERS);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code ConicProjection.class}
     */
    @Override
    public final Class<ConicProjection> getOperationType() {
        return ConicProjection.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The map projection created from the given parameter values.
     */
    @Override
    protected final NormalizedProjection createProjection(final Parameters parameters) {
        return new org.apache.sis.referencing.operation.projection.AlbersEqualArea(this, parameters);
    }
}
