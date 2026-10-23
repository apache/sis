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
public abstract class VectorND<T extends VectorND<T>> extends AbstractVector<T> {

    public VectorND() {
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
        ensureDimension(unit, getDimension());
        return combine(1, unit, -dot(unit));
    }

    @Override
    public abstract T lerp(Vector<?> other, double ratio);

    @Override
    public T lerpAccurate(Vector<?> other, double ratio) {
        ensureDimension(other, getDimension());
        return combineAccurate(1 - ratio, other, ratio);
    }

    @Override
    public T slerp(Vector<?> other, double ratio) {
        return AbstractVector.slerp(this, other, ratio, (a, b) -> combine(a, other, b));
    }

    @Override
    public T vectorTo(Vector<?> other) {
        ensureDimension(other, getDimension());
        return combine(-1, other, 1);
    }

    public static class Byte extends VectorND<Byte> {

        private final byte[] values;

        public Byte(int size) {
            this.values = new byte[size];
        }

        public Byte(byte[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private Byte(byte[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static Byte wrap(byte[] array) {
            return new Byte(array, true);
        }

        public Byte(Vector<?> tuple) {
            this.values = new byte[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (byte) tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.BYTE;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return values[indice];
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

        @Override
        public Byte add(Vector<?> other) {
            ensureDimension(other, values.length);
            final Byte result = new Byte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (values[i] + other.get(i));
            }
            return result;
        }

        @Override
        public Byte subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final Byte result = new Byte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (values[i] - other.get(i));
            }
            return result;
        }

        @Override
        public Byte multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final Byte result = new Byte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (values[i] * other.get(i));
            }
            return result;
        }

        @Override
        public Byte divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final Byte result = new Byte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (values[i] / other.get(i));
            }
            return result;
        }

