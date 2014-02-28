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
import javax.measure.unit.Unit;

import org.opengis.util.CodeList;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;

import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureCanCast;

// Related to JDK7
import java.util.Objects;


/**
 * The definition of a parameter used by an operation method.
 * For {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems}
 * most parameter values are numeric, but other types of parameter values are possible.
 *
 * <p>A parameter descriptor contains the following properties:</p>
 * <ul>
 *   <li>The parameter {@linkplain #getName() name}.</li>
 *   <li>The {@linkplain #getValueClass() class of values}. This is usually {@link Double}, {@code double[]},
 *       {@link Integer}, {@code int[]}, {@link Boolean}, {@link String} or {@link java.net.URI},
 *       but other types are allowed as well.</li>
 *   <li>Whether this parameter is optional or mandatory. This is specified by the {@linkplain #getMinimumOccurs()
 *       minimum occurences} number, which can be 0 or 1 respectively.</li>
 *   <li>The domain of values, as a {@linkplain #getMinimumValue() minimum value}, {@linkplain #getMaximumValue()
 *       maximum value} or an enumeration of {@linkplain #getValidValues() valid values}.</li>
 *   <li>The {@linkplain #getDefaultValue() default value}.</li>
 *   <li>The {@linkplain #getUnit() unit of measurement}.</li>
 * </ul>
 *
 * @param <T> The type of elements to be returned by {@link DefaultParameterValue#getValue()}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see DefaultParameterValue
 * @see DefaultParameterDescriptorGroup
 */
