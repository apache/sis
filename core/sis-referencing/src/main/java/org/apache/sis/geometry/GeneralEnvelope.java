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
import java.lang.reflect.Field;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.math.MathFunctions.isSameSign;


/**
 * A mutable {@code Envelope} (a minimum bounding box or rectangle) of arbitrary dimension.
 * Regardless of dimension, an {@code Envelope} can be represented without ambiguity
 * as two {@linkplain AbstractDirectPosition direct positions} (coordinate points).
 * To encode an {@code Envelope}, it is sufficient to encode these two points.
 *
 * <div class="note"><b>Note:</b>
 * {@code Envelope} uses an arbitrary <cite>Coordinate Reference System</cite>, which does not need to be geographic.
 * This is different than the {@code GeographicBoundingBox} class provided in the metadata package, which can be used
 * as a kind of envelope restricted to a Geographic CRS having Greenwich prime meridian.</div>
 *
 * This particular implementation of {@code Envelope} is said "General" because it uses
 * coordinates of an arbitrary number of dimensions. This is in contrast with
 * {@link Envelope2D}, which can use only two-dimensional coordinates.
 *
 * <p>A {@code GeneralEnvelope} can be created in various ways:</p>
 * <ul>
 *   <li>{@linkplain #GeneralEnvelope(int) From a given number of dimension}, with all ordinates initialized to 0.</li>
 *   <li>{@linkplain #GeneralEnvelope(double[], double[]) From two coordinate points}.</li>
 *   <li>{@linkplain #GeneralEnvelope(Envelope) From a an other envelope} (copy constructor).</li>
 *   <li>{@linkplain #GeneralEnvelope(GeographicBoundingBox) From a geographic bounding box}.</li>
 *   <li>{@linkplain #GeneralEnvelope(CharSequence) From a character sequence}
 *       representing a {@code BBOX} or a <cite>Well Known Text</cite> (WKT) format.</li>
 * </ul>
 *
 * <div class="section">Spanning the anti-meridian of a Geographic CRS</div>
 * The <cite>Web Coverage Service</cite> (WCS) specification authorizes (with special treatment)
 * cases where <var>upper</var> &lt; <var>lower</var> at least in the longitude case. They are
 * envelopes crossing the anti-meridian, like the red box below (the green box is the usual case).
 * The default implementation of methods listed in the right column can handle such cases.
 *
 * <center><table class="compact" summary="Anti-meridian spanning support."><tr><td>
 *   <img style="vertical-align: middle" src="doc-files/AntiMeridian.png" alt="Envelope spannning the anti-meridian">
 * </td><td style="vertical-align: middle">
 * Supported methods:
 * <ul>
 *   <li>{@link #getMinimum(int)}</li>
 *   <li>{@link #getMaximum(int)}</li>
 *   <li>{@link #getMedian(int)}</li>
 *   <li>{@link #getSpan(int)}</li>
 *   <li>{@link #isEmpty()}</li>
 *   <li>{@link #toSimpleEnvelopes() toSimpleEnvelopes()}</li>
 *   <li>{@link #contains(DirectPosition) contains(DirectPosition)}</li>
 *   <li>{@link #contains(Envelope) contains(Envelope)}</li>
 *   <li>{@link #intersects(Envelope) intersects(Envelope)}</li>
 *   <li>{@link #intersect(Envelope)}</li>
 *   <li>{@link #add(Envelope)}</li>
 *   <li>{@link #add(DirectPosition)}</li>
 * </ul>
 * </td></tr></table></center>
 *
 * <div class="section">Envelope validation</div>
 * If and only if this envelope is associated to a non-null CRS, then constructors and setter methods
 * in this class perform the following checks:
 *
 * <ul>
 *   <li>The number of CRS dimensions must be equals to <code>this.{@linkplain #getDimension()}</code>.</li>
 *   <li>For each dimension <var>i</var>,
 *       <code>{@linkplain #getLower(int) getLower}(i) &gt; {@linkplain #getUpper(int) getUpper}(i)</code> is allowed
 *       only if the {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getRangeMeaning() coordinate
 *       system axis range meaning} is {@code WRAPAROUND}.</li>
 * </ul>
 *
 * Note that this class does <em>not</em> require the ordinate values to be between the axis minimum and
 * maximum values. This flexibility exists because out-of-range values happen in practice, while they do
 * not hurt the working of {@code add(…)}, {@code intersect(…)}, {@code contains(…)} and similar methods.
 * This in contrast with the {@code lower > upper} case, which cause the above-cited methods to behave in
 * an unexpected way if the axis does not have wraparound range meaning.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see Envelope2D
 * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox
 */
