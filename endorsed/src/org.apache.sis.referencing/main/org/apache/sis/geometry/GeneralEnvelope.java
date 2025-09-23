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
import java.util.Iterator;
import java.util.Objects;
import java.time.Instant;
import java.io.Serializable;
import java.lang.reflect.Field;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coordinate.MismatchedCoordinateMetadataException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.referencing.internal.shared.TemporalAccessor;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.util.ArgumentCheckByAssertion;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;
import static org.apache.sis.math.MathFunctions.isSameSign;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.math.MathFunctions.isNegativeZero;


/**
 * A mutable {@code Envelope} (a minimum bounding box or rectangle) of arbitrary dimension.
 * Regardless of dimension, an {@code Envelope} can be represented without ambiguity
 * as two {@linkplain AbstractDirectPosition direct positions} (coordinate tuples).
 * To encode an {@code Envelope}, it is sufficient to encode these two points.
 *
 * <p>{@code Envelope} uses an arbitrary <i>Coordinate Reference System</i>, which does not need to be geographic.
 * This is different than the {@code GeographicBoundingBox} class provided in the metadata package, which can be used
 * as a kind of envelope restricted to a Geographic CRS having Greenwich prime meridian.</p>
 *
 * This particular implementation of {@code Envelope} is said "General" because it uses
 * coordinates of an arbitrary number of dimensions. This is in contrast with
 * {@link Envelope2D}, which can use only two-dimensional coordinates.
 *
 * <p>A {@code GeneralEnvelope} can be created in various ways:</p>
 * <ul>
 *   <li>{@linkplain #GeneralEnvelope(int) From a given number of dimension}, with all coordinates initialized to 0.</li>
 *   <li>{@linkplain #GeneralEnvelope(double[], double[]) From two coordinate tuples}.</li>
 *   <li>{@linkplain #GeneralEnvelope(Envelope) From a another envelope} (copy constructor).</li>
 *   <li>{@linkplain #GeneralEnvelope(GeographicBoundingBox) From a geographic bounding box}.</li>
 *   <li>{@linkplain #GeneralEnvelope(CharSequence) From a character sequence}
 *       representing a {@code BBOX} or a <i>Well Known Text</i> (WKT) format.</li>
 * </ul>
 *
 * <h2>Crossing the anti-meridian of a Geographic CRS</h2>
 * The <cite>Web Coverage Service</cite> (WCS) specification authorizes (with special treatment)
 * cases where <var>upper</var> &lt; <var>lower</var> at least in the longitude case. They are
 * envelopes crossing the anti-meridian, like the red box below (the green box is the usual case).
 * The default implementation of methods listed in the right column can handle such cases.
 *
 * <div class="horizontal-flow">
 * <div>
 *   <img style="vertical-align: middle" src="doc-files/AntiMeridian.png" alt="Envelope crossing the anti-meridian">
 * </div><div>
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
 * </div></div>
 *
 * <h2>Envelope validation</h2>
 * If and only if this envelope is associated to a non-null CRS, then constructors and setter methods
 * in this class perform the following checks:
 *
 * <ul>
 *   <li>The number of CRS dimensions must be equal to <code>this.{@linkplain #getDimension()}</code>.</li>
 *   <li>For each dimension <var>i</var>,
 *       <code>{@linkplain #getLower(int) getLower}(i) &gt; {@linkplain #getUpper(int) getUpper}(i)</code> is allowed
 *       only if the {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getRangeMeaning() coordinate
 *       system axis range meaning} is {@code WRAPAROUND}.</li>
 * </ul>
 *
 * Note that this class does <em>not</em> require the coordinate values to be between the axis minimum and
 * maximum values. This flexibility exists because out-of-range values happen in practice, while they do
 * not hurt the working of {@code add(…)}, {@code intersect(…)}, {@code contains(…)} and similar methods.
 * This in contrast with the {@code lower > upper} case, which cause the above-cited methods to behave in
 * an unexpected way if the axis does not have wraparound range meaning.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 *
 * @see Envelope2D
 * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox
 *
 * @since 0.3
 */
