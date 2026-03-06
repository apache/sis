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

import java.util.Arrays;
import java.util.List;
import static org.apache.sis.geometries.math.DataType.BYTE;
import static org.apache.sis.geometries.math.DataType.DOUBLE;
import static org.apache.sis.geometries.math.DataType.FLOAT;
import static org.apache.sis.geometries.math.DataType.INT;
import static org.apache.sis.geometries.math.DataType.LONG;
import static org.apache.sis.geometries.math.DataType.SHORT;
import static org.apache.sis.geometries.math.DataType.UBYTE;
import static org.apache.sis.geometries.math.DataType.UINT;
import static org.apache.sis.geometries.math.DataType.USHORT;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ArrayFactoryJava implements ArrayFactory {

    public static final ArrayFactoryJava INSTANCE = new ArrayFactoryJava();

    private ArrayFactoryJava() {
    }

    @Override
    public Builder builder() {
        return new JavaBuilder();
    }

    private static final class JavaBuilder extends AbstractBuilder<JavaBuilder> {

        @Override
        public NDArray buildND() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Array build() {
            final long[] shape = getShape();
            if (shape.length != 1) throw new IllegalArgumentException("Array shape has more then one dimension");
            final long nbTuple = shape[0];

            final SampleSystem system = getSystem();
            final DataType dataType = getDataType();
            final int nbSample = system.getSize();

            final long arraySize = nbTuple * nbSample;
            if (arraySize > Integer.MAX_VALUE) throw new IllegalArgumentException("Array shape exceed " + Integer.MAX_VALUE + ". Use FFM factory for very large arrays");
            final int arraySizeI = (int) arraySize;

            final Array array;
            switch (dataType) {
                case BYTE : array = new ArrayFactoryJava.Byte(system, new byte[arraySizeI]); break;
                case UBYTE : array = new ArrayFactoryJava.UByte(system, new byte[arraySizeI]); break;
                case SHORT : array = new ArrayFactoryJava.Short(system, new short[arraySizeI]); break;
                case USHORT : array = new ArrayFactoryJava.UShort(system, new short[arraySizeI]); break;
                case INT : array = new ArrayFactoryJava.Int(system, new int[arraySizeI]); break;
                case UINT : array = new ArrayFactoryJava.UInt(system, new int[arraySizeI]); break;
                case LONG : array = new ArrayFactoryJava.Long(system, new long[arraySizeI]); break;
                case FLOAT : array = new ArrayFactoryJava.Float(system, new float[arraySizeI]); break;
                case DOUBLE : array = new ArrayFactoryJava.Double(system, new double[arraySizeI]); break;
                default : throw new IllegalArgumentException("Unexpected data type " + dataType);
            }

            //todo, check if we can reuse the values array
            copyOrFillValues(array);
            return array;
        }

    }

    private static abstract class JavaArray extends AbstractArray {

        protected SampleSystem type;
        protected final int dimension;

        public JavaArray(SampleSystem type) {
            this.type = type;
            this.dimension = type.getSize();
        }

        @Override
        public final ArrayFactory getFactory() {
            return ArrayFactoryJava.INSTANCE;
        }

        @Override
        public final SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public final void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public final int getDimension() {
            return dimension;
        }
    }

    static final class Byte extends JavaArray {

        private final byte[] array;

        Byte(SampleSystem type, byte[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.BYTE;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i]);
            }
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (byte) tuple.get(i);
            }
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            byte temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }
        @Override
        public byte[] toArrayByte() {
            return array.clone();
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            return Arrays.copyOfRange(array,
                    Math.toIntExact(offset * dimension),
                    Math.toIntExact((offset+nbTuple) * dimension));
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public void set(long index, Array array, long offset, long nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final byte[] arr = array.toArrayByte(offset, Math.toIntExact(nb));
            System.arraycopy(arr, 0, this.array,
                    Math.toIntExact(index*dimension),
                    Math.toIntExact(nb*dimension));
        }

        @Override
        public Byte resize(long newSize) {
            return new Byte(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public Byte copy() {
            return new Byte(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return array[Math.toIntExact(tupleIndex*dimension + sampleIndex)];
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = (byte) value;
                }
            };
        }

    }

    static final class UByte extends JavaArray {

        private final byte[] array;

        UByte(SampleSystem type, byte[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.UBYTE;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i] & 0xFF);
            }
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (byte) tuple.get(i);
            }
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            byte temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }

        @Override
        public byte[] toArrayByte() {
            return array.clone();
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            return Arrays.copyOfRange(array,
                    Math.toIntExact(offset*dimension),
                    Math.toIntExact((offset+nbTuple)*dimension));
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (short) (array[k] & 0xFF);
            }
            return result;
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k] & 0xFF;
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k] & 0xFF;
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k] & 0xFF;
            }
            return result;
        }

        @Override
        public void set(long index, Array array, long offset, long nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final byte[] arr = array.toArrayByte(offset, Math.toIntExact(nb));
            System.arraycopy(arr, 0, this.array,
                    Math.toIntExact(index*dimension),
                    Math.toIntExact(nb*dimension));
        }

        @Override
        public UByte resize(long newSize) {
            return new UByte(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public UByte copy() {
            return new UByte(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] & 0xFF;
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = (byte) value;
                }
            };
        }

    }

    static final class Short extends JavaArray {

        private final short[] array;

        Short(SampleSystem type, short[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.SHORT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i]);
            }
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (short) tuple.get(i);
            }
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            short temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }

        @Override
        public short[] toArrayShort() {
            return array.clone();
        }

        @Override
        public double[] toArrayDouble() {
            final double[] result = new double[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = (double) array[i];
            }
            return result;
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            return Arrays.copyOfRange(array,
                    Math.toIntExact(offset*dimension),
                    Math.toIntExact((offset+nbTuple)*dimension));
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public void set(long index, Array array, long offset, long nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final short[] arr = array.toArrayShort(offset, Math.toIntExact(nb));
            System.arraycopy(arr, 0, this.array,
                    Math.toIntExact(index*dimension),
                    Math.toIntExact(nb*dimension));
        }

        @Override
        public Short resize(long newSize) {
            return new Short(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public Short copy() {
            return new Short(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return array[Math.toIntExact(tupleIndex*dimension + sampleIndex)];
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = (short) value;
                }
            };
        }

    }

    static final class UShort extends JavaArray {

        private final short[] array;

        UShort(SampleSystem type, short[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.USHORT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                buffer.set(i, java.lang.Short.toUnsignedInt(array[offset+i]));
            }
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (short) tuple.get(i);
            }
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            short temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }

        @Override
        public short[] toArrayShort() {
            return array.clone();
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            return Arrays.copyOfRange(array,
                    Math.toIntExact(offset*dimension),
                    Math.toIntExact((offset+nbTuple)*dimension));
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k] & 0xFFFF;
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k] & 0xFFFF;
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k] & 0xFFFF;
            }
            return result;
        }


        @Override
        public void set(long index, Array array, long offset, long nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final short[] arr = array.toArrayShort(offset, Math.toIntExact(nb));
            System.arraycopy(arr, 0, this.array,
                    Math.toIntExact(index*dimension),
                    Math.toIntExact(nb*dimension));
        }

        @Override
        public UShort resize(long newSize) {
            return new UShort(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public UShort copy() {
            return new UShort(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return java.lang.Short.toUnsignedInt(array[Math.toIntExact(tupleIndex*dimension + sampleIndex)]);
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = (short) value;
                }
            };
        }

    }

    static final class Int extends JavaArray {

        private final int[] array;

        Int(SampleSystem type, int[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.INT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i]);
            }
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (int) tuple.get(i);
            }
        }

        @Override
        public int[] toArrayInt() {
            return array.clone();
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (short) array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            return Arrays.copyOfRange(array,
                    Math.toIntExact(offset*dimension),
                    Math.toIntExact((offset+nbTuple)*dimension));
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            int temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }

        @Override
        public void set(long index, Array array, long offset, long nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final int[] arr = array.toArrayInt(offset, Math.toIntExact(nb));
            System.arraycopy(arr, 0, this.array,
                    Math.toIntExact(index*dimension),
                    Math.toIntExact(nb*dimension));
        }

        @Override
        public Int resize(long newSize) {
            return new Int(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public Int copy() {
            return new Int(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return array[Math.toIntExact(tupleIndex*dimension + sampleIndex)];
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = (int) value;
                }
            };
        }

    }

    static final class UInt extends JavaArray {

        private final int[] array;

        UInt(SampleSystem type, int[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        UInt(SampleSystem type, List<Integer> array) {
            super(type);
            this.array = array.stream().mapToInt(Integer::intValue).toArray();
            if (array.size() % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.size());
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.UINT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                buffer.set(i, Integer.toUnsignedLong(array[offset+i]));
            }
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (int) tuple.get(i);
            }
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            int temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }

        @Override
        public int[] toArrayInt() {
            return array.clone();
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (byte) (array[k] & 0xFFFFFFFFl);
            }
            return result;
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (short) (array[k] & 0xFFFFFFFFl);
            }
            return result;
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            return Arrays.copyOfRange(array,
                    Math.toIntExact(offset*dimension),
                    Math.toIntExact((offset+nbTuple)*dimension));
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k] & 0xFFFFFFFFl;
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k] & 0xFFFFFFFFl;
            }
            return result;
        }


        @Override
        public void set(long index, Array array, long offset, long nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final int[] arr = array.toArrayInt(offset, Math.toIntExact(nb));
            System.arraycopy(arr, 0, this.array,
                    Math.toIntExact(index*dimension),
                    Math.toIntExact(nb*dimension));
        }

        @Override
        public UInt resize(long newSize) {
            return new UInt(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public UInt copy() {
            return new UInt(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return Integer.toUnsignedLong(array[Math.toIntExact(tupleIndex*dimension + sampleIndex)]);
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = (int) value;
                }
            };
        }

    }

    static final class Long extends JavaArray {

        private final long[] array;

        Long(SampleSystem type, long[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.LONG;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i]);
            }
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            final int offset = Math.toIntExact(index * dimension);
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (int) tuple.get(i);
            }
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            long temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (short) array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (int) array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public Long resize(long newSize) {
            return new Long(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public Long copy() {
            return new Long(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return array[Math.toIntExact(tupleIndex*dimension + sampleIndex)];
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = (int) value;
                }
            };
        }

    }

    static final class Float extends JavaArray {

        private final float[] array;

        Float(SampleSystem type, float[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.FLOAT;
        }

        @Override
        public Tuple get(long index) {
            final Vector buffer = Vectors.create(type, DataType.FLOAT);
            return buffer.set(array, Math.toIntExact(index * dimension));
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            if (dimension != buffer.getDimension()) {
                throw new IllegalArgumentException("TupleArray and Tuple must have the same number of dimensions");
            }
            buffer.set(array, Math.toIntExact(index * dimension));
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            if (dimension != tuple.getDimension()) {
                throw new IllegalArgumentException("TupleArray and Tuple must have the same number of dimensions");
            }
            tuple.toArrayFloat(array, Math.toIntExact(index * dimension));
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            float temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }

        @Override
        public float[] toArrayFloat() {
            return array.clone();
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (short) array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (int) array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            return Arrays.copyOfRange(array,
                    Math.toIntExact(offset*dimension),
                    Math.toIntExact((offset+nbTuple)*dimension));
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public void set(long index, Array array, long offset, long nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final float[] arr = array.toArrayFloat(offset, Math.toIntExact(nb));
            System.arraycopy(arr, 0, this.array,
                    Math.toIntExact(index*dimension),
                    Math.toIntExact(nb*dimension));
        }

        @Override
        public Float resize(long newSize) {
            return new Float(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public Float copy() {
            return new Float(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return array[Math.toIntExact(tupleIndex*dimension + sampleIndex)];
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = (float) value;
                }
            };
        }

    }

    static final class Double extends JavaArray {

        private final double[] array;

        Double(SampleSystem type, double[] array) {
            super(type);
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public long getLength() {
            return array.length / dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.DOUBLE;
        }

        @Override
        public Tuple get(long index) {
            final Vector buffer = Vectors.create(type, DataType.DOUBLE);
            return buffer.set(array, Math.toIntExact(index * dimension));
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            if (dimension != buffer.getDimension()) {
                throw new IllegalArgumentException("TupleArray and Tuple must have the same number of dimensions");
            }
            buffer.set(array, Math.toIntExact(index * dimension));
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            if (dimension != tuple.getDimension()) {
                throw new IllegalArgumentException("TupleArray and Tuple must have the same number of dimensions");
            }
            tuple.toArrayDouble(array, Math.toIntExact(index * dimension));
        }

        @Override
        public void swap(long i, long j) {
            int oi = Math.toIntExact(i*dimension + dimension);
            int oj = Math.toIntExact(j*dimension + dimension);
            double temp;
            switch(dimension) {
                default: for (int k=dimension; k > 5; k--) {
                            oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                         }
                case 5 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 4 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 3 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 2 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
                case 1 : oi--; oj--; temp = array[oi]; array[oi] = array[oj]; array[oj] = temp;
            }
        }

        @Override
        public double[] toArrayDouble() {
            return array.clone();
        }

        @Override
        public byte[] toArrayByte(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (short) array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (int) array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(long offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = Math.toIntExact(offset * dimension); i < length; i++, k++) {
                result[i] = (float) array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(long offset, int nbTuple) {
            final int ioffset = Math.toIntExact(offset);
            return Arrays.copyOfRange(array,
                    ioffset*dimension,
                    (ioffset+nbTuple)*dimension);
        }

        @Override
        public void set(long index, Array array, long offset, long nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final int inb = Math.toIntExact(nb);
            final double[] arr = array.toArrayDouble(offset, inb);
            System.arraycopy(arr, 0, this.array, Math.toIntExact(index*dimension), inb*dimension);
        }

        @Override
        public Double resize(long newSize) {
            return new Double(type, Arrays.copyOf(array, Math.toIntExact(newSize*dimension)));
        }

        @Override
        public Double copy() {
            return new Double(type, array.clone());
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    return array[Math.toIntExact(tupleIndex*dimension + sampleIndex)];
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    array[Math.toIntExact(tupleIndex*dimension + sampleIndex)] = value;
                }
            };
        }

    }

}
