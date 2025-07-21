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

import java.util.Arrays;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
abstract class AbstractTuple<T extends AbstractTuple<T>> implements Tuple<T> {

    protected final SampleSystem type;

    public AbstractTuple(int dimension) {
        this.type = SampleSystem.ofSize(dimension);
    }

    public AbstractTuple(SampleSystem type) {
        ArgumentChecks.ensureNonNull("type", type);
        this.type = type;
    }

    public AbstractTuple(CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("crs", crs);
        this.type = SampleSystem.of(crs);
    }

    @Override
    public SampleSystem getSampleSystem() {
        return type;
    }

    @Override
    public int hashCode() {
        int hash = 31;
        for (int i = 0, n = getDimension(); i < n; i++) {
            hash += 31 * hash + Double.hashCode(get(i));
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
        if (!(obj instanceof Tuple)) {
            return false;
        }
        final Tuple other = (Tuple) obj;

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
        //checking crs is expensive, do it last
        if (!Utilities.equalsIgnoreMetadata(getCoordinateReferenceSystem(), other.getCoordinateReferenceSystem())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+Arrays.toString(toArrayDouble());
    }

    /**
     * Check if given type has the expected number of dimensions.
     *
     * @param typetype to verify
     * @param dimension expected dimension
     * throws IllegalArgumentException if dimension do not match
     */
    protected static void ensureDimension(SampleSystem type, int dimension) {
        if (type.getSize() != dimension) {
            throw new IllegalArgumentException("CoordinateReferenceSystem dimension must be " + dimension);
        }
    }

    /**
     * Check if given CRS has the expected number of dimensions.
     *
     * @param crs Coordinate system to verify
     * @param dimension expected dimension
     * throws IllegalArgumentException if dimension do not match
     */
    protected static void ensureDimension(CoordinateReferenceSystem crs, int dimension) {
        if (crs.getCoordinateSystem().getDimension() != dimension) {
            throw new IllegalArgumentException("CoordinateReferenceSystem dimension must be " + dimension);
        }
    }
}
