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


/**
 * A vector is a subclass of tuple with arithmetic operations defined.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Vector<T extends Vector<T>> extends Tuple<T>, ReadOnly.Vector<T> {

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
    default T add(ReadOnly.Tuple<?> other) {
        set( Vectors.add(toArrayDouble(), other.toArrayDouble()));
        return (T) this;
    }

    /**
     * Subtract other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T subtract(ReadOnly.Tuple<?> other) {
        set( Vectors.subtract(toArrayDouble(), other.toArrayDouble()));
        return (T) this;
    }

    /**
     * Multiply other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T multiply(ReadOnly.Tuple<?> other) {
        return set(Vectors.multiply(toArrayDouble(), other.toArrayDouble()));
    }

    /**
     * Divide other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T divide(ReadOnly.Tuple<?> other) {
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
    default T lerp(ReadOnly.Tuple<?> other, double ratio) {
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
    default T slerp(ReadOnly.Tuple<?> other, double ratio) {
        set( Vectors.slerp(toArrayDouble(), other.toArrayDouble(), ratio));
        return (T) this;
    }

    @Override
    public T copy();

}
