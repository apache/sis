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
package org.apache.sis.maths;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class Vector1D<T extends Vector1D<T>> extends AbstractVector<T> {

    public Vector1D() {
    }

    @Override
    public final int getDimension() {
        return 1;
    }

    @Override
    public abstract T with(int indice, double value) throws IndexOutOfBoundsException;

    @Override
    public abstract T add(Vector<?> other);

    @Override
    public abstract T subtract(Vector<?> other);

    @Override
    public abstract T multiply(Vector<?> other);

    @Override
    public abstract T divide(Vector<?> other);

    @Override
    public abstract T scale(double scale);

    /**
     * Create a new vector, linear combination of this vector and the other vector : a*this + b*other.
     * The other vector is expected to have the same dimension as this vector.
     *
     * @param a coefficient of this vector
     * @param other second vector
     * @param b coefficient of the other vector
     * @return new vector
     */
    protected abstract T combine(double a, Vector<?> other, double b);

    /**
     * Same as {@link #combine(double, Vector, double)} but computed with compensated arithmetic.
     *
     * @param a coefficient of this vector
     * @param other second vector
     * @param b coefficient of the other vector
     * @return new vector
     */
    protected abstract T combineAccurate(double a, Vector<?> other, double b);

    @Override
    public T normalize() {
        return scale(1.0 / length());
    }

    @Override
    public T negate() {
        return scale(-1);
    }

    @Override
    public T perpendicular(Vector<?> unit) {
        ensureDimension(unit, 1);
        return combine(1, unit, -dot(unit));
    }

    @Override
    public abstract T lerp(Vector<?> other, double ratio);

    @Override
    public T lerpAccurate(Vector<?> other, double ratio) {
        ensureDimension(other, 1);
        return combineAccurate(1 - ratio, other, ratio);
    }

    @Override
    public T slerp(Vector<?> other, double ratio) {
        return AbstractVector.slerp(this, other, ratio, (a, b) -> combine(a, other, b));
    }

    @Override
    public T vectorTo(Vector<?> other) {
        ensureDimension(other, 1);
        return combine(-1, other, 1);
    }

    @Override
    public double lengthSquare() {
        final double v0 = get(0);
        return v0*v0;
    }

    @Override
    public double dot(Vector<?> other) {
        ensureDimension(other, 1);
        return get(0) * other.get(0);
    }

    /**
     * A single product is already correctly rounded.
     */
    @Override
    public double dotAccurate(Vector<?> other) {
        return dot(other);
    }

    @Override
    public double distanceSquare(Vector<?> other) {
        ensureDimension(other, 1);
        final double d0 = get(0) - other.get(0);
        return d0*d0;
    }

    public static class Byte extends Vector1D<Byte> {

        public final byte x;

        public Byte() {
            this.x = 0;
        }

        public Byte(byte x) {
            this.x = x;
        }

        public Byte(byte[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public Byte(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = (byte) tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.BYTE;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public Byte add(Vector<?> other) {
            ensureDimension(other, 1);
            return new Byte((byte) (x + other.get(0)));
        }

        @Override
        public Byte subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new Byte((byte) (x - other.get(0)));
        }

        @Override
        public Byte multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new Byte((byte) (x * other.get(0)));
        }

        @Override
        public Byte divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new Byte((byte) (x / other.get(0)));
        }

        @Override
        public Byte scale(double scale) {
            return new Byte((byte) (x * scale));
        }

        @Override
        protected Byte combine(double a, Vector<?> other, double b) {
            return new Byte((byte) (a * x + b * other.get(0)));
        }

        @Override
        protected Byte combineAccurate(double a, Vector<?> other, double b) {
            return new Byte((byte) (LinearCombination.of(a, x, b, other.get(0))));
        }

        @Override
        public Byte with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Byte((byte) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Byte lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new Byte((byte) (other.get(0)));
            }
            return new Byte(
                    (byte) (x + ratio * (other.get(0) - x)));
        }

        @Override
        public Byte copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

        @Override
        public boolean isNaN() {
            return false;
        }

        @Override
        public boolean isInfinite() {
            return false;
        }

    }

    public static class UByte extends Vector1D<UByte> {

        public final byte x;

        public UByte() {
            this.x = 0;
        }

        public UByte(byte x) {
            this.x = x;
        }

        public UByte(byte[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public UByte(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = (byte) tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.UBYTE;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x & 0xFF;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x & 0xFF};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x & 0xFF};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x & 0xFF};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = (short) (x & 0xFF);
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = x & 0xFF;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = x & 0xFF;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = x & 0xFF;
        }

        @Override
        public UByte add(Vector<?> other) {
            ensureDimension(other, 1);
            return new UByte((byte) ((x & 0xFF) + other.get(0)));
        }

        @Override
        public UByte subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new UByte((byte) ((x & 0xFF) - other.get(0)));
        }

        @Override
        public UByte multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new UByte((byte) ((x & 0xFF) * other.get(0)));
        }

        @Override
        public UByte divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new UByte((byte) ((x & 0xFF) / other.get(0)));
        }

        @Override
        public UByte scale(double scale) {
            return new UByte((byte) ((x & 0xFF) * scale));
        }

        @Override
        protected UByte combine(double a, Vector<?> other, double b) {
            return new UByte((byte) (a * (x & 0xFF) + b * other.get(0)));
        }

        @Override
        protected UByte combineAccurate(double a, Vector<?> other, double b) {
            return new UByte((byte) (LinearCombination.of(a, (x & 0xFF), b, other.get(0))));
        }

        @Override
        public UByte with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UByte((byte) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UByte lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new UByte((byte) (other.get(0)));
            }
            return new UByte(
                    (byte) ((x & 0xFF) + ratio * (other.get(0) - (x & 0xFF))));
        }

        @Override
        public UByte copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

        @Override
        public boolean isNaN() {
            return false;
        }

        @Override
        public boolean isInfinite() {
            return false;
        }

    }

    public static class Short extends Vector1D<Short> {

        public final short x;

        public Short() {
            this.x = 0;
        }

        public Short(short x) {
            this.x = x;
        }

        public Short(short[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public Short(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = (short) tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.SHORT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public Short add(Vector<?> other) {
            ensureDimension(other, 1);
            return new Short((short) (x + other.get(0)));
        }

        @Override
        public Short subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new Short((short) (x - other.get(0)));
        }

        @Override
        public Short multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new Short((short) (x * other.get(0)));
        }

        @Override
        public Short divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new Short((short) (x / other.get(0)));
        }

        @Override
        public Short scale(double scale) {
            return new Short((short) (x * scale));
        }

        @Override
        protected Short combine(double a, Vector<?> other, double b) {
            return new Short((short) (a * x + b * other.get(0)));
        }

        @Override
        protected Short combineAccurate(double a, Vector<?> other, double b) {
            return new Short((short) (LinearCombination.of(a, x, b, other.get(0))));
        }

        @Override
        public Short with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Short((short) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Short lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new Short((short) (other.get(0)));
            }
            return new Short(
                    (short) (x + ratio * (other.get(0) - x)));
        }

        @Override
        public Short copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

        @Override
        public boolean isNaN() {
            return false;
        }

        @Override
        public boolean isInfinite() {
            return false;
        }

    }

    public static class UShort extends Vector1D<UShort> {

        public final short x;

        public UShort() {
            this.x = 0;
        }

        public UShort(short x) {
            this.x = x;
        }

        public UShort(short[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public UShort(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = (short) tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.USHORT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return java.lang.Short.toUnsignedInt(x);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{java.lang.Short.toUnsignedInt(x)};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{java.lang.Short.toUnsignedInt(x)};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{java.lang.Short.toUnsignedInt(x)};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = java.lang.Short.toUnsignedInt(x);
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = java.lang.Short.toUnsignedInt(x);
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = java.lang.Short.toUnsignedInt(x);
        }

        @Override
        public UShort add(Vector<?> other) {
            ensureDimension(other, 1);
            return new UShort((short) ((x & 0xFFFF) + other.get(0)));
        }

        @Override
        public UShort subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new UShort((short) ((x & 0xFFFF) - other.get(0)));
        }

        @Override
        public UShort multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new UShort((short) ((x & 0xFFFF) * other.get(0)));
        }

        @Override
        public UShort divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new UShort((short) ((x & 0xFFFF) / other.get(0)));
        }

        @Override
        public UShort scale(double scale) {
            return new UShort((short) ((x & 0xFFFF) * scale));
        }

        @Override
        protected UShort combine(double a, Vector<?> other, double b) {
            return new UShort((short) (a * (x & 0xFFFF) + b * other.get(0)));
        }

        @Override
        protected UShort combineAccurate(double a, Vector<?> other, double b) {
            return new UShort((short) (LinearCombination.of(a, (x & 0xFFFF), b, other.get(0))));
        }

        @Override
        public UShort with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UShort((short) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UShort lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new UShort((short) (other.get(0)));
            }
            return new UShort(
                    (short) ((x & 0xFFFF) + ratio * (other.get(0) - (x & 0xFFFF))));
        }

        @Override
        public UShort copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

        @Override
        public boolean isNaN() {
            return false;
        }

        @Override
        public boolean isInfinite() {
            return false;
        }

    }

    public static class Int extends Vector1D<Int> {

        public final int x;

        public Int() {
            this.x = 0;
        }

        public Int(int x) {
            this.x = x;
        }

        public Int(int[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public Int(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = (int) tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.INT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = (short) x;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public Int add(Vector<?> other) {
            ensureDimension(other, 1);
            return new Int((int) (x + other.get(0)));
        }

        @Override
        public Int subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new Int((int) (x - other.get(0)));
        }

        @Override
        public Int multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new Int((int) (x * other.get(0)));
        }

        @Override
        public Int divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new Int((int) (x / other.get(0)));
        }

        @Override
        public Int scale(double scale) {
            return new Int((int) (x * scale));
        }

        @Override
        protected Int combine(double a, Vector<?> other, double b) {
            return new Int((int) (a * x + b * other.get(0)));
        }

        @Override
        protected Int combineAccurate(double a, Vector<?> other, double b) {
            return new Int((int) (LinearCombination.of(a, x, b, other.get(0))));
        }

        @Override
        public Int with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Int((int) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Int lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new Int((int) (other.get(0)));
            }
            return new Int(
                    (int) (x + ratio * (other.get(0) - x)));
        }

        @Override
        public Int copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

        @Override
        public boolean isNaN() {
            return false;
        }

        @Override
        public boolean isInfinite() {
            return false;
        }

    }

    public static class UInt extends Vector1D<UInt> {

        public final int x;

        public UInt() {
            this.x = 0;
        }

        public UInt(int x) {
            this.x = x;
        }

        public UInt(int[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public UInt(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = (int) (long) tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.UINT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return Integer.toUnsignedLong(x);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{Integer.toUnsignedLong(x)};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{Integer.toUnsignedLong(x)};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = (short) Integer.toUnsignedLong(x);
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = Integer.toUnsignedLong(x);
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = Integer.toUnsignedLong(x);
        }

        @Override
        public UInt add(Vector<?> other) {
            ensureDimension(other, 1);
            return new UInt((int) (long) (Integer.toUnsignedLong(x) + other.get(0)));
        }

        @Override
        public UInt subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new UInt((int) (long) (Integer.toUnsignedLong(x) - other.get(0)));
        }

        @Override
        public UInt multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new UInt((int) (long) (Integer.toUnsignedLong(x) * other.get(0)));
        }

        @Override
        public UInt divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new UInt((int) (long) (Integer.toUnsignedLong(x) / other.get(0)));
        }

        @Override
        public UInt scale(double scale) {
            return new UInt((int) (long) (Integer.toUnsignedLong(x) * scale));
        }

        @Override
        protected UInt combine(double a, Vector<?> other, double b) {
            return new UInt((int) (long) (a * Integer.toUnsignedLong(x) + b * other.get(0)));
        }

        @Override
        protected UInt combineAccurate(double a, Vector<?> other, double b) {
            return new UInt((int) (long) (LinearCombination.of(a, Integer.toUnsignedLong(x), b, other.get(0))));
        }

        @Override
        public UInt with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UInt((int) (long) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UInt lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new UInt((int) (long) (other.get(0)));
            }
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) + ratio * (other.get(0) - Integer.toUnsignedLong(x))));
        }

        @Override
        public UInt copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

        @Override
        public boolean isNaN() {
            return false;
        }

        @Override
        public boolean isInfinite() {
            return false;
        }

    }

    public static class Long extends Vector1D<Long> {

        public final long x;

        public Long() {
            this.x = 0;
        }

        public Long(long x) {
            this.x = x;
        }

        public Long(long[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public Long(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = (long) tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.LONG;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int) x};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = (short) x;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = (int) x;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public Long add(Vector<?> other) {
            ensureDimension(other, 1);
            return new Long((long) (x + other.get(0)));
        }

        @Override
        public Long subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new Long((long) (x - other.get(0)));
        }

        @Override
        public Long multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new Long((long) (x * other.get(0)));
        }

        @Override
        public Long divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new Long((long) (x / other.get(0)));
        }

        @Override
        public Long scale(double scale) {
            return new Long((long) (x * scale));
        }

        @Override
        protected Long combine(double a, Vector<?> other, double b) {
            return new Long((long) (a * x + b * other.get(0)));
        }

        @Override
        protected Long combineAccurate(double a, Vector<?> other, double b) {
            return new Long((long) (LinearCombination.of(a, x, b, other.get(0))));
        }

        @Override
        public Long with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Long((long) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Long lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new Long((long) (other.get(0)));
            }
            return new Long(
                    (long) (x + ratio * (other.get(0) - x)));
        }

        @Override
        public Long copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

        @Override
        public boolean isNaN() {
            return false;
        }

        @Override
        public boolean isInfinite() {
            return false;
        }

    }

    public static class Float extends Vector1D<Float> {

        public final float x;

        public Float() {
            this.x = 0;
        }

        public Float(float x) {
            this.x = x;
        }

        public Float(float[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public Float(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = (float) tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.FLOAT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public double length() {
            return Math.sqrt(x*x);
        }

        @Override
        public double lengthSquare() {
            return x*x;
        }

        @Override
        public double dot(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return x * other.get(0);
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int)x};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = (short) x;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = (int) x;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public Float add(Vector<?> other) {
            ensureDimension(other, 1);
            return new Float((float) (x + other.get(0)));
        }

        @Override
        public Float subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new Float((float) (x - other.get(0)));
        }

        @Override
        public Float multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new Float((float) (x * other.get(0)));
        }

        @Override
        public Float divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new Float((float) (x / other.get(0)));
        }

        @Override
        public Float scale(double scale) {
            return new Float((float) (x * scale));
        }

        @Override
        protected Float combine(double a, Vector<?> other, double b) {
            return new Float((float) (a * x + b * other.get(0)));
        }

        @Override
        protected Float combineAccurate(double a, Vector<?> other, double b) {
            return new Float((float) (LinearCombination.of(a, x, b, other.get(0))));
        }

        @Override
        public Float with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Float((float) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Float lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new Float((float) (other.get(0)));
            }
            return new Float(
                    (float) (x + ratio * (other.get(0) - x)));
        }

        @Override
        public Float copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return java.lang.Float.isFinite(x);
        }

        @Override
        public boolean isNaN() {
            return java.lang.Float.isNaN(x);
        }

        @Override
        public boolean isInfinite() {
            return !isNaN() && (java.lang.Float.isInfinite(x));
        }

        @Override
        public double distanceSquare(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            final double dx = x - other.get(0);
            return dx*dx;
        }

    }

    public static class Double extends Vector1D<Double> {

        public final double x;

        public Double() {
            this.x = 0;
        }

        public Double(double x) {
            this.x = x;
        }

        public Double(double[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public Double(float[] array) {
            ensureLength(array.length, 1);
            this.x = array[0];
        }

        public Double(Vector<?> tuple) {
            ensureDimension(tuple, 1);
            this.x = tuple.get(0);
        }

        @Override
        public DataType getDataType() {
            return DataType.DOUBLE;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public double length() {
            return Math.sqrt(x*x);
        }

        @Override
        public double lengthSquare() {
            return x*x;
        }

        @Override
        public double dot(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return x * other.get(0);
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int)x};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{(float)x};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset] = (short) x;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset] = (int) x;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset] = (float) x;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset] = x;
        }

        @Override
        public Double add(Vector<?> other) {
            ensureDimension(other, 1);
            return new Double(x + other.get(0));
        }

        @Override
        public Double subtract(Vector<?> other) {
            ensureDimension(other, 1);
            return new Double(x - other.get(0));
        }

        @Override
        public Double multiply(Vector<?> other) {
            ensureDimension(other, 1);
            return new Double(x * other.get(0));
        }

        @Override
        public Double divide(Vector<?> other) {
            ensureDimension(other, 1);
            return new Double(x / other.get(0));
        }

        @Override
        public Double scale(double scale) {
            return new Double(x * scale);
        }

        @Override
        protected Double combine(double a, Vector<?> other, double b) {
            return new Double(a * x + b * other.get(0));
        }

        @Override
        protected Double combineAccurate(double a, Vector<?> other, double b) {
            return new Double(LinearCombination.of(a, x, b, other.get(0)));
        }

        @Override
        public Double with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Double(value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Double lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 1);
            if (ratio == 1) {
                return new Double(other.get(0));
            }
            return new Double(
                    x + ratio * (other.get(0) - x));
        }

        @Override
        public Double copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return java.lang.Double.isFinite(x);
        }

        @Override
        public boolean isNaN() {
            return java.lang.Double.isNaN(x);
        }

        @Override
        public boolean isInfinite() {
            return !isNaN() && (java.lang.Double.isInfinite(x));
        }

        @Override
        public double distanceSquare(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            final double dx = x - other.get(0);
            return dx*dx;
        }

    }
}
