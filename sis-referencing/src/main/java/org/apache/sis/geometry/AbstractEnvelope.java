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
import java.io.Serializable;
import javax.measure.unit.Unit;
import javax.measure.converter.ConversionException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;

import static java.lang.Double.doubleToLongBits;
import static org.apache.sis.internal.util.Utilities.SIGN_BIT_MASK;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.StringBuilders.trimFractionalPart;
import static org.apache.sis.math.MathFunctions.epsilonEqual;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.math.MathFunctions.isPositive;

// Related to JDK7
import org.apache.sis.internal.util.Objects;


/**
 * Base class for {@link Envelope} implementations.
 * This base class does not hold any state and does not implement the {@link java.io.Serializable}
 * or {@link Cloneable} interfaces. The internal representation, and the choice to be cloneable or
 * serializable, is left to subclasses.
 *
 * <p>Implementors needs to define at least the following methods:</p>
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
 * {@section Spanning the anti-meridian of a Geographic CRS}
 * The <cite>Web Coverage Service</cite> (WCS) specification authorizes (with special treatment)
 * cases where <var>upper</var> &lt; <var>lower</var> at least in the longitude case. They are
 * envelopes crossing the anti-meridian, like the red box below (the green box is the usual case).
 * The default implementation of methods listed in the right column can handle such cases.
 *
 * <table class="compact" align="center"><tr><td>
 *   <img src="doc-files/AntiMeridian.png">
 * </td><td>
 * Supported methods:
 * <ul>
 *   <li>{@link #getMinimum(int)}</li>
 *   <li>{@link #getMaximum(int)}</li>
 *   <li>{@link #getMedian(int)}</li>
 *   <li>{@link #getSpan(int)}</li>
 *   <li>{@link #contains(DirectPosition)}</li>
 *   <li>{@link #contains(Envelope, boolean)}</li>
 *   <li>{@link #intersects(Envelope, boolean)}</li>
 * </ul>
 * </td></tr></table>
 *
 * {@section Note on positive and negative zeros}
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
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
public abstract class AbstractEnvelope implements Envelope {
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
     * @param  envelope The envelope to cast, or {@code null}.
     * @return The values of the given envelope as an {@code AbstractEnvelope} instance.
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
     * Returns {@code true} if at least one of the specified CRS is null, or both CRS are equals.
     * This special processing for {@code null} values is different from the usual contract of an
     * {@code equals} method, but allow to handle the case where the CRS is unknown.
     *
     * <p>Note that in debug mode (to be used in assertions only), the comparisons are actually a
     * bit more relax than just "ignoring metadata", since some rounding errors are tolerated.</p>
     */
    static boolean equalsIgnoreMetadata(final CoordinateReferenceSystem crs1,
                                        final CoordinateReferenceSystem crs2, final boolean debug)
    {
        return (crs1 == null) || (crs2 == null) || Utilities.deepEquals(crs1, crs2,
                debug ? ComparisonMode.DEBUG : ComparisonMode.IGNORE_METADATA);
    }

    /**
     * Returns the common CRS of specified points.
     *
     * @param  lowerCorner The first position.
     * @param  upperCorner The second position.
     * @return Their common CRS, or {@code null} if none.
     * @throws MismatchedReferenceSystemException if the two positions don't use equal CRS.
     */
    static CoordinateReferenceSystem getCommonCRS(final DirectPosition lowerCorner,
                                                  final DirectPosition upperCorner)
            throws MismatchedReferenceSystemException
    {
        ensureNonNull("lowerCorner", lowerCorner);
        ensureNonNull("upperCorner", upperCorner);
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
     * @param  crs The envelope CRS, or {@code null}.
     * @param  dimension The dimension for which to get the axis.
     * @return The axis at the given dimension, or {@code null}.
     */
    static CoordinateSystemAxis getAxis(final CoordinateReferenceSystem crs, final int dimension) {
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs != null) {
                return cs.getAxis(dimension);
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the axis for the given dimension has the
     * {@link RangeMeaning#WRAPAROUND WRAPAROUND} range meaning.
     *
     * @param  crs The envelope CRS, or {@code null}.
     * @param  dimension The dimension for which to get the axis.
     * @return {@code true} if the range meaning is {@code WRAPAROUND}.
     */
    static boolean isWrapAround(final CoordinateReferenceSystem crs, final int dimension) {
        final CoordinateSystemAxis axis = getAxis(crs, dimension);
        return (axis != null) && RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning());
    }

    /**
     * If the range meaning of the given axis is "wraparound", returns the spanning of that axis.
     * Otherwise returns {@link Double#NaN}.
     *
     * @param  axis The axis for which to get the spanning.
     * @return The spanning of the given axis.
     */
    static double getSpan(final CoordinateSystemAxis axis) {
        if (axis != null && RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning())) {
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
     * {@preformat java
     *     if (a < b && isNegativeUnsafe(a)) {
     *         // ... do some stuff
     *     }
     * }
     */
    static boolean isNegativeUnsafe(final double value) {
        return (Double.doubleToRawLongBits(value) & SIGN_BIT_MASK) != 0;
    }

    /**
     * A coordinate position consisting of all the {@linkplain #getLower(int) lower ordinates}.
     * The default implementation returns a unmodifiable direct position backed by this envelope,
     * so changes in this envelope will be immediately reflected in the returned direct position.
     *
     * {@note The <cite>Web Coverage Service</cite> (WCS) 1.1 specification uses an extended
     * interpretation of the bounding box definition. In a WCS 1.1 data structure, the lower
     * corner defines the edges region in the directions of <em>decreasing</em> coordinate
     * values in the envelope CRS. This is usually the algebraic minimum coordinates, but not
     * always. For example, an envelope crossing the anti-meridian could have a lower corner
     * longitude greater than the upper corner longitude. Such extended interpretation applies
     * mostly to axes having <code>WRAPAROUND</code> range meaning.}
     *
     * @return The lower corner, typically (but not necessarily) containing minimal ordinate values.
     */
    @Override
    public DirectPosition getLowerCorner() {
        // We do not cache the object because it is very cheap to create and we
        // do not want to increase the size of every AbstractEnvelope instances.
        return new LowerCorner();
    }

    /**
     * A coordinate position consisting of all the {@linkplain #getUpper(int) upper ordinates}.
     * The default implementation returns a unmodifiable direct position backed by this envelope,
     * so changes in this envelope will be immediately reflected in the returned direct position.
     *
     * {@note The <cite>Web Coverage Service</cite> (WCS) 1.1 specification uses an extended
     * interpretation of the bounding box definition. In a WCS 1.1 data structure, the upper
     * corner defines the edges region in the directions of <em>increasing</em> coordinate
     * values in the envelope CRS. This is usually the algebraic maximum coordinates, but not
     * always. For example, an envelope crossing the anti-meridian could have an upper corner
     * longitude less than the lower corner longitude. Such extended interpretation applies
     * mostly to axes having <code>WRAPAROUND</code> range meaning.}
     *
     * @return The upper corner, typically (but not necessarily) containing maximal ordinate values.
     */
    @Override
    public DirectPosition getUpperCorner() {
        // We do not cache the object because it is very cheap to create and we
        // do not want to increase the size of every AbstractEnvelope instances.
        return new UpperCorner();
    }

    /**
     * A coordinate position consisting of all the {@linkplain #getMedian(int) middle ordinates}.
     * The default implementation returns a unmodifiable direct position backed by this envelope,
     * so changes in this envelope will be immediately reflected in the returned direct position.
     *
     * @return The median coordinates.
     */
    public DirectPosition getMedian() {
        // We do not cache the object because it is very cheap to create and we
        // do not want to increase the size of every AbstractEnvelope instances.
        return new Median();
    }

    /**
     * Returns the limit in the direction of decreasing ordinate values in the specified dimension.
     * This is usually the algebraic {@linkplain #getMinimum(int) minimum}, except if this envelope
     * spans the anti-meridian.
     *
     * @param  dimension The dimension for which to obtain the ordinate value.
     * @return The starting ordinate value at the given dimension.
     * @throws IndexOutOfBoundsException If the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    public abstract double getLower(int dimension) throws IndexOutOfBoundsException;

    /**
     * Returns the limit in the direction of increasing ordinate values in the specified dimension.
     * This is usually the algebraic {@linkplain #getMaximum(int) maximum}, except if this envelope
     * spans the anti-meridian.
     *
     * @param  dimension The dimension for which to obtain the ordinate value.
     * @return The starting ordinate value at the given dimension.
     * @throws IndexOutOfBoundsException If the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    public abstract double getUpper(int dimension) throws IndexOutOfBoundsException;

    /**
     * Returns the minimal ordinate value for the specified dimension. In the typical case
     * of envelopes <em>not</em> spanning the anti-meridian, this method returns the
     * {@link #getLower(int)} value verbatim. In the case of envelope spanning the anti-meridian,
     * this method returns the {@linkplain CoordinateSystemAxis#getMinimumValue() axis minimum value}.
     *
     * @param  dimension The dimension for which to obtain the ordinate value.
     * @return The minimal ordinate value at the given dimension.
     * @throws IndexOutOfBoundsException If the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    @Override
    public double getMinimum(final int dimension) throws IndexOutOfBoundsException {
        double lower = getLower(dimension);
        if (isNegative(getUpper(dimension) - lower)) { // Special handling for -0.0
            final CoordinateSystemAxis axis = getAxis(getCoordinateReferenceSystem(), dimension);
            lower = (axis != null) ? axis.getMinimumValue() : Double.NEGATIVE_INFINITY;
        }
        return lower;
    }

    /**
     * Returns the maximal ordinate value for the specified dimension. In the typical case
     * of envelopes <em>not</em> spanning the anti-meridian, this method returns the
     * {@link #getUpper(int)} value verbatim. In the case of envelope spanning the anti-meridian,
     * this method returns the {@linkplain CoordinateSystemAxis#getMaximumValue() axis maximum value}.
     *
     * @param  dimension The dimension for which to obtain the ordinate value.
     * @return The maximal ordinate value at the given dimension.
     * @throws IndexOutOfBoundsException If the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    @Override
    public double getMaximum(final int dimension) throws IndexOutOfBoundsException {
        double upper = getUpper(dimension);
        if (isNegative(upper - getLower(dimension))) { // Special handling for -0.0
            final CoordinateSystemAxis axis = getAxis(getCoordinateReferenceSystem(), dimension);
            upper = (axis != null) ? axis.getMaximumValue() : Double.POSITIVE_INFINITY;
        }
        return upper;
    }

    /**
     * Returns the median ordinate along the specified dimension.
     * In most cases, the result is equals (minus rounding error) to:
     *
     * {@preformat java
     *     median = (getUpper(dimension) + getLower(dimension)) / 2;
     * }
     *
     * {@section Spanning the anti-meridian of a Geographic CRS}
     * If <var>upper</var> &lt; <var>lower</var> and the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() range meaning} for the requested
     * dimension is {@linkplain RangeMeaning#WRAPAROUND wraparound}, then the median calculated
     * above is actually in the middle of the space <em>outside</em> the envelope. In such cases,
     * this method shifts the <var>median</var> value by half of the periodicity (180° in the
     * longitude case) in order to switch from <cite>outer</cite> space to <cite>inner</cite>
     * space. If the axis range meaning is not {@code WRAPAROUND}, then this method returns
     * {@link Double#NaN NaN}.
     *
     * @param  dimension The dimension for which to obtain the ordinate value.
     * @return The median ordinate at the given dimension, or {@link Double#NaN}.
     * @throws IndexOutOfBoundsException If the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    @Override
    public double getMedian(final int dimension) throws IndexOutOfBoundsException {
        final double lower = getLower(dimension);
        final double upper = getUpper(dimension);
        double median = 0.5 * (lower + upper);
        if (isNegative(upper - lower)) { // Special handling for -0.0
            median = fixMedian(getAxis(getCoordinateReferenceSystem(), dimension), median);
        }
        return median;
    }

    /**
     * Shifts the median value when the minimum is greater than the maximum.
     * If no shift can be applied, returns {@code NaN}.
     */
    static double fixMedian(final CoordinateSystemAxis axis, final double median) {
        if (axis != null && RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning())) {
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
     * In most cases, the result is equals (minus rounding error) to:
     *
     * {@preformat java
     *     span = getUpper(dimension) - getLower(dimension);
     * }
     *
     * {@section Spanning the anti-meridian of a Geographic CRS}
     * If <var>upper</var> &lt; <var>lower</var> and the
     * {@linkplain CoordinateSystemAxis#getRangeMeaning() range meaning} for the requested
     * dimension is {@linkplain RangeMeaning#WRAPAROUND wraparound}, then the span calculated
     * above is negative. In such cases, this method adds the periodicity (typically 360° of
     * longitude) to the span. If the result is a positive number, it is returned. Otherwise
     * this method returns {@link Double#NaN NaN}.
     *
     * @param  dimension The dimension for which to obtain the span.
     * @return The span (typically width or height) at the given dimension, or {@link Double#NaN}.
     * @throws IndexOutOfBoundsException If the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() envelope dimension}.
     */
    @Override
    public double getSpan(final int dimension) {
        double span = getUpper(dimension) - getLower(dimension);
        if (isNegative(span)) { // Special handling for -0.0
            span = fixSpan(getAxis(getCoordinateReferenceSystem(), dimension), span);
        }
        return span;
    }

    /**
     * Transforms a negative span into a valid value if the axis range meaning is "wraparound".
     * Returns {@code NaN} otherwise.
     *
     * @param  axis The axis for the span dimension, or {@code null}.
     * @param  span The negative span.
     * @return A positive span, or NaN if the span can not be fixed.
     */
    static double fixSpan(final CoordinateSystemAxis axis, double span) {
        if (axis != null && RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning())) {
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
     * @param  dimension The dimension to query.
     * @param  unit The unit for the return value.
     * @return The span in terms of the given unit.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     * @throws ConversionException if the length can't be converted to the specified units.
     */
    public double getSpan(final int dimension, final Unit<?> unit)
            throws IndexOutOfBoundsException, ConversionException
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
     * Determines whether or not this envelope is empty. An envelope is non-empty only if it has
     * at least one {@linkplain #getDimension() dimension}, and the {@linkplain #getSpan(int) span}
     * is greater than 0 along all dimensions. Note that a non-empty envelope is always
     * non-{@linkplain #isNull() null}, but the converse is not always true.
     *
     * @return {@code true} if this envelope is empty.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#isEmpty()
     * @see java.awt.geom.Rectangle2D#isEmpty()
     */
    public boolean isEmpty() {
        final int dimension = getDimension();
        if (dimension == 0) {
            return true;
        }
        for (int i=0; i<dimension; i++) {
            if (!(getSpan(i) > 0)) { // Use '!' in order to catch NaN
                return true;
            }
        }
        assert !isNull() : this;
        return false;
    }

    /**
     * Returns {@code false} if at least one ordinate value is not {@linkplain Double#NaN NaN}.
     * This {@code isNull()} check is a little bit different than the {@link #isEmpty()} check
     * since it returns {@code false} for a partially initialized envelope, while {@code isEmpty()}
     * returns {@code false} only after all dimensions have been initialized. More specifically,
     * the following rules apply:
     *
     * <ul>
     *   <li>If {@code isNull() == true}, then {@code isEmpty() == true}</li>
     *   <li>If {@code isEmpty() == false}, then {@code isNull() == false}</li>
     *   <li>The converse of the above-cited rules are not always true.</li>
     * </ul>
     *
     * @return {@code true} if this envelope has NaN values.
     *
     * @see GeneralEnvelope#setToNull()
     */
    public boolean isNull() {
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
     * If it least one ordinate value in the given point is {@link Double#NaN NaN},
     * then this method returns {@code false}.
     *
     * {@note This method assumes that the specified point uses the same CRS than this envelope.
     *        For performance raisons, it will no be verified unless Java assertions are enabled.}
     *
     * {@section Spanning the anti-meridian of a Geographic CRS}
     * For any dimension, if <var>upper</var> &lt; <var>lower</var> then this method uses an
     * algorithm which is the opposite of the usual one: rather than testing if the given point is
     * inside the envelope interior, this method tests if the given point is <em>outside</em> the
     * envelope <em>exterior</em>.
     *
     * @param  position The point to text.
     * @return {@code true} if the specified coordinate is inside the boundary of this envelope; {@code false} otherwise.
     * @throws MismatchedDimensionException if the specified point doesn't have the expected dimension.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     */
    public boolean contains(final DirectPosition position) throws MismatchedDimensionException {
        ensureNonNull("position", position);
        final int dimension = getDimension();
        AbstractDirectPosition.ensureDimensionMatch("point", position.getDimension(), dimension);
        assert equalsIgnoreMetadata(getCoordinateReferenceSystem(),
                position.getCoordinateReferenceSystem(), true) : position;
        for (int i=0; i<dimension; i++) {
            final double value = position.getOrdinate(i);
            final double lower = getLower(i);
            final double upper = getUpper(i);
            final boolean c1   = (value >= lower);
            final boolean c2   = (value <= upper);
            if (c1 & c2) {
                continue; // Point inside the range, check other dimensions.
            }
            if (c1 | c2) {
                if (isNegative(upper - lower)) {
                    /*
                     * "Spanning the anti-meridian" case: if we reach this point, then the
                     * [upper...lower] range  (note the 'lower' and 'upper' interchanging)
                     * is actually a space outside the envelope and we have checked that
                     * the ordinate value is outside that space.
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
     * If one or more edges from the specified envelope coincide with an edge from this
     * envelope, then this method returns {@code true} only if {@code edgesInclusive}
     * is {@code true}.
     *
     * {@note This method assumes that the specified envelope uses the same CRS than this envelope.
     *        For performance raisons, it will no be verified unless Java assertions are enabled.}
     *
     * {@section Spanning the anti-meridian of a Geographic CRS}
     * For every cases illustrated below, the yellow box is considered completely enclosed
     * in the blue envelope:
     *
     * <center><img src="doc-files/Contains.png"></center>
     *
     * @param  envelope The envelope to test for inclusion.
     * @param  edgesInclusive {@code true} if this envelope edges are inclusive.
     * @return {@code true} if this envelope completely encloses the specified one.
     * @throws MismatchedDimensionException if the specified envelope doesn't have
     *         the expected dimension.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see #intersects(Envelope, boolean)
     * @see #equals(Envelope, double, boolean)
     */
    public boolean contains(final Envelope envelope, final boolean edgesInclusive) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int dimension = getDimension();
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dimension);
        assert equalsIgnoreMetadata(getCoordinateReferenceSystem(),
                envelope.getCoordinateReferenceSystem(), true) : envelope;
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
                // If this envelope does not span the anti-meridian but the given envelope
                // does, we don't contain the given envelope except in the special case
                // where the envelope spanning is equals or greater than the axis spanning
                // (including the case where this envelope expands to infinities).
                if ((lower0 == Double.NEGATIVE_INFINITY && upper0 == Double.POSITIVE_INFINITY) ||
                    (upper0 - lower0 >= getSpan(getAxis(getCoordinateReferenceSystem(), i))))
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
     * If one or more edges from the specified envelope coincide with an edge from this envelope,
     * then this method returns {@code true} only if {@code edgesInclusive} is {@code true}.
     *
     * {@note This method assumes that the specified envelope uses the same CRS than this envelope.
     *        For performance raisons, it will no be verified unless Java assertions are enabled.}
     *
     * {@section Spanning the anti-meridian of a Geographic CRS}
     * This method can handle envelopes spanning the anti-meridian.
     *
     * @param  envelope The envelope to test for intersection.
     * @param  edgesInclusive {@code true} if this envelope edges are inclusive.
     * @return {@code true} if this envelope intersects the specified one.
     * @throws MismatchedDimensionException if the specified envelope doesn't have the expected dimension.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     *
     * @see #contains(Envelope, boolean)
     * @see #equals(Envelope, double, boolean)
     */
    public boolean intersects(final Envelope envelope, final boolean edgesInclusive) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int dimension = getDimension();
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dimension);
        assert equalsIgnoreMetadata(getCoordinateReferenceSystem(),
                envelope.getCoordinateReferenceSystem(), true) : envelope;
        final DirectPosition lowerCorner = envelope.getLowerCorner();
        final DirectPosition upperCorner = envelope.getUpperCorner();
        for (int i=0; i<dimension; i++) {
            final double lower0 = getLower(i);
            final double upper0 = getUpper(i);
            final double lower1 = lowerCorner.getOrdinate(i);
            final double upper1 = upperCorner.getOrdinate(i);
            final boolean lowerCondition, upperCondition;
            if (edgesInclusive) {
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
                 * If both envelopes span the anti-meridian (sp0 & sp1), we have an unconditional
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
                    !contains(new ArrayEnvelope(envelope), edgesInclusive) : envelope;
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if at least one ordinate in the given envelope
     * is {@link Double#NaN}. This is used for assertions only.
     */
    static boolean hasNaN(final Envelope envelope) {
        return hasNaN(envelope.getLowerCorner()) || hasNaN(envelope.getUpperCorner());
    }

    /**
     * Returns {@code true} if at least one ordinate in the given position
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
     * ground resolution of a {@linkplain org.opengis.coverage.grid.GridCoverage.GridCoverage
     * grid coverage}).
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
     * {@note Relative tolerance value (as opposed to absolute tolerance value) help to workaround
     * the fact that tolerance value are CRS dependent. For example the tolerance value need to be
     * smaller for geographic CRS than for UTM projections, because the former typically has a
     * [-180…180]° range while the later can have a range of thousands of meters.}
     *
     * {@section Coordinate Reference System}
     * To be considered equal, the two envelopes must have the same {@linkplain #getDimension() dimension}
     * and their CRS must be {@linkplain org.apache.sis.util.Utilities#equalsIgnoreMetadata equals,
     * ignoring metadata}. If at least one envelope has a null CRS, then the CRS are ignored and the
     * ordinate values are compared as if the CRS were equal.
     *
     * @param  other The envelope to compare with.
     * @param  eps   The tolerance value to use for numerical comparisons.
     * @param  epsIsRelative {@code true} if the tolerance value should be relative to
     *         axis length, or {@code false} if it is an absolute value.
     * @return {@code true} if the given object is equal to this envelope up to the given tolerance value.
     *
     * @see #contains(Envelope, boolean)
     * @see #intersects(Envelope, boolean)
     */
    public boolean equals(final Envelope other, final double eps, final boolean epsIsRelative) {
        ensureNonNull("other", other);
        final int dimension = getDimension();
        if (other.getDimension() != dimension || !equalsIgnoreMetadata(
                getCoordinateReferenceSystem(), other.getCoordinateReferenceSystem(), false))
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

    /**
     * Returns {@code true} if the specified object is an envelope of the same class
     * with equals coordinates and {@linkplain #getCoordinateReferenceSystem() CRS}.
     *
     * {@note This implementation requires that the provided <code>object</code> argument
     * is of the same class than this envelope. We do not relax this rule since not every
     * implementations in the SIS code base follow the same contract.}
     *
     * @param object The object to compare with this envelope.
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
                final long bits = doubleToLongBits(p ? getLower(i) : getUpper(i));
                code = 31 * code + (((int) bits) ^ (int) (bits >>> 32));
            }
        } while ((p = !p) == false);
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        if (crs != null) {
            code += crs.hashCode();
        }
        return code;
    }

    /**
     * Formats this envelope in the <cite>Well Known Text</cite> (WKT) format.
     * The output is of the form "{@code BOX}<var>n</var>{@code D(}{@linkplain #getLowerCorner()
     * lower corner}{@code ,}{@linkplain #getUpperCorner() upper corner}{@code )}"
     * where <var>n</var> is the {@linkplain #getDimension() number of dimensions}.
     * Example:
     *
     * {@preformat wkt
     *   BOX3D(-90 -180 0, 90 180 1)
     * }
     *
     * The string returned by this method can be {@linkplain GeneralEnvelope#GeneralEnvelope(CharSequence) parsed}
     * by the {@code GeneralEnvelope} constructor.
     *
     * @return This envelope as a {@code BOX2D} or {@code BOX3D} (most typical dimensions) in WKT format.
     */
    @Override
    public String toString() {
        return toString(this);
    }

    /**
     * Implementation of the public {@link #toString()} and {@link Envelopes#toWKT(Envelope)} methods
     * for formatting a {@code BOX} element from an envelope in <cite>Well Known Text</cite> (WKT) format.
     *
     * @param  envelope The envelope to format.
     * @return The envelope as a {@code BOX2D} or {@code BOX3D} (most typical dimensions) in WKT format.
     *
     * @see GeneralEnvelope#GeneralEnvelope(String)
     * @see org.apache.sis.measure.CoordinateFormat
     * @see org.apache.sis.io.wkt
     */
    static String toString(final Envelope envelope) {
        final int            dimension   = envelope.getDimension();
        final DirectPosition lowerCorner = envelope.getLowerCorner();
        final DirectPosition upperCorner = envelope.getUpperCorner();
        final StringBuilder  buffer = new StringBuilder(64).append("BOX").append(dimension).append("D(");
        for (int i=0; i<dimension; i++) {
            if (i != 0) {
                buffer.append(' ');
            }
            trimFractionalPart(buffer.append(lowerCorner.getOrdinate(i)));
        }
        buffer.append(',');
        for (int i=0; i<dimension; i++) {
            trimFractionalPart(buffer.append(' ').append(upperCorner.getOrdinate(i)));
        }
        return buffer.append(')').toString();
    }

    /**
     * Base class for unmodifiable direct positions backed by the enclosing envelope.
     * Subclasses must override the {@link #getOrdinate(int)} method in order to delegate
     * the work to the appropriate {@link AbstractEnvelope} method.
     *
     * <p>Instance of this class are serializable if the enclosing envelope is serializable.</p>
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.3 (derived from geotk-2.4)
     * @version 0.3
     * @module
     */
    private abstract class Point extends AbstractDirectPosition implements Serializable {
        private static final long serialVersionUID = 9051824576982927750L;

        /** The coordinate reference system in which the coordinate is given. */
        @Override public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return AbstractEnvelope.this.getCoordinateReferenceSystem();
        }

        /** The length of coordinate sequence (the number of entries). */
        @Override public final int getDimension() {
            return AbstractEnvelope.this.getDimension();
        }

        /** Sets the ordinate value along the specified dimension. */
        @Override public final void setOrdinate(int dimension, double value) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The corner returned by {@link AbstractEnvelope#getLowerCorner()}.
     */
    private final class LowerCorner extends Point {
        private static final long serialVersionUID = 1342844299471364436L;

        @Override public double getOrdinate(final int dimension) throws IndexOutOfBoundsException {
            return getLower(dimension);
        }
    }

    /**
     * The corner returned by {@link AbstractEnvelope#getUpperCorner()}.
     */
    private final class UpperCorner extends Point {
        private static final long serialVersionUID = 8999737674570427517L;

        @Override public double getOrdinate(final int dimension) throws IndexOutOfBoundsException {
            return getUpper(dimension);
        }
    }

    /**
     * The point returned by {@link AbstractEnvelope#getMedian()}.
     */
    private final class Median extends Point {
        private static final long serialVersionUID = 4204675972453668922L;

        @Override public double getOrdinate(final int dimension) throws IndexOutOfBoundsException {
            return getMedian(dimension);
        }
    }
}
