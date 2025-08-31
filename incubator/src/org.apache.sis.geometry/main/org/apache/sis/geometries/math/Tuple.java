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

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CRS;


/**
 * A tuple is an array of values.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Tuple<T extends Tuple <T>> {

    /**
     * @return sample system, never null.
     */
    SampleSystem getSampleSystem();

    /**
     * @return sample system size.
     */
    default int getDimension() {
        return getSampleSystem().getSize();
    }

    /**
     * @return sample system CRS, may be null.
     */
    default CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return getSampleSystem().getCoordinateReferenceSystem();
    }

    /**
     * @return data type of primitives in this array
     */
    DataType getDataType();

    /**
     * Get sample at index.
     *
     * @param indice sample index
     * @return value at index
     * @throws IndexOutOfBoundsException if index is not valid
     */
    double get(int indice) throws IndexOutOfBoundsException;

    /**
     * Set sample value at index.
     *
     * @param indice sample index
     * @param value sample value
     * @throws IndexOutOfBoundsException if index is not valid
     */
    void set(int indice, double value) throws IndexOutOfBoundsException;

    /**
     * Copy values from given direct position to this tuple.
     *
     * @param values to copy from
     * @return this tuple
     * @throws IndexOutOfBoundsException if dimension is smaller then this tuple
     */
    default T set(DirectPosition values) throws IndexOutOfBoundsException{
        for (int i = 0, n = getDimension(); i < n; i++) {
            set(i, values.getCoordinate(i));
        }
        return (T) this;
    }

    /**
     * Set tuple values.
     * @param values array to copy values from.
     * @return this tuple
     * @throws IndexOutOfBoundsException if dimension is smaller then this tuple
     */
    default T set(double[] values) throws IndexOutOfBoundsException {
        return set(values, 0);
    }

    /**
     * Set tuple values.
     * @param values array to copy values from.
     * @param offset offset to start copy from
     * @return this tuple
     * @throws IndexOutOfBoundsException if dimension is smaller then this tuple
     */
    default T set(double[] values, int offset) throws IndexOutOfBoundsException {
        for (int i = 0, n = getDimension(); i < n; i++) {
            set(i, values[i+offset]);
        }
        return (T) this;
    }

    /**
     * Set tuple values.
     * @param values array to copy values from.
     * @return this tuple
     * @throws IndexOutOfBoundsException if dimension is smaller then this tuple
     */
    default T set(float[] values) throws IndexOutOfBoundsException {
        return set(values, 0);
    }

    /**
     * Set tuple values.
     * @param values array to copy values from.
     * @param offset offset to start copy from
     * @return this tuple
     * @throws IndexOutOfBoundsException if dimension is smaller then this tuple
     */
    default T set(float[] values, int offset) throws IndexOutOfBoundsException {
        for (int i = 0, n = getDimension(); i < n; i++) {
            set(i, values[i+offset]);
        }
        return (T) this;
    }

    /**
     * Set tuple values.
     * @param values array to copy values from.
     * @return this tuple
     * @throws IndexOutOfBoundsException if dimension is smaller then this tuple
     */
    default T set(Tuple<?> values) throws IndexOutOfBoundsException {
        for (int i = 0, n = getDimension(); i < n; i++) {
            set(i, values.get(i));
        }
        return (T) this;
    }

    /**
     * Set tuple values.
     * @param value value to set on each ordinate
     * @return this tuple
     */
    default T setAll(double value) {
        for (int i = 0, n = getDimension(); i < n; i++) {
            set(i, value);
        }
        return (T) this;
    }

    /**
     * Tuple to array.
     *
     * @return values as cast byte array
     */
    default byte[] toArrayByte() {
        byte[] buffer = new byte[getDimension()];
        toArrayByte(buffer, 0);
        return buffer;
    }

    /**
     * Tuple to array.
     *
     * @return values as cast short array
     */
    default short[] toArrayShort() {
        short[] buffer = new short[getDimension()];
        toArrayShort(buffer, 0);
        return buffer;
    }

    /**
     * Tuple to array.
     *
     * @return values as cast integer array
     */
    default int[] toArrayInt() {
        int[] buffer = new int[getDimension()];
        toArrayInt(buffer, 0);
        return buffer;
    }

    /**
     * Tuple to array.
     *
     * @return values as cast float array
     */
    default float[] toArrayFloat() {
        float[] buffer = new float[getDimension()];
        toArrayFloat(buffer, 0);
        return buffer;
    }

    /**
     * Tuple to array.
     *
     * @return values as double array
     */
    default double[] toArrayDouble() {
        double[] buffer = new double[getDimension()];
        toArrayDouble(buffer, 0);
        return buffer;
    }

    /**
     * Tuple to array.
     *
     * @param buffer array to write into
     * @param offset offset at which to write
     */
    default void toArrayByte(byte[] buffer, int offset) {
        for (int i = 0, n = getDimension(); i < n; i++) {
            buffer[offset+i] = (byte) get(i);
        }
    }

    /**
     * Tuple to array.
     *
     * @param buffer array to write into
     * @param offset offset at which to write
     */
    default void toArrayShort(short[] buffer, int offset) {
        for (int i = 0, n = getDimension(); i < n; i++) {
            buffer[offset+i] = (short) get(i);
        }
    }

    /**
     * Tuple to array.
     *
     * @param buffer array to write into
     * @param offset offset at which to write
     */
    default void toArrayInt(int[] buffer, int offset) {
        for (int i = 0, n = getDimension(); i < n; i++) {
            buffer[offset+i] = (int) get(i);
        }
    }

    /**
     * Tuple to array.
     *
     * @param buffer array to write into
     * @param offset offset at which to write
     */
    default void toArrayFloat(float[] buffer, int offset) {
        for (int i = 0, n = getDimension(); i < n; i++) {
            buffer[offset+i] = (float) get(i);
        }
    }

    /**
     * Tuple to array.
     *
     * @param buffer array to write into
     * @param offset offset at which to write
     */
    default void toArrayDouble(double[] buffer, int offset) {
        for (int i = 0, n = getDimension(); i < n; i++) {
            buffer[offset+i] = get(i);
        }
    }

    /**
     * Apply given transform on this tuple.
     *
     * @param trs not null
     * @return this tuple
     * @throws TransformException
     */
    default T transform(MathTransform trs) throws TransformException {
        final double[] array = toArrayDouble();
        trs.transform(array, 0, array, 0, 1);
        return set(array);
    }

    /**
     * Transform this tuple and store the result in given tuple.
     *
     * @param trs not null
     * @param target not null to store transform result
     * @return this tuple
     * @throws TransformException
     */
    default void transformTo(MathTransform trs, Tuple<?> target) throws TransformException {
        final double[] array = toArrayDouble();
        trs.transform(array, 0, array, 0, 1);
        target.set(array);
    }

    /**
     * Create a copy of this tuple.
     *
     * @return tuple copy.
     */
    default T copy() {
        T tuple = (T) Vectors.create(getSampleSystem(), getDataType());
        tuple.set(this);
        return tuple;
    }

    /**
     * @return true if all values are finite (not infinite or NaN)
     */
    default boolean isFinite(){
        for (int i = 0, n = getDimension(); i < n; i++) {
            if (!Double.isFinite(get(i))) return false;
        }
        return true;
    }

    /**
     * @return true if all samples match given value.
     */
    default boolean isAll(double value) {
        for (int i = 0, n = getDimension(); i < n; i++) {
            if (value != get(i)) return false;
        }
        return true;
    }

    /**
     * Tuples are equal when all samples and CRS match.
     */
    @Override
    boolean equals(Object candidate);

    /**
     * Test tuples equality.
     *
     * Tuples are a key element in OpenGL and GPU which have variable bits representation of numbers
     * including half-floats, oct-compression, normalisation, quantization, unsigned values and more.
     *
     * This equality test checks values and CRS but does not verify that data types are identical.
     * This behavior allows to compare Tuple following their contract ignoring the storage backend.
     *
     * @param obj second tuple to test against
     * @param tolerance tolerance value for compare operation
     * @return true if tuples are value equal
     */
    default boolean equals(Tuple<?> other, double tolerance) {
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
        for (int i = 0; i < dim; i++) {
            double v1 = get(i);
            double v2 = other.get(i);
            if (v1 != v2) {
                //check with tolerance
                if (Math.abs(v1 - v2) <= tolerance) {
                    continue;
                }
                //check for NaN equality
                if (Double.doubleToRawLongBits(v1) != Double.doubleToRawLongBits(v2)) {
                    return false;
                }
            }
        }
        //checking crs is expensive, do it last
        if (!CRS.equivalent(getCoordinateReferenceSystem(), other.getCoordinateReferenceSystem())) {
            return false;
        }
        return true;
    }

}
