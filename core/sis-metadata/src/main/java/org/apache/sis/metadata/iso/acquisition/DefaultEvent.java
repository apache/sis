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
package org.apache.sis.metadata.iso.acquisition;

import java.util.Collection;
import java.util.Date;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.acquisition.Context;
import org.opengis.metadata.acquisition.Event;
import org.opengis.metadata.acquisition.Instrument;
import org.opengis.metadata.acquisition.Objective;
import org.opengis.metadata.acquisition.PlatformPass;
import org.opengis.metadata.acquisition.Sequence;
import org.opengis.metadata.acquisition.Trigger;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;

import static org.apache.sis.internal.metadata.MetadataUtilities.toDate;
import static org.apache.sis.internal.metadata.MetadataUtilities.toMilliseconds;


/**
 * Identification of a significant collection point within an operation.
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "MI_Event_Type", propOrder = {
    "identifier",
    "trigger",
    "context",
    "sequence",
    "time",
    "expectedObjectives",
    "relatedPass",
    "relatedSensors"
})
@XmlRootElement(name = "MI_Event")
public class DefaultEvent extends ISOMetadata implements Event {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -519920133287763009L;

    /**
     * Initiator of the event.
     */
    private Trigger trigger;

    /**
     * Meaning of the event.
     */
    private Context context;

    /**
     * Relative time ordering of the event.
     */
    private Sequence sequence;

    /**
     * Time the event occurred, or {@link Long#MIN_VALUE} if none.
     */
    private long time = Long.MIN_VALUE;

    /**
     * Objective or objectives satisfied by an event.
     */
    private Collection<Objective> expectedObjectives;

    /**
     * Pass during which an event occurs.
     */
    private PlatformPass relatedPass;

    /**
     * Instrument or instruments for which the event is meaningful.
     */
    private Collection<Instrument> relatedSensors;

    /**
     * Constructs an initially empty acquisition information.
     */
    public DefaultEvent() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Event)
     */
    public DefaultEvent(final Event object) {
        super(object);
        if (object != null) {
            identifiers        = singleton(object.getIdentifier(), Identifier.class);
            trigger            = object.getTrigger();
            context            = object.getContext();
            sequence           = object.getSequence();
            time               = toMilliseconds(object.getTime());
            expectedObjectives = copyCollection(object.getExpectedObjectives(), Objective.class);
            relatedPass        = object.getRelatedPass();
            relatedSensors     = copyCollection(object.getRelatedSensors(), Instrument.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultEvent}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultEvent} instance is created using the
     *       {@linkplain #DefaultEvent(Event) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultEvent castOrCopy(final Event object) {
        if (object == null || object instanceof DefaultEvent) {
            return (DefaultEvent) object;
        }
        return new DefaultEvent(object);
    }

    /**
     * Returns the event name or number.
     *
     * @return Event name or number, or {@code null}.
     */
    @Override
    @XmlElement(name = "identifier", required = true)
    public Identifier getIdentifier() {
        return NonMarshalledAuthority.getMarshallable(identifiers);
    }

    /**
     * Sets the event name or number.
     *
     * @param newValue The event identifier value.
     */
    public void setIdentifier(final Identifier newValue) {
        checkWritePermission();
        identifiers = nonNullCollection(identifiers, Identifier.class);
        NonMarshalledAuthority.setMarshallable(identifiers, newValue);
    }

    /**
     * Returns the initiator of the event.
     *
     * @return Initiator of the event, or {@code null}.
     */
    @Override
    @XmlElement(name = "trigger", required = true)
    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * Sets the initiator of the event.
     *
     * @param newValue The new trigger value.
     */
    public void setTrigger(final Trigger newValue) {
        checkWritePermission();
        trigger = newValue;
    }

    /**
     * Meaning of the event.
     *
     * @return Meaning of the event, or {@code null}.
     */
    @Override
    @XmlElement(name = "context", required = true)
    public Context getContext() {
        return context;
    }

    /**
     * Sets the meaning of the event.
     *
     * @param newValue The new context value.
     */
    public void setContext(final Context newValue) {
        checkWritePermission();
        context = newValue;
    }

    /**
     * Returns the relative time ordering of the event.
     *
     * @return Relative time ordering, or {@code null}.
     */
    @Override
    @XmlElement(name = "sequence", required = true)
    public Sequence getSequence() {
        return sequence;
    }

    /**
     * Sets the relative time ordering of the event.
     *
     * @param newValue The new sequence value.
     */
    public void setSequence(final Sequence newValue) {
        checkWritePermission();
        sequence = newValue;
    }

    /**
     * Returns the time the event occurred.
     *
     * @return Time the event occurred, or {@code null}.
     */
    @Override
    @XmlElement(name = "time", required = true)
    public Date getTime() {
        return toDate(time);
    }

    /**
     * Sets the time the event occurred.
     *
     * @param newValue The new time value.
     */
    public void setTime(final Date newValue) {
        checkWritePermission();
        time = toMilliseconds(newValue);
    }

    /**
     * Returns the objective or objectives satisfied by an event.
     *
     * @return Objectives satisfied by an event.
     */
    @Override
    @XmlElement(name = "expectedObjective")
    public Collection<Objective> getExpectedObjectives() {
        return expectedObjectives = nonNullCollection(expectedObjectives, Objective.class);
    }

    /**
     * Sets the objective or objectives satisfied by an event.
     *
     * @param newValues The new expected objectives values.
     */
    public void setExpectedObjectives(final Collection<? extends Objective> newValues) {
        expectedObjectives = writeCollection(newValues, expectedObjectives, Objective.class);
    }

    /**
     * Returns the pass during which an event occurs. {@code null} if unspecified.
     *
     * @return Pass during which an event occurs, or {@code null}.
     */
    @Override
    @XmlElement(name = "relatedPass")
    public PlatformPass getRelatedPass() {
        return relatedPass;
    }

    /**
     * Sets the pass during which an event occurs.
     *
     * @param newValue The new platform pass value.
     */
    public void setRelatedPass(final PlatformPass newValue) {
        relatedPass = newValue;
    }

    /**
     * Returns the instrument or instruments for which the event is meaningful.
     *
     * @return Instruments for which the event is meaningful.
     */
    @Override
    @XmlElement(name = "relatedSensor")
    public Collection<? extends Instrument> getRelatedSensors() {
        return relatedSensors = nonNullCollection(relatedSensors, Instrument.class);
    }

    /**
     * Sets the instrument or instruments for which the event is meaningful.
     *
     * @param newValues The new instrument values.
     */
    public void setRelatedSensors(final Collection<? extends Instrument> newValues) {
        relatedSensors = writeCollection(newValues, relatedSensors, Instrument.class);
    }
}
