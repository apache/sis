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
import org.apache.sis.internal.util.TemporalUtilities;
import org.apache.sis.internal.metadata.ReferencingServices;


/**
 * Time period covered by the content of the dataset.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link #getStartTime()} for fetching the start time from the temporal primitive.</li>
 *   <li>{@link #getEndTime()} for fetching the end time from the temporal primitive.</li>
 *   <li>{@link #setBounds(Date, Date)} for setting the extent from the given start and end time.</li>
 *   <li>{@link #setBounds(Envelope)} for setting the extent from the given envelope.</li>
 * </ul>
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
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(TemporalExtent)
     */
    public DefaultTemporalExtent(final TemporalExtent object) {
        super(object);
        if (object != null) {
            extent = object.getExtent();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link SpatialTemporalExtent},
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
     * @return The date and time for the content, or {@code null}.
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
    static Date getTime(final TemporalPrimitive extent, final boolean begin) {
        final Instant instant;
        if (extent instanceof Instant) {
            instant = (Instant) extent;
        } else if (extent instanceof Period) {
            instant = begin ? ((Period) extent).getBeginning() : ((Period) extent).getEnding();
        } else {
            return null;
        }
        return instant.getDate();
    }

    /**
     * The start date and time for the content of the dataset.
     * This method tries to infer it from the {@linkplain #getExtent() extent}.
     *
     * @return The start time, or {@code null} if none.
     */
    public Date getStartTime() {
        return getTime(extent, true);
    }

    /**
     * Returns the end date and time for the content of the dataset.
     * This method tries to infer it from the {@linkplain #getExtent() extent}.
     *
     * @return The end time, or {@code null} if none.
     */
    public Date getEndTime() {
        return getTime(extent, false);
    }

    /**
     * Sets the temporal extent to the specified values. This convenience method creates a temporal
     * primitive for the given dates, then invokes {@link #setExtent(TemporalPrimitive)}.
     *
     * <p><b>Note:</b> this method is available only if the {@code sis-temporal} module is available on the classpath,
     * or any other module providing an implementation of the {@link org.opengis.temporal.TemporalFactory} interface.</p>
     *
     * @param  startTime The start date and time for the content of the dataset, or {@code null} if none.
     * @param  endTime   The end date and time for the content of the dataset, or {@code null} if none.
     * @throws UnsupportedOperationException if no implementation of {@code TemporalFactory} has been found
     *         on the classpath.
     */
    public void setBounds(final Date startTime, final Date endTime) throws UnsupportedOperationException {
        TemporalPrimitive value = null;
        if (startTime != null || endTime != null) {
            if (endTime == null || endTime.equals(startTime)) {
                value = TemporalUtilities.createInstant(startTime);
            } else if (startTime == null) {
                value = TemporalUtilities.createInstant(endTime);
            } else {
                value = TemporalUtilities.createPeriod(startTime, endTime);
            }
        }
        setExtent(value);
    }

    /**
     * Sets this temporal extent to values inferred from the specified envelope.
     * The given envelope must have a {@linkplain Envelope#getCoordinateReferenceSystem() CRS},
     * and at least one dimension of that CRS shall be assignable to a property of this extent.
     *
     * <p><b>Note:</b> this method is available only if the {@code sis-referencing} module is
     * available on the classpath.</p>
     *
     * @param  envelope The envelope to use for setting this temporal extent.
     * @throws UnsupportedOperationException if the referencing module or the temporal module is not on the classpath.
     * @throws TransformException if the envelope can not be transformed to a temporal extent.
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
