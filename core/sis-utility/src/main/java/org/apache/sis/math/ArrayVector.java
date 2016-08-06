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
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;


/**
 * A vector backed by an array of a primitive type. This class does not copy the array,
 * so changes in the underlying array is reflected in this vector and vis-versa.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
abstract class ArrayVector<E extends Number> extends Vector implements CheckedContainer<E>, Serializable {
    /** For cross-version compatibility. */
    private static final long serialVersionUID = 3496467575389288163L;

    /** For sub-classes constructor. */
    ArrayVector() {}

    /** Default implementation for the convenience of direct sub-types. */
    @Override public boolean isUnsigned() {
        return false;
    }

    /** Default implementation for the convenience of wrapper of integer types. */
    @Override public boolean    isNaN(int index) {return false;}
    @Override public long   longValue(int index) {throw canNotConvert(long .class);}
    @Override public int     intValue(int index) {throw canNotConvert(int  .class);}
    @Override public short shortValue(int index) {throw canNotConvert(short.class);}
    @Override public byte   byteValue(int index) {throw canNotConvert(byte .class);}

    /**
     * Returns the exception to be thrown when the component type in the backing array can
     * not be converted to the requested type through an identity or widening conversion.
     */
    private ClassCastException canNotConvert(final Class<?> target) {
        return new ClassCastException(Errors.format(Errors.Keys.CanNotConvertFromType_2,
                Numbers.wrapperToPrimitive(getElementType()), target));
    }

    /**
     * Verifies that a value of the given type can be casted to the expected type.
     * The expected type must be one of the {@link Numbers} constants.
     */
    final void verifyType(final Class<? extends Number> type, final byte expected) {
        final byte t = Numbers.getEnumConstant(type);
        if (t < Numbers.BYTE || t > expected) {
            throw new ArrayStoreException(Errors.format(Errors.Keys.CanNotConvertFromType_2,
                type, Numbers.wrapperToPrimitive(getElementType())));
        }
    }

    /**
     * A vector backed by an array of type {@code double[]}.
     */
    static final class Double extends ArrayVector<java.lang.Double> {
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
        @Override public boolean isNaN(final int index) {
            return java.lang.Double.isNaN(array[index]);
        }

        /** Returns the value at the given index. */
        @Override public double doubleValue(final int index) {
            return array[index];
        }

        /**
         * Returns the value casted as a {@code float}, since we may loose precision but the
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
            return old;
        }
    }

    /**
     * A vector backed by an array of type {@code float[]}.
     */
    static final class Float extends ArrayVector<java.lang.Float> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 5395284704294981455L;

        /** The backing array. */
        private final float[] array;

        /** Creates a new vector for the given array. */
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
        @Override public boolean isNaN(final int index) {
            return java.lang.Float.isNaN(array[index]);
        }

        /** Returns the value at the given index. */
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public Number         get(int index) {return array[index];}

        /** Sets the value at the given index. */
        @Override public Number set(final int index, final Number value) {
            final float old = array[index];
            array[index] = value.floatValue();
            return old;
        }
    }

    /**
     * A vector backed by an array of type {@code long[]}.
     */
    static class Long extends ArrayVector<java.lang.Long> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 338413429037224587L;

        /** The backing array. */
        private final long[] array;

        /** Creates a new vector for the given array. */
        Long(final long[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<java.lang.Long> getElementType() {
            return java.lang.Long.class;
        }

        @Override public final int     size()          {return array.length;}
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public long     longValue(int index) {return array[index];}
        @Override public final Number   get(int index) {return longValue(index);}
        @Override public final Number   set(int index, final Number value) {
            verifyType(value.getClass(), Numbers.LONG);
            final long old = array[index];
            array[index] = value.longValue();
            return old;
        }
    }

    /**
     * A vector backed by an array of type {@code int[]}.
     */
    static class Integer extends ArrayVector<java.lang.Integer> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -1292641147544275801L;

        /** The backing array. */
        private final int[] array;

        /** Creates a new vector for the given array. */
        Integer(final int[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<java.lang.Integer> getElementType() {
            return java.lang.Integer.class;
        }

        @Override public final int     size()          {return array.length;}
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public long     longValue(int index) {return array[index];}
        @Override public int       intValue(int index) {return array[index];}
        @Override public final Number   get(int index) {return intValue(index);}
        @Override public final Number   set(int index, final Number value) {
            verifyType(value.getClass(), Numbers.INTEGER);
            final int old = array[index];
            array[index] = value.intValue();
            return old;
        }
    }

    /**
     * A vector backed by an array of type {@code short[]}.
     */
    static class Short extends ArrayVector<java.lang.Short> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -126825963332296000L;

        /** The backing array. */
        private final short[] array;

        /** Creates a new vector for the given array. */
        Short(final short[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<java.lang.Short> getElementType() {
            return java.lang.Short.class;
        }

        @Override public final int     size()          {return array.length;}
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public long     longValue(int index) {return array[index];}
        @Override public int       intValue(int index) {return array[index];}
        @Override public short   shortValue(int index) {return array[index];}
        @Override public final Number   get(int index) {return shortValue(index);}
        @Override public final Number   set(int index, final Number value) {
            verifyType(value.getClass(), Numbers.SHORT);
            final short old = array[index];
            array[index] = value.shortValue();
            return old;
        }
    }

    /**
     * A vector backed by an array of type {@code byte[]}.
     */
    static class Byte extends ArrayVector<java.lang.Byte> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 7933568876180528548L;

        /** The backing array. */
        private final byte[] array;

        /** Creates a new vector for the given array. */
        Byte(final byte[] array) {
            this.array = array;
        }

        /** Returns the type of elements in the backing array. */
        @Override public final Class<java.lang.Byte> getElementType() {
            return java.lang.Byte.class;
        }

        @Override public final int     size()          {return array.length;}
        @Override public double doubleValue(int index) {return array[index];}
        @Override public float   floatValue(int index) {return array[index];}
        @Override public long     longValue(int index) {return array[index];}
        @Override public int       intValue(int index) {return array[index];}
        @Override public short   shortValue(int index) {return array[index];}
        @Override public byte     byteValue(int index) {return array[index];}
        @Override public final Number   get(int index) {return shortValue(index);}
        @Override public final Number   set(int index, final Number value) {
            verifyType(value.getClass(), Numbers.BYTE);
            final byte old = array[index];
            array[index] = value.byteValue();
            return old;
        }
    }

    /**
     * A vector backed by an array of type {@code long[]} to be interpreted as unsigned values.
     */
    static final class UnsignedLong extends Long {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 712968674526282882L;

        /** Creates a new vector for the given array. */
        UnsignedLong(final long[] array) {
            super(array);
        }

        /** Declares this vector as unsigned. */
        @Override public boolean isUnsigned() {return true;}

        /** Returns the unsigned long as a {@code double} value. */
        @Override public double doubleValue(final int index) {
            final long value = super.longValue(index);
            if (value >= 0) {
                return value;
            }
            // This hack is inefficient, but should be rarely needed.
            return java.lang.Double.parseDouble(java.lang.Long.toUnsignedString(value));
        }

        /** Returns the unsigned long as a {@code float} value. */
        @Override public float floatValue(final int index) {
            final long value = super.longValue(index);
            if (value >= 0) {
                return value;
            }
            // This hack is inefficient, but should be rarely needed.
            return java.lang.Float.parseFloat(java.lang.Long.toUnsignedString(value));
        }

        /** Returns the unsigned long as a {@code long} value, if possible. */
        @Override public long longValue(final int index) {
            final long value = super.longValue(index);
            if (value >= 0) return value;
            throw new ArithmeticException();
        }
    }

    /**
     * A vector backed by an array of type {@code int[]} to be interpreted as unsigned values.
     */
    static final class UnsignedInteger extends Integer {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 8420585724189054050L;

        /** Creates a new vector for the given array. */
        UnsignedInteger(final int[] array) {
            super(array);
        }

        /** Declares this vector as unsigned. */
        @Override public boolean isUnsigned()          {return true;}
        @Override public double doubleValue(int index) {return longValue(index);}
        @Override public float   floatValue(int index) {return longValue(index);}
        @Override public long     longValue(int index) {return java.lang.Integer.toUnsignedLong(super.intValue(index));}
        @Override public int       intValue(int index) {
            final int value = super.intValue(index);
            if (value >= 0) return value;
            throw new ArithmeticException();
        }
    }

    /**
     * A vector backed by an array of type {@code short[]} to be interpreted as unsigned values.
     */
    static final class UnsignedShort extends Short {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 8219060080494444776L;

        UnsignedShort(final short[] array) {
            super(array);
        }

        @Override public boolean isUnsigned()          {return true;}
        @Override public double doubleValue(int index) {return intValue(index);}
        @Override public float   floatValue(int index) {return intValue(index);}
        @Override public long     longValue(int index) {return java.lang.Short.toUnsignedLong(super.shortValue(index));}
        @Override public int       intValue(int index) {return java.lang.Short.toUnsignedInt (super.shortValue(index));}
        @Override public short   shortValue(int index) {
            final short value = super.shortValue(index);
            if (value >= 0) return value;
            throw new ArithmeticException();
        }
    }

    /**
     * A vector backed by an array of type {@code byte[]} to be interpreted as unsigned values.
     */
    static final class UnsignedByte extends Byte {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -2150064612523948331L;

        UnsignedByte(final byte[] array) {
            super(array);
        }

        @Override public boolean isUnsigned()          {return true;}
        @Override public double doubleValue(int index) {return intValue(index);}
        @Override public float   floatValue(int index) {return intValue(index);}
        @Override public long     longValue(int index) {return java.lang.Byte.toUnsignedLong (super.byteValue(index));}
        @Override public int       intValue(int index) {return java.lang.Byte.toUnsignedInt  (super.byteValue(index));}
        @Override public short   shortValue(int index) {return (short) intValue(index);}
        @Override public byte     byteValue(int index) {
            final byte value = super.byteValue(index);
            if (value >= 0) return value;
            throw new ArithmeticException();
        }
    }
}
