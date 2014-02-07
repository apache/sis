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

import java.util.Set;
import java.io.Serializable;
import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * The base class of single parameter value or group of parameter values.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public abstract class AbstractParameterValue extends FormattableObject
        implements GeneralParameterValue, Serializable, Cloneable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8458179223988766398L;

    /**
     * The abstract definition of this parameter or group of parameters.
     */
    final GeneralParameterDescriptor descriptor;

    /**
     * Creates a parameter value from the specified descriptor.
     *
     * @param descriptor The abstract definition of this parameter or group of parameters.
     */
    protected AbstractParameterValue(final GeneralParameterDescriptor descriptor) {
        ensureNonNull("descriptor", descriptor);
        this.descriptor = descriptor;
    }

    /**
     * Creates a new instance initialized with the values from the specified parameter object.
     * This is a <em>shallow</em> copy constructor, since the values contained in the given
     * object are not cloned.
     *
     * @param parameter The parameter to copy values from.
     */
    protected AbstractParameterValue(final GeneralParameterValue parameter) {
        ensureNonNull("parameter", parameter);
        descriptor = parameter.getDescriptor();
        if (descriptor == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingValueForProperty_1, "descriptor"));
        }
    }

    /**
     * Returns the abstract definition of this parameter or group of parameters.
     *
     * @return The abstract definition of this parameter or group of parameters.
     */
    @Override
    public GeneralParameterDescriptor getDescriptor() {
        return descriptor;
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
     * @param  <T> The type of parameter value. The given {@code value} should typically be an
     *         instance of this class. This is not required by this method signature but is
     *         checked by this method implementation.
     * @param  descriptor The parameter descriptor to check against.
     * @param  value The value to check, or {@code null}.
     * @param  unit  The unit of the value to check, or {@code null}.
     * @return The value casted to the descriptor parameterized type, or the
     *         {@linkplain ParameterDescriptor#getDefaultValue() default value}
     *         if the given value was null while the parameter is mandatory.
     * @throws InvalidParameterValueException if the parameter value is invalid.
     */
    @SuppressWarnings("unchecked")
    static <T> T ensureValidValue(final ParameterDescriptor<T> descriptor, final Object value, final Unit<?> unit)
            throws InvalidParameterValueException
    {
        if (value == null) {
            if (descriptor.getMinimumOccurs() != 0) {
                return descriptor.getDefaultValue();
            }
            return null;
        }
        final String error;
        final Class<T> type = descriptor.getValueClass();
        if (!type.isInstance(value)) {
            error = Errors.format(Errors.Keys.IllegalParameterValueClass_3, getName(descriptor), type, value.getClass());
        } else {
            /*
             * Before to verify if the given value is inside the bounds, we need to convert the value
             * to the units used by the parameter descriptor.
             */
            T converted = (T) value;
            if (unit != null) {
                final Unit<?> def = descriptor.getUnit();
                if (def == null) {
                    throw unitlessParameter(descriptor);
                }
                if (!unit.equals(def)) {
                    final short expectedID = getUnitMessageID(def);
                    if (getUnitMessageID(unit) != expectedID) {
                        throw new IllegalArgumentException(Errors.format(expectedID, unit));
                    }
                    final UnitConverter converter;
                    try {
                        converter = unit.getConverterToAny(def);
                    } catch (ConversionException e) {
                        throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleUnits_2, unit, def), e);
                    }
                    if (Number.class.isAssignableFrom(type)) {
                        Number n = (Number) value; // Given value.
                        n = converter.convert(n.doubleValue()); // Value in units that we can compare.
                        try {
                            converted = (T) Numbers.cast(n, (Class<? extends Number>) type);
                        } catch (IllegalArgumentException e) {
                            throw new InvalidParameterValueException(e.getLocalizedMessage(), getName(descriptor), value);
                        }
                    }
                }
            }
            final Comparable<T> minimum = descriptor.getMinimumValue();
            final Comparable<T> maximum = descriptor.getMaximumValue();
            if ((minimum != null && minimum.compareTo(converted) > 0) ||
                (maximum != null && maximum.compareTo(converted) < 0))
            {
                error = Errors.format(Errors.Keys.ValueOutOfRange_4, getName(descriptor), minimum, maximum, converted);
            } else {
                final Set<T> validValues = descriptor.getValidValues();
                if (validValues!=null && !validValues.contains(converted)) {
                    error = Errors.format(Errors.Keys.IllegalArgumentValue_2, getName(descriptor), value);
                } else {
                    /*
                     * Passed every tests - the value is valid.
                     * Really returns the original value, not the converted one, because we store the given
                     * unit as well. Conversions will be applied on the fly by the getter method if needed.
                     */
                    return (T) value;
                }
            }
        }
        throw new InvalidParameterValueException(error, getName(descriptor), value);
    }

    /**
     * Returns an exception initialized with a "Unitless parameter" error message for the specified descriptor.
     */
    static IllegalStateException unitlessParameter(final GeneralParameterDescriptor descriptor) {
        return new IllegalStateException(Errors.format(Errors.Keys.UnitlessParameter_1, getName(descriptor)));
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

    /**
     * Returns a hash value for this parameter.
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return descriptor.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Compares the given object with this parameter for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            return descriptor.equals(((AbstractParameterValue) object).descriptor);
        }
        return false;
    }

    /**
     * Returns a copy of this parameter value or group.
     *
     * @return A clone of this parameter value or group.
     */
    @Override
    public AbstractParameterValue clone() {
        try {
            return (AbstractParameterValue) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception); // Should not happen, since we are cloneable
        }
    }

    @Override
    protected String formatTo(final Formatter formatter) {
        return null;
    }
}
