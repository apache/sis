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

import java.util.Map; // For javadoc
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;

import org.apache.sis.util.resources.Errors;


/**
 * Static methods for performing argument checks.
 * Every methods in this class can throw one of the following exceptions:
 *
 * <table class="sis">
 * <caption>Exceptions thrown on illegal argument</caption>
 * <tr>
 *   <th>Exception</th>
 *   <th class="sep">Thrown by</th>
 * </tr><tr>
 *   <td>{@link NullArgumentException}</td>
 *   <td class="sep">
 *     {@link #ensureNonNull(String, Object) ensureNonNull},
 *     {@link #ensureNonEmpty(String, CharSequence) ensureNonEmpty}.
 *   </td>
 * </tr><tr>
 *   <td>{@link IllegalArgumentException}</td>
 *   <td class="sep">
 *     {@link #ensureNonEmpty(String, CharSequence) ensureNonEmpty},
 *     {@link #ensurePositive(String, int) ensurePositive},
 *     {@link #ensureStrictlyPositive(String, int) ensureStrictlyPositive},
 *     {@link #ensureBetween(String, int, int, int) ensureBetween},
 *     {@link #ensureSizeBetween(String, int, int, int) ensureBetween},
 *     {@link #ensureCanCast(String, Class, Object) ensureCanCast}.
 *   </td>
 * </tr><tr>
 *   <td>{@link IndexOutOfBoundsException}</td>
 *   <td class="sep">
 *     {@link #ensureValidIndex(int, int) ensureValidIndex}.
 *   </td>
 * </tr><tr>
 *   <td>{@link MismatchedDimensionException}</td>
 *   <td class="sep">
 *     {@link #ensureDimensionMatches(String, int, DirectPosition) ensureDimensionMatches}.
 *   </td>
 * </tr>
 * </table>
 *
 * More specialized {@code ensureXXX(…)} methods are provided in the following classes:
 * <ul>
 *   <li>{@link org.apache.sis.measure.Units}:
 *       {@link org.apache.sis.measure.Units#ensureAngular  ensureAngular},
 *       {@link org.apache.sis.measure.Units#ensureLinear   ensureLinear},
 *       {@link org.apache.sis.measure.Units#ensureTemporal ensureTemporal},
 *       {@link org.apache.sis.measure.Units#ensureScale    ensureScale}.</li>
 * </ul>
 *
 * <div class="section">Method Arguments</div>
 * By convention, the value to check is always the last parameter given to every methods
 * in this class. The other parameters may include the programmatic name of the argument
 * being checked. This programmatic name is used for building an error message localized
 * in the {@linkplain java.util.Locale#getDefault() default locale} if the check failed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
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
     * @param  name The name of the argument to be checked. Used only if an exception is thrown.
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
     * Makes sure that an array element is non-null. If {@code element} is null, then a
     * {@link NullArgumentException} is thrown with a localized message containing the
     * given name and index. The name and index are formatted as below:
     *
     * <ul>
     *   <li>If the {@code name} contains the {@code "[#]"} sequence of characters, then the {@code '#'} character
     *       is replaced by the {@code index} value. For example if {@code name} is {@code "axes[#].unit"} and the
     *       index is 2, then the formatted message will contain {@code "axes[2].unit"}.</li>
     *   <li>If the {@code name} does not contain the {@code "[#]"} sequence of characters, then {@code index} value
     *       is appended between square brackets. For example if {@code name} is {@code "axes"} and the index is 2,
     *       then the formatted message will contain {@code "axes[2]"}.</li>
     * </ul>
     *
     * @param  name    The name of the argument to be checked. Used only if an exception is thrown.
     * @param  index   The Index of the element to check in an array or a list. Used only if an exception is thrown.
     * @param  element The array or list element to check against null value.
     * @throws NullArgumentException if {@code element} is null.
     */
    public static void ensureNonNullElement(final String name, final int index, final Object element)
            throws NullArgumentException
    {
        if (element == null) {
            final StringBuilder buffer = new StringBuilder(name);
            final int s = name.indexOf("[#]");
            if (s >= 0) {
                buffer.setLength(s + 1);
                buffer.append(index).append(name, s + 2, name.length());
            } else {
                buffer.append('[').append(index).append(']');
            }
            throw new NullArgumentException(Errors.format(Errors.Keys.NullArgument_1, buffer.toString()));
        }
    }

    /**
     * Makes sure that a character sequence is non-null and non-empty. If the given {@code text} is
     * null, then a {@link NullArgumentException} is thrown. Otherwise if the given {@code text} has
     * a {@linkplain CharSequence#length() length} equals to 0, then an {@link IllegalArgumentException}
     * is thrown.
     *
     * @param  name The name of the argument to be checked. Used only if an exception is thrown.
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
     * @param  name The name of the argument to be checked, used only if an exception is thrown.
     *         Can be {@code null} if the name is unknown.
     * @param  expectedType the expected type (class or interface).
     * @param  value The value to check, or {@code null}.
     * @throws IllegalArgumentException if {@code value} is non-null and is not assignable to the given type.
     *
     * @see org.apache.sis.util.collection.Containers#property(Map, Object, Class)
     */
    public static void ensureCanCast(final String name, final Class<?> expectedType, final Object value)
            throws IllegalArgumentException
    {
        if (value != null) {
            final Class<?> valueClass = value.getClass();
            if (!expectedType.isAssignableFrom(valueClass)) {
                final short key;
                final Object[] args;
                if (name != null) {
                    key = Errors.Keys.IllegalArgumentClass_3;
                    args = new Object[] {name, expectedType, valueClass};
                } else {
                    key = Errors.Keys.IllegalClass_2;
                    args = new Object[] {expectedType, valueClass};
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
     *
     * @see #ensurePositive(String, int)
     */
    public static void ensureValidIndex(final int upper, final int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= upper) {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
        }
    }

    /**
     * Ensures that the given index range is valid for a sequence of the given length.
     * This method is designed for methods that expect an index range as their only arguments.
     * For this reason, this method does not take argument names.
     *
     * <p>This method verifies only the {@code lower} and {@code upper} argument values.
     * It does not <strong>not</strong> verify the validity of the {@code length} argument,
     * because this information is assumed to be provided by the implementation rather than
     * the user.</p>
     *
     * @param  length The length of the sequence (array, {@link CharSequence}, <i>etc.</i>).
     * @param  lower  The user-specified lower index, inclusive.
     * @param  upper  The user-specified upper index, exclusive.
     * @throws IndexOutOfBoundsException If the given [{@code lower} … {@code upper}]
     *         range is out of the sequence index range.
     *
     * @see #ensureSizeBetween(String, int, int, int)
     */
    public static void ensureValidIndexRange(final int length, final int lower, final int upper) throws IndexOutOfBoundsException {
        if (lower < 0 || upper < lower || upper > length) {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IllegalRange_2, lower, upper));
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
     *
     * @see #ensureValidIndex(int, int)
     * @see #ensureStrictlyPositive(String, int)
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
     *
     * @see #ensureStrictlyPositive(String, long)
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
     * {@linkplain Float#isNaN(float) NaN} and is greater than or equals to zero. Note that
     * {@linkplain Float#POSITIVE_INFINITY positive infinity} is considered a valid value.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or negative.
     *
     * @see #ensureStrictlyPositive(String, float)
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
     * {@linkplain Double#isNaN(double) NaN} and is greater than or equals to zero. Note that
     * {@linkplain Double#POSITIVE_INFINITY positive infinity} is considered a valid value.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or negative.
     *
     * @see #ensureStrictlyPositive(String, double)
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
     *
     * @see #ensurePositive(String, int)
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
     *
     * @see #ensurePositive(String, long)
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
     * {@linkplain Float#isNaN(float) NaN} and is greater than zero. Note that
     * {@linkplain Float#POSITIVE_INFINITY positive infinity} is considered a valid value.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     *
     * @see #ensurePositive(String, float)
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
     * {@linkplain Double#isNaN(double) NaN} and is greater than zero. Note that
     * {@linkplain Double#POSITIVE_INFINITY positive infinity} is considered a valid value.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     *
     * @see #ensurePositive(String, double)
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
     * Ensures that the given floating point value is not
     * {@linkplain Float#isNaN(float) NaN} neither {@linkplain Float#isInfinite(float)}.
     * The value can be negative, zero or positive.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or infinite.
     */
    public static void ensureFinite(final String name, final float value) {
        final boolean isNaN;
        if ((isNaN = Float.isNaN(value)) == true || Float.isInfinite(value)) {
            throw new IllegalArgumentException(Errors.format(isNaN ?
                    Errors.Keys.NotANumber_1 : Errors.Keys.InfiniteArgumentValue_1, name));
        }
    }

    /**
     * Ensures that the given floating point value is not
     * {@linkplain Double#isNaN(double) NaN} neither {@linkplain Double#isInfinite(double)}.
     * The value can be negative, zero or positive.
     *
     * @param  name   The name of the argument to be checked, used only if an exception is thrown.
     * @param  value  The user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or infinite.
     */
    public static void ensureFinite(final String name, final double value) {
        final boolean isNaN;
        if ((isNaN = Double.isNaN(value)) == true || Double.isInfinite(value)) {
            throw new IllegalArgumentException(Errors.format(isNaN ?
                    Errors.Keys.NotANumber_1 : Errors.Keys.InfiniteArgumentValue_1, name));
        }
    }

    /**
     * Ensures that the given integer value is between the given bounds, inclusive.
     * This is a general-purpose method for checking integer arguments.
     * Note that the following specialized methods are provided for common kinds
     * of integer range checks:
     *
     * <ul>
     *   <li>{@link #ensureSizeBetween(String, int, int, int) ensureSizeBetween(…)}
     *       if the {@code value} argument is a collection size or an array length.</li>
     *   <li>{@link #ensureValidIndex(int, int) ensureValidIndex(…)} if the {@code value}
     *       argument is an index in a list or an array.</li>
     * </ul>
     *
     * @param  name  The name of the argument to be checked. Used only if an exception is thrown.
     * @param  min   The minimal value, inclusive.
     * @param  max   The maximal value, inclusive.
     * @param  value The user argument to check.
     * @throws IllegalArgumentException if the given value is not in the given range.
     *
     * @see #ensureSizeBetween(String, int, int, int)
     * @see #ensureValidIndex(int, int)
     * @see #ensureValidIndexRange(int, int, int)
     */
    public static void ensureBetween(final String name, final int min, final int max, final int value)
            throws IllegalArgumentException
    {
        if (value < min || value > max) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ValueOutOfRange_4, name, min, max, value));
        }
    }

    /**
     * Ensures that the given long value is between the given bounds, inclusive.
     *
     * @param  name  The name of the argument to be checked. Used only if an exception is thrown.
     * @param  min   The minimal value, inclusive.
     * @param  max   The maximal value, inclusive.
     * @param  value The user argument to check.
     * @throws IllegalArgumentException if the given value is not in the given range.
     */
    public static void ensureBetween(final String name, final long min, final long max, final long value)
            throws IllegalArgumentException
    {
        if (value < min || value > max) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ValueOutOfRange_4, name, min, max, value));
        }
    }

    /**
     * Ensures that the given floating point value is between the given bounds, inclusive.
     *
     * @param  name  The name of the argument to be checked. Used only if an exception is thrown.
     * @param  min   The minimal value, inclusive.
     * @param  max   The maximal value, inclusive.
     * @param  value The user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or not in the given range.
     */
    public static void ensureBetween(final String name, final float min, final float max, final float value)
            throws IllegalArgumentException
    {
        if (!(value >= min && value <= max)) { // Use '!' for catching NaN.
            throw new IllegalArgumentException(Float.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name) :
                    Errors.format(Errors.Keys.ValueOutOfRange_4, name, min, max, value));
        }
    }

    /**
     * Ensures that the given floating point value is between the given bounds, inclusive.
     *
     * @param  name  The name of the argument to be checked. Used only if an exception is thrown.
     * @param  min   The minimal value, inclusive.
     * @param  max   The maximal value, inclusive.
     * @param  value The user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or not in the given range.
     */
    public static void ensureBetween(final String name, final double min, final double max, final double value)
            throws IllegalArgumentException
    {
        if (!(value >= min && value <= max)) { // Use '!' for catching NaN.
            throw new IllegalArgumentException(Double.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name)  :
                    Errors.format(Errors.Keys.ValueOutOfRange_4, name, min, max, value));
        }
    }

    /**
     * Ensures that the given collection size of array length is between the given bounds, inclusive.
     * This method performs the same check than {@link #ensureBetween(String, int, int, int)
     * ensureBetween(…)}, but the error message is different in case of failure.
     *
     * @param  name  The name of the argument to be checked. Used only if an exception is thrown.
     * @param  min   The minimal size (inclusive), or 0 if none.
     * @param  max   The maximal size (inclusive), or {@link Integer#MAX_VALUE} if none.
     * @param  size  The user collection size or array length to be checked.
     * @throws IllegalArgumentException if the given value is not in the given range.
     *
     * @see #ensureBetween(String, int, int, int)
     * @see #ensureValidIndexRange(int, int, int)
     */
    public static void ensureSizeBetween(final String name, final int min, final int max, final int size)
            throws IllegalArgumentException
    {
        final String message;
        if (size < min) {
            if (min == 1) {
                message = Errors.format(Errors.Keys.EmptyArgument_1, name);
            } else {
                message = Errors.format(Errors.Keys.InsufficientArgumentSize_3, name, min, size);
            }
        } else if (size > max) {
            message = Errors.format(Errors.Keys.ExcessiveArgumentSize_3, name, max, size);
        } else {
            return;
        }
        throw new IllegalArgumentException(message);
    }

    /**
     * Ensures that the given integer is a valid Unicode code point. The range of valid code points goes
     * from {@link Character#MIN_CODE_POINT U+0000} to {@link Character#MAX_CODE_POINT U+10FFFF} inclusive.
     *
     * @param  name The name of the argument to be checked. Used only if an exception is thrown.
     * @param  code The Unicode code point to verify.
     * @throws IllegalArgumentException if the given value is not a valid Unicode code point.
     *
     * @since 0.4
     */
    public static void ensureValidUnicodeCodePoint(final String name, final int code) throws IllegalArgumentException {
        if (!Character.isValidCodePoint(code)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalUnicodeCodePoint_2, name,
                    (code < Character.MIN_CODE_POINT) ? code : "U+" + Integer.toHexString(code).toUpperCase()));
        }
    }

    /**
     * Ensures that the given CRS, if non-null, has the expected number of dimensions.
     * This method does nothing if the given coordinate reference system is null.
     *
     * @param  name     The name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected The expected number of dimensions.
     * @param  crs      The coordinate reference system to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException if the given coordinate reference system is non-null
     *         and does not have the expected number of dimensions.
     */
    public static void ensureDimensionMatches(final String name, final int expected,
            final CoordinateReferenceSystem crs) throws MismatchedDimensionException
    {
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs != null) { // Should never be null, but let be safe.
                final int dimension = cs.getDimension();
                if (dimension != expected) {
                    throw new MismatchedDimensionException(Errors.format(
                            Errors.Keys.MismatchedDimension_3, name, expected, dimension));
                }
            }
        }
    }

    /**
     * Ensures that the given coordinate system, if non-null, has the expected number of dimensions.
     * This method does nothing if the given coordinate system is null.
     *
     * @param  name     The name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected The expected number of dimensions.
     * @param  cs       The coordinate system to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException if the given coordinate system is non-null
     *         and does not have the expected number of dimensions.
     *
     * @since 0.6
     */
    public static void ensureDimensionMatches(final String name, final int expected,
            final CoordinateSystem cs) throws MismatchedDimensionException
    {
        if (cs != null) {
            final int dimension = cs.getDimension();
            if (dimension != expected) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, name, expected, dimension));
            }
        }
    }

    /**
     * Ensures that the given vector, if non-null, has the expected number of dimensions
     * (taken as its length). This method does nothing if the given vector is null.
     *
     * @param  name     The name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected The expected number of dimensions.
     * @param  vector   The vector to check for its number of dimensions, or {@code null}.
     * @throws MismatchedDimensionException If the given vector is non-null and does not have the
     *         expected number of dimensions (taken as its length).
     */
    public static void ensureDimensionMatches(final String name, final int expected, final double[] vector)
            throws MismatchedDimensionException
    {
        if (vector != null) {
            final int dimension = vector.length;
            if (dimension != expected) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, name, expected, dimension));
            }
        }
    }

    /**
     * Ensures that the given direct position, if non-null, has the expected number of dimensions.
     * This method does nothing if the given direct position is null.
     *
     * @param  name     The name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected The expected number of dimensions.
     * @param  position The direct position to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException If the given direct position is non-null and does
     *         not have the expected number of dimensions.
     */
    public static void ensureDimensionMatches(final String name, final int expected, final DirectPosition position)
            throws MismatchedDimensionException
    {
        if (position != null) {
            final int dimension = position.getDimension();
            if (dimension != expected) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, name, expected, dimension));
            }
        }
    }

    /**
     * Ensures that the given envelope, if non-null, has the expected number of dimensions.
     * This method does nothing if the given envelope is null.
     *
     * @param  name     The name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected The expected number of dimensions.
     * @param  envelope The envelope to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException If the given envelope is non-null and does
     *         not have the expected number of dimensions.
     */
    public static void ensureDimensionMatches(final String name, final int expected, final Envelope envelope)
            throws MismatchedDimensionException
    {
        if (envelope != null) {
            final int dimension = envelope.getDimension();
            if (dimension != expected) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, name, expected, dimension));
            }
        }
    }
}
