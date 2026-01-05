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
package org.apache.sis.math;

import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Type of numbers recognized by Apache <abbr>SIS</abbr>, together with primitive types by extension.
 * This enumeration includes the standard Java numeric types together with subclasses of {@link Number}
 * provided by <abbr>SIS</abbr>.
 *
 * <h2>Order</h2>
 * Loosely speaking, enumeration values are ordered from narrowest type (with {@link #VOID} viewed
 * as if it was a numeric type of size 0) to widest type, with types of variable size ordered last.
 * Some ambiguity exist, in particular between {@link #FRACTION}, {@link #LONG} and {@link #DOUBLE}.
 * Nevertheless, this enumeration provides the following guarantees:
 *
 * <ul>
 *   <li>The ordinal value of the {@link #VOID} value is 0.</li>
 *   <li>Primitive types are enumerated in this exact order, but not necessarily as consecutive values:
 *       {@link #BYTE}, {@link #SHORT}, {@link #INTEGER}, {@link #LONG}, {@link #FLOAT}, {@link #DOUBLE}.</li>
 *   <li>{@link #BIG_INTEGER} and {@link #BIG_DECIMAL} are ordered after primitive types.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @see org.apache.sis.util.Numbers
 * @see org.apache.sis.image.DataType
 *
 * @since 1.6
 */
public enum NumberType {
    /**
     * The void type, viewed as if it was a numeric type with a size of 0 bit.
     */
    VOID(Void.TYPE, Void.class, (byte) 0, (byte) 0),

    /**
     * The {@code boolean} primitive type or its wrapper class.
     * This is sometime handled as an integer value which accepts only the values 0 and 1.
     * This is considered as a type with a {@linkplain #size() size} of 1 bit.
     */
    BOOLEAN(Boolean.TYPE, Boolean.class, (byte) 0, (byte) 1) {
        @Override public Comparable<?> nilValue() {return Boolean.FALSE;}
        @Override public Comparable<?> parse(String text) {
            Boolean n = Strings.parseBoolean(Objects.requireNonNull(text));
            if (n != null) return n;
            throw new IllegalArgumentException(Errors.format(Errors.Keys.CanNotParse_1, text));
        }
    },

    /**
     * The {@code byte} primitive type or its wrapper class.
     */
    BYTE(Byte.TYPE, Byte.class, (byte) 1, (byte) Byte.SIZE) {
        @Override public Comparable<?> nilValue()          {return (byte) 0;}
        @Override public Comparable<?> parse    (String n) {return Byte.valueOf(n);}
        @Override public Number        cast     (Number n) {return (n == null || n instanceof Byte) ? n : n.byteValue();}
        @Override public Number        wrapExact(long   n) {return verify((byte) n, n);}
        @Override public Number        wrapExact(double n) {return verify((byte) n, n);}
    },

    /**
     * The {@code char} primitive type or its wrapper class.
     * This is sometime handled as a 16 bits unsigned integer value.
     * This enumeration value is defined for completeness but is rarely used by <abbr>SIS</abbr>.
     */
    CHARACTER(Character.TYPE, Character.class, (byte) 0, (byte) Character.SIZE) {
        @Override public boolean isWiderThan   (NumberType other) {return other.ordinal() <= BYTE.ordinal();}
        @Override public boolean isNarrowerThan(NumberType other) {return other.isBetween(INTEGER, BIG_DECIMAL);}
        @Override public Comparable<?> nilValue() {return (char) 0;}
        @Override public Comparable<?> parse(String text) {
            switch (text.length()) {
                case 0:  return (char) 0;    // Means "end of string" in C/C++ and NULL in Unicode standard.
                case 1:  return text.charAt(0);
                default: throw new IllegalArgumentException(Errors.format(Errors.Keys.CanNotParse_1, text));
            }
        }
    },

    /**
     * The {@code short} primitive type or its wrapper class.
     */
    SHORT(Short.TYPE, Short.class, (byte) 1, (byte) Short.SIZE) {
        @Override public Comparable<?> nilValue()          {return (short) 0;}
        @Override public Comparable<?> parse    (String n) {return Short.valueOf(n);}
        @Override public Number        cast     (Number n) {return (n == null || n instanceof Short) ? n : n.shortValue();}
        @Override public Number        wrapExact(long   n) {return verify((short) n, n);}
        @Override public Number        wrapExact(double n) {return verify((short) n, n);}
    },

    /**
     * The {@code int} primitive type or its wrapper class.
     */
    INTEGER(Integer.TYPE, Integer.class, (byte) 1, (byte) Integer.SIZE) {
        @Override public Comparable<?> nilValue()          {return 0;}
        @Override public Comparable<?> parse    (String n) {return Integer.valueOf(n);}
        @Override public Number        cast     (Number n) {return (n == null || n instanceof Integer) ? n : n.intValue();}
        @Override public Number        wrapExact(long   n) {return verify((int) n, n);}
        @Override public Number        wrapExact(double n) {return verify((int) n, n);}
        @Override public boolean       isConversionLossless(NumberType target) {
            return target.isBetween(INTEGER, BIG_DECIMAL) && target != FLOAT;
        }
    },

    /**
     * The {@link Fraction} class. This is a ratio of two {@link #INTEGER} values.
     */
    FRACTION(Fraction.class, Fraction.class, (byte) 2, (byte) (2 * Integer.SIZE)) {
        @Override public Comparable<?> nilValue()          {return new Fraction(0, 0);}
        @Override public Comparable<?> parse    (String n) {return new Fraction(n);}
        @Override public Number        cast     (Number n) {return (n == null || n instanceof Fraction) ? n : Fraction.valueOf(n.doubleValue());}
        @Override public Number        wrapExact(double n) {return Fraction.valueOf(n);} // Verification done by constructor.
        @Override public Number        wrapExact(long   n) {return verify(new Fraction((int) n, 1), n);}
        @Override public boolean       isConversionLossless(NumberType target) {
            return equals(target);  // Implicit null check.
        }
    },

    /**
     * The {@code long} primitive type or its wrapper class.
     */
    LONG(Long.TYPE, Long.class, (byte) 1, (byte) Long.SIZE) {
        @Override public Comparable<?> nilValue()          {return 0L;}
        @Override public Comparable<?> parse    (String n) {return Long.valueOf(n);}
        @Override public Number        cast     (Number n) {return (n == null || n instanceof Long) ? n : n.longValue();}
        @Override public Number        wrapExact(long   n) {return n;}
        @Override public Number        wrapExact(double n) {return verify((long) n, n);}
        @Override public boolean       isConversionLossless(NumberType target) {
            return target.isBetween(DOUBLE_DOUBLE, BIG_DECIMAL) || target == this;
        }
    },

    /**
     * The {@code float} primitive type or its wrapper class.
     */
    FLOAT(Float.TYPE, Float.class, (byte) 2, (byte) Float.SIZE) {
        @Override public Comparable<?> nilValue()          {return Float.NaN;}
        @Override public Comparable<?> parse    (String n) {return Float.valueOf(n);}
        @Override public Number        cast     (Number n) {return (n == null || n instanceof Float) ? n : n.floatValue();}
        @Override public Number        wrapExact(double n) {return verify((float) n, n);}
        @Override public Number        wrapExact(long   n) {return verify((float) n, n);}
        @Override public boolean       isConversionLossless(NumberType target) {
            return target.isBetween(FLOAT, DOUBLE_DOUBLE);
        }
    },

    /**
     * The {@code double} primitive type or its wrapper class.
     */
    DOUBLE(Double.TYPE, Double.class, (byte) 2, (byte) Double.SIZE) {
        @Override public Comparable<?> nilValue()          {return Double.NaN;}
        @Override public Comparable<?> parse    (String n) {return Double.valueOf(n);}
        @Override public Number        cast     (Number n) {return (n == null || n instanceof Double) ? n : n.doubleValue();}
        @Override public Number        wrapExact(double n) {return n;}
        @Override public Number        wrapExact(long   n) {return verify((double) n, n);}
        @Override public boolean       isConversionLossless(NumberType target) {
            return target.isBetween(DOUBLE, DOUBLE_DOUBLE);
        }
    },

    /**
     * An internal type used by <abbr>SIS</abbr> for double-double precision arithmetic.
     *
     * @hidden
     */
    DOUBLE_DOUBLE(DoubleDouble.class, DoubleDouble.class, (byte) 2, (byte) (2 * Double.SIZE)) {
        @Override public Comparable<?> nilValue()          {return DoubleDouble.NaN;}
        @Override public Number        wrapExact(long   n) {return DoubleDouble.of(n);}
        @Override public Number        wrapExact(double n) {return DoubleDouble.of(n, false);}
        @Override public Number        cast     (Number n) {return DoubleDouble.of(n, false);}
    },

    /**
     * The {@link BigInteger} class.
     */
    BIG_INTEGER(BigInteger.class, BigInteger.class, (byte) 1, (byte) -1) {
        @Override public Comparable<?> nilValue()          {return BigInteger.ZERO;}
        @Override public Number        wrapExact(long   n) {return BigInteger.valueOf(n);}
        @Override public Number        wrapExact(double n) {return verify(BigDecimal.valueOf((long) n), n);}
        @Override public Comparable<?> parse    (String n) {return new BigInteger(n);}
        @Override public Number        cast     (Number n) {
            if (n == null || n instanceof BigInteger) {
                return n;
            } else if (n instanceof BigDecimal) {
                return ((BigDecimal) n).toBigInteger();
            } else {
                return BigInteger.valueOf(n.longValue());
            }
        }
    },

    /**
     * The {@link BigDecimal} class.
     */
    BIG_DECIMAL(BigDecimal.class, BigDecimal.class, (byte) 2, (byte) -1) {
        @Override public Comparable<?> nilValue()          {return BigDecimal.ZERO;}
        @Override public Number        wrapExact(long   n) {return BigDecimal.valueOf(n);}
        @Override public Number        wrapExact(double n) {return BigDecimal.valueOf(n);}
        @Override public Comparable<?> parse    (String n) {return new BigDecimal(n);}
        @Override public Number        cast     (Number n) {
            if (n == null || n instanceof BigDecimal) {
                return n;
            } else if (n instanceof BigInteger) {
                return new BigDecimal((BigInteger) n);
            } else if (isInteger(n.getClass())) {
                return BigDecimal.valueOf(n.longValue());
            } else {
                return new BigDecimal(n.toString());   // Like `BigDecimal.valueOf(double)` but better with `Float`.
            }
        }
    },

    /**
     * Any {@link Number} class not recognized by Apache <abbr>SIS</abbr>.
     */
    NUMBER(Number.class, Number.class, (byte) 3, (byte) -1),

    /**
     * The null type, used when a {@code Class} argument is {@code null}.
     * This is the only enumeration value for which {@link #classOfValues(boolean)} returns {@code null}.
     */
    NULL(null, null, (byte) 0, (byte) -1);

    /**
     * The primitive type, or {@link #wrapper} if none.
     *
     * @see #classOfValues(boolean)
     * @see Class#isPrimitive()
     */
    final Class<?> primitive;

    /**
     * The wrapper for the primitive type.
     *
     * @see #classOfValues(boolean)
     */
    final Class<?> wrapper;

    /**
     * A classification of the set that contains this type. Supported values are:
     *
     * <ul>
     *   <li>0 for non-numeric types ({@link #VOID}, {@link #BOOLEAN}, {@link #CHARACTER}, {@link #NULL}).</li>
     *   <li>1 for integers.</li>
     *   <li>2 for types capable to store fractional digits.</li>
     *   <li>3 for unknown numeric type ({@link #NUMBER}).</li>
     * </ul>
     *
     * These values are mutually exclusive.
     *
     * @see #isInteger()
     * @see #isFractional()
     * @see #isNumber()
     */
    private final byte category;

    /**
     * The size in bits, or -1 if variable.
     */
    private final byte size;

    /**
     * Creates an enumeration value.
     *
     * @param primitive  the primitive type, or {@code wrapper} if none.
     * @param wrapper    the wrapper for the primitive type.
     * @param category   0=non-numeric, 1=integers, 2=fractional, 3=other.
     * @param size       the size in bits, or -1 if variable.
     */
    private NumberType(final Class<?> primitive, final Class<?> wrapper, final byte category, final byte size) {
        this.primitive = primitive;
        this.wrapper   = wrapper;
        this.category  = category;
        this.size      = size;
    }

    /**
     * Mapping between a type and the enumeration value, if any.
     *
     * <h4>Implementation note</h4>
     * In the particular case of {@code Class} keys, {@code IdentityHashMap} and {@code HashMap} have identical
     * behavior since {@code Class} is final and does not override the {@code equals(Object)} and {@code hashCode()}
     * methods. The {@code IdentityHashMap} Javadoc claims that it is faster than the regular {@code HashMap}.
     * But maybe the most interesting property is that it allocates less objects since {@code IdentityHashMap}
     * implementation does not need the chain of objects created by {@code HashMap}.
     */
    private static final Map<Class<?>, NumberType> MAPPING = new IdentityHashMap<>(13);
    static {
        for (NumberType e : values()) {
            MAPPING.put(e.primitive, e);
            MAPPING.put(e.wrapper, e);
        }
    }

    /**
     * Returns the enumeration value for the given type, or {@code null} if none.
     * This method does not handle the {@link Number} special case.
     *
     * @param  type  the primitive type or {@link Number} class to test (can be {@code null}).
     * @return the enumeration value for the given type, or {@code null} if none.
     */
    private static NumberType valueOrNull(final Class<?> type) {
        return MAPPING.get(type);
    }

    /**
     * Returns the enumeration value for the given class.
     * If the {@code type} argument is null, returns {@link #NULL}.
     * If the {@code type} is an unrecognized {@link Number} class, returns the generic {@link #NUMBER} value.
     * If the {@code type} is an unrecognized non-numerical class, returns an empty value.
     *
     * @param  type  the primitive type or {@link Number} class for which to get an enumeration value, or {@code null}.
     * @return the enumeration value for the given type. May be {@link #NULL} if the given value was null.
     */
    public static Optional<NumberType> forClass(final Class<?> type) {
        NumberType value = valueOrNull(type);   // This is `NULL` if `type` is null.
        if (value == null && Number.class.isAssignableFrom(type)) {
            value = NUMBER;
        }
        return Optional.ofNullable(value);
    }

    /**
     * Returns the enumeration value for the given numeric class. This method is equivalent to
     * <code>{@linkplain #forClass(Class) forClass}(type).orElseThrow()</code> except that the
     * exception is an {@link IllegalArgumentException}. The exception is never thrown if the
     * given class is a primitive type, its wrapper, or a class assignable to {@link Number}.
     *
     * @param  type  the primitive type or {@link Number} class for which to get an enumeration value, or {@code null}.
     * @return the enumeration value for the given type. Never {@code null} but may be {@link #NULL}.
     * @throws IllegalArgumentException if the given class cannot be mapped to any enumeration value.
     */
    public static NumberType forNumberClass(final Class<?> type) {
        NumberType value = valueOrNull(type);
        if (value != null) return value;
        if (Number.class.isAssignableFrom(type)) return NUMBER;
        throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumericalType_1, type));
    }

    /**
     * Returns an enumeration value capable to represent at least approximately all the given classes.
     * Null elements are ignored. If the given array is null or does not contain at least one non-null
     * element, then {@link #NULL} is returned. If the array contains at least one element that cannot
     * be mapped to an enumeration value, then an empty value is returned.
     *
     * <h4>Accuracy</h4>
     * This method does not guarantee that conversions from the given classes to the returned type will be lossless.
     * For example, conversions form {@code long} to {@code double} are not lossless.
     * However, the results of those conversions should be reasonable approximations.
     *
     * @param  types  the primitive types or {@link Number} classes for which to get a common enumeration value.
     * @return the enumeration value for the given types. May be {@link #NULL} if all given values were null.
     */
    public static Optional<NumberType> forClasses(final Class<?>... types) {
        return Optional.ofNullable(forClasses(true, types));
    }

    /**
     * Returns an enumeration value capable to represent at least approximately all the given numeric classes.
     * This method is equivalent to <code>{@linkplain #forClasses(Class) forClass}(types).orElseThrow()</code>
     * except that the exception is an {@link IllegalArgumentException}. The exception is never thrown if all
     * the given classes are primitive types, their wrappers, or classes assignable to {@link Number}.
     *
     * @param  types  the primitive types or {@link Number} classes for which to get a common enumeration value.
     * @return the enumeration value for the given types. Never {@code null} but may be {@link #NULL}.
     * @throws IllegalArgumentException if one of the given classes cannot be mapped to any enumeration value.
     */
    public static NumberType forNumberClasses(final Class<?>... types) {
        return forClasses(false, types);
    }

    /**
     * Implementation of the public {@code forClasses(…)} and {@code forNumberClasses(…)} methods.
     *
     * @param  optional  whether to return {@code null} instead of throwing an exception.
     * @param  types     the primitive types or {@link Number} classes for which to get a common enumeration value.
     * @return the enumeration value for the given types, or {@code null}.
     */
    private static NumberType forClasses(final boolean optional, final Class<?>... types) {
        NumberType widest = VOID;
        boolean found = false;
        if (types != null) {
            for (final Class<?> type : types) {
                final NumberType other = forClass(type).orElse(null);
                if (other == null) {
                    if (optional) return null;
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumericalType_1, type));
                }
                switch (other) {
                    case NULL: continue;
                    case NUMBER: widest = other; continue;
                }
                found = true;
                if (widest.isNarrowerThan(other)) {
                    widest = other;
                }
            }
        }
        return found ? widest : NULL;
    }

    /**
     * Returns {@code true} if this type is an integer type.
     * Those types are {@link #BYTE}, {@link #SHORT}, {@link #INTEGER}, {@link #LONG} and {@link #BIG_INTEGER}.
     *
     * @return whether this type is an integer type.
     */
    public final boolean isInteger() {
        return category == 1;
    }

    /**
     * Returns {@code true} if the argument is one of the integer types known to <abbr>SIS</abbr>.
     * Those types are {@code byte}, {@link Byte}, {@code short}, {@link Short}, {@code int}, {@link Integer},
     * {@code long}, {@link Long} and {@link BigInteger}.
     *
     * <p>This method is mutually exclusive with {@link #isFractional()}:
     * only one of those two methods may return {@code true}.
     * However, both methods may return {@code false}.</p>
     *
     * @param  type  the primitive type or {@link Number} subclass to test (can be {@code null}).
     * @return {@code true} if {@code type} is one of the known types used to represent integers.
     */
    public static boolean isInteger(final Class<?> type) {
        final NumberType value = valueOrNull(type);
        return (value != null) && value.isInteger();
    }

    /**
     * Returns {@code true} if this type is capable to store fractional digits.
     * Those types are {@link #FRACTION}, {@link #FLOAT}, {@link #DOUBLE} and {@link #BIG_DECIMAL}.
     * Floating point types are considered as types or rational numbers where the denominator is a power of 2 or 10.
     *
     * <p>This method is mutually exclusive with {@link #isInteger()}:
     * only one of those two methods may return {@code true}.
     * However, both methods may return {@code false}.</p>
     *
     * @return whether this type is a rational or floating point type.
     */
    public final boolean isFractional() {
        return category == 2;
    }

    /**
     * Returns {@code true} if the argument is one of the types known to <abbr>SIS</abbr>
     * as capable to store fractional digits. This method returns {@code true} for the following types:
     * {@link Fraction}, {@code float}, {@link Float}, {@code double}, {@link Double} and {@link BigDecimal}.
     *
     * @param  type  the primitive type or {@link Number} subclass to test (can be {@code null}).
     * @return {@code true} if {@code type} is one of the known types capable to store fractional digits.
     */
    public static boolean isFractional(final Class<?> type) {
        final NumberType value = valueOrNull(type);
        return (value != null) && value.isFractional();
    }

    /**
     * Returns {@code true} if this type is any type of real numbers. This method returns {@code true}
     * for all {@linkplain #isInteger() integer} and {@linkplain #isFractional() fractional} types.
     *
     * @return whether this type is any type of real numbers.
     */
    public final boolean isReal() {
        return category != 0;
    }

    /**
     * Returns whether this type supports the {@code cast(…)} and {@code wrap(…)} methods.
     * This is true for all {@linkplain #isReal() real numbers} except {@link #NUMBER}.
     *
     * @return whether this type supports the {@code cast(…)} and {@code wrap(…)} methods.
     */
    public final boolean isConvertible() {
        return category == 1 || category == 2;
    }

    /**
     * Returns {@code true} if the argument is one of the real number types known to <abbr>SIS</abbr>.
     * This method returns {@code true} for all {@linkplain #isInteger(Class) integer}
     * and {@linkplain #isFractional(Class) fractional} types.
     *
     * @param  type  the primitive type or {@link Number} subclass to test (can be {@code null}).
     * @return {@code true} if {@code type} is one of the known types used to represent real numbers.
     */
    public static boolean isReal(final Class<?> type) {
        final NumberType value = valueOrNull(type);
        return (value != null) && value.isReal();
    }

    /**
     * Returns whether the ordinal value is between the given boundaries. Both bounds are inclusive.
     * If {@code lower} ≥ {@code upper}, then this method always returns {@code false}.
     *
     * @param  lower  the lower bound, inclusive.
     * @param  upper  the upper bound, inclusive.
     * @return whether this enumeration value is between the given bounds.
     */
    final boolean isBetween(final NumberType lower, final NumberType upper) {
        return ordinal() >= lower.ordinal() && ordinal() <= upper.ordinal();
    }

    /**
     * Returns whether this type is considered narrower than the specified type.
     * If one of the type is unknown, then this method conservatively returns {@code false}.
     *
     * <h4>Accuracy</h4>
     * Note that conversions from the narrowest class are not guaranteed to be lossless.
     * For example, {@code long} is considered a narrower class than {@code double},
     * but conversion from {@code long} to {@code double} can nevertheless loose accuracy.
     * However, if this method returns {@code true}, then conversions to {@code other} type
     * should produce at least a reasonable approximation of the original value.
     *
     * @param  other  the other type.
     * @return whether this type is considered narrower than the specified type.
     */
    public boolean isNarrowerThan(final NumberType other) {
        if (other == CHARACTER) {
            return other.isWiderThan(this);
        }
        return other.ordinal() < NUMBER.ordinal()
                && ordinal() < other.ordinal()
                && category <= other.category;  // Integers considered narrower than floating-point types.
    }

    /**
     * Returns whether this type is considered wider than the specified type.
     * If one of the type is unknown, then this method conservatively returns {@code false}.
     *
     * <h4>Accuracy</h4>
     * Note that conversions to the widest class are not guaranteed to be lossless.
     * For example, {@code double} is considered a wider class than {@code long}, but
     * conversion from {@code long} to {@code double} can nevertheless loose accuracy.
     * However, if this method returns {@code true}, then conversions to this type
     * should produce at least a reasonable approximation of the original value.
     *
     * @param  other  the other type.
     * @return whether this type is considered wider than the specified type.
     */
    public boolean isWiderThan(final NumberType other) {
        if (other == CHARACTER) {
            return other.isNarrowerThan(this);
        }
        return ordinal() < NUMBER.ordinal()
                && ordinal() > other.ordinal()
                && category >= other.category;  // Floating-point types considered wider than integers.
    }

    /**
     * Returns whether conversion of values from this type to the specified type would be lossless.
     * If this type supports negative zero and NaN values, then the target type must also supports
     * these values for the conversion to be considered lossless.
     *
     * @param  target  the target type.
     * @return whether conversions to the target type would be lossless, including negative zero and NaN.
     */
    public boolean isConversionLossless(final NumberType target) {
        return target == this || isNarrowerThan(target);
    }

    /**
     * Returns the primitive or wrapper class represented by this enumeration value.
     * If there is no primitive type, then this method always returns the class.
     * The returned class is null in the particular case of the {@link #NULL} type,
     * and non-null for all other types.
     *
     * @param  primitive  {@code true} for the primitive type, or {@code false} for the wrapper class.
     * @return the primitive or wrapper class represented by this enumeration value, or {@code null}.
     *
     * @see Class#isPrimitive()
     */
    public final Class<?> classOfValues(final boolean primitive) {
        return primitive ? this.primitive : wrapper;
    }

    /**
     * Changes a primitive class to its wrapper (for example {@code int} to {@link Integer}).
     * If the specified class is not a primitive type, then it is returned unchanged.
     *
     * @param  <N>   the primitive and wrapper type (both have the same parametric declaration).
     * @param  type  the primitive type (can be {@code null}).
     * @return the type as a wrapper.
     *
     * @see #wrapperToPrimitive(Class)
     * @see Class#isPrimitive()
     */
    @SuppressWarnings("unchecked")
    public static <N> Class<N> primitiveToWrapper(final Class<N> type) {
        final NumberType value = valueOrNull(type);
        return (value != null) ? (Class<N>) value.wrapper : type;
    }

    /**
     * Changes a wrapper class to its primitive (for example {@link Integer} to {@code int}).
     * If the specified class is not a wrapper type, then it is returned unchanged.
     *
     * @param  <N>   the primitive and wrapper type (both have the same parametric declaration).
     * @param  type  the wrapper type (can be {@code null}).
     * @return the type as a primitive.
     *
     * @see #primitiveToWrapper(Class)
     * @see Class#isPrimitive()
     */
    @SuppressWarnings("unchecked")
    public static <N> Class<N> wrapperToPrimitive(final Class<N> type) {
        final NumberType value = valueOrNull(type);
        return (value != null) ? (Class<N>) value.primitive : type;
    }

    /**
     * Returns the number of bits used in the representation of this type.
     * This size is accurate for values objects such as primitive types.
     * For other objects, the returned size does not include the space
     * occupied by object headers and object references.
     *
     * @return the number of bits, or empty if the size is unknown or variable.
     */
    public final OptionalInt size() {
        return (size != -1) ? OptionalInt.of(Byte.toUnsignedInt(size)) : OptionalInt.empty();
    }

    /**
     * Verifies that the given number contains the expected value.
     * This is a helper method for {@code wrapExact(…)} implementations.
     *
     * @param  number    the wrapped value.
     * @param  expected  the value which has been wrapped.
     * @throws ArithmeticException if the given number does not contain the expected value.
     */
    final Number verify(final Number number, final long expected) {
        if (number.longValue() == expected) {
            return number;
        }
        throw new ArithmeticException(Errors.format(Errors.Keys.CanNotConvertValue_2, expected, wrapper));
    }

    /**
     * Verifies that the given number contains the expected value.
     * This is a helper method for {@code wrapExact(…)} implementations.
     * This method does not distinguish positive zero from negative zero and the various NaN values.
     *
     * @param  number    the wrapped value.
     * @param  expected  the value which has been wrapped.
     * @throws ArithmeticException if the given number does not contain the expected value.
     */
    final Number verify(final Number number, final double expected) {
        final double unwrap = number.doubleValue();
        if (unwrap == expected || (Double.isNaN(unwrap) && Double.isNaN(expected))) {
            return number;
        }
        throw new ArithmeticException(Errors.format(Errors.Keys.CanNotConvertValue_2, expected, wrapper));
    }

    /**
     * Casts a value to the specified type without checking whether is would cause precision lost.
     * This method makes the following choice:
     *
     * <ul>
     *   <li>If this type is {@link #DOUBLE}, then this method returns
     *       <code>{@linkplain Double#valueOf(double) Double.valueOf}(number.doubleValue())</code>;</li>
     *   <li>Otherwise if this type is {@link #FLOAT}, then this method returns
     *       <code>{@linkplain Float#valueOf(float) Float.valueOf}(number.floatValue())</code>;</li>
     *   <li>And likewise for other types in this enumeration.</li>
     * </ul>
     *
     * This method does not verify if this type is wide enough for the given number,
     * If this type is not wide enough, then the behavior depends on the implementation of the corresponding
     * {@code Number.fooValue()} method - typically, the value is just rounded or truncated.
     *
     * @param  number  the number to cast, or {@code null}.
     * @return the number cast to this type, or {@code null} if the given number was null.
     * @throws UnsupportedOperationException if this type cannot cast numbers.
     * @throws NullPointerException if {@code number} is null.
     * @throws IllegalArgumentException if the number cannot be converted to this type
     *         (e.g., {@link Double#NaN} cannot be converted to {@link BigDecimal}).
     *
     * @see org.apache.sis.util.Numbers#cast(Number, Class)
     */
    public Number cast(Number number) {
        if (number == null) {
            return number;
        }
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnknownType_1, this));
    }

    /**
     * Wraps the given floating-point value in an object of the type represented by this enumeration value.
     * The returned value is an instance of the wrapper class returned by
     * <code>{@link #classOfValues(boolean) classOfValues}(false)</code>.
     * The given value shall be convertible to that type without precision lost,
     * otherwise an {@link ArithmeticException} will be thrown.
     *
     * <p>This method does not guaranteed that the sign of zero is preserved
     * (i.e., negative zero is considered equivalent to positive zero)
     * and does not guaranteed that the various NaN values are preserved
     * (i.e., NaN may be replaced by the canonical {@link Double#NaN}).</p>
     *
     * @param  number  the number to wrap.
     * @return the value wrapped in an object of the wrapper class represented by this enumeration value.
     * @throws UnsupportedOperationException if this type cannot wrap numbers.
     * @throws ArithmeticException if the given value cannot be converted without precision lost.
     *
     * @see org.apache.sis.util.Numbers#wrap(double, Class)
     */
    public Number wrapExact(final double number) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.CanNotConvertValue_2, number, wrapper));
    }

    /**
     * Wraps the given integer value in an object of the type represented by this enumeration value.
     * The returned value is an instance of the wrapper class returned by
     * <code>{@link #classOfValues(boolean) classOfValues}(false)</code>.
     * The given value shall be convertible to that type without precision lost,
     * otherwise an {@link ArithmeticException} will be thrown.
     *
     * @param  number  the number to wrap.
     * @return the value wrapped in an object of the wrapper class represented by this enumeration value.
     * @throws UnsupportedOperationException if this type cannot wrap numbers.
     * @throws ArithmeticException if the given value cannot be converted without precision lost.
     *
     * @see org.apache.sis.util.Numbers#wrap(long, Class)
     */
    public Number wrapExact(final long number) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.CanNotConvertValue_2, number, wrapper));
    }

    /**
     * Converts the specified string into a value object.
     * The returned value is an instance of the wrapper class returned by
     * <code>{@link #classOfValues(boolean) classOfValues}(false)</code>.
     *
     * <ul>
     *   <li>If this type is {@link #DOUBLE}, then this method returns
     *       <code>{@linkplain Double#valueOf(String) Double.valueOf}(value)</code>;</li>
     *   <li>If this type is {@link #FLOAT}, then this method returns
     *       <code>{@linkplain Float#valueOf(String) Float.valueOf}(value)</code>;</li>
     *   <li>And likewise for all other known types.</li>
     * </ul>
     *
     * @param  text  the value to parse.
     * @return the value object.
     * @throws UnsupportedOperationException if this type does not support parsing.
     * @throws NullPointerException if {@code text} is null.
     * @throws IllegalArgumentException if {@code text} is not parsable as a value of this type.
     *
     * @see org.apache.sis.util.Numbers#valueOf(String, Class)
     */
    public Comparable<?> parse(String text) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnparsableStringForClass_2, wrapper, text));
    }

    /**
     * Returns the closest value that can be interpreted as <q>no data</q>, which is not the same as zero.
     * This method returns {@code NaN}, {@code 0} or {@code false} or {@code null}, in this preference order.
     * The returned value is an instance of the wrapper class returned by
     * <code>{@link #classOfValues(boolean) classOfValues}(false)</code>.
     *
     * @return the {@code NaN}, {@code 0} or {@code false} or {@code null} value, in this preference order.
     *
     * @see org.apache.sis.util.Numbers#valueOfNil(Class)
     */
    public Comparable<?> nilValue() {
        // Defined in subclasses rather than stored in a field for deferrred class loading.
        return null;
    }
}
