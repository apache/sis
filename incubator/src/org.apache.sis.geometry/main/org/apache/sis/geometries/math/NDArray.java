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

import java.util.stream.Stream;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;


/**
 * An ND array is fixed n dimension size array of tuples.
 *
 * @todo add an implementation based on MemorySegment
 * @todo add support for very large arrays, splitting in multiple tables
 * @todo This class will be expected to provide efficient support for parallal processing used in API when they will available :
 * - Vector API (https://openjdk.org/jeps/426)
 * - GPGPU / Babylon (https://www.youtube.com/watch?v=qkr3E27XYbY)
 *
 * @author Johann Sorel (Geomatys)
 */
public interface NDArray {

    /**
     * @return if shape has any dimension of size zero.
     */
    default boolean isEmpty() {
        final long[] shape = getShape();
        for (int i = 0; i < shape.length; i++) {
            if (shape[i] == 0) return true;
        }
        return false;
    }

    /**
     * @return shape size of the array
     */
    long[] getShape();

    /**
     * @return sample system, never null.
     */
    SampleSystem getSampleSystem();

    /**
     * Set sample system.
     * @param type
     * @throws IllegalArgumentException if new system dimension are different.
     */
    void setSampleSystem(SampleSystem type);

    /**
     * @return tuple coordinate reference system.
     */
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * @return dimension of each tuple.
     */
    int getDimension();

    /**
     * @return number of samples in the array, which is shape mupliply by sample dimension.
     */
    default long getSampleCount() {
        final long[] shape = getShape();
        long nb = 1;
        for (int i = 0; i < shape.length; i++) {
            nb *= shape[i];
        }
        return nb * getDimension();
    }

    /**
     *
     * @return data type of primitives in this array
     */
    DataType getDataType();

    /**
     * Get tuple.
     * Returned tuple is a copy.
     *
     * @param index tuple index.
     * @return tuple values, tuple is a copy.
     */
    default Tuple get(long[] index) {
        Tuple tuple = Vectors.create(getSampleSystem(), getDataType());
        get(index, tuple);
        return tuple;
    }

    /**
     * Get tuple.
     *
     * @param index tuple index.
     * @param buffer tuple to write into.
     */
    void get(long[] index, Tuple buffer);

    /**
     * Set tuple.
     *
     * @param index tuple index
     * @param buffer new tuple values.
     */
    void set(long[] index, Tuple buffer);

    /**
     * Apply given transformation to all tuples.
     *
     * @param trs not null
     */
    void transform(MathTransform trs) throws TransformException;

    /**
     * Transform coordinates to target crs and change tuple array crs.
     *
     * @param crs target CRS
     * @throws FactoryException
     */
    default void transform(CoordinateReferenceSystem crs) throws FactoryException, TransformException {
        ArgumentChecks.ensureNonNull("crs", crs);
        final CoordinateReferenceSystem baseCrs = getCoordinateReferenceSystem();
        if (baseCrs == null) {
            throw new TransformException("This TupleArray do not have a SampleSystem with a CRS");
        }
        final MathTransform trs = CRS.findOperation(baseCrs, crs, null).getMathTransform();
        setSampleSystem(SampleSystem.of(crs));
        transform(trs);
    }

    /**
     * Create a new TupleArray with a different datatype.
     * @param type new data type, not null
     * @return retyped array, if the type is the same a copy is returned
     */
    NDArray retype(DataType type);

    /**
     * Create a new array with given size.
     * Values are copied from this array, zero values are used to fill the new tuples.
     *
     * @param newShape new array size
     * @return resized array
     */
    NDArray reshape(long[] newShape);

    /**
     * @return copy of this array
     */
    NDArray copy();

    /**
     * @return tuple cursor over this array.
     * @todo need an NDCursor API
     */
    //Cursor cursor();

    /**
     * @return tuple stream over this array.
     */
    Stream<Tuple> stream(boolean parallel);

}
