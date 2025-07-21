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
package org.apache.sis.geometries.math;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class VectorND<T extends VectorND<T>> extends AbstractTuple<T> implements Vector<T> {

    public VectorND(int size) {
        super(size);
    }

    protected VectorND(SampleSystem type) {
        super(type);
    }

    public static class Byte extends VectorND<Byte> {

        public final byte[] values;

        public Byte(int size) {
            super(size);
            this.values = new byte[size];
        }

        public Byte(SampleSystem type) {
            super(type);
            this.values = new byte[type.getSize()];
        }

        public Byte(byte[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public Byte(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new byte[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = (byte) value;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UByte extends VectorND<UByte> {

        public final byte[] values;

        public UByte(int size) {
            super(size);
            this.values = new byte[size];
        }

        public UByte(SampleSystem type) {
            super(type);
            this.values = new byte[type.getSize()];
        }

        public UByte(byte[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public UByte(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new byte[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = (byte) value;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Short extends VectorND<Short> {

        public final short[] values;

        public Short(int size) {
            super(size);
            this.values = new short[size];
        }

        public Short(SampleSystem type) {
            super(type);
            this.values = new short[type.getSize()];
        }

        public Short(short[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public Short(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new short[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = (short) value;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UShort extends VectorND<UShort> {

        public final short[] values;

        public UShort(int size) {
            super(size);
            this.values = new short[size];
        }

        public UShort(SampleSystem type) {
            super(type);
            this.values = new short[type.getSize()];
        }

        public UShort(short[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public UShort(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new short[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = (short) value;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Int extends VectorND<Int> {

        public final int[] values;

        public Int(int size) {
            super(size);
            this.values = new int[size];
        }

        public Int(SampleSystem type) {
            super(type);
            this.values = new int[type.getSize()];
        }

        public Int(int[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public Int(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new int[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = (int) value;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UInt extends VectorND<UInt> {

        public final int[] values;

        public UInt(int size) {
            super(size);
            this.values = new int[size];
        }

        public UInt(SampleSystem type) {
            super(type);
            this.values = new int[type.getSize()];
        }

        public UInt(int[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public UInt(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new int[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = (int) value;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Long extends VectorND<Long> {

        public final long[] values;

        public Long(int size) {
            super(size);
            this.values = new long[size];
        }

        public Long(SampleSystem type) {
            super(type);
            this.values = new long[type.getSize()];
        }

        public Long(long[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public Long(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new long[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = (long) value;
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Float extends VectorND<Float> {

        public final float[] values;

        public Float(int size) {
            super(size);
            this.values = new float[size];
        }

        public Float(SampleSystem type) {
            super(type);
            this.values = new float[type.getSize()];
        }

        public Float(float[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public Float(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new float[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = (float) value;
        }

    }

    public static class Double extends VectorND<Double> {

        public final double[] values;

        public Double(int size) {
            super(size);
            this.values = new double[size];
        }

        public Double(SampleSystem type) {
            super(type);
            this.values = new double[type.getSize()];
        }

        public Double(double[] array) {
            super(array.length);
            this.values = array.clone();
        }

        public Double(float[] array) {
            super(array.length);
            this.values = new double[array.length];
            set(array);
        }

        public Double(Tuple tuple) {
            super(tuple.getSampleSystem());
            this.values = new double[tuple.getDimension()];
            set(tuple);
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
        public void set(int indice, double value) {
            if (indice < 0 || indice >= values.length) {
                throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
            values[indice] = value;
        }

    }

}
