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
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.metadata.ReferencingServices;

import static java.lang.Double.doubleToLongBits;

// Related to JDK7
import java.util.Objects;


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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
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
public class DefaultGeographicBoundingBox extends AbstractGeographicExtent
        implements GeographicBoundingBox
{
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
     * @throws IllegalArgumentException If (<var>west bound</var> &gt; <var>east bound</var>)
     *         or (<var>south bound</var> &gt; <var>north bound</var>).
     *         Note that {@linkplain Double#NaN NaN} values are allowed.
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
        this.westBoundLongitude = westBoundLongitude;
        this.eastBoundLongitude = eastBoundLongitude;
        this.southBoundLatitude = southBoundLatitude;
        this.northBoundLatitude = northBoundLatitude;
        final Angle min, max;
        if (westBoundLongitude > eastBoundLongitude) {
            min = new Longitude(westBoundLongitude);
            max = new Longitude(eastBoundLongitude);
        } else if (southBoundLatitude > northBoundLatitude) {
            min = new Latitude(southBoundLatitude);
            max = new Latitude(northBoundLatitude);
        } else {
            return;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, min, max));
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
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     * @return The western-most longitude between -180 and +180°,
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
     *
     * @param newValue The western-most longitude between -180 and +180°,
     *        or {@linkplain Double#NaN NaN} to undefine.
     */
    public void setWestBoundLongitude(final double newValue) {
        checkWritePermission();
        westBoundLongitude = newValue;
    }

    /**
     * Returns the eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     *
     * @return The eastern-most longitude between -180 and +180°,
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
     *
     * @param newValue The eastern-most longitude between -180 and +180°,
     *        or {@linkplain Double#NaN NaN} to undefine.
     */
    public void setEastBoundLongitude(final double newValue) {
        checkWritePermission();
        eastBoundLongitude = newValue;
    }

    /**
     * Returns the southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     *
     * @return The southern-most latitude between -90 and +90°,
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
     *
     * @param newValue The southern-most latitude between -90 and +90°,
     *        or {@linkplain Double#NaN NaN} to undefine.
     */
    public void setSouthBoundLatitude(final double newValue) {
        checkWritePermission();
        southBoundLatitude = newValue;
    }

    /**
     * Returns the northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     *
     * @return The northern-most latitude between -90 and +90°,
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
     *
     * @param newValue The northern-most latitude between -90 and +90°,
     *        or {@linkplain Double#NaN NaN} to undefine.
     */
    public void setNorthBoundLatitude(final double newValue) {
        checkWritePermission();
        northBoundLatitude = newValue;
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
     * @throws IllegalArgumentException If (<var>west bound</var> &gt; <var>east bound</var>)
     *         or (<var>south bound</var> &gt; <var>north bound</var>).
     *         Note that {@linkplain Double#NaN NaN} values are allowed.
     */
    public void setBounds(final double westBoundLongitude,
                                       final double eastBoundLongitude,
                                       final double southBoundLatitude,
                                       final double northBoundLatitude)
            throws IllegalArgumentException
    {
        checkWritePermission();
        final Angle min, max;
        if (westBoundLongitude > eastBoundLongitude) {
            min = new Longitude(westBoundLongitude);
            max = new Longitude(eastBoundLongitude);
            // Exception will be thrown below.
        } else if (southBoundLatitude > northBoundLatitude) {
            min = new Latitude(southBoundLatitude);
            max = new Latitude(northBoundLatitude);
            // Exception will be thrown below.
        } else {
            this.westBoundLongitude = westBoundLongitude;
            this.eastBoundLongitude = eastBoundLongitude;
            this.southBoundLatitude = southBoundLatitude;
            this.northBoundLatitude = northBoundLatitude;
            return;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, min, max));
    }

    /**
     * Constructs a geographic bounding box from the specified envelope. If the envelope contains
     * a CRS, then the bounding box may be projected to a geographic CRS. Otherwise, the envelope
     * is assumed already in appropriate CRS.
     *
     * <p>When coordinate transformation is required, the target geographic CRS is not necessarily
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS#WGS84 WGS84}. This method
     * preserves the same {@linkplain org.opengis.referencing.datum.Ellipsoid ellipsoid} than
     * in the envelope CRS when possible. This is because geographic bounding box are only
     * approximative and the ISO specification do not mandates a particular CRS, so we avoid
     * transformations that are not strictly necessary.</p>
     *
     * <p><b>Note:</b> This method is available only if the referencing module is on the classpath.</p>
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

    /**
     * Adds a geographic bounding box to this box. If the {@linkplain #getInclusion() inclusion}
     * status is the same for this box and the box to be added, then the resulting bounding box
     * is the union of the two boxes. If the inclusion status are opposite (<cite>exclusion</cite>),
     * then this method attempt to exclude some area of specified box from this box.
     * The resulting bounding box is smaller if the exclusion can be performed without ambiguity.
     *
     * @param box The geographic bounding box to add to this box.
     */
    public void add(final GeographicBoundingBox box) {
        checkWritePermission();
        final double λmin = box.getWestBoundLongitude();
        final double λmax = box.getEastBoundLongitude();
        final double φmin = box.getSouthBoundLatitude();
        final double φmax = box.getNorthBoundLatitude();
        /*
         * Reminder: 'inclusion' is a mandatory attribute, so it should never be null for a
         * valid metadata object.  If the metadata object is invalid, it is better to get a
         * an exception than having a code doing silently some inappropriate work.
         */
        if (MetadataUtilities.getInclusion(    getInclusion()) ==
            MetadataUtilities.getInclusion(box.getInclusion()))
        {
            if (λmin < westBoundLongitude) westBoundLongitude = λmin;
            if (λmax > eastBoundLongitude) eastBoundLongitude = λmax;
            if (φmin < southBoundLatitude) southBoundLatitude = φmin;
            if (φmax > northBoundLatitude) northBoundLatitude = φmax;
        } else {
            if (φmin <= southBoundLatitude && φmax >= northBoundLatitude) {
                if (λmin > westBoundLongitude) westBoundLongitude = λmin;
                if (λmax < eastBoundLongitude) eastBoundLongitude = λmax;
            }
            if (λmin <= westBoundLongitude && λmax >= eastBoundLongitude) {
                if (φmin > southBoundLatitude) southBoundLatitude = φmin;
                if (φmax < northBoundLatitude) northBoundLatitude = φmax;
            }
        }
    }

    /**
     * Sets this bounding box to the intersection of this box with the specified one.
     * The {@linkplain #getInclusion() inclusion} status must be the same for both boxes.
     *
     * @param box The geographic bounding box to intersect with this box.
     */
    public void intersect(final GeographicBoundingBox box) {
        checkWritePermission();
        if (MetadataUtilities.getInclusion(    getInclusion()) !=
            MetadataUtilities.getInclusion(box.getInclusion()))
        {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatiblePropertyValue_1, "inclusion"));
        }
        final double λmin = box.getWestBoundLongitude();
        final double λmax = box.getEastBoundLongitude();
        final double φmin = box.getSouthBoundLatitude();
        final double φmax = box.getNorthBoundLatitude();
        if (λmin > westBoundLongitude) westBoundLongitude = λmin;
        if (λmax < eastBoundLongitude) eastBoundLongitude = λmax;
        if (φmin > southBoundLatitude) southBoundLatitude = φmin;
        if (φmax < northBoundLatitude) northBoundLatitude = φmax;
        if (westBoundLongitude > eastBoundLongitude) {
            westBoundLongitude = eastBoundLongitude = 0.5 * (westBoundLongitude + eastBoundLongitude);
        }
        if (southBoundLatitude > northBoundLatitude) {
            southBoundLatitude = northBoundLatitude = 0.5 * (southBoundLatitude + northBoundLatitude);
        }
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
