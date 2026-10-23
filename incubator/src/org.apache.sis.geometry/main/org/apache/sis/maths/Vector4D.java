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
public abstract class Vector4D<T extends Vector4D<T>> extends AbstractVector<T> {

    public Vector4D() {
    }

    @Override
    public final int getDimension() {
        return 4;
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
        ensureDimension(unit, 4);
        return combine(1, unit, -dot(unit));
    }

    @Override
    public abstract T lerp(Vector<?> other, double ratio);

    @Override
    public T lerpAccurate(Vector<?> other, double ratio) {
        ensureDimension(other, 4);
        return combineAccurate(1 - ratio, other, ratio);
    }

    @Override
    public T slerp(Vector<?> other, double ratio) {
        return AbstractVector.slerp(this, other, ratio, (a, b) -> combine(a, other, b));
    }

    @Override
    public T vectorTo(Vector<?> other) {
        ensureDimension(other, 4);
        return combine(-1, other, 1);
    }

    @Override
    public double lengthSquare() {
        final double v0 = get(0);
        final double v1 = get(1);
        final double v2 = get(2);
        final double v3 = get(3);
        return v0*v0 + v1*v1 + v2*v2 + v3*v3;
    }

    @Override
    public double dot(Vector<?> other) {
        ensureDimension(other, 4);
        return get(0) * other.get(0)
             + get(1) * other.get(1)
             + get(2) * other.get(2)
             + get(3) * other.get(3);
    }

    @Override
    public double dotAccurate(Vector<?> other) {
        ensureDimension(other, 4);
        return LinearCombination.of(get(0), other.get(0), get(1), other.get(1), get(2), other.get(2), get(3), other.get(3));
    }

    @Override
    public double distanceSquare(Vector<?> other) {
        ensureDimension(other, 4);
        final double d0 = get(0) - other.get(0);
        final double d1 = get(1) - other.get(1);
        final double d2 = get(2) - other.get(2);
        final double d3 = get(3) - other.get(3);
        return d0*d0 + d1*d1 + d2*d2 + d3*d3;
    }

    public static class Byte extends Vector4D<Byte> {

        public final byte x;
        public final byte y;
        public final byte z;
        public final byte w;

        public Byte() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public Byte(byte x, byte y, byte z, byte w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Byte(byte[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Byte(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = (byte) tuple.get(0);
            this.y = (byte) tuple.get(1);
            this.z = (byte) tuple.get(2);
            this.w = (byte) tuple.get(3);
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x, y, z, w};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x, y, z, w};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y,z,w};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public Byte add(Vector<?> other) {
            ensureDimension(other, 4);
            return new Byte(
                    (byte) (x + other.get(0)),
                    (byte) (y + other.get(1)),
                    (byte) (z + other.get(2)),
                    (byte) (w + other.get(3)));
        }

        @Override
        public Byte subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new Byte(
                    (byte) (x - other.get(0)),
                    (byte) (y - other.get(1)),
                    (byte) (z - other.get(2)),
                    (byte) (w - other.get(3)));
        }

        @Override
        public Byte multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new Byte(
                    (byte) (x * other.get(0)),
                    (byte) (y * other.get(1)),
                    (byte) (z * other.get(2)),
                    (byte) (w * other.get(3)));
        }

