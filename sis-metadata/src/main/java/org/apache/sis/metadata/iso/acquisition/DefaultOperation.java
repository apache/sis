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
import org.opengis.metadata.acquisition.Objective;
import org.opengis.metadata.acquisition.Operation;
import org.opengis.metadata.acquisition.OperationType;
import org.opengis.metadata.acquisition.Plan;
import org.opengis.metadata.acquisition.Platform;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Progress;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;


/**
 * Designations for the operation used to acquire the dataset.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
@XmlType(name = "MI_Operation_Type", propOrder = {
    "description",
    "citation",
    "identifier",
    "status",
    "type",
    "childOperations",
    "objectives",
    "parentOperation",
    "plan",
    "platforms",
    "significantEvents"
})
@XmlRootElement(name = "MI_Operation")
public class DefaultOperation extends ISOMetadata implements Operation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4247450339144267883L;

    /**
     * Description of the mission on which the platform observations are made and the
     * objectives of that mission.
     */
    private InternationalString description;

    /**
     * Identification of the mission.
     */
    private Citation citation;

    /**
     * Status of the data acquisition.
     */
    private Progress status;

    /**
     * Collection technique for the operation.
     */
    private OperationType type;

    /**
     * Sub-missions that make up part of a larger mission.
     */
    private Collection<Operation> childOperations;

    /**
     * Object(s) or area(s) of interest to be sensed.
     */
    private Collection<Objective> objectives;

    /**
     * Heritage of the operation.
     */
    private Operation parentOperation;

    /**
     * Plan satisfied by the operation.
     */
    private Plan plan;

    /**
     * Platform (or platforms) used in the operation.
     */
    private Collection<Platform> platforms;

    /**
     * Record of an event occurring during an operation.
     */
    private Collection<Event> significantEvents;

    /**
     * Constructs an initially empty operation.
     */
    public DefaultOperation() {
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
    public static DefaultOperation castOrCopy(final Operation object) {
        if (object == null || object instanceof DefaultOperation) {
            return (DefaultOperation) object;
        }
        final DefaultOperation copy = new DefaultOperation();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the description of the mission on which the platform observations are made and the
     * objectives of that mission. {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "description")
    public synchronized InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the description of the mission on which the platform observations are made and the
     * objectives of that mission.
     *
     * @param newValue The new description value.
     */
    public synchronized void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the identification of the mission. {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "citation")
    public synchronized Citation getCitation() {
        return citation;
    }

    /**
     * Sets the identification of the mission.
     *
     * @param newValue The new citation value.
     */
    public synchronized void setCitation(final Citation newValue) {
        checkWritePermission();
        citation = newValue;
    }

    /**
     * Returns the unique identification of the operation.
     */
    @Override
    @XmlElement(name = "identifier", required = true)
    public Identifier getIdentifier() {
        return NonMarshalledAuthority.getMarshallable(identifiers);
    }

    /**
     * Sets the unique identification of the operation.
     *
     * @param newValue The new identifier value.
     */
    public synchronized void setIdentifier(final Identifier newValue) {
        checkWritePermission();
        identifiers = nonNullCollection(identifiers, Identifier.class);
        NonMarshalledAuthority.setMarshallable(identifiers, newValue);
    }

    /**
     * Returns the status of the data acquisition.
     */
    @Override
    @XmlElement(name = "status", required = true)
    public synchronized Progress getStatus() {
        return status;
    }

    /**
     * Sets the status of the data acquisition.
     *
     * @param newValue The new status value.
     */
    public synchronized void setStatus(final Progress newValue) {
        checkWritePermission();
        status = newValue;
    }

    /**
     * Returns the collection technique for the operation.
     */
    @Override
    @XmlElement(name = "type")
    public synchronized OperationType getType() {
        return type;
    }

    /**
     * Sets the collection technique for the operation.
     *
     * @param newValue The new type value.
     */
    public synchronized void setType(final OperationType newValue) {
        checkWritePermission();
        type = newValue;
    }

    /**
     * Returns the sub-missions that make up part of a larger mission.
     */
    @Override
    @XmlElement(name = "childOperation")
    public synchronized Collection<Operation> getChildOperations() {
        return childOperations = nonNullCollection(childOperations, Operation.class);
    }

    /**
     * Sets the sub-missions that make up part of a larger mission.
     *
     * @param newValues The new child operations values.
     */
    public synchronized void setChildOperations(final Collection<? extends Operation> newValues) {
        childOperations = writeCollection(newValues, childOperations, Operation.class);
    }

    /**
     * Returns object(s) or area(s) of interest to be sensed.
     */
    @Override
    @XmlElement(name = "objective")
    public synchronized Collection<Objective> getObjectives() {
        return objectives = nonNullCollection(objectives, Objective.class);
    }

    /**
     * Sets Object(s) or area(s) of interest to be sensed.
     *
     * @param newValues The new objectives values.
     */
    public synchronized void setObjectives(final Collection<? extends Objective> newValues) {
        objectives = writeCollection(newValues, objectives, Objective.class);
    }

    /**
     * Returns the heritage of the operation.
     */
    @Override
    @XmlElement(name = "parentOperation", required = true)
    public synchronized Operation getParentOperation() {
        return parentOperation;
    }

    /**
     * Sets the heritage of the operation.
     *
     * @param newValue The new parent operation value.
     */
    public synchronized void setParentOperation(final Operation newValue) {
        checkWritePermission();
        parentOperation = newValue;
    }

    /**
     * Returns the plan satisfied by the operation.
     */
    @Override
    @XmlElement(name = "plan")
    public synchronized Plan getPlan() {
        return plan;
    }

    /**
     * Sets the plan satisfied by the operation.
     *
     * @param newValue The new plan value.
     */
    public synchronized void setPlan(final Plan newValue) {
        checkWritePermission();
        plan = newValue;
    }

    /**
     * Returns the platform (or platforms) used in the operation.
     */
    @Override
    @XmlElement(name = "platform")
    public synchronized Collection<Platform> getPlatforms() {
        return platforms = nonNullCollection(platforms, Platform.class);
    }

    /**
     * Sets the platform (or platforms) used in the operation.
     *
     * @param newValues The new platforms values.
     */
    public synchronized void setPlatforms(final Collection<? extends Platform> newValues) {
        platforms = writeCollection(newValues, platforms, Platform.class);
    }

    /**
     * Returns the record of an event occurring during an operation.
     */
    @Override
    @XmlElement(name = "significantEvent")
    public synchronized Collection<Event> getSignificantEvents() {
        return significantEvents = nonNullCollection(significantEvents, Event.class);
    }

    /**
     * Sets the record of an event occurring during an operation.
     *
     * @param newValues The new significant events value.
     */
    public synchronized void setSignificantEvents(final Collection<? extends Event> newValues) {
        significantEvents = writeCollection(newValues, significantEvents, Event.class);
    }
}
