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
import java.util.Optional;
import java.time.Instant;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.geometry.Envelope;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.SpatialTemporalExtent;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.temporal.TemporalObjects;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.temporal.Period;


/**
 * Time period covered by the content of the dataset.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code EX_TemporalExtent}
 * {@code   └─extent…………………} The date and time for the content of the dataset.</div>
 *
 * In addition to the standard properties, SIS provides the following methods:
 * <ul>
 *   <li>{@link #getBeginning()} for fetching the start time from the temporal primitive.</li>
 *   <li>{@link #getEnding()} for fetching the end time from the temporal primitive.</li>
 *   <li>{@link #setBounds(Date, Date)} for setting the extent from the given start and end time.</li>
 *   <li>{@link #setBounds(Envelope)} for setting the extent from the given envelope.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
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
 * @version 1.5
 * @since   0.3
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
    @SuppressWarnings("serial")
    private TemporalPrimitive extent;

    /**
     * Constructs an initially empty temporal extent.
     */
    public DefaultTemporalExtent() {
    }

    /**
     * Constructs a new instance initialized with the specified values.
     *
     * @param  beginning  the start date and time for the content of the dataset, or {@code null} if none.
     * @param  ending     the end date and time for the content of the dataset, or {@code null} if none.
     *
     * @see #setBounds(Temporal, Temporal)
     *
     * @since 1.5
     */
    public DefaultTemporalExtent(final Temporal beginning, final Temporal ending) {
        extent = TemporalObjects.createPeriod(beginning, ending);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
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
     *       {@linkplain #DefaultTemporalExtent(TemporalExtent) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     *
     * @return the date and time for the content, or {@code null}.
     */
    @Override
    @XmlElement(name = "extent", required = true)
    public TemporalPrimitive getExtent() {
        return extent;
    }

    /**
     * Sets the date and time for the content of the dataset.
     *
     * @param  newValue  the new content date.
     */
    public void setExtent(final TemporalPrimitive newValue) {
        checkWritePermission(extent);
        extent = newValue;
    }

    /**
     * Infers a value from the extent as an {@code Instant} object.
     *
     * @param  begin  {@code true} for the start time, or {@code false} for the end time.
     * @return the requested time as an instant, or {@code null} if none.
     */
    static Temporal getBound(final TemporalPrimitive extent, final boolean begin) {
        if (extent instanceof Period) {
            final var p = (Period) extent;
            return (begin ? p.getBeginning() : p.getEnding()).getPosition();
        }
        return null;
    }

    /**
     * Returns the start of the temporal range for the content of the dataset.
     * This method tries to infer this value from the {@linkplain #getExtent() extent}.
     * The returned object is often an {@link Instant}, but not necessarily.
     *
     * @return the start of the temporal range.
     *
     * @since 1.5
     */
    public Optional<Temporal> getBeginning() {
        return Optional.ofNullable(getBound(extent, true));
    }

    /**
     * Returns the end of the temporal range for the content of the dataset.
     * This method tries to infer this value from the {@linkplain #getExtent() extent}.
     * The returned object is often an {@link Instant}, but not necessarily.
     *
     * @return the end of the temporal range.
     *
     * @since 1.5
     */
    public Optional<Temporal> getEnding() {
        return Optional.ofNullable(getBound(extent, false));
    }

    /**
     * The start date and time for the content of the dataset.
     * This method tries to infer it from the {@linkplain #getExtent() extent}.
     *
     * @return the start time, or {@code null} if none.
     *
     * @deprecated Replaced by {@link #getBeginning()} in order to transition to {@code java.time} API.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public Date getStartTime() {
        return TemporalDate.toDate(getBeginning().orElse(null));
    }

    /**
     * Returns the end date and time for the content of the dataset.
     * This method tries to infer it from the {@linkplain #getExtent() extent}.
     *
     * @return the end time, or {@code null} if none.
     *
     * @deprecated Replaced by {@link #getEnding()} in order to transition to {@code java.time} API.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public Date getEndTime() {
        return TemporalDate.toDate(getEnding().orElse(null));
    }

    /**
     * Sets the temporal extent to the specified values. This convenience method creates a temporal
     * primitive for the given dates, then invokes {@link #setExtent(TemporalPrimitive)}.
     *
     * @param  startTime  the start date and time for the content of the dataset, or {@code null} if none.
     * @param  endTime    the end date and time for the content of the dataset, or {@code null} if none.
     *
     * @deprecated Replaced by {@link #setBounds(Temporal, Temporal)} in order to transition to {@code java.time} API.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public void setBounds(final Date startTime, final Date endTime) {
        setBounds((startTime == null) ? null : startTime.toInstant(),
                    (endTime == null) ? null : endTime.toInstant());
    }

    /**
     * Sets the temporal extent to the specified values. This convenience method creates a temporal
     * primitive for the given dates and/or times, then invokes {@link #setExtent(TemporalPrimitive)}.
     *
     * @param  beginning  the start date and time for the content of the dataset, or {@code null} if none.
     * @param  ending     the end date and time for the content of the dataset, or {@code null} if none.
     *
     * @since 1.5
     */
    public void setBounds(final Temporal beginning, final Temporal ending) {
        setExtent(TemporalObjects.createPeriod(beginning, ending));
    }

    /**
     * Sets this temporal extent to values inferred from the specified envelope.
     * The given envelope must have a {@linkplain Envelope#getCoordinateReferenceSystem() CRS},
     * and at least one dimension of that CRS shall be assignable to a property of this extent.
     *
     * <p><b>Note:</b> this method is available only if the {@code org.apache.sis.referencing}
     * module is available on the module path.</p>
     *
     * @param  envelope  the envelope to use for setting this temporal extent.
     * @throws UnsupportedOperationException if the referencing module is not on the module path.
     * @throws TransformException if the envelope cannot be transformed to a temporal extent.
     *
     * @see DefaultExtent#addElements(Envelope)
     * @see DefaultGeographicBoundingBox#setBounds(Envelope)
     * @see DefaultVerticalExtent#setBounds(Envelope)
     */
    public void setBounds(final Envelope envelope) throws TransformException {
        checkWritePermission(extent);
        if (!ReferencingServices.getInstance().setBounds(envelope, this)) {
            throw new NotSpatioTemporalException(2, envelope);
        }
    }

    /**
     * Sets this temporal extent to the intersection of this extent with the specified one.
     * If there is no intersection between the two extents, then this method sets the temporal primitive to nil.
     * If either this extent or the specified extent has nil primitive, then the intersection result will also be nil.
     *
     * @param  other  the temporal extent to intersect with this extent.
     *
     * @see Extents#intersection(TemporalExtent, TemporalExtent)
     * @see org.apache.sis.geometry.GeneralEnvelope#intersect(Envelope)
     *
     * @since 0.8
     */
    public void intersect(final TemporalExtent other) {
        checkWritePermission(extent);
        final TemporalPrimitive ot = other.getExtent();
        if (ot != null && !(extent instanceof NilObject)) {
            if (extent == null || (ot instanceof NilObject)) {
                extent = ot;
            } else {
                Temporal t0 = getBound(extent, true);
                Temporal t1 = getBound(extent, false);
                Temporal h0 = getBound(ot,     true);
                Temporal h1 = getBound(ot,     false);
                boolean changed = false;
                if (h0 != null && (t0 == null || TemporalDate.compare(h0, t0) > 0)) {
                    t0 = h0;
                    changed = true;
                }
                if (h1 != null && (t1 == null || TemporalDate.compare(h1, t1) < 0)) {
                    t1 = h1;
                    changed = true;
                }
                if (changed) {
                    if (t0 != null && t1 != null && TemporalDate.compare(t0, t1) > 0) {
                        extent = NilReason.MISSING.createNilObject(TemporalPrimitive.class);
                    } else {
                        setBounds(t0, t1);
                    }
                }
            }
        }
    }
}
