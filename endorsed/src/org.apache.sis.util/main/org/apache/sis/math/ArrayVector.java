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

import java.io.Serializable;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.IntSupplier;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.measure.NumberRange;


/**
 * A vector backed by an array of a primitive type. This class does not copy the array,
 * so changes in the underlying array is reflected in this vector and vice-versa.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 *
 * @param <E>  the base type of elements in the vector.
 */
abstract class ArrayVector<E extends Number> extends Vector implements CheckedContainer<E>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3496467575389288163L;

    /**
     * For sub-classes constructor.
     */
    ArrayVector() {
    }

    /**
     * Creates a new instance.
     *
     * @throws IllegalArgumentException if the type of the given object is not recognized by the method.
     */
    static Vector newInstance(final Object array, final boolean isUnsigned) throws IllegalArgumentException {
        final Vector vec;
        if (array instanceof double[]) {
            vec = new Doubles((double[]) array);
        } else if (array instanceof float[]) {
            vec = new Floats((float[]) array);
        } else if (array instanceof long[]) {
            vec = isUnsigned ? new UnsignedLongs((long[]) array)
                             : new         Longs((long[]) array);
        } else if (array instanceof int[]) {
            vec = isUnsigned ? new UnsignedIntegers((int[]) array)
                             : new         Integers((int[]) array);
        } else if (array instanceof short[]) {
            vec = isUnsigned ? new UnsignedShorts((short[]) array)
                             : new         Shorts((short[]) array);
        } else if (array instanceof byte[]) {
            vec = isUnsigned ? new UnsignedBytes((byte[]) array)
                             : new         Bytes((byte[]) array);
        } else if (array instanceof Number[]) {
            vec = new Raw((Number[]) array);
        } else if (array instanceof String[]) {
            vec = new ASCII((String[]) array);
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentClass_2, "array", Classes.getClass(array)));
        }
        return vec;
    }

    /**
     * Returns a vector with the same data as the given vector but encoded in a more compact way,
     * or {@code null} if this method cannot do better than the given {@code Vector} instance.
     * This method shall be invoked only for vector of integer values (this is not verified).
     */
    static Vector compress(final Vector source, final long min, final long max) {
        boolean isSigned = (min >= Byte.MIN_VALUE && max <= Byte.MAX_VALUE);
        if (isSigned || (min >= 0 && max <= 0xFF)) {
            if (source instanceof Bytes) return null;
            final byte[] array = new byte[source.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = (byte) source.intValue(i);
            }
            return isSigned ? new Bytes(array) : new UnsignedBytes(array);
        }
        isSigned = (min >= Short.MIN_VALUE && max <= Short.MAX_VALUE);
        if (isSigned || (min >= 0 && max <= 0xFFFF)) {
            if (source instanceof Shorts) return null;
            final short[] array = new short[source.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = (short) source.intValue(i);
            }
            return isSigned ? new Shorts(array) : new UnsignedShorts(array);
        }
        isSigned = (min >= Integer.MIN_VALUE && max <= Integer.MAX_VALUE);
        if (isSigned || (min >= 0 && max <= 0xFFFFFFFF)) {
            if (source instanceof Integers) return null;
            final int[] array = new int[source.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = (int) source.longValue(i);
            }
            return isSigned ? new Integers(array) : new UnsignedIntegers(array);
        }
        if (!(source instanceof Longs) && !(source instanceof Floats) && !(source instanceof Doubles)) {
            final long[] array = new long[source.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = source.longValue(i);
            }
            return new Longs(array);
        }
        return null;
    }

    /**
     * Returns a vector with the same data as the given vector but encoded in a more compact way,
     * or {@code null} if this method cannot do better than the given {@code Vector} instance.
     * This method shall be invoked only for vector of floating point values (this is not verified).
     */
    static Vector compress(final Vector source, final double tolerance) {
        if (source.getElementType() != Float.class) {
            /*
             * For floating point types, verify if values are equivalent to `float` values.
             * There are two different ways to pad extra fraction digits in `double` values:
             * with zero fraction digits in base 2 representation (the standard Java cast),
             * or with zero fraction digits in base 10 representation.
             */
            final int length = source.size();
            int i = 0;
            double v;
            do if (i >= length) {
                return new Floats(source.floatValues());
            } while (!(Math.abs((v = source.doubleValue(i++)) - (float) v) > tolerance));    // Use `!` for accepting NaN.
            /*
             * Same try as above loop, but now using base 10 representation.
             * This is a more costly computation.
             */
            i = 0;
            do if (i >= length) {
                return new Decimal(source.floatValues());
            } while (!(Math.abs((v = source.doubleValue(i++)) - DecimalFunctions.floatToDouble((float) v)) > tolerance));
        }
        return null;
    }

    /**
     * Indicates that this vector is modifiable.
     *
     * @return {@link Mutability#MODIFIABLE}.
     */
    @Override
    public final Mutability getMutability() {
        return Mutability.MODIFIABLE;
    }

    /**
     * Default implementation for the convenience of wrapper of integer types.
     */
    @Override
    public boolean isNaN(int index) {
        return false;
    }

    /**
     * Verifies that the given value can be cast to the expected type.
     */
    void verifyType(final Number value, final NumberType expected) {
        final Class<? extends Number> type = value.getClass();
        final NumberType t = NumberType.forNumberClass(type);
        if (t.isWiderThan(expected)) {
            throw new ClassCastException(Errors.format(Errors.Keys.CanNotConvertFromType_2, type, expected.primitive));
        }
    }

    /**
     * A vector backed by an array of type {@code double[]}.
     */
    static final class Doubles extends ArrayVector<Double> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -2900375382498345812L;

        /** The backing array. */
        private final double[] array;

        /** Creates a new vector for the given array. */
        Doubles(final double[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public Class<Double> getElementType() {
            return Double.class;
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public boolean isSinglePrecision() {
            return false;
        }

        /** Returns the length of the backing array. */
        @Override public int size() {
            return array.length;
        }

        /** Returns {@code true} if the value at the given index is {@code NaN}. */
        @Override public boolean isNaN(final int index) {
            return Double.isNaN(array[index]);
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Double.toString(array[index]);
        }

        /** Returns the value at the given index. */
        @Override public double doubleValue(final int index) {
            return array[index];
        }

        /**
         * Returns the value cast as a {@code float}, since we may loose precision but the
         * result of the cast is not completely wrong (at worst we get zero of infinity values
         * if the magnitude of the {@code double} value was too small or too large).
         */
        @Override public float floatValue(int index) {
            return (float) array[index];
        }

        /** Returns the value at the given index. */
        @Override public Number get(final int index) {
            return array[index];
        }

        /** Sets the value at the given index. */
        @Override public Number set(final int index, final Number value) {
            final double old = array[index];
            array[index] = value.doubleValue();
            modCount++;
            return old;
        }

        /** Sets the value of all elements in the given range. */
        @Override public void fill(final int fromIndex, final int toIndex, final Number value) {
            Arrays.fill(array, fromIndex, toIndex, value.doubleValue());
            modCount++;
        }

        /** Finds index of a match or mismatch (depending on {@code equality}). */
        @Override int indexOf(final int toSearch, int index, final boolean equality) {
            final long first = Double.doubleToLongBits(array[toSearch]);
            while (index < array.length && (first == Double.doubleToLongBits(array[index])) != equality) index++;
            return index;
        }

        /** Returns {@code true} if this vector is empty or contains only {@code NaN} values. */
        @Override public boolean isEmptyOrNaN() {
            for (final double v : array) {
                if (!Double.isNaN(v)) return false;
            }
            return true;
        }

        /** Returns whether this vector in the given range is equal to the specified vector. */
        @Override boolean equals(final int lower, final int upper, final Vector other, final int otherOffset) {
            if (other instanceof Doubles) {
                return Arrays.equals(array, lower, upper,
                        ((Doubles) other).array, otherOffset, otherOffset + (upper - lower));
            }
            return super.equals(lower, upper, other, otherOffset);
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<Double> range(final IntSupplier indices, int n) {
            // TODO: try to paralellize with streams.
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            while (--n >= 0) {
                final double value = array[(indices != null) ? indices.getAsInt() : n];
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }

        /** Wraps this vector in a buffer. */
        @Override public Optional<Buffer> buffer() {
            return Optional.of(DoubleBuffer.wrap(array));
        }

        /** Returns a copy of current data as a floating point array. */
        @Override public double[] doubleValues() {
            return array.clone();
        }

        /** Returns a copy of current data as a floating point array. */
        @Override public float[] floatValues() {
            return ArraysExt.copyAsFloats(array);
        }

        /** Applies hash code contract specified {@link Vector#hashCode()}. */
        @Override public int hashCode() {
            return Arrays.hashCode(array);
        }
    }

    /**
     * A vector backed by an array of type {@code float[]}. In this class, conversions to the {@code double} type
     * use the standard Java cast operator  (i.e. the {@code double} value is padded with zero fraction digits in
     * its base 2 representation). The {@code ArrayVector.Decimal} subclass overrides this behavior with a more
     * costly cast that tries to preserve the representation in base 10.
     */
    private static class Floats extends ArrayVector<Float> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 5395284704294981455L;

        /** The backing array. */
        private final float[] array;

        /** Creates a new vector for the given array. */
        Floats(final float[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<Float> getElementType() {
            return Float.class;
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public boolean isSinglePrecision() {
            return true;
        }

        /** Returns the length of the backing array. */
        @Override public final int size() {
            return array.length;
        }

        /** Returns {@code true} if the value at the given index is {@code NaN}. */
        @Override public final boolean isNaN(final int index) {
            return Float.isNaN(array[index]);
        }

        /** Returns the string representation at the given index. */
        @Override public final String stringValue(final int index) {
            return Float.toString(array[index]);
        }

        /** Returns the value at the given index. */
        @Override public       double doubleValue(int index) {return array[index];}
        @Override public final float   floatValue(int index) {return array[index];}
        @Override public       Number         get(int index) {return array[index];}

        /** Sets the value at the given index. */
        @Override public final Number set(final int index, final Number value) {
            final float old = array[index];
            array[index] = value.floatValue();
            modCount++;
            return old;
        }

        /** Sets the value of all elements in the given range. */
        @Override public void fill(final int fromIndex, final int toIndex, final Number value) {
            Arrays.fill(array, fromIndex, toIndex, value.floatValue());
            modCount++;
        }

        /** Finds index of a match or mismatch (depending on {@code equality}). */
        @Override final int indexOf(final int toSearch, int index, final boolean equality) {
            final int first = Float.floatToIntBits(array[toSearch]);
            while (index < array.length && (first == Float.floatToIntBits(array[index])) != equality) index++;
            return index;
        }

        /** Returns {@code true} if this vector is empty or contains only {@code NaN} values. */
        @Override public final boolean isEmptyOrNaN() {
            for (final float v : array) {
                if (!Float.isNaN(v)) return false;
            }
            return true;
        }

        /** Returns whether this vector in the given range is equal to the specified vector. */
        @Override final boolean equals(final int lower, final int upper, final Vector other, final int otherOffset) {
            if (other.getClass() == getClass()) {
                return Arrays.equals(array, lower, upper,
                        ((Floats) other).array, otherOffset, otherOffset + (upper - lower));
            }
            return super.equals(lower, upper, other, otherOffset);
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override final NumberRange<?> range(final IntSupplier indices, int n) {
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            while (--n >= 0) {
                final float value = array[(indices != null) ? indices.getAsInt() : n];
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return createRange(min, max);
        }

        /**
         * Creates a range from the given minimum and maximum values, inclusive.
         * The default implementation creates a range of {@code float}, but this method is
         * overridden by {@code ArrayVector.Decimal} which create a range of {@code double}.
         */
        NumberRange<?> createRange(final float min, final float max) {
            return NumberRange.create(min, true, max, true);
        }

        /** Wraps this vector in a buffer. */
        @Override public final Optional<Buffer> buffer() {
            return Optional.of(FloatBuffer.wrap(array));
        }

        /** Returns a copy of current data as a floating point array. */
        @Override public final float[] floatValues() {
            return array.clone();
        }

        /** Applies hash code contract specified {@link Vector#hashCode()}. */
        @Override public int hashCode() {
            return Arrays.hashCode(array);
        }
    }

    /**
     * A vector backed by an array of type {@code float[]} to be converted to {@code double} in a way that minimizes
     * the errors when represented in base 10. This implementation should be used only when there is good reasons to
     * believe that the {@code float} data where encoded in base 10 in the first place (for example in an ASCII file).
     */
    static final class Decimal extends Floats {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 6085386820455858377L;

        /** Creates a new vector for the given array. */
        Decimal(final float[] array) {
            super(array);
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public boolean isSinglePrecision() {
            return false;
        }

        /** Returns the value at the given index. */
        @Override public double doubleValue(final int index) {
            return DecimalFunctions.floatToDouble(super.floatValue(index));
        }

        /** Returns the value at the given index. */
        @Override public Number get(final int index) {
            return doubleValue(index);
        }

        /** Creates a range from the given minimum and maximum values. */
        @Override NumberRange<?> createRange(final float min, final float max) {
            return NumberRange.create(DecimalFunctions.floatToDouble(min), true,
                                      DecimalFunctions.floatToDouble(max), true);
        }

        /** Applies hash code contract specified {@link Vector#hashCode()}. */
        @Override public int hashCode() {
            int hash = 1;
            final int size = size();
            for (int i=0; i<size; i++) {
                hash = PRIME * hash + Double.hashCode(doubleValue(i));
            }
            return hash;
        }
    }

    /**
     * A vector backed by an array of type {@code long[]}. This class handles signed values.
     * The {@code ArrayVector.UnsignedLongs} subclass handle unsigned {@code long} values.
     */
    private static class Longs extends ArrayVector<Long> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 338413429037224587L;

        /** The backing array. */
        private final long[] array;

        /** Creates a new vector for the given array. */
        Longs(final long[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<Long> getElementType() {
            return Long.class;
        }

        /** Returns whether this vector is empty. */
        @Override public final boolean isEmptyOrNaN() {
            return array.length == 0;
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public final boolean isSinglePrecision() {
            return false;
        }

        /** Values in this vector are guaranteed to be integers. */
        @Override public final boolean isInteger() {
            return true;
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Long.toString(array[index]);
        }

        @Override public final int     size()          {return array.length;}
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public long     longValue(int index) {return array[index];}
        @Override public final Number   get(int index) {return longValue(index);}
        @Override public final Number   set(int index, final Number value) {
            verifyType(value, NumberType.LONG);
            final Number old = get(index);
            array[index] = value.longValue();
            modCount++;
            return old;
        }

        /** Sets the value of all elements in the given range. */
        @Override public void fill(final int fromIndex, final int toIndex, final Number value) {
            verifyType(value, NumberType.LONG);
            Arrays.fill(array, fromIndex, toIndex, value.longValue());
            modCount++;
        }

        /** Finds index of a match or mismatch (depending on {@code equality}). */
        @Override final int indexOf(final int toSearch, int index, final boolean equality) {
            final long first = array[toSearch];
            while (index < array.length && (first == array[index]) != equality) index++;
            return index;
        }

        /** Returns whether this vector in the given range is equal to the specified vector. */
        @Override final boolean equals(final int lower, final int upper, final Vector other, final int otherOffset) {
            if (other.getClass() == getClass()) {
                return Arrays.equals(array, lower, upper,
                        ((Longs) other).array, otherOffset, otherOffset + (upper - lower));
            }
            return super.equals(lower, upper, other, otherOffset);
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<?> range(final IntSupplier indices, int n) {
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            while (--n >= 0) {
                final long value = array[(indices != null) ? indices.getAsInt() : n];
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }

        /**
         * Returns the increment between values if this increment is constant, or {@code null} otherwise.
         * Addition or subtraction of unsigned integers are bitwise identical to the same operations on
         * signed integers. Consequently, we do not need to distinguish the two cases during the loop.
         */
        @Override public final Number increment(final double tolerance) {
            if (!(tolerance >= 0 && tolerance < 1)) {                       // Use `!` for catching NaN.
                return super.increment(tolerance);
            }
            int i = array.length;
            if (i >= 2) {
                long p;
                final long inc;
                try {
                    inc = subtract(array[--i], p = array[--i]);
                } catch (ArithmeticException e) {
                    warning("increment", e);
                    return null;
                }
                while (i != 0) {
                    if (p - (p = array[--i]) != inc) {
                        return null;
                    }
                }
                return inc;
            }
            return null;
        }

        /** Wraps this vector in a buffer. */
        @Override public final Optional<Buffer> buffer() {
            return Optional.of(LongBuffer.wrap(array));
        }

        /** Applies hash code contract specified {@link Vector#hashCode()}. */
        @Override public final int hashCode() {
            return Arrays.hashCode(array);
        }
    }

    /**
     * A vector backed by an array of type {@code int[]}. This class handles signed values.
     * The {@code ArrayVector.UnsignedIntegers} subclass handle unsigned {@code long} values.
     */
    private static class Integers extends ArrayVector<Integer> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -1292641147544275801L;

        /** The backing array. */
        private final int[] array;

        /** Creates a new vector for the given array. */
        Integers(final int[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<Integer> getElementType() {
            return Integer.class;
        }

        /** Returns whether this vector is empty. */
        @Override public final boolean isEmptyOrNaN() {
            return array.length == 0;
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public final boolean isSinglePrecision() {
            return false;
        }

        /** Values in this vector are guaranteed to be integers. */
        @Override public final boolean isInteger() {
            return true;
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Integer.toString(array[index]);
        }

        @Override public final int     size()          {return array.length;}
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public long     longValue(int index) {return array[index];}
        @Override public int       intValue(int index) {return array[index];}
        @Override public Number         get(int index) {return array[index];}
        @Override public final Number   set(int index, final Number value) {
            verifyType(value, NumberType.INTEGER);
            final Number old = get(index);
            array[index] = value.intValue();
            modCount++;
            return old;
        }

        /** Sets the value of all elements in the given range. */
        @Override public void fill(final int fromIndex, final int toIndex, final Number value) {
            verifyType(value, NumberType.INTEGER);
            Arrays.fill(array, fromIndex, toIndex, value.intValue());
            modCount++;
        }

        /** Finds index of a match or mismatch (depending on {@code equality}). */
        @Override final int indexOf(final int toSearch, int index, final boolean equality) {
            final int first = array[toSearch];
            while (index < array.length && (first == array[index]) != equality) index++;
            return index;
        }

        /** Returns whether this vector in the given range is equal to the specified vector. */
        @Override final boolean equals(final int lower, final int upper, final Vector other, final int otherOffset) {
            if (other.getClass() == getClass()) {
                return Arrays.equals(array, lower, upper,
                        ((Integers) other).array, otherOffset, otherOffset + (upper - lower));
            }
            return super.equals(lower, upper, other, otherOffset);
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<?> range(final IntSupplier indices, int n) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            while (--n >= 0) {
                final int value = array[(indices != null) ? indices.getAsInt() : n];
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }

        /**
         * Returns the increment between values if this increment is constant, or {@code null} otherwise.
         * Addition or subtraction of unsigned integers are bitwise identical to the same operations on
         * signed integers. Consequently, we do not need to distinguish the two cases during the loop.
         */
        @Override public final Number increment(final double tolerance) {
            if (!(tolerance >= 0 && tolerance < 1)) {                       // Use `!` for catching NaN.
                return super.increment(tolerance);
            }
            int i = array.length;
            if (i >= 2) {
                final long inc = longValue(--i) - longValue(--i);
                final boolean isSigned = (inc >= Integer.MIN_VALUE && inc <= Integer.MAX_VALUE);
                if (isSigned || isUnsigned()) {             // Check against overflow.
                    final int asInt = (int) inc;
                    int p = array[i];
                    while (i != 0) {
                        if (p - (p = array[--i]) != asInt) {
                            return null;
                        }
                    }
                    // Do not use the ?: operator below since it casts `asInt` to Long, which is not wanted.
                    if (isSigned) {
                        return asInt;
                    } else {
                        return inc;
                    }
                }
            }
            return null;
        }

        /** Wraps this vector in a buffer. */
        @Override public final Optional<Buffer> buffer() {
            return Optional.of(IntBuffer.wrap(array));
        }

        /** Applies hash code contract specified {@link Vector#hashCode()}. */
        @Override public final int hashCode() {
            return Arrays.hashCode(array);
        }
    }

    /**
     * A vector backed by an array of type {@code short[]}. This class handles signed values.
     * The {@code ArrayVector.UnsignedShorts} subclass handle unsigned {@code long} values.
     */
    private static class Shorts extends ArrayVector<Short> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -126825963332296000L;

        /** The backing array. */
        private final short[] array;

        /** Creates a new vector for the given array. */
        Shorts(final short[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<Short> getElementType() {
            return Short.class;
        }

        /** Returns whether this vector is empty. */
        @Override public final boolean isEmptyOrNaN() {
            return array.length == 0;
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public final boolean isSinglePrecision() {
            return true;
        }

        /** Values in this vector are guaranteed to be integers. */
        @Override public final boolean isInteger() {
            return true;
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Short.toString(array[index]);
        }

        @Override public final int     size()          {return array.length;}
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public long     longValue(int index) {return array[index];}
        @Override public int       intValue(int index) {return array[index];}
        @Override public short   shortValue(int index) {return array[index];}
        @Override public Number         get(int index) {return array[index];}
        @Override public final Number   set(int index, final Number value) {
            verifyType(value, NumberType.SHORT);
            final Number old = get(index);
            array[index] = value.shortValue();
            modCount++;
            return old;
        }

        /** Sets the value of all elements in the given range. */
        @Override public void fill(final int fromIndex, final int toIndex, final Number value) {
            verifyType(value, NumberType.SHORT);
            Arrays.fill(array, fromIndex, toIndex, value.shortValue());
            modCount++;
        }

        /** Finds index of a match or mismatch (depending on {@code equality}). */
        @Override final int indexOf(final int toSearch, int index, final boolean equality) {
            final short first = array[toSearch];
            while (index < array.length && (first == array[index]) != equality) index++;
            return index;
        }

        /** Returns whether this vector in the given range is equal to the specified vector. */
        @Override final boolean equals(final int lower, final int upper, final Vector other, final int otherOffset) {
            if (other.getClass() == getClass()) {
                return Arrays.equals(array, lower, upper,
                        ((Shorts) other).array, otherOffset, otherOffset + (upper - lower));
            }
            return super.equals(lower, upper, other, otherOffset);
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<?> range(final IntSupplier indices, int n) {
            short min = Short.MAX_VALUE;
            short max = Short.MIN_VALUE;
            while (--n >= 0) {
                final short value = array[(indices != null) ? indices.getAsInt() : n];
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }

        /*
         * Not worth to override `increment(double)` because the array cannot be long anyway
         * (except if the increment is zero) and the implicit conversion of `short` to `int`
         * performed by Java would make the implementation a little bit more tricky.
         */

        /** Wraps this vector in a buffer. */
        @Override public final Optional<Buffer> buffer() {
            return Optional.of(ShortBuffer.wrap(array));
        }

        /** Applies hash code contract specified {@link Vector#hashCode()}. */
        @Override public final int hashCode() {
            return Arrays.hashCode(array);
        }
    }

    /**
     * A vector backed by an array of type {@code byte[]}. This class handles signed values.
     * The {@code ArrayVector.UnsignedBytes} subclass handle unsigned {@code long} values.
     */
    private static class Bytes extends ArrayVector<Byte> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 7933568876180528548L;

        /** The backing array. */
        private final byte[] array;

        /** Creates a new vector for the given array. */
        Bytes(final byte[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<Byte> getElementType() {
            return Byte.class;
        }

        /** Returns whether this vector is empty. */
        @Override public final boolean isEmptyOrNaN() {
            return array.length == 0;
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public final boolean isSinglePrecision() {
            return true;
        }

        /** Values in this vector are guaranteed to be integers. */
        @Override public final boolean isInteger() {
            return true;
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Byte.toString(array[index]);
        }

        @Override public final int     size()          {return array.length;}
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public long     longValue(int index) {return array[index];}
        @Override public int       intValue(int index) {return array[index];}
        @Override public short   shortValue(int index) {return array[index];}
        @Override public byte     byteValue(int index) {return array[index];}
        @Override public Number         get(int index) {return array[index];}
        @Override public final Number   set(int index, final Number value) {
            verifyType(value, NumberType.BYTE);
            final Number old = get(index);
            array[index] = value.byteValue();
            modCount++;
            return old;
        }

        /** Sets the value of all elements in the given range. */
        @Override public void fill(final int fromIndex, final int toIndex, final Number value) {
            verifyType(value, NumberType.BYTE);
            Arrays.fill(array, fromIndex, toIndex, value.byteValue());
            modCount++;
        }

        /** Finds index of a match or mismatch (depending on {@code equality}). */
        @Override final int indexOf(final int toSearch, int index, final boolean equality) {
            final byte first = array[toSearch];
            while (index < array.length && (first == array[index]) != equality) index++;
            return index;
        }

        /** Returns whether this vector in the given range is equal to the specified vector. */
        @Override final boolean equals(int lower, final int upper, final Vector other, int otherOffset) {
            if (other.getClass() == getClass()) {
                return Arrays.equals(array, lower, upper,
                        ((Bytes) other).array, otherOffset, otherOffset + (upper - lower));
            }
            return super.equals(lower, upper, other, otherOffset);
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<?> range(final IntSupplier indices, int n) {
            byte min = Byte.MAX_VALUE;
            byte max = Byte.MIN_VALUE;
            while (--n >= 0) {
                final byte value = array[(indices != null) ? indices.getAsInt() : n];
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }

        /*
         * Not worth to override `increment(double)` because the array cannot be long anyway
         * (except if the increment is zero) and the implicit conversion of `byte` to `int`
         * performed by Java would make the implementation a little bit more tricky.
         */

        /** Wraps this vector in a buffer. */
        @Override public final Optional<Buffer> buffer() {
            return Optional.of(ByteBuffer.wrap(array));
        }

        /** Applies hash code contract specified {@link Vector#hashCode()}. */
        @Override public final int hashCode() {
            return Arrays.hashCode(array);
        }
    }

    /**
     * A vector backed by an array of type {@code long[]} to be interpreted as unsigned values.
     */
    private static final class UnsignedLongs extends Longs {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 712968674526282882L;

        /** Creates a new vector for the given array. */
        UnsignedLongs(final long[] array) {
            super(array);
        }

        /** Declares this vector as unsigned. */
        @Override public boolean isUnsigned() {return true;}

        /** Returns the unsigned long as a {@code double} value. */
        @Override public double doubleValue(final int index) {
            return Numerics.toUnsignedDouble(super.longValue(index));
        }

        /** Returns the unsigned long as a {@code float} value. */
        @Override public float floatValue(final int index) {
            return Numerics.toUnsignedFloat(super.longValue(index));
        }

        /** Returns the unsigned long as a {@code long} value, if possible. */
        @Override public long longValue(final int index) {
            final long value = super.longValue(index);
            if (value >= 0) return value;
            throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, Long.SIZE));
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Long.toUnsignedString(super.longValue(index));
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<Double> range(final IntSupplier indices, int n) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            while (--n >= 0) {
                final double value = doubleValue((indices != null) ? indices.getAsInt() : n);
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }
    }

    /**
     * A vector backed by an array of type {@code int[]} to be interpreted as unsigned values.
     */
    private static final class UnsignedIntegers extends Integers {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 8420585724189054050L;

        /** Creates a new vector for the given array. */
        UnsignedIntegers(final int[] array) {
            super(array);
        }

        /** Declares this vector as unsigned. */
        @Override public boolean isUnsigned()          {return true;}
        @Override public double doubleValue(int index) {return longValue(index);}
        @Override public float   floatValue(int index) {return longValue(index);}
        @Override public long     longValue(int index) {return Integer.toUnsignedLong(super.intValue(index));}
        @Override public int       intValue(int index) {
            final int value = super.intValue(index);
            if (value >= 0) return value;
            throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, Integer.SIZE));
        }

        /** Unconditionally uses a larger type since the value may exceed integer capacity. */
        @Override public Number get(int index) {
            return longValue(index);
        }

        /** Verifies that the given value can be stored as an unsigned integer. */
        @Override void verifyType(final Number value, NumberType expected) {
            final long v = value.longValue();
            if ((v & ~0xFFFFFFFFL) == 0) {
                expected = NumberType.LONG;
            }
            super.verifyType(value, expected);
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Integer.toUnsignedString(super.intValue(index));
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<?> range(final IntSupplier indices, int n) {
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            while (--n >= 0) {
                final long value = longValue((indices != null) ? indices.getAsInt() : n);
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }
    }

    /**
     * A vector backed by an array of type {@code short[]} to be interpreted as unsigned values.
     */
    private static final class UnsignedShorts extends Shorts {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 8219060080494444776L;

        /** Creates a new vector for the given array. */
        UnsignedShorts(final short[] array) {
            super(array);
        }

        /** Declares this vector as unsigned. */
        @Override public boolean isUnsigned()          {return true;}
        @Override public double doubleValue(int index) {return intValue(index);}
        @Override public float   floatValue(int index) {return intValue(index);}
        @Override public long     longValue(int index) {return Short.toUnsignedLong(super.shortValue(index));}
        @Override public int       intValue(int index) {return Short.toUnsignedInt (super.shortValue(index));}
        @Override public short   shortValue(int index) {
            final short value = super.shortValue(index);
            if (value >= 0) return value;
            throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, Short.SIZE));
        }

        /** Unconditionally uses a larger type since the value may exceed short capacity. */
        @Override public Number get(int index) {
            return intValue(index);
        }

        /** Verifies that the given value can be stored as an unsigned integer. */
        @Override void verifyType(final Number value, NumberType expected) {
            final int v = value.intValue();
            if ((v & ~0xFFFF) == 0) {
                expected = NumberType.INTEGER;
            }
            super.verifyType(value, expected);
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Integer.toString(intValue(index));
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<?> range(final IntSupplier indices, int n) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            while (--n >= 0) {
                final int value = intValue((indices != null) ? indices.getAsInt() : n);
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }
    }

    /**
     * A vector backed by an array of type {@code byte[]} to be interpreted as unsigned values.
     */
    private static final class UnsignedBytes extends Bytes {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -2150064612523948331L;

        /** Creates a new vector for the given array. */
        UnsignedBytes(final byte[] array) {
            super(array);
        }

        /** Declares this vector as unsigned. */
        @Override public boolean isUnsigned()          {return true;}
        @Override public double doubleValue(int index) {return intValue(index);}
        @Override public float   floatValue(int index) {return intValue(index);}
        @Override public long     longValue(int index) {return Byte.toUnsignedLong(super.byteValue(index));}
        @Override public int       intValue(int index) {return Byte.toUnsignedInt (super.byteValue(index));}
        @Override public short   shortValue(int index) {return (short) intValue(index);}
        @Override public byte     byteValue(int index) {
            final byte value = super.byteValue(index);
            if (value >= 0) return value;
            throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, Byte.SIZE));
        }

        /** Unconditionally uses a larger type since the value may exceed byte capacity. */
        @Override public Number get(int index) {
            return shortValue(index);
        }

        /** Verifies that the given value can be stored as an unsigned integer. */
        @Override void verifyType(final Number value, NumberType expected) {
            final int v = value.intValue();
            if ((v & ~0xFF) == 0) {
                expected = NumberType.SHORT;
            }
            super.verifyType(value, expected);
        }

        /** Returns the string representation at the given index. */
        @Override public String stringValue(final int index) {
            return Integer.toString(intValue(index));
        }

        /** Finds the minimum and maximum values in the array or in a subset of the array. */
        @Override NumberRange<?> range(final IntSupplier indices, int n) {
            short min = Short.MAX_VALUE;
            short max = Short.MIN_VALUE;
            while (--n >= 0) {
                final short value = shortValue((indices != null) ? indices.getAsInt() : n);
                if (value < min) min = value;
                if (value > max) max = value;
            }
            return NumberRange.create(min, true, max, true);
        }
    }

    /**
     * A vector backed by an array of type {@code String[]}.
     * This is not recommended, but happen for example in GDAL extensions for GeoTIFF files.
     */
    private static final class ASCII extends ArrayVector<Double> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 2801615620517491573L;

        /** The backing array. */
        private final String[] array;

        /** Creates a new vector for the given array. */
        ASCII(final String[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<Double> getElementType() {
            return Double.class;
        }

        /** Returns whether values are convertible to {@code float} type. */
        @Override public final boolean isSinglePrecision() {
            return false;
        }

        /** Returns {@code true} if the element at the given index is null or NaN. */
        @Override public boolean isNaN(final int index) {
            String value = array[index];
            if (value == null) return true;
            if (value.contains("NaN")) {
                value = value.trim();
                switch (value.length()) {
                    case 3: return true;
                    case 4: final char c = value.charAt(0);
                            return (c == '+') || (c == '-');
                }
            }
            return false;
        }

        @Override public int           size()          {return array.length;}
        @Override public String stringValue(int index) {return array[index];}
        @Override public double doubleValue(int index) {return Double .parseDouble(array[index]);}
        @Override public float   floatValue(int index) {return Float  .parseFloat (array[index]);}
        @Override public long     longValue(int index) {return Long   .parseLong  (array[index]);}
        @Override public int       intValue(int index) {return Integer.parseInt   (array[index]);}
        @Override public short   shortValue(int index) {return Short  .parseShort (array[index]);}
        @Override public byte     byteValue(int index) {return Byte   .parseByte  (array[index]);}
        @Override public Number         get(int index) {
            final String value = array[index];
            return (value != null) ? Double.valueOf(value) : null;
        }

        /** Stores the given value in this vector and returns the previous value. */
        @Override public final Number set(final int index, final Number value) {
            final Number old = get(index);
            array[index] = value.toString();
            modCount++;
            return old;
        }

        /** Sets the value of all elements in the given range. */
        @Override public void fill(final int fromIndex, final int toIndex, final Number value) {
            Arrays.fill(array, fromIndex, toIndex, value.toString());
            modCount++;
        }
    }

    /**
     * A vector backed by an array of type {@code Number[]}.
     * The parameterized type should be {@code <? extends Number>},
     * but using {@code <Number>} for simplicity is okay since this class is private
     * and this specific type is forgotten immediately after {@code Raw} construction.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class Raw extends ArrayVector<Number> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 5444263017359778157L;

        /** The backing array. */
        private final Number[] array;

        /** Creates a new vector for the given array. */
        Raw(final Number[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class getElementType() {
            return array.getClass().getComponentType();
        }

        /** Returns {@code true} if the element at the given index is null or NaN. */
        @Override public boolean isNaN(final int index) {
            return Numbers.isNaN(array[index]);
        }

        @Override public int           size()          {return array.length;}
        @Override public Number         get(int index) {return array[index];}
        @Override public double doubleValue(int index) {return array[index].doubleValue();}
        @Override public float   floatValue(int index) {return array[index].floatValue();}
        @Override public long     longValue(int index) {return array[index].longValue();}
        @Override public int       intValue(int index) {return array[index].intValue();}
        @Override public short   shortValue(int index) {return array[index].shortValue();}
        @Override public byte     byteValue(int index) {return array[index].byteValue();}
        @Override public String stringValue(int index) {
            final Number value = array[index];
            return (value != null) ? value.toString() : null;
        }

        /** Stores the given value in this vector and returns the previous value. */
        @Override public final Number set(final int index, final Number value) {
            final Number old = array[index];
            array[index] = value;
            modCount++;
            return old;
        }

        /** Sets the value of all elements in the given range. */
        @Override public void fill(final int fromIndex, final int toIndex, final Number value) {
            Arrays.fill(array, fromIndex, toIndex, value);
            modCount++;
        }
    }
}
