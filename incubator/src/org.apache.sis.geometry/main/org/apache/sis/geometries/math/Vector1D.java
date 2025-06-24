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
public abstract class Vector1D<T extends Vector1D<T>> extends AbstractTuple<T> implements Vector<T> {

    public Vector1D() {
        super(1);
    }

    protected Vector1D(SampleSystem type) {
        super(type);
    }

    @Override
    public final int getDimension() {
        return 1;
    }

    public static class Byte extends Vector1D<Byte> {

        public byte x;

        public Byte() {
        }

        public Byte(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public Byte(SampleSystem type, byte x) {
            this(type);
            this.x = x;
        }

        public Byte(byte x) {
            this.x = x;
        }

        public Byte(byte[] array) {
            this.x = array[0];
        }

        public Byte(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Byte(byte x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        Byte(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (byte) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Byte set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            return this;
        }

        @Override
        public Byte set(double[] values, int offset) {
            x = (byte) values[ offset];
            return this;
        }

        @Override
        public Byte set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            return this;
        }

        @Override
        public Byte set(float[] values, int offset) {
            x = (byte) values[offset];
            return this;
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
        public Byte copy() {
            return new Byte(x, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UByte extends Vector1D<UByte> {

        public byte x;

        public UByte() {
        }

        public UByte(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public UByte(SampleSystem type, byte x) {
            this(type);
            this.x = x;
        }

        public UByte(byte x) {
            this.x = x;
        }

        public UByte(byte[] array) {
            this.x = array[0];
        }

        public UByte(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UByte(byte x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        UByte(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (byte) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UByte set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            return this;
        }

        @Override
        public UByte set(double[] values, int offset) {
            x = (byte) values[ offset];
            return this;
        }

        @Override
        public UByte set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            return this;
        }

        @Override
        public UByte set(float[] values, int offset) {
            x = (byte) values[offset];
            return this;
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
        public UByte copy() {
            return new UByte(x, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Short extends Vector1D<Short> {

        public short x;

        public Short() {
        }

        public Short(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public Short(SampleSystem type, short x) {
            this(type);
            this.x = x;
        }

        public Short(short x) {
            this.x = x;
        }

        public Short(short[] array) {
            this.x = array[0];
        }

        public Short(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Short(short x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        Short(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (short) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Short set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            return this;
        }

        @Override
        public Short set(double[] values, int offset) {
            x = (short) values[ offset];
            return this;
        }

        @Override
        public Short set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            return this;
        }

        @Override
        public Short set(float[] values, int offset) {
            x = (short) values[ offset];
            return this;
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
        public Short copy() {
            return new Short(x, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UShort extends Vector1D<UShort> {

        public short x;

        public UShort() {
        }

        public UShort(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public UShort(SampleSystem type, short x) {
            this(type);
            this.x = x;
        }

        public UShort(short x) {
            this.x = x;
        }

        public UShort(short[] array) {
            this.x = array[0];
        }

        public UShort(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UShort(short x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        UShort(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (short) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UShort set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            return this;
        }

        @Override
        public UShort set(double[] values, int offset) {
            x = (short) values[ offset];
            return this;
        }

        @Override
        public UShort set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            return this;
        }

        @Override
        public UShort set(float[] values, int offset) {
            x = (short) values[ offset];
            return this;
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
        public UShort copy() {
            return new UShort(x, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Int extends Vector1D<Int> {

        public int x;

        public Int() {
        }

        public Int(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public Int(SampleSystem type, int x) {
            this(type);
            this.x = x;
        }

        public Int(int x) {
            this.x = x;
        }

        public Int(int[] array) {
            this.x = array[0];
        }

        public Int(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Int(int x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        Int(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (int) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Int set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            return this;
        }

        @Override
        public Int set(double[] values, int offset) {
            x = (int) values[ offset];
            return this;
        }

        @Override
        public Int set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            return this;
        }

        @Override
        public Int set(float[] values, int offset) {
            x = (int) values[ offset];
            return this;
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
        public Int copy() {
            return new Int(x, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UInt extends Vector1D<UInt> {

        public int x;

        public UInt() {
        }

        public UInt(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public UInt(SampleSystem type, int x) {
            this(type);
            this.x = x;
        }

        public UInt(int x) {
            this.x = x;
        }

        public UInt(int[] array) {
            this.x = array[0];
        }

        public UInt(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UInt(int x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        UInt(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (int) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UInt set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            return this;
        }

        @Override
        public UInt set(double[] values, int offset) {
            x = (int) values[ offset];
            return this;
        }

        @Override
        public UInt set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            return this;
        }

        @Override
        public UInt set(float[] values, int offset) {
            x = (int) values[ offset];
            return this;
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
        public UInt copy() {
            return new UInt(x, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Long extends Vector1D<Long> {

        public long x;

        public Long() {
        }

        public Long(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public Long(SampleSystem type, long x) {
            this(type);
            this.x = x;
        }

        public Long(long x) {
            this.x = x;
        }

        public Long(long[] array) {
            this.x = array[0];
        }

        public Long(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Long(long x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        Long(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (long) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Long set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (long) values[0];
            return this;
        }

        @Override
        public Long set(double[] values, int offset) {
            x = (long) values[ offset];
            return this;
        }

        @Override
        public Long set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (long) values[0];
            return this;
        }

        @Override
        public Long set(float[] values, int offset) {
            x = (long) values[ offset];
            return this;
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
        public Long copy() {
            return new Long(x, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Float extends Vector1D<Float> {

        public float x;

        public Float() {
        }

        public Float(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public Float(SampleSystem type, float x) {
            this(type);
            this.x = x;
        }

        public Float(float x) {
            this.x = x;
        }

        public Float(float[] array) {
            this.x = array[0];
        }

        public Float(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Float(float x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        Float(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (float) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Float set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (float) values[0];
            return this;
        }

        @Override
        public Float set(double[] values, int offset) {
            x = (float) values[ offset];
            return this;
        }

        @Override
        public Float set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = values[0];
            return this;
        }

        @Override
        public Float set(float[] values, int offset) {
            x = values[offset];
            return this;
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
        public Float normalize() {
            final double s = 1.0 / length();
            x *= s;
            return this;
        }

        @Override
        public Float add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            return this;
        }

        @Override
        public Float subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            return this;
        }

        @Override
        public Float scale(double scale) {
            x *= scale;
            return this;
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
        public Float copy() {
            return new Float(x, type);
        }

        @Override
        public boolean isFinite() {
            return java.lang.Float.isFinite(x);
        }

    }

    public static class Double extends Vector1D<Double> {

        public double x;

        public Double() {
        }

        public Double(SampleSystem type) {
            super(type);
            ensureDimension(type, 1);
        }

        public Double(SampleSystem type, double x) {
            this(type);
            this.x = x;
        }

        public Double(double x) {
            this.x = x;
        }

        public Double(double[] array) {
            this.x = array[0];
        }

        public Double(float[] array) {
            this.x = array[0];
        }

        public Double(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 1);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Double(double x, SampleSystem type) {
            super(type);
            this.x = x;
        }

        /**
         * Package private benchmarked performance constructor which does not check type.
         * @param type must be of the correct size, it is not verified
         * @param none not used, to differenciate with public constructor
         */
        Double(SampleSystem type, boolean none) {
            super(type);
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
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Double set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = values[0];
            return this;
        }

        @Override
        public Double set(double[] values, int offset) {
            x = values[ offset];
            return this;
        }

        @Override
        public Double set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = values[0];
            return this;
        }

        @Override
        public Double set(float[] values, int offset) {
            x = values[offset];
            return this;
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
        public Double normalize() {
            final double s = 1.0 / length();
            x *= s;
            return this;
        }

        @Override
        public Double add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            return this;
        }

        @Override
        public Double subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            return this;
        }

        @Override
        public Double scale(double scale) {
            x *= scale;
            return this;
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
        public Double copy() {
            return new Double(x, type);
        }

        @Override
        public boolean isFinite() {
            return java.lang.Double.isFinite(x);
        }

    }
}
