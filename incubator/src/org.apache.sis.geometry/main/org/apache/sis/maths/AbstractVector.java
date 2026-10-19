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
        if (!(obj instanceof ReadOnly.Vector)) {
            return false;
        }
        final ReadOnly.Vector other = (ReadOnly.Vector) obj;

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
    protected static void ensureDimension(ReadOnly.Vector<?> vector, int dimension) {
        if (vector.getDimension() != dimension) {
            throw new IllegalArgumentException("Vector dimension must be " + dimension);
        }
    }
}