        @Override
        public Byte divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new Byte(
                    (byte) (x / other.get(0)),
                    (byte) (y / other.get(1)),
                    (byte) (z / other.get(2)),
                    (byte) (w / other.get(3)));
        }

        @Override
        public Byte scale(double scale) {
            return new Byte(
                    (byte) (x * scale),
                    (byte) (y * scale),
                    (byte) (z * scale),
                    (byte) (w * scale));
        }

        @Override
        protected Byte combine(double a, Vector<?> other, double b) {
            return new Byte(
                    (byte) (a * x + b * other.get(0)),
                    (byte) (a * y + b * other.get(1)),
                    (byte) (a * z + b * other.get(2)),
                    (byte) (a * w + b * other.get(3)));
        }

        @Override
        protected Byte combineAccurate(double a, Vector<?> other, double b) {
            return new Byte(
                    (byte) (LinearCombination.of(a, x, b, other.get(0))),
                    (byte) (LinearCombination.of(a, y, b, other.get(1))),
                    (byte) (LinearCombination.of(a, z, b, other.get(2))),
                    (byte) (LinearCombination.of(a, w, b, other.get(3))));
        }

        @Override
        public Byte with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Byte((byte) value, y, z, w);
                case 1 : return new Byte(x, (byte) value, z, w);
                case 2 : return new Byte(x, y, (byte) value, w);
                case 3 : return new Byte(x, y, z, (byte) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Byte lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new Byte((byte) (other.get(0)), (byte) (other.get(1)), (byte) (other.get(2)), (byte) (other.get(3)));
            }
            return new Byte(
                    (byte) (x + ratio * (other.get(0) - x)),
                    (byte) (y + ratio * (other.get(1) - y)),
                    (byte) (z + ratio * (other.get(2) - z)),
                    (byte) (w + ratio * (other.get(3) - w)));
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

    public static class UByte extends Vector4D<UByte> {

        public final byte x;
        public final byte y;
        public final byte z;
        public final byte w;

        public UByte() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public UByte(byte x, byte y, byte z, byte w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UByte(byte[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public UByte(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = (byte) tuple.get(0);
            this.y = (byte) tuple.get(1);
            this.z = (byte) tuple.get(2);
            this.w = (byte) tuple.get(3);
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
                case 2 : return z & 0xFF;
                case 3 : return w & 0xFF;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x & 0xFF, y & 0xFF, z & 0xFF, w & 0xFF};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x & 0xFF, y & 0xFF, z & 0xFF, w & 0xFF};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x & 0xFF,y & 0xFF,z & 0xFF,w & 0xFF};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) (x & 0xFF);
            buffer[offset+1] = (short) (y & 0xFF);
            buffer[offset+2] = (short) (z & 0xFF);
            buffer[offset+3] = (short) (w & 0xFF);
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x & 0xFF;
            buffer[offset+1] = y & 0xFF;
            buffer[offset+2] = z & 0xFF;
            buffer[offset+3] = w & 0xFF;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x & 0xFF;
            buffer[offset+1] = y & 0xFF;
            buffer[offset+2] = z & 0xFF;
            buffer[offset+3] = w & 0xFF;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x & 0xFF;
            buffer[offset+1] = y & 0xFF;
            buffer[offset+2] = z & 0xFF;
            buffer[offset+3] = w & 0xFF;
        }

        @Override
        public UByte add(Vector<?> other) {
            ensureDimension(other, 4);
            return new UByte(
                    (byte) ((x & 0xFF) + other.get(0)),
                    (byte) ((y & 0xFF) + other.get(1)),
                    (byte) ((z & 0xFF) + other.get(2)),
                    (byte) ((w & 0xFF) + other.get(3)));
        }

        @Override
        public UByte subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new UByte(
                    (byte) ((x & 0xFF) - other.get(0)),
                    (byte) ((y & 0xFF) - other.get(1)),
                    (byte) ((z & 0xFF) - other.get(2)),
                    (byte) ((w & 0xFF) - other.get(3)));
        }

        @Override
        public UByte multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new UByte(
                    (byte) ((x & 0xFF) * other.get(0)),
                    (byte) ((y & 0xFF) * other.get(1)),
                    (byte) ((z & 0xFF) * other.get(2)),
                    (byte) ((w & 0xFF) * other.get(3)));
        }

        @Override
        public UByte divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new UByte(
                    (byte) ((x & 0xFF) / other.get(0)),
                    (byte) ((y & 0xFF) / other.get(1)),
                    (byte) ((z & 0xFF) / other.get(2)),
                    (byte) ((w & 0xFF) / other.get(3)));
        }

        @Override
        public UByte scale(double scale) {
            return new UByte(
                    (byte) ((x & 0xFF) * scale),
                    (byte) ((y & 0xFF) * scale),
                    (byte) ((z & 0xFF) * scale),
                    (byte) ((w & 0xFF) * scale));
        }

        @Override
        protected UByte combine(double a, Vector<?> other, double b) {
            return new UByte(
                    (byte) (a * (x & 0xFF) + b * other.get(0)),
                    (byte) (a * (y & 0xFF) + b * other.get(1)),
                    (byte) (a * (z & 0xFF) + b * other.get(2)),
                    (byte) (a * (w & 0xFF) + b * other.get(3)));
        }

        @Override
        protected UByte combineAccurate(double a, Vector<?> other, double b) {
            return new UByte(
                    (byte) (LinearCombination.of(a, (x & 0xFF), b, other.get(0))),
                    (byte) (LinearCombination.of(a, (y & 0xFF), b, other.get(1))),
                    (byte) (LinearCombination.of(a, (z & 0xFF), b, other.get(2))),
                    (byte) (LinearCombination.of(a, (w & 0xFF), b, other.get(3))));
        }

        @Override
        public UByte with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UByte((byte) value, y, z, w);
                case 1 : return new UByte(x, (byte) value, z, w);
                case 2 : return new UByte(x, y, (byte) value, w);
                case 3 : return new UByte(x, y, z, (byte) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UByte lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new UByte((byte) (other.get(0)), (byte) (other.get(1)), (byte) (other.get(2)), (byte) (other.get(3)));
            }
            return new UByte(
                    (byte) ((x & 0xFF) + ratio * (other.get(0) - (x & 0xFF))),
                    (byte) ((y & 0xFF) + ratio * (other.get(1) - (y & 0xFF))),
                    (byte) ((z & 0xFF) + ratio * (other.get(2) - (z & 0xFF))),
                    (byte) ((w & 0xFF) + ratio * (other.get(3) - (w & 0xFF))));
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

    public static class Short extends Vector4D<Short> {

        public final short x;
        public final short y;
        public final short z;
        public final short w;

        public Short() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public Short(short x, short y, short z, short w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Short(short[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Short(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = (short) tuple.get(0);
            this.y = (short) tuple.get(1);
            this.z = (short) tuple.get(2);
            this.w = (short) tuple.get(3);
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x, y, z, w};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x, y, z, w};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y,z,w};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public Short add(Vector<?> other) {
            ensureDimension(other, 4);
            return new Short(
                    (short) (x + other.get(0)),
                    (short) (y + other.get(1)),
                    (short) (z + other.get(2)),
                    (short) (w + other.get(3)));
        }

        @Override
        public Short subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new Short(
                    (short) (x - other.get(0)),
                    (short) (y - other.get(1)),
                    (short) (z - other.get(2)),
                    (short) (w - other.get(3)));
        }

        @Override
        public Short multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new Short(
                    (short) (x * other.get(0)),
                    (short) (y * other.get(1)),
                    (short) (z * other.get(2)),
                    (short) (w * other.get(3)));
        }

        @Override
        public Short divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new Short(
                    (short) (x / other.get(0)),
                    (short) (y / other.get(1)),
                    (short) (z / other.get(2)),
                    (short) (w / other.get(3)));
        }

        @Override
        public Short scale(double scale) {
            return new Short(
                    (short) (x * scale),
                    (short) (y * scale),
                    (short) (z * scale),
                    (short) (w * scale));
        }

        @Override
        protected Short combine(double a, Vector<?> other, double b) {
            return new Short(
                    (short) (a * x + b * other.get(0)),
                    (short) (a * y + b * other.get(1)),
                    (short) (a * z + b * other.get(2)),
                    (short) (a * w + b * other.get(3)));
        }

        @Override
        protected Short combineAccurate(double a, Vector<?> other, double b) {
            return new Short(
                    (short) (LinearCombination.of(a, x, b, other.get(0))),
                    (short) (LinearCombination.of(a, y, b, other.get(1))),
                    (short) (LinearCombination.of(a, z, b, other.get(2))),
                    (short) (LinearCombination.of(a, w, b, other.get(3))));
        }

        @Override
        public Short with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Short((short) value, y, z, w);
                case 1 : return new Short(x, (short) value, z, w);
                case 2 : return new Short(x, y, (short) value, w);
                case 3 : return new Short(x, y, z, (short) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Short lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new Short((short) (other.get(0)), (short) (other.get(1)), (short) (other.get(2)), (short) (other.get(3)));
            }
            return new Short(
                    (short) (x + ratio * (other.get(0) - x)),
                    (short) (y + ratio * (other.get(1) - y)),
                    (short) (z + ratio * (other.get(2) - z)),
                    (short) (w + ratio * (other.get(3) - w)));
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

    public static class UShort extends Vector4D<UShort> {

        public final short x;
        public final short y;
        public final short z;
        public final short w;

        public UShort() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public UShort(short x, short y, short z, short w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UShort(short[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public UShort(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = (short) tuple.get(0);
            this.y = (short) tuple.get(1);
            this.z = (short) tuple.get(2);
            this.w = (short) tuple.get(3);
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
                case 2 : return java.lang.Short.toUnsignedInt(z);
                case 3 : return java.lang.Short.toUnsignedInt(w);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{
                java.lang.Short.toUnsignedInt(x),
                java.lang.Short.toUnsignedInt(y),
                java.lang.Short.toUnsignedInt(z),
                java.lang.Short.toUnsignedInt(w)
            };
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{
                java.lang.Short.toUnsignedInt(x),
                java.lang.Short.toUnsignedInt(y),
                java.lang.Short.toUnsignedInt(z),
                java.lang.Short.toUnsignedInt(w)
            };
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{
                java.lang.Short.toUnsignedInt(x),
                java.lang.Short.toUnsignedInt(y),
                java.lang.Short.toUnsignedInt(z),
                java.lang.Short.toUnsignedInt(w)
            };
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = java.lang.Short.toUnsignedInt(x);
            buffer[offset+1] = java.lang.Short.toUnsignedInt(y);
            buffer[offset+2] = java.lang.Short.toUnsignedInt(z);
            buffer[offset+3] = java.lang.Short.toUnsignedInt(w);
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = java.lang.Short.toUnsignedInt(x);
            buffer[offset+1] = java.lang.Short.toUnsignedInt(y);
            buffer[offset+2] = java.lang.Short.toUnsignedInt(z);
            buffer[offset+3] = java.lang.Short.toUnsignedInt(w);
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = java.lang.Short.toUnsignedInt(x);
            buffer[offset+1] = java.lang.Short.toUnsignedInt(y);
            buffer[offset+2] = java.lang.Short.toUnsignedInt(z);
            buffer[offset+3] = java.lang.Short.toUnsignedInt(w);
        }

        @Override
        public UShort add(Vector<?> other) {
            ensureDimension(other, 4);
            return new UShort(
                    (short) ((x & 0xFFFF) + other.get(0)),
                    (short) ((y & 0xFFFF) + other.get(1)),
                    (short) ((z & 0xFFFF) + other.get(2)),
                    (short) ((w & 0xFFFF) + other.get(3)));
        }

        @Override
        public UShort subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new UShort(
                    (short) ((x & 0xFFFF) - other.get(0)),
                    (short) ((y & 0xFFFF) - other.get(1)),
                    (short) ((z & 0xFFFF) - other.get(2)),
                    (short) ((w & 0xFFFF) - other.get(3)));
        }

        @Override
        public UShort multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new UShort(
                    (short) ((x & 0xFFFF) * other.get(0)),
                    (short) ((y & 0xFFFF) * other.get(1)),
                    (short) ((z & 0xFFFF) * other.get(2)),
                    (short) ((w & 0xFFFF) * other.get(3)));
        }

        @Override
        public UShort divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new UShort(
                    (short) ((x & 0xFFFF) / other.get(0)),
                    (short) ((y & 0xFFFF) / other.get(1)),
                    (short) ((z & 0xFFFF) / other.get(2)),
                    (short) ((w & 0xFFFF) / other.get(3)));
        }

        @Override
        public UShort scale(double scale) {
            return new UShort(
                    (short) ((x & 0xFFFF) * scale),
                    (short) ((y & 0xFFFF) * scale),
                    (short) ((z & 0xFFFF) * scale),
                    (short) ((w & 0xFFFF) * scale));
        }

        @Override
        protected UShort combine(double a, Vector<?> other, double b) {
            return new UShort(
                    (short) (a * (x & 0xFFFF) + b * other.get(0)),
                    (short) (a * (y & 0xFFFF) + b * other.get(1)),
                    (short) (a * (z & 0xFFFF) + b * other.get(2)),
                    (short) (a * (w & 0xFFFF) + b * other.get(3)));
        }

        @Override
        protected UShort combineAccurate(double a, Vector<?> other, double b) {
            return new UShort(
                    (short) (LinearCombination.of(a, (x & 0xFFFF), b, other.get(0))),
                    (short) (LinearCombination.of(a, (y & 0xFFFF), b, other.get(1))),
                    (short) (LinearCombination.of(a, (z & 0xFFFF), b, other.get(2))),
                    (short) (LinearCombination.of(a, (w & 0xFFFF), b, other.get(3))));
        }

        @Override
        public UShort with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UShort((short) value, y, z, w);
                case 1 : return new UShort(x, (short) value, z, w);
                case 2 : return new UShort(x, y, (short) value, w);
                case 3 : return new UShort(x, y, z, (short) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UShort lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new UShort((short) (other.get(0)), (short) (other.get(1)), (short) (other.get(2)), (short) (other.get(3)));
            }
            return new UShort(
                    (short) ((x & 0xFFFF) + ratio * (other.get(0) - (x & 0xFFFF))),
                    (short) ((y & 0xFFFF) + ratio * (other.get(1) - (y & 0xFFFF))),
                    (short) ((z & 0xFFFF) + ratio * (other.get(2) - (z & 0xFFFF))),
                    (short) ((w & 0xFFFF) + ratio * (other.get(3) - (w & 0xFFFF))));
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

    public static class Int extends Vector4D<Int> {

        public final int x;
        public final int y;
        public final int z;
        public final int w;

        public Int() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public Int(int x, int y, int z, int w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Int(int[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Int(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = (int) tuple.get(0);
            this.y = (int) tuple.get(1);
            this.z = (int) tuple.get(2);
            this.w = (int) tuple.get(3);
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x, y, z, w};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x, y, z, w};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y,z,w};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
            buffer[offset+2] = (short) z;
            buffer[offset+3] = (short) w;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public Int add(Vector<?> other) {
            ensureDimension(other, 4);
            return new Int(
                    (int) (x + other.get(0)),
                    (int) (y + other.get(1)),
                    (int) (z + other.get(2)),
                    (int) (w + other.get(3)));
        }

        @Override
        public Int subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new Int(
                    (int) (x - other.get(0)),
                    (int) (y - other.get(1)),
                    (int) (z - other.get(2)),
                    (int) (w - other.get(3)));
        }

        @Override
        public Int multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new Int(
                    (int) (x * other.get(0)),
                    (int) (y * other.get(1)),
                    (int) (z * other.get(2)),
                    (int) (w * other.get(3)));
        }

        @Override
        public Int divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new Int(
                    (int) (x / other.get(0)),
                    (int) (y / other.get(1)),
                    (int) (z / other.get(2)),
                    (int) (w / other.get(3)));
        }

        @Override
        public Int scale(double scale) {
            return new Int(
                    (int) (x * scale),
                    (int) (y * scale),
                    (int) (z * scale),
                    (int) (w * scale));
        }

        @Override
        protected Int combine(double a, Vector<?> other, double b) {
            return new Int(
                    (int) (a * x + b * other.get(0)),
                    (int) (a * y + b * other.get(1)),
                    (int) (a * z + b * other.get(2)),
                    (int) (a * w + b * other.get(3)));
        }

        @Override
        protected Int combineAccurate(double a, Vector<?> other, double b) {
            return new Int(
                    (int) (LinearCombination.of(a, x, b, other.get(0))),
                    (int) (LinearCombination.of(a, y, b, other.get(1))),
                    (int) (LinearCombination.of(a, z, b, other.get(2))),
                    (int) (LinearCombination.of(a, w, b, other.get(3))));
        }

        @Override
        public Int with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Int((int) value, y, z, w);
                case 1 : return new Int(x, (int) value, z, w);
                case 2 : return new Int(x, y, (int) value, w);
                case 3 : return new Int(x, y, z, (int) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Int lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new Int((int) (other.get(0)), (int) (other.get(1)), (int) (other.get(2)), (int) (other.get(3)));
            }
            return new Int(
                    (int) (x + ratio * (other.get(0) - x)),
                    (int) (y + ratio * (other.get(1) - y)),
                    (int) (z + ratio * (other.get(2) - z)),
                    (int) (w + ratio * (other.get(3) - w)));
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

    public static class UInt extends Vector4D<UInt> {

        public final int x;
        public final int y;
        public final int z;
        public final int w;

        public UInt() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public UInt(int x, int y, int z, int w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UInt(int[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public UInt(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = (int) (long) tuple.get(0);
            this.y = (int) (long) tuple.get(1);
            this.z = (int) (long) tuple.get(2);
            this.w = (int) (long) tuple.get(3);
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
                case 2 : return Integer.toUnsignedLong(z);
                case 3 : return Integer.toUnsignedLong(w);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{x, y, z, w};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{
                Integer.toUnsignedLong(x),
                Integer.toUnsignedLong(y),
                Integer.toUnsignedLong(z),
                Integer.toUnsignedLong(w),
            };
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{
                Integer.toUnsignedLong(x),
                Integer.toUnsignedLong(y),
                Integer.toUnsignedLong(z),
                Integer.toUnsignedLong(w),
            };
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
            buffer[offset+2] = (short) z;
            buffer[offset+3] = (short) w;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = Integer.toUnsignedLong(x);
            buffer[offset+1] = Integer.toUnsignedLong(y);
            buffer[offset+2] = Integer.toUnsignedLong(z);
            buffer[offset+3] = Integer.toUnsignedLong(w);
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = Integer.toUnsignedLong(x);
            buffer[offset+1] = Integer.toUnsignedLong(y);
            buffer[offset+2] = Integer.toUnsignedLong(z);
            buffer[offset+3] = Integer.toUnsignedLong(w);
        }

        @Override
        public UInt add(Vector<?> other) {
            ensureDimension(other, 4);
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) + other.get(0)),
                    (int) (long) (Integer.toUnsignedLong(y) + other.get(1)),
                    (int) (long) (Integer.toUnsignedLong(z) + other.get(2)),
                    (int) (long) (Integer.toUnsignedLong(w) + other.get(3)));
        }

        @Override
        public UInt subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) - other.get(0)),
                    (int) (long) (Integer.toUnsignedLong(y) - other.get(1)),
                    (int) (long) (Integer.toUnsignedLong(z) - other.get(2)),
                    (int) (long) (Integer.toUnsignedLong(w) - other.get(3)));
        }

        @Override
        public UInt multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) * other.get(0)),
                    (int) (long) (Integer.toUnsignedLong(y) * other.get(1)),
                    (int) (long) (Integer.toUnsignedLong(z) * other.get(2)),
                    (int) (long) (Integer.toUnsignedLong(w) * other.get(3)));
        }

        @Override
        public UInt divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) / other.get(0)),
                    (int) (long) (Integer.toUnsignedLong(y) / other.get(1)),
                    (int) (long) (Integer.toUnsignedLong(z) / other.get(2)),
                    (int) (long) (Integer.toUnsignedLong(w) / other.get(3)));
        }

        @Override
        public UInt scale(double scale) {
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) * scale),
                    (int) (long) (Integer.toUnsignedLong(y) * scale),
                    (int) (long) (Integer.toUnsignedLong(z) * scale),
                    (int) (long) (Integer.toUnsignedLong(w) * scale));
        }

        @Override
        protected UInt combine(double a, Vector<?> other, double b) {
            return new UInt(
                    (int) (long) (a * Integer.toUnsignedLong(x) + b * other.get(0)),
                    (int) (long) (a * Integer.toUnsignedLong(y) + b * other.get(1)),
                    (int) (long) (a * Integer.toUnsignedLong(z) + b * other.get(2)),
                    (int) (long) (a * Integer.toUnsignedLong(w) + b * other.get(3)));
        }

        @Override
        protected UInt combineAccurate(double a, Vector<?> other, double b) {
            return new UInt(
                    (int) (long) (LinearCombination.of(a, Integer.toUnsignedLong(x), b, other.get(0))),
                    (int) (long) (LinearCombination.of(a, Integer.toUnsignedLong(y), b, other.get(1))),
                    (int) (long) (LinearCombination.of(a, Integer.toUnsignedLong(z), b, other.get(2))),
                    (int) (long) (LinearCombination.of(a, Integer.toUnsignedLong(w), b, other.get(3))));
        }

        @Override
        public UInt with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new UInt((int) (long) value, y, z, w);
                case 1 : return new UInt(x, (int) (long) value, z, w);
                case 2 : return new UInt(x, y, (int) (long) value, w);
                case 3 : return new UInt(x, y, z, (int) (long) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UInt lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new UInt((int) (long) (other.get(0)), (int) (long) (other.get(1)), (int) (long) (other.get(2)), (int) (long) (other.get(3)));
            }
            return new UInt(
                    (int) (long) (Integer.toUnsignedLong(x) + ratio * (other.get(0) - Integer.toUnsignedLong(x))),
                    (int) (long) (Integer.toUnsignedLong(y) + ratio * (other.get(1) - Integer.toUnsignedLong(y))),
                    (int) (long) (Integer.toUnsignedLong(z) + ratio * (other.get(2) - Integer.toUnsignedLong(z))),
                    (int) (long) (Integer.toUnsignedLong(w) + ratio * (other.get(3) - Integer.toUnsignedLong(w))));
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

    public static class Long extends Vector4D<Long> {

        public final long x;
        public final long y;
        public final long z;
        public final long w;

        public Long() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public Long(long x, long y, long z, long w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Long(long[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Long(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = (long) tuple.get(0);
            this.y = (long) tuple.get(1);
            this.z = (long) tuple.get(2);
            this.w = (long) tuple.get(3);
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int) x, (int) y, (int) z, (int) w};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x, y, z, w};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y,z,w};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
            buffer[offset+2] = (short) z;
            buffer[offset+3] = (short) w;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = (int) x;
            buffer[offset+1] = (int) y;
            buffer[offset+2] = (int) z;
            buffer[offset+3] = (int) w;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public Long add(Vector<?> other) {
            ensureDimension(other, 4);
            return new Long(
                    (long) (x + other.get(0)),
                    (long) (y + other.get(1)),
                    (long) (z + other.get(2)),
                    (long) (w + other.get(3)));
        }

        @Override
        public Long subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new Long(
                    (long) (x - other.get(0)),
                    (long) (y - other.get(1)),
                    (long) (z - other.get(2)),
                    (long) (w - other.get(3)));
        }

        @Override
        public Long multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new Long(
                    (long) (x * other.get(0)),
                    (long) (y * other.get(1)),
                    (long) (z * other.get(2)),
                    (long) (w * other.get(3)));
        }

        @Override
        public Long divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new Long(
                    (long) (x / other.get(0)),
                    (long) (y / other.get(1)),
                    (long) (z / other.get(2)),
                    (long) (w / other.get(3)));
        }

        @Override
        public Long scale(double scale) {
            return new Long(
                    (long) (x * scale),
                    (long) (y * scale),
                    (long) (z * scale),
                    (long) (w * scale));
        }

        @Override
        protected Long combine(double a, Vector<?> other, double b) {
            return new Long(
                    (long) (a * x + b * other.get(0)),
                    (long) (a * y + b * other.get(1)),
                    (long) (a * z + b * other.get(2)),
                    (long) (a * w + b * other.get(3)));
        }

        @Override
        protected Long combineAccurate(double a, Vector<?> other, double b) {
            return new Long(
                    (long) (LinearCombination.of(a, x, b, other.get(0))),
                    (long) (LinearCombination.of(a, y, b, other.get(1))),
                    (long) (LinearCombination.of(a, z, b, other.get(2))),
                    (long) (LinearCombination.of(a, w, b, other.get(3))));
        }

        @Override
        public Long with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Long((long) value, y, z, w);
                case 1 : return new Long(x, (long) value, z, w);
                case 2 : return new Long(x, y, (long) value, w);
                case 3 : return new Long(x, y, z, (long) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Long lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new Long((long) (other.get(0)), (long) (other.get(1)), (long) (other.get(2)), (long) (other.get(3)));
            }
            return new Long(
                    (long) (x + ratio * (other.get(0) - x)),
                    (long) (y + ratio * (other.get(1) - y)),
                    (long) (z + ratio * (other.get(2) - z)),
                    (long) (w + ratio * (other.get(3) - w)));
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

    public static class Float extends Vector4D<Float> {

        public final float x;
        public final float y;
        public final float z;
        public final float w;

        public Float() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public Float(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Float(float[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Float(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = (float) tuple.get(0);
            this.y = (float) tuple.get(1);
            this.z = (float) tuple.get(2);
            this.w = (float) tuple.get(3);
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public double length() {
            return Math.sqrt(x*x + y*y + z*z + w*w);
        }

        @Override
        public double lengthSquare() {
            return x*x + y*y + z*z + w*w;
        }

        @Override
        public double dot(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return x * other.get(0)
                 + y * other.get(1)
                 + z * other.get(2)
                 + w * other.get(3);
        }

        @Override
        public double dotAccurate(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return LinearCombination.of(x, other.get(0), y, other.get(1), z, other.get(2), w, other.get(3));
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int)x, (int)y, (int)z, (int)w};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{x, y, z, w};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y,z,w};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
            buffer[offset+2] = (short) z;
            buffer[offset+3] = (short) w;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = (int) x;
            buffer[offset+1] = (int) y;
            buffer[offset+2] = (int) z;
            buffer[offset+3] = (int) w;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public Float add(Vector<?> other) {
            ensureDimension(other, 4);
            return new Float(
                    (float) (x + other.get(0)),
                    (float) (y + other.get(1)),
                    (float) (z + other.get(2)),
                    (float) (w + other.get(3)));
        }

        @Override
        public Float subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new Float(
                    (float) (x - other.get(0)),
                    (float) (y - other.get(1)),
                    (float) (z - other.get(2)),
                    (float) (w - other.get(3)));
        }

        @Override
        public Float multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new Float(
                    (float) (x * other.get(0)),
                    (float) (y * other.get(1)),
                    (float) (z * other.get(2)),
                    (float) (w * other.get(3)));
        }

        @Override
        public Float divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new Float(
                    (float) (x / other.get(0)),
                    (float) (y / other.get(1)),
                    (float) (z / other.get(2)),
                    (float) (w / other.get(3)));
        }

        @Override
        public Float scale(double scale) {
            return new Float(
                    (float) (x * scale),
                    (float) (y * scale),
                    (float) (z * scale),
                    (float) (w * scale));
        }

        @Override
        protected Float combine(double a, Vector<?> other, double b) {
            return new Float(
                    (float) (a * x + b * other.get(0)),
                    (float) (a * y + b * other.get(1)),
                    (float) (a * z + b * other.get(2)),
                    (float) (a * w + b * other.get(3)));
        }

        @Override
        protected Float combineAccurate(double a, Vector<?> other, double b) {
            return new Float(
                    (float) (LinearCombination.of(a, x, b, other.get(0))),
                    (float) (LinearCombination.of(a, y, b, other.get(1))),
                    (float) (LinearCombination.of(a, z, b, other.get(2))),
                    (float) (LinearCombination.of(a, w, b, other.get(3))));
        }

        @Override
        public Float with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Float((float) value, y, z, w);
                case 1 : return new Float(x, (float) value, z, w);
                case 2 : return new Float(x, y, (float) value, w);
                case 3 : return new Float(x, y, z, (float) value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Float lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new Float((float) (other.get(0)), (float) (other.get(1)), (float) (other.get(2)), (float) (other.get(3)));
            }
            return new Float(
                    (float) (x + ratio * (other.get(0) - x)),
                    (float) (y + ratio * (other.get(1) - y)),
                    (float) (z + ratio * (other.get(2) - z)),
                    (float) (w + ratio * (other.get(3) - w)));
        }

        @Override
        public Float copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return java.lang.Float.isFinite(x) && java.lang.Float.isFinite(y) && java.lang.Float.isFinite(z) && java.lang.Float.isFinite(w);
        }

        @Override
        public boolean isNaN() {
            return java.lang.Float.isNaN(x) || java.lang.Float.isNaN(y) || java.lang.Float.isNaN(z) || java.lang.Float.isNaN(w);
        }

        @Override
        public boolean isInfinite() {
            return !isNaN() && (java.lang.Float.isInfinite(x) || java.lang.Float.isInfinite(y) || java.lang.Float.isInfinite(z) || java.lang.Float.isInfinite(w));
        }

        @Override
        public double distanceSquare(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            final double dx = x - other.get(0);
            final double dy = y - other.get(1);
            final double dz = z - other.get(2);
            final double dw = w - other.get(3);
            return dx*dx + dy*dy + dz*dz + dw*dw;
        }

    }

    public static class Double extends Vector4D<Double> {

        public final double x;
        public final double y;
        public final double z;
        public final double w;

        public Double() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.w = 0;
        }

        public Double(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Double(double[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Double(float[] array) {
            ensureLength(array.length, 4);
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Double(Vector<?> tuple) {
            ensureDimension(tuple, 4);
            this.x = tuple.get(0);
            this.y = tuple.get(1);
            this.z = tuple.get(2);
            this.w = tuple.get(3);
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public double length() {
            return Math.sqrt(x*x + y*y + z*z + w*w);
        }

        @Override
        public double lengthSquare() {
            return x*x + y*y + z*z + w*w;
        }

        @Override
        public double dot(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return x * other.get(0)
                 + y * other.get(1)
                 + z * other.get(2)
                 + w * other.get(3);
        }

        @Override
        public double dotAccurate(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            return LinearCombination.of(x, other.get(0), y, other.get(1), z, other.get(2), w, other.get(3));
        }

        @Override
        public int[] toArrayInt() {
            return new int[]{(int)x, (int)y, (int)z, (int)w};
        }

        @Override
        public float[] toArrayFloat() {
            return new float[]{(float)x, (float)y, (float)z, (float)w};
        }

        @Override
        public double[] toArrayDouble() {
            return new double[]{x,y,z,w};
        }

        @Override
        public void toArrayShort(short[] buffer, int offset) {
            buffer[offset  ] = (short) x;
            buffer[offset+1] = (short) y;
            buffer[offset+2] = (short) z;
            buffer[offset+3] = (short) w;
        }

        @Override
        public void toArrayInt(int[] buffer, int offset) {
            buffer[offset  ] = (int) x;
            buffer[offset+1] = (int) y;
            buffer[offset+2] = (int) z;
            buffer[offset+3] = (int) w;
        }

        @Override
        public void toArrayFloat(float[] buffer, int offset) {
            buffer[offset  ] = (float) x;
            buffer[offset+1] = (float) y;
            buffer[offset+2] = (float) z;
            buffer[offset+3] = (float) w;
        }

        @Override
        public void toArrayDouble(double[] buffer, int offset) {
            buffer[offset  ] = x;
            buffer[offset+1] = y;
            buffer[offset+2] = z;
            buffer[offset+3] = w;
        }

        @Override
        public Double add(Vector<?> other) {
            ensureDimension(other, 4);
            return new Double(
                    x + other.get(0),
                    y + other.get(1),
                    z + other.get(2),
                    w + other.get(3));
        }

        @Override
        public Double subtract(Vector<?> other) {
            ensureDimension(other, 4);
            return new Double(
                    x - other.get(0),
                    y - other.get(1),
                    z - other.get(2),
                    w - other.get(3));
        }

        @Override
        public Double multiply(Vector<?> other) {
            ensureDimension(other, 4);
            return new Double(
                    x * other.get(0),
                    y * other.get(1),
                    z * other.get(2),
                    w * other.get(3));
        }

        @Override
        public Double divide(Vector<?> other) {
            ensureDimension(other, 4);
            return new Double(
                    x / other.get(0),
                    y / other.get(1),
                    z / other.get(2),
                    w / other.get(3));
        }

        @Override
        public Double scale(double scale) {
            return new Double(
                    x * scale,
                    y * scale,
                    z * scale,
                    w * scale);
        }

        @Override
        protected Double combine(double a, Vector<?> other, double b) {
            return new Double(
                    a * x + b * other.get(0),
                    a * y + b * other.get(1),
                    a * z + b * other.get(2),
                    a * w + b * other.get(3));
        }

        @Override
        protected Double combineAccurate(double a, Vector<?> other, double b) {
            return new Double(
                    LinearCombination.of(a, x, b, other.get(0)),
                    LinearCombination.of(a, y, b, other.get(1)),
                    LinearCombination.of(a, z, b, other.get(2)),
                    LinearCombination.of(a, w, b, other.get(3)));
        }

        @Override
        public Double with(int indice, double value) throws IndexOutOfBoundsException {
            switch (indice) {
                case 0 : return new Double(value, y, z, w);
                case 1 : return new Double(x, value, z, w);
                case 2 : return new Double(x, y, value, w);
                case 3 : return new Double(x, y, z, value);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Double lerp(Vector<?> other, double ratio) {
            ensureDimension(other, 4);
            if (ratio == 1) {
                return new Double(other.get(0), other.get(1), other.get(2), other.get(3));
            }
            return new Double(
                    x + ratio * (other.get(0) - x),
                    y + ratio * (other.get(1) - y),
                    z + ratio * (other.get(2) - z),
                    w + ratio * (other.get(3) - w));
        }

        @Override
        public Double copy() {
            return this;
        }

        @Override
        public boolean isFinite() {
            return java.lang.Double.isFinite(x) && java.lang.Double.isFinite(y) && java.lang.Double.isFinite(z) && java.lang.Double.isFinite(w);
        }

        @Override
        public boolean isNaN() {
            return java.lang.Double.isNaN(x) || java.lang.Double.isNaN(y) || java.lang.Double.isNaN(z) || java.lang.Double.isNaN(w);
        }

        @Override
        public boolean isInfinite() {
            return !isNaN() && (java.lang.Double.isInfinite(x) || java.lang.Double.isInfinite(y) || java.lang.Double.isInfinite(z) || java.lang.Double.isInfinite(w));
        }

        @Override
        public double distanceSquare(Vector<?> other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            final double dx = x - other.get(0);
            final double dy = y - other.get(1);
            final double dz = z - other.get(2);
            final double dw = w - other.get(3);
            return dx*dx + dy*dy + dz*dz + dw*dw;
        }

    }

}
