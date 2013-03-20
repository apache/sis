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
 *
 * This package contains documentation from OGC specifications.
 * Open Geospatial Consortium's work is fully acknowledged here.
 */
package org.apache.sis.metadata.iso.distribution;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.StandardOrderProcess;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information about the distributor.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Distributor_Type", propOrder = {
    "distributorContact",
    "distributionOrderProcesses",
    "distributorFormats",
    "distributorTransferOptions"
})
@XmlRootElement(name = "MD_Distributor")
public class DefaultDistributor extends ISOMetadata implements Distributor {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7142984376823483766L;

    /**
     * Party from whom the resource may be obtained. This list need not be exhaustive.
     */
    private ResponsibleParty distributorContact;

    /**
     * Provides information about how the resource may be obtained, and related
     * instructions and fee information.
     */
    private Collection<StandardOrderProcess> distributionOrderProcesses;

    /**
     * Provides information about the format used by the distributor.
     */
    private Collection<Format> distributorFormats;

    /**
     * Provides information about the technical means and media used by the distributor.
     */
    private Collection<DigitalTransferOptions> distributorTransferOptions;

    /**
     * Constructs an initially empty distributor.
     */
    public DefaultDistributor() {
    }

    /**
     * Creates a distributor with the specified contact.
     *
     * @param distributorContact Party from whom the resource may be obtained, or {@code null}.
     */
    public DefaultDistributor(final ResponsibleParty distributorContact) {
        this.distributorContact = distributorContact;
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
    public static DefaultDistributor castOrCopy(final Distributor object) {
        if (object == null || object instanceof DefaultDistributor) {
            return (DefaultDistributor) object;
        }
        final DefaultDistributor copy = new DefaultDistributor();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Party from whom the resource may be obtained. This list need not be exhaustive.
     */
    @Override
    @XmlElement(name = "distributorContact", required = true)
    public synchronized ResponsibleParty getDistributorContact() {
        return distributorContact;
    }

    /**
     * Sets the party from whom the resource may be obtained. This list need not be exhaustive.
     *
     * @param newValue The new distributor contact.
     */
    public synchronized void setDistributorContact(final ResponsibleParty newValue) {
        checkWritePermission();
        distributorContact = newValue;
    }

    /**
     * Provides information about how the resource may be obtained,
     * and related instructions and fee information.
     */
    @Override
    @XmlElement(name = "distributionOrderProcess")
    public synchronized Collection<StandardOrderProcess> getDistributionOrderProcesses() {
        return distributionOrderProcesses = nonNullCollection(distributionOrderProcesses, StandardOrderProcess.class);
    }

    /**
     * Sets information about how the resource may be obtained,
     * and related instructions and fee information.
     *
     * @param newValues The new distribution order processes.
     */
    public synchronized void setDistributionOrderProcesses(final Collection<? extends StandardOrderProcess> newValues) {
        distributionOrderProcesses = copyCollection(newValues, distributionOrderProcesses, StandardOrderProcess.class);
    }

    /**
     * Provides information about the format used by the distributor.
     */
    @Override
    @XmlElement(name = "distributorFormat")
    public synchronized Collection<Format> getDistributorFormats() {
        return distributorFormats = nonNullCollection(distributorFormats, Format.class);
    }

    /**
     * Sets information about the format used by the distributor.
     *
     * @param newValues The new distributor formats.
     */
    public synchronized void setDistributorFormats(final Collection<? extends Format> newValues) {
        distributorFormats = copyCollection(newValues, distributorFormats, Format.class);
    }

    /**
     * Provides information about the technical means and media used by the distributor.
     */
    @Override
    @XmlElement(name = "distributorTransferOptions")
    public synchronized Collection<DigitalTransferOptions> getDistributorTransferOptions() {
        return distributorTransferOptions = nonNullCollection(distributorTransferOptions, DigitalTransferOptions.class);
    }

    /**
     * Provides information about the technical means and media used by the distributor.
     *
     * @param newValues The new distributor transfer options.
     */
    public synchronized void setDistributorTransferOptions(final Collection<? extends DigitalTransferOptions> newValues) {
        distributorTransferOptions = copyCollection(newValues, distributorTransferOptions, DigitalTransferOptions.class);
    }
}
