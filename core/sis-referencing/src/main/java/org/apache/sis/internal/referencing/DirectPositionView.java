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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.AbstractDirectPosition;

// Branch-dependent imports
import org.apache.sis.geometry.UnmodifiableGeometryException;


/**
 * A read-only direct position wrapping an array without performing any copy.
 * This class shall be used for temporary objects only (it is not serializable for this reason).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.5
 * @module
 */
public abstract class DirectPositionView extends AbstractDirectPosition {
    /**
     * The index of the first value in the ordinates array.
     * This field is non-final in order to allow the caller to move the view over an array of coordinates.
     */
    public int offset;

    /**
     * The number of valid ordinate values.
     */
    final int dimension;

    /**
     * Creates a new direct position wrapping the given array.
     *
     * @param  offset     the first value index in the ordinates array.
     * @param  dimension  the number of valid ordinate values.
     */
    DirectPositionView(final int offset, final int dimension) {
        this.offset    = offset;
        this.dimension = dimension;
    }

    /**
     * Returns {@code null} since there is no CRS associated with this position.
     *
     * @return {@code null}.
     */
    @Override
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return null;
    }

    /**
     * Returns the dimension given at construction time.
     *
     * @return number of dimensions.
     */
    @Override
    public final int getDimension() {
        return dimension;
    }

    /**
     * Do not allow any change.
     *
     * @param  dimension  ignored.
     * @param  value      ignored.
     */
    @Override
    public final void setOrdinate(final int dimension, final double value) {
        throw new UnmodifiableGeometryException();
    }

    /**
     * The double-precision version of {@link DirectPositionView}.
     */
    public static final class Double extends DirectPositionView {
        /**
         * The ordinate values. This is a direct reference to the array given to the constructor.
         * The length of this array may be greater then the number of dimensions.
         */
        private final double[] ordinates;

        /**
         * Creates a new direct position wrapping the given array.
         *
         * @param  ordinates  the ordinate values.
         * @param  offset     the first value index in the ordinates array.
         * @param  dimension  the number of valid ordinate values.
         */
        public Double(final double[] ordinates, final int offset, final int dimension) {
            super(offset, dimension);
            this.ordinates = ordinates;
        }

        /**
         * Returns the ordinate at the given index.
         * <strong>This implementation does not check index validity</strong>, unless assertions are enabled.
         *
         * @param  dim  the dimension of the ordinate to get fetch.
         * @return the coordinate value at the given dimension.
         */
        @Override
        public double getOrdinate(final int dim) {
            assert dim >= 0 && dim < dimension : dim;
            return ordinates[offset + dim];
        }

        /**
         * Returns all ordinate values.
         *
         * @return all coordinate values.
         */
        @Override
        public double[] getCoordinate() {
            return Arrays.copyOfRange(ordinates, offset, offset + dimension);
        }
    }

    /**
     * The single-precision version of {@link DirectPositionView}.
     */
    public static final class Float extends DirectPositionView {
        /**
         * The ordinate values. This is a direct reference to the array given to the constructor.
         * The length of this array may be greater then the number of dimensions.
         */
        private final float[] ordinates;

        /**
         * Creates a new direct position wrapping the given array.
         *
         * @param  ordinates  the ordinate values.
         * @param  offset     the first value index in the ordinates array.
         * @param  dimension  the number of valid ordinate values.
         */
        public Float(final float[] ordinates, final int offset, final int dimension) {
            super(offset, dimension);
            this.ordinates = ordinates;
        }

        /**
         * Returns the ordinate at the given index.
         * <strong>This implementation does not check index validity</strong>, unless assertions are enabled.
         *
         * @param  dim  the dimension of the ordinate to get fetch.
         * @return the coordinate value at the given dimension.
         */
        @Override
        public double getOrdinate(final int dim) {
            assert dim >= 0 && dim < dimension : dim;
            return ordinates[offset + dim];
        }
    }
}
