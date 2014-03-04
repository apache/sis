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
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import javax.measure.unit.Unit;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Range;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.internal.system.DefaultFactories.NAMES;

// Related to JDK7
import java.util.Objects;


/**
 * Provides convenience methods for easier {@link DefaultParameterDescriptor} instantiations.
 * This builder can be helpful for map projection <em>providers</em>, or for implementation of
 * any process that use parameters. Map projection or process <em>users</em> do not need this
 * builder since they can invoke {@link ParameterDescriptor#createValue()} on the descriptor
 * provided by the implementor.
 *
 * {@section Identification properties}
 * Each parameter must have a name, which can be specified by any of the {@code name(…)} methods.
 * Parameters can optionally have an arbitrary amount of aliases, which are also specified by the
 * {@code name(…)} methods — each call after the first one adds an alias.
 *
 * <p>Parameters can also have an arbitrary amount of identifiers, which are specified by the
 * {@code identifier(…)} methods. Like names, more than one identifier can be added by invoking
 * the method many time.</p>
 *
 * <p>Parameters can have at most one remark, which is specified by the {@code remarks(…)} method.</p>
 *
 * <p>All the above-cited properties are cleared after a call to any {@code create(…)} method,
 * since those properties are specific to the each parameter. Other properties like codespace,
 * version and cardinality are left unchanged because they may be shared by many parameters.</p>
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
     * The aliases.
     */
    private final List<GenericName> aliases;

    /**
     * The identifiers.
     */
    private final List<ReferenceIdentifier> identifiers;

    /**
     * The codespace as a {@code NameSpace} object, or {@code null} if not yet created.
     *
     * @see #namespace()
     */
    private NameSpace namespace;

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
        properties  = new HashMap<>(8);
        aliases     = new ArrayList<>(4);
        identifiers = new ArrayList<>(4);
    }

    /**
     * Clears the identification information.
     * This does not clear the codespace, version and cardinality (mandatory versus optional) properties.
     */
    private void clearIdentification() {
        properties .put(ParameterDescriptor.NAME_KEY, null);
        properties .remove(ParameterDescriptor.REMARKS_KEY);
        aliases    .clear();
        identifiers.clear();
    }

    /**
     * Sets the property value for the given key, if a change is still possible. The check for change permission
     * is needed for all keys defined in the {@link ReferenceIdentifier} interface. This check is not needed for
     * other keys, so callers do not need to invoke this method for other keys.
     *
     * @param  key The key of the property to set.
     * @param  value The value to set.
     * @return {@code true} if the property changed as a result of this method call.
     * @throws IllegalStateException if a new value is specified in a phase where the value can not be changed.
     */
    private boolean setProperty(final String key, final Object value) throws IllegalStateException {
        if (Objects.equals(properties.get(key), value)) {
            return false;
        }
        if (properties.get(ParameterDescriptor.NAME_KEY) != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, key));
        }
        properties.put(key, value);
        return true;
    }

    /**
     * Returns the namespace, creating it when first needed.
     */
    private NameSpace namespace() {
        if (namespace == null) {
            final String codespace = (String) properties.get(ReferenceIdentifier.CODESPACE_KEY);
            if (codespace != null) {
                namespace = NAMES.createNameSpace(NAMES.createLocalName(null, codespace), null);
            }
        }
        return namespace;
    }

    /**
     * Sets the authority and code space. This method is typically invoked only once, since
     * a group of parameters often uses the same code space for all individual parameters.
     *
     * <p><b>Condition:</b>
     * this method can not be invoked after one or more names or identifiers have been added (by calls to the
     * {@code name(…)} or {@code identifier(…)} methods) for the next descriptor to create. This method can be
     * invoked again after the name, aliases and identifiers have been cleared by a call to {@code createXXX(…)}.</p>
     *
     * <p><b>Life cycle:</b>
     * this property is kept unchanged until this {@code codespace(…)} method is invoked again.</p>
     *
     * @param  authority Bibliographic reference to the authority defining the codes, or {@code null} if none.
     * @param  codespace The parameter codespace, or {@code null} for inferring it from the authority.
     * @return {@code this}, for method call chaining.
     * @throws IllegalStateException if {@code name(…)} or {@code identifier(…)} has been invoked at least
     *         once since builder construction or since the last call to a {@code createXXX(…)} method.
     */
    public DescriptorBuilder codespace(final Citation authority, final String codespace) {
        if (!setProperty(ReferenceIdentifier.CODESPACE_KEY, codespace)) {
            namespace = null;
        }
        setProperty(ReferenceIdentifier.AUTHORITY_KEY, authority);
        return this;
    }

    /**
     * Sets the version of parameter definitions. This method is typically invoked only once,
     * since a group of parameters often uses the same version for all individual parameters.
     *
     * <p><b>Condition:</b>
     * this method can not be invoked after one or more names or identifiers have been added (by calls to the
     * {@code name(…)} or {@code identifier(…)} methods) for the next descriptor to create. This method can be
     * invoked again after the name, aliases and identifiers have been cleared by a call to {@code createXXX(…)}.</p>
     *
     * <p><b>Life cycle:</b>
     * this property is kept unchanged until this {@code version(…)} method is invoked again.</p>
     *
     * @param  version The version of code definitions, or {@code null} if none.
     * @return {@code this}, for method call chaining.
     * @throws IllegalStateException if {@code name(…)} or {@code identifier(…)} has been invoked at least
     *         once since builder construction or since the last call to a {@code createXXX(…)} method.
     */
    public DescriptorBuilder version(final String version) {
        setProperty(ReferenceIdentifier.VERSION_KEY, version);
        return this;
    }

    /**
     * Adds a parameter name given by a {@code String} or {@code InternationalString}.
     * The given string will be combined with the authority, {@linkplain #codespace(Citation, String) code space}
     * and {@linkplain #version(String) version} information for creating the {@link ReferenceIdentifier} or
     * {@link GenericName} object.
     *
     * {@section Name and aliases}
     * This method can be invoked many times. The first invocation sets the
     * {@linkplain AbstractIdentifiedObject#getName() primary name}, and
     * all subsequent invocations add an {@linkplain AbstractIdentifiedObject#getAlias() alias}.
     *
     * <p><b>Life cycle:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  name The parameter name.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder name(final CharSequence name) {
        ensureNonNull("name", name);
        final Object old = properties.put(ParameterDescriptor.NAME_KEY, name.toString());
        if (old != null) {
            properties.put(ParameterDescriptor.NAME_KEY, old); // Restore previous value.
            aliases.add(name instanceof GenericName ? (GenericName) name : NAMES.createLocalName(namespace(), name));
        }
        return this;
    }

    /**
     * Adds a parameter name in an alternative namespace. This method is typically invoked for
     * {@linkplain AbstractIdentifiedObject#getAlias() aliases} defined after the primary name.
     *
     * <div class="note"><b>Example:</b>
     * The "<cite>Longitude of natural origin</cite>" parameter defined by EPSG is named differently
     * by OGC and GeoTIFF. Those alternative names can be defined as below:
     *
     * {@preformat java
     *   builder.name("Longitude of natural origin")        // Primary name in builder default namespace.
     *          .name(Citations.OGC, "central_meridian")    // First alias in "OGC" namespace.
     *          .name(Citations.GEOTIFF, "NatOriginLong");  // Second alias in "GeoTIFF" namespace.
     * }
     *
     * In this example, {@code "central_meridian"} will be the
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#tip() tip} and {@code "OGC"} will be the
     * {@linkplain org.apache.sis.util.iso.DefaultScopedName#head() head} of the first alias.</div>
     *
     * <p><b>Life cycle:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  authority Bibliographic reference to the authority defining the codes, or {@code null} if none.
     * @param  name The parameter alias as a name in the namespace of the given authority.
     * @return {@code this}, for method call chaining.
     *
     * @see #identifier(Citation, String)
     */
    public DescriptorBuilder name(final Citation authority, final CharSequence name) {
        ensureNonNull("name", name);
        final NamedIdentifier identifier;
        if (name instanceof InternationalString) {
            identifier = new NamedIdentifier(authority, (InternationalString) name);
        } else {
            identifier = new NamedIdentifier(authority, name.toString());
        }
        final Object old = properties.put(ParameterDescriptor.NAME_KEY, identifier);
        if (old != null) {
            properties.put(ParameterDescriptor.NAME_KEY, old); // Restore previous value.
            aliases.add(identifier);
        }
        return this;
    }

    /**
     * Adds a parameter name fully specified by the given identifier.
     * This method ignores the authority, {@linkplain #codespace(Citation, String) code space} or
     * {@linkplain #version(String) version} specified to this builder (if any), since the given
     * identifier already contains those information.
     *
     * {@section Name and aliases}
     * This method can be invoked many times. The first invocation sets the
     * {@linkplain AbstractIdentifiedObject#getName() primary name} to the given value, and
     * all subsequent invocations add an {@linkplain AbstractIdentifiedObject#getAlias() alias}.
     *
     * <p><b>Life cycle:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  name The parameter name as an identifier.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder name(final ReferenceIdentifier name) {
        ensureNonNull("name", name);
        final Object old = properties.put(ParameterDescriptor.NAME_KEY, name);
        if (old != null) {
            properties.put(ParameterDescriptor.NAME_KEY, old); // Restore previous value.
            aliases.add(name instanceof GenericName ? (GenericName) name : new NamedIdentifier(name));
        }
        return this;
    }

    /**
     * Adds a parameter name fully specified by the given generic name.
     * This method ignores the authority, {@linkplain #codespace(Citation, String) code space} or
     * {@linkplain #version(String) version} specified to this builder (if any), since the given
     * generic name already contains those information.
     *
     * {@section Name and aliases}
     * This method can be invoked many times. The first invocation sets the
     * {@linkplain AbstractIdentifiedObject#getName() primary name} to the given value, and
     * all subsequent invocations add an {@linkplain AbstractIdentifiedObject#getAlias() alias}.
     *
     * <p><b>Life cycle:</b>
     * the name and all aliases are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  name The parameter name as an identifier.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder name(final GenericName name) {
        ensureNonNull("name", name);
        if (properties.get(ParameterDescriptor.NAME_KEY) == null) {
            properties.put(ParameterDescriptor.NAME_KEY, new NamedIdentifier(name));
        } else {
            aliases.add(name);
        }
        return this;
    }

    /**
     * Adds a parameter identifier given by a {@code String}.
     * The given string will be combined with the authority, {@linkplain #codespace(Citation, String) code space}
     * and {@linkplain #version(String) version} information for creating the {@link ReferenceIdentifier} object.
     *
     * <p><b>Life cycle:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  identifier The parameter identifier.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder identifier(final String identifier) {
        ensureNonNull("identifier", identifier);
        identifiers.add(new ImmutableIdentifier((Citation) properties.get(ReferenceIdentifier.AUTHORITY_KEY),
                (String) properties.get(ReferenceIdentifier.CODESPACE_KEY), identifier));
        return this;
    }

    /**
     * Adds a parameter identifier in an alternative namespace. This method is typically invoked in
     * complement to {@link #name(Citation, CharSequence)}.
     *
     * <p><b>Life cycle:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  authority Bibliographic reference to the authority defining the codes, or {@code null} if none.
     * @param  identifier The parameter identifier as a code in the namespace of the given authority.
     * @return {@code this}, for method call chaining.
     *
     * @see #name(Citation, CharSequence)
     */
    public DescriptorBuilder identifier(final Citation authority, final String identifier) {
        ensureNonNull("identifier", identifier);
        identifiers.add(new ImmutableIdentifier(authority, Citations.getIdentifier(authority), identifier));
        return this;
    }

    /**
     * Adds a parameter identifier fully specified by the given identifier.
     * This method ignores the authority, {@linkplain #codespace(Citation, String) code space} or
     * {@linkplain #version(String) version} specified to this builder (if any), since the given
     * identifier already contains those information.
     *
     * <p><b>Life cycle:</b>
     * all identifiers are cleared after a {@code createXXX(…)} method has been invoked.</p>
     *
     * @param  identifier The parameter identifier.
     * @return {@code this}, for method call chaining.
     */
    public DescriptorBuilder identifier(final ReferenceIdentifier identifier) {
        ensureNonNull("identifier", identifier);
        identifiers.add(identifier);
        return this;
    }

    /**
     * Sets remarks as a {@code String} or {@code InternationalString} instance.
     * Calls to this method overwrite any previous value.
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
        return create(Double.class, valueDomain, null, Double.valueOf(defaultValue));
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
        return create(Double.class, valueDomain, null, Double.valueOf(defaultValue));
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
        return create(Double.class, valueDomain, null, Double.valueOf(defaultValue));
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
        return create(Integer.class, NumberRange.create(minimumValue, true, maximumValue, true), null, defaultValue);
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
        return create(valueClass, valueDomain, null, defaultValue);
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
        return create(valueClass, null, validValues, defaultValue);
    }

    /**
     * Invoked by all {@code createXXX(…)} method for creating the descriptor from the properties currently set
     * in this builder.
     */
    private <T> ParameterDescriptor<T> create(final Class<T> valueClass, final Range<?> valueDomain,
            final T[] validValues, final T defaultValue)
    {
        properties.put(ParameterDescriptor.ALIAS_KEY, aliases.toArray(new GenericName[aliases.size()]));
        properties.put(ParameterDescriptor.IDENTIFIERS_KEY, identifiers.toArray(new ReferenceIdentifier[identifiers.size()]));
        final ParameterDescriptor<T> descriptor = new DefaultParameterDescriptor<>(
                properties, valueClass, valueDomain, validValues, defaultValue, required);
        clearIdentification();
        return descriptor;
    }
}
