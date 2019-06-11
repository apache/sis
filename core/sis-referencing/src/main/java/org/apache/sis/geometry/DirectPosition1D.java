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

/*
 * Do not add dependency to java.awt.geom.Point2D in this class, because not all platforms
 * support Java2D (e.g. Android), or applications that do not need it may want to avoid to
 * to force installation of the Java2D module (e.g. JavaFX/SWT).
 */
import java.util.Objects;
import java.io.Serializable;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;


/**
 * A one-dimensional position within some coordinate reference system.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 *
 * @see DirectPosition2D
 * @see GeneralDirectPosition
 * @see CoordinateFormat
 *
 * @since 0.3
 * @module
 */
public class DirectPosition1D extends AbstractDirectPosition implements Serializable, Cloneable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8318842301025970006L;

    /**
     * The coordinate reference system for this position;
     */
    private CoordinateReferenceSystem crs;

    /**
     * The coordinate value.
     */
    public double coordinate;

    /**
     * Constructs a position initialized to (0) with a {@code null}
     * coordinate reference system.
     */
    public DirectPosition1D() {
    }

    /**
     * Constructs a position with the specified coordinate reference system.
     *
     * @param  crs  the coordinate reference system.
     */
    public DirectPosition1D(final CoordinateReferenceSystem crs) {
        ensureDimensionMatches("crs", 1, crs);
        this.crs = crs;
    }

    /**
     * Constructs a 1D position from the specified coordinate.
     *
     * @param coordinate  the coordinate value.
     */
    public DirectPosition1D(final double coordinate) {
        this.coordinate = coordinate;
    }

    /**
     * Constructs a position initialized to the values parsed from the given string in
     * <cite>Well Known Text</cite> (WKT) format. The given string is typically a {@code POINT}
     * element like below:
     *
     * {@preformat wkt
     *     POINT(6)
     * }
     *
     * @param  wkt  the {@code POINT} or other kind of element to parse.
     * @throws IllegalArgumentException if the given string can not be parsed.
     * @throws MismatchedDimensionException if the given point is not one-dimensional.
     *
     * @see #toString()
     * @see CoordinateFormat
     */
    public DirectPosition1D(final CharSequence wkt) throws IllegalArgumentException {
        final double[] coordinates = parse(wkt);
        if (coordinates == null) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnparsableStringForClass_2, "POINT", wkt));
        }
        ensureDimensionMatches("wkt", 1, coordinates);
        coordinate = coordinates[0];
    }

    /**
     * The length of coordinate sequence (the number of entries).
     * This is always 1 for {@code DirectPosition1D} objects.
     *
     * @return the dimensionality of this position.
     */
    @Override
    public final int getDimension() {
        return 1;
    }

    /**
     * Returns the coordinate reference system in which the coordinate is given.
     * May be {@code null} if this particular {@code DirectPosition} is included
     * in a larger object with such a reference to a CRS.
     *
     * @return the coordinate reference system, or {@code null}.
     */
    @Override
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Sets the coordinate reference system in which the coordinate is given.
     *
     * @param  crs  the new coordinate reference system, or {@code null}.
     */
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        ensureDimensionMatches("crs", 1, crs);
        this.crs = crs;
    }

    /**
     * Returns a sequence of numbers that hold the coordinate of this position in its reference system.
     *
     * <div class="note"><b>API note:</b>
     * This method is final for ensuring consistency with the {@link #coordinate} field, which is public.</div>
     *
     * @return the coordinates.
     */
    @Override
    public final double[] getCoordinate() {
        return new double[] {coordinate};
    }

    /**
     * Returns the coordinate at the specified dimension.
     *
     * <div class="note"><b>API note:</b>
     * This method is final for ensuring consistency with the {@link #coordinate} field, which is public.</div>
     *
     * @param  dimension  the dimension, which must be 0.
     * @return the {@link #coordinate}.
     * @throws IndexOutOfBoundsException if the specified dimension is out of bounds.
     */
    @Override
    public final double getOrdinate(final int dimension) throws IndexOutOfBoundsException {
        if (dimension == 0) {
            return coordinate;
        } else {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, dimension));
        }
    }

    /**
     * Sets the coordinate value along the specified dimension.
     *
     * @param  dimension  the dimension, which must be 0.
     * @param  value      the coordinate value.
     * @throws IndexOutOfBoundsException if the specified dimension is out of bounds.
     */
    @Override
    public void setOrdinate(int dimension, double value) throws IndexOutOfBoundsException {
        if (dimension == 0) {
            coordinate = value;
        } else {
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, dimension));
        }
    }

    /**
     * Sets this coordinate to the specified direct position. If the specified position
     * contains a coordinate reference system (CRS), then the CRS for this position will
     * be set to the CRS of the specified position.
     *
     * @param  position  the new position for this point.
     * @throws MismatchedDimensionException if this point doesn't have the expected dimension.
     */
    @Override
    public void setLocation(final DirectPosition position) throws MismatchedDimensionException {
        ensureDimensionMatches("position", 1, position);
        setCoordinateReferenceSystem(position.getCoordinateReferenceSystem());
        coordinate = position.getOrdinate(0);
    }

    /**
     * Formats this position in the <cite>Well Known Text</cite> (WKT) format.
     * The output is like below:
     *
     * {@preformat wkt
     *   POINT(coordinate)
     * }
     *
     * The string returned by this method can be {@linkplain #DirectPosition1D(CharSequence) parsed}
     * by the {@code DirectPosition1D} constructor.
     */
    @Override
    public String toString() {
        return toString(this, coordinate == (float) coordinate);
    }

    /**
     * Returns a copy of this position.
     *
     * @return a copy of this position.
     */
    @Override
    public DirectPosition1D clone() {
        try {
            return (DirectPosition1D) super.clone();
        } catch (CloneNotSupportedException exception) {
            // Should not happen, since we are cloneable.
            throw new AssertionError(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int code = 31 + Double.hashCode(coordinate) + Objects.hashCode(crs);
        assert code == super.hashCode();
        return code;
    }
}
