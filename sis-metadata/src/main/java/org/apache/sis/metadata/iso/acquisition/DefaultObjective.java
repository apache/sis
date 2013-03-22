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

import static org.apache.sis.internal.jaxb.MarshalContext.filterIdentifiers;


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
    private static final long serialVersionUID = -4633298523976029384L;

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
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultObjective castOrCopy(final Objective object) {
        if (object == null || object instanceof DefaultObjective) {
            return (DefaultObjective) object;
        }
        final DefaultObjective copy = new DefaultObjective();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the code used to identify the objective.
     *
     * {@section Implementation limitation}
     * In the current SIS implementation, the returned list is always unmodifiable. The only way
     * to change the collection of identifiers is to invoke {@link #setIdentifiers(Collection)}.
     * This limitation may be removed in a future SIS version.
     */
    @Override
    @XmlElement(name = "identifier", required = true)
    public Collection<Identifier> getIdentifiers() {
        identifiers = nonNullCollection(identifiers, Identifier.class);
        return filterIdentifiers(identifiers);
    }

    /**
     * Sets the code used to identify the objective.
     * If the given collection contains XML identifiers like {@linkplain IdentifierSpace#ID ID}
     * or {@linkplain IdentifierSpace#UUID UUID}, then those identifiers are ignored.
     *
     * @param newValues The new identifiers values.
     */
    public synchronized void setIdentifiers(final Collection<? extends Identifier> newValues) {
        final Collection<Identifier> oldIds = NonMarshalledAuthority.getIdentifiers(identifiers);
        identifiers = copyCollection(newValues, identifiers, Identifier.class);
        NonMarshalledAuthority.setIdentifiers(identifiers, oldIds);
    }

    /**
     * Returns the priority applied to the target. {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "priority")
    public synchronized InternationalString getPriority() {
        return priority;
    }

    /**
     * Sets the priority applied to the target.
     *
     * @param newValue The new priority value.
     */
    public synchronized void setPriority(final InternationalString newValue) {
        checkWritePermission();
        priority = newValue;
    }

    /**
     * Returns the collection technique for the objective.
     */
    @Override
    @XmlElement(name = "type")
    public synchronized Collection<ObjectiveType> getTypes() {
        return types = nonNullCollection(types, ObjectiveType.class);
    }

    /**
     * Sets the collection technique for the objective.
     *
     * @param newValues The new types values.
     */
    public synchronized void setTypes(final Collection<? extends ObjectiveType> newValues) {
        types = copyCollection(newValues, types, ObjectiveType.class);
    }

    /**
     * Returns the role or purpose performed by or activity performed at the objective.
     */
    @Override
    @XmlElement(name = "function")
    public synchronized Collection<InternationalString> getFunctions() {
        return functions = nonNullCollection(functions, InternationalString.class);
    }

    /**
     * Sets the role or purpose performed by or activity performed at the objective.
     *
     * @param newValues The new functions values.
     */
    public synchronized void setFunctions(final Collection<? extends InternationalString> newValues) {
        functions = copyCollection(newValues, functions, InternationalString.class);
    }

    /**
     * Returns the extent information including the bounding box, bounding polygon, vertical and
     * temporal extent of the objective.
     */
    @Override
    @XmlElement(name = "extent")
    public synchronized Collection<Extent> getExtents() {
        return extents = nonNullCollection(extents, Extent.class);
    }

    /**
     * Set the extent information including the bounding box, bounding polygon, vertical and
     * temporal extent of the objective.
     *
     * @param newValues The new extents values.
     */
    public synchronized void setExtents(final Collection<? extends Extent> newValues) {
        extents = copyCollection(newValues, extents, Extent.class);
    }

    /**
     * Returns the event or events associated with objective completion.
     */
    @Override
    @XmlElement(name = "objectiveOccurence", required = true)
    public synchronized Collection<Event> getObjectiveOccurences() {
        return objectiveOccurences = nonNullCollection(objectiveOccurences, Event.class);
    }

    /**
     * Sets the event or events associated with objective completion.
     *
     * @param newValues The new objective occurrences values.
     */
    public synchronized void setObjectiveOccurences(final Collection<? extends Event> newValues) {
        objectiveOccurences = copyCollection(newValues, objectiveOccurences, Event.class);
    }

    /**
     * Returns the pass of the platform over the objective.
     */
    @Override
    @XmlElement(name = "pass")
    public synchronized Collection<PlatformPass> getPass() {
        return pass = nonNullCollection(pass, PlatformPass.class);
    }

    /**
     * Sets the pass of the platform over the objective.
     *
     * @param newValues The new pass values.
     */
    public synchronized void setPass(final Collection<? extends PlatformPass> newValues) {
        pass = copyCollection(newValues, pass, PlatformPass.class);
    }

    /**
     * Returns the instrument which senses the objective data.
     */
    @Override
    @XmlElement(name = "sensingInstrument")
    public synchronized Collection<Instrument> getSensingInstruments() {
        return sensingInstruments = nonNullCollection(sensingInstruments, Instrument.class);
    }

    /**
     * Sets the instrument which senses the objective data.
     *
     * @param newValues The new sensing instruments values.
     */
    public synchronized void setSensingInstruments(final Collection<? extends Instrument> newValues) {
        sensingInstruments = copyCollection(newValues, sensingInstruments, Instrument.class);
    }
}
