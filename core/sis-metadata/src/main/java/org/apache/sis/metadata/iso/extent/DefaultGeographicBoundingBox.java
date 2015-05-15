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
package org.apache.sis.metadata.iso.extent;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.metadata.ReferencingServices;

import static java.lang.Double.doubleToLongBits;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Geographic position of the dataset. This is only an approximate so specifying the coordinate
 * reference system is unnecessary. The CRS shall be geographic with Greenwich prime meridian,
 * but the datum doesn't need to be WGS84.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link #setBounds(double, double, double, double)} for setting the extent from (λ,φ) values.</li>
 *   <li>{@link #setBounds(Envelope)} for setting the extent from the given envelope.</li>
 *   <li>{@link #setBounds(GeographicBoundingBox)} for setting the extent from an other bounding box.</li>
 *   <li>{@link #add(GeographicBoundingBox)} for expanding this extent to include an other bounding box.</li>
 *   <li>{@link #intersect(GeographicBoundingBox)} for the intersection between the two bounding boxes.</li>
 * </ul>
 *
 * <div class="section">Validation and normalization</div>
 * All constructors and setter methods in this class perform the following argument validation or normalization:
 *
 * <ul>
 *   <li>If the {@linkplain #getSouthBoundLatitude() south bound latitude} is greater than the
 *       {@linkplain #getNorthBoundLatitude() north bound latitude}, then an exception is thrown.</li>
 *   <li>If any latitude is set to a value outside the
 *       [{@linkplain Latitude#MIN_VALUE -90} … {@linkplain Latitude#MAX_VALUE 90}]° range,
 *       then that latitude will be clamped.</li>
 *   <li>If any longitude is set to a value outside the
 *       [{@linkplain Longitude#MIN_VALUE -180} … {@linkplain Longitude#MAX_VALUE 180}]° range,
 *       then a multiple of 360° will be added or subtracted to that longitude in order to bring
 *       it back inside the range.</li>
 * </ul>
 *
 * If the {@linkplain #getWestBoundLongitude() west bound longitude} is greater than the
 * {@linkplain #getEastBoundLongitude() east bound longitude}, then the box spans the anti-meridian.
 * See {@linkplain org.apache.sis.geometry.GeneralEnvelope} for more information on anti-meridian spanning.
 *
 * <div class="note"><b>Note: relationship with Envelope classes</b><br>
 * The {@link org.apache.sis.geometry} package provides various {@code Envelope} classes serving a simular purpose.
 * The main difference is that envelopes can be expressed in any {@linkplain org.apache.sis.referencing.crs.AbstractCRS
 * Coordinate Reference System} (for example using any map projection), may have any number of dimensions, axes may have
 * any {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getDirection() direction} (some maps are south-oriented)
 * and may use any units of measurement. By contrast, geographic bounding box are restricted to two-dimensional
 * geographic CRS with latitude and longitude in decimal degrees, inside the [-90 … +90]° and [-180 … +180]° range
 * respectively, increasing toward north and east respectively, and longitude measured from the <cite>international
 * reference meridian</cite> (Greenwich on Earth). However {@code GeographicBoundingBox} said nothing about the
 * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic datum}. Consequently this bounding
 * box should be used only as a convenient way to give an <em>approximate</em> description of a location.
 * Users can assume a precision of about 0.01° for the latitude and longitude values in this class.
 * If more precision is desired, an {@code Envelope} should be considered instead.
 * </div>
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.geometry.GeneralEnvelope
 */
@XmlType(name = "EX_GeographicBoundingBox_Type", propOrder = {
    "westBoundLongitude",
    "eastBoundLongitude",
    "southBoundLatitude",
    "northBoundLatitude"
})
@XmlRootElement(name = "EX_GeographicBoundingBox")
public class DefaultGeographicBoundingBox extends AbstractGeographicExtent implements GeographicBoundingBox {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -9200149606040429957L;

    /**
     * The western-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     */
    private double westBoundLongitude;

    /**
     * The eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     */
    private double eastBoundLongitude;

    /**
     * The southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     */
    private double southBoundLatitude;

    /**
     * The northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     */
    private double northBoundLatitude;

    /**
     * Constructs an initially {@linkplain #isEmpty() empty} geographic bounding box.
     * All longitude and latitude values are initialized to {@link Double#NaN}.
     */
    public DefaultGeographicBoundingBox() {
        westBoundLongitude = Double.NaN;
        eastBoundLongitude = Double.NaN;
        southBoundLatitude = Double.NaN;
        northBoundLatitude = Double.NaN;
    }

    /**
     * Creates a geographic bounding box initialized to the specified values.
     * The {@linkplain #getInclusion() inclusion} property is set to {@code true}.
     *
     * <p><strong>Caution:</strong> Arguments are expected in the same order than they appear
     * in the ISO 19115 specification. This is different than the order commonly found in the
     * Java2D world, which is rather (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>,
     * <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>).</p>
     *
     * @param westBoundLongitude The minimal λ value.
     * @param eastBoundLongitude The maximal λ value.
     * @param southBoundLatitude The minimal φ value.
     * @param northBoundLatitude The maximal φ value.
     *
     * @throws IllegalArgumentException If (<var>south bound</var> &gt; <var>north bound</var>).
     *         Note that {@linkplain Double#NaN NaN} values are allowed.
     *
     * @see #setBounds(double, double, double, double)
     */
    public DefaultGeographicBoundingBox(final double westBoundLongitude,
                                        final double eastBoundLongitude,
                                        final double southBoundLatitude,
                                        final double northBoundLatitude)
            throws IllegalArgumentException
    {
        super(true);
        verifyBounds(southBoundLatitude, northBoundLatitude);
        this.westBoundLongitude = westBoundLongitude;
        this.eastBoundLongitude = eastBoundLongitude;
        this.southBoundLatitude = southBoundLatitude;
        this.northBoundLatitude = northBoundLatitude;
        normalize();
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(GeographicBoundingBox)
     */
    public DefaultGeographicBoundingBox(final GeographicBoundingBox object) {
        super(object);
        if (object != null) {
            westBoundLongitude = object.getWestBoundLongitude();
            eastBoundLongitude = object.getEastBoundLongitude();
            southBoundLatitude = object.getSouthBoundLatitude();
            northBoundLatitude = object.getNorthBoundLatitude();
            verifyBounds(southBoundLatitude, northBoundLatitude);
            normalize();
        } else {
            westBoundLongitude = Double.NaN;
            eastBoundLongitude = Double.NaN;
            southBoundLatitude = Double.NaN;
            northBoundLatitude = Double.NaN;
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultGeographicBoundingBox}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultGeographicBoundingBox} instance is created using the
     *       {@linkplain #DefaultGeographicBoundingBox(GeographicBoundingBox) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGeographicBoundingBox castOrCopy(final GeographicBoundingBox object) {
        if (object == null || object instanceof DefaultGeographicBoundingBox) {
            return (DefaultGeographicBoundingBox) object;
        }
        return new DefaultGeographicBoundingBox(object);
    }

    /**
     * Returns the western-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     *
     * <p>Note that the returned value is greater than the {@linkplain #getEastBoundLongitude()
     * east bound longitude} if this box is spanning over the anti-meridian.</p>
     *
     * @return The western-most longitude between -180° and +180° inclusive,
     *         or {@linkplain Double#NaN NaN} if undefined.
     */
    @Override
    @ValueRange(minimum=-180, maximum=180)
    @XmlElement(name = "westBoundLongitude", required = true)
    public double getWestBoundLongitude() {
        return westBoundLongitude;
    }

    /**
     * Sets the western-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     * Values outside the [-180 … 180]° range are {@linkplain Longitude#normalize(double) normalized}.
     *
     * @param newValue The western-most longitude between -180° and +180° inclusive,
     *        or {@linkplain Double#NaN NaN} to undefine.
     */
    public void setWestBoundLongitude(double newValue) {
        checkWritePermission();
        if (newValue != Longitude.MAX_VALUE) { // Do not normalize +180° to -180°.
            newValue = Longitude.normalize(newValue);
        }
        westBoundLongitude = newValue;
    }

    /**
     * Returns the eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     *
     * <p>Note that the returned value is smaller than the {@linkplain #getWestBoundLongitude()
     * west bound longitude} if this box is spanning over the anti-meridian.</p>
     *
     * @return The eastern-most longitude between -180° and +180° inclusive,
     *         or {@linkplain Double#NaN NaN} if undefined.
     */
    @Override
    @ValueRange(minimum=-180, maximum=180)
    @XmlElement(name = "eastBoundLongitude", required = true)
    public double getEastBoundLongitude() {
        return eastBoundLongitude;
    }

    /**
     * Sets the eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     * Values outside the [-180 … 180]° range are {@linkplain Longitude#normalize(double) normalized}.
     *
     * @param newValue The eastern-most longitude between -180° and +180° inclusive,
     *        or {@linkplain Double#NaN NaN} to undefine.
     */
    public void setEastBoundLongitude(double newValue) {
        checkWritePermission();
        if (newValue != Longitude.MAX_VALUE) { // Do not normalize +180° to -180°.
            newValue = Longitude.normalize(newValue);
        }
        eastBoundLongitude = newValue;
    }

    /**
     * Returns the southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     *
     * @return The southern-most latitude between -90° and +90° inclusive,
     *         or {@linkplain Double#NaN NaN} if undefined.
     */
    @Override
    @ValueRange(minimum=-90, maximum=90)
    @XmlElement(name = "southBoundLatitude", required = true)
    public double getSouthBoundLatitude()  {
        return southBoundLatitude;
    }

    /**
     * Sets the southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     * Values outside the [-90 … 90]° range are {@linkplain Latitude#clamp(double) clamped}.
     * If the result is greater than the {@linkplain #getNorthBoundLatitude() north bound latitude},
     * then the north bound is set to {@link Double#NaN}.
     *
     * @param newValue The southern-most latitude between -90° and +90° inclusive,
     *        or {@linkplain Double#NaN NaN} to undefine.
     */
    public void setSouthBoundLatitude(final double newValue) {
        checkWritePermission();
        southBoundLatitude = Latitude.clamp(newValue);
        if (southBoundLatitude > northBoundLatitude) {
            northBoundLatitude = Double.NaN;
        }
    }

    /**
     * Returns the northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     *
     * @return The northern-most latitude between -90° and +90° inclusive,
     *         or {@linkplain Double#NaN NaN} if undefined.
     */
    @Override
    @ValueRange(minimum=-90, maximum=90)
    @XmlElement(name = "northBoundLatitude", required = true)
    public double getNorthBoundLatitude()   {
        return northBoundLatitude;
    }

    /**
     * Sets the northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     * Values outside the [-90 … 90]° range are {@linkplain Latitude#clamp(double) clamped}.
     * If the result is smaller than the {@linkplain #getSouthBoundLatitude() south bound latitude},
     * then the south bound is set to {@link Double#NaN}.
     *
     * @param newValue The northern-most latitude between -90° and +90° inclusive,
     *        or {@linkplain Double#NaN NaN} to undefine.
     */
    public void setNorthBoundLatitude(final double newValue) {
        checkWritePermission();
        northBoundLatitude = Latitude.clamp(newValue);
        if (northBoundLatitude < southBoundLatitude) {
            southBoundLatitude = Double.NaN;
        }
    }

    /**
     * Verifies that the given bounding box is valid. This method verifies only the latitude values,
     * because we allow the west bound longitude to be greater then east bound longitude (they are
     * boxes spanning the anti-meridian).
     *
     * <p>This method should be invoked <strong>before</strong> {@link #normalize()}.</p>
     *
     * @throws IllegalArgumentException If (<var>south bound</var> &gt; <var>north bound</var>).
     *         Note that {@linkplain Double#NaN NaN} values are allowed.
     */
    private static void verifyBounds(final double southBoundLatitude, final double northBoundLatitude)
            throws IllegalArgumentException
    {
        if (southBoundLatitude > northBoundLatitude) { // Accept NaN.
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalOrdinateRange_3,
                    new Latitude(southBoundLatitude), new Latitude(northBoundLatitude),
                    Vocabulary.format(Vocabulary.Keys.Latitude)));
        }
    }

    /**
     * Clamps the latitudes and normalizes the longitudes.
     *
     * @see #denormalize(double, double)
     */
    private void normalize() {
        southBoundLatitude = Latitude.clamp(southBoundLatitude);
        northBoundLatitude = Latitude.clamp(northBoundLatitude);
        final double span = eastBoundLongitude - westBoundLongitude;
        if (!(span >= (Longitude.MAX_VALUE - Longitude.MIN_VALUE))) { // 'span' may be NaN.
            westBoundLongitude = Longitude.normalize(westBoundLongitude);
            eastBoundLongitude = Longitude.normalize(eastBoundLongitude);
            if (span != 0) {
                /*
                 * This is the usual case where east and west longitudes are different.
                 * Since -180° and +180° are equivalent, always use -180° for the west
                 * bound (this was ensured by the call to Longitude.normalize(…)), and
                 * always use +180° for the east bound. So we get for example [5 … 180]
                 * instead of [5 … -180] even if both are the same box.
                 */
                if (eastBoundLongitude == Longitude.MIN_VALUE) {
                    eastBoundLongitude = Longitude.MAX_VALUE;
                }
                return;
            }
            /*
             * If the longitude range is anything except [+0 … -0], we are done. Only in the
             * particular case of [+0 … -0] we will replace the range by [-180 … +180].
             */
            if (!MathFunctions.isPositiveZero(westBoundLongitude) ||
                !MathFunctions.isNegativeZero(eastBoundLongitude))
            {
                return;
            }
        }
        /*
         * If we reach this point, the longitude range is either [+0 … -0] or anything
         * spanning 360° or more. Normalize the range to [-180 … +180].
         */
        westBoundLongitude = Longitude.MIN_VALUE;
        eastBoundLongitude = Longitude.MAX_VALUE;
    }

    /**
     * Sets the bounding box to the specified values.
     * The {@linkplain #getInclusion() inclusion} property is left unchanged.
     *
     * <p><strong>Caution:</strong> Arguments are expected in the same order than they appear
     * in the ISO 19115 specification. This is different than the order commonly found in the
     * Java2D world, which is rather (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>,
     * <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>).</p>
     *
     * @param westBoundLongitude The minimal λ value.
     * @param eastBoundLongitude The maximal λ value.
     * @param southBoundLatitude The minimal φ value.
     * @param northBoundLatitude The maximal φ value.
     *
     * @throws IllegalArgumentException If (<var>south bound</var> &gt; <var>north bound</var>).
     *         Note that {@linkplain Double#NaN NaN} values are allowed.
     */
    public void setBounds(final double westBoundLongitude,
                          final double eastBoundLongitude,
                          final double southBoundLatitude,
                          final double northBoundLatitude)
            throws IllegalArgumentException
    {
        checkWritePermission();
        verifyBounds(southBoundLatitude, northBoundLatitude);
        this.westBoundLongitude = westBoundLongitude;
        this.eastBoundLongitude = eastBoundLongitude;
        this.southBoundLatitude = southBoundLatitude;
        this.northBoundLatitude = northBoundLatitude;
        normalize();
    }

    /**
     * Constructs a geographic bounding box from the specified envelope. If the envelope contains
     * a CRS, then the bounding box may be projected to a geographic CRS. Otherwise, the envelope
     * is assumed already in appropriate CRS.
     *
     * <p>When coordinate transformation is required, the target geographic CRS is not necessarily
     * {@linkplain org.apache.sis.referencing.CommonCRS#WGS84 WGS84}. This method preserves the same
     * {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoid} than in the envelope
     * CRS when possible. This is because geographic bounding box are only approximative and the ISO
     * specification do not mandates a particular CRS, so we avoid transformations that are not
     * strictly necessary.</p>
     *
     * <p><b>Note:</b> this method is available only if the referencing module is on the classpath.</p>
     *
     * @param  envelope The envelope to use for setting this geographic bounding box.
     * @throws UnsupportedOperationException if the referencing module is not on the classpath.
     * @throws TransformException if the envelope can not be transformed to a geographic extent.
     *
     * @see DefaultExtent#addElements(Envelope)
     * @see DefaultVerticalExtent#setBounds(Envelope)
     * @see DefaultTemporalExtent#setBounds(Envelope)
     */
    public void setBounds(final Envelope envelope) throws TransformException {
        ArgumentChecks.ensureNonNull("envelope", envelope);
        checkWritePermission();
        ReferencingServices.getInstance().setBounds(envelope, this);
        setInclusion(Boolean.TRUE); // Set only on success.
    }

    /**
     * Sets the bounding box to the same values than the specified box.
     *
     * @param box The geographic bounding box to use for setting the values of this box.
     */
    public void setBounds(final GeographicBoundingBox box) {
        ArgumentChecks.ensureNonNull("box", box);
        setBounds(box.getWestBoundLongitude(), box.getEastBoundLongitude(),
                  box.getSouthBoundLatitude(), box.getNorthBoundLatitude());
        setInclusion(box.getInclusion()); // Set only on success.
    }

    /*
     * IMPLEMENTATION NOTE: For the handling of anti-meridian spanning in union and intersection operations,
     * this class applies a different strategy than GeneralEnvelope. Instead than trying to work directly with
     * the ordinate values without adding or removing offset (which may cause rounding errors), we apply a ±360°
     * shift on longitude values. This simpler strategy is okay here because the range is fixed in the code (not
     * an arbitrarily high range), and GeographicBoundingBox are approximative by definition anyway.
     */

    /**
     * Returns a code telling how to denormalize this box and/or the other box before to compute union or intersection.
     * This method may also modify the {@link #westBoundLongitude} and {@link #eastBoundLongitude}. The codes are:
     *
     * <ul>
     *   <li> 0 : Do nothing - both boxes are normal.</li>
     *   <li> 3 : Do nothing - both boxes are spanning the anti-meridian.</li>
     *   <li>-1 : Caller will need to subtract 360° from {@code λmin}.</li>
     *   <li>+1 : Caller will need to add      360° to   {@code λmax}.</li>
     *   <li>-2 : This method has subtracted   360° from {@link #westBoundLongitude}.</li>
     *   <li>+2 : This method has added        360° to   {@link #eastBoundLongitude}.</li>
     * </ul>
     *
     * @see #normalize()
     */
    private int denormalize(final double λmin, final double λmax) {
        final boolean isSpanningAntiMeridian = westBoundLongitude > eastBoundLongitude;
        if ((λmin > λmax) == isSpanningAntiMeridian) {
            return isSpanningAntiMeridian ? 3 : 0;
        }
        final double left  = westBoundLongitude - λmin;
        final double right = λmax - eastBoundLongitude;
        if (!isSpanningAntiMeridian) {
            /*
             * If we were computing the union between this bounding box and the other box,
             * by how much the width would be increased on the left side and on the right
             * side? (ignore negative values for this first part). What we may get:
             *
             *   (+1) Left side is positive            (-1) Right side is positive
             *
             *          W┌──────────┐                     ┌──────────┐E
             *   ──┐  ┌──┼──────────┼──                 ──┼──────────┼──┐  ┌──
             *     │  │  └──────────┘                     └──────────┘  │  │
             *   ──┘  └────────────────                 ────────────────┘  └──
             *       λmin                                              λmax
             *
             * For each of the above case, if we apply the translation in the opposite way,
             * the result would be much wort (for each lower rectangle, imagine translating
             * the longuest part in the opposite direction instead than the shortest one).
             *
             * Note that only one of 'left' and 'right' can be positive, otherwise we would
             * not be in the case where one box is spanning the anti-meridian while the other
             * box does not.
             */
            if (left  >= 0) return +1;
            if (right >= 0) return -1;
            /*
             * Both 'left' and 'right' are negative. For each alternatives (translating λmin
             * or translating λmax), we will choose the one which give the closest result to
             * a bound of this box:
             *
             *        W┌──────────┐E         Changes in width of the union compared to this box:
             *   ───┐  │        ┌─┼──          ∙ if we move λmax to the right:  Δ = (λmax + 360) - E
             *      │  └────────┼─┘            ∙ if we move λmin to the left:   Δ = W - (λmin + 360)
             *   ───┘           └────
             *    λmax         λmin
             *
             * We want the smallest option. We get the condition below after cancelation of both "+ 360" terms.
             */
            return (left < right) ? -1 : +1;
        }
        /*
         * Same algorithm than above, but with the sign of 'left' an 'right' inversed.
         * The "if" statements have been combined for avoiding to repeat the +/- operations.
         */
        if (!(left <= 0) && right <= 0 || left > right) {
            westBoundLongitude -= Longitude.MAX_VALUE - Longitude.MIN_VALUE;
            return -2;
        } else {
            eastBoundLongitude += Longitude.MAX_VALUE - Longitude.MIN_VALUE;
            return +2;
        }
    }

    /**
     * Adds a geographic bounding box to this box.
     * This method behavior depends on whether the bounding boxes encompass an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>):
     *
     * <ul>
     *   <li>If the {@linkplain #getInclusion() inclusion} status is the same for this box and the box to be added,
     *       then the resulting bounding box is the union of the two boxes.</li>
     *   <li>If the inclusion/exclusion status are opposite, then this method attempts to exclude some area
     *       of the specified box from this box. The resulting bounding box is smaller if the exclusion can
     *       be performed without ambiguity.</li>
     * </ul>
     *
     * In both cases, if either this box or the specified box has {@linkplain Double#NaN NaN} bounds,
     * then the corresponding bounds of the result will bet set to NaN.
     *
     * <div class="note"><b>Rational:</b>
     * this class is stricter about NaN values than other classes like {@link org.apache.sis.geometry.GeneralEnvelope}.
     * The reason is that computations on {@code GeographicBoundingBox} instances are rare ({@code Envelope} instances
     * are preferred for this purpose). Usually, {@code GeographicBoundingBox} values come directly from some metadata
     * structure. Consequently NaN values in {@code GeographicBoundingBox} means <cite>"information is unknown"</cite>
     * more often than <cite>"not yet calculated"</cite>.</div>
     *
     * @param box The geographic bounding box to add to this box.
     *
     * @see org.apache.sis.geometry.GeneralEnvelope#add(Envelope)
     */
    public void add(final GeographicBoundingBox box) {
        checkWritePermission();
        double λmin = box.getWestBoundLongitude();
        double λmax = box.getEastBoundLongitude();
        double φmin = box.getSouthBoundLatitude();
        double φmax = box.getNorthBoundLatitude();
        /*
         * Reminder: 'inclusion' is a mandatory attribute, so it should never be null for a
         * valid metadata object.  If the metadata object is invalid, it is better to get a
         * an exception than having a code doing silently some inappropriate work.
         */
        final boolean i1 = MetadataUtilities.getInclusion(this.getInclusion());
        final boolean i2 = MetadataUtilities.getInclusion(box. getInclusion());
        final int status = denormalize(λmin, λmax); // Must be after call to getInclusion().
        switch (status) {
            case -1: λmin -= Longitude.MAX_VALUE - Longitude.MIN_VALUE; break;
            case +1: λmax += Longitude.MAX_VALUE - Longitude.MIN_VALUE; break;
        }
        if (i1 == i2) {
            westBoundLongitude = Math.min(westBoundLongitude, λmin);
            eastBoundLongitude = Math.max(eastBoundLongitude, λmax);
            southBoundLatitude = Math.min(southBoundLatitude, φmin);
            northBoundLatitude = Math.max(northBoundLatitude, φmax);
        } else {
            if (φmin <= southBoundLatitude && φmax >= northBoundLatitude) {
                westBoundLongitude = Math.max(westBoundLongitude, λmin);
                eastBoundLongitude = Math.min(eastBoundLongitude, λmax);
            }
            if (λmin <= westBoundLongitude && λmax >= eastBoundLongitude) {
                southBoundLatitude = Math.max(southBoundLatitude, φmin);
                northBoundLatitude = Math.min(northBoundLatitude, φmax);
            }
        }
        if (status == 3) {
            if (eastBoundLongitude > westBoundLongitude) {
                westBoundLongitude = Longitude.MIN_VALUE;
                eastBoundLongitude = Longitude.MAX_VALUE;
            }
        }
        normalize();
    }

    /**
     * Sets this bounding box to the intersection of this box with the specified one.
     * The {@linkplain #getInclusion() inclusion} status must be the same for both boxes.
     *
     * <p>If there is no intersection between the two bounding boxes, then this method sets
     * both longitudes and/or both latitudes to {@linkplain Double#NaN NaN}. If either this
     * box or the specified box has NaN bounds, then the corresponding bounds of the
     * intersection result will bet set to NaN.</p>
     *
     * @param box The geographic bounding box to intersect with this box.
     * @throws IllegalArgumentException If the inclusion status is not the same for both boxes.
     *
     * @see Extents#intersection(GeographicBoundingBox, GeographicBoundingBox)
     * @see org.apache.sis.geometry.GeneralEnvelope#intersect(Envelope)
     */
    public void intersect(final GeographicBoundingBox box) throws IllegalArgumentException {
        checkWritePermission();
        if (MetadataUtilities.getInclusion(    getInclusion()) !=
            MetadataUtilities.getInclusion(box.getInclusion()))
        {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatiblePropertyValue_1, "inclusion"));
        }
        double λmin = box.getWestBoundLongitude();
        double λmax = box.getEastBoundLongitude();
        double φmin = box.getSouthBoundLatitude();
        double φmax = box.getNorthBoundLatitude();
        final int status = denormalize(λmin, λmax);
        switch (status) {
            case -1: λmin -= Longitude.MAX_VALUE - Longitude.MIN_VALUE; break;
            case +1: λmax += Longitude.MAX_VALUE - Longitude.MIN_VALUE; break;
        }
        westBoundLongitude = Math.max(westBoundLongitude, λmin);
        eastBoundLongitude = Math.min(eastBoundLongitude, λmax);
        southBoundLatitude = Math.max(southBoundLatitude, φmin);
        northBoundLatitude = Math.min(northBoundLatitude, φmax);
        if (status != 3) {
            if (westBoundLongitude > eastBoundLongitude) {
                westBoundLongitude = eastBoundLongitude = Double.NaN;
            }
        }
        if (southBoundLatitude > northBoundLatitude) {
            southBoundLatitude = northBoundLatitude = Double.NaN;
        }
        normalize();
    }

    /**
     * Returns {@code true} if this metadata is empty. This metadata is considered empty if
     * every bound values are {@linkplain Double#NaN NaN}. Note that this is different than
     * the <cite>Java2D</cite> or <cite>envelope</cite> definition of "emptiness", since we
     * don't test if the area is greater than zero - this method is a metadata test, not a
     * geometric test.
     *
     * @return {@code true} if this metadata does not define any bound value.
     *
     * @see org.apache.sis.geometry.AbstractEnvelope#isAllNaN()
     */
    @Override
    public boolean isEmpty() {
        return Double.isNaN(eastBoundLongitude) &&
               Double.isNaN(westBoundLongitude) &&
               Double.isNaN(northBoundLatitude) &&
               Double.isNaN(southBoundLatitude);
    }

    /**
     * Compares this geographic bounding box with the specified object for equality.
     *
     * @param object The object to compare for equality.
     * @return {@code true} if the given object is equal to this box.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        // Above code really requires DefaultGeographicBoundingBox.class, not getClass().
        // This code is used only for performance raison. The super-class implementation
        // is generic enough for all other cases.
        if (object != null && object.getClass() == DefaultGeographicBoundingBox.class) {
            final DefaultGeographicBoundingBox that = (DefaultGeographicBoundingBox) object;
            return Objects.equals(getInclusion(), that.getInclusion()) &&
                   doubleToLongBits(southBoundLatitude) == doubleToLongBits(that.southBoundLatitude) &&
                   doubleToLongBits(northBoundLatitude) == doubleToLongBits(that.northBoundLatitude) &&
                   doubleToLongBits(eastBoundLongitude) == doubleToLongBits(that.eastBoundLongitude) &&
                   doubleToLongBits(westBoundLongitude) == doubleToLongBits(that.westBoundLongitude);
        }
        return super.equals(object, mode);
    }
}
