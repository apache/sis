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
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.NavigableSet;
import java.util.Collections;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import static java.lang.Double.doubleToLongBits;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.NumberType;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.internal.shared.DoubleDouble;


/**
 * Static methods working with {@code Number} objects, and a few primitive types by extension.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @see org.apache.sis.math.MathFunctions
 *
 * @since 0.3
 */
@SuppressWarnings("UnnecessaryBoxing")
public final class Numbers {
    /**
     * Constant of value {@value} used in {@code switch} statements or as index in arrays.
     * This enumeration provides the following guarantees (some Apache SIS codes rely on them):
     *
     * <ul>
     *   <li>{@code OTHER} value is 0.</li>
     *   <li>Primitive types are enumerated in this exact order
     *       (from lower value to higher value, but not necessarily as consecutive values):
     *       {@code BYTE}, {@code SHORT}, {@code INTEGER}, {@code LONG}, {@code FLOAT}, {@code DOUBLE}.</li>
     *   <li>{@link java.math} types of greater capacity than primitive types ({@code BIG_DECIMAL}
     *       and {@code BIG_INTEGER}) have higher enumeration values.</li>
     *   <li>{@link Fraction} is considered as a kind of floating point value.</li>
     * </ul>
     *
     * @deprecated Replaced by the {@link NumberType} enumeration.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public static final byte
            BIG_DECIMAL=11, BIG_INTEGER=10, FRACTION=7,
            DOUBLE=9, FLOAT=8, LONG=6, INTEGER=5, SHORT=4, BYTE=3, CHARACTER=2, BOOLEAN=1, OTHER=0;

    /**
     * Do not allow instantiation of this class.
     */
    private Numbers() {
    }

    /**
     * Returns {@code true} if the given {@code type} is a floating point type. The floating point types
     * are {@link Float}, {@code float}, {@link Double}, {@code double} and {@link BigDecimal}.
     * {@link Fraction} is also considered as a kind of floating point values.
     *
     * @param  type  the primitive type or wrapper class to test (can be {@code null}).
     * @return {@code true} if {@code type} is one of the known types capable to represent floating point numbers.
     *
     * @see #isInteger(Class)
     *
     * @deprecated Moved to {@link NumberType#isFractional(Class)}.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public static boolean isFloat(final Class<?> type) {
        return NumberType.isFractional(type);
    }

    /**
     * Returns {@code true} if the given {@code type} is an integer type. The integer types are
     * {@link Byte}, {@code byte}, {@link Short}, {@code short}, {@link Integer}, {@code int},
     * {@link Long}, {@code long} and {@link BigInteger}.
     *
     * @param  type  the primitive type or wrapper class to test (can be {@code null}).
     * @return {@code true} if {@code type} is an integer type.
     *
     * @see #isFloat(Class)
     * @see #round(Number)
     *
     * @deprecated Moved to {@link NumberType#isInteger(Class)}.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public static boolean isInteger(final Class<?> type) {
        return NumberType.isInteger(type);
    }

    /**
     * Returns {@code true} if the given {@code type} is a floating point or an integer type.
     * This method returns {@code true} if either {@link #isFloat(Class)} or {@link #isInteger(Class)}
     * returns {@code true} for the given argument, or if the type is assignable to {@link Number}.
     *
     * @param  type  the primitive type or wrapper class to test (can be {@code null}).
     * @return {@code true} if {@code type} is a {@link Number} or a primitive floating point or integer type.
     *
     * @see #isFloat(Class)
     * @see #isInteger(Class)
     *
     * @since 1.1
     *
     * @deprecated Moved to {@link NumberType#isReal(Class)}.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public static boolean isNumber(final Class<?> type) {
        return NumberType.isReal(type);
    }

    /**
     * Returns {@code true} if the given number is null or NaN.
     * Current implementation recognizes {@link Float}, {@link Double} and {@link Fraction} types.
     *
     * @param  value  the number to test (may be {@code null}).
     * @return {@code true} if the given number is null or NaN.
     *
     * @see Float#isNaN()
     * @see Double#isNaN()
     * @see Fraction#isNaN()
     *
     * @since 1.1
     */
    public static boolean isNaN(final Number value) {
        if (value == null) return true;
        if (value instanceof Double)       return ((Double)       value).isNaN();
        if (value instanceof Float)        return ((Float)        value).isNaN();
        if (value instanceof Fraction)     return ((Fraction)     value).isNaN();
        if (value instanceof DoubleDouble) return ((DoubleDouble) value).isNaN();
        return false;
    }

