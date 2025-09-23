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

import java.util.Map;
import java.util.BitSet;
import java.util.Collection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coverage.grid.GridEnvelope;


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
 *   <td>{@link NullPointerException}</td>
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
 *     {@link #ensureCountBetween(String, boolean, int, int, int) ensureCountBetween},
 *     {@link #ensureNonEmptyBounded(String, boolean, int, int, int[]) ensureNonEmptyBounded},
 *     {@link #ensureCanCast(String, Class, Object) ensureCanCast}.
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
 * <h2>Method Arguments</h2>
 * By convention, the value to check is always the last parameter given to every methods in this class.
 * The other parameters may include the programmatic name of the argument being checked.
 * This programmatic name is used for building an error message localized in the
 * {@linkplain java.util.Locale#getDefault() default locale} if the check failed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 * @since   0.3
 */
public final class ArgumentChecks extends Static {
    /**
     * Forbid object creation.
     */
    private ArgumentChecks() {
    }

    /**
     * Makes sure that an argument is non-null. If the given {@code object} is null, then a
     * {@link NullPointerException} is thrown with a localized message containing the given name.
     * This method differs from {@link java.util.Objects#requireNonNull(Object, String)} in that
     * the {@code String} argument is only the parameter name, not the full exception message.
     *
     * <h4>Suggestions about when to use</h4>
     * This method is helpful for validating arguments in a method receiving many arguments,
     * when there is a potential ambiguity about which argument is {@code null}.
     * This method is also useful when the validation is done in a private method invoked by a public method,
     * because it is no longer obvious that a {@link NullPointerException} is caused by a null argument given
     * to the public method.
     *
     * <h4>Suggestions about when to not use</h4>
     * When there is no ambiguity, for example in methods receiving a single argument, the standard
     * {@link java.util.Objects#requireNonNull(Object)} method should be preferred as it is more efficient.
     * Another situation where to not use this method is when an implicit null check would occur early
     * because the argument is used immediately. Since Java 14, the exception thrown by implicit null
     * checks contains sufficiently <a href="https://openjdk.org/jeps/358">helpful message</a>.
     *
     * @param  name    the name of the argument to be checked. Used only if an exception is thrown.
     * @param  object  the user argument to check against null value.
     * @throws NullPointerException if {@code object} is null.
     *
     * @see java.util.Objects#requireNonNull(Object)
     * @see java.util.Objects#requireNonNull(Object, String)
     */
    public static void ensureNonNull(final String name, final Object object) throws NullPointerException {
        if (object == null) {
            throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
    }

    /**
     * Makes sure that an array element is non-null. If {@code element} is null, then a
     * {@link NullPointerException} is thrown with a localized message containing the
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
     * @param  name     the name of the argument to be checked. Used only if an exception is thrown.
     * @param  index    the Index of the element to check in an array or a list. Used only if an exception is thrown.
     * @param  element  the array or list element to check against null value.
     * @throws NullPointerException if {@code element} is null.
     */
    public static void ensureNonNullElement(final String name, final int index, final Object element)
            throws NullPointerException
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
            throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, buffer.toString()));
        }
    }

    /**
     * Makes sure that a character sequence is non-null and non-empty. If the given {@code text} is
     * null, then a {@link NullPointerException} is thrown. Otherwise if the given {@code text} has
     * a {@linkplain CharSequence#length() length} equals to 0, then an {@link IllegalArgumentException}
     * is thrown.
     *
     * @param  name  the name of the argument to be checked. Used only if an exception is thrown.
     * @param  text  the user argument to check against null value and empty sequences.
     * @throws NullPointerException if {@code text} is null.
     * @throws IllegalArgumentException if {@code text} is empty.
     */
    public static void ensureNonEmpty(final String name, final CharSequence text)
            throws NullPointerException, IllegalArgumentException
    {
        if (text == null) {
            throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
        if (text.length() == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, name));
        }
    }

    /**
     * Makes sure that an array is non-null and non-empty. If the given {@code array} is null,
     * then a {@link NullPointerException} is thrown. Otherwise if the array length is equal
     * to 0, then an {@link IllegalArgumentException} is thrown.
     *
     * @param  name   the name of the argument to be checked. Used only if an exception is thrown.
     * @param  array  the user argument to check against null value and empty array.
     * @throws NullPointerException if {@code array} is null.
     * @throws IllegalArgumentException if {@code array} is empty.
     *
     * @since 1.0
     */
    public static void ensureNonEmpty(final String name, final Object[] array)
            throws NullPointerException, IllegalArgumentException
    {
        if (array == null) {
            throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
        if (array.length == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, name));
        }
    }

    /**
     * Makes sure that given collection is non-null and non-empty.
     * If it is null, then a {@link NullPointerException} is thrown.
     * Otherwise if it {@linkplain Collection#isEmpty() is empty}, then an {@link IllegalArgumentException} is thrown.
     *
     * @param  name     the name of the argument to be checked. Used only if an exception is thrown.
     * @param  toCheck  the user argument to check against null value and empty collection.
     * @throws NullPointerException if {@code toCheck} is null.
     * @throws IllegalArgumentException if {@code toCheck} is empty.
     *
     * @since 1.1
     */
    public static void ensureNonEmpty(final String name, final Collection<?> toCheck) {
        if (toCheck == null) {
            throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
        if (toCheck.isEmpty()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, name));
        }
    }

    /**
     * Makes sure that given object is non-null and non-empty.
     * If it is null, then a {@link NullPointerException} is thrown.
     * Otherwise if it {@linkplain Emptiable#isEmpty() is empty}, then an {@link IllegalArgumentException} is thrown.
     *
     * @param  name     the name of the argument to be checked. Used only if an exception is thrown.
     * @param  toCheck  the user argument to check against null value and empty object.
     * @throws NullPointerException if {@code toCheck} is null.
     * @throws IllegalArgumentException if {@code toCheck} is empty.
     *
     * @since 1.4
     */
    public static void ensureNonEmpty(final String name, final Emptiable toCheck) {
        if (toCheck == null) {
            throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
        if (toCheck.isEmpty()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, name));
        }
    }

    /**
     * Ensures that the given {@code values} array contains at least one element and that all elements are within bounds.
     * The minimum and maximum values are inclusive. Optionaly, this method can also ensure that all values are distinct.
     *
     * <p>Note that a successful call to {@code ensureNonEmptyBounded(name, true, 0, max, values)}
     * implies 1 ≤ {@code values.length} ≤ {@code max}.</p>
     *
     * @param  name      the name of the argument to be checked. Used only if an exception is thrown.
     * @param  distinct  {@code true} if each value must be unique.
     * @param  min       the minimal allowed value (inclusive), or {@link Integer#MIN_VALUE} if none.
     * @param  max       the maximal allowed value (inclusive), or {@link Integer#MAX_VALUE} if none.
     * @param  values    integer values to validate.
     * @throws NullPointerException if {@code values} is null.
     * @throws IllegalArgumentException if {@code values} is empty, contains a value lower than {@code min},
     *         contains a value greater than {@code max}, or contains duplicated values while {@code distinct} is {@code true}.
     *
     * @since 1.4
     */
    public static void ensureNonEmptyBounded(final String name, final boolean distinct, final int min, final int max, final int[] values)
            throws IllegalArgumentException
    {
        if (values == null) {
            throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, name));
        }
        if (values.length == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, name));
        }
        long found = 0;                             // Cheap way to check for duplication when (max - min) ≤ 64.
        BitSet more = null;                         // Used only if above cheap way is not sufficient.
        for (int i=0; i<values.length; i++) {
            final int index = values[i];
            if (index < min || index > max) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.ValueOutOfRange_4, Strings.toIndexed(name, i), min, max, index));
            }
            if (distinct) {
                int flag = index - min;
                if (flag <= Long.SIZE) {
                    if (found != (found |= 1L << flag)) {
                        continue;                               // No collision for current index.
                    }
                } else {
                    flag -= Long.SIZE;
                    if (more == null) {
                        more = new BitSet();
                    }
                    if (!more.get(flag)) {
                        more.set(flag);
                        continue;                               // No collision for current index.
                    }
                }
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedNumber_1, index));
            }
        }
    }

    /**
     * Ensures that the specified value is null or an instance assignable to the given type.
     * If this method does not thrown an exception, then the value can be cast to the class
     * represented by {@code expectedType} without throwing a {@link ClassCastException}.
     *
     * @param  name          the name of the argument to be checked, used only if an exception is thrown.
     *                       Can be {@code null} if the name is unknown.
     * @param  expectedType  the expected type (class or interface).
     * @param  value         the value to check, or {@code null}.
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
     * Ensures that the given index is equal or greater than zero and lower than the given
     * upper value. This method is designed for methods that expect an index value as the only
     * argument. For this reason, this method does not take the argument name.
     *
     * @param  upper  the maximal index value, exclusive.
     * @param  index  the index to check.
     * @throws IndexOutOfBoundsException if the given index is negative or not lower than the given upper value.
     *
     * @see #ensurePositive(String, int)
     *
     * @deprecated As of Java 9, replaced by {@link Objects#checkIndex(int, int)}.
     */
    @Deprecated(since="1.5", forRemoval=true)
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
     * @param  length  the length of the sequence (array, {@link CharSequence}, <i>etc.</i>).
     * @param  lower   the user-specified lower index, inclusive.
     * @param  upper   the user-specified upper index, exclusive.
     * @throws IndexOutOfBoundsException if the given [{@code lower} … {@code upper}]
     *         range is out of the sequence index range.
     *
     * @see #ensureCountBetween(String, boolean, int, int, int)
     *
     * @deprecated As of Java 9, replaced by {@link Objects#checkFromToIndex(int, int, int)}.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public static void ensureValidIndexRange(final int length, final int lower, final int upper) throws IndexOutOfBoundsException {
        if (lower < 0 || upper < lower || upper > length) {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IllegalRange_2, lower, upper));
        }
    }

    /**
     * Ensures that the given integer value is greater than or equals to zero.
     * This method is used for checking values that are <strong>not</strong> index.
     * For checking index values, use {@link Objects#checkIndex(int, int)} instead.
     *
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is negative.
     *
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
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
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
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or negative.
     *
     * @see #ensureStrictlyPositive(String, float)
     */
    public static void ensurePositive(final String name, final float value)
            throws IllegalArgumentException
    {
        // Use `!` for catching NaN.
        if (!(value >= 0) || Float.floatToRawIntBits(value) == Integer.MIN_VALUE) {
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
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or negative.
     *
     * @see #ensureStrictlyPositive(String, double)
     */
    public static void ensurePositive(final String name, final double value)
            throws IllegalArgumentException
    {
        // Use `!` for catching NaN.
        if (!(value >= 0) || Double.doubleToRawLongBits(value) == Long.MIN_VALUE) {
            throw new IllegalArgumentException(Double.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name)  :
                    Errors.format(Errors.Keys.NegativeArgument_2, name, value));
        }
    }

    /**
     * Ensures that the given integer value is greater than zero.
     *
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
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
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
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
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     *
     * @see #ensurePositive(String, float)
     */
    public static void ensureStrictlyPositive(final String name, final float value)
            throws IllegalArgumentException
    {
        if (!(value > 0)) {                                                 // Use `!` for catching NaN.
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
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     *
     * @see #ensurePositive(String, double)
     */
    public static void ensureStrictlyPositive(final String name, final double value)
            throws IllegalArgumentException
    {
        if (!(value > 0)) {                                                 // Use `!` for catching NaN.
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
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or infinite.
     */
    public static void ensureFinite(final String name, final float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(Errors.format(Float.isNaN(value) ?
                    Errors.Keys.NotANumber_1 : Errors.Keys.InfiniteArgumentValue_1, name));
        }
    }

    /**
     * Ensures that the given floating point value is not
     * {@linkplain Double#isNaN(double) NaN} neither {@linkplain Double#isInfinite(double)}.
     * The value can be negative, zero or positive.
     *
     * @param  name   the name of the argument to be checked, used only if an exception is thrown.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or infinite.
     */
    public static void ensureFinite(final String name, final double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(Errors.format(Double.isNaN(value) ?
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
     *   <li>{@link #ensureCountBetween(String, boolean, int, int, int) ensureCountBetween(…)}
     *       if the {@code value} argument is a collection size or an array length.</li>
     *   <li>{@link Objects#checkIndex(int, int)} if the {@code value}
     *       argument is an index in a list or an array.</li>
     * </ul>
     *
     * @param  name   the name of the argument to be checked. Used only if an exception is thrown.
     * @param  min    the minimal value, inclusive.
     * @param  max    the maximal value, inclusive.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is not in the given range.
     *
     * @see #ensureCountBetween(String, boolean, int, int, int)
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
     * @param  name   the name of the argument to be checked. Used only if an exception is thrown.
     * @param  min    the minimal value, inclusive.
     * @param  max    the maximal value, inclusive.
     * @param  value  the user argument to check.
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
     * @param  name   the name of the argument to be checked. Used only if an exception is thrown.
     * @param  min    the minimal value, inclusive.
     * @param  max    the maximal value, inclusive.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or not in the given range.
     */
    public static void ensureBetween(final String name, final float min, final float max, final float value)
            throws IllegalArgumentException
    {
        if (!(value >= min && value <= max)) {                              // Use `!` for catching NaN.
            throw new IllegalArgumentException(Float.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name) :
                    Errors.format(Errors.Keys.ValueOutOfRange_4, name, min, max, value));
        }
    }

    /**
     * Ensures that the given floating point value is between the given bounds, inclusive.
     *
     * @param  name   the name of the argument to be checked. Used only if an exception is thrown.
     * @param  min    the minimal value, inclusive.
     * @param  max    the maximal value, inclusive.
     * @param  value  the user argument to check.
     * @throws IllegalArgumentException if the given value is NaN or not in the given range.
     */
    public static void ensureBetween(final String name, final double min, final double max, final double value)
            throws IllegalArgumentException
    {
        if (!(value >= min && value <= max)) {                              // Use `!` for catching NaN.
            throw new IllegalArgumentException(Double.isNaN(value) ?
                    Errors.format(Errors.Keys.NotANumber_1, name)  :
                    Errors.format(Errors.Keys.ValueOutOfRange_4, name, min, max, value));
        }
    }

    /**
     * Ensures that the given number of elements is between the given bounds, inclusive.
     * This method performs the same check as {@link #ensureBetween(String, int, int, int)
     * ensureBetween(…)}, but the error message is different in case of failure.
     *
     * @param  name       the name of the argument to be checked. Used only if an exception is thrown.
     * @param  collection {@code true} if {@code name} is a collection, or {@code false} for a variable argument list.
     * @param  min        the minimal size (inclusive), or 0 if none.
     * @param  max        the maximal size (inclusive), or {@link Integer#MAX_VALUE} if none.
     * @param  count      the number of user-specified arguments, collection size or array length to be checked.
     * @throws IllegalArgumentException if the given value is not in the given range.
     *
     * @see #ensureBetween(String, int, int, int)
     *
     * @since 1.4
     */
    public static void ensureCountBetween(final String name, final boolean collection,
            final int min, final int max, final int count) throws IllegalArgumentException
    {
        final String message;
        if (count < min) {
            if (count == 0) {
                message = Errors.format(Errors.Keys.EmptyArgument_1, name);
            } else {
                message = collection ? Errors.format(Errors.Keys.TooFewCollectionElements_3, name, min, count)
                                     : Errors.format(Errors.Keys.TooFewArguments_2, min, count);
            }
        } else if (count > max) {
            message = collection ? Errors.format(Errors.Keys.TooManyCollectionElements_3, name, max, count)
                                 : Errors.format(Errors.Keys.TooManyArguments_2, max, count);
        } else {
            return;
        }
        throw new IllegalArgumentException(message);
    }

    /**
     * Ensures that the given integer is a valid Unicode code point. The range of valid code points goes
     * from {@link Character#MIN_CODE_POINT U+0000} to {@link Character#MAX_CODE_POINT U+10FFFF} inclusive.
     *
     * @param  name  the name of the argument to be checked. Used only if an exception is thrown.
     * @param  code  the Unicode code point to verify.
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
     * Ensures that a given value is a divisor of specified number.
     * This method verifies that {@code (number % divisor) == 0}.
     * If above condition is not met, the value considered to be wrong is the divisor.
     *
     * @param  name     name of the argument for the divisor value. Used only if an exception is thrown.
     * @param  number   the number to be divided.
     * @param  divisor  the value to verify.
     * @throws IllegalArgumentException if {@code (number % divisor) != 0}.
     * @throws ArithmeticException if {@code divisor == 0}.
     *
     * @since 1.1
     */
    public static void ensureDivisor(final String name, final int number, final int divisor) {
        if ((number % divisor) != 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotADivisorOrMultiple_4, name, 0, number, divisor));
        }
    }

    /**
     * Ensures that a given value is a multiple of specified number.
     * This method verifies that {@code (multiple % number) == 0}.
     * If above condition is not met, the value considered to be wrong is the multiple.
     *
     * @param  name      name of the argument for the multiple value. Used only if an exception is thrown.
     * @param  number    the number to be multiplied.
     * @param  multiple  the value to verify.
     * @throws IllegalArgumentException if {@code (multiple % number) != 0}.
     * @throws ArithmeticException if {@code number == 0}.
     *
     * @since 1.1
     */
    public static void ensureMultiple(final String name, final int number, final int multiple) {
        if ((multiple % number) != 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotADivisorOrMultiple_4, name, 1, number, multiple));
        }
    }

    /**
     * Ensures that the given CRS, if non-null, has the expected number of dimensions.
     * This method does nothing if the given coordinate reference system is null.
     *
     * @param  name      the name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected  the expected number of dimensions.
     * @param  crs       the coordinate reference system to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException if the given coordinate reference system is non-null
     *         and does not have the expected number of dimensions.
     */
    public static void ensureDimensionMatches(final String name, final int expected,
            final CoordinateReferenceSystem crs) throws MismatchedDimensionException
    {
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs != null) {                                       // Should never be null, but let be safe.
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
     * @param  name      the name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected  the expected number of dimensions.
     * @param  cs        the coordinate system to check for its dimension, or {@code null}.
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
     * Ensures that the given array of indices, if non-null, has the expected number of dimensions
     * (taken as its length). This method does nothing if the given array is null.
     *
     * @param  name      the name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected  the expected number of dimensions.
     * @param  indices   the array of indices to check for its number of dimensions, or {@code null}.
     * @throws MismatchedDimensionException if the given array of indices is non-null and does not have
     *         the expected number of dimensions (taken as its length).
     *
     * @since 1.0
     */
    public static void ensureDimensionMatches(final String name, final int expected, final int[] indices)
            throws MismatchedDimensionException
    {
        if (indices != null) {
            final int dimension = indices.length;
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
     * @param  name      the name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected  the expected number of dimensions.
     * @param  vector    the vector to check for its number of dimensions, or {@code null}.
     * @throws MismatchedDimensionException if the given vector is non-null and does not have the
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
     * @param  name      the name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected  the expected number of dimensions.
     * @param  position  the direct position to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException if the given direct position is non-null and does
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
     * @param  name      the name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected  the expected number of dimensions.
     * @param  envelope  the envelope to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException if the given envelope is non-null and does
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

    /**
     * Ensures that the given grid envelope, if non-null, has the expected number of dimensions.
     * This method does nothing if the given grid envelope is null.
     *
     * @param  name      the name of the argument to be checked. Used only if an exception is thrown.
     * @param  expected  the expected number of dimensions.
     * @param  envelope  the grid envelope to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException if the given envelope is non-null and does
     *         not have the expected number of dimensions.
     *
     * @since 1.3
     */
    public static void ensureDimensionMatches(final String name, final int expected, final GridEnvelope envelope)
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

    /**
     * Ensures that the given transform, if non-null, has the expected number of source and target dimensions.
     * This method does nothing if the given transform is null.
     *
     * @param  name            the name of the argument to be checked. Used only if an exception is thrown.
     * @param  expectedSource  the expected number of source dimensions.
     * @param  expectedTarget  the expected number of target dimensions.
     * @param  transform       the transform to check for its dimension, or {@code null}.
     * @throws MismatchedDimensionException if the given transform is non-null and does
     *         not have the expected number of dimensions.
     *
     * @since 1.1
     */
    public static void ensureDimensionsMatch(final String name, int expectedSource, final int expectedTarget,
                                             final MathTransform transform) throws MismatchedDimensionException
    {
        if (transform != null) {
            int side = 0;
            int dimension = transform.getSourceDimensions();
            if (dimension == expectedSource) {
                dimension = transform.getTargetDimensions();
                if (dimension == expectedTarget) {
                    return;
                }
                expectedSource = expectedTarget;
                side = 1;
            }
            throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedTransformDimension_4,
                                                                 name, side, expectedSource, dimension));
        }
    }
}
