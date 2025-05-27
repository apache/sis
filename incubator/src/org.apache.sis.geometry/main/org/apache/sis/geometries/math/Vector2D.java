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
public abstract class Vector2D<T extends Vector2D<T>> extends AbstractTuple<T> implements Vector<T> {

    public Vector2D() {
        super(2);
    }

    protected Vector2D(SampleSystem type) {
        super(type);
    }

    /**
     * Rotate this vector around given reference point.
     *
     * @param ref reference point to rotate around
     * @param angleRad angle in radians
     * @return this vector
     */
    public T rotate(Vector2D<?> ref, double angleRad) {
        final double sin = Math.sin(angleRad);
        final double cos = Math.cos(angleRad);

        final double relx = get(0) - ref.get(0);
        final double rely = get(1) - ref.get(1);

        set(0, relx * cos - rely * sin + ref.get(0));
        set(1, relx * sin + rely * cos + ref.get(1));
        return (T) this;
    }

    @Override
    public final int getDimension() {
        return 2;
    }

    public static class Byte extends Vector2D<Byte> {

        public byte x;
        public byte y;

        public Byte() {
        }

        public Byte(SampleSystem type) {
            super(type);
        }

        public Byte(SampleSystem type, byte x, byte y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public Byte(byte x, byte y) {
            this.x = x;
            this.y = y;
        }

        public Byte(byte[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public Byte(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Byte(byte x, byte y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (byte) value; break;
                case 1 : y = (byte) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Byte set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            y = (byte) values[1];
            return this;
        }

        @Override
        public Byte set(double[] values, int offset) {
            x = (byte) values[ offset];
            y = (byte) values[1 + offset];
            return this;
        }

        @Override
        public Byte set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            y = (byte) values[1];
            return this;
        }

        @Override
        public Byte set(float[] values, int offset) {
            x = (byte) values[ offset];
            y = (byte) values[1 + offset];
            return this;
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
        public Byte copy() {
            return new Byte(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UByte extends Vector2D<UByte> {

        public byte x;
        public byte y;

        public UByte() {
        }

        public UByte(SampleSystem type) {
            super(type);
            ensureDimension(type, 2);
        }

        public UByte(SampleSystem type, byte x, byte y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public UByte(byte x, byte y) {
            this.x = x;
            this.y = y;
        }

        public UByte(byte[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public UByte(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UByte(byte x, byte y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return y & 0xFF;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (byte) value; break;
                case 1 : y = (byte) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UByte set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            y = (byte) values[1];
            return this;
        }

        @Override
        public UByte set(double[] values, int offset) {
            x = (byte) values[ offset];
            y = (byte) values[1 + offset];
            return this;
        }

        @Override
        public UByte set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            y = (byte) values[1];
            return this;
        }

        @Override
        public UByte set(float[] values, int offset) {
            x = (byte) values[ offset];
            y = (byte) values[1 + offset];
            return this;
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
        public UByte copy() {
            return new UByte(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Short extends Vector2D<Short> {

        public short x;
        public short y;

        public Short() {
        }

        public Short(SampleSystem type) {
            super(type);
            ensureDimension(type, 2);
        }

        public Short(SampleSystem type, short x, short y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public Short(short x, short y) {
            this.x = x;
            this.y = y;
        }

        public Short(short[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public Short(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Short(short x, short y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (short) value; break;
                case 1 : y = (short) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Short set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            y = (short) values[1];
            return this;
        }

        @Override
        public Short set(double[] values, int offset) {
            x = (short) values[ offset];
            y = (short) values[1 + offset];
            return this;
        }

        @Override
        public Short set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            y = (short) values[1];
            return this;
        }

        @Override
        public Short set(float[] values, int offset) {
            x = (short) values[ offset];
            y = (short) values[1 + offset];
            return this;
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
        public Short copy() {
            return new Short(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UShort extends Vector2D<UShort> {

        public short x;
        public short y;

        public UShort() {
        }

        public UShort(SampleSystem type) {
            super(type);
            ensureDimension(type, 2);
        }

        public UShort(SampleSystem type, short x, short y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public UShort(short x, short y) {
            this.x = x;
            this.y = y;
        }

        public UShort(short[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public UShort(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UShort(short x, short y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return java.lang.Short.toUnsignedInt(y);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (short) value; break;
                case 1 : y = (short) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UShort set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            y = (short) values[1];
            return this;
        }

        @Override
        public UShort set(double[] values, int offset) {
            x = (short) values[ offset];
            y = (short) values[1 + offset];
            return this;
        }

        @Override
        public UShort set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            y = (short) values[1];
            return this;
        }

        @Override
        public UShort set(float[] values, int offset) {
            x = (short) values[ offset];
            y = (short) values[1 + offset];
            return this;
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
        public UShort copy() {
            return new UShort(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Int extends Vector2D<Int> {

        public int x;
        public int y;

        public Int() {
        }

        public Int(SampleSystem type) {
            super(type);
            ensureDimension(type, 2);
        }

        public Int(SampleSystem type, int x, int y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public Int(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Int(int[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public Int(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Int(int x, int y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (int) value; break;
                case 1 : y = (int) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Int set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            y = (int) values[1];
            return this;
        }

        @Override
        public Int set(double[] values, int offset) {
            x = (int) values[ offset];
            y = (int) values[1 + offset];
            return this;
        }

        @Override
        public Int set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            y = (int) values[1];
            return this;
        }

        @Override
        public Int set(float[] values, int offset) {
            x = (int) values[ offset];
            y = (int) values[1 + offset];
            return this;
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
        public Int copy() {
            return new Int(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UInt extends Vector2D<UInt> {

        public int x;
        public int y;

        public UInt() {
        }

        public UInt(SampleSystem type) {
            super(type);
            ensureDimension(type, 2);
        }

        public UInt(SampleSystem type, int x, int y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public UInt(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public UInt(int[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public UInt(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UInt(int x, int y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return Integer.toUnsignedLong(y);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (int) value; break;
                case 1 : y = (int) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public UInt set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            y = (int) values[1];
            return this;
        }

        @Override
        public UInt set(double[] values, int offset) {
            x = (int) values[ offset];
            y = (int) values[1 + offset];
            return this;
        }

        @Override
        public UInt set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            y = (int) values[1];
            return this;
        }

        @Override
        public UInt set(float[] values, int offset) {
            x = (int) values[ offset];
            y = (int) values[1 + offset];
            return this;
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
        public UInt copy() {
            return new UInt(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Long extends Vector2D<Long> {

        public long x;
        public long y;

        public Long() {
        }

        public Long(SampleSystem type) {
            super(type);
            ensureDimension(type, 2);
        }

        public Long(SampleSystem type, long x, long y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public Long(long x, long y) {
            this.x = x;
            this.y = y;
        }

        public Long(long[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public Long(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Long(long x, long y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (long) value; break;
                case 1 : y = (long) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Long set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (long) values[0];
            y = (long) values[1];
            return this;
        }

        @Override
        public Long set(double[] values, int offset) {
            x = (long) values[ offset];
            y = (long) values[1 + offset];
            return this;
        }

        @Override
        public Long set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (long) values[0];
            y = (long) values[1];
            return this;
        }

        @Override
        public Long set(float[] values, int offset) {
            x = (long) values[ offset];
            y = (long) values[1 + offset];
            return this;
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
        public Long copy() {
            return new Long(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Float extends Vector2D<Float> {

        public float x;
        public float y;

        public Float() {
        }

        public Float(SampleSystem type) {
            super(type);
            ensureDimension(type, 2);
        }

        public Float(SampleSystem type, float x, float y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Float(float[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public Float(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Float(float x, float y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (float) value; break;
                case 1 : y = (float) value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Float set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (float) values[0];
            y = (float) values[1];
            return this;
        }

        @Override
        public Float set(double[] values, int offset) {
            x = (float) values[ offset];
            y = (float) values[1 + offset];
            return this;
        }

        @Override
        public Float set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = values[0];
            y = values[1];
            return this;
        }

        @Override
        public Float set(float[] values, int offset) {
            x = values[ offset];
            y = values[1 + offset];
            return this;
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
        public Float normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            return this;
        }

        @Override
        public Float add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            y += other.get(1);
            return this;
        }

        @Override
        public Float subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            y -= other.get(1);
            return this;
        }

        @Override
        public Float scale(double scale) {
            x *= scale;
            y *= scale;
            return this;
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
        public Float copy() {
            return new Float(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return java.lang.Float.isFinite(x) && java.lang.Float.isFinite(y);
        }

    }

    public static class Double extends Vector2D<Double> {

        public double x;
        public double y;

        public Double() {
        }

        public Double(SampleSystem type) {
            super(type);
            ensureDimension(type, 2);
        }

        public Double(SampleSystem type, double x, double y) {
            this(type);
            this.x = x;
            this.y = y;
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Double(double[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public Double(float[] array) {
            this.x = array[0];
            this.y = array[1];
        }

        public Double(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 2);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Double(double x, double y, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
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
                case 1 : return y;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = value; break;
                case 1 : y = value; break;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public Double set(double[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = values[0];
            y = values[1];
            return this;
        }

        @Override
        public Double set(double[] values, int offset) {
            x = values[ offset];
            y = values[1 + offset];
            return this;
        }

        @Override
        public Double set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = values[0];
            y = values[1];
            return this;
        }

        @Override
        public Double set(float[] values, int offset) {
            x = values[ offset];
            y = values[1 + offset];
            return this;
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
        public Double normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            return this;
        }

        @Override
        public Double add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            y += other.get(1);
            return this;
        }

        @Override
        public Double subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            y -= other.get(1);
            return this;
        }

        @Override
        public Double scale(double scale) {
            x *= scale;
            y *= scale;
            return this;
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
        public Double copy() {
            return new Double(x, y, type);
        }

        @Override
        public boolean isFinite() {
            return java.lang.Double.isFinite(x) && java.lang.Double.isFinite(y);
        }

    }

}