    /**
     * Returns the value of the given number rounded to nearest {@code long} integer.
     * This method is intended for calculations where fractional parts are rounding errors.
     * An {@link ArithmeticException} is thrown in the following cases:
     *
     * <ul>
     *   <li>If the floating point value is NaN or positive or negative infinity.</li>
     *   <li>If the value overflows the capacity of {@value Long#SIZE} bits integers.</li>
     *   <li>If the number is a {@link BigDecimal} with a non-zero fractional part.</li>
     * </ul>
     *
     * The justification for the last case is that {@link BigDecimal} is used when calculations
     * should be exact in base 10. In such case, a fractional part would not be a rounding error.
     *
     * @param  value  the value to return as a long integer.
     * @return the value rounded to nearest long integer.
     * @throws NullPointerException if the given number is {@code null}.
     * @throws ArithmeticException if the value cannot be represented as a long integer.
     *
     * @see Math#round(double)
     * @see BigDecimal#longValueExact()
     * @see BigInteger#longValueExact()
     *
     * @since 1.4
     */
    public static long round(final Number value) {
        final NumberType mapping = NumberType.forNumberClass(value.getClass());
        switch (mapping) {
            case DOUBLE_DOUBLE: return value.longValue();       // Does rounding.
            case BIG_DECIMAL:   return ((BigDecimal) value).longValueExact();
            case BIG_INTEGER:   return ((BigInteger) value).longValueExact();
            default: if (mapping.isInteger()) return value.longValue();
        }
        final double v = value.doubleValue();
        final long   n = Math.round(v);
        if (Math.abs(v - n) <= 0.5) return n;
        throw new ArithmeticException(Errors.format(Errors.Keys.CanNotConvertValue_2, value, Long.TYPE));
    }

    /**
     * Returns the number of bits used by primitive of the specified type.
     * The given type must be a primitive type or its wrapper class.
     *
     * @param  type  the primitive type (can be {@code null}).
     * @return the number of bits, or 0 if {@code type} is null.
     * @throws IllegalArgumentException if the given type is not one of the types supported by this {@code Numbers} class.
     *
     * @deprecated Replaced by {@link NumberType#size()}.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public static int primitiveBitCount(final Class<?> type) throws IllegalArgumentException {
        if (type == null) {
            return 0;
        }
        return NumberType.forNumberClass(type).size().orElseThrow();
    }

    /**
     * Changes a primitive class to its wrapper (for example {@code int} to {@link Integer}).
     * If the specified class is not a primitive type, then it is returned unchanged.
     *
     * @param  <N>   the primitive and wrapper type (both have the same parametric declaration).
     * @param  type  the primitive type (can be {@code null}).
     * @return the type as a wrapper.
     *
     * @deprecated Moved to {@link NumberType#primitiveToWrapper(Class)}.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public static <N> Class<N> primitiveToWrapper(final Class<N> type) {
        return NumberType.primitiveToWrapper(type);
    }

    /**
     * Changes a wrapper class to its primitive (for example {@link Integer} to {@code int}).
     * If the specified class is not a wrapper type, then it is returned unchanged.
     *
     * @param  <N>   the primitive and wrapper type (both have the same parametric declaration).
     * @param  type  the wrapper type (can be {@code null}).
     * @return the type as a primitive.
     *
     * @deprecated Moved to {@link NumberType#wrapperToPrimitive(Class)}.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public static <N> Class<N> wrapperToPrimitive(final Class<N> type) {
        return NumberType.wrapperToPrimitive(type);
    }

    /**
     * Returns the widest type of two numbers. Numbers {@code n1} and {@code n2} can be instance of
     * {@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double},
     * {@link Fraction}, {@link BigInteger} or {@link BigDecimal} types.
     *
     * <p>If one of the given arguments is null, then this method returns the class of the non-null argument.
     * If both arguments are null, then this method returns {@code null}.</p>
     *
     * @param  n1  the first number, or {@code null}.
     * @param  n2  the second number, or {@code null}.
     * @return the widest type of the given numbers, or {@code null} if both {@code n1} and {@code n2} are null.
     * @throws IllegalArgumentException if a number is not an instance of a supported type.
     *
     * @see #widestClass(Number, Number)
     * @see #narrowestClass(Number, Number)
     */
    public static Class<? extends Number> widestClass(final Number n1, final Number n2)
            throws IllegalArgumentException
    {
        return widestClass((n1 != null) ? n1.getClass() : null,
                           (n2 != null) ? n2.getClass() : null);
    }

