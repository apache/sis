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
public abstract class Vector2D<T extends Vector2D<T>> extends AbstractVector<T> {

    public Vector2D() {
    }

    /**
     * Create a new vector of the same type as this vector with the given values.
     *
     * @param x first value
     * @param y second value
     * @return new vector
     */
    protected abstract T create(double x, double y);

    /**
     * Create a new vector, this vector rotated around given reference point.
     * This vector is unchanged.
     *
     * @param ref reference point to rotate around
     * @param angleRad angle in radians
     * @return new vector
     */
    public T _rotate(Vector<?> ref, double angleRad) {
        ensureDimension(ref, 2);
        final double sin = Math.sin(angleRad);
        final double cos = Math.cos(angleRad);

        final double relx = get(0) - ref.get(0);
        final double rely = get(1) - ref.get(1);

        return create(relx * cos - rely * sin + ref.get(0),
                      relx * sin + rely * cos + ref.get(1));
    }

    @Override
    public final int getDimension() {
        return 2;
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
        ensureDimension(unit, 2);
        return combine(1, unit, -dot(unit));
    }

    @Override
    public abstract T lerp(Vector<?> other, double ratio);

    @Override
    public T lerpAccurate(Vector<?> other, double ratio) {
        ensureDimension(other, 2);
        return combineAccurate(1 - ratio, other, ratio);
    }

    @Override
    public T slerp(Vector<?> other, double ratio) {
        return AbstractVector.slerp(this, other, ratio, (a, b) -> combine(a, other, b));
    }

    @Override
    public T vectorTo(Vector<?> other) {
        ensureDimension(other, 2);
        return combine(-1, other, 1);
    }

    @Override
    public double lengthSquare() {
        final double v0 = get(0);
        final double v1 = get(1);
        return v0*v0 + v1*v1;
    }

    @Override
    public double dot(Vector<?> other) {
        ensureDimension(other, 2);
        return get(0) * other.get(0)
             + get(1) * other.get(1);
    }

    @Override
    public double dotAccurate(Vector<?> other) {
        ensureDimension(other, 2);
        return LinearCombination.of(get(0), other.get(0), get(1), other.get(1));
    }

    @Override
    public double distanceSquare(Vector<?> other) {
        ensureDimension(other, 2);
        final double d0 = get(0) - other.get(0);
        final double d1 = get(1) - other.get(1);
        return d0*d0 + d1*d1;
    }

    public static class Byte extends Vector2D<Byte> {

        public final byte x;
        public final byte y;

        public Byte() {
            this.x = 0;
            this.y = 0;
        }

        public Byte(byte x, byte y) {
            this.x = x;
            this.y = y;
        }

        public Byte(byte[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public Byte(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = (byte) tuple.get(0);
            this.y = (byte) tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.BYTE;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x,y};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x,y};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public Byte add(Vector<?> other) {
            ensureDimension(other, 2);
            return new Byte(
                    (byte) (x + other.get(0)),
                    (byte) (y + other.get(1)));
        }

        @Override
        public Byte subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new Byte(
                    (byte) (x - other.get(0)),
                    (byte) (y - other.get(1)));
        }

        @Override
        public Byte multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new Byte(
                    (byte) (x * other.get(0)),
                    (byte) (y * other.get(1)));
        }

