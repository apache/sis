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

import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.measure.Longitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.math.MathFunctions;


/**
 * The provider for <cite>"Transverse Mercator"</cite> projection (EPSG:9807).
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.7
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
     * Width of a Universal Transverse Mercator (UTM) zone, in degrees.
     *
     * @see #zone(double)
     * @see #centralMeridian(int)
     */
    private static final double ZONE_WIDTH = 6;

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

        builder.addName(Mercator1SP.LONGITUDE_OF_ORIGIN.getName());
        LONGITUDE_OF_ORIGIN = createLongitude(except(Mercator1SP.LONGITUDE_OF_ORIGIN, Citations.NETCDF,
                sameNameAs(Citations.NETCDF, LambertConformal2SP.LONGITUDE_OF_FALSE_ORIGIN), builder));

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
     * @return The map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new org.apache.sis.referencing.operation.projection.TransverseMercator(this, parameters);
    }

    /**
     * Sets the parameter values for a Transverse Mercator projection and returns a suggested conversion name.
     *
     * <blockquote><table class="sis">
     *   <caption>Transverse Mercator parameters</caption>
     *   <tr><th>Parameter name</th>                 <th>Value</th></tr>
     *   <tr><td>Latitude of natural origin</td>     <td>Given latitude, or 0° if UTM projection</td></tr>
     *   <tr><td>Longitude of natural origin</td>    <td>Given longitude, optionally snapped to a UTM central meridian</td></tr>
     *   <tr><td>Scale factor at natural origin</td> <td>0.9996</td></tr>
     *   <tr><td>False easting</td>                  <td>500000 metres</td></tr>
     *   <tr><td>False northing</td>                 <td>0 (North hemisphere) or 10000000 (South hemisphere) metres</td></tr>
     * </table></blockquote>
     *
     * @param  group      The parameters for which to set the values.
     * @param  isUTM      {@code true} for Universal Transverse Mercator (UTM) projection.
     * @param  latitude   The latitude in the center of the desired projection.
     * @param  longitude  The longitude in the center of the desired projection.
     * @return A name like <cite>"Transverse Mercator"</cite> or <cite>"UTM zone 10N"</cite>,
     *         depending on the arguments given to this method.
     *
     * @since 0.7
     */
    public static String setParameters(final ParameterValueGroup group,
            final boolean isUTM, double latitude, double longitude)
    {
        final boolean isSouth = MathFunctions.isNegative(latitude);
        int zone = zone(longitude);
        if (isUTM) {
            latitude = 0;
            longitude = centralMeridian(zone);
        } else if (longitude != centralMeridian(zone)) {
            zone = 0;
        }
        String name = NAME;
        if (zone != 0) {
            name = "UTM zone " + zone + (isSouth ? 'S' : 'N');
        }
        group.parameter(Constants.LATITUDE_OF_ORIGIN).setValue(latitude,  NonSI.DEGREE_ANGLE);
        group.parameter(Constants.CENTRAL_MERIDIAN)  .setValue(longitude, NonSI.DEGREE_ANGLE);
        group.parameter(Constants.SCALE_FACTOR)      .setValue(0.9996, Unit.ONE);
        group.parameter(Constants.FALSE_EASTING)     .setValue(500000, SI.METRE);
        group.parameter(Constants.FALSE_NORTHING)    .setValue(isSouth ? 10000000 : 0, SI.METRE);
        return name;
    }

    /**
     * Computes the UTM zone from a meridian in the zone.
     *
     * @param  longitude A meridian inside the desired zone, in degrees relative to Greenwich.
     *         Positive longitudes are toward east, and negative longitudes toward west.
     * @return The UTM zone number numbered from 1 to 60 inclusive, or 0 if the given central meridian was NaN.
     *
     * @since 0.7
     */
    public static int zone(double longitude) {
        /*
         * Casts to int are equivalent to Math.floor(double) for positive values, which is guaranteed
         * to be the case here since we normalize the central meridian to the [MIN_VALUE … MAX_VALUE] range.
         */
        double z = (longitude - Longitude.MIN_VALUE) / ZONE_WIDTH;                          // Zone number with fractional part.
        z -= Math.floor(z / ((Longitude.MAX_VALUE - Longitude.MIN_VALUE) / ZONE_WIDTH))     // Roll in the [0 … 60) range.
                          * ((Longitude.MAX_VALUE - Longitude.MIN_VALUE) / ZONE_WIDTH);
        return (int) (z + 1);   // Cast only after addition in order to handle NaN as documented.
    }

    /**
     * Computes the central meridian of a given UTM zone.
     *
     * @param zone The UTM zone as a number in the [1 … 60] range.
     * @return The central meridian of the given UTM zone.
     *
     * @since 0.7
     */
    public static double centralMeridian(final int zone) {
        return (zone - 0.5) * ZONE_WIDTH + Longitude.MIN_VALUE;
    }
}
