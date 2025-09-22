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
package org.apache.sis.util.privy;


/**
 * Hard coded values (typically identifiers).
 * The set of constants defined in this class may change in any SIS version - do not rely on them.
 *
 * <h2>When to use</h2>
 * Those constants should be used mostly for names, aliases or identifiers. They should generally
 * not be used for abbreviations for instance, even if the abbreviation result in the same string.
 *
 * Those constants do not need to be used systematically in tests neither, especially when the test
 * creates itself the instance to be tested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Constants {
    /**
     * The default separator, which is {@code ':'}.
     * The separator is inserted between the code space and the code in identifiers.
     *
     * @see org.apache.sis.util.iso.DefaultNameSpace#DEFAULT_SEPARATOR
     */
    public static final char DEFAULT_SEPARATOR = ':';

    /**
     * The default indentation value to use in various text formats (both WKT and XML).
     * We use a small value (2 instead of 4) because OGC's XML are very verbose.
     *
     * @see org.apache.sis.setup.OptionKey#INDENTATION
     */
    public static final byte DEFAULT_INDENTATION = 2;

    /**
     * The length of a day in number of seconds.
     * Can be cast to {@code float} with exact precision.
     */
    public static final int SECONDS_PER_DAY = 24*60*60;

    /**
     * The length of a day in number of milliseconds.
     * Can be cast to {@code float} with exact precision.
     */
    public static final int MILLISECONDS_PER_DAY = SECONDS_PER_DAY * 1000;

    /**
     * The length of a day in number of nanoseconds.
     */
    public static final long NANOSECONDS_PER_DAY = MILLISECONDS_PER_DAY * (long) 1_000_000;

    /**
     * Number of milliseconds in one second.
     * Can be cast to {@code float} with exact precision.
     */
    public static final int MILLIS_PER_SECOND = 1000;

    /**
     * Number of nanoseconds in one millisecond.
     * Can be cast to {@code float} with exact precision.
     */
    public static final int NANOS_PER_MILLISECOND = 1000_000;

    /**
     * Number of nanoseconds in one second.
     * Can be cast to {@code float} with exact precision.
     */
    public static final int NANOS_PER_SECOND = 1000_000_000;

    /**
     * Length of a year as defined by the International Union of Geological Sciences (IUGS), in milliseconds.
     * This is the unit of measurement used in EPSG geodetic dataset (EPSG:1029).
     */
    public static final long MILLIS_PER_TROPICAL_YEAR = 31556925445L;

    /**
     * The {@value} timezone ID.
     *
     * @see java.time.ZoneOffset#UTC
     */
    public static final String UTC = "UTC";

    /**
     * The {@value} protocol.
     */
    public static final String HTTP = "http", HTTPS = "https", JDBC = "jdbc";

    /**
     * The {@value} code space.
     */
    public static final String GEOTIFF = "GeoTIFF";

    /**
     * The {@value} code space.
     */
    public static final String UCUM = "UCUM";

    /**
     * The {@value} code space.
     */
    public static final String ESRI = "ESRI";

    /**
     * The {@value} code space.
     */
    public static final String EPSG = "EPSG";

    /**
     * The {@value} authority, which is the maintainer of the {@link #EPSG} database.
     * Used as the EPSG authority, while EPSG is used as the code space.
     */
    public static final String IOGP = "IOGP";

    /**
     * The {@value} authority and code space.
     */
    public static final String OGC = "OGC";

    /**
     * The {@value} code space.
     */
    public static final String SIS = "SIS";

    /**
     * The {@value} code space.
     */
    public static final String GDAL = "GDAL";

    /**
     * The {@value} code space. Uses upper case "N" in the assumption that this name is used
     * in the beginning of sentences. Otherwise, the <abbr>UCAR</abbr> usage is "netCDF".
     */
    public static final String NETCDF = "NetCDF";

    /**
     * The {@value} code space. The project name is {@code "Proj.4"}, but this constant omits
     * the dot because this name is used as a code space and we want to avoid risk of confusion.
     * We use "Proj4" instead of "PROJ" for historical reasons, because we use this identifier
     * for parameters defined by old PROJ versions, before PROJ 6 introduced full support of EPSG names.
     */
    public static final String PROJ4 = "Proj4";

    /**
     * The {@value} code space.
     */
    public static final String CRS = "CRS";

    /**
     * The {@code CRS:27} identifier for a coordinate reference system.
     */
    public static final byte CRS27 = 27;

    /**
     * The {@code CRS:83} identifier for a coordinate reference system.
     */
    public static final byte CRS83 = 83;

    /**
     * The {@code CRS:84} identifier for a coordinate reference system.
     */
    public static final byte CRS84 = 84;

    /**
     * The {@code CRS:88} identifier for a coordinate reference system.
     */
    public static final byte CRS88 = 88;

    /**
     * The {@code CRS:1} identifier for a coordinate reference system.
     */
    public static final byte CRS1 = 1;

    /**
     * Name of a SIS-specific parameter for setting the number of dimensions of a coordinate operation.
     * This constant should be used only in the context of coordinate operations, not in other contexts
     * (e.g. not for the netCDF attribute of the same name).
     */
    public static final String DIM = "dim";

    /**
     * The netCDF parameter name for the Earth radius.
     */
    public static final String EARTH_RADIUS = "earth_radius";

    /**
     * Name of the {@value} projection parameter, which is handled specially during WKT formatting.
     */
    public static final String SEMI_MAJOR = "semi_major",
                               SEMI_MINOR = "semi_minor";

    /**
     * The netCDF parameter name for inverse flattening, and whether that parameter is definitive.
     * The latter is specific to SIS.
     */
    public static final String INVERSE_FLATTENING = "inverse_flattening",
                               IS_IVF_DEFINITIVE  = "is_ivf_definitive";

    /**
     * The OGC parameter name for the central meridian.
     */
    public static final String CENTRAL_MERIDIAN = "central_meridian";

    /**
     * The OGC parameter name for the latitude of origin.
     */
    public static final String LATITUDE_OF_ORIGIN = "latitude_of_origin";

    /**
     * The netCDF parameter name for the standard parallels.
     */
    public static final String STANDARD_PARALLEL = "standard_parallel";

    /**
     * The OGC parameter name for the standard parallels.
     */
    public static final String STANDARD_PARALLEL_1 = "standard_parallel_1",
                               STANDARD_PARALLEL_2 = "standard_parallel_2";

    /**
     * Name of the pseudo-method for change of coordinate system (specific to Apache SIS).
     */
    public static final String COORDINATE_SYSTEM_CONVERSION = "Coordinate system conversion";

    /**
     * The OGC parameter name for the scale factor.
     * While Apache SIS uses EPSG names when possible, the OGC names are convenient in this case
     * because they do not depend on the projection. For example, EPSG has at least three different
     * names for the scale factor, depending on the projection:
     *
     * <ul>
     *   <li><cite>Scale factor at natural origin</cite></li>
     *   <li><cite>Scale factor at projection centre</cite></li>
     *   <li><cite>Scale factor on initial line</cite></li>
     *   <li><cite>Scale factor on pseudo standard parallel</cite></li>
     * </ul>
     *
     * Usage of OGC names avoid the need to choose a name according the projection.
     */
    public static final String SCALE_FACTOR = "scale_factor";

    /**
     * The OGC parameter name for the false easting or northing.
     * While Apache SIS uses EPSG names when possible, the OGC names are convenient in this case
     * because they do not depend on the projection. For example, EPSG has at least two different
     * names for false northing, depending on the projection:
     *
     * <ul>
     *   <li><cite>Northing at false origin</cite></li>
     *   <li><cite>Northing at projection centre</cite></li>
     * </ul>
     *
     * Usage of OGC names avoid the need to choose a name according the projection.
     */
    public static final String FALSE_EASTING  = "false_easting",
                               FALSE_NORTHING = "false_northing";

    /**
     * Name of the {@value} matrix parameters.
     */
    public static final String NUM_ROW = "num_row",
                               NUM_COL = "num_col";

    /**
     * The OGC name for <q>Affine parametric transformation</q>.
     *
     * @see org.apache.sis.referencing.operation.provider.Affine#NAME
     */
    public static final String AFFINE = "Affine";

    /**
     * EPSG code of the {@code A0} coefficient used in affine (general parametric) and polynomial transformations.
     * Codes for parameters {@code A1} to {@code A8} inclusive follow, but the affine coefficients stop at {@code A2}.
     */
    public static final short EPSG_A0 = 8623;

    /**
     * EPSG code of the {@code B0} coefficient used in affine (general parametric) and polynomial transformations.
     * Codes for parameters {@code B1} to {@code B3} inclusive follow, but the affine coefficients stop at {@code B2}.
     */
    public static final short EPSG_B0 = 8639;

    /**
     * The EPSG code for metres.
     */
    public static final short EPSG_METRE = 9001;

    /**
     * The EPSG code for degrees when used in parameters.
     */
    public static final short EPSG_PARAM_DEGREES = 9102;

    /**
     * The EPSG code for degrees when used in axes.
     */
    public static final short EPSG_AXIS_DEGREES = 9122;

    /**
     * The EPSG code for the Greenwich prime meridian.
     */
    public static final short EPSG_GREENWICH = 8901;

    /**
     * EPSG code of "WGS 84 / Arctic Polar Stereographic" projection.
     * Latitude of standard parallel is 71°N. All other parameters are zero.
     */
    public static final short EPSG_ARCTIC_POLAR_STEREOGRAPHIC = 3995;

    /**
     * EPSG code of "WGS 84 / Antarctic Polar Stereographic" projection.
     * Latitude of standard parallel is 71°S. All other parameters are zero.
     */
    public static final short EPSG_ANTARCTIC_POLAR_STEREOGRAPHIC = 3031;

    /**
     * Do not allow instantiation of this class.
     */
    private Constants() {
    }
}
