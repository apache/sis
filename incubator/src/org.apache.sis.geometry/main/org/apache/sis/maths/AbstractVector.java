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

import java.util.Arrays;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
abstract class AbstractVector<T extends AbstractVector<T>> implements Vector<T> {

    public AbstractVector() {
    }

    @Override
    public int hashCode() {
        int hash = 31;
        for (int i = 0, n = getDimension(); i < n; i++) {
            final double value = get(i);
            //positive and negative zero are equal, so they shall have the same hash code.
            hash += 31 * hash + Double.hashCode(value == 0 ? 0 : value);
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Vector)) {
            return false;
        }
        final Vector other = (Vector) obj;

        final int dim = getDimension();
        if (dim != other.getDimension()) {
            return false;
        }
        for (int i = 0; i < dim; i++) {
            double v1 = get(i);
            double v2 = other.get(i);
            if (v1 != v2) {
                //check for NaN equality
                if (Double.doubleToRawLongBits(v1) != Double.doubleToRawLongBits(v2)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+Arrays.toString(toArrayDouble());
    }

    /**
     * Check if given vector has the expected number of dimensions.
     *
     * @param vector vector to verify
     * @param dimension expected dimension
     * throws IllegalArgumentException if dimension do not match
     */
    protected static void ensureDimension(Vector<?> vector, int dimension) {
        if (vector.getDimension() != dimension) {
            throw new IllegalArgumentException("Vector dimension must be " + dimension);
        }
    }

    /**
     * Check if given array has the expected number of values.
     *
     * @param length array length
     * @param dimension expected dimension
     * throws IllegalArgumentException if the array length does not match the dimension
     */
    protected static void ensureLength(int length, int dimension) {
        if (length != dimension) {
            throw new IllegalArgumentException("Dimension mismatch : array length must be " + dimension + " but is " + length);
        }
    }

    /**
     * Creates the result of a linear combination of two vectors.
     *
     * @param <V> type of the result
     */
    @FunctionalInterface
    interface Combination<V> {
        /**
         * @param a coefficient of the first vector
         * @param b coefficient of the second vector
         * @return a*first + b*second
         */
        V combine(double a, double b);
    }

    /**
     * Spherical linear interpolation from start vector to end vector.
     * See {@link Vector#slerp(Vector, double)} for the algorithm description.
     * The result is always a linear combination a*start + b*end, this method computes
     * the two coefficients and gives them to the combination which creates the result.
     *
     * @param start vector to interpolate from
     * @param end vector to interpolate toward
     * @param ratio interpolation factor, zero for start vector and one for end vector
     * @param combination creates a*start + b*end
     * @return result of the combination
     * @throws IllegalArgumentException if both vectors point in opposite directions
     */
    static <V> V slerp(Vector<?> start, Vector<?> end, double ratio, Combination<V> combination) {
        ensureDimension(end, start.getDimension());
        if (ratio <= 0) {
            return combination.combine(1, 0);
        } else if (ratio >= 1) {
            return combination.combine(0, 1);
        }

        /*
         * Angle between the two directions. Dividing by the lengths keeps the angle right
         * for vectors which are not unit vectors. A vector of length zero has no direction,
         * which the parallel case below handles as a linear interpolation.
         */
        final double lengthStart = start.length();
        final double lengthEnd = end.length();
        final double lengths = lengthStart * lengthEnd;
        final double cosAngle = (lengths > 0) ? Maths.clamp(start.dot(end) / lengths, -1, 1) : 1;
        final double angle = Math.acos(cosAngle);
        final double sinAngle = Math.sin(angle);

        if (sinAngle < 1E-9) {
            if (cosAngle < 0) {
                throw new IllegalArgumentException("Cannot interpolate between vectors pointing in "
                        + "opposite directions, they define no unique arc.");
            }
            //nearly parallel vectors, the arc is the straight line joining them
            return combination.combine(1 - ratio, ratio);
        }

        /*
         * The ratios below sweep the angle evenly for vectors of length one, so they are
         * applied to the directions rather than to the vectors, and the result is brought
         * back to the interpolated length. Doing it on the vectors directly would let the
         * longer one pull the direction toward itself.
         */
        final double ratioStart = Math.sin(angle * (1-ratio)) / sinAngle / lengthStart;
        final double ratioEnd   = Math.sin(angle *    ratio ) / sinAngle / lengthEnd;
        final double interpolatedLength = (1-ratio)*lengthStart + ratio * lengthEnd;
        return combination.combine(interpolatedLength * ratioStart, interpolatedLength * ratioEnd);
    }
}
