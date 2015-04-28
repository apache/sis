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
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;

import static java.lang.Double.doubleToLongBits;
import static org.apache.sis.util.StringBuilders.trimFractionalPart;
import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Base class for {@link DirectPosition} implementations.
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
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class AbstractDirectPosition implements DirectPosition {
    /**
     * Constructs a direct position.
     */
    protected AbstractDirectPosition() {
    }

    /**
     * Returns always {@code this}, the direct position for this
     * {@linkplain org.opengis.geometry.coordinate.Position position}.
     *
     * @return {@code this}.
     */
    @Override
    public final DirectPosition getDirectPosition() {
        return this;
    }

    /**
     * Returns a sequence of numbers that hold the coordinate of this position in its
     * reference system.
     *
     * @return The coordinates.
     */
    @Override
    public double[] getCoordinate() {
        final double[] ordinates = new double[getDimension()];
        for (int i=0; i<ordinates.length; i++) {
            ordinates[i] = getOrdinate(i);
        }
        return ordinates;
    }

    /**
     * Sets this direct position to the given position. If the given position is
     * {@code null}, then all ordinate values are set to {@link Double#NaN NaN}.
     *
     * <p>If this position and the given position have a non-null CRS, then the default implementation
     * requires the CRS to be {@linkplain Utilities#equalsIgnoreMetadata equals (ignoring metadata)},
     * otherwise a {@code MismatchedReferenceSystemException} is thrown. However subclass may choose
     * to assign the CRS of this position to the CRS of the given position.</p>
     *
     * @param  position The new position, or {@code null}.
     * @throws MismatchedDimensionException If the given position doesn't have the expected dimension.
     * @throws MismatchedReferenceSystemException If the given position doesn't use the expected CRS.
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
                if (other != null && !Utilities.equalsIgnoreMetadata(crs, other)) {
                    throw new MismatchedReferenceSystemException(Errors.format(Errors.Keys.MismatchedCRS));
                }
            }
            for (int i=0; i<dimension; i++) {
                setOrdinate(i, position.getOrdinate(i));
            }
        } else {
            for (int i=0; i<dimension; i++) {
                setOrdinate(i, Double.NaN);
            }
        }
    }

    /**
     * Ensures that the position is contained in the coordinate system domain.
     * For each dimension, this method compares the ordinate values against the
     * limits of the coordinate system axis for that dimension.
     * If some ordinates are out of range, then there is a choice depending on the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() axis range meaning}:
     *
     * <ul>
     *   <li>If {@link RangeMeaning#EXACT} (typically <em>latitudes</em> ordinates), then values
     *       greater than the {@linkplain CoordinateSystemAxis#getMaximumValue() axis maximal value}
     *       are replaced by the axis maximum, and values smaller than the
     *       {@linkplain CoordinateSystemAxis#getMinimumValue() axis minimal value}
     *       are replaced by the axis minimum.</li>
     *
     *   <li>If {@link RangeMeaning#WRAPAROUND} (typically <em>longitudes</em> ordinates), then
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
                double ordinate = getOrdinate(i);
                final CoordinateSystemAxis axis = cs.getAxis(i);
                final double  minimum = axis.getMinimumValue();
                final double  maximum = axis.getMaximumValue();
                final RangeMeaning rm = axis.getRangeMeaning();
                if (RangeMeaning.EXACT.equals(rm)) {
                         if (ordinate < minimum) ordinate = minimum;
                    else if (ordinate > maximum) ordinate = maximum;
                    else continue;
                } else if (RangeMeaning.WRAPAROUND.equals(rm)) {
                    final double csSpan = maximum - minimum;
                    final double shift  = Math.floor((ordinate - minimum) / csSpan) * csSpan;
                    if (shift == 0) {
                        continue;
                    }
                    ordinate -= shift;
                }
                setOrdinate(i, ordinate);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Returns {@code true} if every values in the given {@code double} array could be casted
     * to the {@code float} type without precision lost. This method treats all {@code NaN} values
     * as equal.
     *
     * @param  values The value to test for their precision.
     * @return {@code true} if every values can be casted to the {@code float} type without precision lost.
     *
     * @see #toString(DirectPosition, boolean)
     */
    static boolean isSimplePrecision(final double... values) {
        for (final double value : values) {
            if (Double.doubleToLongBits(value) != Double.doubleToLongBits((float) value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Formats this position in the <cite>Well Known Text</cite> (WKT) format.
     * The returned string is like below, where {@code x₀}, {@code x₁}, {@code x₂}, <i>etc.</i>
     * are the ordinate values at index 0, 1, 2, <i>etc.</i>:
     *
     * {@preformat wkt
     *   POINT(x₀ x₁ x₂ …)
     * }
     *
     * The string returned by this method can be {@linkplain GeneralDirectPosition#GeneralDirectPosition(CharSequence) parsed}
     * by the {@code GeneralDirectPosition} constructor.
     *
     * @return This position as a {@code POINT} in <cite>Well Known Text</cite> (WKT) format.
     */
    @Override
    public String toString() {
        return toString(this, false);
    }

    /**
     * Implementation of the public {@link #toString()} and {@link DirectPosition2D#toString()} methods
     * for formatting a {@code POINT} element from a direct position in <cite>Well Known Text</cite>
     * (WKT) format.
     *
     * @param  position The position to format.
     * @param  isSimplePrecision {@code true} if every ordinate values can be casted to {@code float}.
     * @return The point as a {@code POINT} in WKT format.
     *
     * @see #isSimplePrecision(double[])
     */
    static String toString(final DirectPosition position, final boolean isSimplePrecision) {
        final StringBuilder buffer = new StringBuilder(32).append("POINT");
        final int dimension = position.getDimension();
        if (dimension == 0) {
            buffer.append("()");
        } else {
            char separator = '(';
            for (int i=0; i<dimension; i++) {
                buffer.append(separator);
                final double ordinate = position.getOrdinate(i);
                if (isSimplePrecision) {
                    buffer.append((float) ordinate);
                } else {
                    buffer.append(ordinate);
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
     * @param  wkt The WKT to parse.
     * @return The ordinates, or {@code null} if none.
     * @throws NumberFormatException If a number can not be parsed.
     * @throws IllegalArgumentException If the parenthesis are not balanced.
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
         * Now process every space-separated ordinates until we reach the closing parenthesis
         * or the end of string.
         */
        double[] ordinates = new double[2];
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
             * Parsing the number may throw a NumberFormatException. But the later is an
             * IllegalArgumentException subclass, so we are compliant with the contract.
             */
            final double value = Double.parseDouble(wkt.subSequence(start, i).toString());
            if (dimension == ordinates.length) {
                ordinates = Arrays.copyOf(ordinates, dimension*2);
            }
            ordinates[dimension++] = value;
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
        return ArraysExt.resize(ordinates, dimension);
    }

    /**
     * Returns a hash value for this coordinate. This method returns a value compliant
     * with the contract documented in the {@link DirectPosition#hashCode()} javadoc.
     * Consequently, it should be possible to mix different {@code DirectPosition}
     * implementations in the same hash map.
     *
     * @return A hash code value for this position.
     */
    @Override
    public int hashCode() {
        final int dimension = getDimension();
        int code = 1;
        for (int i=0; i<dimension; i++) {
            code = code*31 + Numerics.hashCode(doubleToLongBits(getOrdinate(i)));
        }
        return code + Objects.hashCode(getCoordinateReferenceSystem());
    }

    /**
     * Returns {@code true} if the specified object is also a {@code DirectPosition}
     * with equal coordinate and equal CRS.
     *
     * This method performs the comparison as documented in the {@link DirectPosition#equals(Object)}
     * javadoc. In particular, the given object is not required to be of the same implementation class.
     * Consequently, it should be possible to mix different {@code DirectPosition} implementations in
     * the same hash map.
     *
     * @param object The object to compare with this position.
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
                    if (!Numerics.equals(getOrdinate(i), that.getOrdinate(i))) {
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