    /**
     * Returns the widest of the given types. Classes {@code c1} and {@code c2} can be
     * {@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double},
     * {@link Fraction}, {@link BigInteger} or {@link BigDecimal} types.
     *
     * <p>If one of the given arguments is null, then this method returns the non-null argument.
     * If both arguments are null, then this method returns {@code null}.</p>
     *
     * <h4>Example</h4>
     * in the following code, the result stored in {@code type} will be {@code Long.class}:
     *
     * {@snippet lang="java" :
     *     Class<?> type = widestClass(Short.class, Long.class);
     *     }
     *
     * <h4>Accuracy</h4>
     * Note that conversions to the widest class are not guaranteed to be lossless.
     * For example, {@code double} is considered a wider class than {@code long}, but
     * conversion from {@code long} to {@code double} can nevertheless loose accuracy.
     *
     * @param  c1  the first number type, or {@code null}.
     * @param  c2  the second number type, or {@code null}.
     * @return the widest of the given types, or {@code null} if both {@code c1} and {@code c2} are null.
     * @throws IllegalArgumentException if one of the given types is not supported by this {@code Numbers} class.
     *
     * @see #widestClass(Class, Class)
     * @see #narrowestClass(Number, Number)
     */
    public static Class<? extends Number> widestClass(final Class<? extends Number> c1,
                                                      final Class<? extends Number> c2)
            throws IllegalArgumentException
    {
        if (c1 == c2)   return c2;  // Avoid IllegalArgumentException if the type is unknown.
        if (c1 == null) return c2;
        if (c2 == null) return c1;
        return NumberType.forNumberClass(c1).isWiderThan(NumberType.forNumberClass(c2)) ? c1 : c2;
    }

    /**
     * Returns the narrowest type of two numbers. Numbers {@code n1} and {@code n2} can be instance of
     * {@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double},
     * {@link Fraction}, {@link BigInteger} or {@link BigDecimal} types.
     *
     * <p>If one of the given arguments is null, then this method returns the class of the non-null argument.
     * If both arguments are null, then this method returns {@code null}.</p>
     *
     * @param  n1  the first number, or {@code null}.
     * @param  n2  the second number, or {@code null}.
     * @return the narrowest type of the given numbers, or {@code null} if both {@code n1} and {@code n2} are null.
     * @throws IllegalArgumentException if a number is not an instance of a supported type.
     *
     * @see #narrowestClass(Class, Class)
     * @see #widestClass(Class, Class)
     */
    public static Class<? extends Number> narrowestClass(final Number n1, final Number n2)
            throws IllegalArgumentException
    {
        return narrowestClass((n1 != null) ? n1.getClass() : null,
                              (n2 != null) ? n2.getClass() : null);
    }

