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
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.acquisition.EnvironmentalRecord;
import org.opengis.metadata.acquisition.Instrument;
import org.opengis.metadata.acquisition.Objective;
import org.opengis.metadata.acquisition.Operation;
import org.opengis.metadata.acquisition.Plan;
import org.opengis.metadata.acquisition.Platform;
import org.opengis.metadata.acquisition.Requirement;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Designations for the measuring instruments, the platform carrying them, and the mission to
 * which the data contributes.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
@XmlType(name = "MI_AcquisitionInformation_Type", propOrder = {
    "acquisitionPlans",
    "acquisitionRequirements",
    "environmentalConditions",
    "instruments",
    "objectives",
    "operations",
    "platforms"
})
@XmlRootElement(name = "MI_AcquisitionInformation")
public class DefaultAcquisitionInformation extends ISOMetadata implements AcquisitionInformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1232071263806560246L;

    /**
     * Identifies the plan as implemented by the acquisition.
     */
    private Collection<Plan> acquisitionPlans;

    /**
     * Identifies the requirement the data acquisition intends to satisfy.
     */
    private Collection<Requirement> acquisitionRequirements;

    /**
     * A record of the environmental circumstances during the data acquisition.
     */
    private EnvironmentalRecord environmentalConditions;

    /**
     * General information about the instrument used in data acquisition.
     */
    private Collection<Instrument> instruments;

    /**
     * Identification of the area or object to be sensed.
     */
    private Collection<Objective> objectives;

    /**
     * General information about an identifiable activity which provided the data.
     */
    private Collection<Operation> operations;

    /**
     * General information about the platform from which the data were taken.
     */
    private Collection<Platform> platforms;

    /**
     * Constructs an initially empty acquisition information.
     */
    public DefaultAcquisitionInformation() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(AcquisitionInformation)
     */
    public DefaultAcquisitionInformation(final AcquisitionInformation object) {
        super(object);
        acquisitionPlans        = copyCollection(object.getAcquisitionPlans(), Plan.class);
        acquisitionRequirements = copyCollection(object.getAcquisitionRequirements(), Requirement.class);
        environmentalConditions = object.getEnvironmentalConditions();
        instruments             = copyCollection(object.getInstruments(), Instrument.class);
        objectives              = copyCollection(object.getObjectives(), Objective.class);
        operations              = copyCollection(object.getOperations(), Operation.class);
        platforms               = copyCollection(object.getPlatforms(), Platform.class);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultAcquisitionInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultAcquisitionInformation} instance is created using the
     *       {@linkplain #DefaultAcquisitionInformation(AcquisitionInformation) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAcquisitionInformation castOrCopy(final AcquisitionInformation object) {
        if (object == null || object instanceof DefaultAcquisitionInformation) {
            return (DefaultAcquisitionInformation) object;
        }
        return new DefaultAcquisitionInformation(object);
    }

    /**
     * Returns the plan as implemented by the acquisition.
     */
    @Override
    @XmlElement(name = "acquisitionPlan")
    public synchronized Collection<Plan> getAcquisitionPlans() {
        return acquisitionPlans = nonNullCollection(acquisitionPlans, Plan.class);
    }

    /**
     * Sets the plan as implemented by the acquisition.
     *
     * @param newValues The new plan values.
     */
    public synchronized void setAcquisitionPlans(final Collection<? extends Plan> newValues) {
        acquisitionPlans = writeCollection(newValues, acquisitionPlans, Plan.class);
    }

    /**
     * Returns the requirement the data acquisition intends to satisfy.
     */
    @Override
    @XmlElement(name = "acquisitionRequirement")
    public synchronized Collection<Requirement> getAcquisitionRequirements() {
        return acquisitionRequirements = nonNullCollection(acquisitionRequirements, Requirement.class);
    }

    /**
     * Sets the requirement the data acquisition intends to satisfy.
     *
     * @param newValues The new acquisition requirements values.
     */
    public synchronized void setAcquisitionRequirements(final Collection<? extends Requirement> newValues) {
        acquisitionRequirements = writeCollection(newValues, acquisitionRequirements, Requirement.class);
    }

    /**
     * Returns a record of the environmental circumstances during the data acquisition.
     * {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "environmentalConditions")
    public synchronized EnvironmentalRecord getEnvironmentalConditions() {
        return environmentalConditions;
    }

    /**
     * Sets the record of the environmental circumstances during the data acquisition.
     *
     * @param newValue The new environmental record value.
     */
    public synchronized void setEnvironmentalConditions(final EnvironmentalRecord newValue) {
        checkWritePermission();
        environmentalConditions = newValue;
    }

    /**
     * Returns the general information about the instrument used in data acquisition.
     */
    @Override
    @XmlElement(name = "instrument")
    public synchronized Collection<Instrument> getInstruments() {
        return instruments = nonNullCollection(instruments, Instrument.class);
    }

    /**
     * Sets the general information about the instrument used in data acquisition.
     *
     * @param newValues The new instruments values.
     */
    public synchronized void setInstruments(final Collection<? extends Instrument> newValues) {
        instruments = writeCollection(newValues, instruments, Instrument.class);
    }

    /**
     * Returns the area or object to be sensed.
     */
    @Override
    @XmlElement(name = "objective")
    public synchronized Collection<Objective> getObjectives() {
        return objectives = nonNullCollection(objectives, Objective.class);
    }

    /**
     * Sets the area or object to be sensed.
     *
     * @param newValues The new objectives values.
     */
    public synchronized void setObjectives(final Collection<? extends Objective> newValues) {
        objectives = writeCollection(newValues, objectives, Objective.class);
    }

    /**
     * Returns the general information about an identifiable activity which provided the data.
     */
    @Override
    @XmlElement(name = "operation")
    public synchronized Collection<Operation> getOperations() {
        return operations = nonNullCollection(operations, Operation.class);
    }

    /**
     * Sets the general information about an identifiable activity which provided the data.
     *
     * @param newValues The new operations values.
     */
    public synchronized void setOperations(final Collection<? extends Operation> newValues) {
        operations = writeCollection(newValues, operations, Operation.class);
    }

    /**
     * Returns the general information about the platform from which the data were taken.
     */
    @Override
    @XmlElement(name = "platform")
    public synchronized Collection<Platform> getPlatforms() {
        return platforms = nonNullCollection(platforms, Platform.class);
    }

    /**
     * Sets the general information about the platform from which the data were taken.
     *
     * @param newValues The new platforms values.
     */
    public synchronized void setPlatforms(final Collection<? extends Platform> newValues) {
        platforms = writeCollection(newValues, platforms, Platform.class);
    }
}
