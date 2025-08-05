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
import java.util.Objects;
import java.util.Optional;
import java.time.Instant;
import java.io.Serializable;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.util.ArgumentCheckByAssertion;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.referencing.privy.TemporalAccessor;
import org.apache.sis.measure.Range;
import org.apache.sis.math.Vector;

import static java.lang.Double.doubleToLongBits;
import static org.apache.sis.util.privy.Numerics.SIGN_BIT_MASK;
import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;
import static org.apache.sis.util.StringBuilders.trimFractionalPart;
import static org.apache.sis.math.MathFunctions.epsilonEqual;
import static org.apache.sis.math.MathFunctions.isPositive;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.math.MathFunctions.isNegativeZero;


/**
 * Default implementations of most {@code Envelope} methods, leaving the data storage to subclasses.
 * This base class does not hold any state and does not implement the {@link java.io.Serializable}
 * or {@link Cloneable} interfaces. The internal representation, and the choice to be cloneable or
 * serializable, is left to subclasses.
 *
 * <p>Implementers needs to define at least the following methods:</p>
 * <ul>
 *   <li>{@link #getDimension()}</li>
 *   <li>{@link #getCoordinateReferenceSystem()}</li>
 *   <li>{@link #getLower(int)}</li>
 *   <li>{@link #getUpper(int)}</li>
 * </ul>
 *
 * <p>All other methods, including {@link #toString()}, {@link #equals(Object)} and {@link #hashCode()},
 * are implemented on top of the above four methods.</p>
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
 *   <li>{@link #toSimpleEnvelopes()}</li>
 *   <li>{@link #contains(DirectPosition)}</li>
 *   <li>{@link #contains(Envelope)}</li>
 *   <li>{@link #intersects(Envelope)}</li>
 * </ul>
 * </div></div>
 *
 * <h2>Choosing the range of longitude values</h2>
 * Geographic CRS typically have longitude values in the [-180 … +180]° range, but the [0 … 360]°
 * range is also occasionally used. Users of this class need to ensure that this envelope CRS is
 * associated to axes having the desired {@linkplain CoordinateSystemAxis#getMinimumValue() minimum}
 * and {@linkplain CoordinateSystemAxis#getMaximumValue() maximum value}.
 *
 * <h2>Note on positive and negative zeros</h2>
 * The IEEE 754 standard defines two different values for positive zero and negative zero.
 * When used with SIS envelopes and keeping in mind the above discussion, those zeros have
 * different meanings:
 *
 * <ul>
 *   <li>The [-0…0]° range is an empty envelope.</li>
 *   <li>The [0…-0]° range makes a full turn around the globe, like the [-180…180]°
 *       range except that the former range spans across the anti-meridian.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.3
 */
@XmlTransient
public abstract class AbstractEnvelope extends FormattableObject implements Envelope, Emptiable {
    /**
     * An empty array of envelopes, to be returned by {@link #toSimpleEnvelopes()}
     * when en envelope is empty.
     */
    private static final Envelope[] EMPTY = new Envelope[0];

    /**
     * Constructs an envelope.
     */
    protected AbstractEnvelope() {
    }

    /**
     * Returns the given envelope as an {@code AbstractEnvelope} instance.
     * If the given envelope is already an instance of {@code AbstractEnvelope},
     * then it is returned unchanged. Otherwise the coordinate values and the CRS
     * of the given envelope are copied in a new envelope.
     *
     * @param  envelope  the envelope to cast, or {@code null}.
     * @return the values of the given envelope as an {@code AbstractEnvelope} instance.
     *
     * @see GeneralEnvelope#castOrCopy(Envelope)
     * @see ImmutableEnvelope#castOrCopy(Envelope)
     */
    public static AbstractEnvelope castOrCopy(final Envelope envelope) {
        if (envelope == null || envelope instanceof AbstractEnvelope) {
            return (AbstractEnvelope) envelope;
        }
        return new GeneralEnvelope(envelope);
    }

    /**
     * Weakly asserts that the given CRS are equal. This method may return {@code false} without throwing
     * {@link AssertionError}, so callers should still test the returned value in their {@code assert} statement.
     */
    static boolean assertEquals(final CoordinateReferenceSystem expected, final CoordinateReferenceSystem actual) {
        if (equals(expected, actual, ComparisonMode.DEBUG)) return true;
        final String title = IdentifiedObjects.getDisplayName(actual, null);
        if (title != null && !title.equalsIgnoreCase(IdentifiedObjects.getDisplayName(expected, null))) {
            throw new AssertionError(Errors.format(Errors.Keys.IllegalCoordinateSystem_1, title));
        }
        // Error message not informative enough. Let caller makes its own.
        return false;
    }

    /**
     * Returns {@code true} if at least one of the specified CRS is null, or both CRS are approximately equals.
     * This special processing for {@code null} values is different than the usual contract of {@code equals} method,
     * but allows to handle the case where the CRS is unknown.
     */
    private static boolean equals(final CoordinateReferenceSystem crs1,
                                  final CoordinateReferenceSystem crs2, final ComparisonMode mode)
    {
        return (crs1 == null) || (crs2 == null) || Utilities.deepEquals(crs1, crs2, mode);
    }

    /**
     * Returns the common CRS of specified points.
     *
     * @param  lowerCorner  the first position.
     * @param  upperCorner  the second position.
     * @return their common CRS, or {@code null} if none.
     * @throws MismatchedReferenceSystemException if the two positions don't use equal CRS.
     */
    static CoordinateReferenceSystem getCommonCRS(final DirectPosition lowerCorner,
                                                  final DirectPosition upperCorner)
            throws MismatchedReferenceSystemException
    {
        final CoordinateReferenceSystem crs1 = lowerCorner.getCoordinateReferenceSystem();
        final CoordinateReferenceSystem crs2 = upperCorner.getCoordinateReferenceSystem();
        if (crs1 == null) {
            return crs2;
        } else {
            if (crs2 != null && !crs1.equals(crs2)) {
                throw new MismatchedReferenceSystemException(Errors.format(Errors.Keys.MismatchedCRS));
            }
            return crs1;
        }
    }

