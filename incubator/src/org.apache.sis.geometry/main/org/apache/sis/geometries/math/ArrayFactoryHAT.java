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

import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * This class is a place holder for HAT arrays when they will be avaible.
 * An array store on an accelerator.
 *
 * @see https://openjdk.org/projects/babylon/articles/hat-matmul/hat-matmul
 * @author Johann Sorel (Geomatys)
 */
public final class ArrayFactoryHAT implements ArrayFactory {

    private final Object accelerator; //to be replaced by hat.Accelerator

    ArrayFactoryHAT(Object accelerator) {
        this.accelerator = accelerator;
    }

    @Override
    public Builder builder() {
        return new HatBuilder();
    }

    private final class HatBuilder extends AbstractBuilder<HatBuilder> {

        @Override
        public NDArray buildND() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Array build() {
            //to be replaced by hat.buffer.* instances
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    private abstract class HatArray extends AbstractArray {

        protected final Object array; // to be replaced by hat.buffer.* instances
        protected SampleSystem type;
        protected final int dimension;

        public HatArray(SampleSystem type, Object pointer) {
            this.array = pointer;
            this.type = type;
            this.dimension = type.getSize();
        }

        @Override
        public final ArrayFactory getFactory() {
            return ArrayFactoryHAT.this;
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
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private final class Signed32 extends HatArray {

        Signed32(SampleSystem type, Object buffer) {
            super(type, buffer);
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

    private final class Float16 extends HatArray {

        Float16(SampleSystem type, Object buffer) {
            super(type, buffer);
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

    private final class Float32 extends HatArray {

        Float32(SampleSystem type, Object buffer) {
            super(type, buffer);
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

    private final class Float64 extends HatArray {

        Float64(SampleSystem type, Object buffer) {
            super(type, buffer);
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
