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
package org.apache.sis.maths;

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * A Vector is an array of values.
 * With arithmetic operations defined.
 *
 * Note : originaly we had two interfaces 'Tuple' and 'Vector', the Tuple was only a container and Vector extended it
 * with more arithmetic methods. This state created duplicated classes and imposed conversions from Tuple to Vector
 * in many cases. This conversion problem is still raised even further with DirectPosition interface.
 * It has been decided to merge Tuple and Vector to reduce duplication and make code more fluent, readable and user friendly.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Vector<T extends Vector <T>> extends ReadOnly.Vector<T>{

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
    default T set(ReadOnly.Vector<?> values) throws IndexOutOfBoundsException {
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
    default void transformTo(MathTransform trs, Vector<?> target) throws TransformException {
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
     * Normalize this vector.
     * @return this vector
     */
    default T normalize() {
        set(Vectors.normalize(toArrayDouble()));
        return (T) this;
    }

    /**
     * Add other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T add(ReadOnly.Vector<?> other) {
        set( Vectors.add(toArrayDouble(), other.toArrayDouble()));
        return (T) this;
    }

    /**
     * Subtract other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T subtract(ReadOnly.Vector<?> other) {
        set( Vectors.subtract(toArrayDouble(), other.toArrayDouble()));
        return (T) this;
    }

    /**
     * Multiply other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T multiply(ReadOnly.Vector<?> other) {
        return set(Vectors.multiply(toArrayDouble(), other.toArrayDouble()));
    }

    /**
     * Divide other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T divide(ReadOnly.Vector<?> other) {
        return set(Vectors.divide(toArrayDouble(), other.toArrayDouble()));
    }

    /**
     * Negate this vector values.
     * @return this vector
     */
    default T negate() {
        return set(Vectors.negate(toArrayDouble()));
    }

    /**
     * Scale vector by given value.
     *
     * @param scale scaling factor
     * @return this vector
     */
    default T scale(double scale) {
        set( Vectors.scale(toArrayDouble(), scale));
        return (T) this;
    }

    /**
     * Linear interpolation from this vector to the other vector : (1-ratio)*this + ratio*other.
     *
     * @param other vector to interpolate toward
     * @param ratio interpolation factor, zero for this vector and one for the other vector
     * @return this vector
     */
    default T lerp(ReadOnly.Vector<?> other, double ratio) {
        set( Vectors.lerp(toArrayDouble(), other.toArrayDouble(), ratio));
        return (T) this;
    }

    /**
     * Spherical linear interpolation from this vector to the other vector.
     * <p>
     * Where {@link #lerp(ReadOnly.Tuple, double) } moves along the straight line joining
     * the two vectors, this moves along the arc joining them, sweeping the angle between
     * them proportionally to the ratio. Both vectors having the same length, the result
     * keeps it too, which makes this the interpolation to use for directions. When their
     * lengths differ, the angle is still swept evenly and the length is interpolated
     * linearly.
     *
     * @param other vector to interpolate toward
     * @param ratio interpolation factor, zero for this vector and one for the other vector
     * @return this vector
     * @throws IllegalArgumentException if both vectors point in opposite directions, in
     *         which case they define no unique arc to interpolate along
     * @see Vectors#slerp(double[], double[], double, double[])
     */
    default T slerp(ReadOnly.Vector<?> other, double ratio) {
        set( Vectors.slerp(toArrayDouble(), other.toArrayDouble(), ratio));
        return (T) this;
    }

}
