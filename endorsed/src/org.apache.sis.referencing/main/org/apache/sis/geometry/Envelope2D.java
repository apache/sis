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

import java.util.Objects;
import java.awt.geom.Rectangle2D;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Double.doubleToLongBits;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.resources.Errors;
import static org.apache.sis.math.MathFunctions.isSameSign;
import static org.apache.sis.math.MathFunctions.isPositive;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.math.MathFunctions.isNegativeZero;
import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;
import static org.apache.sis.referencing.internal.shared.Formulas.isPoleToPole;
import static org.apache.sis.geometry.AbstractEnvelope.getAxis;
import static org.apache.sis.geometry.AbstractEnvelope.isWrapAround;
import static org.apache.sis.geometry.AbstractEnvelope.isNegativeUnsafe;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coordinate.MismatchedCoordinateMetadataException;


/**
 * A two-dimensional envelope on top of Java2D rectangle.
 * This implementation is provided for inter-operability between Java2D and GeoAPI.
 *
 * <p>This class inherits {@linkplain #x x} and {@linkplain #y y} fields.
 * But despite their names, they don't need to be oriented toward {@linkplain AxisDirection#EAST East} and
 * {@linkplain AxisDirection#NORTH North} respectively. The (<var>x</var>,<var>y</var>) axis can have any
 * direction and should be understood as <dfn>coordinate 0</dfn> and <dfn>coordinate 1</dfn> values instead.
 * This is not specific to this implementation; in Java2D too, the visual axis orientation depend
 * on the {@linkplain java.awt.Graphics2D#getTransform() affine transform in the graphics context}.</p>
 *
 * <h2>Crossing the anti-meridian of a Geographic CRS</h2>
 * The <cite>Web Coverage Service</cite> (WCS) specification authorizes (with special treatment)
 * cases where <var>upper</var> &lt; <var>lower</var> at least in the longitude case. They are
 * envelopes crossing the anti-meridian, like the red box below (the green box is the usual case).
 * For {@code Envelope2D} objects, they are rectangle with negative {@linkplain #width width} or
 * {@linkplain #height height} field values. The default implementation of methods listed in the
 * right column can handle such cases.
 *
 * <div class="horizontal-flow">
 * <div>
 *   <img style="vertical-align: middle" src="doc-files/AntiMeridian.png" alt="Envelope crossing the anti-meridian">
 * </div><div>
 * Supported methods:
 * <ul>
 *   <li>{@link #getMinimum(int)}</li>
 *   <li>{@link #getMaximum(int)}</li>
 *   <li>{@link #getSpan(int)}</li>
 *   <li>{@link #getMedian(int)}</li>
 *   <li>{@link #isEmpty()}</li>
 *   <li>{@link #toRectangles()}</li>
 *   <li>{@link #contains(double,double)}</li>
 *   <li>{@link #contains(Rectangle2D)} and its variant receiving {@code double} arguments</li>
 *   <li>{@link #intersects(Rectangle2D)} and its variant receiving {@code double} arguments</li>
 *   <li>{@link #createIntersection(Rectangle2D)}</li>
 *   <li>{@link #createUnion(Rectangle2D)}</li>
 *   <li>{@link #add(Rectangle2D)}</li>
 *   <li>{@link #add(double,double)}</li>
 * </ul>
 * </div></div>
 *
 * The {@link #getMinX()}, {@link #getMinY()}, {@link #getMaxX()}, {@link #getMaxY()},
 * {@link #getCenterX()}, {@link #getCenterY()}, {@link #getWidth()} and {@link #getHeight()}
 * methods delegate to the above-cited methods.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 *
 * @see GeneralEnvelope
 * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox
 *
 * @since 0.3
 */
