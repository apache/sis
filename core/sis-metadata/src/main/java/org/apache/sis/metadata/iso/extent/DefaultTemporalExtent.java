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

import java.util.Date;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.geometry.Envelope;
import org.apache.sis.internal.geoapi.temporal.Period;
import org.apache.sis.internal.geoapi.temporal.Instant;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.SpatialTemporalExtent;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.metadata.ReferencingServices;


/**
 * Time period covered by the content of the dataset.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link #getStartTime()} for fetching the start time from the temporal primitive.</li>
 *   <li>{@link #getEndTime()} for fetching the end time from the temporal primitive.</li>
 *   <li>{@link #setBounds(Envelope)} for setting the extent from the given envelope.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "EX_TemporalExtent_Type")
@XmlRootElement(name = "EX_TemporalExtent")
@XmlSeeAlso(DefaultSpatialTemporalExtent.class)
public class DefaultTemporalExtent extends ISOMetadata implements TemporalExtent {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6149873501105795242L;

    /**
     * The date and time for the content of the dataset.
     */
    private TemporalPrimitive extent;

    /**
     * Constructs an initially empty temporal extent.
     */
    public DefaultTemporalExtent() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(TemporalExtent)
     */
    public DefaultTemporalExtent(final TemporalExtent object) {
        super(object);
        extent = object.getExtent();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is is an instance of {@link SpatialTemporalExtent},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding
     *       SIS subclass.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultTemporalExtent}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultTemporalExtent} instance is created using the
     *       {@linkplain #DefaultTemporalExtent(TemporalExtent) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultTemporalExtent castOrCopy(final TemporalExtent object) {
        if (object instanceof SpatialTemporalExtent) {
            return DefaultSpatialTemporalExtent.castOrCopy((SpatialTemporalExtent) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof DefaultTemporalExtent) {
            return (DefaultTemporalExtent) object;
        }
        return new DefaultTemporalExtent(object);
    }

    /**
     * Returns the date and time for the content of the dataset.
     * If no extent has been {@linkplain #setExtent(TemporalPrimitive) explicitely set},
     * then this method will build an extent from the {@linkplain #getStartTime() start
     * time} and {@linkplain #getEndTime() end time} if any.
     *
     * @return The content date.
     */
    @Override
    @XmlElement(name = "extent", required = true)
    public TemporalPrimitive getExtent() {
        return extent;
    }

    /**
     * Sets the date and time for the content of the dataset.
     *
     * @param newValue The new content date.
     */
    public void setExtent(final TemporalPrimitive newValue) {
        checkWritePermission();
        extent = newValue;
    }

    /**
     * Infers a value from the extent as a {@link Date} object.
     *
     * @param begin {@code true} if we are asking for the start time,
     *              or {@code false} for the end time.
     * @return The requested time as a Java date, or {@code null} if none.
     */
    private Date getTime(final boolean begin) {
        final TemporalPrimitive extent = this.extent;
        final Instant instant;
        if (extent instanceof Instant) {
            instant = (Instant) extent;
        } else if (extent instanceof Period) {
            instant = begin ? ((Period) extent).getBeginning() : ((Period) extent).getEnding();
        } else {
            return null;
        }
        return instant.getPosition().getDate();
    }

    /**
     * The start date and time for the content of the dataset.
     * This method tries to infer it from the {@linkplain #getExtent() extent}.
     *
     * @return The start time, or {@code null} if none.
     */
    public Date getStartTime() {
        return getTime(true);
    }

    /**
     * Returns the end date and time for the content of the dataset.
     * This method tries to infer it from the {@linkplain #getExtent() extent}.
     *
     * @return The end time, or {@code null} if none.
     */
    public Date getEndTime() {
        return getTime(false);
    }

    /**
     * Sets this temporal extent to values inferred from the specified envelope. The envelope can
     * be multi-dimensional, in which case the {@linkplain Envelope#getCoordinateReferenceSystem()
     * envelope CRS} must have a temporal component.
     *
     * <p><b>Note:</b> This method is available only if the referencing module is on the classpath.</p>
     *
     * @param  envelope The envelope to use for setting this temporal extent.
     * @throws UnsupportedOperationException if the referencing module is not on the classpath.
     * @throws TransformException if the envelope can't be transformed to a temporal extent.
     *
     * @see DefaultExtent#addElements(Envelope)
     * @see DefaultGeographicBoundingBox#setBounds(Envelope)
     * @see DefaultVerticalExtent#setBounds(Envelope)
     */
    public void setBounds(final Envelope envelope) throws TransformException {
        checkWritePermission();
        ReferencingServices.getInstance().setBounds(envelope, this);
    }
}