public class GeneralEnvelope extends ArrayEnvelope implements Cloneable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3796799507279068254L;

    /**
     * Used for setting the {@link #ordinates} field during a {@link #clone()} operation only.
     * Will be fetch when first needed.
     */
    private static volatile Field ordinatesField;

    /**
     * Creates a new envelope using the given array of ordinate values. This constructor stores
     * the given reference directly; it does <strong>not</strong> clone the given array. This is
     * the desired behavior for proper working of {@link SubEnvelope}.
     *
     * @param ordinates The array of ordinate values to store directly (not cloned).
     */
    GeneralEnvelope(final double[] ordinates) {
        super(ordinates);
    }

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
    public GeneralEnvelope(final DirectPosition lowerCorner, final DirectPosition upperCorner)
            throws MismatchedDimensionException, MismatchedReferenceSystemException
    {
        super(lowerCorner, upperCorner);
    }

    /**
     * Constructs an envelope defined by two corners given as sequences of ordinate values.
     * The Coordinate Reference System is initially {@code null}.
     *
     * @param  lowerCorner The limits in the direction of decreasing ordinate values for each dimension.
     * @param  upperCorner The limits in the direction of increasing ordinate values for each dimension.
     * @throws MismatchedDimensionException If the two sequences do not have the same length.
     */
    public GeneralEnvelope(final double[] lowerCorner, final double[] upperCorner) throws MismatchedDimensionException {
        super(lowerCorner, upperCorner);
    }

    /**
     * Constructs an empty envelope of the specified dimension. All ordinates
     * are initialized to 0 and the coordinate reference system is undefined.
     *
     * @param dimension The envelope dimension.
     */
    public GeneralEnvelope(final int dimension) {
        super(dimension);
    }

    /**
     * Constructs an empty envelope with the specified coordinate reference system.
     * All ordinate values are initialized to 0.
     *
     * @param crs The coordinate reference system.
     */
    public GeneralEnvelope(final CoordinateReferenceSystem crs) {
        super(crs);
    }

    /**
     * Constructs a new envelope with the same data than the specified envelope.
     *
     * @param envelope The envelope to copy.
     *
     * @see #castOrCopy(Envelope)
     */
    public GeneralEnvelope(final Envelope envelope) {
        super(envelope);
    }

    /**
     * Constructs a new envelope with the same data than the specified geographic bounding box.
     * The coordinate reference system is set to the
     * {@linkplain org.apache.sis.referencing.CommonCRS#defaultGeographic() default geographic CRS}.
     * Axis order is (<var>longitude</var>, <var>latitude</var>).
     *
     * @param box The bounding box to copy.
     */
    public GeneralEnvelope(final GeographicBoundingBox box) {
        super(box);
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
     * Actually this constructor ignores the geometry type and just applies the following
     * simple rules:
     *
     * <ul>
     *   <li>Character sequences complying to the rules of Java identifiers are skipped.</li>
     *   <li>Coordinates are separated by a coma ({@code ,}) character.</li>
     *   <li>The ordinates in a coordinate are separated by a space.</li>
     *   <li>Ordinate numbers are assumed formatted in US locale.</li>
     *   <li>The coordinate having the highest dimension determines the dimension of this envelope.</li>
     * </ul>
     *
     * This constructor does not check the consistency of the provided text. For example it does not
     * check that every points in a {@code LINESTRING} have the same dimension. However this
     * constructor ensures that the parenthesis are balanced, in order to catch some malformed WKT.
     *
     * <div class="note"><b>Example:</b>
     * The following texts can be parsed by this constructor in addition of the usual {@code BOX} element.
     * This constructor creates the bounding box of those geometries:
     *
     * <ul>
     *   <li>{@code POINT(6 10)}</li>
     *   <li>{@code MULTIPOLYGON(((1 1, 5 1, 1 5, 1 1),(2 2, 3 2, 3 3, 2 2)))}</li>
     *   <li>{@code GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(3 8,7 10))}</li>
     * </ul></div>
     *
     * @param  wkt The {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @throws IllegalArgumentException If the given string can not be parsed.
     *
     * @see Envelopes#fromWKT(CharSequence)
     * @see Envelopes#toString(Envelope)
     */
    public GeneralEnvelope(final CharSequence wkt) throws IllegalArgumentException {
        super(wkt);
    }

    /**
     * Returns the given envelope as a {@code GeneralEnvelope} instance. If the given envelope
     * is already an instance of {@code GeneralEnvelope}, then it is returned unchanged.
     * Otherwise the coordinate values and the CRS of the given envelope are
     * {@linkplain #GeneralEnvelope(Envelope) copied} in a new {@code GeneralEnvelope}.
     *
     * @param  envelope The envelope to cast, or {@code null}.
     * @return The values of the given envelope as a {@code GeneralEnvelope} instance.
     *
     * @see AbstractEnvelope#castOrCopy(Envelope)
     * @see ImmutableEnvelope#castOrCopy(Envelope)
     */
    public static GeneralEnvelope castOrCopy(final Envelope envelope) {
        if (envelope == null || envelope instanceof GeneralEnvelope) {
            return (GeneralEnvelope) envelope;
        }
        return new GeneralEnvelope(envelope);
    }

    /**
     * Sets the coordinate reference system in which the coordinate are given.
     * This method <strong>does not</strong> reproject the envelope, and does
     * not check if the envelope is contained in the new domain of validity.
     *
     * <p>If the envelope coordinates need to be transformed to the new CRS, consider
     * using {@link Envelopes#transform(Envelope, CoordinateReferenceSystem)} instead.</p>
     *
     * @param  crs The new coordinate reference system, or {@code null}.
     * @throws MismatchedDimensionException if the specified CRS doesn't have the expected number of dimensions.
     * @throws IllegalStateException if a range of ordinate values in this envelope is compatible with the given CRS.
     *         See <cite>Envelope validation</cite> in class javadoc for more details.
     */
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs)
            throws MismatchedDimensionException
    {
        ensureDimensionMatches("crs", getDimension(), crs);
        /*
         * The check performed here shall be identical to ArrayEnvelope.verifyRanges(crs, ordinates)
         * except that it may verify only a subset of the ordinate array and throws a different kind
         * of exception in caseo of failure.
         */
        if (crs != null) {
            final int beginIndex = beginIndex();
            final int endIndex = endIndex();
            final int d = ordinates.length >>> 1;
            for (int i=beginIndex; i<endIndex; i++) {
                final double lower = ordinates[i];
                final double upper = ordinates[i + d];
                if (lower > upper) {
                    final int j = i - beginIndex;
                    if (!isWrapAround(crs, j)) {
                        throw new IllegalStateException(illegalRange(crs, j, lower, upper));
                    }
                }
            }
        }
        this.crs = crs;                                             // Set only on success.
    }

    /**
     * Sets the envelope range along the specified dimension.
     *
     * @param  dimension The dimension to set.
     * @param  lower     The limit in the direction of decreasing ordinate values.
     * @param  upper     The limit in the direction of increasing ordinate values.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     * @throws IllegalArgumentException If {@code lower > upper} and the axis range meaning at the given dimension
     *         is not "wraparound". See <cite>Envelope validation</cite> in class javadoc for more details.
     */
    @Override                                                           // Must also be overridden in SubEnvelope
    public void setRange(final int dimension, final double lower, final double upper)
            throws IndexOutOfBoundsException
    {
        final int d = ordinates.length >>> 1;
        ensureValidIndex(d, dimension);
        /*
         * The check performed here shall be identical to ArrayEnvelope.verifyRanges(crs, ordinates),
         * except that there is no loop.
         */
        if (lower > upper && crs != null && !isWrapAround(crs, dimension)) {
            throw new IllegalArgumentException(illegalRange(crs, dimension, lower, upper));
        }
        ordinates[dimension + d] = upper;
        ordinates[dimension]     = lower;
    }

    /**
     * Sets the envelope to the specified values, which must be the lower corner coordinates
     * followed by upper corner coordinates. The number of arguments provided shall be twice
     * this {@linkplain #getDimension() envelope dimension}, and minimum shall not be greater
     * than maximum.
     *
     * <div class="note"><b>Example:</b>
     * (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>, <var>z</var><sub>min</sub>,
     *  <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>, <var>z</var><sub>max</sub>)
     * </div>
     *
     * @param corners Ordinates of the new lower corner followed by the new upper corner.
     */
    public void setEnvelope(final double... corners) {
        verifyArrayLength(ordinates.length >>> 1, corners);
        verifyRanges(crs, corners);
        System.arraycopy(corners, 0, ordinates, 0, ordinates.length);
    }

    /**
     * Verifies that the given array of ordinate values has the expected length
     * for the given number of dimensions.
     *
     * @param dimension The dimension of the envelope.
     * @param corners The user-provided array of ordinate values.
     */
    static void verifyArrayLength(final int dimension, final double[] corners) {
        if ((corners.length & 1) != 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.OddArrayLength_1, corners.length));
        }
        final int d = corners.length >>> 1;
        if (d != dimension) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, "ordinates", dimension, d));
        }
    }

    /**
     * Sets this envelope to the same coordinate values than the specified envelope.
     * If the given envelope has a non-null Coordinate Reference System (CRS), then
     * the CRS of this envelope will be set to the CRS of the given envelope.
     *
     * @param  envelope The envelope to copy coordinates from.
     * @throws MismatchedDimensionException if the specified envelope doesn't have
     *         the expected number of dimensions.
     */
    public void setEnvelope(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        ensureDimensionMatches("envelope", dimension, envelope);
        final DirectPosition lower = envelope.getLowerCorner();
        final DirectPosition upper = envelope.getUpperCorner();
        final int d = ordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            ordinates[iLower] = lower.getOrdinate(i);
            ordinates[iUpper] = upper.getOrdinate(i);
        }
        final CoordinateReferenceSystem envelopeCRS = envelope.getCoordinateReferenceSystem();
        if (envelopeCRS != null) {
            crs = envelopeCRS;
            assert crs.getCoordinateSystem().getDimension() == getDimension() : crs;
            assert envelope.getClass() != getClass() || equals(envelope) : envelope;
        }
    }

    /**
     * Sets the lower corner to {@linkplain Double#NEGATIVE_INFINITY negative infinity}
     * and the upper corner to {@linkplain Double#POSITIVE_INFINITY positive infinity}.
     * The {@linkplain #getCoordinateReferenceSystem() coordinate reference system}
     * (if any) stay unchanged.
     */
    public void setToInfinite() {
        final int beginIndex = beginIndex();
        final int endIndex = endIndex();
        final int d = ordinates.length >>> 1;
        Arrays.fill(ordinates, beginIndex,   endIndex,   Double.NEGATIVE_INFINITY);
        Arrays.fill(ordinates, beginIndex+d, endIndex+d, Double.POSITIVE_INFINITY);
    }

    /**
     * Sets all ordinate values to {@linkplain Double#NaN NaN}.
     * The {@linkplain #getCoordinateReferenceSystem() coordinate reference system}
     * (if any) stay unchanged.
     *
     * @see #isAllNaN()
     */
    public void setToNaN() {                   // Must be overridden in SubEnvelope
        Arrays.fill(ordinates, Double.NaN);
        assert isAllNaN() : this;
    }

    /**
     * Translates the envelope by the given vector. For every dimension <var>i</var>, the
     * {@linkplain #getLower(int) lower} and {@linkplain #getUpper(int) upper} values are
     * increased by {@code vector[i]}.
     *
     * <p>This method does not check if the translation result is inside the coordinate system domain
     * (e.g. [-180 … +180]° of longitude). Callers can normalize the envelope when desired by call to
     * the {@link #normalize()} method.</p>
     *
     * @param vector The translation vector. The length of this array shall be equal to this envelope
     *        {@linkplain #getDimension() dimension}.
     *
     * @since 0.5
     */
    public void translate(final double... vector) {
        ensureNonNull("vector", vector);
        final int beginIndex = beginIndex();
        ensureDimensionMatches("vector", endIndex() - beginIndex, vector);
        final int upperIndex = beginIndex + (ordinates.length >>> 1);
        for (int i=0; i<vector.length; i++) {
            final double t = vector[i];
            ordinates[beginIndex + i] += t;
            ordinates[upperIndex + i] += t;
        }
    }

    /**
     * Adds to this envelope a point of the given array.
     * This method does not check for anti-meridian spanning. It is invoked only
     * by the {@link Envelopes} transform methods, which build "normal" envelopes.
     *
     * @param  array The array which contains the ordinate values.
     * @param  offset Index of the first valid ordinate value in the given array.
     */
    final void addSimple(final double[] array, final int offset) {
        final int d = ordinates.length >>> 1;
        for (int i=0; i<d; i++) {
            final double value = array[offset + i];
            if (value < ordinates[i  ]) ordinates[i  ] = value;
            if (value > ordinates[i+d]) ordinates[i+d] = value;
        }
    }

    /**
     * Adds a point to this envelope. The resulting envelope is the smallest envelope that
     * contains both the original envelope and the specified point.
     *
     * <p>After adding a point, a call to {@link #contains(DirectPosition) contains(DirectPosition)}
     * with the added point as an argument will return {@code true}, except if one of the point
     * ordinates was {@link Double#NaN} in which case the corresponding ordinate has been ignored.</p>
     *
     * <div class="section">Pre-conditions</div>
     * This method assumes that the specified point uses the same CRS than this envelope.
     * For performance reasons, it will no be verified unless Java assertions are enabled.
     *
     * <div class="section">Spanning the anti-meridian of a Geographic CRS</div>
     * This method supports envelopes spanning the anti-meridian. In such cases it is possible to
     * move both envelope borders in order to encompass the given point, as illustrated below (the
     * new point is represented by the {@code +} symbol):
     *
     * {@preformat text
     *    ─────┐   + ┌─────
     *    ─────┘     └─────
     * }
     *
     * The default implementation moves only the border which is closest to the given point.
     *
     * @param  position The point to add.
     * @throws MismatchedDimensionException If the given point does not have the expected number of dimensions.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     */
    public void add(final DirectPosition position) throws MismatchedDimensionException {
        ensureNonNull("position", position);
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        ensureDimensionMatches("position", dimension, position);
        assert equalsIgnoreMetadata(crs, position.getCoordinateReferenceSystem(), true) : position;
        final int d = ordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final double value = position.getOrdinate(i);
            final double min = ordinates[iLower];
            final double max = ordinates[iUpper];
            if (!isNegative(max - min)) {                       // Standard case, or NaN.
                if (value < min) ordinates[iLower] = value;
                if (value > max) ordinates[iUpper] = value;
            } else {
                /*
                 * Spanning the anti-meridian. The [max…min] range (not that min/max are
                 * interchanged) is actually an exclusion area. Changes only the closest
                 * side.
                 */
                addToClosest(iLower, value, max, min);
            }
        }
        assert contains(position) || isEmpty() || hasNaN(position) : position;
    }

    /**
     * Invoked when a point is added to a range spanning the anti-meridian.
     * In the example below, the new point is represented by the {@code +}
     * symbol. The point is added only on the closest side.
     *
     * {@preformat text
     *    ─────┐   + ┌─────
     *    ─────┘     └─────
     * }
     *
     * @param  i     The dimension of the ordinate
     * @param  value The ordinate value to add to this envelope.
     * @param  left  The border on the left side,  which is the <em>max</em> value (yes, this is confusing!)
     * @param  right The border on the right side, which is the <em>min</em> value (yes, this is confusing!)
     */
    private void addToClosest(int i, final double value, double left, double right) {
        left = value - left;
        if (left > 0) {
            right -= value;
            if (right > 0) {
                if (right > left) {
                    i += (ordinates.length >>> 1);
                }
                ordinates[i] = value;
            }
        }
    }

    /**
     * Adds an envelope object to this envelope. The resulting envelope is the union of the
     * two {@code Envelope} objects.
     *
     * <div class="section">Pre-conditions</div>
     * This method assumes that the specified envelope uses the same CRS than this envelope.
     * For performance reasons, it will no be verified unless Java assertions are enabled.
     *
     * <div class="section">Spanning the anti-meridian of a Geographic CRS</div>
     * This method supports envelopes spanning the anti-meridian. If one or both envelopes span
     * the anti-meridian, then the result of the {@code add} operation may be an envelope expanding
     * to infinities. In such case, the ordinate range will be either [-∞…∞] or [0…-0] depending on
     * whatever the original range span the anti-meridian or not.
     *
     * @param  envelope the {@code Envelope} to add to this envelope.
     * @throws MismatchedDimensionException If the given envelope does not have the expected number of dimensions.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#add(GeographicBoundingBox)
     */
    public void add(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        ensureDimensionMatches("envelope", dimension, envelope);
        assert equalsIgnoreMetadata(crs, envelope.getCoordinateReferenceSystem(), true) : envelope;
        final DirectPosition lower = envelope.getLowerCorner();
        final DirectPosition upper = envelope.getUpperCorner();
        final int d = ordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final double min0 = ordinates[iLower];
            final double max0 = ordinates[iUpper];
            final double min1 = lower.getOrdinate(i);
            final double max1 = upper.getOrdinate(i);
            final boolean sp0 = isNegative(max0 - min0);
            final boolean sp1 = isNegative(max1 - min1);
            if (sp0 == sp1) {
                /*
                 * Standard case (for rows in the above pictures), or case where both envelopes
                 * span the anti-meridian (which is almost the same with an additional post-add
                 * check).
                 *    ┌──────────┐          ┌──────────┐
                 *    │  ┌────┐  │    or    │  ┌───────┼──┐
                 *    │  └────┘  │          │  └───────┼──┘
                 *    └──────────┘          └──────────┘
                 *
                 *    ────┐  ┌────          ────┐  ┌────
                 *    ──┐ │  │ ┌──    or    ────┼──┼─┐┌─
                 *    ──┘ │  │ └──          ────┼──┼─┘└─
                 *    ────┘  └────          ────┘  └────
                 */
                if (min1 < min0) ordinates[iLower] = min1;
                if (max1 > max0) ordinates[iUpper] = max1;
                if (!sp0 || isNegativeUnsafe(ordinates[iUpper] - ordinates[iLower])) {
                    continue;               // We are done, go to the next dimension.
                }
                // If we were spanning the anti-meridian before the union but
                // are not anymore after the union, we actually merged to two
                // sides, so the envelope is spanning to infinities. The code
                // close to the end of this loop will set an infinite range.
            } else if (sp0) {
                /*
                 * Only this envelope spans the anti-meridian; the given envelope is normal or
                 * has NaN values.  First we need to exclude the cases were the given envelope
                 * is fully included in this envelope:
                 *   ──────────┐  ┌─────
                 *     ┌────┐  │  │
                 *     └────┘  │  │
                 *   ──────────┘  └─────
                 */
                if (max1 <= max0) continue;             // This is the case of above picture.
                if (min1 >= min0) continue;             // Like above picture, but on the right side.
                /*
                 * At this point, the given envelope partially overlaps the "exclusion area"
                 * of this envelope or has NaN values. We will move at most one edge of this
                 * envelope, in order to leave as much free space as possible.
                 *    ─────┐      ┌─────
                 *       ┌─┼────┐ │
                 *       └─┼────┘ │
                 *    ─────┘      └─────
                 */
                final double left  = min1 - max0;
                final double right = min0 - max1;
                if (left > 0 || right > 0) {
                    // The < and > checks below are not completly redundant.
                    // The difference is when a value is NaN.
                    if (left > right) ordinates[iLower] = min1;
                    if (right > left) ordinates[iUpper] = max1;     // This is the case illustrated above.
                    continue;                                       // We are done, go to the next dimension.
                }
                // If we reach this point, the given envelope fills completly the "exclusion area"
                // of this envelope. As a consequence this envelope is now spanning to infinities.
                // We will set that fact close to the end of this loop.
            } else {
                /*
                 * Opposite of above case: this envelope is "normal" or has NaN values, and the
                 * given envelope spans to infinities.
                 */
                if (max0 <= max1 || min0 >= min1) {
                    ordinates[iLower] = min1;
                    ordinates[iUpper] = max1;
                    continue;
                }
                final double left  = min0 - max1;
                final double right = min1 - max0;
                if (left > 0 || right > 0) {
                    if (left > right) ordinates[iUpper] = max1;
                    if (right > left) ordinates[iLower] = min1;
                    continue;
                }
            }
            /*
             * If we reach that point, we went in one of the many cases where the envelope
             * has been expanded to infinity.  Declares an infinite range while preserving
             * the "normal" / "anti-meridian spanning" state.
             */
            if (sp0) {
                ordinates[iLower] = +0.0;
                ordinates[iUpper] = -0.0;
            } else {
                ordinates[iLower] = Double.NEGATIVE_INFINITY;
                ordinates[iUpper] = Double.POSITIVE_INFINITY;
            }
        }
        assert contains(envelope) || isEmpty() || hasNaN(envelope) : this;
    }

    /**
     * Sets this envelope to the intersection if this envelope with the specified one.
     *
     * <div class="section">Pre-conditions</div>
     * This method assumes that the specified envelope uses the same CRS than this envelope.
     * For performance reasons, it will no be verified unless Java assertions are enabled.
     *
     * <div class="section">Spanning the anti-meridian of a Geographic CRS</div>
     * This method supports envelopes spanning the anti-meridian.
     *
     * @param  envelope the {@code Envelope} to intersect to this envelope.
     * @throws MismatchedDimensionException If the given envelope does not have the expected number of dimensions.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#intersect(GeographicBoundingBox)
     */
    public void intersect(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        ensureDimensionMatches("envelope", dimension, envelope);
        assert equalsIgnoreMetadata(crs, envelope.getCoordinateReferenceSystem(), true) : envelope;
        final DirectPosition lower = envelope.getLowerCorner();
        final DirectPosition upper = envelope.getUpperCorner();
        final int d = ordinates.length >>> 1;
        for (int i=beginIndex; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final double min0  = ordinates[iLower];
            final double max0  = ordinates[iUpper];
            final double min1  = lower.getOrdinate(i);
            final double max1  = upper.getOrdinate(i);
            final double span0 = max0 - min0;
            final double span1 = max1 - min1;
            if (isSameSign(span0, span1)) {                 // Always 'false' if any value is NaN.
                /*
                 * First, verify that the two envelopes intersect.
                 *     ┌──────────┐             ┌─────────────┐
                 *     │  ┌───────┼──┐    or    │  ┌───────┐  │
                 *     │  └───────┼──┘          │  └───────┘  │
                 *     └──────────┘             └─────────────┘
                 */
                if ((min1 > max0 || max1 < min0) && !isNegativeUnsafe(span0)) {
                    /*
                     * The check for !isNegative(span0) is because if both envelopes span the
                     * anti-merdian, then there is always an intersection on both side no matter
                     * what envelope ordinates are because both envelopes extend toward infinities:
                     *     ────┐  ┌────            ────┐  ┌────
                     *     ──┐ │  │ ┌──     or     ────┼──┼─┐┌─
                     *     ──┘ │  │ └──            ────┼──┼─┘└─
                     *     ────┘  └────            ────┘  └────
                     * Since we excluded the above case, entering in this block means that the
                     * envelopes are "normal" and do not intersect, so we set ordinates to NaN.
                     *   ┌────┐
                     *   │    │     ┌────┐
                     *   │    │     └────┘
                     *   └────┘
                     */
                    ordinates[iLower] = ordinates[iUpper] = Double.NaN;
                    continue;
                }
            } else {
                int intersect = 0;                          // A bitmask of intersections (two bits).
                if (!Double.isNaN(span0) && !Double.isNaN(span1)) {
                    if (isNegativeUnsafe(span0)) {
                        /*
                         * The first line below checks for the case illustrated below. The second
                         * line does the same check, but with the small rectangle on the right side.
                         *    ─────┐      ┌─────              ──────────┐  ┌─────
                         *       ┌─┼────┐ │           or        ┌────┐  │  │
                         *       └─┼────┘ │                     └────┘  │  │
                         *    ─────┘      └─────              ──────────┘  └─────
                         */
                        if (min1 <= max0) {intersect  = 1; ordinates[iLower] = min1;}
                        if (max1 >= min0) {intersect |= 2; ordinates[iUpper] = max1;}
                    } else {
                        // Same than above, but with indices 0 and 1 interchanged.
                        // No need to set ordinate values since they would be the same.
                        if (min0 <= max1) {intersect  = 1;}
                        if (max0 >= min1) {intersect |= 2;}
                    }
                }
                /*
                 * Cases 0 and 3 are illustrated below. In case 1 and 2, we will set
                 * only the ordinate value which has not been set by the above code.
                 *
                 *                [intersect=0]          [intersect=3]
                 *              ─────┐     ┌─────      ─────┐     ┌─────
                 *  negative:    max0│ ┌─┐ │min0          ┌─┼─────┼─┐
                 *                   │ └─┘ │              └─┼─────┼─┘
                 *              ─────┘     └─────      ─────┘     └─────
                 *
                 *               max1  ┌─┐  min1          ┌─────────┐
                 * positive:    ─────┐ │ │ ┌─────      ───┼─┐     ┌─┼───
                 *              ─────┘ │ │ └─────      ───┼─┘     └─┼───
                 *                     └─┘                └─────────┘
                 */
                switch (intersect) {
                    default: throw new AssertionError(intersect);
                    case 1: if (max1 < max0) ordinates[iUpper] = max1; break;
                    case 2: if (min1 > min0) ordinates[iLower] = min1; break;
                    case 3: // Fall through
                    case 0: {
                        // Before to declare the intersection as invalid, verify if the envelope
                        // actually span the whole Earth. In such case, the intersection is a no-
                        // operation (or a copy operation).
                        final double min, max;
                        final double csSpan = getSpan(getAxis(crs, i));
                        if (span1 >= csSpan) {
                            min = min0;
                            max = max0;
                        } else if (span0 >= csSpan) {
                            min = min1;
                            max = max1;
                        } else {
                            min = Double.NaN;
                            max = Double.NaN;
                        }
                        ordinates[iLower] = min;
                        ordinates[iUpper] = max;
                        break;
                    }
                }
                continue;
            }
            if (min1 > min0) ordinates[iLower] = min1;
            if (max1 < max0) ordinates[iUpper] = max1;
        }
        // Tests only if the interection result is non-empty.
        assert isEmpty() || AbstractEnvelope.castOrCopy(envelope).contains(this) : this;
    }

    /**
     * Ensures that the envelope is contained in the coordinate system domain.
     * For each dimension, this method compares the ordinate values against the
     * limits of the coordinate system axis for that dimension.
     * If some ordinates are out of range, then there is a choice depending on the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() axis range meaning}:
     *
     * <ul class="verbose">
     *   <li>If {@link RangeMeaning#EXACT} (typically <em>latitudes</em> ordinates), then values
     *       greater than the {@linkplain CoordinateSystemAxis#getMaximumValue() axis maximal value}
     *       are replaced by the axis maximum, and values smaller than the
     *       {@linkplain CoordinateSystemAxis#getMinimumValue() axis minimal value}
     *       are replaced by the axis minimum.</li>
     *
     *   <li>If {@link RangeMeaning#WRAPAROUND} (typically <em>longitudes</em> ordinates), then
     *       a multiple of the axis range (e.g. 360° for longitudes) is added or subtracted.
     *       Example:
     *       <ul>
     *         <li>the [190 … 200]° longitude range is converted to [-170 … -160]°,</li>
     *         <li>the [170 … 200]° longitude range is converted to [+170 … -160]°.</li>
     *       </ul>
     *       See <cite>Spanning the anti-meridian of a Geographic CRS</cite> in the
     *       class javadoc for more information about the meaning of such range.</li>
     * </ul>
     *
     * <div class="section">Spanning the anti-meridian of a Geographic CRS</div>
     * If the envelope is spanning the anti-meridian, then some {@linkplain #getLower(int) lower}
     * ordinate values may become greater than their {@linkplain #getUpper(int) upper} counterpart
     * as a result of this method call. If such effect is undesirable, then this method may be
     * combined with {@link #simplify()} as below:
     *
     * {@preformat java
     *     if (envelope.normalize()) {
     *         envelope.simplify();
     *     }
     * }
     *
     * <div class="section">Choosing the range of longitude values</div>
     * Geographic CRS typically have longitude values in the [-180 … +180]° range, but the [0 … 360]°
     * range is also occasionally used. Callers need to ensure that this envelope CRS is associated
     * to axes having the desired {@linkplain CoordinateSystemAxis#getMinimumValue() minimum} and
     * {@linkplain CoordinateSystemAxis#getMaximumValue() maximum value}.
     *
     * <div class="section">Usage</div>
     * This method is sometime useful before to compute the {@linkplain #add(Envelope) union}
     * or {@linkplain #intersect(Envelope) intersection} of envelopes, in order to ensure that
     * both envelopes are defined in the same domain. This method may also be invoked before
     * to project an envelope, since some projections produce {@link Double#NaN} numbers when
     * given an ordinate value out of bounds.
     *
     * @return {@code true} if this envelope has been modified as a result of this method call,
     *         or {@code false} if no change has been done.
     *
     * @see AbstractDirectPosition#normalize()
     */
    public boolean normalize() {
        boolean changed = false;
        if (crs != null) {
            final int d = ordinates.length >>> 1;
            final int beginIndex = beginIndex();
            final int dimension = endIndex() - beginIndex;
            final CoordinateSystem cs = crs.getCoordinateSystem();
            for (int i=0; i<dimension; i++) {
                final int iLower = beginIndex + i;
                final int iUpper = iLower + d;
                final CoordinateSystemAxis axis = cs.getAxis(i);
                final double  minimum = axis.getMinimumValue();
                final double  maximum = axis.getMaximumValue();
                final RangeMeaning rm = axis.getRangeMeaning();
                if (RangeMeaning.EXACT.equals(rm)) {
                    if (ordinates[iLower] < minimum) {ordinates[iLower] = minimum; changed = true;}
                    if (ordinates[iUpper] > maximum) {ordinates[iUpper] = maximum; changed = true;}
                } else if (RangeMeaning.WRAPAROUND.equals(rm)) {
                    final double csSpan = maximum - minimum;
                    if (csSpan > 0 && csSpan < Double.POSITIVE_INFINITY) {
                        double o1 = ordinates[iLower];
                        double o2 = ordinates[iUpper];
                        if (Math.abs(o2-o1) >= csSpan) {
                            /*
                             * If the range exceed the CS span, then we have to replace it by the
                             * full span, otherwise the range computed by the "else" block is too
                             * small. The full range will typically be [-180 … 180]°.  However we
                             * make a special case if the two bounds are multiple of the CS span,
                             * typically [0 … 360]°. In this case the [0 … -0]° range matches the
                             * original values and is understood by GeneralEnvelope as a range
                             * spanning all the world.
                             */
                            if (o1 != minimum || o2 != maximum) {
                                if ((o1 % csSpan) == 0 && (o2 % csSpan) == 0) {
                                    ordinates[iLower] = +0.0;
                                    ordinates[iUpper] = -0.0;
                                } else {
                                    ordinates[iLower] = minimum;
                                    ordinates[iUpper] = maximum;
                                }
                                changed = true;
                            }
                        } else {
                            o1 = Math.floor((o1 - minimum) / csSpan) * csSpan;
                            o2 = Math.floor((o2 - minimum) / csSpan) * csSpan;
                            if (o1 != 0) {ordinates[iLower] -= o1; changed = true;}
                            if (o2 != 0) {ordinates[iUpper] -= o2; changed = true;}
                        }
                    }
                }
            }
        }
        return changed;
    }

    // Note: As of JDK 1.6.0_31, using {@linkplain #getLower(int)} in the first line crash the
    // Javadoc tools, maybe because getLower/getUpper are defined in a non-public parent class.
    /**
     * Ensures that <var>lower</var> &lt;= <var>upper</var> for every dimensions.
     * If a {@linkplain #getUpper(int) upper ordinate value} is less than a
     * {@linkplain #getLower(int) lower ordinate value}, then there is a choice:
     *
     * <ul>
     *   <li>If the axis has {@link RangeMeaning#WRAPAROUND}, then:<ul>
     *       <li>the lower ordinate value is set to the {@linkplain CoordinateSystemAxis#getMinimumValue() axis minimum value}, and</li>
     *       <li>the upper ordinate value is set to the {@linkplain CoordinateSystemAxis#getMaximumValue() axis maximum value}.</li>
     *     </ul></li>
     *   <li>Otherwise an {@link IllegalStateException} is thrown.</li>
     * </ul>
     *
     * This method is useful when the envelope needs to be used with libraries that do not support
     * envelopes spanning the anti-meridian.
     *
     * @return {@code true} if this envelope has been modified as a result of this method call,
     *         or {@code false} if no change has been done.
     * @throws IllegalStateException If a upper ordinate value is less than a lower ordinate
     *         value on an axis which does not have the {@code WRAPAROUND} range meaning.
     *
     * @see #toSimpleEnvelopes()
     */
    public boolean simplify() throws IllegalStateException {
        boolean changed = false;
        final int d = ordinates.length >>> 1;
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        for (int i=0; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final double lower = ordinates[iLower];
            final double upper = ordinates[iUpper];
            if (isNegative(upper - lower)) {
                final CoordinateSystemAxis axis = getAxis(crs, i);
                if (axis != null && RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning())) {
                    ordinates[iLower] = axis.getMinimumValue();
                    ordinates[iUpper] = axis.getMaximumValue();
                    changed = true;
                } else {
                    throw new IllegalStateException(Errors.format(Errors.Keys.IllegalOrdinateRange_3,
                            lower, upper, (axis != null) ? axis.getName() : i));
                }
            }
        }
        return changed;
    }

    /**
     * Returns a view over this envelope that encompass only some dimensions. The returned object is "live":
     * changes applied on the original envelope is reflected in the sub-envelope view, and conversely.
     *
     * <p>This method is useful for querying and updating only some dimensions.
     * For example in order to expand only the horizontal component of a four dimensional
     * (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>) envelope, one can use:</p>
     *
     * {@preformat java
     *     envelope.subEnvelope(0, 2).add(myPosition2D);
     * }
     *
     * If the sub-envelope needs to be independent from the original envelope, use the following idiom:
     *
     * {@preformat java
     *     GeneralEnvelope copy = envelope.subEnvelope(0, 2).clone();
     * }
     *
     * The sub-envelope is initialized with a {@code null} {@linkplain #getCoordinateReferenceSystem() CRS}.
     * This method does not compute a sub-CRS because it may not be needed, or the sub-CRS may be already
     * known by the caller.
     *
     * @param  beginIndex The index of the first valid ordinate value of the corners.
     * @param  endIndex   The index after the last valid ordinate value of the corners.
     * @return The sub-envelope of dimension {@code endIndex - beginIndex}.
     * @throws IndexOutOfBoundsException If an index is out of bounds.
     *
     * @see org.apache.sis.referencing.CRS#getComponentAt(CoordinateReferenceSystem, int, int)
     */
    // Must be overridden in SubEnvelope
    public GeneralEnvelope subEnvelope(final int beginIndex, final int endIndex) throws IndexOutOfBoundsException {
        ensureValidIndexRange(ordinates.length >>> 1, beginIndex, endIndex);
        return new SubEnvelope(ordinates, beginIndex, endIndex);
        // Do check if we could return "this" as an optimization, in order to keep the
        // method contract simpler (i.e. the returned envelope CRS is always null).
    }

    /**
     * Returns a deep copy of this envelope.
     *
     * @return A clone of this envelope.
     */
    @Override
    public GeneralEnvelope clone() {
        try {
            Field field = ordinatesField;
            if (field == null) {
                ordinatesField = field = GeneralDirectPosition.getOrdinatesField(ArrayEnvelope.class);
            }
            GeneralEnvelope e = (GeneralEnvelope) super.clone();
            field.set(e, ordinates.clone());
            return e;
        } catch (Exception exception) { // (CloneNotSupportedException | ReflectiveOperationException) on JDK7
            // Should not happen, since we are cloneable, the
            // field is known to exist and we made it accessible.
            throw new AssertionError(exception);
        }
    }
}
