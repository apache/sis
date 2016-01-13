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
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Range;
import org.apache.sis.referencing.Builder;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Helper class for {@linkplain DefaultParameterDescriptor parameter descriptor} instantiations.
 * This builder can be helpful to operation or process <em>providers</em> (e.g. map projection
 * implementors). Operation <em>users</em> do not need this builder since they can invoke
 * {@link ParameterDescriptor#createValue()} on the descriptor provided by the implementor.
 *
 * <div class="section">Identification properties</div>
 * The following properties are cleared after a call to any {@code createXXX(…)} method,
 * since those properties are specific to the each parameter. Other properties like codespace,
 * version and cardinality are left unchanged because they may be shared by many parameters.
 *
 * <ul class="verbose">
 *   <li><b>{@linkplain DefaultParameterDescriptor#getName() Names}:</b>
 *   each parameter must have a name, which can be specified by any of the {@link #addName(CharSequence)
 *   addName(…)} methods. Parameters can optionally have an arbitrary amount of aliases, which are also specified
 *   by the {@code addName(…)} methods. Each call after the first one adds an alias.</li>
 *
 *   <li><b>{@linkplain DefaultParameterDescriptor#getIdentifiers() Identifiers}:</b>
 *   parameters can also have an arbitrary amount of identifiers, which are specified by any of the
 *   {@link #addIdentifier(String) addIdentifier(…)} methods. Like names, more than one identifier can be
 *   added by invoking the method many time.</li>
 *
 *   <li><b>{@linkplain DefaultParameterDescriptor#getRemarks() Remarks}:</b>
 *   parameters can have at most one remark, which is specified by the {@code setRemarks(…)} method.</li>
 * </ul>
 *
 *
 * <div class="section">Usage example</div>
 * Parameter descriptors are typically grouped in a {@link ParameterDescriptorGroup}.
 * All parameters usually have the same namespace, which can be declared only once.
 * The following example creates parameters for <cite>"Mercator (variant A)"</cite>
 * projection method (EPSG:9804), previously known as <cite>"Mercator (1SP)"</cite>,
 * centered by default on (0°,0°) with no scale factor and no false easting/northing.
 * The projection is valid from 80°S to 84°N and on all the longitude range (±180°).
 * In this example, the <cite>"Longitude of natural origin"</cite> parameter is giving different aliases
 * for illustrating the case of different softwares or standards using different conventions.
 *
 * {@preformat java
 *   ParameterBuilder builder = new ParameterBuilder();
 *   builder.setCodeSpace(Citations.EPSG, "EPSG")                   // The default namespace to be used below.
 *          .setRequired(true);                                     // All parameters will be considered mandatory.
 *
 *   // Constructs the list of parameters.
 *   ParameterDescriptor<?>[] parameters = {
 *       builder.addName("Latitude of natural origin")              // Name in the default namespace ("EPSG" in this example).
 *              .createBounded( -80,  +84, 0, NonSI.DEGREE_ANGLE),  // Latitude of Mercator projection can not go to the poles.
 *
 *       builder.addIdentifier("8802")                              // Primary key in default namespace ("EPSG" in this example).
 *              .addName("Longitude of natural origin")             // Primary name in default namespace ("EPSG" in this example).
 *              .addName(Citations.OGC, "central_meridian")         // First alias in "OGC" namespace.
 *              .addName(Citations.GEOTIFF, "NatOriginLong")        // Second alias in "GeoTIFF" namespace.
 *              .createBounded(-180, +180, 0, NonSI.DEGREE_ANGLE),  // Projection is valid on all the longitude range (±180°).
 *
 *       builder.addName("Scale factor at natural origin")
 *              .createStrictlyPositive(1, Unit.ONE),
 *
 *       builder.addName("False easting")
 *              .create(0, SI.METRE),
 *
 *       builder.addName("False northing")
 *              .create(0, SI.METRE)
 *   };
 *
 *   // Put all above parameters in a group.
 *   ParameterDescriptorGroup group = builder
 *           .addIdentifier("9804")                                 // Defined in implicit "EPSG" namespace.
 *           .addName      ("Mercator (variant A)")                 // Defined in implicit "EPSG" namespace.
 *           .addName      ("Mercator (1SP)")                       // Defined in implicit "EPSG" namespace.
 *           .addName      (Citations.OGC, "Mercator_1SP")          // "OGC" namespace explicitly shown by toString().
 *           .addName      (Citations.GEOTIFF, "CT_Mercator")       // "GeoTIFF" namespace explicitly shown by toString().
 *           .addIdentifier(Citations.GEOTIFF, "7")
 *           .setRemarks   ("The “Mercator (1SP)” method name was used prior to October 2010.")
 *           .createGroupForMapProjection(parameters);
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
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
     * Creates a new builder initialized to properties of the given object.
     *
     * @param descriptor The descriptor from which to inherit properties, or {@code null}.
     *
     * @since 0.6
     */
    public ParameterBuilder(final GeneralParameterDescriptor descriptor) {
        super(descriptor);
        if (descriptor != null) {
            required = descriptor.getMinimumOccurs() != 0;
        }
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
     * Boxes the given value if non-NaN, or returns {@code null} if the value is {@code NaN}.
     */
    private static Double valueOf(final double value) {
        return Double.isNaN(value) ? null : value;
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
        return create(Double.class, valueDomain, null, valueOf(defaultValue));
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
            valueDomain = MeasurementRange.createGreaterThan(0.0, unit);
        } else {
            valueDomain = NumberRange.create(0.0, false, Double.POSITIVE_INFINITY, false);
        }
        return create(Double.class, valueDomain, null, valueOf(defaultValue));
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
        return create(Double.class, valueDomain, null, valueOf(defaultValue));
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
            valueDomain = new Range<T>(valueClass, minimumValue, true, maximumValue, true);
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
     * a {@linkplain org.opengis.util.CodeList code list} or {@linkplain Enum enumeration} subset.
     * It is not necessary to provide this property when all values from the code list or enumeration are valid.</p>
     *
     * @param  <T>          The compile-time type of the {@code valueClass} argument.
     * @param  valueClass   The class that describe the type of the parameter values.
     * @param  validValues  A finite set of valid values (usually from a code list or enumeration)
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
        final ParameterDescriptor<T> descriptor;
        onCreate(false);
        try {
            descriptor = new DefaultParameterDescriptor<T>(properties, required ? 1 : 0, 1,
                    valueClass, valueDomain, validValues, defaultValue);
        } finally {
            onCreate(true);
        }
        return descriptor;
    }

    /**
     * Creates a descriptor group for the given cardinality and parameters.
     *
     * @param  minimumOccurs The {@linkplain DefaultParameterDescriptorGroup#getMinimumOccurs() minimum}
     *                       number of times that values for this parameter group are required.
     * @param  maximumOccurs The {@linkplain DefaultParameterDescriptorGroup#getMaximumOccurs() maximum}
     *                       number of times that values for this parameter group are required.
     * @param  parameters    The {@linkplain DefaultParameterDescriptorGroup#descriptors() parameter descriptors}
     *                       for the group to create.
     * @return The parameter descriptor group.
     */
    public ParameterDescriptorGroup createGroup(final int minimumOccurs, final int maximumOccurs,
            final GeneralParameterDescriptor... parameters)
    {
        final ParameterDescriptorGroup group;
        onCreate(false);
        try {
            group = new DefaultParameterDescriptorGroup(properties, minimumOccurs, maximumOccurs, parameters);
        } finally {
            onCreate(true);
        }
        return group;
    }

    /**
     * Creates a descriptor group for the given parameters. This is a convenience method for
     * {@link #createGroup(int, int, GeneralParameterDescriptor[])} with a cardinality of [0 … 1]
     * or [1 … 1] depending on the value given to the last call to {@link #setRequired(boolean)}.
     *
     * @param  parameters The {@linkplain DefaultParameterDescriptorGroup#descriptors() parameter descriptors}
     *         for the group to create.
     * @return The parameter descriptor group.
     */
    public ParameterDescriptorGroup createGroup(final GeneralParameterDescriptor... parameters) {
        return createGroup(required ? 1 : 0, 1, parameters);
    }

    /**
     * Creates a descriptor group with the same parameters than another group. This is a convenience constructor
     * for operations that expect the same parameters than another operation, but perform a different process.
     *
     * <div class="note"><b>Example:</b>
     * the various <cite>"Coordinate Frame Rotation"</cite> variants (EPSG codes 1032, 1038 and 9607)
     * expect the same parameters than their <cite>"Position Vector transformation"</cite> counterpart
     * (EPSG codes 1033, 1037 and 9606) but perform the rotation in the opposite direction.</div>
     *
     * @param parameters The existing group from which to copy the parameters.
     * @return The parameter descriptor group.
     *
     * @since 0.7
     */
    public ParameterDescriptorGroup createGroupWithSameParameters(final ParameterDescriptorGroup parameters) {
        final ParameterDescriptorGroup group;
        onCreate(false);
        try {
            group = new DefaultParameterDescriptorGroup(properties, parameters);
        } finally {
            onCreate(true);
        }
        return group;
    }

    /**
     * Creates a descriptor group for a map projection. This method automatically adds mandatory parameters
     * for the <cite>semi-major</cite> and <cite>semi-minor axis length</cite>. Those parameters are usually
     * not explicitely included in parameter definitions since the axis lengths can be inferred from the
     * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoid}.
     * However {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory} needs them.
     *
     * <p>In addition, this method adds hidden parameters for alternative ways to express some standard parameters.
     * Those hidden parameters never appear in the {@linkplain DefaultParameterDescriptorGroup#descriptors() list
     * of parameters}. However when one of those parameters is read or written, the work will be delegated to the
     * standard parameters.</p>
     *
     * <table class="sis">
     *   <caption>Parameters automatically added by this method</caption>
     *   <tr><th>Name</th>                         <th>Visibility</th> <th>Comment</th></tr>
     *   <tr><td>{@code "semi_major"}</td>         <td>Always</td>     <td>Standard parameter defined by WKT 1.</td></tr>
     *   <tr><td>{@code "semi_minor"}</td>         <td>Always</td>     <td>Standard parameter defined by WKT 1.</td></tr>
     *   <tr><td>{@code "earth_radius"}</td>       <td>Hidden</td>     <td>Mapped to {@code "semi_major"} and {@code "semi_minor"} parameters.</td></tr>
     *   <tr><td>{@code "inverse_flattening"}</td> <td>Hidden</td>     <td>Computed from the {@code "semi_major"} and {@code "semi_minor"} parameters.</td></tr>
     *   <tr><td>{@code "standard_parallel"}</td>  <td>Hidden</td>
     *     <td>Array of 1 or 2 elements mapped to {@code "standard_parallel_1"} and {@code "standard_parallel_2"}.</td></tr>
     * </table>
     *
     * <div class="note"><b>Note:</b>
     * When the {@code "earth_radius"} parameter is read, its value is the
     * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius() authalic radius}
     * computed from the semi-major and semi-minor axis lengths.</div>
     *
     * Map projection parameter groups always have a {@linkplain DefaultParameterDescriptorGroup#getMinimumOccurs()
     * minimum} and {@linkplain DefaultParameterDescriptorGroup#getMaximumOccurs() maximum occurrence} of 1,
     * regardless the value given to {@link #setRequired(boolean)}.
     *
     * @param  parameters The {@linkplain DefaultParameterDescriptorGroup#descriptors() parameter descriptors}
     *         for the group to create.
     * @return The parameter descriptor group for a map projection.
     *
     * @since 0.6
     */
    public ParameterDescriptorGroup createGroupForMapProjection(final ParameterDescriptor<?>... parameters) {
        final ParameterDescriptorGroup group;
        onCreate(false);
        try {
            group = new MapProjectionDescriptor(properties, parameters);
        } finally {
            onCreate(true);
        }
        return group;
    }
}
