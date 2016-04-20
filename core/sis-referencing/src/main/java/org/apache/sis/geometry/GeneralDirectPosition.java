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
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A mutable {@code DirectPosition} (the coordinates of a position) of arbitrary dimension.
 * This particular implementation of {@code DirectPosition} is said "General" because it
 * uses an {@linkplain #ordinates array of ordinates} of an arbitrary length. If the direct
 * position is known to be always two-dimensional, then {@link DirectPosition2D} provides
 * a more efficient implementation.
 *
 * <div class="section">Coordinate Reference System (CRS) optionality</div>
 * Since {@code DirectPosition}s, as data types, will often be included in larger objects
 * (such as {@link org.opengis.geometry.Geometry}) that have references
 * to {@code CoordinateReferenceSystem}, the {@link #getCoordinateReferenceSystem()} method
 * may returns {@code null} if this particular {@code DirectPosition} is included in such
 * larger object. In this case, the coordinate reference system is implicitly assumed to take
 * on the value of the containing object's {@code CoordinateReferenceSystem}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see DirectPosition1D
 * @see DirectPosition2D
 */
public class GeneralDirectPosition extends AbstractDirectPosition implements Serializable, Cloneable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5524426558018300122L;

    /**
     * Used for setting the {@link #ordinates} field during a {@link #clone()} operation only.
     * Will be fetch when first needed.
     */
    private static volatile Field ordinatesField;

    /**
     * The ordinates of the direct position. The length of this array is the
     * {@linkplain #getDimension() dimension} of this direct position.
     */
    public final double[] ordinates;

    /**
     * The coordinate reference system for this position, or {@code null}.
     */
    private CoordinateReferenceSystem crs;

    /**
     * Constructs a position using the specified coordinate reference system.
     * The number of dimensions is inferred from the coordinate reference system.
     *
     * @param crs The coordinate reference system to be given to this position.
     */
    public GeneralDirectPosition(final CoordinateReferenceSystem crs) {
        this(crs.getCoordinateSystem().getDimension());
        this.crs = crs;
    }

    /**
     * Constructs a position with the specified number of dimensions.
     *
     * @param  dimension Number of dimensions.
     * @throws NegativeArraySizeException if {@code dimension} is negative.
     */
    public GeneralDirectPosition(final int dimension) throws NegativeArraySizeException {
        ordinates = new double[dimension];
    }

    /**
     * Constructs a position with the specified ordinates.
     * This constructor assigns the given array directly (without clone) to the {@link #ordinates} field.
     * Consequently, callers shall not recycle the same array for creating many instances.
     *
     * <div class="note"><b>Implementation note:</b>
     * The array is not cloned because this is usually not needed, especially in the context of variable
     * argument lengths since the array is often created implicitly. Furthermore the {@link #ordinates}
     * field is public, so cloning the array would not protect the state of this object anyway.</div>
     *
     * @param ordinates The ordinate values. This array is <strong>not</strong> cloned.
     */
    public GeneralDirectPosition(final double... ordinates) {
        this.ordinates = ordinates;
    }

    /**
     * Constructs a position initialized to the same values than the specified point.
     * This is a copy constructor.
     *
     * @param point The position to copy.
     */
    public GeneralDirectPosition(final DirectPosition point) {
        ordinates = point.getCoordinate();                              // Should already be cloned.
        crs = point.getCoordinateReferenceSystem();
        ensureDimensionMatches("crs", ordinates.length, crs);
    }

    /**
     * Constructs a position initialized to the values parsed
     * from the given string in <cite>Well Known Text</cite> (WKT) format.
     * The given string is typically a {@code POINT} element like below:
     *
     * {@preformat wkt
     *     POINT(6 10)
     * }
     *
     * However this constructor is lenient to other types like {@code POINT ZM}.
     *
     * @param  wkt The {@code POINT} or other kind of element to parse.
     * @throws IllegalArgumentException If the given string can not be parsed.
     *
     * @see #toString()
     * @see org.apache.sis.measure.CoordinateFormat
     */
    public GeneralDirectPosition(final CharSequence wkt) throws IllegalArgumentException {
        if ((ordinates = parse(wkt)) == null) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnparsableStringForClass_2, "POINT", wkt));
        }
    }

    /**
     * The length of ordinate sequence (the number of entries).
     * This is always equals to the length of the {@link #ordinates} array.
     *
     * @return The dimensionality of this position.
     */
    @Override
    public final int getDimension() {
        return ordinates.length;
    }

    /**
     * Returns the coordinate reference system in which the coordinate is given.
     * May be {@code null} if this particular {@code DirectPosition} is included
     * in a larger object with such a reference to a CRS.
     *
     * @return The coordinate reference system, or {@code null}.
     */
    @Override
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Sets the coordinate reference system in which the coordinate is given.
     *
     * @param crs The new coordinate reference system, or {@code null}.
     * @throws MismatchedDimensionException if the specified CRS doesn't have the expected
     *         number of dimensions.
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
     * This method is final for ensuring consistency with the {@link #ordinates}, array field, which is public.</div>
     *
     * @return A copy of the {@linkplain #ordinates ordinates} array.
     */
    @Override
    public final double[] getCoordinate() {
        return ordinates.clone();
    }

    /**
     * Sets the ordinate values along all dimensions.
     *
     * @param  ordinates The new ordinates values, or a {@code null} array
     *         for setting all ordinate values to {@link Double#NaN NaN}.
     * @throws MismatchedDimensionException If the length of the specified array is not
     *         equals to the {@linkplain #getDimension() dimension} of this position.
     */
    public void setCoordinate(final double... ordinates) throws MismatchedDimensionException {
        if (ordinates == null) {
            Arrays.fill(this.ordinates, Double.NaN);
        } else {
            ensureDimensionMatches("ordinates", this.ordinates.length, ordinates);
            System.arraycopy(ordinates, 0, this.ordinates, 0, ordinates.length);
        }
    }

    /**
     * Returns the ordinate at the specified dimension.
     *
     * <div class="note"><b>API note:</b>
     * This method is final for ensuring consistency with the {@link #ordinates}, array field, which is public.</div>
     *
     * @param  dimension The dimension in the range 0 to {@linkplain #getDimension() dimension}-1.
     * @return The ordinate at the specified dimension.
     * @throws IndexOutOfBoundsException if the specified dimension is out of bounds.
     */
    @Override
    public final double getOrdinate(final int dimension) throws IndexOutOfBoundsException {
        return ordinates[dimension];
    }

    /**
     * Sets the ordinate value along the specified dimension.
     *
     * @param dimension The dimension for the ordinate of interest.
     * @param value The ordinate value of interest.
     * @throws IndexOutOfBoundsException if the specified dimension is out of bounds.
     */
    @Override
    public void setOrdinate(final int dimension, final double value) throws IndexOutOfBoundsException {
        ordinates[dimension] = value;
    }

    /**
     * Sets this coordinate to the specified direct position. If the specified position
     * contains a coordinate reference system (CRS), then the CRS for this position will
     * be set to the CRS of the specified position.
     *
     * @param  position The new position for this point, or {@code null} for setting all ordinate
     *         values to {@link Double#NaN NaN}.
     * @throws MismatchedDimensionException if the given position doesn't have the expected dimension.
     */
    @Override
    public void setLocation(final DirectPosition position) throws MismatchedDimensionException {
        if (position == null) {
            Arrays.fill(ordinates, Double.NaN);
        } else {
            ensureDimensionMatches("position", ordinates.length, position);
            setCoordinateReferenceSystem(position.getCoordinateReferenceSystem());
            for (int i=0; i<ordinates.length; i++) {
                ordinates[i] = position.getOrdinate(i);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(this, isSimplePrecision(ordinates));
    }

    /**
     * Returns the {@code "ordinates"} field of the given class and gives write permission to it.
     * This method should be invoked only from {@link #clone()} method.
     */
    static Field getOrdinatesField(final Class<?> type) throws NoSuchFieldException {
        final Field field = type.getDeclaredField("ordinates");
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override public Void run() {
                field.setAccessible(true);
                return null;
            }
        });
        return field;
    }

    /**
     * Returns a deep copy of this position.
     *
     * @return A copy of this direct position.
     */
    @Override
    public GeneralDirectPosition clone() {
        try {
            Field field = ordinatesField;
            if (field == null) {
                ordinatesField = field = getOrdinatesField(GeneralDirectPosition.class);
            }
            GeneralDirectPosition e = (GeneralDirectPosition) super.clone();
            field.set(e, ordinates.clone());
            return e;
        } catch (Exception exception) { // (ReflectiveOperationException | CloneNotSupportedException) on JDK7
            // Should not happen, since we are cloneable.
            // Should not happen, since the "ordinates" field exists.
            // etc...
            throw new AssertionError(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int code = Arrays.hashCode(ordinates) + Objects.hashCode(getCoordinateReferenceSystem());
        assert code == super.hashCode();
        return code;
    }
}
