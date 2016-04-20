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
 * Do not add dependency to java.awt.Rectangle2D in this class, because not every platforms
 * support Java2D (e.g. Android),  or applications that do not need it may want to avoid to
 * force installation of the Java2D module (e.g. JavaFX/SWT).
 */
import java.util.Arrays;
import java.io.Serializable;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.internal.referencing.Formulas.isPoleToPole;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Base class of envelopes backed by an array. The ordinate values are stored in the {@link #ordinates} array.
 * The ordinate values of the lower corner are stored in the array portion from index {@link #beginIndex()}
 * inclusive to index {@link #endIndex()} exclusive. The ordinate values of the upper corner are stored in
 * the array portion from index {@code beginIndex() + d} inclusive to index {@code endIndex() + d} exclusive
 * where {@code d = ordinates.length >>> 1}.
 *
 * <p>Unless otherwise indicated by a "{@code // Must be overridden in SubEnvelope}" comment, every methods
 * in {@code ArrayEnvelope} and subclasses must take in account the {@code beginIndex} and {@code endIndex}
 * bounds. A few methods ignore the bounds for performance reason, so they need a dedicated implementation
 * in {@link SubEnvelope}.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
class ArrayEnvelope extends AbstractEnvelope implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1657970968782634545L;

    /**
     * Ordinate values of lower and upper corners. Except for {@link SubEnvelope}, the length of
     * this array is twice the number of dimensions. The first half contains the lower corner,
     * while the second half contains the upper corner.
     */
    final double[] ordinates;

    /**
     * The coordinate reference system, or {@code null}.
     */
    CoordinateReferenceSystem crs;

    /**
     * Creates a new envelope using the given array of ordinate values. This constructor stores
     * the given reference directly; it does <strong>not</strong> clone the given array. This is
     * the desired behavior for proper working of {@link SubEnvelope}.
     *
     * @param ordinates The array of ordinate values to store directly (not cloned).
     */
    ArrayEnvelope(final double[] ordinates) {
        this.ordinates = ordinates;
    }

    /*
     * Constructors below this point have public access because if we decided to make class
     * ArrayEnvelope public, then we would probably want to make those constructors public too.
     */

    /**
     * Constructs an envelope defined by two corners given as direct positions.
     * If at least one corner is associated to a CRS, then the new envelope will also
     * be associated to that CRS.
     *
     * @param  lowerCorner The limits in the direction of decreasing ordinate values for each dimension.
     * @param  upperCorner The limits in the direction of increasing ordinate values for each dimension.
     * @throws MismatchedDimensionException If the two positions do not have the same dimension.
     * @throws MismatchedReferenceSystemException If the CRS of the two position are not equal.
     */
    public ArrayEnvelope(final DirectPosition lowerCorner, final DirectPosition upperCorner)
            throws MismatchedDimensionException, MismatchedReferenceSystemException
    {
        crs = getCommonCRS(lowerCorner, upperCorner);           // This performs also an argument check.
        final int dimension = lowerCorner.getDimension();
        ensureDimensionMatches("crs", dimension, crs);
        ensureSameDimension(dimension, upperCorner.getDimension());
        ordinates = new double[dimension * 2];
        for (int i=0; i<dimension; i++) {
            ordinates[i            ] = lowerCorner.getOrdinate(i);
            ordinates[i + dimension] = upperCorner.getOrdinate(i);
        }
        verifyRanges(crs, ordinates);
    }

    /**
     * Constructs an envelope defined by two corners given as sequences of ordinate values.
     * The Coordinate Reference System is initially {@code null}.
     *
     * @param  lowerCorner The limits in the direction of decreasing ordinate values for each dimension.
     * @param  upperCorner The limits in the direction of increasing ordinate values for each dimension.
     * @throws MismatchedDimensionException If the two sequences do not have the same length.
     */
    public ArrayEnvelope(final double[] lowerCorner, final double[] upperCorner) throws MismatchedDimensionException {
        ensureNonNull("lowerCorner", lowerCorner);
        ensureNonNull("upperCorner", upperCorner);
        ensureSameDimension(lowerCorner.length, upperCorner.length);
        ordinates = Arrays.copyOf(lowerCorner, lowerCorner.length + upperCorner.length);
        System.arraycopy(upperCorner, 0, ordinates, lowerCorner.length, upperCorner.length);
    }

    /**
     * Constructs an empty envelope of the specified dimension. All ordinates
     * are initialized to 0 and the coordinate reference system is undefined.
     *
     * @param dimension The envelope dimension.
     */
    public ArrayEnvelope(final int dimension) {
        ordinates = new double[dimension * 2];
    }

    /**
     * Constructs an empty envelope with the specified coordinate reference system.
     * All ordinate values are initialized to 0.
     *
     * @param crs The coordinate reference system.
     */
    public ArrayEnvelope(final CoordinateReferenceSystem crs) {
        ensureNonNull("crs", crs);
        ordinates = new double[crs.getCoordinateSystem().getDimension() * 2];
        this.crs = crs;
    }

    /**
     * Constructs a new envelope with the same data than the specified envelope.
     *
     * @param envelope The envelope to copy.
     */
    public ArrayEnvelope(final Envelope envelope) {
        ensureNonNull("envelope", envelope);
        crs = envelope.getCoordinateReferenceSystem();
        final int dimension = envelope.getDimension();
        ordinates = new double[dimension * 2];
        final DirectPosition lowerCorner = envelope.getLowerCorner();
        final DirectPosition upperCorner = envelope.getUpperCorner();
        for (int i=0; i<dimension; i++) {
            ordinates[i]           = lowerCorner.getOrdinate(i);
            ordinates[i+dimension] = upperCorner.getOrdinate(i);
        }
        verifyRanges(crs, ordinates);
    }

    /**
     * Constructs a new envelope with the same data than the specified geographic bounding box.
     * The coordinate reference system is set to the
     * {@linkplain org.apache.sis.referencing.CommonCRS#defaultGeographic() default geographic CRS}.
     * Axis order is (<var>longitude</var>, <var>latitude</var>).
     *
     * @param box The bounding box to copy.
     */
    public ArrayEnvelope(final GeographicBoundingBox box) {
        ensureNonNull("box", box);
        ordinates = new double[] {
            box.getWestBoundLongitude(),
            box.getSouthBoundLatitude(),
            box.getEastBoundLongitude(),
            box.getNorthBoundLatitude()
        };
        if (Boolean.FALSE.equals(box.getInclusion())) {
            ArraysExt.swap(ordinates, 0, ordinates.length >>> 1);
            if (!isPoleToPole(ordinates[1], ordinates[3])) {
                ArraysExt.swap(ordinates, 1, (ordinates.length >>> 1) + 1);
            }
        }
        crs = CommonCRS.defaultGeographic();
        verifyRanges(crs, ordinates);
    }

    /**
     * Constructs a new envelope initialized to the values parsed from the given string in
     * {@code BOX} or <cite>Well Known Text</cite> (WKT) format. The given string is typically
     * a {@code BOX} element like below:
     *
     * {@preformat wkt
     *     BOX(-180 -90, 180 90)
     * }
     *
     * However this constructor is lenient to other geometry types like {@code POLYGON}.
     * See the javadoc of the {@link GeneralEnvelope#GeneralEnvelope(CharSequence) GeneralEnvelope}
     * constructor for more information.
     *
     * @param  wkt The {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @throws IllegalArgumentException If the given string can not be parsed.
     */
    public ArrayEnvelope(final CharSequence wkt) throws IllegalArgumentException {
        ensureNonNull("wkt", wkt);
        int levelParenth = 0;               // Number of opening parenthesis: (
        int levelBracket = 0;               // Number of opening brackets: [
        int dimLimit     = 4;               // The length of minimum and maximum arrays.
        int maxDimension = 0;               // The number of valid entries in the minimum and maximum arrays.
        final int length = CharSequences.skipTrailingWhitespaces(wkt, 0, wkt.length());
        double[] minimum = new double[dimLimit];
        double[] maximum = new double[dimLimit];
        int dimension = 0;
        int c;
scan:   for (int i=CharSequences.skipLeadingWhitespaces(wkt, 0, length); i<length; i+=Character.charCount(c)) {
            c = Character.codePointAt(wkt, i);
            if (Character.isUnicodeIdentifierStart(c)) {
                do {
                    i += Character.charCount(c);
                    if (i >= length) break scan;
                    c = Character.codePointAt(wkt, i);
                }
                while (Character.isUnicodeIdentifierPart(c));
            }
            if (Character.isSpaceChar(c)) {
                continue;
            }
            switch (c) {
                case ',':                                      dimension=0; continue;
                case '(':     ++levelParenth;                  dimension=0; continue;
                case '[':     ++levelBracket;                  dimension=0; continue;
                case ')': if (--levelParenth<0) fail(wkt,'('); dimension=0; continue;
                case ']': if (--levelBracket<0) fail(wkt,'['); dimension=0; continue;
            }
            /*
             * At this point we have skipped the leading keyword (BOX, POLYGON, etc.),
             * the spaces and the parenthesis if any. We should be at the beginning of
             * a number. Search the first separator character (which determine the end
             * of the number) and parse the number.
             */
            final int start = i;
            boolean flush = false;
scanNumber: while ((i += Character.charCount(c)) < length) {
                c = wkt.charAt(i);
                if (Character.isSpaceChar(c)) {
                    break;
                }
                switch (c) {
                    case ',':                                      flush=true; break scanNumber;
                    case ')': if (--levelParenth<0) fail(wkt,'('); flush=true; break scanNumber;
                    case ']': if (--levelBracket<0) fail(wkt,'['); flush=true; break scanNumber;
                }
            }
            /*
             * Parsing the number may throw a NumberFormatException. But the later is an
             * IllegalArgumentException subclass, so we are compliant with the contract.
             */
            final double value = Double.parseDouble(wkt.subSequence(start, i).toString());
            /*
             * Adjust the minimum and maximum value using the number that we parsed,
             * increasing the arrays size if necessary. Remember the maximum number
             * of dimensions we have found so far.
             */
            if (dimension == maxDimension) {
                if (dimension == dimLimit) {
                    dimLimit *= 2;
                    minimum = Arrays.copyOf(minimum, dimLimit);
                    maximum = Arrays.copyOf(maximum, dimLimit);
                }
                minimum[dimension] = maximum[dimension] = value;
                maxDimension = ++dimension;
            } else {
                if (value < minimum[dimension]) minimum[dimension] = value;
                if (value > maximum[dimension]) maximum[dimension] = value;
                dimension++;
            }
            if (flush) {
                dimension = 0;
            }
        }
        if (levelParenth != 0) fail(wkt, ')');
        if (levelBracket != 0) fail(wkt, ']');
        ordinates = ArraysExt.resize(minimum, maxDimension << 1);
        System.arraycopy(maximum, 0, ordinates, maxDimension, maxDimension);
    }

    /**
     * Throws an exception for unmatched parenthesis during WKT parsing.
     */
    private static void fail(final CharSequence wkt, char missing) {
        throw new IllegalArgumentException(Errors.format(
                Errors.Keys.NonEquilibratedParenthesis_2, wkt, missing));
    }

    /**
     * Makes sure the specified dimensions are identical.
     */
    static void ensureSameDimension(final int dim1, final int dim2) throws MismatchedDimensionException {
        if (dim1 != dim2) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_2, dim1, dim2));
        }
    }

    /**
     * Verifies the validity of the range of ordinates values in the given array.
     * If the given CRS is null, then this method conservatively does nothing.
     * Otherwise this method performs the following verifications:
     *
     * <ul>
     *   <li>{@code lower > upper} is allowed only for axes having {@link RangeMeaning#WRAPAROUND}.</li>
     * </ul>
     *
     * This method does <strong>not</strong> verify if the ordinate values are between the axis minimum and
     * maximum values. This is because out-of-range values exist in practice but do not impact the working
     * of {@code add(…)}, {@code intersect(…)}, {@code contains(…)} and similar methods. This in contrast
     * with the checks listed above, where failure to meet those conditions will cause the methods to
     * behave in an unexpected way.
     *
     * <div class="section">Implementation consistency</div>
     * The checks performed by this method shall be consistent with the checks performed by the following methods:
     * <ul>
     *   <li>{@link GeneralEnvelope#setCoordinateReferenceSystem(CoordinateReferenceSystem)}</li>
     *   <li>{@link GeneralEnvelope#setRange(int, double, double)}</li>
     *   <li>{@link SubEnvelope#setRange(int, double, double)}</li>
     * </ul>
     *
     * @param crs The coordinate reference system, or {@code null}.
     * @param ordinates The array of ordinate values to verify.
     */
    static void verifyRanges(final CoordinateReferenceSystem crs, final double[] ordinates) {
        if (crs != null) {
            final int dimension = ordinates.length >>> 1;
            for (int i=0; i<dimension; i++) {
                final double lower = ordinates[i];
                final double upper = ordinates[i + dimension];
                if (lower > upper && !isWrapAround(crs, i)) {
                    throw new IllegalArgumentException(illegalRange(crs, i, lower, upper));
                }
            }
        }
    }

    /**
     * Creates an error message for an illegal ordinates range at the given dimension.
     * This is used for formatting the exception message.
     */
    static String illegalRange(final CoordinateReferenceSystem crs,
            final int dimension, final double lower, final double upper)
    {
        Object name = IdentifiedObjects.getName(getAxis(crs, dimension), null);
        if (name == null) {
            name = dimension;       // Paranoiac fallback (name should never be null).
        }
        return Errors.format(Errors.Keys.IllegalOrdinateRange_3, lower, upper, name);
    }

    /**
     * Returns the index of the first valid ordinate value of the lower corner in the {@link #ordinates} array.
     * This is always 0, unless this envelope is a {@link SubEnvelope}.
     *
     * <p>See {@link #endIndex()} for the list of methods that need to be also overridden
     * if this {@code beginIndex()} method is overridden.</p>
     */
    int beginIndex() {
        return 0;
    }

    /**
     * Returns the index after the last valid ordinate value of the lower corner in the {@link #ordinates} array.
     * This is always {@code ordinates.length >>> 1}, unless this envelope is a {@link SubEnvelope}.
     *
     * <p>Unless otherwise indicated by a "{@code // Must be overridden in SubEnvelope}" comment, every methods
     * in {@code ArrayEnvelope} and subclasses must take in account the {@code beginIndex} and {@code endIndex}
     * bounds. The methods listed below ignore the bounds for performance reason, so they need to be overridden
     * in {@link SubEnvelope}:</p>
     *
     * <ul>
     *   <li>{@link #getDimension()}</li>
     *   <li>{@link #getLower(int)}</li>
     *   <li>{@link #getUpper(int)}</li>
     *   <li>{@link #isAllNaN()}</li>
     *   <li>{@link #hashCode()}</li>
     *   <li>{@link #equals(Object)}</li>
     * </ul>
     */
    int endIndex() {
        return ordinates.length >>> 1;
    }

    /**
     * Returns the length of coordinate sequence (the number of entries) in this envelope.
     * This information is available even when the {@linkplain #getCoordinateReferenceSystem()
     * coordinate reference system} is unknown.
     *
     * @return The dimensionality of this envelope.
     */
    @Override                                       // Must also be overridden in SubEnvelope
    public int getDimension() {
        return ordinates.length >>> 1;
    }

    /**
     * Returns the envelope coordinate reference system, or {@code null} if unknown.
     * If non-null, it shall be the same as {@linkplain #getLowerCorner() lower corner}
     * and {@linkplain #getUpperCorner() upper corner} CRS.
     *
     * @return The envelope CRS, or {@code null} if unknown.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        assert crs == null || crs.getCoordinateSystem().getDimension() == getDimension();
        return crs;
    }

    /**
     * {@inheritDoc}
     */
    @Override                                       // Must also be overridden in SubEnvelope
    public double getLower(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(ordinates.length >>> 1, dimension);
        return ordinates[dimension];
    }

    /**
     * {@inheritDoc}
     */
    @Override                                       // Must also be overridden in SubEnvelope
    public double getUpper(final int dimension) throws IndexOutOfBoundsException {
        final int d = ordinates.length >>> 1;
        ensureValidIndex(d, dimension);
        return ordinates[dimension + d];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMinimum(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(endIndex(), dimension);
        final int i = dimension + beginIndex();
        double lower = ordinates[i];
        if (isNegative(ordinates[i + (ordinates.length >>> 1)] - lower)) {      // Special handling for -0.0
            final CoordinateSystemAxis axis = getAxis(crs, dimension);
            lower = (axis != null) ? axis.getMinimumValue() : Double.NEGATIVE_INFINITY;
        }
        return lower;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMaximum(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(endIndex(), dimension);
        final int i = dimension + beginIndex();
        double upper = ordinates[i + (ordinates.length >>> 1)];
        if (isNegative(upper - ordinates[i])) {                                 // Special handling for -0.0
            final CoordinateSystemAxis axis = getAxis(crs, dimension);
            upper = (axis != null) ? axis.getMaximumValue() : Double.POSITIVE_INFINITY;
        }
        return upper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMedian(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(endIndex(), dimension);
        final int i = dimension + beginIndex();
        final double minimum = ordinates[i];
        final double maximum = ordinates[i + (ordinates.length >>> 1)];
        double median = 0.5 * (minimum + maximum);
        if (isNegative(maximum - minimum)) {                                    // Special handling for -0.0
            median = fixMedian(getAxis(crs, dimension), median);
        }
        return median;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSpan(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(endIndex(), dimension);
        final int i = dimension + beginIndex();
        double span = ordinates[i + (ordinates.length >>> 1)] - ordinates[i];
        if (isNegative(span)) {                                                 // Special handling for -0.0
            span = fixSpan(getAxis(crs, dimension), span);
        }
        return span;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        final int beginIndex = beginIndex();
        final int endIndex = endIndex();
        if (beginIndex == endIndex) {
            return true;
        }
        final int d = ordinates.length >>> 1;
        for (int i=beginIndex; i<endIndex; i++) {
            final double span = ordinates[i+d] - ordinates[i];
            if (!(span > 0)) {                                                  // Use '!' in order to catch NaN
                if (!(isNegative(span) && isWrapAround(crs, i - beginIndex))) {
                    return true;
                }
            }
        }
        assert !isAllNaN() : this;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override                                       // Must also be overridden in SubEnvelope
    public boolean isAllNaN() {
        for (int i=0; i<ordinates.length; i++) {
            if (!Double.isNaN(ordinates[i])) {
                return false;
            }
        }
        assert isEmpty() : this;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override                                       // Must also be overridden in SubEnvelope
    public int hashCode() {
        int code = Arrays.hashCode(ordinates);
        if (crs != null) {
            code += crs.hashCode();
        }
        assert code == hashCodeByAPI();
        return code;
    }

    /**
     * Computes the hash code value using the public API instead than direct access to the
     * {@link #ordinates} array. This method is invoked from {@link SubEnvelope}.
     */
    final int hashCodeByAPI() {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override                                       // Must also be overridden in SubEnvelope
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final ArrayEnvelope that = (ArrayEnvelope) object;
            return Arrays.equals(this.ordinates, that.ordinates) &&
                  Objects.equals(this.crs, that.crs);
        }
        return false;
    }

    /**
     * Compares the given object for equality using the public API instead than direct access
     * to the {@link #ordinates} array. This method is invoked from {@link SubEnvelope}.
     */
    final boolean equalsByAPI(final Object object) {
        return super.equals(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(this, AbstractDirectPosition.isSimplePrecision(ordinates));
    }
}
