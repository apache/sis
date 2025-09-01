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
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.math.Vector;

import static org.apache.sis.util.StringBuilders.trimFractionalPart;
import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;


/**
 * Default implementations of some {@code DirectPosition} methods, leaving the data storage to subclasses.
 * A direct position holds the coordinates for a position within some
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS coordinate reference system}.
 * This base class provides default implementations for {@link #toString()},
 * {@link #equals(Object)} and {@link #hashCode()} methods.
 *
 * <p>This base class does not hold any state and does not implement the {@link java.io.Serializable}
 * or {@link Cloneable} interfaces. The internal representation, and the choice to be cloneable or
 * serializable, is left to subclasses.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.3
 */
public abstract class AbstractDirectPosition extends FormattableObject implements DirectPosition {
    /**
     * Constructs a direct position.
     */
    protected AbstractDirectPosition() {
    }

    /**
     * Returns the given position as an {@code AbstractDirectPosition} instance.
     * If the given position is already an instance of {@code AbstractDirectPosition},
     * then it is returned unchanged. Otherwise the coordinate values and the CRS
     * of the given position are copied in a new position.
     *
     * @param  position  the position to cast, or {@code null}.
     * @return the values of the given position as an {@code AbstractDirectPosition} instance.
     *
     * @since 1.0
     */
    public static AbstractDirectPosition castOrCopy(final DirectPosition position) {
        if (position == null || position instanceof AbstractDirectPosition) {
            return (AbstractDirectPosition) position;
        }
        return new GeneralDirectPosition(position);
    }

    /**
     * Returns this direct position.
     *
     * @return {@code this}.
     */
    @Override
    public final DirectPosition getDirectPosition() {
        return this;
    }

    /**
     * Returns the coordinate reference system in which the coordinate tuple is given.
     * May be {@code null} if this particular {@code DirectPosition} is included in a larger object
     * with such a reference to a {@linkplain CoordinateReferenceSystem coordinate reference system}.
     *
     * <p>The default implementation returns {@code null}.
     * Subclasses should override this method if the CRS can be provided.</p>
     *
     * @return the coordinate reference system, or {@code null}.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return null;
    }

    /**
     * Returns a sequence of numbers that hold the coordinate of this position in its reference system.
     *
     * @return the coordinates.
     *
     * @deprecated Renamed {@link #getCoordinates()} for consistency with ISO 19111 terminology.
     */
    @Override
    @Deprecated(since="1.5")
    public double[] getCoordinate() {
        return getCoordinates();
    }

    /**
     * Returns a sequence of numbers that hold the coordinate of this position in its reference system.
     *
     * @return the coordinates.
     */
    public double[] getCoordinates() {
        final double[] coordinates = new double[getDimension()];
        for (int i=0; i<coordinates.length; i++) {
            coordinates[i] = getCoordinate(i);
        }
        return coordinates;
    }

    /**
     * Returns the coordinate at the specified dimension.
     *
     * @param  dimension  the dimension in the range 0 to {@linkplain #getDimension dimension}-1.
     * @return the coordinate at the specified dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() number of dimensions}.
     *
     * @deprecated Renamed {@link #getCoordinate(int)} for consistency with ISO 19111 terminology.
     */
    @Deprecated(since="1.5")
    public double getOrdinate(int dimension) throws IndexOutOfBoundsException {
        return getCoordinate(dimension);
    }

    /**
     * Returns the coordinate at the specified dimension.
     *
     * @param  dimension  the dimension in the range 0 to {@linkplain #getDimension dimension}-1.
     * @return the coordinate at the specified dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() number of dimensions}.
     *
     * @since 1.5
     */
    public abstract double getCoordinate(int dimension) throws IndexOutOfBoundsException;

    /**
     * Sets the coordinate value along the specified dimension.
     *
     * @param  dimension  the dimension for the coordinate of interest.
     * @param  value      the coordinate value of interest.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() position dimension}.
     * @throws UnsupportedOperationException if this direct position is immutable.
     *
     * @deprecated Renamed {@link #setCoordinate(int, double)} for consistency with ISO 19111 terminology.
     */
    @Deprecated(since="1.5")
    public void setOrdinate(int dimension, double value)
            throws IndexOutOfBoundsException, UnsupportedOperationException
    {
        setCoordinate(dimension, value);
    }

