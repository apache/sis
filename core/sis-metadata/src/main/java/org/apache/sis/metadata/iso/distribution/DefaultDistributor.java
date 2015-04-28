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
 * <p><b>Limitations:</b></p>
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
 * @version 0.5
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
    private static final long serialVersionUID = 5706757156163948001L;

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
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Distributor)
     */
    public DefaultDistributor(final Distributor object) {
        super(object);
        if (object != null) {
            distributorContact         = object.getDistributorContact();
            distributionOrderProcesses = copyCollection(object.getDistributionOrderProcesses(), StandardOrderProcess.class);
            distributorFormats         = copyCollection(object.getDistributorFormats(), Format.class);
            distributorTransferOptions = copyCollection(object.getDistributorTransferOptions(), DigitalTransferOptions.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultDistributor}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDistributor} instance is created using the
     *       {@linkplain #DefaultDistributor(Distributor) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDistributor castOrCopy(final Distributor object) {
        if (object == null || object instanceof DefaultDistributor) {
            return (DefaultDistributor) object;
        }
        return new DefaultDistributor(object);
    }

    /**
     * Party from whom the resource may be obtained.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return Party from whom the resource may be obtained, or {@code null}.
     */
    @Override
    @XmlElement(name = "distributorContact", required = true)
    public ResponsibleParty getDistributorContact() {
        return distributorContact;
    }

    /**
     * Sets the party from whom the resource may be obtained.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param newValue The new distributor contact.
     */
    public void setDistributorContact(final ResponsibleParty newValue) {
        checkWritePermission();
        distributorContact = newValue;
    }

    /**
     * Provides information about how the resource may be obtained,
     * and related instructions and fee information.
     *
     * @return Information about how the resource may be obtained.
     */
    @Override
    @XmlElement(name = "distributionOrderProcess")
    public Collection<StandardOrderProcess> getDistributionOrderProcesses() {
        return distributionOrderProcesses = nonNullCollection(distributionOrderProcesses, StandardOrderProcess.class);
    }

    /**
     * Sets information about how the resource may be obtained,
     * and related instructions and fee information.
     *
     * @param newValues The new distribution order processes.
     */
    public void setDistributionOrderProcesses(final Collection<? extends StandardOrderProcess> newValues) {
        distributionOrderProcesses = writeCollection(newValues, distributionOrderProcesses, StandardOrderProcess.class);
    }

    /**
     * Provides information about the format used by the distributor.
     *
     * @return Information about the format used by the distributor.
     */
    @Override
    @XmlElement(name = "distributorFormat")
    public Collection<Format> getDistributorFormats() {
        return distributorFormats = nonNullCollection(distributorFormats, Format.class);
    }

    /**
     * Sets information about the format used by the distributor.
     *
     * @param newValues The new distributor formats.
     */
    public void setDistributorFormats(final Collection<? extends Format> newValues) {
        distributorFormats = writeCollection(newValues, distributorFormats, Format.class);
    }

    /**
     * Provides information about the technical means and media used by the distributor.
     *
     * @return Information about the technical means and media used by the distributor.
     */
    @Override
    @XmlElement(name = "distributorTransferOptions")
    public Collection<DigitalTransferOptions> getDistributorTransferOptions() {
        return distributorTransferOptions = nonNullCollection(distributorTransferOptions, DigitalTransferOptions.class);
    }

    /**
     * Provides information about the technical means and media used by the distributor.
     *
     * @param newValues The new distributor transfer options.
     */
    public void setDistributorTransferOptions(final Collection<? extends DigitalTransferOptions> newValues) {
        distributorTransferOptions = writeCollection(newValues, distributorTransferOptions, DigitalTransferOptions.class);
    }
}
