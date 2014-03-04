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
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Range;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Provides convenience methods for easier {@link DefaultParameterDescriptor} instantiations.
 * This builder can be helpful for map projection <em>providers</em>, or for implementation of
 * any process that use parameters. Map projection or process <em>users</em> should rather invoke
 * {@link ParameterDescriptor#createValue()} on the descriptor provided by the implementor.
 *
 * {@section Usage example}
 * Parameter descriptors are typically grouped in a {@link ParameterDescriptorGroup}.
 * All parameters usually have the same namespace, which can be declared only once.
 * The following example creates parameters for "<cite>Mercator (variant A)</cite>" projection method (EPSG:9804)
 * with all parameter names in the "EPSG" namespace. The default values define a projection centered on (0°,0°)
 * with no scale factor and no false easting/northing.
 *
 * {@preformat java
 *   DescriptorBuilder builder = new DescriptorBuilder();
 *   builder.codespace(Citations.OGP, "EPSG").mandatory();
 *   ParameterDescriptor<Double>[] parameters = {
 *       builder.name("Latitude of natural origin")     .createBounded( -80,  +80, 0, NonSI.DEGREE_ANGLE),
 *       builder.name("Longitude of natural origin")    .createBounded(-180, +180, 0, NonSI.DEGREE_ANGLE),
 *       builder.name("Scale factor at natural origin") .createStrictlyPositive(1, Unit.ONE),
 *       builder.name("False easting")                  .create(0, SI.METRE),
 *       builder.name("False northing")                 .create(0, SI.METRE)
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
     *
     * @see #mandatory()
     * @see #optional()
     */
    private boolean required;

    /**
     * Creates a new builder.
     */
    public DescriptorBuilder() {
        properties = new HashMap<>(4);
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
     * Sets the aliases as {@code String} or {@code InternationalString} instances.
     * An arbitrary amount of aliases can be specified. Each alias is parsed using
     * using the {@value org.apache.sis.util.iso.DefaultNameSpace#DEFAULT_SEPARATOR}
     * separator.
     *
     * <div class="note"><b>Example:</b>
     * The "<cite>Longitude of natural origin</cite>" parameter defined by EPSG is named differently
     * by OGC and GeoTIFF. Those alternative names can be defined as below:
     *
     * {@preformat java
     *   builder.aliases("OGC:central_meridian", "GeoTIFF:NatOriginLong");
     * }
     *
     * In this example, {@code "central_meridian"} will be the name
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#tip() tip} and {@code "OGC"} will be the name
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#head() head}.</div>
     *
     * @param  aliases The aliases, or {@code null} or empty if none.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder aliases(final CharSequence... aliases) {
        properties.put(ParameterDescriptor.ALIAS_KEY, aliases);
        return this;
    }

    /**
     * Sets aliases as {@code GenericName} instances.
     *
     * @param  aliases The aliases, or {@code null} or empty if none.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder aliases(final GenericName... aliases) {
        properties.put(ParameterDescriptor.ALIAS_KEY, aliases);
        return this;
    }

    /**
     * Sets the identifiers as {@code ReferenceIdentifier} instances.
     * This information is optional and can be specified as a complement to the parameter name.
     *
     * @param  identifiers The identifiers, or {@code null} or empty if none.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder identifiers(final ReferenceIdentifier... identifiers) {
        properties.put(ParameterDescriptor.IDENTIFIERS_KEY, identifiers);
        return this;
    }

    /**
     * Sets remarks as {@code String} or {@code InternationalString} instances.
     *
     * @param  remarks The remarks, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder remarks(final CharSequence remarks) {
        properties.put(ParameterDescriptor.REMARKS_KEY, remarks);
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
     * Creates a descriptor for floating point values without domain restriction.
     * All {@code double} values are considered valid.
     *
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  unit         The default unit, or {@code null} if none.
     * @return The parameter descriptor for the given default value and unit.
     */
    public ParameterDescriptor<Double> create(final double defaultValue, final Unit<?> unit) {
        final Range<Double> valueDomain;
        if (unit != null) {
            valueDomain = MeasurementRange.create(Double.NEGATIVE_INFINITY, false, Double.POSITIVE_INFINITY, false, unit);
        } else {
            valueDomain = null;
        }
        return new DefaultParameterDescriptor<>(properties, Double.class,
                valueDomain, null, Double.valueOf(defaultValue), required);
    }

    /**
     * Creates a descriptor for floating point values greater than zero.
     * The zero value is not considered valid. There is no maximal value.
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
                valueDomain, null, Double.valueOf(defaultValue), required);
    }

    /**
     * Creates a descriptor for floating point values restricted to the given domain.
     *
     * @param  minimumValue The minimum parameter value (inclusive), or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  maximumValue The maximum parameter value (inclusive), or {@link Double#POSITIVE_INFINITY} if none.
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  unit         The unit for default, minimum and maximum values, or {@code null} if none.
     * @return The parameter descriptor for the given domain of values.
     */
    public ParameterDescriptor<Double> createBounded(final double minimumValue, final double maximumValue,
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
                valueDomain, null, Double.valueOf(defaultValue), required);
    }

    /**
     * Creates a descriptor for integer values restricted to the given domain.
     *
     * @param  minimumValue The minimum parameter value (inclusive).
     * @param  maximumValue The maximum parameter value (inclusive).
     * @param  defaultValue The default value for the parameter.
     * @return The parameter descriptor for the given domain of values.
     */
    public ParameterDescriptor<Integer> createBounded(final int minimumValue, final int maximumValue,
            final int defaultValue)
    {
        return new DefaultParameterDescriptor<>(properties, Integer.class,
                NumberRange.create(minimumValue, true, maximumValue, true), null, defaultValue, required);
    }

    /**
     * Creates a descriptor for values of the given type restricted to the given domain.
     *
     * @param  <T>          The compile-time type of the {@code valueClass} argument.
     * @param  valueClass   The class that describe the type of the parameter values.
     * @param  minimumValue The minimum parameter value (inclusive), or {@code null} if none.
     * @param  maximumValue The maximum parameter value (inclusive), or {@code null} if none.
     * @param  defaultValue The default value for the parameter, or {@code null} if none.
     * @return The parameter descriptor for the given domain of values.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Comparable<? super T>> ParameterDescriptor<T> createBounded(final Class<T> valueClass,
            final T minimumValue, final T maximumValue, final T defaultValue)
    {
        ensureNonNull("valueClass", valueClass);
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
     * Creates a descriptor for a parameter restricted to a set of valid values.
     * The descriptor has no minimal or maximal value and no unit.
     *
     * <p>The {@code validValues} property is mostly for restricting values to
     * a {@linkplain org.opengis.util.CodeList code list} or enumeration subset.
     * It is not necessary to provide this property when all values from the code list or enumeration are valid.</p>
     *
     * @param <T>          The compile-time type of the {@code valueClass} argument.
     * @param valueClass   The class that describe the type of the parameter values.
     * @param validValues  A finite set of valid values (usually from a {@linkplain CodeList code list})
     *                     or {@code null} if it doesn't apply.
     * @param defaultValue The default value for the parameter, or {@code null} if none.
     * @return The parameter descriptor for the given set of valid values.
     */
    public <T> ParameterDescriptor<T> createEnumerated(final Class<T> valueClass, final T[] validValues, final T defaultValue) {
        return new DefaultParameterDescriptor<>(properties, valueClass, null, validValues, defaultValue, required);
    }
}
