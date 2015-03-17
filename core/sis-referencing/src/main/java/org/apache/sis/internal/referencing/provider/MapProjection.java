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
import javax.measure.unit.SI;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.Projection;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptor;

import static org.opengis.metadata.Identifier.AUTHORITY_KEY;


/**
 * Base class for all map projection providers defined in this package. This base class defines some descriptors
 * for the most commonly used parameters. Subclasses will declare additional parameters and group them in a
 * {@linkplain ParameterDescriptorGroup descriptor group} named {@code PARAMETERS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public abstract class MapProjection extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6280666068007678702L;

    /**
     * All names known to Apache SIS for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#semiMajor semi-major}
     * parameter. This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_major"}, {@code "SemiMajor"} and {@code "a"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#semiMajor
     */
    public static final DefaultParameterDescriptor<Double> SEMI_MAJOR;

    /**
     * All names known to Apache SIS for the
     * {@linkplain org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#semiMinor semi-minor}
     * parameter. This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_minor"}, {@code "SemiMinor"} and {@code "b"}.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#semiMinor
     */
    public static final DefaultParameterDescriptor<Double> SEMI_MINOR;
    static {
        final MeasurementRange<Double> valueDomain = MeasurementRange.createGreaterThan(0, SI.METRE);
        final GenericName[] aliases = {
            new NamedIdentifier(Citations.ESRI,    "Semi_Major"),
            new NamedIdentifier(Citations.NETCDF,  "semi_major_axis"),
            new NamedIdentifier(Citations.GEOTIFF, "SemiMajor"),
            new NamedIdentifier(Citations.PROJ4,   "a")
        };
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(AUTHORITY_KEY, Citations.OGC);
        properties.put(NAME_KEY,      Constants.SEMI_MAJOR);
        properties.put(ALIAS_KEY,     aliases);
        SEMI_MAJOR = new DefaultParameterDescriptor<>(properties, 1, 1, Double.class, valueDomain, null, null);
        /*
         * Change in-place the name and aliases (we do not need to create new objects)
         * before to create the SEMI_MINOR descriptor.
         */
        properties.put(NAME_KEY, Constants.SEMI_MINOR);
        aliases[0] = new NamedIdentifier(Citations.ESRI,    "Semi_Minor");
        aliases[1] = new NamedIdentifier(Citations.NETCDF,  "semi_minor_axis");
        aliases[2] = new NamedIdentifier(Citations.GEOTIFF, "SemiMinor");
        aliases[3] = new NamedIdentifier(Citations.PROJ4,   "b");
        SEMI_MINOR = new DefaultParameterDescriptor<>(properties, 1, 1, Double.class, valueDomain, null, null);
    }

    /**
     * Constructs a math transform provider from a set of parameters. The provider
     * {@linkplain #getIdentifiers() identifiers} will be the same than the parameter ones.
     *
     * @param parameters The set of parameters (never {@code null}).
     */
    protected MapProjection(final ParameterDescriptorGroup parameters) {
        super(2, 2, parameters);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code Projection.class} or a sub-type.
     */
    @Override
    public Class<? extends Projection> getOperationType() {
        return Projection.class;
    }

    /**
     * Creates a map projection from the specified group of parameter values.
     *
     * @param  values The group of parameter values.
     * @return The map projection created from the given parameter values.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    @Override
    public abstract MathTransform2D createMathTransform(ParameterValueGroup values) throws ParameterNotFoundException;
}
