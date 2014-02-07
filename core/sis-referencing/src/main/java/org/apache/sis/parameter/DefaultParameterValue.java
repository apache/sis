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

import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.InvalidParameterTypeException;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import java.util.Objects;
import java.nio.file.Path;
import org.apache.sis.util.Numbers;


/**
 * A parameter value used by an operation method. Most CRS parameter values are numeric and can
 * be obtained by the {@link #intValue()} or {@link #doubleValue()} methods. But other types of
 * parameter values are possible and can be handled by the more generic {@link #getValue()} and
 * {@link #setValue(Object)} methods. The type and constraints on parameter values are given by
 * the {@linkplain #getDescriptor() descriptor}.
 *
 * <p>The following table summarizes the ISO 19111 attributes with the corresponding getter and
 * setter methods:</p>
 *
 * <table class="sis">
 *   <tr><th>ISO attribute</th>             <th>Java type</th>        <th>Getter method</th>                  <th>Setter method</th></tr>
 *   <tr><td></td>                          <td>{@link Object}</td>   <td>{@link #getValue()}</td>            <td>{@link #setValue(Object)}</td></tr>
 *   <tr><td>{@code stringValue}</td>       <td>{@link String}</td>   <td>{@link #stringValue()}</td>         <td>{@code  setValue(Object)}</td></tr>
 *   <tr><td>{@code value}</td>             <td>{@code double}</td>   <td>{@link #doubleValue()}</td>         <td>{@link #setValue(double)}</td></tr>
 *   <tr><td></td>                          <td>{@code double}</td>   <td>{@link #doubleValue(Unit)}</td>     <td>{@link #setValue(double, Unit)}</td></tr>
 *   <tr><td>{@code valueList}</td>         <td>{@code double[]}</td> <td>{@link #doubleValueList()}</td>     <td>{@code  setValue(Object)}</td></tr>
 *   <tr><td></td>                          <td>{@code double[]}</td> <td>{@link #doubleValueList(Unit)}</td> <td>{@link #setValue(double[], Unit)}</td></tr>
 *   <tr><td>{@code integerValue}</td>      <td>{@code int}</td>      <td>{@link #intValue()}</td>            <td>{@link #setValue(int)}</td></tr>
 *   <tr><td>{@code integerValueList}</td>  <td>{@code int[]}</td>    <td>{@link #intValueList()}</td>        <td>{@code  setValue(Object)}</td></tr>
 *   <tr><td>{@code booleanValue}</td>      <td>{@code boolean}</td>  <td>{@link #booleanValue()}</td>        <td>{@link #setValue(boolean)}</td></tr>
 *   <tr><td>{@code valueFile}</td>         <td>{@link URI}</td>      <td>{@link #valueFile()}</td>           <td>{@code  setValue(Object)}</td></tr>
 *   <tr><td>{@code valueFileCitation}</td> <td>{@link Citation}</td> <td>{@code  getValue()}</td>            <td>{@code  setValue(Object)}</td></tr>
 * </table>
 *
 * Instances of {@code ParameterValue} are created by the {@link ParameterDescriptor#createValue()} method.
 * The parameter type can be fetch with the following idiom:
 *
 * {@preformat java
 *     Class<? extends T> valueClass = parameter.getDescriptor().getValueClass();
 * }
 *
 * {@section Implementation note for subclasses}
 * All read and write operations (except constructors, {@link #equals(Object)} and {@link #hashCode()})
 * ultimately delegates to the following methods:
 *
 * <ul>
 *   <li>All getter methods will invoke {@link #getValue()} and {@link #getUnit()} (if needed),
 *       then perform their processing on the values returned by those methods.</li>
 *   <li>All setter methods delegates to the {@link #setValue(Object, Unit)} method.</li>
 * </ul>
 *
 * Consequently, the above-cited methods provide single points that subclasses can override
 * for modifying the behavior of all getter and setter methods.
 *
 * @param <T> The value type.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see DefaultParameterDescriptor
 * @see DefaultParameterGroup
 */
