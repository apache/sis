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
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;

import static java.lang.Double.doubleToLongBits;
import static org.apache.sis.util.Arrays.resize;
import static org.apache.sis.util.StringBuilders.trimFractionalPart;


/**
 * Base class for {@linkplain DirectPosition direct position} implementations.
 * This base class provides default implementations for {@link #toString()},
 * {@link #equals(Object)} and {@link #hashCode()} methods.
 *
 * <p>This class do not holds any state. The decision to implement {@link java.io.Serializable}
 * or {@link Cloneable} interfaces is left to implementors.</p>
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.4)
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
     */
    @Override
    public final DirectPosition getDirectPosition() {
        return this;
    }

    /**
     * Sets this direct position to the given position. If the given position is
     * {@code null}, then all ordinate values are set to {@link Double#NaN NaN}.
     *
     * <p>If this position and the given position have a non-null CRS, then the default implementation
     * requires the CRS to be {@linkplain Utilities#equalsIgnoreMetadata equals (ignoring metadata)},
     * otherwise a {@link MismatchedReferenceSystemException} is thrown. However subclass may choose
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
            ensureDimensionMatch("position", position.getDimension(), dimension);
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
     * Ensures that the given CRS, if non-null, has the expected number of dimensions.
     *
     * @param  crs The coordinate reference system to check, or {@code null}.
     * @param  expected The expected number of dimensions.
     * @throws MismatchedDimensionException if the CRS dimension is not valid.
     */
    static void ensureDimensionMatch(final CoordinateReferenceSystem crs,
            final int expected) throws MismatchedDimensionException
    {
        if (crs != null) {
            final int dimension = crs.getCoordinateSystem().getDimension();
            if (dimension != expected) {
                throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3,
                          crs.getName().getCode(), dimension, expected));
            }
        }
    }

    /**
     * Ensures that the given number of dimensions is equals to the expected value.
     *
     * @param  name The name of the argument to check.
     * @param  dimension The object dimension.
     * @param  expectedDimension The Expected dimension for the object.
     * @throws MismatchedDimensionException if the object doesn't have the expected dimension.
     */
    static void ensureDimensionMatch(final String name, final int dimension,
            final int expectedDimension) throws MismatchedDimensionException
    {
        if (dimension != expectedDimension) {
            throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3,
                        name, dimension, expectedDimension));
        }
    }

    /**
     * Formats this position in the <cite>Well Known Text</cite> (WKT) format.
     * The returned string is like below, where {@code x₀}, {@code x₁}, {@code x₂}, <i>etc.</i>
     * are the {@linkplain #getOrdinate(int) ordinate} values at index 0, 1, 2, <i>etc.</i>:
     *
     * {@preformat wkt
     *   POINT(x₀ x₁ x₂ …)
     * }
     *
     * The string returned by this method can be parsed by the {@link GeneralDirectPosition} constructor.
     *
     * @return This position as a {@code POINT} in <cite>Well Known Text</cite> (WKT) format.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(32).append("POINT(");
        final int dimension = getDimension();
        for (int i=0; i<dimension; i++) {
            if (i != 0) {
                buffer.append(' ');
            }
            trimFractionalPart(buffer.append(getOrdinate(i)));
        }
        return buffer.append(')').toString();
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
                final int key;
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
        return resize(ordinates, dimension);
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
            final long bits = doubleToLongBits(getOrdinate(i));
            code = 31 * code + (((int) bits) ^ (int) (bits >>> 32));
        }
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        if (crs != null) {
            code += crs.hashCode();
        }
        return code;
    }

    /**
     * Returns {@code true} if the specified object is also a {@code DirectPosition}
     * with equal {@linkplain #getCoordinate() coordinate} and equal
     * {@linkplain #getCoordinateReferenceSystem CRS}.
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
                    if (doubleToLongBits(getOrdinate(i)) != doubleToLongBits(that.getOrdinate(i))) {
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
