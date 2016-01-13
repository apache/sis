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
package org.apache.sis.internal.util;

import org.apache.sis.util.Static;


/**
 * Hard coded values (typically identifiers).
 * The set of constants defined in this class may change in any SIS version - do not rely on them.
 *
 * <div class="section">When to use</div>
 * Those constants should be used mostly for names, aliases or identifiers. They should generally
 * not be used for abbreviations for instance, even if the abbreviation result in the same string.
 *
 * Those constants do not need to be used systematically in tests neither, especially when the test
 * creates itself the instance to be tested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
public final class Constants extends Static {
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
     * The NetCDF parameter name for the Earth radius.
     */
    public static final String EARTH_RADIUS = "earth_radius";

    /**
     * Name of the {@value} projection parameter, which is handled specially during WKT formatting.
     */
    public static final String SEMI_MAJOR = "semi_major",
                               SEMI_MINOR = "semi_minor";

    /**
     * The NetCDF parameter name for inverse flattening, and whether that parameter is definitive.
     * The later is specific to SIS.
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
     * The NetCDF parameter name for the standard parallels.
     */
    public static final String STANDARD_PARALLEL = "standard_parallel";

    /**
     * The OGC parameter name for the standard parallels.
     */
    public static final String STANDARD_PARALLEL_1 = "standard_parallel_1",
                               STANDARD_PARALLEL_2 = "standard_parallel_2";

    /**
     * The OGC parameter name for the scale factor.
     * While Apache SIS uses EPSG names when possible, the OGC names are convenient in this case
     * because they do not depend on the projection. For example EPSG has at least three different
     * names for the scale factor, depending on the projection:
     *
     * <ul>
     *   <li><cite>Scale factor at natural origin</cite></li>
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
     * because they do not depend on the projection. For example EPSG has at least two different
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
     * The OGC name for <cite>"Affine parametric transformation"</cite>.
     *
     * @see org.apache.sis.internal.referencing.provider.Affine#NAME
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
     * Do not allow instantiation of this class.
     */
    private Constants() {
    }
}