    /**
     * Sets the coordinate value along the specified dimension.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}.
     * Subclasses need to override this method if this direct position is mutable.</p>
     *
     * @param  dimension  the dimension for the coordinate of interest.
     * @param  value      the coordinate value of interest.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() position dimension}.
     * @throws UnsupportedOperationException if this direct position is immutable.
     *
     * @since 1.5
     */
    public void setCoordinate(int dimension, double value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, getClass()));
    }

    /**
     * Sets this direct position to the given position. If the given position is
     * {@code null}, then all coordinate values are set to {@link Double#NaN NaN}.
     *
     * <p>If this position and the given position have a non-null CRS, then the default implementation
     * requires the CRS to be {@linkplain CRS#equivalent equivalent},
     * otherwise a {@code MismatchedReferenceSystemException} is thrown. However, subclass may choose
     * to assign the CRS of this position to the CRS of the given position.</p>
     *
     * @param  position  the new position, or {@code null}.
     * @throws MismatchedDimensionException if the given position doesn't have the expected dimension.
     * @throws MismatchedReferenceSystemException if the given position doesn't use the expected CRS.
     */
    public void setLocation(final DirectPosition position)
            throws MismatchedDimensionException, MismatchedReferenceSystemException
    {
        final int dimension = getDimension();
        if (position != null) {
            ensureDimensionMatches("position", dimension, position);
            final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
            if (crs != null) {
                final CoordinateReferenceSystem other = position.getCoordinateReferenceSystem();
                if (other != null && !CRS.equivalent(crs, other)) {
                    throw new MismatchedReferenceSystemException(Errors.format(Errors.Keys.MismatchedCRS));
                }
            }
            for (int i=0; i<dimension; i++) {
                setCoordinate(i, position.getOrdinate(i));
            }
        } else {
            for (int i=0; i<dimension; i++) {
                setCoordinate(i, Double.NaN);
            }
        }
    }

    /**
     * Ensures that the position is contained in the coordinate system domain.
     * For each dimension, this method compares the coordinate values against the
     * limits of the coordinate system axis for that dimension.
     * If some coordinates are out of range, then there is a choice depending on the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() axis range meaning}:
     *
     * <ul>
     *   <li>If {@link RangeMeaning#EXACT} (typically <em>latitudes</em> coordinates), then values
     *       greater than the {@linkplain CoordinateSystemAxis#getMaximumValue() axis maximal value}
     *       are replaced by the axis maximum, and values smaller than the
     *       {@linkplain CoordinateSystemAxis#getMinimumValue() axis minimal value}
     *       are replaced by the axis minimum.</li>
     *
     *   <li>If {@link RangeMeaning#WRAPAROUND} (typically <em>longitudes</em> coordinates), then
     *       a multiple of the axis range (e.g. 360° for longitudes) is added or subtracted.</li>
     * </ul>
     *
     * @return {@code true} if this position has been modified as a result of this method call,
     *         or {@code false} if no change has been done.
     *
     * @see GeneralEnvelope#normalize()
     */
    public boolean normalize() {
        boolean changed = false;
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        if (crs != null) {
            final int dimension = getDimension();
            final CoordinateSystem cs = crs.getCoordinateSystem();
            for (int i=0; i<dimension; i++) {
                double coordinate = getCoordinate(i);
                final CoordinateSystemAxis axis = cs.getAxis(i);
                final double  minimum = axis.getMinimumValue();
                final double  maximum = axis.getMaximumValue();
                final RangeMeaning rm = axis.getRangeMeaning();
                if (rm == RangeMeaning.EXACT) {
                         if (coordinate < minimum) coordinate = minimum;
                    else if (coordinate > maximum) coordinate = maximum;
                    else continue;
                } else if (rm == RangeMeaning.WRAPAROUND) {
                    final double csSpan = maximum - minimum;
                    final double shift  = Math.floor((coordinate - minimum) / csSpan) * csSpan;
                    if (shift == 0) {
                        continue;
                    }
                    coordinate -= shift;
                }
                setCoordinate(i, coordinate);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Formats this position in the <i>Well Known Text</i> (WKT) format.
     * The format is like below, where {@code x₀}, {@code x₁}, {@code x₂}, <i>etc.</i>
     * are the coordinate values at index 0, 1, 2, <i>etc.</i>:
     *
     * {@snippet lang="wkt" :
     *   POINT[x₀ x₁ x₂ …]
     *   }
     *
     * If the coordinate reference system is geodetic or projected, then coordinate values are formatted
     * with a precision equivalent to one centimetre on Earth (the actual number of fraction digits is
     * adjusted for the axis unit of measurement and the planet size if different than Earth).
     *
     * @param  formatter  the formatter where to format the inner content of this point.
     * @return the WKT keyword, which is {@code "Point"} for this element.
     *
     * @since 1.0
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final Vector[] points = {
            Vector.create(getCoordinates())
        };
        formatter.append(points, WKTUtilities.suggestFractionDigits(getCoordinateReferenceSystem(), points));
        return WKTKeywords.Point;
    }

    /**
     * Formats this position in the <i>Well Known Text</i> (WKT) format.
     * The returned string is like below, where {@code x₀}, {@code x₁}, {@code x₂}, <i>etc.</i>
     * are the coordinate values at index 0, 1, 2, <i>etc.</i>:
     *
     * {@snippet lang="wkt" :
     *   POINT(x₀ x₁ x₂ …)
     *   }
     *
     * This method formats the numbers as with {@link Double#toString(double)} (i.e. without fixed number of fraction digits).
     * The string returned by this method can be {@linkplain GeneralDirectPosition#GeneralDirectPosition(CharSequence) parsed}
     * by the {@code GeneralDirectPosition} constructor.
     *
     * @return this position as a {@code POINT} in <i>Well Known Text</i> (WKT) format.
     */
    @Override
    public String toString() {
        return toString(this, false);
    }

    /**
     * Implementation of the public {@link #toString()} and {@link DirectPosition2D#toString()} methods
     * for formatting a {@code POINT} element from a direct position in <i>Well Known Text</i>
     * (WKT) format.
     *
     * @param  position           the position to format.
     * @param  isSinglePrecision  {@code true} if every coordinate values can be cast to {@code float}.
     * @return the point as a {@code POINT} in WKT format.
     *
     * @see ArraysExt#isSinglePrecision(double[])
     */
    static String toString(final DirectPosition position, final boolean isSinglePrecision) {
        final StringBuilder buffer = new StringBuilder(32).append("POINT");
        final int dimension = position.getDimension();
        if (dimension == 0) {
            buffer.append("()");
        } else {
            char separator = '(';
            for (int i=0; i<dimension; i++) {
                buffer.append(separator);
                final double coordinate = position.getOrdinate(i);
                if (isSinglePrecision) {
                    buffer.append((float) coordinate);
                } else {
                    buffer.append(coordinate);
                }
                trimFractionalPart(buffer);
                separator = ' ';
            }
            buffer.append(')');
        }
        return buffer.toString();
    }

    /**
     * Parses the given WKT.
     *
     * @param  wkt  the WKT to parse.
     * @return the coordinates, or {@code null} if none.
     * @throws NumberFormatException if a number cannot be parsed.
     * @throws IllegalArgumentException if the parenthesis are not balanced.
     */
    static double[] parse(final CharSequence wkt) throws NumberFormatException, IllegalArgumentException {
        /*
         * Ignore leading and trailing whitespaces, including line feeds. After this step,
         * line feeds in the middle of a POINT element are considered errors.
         */
        int length = CharSequences.skipTrailingWhitespaces(wkt, 0, wkt.length());
        int i = CharSequences.skipLeadingWhitespaces(wkt, 0, length);
        int c;
        /*
         * Skip the leading identifier (typically "POINT" or "POINT ZM") and the following
         * whitespaces, if any. If we reach the end of string before to find a character which
         * is neither a space character or an identifier part, then return null.
         */
        while (true) {
            if (i >= length) return null;
            c = Character.codePointAt(wkt, i);
            if (Character.isUnicodeIdentifierStart(c)) {
                do {
                    i += Character.charCount(c);
                    if (i >= length) return null;
                    c = Character.codePointAt(wkt, i);
                } while (Character.isUnicodeIdentifierPart(c));
            }
            if (!Character.isSpaceChar(c)) break;
            i += Character.charCount(c);
        }
        /*
         * Skip the opening parenthesis, and the following whitespaces if any.
         * If we find an opening parenthesis, search for the closing parenthesis.
         */
        if (c == '(' || c == '[') {
            i += Character.charCount(c);
            i = CharSequences.skipLeadingWhitespaces(wkt, i, length);
            final char close = (c == '(') ? ')' : ']';
            final int pos = CharSequences.lastIndexOf(wkt, close, i, length);
            if (pos != --length) {
                final short key;
                final Object[] args;
                if (pos < 0) {
                    key  = Errors.Keys.NonEquilibratedParenthesis_2;
                    args = new Object[] {wkt, close};
                } else {
                    key  = Errors.Keys.UnparsableStringForClass_3;
                    args = new Object[] {"POINT", wkt, CharSequences.trimWhitespaces(wkt, pos+1, length+1)};
                }
                throw new IllegalArgumentException(Errors.format(key, args));
            }
            c = Character.codePointAt(wkt, i);
        }
        /*
         * Index i is either at the beginning of a number or at the closing parenthesis.
         * Now process every space-separated coordinates until we reach the closing parenthesis
         * or the end of string.
         */
        double[] coordinates = new double[2];
        int dimension = 0;
parse:  while (i < length) {
            final int start = i;
            do {
                i += Character.charCount(c);
                if (i >= length) {
                    c = 0;
                    break;
                }
                c = Character.codePointAt(wkt, i);
            } while (!Character.isSpaceChar(c));
            /*
             * Parsing the number may throw a NumberFormatException. But the latter is an
             * IllegalArgumentException subclass, so we are compliant with the contract.
             */
            final double value = Double.parseDouble(wkt.subSequence(start, i).toString());
            if (dimension == coordinates.length) {
                coordinates = Arrays.copyOf(coordinates, dimension*2);
            }
            coordinates[dimension++] = value;
            /*
             * Skip whitespaces. If we reach the end of string without finding
             * the closing parenthesis, check if we were suppose to have any.
             */
            while (Character.isSpaceChar(c)) {
                i += Character.charCount(c);
                if (i >= length) {
                    break parse;
                }
                c = Character.codePointAt(wkt, i);
            }
        }
        return ArraysExt.resize(coordinates, dimension);
    }

    /**
     * Returns a hash value for this coordinate tuple. This method returns a value compliant
     * with the contract documented in the {@link DirectPosition#hashCode()} javadoc.
     * Consequently, it should be possible to mix different {@code DirectPosition}
     * implementations in the same hash map.
     *
     * @return a hash code value for this position.
     */
    @Override
    public int hashCode() {
        final int dimension = getDimension();
        int code = 1;
        for (int i=0; i<dimension; i++) {
            code = code*31 + Double.hashCode(getCoordinate(i));
        }
        return code + Objects.hashCode(getCoordinateReferenceSystem());
    }

    /**
     * Returns {@code true} if the specified object is also a {@code DirectPosition}
     * with equal coordinates and equal CRS.
     *
     * This method performs the comparison as documented in the {@link DirectPosition#equals(Object)}
     * javadoc. In particular, the given object is not required to be of the same implementation class.
     * Consequently, it should be possible to mix different {@code DirectPosition} implementations in
     * the same hash map.
     *
     * @param  object  the object to compare with this position.
     * @return {@code true} if the given object is equal to this position.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof DirectPosition) {
            final DirectPosition that = (DirectPosition) object;
            final int dimension = getDimension();
            if (dimension == that.getDimension()) {
                for (int i=0; i<dimension; i++) {
                    if (!Numerics.equals(getCoordinate(i), that.getOrdinate(i))) {
                        return false;
                    }
                }
                if (Objects.equals(this.getCoordinateReferenceSystem(),
                                   that.getCoordinateReferenceSystem()))
                {
                    assert hashCode() == that.hashCode() : this;
                    return true;
                }
            }
        }
        return false;
    }
}
