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
package org.apache.sis.internal.referencing;

import java.util.Arrays;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Branch-dependent imports
import org.apache.sis.geometry.UnmodifiableGeometryException;


/**
 * A read-only direct position wrapping an array without performing any copy.
 * This class shall be used for temporary objects only (it is not serializable for this reason).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class DirectPositionView implements DirectPosition {
    /**
     * The ordinate values. This is a direct reference to the array given to the constructor.
     * The length of this array may be greater then the number of dimensions.
     */
    private final double[] ordinates;

    /**
     * The index of the first value in the {@linkplain #ordinates} array.
     * This field is non-final in order to allow the caller to move the view over an array of coordinates.
     */
    public int offset;

    /**
     * The number of valid ordinate values.
     */
    private final int dimension;

    /**
     * Creates a new direct position wrapping the given array.
     *
     * @param ordinates The ordinate values.
     * @param offset    The first value index in the ordinates array.
     * @param dimension The number of valid ordinate values.
     */
    public DirectPositionView(final double[] ordinates, final int offset, final int dimension) {
        this.ordinates = ordinates;
        this.offset    = offset;
        this.dimension = dimension;
    }

    /**
     * Returns {@code null} since there is no CRS associated with this position.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return null;
    }

    /**
     * Returns the dimension given at construction time.
     */
    @Override
    public int getDimension() {
        return dimension;
    }

    /**
     * Returns all ordinate values.
     */
    @Override
    public double[] getCoordinate() {
        return Arrays.copyOfRange(ordinates, offset, offset + dimension);
    }

    /**
     * Returns the ordinate at the given index.
     * <strong>This implementation does not check index validity</strong>, unless assertions are enabled.
     *
     * @param dim The dimension of the ordinate to get fetch.
     */
    @Override
    public double getOrdinate(final int dim) {
        assert dim >= 0 && dim < dimension : dim;
        return ordinates[offset + dim];
    }

    /**
     * Do not allow any change.
     */
    @Override
    public void setOrdinate(final int dimension, final double value) {
        throw new UnmodifiableGeometryException();
    }

    /**
     * Returns the direct position, which is {@code this}.
     */
    @Override
    public DirectPosition getDirectPosition() {
        return this;
    }
}
