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
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.measure.Longitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.Units;


/**
 * The provider for <cite>"Transverse Mercator"</cite> projection (EPSG:9807).
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.8
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/transverse_mercator.html">Transverse Mercator on RemoteSensing.org</a>
 */
@XmlTransient
public final class TransverseMercator extends AbstractMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3386587506686432398L;

    /**
     * The {@value} string, which is also the EPSG name for this projection.
     */
    public static final String NAME = "Transverse Mercator";

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * Valid values range is [-90 … 90]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Scale factor at natural origin</cite> (k₀) parameter value.
     * Valid values range is (0 … ∞) and default value is 1.
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        LATITUDE_OF_ORIGIN = createLatitude(builder
                .addNamesAndIdentifiers(Mercator1SP.LATITUDE_OF_ORIGIN), true);

        LONGITUDE_OF_ORIGIN = createLongitude(renameAlias(Mercator1SP.LONGITUDE_OF_ORIGIN,
                Citations.NETCDF, LambertConformal2SP.LONGITUDE_OF_FALSE_ORIGIN, builder));

        SCALE_FACTOR = createScale(builder
                .addNamesAndIdentifiers(Mercator1SP.SCALE_FACTOR)
                .rename(Citations.NETCDF, "scale_factor_at_central_meridian"));

        PARAMETERS = builder
                .addIdentifier(              "9807")
                .addName(                    NAME)
                .addName(                    "Gauss-Kruger")
                .addName(                    "Gauss-Boaga")
                .addName(                    "TM")
                .addName(Citations.OGC,      "Transverse_Mercator")
                .addName(Citations.ESRI,     "Transverse_Mercator")
                .addName(Citations.ESRI,     "Gauss_Kruger")
                .addName(Citations.NETCDF,   "TransverseMercator")
                .addName(Citations.GEOTIFF,  "CT_TransverseMercator")
                .addName(Citations.S57,      "Transverse Mercator")
                .addName(Citations.S57,      "TME")
                .addName(Citations.PROJ4,    "tmerc")
                .addIdentifier(Citations.GEOTIFF,  "1")
                .addIdentifier(Citations.MAP_INFO, "8")
                .addIdentifier(Citations.S57,     "13")
                .createGroupForMapProjection(
                        LATITUDE_OF_ORIGIN,
                        LONGITUDE_OF_ORIGIN,
                        SCALE_FACTOR,
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public TransverseMercator() {
        super(PARAMETERS);
    }

    /**
     * {@inheritDoc}
     *
     * @return the map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new org.apache.sis.referencing.operation.projection.TransverseMercator(this, parameters);
    }

    /**
     * Computes zone numbers and central meridian.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     */
    public static enum Zoner {
        /**
         * Universal Transverse Mercator (UTM) projection zones.
         * The zone computation includes special cases for Norway and Svalbard.
         *
         * <blockquote><table class="sis">
         *   <caption>Universal Transverse Mercator parameters</caption>
         *   <tr><th>Parameter name</th>                 <th>Value</th></tr>
         *   <tr><td>Latitude of natural origin</td>     <td>0°</td></tr>
         *   <tr><td>Longitude of natural origin</td>    <td>Given longitude snapped to a UTM central meridian</td></tr>
         *   <tr><td>Scale factor at natural origin</td> <td>0.9996</td></tr>
         *   <tr><td>False easting</td>                  <td>500000 metres</td></tr>
         *   <tr><td>False northing</td>                 <td>0 (North hemisphere) or 10000000 (South hemisphere) metres</td></tr>
         * </table></blockquote>
         */
        UTM(Longitude.MIN_VALUE, 6, 0.9996, 500000, 10000000) {
            /** Computes the zone from a meridian in the zone. */
            @Override public int zone(final double φ, final double λ) {
                int zone = super.zone(φ, λ);
                switch (zone) {
                    /*
                     * Between 56° and 64°, zone  32 is widened to 9° at the expense of zone 31 to accommodate Norway.
                     * Between 72° and 84°, zones 33 and 35 are widened to 12° to accommodate Svalbard. To compensate,
                     * zones 31 and 37 are widened to 9° and zones 32, 34, and 36 are eliminated.
                     * In this switch statement, only the zones that are reduced or eliminated needs to appear.
                     */
                    case 31: if (isNorway  (φ)) {if (λ >=  3) zone++;             } break;   //  3° is zone 31 central meridian.
                    case 32: if (isSvalbard(φ)) {if (λ >=  9) zone++; else zone--;} break;   //  9° is zone 32 central meridian.
                    case 34: if (isSvalbard(φ)) {if (λ >= 21) zone++; else zone--;} break;   // 21° is zone 34 central meridian.
                    case 36: if (isSvalbard(φ)) {if (λ >= 33) zone++; else zone--;} break;   // 33° is zone 36 central meridian.
                }
                return zone;
            }

            /** Indicates whether the given zone needs to be handled in a special way for the given latitude. */
            @Override public boolean isSpecialCase(final int zone, final double φ) {
                if (zone >= 31 && zone <= 37) {
                    return isSvalbard(φ) || (zone <= 32 && isNorway(φ));
                }
                return false;
            }

            /** Indicates whether the given geographic area intersects the regions that need to be handled in a special way. */
            @Override public boolean isSpecialCase(final double φmin, final double φmax, final double λmin, final double λmax) {
                if (φmax >= NORWAY_BOUNDS && φmin < NORTH_BOUNDS) {
                    return super.zone(0, λmax) >= 31 && super.zone(0, λmin) <= 37;
                }
                return false;
            }
        },

        /**
         * Modified Transverse Mercator (MTM) projection zones.
         * This projection is used in Canada only.
         *
         * <blockquote><table class="sis">
         *   <caption>Modified Transverse Mercator parameters</caption>
         *   <tr><th>Parameter name</th>                 <th>Value</th></tr>
         *   <tr><td>Latitude of natural origin</td>     <td>0°</td></tr>
         *   <tr><td>Longitude of natural origin</td>    <td>Given longitude snapped to a MTM central meridian</td></tr>
         *   <tr><td>Scale factor at natural origin</td> <td>0.9999</td></tr>
         *   <tr><td>False easting</td>                  <td>304800 metres</td></tr>
         *   <tr><td>False northing</td>                 <td>0 metres</td></tr>
         * </table></blockquote>
         */
        MTM(-51.5, -3, 0.9999, 304800, Double.NaN),

        /**
         * Like UTM, but allows <cite>latitude of origin</cite> and <cite>central meridian</cite> to be anywhere.
         * The given central meridian is not snapped to the UTM zone center and no special case is applied for
         * Norway or Svalbard.
         *
         * <p>This zoner matches the behavior of {@code AUTO(2):42002} authority code specified in the
         * OGC <cite>Web Map Service</cite> (WMS) specification.</p>
         */
        ANY(Longitude.MIN_VALUE, 6, 0.9996, 500000, 10000000);

        /**
         * Longitude of the beginning of zone 1. This is the westmost longitude if {@link #width} is positive,
         * or the eastmost longitude if {@code width} is negative.
         */
        public final double origin;

        /**
         * Width of a zone, in degrees of longitude.
         * Positive if zone numbers are increasing eastward, or negative if increasing westwards.
         *
         * @see #zone(double)
         * @see #centralMeridian(int)
         */
        public final double width;

        /**
         * The scale factor of zoned projections.
         */
        public final double scale;

        /**
         * The false easting of zoned projections, in metres.
         */
        public final double easting;

        /**
         * The false northing in South hemisphere of zoned projection, in metres.
         */
        public final double northing;

        /**
         * Creates a new instance for computing zones using the given parameters.
         */
        private Zoner(final double origin, final double width, final double scale, final double easting, final double northing) {
            this.origin   = origin;
            this.width    = width;
            this.scale    = scale;
            this.easting  = easting;
            this.northing = northing;
        }

        /**
         * Sets the parameter values for a Transverse Mercator projection and returns a suggested conversion name.
         *
         * <blockquote><table class="sis">
         *   <caption>Transverse Mercator parameters</caption>
         *   <tr><th>Parameter name</th>                 <th>Value</th></tr>
         *   <tr><td>Latitude of natural origin</td>     <td>Given latitude, or 0° if zoned projection</td></tr>
         *   <tr><td>Longitude of natural origin</td>    <td>Given longitude, optionally snapped to a zone central meridian</td></tr>
         *   <tr><td>Scale factor at natural origin</td> <td>0.9996 for UTM or 0.9999 for MTM</td></tr>
         *   <tr><td>False easting</td>                  <td>500000 metres for UTM or 304800 metres for MTM</td></tr>
         *   <tr><td>False northing</td>                 <td>0 (North hemisphere) or 10000000 (South hemisphere) metres</td></tr>
         * </table></blockquote>
         *
         * @param  group      the parameters for which to set the values.
         * @param  latitude   the latitude in the center of the desired projection.
         * @param  longitude  the longitude in the center of the desired projection.
         * @return a name like <cite>"Transverse Mercator"</cite> or <cite>"UTM zone 10N"</cite>,
         *         depending on the arguments given to this method.
         */
        public final String setParameters(final ParameterValueGroup group, double latitude, double longitude) {
            final boolean isSouth = MathFunctions.isNegative(latitude);
            int zone = zone(latitude, longitude);
            String name;
            if (this == ANY) {
                name = "UTM";
                if (latitude != 0 || longitude != centralMeridian(zone)) {
                    name = NAME;
                    zone = 0;
                }
            } else {
                name      = name();
                latitude  = 0;
                longitude = centralMeridian(zone);
            }
            if (zone != 0) {
                name = name + " zone " + zone + (isSouth ? 'S' : 'N');
            }
            group.parameter(Constants.LATITUDE_OF_ORIGIN).setValue(latitude,  Units.DEGREE);
            group.parameter(Constants.CENTRAL_MERIDIAN)  .setValue(longitude, Units.DEGREE);
            group.parameter(Constants.SCALE_FACTOR)      .setValue(scale,     Units.UNITY);
            group.parameter(Constants.FALSE_EASTING)     .setValue(easting,   Units.METRE);
            group.parameter(Constants.FALSE_NORTHING)    .setValue(isSouth ? northing : 0, Units.METRE);
            return name;
        }

        /**
         * If the given parameter values are those of a zoned projection, returns the zone number (negative if South).
         * Otherwise returns 0. It is caller's responsibility to verify that the operation method is {@value #NAME}.
         *
         * @param  group  the Transverse Mercator projection parameters.
         * @return zone number (positive if North, negative if South),
         *         or 0 if the given parameters are not for a zoned projection.
         */
        public final int zone(final ParameterValueGroup group) {
            if (Numerics.epsilonEqual(group.parameter(Constants.SCALE_FACTOR)      .doubleValue(Units.UNITY), scale,   Numerics.COMPARISON_THRESHOLD) &&
                Numerics.epsilonEqual(group.parameter(Constants.FALSE_EASTING)     .doubleValue(Units.METRE), easting, Formulas.LINEAR_TOLERANCE) &&
                Numerics.epsilonEqual(group.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue(Units.DEGREE),      0, Formulas.ANGULAR_TOLERANCE))
            {
                double v = group.parameter(Constants.FALSE_NORTHING).doubleValue(Units.METRE);
                final boolean isNorth = Numerics.epsilonEqual(v, 0, Formulas.LINEAR_TOLERANCE);
                if (isNorth || Numerics.epsilonEqual(v, northing, Formulas.LINEAR_TOLERANCE)) {
                    v = group.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(Units.DEGREE);
                    int zone = zone(0, v);
                    if (Numerics.epsilonEqual(centralMeridian(zone), v, Formulas.ANGULAR_TOLERANCE)) {
                        if (!isNorth) zone = -zone;
                        return zone;
                    }
                }
            }
            return 0;
        }

        /**
         * Computes the zone from a meridian in the zone.
         *
         * @param  φ  a latitude for which to get the zone. Used for taking in account the special cases.
         * @param  λ  a meridian inside the desired zone, in degrees relative to Greenwich.
         *            Positive longitudes are toward east, and negative longitudes toward west.
         * @return the zone number numbered from 1 inclusive, or 0 if the given central meridian was NaN.
         */
        public int zone(final double φ, final double λ) {
            double z = (λ - origin) / width;                                              // Zone number with fractional part.
            final double count = (Longitude.MAX_VALUE - Longitude.MIN_VALUE) / width;
            z -= Math.floor(z / count) * count;                                           // Roll in the [0 … 60) range.
            /*
             * Casts to int are equivalent to Math.floor(double) for positive values, which is guaranteed
             * to be the case here since we normalize the central meridian to the [MIN_VALUE … MAX_VALUE]
             * range. We cast only after addition in order to handle NaN as documented.
             */
            return (int) (z + 1);
        }

        /**
         * Returns the number of zones.
         *
         * @return number of zones.
         */
        public final int zoneCount() {
            return (int) ((Longitude.MAX_VALUE - Longitude.MIN_VALUE) / width);
        }

        /**
         * Computes the central meridian of a given zone.
         *
         * @param  zone  the zone as a number starting with 1.
         * @return the central meridian of the given zone.
         */
        public final double centralMeridian(final int zone) {
            return (zone - 0.5) * width + origin;
        }

        /**
         * Indicates whether the given zone needs to be handled in a special way for the given latitude.
         *
         * @param  zone  the zone to test if it is a special case.
         * @param  φ     the latitude for which to test if there is a special case.
         * @return whether the given zone at the given latitude is a special case.
         */
        public boolean isSpecialCase(final int zone, final double φ) {
            return false;
        }

        /**
         * Indicates whether the given geographic area intersects the regions that need to be handled in a special way.
         *
         * @param  φmin  southernmost latitude in degrees.
         * @param  φmax  northernmost latitude in degrees.
         * @param  λmin  westernmost longitude in degrees.
         * @param  λmax  easternmost longitude in degrees.
         * @return whether the given area intersects a region that needs to be handled as a special case.
         */
        public boolean isSpecialCase(final double φmin, final double φmax, final double λmin, final double λmax) {
            return false;
        }

        /**
         * First exception in UTM projection, corresponding to latitude band V.
         * This method is public for {@code MilitaryGridReferenceSystemTest.verifyZonerConsistency()} purpose only.
         *
         * @param  φ  the latitude in degrees to test.
         * @return whether the given latitude is in the Norway latitude band.
         */
        public static boolean isNorway(final double φ) {
            return (φ >= NORWAY_BOUNDS) && (φ < 64);
        }

        /**
         * Second exception in UTM projection, corresponding to latitude band X.
         * This method is public for {@code MilitaryGridReferenceSystemTest.verifyZonerConsistency()} purpose only.
         *
         * @param  φ  the latitude in degrees to test.
         * @return whether the given latitude is in the Svalbard latitude band.
         */
        public static boolean isSvalbard(final double φ) {
            return (φ >= SVALBARD_BOUNDS) && (φ < NORTH_BOUNDS);
        }

        /**
         * Southernmost bound of the first latitude band ({@code 'C'}), inclusive.
         *
         * @see #NORTH_BOUNDS
         */
        public static final double SOUTH_BOUNDS = -80;

        /**
         * Southernmost bounds (inclusive) of the latitude band that contains Norway ({@code 'V'}).
         * This is the first latitude band where we may need to handle special cases (Norway and Svalbard).
         */
        private static final double NORWAY_BOUNDS = 56;

        /**
         * Southernmost bounds (inclusive) of the last latitude band, which contains Svalbard.
         * This latitude band is 12° height instead of 8°.
         */
        public static final double SVALBARD_BOUNDS = 72;

        /**
         * Northernmost bound of the last latitude band ({@code 'X'}), exclusive.
         *
         * @see #SOUTH_BOUNDS
         */
        public static final double NORTH_BOUNDS = 84;
    }
}