        @Override
        public Byte divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new Byte(
                    (byte) (x / other.get(0)),
                    (byte) (y / other.get(1)));
        }

        @Override
        public Byte scale(double scale) {
            return new Byte(
                    (byte) (x * scale),
                    (byte) (y * scale));
        }

        @Override
        protected Byte combine(double a, Vector<?> other, double b) {
            return new Byte(
                    (byte) (a * x + b * other.get(0)),
                    (byte) (a * y + b * other.get(1)));
        }

        @Override
        protected Byte combineAccurate(double a, Vector<?> other, double b) {
            return new Byte(
                    (byte) (LinearCombination.of(a, x, b, other.get(0))),
                    (byte) (LinearCombination.of(a, y, b, other.get(1))));
        }

        @Override
        protected Byte create(double x, double y) {
            return new Byte((byte) x, (byte) y);
        }

        @Override
        public Byte with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Byte((byte) value, y);
                case 1 : return new Byte(x, (byte) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Byte lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new Byte((byte) (other.get(0)), (byte) (other.get(1)));
            }
            return new Byte(
                    (byte) (x + ratio * (other.get(0) - x)),
                    (byte) (y + ratio * (other.get(1) - y)));
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

    public static class UByte extends Vector2D<UByte> {

        public final byte x;
        public final byte y;

        public UByte() {
            this.x = 0;
            this.y = 0;
        }

        public UByte(byte x, byte y) {
            this.x = x;
            this.y = y;
        }

        public UByte(byte[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public UByte(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = (byte) tuple.get(0);
            this.y = (byte) tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.UBYTE;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x & 0xFF;
                case 1 : return y & 0xFF;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x & 0xFF,y & 0xFF};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x & 0xFF,y & 0xFF};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x & 0xFF,y & 0xFF};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) (x & 0xFF);
            buffer[offset+1] = (short) (y & 0xFF);
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x & 0xFF;
            buffer[offset+1] = y & 0xFF;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x & 0xFF;
            buffer[offset+1] = y & 0xFF;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x & 0xFF;
            buffer[offset+1] = y & 0xFF;
        }

        @Override
        public UByte add(Vector<?> other) {
            ensureDimension(other, 2);
            return new UByte(
                    (byte) ((x & 0xFF) + other.get(0)),
                    (byte) ((y & 0xFF) + other.get(1)));
        }

        @Override
        public UByte subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new UByte(
                    (byte) ((x & 0xFF) - other.get(0)),
                    (byte) ((y & 0xFF) - other.get(1)));
        }

        @Override
        public UByte multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new UByte(
                    (byte) ((x & 0xFF) * other.get(0)),
                    (byte) ((y & 0xFF) * other.get(1)));
        }

        @Override
        public UByte divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new UByte(
                    (byte) ((x & 0xFF) / other.get(0)),
                    (byte) ((y & 0xFF) / other.get(1)));
        }

        @Override
        public UByte scale(double scale) {
            return new UByte(
                    (byte) ((x & 0xFF) * scale),
                    (byte) ((y & 0xFF) * scale));
        }

        @Override
        protected UByte combine(double a, Vector<?> other, double b) {
            return new UByte(
                    (byte) (a * (x & 0xFF) + b * other.get(0)),
                    (byte) (a * (y & 0xFF) + b * other.get(1)));
        }

        @Override
        protected UByte combineAccurate(double a, Vector<?> other, double b) {
            return new UByte(
                    (byte) (LinearCombination.of(a, (x & 0xFF), b, other.get(0))),
                    (byte) (LinearCombination.of(a, (y & 0xFF), b, other.get(1))));
        }

        @Override
        protected UByte create(double x, double y) {
            return new UByte((byte) x, (byte) y);
        }

        @Override
        public UByte with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UByte((byte) value, y);
                case 1 : return new UByte(x, (byte) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UByte lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new UByte((byte) (other.get(0)), (byte) (other.get(1)));
            }
            return new UByte(
                    (byte) ((x & 0xFF) + ratio * (other.get(0) - (x & 0xFF))),
                    (byte) ((y & 0xFF) + ratio * (other.get(1) - (y & 0xFF))));
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

    public static class Short extends Vector2D<Short> {

        public final short x;
        public final short y;

        public Short() {
            this.x = 0;
            this.y = 0;
        }

        public Short(short x, short y) {
            this.x = x;
            this.y = y;
        }

        public Short(short[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public Short(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = (short) tuple.get(0);
            this.y = (short) tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.SHORT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x,y};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x,y};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public Short add(Vector<?> other) {
            ensureDimension(other, 2);
            return new Short(
                    (short) (x + other.get(0)),
                    (short) (y + other.get(1)));
        }

        @Override
        public Short subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new Short(
                    (short) (x - other.get(0)),
                    (short) (y - other.get(1)));
        }

        @Override
        public Short multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new Short(
                    (short) (x * other.get(0)),
                    (short) (y * other.get(1)));
        }

        @Override
        public Short divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new Short(
                    (short) (x / other.get(0)),
                    (short) (y / other.get(1)));
        }

        @Override
        public Short scale(double scale) {
            return new Short(
                    (short) (x * scale),
                    (short) (y * scale));
        }

        @Override
        protected Short combine(double a, Vector<?> other, double b) {
            return new Short(
                    (short) (a * x + b * other.get(0)),
                    (short) (a * y + b * other.get(1)));
        }

        @Override
        protected Short combineAccurate(double a, Vector<?> other, double b) {
            return new Short(
                    (short) (LinearCombination.of(a, x, b, other.get(0))),
                    (short) (LinearCombination.of(a, y, b, other.get(1))));
        }

        @Override
        protected Short create(double x, double y) {
            return new Short((short) x, (short) y);
        }

        @Override
        public Short with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Short((short) value, y);
                case 1 : return new Short(x, (short) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Short lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new Short((short) (other.get(0)), (short) (other.get(1)));
            }
            return new Short(
                    (short) (x + ratio * (other.get(0) - x)),
                    (short) (y + ratio * (other.get(1) - y)));
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

    public static class UShort extends Vector2D<UShort> {

        public final short x;
        public final short y;

        public UShort() {
            this.x = 0;
            this.y = 0;
        }

        public UShort(short x, short y) {
            this.x = x;
            this.y = y;
        }

        public UShort(short[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public UShort(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = (short) tuple.get(0);
            this.y = (short) tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.USHORT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return java.lang.Short.toUnsignedInt(x);
                case 1 : return java.lang.Short.toUnsignedInt(y);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{java.lang.Short.toUnsignedInt(x),java.lang.Short.toUnsignedInt(y)};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{java.lang.Short.toUnsignedInt(x),java.lang.Short.toUnsignedInt(y)};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{java.lang.Short.toUnsignedInt(x),java.lang.Short.toUnsignedInt(y)};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = java.lang.Short.toUnsignedInt(x);
            buffer[offset+1] = java.lang.Short.toUnsignedInt(y);
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = java.lang.Short.toUnsignedInt(x);
            buffer[offset+1] = java.lang.Short.toUnsignedInt(y);
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = java.lang.Short.toUnsignedInt(x);
            buffer[offset+1] = java.lang.Short.toUnsignedInt(y);
        }

        @Override
        public UShort add(Vector<?> other) {
            ensureDimension(other, 2);
            return new UShort(
                    (short) ((x & 0xFFFF) + other.get(0)),
                    (short) ((y & 0xFFFF) + other.get(1)));
        }

        @Override
        public UShort subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new UShort(
                    (short) ((x & 0xFFFF) - other.get(0)),
                    (short) ((y & 0xFFFF) - other.get(1)));
        }

        @Override
        public UShort multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new UShort(
                    (short) ((x & 0xFFFF) * other.get(0)),
                    (short) ((y & 0xFFFF) * other.get(1)));
        }

        @Override
        public UShort divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new UShort(
                    (short) ((x & 0xFFFF) / other.get(0)),
                    (short) ((y & 0xFFFF) / other.get(1)));
        }

        @Override
        public UShort scale(double scale) {
            return new UShort(
                    (short) ((x & 0xFFFF) * scale),
                    (short) ((y & 0xFFFF) * scale));
        }

        @Override
        protected UShort combine(double a, Vector<?> other, double b) {
            return new UShort(
                    (short) (a * (x & 0xFFFF) + b * other.get(0)),
                    (short) (a * (y & 0xFFFF) + b * other.get(1)));
        }

        @Override
        protected UShort combineAccurate(double a, Vector<?> other, double b) {
            return new UShort(
                    (short) (LinearCombination.of(a, (x & 0xFFFF), b, other.get(0))),
                    (short) (LinearCombination.of(a, (y & 0xFFFF), b, other.get(1))));
        }

        @Override
        protected UShort create(double x, double y) {
            return new UShort((short) x, (short) y);
        }

        @Override
        public UShort with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UShort((short) value, y);
                case 1 : return new UShort(x, (short) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UShort lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new UShort((short) (other.get(0)), (short) (other.get(1)));
            }
            return new UShort(
                    (short) ((x & 0xFFFF) + ratio * (other.get(0) - (x & 0xFFFF))),
                    (short) ((y & 0xFFFF) + ratio * (other.get(1) - (y & 0xFFFF))));
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

    public static class Int extends Vector2D<Int> {

        public final int x;
        public final int y;

        public Int() {
            this.x = 0;
            this.y = 0;
        }

        public Int(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Int(int[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public Int(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = (int) tuple.get(0);
            this.y = (int) tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.INT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x,y};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x,y};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public Int add(Vector<?> other) {
            ensureDimension(other, 2);
            return new Int(
                    (int) (x + other.get(0)),
                    (int) (y + other.get(1)));
        }

        @Override
        public Int subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new Int(
                    (int) (x - other.get(0)),
                    (int) (y - other.get(1)));
        }

        @Override
        public Int multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new Int(
                    (int) (x * other.get(0)),
                    (int) (y * other.get(1)));
        }

        @Override
        public Int divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new Int(
                    (int) (x / other.get(0)),
                    (int) (y / other.get(1)));
        }

        @Override
        public Int scale(double scale) {
            return new Int(
                    (int) (x * scale),
                    (int) (y * scale));
        }

        @Override
        protected Int combine(double a, Vector<?> other, double b) {
            return new Int(
                    (int) (a * x + b * other.get(0)),
                    (int) (a * y + b * other.get(1)));
        }

        @Override
        protected Int combineAccurate(double a, Vector<?> other, double b) {
            return new Int(
                    (int) (LinearCombination.of(a, x, b, other.get(0))),
                    (int) (LinearCombination.of(a, y, b, other.get(1))));
        }

        @Override
        protected Int create(double x, double y) {
            return new Int((int) x, (int) y);
        }

        @Override
        public Int with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Int((int) value, y);
                case 1 : return new Int(x, (int) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Int lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new Int((int) (other.get(0)), (int) (other.get(1)));
            }
            return new Int(
                    (int) (x + ratio * (other.get(0) - x)),
                    (int) (y + ratio * (other.get(1) - y)));
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

    public static class UInt extends Vector2D<UInt> {

        public final int x;
        public final int y;

        public UInt() {
            this.x = 0;
            this.y = 0;
        }

        public UInt(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public UInt(int[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public UInt(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = (int) (long) tuple.get(0);
            this.y = (int) (long) tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.UINT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return Integer.toUnsignedLong(x);
                case 1 : return Integer.toUnsignedLong(y);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x,y};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{Integer.toUnsignedLong(x),Integer.toUnsignedLong(y)};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{Integer.toUnsignedLong(x),Integer.toUnsignedLong(y)};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) Integer.toUnsignedLong(x);
            buffer[offset+1] = (short) Integer.toUnsignedLong(y);
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = Integer.toUnsignedLong(x);
            buffer[offset+1] = Integer.toUnsignedLong(y);
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = Integer.toUnsignedLong(x);
            buffer[offset+1] = Integer.toUnsignedLong(y);
        }

        @Override
        public UInt add(Vector<?> other) {
            ensureDimension(other, 2);
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) + other.get(0)),
                    (int) (long) (Integer.toUnsignedLong(y) + other.get(1)));
        }

        @Override
        public UInt subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) - other.get(0)),
                    (int) (long) (Integer.toUnsignedLong(y) - other.get(1)));
        }

        @Override
        public UInt multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) * other.get(0)),
                    (int) (long) (Integer.toUnsignedLong(y) * other.get(1)));
        }

        @Override
        public UInt divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) / other.get(0)),
                    (int) (long) (Integer.toUnsignedLong(y) / other.get(1)));
        }

        @Override
        public UInt scale(double scale) {
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) * scale),
                    (int) (long) (Integer.toUnsignedLong(y) * scale));
        }

        @Override
        protected UInt combine(double a, Vector<?> other, double b) {
            return new UInt(
                    (int) (long) (a * Integer.toUnsignedLong(x) + b * other.get(0)),
                    (int) (long) (a * Integer.toUnsignedLong(y) + b * other.get(1)));
        }

        @Override
        protected UInt combineAccurate(double a, Vector<?> other, double b) {
            return new UInt(
                    (int) (long) (LinearCombination.of(a, Integer.toUnsignedLong(x), b, other.get(0))),
                    (int) (long) (LinearCombination.of(a, Integer.toUnsignedLong(y), b, other.get(1))));
        }

        @Override
        protected UInt create(double x, double y) {
            return new UInt((int) (long) x, (int) (long) y);
        }

        @Override
        public UInt with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UInt((int) (long) value, y);
                case 1 : return new UInt(x, (int) (long) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UInt lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new UInt((int) (long) (other.get(0)), (int) (long) (other.get(1)));
            }
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) + ratio * (other.get(0) - Integer.toUnsignedLong(x))),
                    (int) (long) (Integer.toUnsignedLong(y) + ratio * (other.get(1) - Integer.toUnsignedLong(y))));
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

    public static class Long extends Vector2D<Long> {

        public final long x;
        public final long y;

        public Long() {
            this.x = 0;
            this.y = 0;
        }

        public Long(long x, long y) {
            this.x = x;
            this.y = y;
        }

        public Long(long[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public Long(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = (long) tuple.get(0);
            this.y = (long) tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.LONG;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int) x, (int) y};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x,y};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = (int) x;
            buffer[offset+1] = (int) y;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public Long add(Vector<?> other) {
            ensureDimension(other, 2);
            return new Long(
                    (long) (x + other.get(0)),
                    (long) (y + other.get(1)));
        }

        @Override
        public Long subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new Long(
                    (long) (x - other.get(0)),
                    (long) (y - other.get(1)));
        }

        @Override
        public Long multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new Long(
                    (long) (x * other.get(0)),
                    (long) (y * other.get(1)));
        }

        @Override
        public Long divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new Long(
                    (long) (x / other.get(0)),
                    (long) (y / other.get(1)));
        }

        @Override
        public Long scale(double scale) {
            return new Long(
                    (long) (x * scale),
                    (long) (y * scale));
        }

        @Override
        protected Long combine(double a, Vector<?> other, double b) {
            return new Long(
                    (long) (a * x + b * other.get(0)),
                    (long) (a * y + b * other.get(1)));
        }

        @Override
        protected Long combineAccurate(double a, Vector<?> other, double b) {
            return new Long(
                    (long) (LinearCombination.of(a, x, b, other.get(0))),
                    (long) (LinearCombination.of(a, y, b, other.get(1))));
        }

        @Override
        protected Long create(double x, double y) {
            return new Long((long) x, (long) y);
        }

        @Override
        public Long with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Long((long) value, y);
                case 1 : return new Long(x, (long) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Long lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new Long((long) (other.get(0)), (long) (other.get(1)));
            }
            return new Long(
                    (long) (x + ratio * (other.get(0) - x)),
                    (long) (y + ratio * (other.get(1) - y)));
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

    public static class Float extends Vector2D<Float> {

        public final float x;
        public final float y;

        public Float() {
            this.x = 0;
            this.y = 0;
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Float(float[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public Float(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = (float) tuple.get(0);
            this.y = (float) tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.FLOAT;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public double length() {
            return Math.sqrt(x*x + y*y);
        }

        @Override
        public double lengthSquare() {
            return x*x + y*y;
        }

        @Override
        public double dot(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return x * other.get(0)
                 + y * other.get(1);
        }

        @Override
        public double dotAccurate(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return LinearCombination.of(x, other.get(0), y, other.get(1));
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int)x, (int)y};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x,y};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = (int) x;
            buffer[offset+1] = (int) y;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public Float add(Vector<?> other) {
            ensureDimension(other, 2);
            return new Float(
                    (float) (x + other.get(0)),
                    (float) (y + other.get(1)));
        }

        @Override
        public Float subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new Float(
                    (float) (x - other.get(0)),
                    (float) (y - other.get(1)));
        }

        @Override
        public Float multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new Float(
                    (float) (x * other.get(0)),
                    (float) (y * other.get(1)));
        }

        @Override
        public Float divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new Float(
                    (float) (x / other.get(0)),
                    (float) (y / other.get(1)));
        }

        @Override
        public Float scale(double scale) {
            return new Float(
                    (float) (x * scale),
                    (float) (y * scale));
        }

        @Override
        protected Float combine(double a, Vector<?> other, double b) {
            return new Float(
                    (float) (a * x + b * other.get(0)),
                    (float) (a * y + b * other.get(1)));
        }

        @Override
        protected Float combineAccurate(double a, Vector<?> other, double b) {
            return new Float(
                    (float) (LinearCombination.of(a, x, b, other.get(0))),
                    (float) (LinearCombination.of(a, y, b, other.get(1))));
        }

        @Override
        protected Float create(double x, double y) {
            return new Float((float) x, (float) y);
        }

        @Override
        public Float with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Float((float) value, y);
                case 1 : return new Float(x, (float) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Float lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new Float((float) (other.get(0)), (float) (other.get(1)));
            }
            return new Float(
                    (float) (x + ratio * (other.get(0) - x)),
                    (float) (y + ratio * (other.get(1) - y)));
        }

        @Override
        public Float copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return java.lang.Float.isFinite(x) && java.lang.Float.isFinite(y);
        }

        @Override
        public boolean isNaN() {
            return java.lang.Float.isNaN(x) || java.lang.Float.isNaN(y);
        }

        @Override
        public boolean isInfinite() {
            return !isNaN() && (java.lang.Float.isInfinite(x) || java.lang.Float.isInfinite(y));
        }

        @Override
        public double distanceSquare(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            final double dx = x - other.get(0);
            final double dy = y - other.get(1);
            return dx*dx + dy*dy;
        }

    }

    public static class Double extends Vector2D<Double> {

        public final double x;
        public final double y;

        public Double() {
            this.x = 0;
            this.y = 0;
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Double(double[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public Double(float[] array) {
            ensureLength(array.length, 2);
            this.x = array[0];
            this.y = array[1];
        }

        public Double(Vector<?> tuple) {
            ensureDimension(tuple, 2);
            this.x = tuple.get(0);
            this.y = tuple.get(1);
        }

        @Override
        public DataType getDataType() {
            return DataType.DOUBLE;
        }

        @Override
        public double get(int indice) {
            switch (indice) {
                case 0 : return x;
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public double length() {
            return Math.sqrt(x*x + y*y);
        }

        @Override
        public double lengthSquare() {
            return x*x + y*y;
        }

        @Override
        public double dot(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return x * other.get(0)
                 + y * other.get(1);
        }

        @Override
        public double dotAccurate(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return LinearCombination.of(x, other.get(0), y, other.get(1));
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int)x, (int)y};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{(float)x,(float)y};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = (int) x;
            buffer[offset+1] = (int) y;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = (float) x;
            buffer[offset+1] = (float) y;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
        }

        @Override
        public Double add(Vector<?> other) {
            ensureDimension(other, 2);
            return new Double(
                    x + other.get(0),
                    y + other.get(1));
        }

        @Override
        public Double subtract(Vector<?> other) {
            ensureDimension(other, 2);
            return new Double(
                    x - other.get(0),
                    y - other.get(1));
        }

        @Override
        public Double multiply(Vector<?> other) {
            ensureDimension(other, 2);
            return new Double(
                    x * other.get(0),
                    y * other.get(1));
        }

        @Override
        public Double divide(Vector<?> other) {
            ensureDimension(other, 2);
            return new Double(
                    x / other.get(0),
                    y / other.get(1));
        }

        @Override
        public Double scale(double scale) {
            return new Double(
                    x * scale,
                    y * scale);
        }

        @Override
        protected Double combine(double a, Vector<?> other, double b) {
            return new Double(
                    a * x + b * other.get(0),
                    a * y + b * other.get(1));
        }

        @Override
        protected Double combineAccurate(double a, Vector<?> other, double b) {
            return new Double(
                    LinearCombination.of(a, x, b, other.get(0)),
                    LinearCombination.of(a, y, b, other.get(1)));
        }

        @Override
        protected Double create(double x, double y) {
            return new Double(x, y);
        }

        @Override
        public Double with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Double(value, y);
                case 1 : return new Double(x, value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Double lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 2);
            if (ratio == 1) {
                return new Double(other.get(0), other.get(1));
            }
            return new Double(
                    x + ratio * (other.get(0) - x),
                    y + ratio * (other.get(1) - y));
        }

        @Override
        public Double copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return java.lang.Double.isFinite(x) && java.lang.Double.isFinite(y);
        }

        @Override
        public boolean isNaN() {
            return java.lang.Double.isNaN(x) || java.lang.Double.isNaN(y);
        }

        @Override
        public boolean isInfinite() {
            return !isNaN() && (java.lang.Double.isInfinite(x) || java.lang.Double.isInfinite(y));
        }

        @Override
        public double distanceSquare(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            final double dx = x - other.get(0);
            final double dy = y - other.get(1);
            return dx*dx + dy*dy;
        }

    }

}
