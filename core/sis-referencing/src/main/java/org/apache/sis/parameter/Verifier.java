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
import java.util.Set;
import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;


/**
 * Verifies the validity of a given value.
 * An instance of {@code Verifier} is created only if an error is detected.
 * In such case, the error message is given by {@link #message(String, Object)}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
final class Verifier {
    /**
     * The {@link Errors.Keys} value that describe the invalid value.
     */
    private final short errorKey;

    /**
     * {@code true} if the last element in {@link #arguments} shall be set to the erroneous value.
     */
    private final boolean needsValue;

    /**
     * The arguments to be used with the error message to format.
     * The current implementation relies on the following invariants:
     *
     * <ul>
     *   <li>The first element in this array is always the parameter name.</li>
     *   <li>The last element shall be set to the erroneous value if {@link #needsValue} is {@code true}.</li>
     * </ul>
     */
    private final Object[] arguments;

    /**
     * Stores information about an error.
     */
    private Verifier(final short errorKey, final boolean needsValue, final Object... arguments) {
        this.errorKey   = errorKey;
        this.needsValue = needsValue;
        this.arguments  = arguments;
    }

    /**
     * Ensures that the given value is valid according the specified parameter descriptor.
     * This convenience method ensures that {@code value} is assignable to the
     * {@linkplain ParameterDescriptor#getValueClass() expected class}, is between the
     * {@linkplain ParameterDescriptor#getMinimumValue() minimum} and
     * {@linkplain ParameterDescriptor#getMaximumValue() maximum} values and is one of the
     * {@linkplain ParameterDescriptor#getValidValues() set of valid values}.
     * If the value fails any of those tests, then an exception is thrown.
     *
     * @param  <T> The type of parameter value. The given {@code value} should typically be an instance of this class.
     *             This is not required by this method signature but is checked by this method implementation.
     *
     * @param  descriptor The parameter descriptor to check against.
     * @param  value      The value to check, or {@code null}.
     * @param  unit       The unit of the value to check, or {@code null}.
     * @return The value casted to the descriptor parameterized type, or the
     *         {@linkplain ParameterDescriptor#getDefaultValue() default value}
     *         if the given value was null while the parameter is mandatory.
     * @throws InvalidParameterValueException if the parameter value is invalid.
     */
    @SuppressWarnings("unchecked")
    static <T> T ensureValidValue(final ParameterDescriptor<T> descriptor, final Object value, final Unit<?> unit)
            throws InvalidParameterValueException
    {
        final Class<T> type = descriptor.getValueClass();
        /*
         * Before to verify if the given value is inside the bounds, we need to convert the value
         * to the units used by the parameter descriptor.
         */
        Object convertedValue = value;
        if (unit != null) {
            final Unit<?> def = descriptor.getUnit();
            if (def == null) {
                final String name = getName(descriptor);
                throw new InvalidParameterValueException(Errors.format(Errors.Keys.UnitlessParameter_1, name), name, unit);
            }
            if (!unit.equals(def)) {
                final short expectedID = getUnitMessageID(def);
                if (getUnitMessageID(unit) != expectedID) {
                    throw new IllegalArgumentException(Errors.format(expectedID, unit));
                }
                if (value != null && Number.class.isAssignableFrom(type)) {
                    final UnitConverter converter;
                    try {
                        converter = unit.getConverterToAny(def);
                    } catch (ConversionException e) {
                        throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleUnits_2, unit, def), e);
                    }
                    Number n = (Number) value; // Given value.
                    n = converter.convert(n.doubleValue()); // Value in units that we can compare.
                    try {
                        convertedValue = Numbers.cast(n, (Class<? extends Number>) type);
                    } catch (IllegalArgumentException e) {
                        throw new InvalidParameterValueException(e.getLocalizedMessage(), getName(descriptor), value);
                    }
                }
            }
        }
        if (convertedValue != null) {
            final Comparable<T> minimum = descriptor.getMinimumValue();
            final Comparable<T> maximum = descriptor.getMaximumValue();
            final Verifier error = ensureValidValue(type, descriptor.getValidValues(), minimum, maximum, convertedValue);
            if (error != null) {
                final String name = getName(descriptor);
                throw new InvalidParameterValueException(error.message(null, name, value), name, value);
            }
        } else if (descriptor.getMinimumOccurs() != 0) {
            return descriptor.getDefaultValue();
        }
        /*
         * Passed every tests - the value is valid.
         * Really returns the original value, not the converted one, because we store the given
         * unit as well. Conversions will be applied on the fly by the getter method if needed.
         */
        return (T) value;
    }

    /**
     * Compares the given value against the given descriptor properties. If the value is valid, returns {@code null}.
     * Otherwise returns an object that can be used for formatting the error message.
     *
     * @param convertedValue The value <em>converted to the units specified by the descriptor</em>.
     *        This is not necessarily the user-provided value.
     */
    @SuppressWarnings("unchecked")
    static <T> Verifier ensureValidValue(final Class<T> type, final Set<T> validValues,
            final Comparable<T> minimum, final Comparable<T> maximum, final Object convertedValue)
    {
        if (!type.isInstance(convertedValue)) {
            return new Verifier(Errors.Keys.IllegalParameterValueClass_3, false, null, type, convertedValue.getClass());
        }
        if ((minimum != null && minimum.compareTo((T) convertedValue) > 0) ||
            (maximum != null && maximum.compareTo((T) convertedValue) < 0))
        {
            return new Verifier(Errors.Keys.ValueOutOfRange_4, true, null, minimum, maximum, convertedValue);
        }
        if (validValues != null && !validValues.contains(convertedValue)) {
            return new Verifier(Errors.Keys.IllegalParameterValue_2, true, null, convertedValue);
        }
        return null;
    }

    /**
     * Returns an error message for the error detected by
     * {@link #ensureValidValue(Class, Set, Comparable, Comparable, Object)}.
     *
     * @param name  The parameter name.
     * @param value The user-supplied value (not necessarily equals to the converted value).
     */
    String message(final Map<?,?> properties, final String name, final Object value) {
        arguments[0] = name;
        if (needsValue) {
            arguments[arguments.length - 1] = value;
        }
        return Errors.getResources(properties).getString(errorKey, arguments);
    }

    /**
     * Convenience method returning the name of the specified descriptor.
     * This method is used mostly for output to be read by human, not for processing.
     * Consequently, we may consider to returns a localized name in a future version.
     */
    static String getName(final GeneralParameterDescriptor descriptor) {
        return descriptor.getName().getCode();
    }

    /**
     * Returns the unit type as one of error message code.
     * Used for checking unit with a better error message formatting if needed.
     */
    static short getUnitMessageID(final Unit<?> unit) {
        if (Units.isLinear  (unit)) return Errors.Keys.NonLinearUnit_1;
        if (Units.isAngular (unit)) return Errors.Keys.NonAngularUnit_1;
        if (Units.isTemporal(unit)) return Errors.Keys.NonTemporalUnit_1;
        if (Units.isScale   (unit)) return Errors.Keys.NonScaleUnit_1;
        return Errors.Keys.IncompatibleUnit_1;
    }
}
