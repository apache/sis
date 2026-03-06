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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * A tuple is an array of values.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Tuple<T extends Tuple <T>> extends ReadOnly.Tuple<T>{

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
    default T set(ReadOnly.Tuple<?> values) throws IndexOutOfBoundsException {
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
     * Apply given transform on this tuple.
     *
     * @param trs not null
     * @return this tuple
     */
    default T transform(Transform trs) {
        trs.transform(this, this);
        return (T) this;
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

}
