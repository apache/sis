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

import java.io.Serializable;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.InvalidParameterTypeException;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.internal.jaxb.gml.Measure;
import org.apache.sis.internal.jaxb.gml.MeasureList;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.util.PatchedUnitFormat;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.UnconvertibleObjectException;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A single parameter value used by an operation method. {@code ParameterValue} instances are elements in
 * a {@linkplain DefaultParameterValueGroup parameter value group}, in a way similar to {@code Map.Entry}
 * instances in a {@code java.util.Map}.
 *
 * <p>In the context of coordinate operations, most parameter values are numeric and can be obtained by the
 * {@link #intValue()} or {@link #doubleValue()} methods. But other types of parameter values are possible
 * and can be handled by the more generic {@link #getValue()} and {@link #setValue(Object)} methods.
 * All {@code xxxValue()} methods in this class are convenience methods converting the value from {@code Object}
 * to some commonly used types. Those types are specified in ISO 19111 as an union of attributes, listed below with
 * the corresponding getter and setter methods:</p>
 *
 * <table class="sis">
 *   <caption>Mapping from ISO attributes to getters and setters</caption>
 *   <tr><th>ISO attribute</th>     <th>Java type</th>        <th>Getter method</th>                  <th>Setter method</th></tr>
 *   <tr><td></td>                  <td>{@link Object}</td>   <td>{@link #getValue()}</td>            <td>{@link #setValue(Object)}</td></tr>
 *   <tr><td>stringValue</td>       <td>{@link String}</td>   <td>{@link #stringValue()}</td>         <td>{@link #setValue(Object)}</td></tr>
 *   <tr><td>value</td>             <td>{@code double}</td>   <td>{@link #doubleValue()}</td>         <td>{@link #setValue(double)}</td></tr>
 *   <tr><td></td>                  <td>{@code double}</td>   <td>{@link #doubleValue(Unit)}</td>     <td>{@link #setValue(double, Unit)}</td></tr>
 *   <tr><td>valueList</td>         <td>{@code double[]}</td> <td>{@link #doubleValueList()}</td>     <td>{@link #setValue(Object)}</td></tr>
 *   <tr><td></td>                  <td>{@code double[]}</td> <td>{@link #doubleValueList(Unit)}</td> <td>{@link #setValue(double[], Unit)}</td></tr>
 *   <tr><td>integerValue</td>      <td>{@code int}</td>      <td>{@link #intValue()}</td>            <td>{@link #setValue(int)}</td></tr>
 *   <tr><td>integerValueList</td>  <td>{@code int[]}</td>    <td>{@link #intValueList()}</td>        <td>{@link #setValue(Object)}</td></tr>
 *   <tr><td>booleanValue</td>      <td>{@code boolean}</td>  <td>{@link #booleanValue()}</td>        <td>{@link #setValue(boolean)}</td></tr>
 *   <tr><td>valueFile</td>         <td>{@link URI}</td>      <td>{@link #valueFile()}</td>           <td>{@link #setValue(Object)}</td></tr>
 *   <tr><td>valueFileCitation</td> <td>{@link Citation}</td> <td>{@link #getValue()}</td>            <td>{@link #setValue(Object)}</td></tr>
 * </table>
 *
 * The type and constraints on parameter values are given by the {@linkplain #getDescriptor() descriptor},
 * which is specified at construction time. The parameter type can be fetch with the following idiom:
 *
 * {@preformat java
 *     Class<T> valueClass = parameter.getDescriptor().getValueClass();
 * }
 *
 * <div class="section">Instantiation</div>
 * A {@linkplain DefaultParameterDescriptor parameter descriptor} must be defined before parameter value can be created.
 * Descriptors are usually pre-defined by map projection or process providers. Given a descriptor, a parameter value can
 * be created by a call to the {@link #DefaultParameterValue(ParameterDescriptor)} constructor or by a call to the
 * {@link ParameterDescriptor#createValue()} method. The later is recommended since it allows descriptors to return
 * specialized implementations.
 *
 * <div class="section">Implementation note for subclasses</div>
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
 * @param <T> The type of the value stored in this parameter.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see DefaultParameterDescriptor
 * @see DefaultParameterValueGroup
 */
@XmlType(name = "ParameterValueType", propOrder = {
    "xmlValue",
    "descriptor"
})
@XmlRootElement(name = "ParameterValue")
public class DefaultParameterValue<T> extends FormattableObject implements ParameterValue<T>,
        LenientComparable, Serializable, Cloneable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5837826787089486776L;

    /**
     * The definition of this parameter.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDescriptor(ParameterDescriptor)}</p>
     *
     * @see #getDescriptor()
     */
    private ParameterDescriptor<T> descriptor;

    /**
     * The value, or {@code null} if undefined.
     * Except for the constructors, the {@link #equals(Object)} and the {@link #hashCode()} methods,
     * this field should be read only by {@link #getValue()} and written only by {@link #setValue(Object, Unit)}.
     *
     * @since 0.7
     */
    protected T value;

    /**
     * The unit of measure for the value, or {@code null} if it does not apply.
     * Except for the constructors, the {@link #equals(Object)} and the {@link #hashCode()} methods,
     * this field should be read only by {@link #getUnit()} and written only by {@link #setValue(Object, Unit)}.
     *
     * @since 0.7
     */
    protected Unit<?> unit;

    /**
     * Creates a parameter value from the specified descriptor.
     * The value will be initialized to the default value, if any.
     *
     * @param descriptor The abstract definition of this parameter.
     */
    public DefaultParameterValue(final ParameterDescriptor<T> descriptor) {
        ensureNonNull("descriptor", descriptor);
        this.descriptor = descriptor;
        this.value      = descriptor.getDefaultValue();
        this.unit       = descriptor.getUnit();
    }

    /**
     * Creates a new instance initialized with the values from the specified parameter object.
     * This is a <em>shallow</em> copy constructor, since the value contained in the given
     * object is not cloned.
     *
     * @param parameter The parameter to copy values from.
     *
     * @see #clone()
     * @see #unmodifiable(ParameterValue)
     */
    public DefaultParameterValue(final ParameterValue<T> parameter) {
        ensureNonNull("parameter", parameter);
        descriptor = parameter.getDescriptor();
        value      = parameter.getValue();
        unit       = parameter.getUnit();
    }

    /**
     * Returns the definition of this parameter.
     *
     * @return The definition of this parameter.
     */
    @Override
    @XmlElement(name = "operationParameter", required = true)
    public ParameterDescriptor<T> getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the unit of measure of the parameter value.
     * If the parameter value has no unit (for example because it is a {@link String} type),
     * then this method returns {@code null}. Note that "no unit" does not mean "dimensionless".
     *
     * <div class="section">Implementation note for subclasses</div>
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
     * <div class="section">Implementation note for subclasses</div>
     * All getter methods will invoke this {@code getValue()} method.
     * Subclasses can override this method if they need to compute the value dynamically.
     *
     * @return The parameter value as an object, or {@code null} if no value has been set
     *         and there is no default value.
     *
     * @see #setValue(Object)
     * @see Parameters#getValue(ParameterDescriptor)
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
     * @see Parameters#booleanValue(ParameterDescriptor)
     */
    @Override
    public boolean booleanValue() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw missingOrIncompatibleValue(value);
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
     * @see Parameters#intValue(ParameterDescriptor)
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
        throw missingOrIncompatibleValue(value);
    }

    /**
     * Returns an ordered sequence of two or more integer values of this parameter, usually used for counts.
     *
     * <p>The default implementation invokes {@link #getValue()} and casts the result if possible,
     * or throws an exception otherwise. If the value can be casted, then the array is cloned before
     * to be returned.</p>
     *
     * @return A copy of the sequence of values represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not an array of {@code int}s.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #setValue(Object)
     * @see #intValue()
     * @see Parameters#intValueList(ParameterDescriptor)
     */
    @Override
    public int[] intValueList() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof int[]) {
            return ((int[]) value).clone();
        }
        throw missingOrIncompatibleValue(value);
    }

    /**
     * Returns the numeric value of this parameter.
     * The units of measurement are specified by {@link #getUnit()}.
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
     * @see Parameters#doubleValue(ParameterDescriptor)
     */
    @Override
    public double doubleValue() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw missingOrIncompatibleValue(value);
    }

    /**
     * Returns an ordered sequence of two or more numeric values of this parameter,
     * where each value has the same associated unit of measure.
     *
     * <p>The default implementation invokes {@link #getValue()} and casts the result if possible,
     * or throws an exception otherwise. If the value can be casted, then the array is cloned before
     * to be returned.</p>
     *
     * @return A copy of the sequence of values represented by this parameter.
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
            return ((double[]) value).clone();
        }
        throw missingOrIncompatibleValue(value);
    }

    /**
     * Returns the converter to be used by {@link #doubleValue(Unit)} and {@link #doubleValueList(Unit)}.
     */
    private UnitConverter getConverterTo(final Unit<?> unit) {
        final Unit<?> source = getUnit();
        if (source == null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnitlessParameter_1, Verifier.getDisplayName(descriptor)));
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
     * @see Parameters#doubleValue(ParameterDescriptor)
     */
    @Override
    public double doubleValue(final Unit<?> unit) throws IllegalArgumentException, IllegalStateException {
        final double value = doubleValue(); // Invoke first in case it throws an exception.
        return getConverterTo(unit).convert(value);
    }

    /**
     * Returns an ordered sequence of numeric values in the specified unit of measure.
     * This convenience method applies unit conversions on the fly as needed.
     *
     * <p>The default implementation invokes {@link #doubleValueList()} and {@link #getUnit()},
     * then converts the values to the given unit of measurement.</p>
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
     * @see Parameters#doubleValueList(ParameterDescriptor)
     */
    @Override
    public double[] doubleValueList(final Unit<?> unit) throws IllegalArgumentException, IllegalStateException {
        final UnitConverter converter = getConverterTo(unit);
        final double[] values = doubleValueList();
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
     * @see Parameters#stringValue(ParameterDescriptor)
     */
    @Override
    public String stringValue() throws IllegalStateException {
        final T value = getValue();
        if (value instanceof CharSequence) {
            return value.toString();
        }
        throw missingOrIncompatibleValue(value);
    }

    /**
     * Returns a reference to a file or a part of a file containing one or more parameter values.
     * The default implementation can convert the following value types:
     * {@link URI}, {@link URL}, {@link File}.
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
        Exception cause = null;
        try {
            if (value instanceof URL) {
                return ((URL) value).toURI();
            }
        } catch (URISyntaxException exception) {
            cause = exception;
        }
        final String name = Verifier.getDisplayName(descriptor);
        if (value != null) {
            throw new InvalidParameterTypeException(getClassTypeError(), name);
        }
        throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForParameter_1, cause, name));
    }

    /**
     * Returns {@code true} if the given value is an instance of one of the types documented in {@link #valueFile()}.
     */
    private static boolean isFile(final Object value) {
        return (value instanceof URI || value instanceof URL || value instanceof File);
    }

    /**
     * Same as {@link #isFile(Object)}, but accepts also a {@link String} if the type specified
     * in the parameter descriptor is one of the types documented in {@link #valueFile()}.
     */
    private boolean isOrNeedFile(final Object value) {
        if (value instanceof String) {
            final Class<?> type = descriptor.getValueClass();
            return (type == URI.class) || (type == URL.class)
                   || File.class.isAssignableFrom(type);
        }
        return isFile(value);
    }

    /**
     * Returns the exception to throw when an incompatible method is invoked for the value type.
     */
    private IllegalStateException missingOrIncompatibleValue(final Object value) {
        final String name = Verifier.getDisplayName(descriptor);
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
                (descriptor != null) ? ((ParameterDescriptor<?>) descriptor).getValueClass() : "?");
    }

    /**
     * Sets the parameter value as an object. The object type is typically (but not limited to) {@link Double},
     * {@code double[]}, {@link Integer}, {@code int[]}, {@link Boolean}, {@link String} or {@link URI}.
     * If the given value is {@code null}, then this parameter is set to the
     * {@linkplain DefaultParameterDescriptor#getDefaultValue() default value}.
     *
     * <p>The default implementation delegates to {@link #setValue(Object, Unit)}.
     * This implementation does not clone the given value. In particular, references to {@code int[]}
     * and {@code double[]} arrays are stored <cite>as-is</cite>.</p>
     *
     * @param  value The parameter value, or {@code null} to restore the default.
     * @throws InvalidParameterValueException if the type of {@code value} is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example the value is numeric and out of range).
     *
     * @see #getValue()
     */
    @Override
    public void setValue(Object value) throws InvalidParameterValueException {
        /*
         * Try to convert the value only for a limited amount of types. In particular we want to allow conversions
         * between java.io.File and java.nio.file.Path for easier transition between JDK6 and JDK7. We do not want
         * to allow too many conversions for reducing the risk of unexpected behavior.  If we fail to convert, try
         * to set the value anyway since the user may have redefined the setValue(Object, Unit) method.
         */
        if (isOrNeedFile(value)) try {
            value = ObjectConverters.convert(value, descriptor.getValueClass());
        } catch (UnconvertibleObjectException e) {
            // Level.FINE (not WARNING) because this log duplicates the exception
            // that 'setValue(Object, Unit)' may throw (with a better message).
            Logging.recoverableException(Logging.getLogger(Loggers.COORDINATE_OPERATION),
                    DefaultParameterValue.class, "setValue", e);
        }
        /*
         * Use 'unit' instead than 'getUnit()' despite class Javadoc claims because units are not expected
         * to be involved in this method. We just want the current unit setting to be unchanged.
         */
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
        setValue(value, unit);
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
        Number n = value;
        final Class<T> valueClass = descriptor.getValueClass();
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
     *
     * @throws IllegalArgumentException If the given value can not be converted to the given type.
     */
    @SuppressWarnings("unchecked")
    private static Number wrap(final double value, final Class<?> valueClass) throws IllegalArgumentException {
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
        try {
            // Use 'unit' instead than 'getUnit()' despite class Javadoc claims because units are not expected
            // to be involved in this method. We just want the current unit setting to be unchanged.
            setValue(wrap(value, descriptor.getValueClass()), unit);
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException(e.getLocalizedMessage(), Verifier.getDisplayName(descriptor), value);
        }
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
        try {
            setValue(wrap(value, descriptor.getValueClass()), unit);
        } catch (InvalidParameterValueException e) {
            throw e;        // Need to be thrown explicitely because it is a subclass of IllegalArgumentException.
        } catch (IllegalArgumentException e) {
            throw (InvalidParameterValueException) new InvalidParameterValueException(
                    e.getLocalizedMessage(), Verifier.getDisplayName(descriptor), value).initCause(e);
        }
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
        setValue((Object) values, unit);
    }

    /**
     * Sets the parameter value and its associated unit.
     * If the given value is {@code null}, then this parameter is set to the
     * {@linkplain DefaultParameterDescriptor#getDefaultValue() default value}.
     *
     * <p>Current implementation does not clone the given value. In particular, references to
     * {@code int[]} and {@code double[]} arrays are stored <cite>as-is</cite>.</p>
     *
     * <div class="section">Implementation note for subclasses</div>
     * This method is invoked by all setter methods in this class, thus providing a single point that
     * subclasses can override if they want to perform more processing on the value before its storage,
     * or to be notified about value changes.
     *
     * @param  value The parameter value, or {@code null} to restore the default.
     * @param  unit  The unit associated to the new parameter value, or {@code null}.
     * @throws InvalidParameterValueException if the type of {@code value} is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example the value is numeric and out of range).
     *
     * @see #validate(Object)
     */
    @SuppressWarnings("unchecked")
    protected void setValue(final Object value, final Unit<?> unit) throws InvalidParameterValueException {
        final T convertedValue = Verifier.ensureValidValue(descriptor, value, unit);
        if (value != null) {
            validate(convertedValue);
            this.value = (T) value;                 // Type has been verified by Verifier.ensureValidValue(…).
        } else {
            this.value = descriptor.getDefaultValue();
        }
        this.unit = unit;                           // Assign only on success.
    }

    /**
     * Invoked by {@link #setValue(Object, Unit)} after the basic verifications have been done and before
     * the value is stored. Subclasses can override this method for performing additional verifications.
     *
     * <div class="section">Unit of measurement</div>
     * If the user specified a unit of measurement, then the value given to this method has been converted
     * to the unit specified by the {@linkplain #getDescriptor() descriptor}, for easier comparisons against
     * standardized values. This converted value may be different than the value to be stored in this
     * {@code ParameterValue}, since the later value will be stored in the unit specified by the user.
     *
     * <div class="section">Standard validations</div>
     * The checks for {@linkplain DefaultParameterDescriptor#getValueClass() value class},
     * for {@linkplain DefaultParameterDescriptor#getValueDomain() value domain} and for
     * {@linkplain DefaultParameterDescriptor#getValidValues() valid values} are performed
     * before this method is invoked. The default implementation of this method does nothing.
     *
     * @param  value The value converted to the unit of measurement specified by the descriptor.
     * @throws InvalidParameterValueException If the given value is invalid for implementation-specific reasons.
     */
    protected void validate(final T value) throws InvalidParameterValueException {
    }

    /**
     * Compares the specified object with this parameter for equality.
     * The strictness level is controlled by the second argument.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal according the given comparison mode.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        if (object != null) {
            if (mode == ComparisonMode.STRICT) {
                if (getClass() == object.getClass()) {
                    final DefaultParameterValue<?> that = (DefaultParameterValue<?>) object;
                    return Objects.equals(descriptor, that.descriptor) &&
                           Objects.equals(value,      that.value) &&
                           Objects.equals(unit,       that.unit);
                }
            } else if (object instanceof ParameterValue<?>) {
                final ParameterValue<?> that = (ParameterValue<?>) object;
                return deepEquals(getDescriptor(), that.getDescriptor(), mode) &&
                       deepEquals(getValue(),      that.getValue(), mode) &&
                       Objects.equals(getUnit(),   that.getUnit());
            }
        }
        return false;
    }

    /**
     * Compares the specified object with this parameter for equality.
     * This method is implemented as below:
     *
     * {@preformat java
     *     return equals(other, ComparisonMode.STRICT);
     * }
     *
     * Subclasses shall override {@link #equals(Object, ComparisonMode)} instead than this method.
     *
     * @param  object The object to compare to {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        return equals(object, ComparisonMode.STRICT);
    }

    /**
     * Returns a hash value for this parameter.
     *
     * @return The hash code value. This value doesn't need to be the same
     *         in past or future versions of this class.
     */
    @Override
    public int hashCode() {
        int code = 37 * descriptor.hashCode();
        if (value != null) code +=   value.hashCode();
        if (unit  != null) code += 31*unit.hashCode();
        return code;
    }

    /**
     * Returns a clone of this parameter value.
     *
     * @see #DefaultParameterValue(ParameterValue)
     * @see #unmodifiable(ParameterValue)
     */
    @Override
    @SuppressWarnings("unchecked")
    public DefaultParameterValue<T> clone() {
        try {
            return (DefaultParameterValue<T>) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception); // Should not happen, since we are cloneable
        }
    }

    /**
     * Returns an unmodifiable implementation of the given parameter value.
     * This method shall be used only with:
     *
     * <ul>
     *   <li>immutable {@linkplain #getDescriptor() descriptor},</li>
     *   <li>immutable or null {@linkplain #getUnit() unit}, and</li>
     *   <li>immutable or {@linkplain Cloneable cloneable} parameter {@linkplain #getValue() value}.</li>
     * </ul>
     *
     * If the parameter value implements the {@link Cloneable} interface and has a public {@code clone()} method,
     * then that value will be cloned every time the {@link #getValue()} method is invoked.
     * The value is not cloned by this method however; it is caller's responsibility to not modify the value of
     * the given {@code parameter} instance after this method call.
     *
     * <div class="section">Instances sharing</div>
     * If this method is invoked more than once with equal {@linkplain #getDescriptor() descriptor},
     * {@linkplain #getValue() value} and {@linkplain #getUnit() unit}, then this method will return
     * the same {@code DefaultParameterValue} instance on a <cite>best effort</cite> basis.
     *
     * <div class="note"><b>Rational:</b>
     * the same parameter value is often used in many different coordinate operations. For example all <cite>Universal
     * Transverse Mercator</cite> (UTM) projections use the same scale factor (0.9996) and false easting (500000 metres).
     * </div>
     *
     * @param  <T> The type of the value stored in the given parameter.
     * @param  parameter The parameter to make unmodifiable, or {@code null}.
     * @return An unmodifiable implementation of the given parameter, or {@code null} if the given parameter was null.
     *
     * @since 0.6
     *
     * @see DefaultParameterValueGroup#unmodifiable(ParameterValueGroup)
     */
    public static <T> DefaultParameterValue<T> unmodifiable(final ParameterValue<T> parameter) {
        return UnmodifiableParameterValue.create(parameter);
    }

    /**
     * Formats this parameter as a <cite>Well Known Text</cite> {@code Parameter[…]} element.
     * Example:
     *
     * {@preformat wkt
     *   Parameter["False easting", 0.0, LengthUnit["metre", 1]]
     * }
     *
     * <div class="section">Unit of measurement</div>
     * The units of measurement were never specified in WKT 1 format, and are optional in WKT 2 format.
     * If the units are not specified, then they are inferred from the context.
     * Typically, parameter values that are lengths are given in the unit for the projected CRS axes
     * while parameter values that are angles are given in the unit for the base geographic CRS.
     *
     * <div class="note"><b>Example:</b>
     * The snippet below show WKT representations of the map projection parameters of a projected CRS
     * (most other elements are omitted). The map projection uses a <cite>"Latitude of natural origin"</cite>
     * parameters which is set to 52 <strong>grads</strong>, as defined in the {@code UNIT[…]} element of the
     * enclosing CRS. A similar rule applies to <cite>“False easting”</cite> and <cite>“False northing”</cite>
     * parameters, which are in kilometres in this example.
     *
     * <p><b>WKT 1:</b></p>
     * {@preformat wkt
     *   PROJCS[…,
     *     GEOGCS[…,
     *       UNIT[“grad”, 0.015707963267948967]],       // Unit for all angles
     *     PROJECTION[“Lambert_Conformal_Conic_1SP”]
     *     PARAMETER[“latitude_of_origin”, 52.0],       // In grads
     *     PARAMETER[“scale_factor”, 0.99987742],
     *     PARAMETER[“false_easting”, 600.0],           // In kilometres
     *     PARAMETER[“false_northing”, 2200.0],         // In kilometres
     *     UNIT[“km”, 1000]]                            // Unit for all lengths
     * }
     *
     * <p><b>WKT 2:</b></p>
     * {@preformat wkt
     *   ProjectedCRS[…
     *     BaseGeodCRS[…
     *       AngleUnit[“grad”, 0.015707963267948967]],
     *     Conversion["Lambert zone II",
     *       Method["Lambert Conic Conformal (1SP)"],
     *       Parameter["Latitude of natural origin", 52.0],
     *       Parameter["Scale factor at natural origin", 0.99987742],
     *       Parameter["False easting", 600.0],
     *       Parameter["False northing", 2200.0]],
     *     CS["Cartesian", 2],
     *       LengthUnit["km", 1000]]
     * }
     * </div>
     *
     * @param  formatter The formatter where to format the inner content of this WKT element.
     * @return {@code "Parameter"} or {@code "ParameterFile"}.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#119">WKT 2 specification §17.2.4</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final ParameterDescriptor<T> descriptor = getDescriptor();  // Gives to users a chance to override this property.
        WKTUtilities.appendName(descriptor, formatter, ElementKind.PARAMETER);
        final Convention convention = formatter.getConvention();
        final boolean isWKT1 = convention.majorVersion() == 1;
        Unit<?> unit = getUnit();                                   // Gives to users a chance to override this property.
        if (unit == null) {
            final T value = getValue();                             // Gives to users a chance to override this property.
            if (!isWKT1 && isFile(value)) {
                formatter.append(value.toString(), null);
                return WKTKeywords.ParameterFile;
            } else {
                formatter.appendAny(value);
            }
        } else {
            /*
             * In the WKT 1 specification, the unit of measurement is given by the context.
             * If this parameter value does not use the same unit, we will need to convert it.
             * Otherwise we can write the value as-is.
             *
             * Note that we take the descriptor unit as a starting point instead than this parameter unit
             * in order to give precedence to the descriptor units in Convention.WKT1_COMMON_UNITS mode.
             */
            Unit<?> contextualUnit;
            if (descriptor == null || (contextualUnit = descriptor.getUnit()) == null) {
                // Should be very rare (probably a buggy descriptor), but we try to be safe.
                contextualUnit = unit;
            }
            contextualUnit = formatter.toContextualUnit(contextualUnit);
            boolean ignoreUnits;
            if (isWKT1) {
                unit = contextualUnit;
                ignoreUnits = true;
            } else {
                if (convention != Convention.INTERNAL) {
                    unit = PatchedUnitFormat.toFormattable(unit);
                }
                ignoreUnits = unit.equals(contextualUnit);
            }
            double value;
            try {
                value = doubleValue(unit);
            } catch (IllegalStateException exception) {
                // May happen if a parameter is mandatory (e.g. "semi-major")
                // but no value has been set for this parameter.
                if (descriptor != null) {
                    formatter.setInvalidWKT(descriptor, exception);
                } else {
                    // Null descriptor should be illegal but may happen after unmarshalling of invalid GML.
                    // We make this WKT formatting robust since it is used by 'toString()' implementation.
                    formatter.setInvalidWKT(DefaultParameterValue.class, exception);
                }
                value = Double.NaN;
            }
            formatter.append(value);
            /*
             * In the WKT 2 specification, the unit and the identifier are optional but recommended.
             * We follow that recommendation in strict WKT2 mode, but omit them in non-strict modes.
             * The only exception is when the parameter unit is not the same than the contextual unit,
             * in which case we have no choice: we must format that unit, unless the numerical value
             * is identical in both units (typically the 0 value).
             */
            if (!ignoreUnits && !Double.isNaN(value)) {
                // Test equivalent to unit.equals(contextualUnit) but more aggressive.
                ignoreUnits = Numerics.equals(value, doubleValue(contextualUnit));
            }
            if (ignoreUnits && convention != Convention.INTERNAL) {
                // One last check about if we are really allowed to ignore units.
                ignoreUnits = convention.isSimplified() && hasContextualUnit(formatter);
            }
            if (!ignoreUnits) {
                if (!isWKT1) {
                    formatter.append(unit);
                } else if (!ignoreUnits) {
                    if (descriptor != null) {
                        formatter.setInvalidWKT(descriptor, null);
                    } else {
                        // Null descriptor should be illegal but may happen after unmarshalling of invalid GML.
                        // We make this WKT formatting robust since it is used by 'toString()' implementation.
                        formatter.setInvalidWKT(DefaultParameterValue.class, null);
                    }
                }
            }
        }
        // ID will be added by the Formatter itself.
        return WKTKeywords.Parameter;
    }

    /**
     * Returns {@code true} if the given formatter has contextual units, in which case the WKT format can omit
     * the unit of measurement. The contextual units may be defined either in the direct parent or in the parent
     * of the parent, depending if we are formatting WKT1 or WKT2 respectively. This is because WKT2 wraps the
     * parameters in an additional {@code CONVERSION[…]} element which is not present in WKT1.
     *
     * <p>Taking the example documented in {@link #formatTo(Formatter)}:</p>
     * <ul>
     *   <li>in WKT 1, {@code PROJCS[…]} is the immediate parent of {@code PARAMETER[…]}, but</li>
     *   <li>in WKT 2, {@code ProjectedCRS[…]} is the parent of {@code Conversion[…]},
     *       which is the parent of {@code Parameter[…]}.</li>
     * </ul>
     */
    private static boolean hasContextualUnit(final Formatter formatter) {
        return formatter.hasContextualUnit(1) ||    // In WKT1
               formatter.hasContextualUnit(2);      // In WKT2
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor for JAXB only. The descriptor is initialized to {@code null},
     * but will be assigned a value after XML unmarshalling.
     */
    private DefaultParameterValue() {
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     * May also be invoked by {@link DefaultParameterValueGroup} if the descriptor as been completed
     * with additional information provided in the {@code <gml:group>} element of a descriptor group.
     *
     * @see #getDescriptor()
     */
    final void setDescriptor(final ParameterDescriptor<T> descriptor) {
        this.descriptor = descriptor;
        assert (value == null) || descriptor.getValueClass().isInstance(value) : this;
    }

    /**
     * Invoked by JAXB for obtaining the object to marshal.
     * The property name depends on its type after conversion by this method.
     */
    @XmlElements({
        @XmlElement(name = "value",             type = Measure.class),
        @XmlElement(name = "integerValue",      type = Integer.class),
        @XmlElement(name = "booleanValue",      type = Boolean.class),
        @XmlElement(name = "stringValue",       type = String .class),
        @XmlElement(name = "valueFile",         type = URI    .class),
        @XmlElement(name = "integerValueList",  type = IntegerList.class),
        @XmlElement(name = "valueList",         type = MeasureList.class)
    })
    private Object getXmlValue() {
        final Object value = getValue();    // Give to user a chance to override.
        if (value != null) {
            if (value instanceof Number) {
                final Number n = (Number) value;
                if (Numbers.isInteger(n.getClass())) {
                    final int xmlValue = n.intValue();
                    if (xmlValue >= 0 && xmlValue == n.doubleValue()) {
                        return xmlValue;
                    }
                }
                return new Measure(((Number) value).doubleValue(), getUnit());
            }
            if (value instanceof CharSequence) {
                return value.toString();
            }
            if (isFile(value)) {
                return ObjectConverters.convert(value, URI.class);
            }
            final Class<?> type = Numbers.primitiveToWrapper(value.getClass().getComponentType());
            if (type != null && Number.class.isAssignableFrom(type)) {
                if (Numbers.isInteger(type)) {
                    return new IntegerList(value);
                }
                return new MeasureList(value, type, getUnit());
            }
        }
        return value;
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     */
    @SuppressWarnings("unchecked")
    private void setXmlValue(Object xmlValue) {
        if (value == null && unit == null) {
            if (xmlValue instanceof Measure) {
                final Measure measure = (Measure) xmlValue;
                xmlValue = measure.value;
                unit = measure.unit;
            } else if (xmlValue instanceof MeasureList) {
                final MeasureList measure = (MeasureList) xmlValue;
                xmlValue = measure.toArray();
                unit = measure.unit;
            } else if (xmlValue instanceof IntegerList) {
                xmlValue = ((IntegerList) xmlValue).toArray();
            }
            if (descriptor != null) {
                /*
                 * Should never happen with default SIS implementation, but may happen if the user created
                 * a sub-type of DefaultParameterValue with a default constructor providing the descriptor.
                 */
                value = ObjectConverters.convert(xmlValue, descriptor.getValueClass());
            } else {
                /*
                 * Temporarily accept the value without checking its type. This is required because the
                 * descriptor is normally defined after the value in a GML document. The type will need
                 * to be verified when the descriptor will be set.
                 *
                 * There is no way we can prove that this cast is correct before the descriptor is set,
                 * and maybe that descriptor will never be set if the GML document is illegal. However
                 * this code is executed only during XML unmarshalling, in which case our unmarshalling
                 * process will construct a descriptor compatible with the value rather than the converse.
                 */
                value = (T) xmlValue;
            }
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultParameterValue.class, "setXmlValue", "value");
        }
    }
}
