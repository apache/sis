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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.acquisition.Event;
import org.opengis.metadata.acquisition.Instrument;
import org.opengis.metadata.acquisition.Objective;
import org.opengis.metadata.acquisition.ObjectiveType;
import org.opengis.metadata.acquisition.PlatformPass;
import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;


/**
 * Describes the characteristics, spatial and temporal extent of the intended object to be observed.
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
 * @version 0.7
 * @module
 */
@XmlType(name = "MI_Objective_Type", propOrder = {
    "identifiers",
    "priority",
    "types",
    "functions",
    "extents",
    "objectiveOccurences",
    "pass",
    "sensingInstruments"
})
@XmlRootElement(name = "MI_Objective")
public class DefaultObjective extends ISOMetadata implements Objective {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8273806197892815938L;

    /**
     * Priority applied to the target.
     */
    private InternationalString priority;

    /**
     * Collection technique for the objective.
     */
    private Collection<ObjectiveType> types;

    /**
     * Role or purpose performed by or activity performed at the objective.
     */
    private Collection<InternationalString> functions;

    /**
     * Extent information including the bounding box, bounding polygon, vertical and
     * temporal extent of the objective.
     */
    private Collection<Extent> extents;

    /**
     * Event or events associated with objective completion.
     */
    private Collection<Event> objectiveOccurences;

    /**
     * Pass of the platform over the objective.
     */
    private Collection<PlatformPass> pass;

    /**
     * Instrument which senses the objective data.
     */
    private Collection<Instrument> sensingInstruments;

