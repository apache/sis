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
import org.apache.sis.util.ArgumentChecks;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class TupleArrayND extends AbstractTupleArray {

    static final class Byte extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final byte[] array;

        Byte(SampleSystem type, byte[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }


        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.BYTE;
        }

        @Override
        public void get(int index, Tuple buffer) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i]);
            }
        }

        @Override
        public void set(int index, Tuple tuple) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (byte) tuple.get(i);
            }
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public byte[] toArrayByte(int offset, int nbTuple) {
            return Arrays.copyOfRange(array, offset * dimension, (offset+nbTuple) * dimension);
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public void set(int index, TupleArray array, int offset, int nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final byte[] arr = array.toArrayByte(offset, nb);
            System.arraycopy(arr, 0, this.array, index*dimension, nb*dimension);
        }

        @Override
        public Byte resize(int newSize) {
            return new Byte(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public Byte copy() {
            return new Byte(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return array[tupleIndex*dimension + sampleIndex];
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = (byte) value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNb{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }

    }

    static final class UByte extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final byte[] array;

        UByte(SampleSystem type, byte[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.UBYTE;
        }

        @Override
        public void get(int index, Tuple buffer) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i] & 0xFF);
            }
        }

        @Override
        public void set(int index, Tuple tuple) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (byte) tuple.get(i);
            }
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public byte[] toArrayByte(int offset, int nbTuple) {
            return Arrays.copyOfRange(array, offset*dimension, (offset+nbTuple)*dimension);
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (short) (array[k] & 0xFF);
            }
            return result;
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k] & 0xFF;
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k] & 0xFF;
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k] & 0xFF;
            }
            return result;
        }

        @Override
        public void set(int index, TupleArray array, int offset, int nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final byte[] arr = array.toArrayByte(offset, nb);
            System.arraycopy(arr, 0, this.array, index*dimension, nb*dimension);
        }

        @Override
        public UByte resize(int newSize) {
            return new UByte(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public UByte copy() {
            return new UByte(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return array[tupleIndex*dimension + sampleIndex] & 0xFF;
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = (byte) value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNub{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }

    }

    static final class Short extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final short[] array;

        Short(SampleSystem type, short[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.SHORT;
        }

        @Override
        public void get(int index, Tuple buffer) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i]);
            }
        }

        @Override
        public void set(int index, Tuple tuple) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (short) tuple.get(i);
            }
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public byte[] toArrayByte(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            return Arrays.copyOfRange(array, offset*dimension, (offset+nbTuple)*dimension);
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public void set(int index, TupleArray array, int offset, int nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final short[] arr = array.toArrayShort(offset, nb);
            System.arraycopy(arr, 0, this.array, index*dimension, nb*dimension);
        }

        @Override
        public Short resize(int newSize) {
            return new Short(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public Short copy() {
            return new Short(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return array[tupleIndex*dimension + sampleIndex];
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = (short) value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNs{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }

    }

    static final class UShort extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final short[] array;

        UShort(SampleSystem type, short[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.USHORT;
        }

        @Override
        public void get(int index, Tuple buffer) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                buffer.set(i, java.lang.Short.toUnsignedInt(array[offset+i]));
            }
        }

        @Override
        public void set(int index, Tuple tuple) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (short) tuple.get(i);
            }
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public byte[] toArrayByte(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            return Arrays.copyOfRange(array, offset*dimension, (offset+nbTuple)*dimension);
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k] & 0xFFFF;
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k] & 0xFFFF;
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k] & 0xFFFF;
            }
            return result;
        }


        @Override
        public void set(int index, TupleArray array, int offset, int nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final short[] arr = array.toArrayShort(offset, nb);
            System.arraycopy(arr, 0, this.array, index*dimension, nb*dimension);
        }

        @Override
        public UShort resize(int newSize) {
            return new UShort(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public UShort copy() {
            return new UShort(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return java.lang.Short.toUnsignedInt(array[tupleIndex*dimension + sampleIndex]);
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = (short) value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNus{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }

    }

    static final class Int extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final int[] array;

        Int(SampleSystem type, int[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.INT;
        }

        @Override
        public void get(int index, Tuple buffer) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i]);
            }
        }

        @Override
        public void set(int index, Tuple tuple) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (int) tuple.get(i);
            }
        }

        @Override
        public int[] toArrayInt() {
            return array.clone();
        }

        @Override
        public byte[] toArrayByte(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (short) array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            return Arrays.copyOfRange(array, offset*dimension, (offset+nbTuple)*dimension);
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public void set(int index, TupleArray array, int offset, int nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final int[] arr = array.toArrayInt(offset, nb);
            System.arraycopy(arr, 0, this.array, index*dimension, nb*dimension);
        }

        @Override
        public Int resize(int newSize) {
            return new Int(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public Int copy() {
            return new Int(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return array[tupleIndex*dimension + sampleIndex];
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = (int) value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNi{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }
    }

    static final class UInt extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final int[] array;

        UInt(SampleSystem type, int[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        UInt(SampleSystem type, List<Integer> array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array.stream().mapToInt(Integer::intValue).toArray();
            if (array.size() % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.size());
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.UINT;
        }

        @Override
        public void get(int index, Tuple buffer) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                buffer.set(i, Integer.toUnsignedLong(array[offset+i]));
            }
        }

        @Override
        public void set(int index, Tuple tuple) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (int) tuple.get(i);
            }
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public byte[] toArrayByte(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (byte) (array[k] & 0xFFFFFFFFl);
            }
            return result;
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (short) (array[k] & 0xFFFFFFFFl);
            }
            return result;
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            return Arrays.copyOfRange(array, offset*dimension, (offset+nbTuple)*dimension);
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k] & 0xFFFFFFFFl;
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k] & 0xFFFFFFFFl;
            }
            return result;
        }


        @Override
        public void set(int index, TupleArray array, int offset, int nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final int[] arr = array.toArrayInt(offset, nb);
            System.arraycopy(arr, 0, this.array, index*dimension, nb*dimension);
        }

        @Override
        public UInt resize(int newSize) {
            return new UInt(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public UInt copy() {
            return new UInt(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return Integer.toUnsignedLong(array[tupleIndex*dimension + sampleIndex]);
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = (int) value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNui{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }
    }

    static final class Long extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final long[] array;

        Long(SampleSystem type, long[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.LONG;
        }

        @Override
        public void get(int index, Tuple buffer) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                buffer.set(i, array[offset+i]);
            }
        }

        @Override
        public void set(int index, Tuple tuple) {
            final int offset = index * dimension;
            for (int i=0;i<dimension;i++) {
                array[offset+i] = (int) tuple.get(i);
            }
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public byte[] toArrayByte(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (short) array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (int) array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public Long resize(int newSize) {
            return new Long(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public Long copy() {
            return new Long(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return array[tupleIndex*dimension + sampleIndex];
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = (int) value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNl{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }
    }

    static final class Float extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final float[] array;

        Float(SampleSystem type, float[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.FLOAT;
        }

        @Override
        public Tuple get(int index) {
            final Vector buffer = Vectors.create(type, DataType.FLOAT);
            return buffer.set(array, index * dimension);
        }

        @Override
        public void get(int index, Tuple buffer) {
            if (dimension != buffer.getDimension()) {
                throw new IllegalArgumentException("TupleArray and Tuple must have the same number of dimensions");
            }
            buffer.set(array, index * dimension);
        }

        @Override
        public void set(int index, Tuple tuple) {
            if (dimension != tuple.getDimension()) {
                throw new IllegalArgumentException("TupleArray and Tuple must have the same number of dimensions");
            }
            tuple.toArrayFloat(array, index * dimension);
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public byte[] toArrayByte(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (short) array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (int) array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            return Arrays.copyOfRange(array, offset*dimension, (offset+nbTuple)*dimension);
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final double[] result = new double[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = array[k];
            }
            return result;
        }

        @Override
        public void set(int index, TupleArray array, int offset, int nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final float[] arr = array.toArrayFloat(offset, nb);
            System.arraycopy(arr, 0, this.array, index*dimension, nb*dimension);
        }

        @Override
        public Float resize(int newSize) {
            return new Float(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public Float copy() {
            return new Float(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return array[tupleIndex*dimension + sampleIndex];
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = (float) value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNf{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }
    }

    static final class Double extends TupleArrayND {

        private SampleSystem type;
        private final int dimension;
        private final double[] array;

        Double(SampleSystem type, double[] array) {
            this.type = type;
            this.dimension = type.getSize();
            this.array = array;
            if (array.length % dimension != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.length);
            }
        }

        @Override
        public SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public int getLength() {
            return array.length / dimension;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return type.getCoordinateReferenceSystem();
        }

        @Override
        public void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target crs has a different number of dimensions");
            }
            this.type = type;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public DataType getDataType() {
            return DataType.DOUBLE;
        }

        @Override
        public Tuple get(int index) {
            final Vector buffer = Vectors.create(type, DataType.DOUBLE);
            return buffer.set(array, index * dimension);
        }

        @Override
        public void get(int index, Tuple buffer) {
            if (dimension != buffer.getDimension()) {
                throw new IllegalArgumentException("TupleArray and Tuple must have the same number of dimensions");
            }
            buffer.set(array, index * dimension);
        }

        @Override
        public void set(int index, Tuple tuple) {
            if (dimension != tuple.getDimension()) {
                throw new IllegalArgumentException("TupleArray and Tuple must have the same number of dimensions");
            }
            tuple.toArrayDouble(array, index * dimension);
        }

        @Override
        public void swap(int i, int j) {
            int oi = i*dimension + dimension;
            int oj = j*dimension + dimension;
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
        public byte[] toArrayByte(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final byte[] result = new byte[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (byte) array[k];
            }
            return result;
        }

        @Override
        public short[] toArrayShort(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final short[] result = new short[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (short) array[k];
            }
            return result;
        }

        @Override
        public int[] toArrayInt(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final int[] result = new int[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (int) array[k];
            }
            return result;
        }

        @Override
        public float[] toArrayFloat(int offset, int nbTuple) {
            final int length = nbTuple * dimension;
            final float[] result = new float[length];
            for (int i = 0, k = offset * dimension; i < length; i++, k++) {
                result[i] = (float) array[k];
            }
            return result;
        }

        @Override
        public double[] toArrayDouble(int offset, int nbTuple) {
            return Arrays.copyOfRange(array, offset*dimension, (offset+nbTuple)*dimension);
        }

        @Override
        public void set(int index, TupleArray array, int offset, int nb) {
            ArgumentChecks.ensureCountBetween("dimension", true, dimension, dimension, array.getDimension());
            final double[] arr = array.toArrayDouble(offset, nb);
            System.arraycopy(arr, 0, this.array, index*dimension, nb*dimension);
        }

        @Override
        public Double resize(int newSize) {
            return new Double(type, Arrays.copyOf(array, newSize*dimension));
        }

        @Override
        public Double copy() {
            return new Double(type, array.clone());
        }

        @Override
        public TupleArrayCursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(int tupleIndex, int sampleIndex) {
                    return array[tupleIndex*dimension + sampleIndex];
                }

                @Override
                public void set(int tupleIndex, int sampleIndex, double value) {
                    array[tupleIndex*dimension + sampleIndex] = value;
                }
            };
        }

        @Override
        public String toString() {
            return "TupleArrayNd{" + "dimension=" + dimension + ", tuple.length=" + getLength() + ", array.length=" + array.length + '}';
        }
    }

}
