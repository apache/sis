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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Factory to create new arrays of different types.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface ArrayFactory {

    public static final ArrayFactory JAVA = JavaFactory.INSTANCE;

    public static ArrayFactory newFFMFactory(SegmentAllocator allocator) {
        throw new UnsupportedOperationException();
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
         * @param values, can be any java array type, or a Iterable<Tuple> or a ByteBuffer
         * @return
         */
        Builder values(Object values);

        /**
         * Set the initial array values to the given value.
         * Value will be duplicated to fill the array.
         *
         * @param values, can be any java array type, or a Iterable<Tuple> or a ByteBuffer
         * @return this builder
         */
        Builder fill(Object values);

        /**
         * Create the ND array.
         * If datatype is not set the type will be infered from the values if they are defined.
         * If shape is not set the type will be infered from the values if they are defined.
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

}
