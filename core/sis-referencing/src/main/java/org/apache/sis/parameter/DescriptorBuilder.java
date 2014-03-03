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
import java.util.HashMap;
import javax.measure.unit.Unit;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Range;


/**
 * Builds instances of {@link DefaultParameterDescriptor}.
 * This convenience class aims to facilitate the creation of group of parameter descriptors.
 *
 * {@section Example}
 * The following example creates the parameters for "<cite>Mercator (variant A)</cite>" projection method
 * (EPSG:9804). All parameter names will be in the "EPSG" namespace. The default values define a projection
 * centered on (0°,0°) with no scale factor and no false easting/northing.
 *
 * {@preformat java
 *   DescriptorBuilder builder = new DescriptorBuilder();
 *   builder.codespace(Citations.OGP, "EPSG").mandatory();
 *   ParameterDescriptor<Double>[] parameters = {
 *       builder.name("Latitude of natural origin")     .create( -80,  +80, 0, NonSI.DEGREE_ANGLE),
 *       builder.name("Longitude of natural origin")    .create(-180, +180, 0, NonSI.DEGREE_ANGLE),
 *       builder.name("Scale factor at natural origin") .createStrictlyPositive(1, Unit.ONE),
 *       builder.name("False easting")                  .createUnbounded(0, SI.METRE),
 *       builder.name("False northing")                 .createUnbounded(0, SI.METRE)
 *   };
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class DescriptorBuilder {
    /**
     * The name, alias, identifiers and remarks properties.
     */
    private final Map<String,Object> properties;

    /**
     * {@code true} if the parameter is mandatory, or {@code false} if optional.
     */
    private boolean required;

    /**
     * Creates a new builder.
     */
    public DescriptorBuilder() {
        properties = new HashMap<>(4);
    }

    /**
     * Sets the name to the given identifier. If an authority, {@linkplain #codespace(Citation, String) code space}
     * or {@linkplain #version(String) version} have been specified to this builder, they will be ignored since the
     * given identifier is expected to contain all those information.
     *
     * @param  name The parameter name as an identifier.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder name(final ReferenceIdentifier name) {
        properties.put(ParameterDescriptor.NAME_KEY, name);
        return this;
    }

    /**
     * Sets the name to the given string. The given string will be combined with the authority,
     * {@linkplain #codespace(Citation, String) code space} and {@linkplain #version(String) version}
     * information for creating a {@link ReferenceIdentifier}.
     *
     * @param  code The parameter name as a string.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder name(final String code) {
        properties.put(ParameterDescriptor.NAME_KEY, code);
        return this;
    }

    /**
     * Sets the authority and code space. This method is typically invoked only once, since
     * a group of parameters often uses the same code space for all individual parameters.
     *
     * @param  authority Bibliographic reference to the authority defining the codes, or {@code null} if none.
     * @param  codespace The parameter codespace, or {@code null} for inferring it from the authority.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder codespace(final Citation authority, final String codespace) {
        properties.put(ReferenceIdentifier.AUTHORITY_KEY, authority);
        properties.put(ReferenceIdentifier.CODESPACE_KEY, codespace);
        return this;
    }

    /**
     * Sets the version of code definitions. This method is typically invoked only once,
     * since a group of parameters often uses the same version for all individual parameters.
     *
     * @param  version The version of code definitions, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder version(final String version) {
        properties.put(ReferenceIdentifier.VERSION_KEY, version);
        return this;
    }

    /**
     * Sets the parameter as mandatory.
     * The minimum number of times that values are required will be 1.
     *
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder mandatory() {
        this.required = true;
        return this;
    }

    /**
     * Sets the parameter as optional.
     * The minimum number of times that values are required will be 0.
     *
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder optional() {
        this.required = false;
        return this;
    }

    /**
     * Constructs a descriptor for a parameter in a range of integer values.
     *
     * @param  minimumValue The minimum parameter value (inclusive).
     * @param  maximumValue The maximum parameter value (inclusive).
     * @param  defaultValue The default value for the parameter.
     * @return The parameter descriptor for the given range of values.
     */
    public ParameterDescriptor<Integer> create(final int minimumValue, final int maximumValue,
            final int defaultValue)
    {
        return new DefaultParameterDescriptor<>(properties, Integer.class,
                NumberRange.create(minimumValue, true, maximumValue, true), null, defaultValue, required);
    }

    /**
     * Constructs a descriptor for a parameter in a range of floating point values.
     *
     * @param  minimumValue The minimum parameter value (inclusive), or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  maximumValue The maximum parameter value (inclusive), or {@link Double#POSITIVE_INFINITY} if none.
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  unit         The unit for default, minimum and maximum values, or {@code null} if none.
     * @return The parameter descriptor for the given range of values.
     */
    public ParameterDescriptor<Double> create(final double minimumValue, final double maximumValue,
            final double defaultValue, final Unit<?> unit)
    {
        final Range<Double> valueDomain;
        if (unit != null) {
            valueDomain = MeasurementRange.create(minimumValue, true, maximumValue, true, unit);
        } else if (minimumValue != Double.NEGATIVE_INFINITY || maximumValue != Double.POSITIVE_INFINITY) {
            valueDomain = NumberRange.create(minimumValue, true, maximumValue, true);
        } else {
            valueDomain = null;
        }
        return new DefaultParameterDescriptor<>(properties, Double.class,
                valueDomain, null, Numerics.valueOf(defaultValue), required);
    }

    /**
     * Constructs a descriptor for a strictly positive parameter value.
     * The value must be greater than zero, and there is no maximum value.
     *
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  unit         The default unit, or {@code null} if none.
     * @return The parameter descriptor for the given default value and unit.
     */
    public ParameterDescriptor<Double> createStrictlyPositive(final double defaultValue, final Unit<?> unit) {
        final Range<Double> valueDomain;
        if (unit != null) {
            valueDomain = MeasurementRange.create(0.0, false, Double.POSITIVE_INFINITY, false, unit);
        } else {
            valueDomain = NumberRange.create(0.0, false, Double.POSITIVE_INFINITY, false);
        }
        return new DefaultParameterDescriptor<>(properties, Double.class,
                valueDomain, null, Numerics.valueOf(defaultValue), required);
    }

    /**
     * Constructs a descriptor without any restriction on the range of values.
     *
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  unit         The default unit, or {@code null} if none.
     * @return The parameter descriptor for the given default value and unit.
     */
    public ParameterDescriptor<Double> createUnbounded(final double defaultValue, final Unit<?> unit) {
        final Range<Double> valueDomain;
        if (unit != null) {
            valueDomain = MeasurementRange.create(Double.NEGATIVE_INFINITY, false, Double.POSITIVE_INFINITY, false, unit);
        } else {
            valueDomain = null;
        }
        return new DefaultParameterDescriptor<>(properties, Double.class,
                valueDomain, null, Numerics.valueOf(defaultValue), required);
    }

    /**
     * Constructs a descriptor for a parameter in a range of values.
     *
     * @param <T>          The compile-time type of the {@code valueClass} argument.
     * @param valueClass   The class that describe the type of the parameter values.
     * @param minimumValue The minimum parameter value (inclusive), or {@code null} if none.
     * @param maximumValue The maximum parameter value (inclusive), or {@code null} if none.
     * @param defaultValue The default value for the parameter, or {@code null} if none.
     * @return The parameter descriptor for the given range of values.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Comparable<? super T>> ParameterDescriptor<T> create(final Class<T> valueClass,
            final T minimumValue, final T maximumValue, final T defaultValue)
    {
        final Range<T> valueDomain;
        if (minimumValue == null && maximumValue == null) {
            valueDomain = null;
        } else if (Number.class.isAssignableFrom(valueClass)) {
            valueDomain = new NumberRange((Class) valueClass, (Number) minimumValue, true, (Number) maximumValue, true);
        } else {
            valueDomain = new Range<>(valueClass, minimumValue, true, maximumValue, true);
        }
        return new DefaultParameterDescriptor<>(properties, valueClass, valueDomain, null, defaultValue, required);
    }

    /**
     * Constructs a descriptor for a parameter having a set of valid values.
     * The descriptor has no minimal or maximal value and no unit.
     *
     * @param <T>          The compile-time type of the {@code valueClass} argument.
     * @param valueClass   The class that describe the type of the parameter values.
     * @param validValues  A finite set of valid values (usually from a {@linkplain CodeList code list})
     *                     or {@code null} if it doesn't apply.
     * @param defaultValue The default value for the parameter, or {@code null} if none.
     * @return The parameter descriptor for the given set of valid values.
     */
    public <T> ParameterDescriptor<T> createForEnumeration(final Class<T> valueClass, final T[] validValues, final T defaultValue) {
        return new DefaultParameterDescriptor<>(properties, valueClass, null, validValues, defaultValue, required);
    }
}
