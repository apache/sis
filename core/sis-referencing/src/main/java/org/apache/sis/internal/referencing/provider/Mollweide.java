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

import org.apache.sis.internal.util.Constants;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;

/**
 * The provider for <cite>"Mollweide"</cite> projection.
 * There are no EPSG projection using this operation.
 *
 * @see <a href="http://mathworld.wolfram.com/MollweideProjection.html">Mathworld formulas</a>
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
public class Mollweide extends MapProjection {

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.apache.sis.internal.util.Constants#centralMeridian
     * central meridian} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is [-180 &hellip; 180]&deg; and default value is 0&deg;.
     */
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.apache.sis.internal.util.Constants.Parameters#falseEasting
     * false easting} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.apache.sis.internal.util.Constants.Parameters#falseNorthing
     * false northing} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    /**
     * Parameters creation, which must be done before to initialize the {@link #PARAMETERS} field.
     * Note that the central Meridian and Latitude of Origin are shared with ObliqueStereographic.
     */
    static {
        final ParameterBuilder builder = new ParameterBuilder();

        CENTRAL_MERIDIAN = createLongitude(builder.addName(Constants.CENTRAL_MERIDIAN));
        FALSE_EASTING = createShift(builder.addName(Constants.FALSE_EASTING));
        FALSE_NORTHING = createShift(builder.addName(Constants.FALSE_NORTHING));

        PARAMETERS = new ParameterBuilder()
                .addName("Mollweide")
                .createGroupForMapProjection(
                        CENTRAL_MERIDIAN,
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    public Mollweide() {
        super(PARAMETERS);
    }

    @Override
    protected NormalizedProjection createProjection(Parameters parameters) throws ParameterNotFoundException {
        return new org.apache.sis.referencing.operation.projection.Mollweide(this, Parameters.castOrWrap(parameters));
    }

}