    /**
     * Returns the axis of the given coordinate reference system for the given dimension,
     * or {@code null} if none.
     *
     * @param  crs        the envelope CRS, or {@code null}.
     * @param  dimension  the dimension for which to get the axis.
     * @return the axis at the given dimension, or {@code null}.
     */
    static CoordinateSystemAxis getAxis(final CoordinateReferenceSystem crs, final int dimension) {
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs != null) {                                       // Paranoiac check (should never be null).
                return cs.getAxis(dimension);
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the axis for the given dimension has the
     * {@link RangeMeaning#WRAPAROUND WRAPAROUND} range meaning.
     *
     * @param  crs        the envelope CRS, or {@code null}.
     * @param  dimension  the dimension for which to get the axis.
     * @return {@code true} if the range meaning is {@code WRAPAROUND}.
     */
    static boolean isWrapAround(final CoordinateReferenceSystem crs, final int dimension) {
        return isWrapAround(getAxis(crs, dimension));
    }

    /**
     * Returns {@code true} if the given axis is non-null and has the
     * {@link RangeMeaning#WRAPAROUND WRAPAROUND} range meaning.
     *
     * @param  axis  the axis to test, or {@code null}.
     * @return {@code true} if the range meaning is {@code WRAPAROUND}.
     *
     * @see org.apache.sis.referencing.privy.CoordinateOperations#isWrapAround(CoordinateSystemAxis)
     */
    static boolean isWrapAround(final CoordinateSystemAxis axis) {
        return (axis != null) && axis.getRangeMeaning() == RangeMeaning.WRAPAROUND;
    }

    /**
     * If the range meaning of the given axis is "wraparound", returns the spanning of that axis.
     * Otherwise returns {@link Double#NaN}.
     *
     * @param  axis  the axis for which to get the spanning.
     * @return the spanning of the given axis.
     */
    static double getCycle(final CoordinateSystemAxis axis) {
        if (isWrapAround(axis)) {
            return axis.getMaximumValue() - axis.getMinimumValue();
        }
        return Double.NaN;
    }

    /**
     * Returns {@code true} if the given value is negative, without checks for {@code NaN}.
     * This method should be invoked only when the number is known to not be {@code NaN},
     * otherwise the safer {@link org.apache.sis.math.MathFunctions#isNegative(double)} method
     * shall be used instead. Note that the check for {@code NaN} doesn't need to be explicit.
     * For example in the following code, {@code NaN} values were implicitly checked by
     * the {@code (a < b)} comparison:
     *
     * {@snippet lang="java" :
     *     if (a < b && isNegativeUnsafe(a)) {
     *         // ... do some stuff
     *     }
     *     }
     */
    static boolean isNegativeUnsafe(final double value) {
        return (Double.doubleToRawLongBits(value) & SIGN_BIT_MASK) != 0;
    }

    /**
     * A coordinate position consisting of all the lower coordinate values.
     * The default implementation returns a view over the {@link #getLower(int)} method,
     * so changes in this envelope will be immediately reflected in the returned direct position.
     * If the particular case of the {@code GeneralEnvelope} subclass, the returned position
     * supports also {@linkplain DirectPosition#setOrdinate(int, double) write operations},
     * so changes in the position are reflected back in the envelope.
     *
     * <h4>Note on wraparound</h4>
     * The <cite>Web Coverage Service</cite> (WCS) 1.1 specification uses an extended interpretation of the
     * bounding box definition. In a WCS 1.1 data structure, the lower corner defines the edges region in the
     * directions of <em>decreasing</em> coordinate values in the envelope CRS. This is usually the algebraic
     * minimum coordinates, but not always. For example, an envelope crossing the anti-meridian could have a
     * lower corner longitude greater than the upper corner longitude. Such extended interpretation applies
     * mostly to axes having {@code WRAPAROUND} range meaning.
     *
     * @return a view over the lower corner, typically (but not necessarily) containing minimal coordinate values.
     *
     * @see #getLower(int)
     * @see #getMinimum(int)
     */
    @Override
    public DirectPosition getLowerCorner() {
        // We do not cache the object because it is very cheap to create and we
        // do not want to increase the size of every AbstractEnvelope instances.
        return new LowerCorner();
    }

    /**
     * A coordinate position consisting of all the upper coordinate values.
     * The default implementation returns a view over the {@link #getUpper(int)} method,
     * so changes in this envelope will be immediately reflected in the returned direct position.
     * If the particular case of the {@code GeneralEnvelope} subclass, the returned position
     * supports also {@linkplain DirectPosition#setOrdinate(int, double) write operations},
     * so changes in the position are reflected back in the envelope.
     *
     * <h4>Note on wraparound</h4>
     * The <cite>Web Coverage Service</cite> (WCS) 1.1 specification uses an extended interpretation of the
     * bounding box definition. In a WCS 1.1 data structure, the upper corner defines the edges region in the
     * directions of <em>increasing</em> coordinate values in the envelope CRS. This is usually the algebraic
     * maximum coordinates, but not always. For example, an envelope crossing the anti-meridian could have an
     * upper corner longitude less than the lower corner longitude. Such extended interpretation applies
     * mostly to axes having {@code WRAPAROUND} range meaning.
     *
     * @return a view over the upper corner, typically (but not necessarily) containing maximal coordinate values.
     *
     * @see #getUpper(int)
     * @see #getMaximum(int)
     */
    @Override
    public DirectPosition getUpperCorner() {
        // We do not cache the object because it is very cheap to create and we
        // do not want to increase the size of every AbstractEnvelope instances.
        return new UpperCorner();
    }

    /**
     * A coordinate position consisting of all the median coordinate values.
     * The default implementation returns a view over the {@link #getMedian(int)} method,
     * so changes in this envelope will be immediately reflected in the returned direct position.
     *
     * @return the median coordinates.
     *
     * @see #getMedian(int)
     */
    public DirectPosition getMedian() {
        // We do not cache the object because it is very cheap to create and we
        // do not want to increase the size of every AbstractEnvelope instances.
        return new Median();
    }

    /**
     * Returns the limit in the direction of decreasing coordinate values in the specified dimension.
     * This is usually the algebraic {@linkplain #getMinimum(int) minimum}, except if this envelope
     * spans the anti-meridian.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the starting coordinate value at the given dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     *
     * @see #getLowerCorner()
     * @see #getMinimum(int)
     */
    public abstract double getLower(int dimension) throws IndexOutOfBoundsException;

    /**
     * Returns the limit in the direction of increasing coordinate values in the specified dimension.
     * This is usually the algebraic {@linkplain #getMaximum(int) maximum}, except if this envelope
     * spans the anti-meridian.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the starting coordinate value at the given dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     *
     * @see #getUpperCorner()
     * @see #getMaximum(int)
     */
    public abstract double getUpper(int dimension) throws IndexOutOfBoundsException;

    /**
     * Returns the minimal coordinate value for the specified dimension. In the typical case
     * of non-empty envelopes <em>not</em> crossing the anti-meridian, this method returns the
     * {@link #getLower(int)} value verbatim. In the case of envelope crossing the anti-meridian,
     * this method returns the {@linkplain CoordinateSystemAxis#getMinimumValue() axis minimum value}.
     * If the range in the given dimension is invalid, then this method returns {@code NaN}.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the minimal coordinate value at the given dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    @Override
    public double getMinimum(final int dimension) throws IndexOutOfBoundsException {
        double lower = getLower(dimension);
        if (isNegative(getUpper(dimension) - lower)) {              // Special handling for -0.0
            final CoordinateSystemAxis axis = getAxis(getCoordinateReferenceSystem(), dimension);
            lower = isWrapAround(axis) ? axis.getMinimumValue() : Double.NaN;
        }
        return lower;
    }

    /**
     * Returns the maximal coordinate value for the specified dimension. In the typical case
     * of non-empty envelopes <em>not</em> crossing the anti-meridian, this method returns the
     * {@link #getUpper(int)} value verbatim. In the case of envelope crossing the anti-meridian,
     * this method returns the {@linkplain CoordinateSystemAxis#getMaximumValue() axis maximum value}.
     * If the range in the given dimension is invalid, then this method returns {@code NaN}.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the maximal coordinate value at the given dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    @Override
    public double getMaximum(final int dimension) throws IndexOutOfBoundsException {
        double upper = getUpper(dimension);
        if (isNegative(upper - getLower(dimension))) {              // Special handling for -0.0
            final CoordinateSystemAxis axis = getAxis(getCoordinateReferenceSystem(), dimension);
            upper = isWrapAround(axis) ? axis.getMaximumValue() : Double.NaN;
        }
        return upper;
    }

    /**
     * Returns the median coordinate along the specified dimension.
     * In most cases, the result is equal (minus rounding error) to:
     *
     * {@snippet lang="java" :
     *     median = (getUpper(dimension) + getLower(dimension)) / 2;
     *     }
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * If <var>upper</var> &lt; <var>lower</var> and the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() range meaning} for the requested
     * dimension is {@linkplain RangeMeaning#WRAPAROUND wraparound}, then the median calculated
     * above is actually in the middle of the space <em>outside</em> the envelope. In such cases,
     * this method shifts the <var>median</var> value by half of the periodicity (180° in the
     * longitude case) in order to switch from <i>outer</i> space to <i>inner</i>
     * space. If the axis range meaning is not {@code WRAPAROUND}, then this method returns
     * {@link Double#NaN NaN}.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the median coordinate at the given dimension, or {@link Double#NaN}.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     *
     * @see #getMedian()
     */
    @Override
    public double getMedian(final int dimension) throws IndexOutOfBoundsException {
        final double lower = getLower(dimension);
        final double upper = getUpper(dimension);
        double median = 0.5 * (lower + upper);
        if (isNegative(upper - lower)) {                            // Special handling for -0.0
            median = fixMedian(getAxis(getCoordinateReferenceSystem(), dimension), median);
        }
        return median;
    }

    /**
     * Shifts the median value when the minimum is greater than the maximum.
     * If no shift can be applied, returns {@code NaN}.
     */
    static double fixMedian(final CoordinateSystemAxis axis, final double median) {
        if (isWrapAround(axis)) {
            final double minimum = axis.getMinimumValue();
            final double maximum = axis.getMaximumValue();
            final double cycle   = maximum - minimum;
            if (cycle > 0 && cycle != Double.POSITIVE_INFINITY) {
                // The copySign is for shifting in the direction of the valid range center.
                return median + 0.5 * Math.copySign(cycle, 0.5*(minimum + maximum) - median);
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the envelope span (typically width or height) along the specified dimension.
     * In most cases, the result is equal (minus rounding error) to:
     *
     * {@snippet lang="java" :
     *     span = getUpper(dimension) - getLower(dimension);
     *     }
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * If <var>upper</var> &lt; <var>lower</var> and the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() range meaning} for the requested
     * dimension is {@linkplain RangeMeaning#WRAPAROUND wraparound}, then the span calculated
     * above is negative. In such cases, this method adds the periodicity (typically 360° of
     * longitude) to the span. If the result is a positive number, it is returned. Otherwise
     * this method returns {@link Double#NaN NaN}.
     *
     * @param  dimension  the dimension for which to obtain the span.
     * @return the span (typically width or height) at the given dimension, or {@link Double#NaN}.
     * @throws IndexOutOfBoundsException if the given index is negative or is equal or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    @Override
    public double getSpan(final int dimension) {
        double span = getUpper(dimension) - getLower(dimension);
        if (isNegative(span)) {                                     // Special handling for -0.0
            span = fixSpan(getAxis(getCoordinateReferenceSystem(), dimension), span);
        }
        return span;
    }

    /**
     * Transforms a negative span into a valid value if the axis range meaning is "wraparound".
     * Returns {@code NaN} otherwise.
     *
     * @param  axis  the axis for the span dimension, or {@code null}.
     * @param  span  the negative span.
     * @return a positive span, or NaN if the span cannot be fixed.
     */
    static double fixSpan(final CoordinateSystemAxis axis, double span) {
        if (isWrapAround(axis)) {
            final double cycle = axis.getMaximumValue() - axis.getMinimumValue();
            if (cycle > 0 && cycle != Double.POSITIVE_INFINITY) {
                span += cycle;
                if (span >= 0) {
                    return span;
                }
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the envelope span along the specified dimension, in terms of the given units.
     * The default implementation invokes {@link #getSpan(int)} and converts the result.
     *
     * @param  dimension  the dimension to query.
     * @param  unit  the unit for the return value.
     * @return the span in terms of the given unit.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws IncommensurableException if the length cannot be converted to the specified units.
     */
    public double getSpan(final int dimension, final Unit<?> unit)
            throws IndexOutOfBoundsException, IncommensurableException
    {
        double value = getSpan(dimension);
        final CoordinateSystemAxis axis = getAxis(getCoordinateReferenceSystem(), dimension);
        if (axis != null) {
            final Unit<?> source = axis.getUnit();
            if (source != null) {
                value = source.getConverterToAny(unit).convert(value);
            }
        }
        return value;
    }

    /**
     * Returns the time range of the first dimension associated to a temporal CRS.
     * This convenience method converts floating point values to instants using
     * {@link org.apache.sis.referencing.crs.DefaultTemporalCRS#toInstant(double)}.
     *
     * @return time range in this given envelope.
     *
     * @see Envelopes#toTimeRange(Envelope)
     * @see GeneralEnvelope#setTimeRange(Instant, Instant)
     *
     * @since 1.1
     */
    public Optional<Range<Instant>> getTimeRange() {
        final TemporalAccessor t = TemporalAccessor.of(getCoordinateReferenceSystem(), 0);
        return (t != null) ? Optional.of(t.getTimeRange(this)) : Optional.empty();
    }

    /**
     * Returns this envelope as an array of simple (without wraparound) envelopes.
     * The length of the returned array depends on the number of dimensions where a
     * {@linkplain org.opengis.referencing.cs.RangeMeaning#WRAPAROUND wraparound} range is found.
     * Typically, wraparound occurs only in the range of longitude values, when the range crosses
     * the anti-meridian (a.k.a. date line). However, this implementation will take in account any
     * axis having wraparound {@linkplain CoordinateSystemAxis#getRangeMeaning() range meaning}.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If this envelope {@linkplain #isEmpty() is empty}, then this method returns an empty array.</li>
     *   <li>If this envelope does not have any wraparound behavior, then this method returns {@code this}
     *       in an array of length 1. This envelope is <strong>not</strong> cloned.</li>
     *   <li>If this envelope crosses the <i>anti-meridian</i> (a.k.a. <i>date line</i>)
     *       then this method returns two separated envelopes covering the same area as this envelopes.
     *   <li>While uncommon, the envelope could theoretically crosses the limit of other axis having
     *       wraparound range meaning. If wraparounds occur along <var>n</var> axes, then this method
     *       may return 2ⁿ separated simple envelopes.
     * </ul>
     *
     * @return a representation of this envelope as an array of non-empty envelope.
     *
     * @see Envelope2D#toRectangles()
     * @see GeneralEnvelope#simplify()
     *
     * @since 0.4
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Envelope[] toSimpleEnvelopes() {
        long isWrapAround = 0;                              // A bitmask of the dimensions having a "wrap around" behavior.
        CoordinateReferenceSystem crs = null;
        final int dimension = getDimension();
        for (int i=0; i!=dimension; i++) {
            final double span = getUpper(i) - getLower(i);  // Do not use getSpan(i).
            if (!(span > 0)) {                              // Use `!` for catching NaN.
                if (!isNegative(span)) {
                    return EMPTY;                           // Span is positive zero.
                }
                if (crs == null) {
                    crs = getCoordinateReferenceSystem();
                }
                if (!isWrapAround(crs, i)) {
                    return EMPTY;
                }
                if (i >= Long.SIZE) {
                    // Actually the limit in our current implementation is not the number of axes, but the index of
                    // axes where a wraparound has been found. However, we consider that having more than 64 axes in
                    // a CRS is unusual enough for not being worth to make the distinction in the error message.
                    throw new IllegalStateException(Errors.format(Errors.Keys.ExcessiveListSize_2, "axis", dimension));
                }
                isWrapAround |= (1L << i);
            }
        }
        /*
         * The number of simple envelopes is 2ⁿ where n is the number of wraparound found. In most
         * cases, isWrapAround == 0 so we have an array of length 1 containing only this envelope.
         */
        final int bitCount = Long.bitCount(isWrapAround);
        if (bitCount >= Integer.SIZE - 1) {
            // Should be very unusual, but let be paranoiac.
            throw new IllegalStateException(Errors.format(Errors.Keys.ExcessiveListSize_2, "wraparound", bitCount));
        }
        final Envelope[] envelopes = new Envelope[1 << bitCount];
        if (envelopes.length == 1) {
            envelopes[0] = this;
        } else {
            /*
             * Need to create at least 2 envelopes. Instantiate now all envelopes with coordinate values
             * initialized to a copy of this envelope. We will write directly in their internal arrays later.
             */
            double[] c = new double[dimension * 2];
            for (int i=0; i<dimension; i++) {
                c[i            ] = getLower(i);
                c[i + dimension] = getUpper(i);
            }
            final double[][] coordinates = new double[envelopes.length][];
            for (int i=0; i<envelopes.length; i++) {
                final GeneralEnvelope envelope = new GeneralEnvelope(i == 0 ? c : c.clone());
                envelope.crs = crs;
                envelopes[i] = envelope;
                coordinates[i] = envelope.coordinates;
            }
            /*
             * Assign the minimum and maximum coordinate values in the dimension where a wraparound has been found.
             * The `for` loop below iterates only over the `i` values for which the `isWrapAround` bit is set to 1.
             */
            int mask = 1;               // For identifying whether we need to set the lower or the upper coordinate.
            @SuppressWarnings("null")   // CRS should not be null at this point.
            final CoordinateSystem cs = crs.getCoordinateSystem();
            for (int i; (i = Long.numberOfTrailingZeros(isWrapAround)) != Long.SIZE; isWrapAround &= ~(1L << i)) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                final double min = axis.getMinimumValue();
                final double max = axis.getMaximumValue();
                for (int j=0; j<coordinates.length; j++) {
                    c = coordinates[j];
                    if ((j & mask) == 0) {
                        c[i + dimension] = max;
                    } else {
                        c[i] = min;
                    }
                }
                mask <<= 1;
            }
        }
        return envelopes;
    }

    /**
     * Determines whether or not this envelope contains only finite values.
     * More specifically this method returns {@code false} if at least one
     * coordinate value is NaN or infinity, and {@code true} otherwise.
     * Note that a finite envelope may be {@linkplain #isEmpty() empty}.
     *
     * @return {@code true} if this envelope contains only finite coordinate values.
     *
     * @since 1.4
     */
    public boolean isFinite() {
        for (int i = getDimension(); --i >= 0;) {
            if (!Double.isFinite(getLower(i)) || !Double.isFinite(getUpper(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether or not this envelope is empty. An envelope is empty if it has zero
     * {@linkplain #getDimension() dimension}, or if the {@linkplain #getSpan(int) span} of
     * at least one axis is negative, 0 or {@link Double#NaN NaN}.
     *
     * <div class="note"><b>Note:</b>
     * Strictly speaking, there is an ambiguity if a span is {@code NaN} or if the envelope contains
     * both 0 and infinite spans (since 0⋅∞ = {@code NaN}). In such cases, this method arbitrarily
     * ignores the infinite values and returns {@code true}.</div>
     *
     * If {@code isEmpty()} returns {@code false}, then {@link #isAllNaN()} is guaranteed to
     * also return {@code false}. However, the converse is not always true.
     *
     * @return {@code true} if this envelope is empty.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#isEmpty()
     * @see java.awt.geom.Rectangle2D#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        final int dimension = getDimension();
        if (dimension == 0) {
            return true;
        }
        for (int i=0; i<dimension; i++) {
            if (!(getSpan(i) > 0)) {            // Use `!` in order to catch NaN
                return true;
            }
        }
        assert !isAllNaN() : this;
        return false;
    }

    /**
     * Returns {@code false} if at least one coordinate value is not {@linkplain Double#NaN NaN}.
     * This {@code isAllNaN()} check is different than the {@link #isEmpty()} check since it
     * returns {@code false} for a partially initialized envelope, while {@code isEmpty()}
     * returns {@code false} only after all dimensions have been initialized.
     * More specifically, the following rules apply:
     *
     * <ul>
     *   <li>If {@code isAllNaN() == true}, then {@code isEmpty() == true}</li>
     *   <li>If {@code isEmpty() == false}, then {@code isAllNaN() == false}</li>
     *   <li>The converse of the above-cited rules are not always true.</li>
     * </ul>
     *
     * Note that an all-NaN envelope can still have a non-null
     * {@linkplain #getCoordinateReferenceSystem() coordinate reference system}.
     *
     * @return {@code true} if this envelope has NaN values.
     *
     * @see GeneralEnvelope#setToNaN()
     * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#isEmpty()
     */
    public boolean isAllNaN() {
        final int dimension = getDimension();
        for (int i=0; i<dimension; i++) {
            if (!Double.isNaN(getLower(i)) || !Double.isNaN(getUpper(i))) {
                return false;
            }
        }
        assert isEmpty() : this;
        return true;
    }

    /**
     * Tests if a specified coordinate is inside the boundary of this envelope.
     * Both lower and upper values of this envelope are considered inclusive.
     * If it least one coordinate value in the given point is {@link Double#NaN NaN},
     * then this method returns {@code false}.
     *
     * <h4>Preconditions</h4>
     * This method assumes that the specified point uses a CRS equivalent to this envelope CRS.
     * For performance reasons, this condition is not verified unless Java assertions are enabled.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * For any dimension, if <var>upper</var> &lt; <var>lower</var> then this method uses an
     * algorithm which is the opposite of the usual one: rather than testing if the given point is
     * inside the envelope interior, this method tests if the given point is <em>outside</em> the
     * envelope <em>exterior</em>.
     *
     * @param  position  the point to text.
     * @return {@code true} if the specified coordinate is inside the boundary of this envelope; {@code false} otherwise.
     * @throws MismatchedDimensionException if the specified point does not have the expected number of dimensions.
     * @throws AssertionError if assertions are enabled and the envelopes have mismatched CRS.
     */
    @ArgumentCheckByAssertion
    public boolean contains(final DirectPosition position) throws MismatchedDimensionException {
        // Implicit null check below.
        final int dimension = getDimension();
        ensureDimensionMatches("point", dimension, position);
        assert assertEquals(getCoordinateReferenceSystem(), position.getCoordinateReferenceSystem()) : position;
        for (int i=0; i<dimension; i++) {
            final double value = position.getOrdinate(i);
            final double lower = getLower(i);
            final double upper = getUpper(i);
            final boolean c1   = (value >= lower);
            final boolean c2   = (value <= upper);
            if (c1 & c2) {
                continue;               // Point inside the range, check other dimensions.
            }
            if (c1 | c2) {
                if (isNegative(upper - lower)) {
                    /*
                     * "Crossing the anti-meridian" case: if we reach this point, then the
                     * [upper...lower] range  (note the `lower` and `upper` interchanging)
                     * is actually a space outside the envelope and we have checked that
                     * the coordinate value is outside that space.
                     */
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if this envelope completely encloses the specified envelope.
     * The default implementation delegates to:
     *
     * <blockquote><pre>{@linkplain #contains(Envelope, boolean) contains}(envelope, <b>true</b>)</pre></blockquote>
     *
     * <h4>Preconditions</h4>
     * This method assumes that the specified envelope uses the same CRS as this envelope.
     * For performance reasons, this condition is not verified unless Java assertions are enabled.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * For every cases illustrated below, the yellow box is considered completely enclosed
     * in the blue envelope:
     *
     * <p><img src="doc-files/Contains.png" alt="Examples of envelope inclusions"></p>
     *
     * @param  envelope  the envelope to test for inclusion.
     * @return {@code true} if this envelope completely encloses the specified one.
     * @throws MismatchedDimensionException if the specified envelope doesn't have the expected dimension.
     * @throws AssertionError if assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see #intersects(Envelope)
     * @see #equals(Envelope, double, boolean)
     *
     * @since 0.4
     */
    @ArgumentCheckByAssertion
    public boolean contains(final Envelope envelope) throws MismatchedDimensionException {
        return contains(envelope, true);
    }

    /**
     * Returns {@code true} if this envelope completely encloses the specified envelope.
     * If one or more edges from the specified envelope coincide with an edge from this
     * envelope, then this method returns {@code true} only if {@code edgesInclusive}
     * is {@code true}.
     *
     * <p>This method is subject to the same preconditions as {@link #contains(Envelope)},
     * and handles envelopes crossing the anti-meridian in the same way.</p>
     *
     * @param  envelope        the envelope to test for inclusion.
     * @param  edgesInclusive  {@code true} if this envelope edges are inclusive.
     * @return {@code true} if this envelope completely encloses the specified one.
     * @throws MismatchedDimensionException if the specified envelope doesn't have the expected dimension.
     * @throws AssertionError if assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see #intersects(Envelope, boolean)
     */
    @ArgumentCheckByAssertion
    public boolean contains(final Envelope envelope, final boolean edgesInclusive) throws MismatchedDimensionException {
        // Implicit null check below.
        final int dimension = getDimension();
        ensureDimensionMatches("envelope", dimension, envelope);
        assert assertEquals(getCoordinateReferenceSystem(), envelope.getCoordinateReferenceSystem()) : envelope;
        final DirectPosition lowerCorner = envelope.getLowerCorner();
        final DirectPosition upperCorner = envelope.getUpperCorner();
        for (int i=0; i<dimension; i++) {
            final double lower0 = getLower(i);
            final double upper0 = getUpper(i);
            final double lower1 = lowerCorner.getOrdinate(i);
            final double upper1 = upperCorner.getOrdinate(i);
            final boolean lowerCondition, upperCondition;
            if (edgesInclusive) {
                lowerCondition = (lower1 >= lower0);
                upperCondition = (upper1 <= upper0);
            } else {
                lowerCondition = (lower1 > lower0);
                upperCondition = (upper1 < upper0);
            }
            if (lowerCondition & upperCondition) {
                /*         upperCnd          upperCnd
                 *  ┌─────────────┐          ────┐  ┌────                      ┌─┐
                 *  │  ┌───────┐  │    or    ──┐ │  │ ┌──    excluding    ───┐ │ │ ┌───
                 *  │  └───────┘  │          ──┘ │  │ └──                 ───┘ │ │ └───
                 *  └─────────────┘          ────┘  └────                      └─┘
                 *  lowerCnd                        lowerCnd
                 */
                // (upper1-lower1) is negative if the small rectangle in above pictures spans the anti-meridian.
                if (!isNegativeUnsafe(upper1 - lower1) || isNegativeUnsafe(upper0 - lower0)) {
                    // Not the excluded case, go to next dimension.
                    continue;
                }
                /*
                 * If this envelope does not cross the anti-meridian but the given envelope does,
                 * then this envelope does not contain the given envelope except in the special
                 * case where the envelope spanning is equal or greater than the axis spanning
                 * (including the case where this envelope expands to infinities).
                 */
                if ((lower0 == Double.NEGATIVE_INFINITY && upper0 == Double.POSITIVE_INFINITY) ||
                    (upper0 - lower0 >= getCycle(getAxis(getCoordinateReferenceSystem(), i))))
                {
                    continue;
                }
            } else if (lowerCondition != upperCondition) {
                /*     upperCnd                     !upperCnd
                 *  ──────────┐  ┌─────              ─────┐  ┌─────────
                 *    ┌────┐  │  │           or           │  │  ┌────┐
                 *    └────┘  │  │                        │  │  └────┘
                 *  ──────────┘  └─────              ─────┘  └─────────
                 *               !lowerCnd                   lowerCnd */
                if (isNegative(upper0 - lower0)) {
                    if (isPositive(upper1 - lower1)) {
                        continue;
                    }
                    // Special case for the [0…-0] range, if inclusive.
                    if (edgesInclusive && Double.doubleToRawLongBits(lower0) == 0L &&
                            Double.doubleToRawLongBits(upper0) == SIGN_BIT_MASK)
                    {
                        continue;
                    }
                }
            } else if (isNegativeZero(upper0 - lower0)) {
                /*      !upperCnd
                 *  ────────┬────────
                 *        ┌─┼────┐
                 *        └─┼────┘
                 *  ────────┴────────
                 *      !lowerCnd */
                continue;
            }
            return false;
        }
        // The check for ArrayEnvelope.class is for avoiding never-ending callbacks.
        assert envelope.getClass() == ArrayEnvelope.class ||
               intersects(new ArrayEnvelope(envelope), edgesInclusive) : envelope;
        return true;
    }

    /**
     * Returns {@code true} if this envelope intersects the specified envelope.
     * This method returns {@code true} if two envelope <em>interiors</em> have at least one point in common
     * (in other words, their intersection is non-{@linkplain #isEmpty() empty}).
     * The default implementation delegates to:
     *
     * <blockquote><pre>{@linkplain #intersects(Envelope, boolean) intersects}(envelope, <b>false</b>)</pre></blockquote>
     *
     * <h4>Preconditions</h4>
     * This method assumes that the specified envelope uses the same CRS as this envelope.
     * For performance reasons, this condition is not verified unless Java assertions are enabled.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method can handle envelopes crossing the anti-meridian.
     *
     * @param  envelope  the envelope to test for intersection.
     * @return {@code true} if this envelope intersects the specified one.
     * @throws MismatchedDimensionException if the specified envelope doesn't have the expected dimension.
     * @throws AssertionError if assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see #contains(Envelope, boolean)
     * @see #equals(Envelope, double, boolean)
     *
     * @since 0.4
     */
    @ArgumentCheckByAssertion
    public boolean intersects(final Envelope envelope) throws MismatchedDimensionException {
        return intersects(envelope, false);
    }

    /**
     * Returns {@code true} if this envelope intersects or (optionally) touches the specified envelope.
     * The {@code touch} argument controls the value to return if only the envelope boundaries
     * (not the interiors) have a point in common:
     *
     * <ul>
     *   <li>If {@code false}, this method returns {@code true} if the intersection between the two envelopes
     *       is non-{@linkplain #isEmpty() empty} (i.e. the envelope <em>interiors</em> have points in common).
     *       This is the usual definition of {@code intersects} operation.</li>
     *   <li>If {@code true}, this method returns {@code true} if the two envelopes intersect each other
     *       <em>or</em> touch each other.</li>
     * </ul>
     *
     * This method is subject to the same preconditions as {@link #intersects(Envelope)},
     * and handles envelopes crossing the anti-meridian in the same way.
     *
     * @param  envelope  the envelope to test for intersection.
     * @param  touch     the value to return if the two envelopes touch each other.
     * @return {@code true} if this envelope intersects the specified envelope, or
     *         {@code touch} if this envelope touches the specified envelope, or {@code false} otherwise.
     * @throws MismatchedDimensionException if the specified envelope does not have the expected dimension.
     * @throws AssertionError if assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see #contains(Envelope, boolean)
     * @see #equals(Envelope, double, boolean)
     */
    @ArgumentCheckByAssertion
    public boolean intersects(final Envelope envelope, final boolean touch) throws MismatchedDimensionException {
        // Implicit null check below.
        final int dimension = getDimension();
        ensureDimensionMatches("envelope", dimension, envelope);
        assert assertEquals(getCoordinateReferenceSystem(), envelope.getCoordinateReferenceSystem()) : envelope;
        final DirectPosition lowerCorner = envelope.getLowerCorner();
        final DirectPosition upperCorner = envelope.getUpperCorner();
        for (int i=0; i<dimension; i++) {
            final double lower0 = getLower(i);
            final double upper0 = getUpper(i);
            final double lower1 = lowerCorner.getOrdinate(i);
            final double upper1 = upperCorner.getOrdinate(i);
            final boolean lowerCondition, upperCondition;
            if (touch) {
                lowerCondition = (lower1 <= upper0);
                upperCondition = (upper1 >= lower0);
            } else {
                lowerCondition = (lower1 < upper0);
                upperCondition = (upper1 > lower0);
            }
            if (upperCondition & lowerCondition) {
                /*     ┌──────────┐
                 *     │  ┌───────┼──┐
                 *     │  └───────┼──┘
                 *     └──────────┘ (this is the most standard case) */
                continue;
            }
            final boolean sp0 = isNegative(upper0 - lower0);
            final boolean sp1 = isNegative(upper1 - lower1);
            if (sp0 | sp1) {
                /*
                 * If both envelopes cross the anti-meridian (sp0 & sp1), we have an unconditional
                 * intersection (since both envelopes extend to infinities). Otherwise we have one
                 * of the cases illustrated below. Note that the rectangle could also intersect on
                 * only once side.
                 *         ┌──────────┐                   ─────┐      ┌─────
                 *     ────┼───┐  ┌───┼────      or          ┌─┼──────┼─┐
                 *     ────┼───┘  └───┼────                  └─┼──────┼─┘
                 *         └──────────┘                   ─────┘      └───── */
                if ((sp0 & sp1) | (upperCondition | lowerCondition)) {
                    continue;
                }
            }
            // The check for ArrayEnvelope.class is for avoiding never-ending callbacks.
            assert envelope.getClass() == ArrayEnvelope.class || hasNaN(envelope) ||
                    !contains(new ArrayEnvelope(envelope), touch) : envelope;
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if at least one coordinate in the given envelope
     * is {@link Double#NaN}. This is used for assertions only.
     */
    static boolean hasNaN(final Envelope envelope) {
        return hasNaN(envelope.getLowerCorner()) || hasNaN(envelope.getUpperCorner());
    }

    /**
     * Returns {@code true} if at least one coordinate in the given position
     * is {@link Double#NaN}. This is used for assertions only.
     */
    static boolean hasNaN(final DirectPosition position) {
        for (int i=position.getDimension(); --i>=0;) {
            if (Double.isNaN(position.getOrdinate(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares to the specified envelope for equality up to the specified tolerance value.
     * The tolerance value {@code eps} can be either relative to the {@linkplain #getSpan(int)
     * envelope span} along each dimension or can be an absolute value (as for example some
     * ground resolution of a {@linkplain org.apache.sis.coverage.grid.GridCoverage grid coverage}).
     *
     * <ul>
     *   <li>If {@code epsIsRelative} is set to {@code true}, the actual tolerance value for a
     *       given dimension <var>i</var> is {@code eps} × {@code span} where {@code span}
     *       is the maximum of {@linkplain #getSpan(int) this envelope span} and the specified
     *       envelope span along dimension <var>i</var>.</li>
     *   <li>If {@code epsIsRelative} is set to {@code false}, the actual tolerance value for a
     *       given dimension <var>i</var> is {@code eps}.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * Relative tolerance values (as opposed to absolute tolerance values) help to workaround the
     * fact that tolerance value are CRS dependent. For example, the tolerance value need to be
     * smaller for geographic CRS than for UTM projections, because the former typically has a
     * [-180…180]° range while the latter can have a range of thousands of meters.</div>
     *
     * <h4>Coordinate Reference System</h4>
     * To be considered equal, the two envelopes must have the same {@linkplain #getDimension() number of dimensions}
     * and their CRS must be {@linkplain org.apache.sis.util.ComparisonMode#APPROXIMATE approximately equal}.
     * If at least one envelope has a null CRS, then the CRS are ignored and the coordinate values are compared
     * as if the CRS were equal.
     *
     * @param  other          the envelope to compare with.
     * @param  eps            the tolerance value to use for numerical comparisons.
     * @param  epsIsRelative  {@code true} if the tolerance value should be relative to axis length,
     *                        or {@code false} if it is an absolute value.
     * @return {@code true} if the given object is equal to this envelope up to the given tolerance value.
     *
     * @see #contains(Envelope)
     * @see #intersects(Envelope)
     */
    public boolean equals(final Envelope other, final double eps, final boolean epsIsRelative) {
        // Implicit null check below.
        final int dimension = getDimension();
        if (other.getDimension() != dimension ||
                !equals(getCoordinateReferenceSystem(), other.getCoordinateReferenceSystem(),
                        (eps == 0) ? ComparisonMode.IGNORE_METADATA : ComparisonMode.APPROXIMATE))
        {
            return false;
        }
        final DirectPosition lowerCorner = other.getLowerCorner();
        final DirectPosition upperCorner = other.getUpperCorner();
        for (int i=0; i<dimension; i++) {
            double ε = eps;
            if (epsIsRelative) {
                final double span = Math.max(getSpan(i), other.getSpan(i));
                if (span > 0 && span < Double.POSITIVE_INFINITY) {
                    ε *= span;
                }
            }
            if (!epsilonEqual(getLower(i), lowerCorner.getOrdinate(i), ε) ||
                !epsilonEqual(getUpper(i), upperCorner.getOrdinate(i), ε))
            {
                return false;
            }
        }
        return true;
    }

    /*
     * Do not implement `LenientComparable.equals(Object, ComparisonMode)` because the choice of a tolerance
     * threshold is too arbitrary.  We want users to invoke above `equals(Envelope, double, boolean)` method
     * themselves with a tolerance computed for example from the resolution of their data.
     */

    /**
     * Returns {@code true} if the specified object is an envelope of the same class
     * with equals coordinates and {@linkplain #getCoordinateReferenceSystem() CRS}.
     *
     * <h4>Implementation note</h4>
     * This implementation requires that the provided {@code object} argument is of the same class as this envelope.
     * We do not relax this rule since not every implementations in the SIS code base follow the same contract.
     *
     * @param  object  the object to compare with this envelope.
     * @return {@code true} if the given object is equal to this envelope.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final AbstractEnvelope that = (AbstractEnvelope) object;
            final int dimension = getDimension();
            if (dimension == that.getDimension()) {
                for (int i=0; i<dimension; i++) {
                    if (doubleToLongBits(getLower(i)) != doubleToLongBits(that.getLower(i)) ||
                        doubleToLongBits(getUpper(i)) != doubleToLongBits(that.getUpper(i)))
                    {
                        assert !equals(that, 0.0, false) : this;
                        return false;
                    }
                }
                if (Objects.equals(this.getCoordinateReferenceSystem(),
                                   that.getCoordinateReferenceSystem()))
                {
                    assert hashCode() == that.hashCode() : this;
                    assert equals(that, 0.0, false) : this;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a hash value for this envelope.
     */
    @Override
    public int hashCode() {
        final int dimension = getDimension();
        int code = 1;
        boolean p = true;
        do {
            for (int i=0; i<dimension; i++) {
                code = code*31 + Double.hashCode(p ? getLower(i) : getUpper(i));
            }
        } while ((p = !p) == false);
        return code + Objects.hashCode(getCoordinateReferenceSystem());
    }

    /**
     * Formats this envelope as a "{@code BOX}" element.
     * The output is of the form "{@code BOX}<var>n</var>{@code D(}{@linkplain #getLowerCorner()
     * lower corner}{@code ,}{@linkplain #getUpperCorner() upper corner}{@code )}"
     * where <var>n</var> is the {@linkplain #getDimension() number of dimensions}.
     * The number of dimension is written only if different than 2.
     * Examples:
     * <ul>
     *   <li>{@code BOX(-90 -180, 90 180)}</li>
     *   <li>{@code BOX3D(-90 -180 0, 90 180 1)}</li>
     * </ul>
     *
     * This method formats the numbers as with {@link Double#toString(double)} (i.e. without fixed number of fraction digits).
     * The string returned by this method can be {@linkplain GeneralEnvelope#GeneralEnvelope(CharSequence) parsed}
     * by the {@code GeneralEnvelope} constructor.
     *
     * <h4>Note on standards</h4>
     * The {@code BOX} element is not part of the standard <i>Well Known Text</i> (WKT) format.
     * However, it is understood by many software libraries, for example GDAL and PostGIS.
     *
     * @return this envelope as a {@code BOX} or {@code BOX3D} (most typical dimensions) element.
     */
    @Override
    public String toString() {
        return toString(this, false);
    }

    /**
     * Implementation of the public {@link #toString()} and {@link Envelopes#toString(Envelope)}
     * methods for formatting a {@code BOX} element from an envelope.
     *
     * @param  envelope           the envelope to format.
     * @param  isSinglePrecision  {@code true} if every lower and upper corner values can be cast to {@code float}.
     * @return this envelope as a {@code BOX} or {@code BOX3D} (most typical dimensions) element.
     *
     * @see GeneralEnvelope#GeneralEnvelope(CharSequence)
     * @see CoordinateFormat
     * @see org.apache.sis.io.wkt
     */
    static String toString(final Envelope envelope, final boolean isSinglePrecision) {
        final int dimension = envelope.getDimension();
        final StringBuilder buffer = new StringBuilder(64).append("BOX");
        if (dimension != 2) {
            buffer.append(dimension).append('D');
        }
        if (dimension == 0) {
            buffer.append("()");
        } else {
            final DirectPosition lowerCorner = envelope.getLowerCorner();
            final DirectPosition upperCorner = envelope.getUpperCorner();
            boolean isUpper = false;
            do {                                                        // Executed exactly twice.
                for (int i=0; i<dimension; i++) {
                    buffer.append(i == 0 && !isUpper ? '(' : ' ');
                    final double coordinate = (isUpper ? upperCorner : lowerCorner).getOrdinate(i);
                    if (isSinglePrecision) {
                        buffer.append((float) coordinate);
                    } else {
                        buffer.append(coordinate);
                    }
                    trimFractionalPart(buffer);
                }
                buffer.append(isUpper ? ')' : ',');
            } while ((isUpper = !isUpper) == true);
        }
        return buffer.toString();
    }

    /**
     * Formats this envelope as a "{@code BOX}" element.
     * The output is of the form "{@code BOX}<var>n</var>{@code D[}{@linkplain #getLowerCorner()
     * lower corner}{@code ,}{@linkplain #getUpperCorner() upper corner}{@code ]}"
     * where <var>n</var> is the {@linkplain #getDimension() number of dimensions}.
     * The number of dimension is written only if different than 2.
     *
     * <p>If the coordinate reference system is geodetic or projected, then coordinate values are formatted
     * with a precision equivalent to one centimetre on Earth (the actual number of fraction digits is
     * adjusted for the axis unit of measurement and the planet size if different than Earth).</p>
     *
     * <h4>Note on standards</h4>
     * The {@code BOX} element is not part of the standard <i>Well Known Text</i> (WKT) format.
     * However, it is understood by many software libraries, for example GDAL and PostGIS.
     *
     * @param  formatter  the formatter where to format the inner content of this envelope.
     * @return the pseudo-WKT keyword, which is {@code "Box"} for this element.
     *
     * @since 1.0
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final Vector[] points = {
            Vector.create(getLowerCorner().getCoordinate()),
            Vector.create(getUpperCorner().getCoordinate())
        };
        formatter.append(points, WKTUtilities.suggestFractionDigits(getCoordinateReferenceSystem(), points));
        final int dimension = getDimension();
        String keyword = "Box";
        if (dimension != 2) {
            keyword = new StringBuilder(keyword).append(dimension).append('D').toString();
        }
        formatter.setInvalidWKT(Envelope.class, null);
        return keyword;
    }

    /**
     * Base class for unmodifiable direct positions backed by the enclosing envelope.
     * Subclasses must override the {@link #getOrdinate(int)} method in order to delegate
     * the work to the appropriate {@link AbstractEnvelope} method.
     *
     * <p>Instance of this class are serializable if the enclosing envelope is serializable.</p>
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     */
    private abstract class Point extends AbstractDirectPosition implements Serializable {
        private static final long serialVersionUID = -4868610696294317932L;

        /** The coordinate reference system in which the coordinate is given. */
        @Override public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return AbstractEnvelope.this.getCoordinateReferenceSystem();
        }

        /** The length of coordinate sequence (the number of entries). */
        @Override public final int getDimension() {
            return AbstractEnvelope.this.getDimension();
        }
    }

    /**
     * The corner returned by {@link AbstractEnvelope#getLowerCorner()}.
     */
    private final class LowerCorner extends Point {
        private static final long serialVersionUID = 1310741484466506178L;

        @Override public double getCoordinate(final int dimension) throws IndexOutOfBoundsException {
            return getLower(dimension);
        }

        /** Sets the coordinate value along the specified dimension. */
        @Override public void setCoordinate(final int dimension, final double value) {
            setRange(dimension, value, getUpper(dimension));
        }
    }

    /**
     * The corner returned by {@link AbstractEnvelope#getUpperCorner()}.
     */
    private final class UpperCorner extends Point {
        private static final long serialVersionUID = -6458663549974061472L;

        @Override public double getCoordinate(final int dimension) throws IndexOutOfBoundsException {
            return getUpper(dimension);
        }

        /** Sets the coordinate value along the specified dimension. */
        @Override public void setCoordinate(final int dimension, final double value) {
            setRange(dimension, getLower(dimension), value);
        }
    }

    /**
     * The point returned by {@link AbstractEnvelope#getMedian()}.
     */
    private final class Median extends Point {
        private static final long serialVersionUID = -5826011018957321729L;

        @Override public double getCoordinate(final int dimension) throws IndexOutOfBoundsException {
            return getMedian(dimension);
        }
    }

    /**
     * Invoked by {@link LowerCorner} and {@link UpperCorner} when a coordinate is modified.
     * The default implementation throws an {@link UnsupportedOperationException} in every cases.
     * This method is overridden and made public by {@link GeneralEnvelope}.
     *
     * <p>The declaration in this {@code AbstractEnvelope} class is not public on purpose,
     * since this class intentionally have no public setter methods. This is necessary for
     * preserving the immutable aspect of {@link ImmutableEnvelope} subclass among others.</p>
     *
     * @param  dimension  the dimension to set.
     * @param  lower      the limit in the direction of decreasing coordinate values.
     * @param  upper      the limit in the direction of increasing coordinate values.
     * @throws UnsupportedOperationException if this envelope is not modifiable.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws IllegalArgumentException if {@code lower > upper}, this envelope has a CRS
     *         and the axis range meaning at the given dimension is not "wraparound".
     */
    void setRange(final int dimension, final double lower, final double upper)
            throws IndexOutOfBoundsException
    {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, getClass()));
    }
}
