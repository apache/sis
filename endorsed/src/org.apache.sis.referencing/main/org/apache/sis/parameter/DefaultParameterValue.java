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

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.io.Serializable;
import java.io.File;
import java.nio.file.Path;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.InvalidParameterTypeException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.gml.Measure;
import org.apache.sis.xml.bind.gml.MeasureList;
import org.apache.sis.xml.internal.shared.ExternalLinkHandler;
import org.apache.sis.metadata.internal.shared.ImplementationHelper;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.measure.Units;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;


/**
 * A single parameter value used by an operation method. Each {@code ParameterValue} is a
 * (<var>key</var>, <var>value</var>) pair, and an arbitrary number of those pairs can be
 * stored in a a {@linkplain DefaultParameterValueGroup parameter value group}.
 * In the context of coordinate operations, parameter values are often numeric and can be obtained by the
 * {@link #intValue()} or {@link #doubleValue()} methods. But other types of parameter values are possible
 * and can be handled by the more generic {@link #getValue()} and {@link #setValue(Object)} methods.
 * All {@code xxxValue()} methods in this class are convenience methods converting the value from {@code Object}
 * to some commonly used types. Those types are specified in ISO 19111 as a union of attributes, listed below with
 * the corresponding getter and setter methods:
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
 * {@snippet lang="java" :
 *     Class<T> valueClass = parameter.getDescriptor().getValueClass();
 *     }
 *
 * <h2>Absolute paths of value files</h2>
 * Parameters that are too complex for being expressed as an {@code int[]}, {@code double[]} or {@code String} type
 * may be encoded in auxiliary files. It is the case, for example, of gridded data such as datum shift grids.
 * The name of an auxiliary file is given by {@link #valueFile()}, but often as a <em>relative</em> path.
 * The directory where that file is located depends on the operation using the parameter.
 * For example, datum shift grids used by coordinate transformations are searched in the
 * {@code $SIS_DATA/DatumChanges} directory, where {@code $SIS_DATA} is the value of the environment variable.
 * However, the latest approach requires that all potentially used auxiliary files are preexisting on the local machine.
 * This assumption may be applicable for parameters coming from a well-known registry such as EPSG, but cannot work
 * with arbitrary operations where the auxiliary files need to be transferred together with the parameter values.
 * For the latter case, an alternative is to consider the auxiliary files as relative to the GML document or WKT file
 * that provides the parameter values. For allowing users to resolve or download auxiliary files in that way,
 * a {@link #getSourceFile()} method is provided. Operations can then use {@link URI#resolve(URI)} for getting the
 * absolute path of an auxiliary file from the same server or directory than the GML or WKT file of parameter values.
 *
 * <h2>Instantiation</h2>
 * A {@linkplain DefaultParameterDescriptor parameter descriptor} must be defined before parameter value can be created.
 * Descriptors are usually predefined (often hard-coded) by map projection or process providers. Given a descriptor,
 * the preferred way to create a parameter value is to invoke the {@link ParameterDescriptor#createValue()} method.
 * It is also possible to invoke the {@linkplain #DefaultParameterValue(ParameterDescriptor) constructor} directly,
 * but the former is recommended because it allows descriptors to return specialized implementations.
 *
 * <h2>Implementation note for subclasses</h2>
 * All read and write operations except constructors, {@link #equals(Object)} and {@link #hashCode()},
 * ultimately delegates to the following methods:
 *
 * <ul>
 *   <li>The source file property is accessed by {@link #getSourceFile()} and {@link #setSourceFile(URI)}.</li>
 *   <li>All other getter methods will invoke {@link #getValue()} and {@link #getUnit()} (if needed),
 *       then perform their processing on the values returned by those methods.</li>
 *   <li>All other setter methods delegate to the {@link #setValue(Object, Unit)} method.</li>
 * </ul>
 *
 * Consequently, the above-cited methods provide single points that subclasses can override
 * for modifying the behavior of all getter and setter methods.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @param  <T>  the type of the value stored in this parameter.
 *
 * @see DefaultParameterDescriptor
 * @see DefaultParameterValueGroup
 *
 * @since 0.4
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
     * The logger for parameters.
     */
    static final Logger LOGGER = Logger.getLogger(Loggers.PARAMETER);

    /**
     * The definition of this parameter.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDescriptor(ParameterDescriptor)}</p>
     *
     * @see #getDescriptor()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private ParameterDescriptor<T> descriptor;

    /**
     * The value, or {@code null} if undefined.
     * Except for the constructors, the {@link #equals(Object)} and the {@link #hashCode()} methods,
     * this field should be read only by {@link #getValue()} and written only by {@link #setValue(Object, Unit)}.
     *
     * @since 0.7
     */
    @SuppressWarnings("serial")         // Not statically typed as Serializable.
    protected T value;

    /**
     * The unit of measure for the value, or {@code null} if it does not apply.
     * Except for the constructors, the {@link #equals(Object)} and the {@link #hashCode()} methods,
     * this field should be read only by {@link #getUnit()} and written only by {@link #setValue(Object, Unit)}.
     *
     * @since 0.7
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected Unit<?> unit;

    /**
     * If the parameter value is a relative path, the base URI to use for resolving that path.
     * This field is {@code null} by default, unless this parameter value has been read from a
     * GML document or a WKT file, in which case this field contains the URI of that document.
     * This information allows to interpret {@link #valueFile()} as relative to the GML or WKT file.
     *
     * @see #getSourceFile()
     * @see URI#resolve(URI)
     */
    private URI sourceFile;

    /**
     * Creates a parameter value from the specified descriptor.
     * The value will be initialized to the default value, if any.
     *
     * @param  descriptor  the abstract definition of this parameter.
     */
    public DefaultParameterValue(final ParameterDescriptor<T> descriptor) {
        this.descriptor = descriptor;
        this.value      = descriptor.getDefaultValue();
        this.unit       = descriptor.getUnit();
    }

    /**
     * Creates a new instance initialized with the values from the specified parameter object.
     * This is a <em>shallow</em> copy constructor, since the {@linkplain #getValue() value}
     * contained in the given object is not cloned.
     *
     * @param  parameter  the parameter to copy values from.
     *
     * @see #clone()
     * @see #unmodifiable(ParameterValue)
     */
    public DefaultParameterValue(final ParameterValue<T> parameter) {
        descriptor = parameter.getDescriptor();
        value      = parameter.getValue();
        unit       = parameter.getUnit();
        if (parameter instanceof DefaultParameterValue<?>) {
            sourceFile = ((DefaultParameterValue<?>) parameter).getSourceFile().orElse(null);
        }
    }

    /**
     * Returns the definition of this parameter.
     *
     * @return description of this parameter.
     */
    @Override
    @XmlElement(name = "operationParameter", required = true)
    public ParameterDescriptor<T> getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the <abbr>URI</abbr> of the <abbr>GML</abbr> document
     * or <abbr>WKT</abbr> file from which the parameter values are read.
     * This information allows to interpret {@link #valueFile()} as a path relative to the file that defined
     * this parameter value. For example, the following snippet gets the file, then tries to make it absolute:
     *
     * {@snippet lang="java" :
     *     DefaultParameterValue<?> pv = ...;
     *     URI file = pv.valueFile();
     *     file = pv.getSourceFile().map((base) -> base.resolve(file)).orElse(file);
     *     }
     *
     * @return the <abbr>URI</abbr> of the document from which the parameter values are read.
     *
     * @see #setSourceFile(URI)
     * @see Parameters#getSourceFile(ParameterDescriptor)
     * @see org.apache.sis.io.wkt.WKTFormat#getSourceFile()
     * @see org.apache.sis.xml.MarshalContext#getDocumentURI()
     * @see URI#resolve(URI)
     *
     * @since 1.5
     */
    public Optional<URI> getSourceFile() {
        return Optional.ofNullable(sourceFile);
    }

    /**
     * Returns the unit of measure of the parameter value.
     * If the parameter value has no unit (for example because it is a {@link String} type),
     * then this method returns {@code null}. Note that "no unit" does not mean "dimensionless".
     *
     * <h4>Implementation note for subclasses</h4>
     * All getter methods which need unit information will invoke this {@code getUnit()} method.
     * Subclasses can override this method if they need to compute the unit dynamically.
     *
     * @return the unit of measure, or {@code null} if none.
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
     * <h4>Implementation note for subclasses</h4>
     * All getter methods will invoke this {@code getValue()} method.
     * Subclasses can override this method if they need to compute the value dynamically.
     *
     * @return the parameter value as an object, or {@code null} if no value has been set
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
     * @return the boolean value represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not a boolean type.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #setValue(boolean)
     * @see Parameters#booleanValue(ParameterDescriptor)
     */
    @Override
    public boolean booleanValue() throws IllegalStateException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
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
     * @return the numeric value represented by this parameter after conversion to type {@code int}.
     * @throws InvalidParameterTypeException if the value is not an integer type.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #setValue(int)
     * @see #intValueList()
     * @see Parameters#intValue(ParameterDescriptor)
     */
    @Override
    public int intValue() throws IllegalStateException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
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
     * or throws an exception otherwise. If the value can be cast, then the array is cloned before
     * to be returned.</p>
     *
     * @return a copy of the sequence of values represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not an array of {@code int}s.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #setValue(Object)
     * @see #intValue()
     * @see Parameters#intValueList(ParameterDescriptor)
     */
    @Override
    public int[] intValueList() throws IllegalStateException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
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
     * @return the numeric value represented by this parameter after conversion to type {@code double}.
     *         This method returns {@link Double#NaN} only if such "value" has been explicitly set.
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
        @SuppressWarnings("LocalVariableHidesMemberVariable")
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
     * or throws an exception otherwise. If the value can be cast, then the array is cloned before
     * to be returned.</p>
     *
     * @return a copy of the sequence of values represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not an array of {@code double}s.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getUnit()
     * @see #setValue(Object)
     * @see #doubleValue()
     */
    @Override
    public double[] doubleValueList() throws IllegalStateException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final T value = getValue();
        if (value instanceof double[]) {
            return ((double[]) value).clone();
        }
        throw missingOrIncompatibleValue(value);
    }

    /**
     * Returns the converter to be used by {@link #doubleValue(Unit)} and {@link #doubleValueList(Unit)}.
     */
    private UnitConverter getConverterTo(final Unit<?> target) {
        final Unit<?> source = getUnit();
        if (source == null) {
            throw new IllegalStateException(Resources.format(Resources.Keys.UnitlessParameter_1, Verifier.getDisplayName(descriptor)));
        }
        ArgumentChecks.ensureNonNull("unit", target);
        final short expectedID = Verifier.getUnitMessageID(source);
        if (Verifier.getUnitMessageID(target) != expectedID) {
            throw new IllegalArgumentException(Errors.format(expectedID, target));
        }
        try {
            return source.getConverterToAny(target);
        } catch (IncommensurableException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleUnits_2, source, target), e);
        }
    }

    /**
     * Returns the numeric value of this parameter in the given unit of measure.
     * This convenience method applies unit conversions on the fly as needed.
     *
     * <p>The default implementation invokes {@link #doubleValue()} and {@link #getUnit()},
     * then converts the values to the given unit of measurement.</p>
     *
     * @param  unit  the unit of measure for the value to be returned.
     * @return the numeric value represented by this parameter after conversion to type
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
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final double value = doubleValue();                 // Invoke first in case it throws an exception.
        return getConverterTo(unit).convert(value);
    }

    /**
     * Returns an ordered sequence of numeric values in the specified unit of measure.
     * This convenience method applies unit conversions on the fly as needed.
     *
     * <p>The default implementation invokes {@link #doubleValueList()} and {@link #getUnit()},
     * then converts the values to the given unit of measurement.</p>
     *
     * @param  unit  the unit of measure for the value to be returned.
     * @return the sequence of values represented by this parameter after conversion to type
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
     * @return the string value represented by this parameter.
     * @throws InvalidParameterTypeException if the value is not a string.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getValue()
     * @see #setValue(Object)
     * @see Parameters#stringValue(ParameterDescriptor)
     */
    @Override
    public String stringValue() throws IllegalStateException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final T value = getValue();
        if (value instanceof CharSequence) {
            return value.toString();
        }
        throw missingOrIncompatibleValue(value);
    }

    /**
     * Returns a reference to a file or a part of a file containing one or more parameter values.
     * The default implementation can convert the following value types:
     * {@link URI}, {@link URL}, {@link Path}, {@link File}.
     *
     * <h4>Relative paths to absolute paths</h4>
     * This parameter value is often a path relative to an unspecified directory. The base directory
     * depends on the context. For example, it may be a directory where all datum grids are cached.
     * Sometime, it is convenient to interpret the path as relative to the GML document or WKT file
     * that defined this parameter value. For such resolution, see {@link #getSourceFile()}.
     *
     * @return the reference to a file containing parameter values.
     * @throws InvalidParameterTypeException if the value is not a reference to a file or a URI.
     * @throws IllegalStateException if the value is not defined and there is no default value.
     *
     * @see #getValue()
     * @see #setValue(Object)
     */
    @Override
    public URI valueFile() throws IllegalStateException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final T value = getValue();
        if (value instanceof URI)  return   (URI) value;
        if (value instanceof File) return ((File) value).toURI();
        if (value instanceof Path) return ((Path) value).toUri();
        Exception cause = null;
        if (value instanceof URL) try {
            return ((URL) value).toURI();
        } catch (URISyntaxException exception) {
            cause = exception;
        }
        final String name = Verifier.getDisplayName(descriptor);
        if (value != null) {
            throw new InvalidParameterTypeException(getClassTypeError(), name);
        }
        throw new IllegalStateException(Resources.format(Resources.Keys.MissingValueForParameter_1, name), cause);
    }

    /**
     * Returns {@code true} if the given value is an instance of one of the types documented in {@link #valueFile()}.
     */
    private static boolean isFile(final Object value) {
        return (value instanceof URI || value instanceof URL || value instanceof File || value instanceof Path);
    }

    /**
     * Same as {@link #isFile(Object)}, but accepts also a {@link String} if the type specified
     * in the parameter descriptor is one of the types documented in {@link #valueFile()}.
     */
    private boolean isOrNeedFile(final Object newValue) {
        if (newValue instanceof String) {
            final Class<?> type = descriptor.getValueClass();
            return (type == URI.class) || (type == URL.class)
                   || Path.class.isAssignableFrom(type)
                   || File.class.isAssignableFrom(type);
        }
        return isFile(newValue);
    }

    /**
     * Sets the URI of the GML document or WKT file from which this parameter value has been read.
     * The given URI is a hint to be returned by {@link #getSourceFile()} for allowing callers to
     * {@linkplain URI#resolve(URI) resolve} relative {@linkplain #valueFile() value files}.
     *
     * @param document  URI of the document from which this parameter value has been read, or {@code null} if none.
     *
     * @see #getSourceFile()
     * @see org.apache.sis.io.wkt.WKTFormat#setSourceFile(URI)
     * @see URI#resolve(URI)
     *
     * @see 1.5
     */
    public void setSourceFile(final URI document) {
        sourceFile = document;
    }

    /**
     * Returns the exception to throw when an incompatible method is invoked for the value type.
     */
    private IllegalStateException missingOrIncompatibleValue(final Object newValue) {
        final String name = Verifier.getDisplayName(descriptor);
        if (newValue != null) {
            return new InvalidParameterTypeException(getClassTypeError(), name);
        }
        return new IllegalStateException(Resources.format(Resources.Keys.MissingValueForParameter_1, name));
    }

    /**
     * Formats an error message for illegal method call for the current value type.
     */
    private String getClassTypeError() {
        return Resources.format(Resources.Keys.IllegalOperationForValueClass_1,
                (descriptor != null) ? ((ParameterDescriptor<?>) descriptor).getValueClass() : "?");
    }

    /**
     * Converts the given number to the expected type, with a special case for conversion from float to double type.
     * Widening conversions are aimed to be exact in base 10 instead of base 2. If {@code expectedClass} is not a
     * {@link Number} subtype, then this method does nothing. If the cast would result in information lost, than
     * this method returns the given value unchanged for allowing a more accurate error message to happen later.
     *
     * @param  value          the value to cast (can be {@code null}).
     * @param  expectedClass  the desired class as a wrapper class (not a primitive type).
     * @return value converted to the desired class, or {@code value} if no cast is needed or can be done.
     */
    @SuppressWarnings("unchecked")
    private static Number cast(final Number value, final Class<?> expectedClass) {
        if (expectedClass == Double.class && value instanceof Float) {
            return DecimalFunctions.floatToDouble(value.floatValue());
        } else if (Number.class.isAssignableFrom(expectedClass)) {
            final Number n = Numbers.cast(value, (Class<? extends Number>) expectedClass);
            if (Numerics.equals(n.doubleValue(), value.doubleValue())) {
                return n;
            }
        }
        return value;
    }

    /**
     * Sets the parameter value as an object. The object type is typically (but not limited to) {@link Double},
     * {@code double[]}, {@link Integer}, {@code int[]}, {@link Boolean}, {@link String} or {@link URI}.
     * If the given value is {@code null}, then this parameter is set to the
     * {@linkplain DefaultParameterDescriptor#getDefaultValue() default value}.
     * If the given value is not an instance of the expected type, then this method may perform automatically
     * a type conversion (for example from {@link Float} to {@link Double} or from {@link Path} to {@link URI})
     * if such conversion can be done without information lost.
     *
     * @param  newValue  the parameter value, or {@code null} to restore the default.
     * @throws InvalidParameterValueException if the type of {@code value} is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example the value is numeric and out of range).
     *
     * @see #getValue()
     */
    @Override
    public void setValue(Object newValue) throws InvalidParameterValueException {
        /*
         * Try to convert the value only for a limited number of types. In particular we want to allow conversions
         * between java.io.File and java.nio.file.Path for easier transition between JDK6 and JDK7. We do not want
         * to allow too many conversions for reducing the risk of unexpected behavior.  If we fail to convert, try
         * to set the value anyway since the user may have redefined the `setValue(Object, Unit)` method.
         */
        if (newValue != null) {
            final Class<?> expectedClass = descriptor.getValueClass();
            if (!expectedClass.isInstance(newValue)) {
                if (newValue instanceof Number) {
                    newValue = cast((Number) newValue, expectedClass);
                } else if (isOrNeedFile(newValue)) try {
                    newValue = ObjectConverters.convert(newValue, expectedClass);
                } catch (UnconvertibleObjectException e) {
                    /*
                     * Level.FINE (not WARNING) because this log duplicates the exception
                     * that `setValue(Object, Unit)` may throw (with a better message).
                     */
                    Logging.recoverableException(LOGGER, DefaultParameterValue.class, "setValue", e);
                } else {
                    /*
                     * If the given value is an array, verify if array elements need to be converted
                     * for example from `float` to `double`. This is a "all or nothing" operation:
                     * if at least one element cannot be converted, then the whole array is unchanged.
                     */
                    Class<?> componentType = expectedClass.getComponentType();
convert:            if (componentType != null) {
                        final Object array = newValue.getClass().isArray() ? newValue : new Object[] {newValue};
                        final int length = Array.getLength(array);
                        if (length > 0) {
                            final Object copy = Array.newInstance(componentType, length);
                            componentType = Numbers.primitiveToWrapper(componentType);
                            for (int i=0; i<length; i++) {
                                Object element = Array.get(array, i);
                                if (element != null) {
                                    if (!(element instanceof Number)) break convert;
                                    element = cast((Number) element, componentType);
                                    if (!(componentType.isInstance(element))) break convert;
                                    Array.set(copy, i, element);
                                }
                            }
                            newValue = copy;
                        }
                    }
                }
            }
        }
        /*
         * Code below uses `unit` instead of `getUnit()` despite class Javadoc claim because units are not expected
         * to be involved in this method. We access this field only as a matter of principle, for making sure that no
         * property other than the value is altered by this method call.
         */
        setValue(newValue, unit);
    }

    /**
     * Sets the parameter value as a boolean.
     *
     * @param  newValue  the parameter value.
     * @throws InvalidParameterValueException if the boolean type is inappropriate for this parameter.
     *
     * @see #booleanValue()
     */
    @Override
    public void setValue(final boolean newValue) throws InvalidParameterValueException {
        setValue(newValue, unit);
        /*
         * Above code used `unit` instead of `getUnit()` despite class Javadoc claim because units are not expected
         * to be involved in this method. We access this field only as a matter of principle, for making sure that no
         * property other than the value is altered by this method call.
         */
    }

    /**
     * Sets the parameter value as an integer. This method automatically wraps the given integer
     * in an object of the type specified by the {@linkplain #getDescriptor() descriptor} if that
     * conversion can be done without information lost.
     *
     * @param  newValue  the parameter value.
     * @throws InvalidParameterValueException if the integer type is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example a value out of range).
     *
     * @see #intValue()
     */
    @Override
    public void setValue(final int newValue) throws InvalidParameterValueException {
        Number n = newValue;
        Number c = cast(n, descriptor.getValueClass());
        if (c.intValue() == newValue) n = c;
        setValue(n, unit);
        /*
         * Above code used `unit` instead of `getUnit()` despite class Javadoc claim because units are not expected
         * to be involved in this method. We access this field only as a matter of principle, for making sure that no
         * property other than the value is altered by this method call.
         */
    }

    /**
     * Wraps the given value in a type compatible with the expected value class, if possible.
     * If the value cannot be wrapped, then this method fallbacks on the {@link Double} class
     * consistently with this method being invoked only by {@code setValue(double, …)} methods.
     *
     * @throws IllegalArgumentException if the given value cannot be converted to the given type.
     */
    @SuppressWarnings("unchecked")
    private static Number wrap(final double value, final Class<?> valueClass) throws IllegalArgumentException {
        if (Number.class.isAssignableFrom(valueClass)) {
            return Numbers.wrap(value, (Class<? extends Number>) valueClass);
        } else {
            return value;
        }
    }

    /**
     * Sets the parameter value as a floating point. The unit, if any, stay unchanged. This method automatically
     * wraps the given number in an object of the type specified by the {@linkplain #getDescriptor() descriptor}.
     *
     * @param  newValue  the parameter value.
     * @throws InvalidParameterValueException if the floating point type is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example a value out of range).
     *
     * @see #setValue(double,Unit)
     * @see #doubleValue()
     */
    @Override
    public void setValue(final double newValue) throws InvalidParameterValueException {
        final Number n;
        try {
            n = wrap(newValue, descriptor.getValueClass());
        } catch (IllegalArgumentException e) {
            throw (InvalidParameterValueException) new InvalidParameterValueException(
                    e.getLocalizedMessage(), Verifier.getDisplayName(descriptor), newValue).initCause(e);
        }
        setValue(n, unit);
        /*
         * Above code used `unit` instead of `getUnit()` despite class Javadoc claim because units are not expected
         * to be involved in this method. We access this field only as a matter of principle, for making sure that
         * no property other than the value is altered by this method call.
         */
    }

    /**
     * Sets the parameter value as a floating point and its associated unit. This method automatically wraps
     * the given number in an object of the type specified by the {@linkplain #getDescriptor() descriptor}.
     *
     * @param  newValue  the parameter value.
     * @param  unit      the unit for the specified value.
     * @throws InvalidParameterValueException if the floating point type is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example a value out of range).
     *
     * @see #setValue(double)
     * @see #doubleValue(Unit)
     */
    @Override
    public void setValue(final double newValue, final Unit<?> unit) throws InvalidParameterValueException {
        final Number n;
        try {
            n = wrap(newValue, descriptor.getValueClass());
        } catch (IllegalArgumentException e) {
            throw (InvalidParameterValueException) new InvalidParameterValueException(
                    e.getLocalizedMessage(), Verifier.getDisplayName(descriptor), newValue).initCause(e);
        }
        setValue(n, unit);
    }

    /**
     * Sets the parameter value as an array of floating point and their associated unit.
     *
     * @param  newValues  the parameter values.
     * @param  unit       the unit for the specified value.
     * @throws InvalidParameterValueException if the floating point array type is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example a value out of range).
     */
    @Override
    public void setValue(final double[] newValues, final Unit<?> unit) throws InvalidParameterValueException {
        setValue((Object) newValues, unit);
    }

    /**
     * Sets the parameter value and its associated unit.
     * If the given value is {@code null}, then this parameter is set to the
     * {@linkplain DefaultParameterDescriptor#getDefaultValue() default value}.
     * Otherwise the given value shall be an instance of the class expected by the {@linkplain #getDescriptor() descriptor}.
     *
     * <ul>
     *   <li>This method does not perform any type conversion. Type conversion, if desired, should be
     *       applied by the public {@code setValue(…)} methods before to invoke this protected method.</li>
     *   <li>This method does not clone the given value. In particular, references to {@code int[]} and
     *       {@code double[]} arrays are stored <em>as-is</em>.</li>
     * </ul>
     *
     * <h4>Implementation note for subclasses</h4>
     * This method is invoked by all setter methods in this class, thus providing a single point that
     * subclasses can override if they want to perform more processing on the value before its storage,
     * or to be notified about value changes.
     *
     * @param  newValue  the parameter value, or {@code null} to restore the default.
     * @param  unit      the unit associated to the new parameter value, or {@code null}.
     * @throws InvalidParameterValueException if the type of {@code value} is inappropriate for this parameter,
     *         or if the value is illegal for some other reason (for example the value is numeric and out of range).
     *
     * @see #validate(Object)
     */
    @SuppressWarnings("unchecked")
    protected void setValue(final Object newValue, final Unit<?> unit) throws InvalidParameterValueException {
        final T convertedValue = Verifier.ensureValidValue(descriptor, newValue, unit);
        if (newValue != null) {
            validate(convertedValue);
            this.value = (T) newValue;              // Type has been verified by Verifier.ensureValidValue(…).
        } else {
            this.value = descriptor.getDefaultValue();
        }
        this.unit = unit;                           // Assign only on success.
    }

    /**
     * Invoked by {@link #setValue(Object, Unit)} after the basic verifications have been done and before
     * the value is stored. Subclasses can override this method for performing additional verifications.
     *
     * <h4>Unit of measurement</h4>
     * If the user specified a unit of measurement, then the value given to this method has been converted
     * to the unit specified by the {@linkplain #getDescriptor() descriptor}, for easier comparisons against
     * standardized values. This converted value may be different than the value to be stored in this
     * {@code ParameterValue}, since the latter value will be stored in the unit specified by the user.
     *
     * <h4>Standard validations</h4>
     * The checks for {@linkplain DefaultParameterDescriptor#getValueClass() value class},
     * for {@linkplain DefaultParameterDescriptor#getValueDomain() value domain} and for
     * {@linkplain DefaultParameterDescriptor#getValidValues() valid values} are performed
     * before this method is invoked. The default implementation of this method does nothing.
     *
     * @param  newValue  the value converted to the unit of measurement specified by the descriptor.
     * @throws InvalidParameterValueException if the given value is invalid for implementation-specific reasons.
     */
    protected void validate(final T newValue) throws InvalidParameterValueException {
    }

    /**
     * Compares the specified object with this parameter for equality.
     * The strictness level is controlled by the second argument.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal according the given comparison mode.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;            // Slight optimization
        }
        if (object != null) {
            if (mode == ComparisonMode.STRICT) {
                if (getClass() == object.getClass()) {
                    final DefaultParameterValue<?> that = (DefaultParameterValue<?>) object;
                    return Objects.equals    (descriptor, that.descriptor) &&
                           Objects.deepEquals(sourceFile, that.sourceFile) &&
                           Objects.deepEquals(value,      that.value) &&
                           Objects.equals    (unit,       that.unit);
                }
            } else if (object instanceof ParameterValue<?>) {
                final ParameterValue<?> that = (ParameterValue<?>) object;
                return Utilities.deepEquals(getDescriptor(), that.getDescriptor(), mode) &&
                       Utilities.deepEquals(getValue(),      that.getValue(),      mode) &&
                       Utilities.deepEquals(getUnit(),       that.getUnit(),       mode);
            }
        }
        return false;
    }

    /**
     * Compares the specified object with this parameter for equality.
     * This method is implemented as below:
     *
     * {@snippet lang="java" :
     *     return equals(other, ComparisonMode.STRICT);
     *     }
     *
     * Subclasses shall override {@link #equals(Object, ComparisonMode)} instead of this method.
     *
     * @param  object  the object to compare to {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        return equals(object, ComparisonMode.STRICT);
    }

    /**
     * Returns a hash value for this parameter.
     * This value does not need to be the same in past or future versions of this class.
     *
     * @return the hash code value.
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
            throw new AssertionError(exception);                // Should not happen, since we are cloneable
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
     * <h4>Instances sharing</h4>
     * If this method is invoked more than once with equal {@linkplain #getDescriptor() descriptor},
     * {@linkplain #getValue() value} and {@linkplain #getUnit() unit}, then this method will return
     * the same {@code DefaultParameterValue} instance on a <em>best effort</em> basis.
     * The rational for sharing is because the same parameter value is often used in many different coordinate operations.
     * For example, all <cite>Universal Transverse Mercator</cite> (UTM) projections use the same scale factor (0.9996)
     * and the same false easting (500000 metres).
     *
     * @param  <T>        the type of the value stored in the given parameter.
     * @param  parameter  the parameter to make unmodifiable, or {@code null}.
     * @return a unmodifiable implementation of the given parameter, or {@code null} if the given parameter was null.
     *
     * @see DefaultParameterValueGroup#unmodifiable(ParameterValueGroup)
     *
     * @since 0.6
     */
    public static <T> DefaultParameterValue<T> unmodifiable(final ParameterValue<T> parameter) {
        return UnmodifiableParameterValue.create(parameter);
    }

    /**
     * Formats this parameter as a <i>Well Known Text</i> {@code Parameter[…]} element.
     * Example:
     *
     * {@snippet lang="wkt" :
     *   Parameter["False easting", 0.0, LengthUnit["metre", 1]]
     *   }
     *
     * <h4>Unit of measurement</h4>
     * The units of measurement were never specified in WKT 1 format, and are optional in WKT 2 format.
     * If the units are not specified, then they are inferred from the context.
     * Typically, parameter values that are lengths are given in the unit for the projected CRS axes
     * while parameter values that are angles are given in the unit for the base geographic CRS.
     *
     * <h4>Example</h4>
     * The snippet below show WKT representations of the map projection parameters of a projected CRS
     * (most other elements are omitted). The map projection uses a <q>Latitude of natural origin</q>
     * parameters which is set to 52 <strong>grads</strong>, as defined in the {@code UNIT[…]} element of the
     * enclosing CRS. A similar rule applies to <q>False easting</q> and <q>False northing</q>
     * parameters, which are in kilometres in this example.
     *
     * <p><b>WKT 1:</b></p>
     * {@snippet lang="wkt" :
     *   PROJCS[…,
     *     GEOGCS[…,
     *       UNIT[“grad”, 0.015707963267948967]],       // Unit for all angles
     *     PROJECTION[“Lambert_Conformal_Conic_1SP”]
     *     PARAMETER[“latitude_of_origin”, 52.0],       // In grads
     *     PARAMETER[“scale_factor”, 0.99987742],
     *     PARAMETER[“false_easting”, 600.0],           // In kilometres
     *     PARAMETER[“false_northing”, 2200.0],         // In kilometres
     *     UNIT[“kilometre”, 1000]]                     // Unit for all lengths
     *   }
     *
     * <p><b>WKT 2:</b></p>
     * {@snippet lang="wkt" :
     *   ProjectedCRS[…
     *     BaseGeogCRS[…
     *       AngleUnit[“grad”, 0.015707963267948967]],
     *     Conversion[“Lambert zone II”,
     *       Method[“Lambert Conic Conformal (1SP)”],
     *       Parameter[“Latitude of natural origin”, 52.0],
     *       Parameter[“Scale factor at natural origin”, 0.99987742],
     *       Parameter[“False easting”, 600.0],
     *       Parameter[“False northing”, 2200.0]],
     *     CS[“Cartesian”, 2],
     *       LengthUnit[“kilometre”, 1000]]
     *   }
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "Parameter"} or {@code "ParameterFile"}.
     */
    @Override
    @SuppressWarnings("LocalVariableHidesMemberVariable")
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
                return WKTKeywords.Parameter;
            }
        }
        /*
         * In the WKT 1 specification, the unit of measurement is given by the context.
         * If this parameter value does not use the same unit, we will need to convert it.
         * Otherwise we can write the value as-is.
         *
         * Note that we take the descriptor unit as a starting point instead of this parameter unit
         * in order to give precedence to the descriptor units in Convention.WKT1_COMMON_UNITS mode.
         */
        Unit<?> contextualUnit;
        if (descriptor == null || (contextualUnit = descriptor.getUnit()) == null) {
            // Should be very rare (probably a buggy descriptor), but we try to be safe.
            contextualUnit = unit;
        }
        contextualUnit = formatter.toContextualUnit(contextualUnit);
        if (isWKT1) {
            unit = contextualUnit;
        } else if (convention != Convention.INTERNAL) {
            unit = WKTUtilities.toFormattable(unit);
        }
        double value;
        try {
            value = doubleValue(unit);
        } catch (IllegalStateException exception) {
            /*
             * May happen if a parameter is mandatory (e.g. "semi-major") but no value has been set for this parameter.
             * The name of the problematic parameter (needed for producing an error message) is given by the descriptor.
             * Null descriptor should be illegal but may happen after unmarshalling of invalid GML.
             * We make this WKT formatting robust since it is used by `toString()` implementation.
             */
            if (descriptor != null) {
                formatter.setInvalidWKT(descriptor, exception);
            } else {
                formatter.setInvalidWKT(DefaultParameterValue.class, exception);
            }
            value = Double.NaN;
        }
        formatter.append(value);
        /*
         * In the WKT 2 specification, the unit and the identifier are optional but recommended.
         * We follow that recommendation in strict WKT2 mode, but omit unit in non-strict modes.
         * Except if the parameter unit is not the same as the contextual unit, in which case it
         * is not possible to omit the unit (unless the value is the same in both units, like 0).
         */
        boolean ignoreUnits = false;
        if (convention.isSimplified()) {
            /*
             * The contextual units may be defined either in the direct parent, or in the parent of the parent,
             * depending if we are formatting WKT 1 or WKT 2 respectively. This is because WKT 2 wraps the
             * parameters in an additional `CONVERSION[…]` element which is not present in WKT 1.
             * Taking the example documented in the Javadoc of this method:
             *
             * - in WKT 1, `PROJCS[…]` is the immediate parent of `PARAMETER[…]`, but
             * - in WKT 2, `ProjectedCRS[…]` is the parent of `Conversion[…]`,
             *   which is the parent of `Parameter[…]`.
             */
            if (formatter.hasContextualUnit(isWKT1 ? 1 : 2) ||
                formatter.getEnclosingElement(1) instanceof MathTransform)
            {
                ignoreUnits = Double.isNaN(value)
                        ? unit.equals(contextualUnit)
                        : doubleValue(contextualUnit) == value;   // Equivalent to above line but more aggressive.
                /*
                 * ISO 19162:2019 became more restrictive than older standards about contextual units in parameters.
                 * For avoiding ambiguity, allow the omission only for decimal degrees and base units.
                 */
                if (ignoreUnits && !isWKT1) {
                    ignoreUnits = Units.isAngular(unit)
                            ? Units.DEGREE.equals(unit)
                            : Units.toStandardUnit(unit) == 1;
                }
            }
        }
        if (!ignoreUnits) {
            if (!isWKT1) {
                formatter.append(unit);
            } else if (descriptor != null) {
                formatter.setInvalidWKT(descriptor, null);
            } else {
                /*
                 * Null descriptor should be illegal but may happen after unmarshalling of invalid GML.
                 * We make this WKT formatting robust since it is used by `toString()` implementation.
                 */
                formatter.setInvalidWKT(DefaultParameterValue.class, null);
            }
        }
        // ID will be added by the Formatter itself.
        return WKTKeywords.Parameter;
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Default constructor for JAXB only. The descriptor is initialized to {@code null},
     * but will be assigned a value after XML unmarshalling.
     */
    private DefaultParameterValue() {
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     * May also be invoked by {@link DefaultParameterValueGroup} if the descriptor has been completed
     * with additional information provided in the {@code <gml:group>} element of a descriptor group.
     *
     * @see #getDescriptor()
     */
    final void setDescriptor(final ParameterDescriptor<T> descriptor) {
        this.descriptor = descriptor;
        if (descriptor instanceof DefaultParameterDescriptor<?>) {
            ((DefaultParameterDescriptor<?>) descriptor).setValueClass(this);
        }
        /*
         * A previous version was doing `assert descriptor.getValueClass().isInstance(value)`
         * where the value class was inferred by `DefaultParameterDescriptor()`. But it does
         * not always work, and the `NullPointerException` seems to be caught by JAXB.
         */
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
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Object value = getValue();                        // Give to user a chance to override.
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
        ExternalLinkHandler linkHandler = Context.linkHandler(Context.current());
        if (linkHandler != null) {
            sourceFile = linkHandler.getURI();
        }
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
            ImplementationHelper.propertyAlreadySet(DefaultParameterValue.class, "setXmlValue", "value");
        }
    }
}
