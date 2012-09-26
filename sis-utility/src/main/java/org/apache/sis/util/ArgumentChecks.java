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
package org.apache.sis.util;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;

import org.apache.sis.resources.Errors;


/**
 * Provides static methods for performing argument checks.
 * Every methods in this class can throw one of the following exceptions:
 * <p>
 * <table class="sis">
 * <tr><th>Exception</th><th class="sep">Thrown by</th></tr>
 * <tr><td>{@link NullArgumentException}</td>
 * <td class="sep">{@link #ensureNonNull(String, Object) ensureNonNull},
 * {@link #ensureNonEmpty(String, CharSequence) ensureNonEmpty}</td></tr>
 *
 * <tr><td>{@link IllegalArgumentException}</td>
 * <td class="sep">{@link #ensureNonEmpty(String, CharSequence) ensureNonEmpty},
 * {@link #ensurePositive(String, int) ensurePositive},
 * {@link #ensureStrictlyPositive(String, int) ensureStrictlyPositive},
 * {@link #ensureBetween(String, int, int, int) ensureBetween},
 * {@link #ensureCanCast(String, Class, Object) ensureCanCast}</td></tr>
 *
 * <tr><td>{@link IndexOutOfBoundsException}</td>
 * <td class="sep">{@link #ensureValidIndex(int, int) ensureValidIndex}</td></tr>
 *
 * <tr><td>{@link MismatchedDimensionException}</td>
 * <td class="sep">{@link #ensureDimensionMatches(String, DirectPosition, int) ensureDimensionMatches}</td></tr>
 * </table>
 *
 * {@section Method Arguments}
 * By convention, the value to check is always the last parameter given to every methods
 * in this class. The other parameters may include the programmatic name of the argument
 * being checked. This programmatic name is used for building an error message localized
 * in the {@linkplain java.util.Locale#getDefault() default locale} if the check failed.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.17)
 * @version 0.3
 * @module
 */
public final class ArgumentChecks extends Static {
    /**
     * Forbid object creation.
     */
    private ArgumentChecks() {
    }

    /**
     * Makes sure that an argument is non-null. If the given {@code object} is null, then a
     * {@link NullArgumentException} is thrown with a localized message containing the given name.
     *
     * @param  name The name of the argument to be checked. Used only in case an exception is thrown.
     * @param  object The user argument to check against null value.
     * @throws NullArgumentException if {@code object} is null.
     */
    public static void ensureNonNull(final String name, final Object object)
            throws NullArgumentException
    {
        if (object == null) {
            throw new NullArgumentException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
    }

    /**
     * Makes sure that an array element is non-null. If {@code array[index]} is null, then a
     * {@link NullArgumentException} is thrown with a localized message containing the given name.
     *
     * @param  name The name of the argument to be checked. Used only in case an exception is thrown.
     * @param  index Index of the element to check.
     * @param  array The user argument to check against null element.
     * @throws NullArgumentException if {@code array} or {@code array[index]} is null.
     */
    public static void ensureNonNull(final String name, final int index, final Object[] array)
            throws NullArgumentException
    {
        if (array == null) {
            throw new NullArgumentException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
        if (array[index] == null) {
            throw new NullArgumentException(Errors.format(
                    Errors.Keys.NullArgument_1, name + '[' + index + ']'));
        }
    }

    /**
     * Makes sure that a character sequence is non-null and non-empty. If the given {@code text} is
     * null, then a {@link NullArgumentException} is thrown. Otherwise if the given {@code text} has
     * a {@linkplain CharSequence#length() length} equals to 0, then an {@link IllegalArgumentException}
     * is thrown.
     *
     * @param  name The name of the argument to be checked. Used only in case an exception is thrown.
     * @param  text The user argument to check against null value and empty sequences.
     * @throws NullArgumentException if {@code text} is null.
     * @throws IllegalArgumentException if {@code text} is empty.
     */
    public static void ensureNonEmpty(final String name, final CharSequence text)
            throws NullArgumentException, IllegalArgumentException
    {
        if (text == null) {
            throw new NullArgumentException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
        if (text.length() == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, name));
        }
    }

    /**
     * Ensures that the specified value is null or an instance assignable to the given type.
     * If this method does not thrown an exception, then the value can be casted to the class
     * represented by {@code expectedType} without throwing a {@link ClassCastException}.
     *
     * @param  <T> The compile-time type of the value.
     * @param  name The name of the argument to be checked, used only if an exception is thrown.
     *         Can be {@code null} if the name is unknown.
     * @param  expectedType the expected type (class or interface).
     * @param  value The value to check, or {@code null}.
     * @throws IllegalArgumentException if {@code value} is non-null and is not assignable
     *         to the given type.
     */
    public static <T> void ensureCanCast(final String name, final Class<? extends T> expectedType, final T value)
            throws IllegalArgumentException
    {
        if (value != null) {
            final Class<?> valueClass = value.getClass();
            if (!expectedType.isAssignableFrom(valueClass)) {
                final int key;
                final Object[] args;
                if (name != null) {
                    key = Errors.Keys.IllegalArgumentClass_3;
                    args = new Object[] {name, valueClass, expectedType};
                } else {
                    key = Errors.Keys.IllegalClass_2;
                    args = new Object[] {valueClass, expectedType};
                }
                throw new IllegalArgumentException(Errors.format(key, args));
            }
        }
    }

    /**
     * Ensures that the given index is equals or greater than zero and lower than the given
     * upper value. This method is designed for methods that expect an index value as the only
     * argument. For this reason, this method does not take the argument name.
     *
     * @param  upper The maximal index value, exclusive.
     * @param  index The index to check.
     * @throws IndexOutOfBoundsException If the given index is negative or not lower than the
     *         given upper value.
     */
    public static void ensureValidIndex(final int upper, final int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= upper) {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
        }
    }

