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
package org.apache.sis.geometry;

import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * An immutable {@code DirectPosition} (the coordinates of a position) of arbitrary dimension.
 * This final class is immutable and thus inherently thread-safe if the {@link CoordinateReferenceSystem}
 * instance given to the constructor is immutable. This is usually the case in Apache <abbr>SIS</abbr>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since SIS 1.6
 */
public final class ImmutableDirectPosition extends AbstractDirectPosition implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4275832076346637274L;

    /**
     * The coordinate reference system, or {@code null}.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final CoordinateReferenceSystem crs;

    /**
     * The coordinates of the direct position. The length of this array is
     * the {@linkplain #getDimension() dimension} of this direct position.
     */
    private final double[] coordinates;

    /**
     * Constructs a position defined by a sequence of coordinate values.
     *
     * @param  crs          the <abbr>CRS</abbr> to assign to this direct position, or {@code null}.
     * @param  coordinates  the coordinate values for each dimension.
     * @throws MismatchedDimensionException if the CRS dimension is not equal to the number of coordinates.
     */
    public ImmutableDirectPosition(final CoordinateReferenceSystem crs, final double... coordinates)
            throws MismatchedDimensionException
    {
        this.crs = crs;
        this.coordinates = coordinates.clone();
        ArgumentChecks.ensureDimensionMatches("crs", coordinates.length, crs);
    }

    /**
     * Returns the given position as an {@code ImmutableDirectPosition} instance.
     * If the given position is already an instance of {@code ImmutableDirectPosition},
     * then it is returned unchanged. Otherwise, the coordinate values and the <abbr>CRS</abbr>
     * of the given position are copied in a new position.
     *
     * @param  position  the position to cast or copy, or {@code null}.
     * @return the values of the given position as an {@code ImmutableDirectPosition} instance.
     */
    public static ImmutableDirectPosition castOrCopy(final DirectPosition position) {
        if (position == null || position instanceof ImmutableDirectPosition) {
            return (ImmutableDirectPosition) position;
        }
        return new ImmutableDirectPosition(position.getCoordinateReferenceSystem(), position.getCoordinates());
    }

    /**
     * The length of coordinate sequence (the number of entries).
     *
     * @return the dimensionality of this position.
     */
    @Override
    public int getDimension() {
        return coordinates.length;
    }

    /**
     * Returns the coordinate reference system in which the coordinates are given.
     * May be {@code null} if this particular {@code DirectPosition} is included
     * in a larger object with such a reference to a <abbr>CRS</abbr>.
     *
     * @return the coordinate reference system, or {@code null}.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Returns a sequence of numbers that hold the coordinates of this position in its reference system.
     *
     * @return a copy of the coordinates array.
     */
    @Override
    public double[] getCoordinates() {
        return coordinates.clone();
    }

    /**
     * Returns the coordinate at the specified dimension.
     *
     * @param  dimension  the dimension in the range 0 to {@linkplain #getDimension() dimension}-1.
     * @return the coordinate at the specified dimension.
     * @throws IndexOutOfBoundsException if the specified dimension is out of bounds.
     */
    @Override
    public double getCoordinate(final int dimension) throws IndexOutOfBoundsException {
        return coordinates[dimension];
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public String toString() {
        return toString(this, ArraysExt.isSinglePrecision(coordinates));
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(coordinates) + Objects.hashCode(crs);
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ImmutableDirectPosition) {
            final var that = (ImmutableDirectPosition) object;
            return Arrays.equals(coordinates, that.coordinates) && Objects.equals(crs, that.crs);
        }
        return super.equals(object);        // Comparison of other implementation classes.
    }
}