public class DefaultParameterValue<T> extends AbstractParameterValue implements ParameterValue<T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5837826787089486776L;

    /**
     * The value, or {@code null} if undefined.
     * Except for the constructors, the {@link #equals(Object)} and the {@link #hashCode()} methods,
     * this field shall be read only by {@link #getValue()} and written by {@link #setValue(Object, Unit)}.
     */
    private T value;

    /**
     * The unit of measure for the value, or {@code null} if it doesn't apply.
     * Except for the constructors, the {@link #equals(Object)} and the {@link #hashCode()} methods,
     * this field shall be read only by {@link #getUnit()} and written by {@link #setValue(Object, Unit)}.
     */
    private Unit<?> unit;

    /**
     * Creates a parameter value from the specified descriptor.
     * The value will be initialized to the default value, if any.
     *
     * @param descriptor The abstract definition of this parameter.
     */
    public DefaultParameterValue(final ParameterDescriptor<T> descriptor) {
        super(descriptor);
        value = descriptor.getDefaultValue();
        unit  = descriptor.getUnit();
    }

    /**
     * Creates a new instance initialized with the values from the specified parameter object.
     * This is a <em>shallow</em> copy constructor, since the value contained in the given
     * object is not cloned.
     *
     * @param parameter The parameter to copy values from.
     */
    public DefaultParameterValue(final ParameterValue<T> parameter) {
        super(parameter); // Require <T>, not <? extends T>.
        value = parameter.getValue();
        unit  = parameter.getUnit();
    }

    /**
     * Returns the abstract definition of this parameter.
     */
    @Override
    @SuppressWarnings("unchecked") // Type checked by the constructor.
    public ParameterDescriptor<T> getDescriptor() {
        return (ParameterDescriptor<T>) super.getDescriptor();
    }

    /**
     * Returns the unit of measure of the parameter value.
     * If the parameter value has no unit (for example because it is a {@link String} type),
     * then this method returns {@code null}. Note that "no unit" does not mean "dimensionless".
     *
     * {@section Implementation note for subclasses}
     * All getter methods which need unit information will invoke this {@code getUnit()} method.
     * Subclasses can override this method if they need to compute the unit dynamically.
     *
     * @return The unit of measure, or {@code null} if none.
     *
     * @see #doubleValue()
     * @see #doubleValueList()
     * @see #getValue()
     */
    @Override
    public Unit<?> getUnit() {
        return unit;
    }

    /**
     * Returns the parameter value as an object.
     * If no value has been set, then this method returns the
     * {@linkplain DefaultParameterDescriptor#getDefaultValue() default value} (which may be null).
     *
     * {@section Implementation note for subclasses}
     * All getter methods will invoke this {@code getValue()} method.
     * Subclasses can override this method if they need to compute the value dynamically.
     *
     * @return The parameter value as an object, or {@code null} if no value has been set
     *         and there is no default value.
     *
     * @see #setValue(Object)
     */
    @Override
    public T getValue() {
        return value;
    }

    /**
     * Returns the boolean value of this parameter.
     * A boolean value does not have an associated unit of measure.
     *
     * <p>The default implementation invokes {@link #getValue()} and casts the result if possible,
     * or throws an exception otherwise.</p>
     *
     * @return The boolean value represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not a boolean type.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #setValue(boolean)
     */
    @Override
    public boolean booleanValue() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        throw incompatibleValue(value);
    }

    /**
     * Returns the integer value of this parameter, usually used for a count.
     * An integer value does not have an associated unit of measure.
     *
     * <p>The default implementation invokes {@link #getValue()} and casts the result if possible,
     * or throws an exception otherwise.</p>
     *
     * @return The numeric value represented by this parameter after conversion to type {@code int}.
     * @throws InvalidParameterTypeException if the value is not an integer type.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #setValue(int)
     * @see #intValueList()
     */
    @Override
    public int intValue() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof Number) {
            final int integer = ((Number) value).intValue();
            if (integer == ((Number) value).doubleValue()) {
                return integer;
            }
        }
        throw incompatibleValue(value);
    }

    /**
     * Returns an ordered sequence of two or more integer values of this parameter, usually used for counts.
     * These integer values do not have an associated unit of measure.
     *
     * <p>The default implementation invokes {@link #getValue()} and casts the result if possible,
     * or throws an exception otherwise. Note that the returned array is a direct reference to the
     * array stored by this parameter.</p>
     *
     * @return The sequence of values represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not an array of {@code int}s.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #setValue(Object)
     * @see #intValue()
     */
    @Override
    public int[] intValueList() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof int[]) {
            return (int[]) value;
        }
        throw incompatibleValue(value);
    }

    /**
     * Returns the numeric value of this parameter.
     * The units of measurement are specified by {@link #getUnit()}.
     *
     * {@note The behavior of this method is slightly different than the equivalent method in
     *        the <code>FloatParameter</code> class, since this method throws an exception instead than
     *        returning <code>NaN</code> if no value has been explicitely set. This method behaves that
     *        way for consistency will other methods defined in this class, since all of them except
     *        <code>getValue()</code> throw an exception in such case.}
     *
     * <p>The default implementation invokes {@link #getValue()} and casts the result if possible,
     * or throws an exception otherwise.</p>
     *
     * @return The numeric value represented by this parameter after conversion to type {@code double}.
     *         This method returns {@link Double#NaN} only if such "value" has been explicitely set.
     * @throws InvalidParameterTypeException if the value is not a numeric type.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getUnit()
     * @see #setValue(double)
     * @see #doubleValueList()
     */
    @Override
    public double doubleValue() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw incompatibleValue(value);
    }

    /**
     * Returns an ordered sequence of two or more numeric values of this parameter,
     * where each value has the same associated unit of measure.
     *
     * <p>The default implementation invokes {@link #getValue()} and casts the result if possible,
     * or throws an exception otherwise. Note that the returned array is a direct reference to the
     * array stored by this parameter.</p>
     *
     * @return The sequence of values represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not an array of {@code double}s.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getUnit()
     * @see #setValue(Object)
     * @see #doubleValue()
     */
    @Override
    public double[] doubleValueList() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof double[]) {
            return (double[]) value;
        }
        throw incompatibleValue(value);
    }

    /**
     * Returns the converter to be used by {@link #doubleValue(Unit)} and {@link #doubleValueList(Unit)}.
     *
     * @see #getConverterFrom(Unit)
     */
    private UnitConverter getConverterTo(final Unit<?> unit) {
        final Unit<?> source = getUnit();
        if (source == null) {
            throw Verifier.unitlessParameter(descriptor);
        }
        ensureNonNull("unit", unit);
        final short expectedID = Verifier.getUnitMessageID(source);
        if (Verifier.getUnitMessageID(unit) != expectedID) {
            throw new IllegalArgumentException(Errors.format(expectedID, unit));
        }
        try {
            return source.getConverterToAny(unit);
        } catch (ConversionException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleUnits_2, source, unit), e);
        }
    }

    /**
     * Returns the numeric value of this parameter in the given unit of measure.
     * This convenience method applies unit conversions on the fly as needed.
     *
     * <p>The default implementation invokes {@link #doubleValue()} and {@link #getUnit()},
     * then converts the values to the given unit of measurement.</p>
     *
     * @param  unit The unit of measure for the value to be returned.
     * @return The numeric value represented by this parameter after conversion to type
     *         {@code double} and conversion to {@code unit}.
     * @throws IllegalArgumentException if the specified unit is invalid for this parameter.
     * @throws InvalidParameterTypeException if the value is not a numeric type.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getUnit()
     * @see #setValue(double,Unit)
     * @see #doubleValueList(Unit)
     */
    @Override
    public double doubleValue(final Unit<?> unit) throws IllegalArgumentException, IllegalStateException {
        return getConverterTo(unit).convert(doubleValue());
    }

    /**
     * Returns an ordered sequence of numeric values in the specified unit of measure.
     * This convenience method applies unit conversions on the fly as needed.
     *
     * <p>The default implementation invokes {@link #doubleValueList()} and {@link #getUnit()},
     * clone the array, then converts the values to the given unit of measurement.</p>
     *
     * @param  unit The unit of measure for the value to be returned.
     * @return The sequence of values represented by this parameter after conversion to type
     *         {@code double} and conversion to {@code unit}.
     * @throws IllegalArgumentException if the specified unit is invalid for this parameter.
     * @throws InvalidParameterTypeException if the value is not an array of {@code double}s.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getUnit()
     * @see #setValue(double[],Unit)
     * @see #doubleValue(Unit)
     */
    @Override
    public double[] doubleValueList(final Unit<?> unit) throws IllegalArgumentException, IllegalStateException {
        final UnitConverter converter = getConverterTo(unit);
        final double[] values = doubleValueList().clone();
        for (int i=0; i<values.length; i++) {
            values[i] = converter.convert(values[i]);
        }
        return values;
    }

    /**
     * Returns the string value of this parameter.
     * A string value does not have an associated unit of measure.
     *
     * @return The string value represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not a string.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getValue()
     * @see #setValue(Object)
     */
    @Override
    public String stringValue() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof CharSequence) {
            return value.toString();
        }
        throw incompatibleValue(value);
    }

    /**
     * Returns a reference to a file or a part of a file containing one or more parameter values.
     * The default implementation can convert the following value types:
     * {@link URI}, {@link URL}, {@link Path}, {@link File}.
     *
     * @return The reference to a file containing parameter values.
     * @throws InvalidParameterTypeException if the value is not a reference to a file or an URI.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getValue()
     * @see #setValue(Object)
     */
    @Override
    public URI valueFile() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof URI) {
            return (URI) value;
        }
        if (value instanceof File) {
            return ((File) value).toURI();
        }
        if (value instanceof Path) {
            return ((Path) value).toUri();
        }
        Exception cause = null;
        try {
            if (value instanceof URL) {
                return ((URL) value).toURI();
            }
        } catch (URISyntaxException exception) {
            cause = exception;
        }
        final String name = Verifier.getName(descriptor);
        if (value != null) {
            throw new InvalidParameterTypeException(getClassTypeError(), name);
        }
        throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForParameter_1, cause, name));
    }

    /**
     * Returns the exception to throw when an incompatible method is invoked for the value type.
     */
    private IllegalStateException incompatibleValue(final Object value) {
        final String name = Verifier.getName(descriptor);
        if (value != null) {
            return new InvalidParameterTypeException(getClassTypeError(), name);
        }
        return new IllegalStateException(Errors.format(Errors.Keys.MissingValueForParameter_1, name));
    }

    /**
     * Formats an error message for illegal method call for the current value type.
     */
    private String getClassTypeError() {
        return Errors.format(Errors.Keys.IllegalOperationForValueClass_1,
                ((ParameterDescriptor<?>) descriptor).getValueClass());
    }

    /**
     * Sets the parameter value as an object. The object type is typically a {@link Double}, {@link Integer},
     * {@link Boolean}, {@link String}, {@link URI}, {@code double[]} or {@code int[]}.
     *
     * <p>The default implementation delegates to {@link #setValue(Object, Unit)}.</p>
     *
     * @param  value The parameter value, or {@code null} to restore the default value.
     * @throws InvalidParameterValueException if the type of {@code value} is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example the value is numeric and out of range).
     *
     * @see #getValue()
     */
    @Override
    public void setValue(final Object value) throws InvalidParameterValueException {
        // Use 'unit' instead than 'getUnit()' despite class Javadoc claims because units are not expected
        // to be involved in this method. We just want the current unit setting to be unchanged.
        setValue(value, unit);
    }

    /**
     * Sets the parameter value as a boolean.
     *
     * <p>The default implementation delegates to {@link #setValue(Object, Unit)}.</p>
     *
     * @param  value The parameter value.
     * @throws InvalidParameterValueException if the boolean type is inappropriate for this parameter.
     *
     * @see #booleanValue()
     */
    @Override
    public void setValue(final boolean value) throws InvalidParameterValueException {
        // Use 'unit' instead than 'getUnit()' despite class Javadoc claims because units are not expected
        // to be involved in this method. We just want the current unit setting to be unchanged.
        setValue(Boolean.valueOf(value), unit);
    }

    /**
     * Sets the parameter value as an integer.
     *
     * <p>The default implementation wraps the given integer in an object of the type specified by the
     * {@linkplain #getDescriptor() descriptor}, then delegates to {@link #setValue(Object, Unit)}.</p>
     *
     * @param  value The parameter value.
     * @throws InvalidParameterValueException if the integer type is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example a value out of range).
     *
     * @see #intValue()
     */
    @Override
    public void setValue(final int value) throws InvalidParameterValueException {
        Number n = Integer.valueOf(value);
        final Class<?> valueClass = ((ParameterDescriptor<?>) descriptor).getValueClass();
        if (Number.class.isAssignableFrom(valueClass)) {
            @SuppressWarnings("unchecked")
            final Number c = Numbers.cast(value, (Class<? extends Number>) valueClass);
            if (c.intValue() == value) {
                n = c;
            }
        }
        setValue(n, unit);
        // Use 'unit' instead than 'getUnit()' despite class Javadoc claims because units are not expected
        // to be involved in this method. We just want the current unit setting to be unchanged.
    }

    /**
     * Wraps the given value in a type compatible with the expected value class, if possible.
     */
    @SuppressWarnings("unchecked")
    private static Number wrap(final double value, final Class<?> valueClass) {
        if (Number.class.isAssignableFrom(valueClass)) {
            return Numbers.wrap(value, (Class<? extends Number>) valueClass);
        } else {
            return Numerics.valueOf(value);
        }
    }

    /**
     * Sets the parameter value as a floating point. The unit, if any, stay unchanged.
     *
     * <p>The default implementation wraps the given number in an object of the type specified by the
     * {@linkplain #getDescriptor() descriptor}, then delegates to {@link #setValue(Object, Unit)}.</p>
     *
     * @param value The parameter value.
     * @throws InvalidParameterValueException if the floating point type is inappropriate for this
     *         parameter, or if the value is illegal for some other reason (for example a value out
     *         of range).
     *
     * @see #setValue(double,Unit)
     * @see #doubleValue()
     */
    @Override
    public void setValue(final double value) throws InvalidParameterValueException {
        // Use 'unit' instead than 'getUnit()' despite class Javadoc claims because units are not expected
        // to be involved in this method. We just want the current unit setting to be unchanged.
        setValue(wrap(value, ((ParameterDescriptor<?>) descriptor).getValueClass()), unit);
    }

    /**
     * Sets the parameter value as a floating point and its associated unit.
     *
     * <p>The default implementation wraps the given number in an object of the type specified by the
     * {@linkplain #getDescriptor() descriptor}, then delegates to {@link #setValue(Object, Unit)}.</p>
     *
     * @param  value The parameter value.
     * @param  unit The unit for the specified value.
     * @throws InvalidParameterValueException if the floating point type is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example a value out of range).
     *
     * @see #setValue(double)
     * @see #doubleValue(Unit)
     */
    @Override
    public void setValue(final double value, final Unit<?> unit) throws InvalidParameterValueException {
        setValue(wrap(value, ((ParameterDescriptor<?>) descriptor).getValueClass()), unit);
    }

    /**
     * Sets the parameter value as an array of floating point and their associated unit.
     *
     * <p>The default implementation delegates to {@link #setValue(Object, Unit)}.</p>
     *
     * @param  values The parameter values.
     * @param  unit The unit for the specified value.
     * @throws InvalidParameterValueException if the floating point array type is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example a value out of range).
     */
    @Override
    public void setValue(final double[] values, final Unit<?> unit) throws InvalidParameterValueException {
        setValue(value, unit);
    }

    /**
     * Sets the parameter value and its associated unit.
     * This method is invoked by all setter methods in this class, thus providing a single point that
     * subclasses can override if they want to perform more processing on the value before its storage,
     * or to be notified about value changes.
     *
     * @param  value The parameter value, or {@code null} to restore the default value.
     * @param  unit  The unit associated to the new parameter value, or {@code null}.
     * @throws InvalidParameterValueException if the type of {@code value} is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example the value is numeric and out of range).
     */
    @SuppressWarnings("unchecked") // Safe because descriptor type is enforced by constructor signature.
    protected void setValue(final Object value, final Unit<?> unit) throws InvalidParameterValueException {
        this.value = Verifier.ensureValidValue((ParameterDescriptor<T>) descriptor, value, unit);
        this.unit  = unit; // Assign only on success.
    }

    /**
     * Compares the specified object with this parameter for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        if (super.equals(object)) {
            final DefaultParameterValue<?> that = (DefaultParameterValue<?>) object;
            return Objects.equals(this.value, that.value) &&
                   Objects.equals(this.unit,  that.unit);
        }
        return false;
    }

    /**
     * Returns a hash value for this parameter.
     *
     * @return The hash code value. This value doesn't need to be the same
     *         in past or future versions of this class.
     */
    @Override
    public int hashCode() {
        int code = 37 * super.hashCode();
        if (value != null) code +=   value.hashCode();
        if (unit  != null) code += 31*unit.hashCode();
        return code;
    }

    /**
     * Returns a clone of this parameter value.
     */
    @Override
    @SuppressWarnings("unchecked")
    public DefaultParameterValue<T> clone() {
        return (DefaultParameterValue<T>) super.clone();
    }
}
