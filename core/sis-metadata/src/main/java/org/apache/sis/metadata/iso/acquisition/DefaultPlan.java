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
import org.opengis.metadata.acquisition.GeometryType;
import org.opengis.metadata.acquisition.Operation;
import org.opengis.metadata.acquisition.Plan;
import org.opengis.metadata.acquisition.Requirement;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Progress;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Designations for the planning information related to meeting the data acquisition requirements.
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
@XmlType(name = "MI_Plan_Type", propOrder = {
    "type",
    "status",
    "citation",
    "operations",
    "satisfiedRequirements"
})
@XmlRootElement(name = "MI_Plan")
public class DefaultPlan extends ISOMetadata implements Plan {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1212695055582082867L;

    /**
     * Manner of sampling geometry that the planner expects for collection of objective data.
     */
    private GeometryType type;

    /**
     * Current status of the plan (pending, completed, etc.)
     */
    private Progress status;

    /**
     * Identification of authority requesting target collection.
     */
    private Citation citation;

    /**
     * Identification of the activity or activities that satisfy a plan.
     */
    private Collection<Operation> operations;

    /**
     * Requirement satisfied by the plan.
     */
    private Collection<Requirement> satisfiedRequirements;

    /**
     * Constructs an initially empty plan.
     */
    public DefaultPlan() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Plan)
     */
    public DefaultPlan(final Plan object) {
        super(object);
        if (object != null) {
            type                  = object.getType();
            status                = object.getStatus();
            citation              = object.getCitation();
            operations            = copyCollection(object.getOperations(), Operation.class);
            satisfiedRequirements = copyCollection(object.getSatisfiedRequirements(), Requirement.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultPlan}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultPlan} instance is created using the
     *       {@linkplain #DefaultPlan(Plan) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultPlan castOrCopy(final Plan object) {
        if (object == null || object instanceof DefaultPlan) {
            return (DefaultPlan) object;
        }
        return new DefaultPlan(object);
    }

    /**
     * Returns the manner of sampling geometry that the planner expects for collection of
     * objective data. {@code null} if unspecified.
     *
     * @return Manner of sampling geometry, or {@code null}.
     */
    @Override
    @XmlElement(name = "type")
    public GeometryType getType() {
        return type;
    }

    /**
     * Sets the manner of sampling geometry that the planner expects for collection of
     * objective data.
     *
     * @param newValue The new type value.
     */
    public void setType(final GeometryType newValue) {
        checkWritePermission();
        type = newValue;
    }

    /**
     * Returns the current status of the plan (pending, completed, etc.)
     *
     * @return Current status of the plan, or {@code null}.
     */
    @Override
    @XmlElement(name = "status", required = true)
    public Progress getStatus() {
        return status;
    }

    /**
     * Sets the current status of the plan (pending, completed, etc.)
     *
     * @param newValue The new status value.
     */
    public void setStatus(final Progress newValue) {
        checkWritePermission();
        status = newValue;
    }

    /**
     * Returns the identification of authority requesting target collection.
     *
     * @return Identification of authority requesting target collection, or {@code null}.
     */
    @Override
    @XmlElement(name = "citation", required = true)
    public Citation getCitation() {
        return citation;
    }

    /**
     * Sets the identification of authority requesting target collection.
     *
     * @param newValue The new citation value.
     */
    public void setCitation(final Citation newValue) {
        checkWritePermission();
        citation = newValue;
    }

    /**
     * Returns the identification of the activity or activities that satisfy a plan.
     *
     * @return Identification of the activity or activities.
     */
    @Override
    @XmlElement(name = "operation")
    public Collection<Operation> getOperations() {
        return operations = nonNullCollection(operations, Operation.class);
    }

    /**
     * Sets the identification of the activity or activities that satisfy a plan.
     *
     * @param newValues The new identifications of the activity.
     */
    public void setOperations(final Collection<? extends Operation> newValues) {
        operations = writeCollection(newValues, operations, Operation.class);
    }

    /**
     * Returns the requirement satisfied by the plan.
     *
     * @return Requirement satisfied by the plan.
     */
    @Override
    @XmlElement(name = "satisfiedRequirement")
    public Collection<Requirement> getSatisfiedRequirements() {
        return satisfiedRequirements = nonNullCollection(satisfiedRequirements, Requirement.class);
    }

    /**
     * Sets the requirement satisfied by the plan.
     *
     * @param newValues The new satisfied requirements.
     */
    public void setSatisfiedRequirements(final Collection<? extends Requirement> newValues) {
        satisfiedRequirements = writeCollection(newValues, satisfiedRequirements, Requirement.class);
    }
}