public class GeneralEnvelope extends ArrayEnvelope implements Cloneable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3796799507279068254L;

    /**
     * Used for setting the {@link #coordinates} field during a {@link #clone()} operation only.
     * Will be fetch when first needed.
     */
    private static volatile Field coordinatesField;

    /**
     * Creates a new envelope using the given array of coordinate values. This constructor stores
     * the given reference directly; it does <strong>not</strong> clone the given array. This is
     * the desired behavior for proper working of {@link SubEnvelope}.
     *
     * @param coordinates  the array of coordinate values to store directly (not cloned).
     */
    GeneralEnvelope(final double[] coordinates) {
        super(coordinates);
    }

    /**
     * Constructs an envelope defined by two corners given as direct positions.
     * If at least one corner is associated to a CRS, then the new envelope will also
     * be associated to that CRS.
     *
     * @param  lowerCorner  the limits in the direction of decreasing coordinate values for each dimension.
     * @param  upperCorner  the limits in the direction of increasing coordinate values for each dimension.
     * @throws MismatchedDimensionException if the two positions do not have the same dimension.
     * @throws MismatchedCoordinateMetadataException if the CRS of the two position are not equal.
     */
    public GeneralEnvelope(final DirectPosition lowerCorner, final DirectPosition upperCorner)
            throws MismatchedDimensionException, MismatchedCoordinateMetadataException
    {
        super(lowerCorner, upperCorner);
    }

    /**
     * Constructs an envelope defined by two corners given as sequences of coordinate values.
     * The Coordinate Reference System is initially {@code null}.
     *
     * @param  lowerCorner  the limits in the direction of decreasing coordinate values for each dimension.
     * @param  upperCorner  the limits in the direction of increasing coordinate values for each dimension.
     * @throws MismatchedDimensionException if the two sequences do not have the same length.
     */
    public GeneralEnvelope(final double[] lowerCorner, final double[] upperCorner) throws MismatchedDimensionException {
        super(lowerCorner, upperCorner);
    }

    /**
     * Constructs an empty envelope of the specified dimension. All coordinates
     * are initialized to 0 and the coordinate reference system is undefined.
     *
     * @param  dimension  the envelope dimension.
     */
    public GeneralEnvelope(final int dimension) {
        super(dimension);
    }

    /**
     * Constructs an empty envelope with the specified coordinate reference system.
     * All coordinate values are initialized to 0.
     *
     * @param  crs  the coordinate reference system.
     */
    public GeneralEnvelope(final CoordinateReferenceSystem crs) {
        super(crs);
    }

    /**
     * Constructs a new envelope with the same data as the specified envelope.
     *
     * @param envelope  the envelope to copy.
     *
     * @see #castOrCopy(Envelope)
     */
    public GeneralEnvelope(final Envelope envelope) {
        super(envelope);
    }

    /**
     * Constructs a new envelope with the same data as the specified geographic bounding box.
     * The coordinate reference system is set to the
     * {@linkplain org.apache.sis.referencing.CommonCRS#defaultGeographic() default geographic CRS}.
     * Axis order is (<var>longitude</var>, <var>latitude</var>).
     *
     * @param box  the bounding box to copy.
     */
    public GeneralEnvelope(final GeographicBoundingBox box) {
        super(box);
    }

    /**
     * Constructs a new envelope initialized to the values parsed from the given string in
     * {@code BOX} or <i>Well Known Text</i> (WKT) format. The given string is typically
     * a {@code BOX} element like below:
     *
     * {@snippet lang="wkt" :
     *   BOX(-180 -90, 180 90)
     *   }
     *
     * However, this constructor is lenient to other geometry types like {@code POLYGON}.
     * Actually this constructor ignores the geometry type and just applies the following
     * simple rules:
     *
     * <ul>
     *   <li>Character sequences complying to the rules of Java identifiers are skipped.</li>
     *   <li>Coordinates are separated by a coma ({@code ,}) character.</li>
     *   <li>The coordinates in a coordinate tuple are separated by a space.</li>
     *   <li>Coordinate numbers are assumed formatted in US locale.</li>
     *   <li>The coordinate having the highest dimension determines the dimension of this envelope.</li>
     * </ul>
     *
     * This constructor does not check the consistency of the provided text. For example, it does not
     * check that every points in a {@code LINESTRING} have the same dimension. However, this
     * constructor ensures that the parenthesis are balanced, in order to catch some malformed WKT.
     *
     * <h4>Example</h4>
     * The following texts can be parsed by this constructor in addition of the usual {@code BOX} element.
     * This constructor creates the bounding box of those geometries:
     *
     * <ul>
     *   <li>{@code POINT(6 10)}</li>
     *   <li>{@code MULTIPOLYGON(((1 1, 5 1, 1 5, 1 1),(2 2, 3 2, 3 3, 2 2)))}</li>
     *   <li>{@code GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(3 8,7 10))}</li>
     * </ul>
     *
     * @param  wkt  the {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @throws IllegalArgumentException if the given string cannot be parsed.
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
     * @param  envelope  the envelope to cast, or {@code null}.
     * @return the values of the given envelope as a {@code GeneralEnvelope} instance.
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
     * @param  crs  the new coordinate reference system, or {@code null}.
     * @throws MismatchedDimensionException if the specified CRS doesn't have the expected number of dimensions.
     * @throws IllegalStateException if a range of coordinate values in this envelope is compatible with the given CRS.
     *         See <cite>Envelope validation</cite> in class javadoc for more details.
     */
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs)
            throws MismatchedDimensionException
    {
        if (crs != null) {
            ensureDimensionMatches("crs", getDimension(), crs);
            /*
             * The check performed here shall be identical to ArrayEnvelope.verifyRanges(crs, coordinates)
             * except that it may verify only a subset of the coordinate array and throws a different kind
             * of exception in case of failure.
             */
            final int beginIndex = beginIndex();
            final int endIndex = endIndex();
            final int d = coordinates.length >>> 1;
            for (int i=beginIndex; i<endIndex; i++) {
                final double lower = coordinates[i];
                final double upper = coordinates[i + d];
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
     * @param  dimension  the dimension to set.
     * @param  lower      the limit in the direction of decreasing coordinate values.
     * @param  upper      the limit in the direction of increasing coordinate values.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws IllegalArgumentException if {@code lower > upper} and the axis range meaning at the given dimension
     *         is not "wraparound". See <cite>Envelope validation</cite> in class javadoc for more details.
     */
    @Override                                                           // Must also be overridden in SubEnvelope
    public void setRange(final int dimension, final double lower, final double upper)
            throws IndexOutOfBoundsException
    {
        final int d = coordinates.length >>> 1;
        Objects.checkIndex(dimension, d);
        /*
         * The check performed here shall be identical to ArrayEnvelope.verifyRanges(crs, coordinates),
         * except that there is no loop.
         */
        if (lower > upper && crs != null && !isWrapAround(crs, dimension)) {
            throw new IllegalArgumentException(illegalRange(crs, dimension, lower, upper));
        }
        coordinates[dimension + d] = upper;
        coordinates[dimension]     = lower;
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
     * @param corners  coordinates of the new lower corner followed by the new upper corner.
     */
    public void setEnvelope(final double... corners) {
        verifyArrayLength(coordinates.length >>> 1, corners);
        verifyRanges(crs, corners);
        System.arraycopy(corners, 0, coordinates, 0, coordinates.length);
    }

    /**
     * Verifies that the given array of coordinate values has the expected length
     * for the given number of dimensions.
     *
     * @param  dimension  the dimension of the envelope.
     * @param  corners    the user-provided array of coordinate values.
     */
    static void verifyArrayLength(final int dimension, final double[] corners) {
        if ((corners.length & 1) != 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.OddArrayLength_1, corners.length));
        }
        final int d = corners.length >>> 1;
        if (d != dimension) {
            throw new org.opengis.geometry.MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, "coordinates", dimension, d));
        }
    }

    /**
     * Sets this envelope to the same coordinate values as the specified envelope.
     * If the given envelope has a non-null Coordinate Reference System (CRS), then
     * the CRS of this envelope will be set to the CRS of the given envelope.
     *
     * @param  envelope  the envelope to copy coordinates from.
     * @throws MismatchedDimensionException if the specified envelope does not have
     *         the expected number of dimensions.
     */
    public void setEnvelope(final Envelope envelope) throws MismatchedDimensionException {
        if (envelope == this) {
            return;     // Optimization for methods chaining like env.setEnvelope(Envelopes.transform(env, crs))
        }
        Objects.requireNonNull(envelope);
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        ensureDimensionMatches("envelope", dimension, envelope);
        final int d = coordinates.length >>> 1;
        if (envelope instanceof ArrayEnvelope) {
            /*
             * Optimization for a common case. This code path is used by Envelopes.compound(…).
             * The main intent is to avoid the creation of temporary DirectPosition objects.
             */
            final double[] source = ((ArrayEnvelope) envelope).coordinates;
            final int srcOffset = ((ArrayEnvelope) envelope).beginIndex();
            System.arraycopy(source, srcOffset, coordinates, beginIndex, dimension);
            System.arraycopy(source, srcOffset + (source.length >>> 1), coordinates, beginIndex + d, dimension);
        } else {
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            for (int i=0; i<dimension; i++) {
                final int iLower = beginIndex + i;
                final int iUpper = iLower + d;
                coordinates[iLower] = lower.getCoordinate(i);
                coordinates[iUpper] = upper.getCoordinate(i);
            }
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
        final int d = coordinates.length >>> 1;
        Arrays.fill(coordinates, beginIndex,   endIndex,   Double.NEGATIVE_INFINITY);
        Arrays.fill(coordinates, beginIndex+d, endIndex+d, Double.POSITIVE_INFINITY);
    }

    /**
     * Sets all coordinate values to {@linkplain Double#NaN NaN}.
     * The {@linkplain #getCoordinateReferenceSystem() coordinate reference system}
     * (if any) stay unchanged.
     *
     * @see #isAllNaN()
     */
    public void setToNaN() {                   // Must be overridden in SubEnvelope
        Arrays.fill(coordinates, Double.NaN);
        assert isAllNaN() : this;
    }

    /**
     * If this envelope has a temporal component, sets its temporal dimension to the given range.
     * Otherwise this method does nothing. This convenience method converts the given instants to
     * floating point values using {@link org.apache.sis.referencing.crs.DefaultTemporalCRS},
     * then delegates to {@link #setRange(int, double, double)}.
     *
     * <p>Null value means no time limit. More specifically
     * null {@code startTime} is mapped to {@linkplain Double#NEGATIVE_INFINITY −∞} and
     * null {@code endTime}   is mapped to {@linkplain Double#POSITIVE_INFINITY +∞}.
     * This rule makes easy to create <q>is before</q> or <q>is after</q> temporal filters,
     * which can be combined with other envelopes using {@linkplain #intersect(Envelope) intersection}
     * for logical AND, or {@linkplain #add(Envelope) union} for logical OR operations.</p>
     *
     * @param  startTime  the lower temporal value, or {@code null} if unbounded.
     * @param  endTime    the upper temporal value, or {@code null} if unbounded.
     * @return whether the temporal component has been set, or {@code false}
     *         if no temporal dimension has been found in this envelope.
     *
     * @since 1.0
     *
     * @see #getTimeRange()
     * @see Envelopes#toTimeRange(Envelope)
     */
    public boolean setTimeRange(final Instant startTime, final Instant endTime) {
        final TemporalAccessor t = TemporalAccessor.of(crs, 0);
        if (t != null) {
            double lower = t.timeCRS.toValue(startTime);
            double upper = t.timeCRS.toValue(endTime);
            if (Double.isNaN(lower)) lower = Double.NEGATIVE_INFINITY;
            if (Double.isNaN(upper)) upper = Double.POSITIVE_INFINITY;
            setRange(t.dimension, lower, upper);
            return true;
        } else {
            return false;
        }
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
     * @param vector  the translation vector. The length of this array shall be equal to this envelope
     *                {@linkplain #getDimension() dimension}.
     *
     * @since 0.5
     */
    public void translate(final double... vector) {
        Objects.requireNonNull(vector);
        final int beginIndex = beginIndex();
        ensureDimensionMatches("vector", endIndex() - beginIndex, vector);
        final int upperIndex = beginIndex + (coordinates.length >>> 1);
        for (int i=0; i<vector.length; i++) {
            final double t = vector[i];
            coordinates[beginIndex + i] += t;
            coordinates[upperIndex + i] += t;
        }
    }

    /**
     * Adds a point to this envelope. The resulting envelope is the smallest envelope that
     * contains both the original envelope and the specified point.
     *
     * <p>After adding a point, a call to {@link #contains(DirectPosition) contains(DirectPosition)}
     * with the added point as an argument will return {@code true}, except if one of the point
     * coordinates was {@link Double#NaN} in which case the corresponding coordinate has been ignored.</p>
     *
     * <h4>Preconditions</h4>
     * This method assumes that the specified point uses a CRS equivalent to this envelope CRS.
     * For performance reasons, this condition is not verified unless Java assertions are enabled.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports envelopes crossing the anti-meridian. In such cases it is possible to
     * move both envelope borders in order to encompass the given point, as illustrated below (the
     * new point is represented by the {@code +} symbol):
     *
     * <pre class="text">
     *    ─────┐   + ┌─────
     *    ─────┘     └─────</pre>
     *
     * The default implementation moves only the border which is closest to the given point.
     *
     * @param  position  the point to add.
     * @throws MismatchedDimensionException if the given point does not have the expected number of dimensions.
     * @throws AssertionError if assertions are enabled and the envelopes have mismatched CRS.
     */
    @ArgumentCheckByAssertion
    public void add(final DirectPosition position) throws MismatchedDimensionException {
        Objects.requireNonNull(position);
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        ensureDimensionMatches("position", dimension, position);
        assert assertEquals(crs, position.getCoordinateReferenceSystem()) : position;
        final int d = coordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final double value = position.getCoordinate(i);
            final double min = coordinates[iLower];
            final double max = coordinates[iUpper];
            if (!isNegative(max - min)) {                       // Standard case, or NaN.
                if (value < min) coordinates[iLower] = value;
                if (value > max) coordinates[iUpper] = value;
            } else {
                /*
                 * Crossing the anti-meridian. The [max…min] range (note that min/max are interchanged)
                 * is actually an exclusion area. Changes only the closest side.
                 */
                addToClosest(iLower, value, max, min);
            }
        }
        assert contains(position) || isEmpty() || hasNaN(position) : position;
    }

    /**
     * Invoked when a point is added to a range crossing the anti-meridian.
     * In the example below, the new point is represented by the {@code +}
     * symbol. The point is added only on the closest side.
     *
     * <pre class="text">
     *    ─────┐   + ┌─────
     *    ─────┘     └─────</pre>
     *
     * @param  i      the dimension of the coordinate
     * @param  value  the coordinate value to add to this envelope.
     * @param  left   the border on the left side,  which is the <em>max</em> value (yes, this is confusing!)
     * @param  right  the border on the right side, which is the <em>min</em> value (yes, this is confusing!)
     */
    private void addToClosest(int i, final double value, double left, double right) {
        left = value - left;
        if (left > 0) {
            right -= value;
            if (right > 0) {
                if (right > left) {
                    i += (coordinates.length >>> 1);
                }
                coordinates[i] = value;
            }
        }
    }

    /**
     * Adds an envelope object to this envelope.
     * The resulting envelope is the union of the two {@code Envelope} objects.
     *
     * <h4>Preconditions</h4>
     * This method assumes that the specified envelope uses a CRS equivalent to this envelope CRS.
     * For performance reasons, this condition is not verified unless Java assertions are enabled.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports envelopes crossing the anti-meridian. If one or both envelopes cross
     * the anti-meridian, then the result of the {@code add} operation may be an envelope expanding
     * to infinities. In such case, the coordinate range will be either [−∞…∞] or [0…−0] depending on
     * whatever the original range crosses the anti-meridian or not.
     *
     * <h4>Handling of NaN values</h4>
     * {@link Double#NaN} values may be present in any dimension, in the lower coordinate, upper coordinate or both.
     * The behavior of this method in such case depends where the {@code NaN} values appear and whether an envelope
     * spans the anti-meridian:
     *
     * <ul class="verbose">
     *   <li>If this envelope or the given envelope spans anti-meridian in the dimension containing {@code NaN} coordinates,
     *     then this method does not changes the coordinates in that dimension. The rational for such conservative approach
     *     is because union computation depends on whether the other envelope spans anti-meridian too, which is unknown
     *     because at least one envelope bounds is {@code NaN}. Since anti-meridian crossing has been detected in an envelope,
     *     there is suspicion about whether the other envelope could cross anti-meridian too.</li>
     *   <li>Otherwise since the envelope containing real values does not cross anti-meridian in that dimension,
     *     this method assumes that the envelope containing {@code NaN} values does not cross anti-meridian neither.
     *     This assumption is not guaranteed to be true, but cover common cases.
     *     With this assumption in mind:
     *   <ul class="verbose">
     *     <li>All {@code NaN} coordinates in the <em>given</em> envelope are ignored, <i>i.e.</i>
     *       this method does not replace finite coordinates in this envelope by {@code NaN} values from the given envelope.
     *       Note that if only the lower or upper bound is {@code NaN}, the other bound will still
     *       be used for computing union with the assumption described in above paragraph.</li>
     *     <li>All {@code NaN} coordinates in <em>this</em> envelope are left unchanged, <i>i.e.</i> the union will
     *       still contain all the {@code NaN} values that this envelope had before {@code add(Envelope)} invocation.
     *       Note that if only the lower or upper bound is {@code NaN}, the other bound will still
     *       be used for computing union with the assumption described in above paragraph.</li>
     *   </ul></li>
     * </ul>
     *
     * @param  envelope  the {@code Envelope} to add to this envelope.
     * @throws MismatchedDimensionException if the given envelope does not have the expected number of dimensions.
     * @throws AssertionError if assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see Envelopes#union(Envelope...)
     * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#add(GeographicBoundingBox)
     */
    @ArgumentCheckByAssertion
    public void add(final Envelope envelope) throws MismatchedDimensionException {
        Objects.requireNonNull(envelope);
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        ensureDimensionMatches("envelope", dimension, envelope);
        assert assertEquals(crs, envelope.getCoordinateReferenceSystem()) : envelope;
        final DirectPosition lower = envelope.getLowerCorner();
        final DirectPosition upper = envelope.getUpperCorner();
        final int d = coordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final double min0 = coordinates[iLower];
            final double max0 = coordinates[iUpper];
            final double min1 = lower.getCoordinate(i);
            final double max1 = upper.getCoordinate(i);
            final boolean sp0 = isNegative(max0 - min0);
            final boolean sp1 = isNegative(max1 - min1);
            if (sp0 == sp1) {
                /*
                 * Standard case (for rows in the above pictures), or case where both envelopes
                 * cross the anti-meridian (which is almost the same with an additional post-add
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
                if (min1 < min0) coordinates[iLower] = min1;
                if (max1 > max0) coordinates[iUpper] = max1;
                if (!sp0 || isNegativeUnsafe(coordinates[iUpper] - coordinates[iLower])) {
                    continue;               // We are done, go to the next dimension.
                }
                // If we were crossing the anti-meridian before the union but
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
                    if (left > right) coordinates[iLower] = min1;
                    if (right > left) coordinates[iUpper] = max1;     // This is the case illustrated above.
                    continue;                                         // We are done, go to the next dimension.
                }
                // If we reach this point, the given envelope fills completly the "exclusion area"
                // of this envelope. As a consequence this envelope is now spanning to infinities.
                // We will set that fact close to the end of this loop.
            } else {
                /*
                 * Opposite of above case: this envelope is "normal" or has NaN values,
                 * and the given envelope spans to infinities.
                 */
                if (max0 <= max1 || min0 >= min1) {
                    coordinates[iLower] = min1;
                    coordinates[iUpper] = max1;
                    continue;
                }
                final double left  = min0 - max1;
                final double right = min1 - max0;
                if (left > 0 || right > 0) {
                    if (left > right) coordinates[iUpper] = max1;
                    if (right > left) coordinates[iLower] = min1;
                    continue;
                }
            }
            /*
             * If we reach that point, we went in one of the many cases where the envelope
             * has been expanded to infinity.  Declares an infinite range while preserving
             * the "normal" / "anti-meridian crossing" state.
             */
            if (sp0) {
                coordinates[iLower] = +0.0;
                coordinates[iUpper] = -0.0;
            } else {
                coordinates[iLower] = Double.NEGATIVE_INFINITY;
                coordinates[iUpper] = Double.POSITIVE_INFINITY;
            }
        }
        assert contains(envelope) || isEmpty() || hasNaN(envelope) : this;
    }

    /**
     * Sets this envelope to the intersection of this envelope with the specified one.
     *
     * <h4>Preconditions</h4>
     * This method assumes that the specified envelope uses a CRS equivalent to this envelope CRS.
     * For performance reasons, this condition is not verified unless Java assertions are enabled.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports envelopes crossing the anti-meridian.
     *
     * <h4>Handling of NaN values</h4>
     * {@link Double#NaN} values may be present in any dimension, in the lower coordinate, upper coordinate or both.
     * The behavior of this method in such case depends where the {@code NaN} values appear and whether an envelope
     * spans the anti-meridian:
     *
     * <ul class="verbose">
     *   <li>If this envelope or the given envelope spans anti-meridian in the dimension containing {@code NaN} coordinates,
     *     then this method does not changes the coordinates in that dimension. The rational for such conservative approach
     *     is because intersection computation depends on whether the other envelope spans anti-meridian too, which is unknown
     *     because at least one envelope bounds is {@code NaN}. Since anti-meridian crossing has been detected in an envelope,
     *     there is suspicion about whether the other envelope could cross anti-meridian too.</li>
     *   <li>Otherwise since the envelope containing real values does not cross anti-meridian in that dimension,
     *     this method assumes that the envelope containing {@code NaN} values does not cross anti-meridian neither.
     *     This assumption is not guaranteed to be true, but cover common cases.
     *     With this assumption in mind:
     *   <ul class="verbose">
     *     <li>All {@code NaN} coordinates in the <em>given</em> envelope are ignored, <i>i.e.</i>
     *       this method does not replace finite coordinates in this envelope by {@code NaN} values from the given envelope.
     *       Note that if only the lower or upper bound is {@code NaN}, the other bound will still
     *       be used for computing intersection with the assumption described in above paragraph.</li>
     *     <li>All {@code NaN} coordinates in <em>this</em> envelope are left unchanged, <i>i.e.</i> the intersection will
     *       still contain all the {@code NaN} values that this envelope had before {@code intersect(Envelope)} invocation.
     *       Note that if only the lower or upper bound is {@code NaN}, the other bound will still
     *       be used for computing intersection with the assumption described in above paragraph.</li>
     *   </ul></li>
     * </ul>
     *
     * {@link Double#NaN} coordinates may appear as a result of intersection, even if such values were not present
     * in any source envelopes, if the two envelopes do not intersect in some dimensions.
     *
     * @param  envelope  the {@code Envelope} to intersect to this envelope.
     * @throws MismatchedDimensionException if the given envelope does not have the expected number of dimensions.
     * @throws AssertionError if assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see Envelopes#intersect(Envelope...)
     * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#intersect(GeographicBoundingBox)
     */
    @ArgumentCheckByAssertion
    public void intersect(final Envelope envelope) throws MismatchedDimensionException {
        Objects.requireNonNull(envelope);
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        ensureDimensionMatches("envelope", dimension, envelope);
        assert assertEquals(crs, envelope.getCoordinateReferenceSystem()) : envelope;
        final DirectPosition lower = envelope.getLowerCorner();
        final DirectPosition upper = envelope.getUpperCorner();
        final int d = coordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final double min0  = coordinates[iLower];
            final double max0  = coordinates[iUpper];
            final double min1  = lower.getCoordinate(i);
            final double max1  = upper.getCoordinate(i);
            final double span0 = max0 - min0;
            final double span1 = max1 - min1;
            if (isSameSign(span0, span1)) {                 // Always `false` if any value is NaN.
                /*
                 * First, verify that the two envelopes intersect.
                 *     ┌──────────┐             ┌─────────────┐
                 *     │  ┌───────┼──┐    or    │  ┌───────┐  │
                 *     │  └───────┼──┘          │  └───────┘  │
                 *     └──────────┘             └─────────────┘
                 */
                if ((min1 > max0 || max1 < min0) && !isNegativeUnsafe(span0)) {
                    /*
                     * The check for !isNegative(span0) is because if both envelopes cross the
                     * anti-merdian, then there is always an intersection on both side no matter
                     * what envelope coordinates are because both envelopes extend toward infinities:
                     *     ────┐  ┌────            ────┐  ┌────
                     *     ──┐ │  │ ┌──     or     ────┼──┼─┐┌─
                     *     ──┘ │  │ └──            ────┼──┼─┘└─
                     *     ────┘  └────            ────┘  └────
                     * Since we excluded the above case, entering in this block means that the
                     * envelopes are "normal" and do not intersect, so we set coordinates to NaN.
                     *   ┌────┐
                     *   │    │     ┌────┐
                     *   │    │     └────┘
                     *   └────┘
                     */
                    coordinates[iLower] = coordinates[iUpper] = Double.NaN;
                    continue;
                }
            } else if (!Double.isNaN(span0) && !Double.isNaN(span1)) {
                int intersect = 0;                          // A bitmask of intersections (two bits).
                if (isNegativeUnsafe(span0)) {
                    /*
                     * The first line below checks for the case illustrated below. The second
                     * line does the same check, but with the small rectangle on the right side.
                     *    ─────┐      ┌─────              ──────────┐  ┌─────
                     *       ┌─┼────┐ │           or        ┌────┐  │  │
                     *       └─┼────┘ │                     └────┘  │  │
                     *    ─────┘      └─────              ──────────┘  └─────
                     */
                    if (min1 <= max0) {intersect  = 1; coordinates[iLower] = min1;}
                    if (max1 >= min0) {intersect |= 2; coordinates[iUpper] = max1;}
                } else {
                    // Same as above, but with indices 0 and 1 interchanged.
                    // No need to set coordinate values since they would be the same.
                    if (min0 <= max1) {intersect  = 1;}
                    if (max0 >= min1) {intersect |= 2;}
                }
                /*
                 * Cases 0 and 3 are illustrated below. In case 1 and 2, we will set
                 * only the coordinate value which has not been set by the above code.
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
                    case 1: if (max1 < max0) coordinates[iUpper] = max1; continue;
                    case 2: if (min1 > min0) coordinates[iLower] = min1; continue;
                    case 3: // Fall through
                    case 0: {
                        /*
                         * Before to declare the intersection as invalid, verify if the envelope actually spans
                         * the whole Earth. In such case, the intersection is a no-operation (or a copy operation).
                         */
                        final double min, max;
                        final double cycle = getCycle(getAxis(crs, i));
                        if (span1 >= cycle || isNegativeZero(span1)) {        // Negative zero if [+0 … -0] range.
                            min = min0;
                            max = max0;
                        } else if (span0 >= cycle || isNegativeZero(span0)) {
                            min = min1;
                            max = max1;
                        } else {
                            min = Double.NaN;
                            max = Double.NaN;
                        }
                        coordinates[iLower] = min;
                        coordinates[iUpper] = max;
                        continue;
                    }
                }
            } else {
                /*
                 * We reach this point only if at least one value is NaN. It may be in this envelope or in the given envelope.
                 * If one of the two envelopes spans the anti-meridian in current dimension, do nothing for the reasons given
                 * in method javadoc. Otherwise process the non-NaN coordinates like ordinary envelopes, ignoring NaN.
                 */
                if (isNegative(span0) || isNegative(span1)) {
                    continue;
                }
            }
            if (min1 > min0) coordinates[iLower] = min1;
            if (max1 < max0) coordinates[iUpper] = max1;
        }
        // Tests only if the interection result is non-empty.
        assert isEmpty() || hasNaN(envelope) || AbstractEnvelope.castOrCopy(envelope).contains(this) : this;
    }

    /**
     * Ensures that the envelope is contained inside the coordinate system domain.
     * For each dimension, this method compares the coordinate values against the
     * limits of the coordinate system axis for that dimension.
     * If some coordinates are out of range, then there is a choice depending on the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() axis range meaning}:
     *
     * <ul class="verbose">
     *   <li>If {@link RangeMeaning#EXACT} (typically <em>latitudes</em> coordinates), then values
     *       greater than the {@linkplain CoordinateSystemAxis#getMaximumValue() axis maximal value}
     *       are replaced by the axis maximum, and values smaller than the
     *       {@linkplain CoordinateSystemAxis#getMinimumValue() axis minimal value}
     *       are replaced by the axis minimum.</li>
     *
     *   <li>If {@link RangeMeaning#WRAPAROUND} (typically <em>longitudes</em> coordinates), then
     *       a multiple of the axis range (e.g. 360° for longitudes) is added or subtracted.
     *       Example:
     *       <ul>
     *         <li>the [190 … 200]° longitude range is converted to [-170 … -160]°,</li>
     *         <li>the [170 … 200]° longitude range is converted to [+170 … -160]°.</li>
     *       </ul>
     *       See <cite>Crossing the anti-meridian of a Geographic CRS</cite> in the
     *       class javadoc for more information about the meaning of such range.</li>
     * </ul>
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * If the envelope is crossing the anti-meridian, then some {@linkplain #getLower(int) lower}
     * coordinate values may become greater than their {@linkplain #getUpper(int) upper} counterpart
     * as a result of this method call. If such effect is undesirable, then this method may be
     * combined with {@link #simplify()} as below:
     *
     * {@snippet lang="java" :
     *     if (envelope.normalize()) {
     *         envelope.simplify();
     *     }
     *     }
     *
     * <h4>Choosing the range of longitude values</h4>
     * Geographic CRS typically have longitude values in the [-180 … +180]° range, but the [0 … 360]°
     * range is also occasionally used. Callers need to ensure that this envelope CRS is associated
     * to axes having the desired {@linkplain CoordinateSystemAxis#getMinimumValue() minimum} and
     * {@linkplain CoordinateSystemAxis#getMaximumValue() maximum value}.
     *
     * <h4>Usage</h4>
     * This method is sometimes useful before to compute the {@linkplain #add(Envelope) union}
     * or {@linkplain #intersect(Envelope) intersection} of envelopes, in order to ensure that
     * both envelopes are defined in the same domain. This method may also be invoked before
     * to project an envelope, since some projections produce {@link Double#NaN} numbers when
     * given an coordinate value out of bounds.
     *
     * @return {@code true} if this envelope has been modified as a result of this method call,
     *         or {@code false} if no change has been done.
     *
     * @see AbstractDirectPosition#normalize()
     * @see WraparoundMethod#NORMALIZE
     */
    public boolean normalize() {
        if (crs == null) {
            return false;
        }
        final int beginIndex = beginIndex();
        return normalize(crs.getCoordinateSystem(), beginIndex, endIndex() - beginIndex, null);
    }

    /**
     * Normalizes only the dimensions returned by the given iterator, or all dimensions if the iterator is null.
     * This is used for normalizing the result of a coordinate operation where a wrap around axis does not
     * necessarily means that the coordinates need to be normalized along that axis.
     *
     * @param  cs          the coordinate system of this envelope CRS (as an argument because sometimes already known).
     * @param  beginIndex  index of the first coordinate value in {@link #coordinates} array. Non-zero for sub-envelopes.
     * @param  count       number of coordinates, i.e. this envelope dimensions.
     * @param  dimensions  the dimensions to check for normalization, or {@code null} for all dimensions.
     * @return {@code true} if this envelope has been modified as a result of this method call.
     */
    final boolean normalize(final CoordinateSystem cs, final int beginIndex, final int count, final Iterator<Integer> dimensions) {
        boolean changed = false;
        final int d = coordinates.length >>> 1;
        for (int j=0; j<count; j++) {
            final int i = (dimensions != null) ? dimensions.next() : j;
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final CoordinateSystemAxis axis = cs.getAxis(i);
            final double  minimum = axis.getMinimumValue();
            final double  maximum = axis.getMaximumValue();
            final RangeMeaning rm = axis.getRangeMeaning();
            if (rm == RangeMeaning.EXACT) {
                if (coordinates[iLower] < minimum) {coordinates[iLower] = minimum; changed = true;}
                if (coordinates[iUpper] > maximum) {coordinates[iUpper] = maximum; changed = true;}
            } else if (rm == RangeMeaning.WRAPAROUND) {
                final double cycle = maximum - minimum;
                if (cycle > 0 && cycle < Double.POSITIVE_INFINITY) {
                    double o1 = coordinates[iLower];
                    double o2 = coordinates[iUpper];
                    if (Math.abs(o2-o1) >= cycle) {
                        /*
                         * If the range exceed the CS span, then we have to replace it by the
                         * full span, otherwise the range computed by the "else" block is too
                         * small. The full range will typically be [-180 … 180]°. However, we
                         * make a special case if the two bounds are multiple of the CS span,
                         * typically [0 … 360]°. In this case the [0 … -0]° range matches the
                         * original values and is understood by GeneralEnvelope as a range
                         * spanning all the world.
                         */
                        if (o1 != minimum || o2 != maximum) {
                            if ((o1 % cycle) == 0 && (o2 % cycle) == 0) {
                                coordinates[iLower] = +0.0;
                                coordinates[iUpper] = -0.0;
                            } else {
                                coordinates[iLower] = minimum;
                                coordinates[iUpper] = maximum;
                            }
                            changed = true;
                        }
                    } else {
                        o1 = Math.floor((o1 - minimum) / cycle) * cycle;
                        o2 = Math.floor((o2 - minimum) / cycle) * cycle;
                        if (o1 != 0) {coordinates[iLower] -= o1; changed = true;}
                        if (o2 != 0) {coordinates[iUpper] -= o2; changed = true;}
                    }
                }
            }
        }
        return changed;
    }

    /**
     * Ensures that <var>lower</var> ≤ <var>upper</var> for every dimensions.
     * If a {@linkplain #getUpper(int) upper coordinate value} is less than a
     * {@linkplain #getLower(int) lower coordinate value}, then there is a choice:
     *
     * <ul>
     *   <li>If the axis has {@link RangeMeaning#WRAPAROUND}, then:<ul>
     *       <li>the lower coordinate value is set to the {@linkplain CoordinateSystemAxis#getMinimumValue() axis minimum value}, and</li>
     *       <li>the upper coordinate value is set to the {@linkplain CoordinateSystemAxis#getMaximumValue() axis maximum value}.</li>
     *     </ul></li>
     *   <li>Otherwise an {@link IllegalStateException} is thrown.</li>
     * </ul>
     *
     * This method is useful when the envelope needs to be used with libraries that do not support
     * envelopes crossing the anti-meridian.
     *
     * @return {@code true} if this envelope has been modified as a result of this method call,
     *         or {@code false} if no change has been done.
     * @throws IllegalStateException if a upper coordinate value is less than a lower coordinate
     *         value on an axis which does not have the {@code WRAPAROUND} range meaning.
     *
     * @see #toSimpleEnvelopes()
     * @see WraparoundMethod#EXPAND
     */
    public boolean simplify() throws IllegalStateException {
        return apply(WraparoundMethod.EXPAND);
    }

    /**
     * If this envelope is crossing the limit of a wraparound axis, modifies coordinates by application
     * of the specified strategy. This applies typically to longitude values crossing the anti-meridian,
     * but other kinds of wraparound axes may also exist. Possible values are listed below.
     *
     * <table class="sis">
     *   <caption>Legal argument values</caption>
     *   <tr><th>Value</th><th>Action</th></tr>
     *   <tr><td>{@link WraparoundMethod#NONE}:</td>             <td>Do nothing and return {@code false}.</td></tr>
     *   <tr><td>{@link WraparoundMethod#NORMALIZE}:</td>        <td>Delegate to {@link #normalize()}.</td></tr>
     *   <tr><td>{@link WraparoundMethod#EXPAND}:</td>           <td>Equivalent to {@link #simplify()}.</td></tr>
     *   <tr><td>{@link WraparoundMethod#CONTIGUOUS}:</td>       <td>See enumeration javadoc.</td></tr>
     *   <tr><td>{@link WraparoundMethod#CONTIGUOUS_LOWER}:</td> <td>See enumeration javadoc.</td></tr>
     *   <tr><td>{@link WraparoundMethod#CONTIGUOUS_UPPER}:</td> <td>See enumeration javadoc.</td></tr>
     *   <tr><td>{@link WraparoundMethod#SPLIT}:</td>            <td>Throw {@link IllegalArgumentException}.</td></tr>
     * </table>
     *
     * @param  method  the strategy to use for representing a region crossing the anti-meridian or other wraparound limit.
     * @return {@code true} if this envelope has been modified as a result of this method call,
     *         or {@code false} if no change has been done.
     * @throws IllegalStateException if a upper coordinate value is less than a lower coordinate
     *         value on an axis which does not have the {@code WRAPAROUND} range meaning.
     *
     * @since 1.1
     */
    public boolean wraparound(final WraparoundMethod method) throws IllegalStateException {
        switch (method) {
            case EXPAND:
            case CONTIGUOUS:
            case CONTIGUOUS_LOWER:
            case CONTIGUOUS_UPPER: return apply(method);
            case NORMALIZE:        return normalize();
            case NONE:             return false;
            default: throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "method", method));
        }
    }

    /**
     * Implementation of {@link #simplify()} and {@link #wraparound(WraparoundMethod)}.
     * Argument can be only {@link WraparoundMethod#EXPAND} or {@code CONTIGUOUS*}.
     */
    private boolean apply(final WraparoundMethod method) throws IllegalStateException {
        boolean changed = false;
        final int d = coordinates.length >>> 1;
        final int beginIndex = beginIndex();
        final int dimension = endIndex() - beginIndex;
        for (int i=0; i<dimension; i++) {
            final int iLower = beginIndex + i;
            final int iUpper = iLower + d;
            final double lower = coordinates[iLower];
            final double upper = coordinates[iUpper];
            if (isNegative(upper - lower)) {                        // Use `isNegative(…)` for catching [+0 … -0] range.
                final CoordinateSystemAxis axis = getAxis(crs, i);
                if (isWrapAround(axis)) {
                    changed = true;
                    final double minimum = axis.getMinimumValue();
                    final double maximum = axis.getMaximumValue();
                    if (method != WraparoundMethod.EXPAND) {
                        double cycle = maximum - minimum;
                        if (Double.isFinite(cycle)) {
                            cycle *= Math.ceil((lower - upper) / cycle);
                            boolean up = (method == WraparoundMethod.CONTIGUOUS_UPPER);
                            if (!up && method != WraparoundMethod.CONTIGUOUS_LOWER) {
                                up = (upper - minimum <= maximum - lower);
                            }
                            if (up) coordinates[iUpper] += cycle;
                            else    coordinates[iLower] -= cycle;
                            continue;
                        }
                    }
                    coordinates[iLower] = minimum;
                    coordinates[iUpper] = maximum;
                } else {
                    throw new IllegalStateException(Errors.format(Errors.Keys.IllegalCoordinateRange_3,
                            lower, upper, (axis != null) ? axis.getName() : i));
                }
            }
        }
        return changed;
    }

    /**
     * Returns a view over the two horizontal dimensions of this envelope. The horizontal dimensions are
     * {@linkplain CRS#getHorizontalComponent(CoordinateReferenceSystem) inferred from the CRS}. If this
     * method cannot infer the horizontal dimensions, then an {@link IllegalStateException} is thrown.
     *
     * <p>The returned envelope is a <em>view</em>: changes in the returned envelope are reflected in this
     * envelope, and conversely. The returned envelope will have its CRS defined.</p>
     *
     * @return a view over the horizontal components of this envelope. May be {@code this}.
     * @throws IllegalStateException if this method cannot infer the horizontal components of this envelope.
     *
     * @see #subEnvelope(int, int)
     * @see CRS#getHorizontalComponent(CoordinateReferenceSystem)
     *
     * @since 1.1
     */
    public GeneralEnvelope horizontal() throws IllegalStateException {
        if (crs == null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnspecifiedCRS));
        }
        final int tgtDim = 2;
        final int dimension = getDimension();
        if (dimension >= tgtDim) {
            final CoordinateReferenceSystem singleCRS = CRS.getHorizontalComponent(crs);
            if (singleCRS == crs) {
                return this;
            }
            if (singleCRS != null) {
                final int i = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), singleCRS.getCoordinateSystem());
                if (i >= 0) {
                    final GeneralEnvelope sub = subEnvelope(i, i+tgtDim);
                    sub.setCoordinateReferenceSystem(singleCRS);
                    return sub;
                }
            }
        }
        String name = IdentifiedObjects.getDisplayName(crs, null);
        if (name == null) name = Integer.toString(dimension) + 'D';
        throw new IllegalStateException(Errors.format(Errors.Keys.NonHorizontalCRS_1, name));
    }

    /*
     * We do not provide vertical() and temporal() methods at this time. The interest of one-dimensional envelopes
     * is not obvious. Furthermore, in the vertical case it is not clear when we should do about ellipsoidal height,
     * and in the temporal case what we should do with envelopes having 2 temporal axes (as seen in meteorological
     * data). Should we return the two temporal axes in two-dimensional envelopes?
     */

    /**
     * Returns a view over this envelope that encompass only some dimensions. The returned object is "live":
     * changes applied on the original envelope is reflected in the sub-envelope view, and conversely.
     *
     * <p>This method is useful for querying and updating only some dimensions.
     * For example, in order to expand only the horizontal component of a four dimensional
     * (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>) envelope, one can use:</p>
     *
     * {@snippet lang="java" :
     *     envelope.subEnvelope(0, 2).add(myPosition2D);
     *     }
     *
     * If the sub-envelope needs to be independent from the original envelope, use the following idiom:
     *
     * {@snippet lang="java" :
     *     GeneralEnvelope copy = envelope.subEnvelope(0, 2).clone();
     *     }
     *
     * The sub-envelope is initialized with a {@code null} {@linkplain #getCoordinateReferenceSystem() CRS}.
     * This method does not compute a sub-CRS because it may not be needed, or the sub-CRS may be already
     * known by the caller.
     *
     * @param  beginIndex  the index of the first valid coordinate value of the corners.
     * @param  endIndex    the index after the last valid coordinate value of the corners.
     * @return the sub-envelope of dimension {@code endIndex - beginIndex}.
     * @throws IndexOutOfBoundsException if an index is out of bounds.
     *
     * @see #horizontal()
     * @see org.apache.sis.referencing.CRS#getComponentAt(CoordinateReferenceSystem, int, int)
     */
    // Must be overridden in SubEnvelope
    public GeneralEnvelope subEnvelope(final int beginIndex, final int endIndex) throws IndexOutOfBoundsException {
        Objects.checkFromToIndex(beginIndex, endIndex, coordinates.length >>> 1);
        return new SubEnvelope(coordinates, beginIndex, endIndex);
        /*
         * Do not check if we could return "this" as an optimization, in order to keep
         * the method contract simpler (i.e. the returned envelope CRS is always null).
         */
    }

    /**
     * Returns a deep copy of this envelope.
     *
     * @return a clone of this envelope.
     */
    @Override
    public GeneralEnvelope clone() {
        try {
            Field field = coordinatesField;
            if (field == null) {
                coordinatesField = field = GeneralDirectPosition.getCoordinatesField(ArrayEnvelope.class);
            }
            GeneralEnvelope e = (GeneralEnvelope) super.clone();
            field.set(e, coordinates.clone());
            return e;
        } catch (CloneNotSupportedException | ReflectiveOperationException exception) {
            /*
             * Should not happen, since we are cloneable, the
             * field is known to exist and we made it accessible.
             */
            throw new AssertionError(exception);
        }
    }
}
