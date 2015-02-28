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

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.internal.referencing.DeprecatedName;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArraysExt;

import static org.opengis.referencing.operation.SingleOperation.*;
import static org.apache.sis.metadata.iso.citation.Citations.*;


/**
 * Collection of {@linkplain MapProjection map projection} parameters containing every names known to Apache SIS.
 * This class can be used for lenient parsing of projection parameters, when they are not used in a way strictly
 * compliant to their standard.
 *
 * <p>The same parameter may have different names according different authorities ({@linkplain Citations#EPSG EPSG},
 * {@linkplain Citations#OGC OGC}, {@linkplain Citations#ESRI ESRI}, <cite>etc.</cite>). But in addition, the same
 * authority may use different names for a parameter which, from a computational point of view, serves the same purpose
 * in every projections. For example the EPSG database uses all the following names for the {@link #CENTRAL_MERIDIAN}
 * parameter, even if the value is always used in the same way:</p>
 *
 * <ul>
 *   <li>Longitude of origin</li>
 *   <li>Longitude of false origin</li>
 *   <li>Longitude of natural origin</li>
 *   <li>Spherical longitude of origin</li>
 *   <li>Longitude of projection centre</li>
 * </ul>
 *
 * In every {@link MapProjection} subclass, only the official parameter names are declared.
 * For example the {@link Mercator1SP} class uses "<cite>Longitude of natural origin</cite>"
 * for the above-cited {@code CENTRAL_MERIDIAN} parameter, while {@link ObliqueMercator} uses
 * "<cite>Longitude of projection centre</cite>". However not every softwares use the right
 * parameter name with the right projection. This {@code UniversalParameters} class can be
 * used for processing parameters which may have the wrong name.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class UniversalParameters extends DefaultParameterDescriptor<Double> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4608976443553166518L;

    /**
     * All names known to Apache SIS for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#semiMajor semi-major}
     * parameter. This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_major"}, {@code "SemiMajor"} and {@code "a"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#semiMajor
     */
    public static final UniversalParameters SEMI_MAJOR = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(OGC,     Constants.SEMI_MAJOR),
            new NamedIdentifier(ESRI,    "Semi_Major"),
            new NamedIdentifier(NETCDF,  "semi_major_axis"),
            new NamedIdentifier(GEOTIFF, "SemiMajor"),
            new NamedIdentifier(PROJ4,   "a")
        }, Double.NaN, 0, Double.POSITIVE_INFINITY, SI.METRE, true);

    /**
     * All names known to Apache SIS for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#semiMinor semi-minor}
     * parameter. This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_minor"}, {@code "SemiMinor"} and {@code "b"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#semiMinor
     */
    public static final UniversalParameters SEMI_MINOR = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(OGC,     Constants.SEMI_MINOR),
            new NamedIdentifier(ESRI,    "Semi_Minor"),
            new NamedIdentifier(NETCDF,  "semi_minor_axis"),
            new NamedIdentifier(GEOTIFF, "SemiMinor"),
            new NamedIdentifier(PROJ4,   "b")
        }, Double.NaN, 0, Double.POSITIVE_INFINITY, SI.METRE, true);

    /**
     * All names known to Apache SIS for the Earth radius parameter.
     * This is used in some NetCDF files instead of {@link #SEMI_MAJOR} and {@link #SEMI_MINOR}.
     * This is not a standard parameter.
     */
    static final ParameterDescriptor<Double> EARTH_RADIUS = createDescriptor(new NamedIdentifier[] {
            new NamedIdentifier(NETCDF, MapProjectionDescriptor.EARTH_RADIUS)
        }, Double.NaN, 0.0, Double.POSITIVE_INFINITY, SI.METRE, false);

    /**
     * All known names for the inverse flattening parameter.
     * This is used in some NetCDF files instead of {@link #SEMI_MINOR}.
     * This is not a standard parameter.
     */
    static final ParameterDescriptor<Double> INVERSE_FLATTENING = createDescriptor(new NamedIdentifier[] {
            new NamedIdentifier(NETCDF, MapProjectionDescriptor.INVERSE_FLATTENING)
        }, Double.NaN, 0.0, Double.POSITIVE_INFINITY, Unit.ONE, false);

    /**
     * All known names for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#centralMeridian
     * central meridian} parameter.
     * This parameter is mandatory - meaning that it appears in {@link ParameterValueGroup}
     * even if the user didn't set it explicitly - and its default value is 0°.
     * The range of valid values is [-180 … 180]°.
     *
     * <p>Some names for this parameter are {@code "Longitude of origin"}, {@code "Longitude of false origin"},
     * {@code "Longitude of natural origin"}, {@code "Spherical longitude of origin"},
     * {@code "Longitude of projection centre"}, {@code "Longitude_Of_Center"},
     * {@code "longitude_of_projection_origin"}, {@code "central_meridian"}, {@code "longitude_of_central_meridian"},
     * {@code "NatOriginLong"}, {@code "FalseOriginLong"}, {@code "ProjCenterLong"}, {@code "CenterLong"}
     * and {@code "lon_0"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#centralMeridian
     */
    public static final UniversalParameters CENTRAL_MERIDIAN = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(EPSG,    "Longitude of origin"),
            new NamedIdentifier(EPSG,    "Longitude of false origin"),
            new NamedIdentifier(EPSG,    "Longitude of natural origin"),
            new NamedIdentifier(EPSG,    "Spherical longitude of origin"),
            new NamedIdentifier(EPSG,    "Longitude of projection centre"),
            new NamedIdentifier(OGC,     "central_meridian"),
            new NamedIdentifier(OGC,     "longitude_of_center"),
            new NamedIdentifier(ESRI,    "Central_Meridian"),
            new NamedIdentifier(ESRI,    "Longitude_Of_Center"),
            new NamedIdentifier(ESRI,    "Longitude_Of_Origin"),
            new NamedIdentifier(NETCDF,  "longitude_of_projection_origin"),
            new NamedIdentifier(NETCDF,  "longitude_of_central_meridian"),
            new NamedIdentifier(GEOTIFF, "NatOriginLong"),
            new NamedIdentifier(GEOTIFF, "FalseOriginLong"),
            new NamedIdentifier(GEOTIFF, "ProjCenterLong"),
            new NamedIdentifier(GEOTIFF, "CenterLong"),
            new NamedIdentifier(GEOTIFF, "StraightVertPoleLong"),
            new NamedIdentifier(PROJ4,   "lon_0")
        }, 0, -180, 180, NonSI.DEGREE_ANGLE, true);

    /**
     * All known names for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#latitudeOfOrigin
     * latitude of origin} parameter.
     * This parameter is mandatory - meaning that it appears in {@link ParameterValueGroup}
     * even if the user didn't set it explicitly - and its default value is 0°.
     * The range of valid values is [-90 … 90]°.
     *
     * <p>Some names for this parameter are {@code "Latitude of false origin"},
     * {@code "Latitude of natural origin"}, {@code "Spherical latitude of origin"},
     * {@code "Latitude of projection centre"}, {@code "latitude_of_center"},
     * {@code "latitude_of_projection_origin"}, {@code "latitude_of_origin"},
     * {@code "NatOriginLat"}, {@code "FalseOriginLat"}, {@code "ProjCenterLat"}, {@code "CenterLat"}
     * and @code "lat_0"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#latitudeOfOrigin
     */
    public static final UniversalParameters LATITUDE_OF_ORIGIN;

    /**
     * All known names for the standard parallels parameter, as an array of 1 or 2 elements.
     * This is used in some NetCDF files instead of {@link #STANDARD_PARALLEL_1} and
     * {@link #STANDARD_PARALLEL_2}. This is not a standard parameter.
     */
    static final ParameterDescriptor<double[]> STANDARD_PARALLEL;

    /**
     * All known names for the standard parallel 1 parameter.
     * This parameter is optional. The range of valid values is [-90 … 90]°.
     *
     * <blockquote><b>EPSG description:</b> For a conic projection with two standard parallels,
     * this is the latitude of intersection of the cone with the ellipsoid that is nearest the pole.
     * Scale is true along this parallel.</blockquote>
     *
     * <p>Some names for this parameter are {@code "Latitude of standard parallel"},
     * {@code "Latitude of pseudo standard parallel"}, {@code "standard_parallel_1"},
     * {@code "pseudo_standard_parallel_1"}, {@code "StdParallel1"} and {@code "lat_1"}.</p>
     */
    public static final UniversalParameters STANDARD_PARALLEL_1;

    /**
     * Creates the above constants together in order to share instances of identifiers
     * that appear in both cases. Those common identifiers are misplaced for historical
     * reasons (in the EPSG case, one of them is usually deprecated). We still need to
     * declare them in both places for compatibility with historical data.
     */
    static {
        final NamedIdentifier esri = new NamedIdentifier(ESRI, "Standard_Parallel_1");
        final NamedIdentifier epsg = new NamedIdentifier(EPSG, "Latitude of 1st standard parallel");
        final NamedIdentifier nc   = new NamedIdentifier(NETCDF, MapProjectionDescriptor.STANDARD_PARALLEL);

        LATITUDE_OF_ORIGIN = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(EPSG,    "Latitude of false origin"),
            new NamedIdentifier(EPSG,    "Latitude of natural origin"),
            new NamedIdentifier(EPSG,    "Spherical latitude of origin"),
            new NamedIdentifier(EPSG,    "Latitude of projection centre"), epsg,
            new NamedIdentifier(OGC,     "latitude_of_origin"),
            new NamedIdentifier(OGC,     "latitude_of_center"),
            new NamedIdentifier(ESRI,    "Latitude_Of_Origin"),
            new NamedIdentifier(ESRI,    "Latitude_Of_Center"), esri,
            new NamedIdentifier(NETCDF,  "latitude_of_projection_origin"),
            new NamedIdentifier(GEOTIFF, "NatOriginLat"),
            new NamedIdentifier(GEOTIFF, "FalseOriginLat"),
            new NamedIdentifier(GEOTIFF, "ProjCenterLat"),
            new NamedIdentifier(GEOTIFF, "CenterLat"),
            new NamedIdentifier(PROJ4,   "lat_0")
        }, 0, -90, 90, NonSI.DEGREE_ANGLE, true);

        STANDARD_PARALLEL = new DefaultParameterDescriptor<>(Collections.singletonMap(NAME_KEY, nc), 1, 1,
            double[].class, null, null, null);

        STANDARD_PARALLEL_1 = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(EPSG,    "Latitude of standard parallel"), epsg,
            new NamedIdentifier(EPSG,    "Latitude of pseudo standard parallel"),
            new NamedIdentifier(OGC,     MapProjectionDescriptor.STANDARD_PARALLEL_1),
            new NamedIdentifier(OGC,     "pseudo_standard_parallel_1"),
            new NamedIdentifier(ESRI,    "Pseudo_Standard_Parallel_1"), esri,
            new NamedIdentifier(NETCDF,  "standard_parallel[1]"), nc, // Because this parameter is an array.
            new NamedIdentifier(GEOTIFF, "StdParallel1"),
            new NamedIdentifier(PROJ4,   "lat_1")
        }, Double.NaN, -90, 90, NonSI.DEGREE_ANGLE, false);
    }

    /**
     * All known names for the standard parallel 2 parameter.
     * This parameter is optional. The range of valid values is [-90 … 90]°.
     *
     * <blockquote><b>EPSG description:</b> For a conic projection with two standard parallels,
     * this is the latitude of intersection of the cone with the ellipsoid that is furthest from the pole.
     * Scale is true along this parallel.</blockquote>
     *
     * <p>Some names for this parameter are {@code "Latitude of 2nd standard parallel"},
     * {@code "standard_parallel_2"}, {@code "StdParallel2"} and {@code "lat_2"}.</p>
     */
    public static final UniversalParameters STANDARD_PARALLEL_2 = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(EPSG,    "Latitude of 2nd standard parallel"),
            new NamedIdentifier(OGC,     MapProjectionDescriptor.STANDARD_PARALLEL_2),
            new NamedIdentifier(ESRI,    "Standard_Parallel_2"),
            new NamedIdentifier(NETCDF,  "standard_parallel[2]"),
            new NamedIdentifier(GEOTIFF, "StdParallel2"),
            new NamedIdentifier(PROJ4,   "lat_2")
        }, Double.NaN, -90, 90, NonSI.DEGREE_ANGLE, false);

    /**
     * All known names for the {@code latitudeOf1stPoint} parameter.
     * This parameter is mandatory and has no default value.
     * The range of valid values is [-90 … 90]°.
     */
    public static final UniversalParameters LAT_OF_1ST_POINT = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(Citations.ESRI, "Latitude_Of_1st_Point")
        }, Double.NaN, -90, 90, NonSI.DEGREE_ANGLE, true);

    /**
     * All known names for the {@code longitudeOf1stPoint} parameter.
     * This parameter is mandatory and has no default value.
     * The range of valid values is [-180 … 180]°.
     */
    public static final UniversalParameters LONG_OF_1ST_POINT = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(Citations.ESRI, "Longitude_Of_1st_Point")
        }, Double.NaN, -180, 180, NonSI.DEGREE_ANGLE, true);

    /**
     * All known names for the {@code latitudeOf2ndPoint} parameter.
     * This parameter is mandatory and has no default value.
     * The range of valid values is [-90 … 90]°.
     */
    public static final UniversalParameters LAT_OF_2ND_POINT = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(Citations.ESRI, "Latitude_Of_2nd_Point")
        }, Double.NaN, -90, 90, NonSI.DEGREE_ANGLE, true);

    /**
     * All known names for the {@code longitudeOf2ndPoint} parameter.
     * This parameter is mandatory and has no default value.
     * The range of valid values is [-180 … 180]°.
     */
    public static final UniversalParameters LONG_OF_2ND_POINT = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(Citations.ESRI, "Longitude_Of_2nd_Point")
        }, Double.NaN, -180, 180, NonSI.DEGREE_ANGLE, true);

    /**
     * All known names for the {@code azimuth} parameter.
     * This parameter is mandatory and has no default value.
     *
     * <blockquote><b>EPSG description:</b> The azimuthal direction (north zero, east of north being positive)
     * of the great circle which is the centre line of an oblique projection.
     * The azimuth is given at the projection center.</blockquote>
     *
     * <p>Some names for this parameter are {@code "Azimuth of initial line"},
     * {@code "Co-latitude of cone axis"}, {@code "azimuth"} and {@code "AzimuthAngle"}.</p>
     */
    public static final UniversalParameters AZIMUTH = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(EPSG,     "Azimuth of initial line"),
            new NamedIdentifier(EPSG,     "Co-latitude of cone axis"), // Used in Krovak projection.
            new NamedIdentifier(OGC,      "azimuth"),
            new NamedIdentifier(ESRI,     "Azimuth"),
            new NamedIdentifier(GEOTIFF,  "AzimuthAngle")
        }, Double.NaN, -360, 360, NonSI.DEGREE_ANGLE, true);

    /**
     * All known names for the {@code rectifiedGridAngle} parameter.
     * This is an optional parameter with valid values ranging [-360 … 360]°.
     * The default value is the value of the {@linkplain #AZIMUTH azimuth} parameter.
     *
     * <blockquote><b>EPSG description:</b> The angle at the natural origin of an oblique projection through which
     * the natural coordinate reference system is rotated to make the projection north axis parallel with true north.
     * </blockquote>
     *
     * <p>Some names for this parameter are {@code "Angle from Rectified to Skew Grid"},
     * {@code "rectified_grid_angle"}, {@code "RectifiedGridAngle"} and {@code "XY_Plane_Rotation"}.</p>
     */
    public static final UniversalParameters RECTIFIED_GRID_ANGLE = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(Citations.EPSG,     "Angle from Rectified to Skew Grid"),
            new NamedIdentifier(Citations.OGC,      "rectified_grid_angle"),
            new NamedIdentifier(Citations.ESRI,     "XY_Plane_Rotation"),
            new NamedIdentifier(Citations.GEOTIFF,  "RectifiedGridAngle")
        }, Double.NaN, -360, 360, NonSI.DEGREE_ANGLE, false);

    /**
     * All known names for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#scaleFactor
     * scale factor} parameter.
     * This parameter is mandatory - meaning that it appears in {@link ParameterValueGroup}
     * even if the user didn't set it explicitly - and its default value is 1.
     * The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "Scale factor at natural origin"},
     * {@code "Scale factor on initial line"}, {@code "Scale factor on pseudo standard parallel"},
     * {@code "scale_factor"}, {@code "scale_factor_at_projection_origin"}, {@code "scale_factor_at_central_meridian"},
     * {@code "ScaleAtNatOrigin"}, {@code "ScaleAtCenter"} and {@code "k"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#scaleFactor
     */
    public static final UniversalParameters SCALE_FACTOR = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(EPSG,    "Scale factor at natural origin"),
            new NamedIdentifier(EPSG,    "Scale factor on initial line"),
            new NamedIdentifier(EPSG,    "Scale factor on pseudo standard parallel"),
            new NamedIdentifier(OGC,     "scale_factor"),
            new NamedIdentifier(ESRI,    "Scale_Factor"),
            new NamedIdentifier(NETCDF,  "scale_factor_at_projection_origin"),
            new NamedIdentifier(NETCDF,  "scale_factor_at_central_meridian"),
            new NamedIdentifier(GEOTIFF, "ScaleAtNatOrigin"),
            new NamedIdentifier(GEOTIFF, "ScaleAtCenter"),
            new NamedIdentifier(PROJ4,   "k")
        }, 1, 0, Double.POSITIVE_INFINITY, Unit.ONE, true);

    /**
     * All known names for the {@code "X_Scale"} parameter.
     * This parameter is optional and its default value is 1.
     * The range of valid values is unrestricted (but value 0 is not recommended).
     * In particular, negative values can be used for reverting the axis orientation.
     *
     * <p>This is an ESRI-specific parameter, sometime used instead of {@code "AXIS"} elements
     * in <cite>Well Known Text</cite> for resolving axis orientation (especially for the
     * {@linkplain Krovak} projection). However its usage could be extended to any projection.
     * The choice to allow this parameter or not is taken on a projection-by-projection basis.</p>
     */
    public static final UniversalParameters X_SCALE = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(ESRI, "X_Scale")
        }, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Unit.ONE, false);

    /**
     * All known names for the {@code "Y_Scale"} parameter.
     * This parameter is optional and its default value is 1.
     * The range of valid values is unrestricted (but value 0 is not recommended).
     * In particular, negative values can be used for reverting the axis orientation.
     *
     * <p>This is an ESRI-specific parameter, sometime used instead of {@code "AXIS"} elements
     * in <cite>Well Known Text</cite> for resolving axis orientation (especially for the
     * {@linkplain Krovak} projection). However its usage could be extended to any projection.
     * The choice to allow this parameter or not is taken on a projection-by-projection basis.</p>
     */
    public static final UniversalParameters Y_SCALE = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(ESRI, "Y_Scale")
        }, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Unit.ONE, false);

    /**
     * All known names for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#falseEasting
     * false easting} parameter.
     * This parameter is mandatory - meaning that it appears in {@link ParameterValueGroup}
     * even if the user didn't set it explicitly - and its default value is 0 metres.
     * The range of valid values is unrestricted.
     *
     * <p>Some names for this parameter are {@code "Easting at false origin"}, {@code "Easting at projection centre"},
     * {@code "false_easting"}, {@code "FalseEasting"}, {@code "FalseOriginEasting"} and {@code "x_0"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#falseEasting
     */
    public static final UniversalParameters FALSE_EASTING = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(EPSG,    "False easting"),
            new NamedIdentifier(EPSG,    "Easting at false origin"),
            new NamedIdentifier(EPSG,    "Easting at projection centre"),
            new NamedIdentifier(OGC,     "false_easting"),
            new NamedIdentifier(ESRI,    "False_Easting"),
            new NamedIdentifier(NETCDF,  "false_easting"),
            new NamedIdentifier(GEOTIFF, "FalseEasting"),
            new NamedIdentifier(GEOTIFF, "FalseOriginEasting"),
            new NamedIdentifier(PROJ4,   "x_0")
        }, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, SI.METRE, true);

    /**
     * All known names for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#falseNorthing
     * false northing} parameter.
     * This parameter is mandatory - meaning that it appears in {@link ParameterValueGroup}
     * even if the user didn't set it explicitly - and its default value is 0 metres.
     * The range of valid values is unrestricted.
     *
     * <p>Some names for this parameter are {@code "Northing at false origin"}, {@code "Northing at projection centre"},
     * {@code "false_northing"}, {@code "FalseNorthing"}, {@code "FalseOriginNorthing"} and {@code "y_0"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#falseNorthing
     */
    public static final UniversalParameters FALSE_NORTHING = new UniversalParameters(new NamedIdentifier[] {
            new NamedIdentifier(EPSG,    "False northing"),
            new NamedIdentifier(EPSG,    "Northing at false origin"),
            new NamedIdentifier(EPSG,    "Northing at projection centre"),
            new NamedIdentifier(OGC,     "false_northing"),
            new NamedIdentifier(ESRI,    "False_Northing"),
            new NamedIdentifier(NETCDF,  "false_northing"),
            new NamedIdentifier(GEOTIFF, "FalseNorthing"),
            new NamedIdentifier(GEOTIFF, "FalseOriginNorthing"),
            new NamedIdentifier(PROJ4,   "y_0")
        }, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, SI.METRE, true);

    /**
     * The identifiers which can be declared to the descriptor. Only a subset of those values
     * will actually be used. The subset is specified by a call to a {@code select} method.
     */
    private final NamedIdentifier[] identifiers;

    /**
     * Locates the identifiers by their {@linkplain Identifier#getCode() code}.
     * If there is more than one parameter instance for the same name, this map contains
     * only the first occurrence. The other occurrences can be obtained by {@link #nextSameName}.
     */
    private final Map<String,NamedIdentifier> identifiersMap;

    /**
     * If there is many parameter instances for the same name, allow to iterate over the
     * other instances. Otherwise, {@code null}.
     */
    private final Map<NamedIdentifier,NamedIdentifier> nextSameName;

    /**
     * Creates a new instance of {@code UniversalParameters} for the given identifiers.
     * The array given in argument should never be modified, since it will not be cloned.
     *
     * @param identifiers  The parameter identifiers. Must contains at least one entry.
     * @param defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param minimum      The minimum parameter value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param maximum      The maximum parameter value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param unit         The unit for default, minimum and maximum values.
     * @param required     {@code true} if the parameter is mandatory.
     */
    private UniversalParameters(final NamedIdentifier[] identifiers, final double defaultValue,
            final double minimum, final double maximum, final Unit<?> unit, final boolean required)
    {
        super(toMap(identifiers), required ? 1 : 0, 1, Double.class,
                MeasurementRange.create(minimum, true, maximum, true, unit), null,
                Double.isNaN(defaultValue) ? null : Double.valueOf(defaultValue));
        this.identifiers = identifiers;
        identifiersMap = new HashMap<>(Containers.hashMapCapacity(identifiers.length));
        Map<NamedIdentifier,NamedIdentifier> nextSameName = null;
        /*
         * Put elements in reverse order in order to give precedence to the first occurrence.
         */
        for (int i=identifiers.length; --i >= 0;) {
            final NamedIdentifier id = identifiers[i];
            final NamedIdentifier old = identifiersMap.put(id.getCode(), id);
            if (old != null) {
                if (nextSameName == null) {
                    nextSameName = new IdentityHashMap<>(4);
                }
                nextSameName.put(id, old);
            }
        }
        this.nextSameName = nextSameName;
    }

    /**
     * Returns a new descriptor having the same identifiers than this descriptor.
     * The given array is used for disambiguation when the same authority defines many names.
     *
     * @param  excludes The authorities to exclude, or {@code null} if none.
     * @param  names    The names to be used for disambiguation.
     * @return The requested identifiers.
     */
    final ParameterDescriptor<Double> select(final Citation[] excludes, final String... names) {
        return select(getMinimumOccurs() != 0, getDefaultValue(), excludes, null, names);
    }

    /**
     * Returns a new descriptor having the same identifiers than this descriptor but a different
     * {@code mandatory} status and default value. The given array is used for disambiguation when
     * the same authority defines many names.
     *
     * @param  required     Whatever the parameter shall be mandatory or not, or {@code null} if unchanged.
     * @param  defaultValue The default value, or {@code null} for keeping it unchanged.
     * @param  excludes     The authorities to exclude, or {@code null} if none.
     * @param  deprecated   The names of deprecated identifiers, or {@code null} if none.
     * @param  names        The names to be used for disambiguation.
     *                      The same name may be used for more than one authority.
     * @return The requested identifiers.
     */
    final ParameterDescriptor<Double> select(final Boolean required, final Double defaultValue,
            final Citation[] excludes, final String[] deprecated, final String... names)
    {
        final Map<Citation,Boolean> authorities = new HashMap<>();
        NamedIdentifier[] selected = new NamedIdentifier[identifiers.length];
        long usedIdent = 0; // A bitmask of elements from the 'identifiers' array which have been used.
        long usedNames = 0; // A bitmask of elements from the given 'names' array which have been used.
        /*
         * Finds every identifiers which have not been excluded. In this process, also take note
         * of every identifiers explicitly requested by the names array given in argument.
         */
        int included = 0;
        for (final NamedIdentifier candidate : identifiers) {
            final Citation authority = candidate.getAuthority();
            if (ArraysExt.contains(excludes, authority)) {
                continue;
            }
            selected[included] = candidate;
            final String code = candidate.getCode();
            for (int j=names.length; --j>=0;) {
                if (code.equals(names[j])) {
                    if (authorities.put(authority, Boolean.TRUE) != null) {
                        throw new IllegalArgumentException(Errors.format(
                                Errors.Keys.ValueAlreadyDefined_1, authority));
                    }
                    usedNames |= (1 << j);
                    usedIdent |= (1 << included);
                    break;
                }
            }
            included++;
        }
        /*
         * If a name has not been used, this is considered as an error. We perform
         * this check for reducing the risk of erroneous declaration in providers.
         * Note that the same name may be used for more than one authority.
         */
        if (usedNames != (1 << names.length) - 1) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnexpectedParameter_1, names[Long.numberOfTrailingZeros(~usedNames)]));
        }
        /*
         * If some identifiers were selected as a result of explicit requirement through the
         * names array, discards all other identifiers of that authority. Otherwise if there
         * is some remaining authorities declaring exactly one identifier, inherits that
         * identifier silently. If more than one identifier is found for the same authority,
         * this is considered an error.
         */
        int n = 0;
        for (int i=0; i<included; i++) {
            final NamedIdentifier candidate = selected[i];
            if ((usedIdent & (1 << i)) == 0) {
                final Citation authority = candidate.getAuthority();
                final Boolean explicit = authorities.put(authority, Boolean.FALSE);
                if (explicit != null) {
                    // An identifier was already specified for this authority.
                    // If the identifier was specified explicitly by the user,
                    // do nothing. Otherwise we have ambiguity.
                    if (explicit) {
                        authorities.put(authority, Boolean.TRUE); // Restore the previous value.
                        continue;
                    }
                    throw new IllegalStateException(String.valueOf(candidate));
                }
            }
            selected[n++] = candidate;
        }
        /*
         * Adds deprecated names, if any. Those names will appears last in the names array.
         * Note that at the difference of ordinary names, we don't share deprecated names
         * between different provider. Deprecated names are rare enough that this is not needed.
         */
        if (deprecated != null) {
            selected = ArraysExt.resize(selected, n + deprecated.length);
            for (final String code : deprecated) {
                selected[n++] = new DeprecatedName(identifiersMap.get(code));
            }
        }
        selected = ArraysExt.resize(selected, n);
        return new DefaultParameterDescriptor<>(toMap(selected),
                (required != null) ? (required ? 1 : 0) : getMinimumOccurs(), 1,
                Double.class, getValueDomain(), null,
                (defaultValue != null) ? defaultValue : getDefaultValue());
    }

    /**
     * Returns the element from the given collection having at least one of the names known to
     * this {@code UniversalParameters} instance. If no such element is found, returns {@code null}.
     *
     * @param  candidates The collection of descriptors to compare with the names known to this
     *         {@code UniversalParameters} instance.
     * @return A descriptor from the given collection, or {@code null} if this method did not
     *         found any descriptor having at least one known name.
     * @throws IllegalArgumentException If more than one descriptor having a known name is found.
     */
    public ParameterDescriptor<?> find(final Collection<GeneralParameterDescriptor> candidates)
            throws IllegalArgumentException
    {
        ParameterDescriptor<?> found = null;
        for (final GeneralParameterDescriptor candidate : candidates) {
            final Identifier candidateId = candidate.getName();
            NamedIdentifier identifier = identifiersMap.get(candidateId.getCode());
            while (identifier != null) {
                final Citation authority = candidateId.getAuthority();
                if (authority == null || identifierMatches(authority, identifier.getAuthority())) {
                    if (candidate instanceof ParameterDescriptor<?>) {
                        if (found != null) {
                            throw new IllegalArgumentException(Errors.format(Errors.Keys.AmbiguousName_3,
                                    IdentifiedObjects.toString(found.getName()),
                                    IdentifiedObjects.toString(candidate.getName()),
                                    getName().getCode()));
                        }
                        found = (ParameterDescriptor<?>) candidate;
                        break; // Continue the 'for' loop.
                    } else {
                        // Name matches, but this is not an instance of parameter descriptor.
                        // It is probably an error. For now continue the search, but future
                        // implementations may do some other action here.
                    }
                }
                if (nextSameName == null) break;
                identifier = nextSameName.get(identifier);
            }
        }
        return found;
    }

    /**
     * Constructs a parameter descriptor for a floating point value. The parameter is
     * identified by codes in the namespace of one or more authorities ({@link Citations#OGC OGC},
     * {@link Citations#EPSG EPSG}, <i>etc.</i>). Those codes are declared as elements in the
     * {@code identifiers} array argument. The first element ({@code identifiers[0]}) is both the
     * {@linkplain ParameterDescriptor#getName main name} and the
     * {@linkplain ParameterDescriptor#getIdentifiers identifiers}.
     * All others elements are {@linkplain ParameterDescriptor#getAlias aliases}.
     *
     * <p>The {@code required} argument is handled as below:</p>
     * <ul>
     *   <li><p>If {@code true}, then the descriptor created by this method is flagged as
     *   <cite>mandatory</cite>, meaning that it will always appear in the list of parameter values
     *   that a user shall provide. However the value will be initialized with the given default
     *   value (if different than {@linkplain Double#NaN NaN}), so the user may not needs to supply
     *   explicitly a value.</p></li>
     *
     *   <li><p>If {@code false}, then the descriptor created by this method is flagged as
     *   <cite>optional</cite>, meaning that it will appear in the list of parameter values
     *   only if set to a value different than the default value.</p></li>
     * </ul>
     *
     * @param  identifiers  The parameter identifiers. Must contains at least one entry.
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  minimum      The minimum parameter value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  maximum      The maximum parameter value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  unit         The unit for default, minimum and maximum values.
     * @param  required     {@code true} if the parameter is mandatory, or {@code false} if optional.
     * @return The descriptor for the given identifiers.
     */
    static ParameterDescriptor<Double> createDescriptor(
            final Identifier[] identifiers, final double defaultValue,
            final double minimum, final double maximum, final Unit<?> unit, final boolean required)
    {
        final MeasurementRange<Double> valueDomain = MeasurementRange.create(minimum, true, maximum, true, unit);
        return new DefaultParameterDescriptor<>(toMap(identifiers), required ? 1 : 0, 1,
                Double.class, valueDomain, null, Double.isNaN(defaultValue) ? null : defaultValue);
    }

    /**
     * Constructs a parameter group from a set of alias. The parameter group is
     * identified by codes provided by one or more authorities. Common authorities are
     * {@link Citations#OGC OGC} and {@link Citations#EPSG EPSG} for example.
     *
     * <p>Special rules:</p>
     * <ul>
     *   <li>The first entry in the {@code identifiers} array is the
     *       {@linkplain ParameterDescriptorGroup#getName() primary name}.</li>
     *   <li>If an identifier does not implements the {@link GenericName} interface, it is
     *       used as an {@linkplain ParameterDescriptorGroup#getIdentifiers identifiers}.</li>
     *   <li>All others are {@linkplain ParameterDescriptorGroup#getAlias aliases}.</li>
     * </ul>
     *
     * <b>Note:</b> This method may modify in-place the given parameters array.
     * Do not pass a cached array.
     *
     * @param  identifiers  The operation identifiers. Must contains at least one entry.
     * @param  excludes     The authorities to exclude from all parameters, or {@code null} if none.
     * @param  parameters   The set of parameters, or {@code null} or an empty array if none.
     * @param  dynamicParameters   Non-standard dynamic parameters to add as bitwise combination of
     *                      {@link MapProjectionDescriptor}, or 0 if none.
     * @return The descriptor for the given identifiers.
     */
    static ParameterDescriptorGroup createDescriptorGroup(final Identifier[] identifiers,
            final Citation[] excludes, final DefaultParameterDescriptor<?>[] parameters, final int dynamicParameters)
    {
        if (excludes != null) {
            final Map<String,Object> properties = new HashMap<>();
            for (int i=0; i<parameters.length; i++) {
                final DefaultParameterDescriptor<?> param = parameters[i];
                if (param.getValueClass() != Double.class) {
                    continue;
                }
                properties.putAll(IdentifiedObjects.getProperties(param));
                boolean forAlias = false;
                boolean modified = false;
                Object[] aliases;
                do { // Executed exactly twice: once for identifier, then once for aliases.
                    final String key = forAlias ? ALIAS_KEY : IDENTIFIERS_KEY;
                    aliases = (Object[]) properties.get(key);
                    if (aliases != null) {
                        int n = 0;
                        for (final Object alias : aliases) {
                            if (alias instanceof Identifier) {
                                if (ArraysExt.contains(excludes, ((Identifier) alias).getAuthority())) {
                                    continue;
                                }
                            }
                            aliases[n++] = alias;
                        }
                        // If at least one alias or identifier has been removed, remember that we
                        // will need to create a new parameter in replacement to the provided one.
                        if (n != aliases.length) {
                            properties.put(key, Arrays.copyOf(aliases, n));
                            modified = true;
                        }
                    }
                } while ((forAlias = !forAlias) == true);
                if (modified) {
                    properties.put(NAME_KEY, aliases[0]); // In case the primary name was one of the excluded names.
                    parameters[i] = new DefaultParameterDescriptor<>(properties, param.getMinimumOccurs(), 1,
                            Double.class, param.getValueDomain(), null, (Double) param.getDefaultValue());
                }
                properties.clear();
            }
        }
        final Map<String,Object> properties = toMap(identifiers);
        return (dynamicParameters == 0) ? new DefaultParameterDescriptorGroup(properties, 1, 1, parameters) :
                new MapProjectionDescriptor(properties, parameters, dynamicParameters);
    }

    /**
     * Puts the identifiers into a properties map suitable for {@link ParameterDescriptorGroup} constructor.
     * The first identifier is used as the primary name. All other elements are aliases.
     */
    private static Map<String,Object> toMap(final Identifier[] identifiers) {
        int idCount    = 0;
        int aliasCount = 0;
        GenericName[] alias = null;
        Identifier[] id = null;
        for (int i=0; i<identifiers.length; i++) {
            final Identifier candidate = identifiers[i];
            if (candidate instanceof GenericName) {
                if (alias == null) {
                    alias = new GenericName[identifiers.length - i];
                }
                alias[aliasCount++] = (GenericName) candidate;
            } else {
                if (id == null) {
                    id = new Identifier[identifiers.length - i];
                }
                id[idCount++] = candidate;
            }
        }
        id    = ArraysExt.resize(id,    idCount);
        alias = ArraysExt.resize(alias, aliasCount);
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY,        identifiers[0]);
        properties.put(IDENTIFIERS_KEY, id);
        properties.put(ALIAS_KEY,       alias);
        return properties;
    }
}