public class DefaultParameterDescriptor<T> extends AbstractIdentifiedObject implements ParameterDescriptor<T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7433401733923393656L;

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getValidValues()}.
     *
     * <p>This property is mostly for restricting values to a {@linkplain CodeList code list} or enumeration subset.
     * It is not necessary to provide this property when all values from the code list or enumeration are valid.</p>
     */
    public static final String VALID_VALUES_KEY = "validValues";

    /**
     * {@code true} if this parameter is mandatory, or {@code false} if it is optional.
     *
     * @see #getMinimumOccurs()
     */
    private final boolean required;

    /**
     * The class that describe the type of parameter values.
     *
     * @see #getValueClass()
     */
    private final Class<T> valueClass;

    /**
     * A set of valid values (usually from a {@linkplain CodeList code list})
     * or {@code null} if it doesn't apply. This set is immutable.
     *
     * @see #getValidValues()
     */
    private final Set<T> validValues;

    /**
     * The minimum and maximum parameter value with their unit of measurement, or {@code null} if none.
     * If non-null, then the range element type shall be the same than {@link #valueClass}.
     *
     * @see #getValueDomain()
     */
    private final Range<? extends T> valueDomain;

    /**
     * The default value for the parameter, or {@code null}.
     *
     * @see #getDefaultValue()
     */
    private final T defaultValue;

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
     * The {@code valueDomain} argument groups the {@linkplain #getMinimumValue() minimum value},
     * {@linkplain #getMaximumValue() maximum value}, {@linkplain #getUnit() unit of measurement}
     * (if any) and information about whether the bounds are inclusive or exclusive. This argument
     * can be provided only if the {@code valueClass} is assignable to {@link Comparable}, and the
     * value returned by {@link Range#getElementType()} shall be the same than {@code valueClass}.
     *
     * @param properties   The properties to be given to the identified object.
     * @param valueClass   The class that describes the type of the parameter value.
     * @param valueDomain  The minimum value, maximum value and unit of measurement, or {@code null} if none.
     * @param defaultValue The default value for the parameter, or {@code null} if none.
     * @param required     {@code true} if this parameter is mandatory, or {@code false} if it is optional.
     */
    @SuppressWarnings("unchecked")
    public DefaultParameterDescriptor(final Map<String,?>      properties,
                                      final Class<T>           valueClass,
                                      final Range<? extends T> valueDomain,
                                      final T                  defaultValue,
                                      final boolean            required)
    {
        super(properties);
        ensureNonNull("valueClass",   valueClass);
        ensureCanCast("defaultValue", valueClass, defaultValue);
        if (valueDomain != null) {
            final Class<?> elementType = valueDomain.getElementType();
            if (elementType != valueClass) {
                throw new IllegalArgumentException(Errors.getResources(properties).getString(
                        Errors.Keys.IllegalArgumentClass_2, "valueDomain",
                        "Range<" + Classes.getShortName(elementType) + '>'));
            }
            if (valueDomain.isEmpty()) {
                throw new IllegalArgumentException(Errors.getResources(properties)
                        .getString(Errors.Keys.IllegalRange_2, valueDomain.getMinValue(), valueDomain.getMaxValue()));
            }
        }
        this.required     = required;
        this.valueClass   = valueClass;
        this.valueDomain  = valueDomain;
        this.defaultValue = Numerics.cached(defaultValue);
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
            final Set<T> valids = CollectionsExt.createSetForType(valueClass, array.length);
            for (Object value : array) {
                if (value != null) {
                    value = Numerics.cached(value);
                    final Verifier error = Verifier.ensureValidValue(valueClass, null, valueDomain, value);
                    if (error != null) {
                        throw new IllegalArgumentException(error.message(properties, super.getName().getCode(), value));
                    }
                }
                valids.add((T) value);
            }
            validValues = CollectionsExt.unmodifiableOrCopy(valids);
        } else {
            validValues = null;
        }
        /*
         * Finally, verify the default value if any.
         */
        if (defaultValue != null) {
            final Verifier error = Verifier.ensureValidValue(valueClass, validValues, valueDomain, defaultValue);
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
    @SuppressWarnings("unchecked")
    protected DefaultParameterDescriptor(final ParameterDescriptor<T> descriptor) {
        super(descriptor);
        required     = descriptor.getMinimumOccurs() != 0;
        valueClass   = descriptor.getValueClass();
        validValues  = descriptor.getValidValues();
        defaultValue = descriptor.getDefaultValue();
        if (Number.class.isAssignableFrom(valueClass) && Comparable.class.isAssignableFrom(valueClass)) {
            valueDomain = Parameters.getValueDomain((ParameterDescriptor) descriptor);
        } else {
            valueDomain = null;
        }
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ParameterDescriptor.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ParameterDescriptor}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their own
     * set of interfaces.</div>
     *
     * @return {@code ParameterDescriptor.class} or a user-defined sub-interface.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends ParameterDescriptor<T>> getInterface() {
        return (Class) ParameterDescriptor.class;
    }

    /**
     * The minimum number of times that values for this parameter group or parameter are required.
     * A value of 0 means an optional parameter and a value of 1 means a mandatory parameter.
     *
     * @see #getMaximumOccurs()
     */
    @Override
    public int getMinimumOccurs() {
        return required ? 1 : 0;
    }

    /**
     * The maximum number of times that values for this parameter group or parameter can be included.
     * For a {@code ParameterDescriptor}, the value is always 1.
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
    public final Class<T> getValueClass() {
        return valueClass;
    }

    /**
     * If this parameter allows only a finite set of values, returns that set.
     * The set of valid values is usually a {@linkplain CodeList code list} or enumeration.
     * This method returns {@code null} if this parameter does not limit values to a finite set.
     *
     * @return A finite set of valid values (usually from a {@linkplain CodeList code list}),
     *         or {@code null} if it does not apply or if there is no restriction.
     */
    @Override
    public Set<T> getValidValues() {
        return validValues;
    }

    /**
     * Returns the domain of values with their unit of measurement (if any), or {@code null} if none.
     * The {@code Range} object combines the {@linkplain #getValueClass() value class},
     * {@linkplain #getMinimumValue() minimum value} and {@link #getMaximumValue() maximum value}
     * and whether these values are inclusive or inclusive. If the range is an instance of
     * {@link MeasurementRange}, then it contains also the {@linkplain #getUnit() unit of measurement}.
     *
     * <div class="note"><b>API note:</b> If this method returns a non-null value, then its type is exactly
     * {@code Range<T>}. The {@code <? extends T>} in this method signature is because range types need to
     * extend {@link Comparable}, while {@code ParameterDescriptor<T>} does not have this requirement.</div>
     *
     * @return The domain of values, or {@code null}.
     *
     * @see Parameters#getValueDomain(ParameterDescriptor)
     */
    public Range<? extends T> getValueDomain() {
        return valueDomain;
    }

    /**
     * Returns the minimum parameter value. If there is no minimum value, or if minimum
     * value is inappropriate for the {@linkplain #getValueClass() value class}, then
     * this method returns {@code null}.
     *
     * <p>This is a convenience method for
     * <code>{@linkplain #getValueDomain()}.{@linkplain Range#getMinValue() getMinValue()}</code>.
     * Note that this method said nothing about whether the value is {@linkplain Range#isMinIncluded() inclusive}.</p>
     *
     * @return The minimum parameter value (often an instance of {@link Double}), or {@code null} if unbounded.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Comparable<T> getMinimumValue() {
        return (valueDomain != null) ? (Comparable<T>) valueDomain.getMinValue() : null;
    }

    /**
     * Returns the maximum parameter value. If there is no maximum value, or if maximum
     * value is inappropriate for the {@linkplain #getValueClass() value type}, then
     * this method returns {@code null}.
     *
     * <p>This is a convenience method for
     * <code>{@linkplain #getValueDomain()}.{@linkplain Range#getMaxValue() getMaxValue()}</code>.
     * Note that this method said nothing about whether the value is {@linkplain Range#isMaxIncluded() inclusive}.</p>
     *
     * @return The minimum parameter value (often an instance of {@link Double}), or {@code null} if unbounded.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Comparable<T> getMaximumValue() {
        return (valueDomain != null) ? (Comparable<T>) valueDomain.getMaxValue() : null;
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
     * Returns the unit of measurement for the
     * {@linkplain #getMinimumValue() minimum},
     * {@linkplain #getMaximumValue() maximum} and
     * {@linkplain #getDefaultValue() default} values.
     * This attribute apply only if the values is of numeric type (usually an instance of {@link Double}).
     *
     * <p>This is a convenience method for
     * <code>{@linkplain #getValueDomain()}.{@linkplain MeasurementRange#unit() unit()}</code>.</p>
     *
     * @return The unit for numeric value, or {@code null} if it doesn't apply to the value type.
     */
    @Override
    public Unit<?> getUnit() {
        return (valueDomain instanceof MeasurementRange<?>) ? ((MeasurementRange<?>) valueDomain).unit() : null;
    }

    /**
     * Compares the specified object with this parameter for equality.
     * The strictness level is controlled by the second argument.
     * This method compares the following properties in every cases:
     *
     * <ul>
     *   <li>{@link #getName()}, compared {@linkplain #isHeuristicMatchForName(String) heuristically}
     *       in {@code IGNORE_METADATA} or less strict mode.</li>
     *   <li>{@link #getValueClass()}</li>
     *   <li>{@link #getDefaultValue()}</li>
     *   <li>{@link #getUnit()}</li>
     * </ul>
     *
     * All other properties (minimum and maximum occurrences, minimum, maximum and valid values)
     * are compared only for modes stricter than {@link ComparisonMode#IGNORE_METADATA}.
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
                    return getMinimumOccurs() == that.getMinimumOccurs() &&
                           getMaximumOccurs() == that.getMaximumOccurs() &&
                           getValueClass()    == that.getValueClass()    &&
                           Objects.    equals(getValidValues(),  that.getValidValues())  &&
                           Objects.    equals(getMinimumValue(), that.getMinimumValue()) &&
                           Objects.    equals(getMaximumValue(), that.getMaximumValue()) &&
                           Objects.deepEquals(getDefaultValue(), that.getDefaultValue()) &&
                           Objects.    equals(getUnit(),         that.getUnit());
                }
                case STRICT: {
                    final DefaultParameterDescriptor<?> that = (DefaultParameterDescriptor<?>) object;
                    return                    this.required   == that.required     &&
                                              this.valueClass == that.valueClass   &&
                           Objects.    equals(this.validValues,  that.validValues) &&
                           Objects.    equals(this.valueDomain,  that.valueDomain) &&
                           Objects.deepEquals(this.defaultValue, that.defaultValue);
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
        return Arrays.deepHashCode(new Object[] {required, valueClass, valueDomain, defaultValue})
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
        if (valueDomain != null) {
            buffer.append(", valid=").append(valueDomain);
        } else if (validValues != null) {
            buffer.append(", valid=").append(validValues);
        }
        if (defaultValue != null) {
            buffer.append(", default=").append(defaultValue);
        }
        return buffer.append(']').toString();
    }
}
