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
import java.util.NoSuchElementException;
import javax.measure.unit.SI;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Projection;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Messages;

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
     * All names known to Apache SIS for the <cite>semi-major</cite> parameter.
     * This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_major"}, {@code "SemiMajor"} and {@code "a"}.</p>
     */
    public static final DefaultParameterDescriptor<Double> SEMI_MAJOR;

    /**
     * All names known to Apache SIS for the <cite>semi-minor</cite> parameter.
     * This parameter is mandatory and has no default value. The range of valid values is (0 … ∞).
     *
     * <p>Some names for this parameter are {@code "semi_minor"}, {@code "SemiMinor"} and {@code "b"}.</p>
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
     * @param  factory The factory to use for creating and concatenating the (de)normalization transforms.
     * @param  parameters The group of parameter values.
     * @return The map projection created from the given parameter values.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws FactoryException if the map projection can not be created.
     */
    @Override
    public final MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup parameters)
            throws ParameterNotFoundException, FactoryException
    {
        return createProjection(Parameters.castOrWrap(parameters)).createMapProjection(factory);
    }

    /**
     * Creates a map projection on an ellipsoid having a semi-major axis length of 1.
     *
     * @param  parameters The group of parameter values.
     * @return The map projection created from the given parameter values.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    protected abstract NormalizedProjection createProjection(final Parameters parameters) throws ParameterNotFoundException;




    //////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                          ////////
    ////////                       HELPER METHODS FOR SUBCLASSES                      ////////
    ////////                                                                          ////////
    ////////    Following methods are defined for sharing the same GenericName or     ////////
    ////////    Identifier instances when possible.                                   ////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the name of the given authority declared in the given parameter descriptor.
     * This method is used only as a way to avoid creating many instances of the same name.
     */
    static GenericName sameNameAs(final Citation authority, final GeneralParameterDescriptor parameters) {
        for (final GenericName candidate : parameters.getAlias()) {
            if (candidate instanceof Identifier && ((Identifier) candidate).getAuthority() == authority) {
                return candidate;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Copies all names except the EPSG one from the given parameter into the builder.
     * The EPSG name is presumed the first name and identifier (this is not verified).
     */
    static ParameterBuilder exceptEPSG(final ParameterDescriptor<?> source, final ParameterBuilder builder) {
        for (final GenericName alias : source.getAlias()) {
            builder.addName(alias);
        }
        return builder;
    }

    /**
     * Creates a remarks for parameters that are not formally EPSG parameter.
     *
     * @param origin The name of the projection for where the parameter is formally used.
     */
    static InternationalString notFormalParameter(final String origin) {
        return Messages.formatInternational(Messages.Keys.NotFormalProjectionParameter_1, origin);
    }
}
