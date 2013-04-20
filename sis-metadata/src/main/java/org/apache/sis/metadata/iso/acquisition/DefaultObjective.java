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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
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
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Objective)
     */
    public DefaultObjective(final Objective object) {
        super(object);
        identifiers         = copyCollection(object.getIdentifiers(), Identifier.class);
        priority            = object.getPriority();
        types               = copyCollection(object.getTypes(), ObjectiveType.class);
        functions           = copyCollection(object.getFunctions(), InternationalString.class);
        extents             = copyCollection(object.getExtents(), Extent.class);
        objectiveOccurences = copyCollection(object.getObjectiveOccurences(), Event.class);
        pass                = copyCollection(object.getPass(), PlatformPass.class);
        sensingInstruments  = copyCollection(object.getSensingInstruments(), Instrument.class);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     * {@section Unified identifiers view}
     * In this SIS implementation, the collection returned by this method includes the XML identifiers
     * ({@linkplain IdentifierSpace#ID ID}, {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>),
     * thus providing a unified view of every kind of identifiers associated to this objective.
     *
     * {@note The <code>&lt:gmd:identifier&gt;</code> element marshalled to XML will exclude
     *        all the above cited identifiers, for ISO 19139 compliance. Those identifiers
     *        will appear in other XML elements or attributes.}
     */
    @Override
    @XmlElement(name = "identifier", required = true)
    public Collection<Identifier> getIdentifiers() {
        identifiers = nonNullCollection(identifiers, Identifier.class);
        return NonMarshalledAuthority.excludeOnMarshalling(identifiers);
    }

    /**
     * Sets the code used to identify the objective.
     *
     * <p>This method overwrites all previous identifiers with the given new values,
     * <strong>except</strong> the XML identifiers ({@linkplain IdentifierSpace#ID ID},
     * {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>), if any. We do not overwrite
     * the XML identifiers because they are usually associated to object identity.</p>
     *
     * @param newValues The new identifiers values.
     */
    public void setIdentifiers(final Collection<? extends Identifier> newValues) {
        final Collection<Identifier> oldIds = NonMarshalledAuthority.filteredCopy(identifiers);
        identifiers = writeCollection(newValues, identifiers, Identifier.class);
        NonMarshalledAuthority.replace(identifiers, oldIds);
    }

    /**
     * Returns the priority applied to the target. {@code null} if unspecified.
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
     */
    @Override
    @XmlElement(name = "extent")
    public Collection<Extent> getExtents() {
        return extents = nonNullCollection(extents, Extent.class);
    }

    /**
     * Set the extent information including the bounding box, bounding polygon, vertical and
     * temporal extent of the objective.
     *
     * @param newValues The new extents values.
     */
    public void setExtents(final Collection<? extends Extent> newValues) {
        extents = writeCollection(newValues, extents, Extent.class);
    }

    /**
     * Returns the event or events associated with objective completion.
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
