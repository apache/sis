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
import java.lang.reflect.Array;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;


/**
 * A vector backed by an array. This class does not copy the array, so changes in the underlying
 * array is reflected in this vector and vis-versa. The backing array is typically an array of a
 * primitive type, but array of wrappers are also accepted.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class ArrayVector extends Vector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3496467575389288163L;

    /**
     * The backing array. This is typically an array of a primitive type,
     * but can also be an array of wrappers.
     */
    private final Object array;

    /**
     * Creates a new vector for the given array.
     */
    ArrayVector(final Object array) {
        this.array = array;
    }

    /**
     * Returns the type of elements in the backing array.
     * This method returns alway the wrapper type, as documented in the parent class.
     */
    @Override
    public Class<? extends Number> getElementType() {
        return Numbers.primitiveToWrapper(array.getClass().getComponentType()).asSubclass(Number.class);
    }

    /**
     * Returns the length of the backing array.
     */
    @Override
    public int size() {
        return Array.getLength(array);
    }

    /**
     * Returns {@code true} if the value at the given index is {@code NaN}. The default implementation
     * returns {@code true} if {@code get(index)} returned {@code null} or {@link Double#NaN}. However
     * subclasses will typically provide more efficient implementations. For example vectors of integer
     * type return {@code false} unconditionally.
     *
     * @param  index the index in the [0…{@linkplain #size size}-1] range.
     * @return {@code true} if the value at the given index is {@code NaN}.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public boolean isNaN(final int index) throws IndexOutOfBoundsException {
        final Number n = get(index);
        return (n == null) || java.lang.Double.isNaN(n.doubleValue());
    }

    /**
     * Returns the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code double} by an identity or widening conversion.
     */
    @Override
    public double doubleValue(final int index) throws ArrayIndexOutOfBoundsException, ClassCastException {
        try {
            return Array.getDouble(array, index);
        } catch (IllegalArgumentException cause) {
            throw canNotConvert(double.class, cause);
        }
    }

    /**
     * Returns the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code float} by an identity or widening conversion.
     */
    @Override
    public float floatValue(final int index) throws ArrayIndexOutOfBoundsException, ClassCastException {
        try {
            return Array.getFloat(array, index);
        } catch (IllegalArgumentException cause) {
            throw canNotConvert(float.class, cause);
        }
    }

    /**
     * Returns the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code long} by an identity or widening conversion.
     */
    @Override
    public long longValue(final int index) throws ArrayIndexOutOfBoundsException, ClassCastException {
        try {
            return Array.getLong(array, index);
        } catch (IllegalArgumentException cause) {
            throw canNotConvert(long.class, cause);
        }
    }

    /**
     * Returns the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to an
     *         {@code int} by an identity or widening conversion.
     */
    @Override
    public int intValue(final int index) throws ArrayIndexOutOfBoundsException, ClassCastException {
        try {
            return Array.getInt(array, index);
        } catch (IllegalArgumentException cause) {
            throw canNotConvert(int.class, cause);
        }
    }

    /**
     * Returns the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code short} by an identity or widening conversion.
     */
    @Override
    public short shortValue(final int index) throws ArrayIndexOutOfBoundsException, ClassCastException {
        try {
            return Array.getShort(array, index);
        } catch (IllegalArgumentException cause) {
            throw canNotConvert(short.class, cause);
        }
    }

    /**
     * Returns the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     * @throws ClassCastException if the component type can not be converted to a
     *         {@code byte} by an identity or widening conversion.
     */
    @Override
    public byte byteValue(final int index) throws ArrayIndexOutOfBoundsException, ClassCastException {
        try {
            return Array.getByte(array, index);
        } catch (IllegalArgumentException cause) {
            throw canNotConvert(byte.class, cause);
        }
    }

    /**
     * Returns the exception to be thrown when the component type in the backing array can
     * not be converted to the requested type through an identity or widening conversion.
     */
    private ClassCastException canNotConvert(final Class<?> target, final IllegalArgumentException cause) {
        final ClassCastException exception = new ClassCastException(Errors.format(
                Errors.Keys.CanNotConvertFromType_2, array.getClass().getComponentType(), target));
        exception.initCause(cause);
        return exception;
    }

    /**
     * Returns the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public Number get(final int index) throws ArrayIndexOutOfBoundsException {
        return (Number) Array.get(array, index);
    }

    /**
     * Sets the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     * @throws ArrayStoreException if the backing array can not store the given object.
     */
    @Override
    public Number set(final int index, final Number value)
            throws ArrayIndexOutOfBoundsException, ArrayStoreException
    {
        final Number old = (Number) Array.get(array, index);
        try {
            Array.set(array, index, value);
        } catch (IllegalArgumentException cause) {
            throw (ArrayStoreException) new ArrayStoreException(Errors.format(Errors.Keys.CanNotConvertFromType_2,
                    Classes.getClass(value), array.getClass().getComponentType())).initCause(cause);
        }
        return old;
    }




    /**
     * A vector backed by an array of type {@code double[]}. This class does not copy the
     * array, so changes in the underlying array is reflected in this vector and vis-versa.
     */
    static final class Double extends Vector implements Serializable {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -2900375382498345812L;

        /** The backing array. */
        private final double[] array;

        /** Creates a new vector for the given array. */
        Double(final double[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public Class<java.lang.Double> getElementType() {
            return java.lang.Double.class;
        }

        /** Returns the length of the backing array. */
        @Override public int size() {
            return array.length;
        }

        /** Returns {@code true} if the value at the given index is {@code NaN}. */
        @Override public boolean isNaN(final int index) throws ArrayIndexOutOfBoundsException {
            return java.lang.Double.isNaN(array[index]);
        }

        /** Returns the value at the given index. */
        @Override public double doubleValue(final int index) throws ArrayIndexOutOfBoundsException {
            return array[index];
        }

        /**
         * Returns the value casted as a {@code float}, since we may loose precision but the
         * result of the cast is not completely wrong (at worst we get zero of infinity values
         * if the magnitude of the {@code double} value was too small or too large).
         */
        @Override public float floatValue(int index) throws ArrayIndexOutOfBoundsException {
            return (float) array[index];
        }

        /** Can not cast safely to integer values. */
        @Override public long   longValue(int index) throws ClassCastException {throw canNotConvert(long .class);}
        @Override public int     intValue(int index) throws ClassCastException {throw canNotConvert(int  .class);}
        @Override public short shortValue(int index) throws ClassCastException {throw canNotConvert(short.class);}
        @Override public byte   byteValue(int index) throws ClassCastException {throw canNotConvert(byte .class);}

        /**
         * Returns the exception to be thrown when the component type in the backing array can
         * not be converted to the requested type through an identity or widening conversion.
         */
        private static ClassCastException canNotConvert(final Class<?> target) {
            return new ClassCastException(Errors.format(
                    Errors.Keys.CanNotConvertFromType_2, double.class, target));
        }

        /** Returns the value at the given index. */
        @Override public java.lang.Double get(final int index) throws ArrayIndexOutOfBoundsException {
            return array[index];
        }

        /** Sets the value at the given index. */
        @Override public java.lang.Double set(final int index, final Number value) throws ArrayIndexOutOfBoundsException {
            final double old = array[index];
            array[index] = value.doubleValue();
            return old;
        }
    }




    /**
     * A vector backed by an array of type {@code float[]}. This class does not copy the
     * array, so changes in the underlying array is reflected in this vector and vis-versa.
     */
    static final class Float extends Vector implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 5395284704294981455L;

        /**
         * The backing array.
         */
        private final float[] array;

        /**
         * Creates a new vector for the given array.
         */
        Float(final float[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public Class<java.lang.Float> getElementType() {
            return java.lang.Float.class;
        }

        /** Returns the length of the backing array. */
        @Override public int size() {
            return array.length;
        }

        /** Returns {@code true} if the value at the given index is {@code NaN}. */
        @Override public boolean isNaN(final int index) throws ArrayIndexOutOfBoundsException {
            return java.lang.Float.isNaN(array[index]);
        }

        /** Returns the value at the given index. */
        @Override public double doubleValue(final int index) throws ArrayIndexOutOfBoundsException {
            return array[index];
        }

        /** Returns the value at the given index. */
        @Override public float floatValue(int index) throws ArrayIndexOutOfBoundsException {
            return array[index];
        }

        /** Can not cast safely to integer values. */
        @Override public long   longValue(int index) throws ClassCastException {throw canNotConvert(long .class);}
        @Override public int     intValue(int index) throws ClassCastException {throw canNotConvert(int  .class);}
        @Override public short shortValue(int index) throws ClassCastException {throw canNotConvert(short.class);}
        @Override public byte   byteValue(int index) throws ClassCastException {throw canNotConvert(byte .class);}

        /**
         * Returns the exception to be thrown when the component type in the backing array can
         * not be converted to the requested type through an identity or widening conversion.
         */
        private static ClassCastException canNotConvert(final Class<?> target) {
            return new ClassCastException(Errors.format(
                    Errors.Keys.CanNotConvertFromType_2, float.class, target));
        }

        /** Returns the value at the given index. */
        @Override public java.lang.Float get(final int index) throws ArrayIndexOutOfBoundsException {
            return array[index];
        }

        /** Sets the value at the given index. */
        @Override public java.lang.Float set(final int index, final Number value) throws ArrayIndexOutOfBoundsException {
            final float old = array[index];
            array[index] = value.floatValue();
            return old;
        }
    }
}
