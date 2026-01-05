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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.reflect.Array;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.math.NumberType;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.Units;


/**
 * Verifies the validity of a given value.
 * An instance of {@code Verifier} is created only if an error is detected.
 * In such case, the error message is given by {@link #message(Map, String, Object)}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class Verifier {
    /**
     * The {@link Errors.Keys} or {@link Resources.Keys} value that describe the invalid value.
     */
    private final short errorKey;

    /**
     * {@code false} if {@link #errorKey} is a {@code Errors.Keys} constants,
     * or {@code true} if it is a {@code Resources.Keys} constant.
     */
    private final boolean internal;

    /**
     * The arguments to be used with the error message to format.
     * The current implementation relies on the following invariants:
     *
     * <ul>
     *   <li>The first element in this array will be the parameter name. Before the name is known,
     *       this element is either {@code null} or the index to append to the name.</li>
     *   <li>The last element may be set to the erroneous value.</li>
     * </ul>
     */
    private final Object[] arguments;

    /**
     * Stores information about an error.
     */
    private Verifier(final boolean internal, final short errorKey, final Object... arguments) {
        this.errorKey  = errorKey;
        this.internal  = internal;
        this.arguments = arguments;
    }

    /**
     * Ensures that the given value is valid according the specified parameter descriptor.
     * This method ensures that {@code value} is assignable to the
     * {@linkplain ParameterDescriptor#getValueClass() expected class}, is between the
     * {@linkplain ParameterDescriptor#getMinimumValue() minimum} and
     * {@linkplain ParameterDescriptor#getMaximumValue() maximum} values and is one of the
     * {@linkplain ParameterDescriptor#getValidValues() set of valid values}.
     * If the value fails any of those tests, then an exception is thrown.
     *
     * <p>This method does not attempt to convert the given value (for example from {@link Float} to {@link Double}) because
     * such conversions should be done by the caller if desired. See {@link DefaultParameterValue#setValue(Object, Unit)}.</p>
     *
     * @param  <T>         the type of parameter value. The given {@code value} should typically be an instance of this class.
     *                     This is not required by this method signature but is checked by this method implementation.
     * @param  descriptor  the parameter descriptor to check against.
     * @param  value       the value to check, or {@code null}.
     * @param  unit        the unit of the value to check, or {@code null}.
     * @return the given value converted to the descriptor unit if any,
     *         then cast to the descriptor parameterized type.
     * @throws InvalidParameterValueException if the parameter value is invalid.
     */
    static <T> T ensureValidValue(final ParameterDescriptor<T> descriptor, final Object value, final Unit<?> unit)
            throws InvalidParameterValueException
    {
        final Class<T> expectedClass = descriptor.getValueClass();
        /*
         * Convert the given value to the units used by the parameter descriptor.
         * Notes:
         *
         *   1) We need to perform this conversion before to verify if the given value is inside the bounds.
         *   2) This conversion may change the type (e.g. from java.lang.Float to Double) as a side effect.
         *   3) We execute this code even if value is null because it contains a check of unit validity.
         */
        UnitConverter converter = null;
        Object convertedValue = value;
        if (unit != null) {
            final Unit<?> def = descriptor.getUnit();
            if (def != null && !unit.equals(def)) {
                // Verify the unit dimension: linear, angular, temporal or scale.
                final short expectedID = getUnitMessageID(def);
                if (getUnitMessageID(unit) != expectedID) {
                    throw new InvalidParameterValueException(Errors.format(expectedID, unit), getDisplayName(descriptor), value);
                }
                if (value != null) {
                    /*
                     * Verify the value type before to perform unit conversion. This will indirectly verifies that the value
                     * is an instance of `java.lang.Number` or an array of numbers because non-null units are associated to
                     * `MeasurementRange` in SIS implementation, which accepts only numeric values.
                     */
                    if (!expectedClass.isInstance(value)) {
                        final String name = getDisplayName(descriptor);
                        throw new InvalidParameterValueException(
                                Resources.format(Resources.Keys.IllegalParameterValueClass_3,
                                name, expectedClass, value.getClass()), name, value);
                    }
                    /*
                     * From this point we will perform the actual unit conversion. The value may be either
                     * a Number instance, or an array of numbers (typically an array of type double[]). In
                     * the array case, we will store the converted values in a new array of expected type.
                     */
                    try {
                        converter = unit.getConverterToAny(def);
                    } catch (IncommensurableException e) {
                        throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleUnits_2, unit, def), e);
                    }
                    final Class<?> componentType = expectedClass.getComponentType();
                    int i = -1;
                    try {
                        if (componentType == null) {
                            // Usual case where the expected value is a singleton.
                            Number n = converter.convert((Number) value);
                            convertedValue = NumberType.forNumberClass(expectedClass).cast(n);
                        } else {
                            final int length = Array.getLength(value);
                            if (length != 0) {
                                final Class<? extends Number> numberType =
                                        NumberType.primitiveToWrapper(componentType).asSubclass(Number.class);
                                convertedValue = Array.newInstance(componentType, length);
                                for (i=0; i<length; i++) {
                                    Number n = (Number) Array.get(value, i);
                                    n = converter.convert(n);                       // Value in units that we can compare.
                                    n = NumberType.forNumberClass(numberType).cast(n);
                                    Array.set(convertedValue, i, n);
                                }
                            }
                        }
                    } catch (ClassCastException | IllegalArgumentException cause) {
                        String name = getDisplayName(descriptor);
                        if (i >= 0) name = Strings.toIndexed(name, i);
                        var e = new InvalidParameterValueException(cause.getLocalizedMessage(), name, value);
                        e.initCause(cause);
                        throw e;
                    }
                }
            }
        }
        /*
         * At this point the user's value has been fully converted to the unit of measurement specified
         * by the ParameterDescriptor.  Now compare the converted value to the restriction given by the
         * descriptor (set of valid values and range of value domain).
         */
        if (convertedValue != null) {
            final Verifier error;
            final Set<T> validValues = descriptor.getValidValues();
            if (descriptor instanceof DefaultParameterDescriptor<?>) {
                error = ensureValidValue(expectedClass, validValues,
                        ((DefaultParameterDescriptor<?>) descriptor).getValueDomain(), convertedValue);
            } else {
                error = ensureValidValue(expectedClass, validValues,
                        descriptor.getMinimumValue(), descriptor.getMaximumValue(), convertedValue);
            }
            /*
             * If we found an error, we will usually throw an exception. An exception to this rule is
             * when EPSGDataAccess is creating a deprecated ProjectedCRS in which some parameters are
             * known to be invalid (the CRS was deprecated precisely for that reason). In such cases,
             * we will log a warning instead of throwing an exception.
             */
            if (error != null) {
                error.convertRange(converter);
                final String name = getDisplayName(descriptor);
                final String message = error.message(null, name, value);
                if (Semaphores.SUSPEND_PARAMETER_CHECK.get()) {
                    Logging.completeAndLog(DefaultParameterValue.LOGGER, DefaultParameterValue.class,
                                           "setValue", new LogRecord(Level.WARNING, message));
                } else {
                    throw new InvalidParameterValueException(message, name, value);
                }
            }
        }
        return expectedClass.cast(convertedValue);
    }

    /**
     * Compares the given value against the given descriptor properties. If the value is valid, returns {@code null}.
     * Otherwise returns an object that can be used for formatting the error message.
     *
     * @param convertedValue  the value <em>converted to the units specified by the descriptor</em>.
     *        This is not necessarily the user-provided value.
     */
    @SuppressWarnings("unchecked")
    static <T> Verifier ensureValidValue(final Class<T> valueClass, final Set<T> validValues,
            final Range<?> valueDomain, final Object convertedValue)
    {
        final Verifier verifier = ensureValidValue(valueClass, validValues, null, null, convertedValue);
        if (verifier == null && valueDomain != null) {
            final boolean isArray = valueClass.isArray();
            /*
             * Following assertion should never fail with DefaultParameterDescriptor instances.
             * It could fail if the user overrides DefaultParameterDescriptor.getValueDomain()
             * in a way that break the method contract.
             */
            assert valueDomain.getElementType() ==
                    (isArray ? NumberType.primitiveToWrapper(valueClass.getComponentType()) : valueClass) : valueDomain;
            final int length = isArray ? Array.getLength(convertedValue) : 1;
            for (int i=0; i<length; i++) {
                final Object value = isArray ? Array.get(convertedValue, i) : convertedValue;
                if (!((Range) valueDomain).contains((Comparable<?>) value)) {
                    final short errorKey;
                    final Object[] arguments;
                    final Object minValue = valueDomain.getMinValue();
                    if ((minValue instanceof Number) && ((Number) minValue).doubleValue() == 0 && !valueDomain.isMinIncluded()
                        && (value instanceof Number) && ((Number) value).doubleValue() <= 0)
                    {
                        errorKey     = Errors.Keys.ValueNotGreaterThanZero_2;
                        arguments    = new Object[2];
                    } else {
                        errorKey     = Errors.Keys.ValueOutOfRange_4;
                        arguments    = new Object[4];
                        arguments[1] = minValue;
                        arguments[2] = valueDomain.getMaxValue();
                   }
                    if (isArray) arguments[0] = i;
                    arguments[arguments.length - 1] = value;
                    return new Verifier(false, errorKey, arguments);
                }
            }
        }
        return verifier;
    }

    /**
     * Same as {@link #ensureValidValue(Class, Set, Range, Object)}, used as a fallback when
     * the descriptor is not an instance of {@link DefaultParameterDescriptor}.
     *
     * <h4>Implementation note</h4>
     * At the difference of {@code ensureValidValue(…, Range, …)}, this method does not need to verify array elements
     * because the type returned by {@link ParameterDescriptor#getMinimumValue()} and {@code getMaximumValue()}
     * methods (namely {@code Comparable<T>}) does not allow usage with arrays.
     *
     * @param convertedValue  the value <em>converted to the units specified by the descriptor</em>.
     *        This is not necessarily the user-provided value.
     */
    @SuppressWarnings({"unchecked", "element-type-mismatch"})
    private static <T> Verifier ensureValidValue(final Class<T> valueClass,
                                                 final Set<T> validValues,
                                                 final Comparable<T> minimum,
                                                 final Comparable<T> maximum,
                                                 final Object convertedValue)
    {
        if (!valueClass.isInstance(convertedValue)) {
            return new Verifier(true, Resources.Keys.IllegalParameterValueClass_3,
                    null, valueClass, convertedValue.getClass());
        }
        if (validValues != null && !validValues.contains(convertedValue)) {
            return new Verifier(true, Resources.Keys.IllegalParameterValue_2, null, convertedValue);
        }
        if ((minimum != null && minimum.compareTo((T) convertedValue) > 0) ||
            (maximum != null && maximum.compareTo((T) convertedValue) < 0))
        {
            return new Verifier(false, Errors.Keys.ValueOutOfRange_4, null, minimum, maximum, convertedValue);
        }
        return null;
    }

    /**
     * Converts the information about a "value out of range" error. The range in the error message will be formatted
     * in the unit given by the user, which is not necessarily the same as the unit of the parameter descriptor.
     *
     * @param converter  the conversion from user unit to descriptor unit, or {@code null} if none.
     *        This method uses the inverse of that conversion for converting the given minimum and maximum values.
     */
    private void convertRange(UnitConverter converter) {
        if (converter != null && !internal && errorKey == Errors.Keys.ValueOutOfRange_4) {
            converter = converter.inverse();
            for (int i=1; i<=2; i++) {
                Object value = arguments[i];
                if (value instanceof Number) {
                    arguments[i] = converter.convert(((Number) value).doubleValue());
                }
            }
        }
    }

    /**
     * Returns an error message for the error detected by
     * {@link #ensureValidValue(Class, Set, Range, Object)}.
     *
     * @param name   the parameter name.
     * @param value  the user supplied value (not necessarily equals to the converted value).
     */
    String message(final Map<?,?> properties, String name, Object value) {
        final Object index = arguments[0];
        if (index != null) {
            name = Strings.bracket(name, index);
            value = Array.get(value, (Integer) index);
        }
        arguments[0] = name;
        final IndexedResourceBundle resources;
        if (internal) {
            resources = Resources.forProperties(properties);
            switch (errorKey) {
                case Resources.Keys.IllegalParameterValue_2: {
                    arguments[1] = value;
                    break;
                }
            }
        } else {
            resources =  Errors.forProperties(properties);
            switch (errorKey) {
                case Errors.Keys.ValueOutOfRange_4: {
                    if (arguments[1] == null) arguments[1] = "−∞";
                    if (arguments[2] == null) arguments[2] =  "∞";
                    arguments[3] = value;
                    break;
                }
                case Errors.Keys.ValueNotGreaterThanZero_2: {
                    arguments[1] = value;
                    break;
                }
            }
        }
        return resources.getString(errorKey, arguments);
    }

    /**
     * Convenience method returning the name of the specified descriptor.
     * This method is used mostly for output to be read by human, not for processing.
     * Consequently, we may consider to return a localized name in a future version.
     *
     * <p>This method is null-safe even if none of the references checked here should be null.
     * We make this method safe because it is indirectly invoked by methods like {@code toString()}
     * which are not expected to fail even if the object is invalid.</p>
     *
     * <p><b>This method should NOT be invoked for programmatic usage</b> (e.g. setting a parameter
     * value) because the string returned in case of invalid descriptor is arbitrary.</p>
     */
    static String getDisplayName(final GeneralParameterDescriptor descriptor) {
        if (descriptor != null) {
            final Identifier name = descriptor.getName();
            if (name != null) {
                final String code = name.getCode();
                if (code != null) {
                    return code;
                }
            }
        }
        return Vocabulary.format(Vocabulary.Keys.Unnamed);
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
