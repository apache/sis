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
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.referencing.CRS;

import static org.apache.sis.util.Arrays.resize;
import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.internal.referencing.Utilities.isPoleToPole;

// Related to JDK7
import org.apache.sis.internal.util.Objects;


/**
 * Base class of envelopes backed by an array.
 * See {@link GeneralEnvelope} javadoc for more information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
class ArrayEnvelope extends AbstractEnvelope implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7284917239693486738L;

    /**
     * Ordinate values of lower and upper corners. The length of this array is twice the
     * number of dimensions. The first half contains the lower corner, while the second
     * half contains the upper corner.
     */
    final double[] ordinates;

    /**
     * The coordinate reference system, or {@code null}.
     */
    CoordinateReferenceSystem crs;

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
        crs = getCommonCRS(lowerCorner, upperCorner); // This performs also an argument check.
        final int dimension = lowerCorner.getDimension();
        AbstractDirectPosition.ensureDimensionMatch(crs, dimension);
        ensureSameDimension(dimension, upperCorner.getDimension());
        ordinates = new double[dimension * 2];
        for (int i=0; i<dimension; i++) {
            ordinates[i            ] = lowerCorner.getOrdinate(i);
            ordinates[i + dimension] = upperCorner.getOrdinate(i);
        }
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
        if (envelope instanceof ArrayEnvelope) {
            final ArrayEnvelope e = (ArrayEnvelope) envelope;
            ordinates = e.ordinates.clone();
            crs = e.crs;
        } else {
            crs = envelope.getCoordinateReferenceSystem();
            final int dimension = envelope.getDimension();
            ordinates = new double[dimension * 2];
            final DirectPosition lowerCorner = envelope.getLowerCorner();
            final DirectPosition upperCorner = envelope.getUpperCorner();
            for (int i=0; i<dimension; i++) {
                ordinates[i]           = lowerCorner.getOrdinate(i);
                ordinates[i+dimension] = upperCorner.getOrdinate(i);
            }
        }
    }

    /**
     * Constructs a new envelope with the same data than the specified geographic bounding box.
     * The coordinate reference system is set to {@code "CRS:84"}.
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
            swap(0);
            if (!isPoleToPole(ordinates[1], ordinates[3])) {
                swap(1);
            }
        }
        try {
            crs = CRS.forCode("CRS:84");
        } catch (FactoryException e) {
            // Should never happen since we asked for a CRS which should always be present.
            throw new AssertionError(e);
        }
    }

    /**
     * Constructs a new envelope initialized to the values parsed from the given string in
     * <cite>Well Known Text</cite> (WKT) format. The given string is typically a {@code BOX}
     * element like below:
     *
     * {@preformat wkt
     *     BOX(-180 -90, 180 90)
     * }
     *
     * However this constructor is lenient to other geometry types like {@code POLYGON}.
     * See the javadoc of the {@link GeneralEnvelope#GeneralEnvelope(String) GeneralEnvelope}
     * constructor for more information.
     *
     * @param  wkt The {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @throws IllegalArgumentException If the given string can not be parsed.
     */
    public ArrayEnvelope(final CharSequence wkt) throws IllegalArgumentException {
        ensureNonNull("wkt", wkt);
        int levelParenth = 0; // Number of opening parenthesis: (
        int levelBracket = 0; // Number of opening brackets: [
        int dimLimit     = 4; // The length of minimum and maximum arrays.
        int maxDimension = 0; // The number of valid entries in the minimum and maximum arrays.
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
        ordinates = resize(minimum, maxDimension << 1);
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
     * Swaps two ordinate values.
     */
    private void swap(final int i) {
        final int m = i + (ordinates.length >>> 1);
        final double t = ordinates[i];
        ordinates[i] = ordinates[m];
        ordinates[m] = t;
    }

    /**
     * Returns the length of coordinate sequence (the number of entries) in this envelope.
     * This information is available even when the {@linkplain #getCoordinateReferenceSystem()
     * coordinate reference system} is unknown.
     *
     * @return The dimensionality of this envelope.
     */
    @Override
    public final int getDimension() {
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
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        assert crs == null || crs.getCoordinateSystem().getDimension() == getDimension();
        return crs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLower(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(ordinates.length >>> 1, dimension);
        return ordinates[dimension];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getUpper(final int dimension) throws IndexOutOfBoundsException {
        final int dim = ordinates.length >>> 1;
        ensureValidIndex(dim, dimension);
        return ordinates[dimension + dim];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMinimum(final int dimension) throws IndexOutOfBoundsException {
        final int dim = ordinates.length >>> 1;
        ensureValidIndex(dim, dimension);
        double lower = ordinates[dimension];
        if (isNegative(ordinates[dimension + dim] - lower)) { // Special handling for -0.0
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
        final int dim = ordinates.length >>> 1;
        ensureValidIndex(dim, dimension);
        double upper = ordinates[dimension + dim];
        if (isNegative(upper - ordinates[dimension])) { // Special handling for -0.0
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
        ensureValidIndex(ordinates.length >>> 1, dimension);
        final double minimum = ordinates[dimension];
        final double maximum = ordinates[dimension + (ordinates.length >>> 1)];
        double median = 0.5 * (minimum + maximum);
        if (isNegative(maximum - minimum)) { // Special handling for -0.0
            median = fixMedian(getAxis(crs, dimension), median);
        }
        return median;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSpan(final int dimension) throws IndexOutOfBoundsException {
        ensureValidIndex(ordinates.length >>> 1, dimension);
        double span = ordinates[dimension + (ordinates.length >>> 1)] - ordinates[dimension];
        if (isNegative(span)) { // Special handling for -0.0
            span = fixSpan(getAxis(crs, dimension), span);
        }
        return span;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        final int dimension = ordinates.length >>> 1;
        if (dimension == 0) {
            return true;
        }
        for (int i=0; i<dimension; i++) {
            final double span = ordinates[i+dimension] - ordinates[i];
            if (!(span > 0)) { // Use '!' in order to catch NaN
                if (!(isNegative(span) && isWrapAround(crs, i))) {
                    return true;
                }
            }
        }
        assert !isNull() : this;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNull() {
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
    @Override
    public int hashCode() {
        int code = Arrays.hashCode(ordinates);
        if (crs != null) {
            code += crs.hashCode();
        }
        assert code == super.hashCode();
        return code;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final ArrayEnvelope that = (ArrayEnvelope) object;
            return Arrays.equals(this.ordinates, that.ordinates) &&
                  Objects.equals(this.crs, that.crs);
        }
        return false;
    }
}