public class Envelope2D extends Rectangle2D.Double implements Envelope, Emptiable, Cloneable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 761232175464415062L;

    /**
     * The number of dimensions in every {@code Envelope2D}.
     */
    private static final int DIMENSION = 2;

    /**
     * An empty array of Java2D rectangles, to be returned by {@link #toRectangles()}
     * when en envelope is empty.
     */
    private static final Rectangle2D.Double[] EMPTY = new Rectangle2D.Double[0];

    /**
     * The coordinate reference system, or {@code null}.
     */
    @SuppressWarnings("serial")             // Most SIS implementations are serializable.
    private CoordinateReferenceSystem crs;

    /**
     * Constructs an initially empty envelope with no CRS.
     */
    public Envelope2D() {
    }

    /**
     * Creates a new envelope from the given bounding box. This constructor cannot be public,
     * because the {@code xmax} and {@code ymax} arguments are not the ones usually expected for
     * {@link Rectangle2D} objects (the standard arguments are {@code width} and {@code height}).
     * Making this constructor public would probably be a too high risk of confusion.
     *
     * <p>This constructor is needed because the other constructors (expecting envelopes or other
     * rectangles) cannot query directly the {@link Envelope#getSpan(int)} or equivalent methods,
     * because the return value is not the one expected by this class when the envelope spans the
     * anti-meridian.</p>
     */
    private Envelope2D(final double xmin, final double ymin, final double xmax, final double ymax) {
        super(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /**
     * Creates a new envelope from the given positions and CRS.
     * It is the caller responsibility to check the validity of the given CRS.
     *
     * @see #Envelope2D(DirectPosition, DirectPosition)
     */
    private Envelope2D(final CoordinateReferenceSystem crs,
                       final DirectPosition lowerCorner,
                       final DirectPosition upperCorner)
    {
        /*
         * JDK constraint: The call to ensureDimensionMatch(…) should have been first if Sun/Oracle
         * fixed RFE #4093999 (Relax constraint on placement of this()/super() call in constructors).
         */
        this(lowerCorner.getCoordinate(0), lowerCorner.getCoordinate(1),
             upperCorner.getCoordinate(0), upperCorner.getCoordinate(1));
        ensureDimensionMatches("crs", DIMENSION, crs);
        this.crs = crs;
    }

    /**
     * Constructs a two-dimensional envelope defined by the specified coordinates.
     * The {@code lowerCorner} and {@code upperCorner} arguments are not necessarily
     * the minimal and maximal values respectively.
     * See the class javadoc about crossing the anti-meridian for more details.
     *
     * @param  lowerCorner  the first position.
     * @param  upperCorner  the second position.
     * @throws MismatchedCoordinateMetadataException if the two positions don't use the same CRS.
     * @throws MismatchedDimensionException if the two positions are not two-dimensional.
     */
    public Envelope2D(final DirectPosition lowerCorner, final DirectPosition upperCorner)
            throws MismatchedCoordinateMetadataException, MismatchedDimensionException
    {
        this(AbstractEnvelope.getCommonCRS(lowerCorner, upperCorner), lowerCorner, upperCorner);
    }

    /**
     * Constructs a two-dimensional envelope defined by another {@link Envelope}.
     *
     * @param  envelope  the envelope to copy (cannot be {@code null}).
     * @throws MismatchedDimensionException if the given envelope is not two-dimensional.
     */
    public Envelope2D(final Envelope envelope) throws MismatchedDimensionException {
        this(envelope.getCoordinateReferenceSystem(), envelope.getLowerCorner(), envelope.getUpperCorner());
    }

    /**
     * Constructs a new envelope with the same data as the specified geographic bounding box.
     * The coordinate reference system is set to the
     * {@linkplain org.apache.sis.referencing.CommonCRS#defaultGeographic() default geographic CRS}.
     * Axis order is (<var>longitude</var>, <var>latitude</var>).
     *
     * @param box The bounding box to copy (cannot be {@code null}).
     */
    public Envelope2D(final GeographicBoundingBox box) {
        this(box.getWestBoundLongitude(),
             box.getSouthBoundLatitude(),
             box.getEastBoundLongitude(),
             box.getNorthBoundLatitude());
        crs = CommonCRS.defaultGeographic();
        if (Boolean.FALSE.equals(box.getInclusion())) {
            x += width;
            width = -width;
            if (!isPoleToPole(y, y+height)) {
                y += height;
                height = -height;
            }
        }
    }

    /**
     * Constructs two-dimensional envelope defined by another {@link Rectangle2D}.
     * If the given rectangle has negative width or height, they will be interpreted
     * as an envelope crossing the anti-meridian.
     *
     * @param crs   the coordinate reference system, or {@code null}.
     * @param rect  the rectangle to copy (cannot be {@code null}).
     * @throws MismatchedDimensionException if the given CRS is not two-dimensional.
     */
    public Envelope2D(final CoordinateReferenceSystem crs, final Rectangle2D rect)
            throws MismatchedDimensionException
    {
        super(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());     // Really 'super', not 'this'.
        ensureDimensionMatches("crs", DIMENSION, crs);
        this.crs = crs;
    }

    /**
     * Constructs two-dimensional envelope defined by the specified coordinates. Despite
     * their name, the (<var>x</var>,<var>y</var>) coordinates don't need to be oriented
     * toward ({@linkplain AxisDirection#EAST East}, {@linkplain AxisDirection#NORTH North}).
     * Those parameter names simply match the {@linkplain #x x} and {@linkplain #y y} fields.
     * The actual axis orientations are determined by the specified CRS.
     * See the <a href="#skip-navbar_top">class javadoc</a> for details.
     *
     * @param  crs     the coordinate reference system, or {@code null}.
     * @param  x       the <var>x</var> minimal value.
     * @param  y       the <var>y</var> minimal value.
     * @param  width   the envelope width. May be negative for envelope crossing the anti-meridian.
     * @param  height  the envelope height. May be negative for envelope crossing the anti-meridian.
     * @throws MismatchedDimensionException if the given CRS is not two-dimensional.
     */
    public Envelope2D(final CoordinateReferenceSystem crs, final double x, final double y,
            final double width, final double height) throws MismatchedDimensionException
    {
        super(x, y, width, height);                             // Really 'super', not 'this'.
        ensureDimensionMatches("crs", DIMENSION, crs);
        this.crs = crs;
    }

    /**
     * Returns the coordinate reference system in which the coordinates are given.
     *
     * @return the coordinate reference system, or {@code null}.
     */
    @Override
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Sets the coordinate reference system in which the coordinate are given.
     * This method <strong>does not</strong> reproject the envelope.
     * If the envelope coordinates need to be transformed to the new CRS, consider using
     * {@link Envelopes#transform(Envelope, CoordinateReferenceSystem)} instead.
     *
     * @param  crs  the new coordinate reference system, or {@code null}.
     */
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        ensureDimensionMatches("crs", DIMENSION, crs);
        this.crs = crs;
    }

    /**
     * Sets this envelope to the given rectangle. If the given rectangle is also an instance of {@link Envelope}
     * (typically as another {@code Envelope2D}) and has a non-null Coordinate Reference System (CRS), then the
     * CRS of this envelope will be set to the CRS of the given envelope.
     *
     * @param rect  the rectangle to copy coordinates from.
     *
     * @since 0.8
     */
    @Override
    public void setRect(final Rectangle2D rect) {
        if (rect == this) {
            return;         // Optimization for methods chaining like env.setRect(Shapes.transform(…, env))
        }
        if (rect instanceof Envelope) {
            final CoordinateReferenceSystem envelopeCRS = ((Envelope) rect).getCoordinateReferenceSystem();
            if (envelopeCRS != null) {
                setCoordinateReferenceSystem(envelopeCRS);
            }
        }
        super.setRect(rect);
    }

    /**
     * Returns the number of dimensions, which is always 2.
     *
     * @return always 2 for bi-dimensional objects.
     */
    @Override
    public final int getDimension() {
        return DIMENSION;
    }

    /**
     * The limits in the direction of decreasing coordinate values for the two dimensions.
     * This is typically a coordinate position consisting of the minimal coordinates for
     * the two dimensions for all points within the {@code Envelope}.
     *
     * <p>The object returned by this method is a copy. Change in the returned position
     * will not affect this envelope, and conversely.</p>
     *
     * <h4>Note on wraparound</h4>
     * The <cite>Web Coverage Service</cite> (WCS) 1.1 specification uses an extended interpretation of the
     * bounding box definition. In a WCS 1.1 data structure, the lower corner defines the edges region in the
     * directions of <em>decreasing</em> coordinate values in the envelope CRS. This is usually the algebraic
     * minimum coordinates, but not always. For example, an envelope crossing the anti-meridian could have a
     * lower corner longitude greater than the upper corner longitude. Such extended interpretation applies
     * mostly to axes having {@code WRAPAROUND} range meaning.
     *
     * @return a copy of the lower corner, typically (but not necessarily) containing minimal coordinate values.
     *
     * @see #getMinX()
     * @see #getMinY()
     * @see #getMinimum(int)
     */
    @Override
    public DirectPosition2D getLowerCorner() {
        return new DirectPosition2D(crs, x, y);
    }

    /**
     * The limits in the direction of increasing coordinate values for the two dimensions.
     * This is typically a coordinate position consisting of the maximal coordinates for
     * the two dimensions for all points within the {@code Envelope}.
     *
     * <p>The object returned by this method is a copy. Change in the returned position
     * will not affect this envelope, and conversely.</p>
     *
     * <h4>Note on wraparound</h4>
     * The <cite>Web Coverage Service</cite> (WCS) 1.1 specification uses an extended interpretation of the
     * bounding box definition. In a WCS 1.1 data structure, the upper corner defines the edges region in the
     * directions of <em>increasing</em> coordinate values in the envelope CRS. This is usually the algebraic
     * maximum coordinates, but not always. For example, an envelope crossing the anti-meridian could have an
     * upper corner longitude less than the lower corner longitude. Such extended interpretation applies
     * mostly to axes having {@code WRAPAROUND} range meaning.
     *
     * @return a copy of the upper corner, typically (but not necessarily) containing maximal coordinate values.
     *
     * @see #getMaxX()
     * @see #getMaxY()
     * @see #getMaximum(int)
     */
    @Override
    public DirectPosition2D getUpperCorner() {
        return new DirectPosition2D(crs,
                (x != 0) ? x+width  : width,        // Preserve the sign of `width` if -0.0.
                (y != 0) ? y+height : height);
    }

    /**
     * A coordinate position consisting of all the median coordinate values.
     *
     * <p>The object returned by this method is a copy. Change in the returned position
     * will not affect this envelope, and conversely.</p>
     *
     * @return a copy of the median coordinates.
     *
     * @see #getMedian(int)
     *
     * @since 1.1
     */
    public DirectPosition2D getMedian() {
        return new DirectPosition2D(crs, getMedian(0), getMedian(1));
    }

    /**
     * Creates an exception for an index out of bounds.
     */
    private static IndexOutOfBoundsException indexOutOfBounds(final int dimension) {
        return new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, dimension));
    }

    /**
     * Returns the minimal coordinate along the specified dimension. This method handles
     * anti-meridian as documented in the {@link AbstractEnvelope#getMinimum(int)} method.
     *
     * @param  dimension  the dimension to query.
     * @return the minimal coordinate value along the given dimension.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public double getMinimum(final int dimension) throws IndexOutOfBoundsException {
        final double value, span;
        switch (dimension) {
            case 0:  value=x; span=width;  break;
            case 1:  value=y; span=height; break;
            default: throw indexOutOfBounds(dimension);
        }
        if (isNegative(span)) {                                         // Special handling for -0.0
            final CoordinateSystemAxis axis = getAxis(crs, dimension);
            return isWrapAround(axis) ? axis.getMinimumValue() : NaN;
        }
        return value;
    }

    /**
     * Returns the maximal coordinate along the specified dimension. This method handles
     * anti-meridian as documented in the {@link AbstractEnvelope#getMaximum(int)} method.
     *
     * @param  dimension  the dimension to query.
     * @return the maximal coordinate value along the given dimension.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public double getMaximum(final int dimension) throws IndexOutOfBoundsException {
        final double value, span;
        switch (dimension) {
            case 0:  value=x; span=width;  break;
            case 1:  value=y; span=height; break;
            default: throw indexOutOfBounds(dimension);
        }
        if (isNegative(span)) {                                         // Special handling for -0.0
            final CoordinateSystemAxis axis = getAxis(crs, dimension);
            return isWrapAround(axis) ? axis.getMaximumValue() : NaN;
        }
        return value + span;
    }

    /**
     * Returns the median coordinate along the specified dimension. This method handles
     * anti-meridian as documented in the {@link AbstractEnvelope#getMedian(int)} method.
     *
     * @param  dimension  the dimension to query.
     * @return the mid coordinate value along the given dimension.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     *
     * @see #getMedian()
     */
    @Override
    public double getMedian(final int dimension) throws IndexOutOfBoundsException {
        double value, span;
        switch (dimension) {
            case 0:  value=x; span=width;  break;
            case 1:  value=y; span=height; break;
            default: throw indexOutOfBounds(dimension);
        }
        value += 0.5*span;
        if (isNegative(span)) {                                         // Special handling for -0.0
            value = AbstractEnvelope.fixMedian(getAxis(crs, dimension), value);
        }
        return value;
    }

    /**
     * Returns the envelope span along the specified dimension. This method handles
     * anti-meridian as documented in the {@link AbstractEnvelope#getSpan(int)} method.
     *
     * @param  dimension  the dimension to query.
     * @return the rectangle width or height, depending the given dimension.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
      */
    @Override
    public double getSpan(final int dimension) throws IndexOutOfBoundsException {
        double span;
        switch (dimension) {
            case 0:  span=width;  break;
            case 1:  span=height; break;
            default: throw indexOutOfBounds(dimension);
        }
        if (isNegative(span)) {                                         // Special handling for -0.0
            span = AbstractEnvelope.fixSpan(getAxis(crs, dimension), span);
        }
        return span;
    }

    // Do not override getX() and getY() - their default implementations is okay.

    /**
     * Returns the {@linkplain #getMinimum(int) minimal} coordinate value for dimension 0.
     * The default implementation invokes <code>{@linkplain #getMinimum(int) getMinimum}(0)</code>.
     * The result is the standard {@link Rectangle2D} value (namely {@linkplain #x x})
     * only if the envelope is not crossing the anti-meridian.
     *
     * @return the minimal coordinate value for dimension 0.
     */
    @Override
    public double getMinX() {
        return getMinimum(0);
    }

    /**
     * Returns the {@linkplain #getMinimum(int) minimal} coordinate value for dimension 1.
     * The default implementation invokes <code>{@linkplain #getMinimum(int) getMinimum}(1)</code>.
     * The result is the standard {@link Rectangle2D} value (namely {@linkplain #y y})
     * only if the envelope is not crossing the anti-meridian.
     *
     * @return the minimal coordinate value for dimension 1.
     */
    @Override
    public double getMinY() {
        return getMinimum(1);
    }

    /**
     * Returns the {@linkplain #getMaximum(int) maximal} coordinate value for dimension 0.
     * The default implementation invokes <code>{@linkplain #getMaximum(int) getMinimum}(0)</code>.
     * The result is the standard {@link Rectangle2D} value (namely {@linkplain #x x} + {@linkplain #width width})
     * only if the envelope is not crossing the anti-meridian.
     *
     * @return the maximal coordinate value for dimension 0.
     */
    @Override
    public double getMaxX() {
        return getMaximum(0);
    }

    /**
     * Returns the {@linkplain #getMaximum(int) maximal} coordinate value for dimension 1.
     * The default implementation invokes <code>{@linkplain #getMaximum(int) getMinimum}(1)</code>.
     * The result is the standard {@link Rectangle2D} value (namely {@linkplain #y y} + {@linkplain #height height})
     * only if the envelope is not crossing the anti-meridian.
     *
     * @return the maximal coordinate value for dimension 1.
     */
    @Override
    public double getMaxY() {
        return getMaximum(1);
    }

    /**
     * Returns the {@linkplain #getMedian(int) median} coordinate value for dimension 0.
     * The default implementation invokes <code>{@linkplain #getMedian(int) getMedian}(0)</code>.
     * The result is the standard {@link Rectangle2D} value (namely {@linkplain #x x} + {@linkplain #width width}/2)
     * only if the envelope is not crossing the anti-meridian.
     *
     * @return the median coordinate value for dimension 0.
     */
    @Override
    public double getCenterX() {
        return getMedian(0);
    }

    /**
     * Returns the {@linkplain #getMedian(int) median} coordinate value for dimension 1.
     * The default implementation invokes <code>{@linkplain #getMedian(int) getMedian}(1)</code>.
     * The result is the standard {@link Rectangle2D} value (namely {@linkplain #y y} + {@linkplain #height height}/2)
     * only if the envelope is not crossing the anti-meridian.
     *
     * @return the median coordinate value for dimension 1.
     */
    @Override
    public double getCenterY() {
        return getMedian(1);
    }

    /**
     * Returns the {@linkplain #getSpan(int) span} for dimension 0.
     * The default implementation invokes <code>{@linkplain #getSpan(int) getSpan}(0)</code>.
     * The result is the standard {@link Rectangle2D} value (namely {@linkplain #width width})
     * only if the envelope is not crossing the anti-meridian.
     *
     * @return the span for dimension 0.
     */
    @Override
    public double getWidth() {
        return getSpan(0);
    }

    /**
     * Returns the {@linkplain #getSpan(int) span} for dimension 1.
     * The default implementation invokes <code>{@linkplain #getSpan(int) getSpan}(1)</code>.
     * The result is the standard {@link Rectangle2D} value (namely {@linkplain #height height})
     * only if the envelope is not crossing the anti-meridian.
     *
     * @return the span for dimension 1.
     */
    @Override
    public double getHeight() {
        return getSpan(1);
    }

    /**
     * Determines whether the envelope is empty. A negative {@linkplain #width} or
     * (@linkplain #height} is considered as a non-empty area if the corresponding
     * axis has the {@linkplain org.opengis.referencing.cs.RangeMeaning#WRAPAROUND
     * wraparound} range meaning.
     *
     * <p>Note that if the {@linkplain #width} or {@linkplain #height} value is
     * {@link java.lang.Double#NaN NaN}, then the envelope is considered empty.
     * This is different than the default {@link java.awt.geom.Rectangle2D.Double#isEmpty()}
     * implementation, which doesn't check for {@code NaN} values.</p>
     *
     * @return {@code true} if this envelope is empty.
     */
    @Override
    public boolean isEmpty() {
        return !((width  > 0 || (isNegative(width)  && isWrapAround(crs, 0)))
              && (height > 0 || (isNegative(height) && isWrapAround(crs, 1))));
    }

    /**
     * Returns this envelope as non-empty Java2D rectangle objects. This method returns an array of length 0, 1,
     * 2 or 4 depending on whether the envelope crosses the anti-meridian or the limit of any other axis having
     * {@linkplain org.opengis.referencing.cs.RangeMeaning#WRAPAROUND wraparound} range meaning.
     * More specifically:
     *
     * <ul>
     *   <li>If this envelope {@linkplain #isEmpty() is empty}, then this method returns an empty array.</li>
     *   <li>If this envelope does not have any wraparound behavior, then this method returns a copy
     *       of this envelope as an instance of {@code Rectangle2D.Double} in an array of length 1.</li>
     *   <li>If this envelope crosses the <i>anti-meridian</i> (a.k.a. <i>date line</i>)
     *       then this method represents this envelope as two separated rectangles.
     *   <li>While uncommon, the envelope could theoretically crosses the limit of other axis having
     *       wraparound range meaning. If wraparound occur along the two axes, then this method
     *       represents this envelope as four separated rectangles.
     * </ul>
     *
     * <div class="note"><b>API note:</b>
     * The return type is the {@code Rectangle2D.Double} implementation class rather than the {@code Rectangle2D}
     * abstract class because the {@code Envelope2D} class hierarchy already exposes this implementation choice.</div>
     *
     * @return a representation of this envelope as an array of non-empty Java2D rectangles.
     *         The array never contains {@code this}.
     *
     * @see GeneralEnvelope#toSimpleEnvelopes()
     *
     * @since 0.4
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Rectangle2D.Double[] toRectangles() {
        int isWrapAround = 0;                   // A bitmask of the dimensions having a "wrap around" behavior.
        for (int i=0; i!=DIMENSION; i++) {
            final double span = (i == 0) ? width : height;
            if (!(span > 0)) {                                                      // Use '!' for catching NaN.
                if (!isNegative(span) || !isWrapAround(crs, i)) {
                    return EMPTY;
                }
                isWrapAround |= (1 << i);
            }
        }
        /*
         * The number of rectangles is 2ⁿ where n is the number of wraparound found.
         */
        final Rectangle2D.Double[] rect = new Rectangle2D.Double[1 << Integer.bitCount(isWrapAround)];
        for (int i=0; i<rect.length; i++) {
            rect[i] = new Rectangle2D.Double(x, y, width, height);
        }
        if ((isWrapAround & 1) != 0) {
            /*
             *  (x+width)   (x)
             *          ↓   ↓
             *    ──────┐   ┌───────
             *    …next │   │ start…
             *    ──────┘   └───────
             */
            final CoordinateSystemAxis axis = getAxis(crs, 0);
            final Rectangle2D.Double start = rect[0];
            final Rectangle2D.Double next  = rect[1];
            start.width = axis.getMaximumValue() - x;
            next.x      = axis.getMinimumValue();
            next.width += x - next.x;
        }
        if ((isWrapAround & 2) != 0) {
            /*
             *              │   ⋮   │
             *              │ start │
             * (y)        → └───────┘
             * (y+height) → ┌───────┐
             *              │ next  │
             *              │   ⋮   │
             */
            final CoordinateSystemAxis axis = getAxis(crs, 1);
            final Rectangle2D.Double start = rect[0];
            final Rectangle2D.Double next  = rect[isWrapAround - 1];    // == 1 if y is the only wraparound axis, or 2 otherwise.
            start.height = axis.getMaximumValue() - y;
            next.y       = axis.getMinimumValue();
            next.height += y - next.y;
        }
        if (isWrapAround == 3) {
            /*
             * If there is a wraparound along both axes, copy the values.
             * The (x) and (y) labels indicate which values to copy.
             *
             *      (y) R1 │   │ R0
             *    ─────────┘   └─────────
             *    ─────────┐   ┌─────────
             *    (x,y) R3 │   │ R2 (x)
             */
            rect[1].height = rect[0].height;
            rect[2].width  = rect[0].width;
            rect[3].x      = rect[1].x;
            rect[3].width  = rect[1].width;
            rect[3].y      = rect[2].y;
            rect[3].height = rect[2].height;
        }
        return rect;
    }

    /**
     * Tests if a specified coordinate is inside the boundary of this envelope. If it least one
     * of the given coordinate value is {@link java.lang.Double#NaN NaN}, then this method returns
     * {@code false}.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports anti-meridian in the same way as
     * {@link AbstractEnvelope#contains(DirectPosition)}.
     *
     * @param  px  the first coordinate value of the point to text.
     * @param  py  the second coordinate value of the point to text.
     * @return {@code true} if the specified coordinate is inside the boundary of this envelope;
     *         {@code false} otherwise.
     */
    @Override
    public boolean contains(final double px, final double py) {
        boolean c1 = (px >= x);
        boolean c2 = (px <= x + width);
        // See AbstractEnvelope.contains(DirectPosition) for explanation.
        if ((c1 & c2) || ((c1 | c2) && isNegative(width))) {
            // Same check, but for y axis.
            c1 = (py >= y);
            c2 = (py <= y + height);
            return (c1 & c2) || ((c1 | c2) && isNegative(height));
        }
        return false;
    }

    /**
     * Returns {@code true} if this envelope completely encloses the specified rectangle. If this
     * envelope or the given rectangle have at least one {@link java.lang.Double#NaN NaN} value,
     * then this method returns {@code false}.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports anti-meridian in the same way as
     * {@link AbstractEnvelope#contains(Envelope)}.
     *
     * @param  rect  the rectangle to test for inclusion.
     * @return {@code true} if this envelope completely encloses the specified rectangle.
     */
    @Override
    public boolean contains(final Rectangle2D rect) {
        if (rect instanceof Envelope2D) {
            // Need to bypass the overriden getWidth()/getHeight().
            final Envelope2D env = (Envelope2D) rect;
            return contains(env.x, env.y, env.width, env.height);
        }
        return super.contains(rect);
    }

    /**
     * Returns {@code true} if this envelope completely encloses the specified rectangle. If this
     * envelope or the given rectangle have at least one {@link java.lang.Double#NaN NaN} value,
     * then this method returns {@code false}.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports anti-meridian in the same way as
     * {@link AbstractEnvelope#contains(Envelope)}.
     *
     * @param  rx  the <var>x</var> coordinate of the lower corner of the rectangle to test for inclusion.
     * @param  ry  the <var>y</var> coordinate of the lower corner of the rectangle to test for inclusion.
     * @param  rw  the width of the rectangle to test for inclusion. May be negative if the rectangle spans the anti-meridian.
     * @param  rh  the height of the rectangle to test for inclusion. May be negative.
     * @return {@code true} if this envelope completely encloses the specified one.
     */
    @Override
    public boolean contains(final double rx, final double ry, final double rw, final double rh) {
        for (int i=0; i!=DIMENSION; i++) {
            final double min0, min1, span0, span1;
            if (i == 0) {
                min0 =  x;  span0 = width;
                min1 = rx;  span1 = rw;
            } else {
                min0 =  y;  span0 = height;
                min1 = ry;  span1 = rh;
            }
            /*
             * See AbstractEnvelope.contains(Envelope) for an illustration of the algorithm applied here.
             */
            final boolean minCondition = (min1 >= min0);
            final boolean maxCondition = (min1 + span1 <= min0 + span0);
            if (minCondition & maxCondition) {
                if (!isNegativeUnsafe(span1) || isNegativeUnsafe(span0)) {
                    continue;
                }
                if (span0 >= AbstractEnvelope.getCycle(getAxis(crs, i))) {
                    continue;
                }
            } else if (minCondition != maxCondition) {
                if (isNegative(span0) && isPositive(span1)) {
                    continue;
                }
            } else if (isNegativeZero(span0)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if this envelope intersects the specified envelope. If this envelope
     * or the given rectangle have at least one {@link java.lang.Double#NaN NaN} value, then this
     * method returns {@code false}.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports anti-meridian in the same way as
     * {@link AbstractEnvelope#intersects(Envelope)}.
     *
     * @param  rect  the rectangle to test for intersection.
     * @return {@code true} if this envelope intersects the specified rectangle.
     */
    @Override
    public boolean intersects(final Rectangle2D rect) {
        if (rect instanceof Envelope2D) {
            // Need to bypass the overriden getWidth()/getHeight().
            final Envelope2D env = (Envelope2D) rect;
            return intersects(env.x, env.y, env.width, env.height);
        }
        return super.intersects(rect);
    }

    /**
     * Returns {@code true} if this envelope intersects the specified envelope. If this envelope
     * or the given rectangle have at least one {@link java.lang.Double#NaN NaN} value, then this
     * method returns {@code false}.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports anti-meridian in the same way as
     * {@link AbstractEnvelope#intersects(Envelope)}.
     *
     * @param  rx  the <var>x</var> coordinate of the lower corner of the rectangle to test for intersection.
     * @param  ry  the <var>y</var> coordinate of the lower corner of the rectangle to test for intersection.
     * @param  rw  the width of the rectangle to test for inclusion. May be negative if the rectangle spans the anti-meridian.
     * @param  rh  the height of the rectangle to test for inclusion. May be negative.
     * @return {@code true} if this envelope intersects the specified rectangle.
     */
    @Override
    public boolean intersects(final double rx, final double ry, final double rw, final double rh) {
        for (int i=0; i!=DIMENSION; i++) {
            final double min0, min1, span0, span1;
            if (i == 0) {
                min0 =  x;  span0 = width;
                min1 = rx;  span1 = rw;
            } else {
                min0 =  y;  span0 = height;
                min1 = ry;  span1 = rh;
            }
            /*
             * See AbstractEnvelope.intersects(Envelope) for an illustration of the algorithm applied here.
             * We use < operator, not <=, for consistency with the standard "intersects" definition.
             */
            final boolean minCondition = (min1 < min0 + span0);
            final boolean maxCondition = (min1 + span1 > min0);
            if (maxCondition & minCondition) {
                continue;
            }
            final boolean sp0 = isNegative(span0);
            final boolean sp1 = isNegative(span1);
            if (sp0 | sp1) {
                if ((sp0 & sp1) | (maxCondition | minCondition)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Returns the intersection of this envelope with the specified rectangle. If this envelope
     * or the given rectangle have at least one {@link java.lang.Double#NaN NaN} values, then this
     * method returns an {@linkplain #isEmpty() empty} envelope.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports anti-meridian in the same way as
     * {@link GeneralEnvelope#intersect(Envelope)}.
     *
     * @param  rect  the rectangle to be intersected with this envelope.
     * @return the intersection of the given rectangle with this envelope.
     */
    @Override
    public Envelope2D createIntersection(final Rectangle2D rect) {
        final Envelope2D env = (rect instanceof Envelope2D) ? (Envelope2D) rect : null;
        final Envelope2D inter = new Envelope2D(crs, NaN, NaN, NaN, NaN);
        for (int i=0; i!=DIMENSION; i++) {
            final double min0, min1, span0, span1;
            if (i == 0) {
                min0  = x;
                span0 = width;
                min1  = rect.getX();
                span1 = (env != null) ? env.width : rect.getWidth();
            } else {
                min0  = y;
                span0 = height;
                min1  = rect.getY();
                span1 = (env != null) ? env.height : rect.getHeight();
            }
            /*
             * The purpose for (min != 0) test before addition is to preserve the sign of zero.
             * In the [0 … -0] range, the span is -0. But computing max = 0 + -0 result in +0,
             * while we need max = -0 in this case.
             */
            final double max0 = (min0 != 0) ? min0 + span0 : span0;
            final double max1 = (min1 != 0) ? min1 + span1 : span1;
            double min = Math.max(min0, min1);
            double max = Math.min(max0, max1);
            /*
             * See GeneralEnvelope.intersect(Envelope) for an explanation of the algorithm applied below.
             */
            if (isSameSign(span0, span1)) {                 // Always 'false' if any value is NaN.
                if ((min1 > max0 || max1 < min0) && !isNegativeUnsafe(span0)) {
                    continue;                               // No intersection: leave coordinate values to NaN
                }
            } else if (isNaN(span0) || isNaN(span1)) {
                continue;                                   // Leave coordinate values to NaN
            } else {
                int intersect = 0;                          // A bitmask of intersections (two bits).
                if (isNegativeUnsafe(span0)) {
                    if (min1 <= max0) {min = min1; intersect  = 1;}
                    if (max1 >= min0) {max = max1; intersect |= 2;}
                } else {
                    if (min0 <= max1) {min = min0; intersect  = 1;}
                    if (max0 >= min1) {max = max0; intersect |= 2;}
                }
                if (intersect == 0 || intersect == 3) {
                    final double csSpan = AbstractEnvelope.getCycle(getAxis(crs, i));
                    if (span1 >= csSpan || isNegativeZero(span1)) {
                        min = min0;
                        max = max0;
                    } else if (span0 >= csSpan || isNegativeZero(span0)) {
                        min = min1;
                        max = max1;
                    } else {
                        continue;                           // Leave coordinate values to NaN
                    }
                }
            }
            inter.setRange(i, min, max);
        }
        assert inter.isEmpty() || (contains(inter) && rect.contains(inter)) : inter;
        return inter;
    }

    /**
     * Returns the union of this envelope with the specified rectangle.
     * The default implementation clones this envelope, then delegates
     * to {@link #add(Rectangle2D)}.
     *
     * @param  rect  the rectangle to add to this envelope.
     * @return the union of the given rectangle with this envelope.
     */
    @Override
    public Envelope2D createUnion(final Rectangle2D rect) {
        final Envelope2D union = clone();
        union.add(rect);
        assert union.isEmpty() || (union.contains(this) && union.contains(rect)) : union;
        return union;
    }

    /**
     * Adds another rectangle to this rectangle. The resulting rectangle is the union of the
     * two {@code Rectangle} objects.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports anti-meridian in the same way as {@link GeneralEnvelope#add(Envelope)},
     * except if the result is a rectangle expanding to infinities. In that later case, the field values
     * are set to {@code NaN} because infinite values are a problematic in {@link Rectangle2D} objects.
     *
     * @param  rect  the rectangle to add to this envelope.
     */
    @Override
    public void add(final Rectangle2D rect) {
        final Envelope2D env = (rect instanceof Envelope2D) ? (Envelope2D) rect : null;
        for (int i=0; i!=DIMENSION; i++) {
            final double min0, min1, span0, span1;
            if (i == 0) {
                min0  = x;
                span0 = width;
                min1  = rect.getX();
                span1 = (env != null) ? env.width : rect.getWidth();
                x = width = NaN;
            } else {
                min0  = y;
                span0 = height;
                min1  = rect.getY();
                span1 = (env != null) ? env.height : rect.getHeight();
                y = height = NaN;
            }
            final double max0 = min0 + span0;
            final double max1 = min1 + span1;
            double min = Math.min(min0, min1);
            double max = Math.max(max0, max1);
            /*
             * See GeneralEnvelope.add(Envelope) for an explanation of the algorithm applied below.
             * Note that the "continue" statement has reverse meaning: coordinates are left to NaN.
             */
            final boolean sp0 = isNegative(span0);
            final boolean sp1 = isNegative(span1);
            if (sp0 == sp1) {
                if (sp0 && !isNegativeUnsafe(max - min)) {
                    continue;                                   // Leave coordinates to NaN.
                }
            } else if (sp0) {
                if (max1 <= max0 || min1 >= min0) {
                    min = min0;
                    max = max0;
                } else {
                    final double left  = min1 - max0;
                    final double right = min0 - max1;
                    if (!(left > 0 || right > 0)) {
                        continue;                               // Leave coordinates to NaN.
                    }
                    if (left > right) {min = min1; max = max0;}
                    if (right > left) {min = min0; max = max1;}
                }
            } else {
                if (max0 <= max1 || min0 >= min1) {
                    min = min1;
                    max = max1;
                } else {
                    final double left  = min0 - max1;
                    final double right = min1 - max0;
                    if (!(left > 0 || right > 0)) {
                        continue;                               // Leave coordinates to NaN.
                    }
                    if (left > right) {min = min0; max = max1;}
                    if (right > left) {min = min1; max = max0;}
                }
            }
            setRange(i, min, max);
        }
    }

    /**
     * Sets the envelope range along the specified dimension.
     *
     * @param  dimension  the dimension to set.
     * @param  minimum    the minimum value along the specified dimension.
     * @param  maximum    the maximum value along the specified dimension.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    private void setRange(final int dimension, final double minimum, final double maximum)
            throws IndexOutOfBoundsException
    {
        final double span = maximum - minimum;
        switch (dimension) {
            case 0: x = minimum; width  = span; break;
            case 1: y = minimum; height = span; break;
            default: throw indexOutOfBounds(dimension);
        }
    }

    /**
     * Adds a point to this rectangle. The resulting rectangle is the smallest rectangle that
     * contains both the original rectangle and the specified point.
     * <p>
     * After adding a point, a call to {@link #contains(double, double)} with the added point
     * as an argument will return {@code true}, except if one of the point coordinates was
     * {@link java.lang.Double#NaN} in which case the corresponding coordinate has been ignored.
     *
     * <h4>Crossing the anti-meridian of a Geographic CRS</h4>
     * This method supports anti-meridian in the same way as
     * {@link GeneralEnvelope#add(DirectPosition)}.
     *
     * @param  px  the first coordinate of the point to add.
     * @param  py  the second coordinate of the point to add.
     */
    @Override
    public void add(final double px, final double py) {
        double off = px - x;
        if (!isNegative(width)) {                           // Standard case, or NaN.
            if (off < 0) {x=px; width -= off;}
            if (off > width)   {width  = off;}
        } else if (off < 0) {
            final double r = width - off;
            if (r < 0) {
                if (r > off) width  = off;
                else {x=px;  width -= off;}
            }
        }
        off = py - y;
        if (!isNegative(height)) {
            if (off < 0) {y=py; height -= off;}
            if (off > height)  {height  = off;}
        } else if (off < 0) {
            final double r = height - off;
            if (r < 0) {
                if (r > off) height  = off;
                else {y=py;  height -= off;}
            }
        }
        assert contains(px, py) || isEmpty() || isNaN(px) || isNaN(py);
    }

    /**
     * Compares the specified object with this envelope for equality. If the given object is not
     * an instance of {@code Envelope2D}, then the two objects are compared as plain rectangles,
     * i.e. the {@linkplain #getCoordinateReferenceSystem() coordinate reference system} of this
     * envelope is ignored.
     *
     * <h4>Note on {@code hashCode()}</h4>
     * This class does not override the {@link #hashCode()} method for consistency with the
     * {@link Rectangle2D#equals(Object)} method, which compare arbitrary {@code Rectangle2D}
     * implementations.
     *
     * @param  object  the object to compare with this envelope.
     * @return {@code true} if the given object is equal to this envelope.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof Envelope2D) {
            final Envelope2D other = (Envelope2D) object;
            return doubleToLongBits(x)      == doubleToLongBits(other.x)      &&
                   doubleToLongBits(y)      == doubleToLongBits(other.y)      &&
                   doubleToLongBits(width)  == doubleToLongBits(other.width)  &&
                   doubleToLongBits(height) == doubleToLongBits(other.height) &&
                   Objects.equals(crs, other.crs);
        } else {
            return super.equals(object);
        }
    }

    /**
     * Returns {@code true} if {@code this} envelope bounds is equal to {@code that} envelope
     * bounds in two specified dimensions. The coordinate reference system is not compared, since
     * it doesn't need to have the same number of dimensions.
     *
     * @param that  the envelope to compare to.
     * @param xDim  the dimension of {@code that} envelope to compare to the <var>x</var> dimension of {@code this} envelope.
     * @param yDim  the dimension of {@code that} envelope to compare to the <var>y</var> dimension of {@code this} envelope.
     * @param eps   a small tolerance number for floating point number comparisons. This value will be scaled
     *              according this envelope {@linkplain #width width} and {@linkplain #height height}.
     * @return {@code true} if the envelope bounds are the same (up to the specified tolerance
     *         level) in the specified dimensions, or {@code false} otherwise.
     */
    public boolean boundsEquals(final Envelope that, final int xDim, final int yDim, double eps) {
        eps *= 0.5*(width + height);
        for (int i=0; i<4; i++) {
            final int dim2D = (i & 1);
            final int dimND = (dim2D == 0) ? xDim : yDim;
            final double value2D, valueND;
            if ((i & 2) == 0) {
                value2D = this.getMinimum(dim2D);
                valueND = that.getMinimum(dimND);
            } else {
                value2D = this.getMaximum(dim2D);
                valueND = that.getMaximum(dimND);
            }
            // Use '!' for catching NaN values.
            if (!(Math.abs(value2D - valueND) <= eps)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a clone of this envelope.
     *
     * @return a clone of this envelope.
     */
    @Override
    public Envelope2D clone() {
        return (Envelope2D) super.clone();
    }

    /**
     * Formats this envelope as a "{@code BOX}" element.
     * The output is of the form "{@code BOX(}{@linkplain #getLowerCorner()
     * lower corner}{@code ,}{@linkplain #getUpperCorner() upper corner}{@code )}".
     * Example:
     *
     * {@snippet lang="wkt" :
     *   BOX(-90 -180, 90 180)
     *   }
     *
     * @see Envelopes#toString(Envelope)
     */
    @Override
    public String toString() {
        return AbstractEnvelope.toString(this, false);
    }
}