    /**
     * Returns the narrowest of the given types. Classes {@code c1} and {@code c2} can be
     * {@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double},
     * {@link Fraction}, {@link BigInteger} or {@link BigDecimal} types.
     *
     * <p>If one of the given arguments is null, then this method returns the non-null argument.
     * If both arguments are null, then this method returns {@code null}.</p>
     *
     * <h4>Example</h4>
     * in the following code, the result stored in {@code type} will be {@code Short.class}:
     *
     * {@snippet lang="java" :
     *     Class<?> type = narrowestClass(Short.class, Long.class);
     *     }
     *
     * @param  c1  the first number type, or {@code null}.
     * @param  c2  the second number type, or {@code null}.
     * @return the narrowest of the given types, or {@code null} if both {@code c1} and {@code c2} are null.
     * @throws IllegalArgumentException if one of the given types is not supported by this {@code Numbers} class.
     *
     * @see #narrowestClass(Number, Number)
     * @see #widestClass(Class, Class)
     */
    public static Class<? extends Number> narrowestClass(final Class<? extends Number> c1,
                                                         final Class<? extends Number> c2)
            throws IllegalArgumentException
    {
        if (c1 == c2)   return c2;  // Avoid IllegalArgumentException if the type is unknown.
        if (c1 == null) return c2;
        if (c2 == null) return c1;
        return NumberType.forNumberClass(c1).isNarrowerThan(NumberType.forNumberClass(c2)) ? c1 : c2;
    }

    /**
     * Returns the smallest class capable to hold the specified value.
     * This method applies the following choices, in that order:
     *
     * <ul>
     *   <li>If the given value is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given value cannot be cast from {@code double} to another type
     *       without precision lost, return {@code Double.class}.</li>
     *   <li>Otherwise if the given value cannot be cast from {@code float} to another type
     *       without precision lost, return {@code Float.class}.</li>
     *   <li>Otherwise if the given value is between {@value java.lang.Byte#MIN_VALUE} and
     *       {@value java.lang.Byte#MAX_VALUE}, then this method returns {@code Byte.class};</li>
     *   <li>Otherwise if the given value is between {@value java.lang.Short#MIN_VALUE} and
     *       {@value java.lang.Short#MAX_VALUE}, then this method returns {@code Short.class};</li>
     *   <li>Otherwise if the given value is between {@value java.lang.Integer#MIN_VALUE} and
     *       {@value java.lang.Integer#MAX_VALUE}, then this method returns {@code Integer.class};</li>
     *   <li>Otherwise this method returns {@code Long.class};</li>
     * </ul>
     *
     * @param  value  the value to be wrapped in a finer (if possible) {@link Number}.
     * @return the narrowest type capable to hold the given value.
     *
     * @see #narrowestNumber(Number)
     */
    @SuppressWarnings("fallthrough")
    public static Class<? extends Number> narrowestClass(final Number value) {
        if (value == null) {
            return null;
        }
        boolean isFloat = false;
        final long longValue = value.longValue();
        switch (NumberType.forNumberClass(value.getClass())) {
            default: {
                final double doubleValue = value.doubleValue();
                final float  floatValue  = (float) doubleValue;
                isFloat = (doubleToLongBits(floatValue) == doubleToLongBits(doubleValue));
                if (doubleValue != longValue) {
                    return isFloat ? Float.class : Double.class;
                }
                // Fall through.
            }
            case LONG:    if (((int)   longValue) != longValue) return isFloat ? Float.class : Long.class;
            case INTEGER: if (((short) longValue) != longValue) return Integer.class;
            case SHORT:   if (((byte)  longValue) != longValue) return Short  .class;
            case BYTE:    return Byte.class;
        }
    }

