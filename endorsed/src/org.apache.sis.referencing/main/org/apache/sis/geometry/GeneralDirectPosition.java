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
import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import java.lang.reflect.Field;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArraysExt;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;


/**
 * A mutable {@code DirectPosition} (the coordinates of a position) of arbitrary dimension.
 * This particular implementation of {@code DirectPosition} is said "General" because it
 * uses an {@link #coordinates array of coordinates} of an arbitrary length. If the direct
 * position is known to be always two-dimensional, then {@link DirectPosition2D} provides
 * a more efficient implementation.
 *
 * <h2>Coordinate Reference System (CRS) optionality</h2>
 * Since {@code DirectPosition}s, as data types, will often be included in larger objects
 * (such as {@link org.opengis.geometry.Geometry}) that have references
 * to {@code CoordinateReferenceSystem}, the {@link #getCoordinateReferenceSystem()} method
 * may returns {@code null} if this particular {@code DirectPosition} is included in such
 * larger object. In this case, the coordinate reference system is implicitly assumed to take
 * on the value of the containing object's {@code CoordinateReferenceSystem}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @see DirectPosition1D
 * @see DirectPosition2D
 * @see CoordinateFormat
 *
 * @since 0.3
 */
public class GeneralDirectPosition extends AbstractDirectPosition implements Serializable, Cloneable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1775358214919832302L;

    /**
     * Used for setting the {@link #coordinates} field during a {@link #clone()} operation only.
     * Will be fetch when first needed.
     */
    private static volatile Field coordinatesField;

    /**
     * The coordinates of the direct position. The length of this array is the
     * {@linkplain #getDimension() dimension} of this direct position.
     */
    public final double[] coordinates;

    /**
     * The coordinate reference system for this position, or {@code null}.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private CoordinateReferenceSystem crs;

    /**
     * Constructs a position using the specified coordinate reference system.
     * The number of dimensions is inferred from the coordinate reference system.
     * All coordinate values are initialized to zero.
     *
     * @param  crs  the coordinate reference system to be given to this position.
     */
    public GeneralDirectPosition(final CoordinateReferenceSystem crs) {
        this(crs.getCoordinateSystem().getDimension());
        this.crs = crs;
    }

    /**
     * Constructs a position with the specified number of dimensions.
     * All coordinate values are initialized to zero.
     *
     * @param  dimension  number of dimensions.
     * @throws NegativeArraySizeException if {@code dimension} is negative.
     */
    public GeneralDirectPosition(final int dimension) throws NegativeArraySizeException {
        coordinates = new double[dimension];
    }

    /**
     * Constructs a position with the specified coordinates.
     * This constructor assigns the given array directly (without clone) to the {@link #coordinates} field.
     * Consequently, callers shall not recycle the same array for creating many instances.
     *
     * <h4>Implementation notes</h4>
     * The array is not cloned because this is usually not needed, especially in the context of variable
     * argument lengths since the array is often created implicitly. Furthermore, the {@link #coordinates}
     * field is public, so cloning the array would not protect the state of this object anyway.
     *
     * <p><b>Caution:</b> if only one number is specified, make sure that the number type is {@code double},
     * {@code float} or {@code long} otherwise the {@link #GeneralDirectPosition(int)} constructor would be
     * invoked with a very different meaning. For example, for creating a one-dimensional coordinate initialized
     * to the coordinate value 100, use <code>new GeneralDirectPosition(100<u>.0</u>)</code>, <strong>not</strong>
     * {@code new GeneralDirectPosition(100)}, because the latter would actually create a position with 100 dimensions.</p>
     *
     * @param coordinates  the coordinate values. This array is <strong>not</strong> cloned.
     */
    public GeneralDirectPosition(final double... coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Constructs a position initialized to the same values as the specified point.
     * This is a copy constructor.
     *
     * @param point  the position to copy.
     */
    public GeneralDirectPosition(final DirectPosition point) {
        coordinates = point.getCoordinate();                            // Should already be cloned.
        crs = point.getCoordinateReferenceSystem();
        ensureDimensionMatches("crs", coordinates.length, crs);
    }

    /**
     * Constructs a position initialized to the values parsed
     * from the given string in <i>Well Known Text</i> (WKT) format.
     * The given string is typically a {@code POINT} element like below:
     *
     * {@snippet lang="wkt" :
     *   POINT(6 10)
     *   }
     *
     * However, this constructor is lenient to other types like {@code POINT ZM}.
     *
     * @param  wkt  the {@code POINT} or other kind of element to parse.
     * @throws IllegalArgumentException if the given string cannot be parsed.
     *
     * @see #toString()
     * @see CoordinateFormat
     */
    public GeneralDirectPosition(final CharSequence wkt) throws IllegalArgumentException {
        if ((coordinates = parse(wkt)) == null) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnparsableStringForClass_2, "POINT", wkt));
        }
    }

    /**
     * The length of coordinate sequence (the number of entries).
     * This is always equals to the length of the {@link #coordinates} array.
     *
     * @return the dimensionality of this position.
     */
    @Override
    public final int getDimension() {
        return coordinates.length;
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
     * @throws MismatchedDimensionException if the specified CRS does not have the expected number of dimensions.
     */
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs)
            throws MismatchedDimensionException
    {
        ensureDimensionMatches("crs", getDimension(), crs);
        this.crs = crs;
    }

    /**
     * Returns a sequence of numbers that hold the coordinate of this position in its reference system.
     *
     * <div class="note"><b>API note:</b>
     * This method is final for ensuring consistency with the {@link #coordinates}, array field, which is public.</div>
     *
     * @return a copy of the {@link #coordinates coordinates} array.
     *
     * @since 1.5
     */
    @Override
    public final double[] getCoordinates() {
        return coordinates.clone();
    }

    /**
     * Sets the coordinate values along all dimensions.
     *
     * @param  coordinates  the new coordinates values, or a {@code null} array for
     *                      setting all coordinate values to {@link Double#NaN NaN}.
     * @throws MismatchedDimensionException if the length of the specified array is not
     *         equals to the {@linkplain #getDimension() dimension} of this position.
     *
     * @since 1.5
     */
    public void setCoordinates(final double... coordinates) throws MismatchedDimensionException {
        if (coordinates == null) {
            Arrays.fill(this.coordinates, Double.NaN);
        } else {
            ensureDimensionMatches("coordinates", this.coordinates.length, coordinates);
            System.arraycopy(coordinates, 0, this.coordinates, 0, coordinates.length);
        }
    }

    /**
     * Returns the coordinate at the specified dimension.
     *
     * <div class="note"><b>API note:</b>
     * This method is final for ensuring consistency with the {@link #coordinates}, array field, which is public.</div>
     *
     * @param  dimension  the dimension in the range 0 to {@linkplain #getDimension() dimension}-1.
     * @return the coordinate at the specified dimension.
     * @throws IndexOutOfBoundsException if the specified dimension is out of bounds.
     *
     * @since 1.5
     */
    @Override
    public final double getCoordinate(final int dimension) throws IndexOutOfBoundsException {
        return coordinates[dimension];
    }

    /**
     * Sets the coordinate value along the specified dimension.
     *
     * @param  dimension  the dimension for the coordinate of interest.
     * @param  value      the coordinate value of interest.
     * @throws IndexOutOfBoundsException if the specified dimension is out of bounds.
     *
     * @since 1.5
     */
    @Override
    public void setCoordinate(final int dimension, final double value) throws IndexOutOfBoundsException {
        coordinates[dimension] = value;
    }

    /**
     * Sets this coordinate to the specified direct position. If the specified position
     * contains a coordinate reference system (CRS), then the CRS for this position will
     * be set to the CRS of the specified position.
     *
     * @param  position  the new position for this point,
     *                   or {@code null} for setting all coordinate values to {@link Double#NaN NaN}.
     * @throws MismatchedDimensionException if the given position does not have the expected dimension.
     */
    @Override
    public void setLocation(final DirectPosition position) throws MismatchedDimensionException {
        if (position == null) {
            Arrays.fill(coordinates, Double.NaN);
        } else {
            ensureDimensionMatches("position", coordinates.length, position);
            setCoordinateReferenceSystem(position.getCoordinateReferenceSystem());
            for (int i=0; i<coordinates.length; i++) {
                coordinates[i] = position.getOrdinate(i);
            }
        }
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public String toString() {
        return toString(this, ArraysExt.isSinglePrecision(coordinates));
    }

    /**
     * Returns the {@code "coordinates"} field of the given class and gives write permission to it.
     * This method should be invoked only from {@link #clone()} method.
     */
    static Field getCoordinatesField(final Class<?> type) throws NoSuchFieldException {
        final Field field = type.getDeclaredField("coordinates");
        field.setAccessible(true);
        return field;
    }

    /**
     * Returns a deep copy of this position.
     *
     * @return a copy of this direct position.
     */
    @Override
    public GeneralDirectPosition clone() {
        try {
            Field field = coordinatesField;
            if (field == null) {
                coordinatesField = field = getCoordinatesField(GeneralDirectPosition.class);
            }
            GeneralDirectPosition e = (GeneralDirectPosition) super.clone();
            field.set(e, coordinates.clone());
            return e;
        } catch (ReflectiveOperationException | CloneNotSupportedException exception) {
            /*
             * Should not happen, since we are cloneable.
             * Should not happen, since the "coordinates" field exists.
             * etc…
             */
            throw new AssertionError(exception);
        }
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public int hashCode() {
        final int code = Arrays.hashCode(coordinates) + Objects.hashCode(getCoordinateReferenceSystem());
        assert code == super.hashCode();
        return code;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof GeneralDirectPosition) {
            final GeneralDirectPosition that = (GeneralDirectPosition) object;
            return Arrays.equals(coordinates, that.coordinates) && Objects.equals(crs, that.crs);
        }
        return super.equals(object);                // Comparison of other implementation classes.
    }
}
