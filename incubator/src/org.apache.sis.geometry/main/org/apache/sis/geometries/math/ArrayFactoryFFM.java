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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import static org.apache.sis.geometries.math.DataType.BYTE;
import static org.apache.sis.geometries.math.DataType.DOUBLE;
import static org.apache.sis.geometries.math.DataType.FLOAT;
import static org.apache.sis.geometries.math.DataType.INT;
import static org.apache.sis.geometries.math.DataType.LONG;
import static org.apache.sis.geometries.math.DataType.SHORT;
import static org.apache.sis.geometries.math.DataType.UBYTE;
import static org.apache.sis.geometries.math.DataType.UINT;
import static org.apache.sis.geometries.math.DataType.USHORT;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ArrayFactoryFFM implements ArrayFactory {

    private final SegmentAllocator allocator;

    ArrayFactoryFFM(SegmentAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public Builder builder() {
        return new FFMBuilder();
    }

    private final class FFMBuilder extends AbstractBuilder<FFMBuilder> {

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
            final long arraySizeBytes = arraySize * (dataType.size() / 8);

            final MemorySegment pointer = allocator.allocate(arraySizeBytes);

            final Array array;
            switch (dataType) {
                case BYTE : array = new ArrayFactoryFFM.Byte(system, pointer); break;
                case UBYTE : array = new ArrayFactoryFFM.UByte(system, pointer); break;
                case SHORT : array = new ArrayFactoryFFM.Short(system, pointer); break;
                case USHORT : array = new ArrayFactoryFFM.UShort(system, pointer); break;
                case INT : array = new ArrayFactoryFFM.Int(system, pointer); break;
                case UINT : array = new ArrayFactoryFFM.UInt(system, pointer); break;
                case LONG : array = new ArrayFactoryFFM.Long(system, pointer); break;
                case FLOAT : array = new ArrayFactoryFFM.Float(system, pointer); break;
                case DOUBLE : array = new ArrayFactoryFFM.Double(system, pointer); break;
                default : throw new IllegalArgumentException("Unexpected data type " + dataType);
            }

            //todo, check if we can reuse the values array
            copyOrFillValues(array);
            return array;
        }

    }

    private abstract class FFMArray extends AbstractArray {

        protected final MemorySegment array;
        protected SampleSystem type;
        protected final int dimension;
        private final long length;

        public FFMArray(SampleSystem type, MemorySegment pointer) {
            this.array = pointer;
            this.type = type;
            this.dimension = type.getSize();

            final int tupleSizeBytes = (getDataType().size() / 8) * dimension;
            this.length = pointer.byteSize() / tupleSizeBytes;

            if (array.byteSize() % tupleSizeBytes != 0) {
                throw new IllegalArgumentException("Array size is not compatible, expected n*" + dimension + " but size is " + array.byteSize());
            }
        }

        @Override
        public final ArrayFactory getFactory() {
            return ArrayFactoryFFM.this;
        }

        @Override
        public final SampleSystem getSampleSystem() {
            return type;
        }

        @Override
        public final void setSampleSystem(SampleSystem type) {
            if (dimension != type.getSize()) {
                throw new IllegalArgumentException("Target type has a different number of dimensions");
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

        @Override
        public final long getLength() {
            return length;
        }
    }

    private final class Byte extends FFMArray {

        private Byte(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.BYTE;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private final class UByte extends FFMArray {

        UByte(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.UBYTE;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private final class Short extends FFMArray {

        Short(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.SHORT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private final class UShort extends FFMArray {

        UShort(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.USHORT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private final class Int extends FFMArray {

        Int(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.INT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private final class UInt extends FFMArray {

        UInt(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.UINT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private final class Long extends FFMArray {

        Long(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.LONG;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private final class Float extends FFMArray {

        Float(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.FLOAT;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private final class Double extends FFMArray {

        Double(SampleSystem type, MemorySegment array) {
            super(type, array);
        }

        @Override
        public DataType getDataType() {
            return DataType.DOUBLE;
        }

        @Override
        public void get(long index, Tuple<?> buffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(long index, ReadOnly.Tuple<?> tuple) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor cursor() {
            return new AbstractCursor(this) {
                @Override
                public double get(long tupleIndex, int sampleIndex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(long tupleIndex, int sampleIndex, double value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

}