        @Override
        public Byte scale(double scale) {
            final Byte result = new Byte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (values[i] * scale);
            }
            return result;
        }

        @Override
        protected Byte combine(double a, Vector<?> other, double b) {
            final Byte result = new Byte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (a * values[i] + b * other.get(i));
            }
            return result;
        }

        @Override
        protected Byte combineAccurate(double a, Vector<?> other, double b) {
            final Byte result = new Byte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (LinearCombination.of(a, values[i], b, other.get(i)));
            }
            return result;
        }

        @Override
        public Byte with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final byte[] array = values.clone();
            array[indice] = (byte) value;
            return new Byte(array, true);
        }

        @Override
        public Byte lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final byte[] array = new byte[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = values[i];
                array[i] = (byte) ((ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v));
            }
            return new Byte(array, true);
        }

        @Override
        public Byte copy() {
            return this;
        }

    }

    public static class UByte extends VectorND<UByte> {

        private final byte[] values;

        public UByte(int size) {
            this.values = new byte[size];
        }

        public UByte(byte[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private UByte(byte[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static UByte wrap(byte[] array) {
            return new UByte(array, true);
        }

        public UByte(Vector<?> tuple) {
            this.values = new byte[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (byte) tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.UBYTE;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return values[indice] & 0xFF;
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

        @Override
        public UByte add(Vector<?> other) {
            ensureDimension(other, values.length);
            final UByte result = new UByte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) ((values[i] & 0xFF) + other.get(i));
            }
            return result;
        }

        @Override
        public UByte subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final UByte result = new UByte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) ((values[i] & 0xFF) - other.get(i));
            }
            return result;
        }

        @Override
        public UByte multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final UByte result = new UByte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) ((values[i] & 0xFF) * other.get(i));
            }
            return result;
        }

        @Override
        public UByte divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final UByte result = new UByte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) ((values[i] & 0xFF) / other.get(i));
            }
            return result;
        }

        @Override
        public UByte scale(double scale) {
            final UByte result = new UByte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) ((values[i] & 0xFF) * scale);
            }
            return result;
        }

        @Override
        protected UByte combine(double a, Vector<?> other, double b) {
            final UByte result = new UByte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (a * (values[i] & 0xFF) + b * other.get(i));
            }
            return result;
        }

        @Override
        protected UByte combineAccurate(double a, Vector<?> other, double b) {
            final UByte result = new UByte(values.length);
            final byte[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (byte) (LinearCombination.of(a, (values[i] & 0xFF), b, other.get(i)));
            }
            return result;
        }

        @Override
        public UByte with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final byte[] array = values.clone();
            array[indice] = (byte) value;
            return new UByte(array, true);
        }

        @Override
        public UByte lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final byte[] array = new byte[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = (values[i] & 0xFF);
                array[i] = (byte) ((ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v));
            }
            return new UByte(array, true);
        }

        @Override
        public UByte copy() {
            return this;
        }

    }

    public static class Short extends VectorND<Short> {

        private final short[] values;

        public Short(int size) {
            this.values = new short[size];
        }

        public Short(short[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private Short(short[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static Short wrap(short[] array) {
            return new Short(array, true);
        }

        public Short(Vector<?> tuple) {
            this.values = new short[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (short) tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.SHORT;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return values[indice];
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

        @Override
        public Short add(Vector<?> other) {
            ensureDimension(other, values.length);
            final Short result = new Short(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (values[i] + other.get(i));
            }
            return result;
        }

        @Override
        public Short subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final Short result = new Short(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (values[i] - other.get(i));
            }
            return result;
        }

        @Override
        public Short multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final Short result = new Short(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (values[i] * other.get(i));
            }
            return result;
        }

        @Override
        public Short divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final Short result = new Short(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (values[i] / other.get(i));
            }
            return result;
        }

        @Override
        public Short scale(double scale) {
            final Short result = new Short(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (values[i] * scale);
            }
            return result;
        }

        @Override
        protected Short combine(double a, Vector<?> other, double b) {
            final Short result = new Short(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (a * values[i] + b * other.get(i));
            }
            return result;
        }

        @Override
        protected Short combineAccurate(double a, Vector<?> other, double b) {
            final Short result = new Short(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (LinearCombination.of(a, values[i], b, other.get(i)));
            }
            return result;
        }

        @Override
        public Short with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final short[] array = values.clone();
            array[indice] = (short) value;
            return new Short(array, true);
        }

        @Override
        public Short lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final short[] array = new short[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = values[i];
                array[i] = (short) ((ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v));
            }
            return new Short(array, true);
        }

        @Override
        public Short copy() {
            return this;
        }

    }

    public static class UShort extends VectorND<UShort> {

        private final short[] values;

        public UShort(int size) {
            this.values = new short[size];
        }

        public UShort(short[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private UShort(short[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static UShort wrap(short[] array) {
            return new UShort(array, true);
        }

        public UShort(Vector<?> tuple) {
            this.values = new short[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (short) tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.USHORT;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return java.lang.Short.toUnsignedInt(values[indice]);
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

        @Override
        public UShort add(Vector<?> other) {
            ensureDimension(other, values.length);
            final UShort result = new UShort(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) ((values[i] & 0xFFFF) + other.get(i));
            }
            return result;
        }

        @Override
        public UShort subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final UShort result = new UShort(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) ((values[i] & 0xFFFF) - other.get(i));
            }
            return result;
        }

        @Override
        public UShort multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final UShort result = new UShort(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) ((values[i] & 0xFFFF) * other.get(i));
            }
            return result;
        }

        @Override
        public UShort divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final UShort result = new UShort(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) ((values[i] & 0xFFFF) / other.get(i));
            }
            return result;
        }

        @Override
        public UShort scale(double scale) {
            final UShort result = new UShort(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) ((values[i] & 0xFFFF) * scale);
            }
            return result;
        }

        @Override
        protected UShort combine(double a, Vector<?> other, double b) {
            final UShort result = new UShort(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (a * (values[i] & 0xFFFF) + b * other.get(i));
            }
            return result;
        }

        @Override
        protected UShort combineAccurate(double a, Vector<?> other, double b) {
            final UShort result = new UShort(values.length);
            final short[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (short) (LinearCombination.of(a, (values[i] & 0xFFFF), b, other.get(i)));
            }
            return result;
        }

        @Override
        public UShort with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final short[] array = values.clone();
            array[indice] = (short) value;
            return new UShort(array, true);
        }

        @Override
        public UShort lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final short[] array = new short[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = (values[i] & 0xFFFF);
                array[i] = (short) ((ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v));
            }
            return new UShort(array, true);
        }

        @Override
        public UShort copy() {
            return this;
        }

    }

    public static class Int extends VectorND<Int> {

        private final int[] values;

        public Int(int size) {
            this.values = new int[size];
        }

        public Int(int[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private Int(int[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static Int wrap(int[] array) {
            return new Int(array, true);
        }

        public Int(Vector<?> tuple) {
            this.values = new int[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (int) tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.INT;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return values[indice];
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

        @Override
        public Int add(Vector<?> other) {
            ensureDimension(other, values.length);
            final Int result = new Int(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (values[i] + other.get(i));
            }
            return result;
        }

        @Override
        public Int subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final Int result = new Int(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (values[i] - other.get(i));
            }
            return result;
        }

        @Override
        public Int multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final Int result = new Int(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (values[i] * other.get(i));
            }
            return result;
        }

        @Override
        public Int divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final Int result = new Int(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (values[i] / other.get(i));
            }
            return result;
        }

        @Override
        public Int scale(double scale) {
            final Int result = new Int(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (values[i] * scale);
            }
            return result;
        }

        @Override
        protected Int combine(double a, Vector<?> other, double b) {
            final Int result = new Int(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (a * values[i] + b * other.get(i));
            }
            return result;
        }

        @Override
        protected Int combineAccurate(double a, Vector<?> other, double b) {
            final Int result = new Int(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (LinearCombination.of(a, values[i], b, other.get(i)));
            }
            return result;
        }

        @Override
        public Int with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final int[] array = values.clone();
            array[indice] = (int) value;
            return new Int(array, true);
        }

        @Override
        public Int lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final int[] array = new int[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = values[i];
                array[i] = (int) ((ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v));
            }
            return new Int(array, true);
        }

        @Override
        public Int copy() {
            return this;
        }

    }

    public static class UInt extends VectorND<UInt> {

        private final int[] values;

        public UInt(int size) {
            this.values = new int[size];
        }

        public UInt(int[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private UInt(int[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static UInt wrap(int[] array) {
            return new UInt(array, true);
        }

        public UInt(Vector<?> tuple) {
            this.values = new int[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (int) (long) tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.UINT;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return Integer.toUnsignedLong(values[indice]);
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

        @Override
        public UInt add(Vector<?> other) {
            ensureDimension(other, values.length);
            final UInt result = new UInt(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (long) (Integer.toUnsignedLong(values[i]) + other.get(i));
            }
            return result;
        }

        @Override
        public UInt subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final UInt result = new UInt(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (long) (Integer.toUnsignedLong(values[i]) - other.get(i));
            }
            return result;
        }

        @Override
        public UInt multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final UInt result = new UInt(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (long) (Integer.toUnsignedLong(values[i]) * other.get(i));
            }
            return result;
        }

        @Override
        public UInt divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final UInt result = new UInt(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (long) (Integer.toUnsignedLong(values[i]) / other.get(i));
            }
            return result;
        }

        @Override
        public UInt scale(double scale) {
            final UInt result = new UInt(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (long) (Integer.toUnsignedLong(values[i]) * scale);
            }
            return result;
        }

        @Override
        protected UInt combine(double a, Vector<?> other, double b) {
            final UInt result = new UInt(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (long) (a * Integer.toUnsignedLong(values[i]) + b * other.get(i));
            }
            return result;
        }

        @Override
        protected UInt combineAccurate(double a, Vector<?> other, double b) {
            final UInt result = new UInt(values.length);
            final int[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (int) (long) (LinearCombination.of(a, Integer.toUnsignedLong(values[i]), b, other.get(i)));
            }
            return result;
        }

        @Override
        public UInt with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final int[] array = values.clone();
            array[indice] = (int) (long) value;
            return new UInt(array, true);
        }

        @Override
        public UInt lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final int[] array = new int[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = Integer.toUnsignedLong(values[i]);
                array[i] = (int) (long) ((ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v));
            }
            return new UInt(array, true);
        }

        @Override
        public UInt copy() {
            return this;
        }

    }

    public static class Long extends VectorND<Long> {

        private final long[] values;

        public Long(int size) {
            this.values = new long[size];
        }

        public Long(long[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private Long(long[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static Long wrap(long[] array) {
            return new Long(array, true);
        }

        public Long(Vector<?> tuple) {
            this.values = new long[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (long) tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.LONG;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return values[indice];
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

        @Override
        public Long add(Vector<?> other) {
            ensureDimension(other, values.length);
            final Long result = new Long(values.length);
            final long[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (long) (values[i] + other.get(i));
            }
            return result;
        }

        @Override
        public Long subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final Long result = new Long(values.length);
            final long[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (long) (values[i] - other.get(i));
            }
            return result;
        }

        @Override
        public Long multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final Long result = new Long(values.length);
            final long[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (long) (values[i] * other.get(i));
            }
            return result;
        }

        @Override
        public Long divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final Long result = new Long(values.length);
            final long[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (long) (values[i] / other.get(i));
            }
            return result;
        }

        @Override
        public Long scale(double scale) {
            final Long result = new Long(values.length);
            final long[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (long) (values[i] * scale);
            }
            return result;
        }

        @Override
        protected Long combine(double a, Vector<?> other, double b) {
            final Long result = new Long(values.length);
            final long[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (long) (a * values[i] + b * other.get(i));
            }
            return result;
        }

        @Override
        protected Long combineAccurate(double a, Vector<?> other, double b) {
            final Long result = new Long(values.length);
            final long[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (long) (LinearCombination.of(a, values[i], b, other.get(i)));
            }
            return result;
        }

        @Override
        public Long with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final long[] array = values.clone();
            array[indice] = (long) value;
            return new Long(array, true);
        }

        @Override
        public Long lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final long[] array = new long[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = values[i];
                array[i] = (long) ((ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v));
            }
            return new Long(array, true);
        }

        @Override
        public Long copy() {
            return this;
        }

    }

    public static class Float extends VectorND<Float> {

        private final float[] values;

        public Float(int size) {
            this.values = new float[size];
        }

        public Float(float[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private Float(float[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static Float wrap(float[] array) {
            return new Float(array, true);
        }

        public Float(Vector<?> tuple) {
            this.values = new float[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = (float) tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.FLOAT;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return values[indice];
        }

        @Override
        public double lengthSquare() {
            double length = 0;
            for (int i = 0; i < values.length; i++) {
                final double v = values[i];
                length += v * v;
            }
            return length;
        }

        @Override
        public double dot(Vector<?> other) {
            ensureDimension(other, values.length);
            double dot = 0;
            for (int i = 0; i < values.length; i++) {
                dot += values[i] * other.get(i);
            }
            return dot;
        }

        @Override
        public double dotAccurate(Vector<?> other) {
            ensureDimension(other, values.length);
            return LinearCombination.dot(values, other);
        }

        @Override
        public Float add(Vector<?> other) {
            ensureDimension(other, values.length);
            final Float result = new Float(values.length);
            final float[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (float) (values[i] + other.get(i));
            }
            return result;
        }

        @Override
        public Float subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final Float result = new Float(values.length);
            final float[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (float) (values[i] - other.get(i));
            }
            return result;
        }

        @Override
        public Float multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final Float result = new Float(values.length);
            final float[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (float) (values[i] * other.get(i));
            }
            return result;
        }

        @Override
        public Float divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final Float result = new Float(values.length);
            final float[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (float) (values[i] / other.get(i));
            }
            return result;
        }

        @Override
        public Float scale(double scale) {
            final Float result = new Float(values.length);
            final float[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (float) (values[i] * scale);
            }
            return result;
        }

        @Override
        protected Float combine(double a, Vector<?> other, double b) {
            final Float result = new Float(values.length);
            final float[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (float) (a * values[i] + b * other.get(i));
            }
            return result;
        }

        @Override
        protected Float combineAccurate(double a, Vector<?> other, double b) {
            final Float result = new Float(values.length);
            final float[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = (float) (LinearCombination.of(a, values[i], b, other.get(i)));
            }
            return result;
        }

        @Override
        public Float with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final float[] array = values.clone();
            array[indice] = (float) value;
            return new Float(array, true);
        }

        @Override
        public boolean isNaN() {
            for (int i = 0; i < values.length; i++) {
                if (java.lang.Float.isNaN(values[i])) return true;
            }
            return false;
        }

        @Override
        public boolean isInfinite() {
            boolean infinite = false;
            for (int i = 0; i < values.length; i++) {
                if (java.lang.Float.isNaN(values[i])) return false;
                if (java.lang.Float.isInfinite(values[i])) infinite = true;
            }
            return infinite;
        }

        @Override
        public double distanceSquare(Vector<?> other) {
            ensureDimension(other, values.length);
            double sum = 0;
            for (int i = 0; i < values.length; i++) {
                final double d = values[i] - other.get(i);
                sum += d * d;
            }
            return sum;
        }

        @Override
        public Float lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final float[] array = new float[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = values[i];
                array[i] = (float) ((ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v));
            }
            return new Float(array, true);
        }

        @Override
        public Float copy() {
            return this;
        }

    }

    public static class Double extends VectorND<Double> {

        private final double[] values;

        public Double(int size) {
            this.values = new double[size];
        }

        public Double(double[] array) {
            this.values = array.clone();
        }

        /**
         * Uses the given array directly, without copy.
         */
        private Double(double[] array, boolean share) {
            this.values = array;
        }

        /**
         * Create a vector using the given array directly, without copy.
         * The caller must not modify the array after this call.
         *
         * @param array vector values, not copied
         * @return new vector
         */
        static Double wrap(double[] array) {
            return new Double(array, true);
        }

        public Double(float[] array) {
            this.values = new double[array.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = array[i];
            }
        }

        public Double(Vector<?> tuple) {
            this.values = new double[tuple.getDimension()];
            for (int i = 0; i < values.length; i++) {
                values[i] = tuple.get(i);
            }
        }

        @Override
        public DataType getDataType() {
            return DataType.DOUBLE;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double get(int indice) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            return values[indice];
        }

        @Override
        public double lengthSquare() {
            double length = 0;
            for (int i = 0; i < values.length; i++) {
                length += values[i] * values[i];
            }
            return length;
        }

        @Override
        public double dot(Vector<?> other) {
            ensureDimension(other, values.length);
            double dot = 0;
            for (int i = 0; i < values.length; i++) {
                dot += values[i] * other.get(i);
            }
            return dot;
        }

        @Override
        public double dotAccurate(Vector<?> other) {
            ensureDimension(other, values.length);
            return LinearCombination.dot(values, other);
        }

        @Override
        public Double add(Vector<?> other) {
            ensureDimension(other, values.length);
            final Double result = new Double(values.length);
            final double[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = values[i] + other.get(i);
            }
            return result;
        }

        @Override
        public Double subtract(Vector<?> other) {
            ensureDimension(other, values.length);
            final Double result = new Double(values.length);
            final double[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = values[i] - other.get(i);
            }
            return result;
        }

        @Override
        public Double multiply(Vector<?> other) {
            ensureDimension(other, values.length);
            final Double result = new Double(values.length);
            final double[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = values[i] * other.get(i);
            }
            return result;
        }

        @Override
        public Double divide(Vector<?> other) {
            ensureDimension(other, values.length);
            final Double result = new Double(values.length);
            final double[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = values[i] / other.get(i);
            }
            return result;
        }

        @Override
        public Double scale(double scale) {
            final Double result = new Double(values.length);
            final double[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = values[i] * scale;
            }
            return result;
        }

        @Override
        protected Double combine(double a, Vector<?> other, double b) {
            final Double result = new Double(values.length);
            final double[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = a * values[i] + b * other.get(i);
            }
            return result;
        }

        @Override
        protected Double combineAccurate(double a, Vector<?> other, double b) {
            final Double result = new Double(values.length);
            final double[] dest = result.values;
            for (int i = 0; i < dest.length; i++) {
                dest[i] = LinearCombination.of(a, values[i], b, other.get(i));
            }
            return result;
        }

        @Override
        public Double with(int indice, double value) throws IndexOutOfBoundsException {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            final double[] array = values.clone();
            array[indice] = value;
            return new Double(array, true);
        }

        @Override
        public boolean isNaN() {
            for (int i = 0; i < values.length; i++) {
                if (java.lang.Double.isNaN(values[i])) return true;
            }
            return false;
        }

        @Override
        public boolean isInfinite() {
            boolean infinite = false;
            for (int i = 0; i < values.length; i++) {
                if (java.lang.Double.isNaN(values[i])) return false;
                if (java.lang.Double.isInfinite(values[i])) infinite = true;
            }
            return infinite;
        }

        @Override
        public double distanceSquare(Vector<?> other) {
            ensureDimension(other, values.length);
            double sum = 0;
            for (int i = 0; i < values.length; i++) {
                final double d = values[i] - other.get(i);
                sum += d * d;
            }
            return sum;
        }

        @Override
        public Double lerp(Vector<?> other, double ratio) {
            ensureDimension(other, values.length);
            final double[] array = new double[values.length];
            for (int i = 0; i < array.length; i++) {
                final double v = values[i];
                array[i] = (ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v);
            }
            return new Double(array, true);
        }

        @Override
        public Double copy() {
            return this;
        }

    }

}