    /**
     * Returns the given number wrapped in the smallest class capable to hold the specified value.
     * This method is equivalent to the following code, in a slightly more efficient way:
     *
     * {@snippet lang="java" :
     *     return cast(value, narrowestClass(value));
     *     }
     *
     * @param  value  the value to be wrapped in a finer (if possible) {@link Number}.
     * @return the narrowest type capable to hold the given value.
     *
     * @see #narrowestClass(Number)
     * @see #cast(Number, Class)
     */
    @SuppressWarnings("fallthrough")
    public static Number narrowestNumber(final Number value) {
        if (value == null) {
            return null;
        }
        final Number candidate;
        boolean isFloat = false;
        final long longValue = value.longValue();
        switch (NumberType.forNumberClass(value.getClass())) {
            default: {
                final double doubleValue = value.doubleValue();
                final float  floatValue  = (float) doubleValue;
                isFloat = (doubleToLongBits(floatValue) == doubleToLongBits(doubleValue));
                if (doubleValue != longValue) {
                    // Do not use "isFloat ? … : …" operator as it inserts undesired automatic auto-(un)boxing.
                    if (isFloat) candidate = floatValue;
                    else         candidate = doubleValue;
                    break;
                }
                // Fall through everywhere.
            }
            case LONG: {
                if (((int) longValue) != longValue) {
                    // Do not use "isFloat ? … : …" operator as it inserts undesired automatic auto-(un)boxing.
                    if (isFloat) candidate = Float.valueOf(longValue);
                    else         candidate = Long .valueOf(longValue);
                    break;
                }
            }
            case INTEGER: {
                if (((short) longValue) != longValue) {
                    candidate = Integer.valueOf((int) longValue);
                    break;
                }
            }
            case SHORT: {
                if (((byte) longValue) != longValue) {
                    candidate = Short.valueOf((short) longValue);
                    break;
                }
            }
            case BYTE: {
                candidate = Byte.valueOf((byte) longValue);
                break;
            }
        }
        // Keep the existing instance if possible.
        return value.equals(candidate) ? value : candidate;
    }

