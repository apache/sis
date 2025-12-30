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
package org.apache.sis.parameter;

import java.util.Map;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.shared.Constants;


/**
 * Map projection parameters, with special processing for alternative ways to express the ellipsoid axis length
 * and the standard parallels. Those alternative ways are non-standard; when a value is set to such alternative
 * parameter, the value is translated to standard parameter values as soon as possible.
 *
 * <p>The non-standard parameters are:</p>
 * <ul>
 *   <li>{@code "earth_radius"} and {@code "inverse_flattening"}, which are mapped to the
 *       {@code "semi_major"} and {@code "semi_minor"} parameters.</li>
 *   <li>{@code "standard_parallel"} with an array value of 1 or 2 elements, which is mapped to
 *       {@code "standard_parallel_1"} and {@code "standard_parallel_2"}</li>
 * </ul>
 *
 * The main purpose of this class is to support transparently the netCDF ways to express some parameter values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
final class MapProjectionDescriptor extends DefaultParameterDescriptorGroup {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -9142116135803309453L;

    /**
     * {@code true} if the {@value Constants#STANDARD_PARALLEL} parameter can be added.
     */
    final boolean hasStandardParallels;

    /**
     * Creates a new parameter descriptor from the given properties and parameters.
     *
     * @param properties  names, aliases and identifiers of the parameter group.
     * @param parameters  the "real" parameters.
     */
    MapProjectionDescriptor(final Map<String,?> properties, final ParameterDescriptor<?>[] parameters) {
        super(properties, addAxisLengths(parameters));
        boolean hasP1 = false;
        boolean hasP2 = false;
        for (final ParameterDescriptor<?> param : parameters) {
            switch (param.getName().getCode()) {
                case Constants.STANDARD_PARALLEL_1: hasP1 = true; break;
                case Constants.STANDARD_PARALLEL_2: hasP2 = true; break;
                default: {
                    for (final GenericName alias : param.getAlias()) {
                        switch (alias.tip().toString()) {
                            case Constants.STANDARD_PARALLEL_1: hasP1 = true; break;
                            case Constants.STANDARD_PARALLEL_2: hasP2 = true; break;
                        }
                    }
                }
            }
            if (hasP1 & hasP2) break;
        }
        hasStandardParallels = (hasP1 & hasP2);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="7", fixed="25")
    private static ParameterDescriptor<?>[] addAxisLengths(final ParameterDescriptor<?>[] parameters) {
        final ParameterDescriptor<?>[] ext = new ParameterDescriptor<?>[parameters.length + 2];
        ext[0] = MapProjection.SEMI_MAJOR;
        ext[1] = MapProjection.SEMI_MINOR;
        System.arraycopy(parameters, 0, ext, 2, parameters.length);
        return ext;
    }

    /**
     * Returns {@code true} if the given parameter names should be considered equals.
     * The algorithm used here shall be basically the same as the one used (indirectly)
     * by {@link DefaultParameterDescriptorGroup#descriptor(String)}.
     *
     * @see org.apache.sis.referencing.IdentifiedObjects#isHeuristicMatchForName(IdentifiedObject, String)
     */
    static boolean isHeuristicMatchForName(final String n1, final String n2) {
        return CharSequences.equalsFiltered(n1, n2, Characters.Filter.LETTERS_AND_DIGITS, true);
    }

    /**
     * Returns the parameter descriptor for the given name. If the given name is one of the dynamic parameters,
     * returns a descriptor for that parameter without adding it to the list of parameter values.
     *
     * @param  name  the case insensitive name of the parameter to search for.
     * @return the parameter for the given name.
     * @throws ParameterNotFoundException if there is no parameter for the given name.
     */
    @Override
    public GeneralParameterDescriptor descriptor(final String name) throws ParameterNotFoundException {
        if (isHeuristicMatchForName(name, Constants.EARTH_RADIUS)) {
            return MapProjectionParameters.EarthRadius.DESCRIPTOR;
        }
        if (isHeuristicMatchForName(name, Constants.INVERSE_FLATTENING)) {
            return MapProjectionParameters.InverseFlattening.DESCRIPTOR;
        }
        if (isHeuristicMatchForName(name, Constants.IS_IVF_DEFINITIVE)) {
            return MapProjectionParameters.IsIvfDefinitive.DESCRIPTOR;
        }
        if (hasStandardParallels) {
            if (isHeuristicMatchForName(name, Constants.STANDARD_PARALLEL)) {
                return MapProjectionParameters.StandardParallel.DESCRIPTOR;
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
