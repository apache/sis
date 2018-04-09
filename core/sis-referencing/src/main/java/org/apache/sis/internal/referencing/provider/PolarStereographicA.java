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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;


/**
 * The provider for <cite>"Polar Stereographic (Variant A)"</cite> projection (EPSG:9810).
 * Also used for the definition of Universal Polar Stereographic (UPS) projection.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see <a href="http://geotiff.maptools.org/proj_list/polar_stereographic.html">GeoTIFF parameters for Polar Stereographic</a>
 *
 * @since 0.6
 * @module
 */
@XmlTransient
public final class PolarStereographicA extends AbstractStereographic {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 538262714055500925L;

    /**
     * The EPSG name for this projection.
     */
    public static final String NAME = "Polar Stereographic (variant A)";

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9810";

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * Valid values can be -90° or 90° only. There is no default value.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN = LambertConformal1SP.LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Scale factor at natural origin</cite> (k₀) parameter value.
     * Valid values range is (0 … ∞) and default value is 1.
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR = Mercator1SP.SCALE_FACTOR;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        LONGITUDE_OF_ORIGIN = createLongitude(builder
                .addNamesAndIdentifiers(ObliqueStereographic.LONGITUDE_OF_ORIGIN)
                .reidentify(Citations.GEOTIFF, "3095")
                .rename(Citations.GEOTIFF, "StraightVertPoleLong"));

        PARAMETERS = builder
                .addIdentifier(             IDENTIFIER)
                .addName(                   NAME)
                .addName(Citations.OGC,     "Polar_Stereographic")
                .addName(Citations.GEOTIFF, "CT_PolarStereographic")
                .addName(Citations.PROJ4,   "stere")
                .addIdentifier(Citations.GEOTIFF, "15")
                .createGroupForMapProjection(
                        LATITUDE_OF_ORIGIN,     // Can be only ±90°
                        LONGITUDE_OF_ORIGIN,
                        SCALE_FACTOR,
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public PolarStereographicA() {
        super(PARAMETERS);
    }

    /**
     * False Easting and false Northing value used in Universal Polar Stereographic (UPS) projections.
     * Represented as an integer for the convenience of Military Reference Grid System (MGRS) or other
     * grid systems.
     */
    public static final int UPS_SHIFT = 2000000;

    /**
     * Sets the parameter values for a Universal Polar Stereographic projection
     * and returns a suggested conversion name.
     *
     * <blockquote><table class="sis">
     *   <caption>Universal Polar Stereographic parameters</caption>
     *   <tr><th>Parameter name</th>                 <th>Value</th></tr>
     *   <tr><td>Latitude of natural origin</td>     <td>90°N or 90°S</td></tr>
     *   <tr><td>Longitude of natural origin</td>    <td>0°</td></tr>
     *   <tr><td>Scale factor at natural origin</td> <td>0.994</td></tr>
     *   <tr><td>False easting</td>                  <td>2000000 metres</td></tr>
     *   <tr><td>False northing</td>                 <td>2000000 metres</td></tr>
     * </table></blockquote>
     *
     * @param  group  the parameters for which to set the values.
     * @param  north  {@code true} for North pole, or {@code false} for South pole.
     * @return a name like <cite>"Universal Polar Stereographic North"</cite>,
     *         depending on the arguments given to this method.
     *
     * @since 0.8
     */
    public static String setParameters(final ParameterValueGroup group, final boolean north) {
        group.parameter(Constants.LATITUDE_OF_ORIGIN).setValue(north ? Latitude.MAX_VALUE : Latitude.MIN_VALUE, Units.DEGREE);
        group.parameter(Constants.CENTRAL_MERIDIAN)  .setValue(0,         Units.DEGREE);
        group.parameter(Constants.SCALE_FACTOR)      .setValue(0.994,     Units.UNITY);
        group.parameter(Constants.FALSE_EASTING)     .setValue(UPS_SHIFT, Units.METRE);
        group.parameter(Constants.FALSE_NORTHING)    .setValue(UPS_SHIFT, Units.METRE);
        return "Universal Polar Stereographic " + (north ? "North" : "South");
    }

    /**
     * If the given parameter values are those of a Universal Polar Stereographic projection,
     * returns -1 for South pole or +1 for North pole. Otherwise returns 0. It is caller's
     * responsibility to verify that the operation method is {@value #NAME}.
     *
     * @param  group  the Transverse Mercator projection parameters.
     * @return +1 if UPS north, -1 if UPS south, or 0 if the given parameters are not for a UPS projection.
     *
     * @since 0.8
     */
    public static int isUPS(final ParameterValueGroup group) {
        if (Numerics.epsilonEqual(group.parameter(Constants.SCALE_FACTOR)    .doubleValue(Units.UNITY),     0.994, Numerics.COMPARISON_THRESHOLD) &&
            Numerics.epsilonEqual(group.parameter(Constants.FALSE_EASTING)   .doubleValue(Units.METRE), UPS_SHIFT, Formulas.LINEAR_TOLERANCE) &&
            Numerics.epsilonEqual(group.parameter(Constants.FALSE_NORTHING)  .doubleValue(Units.METRE), UPS_SHIFT, Formulas.LINEAR_TOLERANCE) &&
            Numerics.epsilonEqual(group.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(Units.DEGREE),        0, Formulas.ANGULAR_TOLERANCE))
        {
            final double φ = group.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue(Units.DEGREE);
            if (Numerics.epsilonEqual(φ, Latitude.MAX_VALUE, Formulas.ANGULAR_TOLERANCE)) return +1;
            if (Numerics.epsilonEqual(φ, Latitude.MIN_VALUE, Formulas.ANGULAR_TOLERANCE)) return -1;
        }
        return 0;
    }
}
