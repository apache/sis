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

/**
 * A vector is a subclass of tuple with arithmetic operations defined.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Vector<T extends Vector<T>> extends Tuple<T> {

    /**
     * @return vector length
     */
    default double length() {
        return Vectors.length(toArrayDouble());
    }

    /**
     * @return vector squere length
     */
    default double lengthSquare() {
        return Vectors.lengthSquare(toArrayDouble());
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
     * Normalize this vector.
     * @param result where to store the result
     * @return result of the operation, this vector is unchanged
     */
    default <R extends Tuple<?>> R normalize(R result) {
        if (result == null) result = (R) this.copy();
        result.set(this);
        Vectors.castOrWrap(result).normalize();
        return result;
    }

    /**
     * Add other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T add(Tuple<?> other) {
        set( Vectors.add(toArrayDouble(), other.toArrayDouble()));
        return (T) this;
    }

    /**
     * Subtract other vector values to this vector.
     * @param other
     * @return this vector
     */
    default T subtract(Tuple<?> other) {
        set( Vectors.subtract(toArrayDouble(), other.toArrayDouble()));
        return (T) this;
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
     * Cross product.
     *
     * @param other
     * @return cross product result
     */
    default T cross(Tuple<?> other) {
        double[] v1 = toArrayDouble();
        double[] v2 = other.toArrayDouble();
        double[] buffer = new double[v1.length];
        Vectors.cross(v1, v2, buffer);
        Vector<?> res = Vectors.createDouble(buffer.length);
        res.set(buffer);
        return (T) res;
    }

    /**
     * Dot product.
     *
     * @param other
     * @return dot product result
     */
    default double dot(Tuple<?> other) {
        double dot = 0;
        for (int i=0,n=getDimension();i<n;i++){
            dot += get(i) * other.get(i);
        }
        return dot;
    }

    /**
     * Increase size of the tuple by one value.
     * Vector CRS will be lost.
     *
     * @param d last dimension value
     * @return Vector
     */
    default Vector<?> extend(double d) {
        final int dim = getDimension();
        final Vector v = Vectors.create(dim+1, getDataType());
        for (int i = 0; i < dim; i++) {
            v.set(i, get(i));
        }
        v.set(dim, d);
        return v;
    }
}