    /**
     * Ensures that the given integer value is greater than or equals to zero.
     * This method is used for checking values that are <strong>not</strong> index.
     * For checking index values, use {@link #ensureValidIndex(int, int)} instead.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public static void ensurePositive(final String name, final int value)
            throws IllegalArgumentException
    {
        if (value < 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.NegativeArgument_2, name, value));
        }
    }

    /**
     * Ensures that the given long value is greater than or equals to zero.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public static void ensurePositive(final String name, final long value)
            throws IllegalArgumentException
    {
        if (value < 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.NegativeArgument_2, name, value));
        }
    }

    /**
     * Ensures that the given floating point value is not
     * {@linkplain Float#NaN NaN} and is greater than or equals to zero. Note that
     * {@linkplain Float#POSITIVE_INFINITY positive infinity} is considered a valid value.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is {@linkplain Float#NaN NaN} or negative.
     */
    public static void ensurePositive(final String name, final float value)
            throws IllegalArgumentException
    {
        if (!(value >= 0)) { // Use '!' for catching NaN.
            throw new IllegalArgumentException(Float.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name) :
                    Errors.format(Errors.Keys.NegativeArgument_2, name, value));
        }
    }

    /**
     * Ensures that the given floating point value is not
     * {@linkplain Double#NaN NaN} and is greater than or equals to zero. Note that
     * {@linkplain Double#POSITIVE_INFINITY positive infinity} is considered a valid value.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is {@linkplain Double#NaN NaN} or negative.
     */
    public static void ensurePositive(final String name, final double value)
            throws IllegalArgumentException
    {
        if (!(value >= 0)) { // Use '!' for catching NaN.
            throw new IllegalArgumentException(Double.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name)  :
                    Errors.format(Errors.Keys.NegativeArgument_2, name, value));
        }
    }

    /**
     * Ensures that the given integer value is greater than zero.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is negative or equals to zero.
     */
    public static void ensureStrictlyPositive(final String name, final int value)
            throws IllegalArgumentException
    {
        if (value <= 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ValueNotGreaterThanZero_2, name, value));
        }
    }

    /**
     * Ensures that the given long value is greater than zero.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is negative or equals to zero.
     */
    public static void ensureStrictlyPositive(final String name, final long value)
            throws IllegalArgumentException
    {
        if (value <= 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ValueNotGreaterThanZero_2, name, value));
        }
    }

    /**
     * Ensures that the given floating point value is not
     * {@linkplain Float#NaN NaN} and is greater than zero. Note that
     * {@linkplain Float#POSITIVE_INFINITY positive infinity} is considered a valid value.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is {@linkplain Float#NaN NaN},
     *         zero or negative.
     */
    public static void ensureStrictlyPositive(final String name, final float value)
            throws IllegalArgumentException
    {
        if (!(value > 0)) { // Use '!' for catching NaN.
            throw new IllegalArgumentException(Float.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name) :
                    Errors.format(Errors.Keys.ValueNotGreaterThanZero_2, name, value));
        }
    }

    /**
     * Ensures that the given floating point value is not
     * {@linkplain Double#NaN NaN} and is greater than zero. Note that
     * {@linkplain Double#POSITIVE_INFINITY positive infinity} is considered a valid value.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is {@linkplain Double#NaN NaN},
     *         zero or negative.
     */
    public static void ensureStrictlyPositive(final String name, final double value)
            throws IllegalArgumentException
    {
        if (!(value > 0)) { // Use '!' for catching NaN.
            throw new IllegalArgumentException(Double.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name)  :
                    Errors.format(Errors.Keys.ValueNotGreaterThanZero_2, name, value));
        }
    }

    /**
     * Ensures that the given integer value is between the given bounds, inclusive.
     *
     * @param  name  The name of the argument to be checked. Used only in case an exception is thrown.
     * @param  min   The minimal value, inclusive.
     * @param  max   The maximal value, inclusive.
     * @param  value The value to be tested.
     * @throws IllegalArgumentException if the given value is not in the given range.
     */
    public static void ensureBetween(final String name, final int min, final int max, final int value)
            throws IllegalArgumentException
    {
        if (value < min || value > max) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ValueOutOfRange_4, name, value, min, max));
        }
    }

    /**
     * Ensures that the given long value is between the given bounds, inclusive.
     *
     * @param  name  The name of the argument to be checked. Used only in case an exception is thrown.
     * @param  min   The minimal value, inclusive.
     * @param  max   The maximal value, inclusive.
     * @param  value The value to be tested.
     * @throws IllegalArgumentException if the given value is not in the given range.
     */
    public static void ensureBetween(final String name, final long min, final long max, final long value)
            throws IllegalArgumentException
    {
        if (value < min || value > max) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ValueOutOfRange_4, name, value, min, max));
        }
    }

    /**
     * Ensures that the given floating point value is between the given bounds, inclusive.
     *
     * @param  name  The name of the argument to be checked. Used only in case an exception is thrown.
     * @param  min   The minimal value, inclusive.
     * @param  max   The maximal value, inclusive.
     * @param  value The value to be tested.
     * @throws IllegalArgumentException if the given value is {@linkplain Float#NaN NaN}
     *         or not in the given range.
     */
    public static void ensureBetween(final String name, final float min, final float max, final float value)
            throws IllegalArgumentException
    {
        if (!(value >= min && value <= max)) { // Use '!' for catching NaN.
            throw new IllegalArgumentException(Float.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name) :
                    Errors.format(Errors.Keys.ValueOutOfRange_4, name, value, min, max));
        }
    }

    /**
     * Ensures that the given floating point value is between the given bounds, inclusive.
     *
     * @param  name  The name of the argument to be checked. Used only in case an exception is thrown.
     * @param  min   The minimal value, inclusive.
     * @param  max   The maximal value, inclusive.
     * @param  value The value to be tested.
     * @throws IllegalArgumentException if the given value is {@linkplain Float#NaN NaN}
     *         or not in the given range.
     */
    public static void ensureBetween(final String name, final double min, final double max, final double value)
            throws IllegalArgumentException
    {
        if (!(value >= min && value <= max)) { // Use '!' for catching NaN.
            throw new IllegalArgumentException(Double.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name)  :
                    Errors.format(Errors.Keys.ValueOutOfRange_4, name, value, min, max));
        }
    }

    /**
     * Ensures that the given direct position has the expected number of dimensions.
     * This method does nothing if the given direct position is null.
     *
     * @param  name     The name of the argument to be checked. Used only in case an exception is thrown.
     * @param  position The direct position to check for its dimension.
     * @param  expected The expected number of dimensions.
     * @throws MismatchedDimensionException If the given direct position is non-null and does
     *         not have the expected number of dimensions.
     */
    public static void ensureDimensionMatches(final String name, final DirectPosition position, final int expected)
            throws MismatchedDimensionException
    {
        if (position != null) {
            final int dimension = position.getDimension();
            if (dimension != expected) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.UnexpectedArgumentDimension_3, name, dimension, expected));
            }
        }
    }
}
