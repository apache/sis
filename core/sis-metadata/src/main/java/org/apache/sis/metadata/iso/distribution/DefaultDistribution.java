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
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the distributor of and options for obtaining the resource.
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
@XmlType(name = "MD_Distribution_Type", propOrder = {
    "distributionFormats",
    "distributors",
    "transferOptions"
})
@XmlRootElement(name = "MD_Distribution")
public class DefaultDistribution extends ISOMetadata implements Distribution {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1331353255189686369L;

    /**
     * Brief description of a set of distribution options.
     */
    private InternationalString description;

    /**
     * Provides a description of the format of the data to be distributed.
     */
    private Collection<Format> distributionFormats;

    /**
     * Provides information about the distributor.
     */
    private Collection<Distributor> distributors;

    /**
     * Provides information about technical means and media by which a resource is obtained
     * from the distributor.
     */
    private Collection<DigitalTransferOptions> transferOptions;

    /**
     * Constructs an initially empty distribution.
     */
    public DefaultDistribution() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Distribution)
     */
    public DefaultDistribution(final Distribution object) {
        super(object);
        if (object != null) {
            distributionFormats = copyCollection(object.getDistributionFormats(), Format.class);
            distributors        = copyCollection(object.getDistributors(), Distributor.class);
            transferOptions     = copyCollection(object.getTransferOptions(), DigitalTransferOptions.class);
            if (object instanceof DefaultDistribution) {
                description = ((DefaultDistribution) object).getDescription();
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultDistribution}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDistribution} instance is created using the
     *       {@linkplain #DefaultDistribution(Distribution) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDistribution castOrCopy(final Distribution object) {
        if (object == null || object instanceof DefaultDistribution) {
            return (DefaultDistribution) object;
        }
        return new DefaultDistribution(object);
    }

    /**
     * Returns a brief description of a set of distribution options.
     *
     * @return Brief description of a set of distribution options.
     *
     * @since 0.5
     */
    @UML(identifier="description", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets a brief description of a set of distribution options.
     *
     * @param newValue The new description.
     *
     * @since 0.5
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Provides a description of the format of the data to be distributed.
     *
     * @return Description of the format of the data to be distributed.
     */
    @Override
    @XmlElement(name = "distributionFormat")
    public Collection<Format> getDistributionFormats() {
        return distributionFormats = nonNullCollection(distributionFormats, Format.class);
    }

    /**
     * Sets a description of the format of the data to be distributed.
     *
     * @param newValues The new distribution formats.
     */
    public void setDistributionFormats(final Collection<? extends Format> newValues) {
        distributionFormats = writeCollection(newValues, distributionFormats, Format.class);
    }

    /**
     * Provides information about the distributor.
     *
     * @return Information about the distributor.
     */
    @Override
    @XmlElement(name = "distributor")
    public Collection<Distributor> getDistributors() {
        return distributors = nonNullCollection(distributors, Distributor.class);
    }

    /**
     * Sets information about the distributor.
     *
     * @param newValues The new distributors.
     */
    public void setDistributors(final Collection<? extends Distributor> newValues) {
        distributors = writeCollection(newValues, distributors, Distributor.class);
    }

    /**
     * Provides information about technical means and media by which a resource is obtained from the distributor.
     *
     * @return Technical means and media by which a resource is obtained from the distributor.
     */
    @Override
    @XmlElement(name = "transferOptions")
    public Collection<DigitalTransferOptions> getTransferOptions() {
        return transferOptions = nonNullCollection(transferOptions, DigitalTransferOptions.class);
    }

    /**
     * Sets information about technical means and media by which a resource is obtained
     * from the distributor.
     *
     * @param newValues The new transfer options.
     */
    public void setTransferOptions(final Collection<? extends DigitalTransferOptions> newValues) {
        transferOptions = writeCollection(newValues, transferOptions, DigitalTransferOptions.class);
    }
}
