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

import java.lang.foreign.SegmentAllocator;
import java.util.Collection;
import java.util.Iterator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Factory to create new arrays of different types.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface ArrayFactory {

    public static final ArrayFactory JAVA = ArrayFactoryJava.INSTANCE;

    public static ArrayFactory newFFMFactory(SegmentAllocator allocator) {
        return new ArrayFactoryFFM(allocator);
    }

    Builder builder();

    public static interface Builder {

        /**
         * Set the array shape.
         *
         * @param shape
         * @return this builder
         */
        Builder shape(long ... shape);

        /**
         * Set the array system.
         *
         * @param crs
         * @return this builder
         */
        default Builder system(CoordinateReferenceSystem crs) {
            return system(SampleSystem.of(crs));
        }

        /**
         * Set the array system.
         *
         * @param system
         * @return this builder
         */
        Builder system(SampleSystem system);

        /**
         * Set array datatype.
         *
         * @param type
         * @return this builder
         */
        Builder dataType(DataType type);

        /**
         * Set the initial array values to the given values.
         *
         * @param values, can be any java primitive array, a Collection<Tuple or primitive array> or another Array
         * @param canUse, if true the factory is allowed to use the given values instances directly if it is possible.
         *                 if false, values will always be copied
         * @return this builder
         */
        Builder values(Object values, boolean canUse);

        /**
         * Set the initial array values to the given value.
         * Value will be duplicated to fill the array.
         *
         * @param values, can be any java primitive array, a Tuple
         * @return this builder
         */
        Builder fill(Object values);

        /**
         * Create the ND array.
         * If datatype is not set the type will be inferred from the values if they are defined.
         * If shape is not set the type will be inferred from the values if they are defined.
         * If samplesystem is not set, an undefined one will be used.
         *
         * @return NDArray
         * @throws IllegalArgumentException if parameters are incorrect.
         */
        NDArray buildND();

        /**
         * Create the array.
         *
         * @return Array
         * @throws IllegalArgumentException if parameters are incorrect.
         * @see #buildND()
         */
        Array build();

    }


    public static abstract class AbstractBuilder<T extends Builder> implements Builder {

        protected long[] shape;
        protected SampleSystem system;
        protected DataType dataType;
        protected Object values;
        protected boolean canUseValues;
        protected Object fill;

        @Override
        public T shape(long... shape) {
            this.shape = shape;
            return (T)this;
        }

        @Override
        public T system(SampleSystem system) {
            this.system = system;
            return (T)this;
        }

        @Override
        public T dataType(DataType dataType) {
            this.dataType = dataType;
            return (T)this;
        }

        @Override
        public T values(Object values, boolean canUse) {
            this.values = values;
            this.canUseValues = canUse;
            return (T)this;
        }

        @Override
        public T fill(Object values) {
            this.fill = values;
            return (T)this;
        }

        protected SampleSystem getSystem() {
            if (system != null) {
                return system;
            } else if (values instanceof NDArray nd) {
                return nd.getSampleSystem();
            } else if (values instanceof Collection<?> col) {
                final Iterator<?> ite = col.iterator();
                if (ite.hasNext()) {
                    final Object o = ite.next();
                    if (o instanceof Tuple<?> t) {
                        return t.getSampleSystem();
                    } else if (o != null && o.getClass().isArray()) {
                        final int size = java.lang.reflect.Array.getLength(o);
                        return SampleSystem.ofSize(size);
                    } else {
                        throw new IllegalArgumentException("Values iterable is not made of Tuple or primitive array");
                    }
                }
            }
            return SampleSystem.ofSize(1);
        }

        protected long[] getShape() {
            final SampleSystem system = getSystem();
            final int nbSample = system.getSize();

            if (shape != null) {
                return shape;
            } else if (values instanceof NDArray nd) {
                return nd.getShape();
            } else if (values != null && values.getClass().isArray()) {
                final int size = java.lang.reflect.Array.getLength(values);
                if ((size % nbSample) != 0) throw new IllegalArgumentException("Values size : " + size + "is not a multiple of sample system size : " + nbSample);
                return new long[]{size / nbSample};
            } else if (values instanceof Collection<?> col) {
                return new long[]{col.size()};
            }
            throw new IllegalArgumentException("Array shape could not be inferred from parameters");
        }

        protected DataType getDataType() {
            if (dataType != null) {
                return dataType;
            } else if (values instanceof NDArray nd) {
                return nd.getDataType();
            } else if (values != null && values.getClass().isArray()) {
                final Class<?> componentType = values.getClass().getComponentType();
                return DataType.forPrimitiveType(componentType, false);
            } else if (values instanceof Collection<?> col) {
                final Iterator<?> ite = col.iterator();
                if (ite.hasNext()) {
                    final Object o = ite.next();
                    if (o instanceof Tuple<?> t) {
                        return t.getDataType();
                    } else if (o != null && o.getClass().isArray()) {
                        final Class<?> componentType = values.getClass().getComponentType();
                        return DataType.forPrimitiveType(componentType, false);
                    } else {
                        throw new IllegalArgumentException("Values iterable is not made of Tuple or primitive array");
                    }
                }
            }
            throw new IllegalArgumentException("Array data type could not be inferred from parameters");
        }

        protected void copyOrFillValues(Array target) {
            final int nbDim = target.getSampleSystem().getSize();

            if (values != null) {
                if (values instanceof Array array) {
                    target.set(0, array, 0, array.getLength());
                } else if (values != null && values.getClass().isArray()) {
                    int idx = 0;
                    final Cursor cursor = target.cursor();
                    while (cursor.next()) {
                        final Tuple tuple = cursor.samples();
                        for (int i = 0; i < nbDim; i++) {
                            tuple.set(i, java.lang.reflect.Array.getDouble(values, idx));
                            idx++;
                        }
                    }
                } else if (values instanceof Collection<?> col) {
                    final Iterator<?> ite = col.iterator();
                    final Cursor cursor = target.cursor();
                    while (ite.hasNext()) {
                        cursor.next();
                        final Object o = ite.next();
                        final Tuple tuple = cursor.samples();
                        if (o instanceof Tuple<?> t) {
                            tuple.set(t);
                        } else if (o != null && o.getClass().isArray()) {
                            for (int i = 0; i < nbDim; i++) {
                                tuple.set(i, java.lang.reflect.Array.getDouble(o, i));
                            }
                        } else {
                            throw new IllegalArgumentException("Values iterable is not made of Tuple or primitive array");
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Values type not supported");
                }
            } else if (fill != null) {

                Tuple<?> f;
                if (fill instanceof ReadOnly.Tuple<?> t) {
                    f = t.copy();
                } else if (fill.getClass().isArray()) {
                    f = Vectors.create(system, target.getDataType());
                    for (int i = 0; i < nbDim; i++) {
                        f.set(i, java.lang.reflect.Array.getDouble(fill, i));
                    }
                } else {
                 throw new IllegalArgumentException("Fill value is not a Tuple or primitive array");
                }

                target.set(f);
            }

        }

    }
}