    /**
     * Returns the smallest number capable to hold the specified value.
     *
     * @param  value  the value to be wrapped in a {@link Number}.
     * @return the narrowest type capable to hold the given value.
     * @throws NumberFormatException if the given value cannot be parsed as a number.
     *
     * @see #narrowestNumber(Number)
     */
    public static Number narrowestNumber(final String value) throws NumberFormatException {
        /*
         * Do not trim whitespaces. It is up to the caller to do that if he wants.
         * For such low level function, we are better to avoid hidden initiative.
         */
        final int length = value.length();
        for (int i=0; i<length; i++) {
            final char c = value.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') {
                return narrowestNumber(Double.valueOf(value));
            }
        }
        return narrowestNumber(Long.valueOf(value));
    }

    /**
     * Casts a number to the specified type. The target type can be one of {@link Byte},
     * {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double},
     * {@link Fraction}, {@link BigInteger} or {@link BigDecimal}.
     * This method makes the following choice:
     *
     * <ul>
     *   <li>If the given value is {@code null} or an instance of the given type, then it is returned unchanged.</li>
     *   <li>Otherwise if the given type is {@code Double.class}, then this method returns
     *       <code>{@linkplain Double#valueOf(double) Double.valueOf}(number.doubleValue())</code>;</li>
     *   <li>Otherwise if the given type is {@code Float.class}, then this method returns
     *       <code>{@linkplain Float#valueOf(float) Float.valueOf}(number.floatValue())</code>;</li>
     *   <li>And likewise for all remaining known types.</li>
     * </ul>
     *
     * This method does not verify if the given type is wide enough for the given value,
     * because the type has typically been calculated by {@link #widestClass(Number, Number)}
     * or {@link #narrowestClass(Number)}. If nevertheless the given type is not wide enough,
     * then the behavior depends on the implementation of the corresponding
     * {@code Number.fooValue()} method - typically, the value is just rounded or truncated.
     *
     * @param  <N>     the class to cast to.
     * @param  number  the number to cast, or {@code null}.
     * @param  type    the destination type.
     * @return the number cast to the given type, or {@code null} if the given value was null.
     * @throws IllegalArgumentException if the given type is not supported by this {@code Numbers} class,
     *         or the number cannot be converted to the specified type (e.g. {@link Double#NaN} cannot
     *         be converted to {@link BigDecimal}).
     *
     * @see NumberType#cast(Number)
     */
    @SuppressWarnings("unchecked")
    public static <N extends Number> N cast(final Number number, final Class<N> type) throws IllegalArgumentException {
        if (number == null || type.isInstance(number)) {
            return (N) number;
        }
        try {
            return type.cast(NumberType.forNumberClass(type).cast(number));
        } catch (UnsupportedOperationException | ArithmeticException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumericalType_1, type), e);
        }
    }

    /**
     * Wraps the given floating-point value in a {@code Number} of the specified class.
     * The given type shall be one of {@link Byte}, {@link Short}, {@link Integer}, {@link Long},
     * {@link Float}, {@link Double}, {@link Fraction}, {@link BigInteger} and {@link BigDecimal} classes.
     * Furthermore, the given value shall be convertible to the given class without precision lost,
     * otherwise an {@link IllegalArgumentException} will be thrown.
     *
     * @param  <N>    the wrapper class.
     * @param  value  the value to wrap.
     * @param  type   the desired wrapper class.
     * @return the value wrapped in an object of the given class.
     * @throws IllegalArgumentException if the given type is not supported by this {@code Numbers} class,
     *         or if the given value cannot be wrapped in an instance of the given class without precision lost.
     *
     * @see NumberType#wrapExact(double)
     */
    public static <N extends Number> N wrap(final double value, final Class<N> type) throws IllegalArgumentException {
        try {
            return type.cast(NumberType.forNumberClass(type).wrapExact(value));
        } catch (UnsupportedOperationException | ArithmeticException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.CanNotConvertValue_2, value, type), e);
        }
    }

    /**
     * Wraps the given integer value in a {@code Number} of the specified class.
     * The given type shall be one of {@link Byte}, {@link Short}, {@link Integer}, {@link Long},
     * {@link Float}, {@link Double}, {@link Fraction}, {@link BigInteger} and {@link BigDecimal} classes.
     * Furthermore, the given value shall be convertible to the given class without precision lost,
     * otherwise an {@link IllegalArgumentException} will be thrown.
     *
     * @param  <N>    the wrapper class.
     * @param  value  the value to wrap.
     * @param  type   the desired wrapper class.
     * @return the value wrapped in an object of the given class.
     * @throws IllegalArgumentException if the given type is not supported by this {@code Numbers} class,
     *         or if the given value cannot be wrapped in an instance of the given class without precision lost.
     *
     * @since 0.8
     *
     * @see NumberType#wrapExact(long)
     */
    public static <N extends Number> N wrap(final long value, final Class<N> type) throws IllegalArgumentException {
        try {
            return type.cast(NumberType.forNumberClass(type).wrapExact(value));
        } catch (UnsupportedOperationException | ArithmeticException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.CanNotConvertValue_2, value, type), e);
        }
    }

    /**
     * Converts the specified string into a value object.
     * The value object can be an instance of {@link BigDecimal}, {@link BigInteger}, {@link Fraction},
     * {@link Double}, {@link Float}, {@link Long}, {@link Integer}, {@link Short}, {@link Byte},
     * {@link Boolean}, {@link Character} or {@link String} according the specified type.
     * This method makes the following choice:
     *
     * <ul>
     *   <li>If the given type is {@code Double.class}, then this method returns
     *       <code>{@linkplain Double#valueOf(String) Double.valueOf}(value)</code>;</li>
     *   <li>If the given type is {@code Float.class}, then this method returns
     *       <code>{@linkplain Float#valueOf(String) Float.valueOf}(value)</code>;</li>
     *   <li>And likewise for all remaining known types.</li>
     * </ul>
     *
     * @param  <T>    the requested type.
     * @param  text   the text to parse.
     * @param  type   the requested type.
     * @return the value object, or {@code null} if {@code value} was null.
     * @throws IllegalArgumentException if {@code type} is not a recognized type.
     * @throws NumberFormatException if {@code type} is a subclass of {@link Number}
     *         and the string value is not parsable as a number of the specified type.
     *
     * @see NumberType#parse(String)
     */
    public static <T> T valueOf(final String text, final Class<T> type)
            throws IllegalArgumentException, NumberFormatException
    {
        Object value;
        if (text == null || type == String.class) {
            value = text;
        } else {
            value = NumberType.forNumberClass(type).parse(text);
        }
        return type.cast(value);
    }

    /**
     * Returns a {@code NaN}, zero, empty or {@code null} value of the given type.
     * This method tries to return the closest value that can be interpreted as <q>none</q>,
     * which is usually not the same as <q>zero</q>. More specifically:
     *
     * <ul>
     *   <li>If the given type is a floating point <strong>primitive</strong> type ({@code float}
     *       or {@code double}), then this method returns {@link Float#NaN} or {@link Double#NaN}
     *       depending on the given type.</li>
     *
     *   <li>If the given type is an integer <strong>primitive</strong> type or the character type
     *       ({@code long}, {@code int}, {@code short}, {@code byte} or {@code char}), then this
     *       method returns the zero value of the given type.</li>
     *
     *   <li>If the given type is the {@code boolean} <strong>primitive</strong> type, then this
     *       method returns {@link Boolean#FALSE}.</li>
     *
     *   <li>If the given type is an array or a collection, then this method returns an empty
     *       array or collection. The given type is honored on a <em>best effort</em> basis.</li>
     *
     *   <li>For all other cases, including the wrapper classes of primitive types, this method
     *       returns {@code null}.</li>
     * </ul>
     *
     * Despite being defined in the {@code Numbers} class, the scope of this method has been extended
     * to array and collection types because those objects can also be seen as mathematical concepts.
     *
     * @param  <T>   the compile-time type of the requested object.
     * @param  type  the type of the object for which to get a nil value.
     * @return an object of the given type which represents a nil value, or {@code null}.
     *
     * @see NumberType#nilValue()
     * @see org.apache.sis.xml.NilObject
     */
    public static <T> T valueOfNil(final Class<T> type) {
        if (type == null) {
            return null;
        }
        Object value = NumberType.forClass(type).orElse(NumberType.NULL).nilValue();
        if (value == null) {
            value = NIL_VALUES.get(type);
            if (value == null && type != Object.class) {
                if (type.isAssignableFrom(Set.class)) {
                    value = Collections.EMPTY_SET;
                } else {
                    Class<?> element = type.getComponentType();
                    if (element != null) {
                        value = Array.newInstance(element, 0);
                    }
                }
            }
        }
        return type.cast(value);
    }

    /**
     * Nil values for each type other than the ones managed by {@link NumberType}.
     */
    private static final Map<Class<?>, Object> NIL_VALUES = Map.of(
            Map.class,          Collections.EMPTY_MAP,
            Set.class,          Collections.EMPTY_SET,
            List.class,         Collections.EMPTY_LIST,
            Queue.class,        Containers.emptyQueue(),
            SortedSet.class,    Collections.emptySortedSet(),
            NavigableSet.class, Collections.emptyNavigableSet());

    /**
     * Returns a numeric constant for the given type.
     * The constants are {@link #BIG_DECIMAL}, {@link #BIG_INTEGER}, {@link #FRACTION},
     * {@link #DOUBLE}, {@link #FLOAT}, {@link #LONG}, {@link #INTEGER},
     * {@link #SHORT}, {@link #BYTE}, {@link #CHARACTER}, {@link #BOOLEAN}, or {@link #OTHER}
     * constants for the given type. This is a commodity for usage in {@code switch} statements.
     *
     * @param  type  a type (usually either a primitive type or its wrapper), or {@code null}.
     * @return the constant for the given type, or {@link #OTHER} if unknown.
     *
     * @deprecated Replaced by the {@link NumberType} enumeration.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public static byte getEnumConstant(final Class<?> type) {
        switch (NumberType.forClass(type).orElse(NumberType.NULL)) {
            case BIG_DECIMAL: return BIG_DECIMAL;
            case BIG_INTEGER: return BIG_INTEGER;
            case FRACTION:    return FRACTION;
            case DOUBLE:      return DOUBLE;
            case FLOAT:       return FLOAT;
            case LONG:        return LONG;
            case INTEGER:     return INTEGER;
            case SHORT:       return SHORT;
            case BYTE:        return BYTE;
            case CHARACTER:   return CHARACTER;
            case BOOLEAN:     return BOOLEAN;
            default:          return OTHER;
        }
    }
}