    /**
     * Constructs an initially empty objective.
     */
    public DefaultObjective() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Objective)
     */
    public DefaultObjective(final Objective object) {
        super(object);
        if (object != null) {
            identifiers         = copyCollection(object.getIdentifiers(), Identifier.class);
            priority            = object.getPriority();
            types               = copyCollection(object.getTypes(), ObjectiveType.class);
            functions           = copyCollection(object.getFunctions(), InternationalString.class);
            extents             = copyCollection(object.getExtents(), Extent.class);
            objectiveOccurences = copyCollection(object.getObjectiveOccurences(), Event.class);
            pass                = copyCollection(object.getPass(), PlatformPass.class);
            sensingInstruments  = copyCollection(object.getSensingInstruments(), Instrument.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultObjective}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultObjective} instance is created using the
     *       {@linkplain #DefaultObjective(Objective) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultObjective castOrCopy(final Objective object) {
        if (object == null || object instanceof DefaultObjective) {
            return (DefaultObjective) object;
        }
        return new DefaultObjective(object);
    }

    /**
     * Returns the code used to identify the objective.
     *
     * <div class="section">Unified identifiers view</div>
     * In this SIS implementation, the collection returned by this method includes the XML identifiers
     * ({@linkplain IdentifierSpace#ID ID}, {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>),
     * thus providing a unified view of every kind of identifiers associated to this objective.
     *
     * <div class="note"><b>XML note:</b>
     * The {@code <gmd:identifier>} element marshalled to XML will exclude all the above cited identifiers,
     * for ISO 19139 compliance. Those identifiers will appear in other XML elements or attributes.</div>
     *
     * @return Identify the objective.
     */
    @Override
    @XmlElement(name = "identifier", required = true)
    public Collection<Identifier> getIdentifiers() {
        return NonMarshalledAuthority.filterOnMarshalling(super.getIdentifiers());
    }

    /**
     * Sets the code used to identify the objective.
     *
     * <p>XML identifiers ({@linkplain IdentifierSpace#ID ID}, {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>),
     * are not affected by this method, unless they are explicitely provided in the given collection.</p>
     *
     * @param newValues The new identifiers values.
     */
    public void setIdentifiers(Collection<? extends Identifier> newValues) {
        newValues = NonMarshalledAuthority.setMarshallables(identifiers, newValues);
        identifiers = writeCollection(newValues, identifiers, Identifier.class);
    }

    /**
     * Returns the priority applied to the target. {@code null} if unspecified.
     *
     * @return Priority applied, or {@code null}.
     */
    @Override
    @XmlElement(name = "priority")
    public InternationalString getPriority() {
        return priority;
    }

    /**
     * Sets the priority applied to the target.
     *
     * @param newValue The new priority value.
     */
    public void setPriority(final InternationalString newValue) {
        checkWritePermission();
        priority = newValue;
    }

    /**
     * Returns the collection technique for the objective.
     *
     * @return Collection technique for the objective.
     */
    @Override
    @XmlElement(name = "type")
    public Collection<ObjectiveType> getTypes() {
        return types = nonNullCollection(types, ObjectiveType.class);
    }

    /**
     * Sets the collection technique for the objective.
     *
     * @param newValues The new types values.
     */
    public void setTypes(final Collection<? extends ObjectiveType> newValues) {
        types = writeCollection(newValues, types, ObjectiveType.class);
    }

    /**
     * Returns the role or purpose performed by or activity performed at the objective.
     *
     * @return Role or purpose performed by or activity performed at the objective.
     */
    @Override
    @XmlElement(name = "function")
    public Collection<InternationalString> getFunctions() {
        return functions = nonNullCollection(functions, InternationalString.class);
    }

    /**
     * Sets the role or purpose performed by or activity performed at the objective.
     *
     * @param newValues The new functions values.
     */
    public void setFunctions(final Collection<? extends InternationalString> newValues) {
        functions = writeCollection(newValues, functions, InternationalString.class);
    }

    /**
     * Returns the extent information including the bounding box, bounding polygon, vertical and
     * temporal extent of the objective.
     *
     * @return Extent information.
     */
    @Override
    @XmlElement(name = "extent")
    public Collection<Extent> getExtents() {
        return extents = nonNullCollection(extents, Extent.class);
    }

    /**
     * Sets the extent information including the bounding box, bounding polygon, vertical and
     * temporal extent of the objective.
     *
     * @param newValues The new extents values.
     */
    public void setExtents(final Collection<? extends Extent> newValues) {
        extents = writeCollection(newValues, extents, Extent.class);
    }

    /**
     * Returns the event or events associated with objective completion.
     *
     * @return Events associated with objective completion.
     */
    @Override
    @XmlElement(name = "objectiveOccurence", required = true)
    public Collection<Event> getObjectiveOccurences() {
        return objectiveOccurences = nonNullCollection(objectiveOccurences, Event.class);
    }

    /**
     * Sets the event or events associated with objective completion.
     *
     * @param newValues The new objective occurrences values.
     */
    public void setObjectiveOccurences(final Collection<? extends Event> newValues) {
        objectiveOccurences = writeCollection(newValues, objectiveOccurences, Event.class);
    }

    /**
     * Returns the pass of the platform over the objective.
     *
     * @return Pass of the platform.
     */
    @Override
    @XmlElement(name = "pass")
    public Collection<PlatformPass> getPass() {
        return pass = nonNullCollection(pass, PlatformPass.class);
    }

    /**
     * Sets the pass of the platform over the objective.
     *
     * @param newValues The new pass values.
     */
    public void setPass(final Collection<? extends PlatformPass> newValues) {
        pass = writeCollection(newValues, pass, PlatformPass.class);
    }

    /**
     * Returns the instrument which senses the objective data.
     *
     * @return Instrument which senses the objective data.
     */
    @Override
    @XmlElement(name = "sensingInstrument")
    public Collection<Instrument> getSensingInstruments() {
        return sensingInstruments = nonNullCollection(sensingInstruments, Instrument.class);
    }

    /**
     * Sets the instrument which senses the objective data.
     *
     * @param newValues The new sensing instruments values.
     */
    public void setSensingInstruments(final Collection<? extends Instrument> newValues) {
        sensingInstruments = writeCollection(newValues, sensingInstruments, Instrument.class);
    }
}
