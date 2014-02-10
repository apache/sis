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

import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.LinkedHashSet;
import javax.measure.unit.Unit;

import org.opengis.util.CodeList;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;

import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureCanCast;
import static org.apache.sis.util.collection.Containers.hashMapCapacity;
import static org.apache.sis.internal.util.CollectionsExt.unmodifiableOrCopy;

// Related to JDK7
import java.util.Objects;


/**
 * The definition of a parameter used by an operation method.
 * For {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems}
 * most parameter values are numeric, but other types of parameter values are possible.
 * The {@linkplain #getValueClass() value class} for such numeric parameters is usually
 * {@link Double} or {@link Integer}, but other number types are accepted as well.
 *
 * @param <T> The type of elements to be returned by {@link DefaultParameterValue#getValue()}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see Parameter
 * @see DefaultParameterDescriptorGroup
 */
public class DefaultParameterDescriptor<T> extends AbstractParameterDescriptor implements ParameterDescriptor<T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -295668622297737705L;

    /**
     * Key for the {@value} property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getMinimumValue()}.
     */
    public static final String MINIMUM_VALUE_KEY = "minimumValue";

    /**
     * Key for the {@value} property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getMaximumValue()}.
     */
    public static final String MAXIMUM_VALUE_KEY = "maximumValue";

    /**
     * Key for the {@value} property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getValidValues()}.
     */
    public static final String VALID_VALUES_KEY = "validValues";

    /**
     * The class that describe the type of parameter values.
     */
    private final Class<T> valueClass;

    /**
     * A set of valid values (usually from a {@linkplain CodeList code list})
     * or {@code null} if it doesn't apply. This set is immutable.
     */
    private final Set<T> validValues;

    /**
     * The default value for the parameter, or {@code null}.
     */
    private final T defaultValue;

    /**
     * The minimum parameter value, or {@code null}.
     */
    private final Comparable<T> minimumValue;

    /**
     * The maximum parameter value, or {@code null}.
     */
    private final Comparable<T> maximumValue;

    /**
     * The unit for default, minimum and maximum values, or {@code null}.
     */
    private final Unit<?> unit;

    /**
     * Constructs a descriptor from a set of properties. The properties given in argument follow the same rules
     * than for the {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * Additionally, the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value #MINIMUM_VALUE_KEY}</td>
     *     <td>{@code Comparable<T>}</td>
     *     <td>{@link #getMinimumValue()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #MAXIMUM_VALUE_KEY}</td>
     *     <td>{@code Comparable<T>}</td>
     *     <td>{@link #getMaximumValue()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value #VALID_VALUES_KEY}</td>
     *     <td>{@code Collection<T>} or {@code T[]}</td>
     *     <td>{@link #getValidValues()}</td>
     *   </tr>
     *   <tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * Generally speaking, information provided in the {@code properties} map are considered ignorable metadata
     * (except the parameter name) while information provided as explicit arguments may have an impact on coordinate
     * transformation results. See {@link #equals(Object, ComparisonMode)} for more information.
     *
     * @param properties   The properties to be given to the identified object.
     * @param valueClass   The class that describes the type of the parameter value.
     * @param defaultValue The default value for the parameter, or {@code null} if none.
     * @param unit         The unit of measurement for the default, minimum and maximum values, or {@code null} if none.
     * @param required     {@code true} if this parameter is mandatory, or {@code false} if it is optional.
     */
    @SuppressWarnings("unchecked")
    public DefaultParameterDescriptor(final Map<String,?> properties,
                                      final Class<T>      valueClass,
                                      final T             defaultValue,
                                      final Unit<?>       unit,
                                      final boolean       required)
    {
        super(properties, required ? 1 : 0, 1);
        final Comparable<T> minimumValue = Containers.property(properties, MINIMUM_VALUE_KEY, Comparable.class);
        final Comparable<T> maximumValue = Containers.property(properties, MAXIMUM_VALUE_KEY, Comparable.class);
        ensureNonNull("valueClass",      valueClass);
        ensureCanCast("defaultValue",    valueClass, defaultValue);
        ensureCanCast(MINIMUM_VALUE_KEY, valueClass, minimumValue);
        ensureCanCast(MAXIMUM_VALUE_KEY, valueClass, maximumValue);
        this.valueClass   = valueClass;
        this.defaultValue = Numerics.cached(defaultValue);
        this.minimumValue = Numerics.cached(minimumValue);
        this.maximumValue = Numerics.cached(maximumValue);
        this.unit         = unit;
        /*
         * If the caller specified a unit of measurement, then
         * verify that the values are of some numerical type.
         */
        if (unit != null) {
            Class<?> componentType = valueClass;
            for (Class<?> c; (c = componentType.getComponentType()) != null;) {
                componentType = c;
            }
            componentType = Numbers.primitiveToWrapper(componentType);
            if (!Number.class.isAssignableFrom(componentType)) {
                throw new IllegalArgumentException(Errors.getResources(properties).getString(
                        Errors.Keys.IllegalUnitFor_2, super.getName().getCode(), unit));
            }
        }
        /*
         * If the caller specified minimum and maximum values, then
         * verify that the minimum is not greater than the maximum.
         */
        if (minimumValue != null && maximumValue != null) {
            if (minimumValue.compareTo(valueClass.cast(maximumValue)) > 0) {
                throw new IllegalArgumentException(Errors.getResources(properties)
                        .getString(Errors.Keys.IllegalRange_2, minimumValue, maximumValue));
            }
        }
        /*
         * If the caller specified a set of valid values, then copy the values in
         * a new set and verify their type and inclusion in the [min … max] range.
         */
        final Object values = properties.get(VALID_VALUES_KEY);
        if (values != null) {
            final Object[] array;
            if (values instanceof Object[]) {
                array = (Object[]) values;
            } else if (values instanceof Collection<?>) {
                array = ((Collection<?>) values).toArray();
            } else {
                throw new IllegalArgumentException(Errors.getResources(properties)
                        .getString(Errors.Keys.IllegalPropertyClass_2, VALID_VALUES_KEY, values.getClass()));
            }
            final Set<T> valids = new LinkedHashSet<>(hashMapCapacity(array.length));
            for (Object value : array) {
                if (value != null) {
                    value = Numerics.cached(value);
                    final Verifier error = Verifier.ensureValidValue(valueClass, null, minimumValue, maximumValue, value);
                    if (error != null) {
                        throw new IllegalArgumentException(error.message(properties, super.getName().getCode(), value));
                    }
                }
                valids.add((T) value);
            }
            validValues = unmodifiableOrCopy(valids);
        } else {
            validValues = null;
        }
        /*
         * Finally, verify the default value if any.
         */
        if (defaultValue != null) {
            final Verifier error = Verifier.ensureValidValue(valueClass, validValues, minimumValue, maximumValue, defaultValue);
            if (error != null) {
                throw new IllegalArgumentException(error.message(properties, super.getName().getCode(), defaultValue));
            }
        }
    }

    /**
     * Creates a new descriptor with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param descriptor The descriptor to shallow copy.
     */
    public DefaultParameterDescriptor(final ParameterDescriptor<T> descriptor) {
        super(descriptor);
        valueClass   = descriptor.getValueClass();
        validValues  = descriptor.getValidValues();
        defaultValue = descriptor.getDefaultValue();
        minimumValue = descriptor.getMinimumValue();
        maximumValue = descriptor.getMaximumValue();
        unit         = descriptor.getUnit();
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ParameterDescriptor.class}.
     *
     * {@note Subclasses usually do not need to override this method since GeoAPI does not define
     *        <code>ParameterDescriptor</code> sub-interface. Overriding possibility is left mostly
     *        for implementors who wish to extend GeoAPI with their own set of interfaces.}
     *
     * @return {@code ParameterDescriptor.class} or a user-defined sub-interface.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends ParameterDescriptor<T>> getInterface() {
        return (Class) ParameterDescriptor.class;
    }

    /**
     * The maximum number of times that values for this parameter group or parameter can be included.
     * For a {@linkplain DefaultParameterDescriptor single parameter}, the value is always 1.
     *
     * @return The maximum occurrence.
     *
     * @see #getMinimumOccurs()
     */
    @Override
    public int getMaximumOccurs() {
        return 1;
    }

    /**
     * Creates a new parameter value instance initialized with the {@linkplain #getDefaultValue() default value}.
     * The {@linkplain DefaultParameterDescriptor parameter descriptor} for the created parameter value will be
     * {@code this} object.
     *
     * @return A parameter initialized to the default value.
     */
    @Override
    public ParameterValue<T> createValue() {
        return new DefaultParameterValue<>(this);
    }

    /**
     * Returns the class that describe the type of the parameter.
     *
     * @return The parameter value class.
     */
    @Override
    public Class<T> getValueClass() {
        return valueClass;
    }

    /**
     * If this parameter allows only a finite set of values, returns that set.
     * The set of valid values is usually a {@linkplain CodeList code list} or enumerations.
     * This method returns {@code null} if this parameter does not limit values to a finite set.
     *
     * @return A finite set of valid values (usually from a {@linkplain CodeList code list}),
     *         or {@code null} if it does not apply.
     */
    @Override
    public Set<T> getValidValues() {
        return validValues;
    }

    /**
     * Returns the default value for the parameter. The return type can be any type
     * including a {@link Number} or a {@link String}. If there is no default value,
     * then this method returns {@code null}.
     *
     * @return The default value, or {@code null} in none.
     */
    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the minimum parameter value. If there is no minimum value, or if minimum
     * value is inappropriate for the {@linkplain #getValueClass() value class}, then
     * this method returns {@code null}.
     *
     * @return The minimum parameter value (often an instance of {@link Double}), or {@code null}.
     */
    @Override
    public Comparable<T> getMinimumValue() {
        return minimumValue;
    }

    /**
     * Returns the maximum parameter value. If there is no maximum value, or if maximum
     * value is inappropriate for the {@linkplain #getValueClass() value type}, then
     * this method returns {@code null}.
     *
     * @return The minimum parameter value (often an instance of {@link Double}), or {@code null}.
     */
    @Override
    public Comparable<T> getMaximumValue() {
        return maximumValue;
    }

    /**
     * Returns the unit of measurement for the
     * {@linkplain #getDefaultValue() default},
     * {@linkplain #getMinimumValue() minimum} and
     * {@linkplain #getMaximumValue() maximum} values.
     * This attribute apply only if the values is of numeric type (usually an instance of {@link Double}).
     *
     * @return The unit for numeric value, or {@code null} if it doesn't apply to the value type.
     */
    @Override
    public Unit<?> getUnit() {
        return unit;
    }

    /**
     * Compares the specified object with this parameter for equality.
     * The strictness level is controlled by the second argument.
     * This method compares the following properties in every cases:
     *
     * <ul>
     *   <li>{@link #getName()}</li>
     *   <li>{@link #getMinimumOccurs()}</li>
     *   <li>{@link #getMaximumOccurs()}</li>
     *   <li>{@link #getValueClass()}</li>
     *   <li>{@link #getDefaultValue()}</li>
     *   <li>{@link #getUnit()}</li>
     * </ul>
     *
     * All other properties (minimum, maximum and valid values) are compared only
     * for modes stricter than {@link ComparisonMode#IGNORE_METADATA}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                default: {
                    /*
                     * Tests for name, since parameters with different name have completely different meaning.
                     * For example there is no difference between "semi_major" and "semi_minor" parameters
                     * except the name. We do not perform this comparison if the user asked for metadata
                     * comparison, because in such case the names have already been compared by the super-class.
                     */
                    final ParameterDescriptor<?> that = (ParameterDescriptor<?>) object;
                    return getValueClass() == that.getValueClass() &&
                           Objects.deepEquals(getDefaultValue(), that.getDefaultValue()) &&
                           Objects.equals(getUnit(), that.getUnit()) &&
                           (isHeuristicMatchForName(that.getName().getCode()) ||
                            IdentifiedObjects.isHeuristicMatchForName(that, getName().getCode()));
                }
                case BY_CONTRACT: {
                    final ParameterDescriptor<?> that = (ParameterDescriptor<?>) object;
                    return                    getValueClass() == that.getValueClass()    &&
                           Objects.    equals(getValidValues(),  that.getValidValues())  &&
                           Objects.deepEquals(getDefaultValue(), that.getDefaultValue()) &&
                           Objects.    equals(getMinimumValue(), that.getMinimumValue()) &&
                           Objects.    equals(getMaximumValue(), that.getMaximumValue()) &&
                           Objects.    equals(getUnit(),         that.getUnit());
                }
                case STRICT: {
                    final DefaultParameterDescriptor<?> that = (DefaultParameterDescriptor<?>) object;
                    return                    this.valueClass == that.valueClass    &&
                           Objects.    equals(this.validValues,  that.validValues)  &&
                           Objects.deepEquals(this.defaultValue, that.defaultValue) &&
                           Objects.    equals(this.minimumValue, that.minimumValue)      &&
                           Objects.    equals(this.maximumValue, that.maximumValue)      &&
                           Objects.    equals(this.unit,         that.unit);
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected long computeHashCode() {
        return Arrays.deepHashCode(new Object[] {valueClass, defaultValue, minimumValue, maximumValue, unit})
                + super.computeHashCode();
    }

    /**
     * Returns a string representation of this descriptor. The string returned by this
     * method is for information purpose only and may change in future SIS version.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(Classes.getShortClassName(this))
                .append("[\"").append(getName().getCode()).append("\", ")
                .append(getMinimumOccurs() == 0 ? "optional" : "mandatory");
        buffer.append(", class=").append(Classes.getShortName(valueClass));
        if (minimumValue != null || maximumValue != null) {
            buffer.append(", valid=[").append(minimumValue != null ? minimumValue : "-∞")
                  .append(" … ") .append(maximumValue != null ? maximumValue :  "∞").append(']');
        } else if (validValues != null) {
            buffer.append(", valid=").append(validValues);
        }
        if (defaultValue != null) {
            buffer.append(", default=").append(defaultValue);
        }
        if (unit != null) {
            buffer.append(", unit=").append(unit);
        }
        return buffer.append(']').toString();
    }
}
