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
package org.apache.sis.referencing.internal.shared;

import java.util.Arrays;
import org.apache.sis.geometry.AbstractDirectPosition;


/**
 * A read-only direct position wrapping an array without performing any copy.
 * This class shall be used for temporary objects only (it is not serializable for this reason).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class DirectPositionView extends AbstractDirectPosition {
    /**
     * The index of the first value in the coordinates array.
     * This field is non-final in order to allow the caller to move the view over an array of coordinates.
     */
    public int offset;

    /**
     * The number of valid coordinate values.
     */
    final int dimension;

    /**
     * Creates a new direct position wrapping the given array.
     *
     * @param  offset     the first value index in the coordinates array.
     * @param  dimension  the number of valid coordinate values.
     */
    DirectPositionView(final int offset, final int dimension) {
        this.offset    = offset;
        this.dimension = dimension;
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
     * The double-precision version of {@link DirectPositionView}.
     */
    public static final class Double extends DirectPositionView {
        /**
         * The coordinate values. This is a direct reference to the array given to the constructor.
         * The length of this array may be greater then the number of dimensions.
         */
        private final double[] coordinates;

        /**
         * Creates a new direct position wrapping the given array.
         *
         * @param  coordinates  the coordinate values.
         */
        public Double(final double[] coordinates) {
            super(0, coordinates.length);
            this.coordinates = coordinates;
        }

        /**
         * Creates a new direct position wrapping the given array.
         *
         * @param  coordinates  the coordinate values.
         * @param  offset     the first value index in the coordinates array.
         * @param  dimension  the number of valid coordinate values.
         */
        public Double(final double[] coordinates, final int offset, final int dimension) {
            super(offset, dimension);
            this.coordinates = coordinates;
        }

        /**
         * Returns the coordinate at the given index.
         * <strong>This implementation does not check index validity</strong>, unless assertions are enabled.
         *
         * @param  dim  the dimension of the coordinate to get fetch.
         * @return the coordinate value at the given dimension.
         */
        @Override
        public double getCoordinate(final int dim) {
            assert dim >= 0 && dim < dimension : dim;
            return coordinates[offset + dim];
        }

        /**
         * Returns all coordinate values.
         *
         * @return all coordinate values.
         */
        @Override
        public double[] getCoordinates() {
            return Arrays.copyOfRange(coordinates, offset, offset + dimension);
        }
    }

    /**
     * The single-precision version of {@link DirectPositionView}.
     */
    public static final class Float extends DirectPositionView {
        /**
         * The coordinate values. This is a direct reference to the array given to the constructor.
         * The length of this array may be greater then the number of dimensions.
         */
        private final float[] coordinates;

        /**
         * Creates a new direct position wrapping the given array.
         *
         * @param  coordinates  the coordinate values.
         * @param  offset     the first value index in the coordinates array.
         * @param  dimension  the number of valid coordinate values.
         */
        public Float(final float[] coordinates, final int offset, final int dimension) {
            super(offset, dimension);
            this.coordinates = coordinates;
        }

        /**
         * Returns the coordinate at the given index.
         * <strong>This implementation does not check index validity</strong>, unless assertions are enabled.
         *
         * @param  dim  the dimension of the coordinate to get fetch.
         * @return the coordinate value at the given dimension.
         */
        @Override
        public double getCoordinate(final int dim) {
            assert dim >= 0 && dim < dimension : dim;
            return coordinates[offset + dim];
        }
    }
}
