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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.util.CharSequences;


/**
 * Map projection parameters, with special processing for alternative ways to express the ellipsoid axis length
 * and the standard parallels. Those alternative ways are non-standard; when a value is set to such alternative
 * parameter, the value is translated to standard parameter values as soon as possible.
 *
 * <p>The non-standard parameters are:</p>
 * <ul>
 *   <li>{@code "earth_radius"} and {@code "inverse_flattening"}, which are mapped to the
 *       {@link UniversalParameters#SEMI_MAJOR} and {@link UniversalParameters#SEMI_MINOR} parameters.</li>
 *   <li>{@code "standard_parallel"} with an array value of 1 or 2 elements, which is mapped to
 *       {@link UniversalParameters#STANDARD_PARALLEL_1} and
 *       {@link UniversalParameters#STANDARD_PARALLEL_2}</li>
 * </ul>
 *
 * The main purpose of this class is to supported transparently the NetCDF ways to express some parameter values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/reference/StandardCoordinateTransforms.html">NetCDF projection parameters</a>
 */
final class MapProjectionDescriptor extends DefaultParameterDescriptorGroup {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -9142116135803309453L;

    /**
     * The NetCDF parameter name for the Earth radius.
     */
    static final String EARTH_RADIUS = "earth_radius";

    /**
     * The NetCDF parameter name for inverse flattening.
     */
    static final String INVERSE_FLATTENING = "inverse_flattening";

    /**
     * The NetCDF parameter name for the standard parallels.
     */
    static final String STANDARD_PARALLEL = "standard_parallel";

    /**
     * The OGC parameter name for the standard parallels.
     */
    static final String STANDARD_PARALLEL_1 = "standard_parallel_1",
                        STANDARD_PARALLEL_2 = "standard_parallel_2";

    /**
     * A constant for the {@linkplain UniversalParameters#createDescriptorGroup factory method}
     * method which indicate that the {@link #EARTH_RADIUS} parameter needs to be added.
     */
    static final int ADD_EARTH_RADIUS = 1;

    /**
     * A constant for the {@linkplain UniversalParameters#createDescriptorGroup factory method}
     * method which indicate that the {@link #STANDARD_PARALLEL} parameter needs to be added.
     */
    static final int ADD_STANDARD_PARALLEL = 2;

    /**
     * Bitwise combination of {@code ADD_*} constants indicating which dynamic parameters to add.
     */
    final int dynamicParameters;

    /**
     * Creates a new parameter descriptor from the given properties and parameters.
     *
     * @param properties Names, aliases and identifiers of the parameter group.
     * @param parameters The "real" parameters.
     * @param dynamicParameters Bitwise combination of {@code ADD_*} constants
     *        indicating which dynamic parameters to add.
     */
    MapProjectionDescriptor(final Map<String,?> properties, final ParameterDescriptor<?>[] parameters,
            final int dynamicParameters)
    {
        super(properties, 1, 1, parameters);
        this.dynamicParameters = dynamicParameters;
    }

    /**
     * Returns the parameter descriptor for the given name. If the given name is one of the dynamic parameters,
     * returns a descriptor for that parameter without adding it to the list of parameter values.
     *
     * @param  name The case insensitive name of the parameter to search for.
     * @return The parameter for the given name.
     * @throws ParameterNotFoundException if there is no parameter for the given name.
     */
    @Override
    public GeneralParameterDescriptor descriptor(String name) throws ParameterNotFoundException {
        name = CharSequences.trimWhitespaces(name);
        if ((dynamicParameters & ADD_EARTH_RADIUS) != 0) {
            if (name.equalsIgnoreCase(EARTH_RADIUS)) {
                return UniversalParameters.EARTH_RADIUS;
            }
            if (name.equalsIgnoreCase(INVERSE_FLATTENING)) {
                return UniversalParameters.INVERSE_FLATTENING;
            }
        }
        if ((dynamicParameters & ADD_STANDARD_PARALLEL) != 0) {
            if (name.equalsIgnoreCase(STANDARD_PARALLEL)) {
                return UniversalParameters.STANDARD_PARALLEL;
            }
        }
        return super.descriptor(name);
    }

    /**
     * Returns the parameter group implementation which can handle the dynamic parameters.
     */
    @Override
    public ParameterValueGroup createValue() {
        return new MapProjectionParameters(this);
    }
}
