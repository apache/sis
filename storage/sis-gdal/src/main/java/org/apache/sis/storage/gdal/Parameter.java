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
package org.apache.sis.storage.gdal;

import java.net.URI;
import java.util.Set;
import java.util.Collection;
import javax.measure.Unit;
import javax.measure.IncommensurableException;

import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.InvalidParameterTypeException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.ReferenceIdentifier;

import org.apache.sis.measure.Units;


/**
 * A relatively "simple" implementation of a parameter value for {@code double} values.
 * In order to keep the model simpler, this parameter value is also its own descriptor.
 * This is not quite a recommended practice (such descriptors are less suitable for use in {@link java.util.HashMap}),
 * but allow us to keep the amount of classes smaller and closely related interfaces together.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class Parameter extends PJObject implements ParameterValue<Double>, ParameterDescriptor<Double>, Cloneable {
    /**
     * The parameter value.
     *
     * @see #doubleValue()
     * @see #setValue(double)
     */
    private double value;

    /**
     * Creates a new parameter with the given identifier and aliases.
     */
    Parameter(final ReferenceIdentifier identifier, final Collection<GenericName> aliases) {
        super(identifier, aliases);
    }

    /**
     * Creates a new parameter as a copy of the given one.
     */
    Parameter(final ParameterValue<?> param) {
        super(param.getDescriptor());
        value = param.doubleValue();
    }

    /**
     * Returns the descriptor of the parameter value, which is {@code this}.
     */
    @Override
    public ParameterDescriptor<Double> getDescriptor() {
        return this;
    }

    /**
     * Returns the minimum number of times that values for this parameter are required.
     * This method returns 1, meaning that a value shall alway be supplied
     * (the {@link #getValue()} method never return {@code null}).
     */
    @Override
    public int getMinimumOccurs() {
        return 1;
    }

    /**
     * Returns the maximum number of times that values for this parameter can be included.
     * This method unconditionally returns 1, which is the mandatory value for
     * {@link ParameterValue} implementations.
     */
    @Override
    public int getMaximumOccurs() {
        return 1;
    }

    /**
     * Unconditionally returns {@code Double.class}, which is the hard-coded type of values
     * in this parameter implementation.
     */
    @Override
    public Class<Double> getValueClass() {
        return Double.class;
    }

    /**
     * Returns {@code null}, since this simple class has no information about units of measurement.
     */
    @Override
    public Unit<?> getUnit() {
        return null;
    }

    /**
     * Returns {@code null}, since this simple class has no information about the
     * range of valid values.
     */
    @Override
    public Comparable<Double> getMinimumValue() {
        return null;
    }

    /**
     * Returns {@code null}, since this simple class has no information about the
     * range of valid values.
     */
    @Override
    public Comparable<Double> getMaximumValue() {
        return null;
    }

    /**
     * Returns {@code null}, since this simple class has no information about the
     * set of valid values.
     */
    @Override
    public Set<Double> getValidValues() {
        return null;
    }

    /**
     * Returns {@code null}, since this simple class has no information about the default value.
     */
    @Override
    public Double getDefaultValue() {
        return null;
    }

    /**
     * Returns the parameter {@linkplain #value} as an object.
     */
    @Override
    public Double getValue() {
        return value;
    }

    /**
     * Returns the numeric {@linkplain #value} represented by this parameter.
     */
    @Override
    public double doubleValue() {
        return value;
    }

    /**
     * Returns the standard unit used by Proj.4 for any value in the given unit.
     */
    private static Unit<?> getStandardUnit(final Unit<?> unit) {
        if (Units.isLinear(unit))  return Units.METRE;
        if (Units.isAngular(unit)) return Units.DEGREE;
        if (Units.isScale(unit))   return Units.UNITY;
        return null;
    }

    /**
     * Returns the numeric value of the operation parameter in the specified unit of measure.
     * This convenience method applies unit conversion from metres or decimal degrees as needed.
     *
     * @param  unit  the unit of measure for the value to be returned.
     * @return the numeric value represented by this parameter after conversion to {@code unit}.
     */
    @Override
    public double doubleValue(final Unit<?> unit) {
        double c = value;
        final Unit<?> standardUnit = getStandardUnit(unit);
        if (standardUnit != null) try {
            c = standardUnit.getConverterToAny(unit).convert(c);
        } catch (IncommensurableException e) {
            throw new IllegalArgumentException(e);              // Should never happen actually.
        }
        return c;
    }

    /**
     * Creates an exception for an invalid type.
     */
    private InvalidParameterTypeException invalidType(final String type) {
        return new InvalidParameterTypeException("Value " + value + " is not " + type + '.', name.getCode());
    }

    /**
     * Returns the value as an integer if it can be casted without information lost,
     * or throw an exception otherwise.
     */
    @Override
    public int intValue() throws InvalidParameterTypeException {
        final int r = (int) value;
        if (r == value) {
            return r;
        }
        throw invalidType("an integer");
    }

    /**
     * Unsupported operation, since this parameter is not a boolean.
     */
    @Override
    public boolean booleanValue() throws InvalidParameterTypeException {
        throw invalidType("a boolean");
    }

    /**
     * Unsupported operation, since this parameter is not a string.
     */
    @Override
    public String stringValue() throws InvalidParameterTypeException {
        throw invalidType("a string");
    }

    /**
     * Unsupported operation, since this parameter is not an array.
     */
    @Override
    public double[] doubleValueList(final Unit<?> unit) throws IllegalArgumentException, IllegalStateException {
        throw invalidType("an array");
    }

    /**
     * Unsupported operation, since this parameter is not an array.
     */
    @Override
    public double[] doubleValueList() throws InvalidParameterTypeException {
        throw invalidType("an array");
    }

    /**
     * Unsupported operation, since this parameter is not an array.
     */
    @Override
    public int[] intValueList() throws InvalidParameterTypeException {
        throw invalidType("an array");
    }

    /**
     * Unsupported operation, since this parameter is not a file.
     */
    @Override
    public URI valueFile() throws InvalidParameterTypeException {
        throw invalidType("a file");
    }

    /**
     * Sets the parameter to the given value, after conversion to metres or decimal degrees.
     */
    @Override
    public void setValue(double value, final Unit<?> unit) {
        final Unit<?> standardUnit = getStandardUnit(unit);
        if (standardUnit != null) try {
            value = unit.getConverterToAny(standardUnit).convert(value);
        } catch (IncommensurableException e) {
            throw new IllegalArgumentException(e);              // Should never happen actually.
        }
        this.value = value;
    }

    /**
     * Sets the parameter value as a floating point.
     */
    @Override
    public void setValue(final double value) throws InvalidParameterValueException {
        this.value = value;
    }

    /**
     * Creates an exception for an invalid value.
     */
    private InvalidParameterValueException invalidValue(final String type, final Object value) {
        return new InvalidParameterValueException("This parameter does not support " + type + '.', name.getCode(), value);
    }

    /**
     * Unsupported operation, since this parameter is not for arrays.
     */
    @Override
    public void setValue(final double[] values, final Unit<?> unit) throws InvalidParameterValueException {
        throw invalidValue("arrays", values);
    }

    /**
     * Sets the parameter value as an integer, converted to floating point.
     */
    @Override
    public void setValue(final int value) throws InvalidParameterValueException {
        this.value = value;
    }

    /**
     * Unsupported operation, since this parameter is not for booleans.
     */
    @Override
    public void setValue(final boolean value) throws InvalidParameterValueException {
        throw invalidValue("booleans", value);
    }

    /**
     * Unsupported operation, since this parameter is not for strings.
     */
    @Override
    public void setValue(final Object value) throws InvalidParameterValueException {
        throw invalidValue("strings", value);
    }

    /**
     * Returns a new parameter with the same {@linkplain #name} than this parameter.
     * The {@linkplain #value} is left to their default value.
     *
     * @see #clone()
     */
    @Override
    public Parameter createValue() {
        return new Parameter(name, aliases);
    }

    /**
     * Returns a copy of this parameter value. This method is similar to {@link #createValue()}
     * except that the {@linkplain #value} is initialized to the same value than the cloned parameter.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")          // Okay since this class is final.
    public Parameter clone() {
        return new Parameter(this);
    }

    /**
     * Returns the string representation of this parameter value.
     */
    @Override
    public String toString() {
        return super.toString() + " = " + value;
    }
}
