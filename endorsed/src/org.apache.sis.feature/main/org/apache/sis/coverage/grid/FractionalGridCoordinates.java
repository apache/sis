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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.StringBuilders;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.grid.GridCoordinates;


/**
 * Grid coordinates which may have fraction digits after the integer part.
 * Grid coordinates specify the location of a cell within a {@link GridCoverage}.
 * They are normally integer numbers, but fractional parts may exist for example
 * after converting a geospatial {@link DirectPosition} to grid coordinates.
 * Preserving that fractional part is needed for interpolations.
 * This class can store such fractional part and can also compute a {@link GridExtent}
 * containing the coordinates, which can be used for requesting data for interpolations.
 *
 * <p>Current implementation stores coordinate values as {@code double} precision floating-point numbers
 * and {@linkplain Math#round(double) rounds} them to 64-bits integers on the fly. If a {@code double}
 * cannot be {@linkplain #getCoordinateValue(int) returned} as a {@code long}, or if a {@code long}
 * cannot be {@linkplain #setCoordinateValue(int, long) stored} as a {@code double}, then an
 * {@link ArithmeticException} is thrown.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 *
 * @see GridCoverage.Evaluator#toGridCoordinates(DirectPosition)
 *
 * @since 1.1
 */
public class FractionalGridCoordinates implements GridCoordinates, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5652265407347129550L;

    /**
     * The grid coordinates as floating-point numbers.
     */
    private final double[] coordinates;

    /**
     * Creates a new grid coordinates with the given number of dimensions.
     *
     * <h4>Usage note</h4>
     * {@code FractionalGridCoordinates} are usually not created directly, but are instead obtained
     * indirectly for example from the {@linkplain GridCoverage.Evaluator#toGridCoordinates(DirectPosition)
     * conversion of a geospatial position}.
     *
     * @param  dimension  the number of dimensions.
     */
    public FractionalGridCoordinates(final int dimension) {
        coordinates = new double[dimension];
    }

    /**
     * Creates a new grid coordinates with the given coordinates.
     * The array length is the number of dimensions.
     *
     * @param  coordinates  the grid coordinates.
     */
    FractionalGridCoordinates(final double[] coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Creates a new grid coordinates initialized to a copy of the given coordinates.
     *
     * @param  other  the coordinates to copy.
     */
    public FractionalGridCoordinates(final FractionalGridCoordinates other) {
        coordinates = other.coordinates.clone();
    }

    /**
     * Returns the number of dimension of this grid coordinates.
     *
     * @return  the number of dimensions.
     */
    @Override
    public int getDimension() {
        return coordinates.length;
    }

    /**
     * Returns one integer value for each dimension of the grid.
     * The default implementation invokes {@link #getCoordinateValue(int)}
     * for each element in the returned array.
     *
     * @return a copy of the coordinates. Changes in the returned array will
     *         not be reflected back in this {@code GridCoordinates} object.
     * @throws ArithmeticException if a coordinate value is outside the range
     *         of values representable as a 64-bits integer value.
     */
    @Override
    public long[] getCoordinateValues() {
        final long[] indices = new long[coordinates.length];
        for (int i=0; i<indices.length; i++) {
            indices[i] = getCoordinateValue(i);
        }
        return indices;
    }

    /**
     * Returns the grid coordinate value at the specified dimension.
     * Floating-point values are rounded to the nearest 64-bits integer values.
     * If the coordinate value is NaN or outside the range of {@code long} values,
     * then an {@link ArithmeticException} is thrown.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the coordinate value at the given dimension,
     *         {@linkplain Math#round(double) rounded} to nearest integer.
     * @throws IndexOutOfBoundsException if the given index is negative or is
     *         equal or greater than the {@linkplain #getDimension grid dimension}.
     * @throws ArithmeticException if the coordinate value is outside the range
     *         of values representable as a 64-bits integer value.
     */
    @Override
    public long getCoordinateValue(final int dimension) {
        final double value = coordinates[dimension];
        if (value >= ValuesAtPointIterator.DOMAIN_MINIMUM && value <= ValuesAtPointIterator.DOMAIN_MAXIMUM) {
            return Math.round(value);
        }
        throw new ArithmeticException(Resources.format(Resources.Keys.UnconvertibleGridCoordinate_2, "long", value));
    }

    /**
     * Returns a grid coordinate value together with its fractional part, if any.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the coordinate value at the given dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is
     *         equal or greater than the {@linkplain #getDimension grid dimension}.
     */
    public double getCoordinateFractional(final int dimension) {
        return coordinates[dimension];
    }

    /**
     * Sets the coordinate value at the specified dimension.
     * The given value shall be convertible to {@code double} without precision lost.
     *
     * @param  dimension  the dimension for which to set the coordinate value.
     * @param  value      the new value.
     * @throws IndexOutOfBoundsException if the given index is negative or is
     *         equal or greater than the {@linkplain #getDimension grid dimension}.
     * @throws ArithmeticException if this method cannot store the given grid coordinate
     *         without precision lost.
     */
    @Override
    public void setCoordinateValue(final int dimension, final long value) {
        if ((coordinates[dimension] = value) != value) {
            throw new ArithmeticException(Resources.format(Resources.Keys.UnconvertibleGridCoordinate_2, "double", value));
        }
    }

    /**
     * Returns the grid coordinates converted to a geospatial position using the given transform.
     * This is the reverse of {@link GridCoverage.Evaluator#toGridCoordinates(DirectPosition)}.
     * The {@code gridToCRS} argument is typically {@link GridGeometry#getGridToCRS(PixelInCell)}
     * with {@link PixelInCell#CELL_CENTER}.
     *
     * @param  gridToCRS  the transform to apply on grid coordinates.
     * @return the grid coordinates converted using the given transform.
     * @throws TransformException if the grid coordinates cannot be converted by {@code gridToCRS}.
     *
     * @see GridCoverage.Evaluator#toGridCoordinates(DirectPosition)
     */
    public DirectPosition toPosition(final MathTransform gridToCRS) throws TransformException {
        final var position = new GeneralDirectPosition(gridToCRS.getTargetDimensions());
        gridToCRS.transform(coordinates, 0, position.coordinates, 0, 1);
        return position;
    }

    /**
     * Returns a string representation of this grid coordinates for debugging purposes.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder("GridCoordinates[");
        writeCoordinates(buffer);
        return buffer.append(']').toString();
    }

    /**
     * Writes coordinates in the given buffer.
     */
    private void writeCoordinates(final StringBuilder buffer) {
        for (int i=0; i<coordinates.length; i++) {
            if (i != 0) buffer.append(' ');
            StringBuilders.trimFractionalPart(buffer.append(coordinates[i]));
        }
    }

    /**
     * Returns a hash code value for this grid coordinates.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(coordinates) ^ (int) serialVersionUID;
    }

    /**
     * Compares this grid coordinates with the specified object for equality.
     *
     * @param  object  the object to compares with this grid coordinates.
     * @return {@code true} if the given object is equal to this grid coordinates.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {                           // Slight optimization.
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            return Arrays.equals(((FractionalGridCoordinates) object).coordinates, coordinates);
        }
        return false;
    }
}
