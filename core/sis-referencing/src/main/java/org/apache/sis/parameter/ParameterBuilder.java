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

import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Range;
import org.apache.sis.referencing.Builder;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Provides convenience methods for easier {@code ParameterDescriptorGroup} instantiations.
 * This builder can be helpful for map projection <em>providers</em>, or for implementation of
 * any process that use parameters. Map projection or process <em>users</em> do not need this
 * builder since they can invoke {@link ParameterDescriptor#createValue()} on the descriptor
 * provided by the implementor.
 *
 * {@section Identification properties}
 * Each parameter must have a name, which can be specified by any of the {@code addName(…)} methods.
 * Parameters can optionally have an arbitrary amount of aliases, which are also specified by the
 * {@code addName(…)} methods — each call after the first one adds an alias.
 *
 * <p>Parameters can also have an arbitrary amount of identifiers, which are specified by the
 * {@code addIdentifier(…)} methods. Like names, more than one identifier can be added by invoking
 * the method many time.</p>
 *
 * <p>Parameters can have at most one remark, which is specified by the {@code setRemarks(…)} method.</p>
 *
 * <p>All the above-cited properties are cleared after a call to any {@code createXXX(…)} method,
 * since those properties are specific to the each parameter. Other properties like codespace,
 * version and cardinality are left unchanged because they may be shared by many parameters.</p>
 *
 * {@section Usage example}
 * Parameter descriptors are typically grouped in a {@link ParameterDescriptorGroup}.
 * All parameters usually have the same namespace, which can be declared only once.
 * The following example creates parameters for "<cite>Mercator (variant A)</cite>" projection method (EPSG:9804)
 * with all parameter names in the "EPSG" namespace. The default values define a projection centered on (0°,0°),
 * with no scale factor and no false easting/northing. The projection is valid from 80°S to 84°N and on all the
 * longitude range (±180°).
 *
 * {@preformat java
 *   ParameterBuilder builder = new ParameterBuilder();
 *   builder.setCodeSpace(Citations.OGP, "EPSG").setRequired(true);
 *   ParameterDescriptor<Double>[] parameters = {
 *       builder.addName("Latitude of natural origin")    .createBounded( -80,  +84, 0, NonSI.DEGREE_ANGLE),
 *       builder.addName("Longitude of natural origin")   .createBounded(-180, +180, 0, NonSI.DEGREE_ANGLE),
 *       builder.addName("Scale factor at natural origin").createStrictlyPositive(1, Unit.ONE),
 *       builder.addName("False easting")                 .create(0, SI.METRE),
 *       builder.addName("False northing")                .create(0, SI.METRE)
 *   };
 * }
 *
 * Parameters often have more than one name, because different softwares or standards use different conventions.
 * In the above example, the line creating the <cite>Longitude of natural origin</cite> parameter could be replaced
 * by the following code in order to declare its aliases:
 *
 * {@preformat java
 *   builder.addName("Longitude of natural origin")        // Primary name in builder default namespace.
 *          .addName(Citations.OGC, "central_meridian")    // First alias in "OGC" namespace.
 *          .addName(Citations.GEOTIFF, "NatOriginLong")   // Second alias in "GeoTIFF" namespace.
 *          .addIdentifier("8802")                         // Primary key in EPSG database.
 *          .createBounded(-80, +84, 0, NonSI.DEGREE_ANGLE);
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class ParameterBuilder extends Builder<ParameterBuilder> {
    /**
     * {@code true} if the parameter is mandatory, or {@code false} if optional.
     *
     * @see #setRequired(boolean)
     */
    private boolean required;

    /**
     * Creates a new builder.
     */
    public ParameterBuilder() {
    }

    /**
     * Sets whether the parameter is mandatory or optional.
     * This property determines the {@linkplain DefaultParameterDescriptor#getMinimumOccurs() minimum number
     * of times} that values are required, which will be 0 for an optional parameter and 1 for a mandatory one.
     *
     * <p><b>Default value:</b>
     * If this method is never invoked, then the default value is {@code false}.</p>
     *
     * <p><b>Lifetime:</b>
     * this property is kept unchanged until this {@code setRequired(…)} method is invoked again.</p>
     *
     * @param  required {@code true} for a mandatory parameter, or {@code false} for an optional one.
     * @return {@code this}, for method call chaining.
     */
    public ParameterBuilder setRequired(final boolean required) {
        this.required = required;
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
     * Creates a descriptor for values of the given type without domain restriction.
     *
     * @param  <T>          The compile-time type of the {@code valueClass} argument.
     * @param  valueClass   The class that describe the type of the parameter values.
     * @param  defaultValue The default value for the parameter, or {@code null} if none.
     * @return The parameter descriptor for the given default value and unit.
     */
    public <T> ParameterDescriptor<T> create(final Class<T> valueClass, final T defaultValue) {
        return create(valueClass, null, null, defaultValue);
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
     * Creates a descriptor for values in the domain represented by the given {@code Range} object.
     * This method allows to specify whether the minimum and maximum values are inclusive or not.
     *
     * @param  <T>          The type of the parameter values.
     * @param  valueDomain  The minimum value, maximum value and unit of measurement.
     * @param  defaultValue The default value for the parameter, or {@code null} if none.
     * @return The parameter descriptor for the given domain of values.
     */
    public <T extends Comparable<? super T>> ParameterDescriptor<T> createBounded(
            final Range<T> valueDomain, final T defaultValue)
    {
        ensureNonNull("valueDomain", valueDomain);
        return create(valueDomain.getElementType(), valueDomain, null, defaultValue);
    }

    /**
     * Creates a descriptor for a parameter restricted to a set of valid values.
     * The descriptor has no minimal or maximal value and no unit.
     *
     * <p>The {@code validValues} property is mostly for restricting values to
     * a {@linkplain org.opengis.util.CodeList code list} or enumeration subset.
     * It is not necessary to provide this property when all values from the code list or enumeration are valid.</p>
     *
     * @param  <T>          The compile-time type of the {@code valueClass} argument.
     * @param  valueClass   The class that describe the type of the parameter values.
     * @param  validValues  A finite set of valid values (usually from a {@linkplain CodeList code list})
     *                      or {@code null} if it doesn't apply.
     * @param  defaultValue The default value for the parameter, or {@code null} if none.
     * @return The parameter descriptor for the given set of valid values.
     */
    public <T> ParameterDescriptor<T> createEnumerated(final Class<T> valueClass, final T[] validValues, final T defaultValue) {
        ensureNonNull("valueClass", valueClass);
        return create(valueClass, null, validValues, defaultValue);
    }

    /**
     * Invoked by all {@code createXXX(…)} methods for creating the descriptor from the properties
     * currently set in this builder. Identification information are cleared after this method call.
     */
    private <T> ParameterDescriptor<T> create(final Class<T> valueClass, final Range<?> valueDomain,
            final T[] validValues, final T defaultValue)
    {
        onCreate(false);
        final ParameterDescriptor<T> descriptor = new DefaultParameterDescriptor<>(
                properties, valueClass, valueDomain, validValues, defaultValue, required);
        onCreate(true);
        return descriptor;
    }
}
