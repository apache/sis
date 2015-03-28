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
import java.util.Collection;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Workaround;


/**
 * Base class for all providers defined in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
abstract class AbstractProvider extends DefaultOperationMethod implements MathTransformProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2239172887926695217L;

    /**
     * Constructs a math transform provider from the given properties and a set of parameters.
     *
     * @param properties      Set of properties. Shall contain at least {@code "name"}.
     * @param sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param targetDimension Number of dimensions in the target CRS of this operation method.
     * @param parameters      The set of parameters (never {@code null}).
     */
    AbstractProvider(final Map<String,?> properties,
                     final int sourceDimension,
                     final int targetDimension,
                     final ParameterDescriptorGroup parameters)
    {
        super(properties, sourceDimension, targetDimension, parameters);
    }

    /**
     * Constructs a math transform provider from a set of parameters. The provider name and
     * {@linkplain #getIdentifiers() identifiers} will be the same than the parameter ones.
     *
     * @param sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param targetDimension Number of dimensions in the target CRS of this operation method.
     * @param parameters      The set of parameters (never {@code null}).
     */
    AbstractProvider(final int sourceDimension,
                     final int targetDimension,
                     final ParameterDescriptorGroup parameters)
    {
        super(toMap(parameters), sourceDimension, targetDimension, parameters);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Map<String,Object> toMap(final IdentifiedObject parameters) {
        ArgumentChecks.ensureNonNull("parameters", parameters);
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY, parameters.getName());
        final Collection<Identifier> identifiers = parameters.getIdentifiers();
        int size = identifiers.size();
        if (size != 0) {
            properties.put(IDENTIFIERS_KEY, identifiers.toArray(new Identifier[size]));
        }
        final Collection<GenericName> aliases = parameters.getAlias();
        size = aliases.size();
        if (size != 0) {
            properties.put(ALIAS_KEY, aliases.toArray(new GenericName[size]));
        }
        return properties;
    }

    /**
     * Creates the parameter builder with the default namespace set to EPSG.
     */
    static ParameterBuilder builder() {
        return new ParameterBuilder().setCodeSpace(Citations.OGP, Constants.EPSG).setRequired(true);
    }

    /**
     * Creates a descriptor for a constant value in degrees.
     */
    static ParameterDescriptor<Double> createConstant(final ParameterBuilder builder, final Double constant) {
        return builder.createBounded(MeasurementRange.create(constant, true, constant, true, NonSI.DEGREE_ANGLE), constant);
    }

    /**
     * Creates a descriptor for a latitude parameter in degrees.
     */
    static ParameterDescriptor<Double> createLatitude(final ParameterBuilder builder, final boolean includePoles) {
        return builder.createBounded(MeasurementRange.create(
                Latitude.MIN_VALUE, includePoles,
                Latitude.MAX_VALUE, includePoles,
                NonSI.DEGREE_ANGLE), 0.0);
    }

    /**
     * Creates a descriptor for a longitude parameter in degrees.
     */
    static ParameterDescriptor<Double> createLongitude(final ParameterBuilder builder) {
        return builder.createBounded(MeasurementRange.create(
                Longitude.MIN_VALUE, true,
                Longitude.MAX_VALUE, true,
                NonSI.DEGREE_ANGLE), 0.0);
    }

    /**
     * Creates a descriptor for a scale parameter with a default value of 1.
     */
    static ParameterDescriptor<Double> createScale(final ParameterBuilder builder) {
        return builder.createStrictlyPositive(1.0, Unit.ONE);
    }

    /**
     * Creates a false easting or northing parameter in metre with a default value of 0.
     */
    static ParameterDescriptor<Double> createShift(final ParameterBuilder builder) {
        return builder.create(0.0, SI.METRE);
    }
}
