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
import java.util.stream.StreamSupport;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;


/**
 * A array is fixed size array of tuples.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Array extends NDArray {

    /**
     * @return if length is zero.
     */
    @Override
    default boolean isEmpty() {
        return getLength() == 0;
    }

    @Override
    public default long[] getShape() {
        return new long[]{getLength()};
    }

    /**
     * @return number of tuples in the array.
     */
    long getLength();

    /**
     * @return number of samples in the array, which is dimension mupliply by length.
     */
    @Override
    default long getSampleCount() {
        return ((long) getLength()) * getDimension();
    }

    /**
     * Get tuple.
     * Returned tuple is a copy.
     *
     * @param index tuple index.
     * @return tuple values, tuple is a copy.
     */
    default Tuple get(long index) {
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
    void get(long index, Tuple buffer);

    /**
     * Set tuple.
     *
     * @param index tuple index
     * @param buffer new tuple values.
     */
    void set(long index, Tuple buffer);

    /**
     * Get tuple.
     *
     * @param index tuple index.
     * @param buffer tuple to write into.
     */
    @Override
    default void get(long[] index, Tuple buffer) {
        if (index.length != 1) throw new IllegalArgumentException("New shape must have a dimension of one");
        get(index[0], buffer);
    }

    /**
     * Set tuple.
     *
     * @param index tuple index
     * @param buffer new tuple values.
     */
    @Override
    default void set(long[] index, Tuple buffer) {
        if (index.length != 1) throw new IllegalArgumentException("New shape must have a dimension of one");
        set(index[0], buffer);
    }

    /**
     * Set a range on tuple from given array.
     * @param index starting index in this array
     * @param array arry to copy from
     * @param offset starting offset in given array
     * @param nb number of tuples to copy
     */
    default void set(long index, Array array, long offset, long nb) {
        final Vector v = Vectors.create(array.getDimension(), array.getDataType());
        for (long i = 0; i < nb; i++) {
            array.get(offset + i, v);
            set(index + i, v);
        }
    }

    /**
     * Efficient swap tuple at given indexes.
     * @param i
     * @param j
     */
    default void swap(long i, long j) {
        final Tuple ti = get(i);
        final Tuple tj = get(j);
        set(i, tj);
        set(j, ti);
    }

    /**
     * @return this array tuples as an interleaved array cast to bytes.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default byte[] toArrayByte() {
        return toArrayByte(0, Math.toIntExact(getLength()));
    }

    /**
     * @return this array tuples as an interleaved array cast to shorts.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default short[] toArrayShort() {
        return toArrayShort(0, Math.toIntExact(getLength()));
    }

    /**
     * @return this array tuples as an interleaved array cast to integers.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default int[] toArrayInt() {
        return toArrayInt(0, Math.toIntExact(getLength()));
    }

    /**
     * @return this array tuples as an interleaved array cast to floats.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default float[] toArrayFloat() {
        return toArrayFloat(0, Math.toIntExact(getLength()));
    }

    /**
     * @return this array tuples as an interleaved array cast to doubles.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default double[] toArrayDouble() {
        return toArrayDouble(0, Math.toIntExact(getLength()));
    }

    /**
     * @param offset index of the first tuple to copy
     * @param nbTuple number of tuples to copy
     * @return this array tuples as an interleaved array cast to bytes.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default byte[] toArrayByte(long offset, int nbTuple) {
        final int dimension = getDimension();
        final byte[] array = new byte[nbTuple*dimension];
        final Tuple v = Vectors.createByte(dimension);
        long k = offset;
        for (int i = 0; i < nbTuple; i++, k++) {
            get(k, v);
            v.toArrayByte(array, i*dimension);
        }
        return array;
    }

    /**
     * @param offset index of the first tuple to copy
     * @param nbTuple number of tuples to copy
     * @return this array tuples as an interleaved array cast to shorts.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default short[] toArrayShort(long offset, int nbTuple) {
        final int dimension = getDimension();
        final short[] array = new short[nbTuple*dimension];
        final Tuple v = Vectors.createShort(dimension);
        long k = offset;
        for (int i = 0; i < nbTuple; i++, k++) {
            get(k, v);
            v.toArrayShort(array, i*dimension);
        }
        return array;
    }

    /**
     * @param offset index of the first tuple to copy
     * @param nbTuple number of tuples to copy
     * @return this array tuples as an interleaved array cast to integers.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default int[] toArrayInt(long offset, int nbTuple) {
        final int dimension = getDimension();
        final int[] array = new int[nbTuple*dimension];
        final Tuple v = Vectors.createInt(dimension);
        long k = offset;
        for (int i = 0; i < nbTuple; i++, k++) {
            get(k, v);
            v.toArrayInt(array, i*dimension);
        }
        return array;
    }

    /**
     * @param offset index of the first tuple to copy
     * @param nbTuple number of tuples to copy
     * @return this array tuples as an interleaved array cast to floats.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default float[] toArrayFloat(long offset, int nbTuple) {
        final int dimension = getDimension();
        final float[] array = new float[nbTuple*dimension];
        final Vector v = Vectors.createFloat(dimension);
        long k = offset;
        for (int i = 0; i < nbTuple; i++, k++) {
            get(k, v);
            v.toArrayFloat(array, i*dimension);
        }
        return array;
    }

    /**
     * @param offset index of the first tuple to copy
     * @param nbTuple number of tuples to copy
     * @return this array tuples as an interleaved array cast to doubles.
     *         Example : [x1,x2,x3, y1,y2,y3, ... z1, z2,z3]
     */
    default double[] toArrayDouble(long offset, int nbTuple) {
        final int dimension = getDimension();
        final double[] array = new double[nbTuple*dimension];
        final Cursor cursor = cursor();
        long k = offset;
        for (int i = 0; i < nbTuple; i++, k++) {
            cursor.moveTo(k);
            cursor.samples().toArrayDouble(array, i*dimension);
        }
        return array;
    }

    /**
     * Apply given transformation to all tuples.
     *
     * @param trs not null
     */
    @Override
    default void transform(MathTransform trs) throws TransformException {
        final Vector v = Vectors.createDouble(getDimension());
        for (long i=0,n=getLength();i<n;i++) {
            get(i, v);
            v.transform(trs);
            set(i,v);
        }
    }

    /**
     * Transform coordinates to target crs and change tuple array crs.
     *
     * @param crs target CRS
     * @throws FactoryException
     */
    @Override
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
    @Override
    default Array retype(DataType type) {
        Array formated = NDArrays.of(getSampleSystem(), type, getLength());
        formated.set(0, this, 0, formated.getLength());
        return formated;
    }

    @Override
    default NDArray reshape(long[] newShape) {
        if (newShape.length != 1) throw new IllegalArgumentException("New shape must have a dimension of one");
        return resize(newShape[0]);
    }

    /**
     * Create a new array with given size.
     * Values are copied from this array, zero values are used to fill the new tuples.
     *
     * @param newSize new array size
     * @return resized array
     */
    Array resize(long newSize);

    /**
     * @return copy of this array
     */
    @Override
    Array copy();

    /**
     * @return tuple cursor over this array.
     */
    Cursor cursor();

    /**
     * @return tuple stream over this array.
     */
    @Override
    default Stream<Tuple> stream(boolean parallel) {
        return StreamSupport.stream(new ArraySpliterator(this), parallel);
    }

    /**
     * Test tuple arrays equality.
     *
     * Tuples are a key element in OpenGL and GPU which have variable bits representation of numbers
     * including half-floats, oct-compression, normalisation, quantization, unsigned values and more.
     *
     * This equality test checks values and CRS but does not verify that data types are identical.
     * This behavior allows to compare Tuple following their contract ignoring the storage backend.
     *
     * @param other second tuple array to test against
     * @param tolerance tolerance value for compare operation
     * @return true if tuples are value equal
     */
    default boolean equals(Array other, double tolerance) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }

        final int dim = getDimension();
        if (dim != other.getDimension()) {
            return false;
        }
        final long length = getLength();
        if (length != other.getLength()) {
            return false;
        }
        if (!getSampleSystem().equals(other.getSampleSystem())) {
            return false;
        }
        for (long i = 0; i < length; i++) {
            Tuple v1 = get(i);
            Tuple v2 = other.get(i);
            if (!v1.equals(v2, tolerance)) {
                return false;
            }
        }
        return true;
    }

}
