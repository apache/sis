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
public abstract class Vector4D<T extends Vector4D<T>> extends AbstractTuple<T> implements Vector<T> {

    public Vector4D() {
        super(4);
    }

    protected Vector4D(SampleSystem type) {
        super(type);
    }

    @Override
    public final int getDimension() {
        return 4;
    }

    public static class Byte extends Vector4D<Byte> {

        public byte x;
        public byte y;
        public byte z;
        public byte w;

        public Byte() {
        }

        public Byte(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public Byte(SampleSystem type, byte x, byte y, byte z, byte w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Byte(byte x, byte y, byte z, byte w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Byte(byte[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Byte(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Byte(byte x, byte y, byte z,  byte w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (byte) value; break;
                case 1 : y = (byte) value; break;
                case 2 : z = (byte) value; break;
                case 3 : w = (byte) value; break;
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
            z = (byte) values[2];
            w = (byte) values[3];
            return this;
        }

        @Override
        public Byte set(double[] values, int offset) {
            x = (byte) values[ offset];
            y = (byte) values[1 + offset];
            z = (byte) values[2 + offset];
            w = (byte) values[3 + offset];
            return this;
        }

        @Override
        public Byte set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            y = (byte) values[1];
            z = (byte) values[2];
            w = (byte) values[3];
            return this;
        }

        @Override
        public Byte set(float[] values, int offset) {
            x = (byte) values[ offset];
            y = (byte) values[1 + offset];
            z = (byte) values[2 + offset];
            w = (byte) values[3 + offset];
            return this;
        }

        @Override
        public Byte normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            z *= s;
            w *= s;
            return this;
        }

        @Override
        public Byte add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            y += other.get(1);
            z += other.get(2);
            w += other.get(3);
            return this;
        }

        @Override
        public Byte subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            y -= other.get(1);
            z -= other.get(2);
            w -= other.get(3);
            return this;
        }

        @Override
        public Byte scale(double scale) {
            x *= scale;
            y *= scale;
            z *= scale;
            w *= scale;
            return this;
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
        public Byte copy() {
            return new Byte(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UByte extends Vector4D<UByte> {

        public byte x;
        public byte y;
        public byte z;
        public byte w;

        public UByte() {
        }

        public UByte(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public UByte(SampleSystem type, byte x, byte y, byte z, byte w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UByte(byte x, byte y, byte z, byte w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UByte(byte[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public UByte(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UByte(byte x, byte y, byte z, byte w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return z & 0xFF;
                case 3 : return w & 0xFF;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (byte) value; break;
                case 1 : y = (byte) value; break;
                case 2 : z = (byte) value; break;
                case 3 : w = (byte) value; break;
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
            z = (byte) values[2];
            w = (byte) values[3];
            return this;
        }

        @Override
        public UByte set(double[] values, int offset) {
            x = (byte) values[ offset];
            y = (byte) values[1 + offset];
            z = (byte) values[2 + offset];
            w = (byte) values[3 + offset];
            return this;
        }

        @Override
        public UByte set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (byte) values[0];
            y = (byte) values[1];
            z = (byte) values[2];
            w = (byte) values[3];
            return this;
        }

        @Override
        public UByte set(float[] values, int offset) {
            x = (byte) values[ offset];
            y = (byte) values[1 + offset];
            z = (byte) values[2 + offset];
            w = (byte) values[3 + offset];
            return this;
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
        public UByte copy() {
            return new UByte(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Short extends Vector4D<Short> {

        public short x;
        public short y;
        public short z;
        public short w;

        public Short() {
        }

        public Short(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public Short(SampleSystem type, short x, short y, short z, short w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Short(short x, short y, short z, short w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Short(short[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Short(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check crs.
         */
        private Short(short x, short y, short z,  short w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (short) value; break;
                case 1 : y = (short) value; break;
                case 2 : z = (short) value; break;
                case 3 : w = (short) value; break;
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
            z = (short) values[2];
            w = (short) values[3];
            return this;
        }

        @Override
        public Short set(double[] values, int offset) {
            x = (short) values[ offset];
            y = (short) values[1 + offset];
            z = (short) values[2 + offset];
            w = (short) values[3 + offset];
            return this;
        }

        @Override
        public Short set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            y = (short) values[1];
            z = (short) values[2];
            w = (short) values[3];
            return this;
        }

        @Override
        public Short set(float[] values, int offset) {
            x = (short) values[ offset];
            y = (short) values[1 + offset];
            z = (short) values[2 + offset];
            w = (short) values[3 + offset];
            return this;
        }

        @Override
        public Short normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            z *= s;
            w *= s;
            return this;
        }

        @Override
        public Short add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            y += other.get(1);
            z += other.get(2);
            w += other.get(3);
            return this;
        }

        @Override
        public Short subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            y -= other.get(1);
            z -= other.get(2);
            w -= other.get(3);
            return this;
        }

        @Override
        public Short scale(double scale) {
            x *= scale;
            y *= scale;
            z *= scale;
            w *= scale;
            return this;
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
        public Short copy() {
            return new Short(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UShort extends Vector4D<UShort> {

        public short x;
        public short y;
        public short z;
        public short w;

        public UShort() {
        }

        public UShort(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public UShort(SampleSystem type, short x, short y, short z, short w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UShort(short x, short y, short z, short w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UShort(short[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public UShort(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UShort(short x, short y, short z, short w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return java.lang.Short.toUnsignedInt(z);
                case 3 : return java.lang.Short.toUnsignedInt(w);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (short) value; break;
                case 1 : y = (short) value; break;
                case 2 : z = (short) value; break;
                case 3 : w = (short) value; break;
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
            z = (short) values[2];
            w = (short) values[3];
            return this;
        }

        @Override
        public UShort set(double[] values, int offset) {
            x = (short) values[ offset];
            y = (short) values[1 + offset];
            z = (short) values[2 + offset];
            w = (short) values[3 + offset];
            return this;
        }

        @Override
        public UShort set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (short) values[0];
            y = (short) values[1];
            z = (short) values[2];
            w = (short) values[3];
            return this;
        }

        @Override
        public UShort set(float[] values, int offset) {
            x = (short) values[ offset];
            y = (short) values[1 + offset];
            z = (short) values[2 + offset];
            w = (short) values[3 + offset];
            return this;
        }

        @Override
        public UShort normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            z *= s;
            w *= s;
            return this;
        }

        @Override
        public UShort add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            y += other.get(1);
            z += other.get(2);
            w += other.get(3);
            return this;
        }

        @Override
        public UShort subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            y -= other.get(1);
            z -= other.get(2);
            w -= other.get(3);
            return this;
        }

        @Override
        public UShort scale(double scale) {
            x *= scale;
            y *= scale;
            z *= scale;
            w *= scale;
            return this;
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
        public UShort copy() {
            return new UShort(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Int extends Vector4D<Int> {

        public int x;
        public int y;
        public int z;
        public int w;

        public Int() {
        }

        public Int(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public Int(SampleSystem type, int x, int y, int z, int w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Int(int x, int y, int z, int w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Int(int[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Int(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Int(int x, int y, int z, int w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (int) value; break;
                case 1 : y = (int) value; break;
                case 2 : z = (int) value; break;
                case 3 : w = (int) value; break;
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
            z = (int) values[2];
            w = (int) values[3];
            return this;
        }

        @Override
        public Int set(double[] values, int offset) {
            x = (int) values[ offset];
            y = (int) values[1 + offset];
            z = (int) values[2 + offset];
            w = (int) values[3 + offset];
            return this;
        }

        @Override
        public Int set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            y = (int) values[1];
            z = (int) values[2];
            w = (int) values[3];
            return this;
        }

        @Override
        public Int set(float[] values, int offset) {
            x = (int) values[ offset];
            y = (int) values[1 + offset];
            z = (int) values[2 + offset];
            w = (int) values[3 + offset];
            return this;
        }

        @Override
        public Int normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            z *= s;
            w *= s;
            return this;
        }

        @Override
        public Int add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            y += other.get(1);
            z += other.get(2);
            w += other.get(3);
            return this;
        }

        @Override
        public Int subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            y -= other.get(1);
            z -= other.get(2);
            w -= other.get(3);
            return this;
        }

        @Override
        public Int scale(double scale) {
            x *= scale;
            y *= scale;
            z *= scale;
            w *= scale;
            return this;
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
        public Int copy() {
            return new Int(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class UInt extends Vector4D<UInt> {

        public int x;
        public int y;
        public int z;
        public int w;

        public UInt() {
        }

        public UInt(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public UInt(SampleSystem type, int x, int y, int z, int w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UInt(int x, int y, int z, int w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public UInt(int[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public UInt(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private UInt(int x, int y, int z, int w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return Integer.toUnsignedLong(z);
                case 3 : return Integer.toUnsignedLong(w);
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (int) value; break;
                case 1 : y = (int) value; break;
                case 2 : z = (int) value; break;
                case 3 : w = (int) value; break;
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
            z = (int) values[2];
            w = (int) values[3];
            return this;
        }

        @Override
        public UInt set(double[] values, int offset) {
            x = (int) values[ offset];
            y = (int) values[1 + offset];
            z = (int) values[2 + offset];
            w = (int) values[3 + offset];
            return this;
        }

        @Override
        public UInt set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (int) values[0];
            y = (int) values[1];
            z = (int) values[2];
            w = (int) values[3];
            return this;
        }

        @Override
        public UInt set(float[] values, int offset) {
            x = (int) values[ offset];
            y = (int) values[1 + offset];
            z = (int) values[2 + offset];
            w = (int) values[3 + offset];
            return this;
        }

        @Override
        public UInt normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            z *= s;
            w *= s;
            return this;
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
        public UInt copy() {
            return new UInt(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Long extends Vector4D<Long> {

        public long x;
        public long y;
        public long z;
        public long w;

        public Long() {
        }

        public Long(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public Long(SampleSystem type, long x, long y, long z, long w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Long(long x, long y, long z, long w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Long(long[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Long(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Long(long x, long y, long z, long w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (long) value; break;
                case 1 : y = (long) value; break;
                case 2 : z = (long) value; break;
                case 3 : w = (long) value; break;
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
            z = (long) values[2];
            w = (long) values[3];
            return this;
        }

        @Override
        public Long set(double[] values, int offset) {
            x = (long) values[ offset];
            y = (long) values[1 + offset];
            z = (long) values[2 + offset];
            w = (long) values[3 + offset];
            return this;
        }

        @Override
        public Long set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = (long) values[0];
            y = (long) values[1];
            z = (long) values[2];
            w = (long) values[3];
            return this;
        }

        @Override
        public Long set(float[] values, int offset) {
            x = (long) values[ offset];
            y = (long) values[1 + offset];
            z = (long) values[2 + offset];
            w = (long) values[3 + offset];
            return this;
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
        public Long copy() {
            return new Long(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return true;
        }

    }

    public static class Float extends Vector4D<Float> {

        public float x;
        public float y;
        public float z;
        public float w;

        public Float() {
        }

        public Float(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public Float(SampleSystem type, float x, float y, float z, float w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Float(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Float(float[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Float(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Float(float x, float y, float z,  float w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = (float) value; break;
                case 1 : y = (float) value; break;
                case 2 : z = (float) value; break;
                case 3 : w = (float) value; break;
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
            z = (float) values[2];
            w = (float) values[3];
            return this;
        }

        @Override
        public Float set(double[] values, int offset) {
            x = (float) values[ offset];
            y = (float) values[1 + offset];
            z = (float) values[2 + offset];
            w = (float) values[3 + offset];
            return this;
        }

        @Override
        public Float set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = values[0];
            y = values[1];
            z = values[2];
            w = values[3];
            return this;
        }

        @Override
        public Float set(float[] values, int offset) {
            x = values[ offset];
            y = values[1 + offset];
            z = values[2 + offset];
            w = values[3 + offset];
            return this;
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
        public Float normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            z *= s;
            w *= s;
            return this;
        }

        @Override
        public Float add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            y += other.get(1);
            z += other.get(2);
            w += other.get(3);
            return this;
        }

        @Override
        public Float subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            y -= other.get(1);
            z -= other.get(2);
            w -= other.get(3);
            return this;
        }

        @Override
        public Float scale(double scale) {
            x *= scale;
            y *= scale;
            z *= scale;
            w *= scale;
            return this;
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
        public Float copy() {
            return new Float(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return java.lang.Float.isFinite(x) && java.lang.Float.isFinite(y) && java.lang.Float.isFinite(z) && java.lang.Float.isFinite(w);
        }

    }

    public static class Double extends Vector4D<Double> {

        public double x;
        public double y;
        public double z;
        public double w;

        public Double() {
        }

        public Double(SampleSystem type) {
            super(type);
            ensureDimension(type, 4);
        }

        public Double(SampleSystem type, double x, double y, double z, double w) {
            this(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Double(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Double(double[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Double(float[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
            this.w = array[3];
        }

        public Double(Tuple tuple) {
            super(tuple.getSampleSystem());
            ensureDimension(type, 4);
            set(tuple);
        }

        /**
         * Copy constructor which does not check type.
         */
        private Double(double x, double y, double z,  double w, SampleSystem type) {
            super(type);
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
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
                case 2 : return z;
                case 3 : return w;
                default : throw new IndexOutOfBoundsException("Invalid index " + indice);
            }
        }

        @Override
        public void set(int indice, double value) {
            switch (indice) {
                case 0 : x = value; break;
                case 1 : y = value; break;
                case 2 : z = value; break;
                case 3 : w = value; break;
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
            z = values[2];
            w = values[3];
            return this;
        }

        @Override
        public Double set(double[] values, int offset) {
            x = values[ offset];
            y = values[1 + offset];
            z = values[2 + offset];
            w = values[3 + offset];
            return this;
        }

        @Override
        public Double set(float[] values) {
            if (getDimension() != values.length) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+values.length);
            }
            x = values[0];
            y = values[1];
            z = values[2];
            w = values[3];
            return this;
        }

        @Override
        public Double set(float[] values, int offset) {
            x = values[ offset];
            y = values[1 + offset];
            z = values[2 + offset];
            w = values[3 + offset];
            return this;
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
        public Double normalize() {
            final double s = 1.0 / length();
            x *= s;
            y *= s;
            z *= s;
            w *= s;
            return this;
        }

        @Override
        public Double add(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x += other.get(0);
            y += other.get(1);
            z += other.get(2);
            w += other.get(3);
            return this;
        }

        @Override
        public Double subtract(Tuple other) {
            if (getDimension() != other.getDimension()) {
                throw new IllegalArgumentException("Vectors size are different : "+getDimension()+" and "+other.getDimension());
            }
            x -= other.get(0);
            y -= other.get(1);
            z -= other.get(2);
            w -= other.get(3);
            return this;
        }

        @Override
        public Double scale(double scale) {
            x *= scale;
            y *= scale;
            z *= scale;
            w *= scale;
            return this;
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
        public Double copy() {
            return new Double(x, y, z, w, type);
        }

        @Override
        public boolean isFinite() {
            return java.lang.Double.isFinite(x) && java.lang.Double.isFinite(y) && java.lang.Double.isFinite(z) && java.lang.Double.isFinite(w);
        }

    }

}
