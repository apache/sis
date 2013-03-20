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
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information about the distributor of and options for obtaining the resource.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
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
    private static final long serialVersionUID = -5899590027802365131L;

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
    public static DefaultDistribution castOrCopy(final Distribution object) {
        if (object == null || object instanceof DefaultDistribution) {
            return (DefaultDistribution) object;
        }
        final DefaultDistribution copy = new DefaultDistribution();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Provides a description of the format of the data to be distributed.
     */
    @Override
    @XmlElement(name = "distributionFormat")
    public synchronized Collection<Format> getDistributionFormats() {
        return distributionFormats = nonNullCollection(distributionFormats, Format.class);
    }

    /**
     * Sets a description of the format of the data to be distributed.
     *
     * @param newValues The new distribution formats.
     */
    public synchronized void setDistributionFormats(final Collection<? extends Format> newValues) {
        distributionFormats = copyCollection(newValues, distributionFormats, Format.class);
    }

    /**
     * Provides information about the distributor.
     */
    @Override
    @XmlElement(name = "distributor")
    public synchronized Collection<Distributor> getDistributors() {
        return distributors = nonNullCollection(distributors, Distributor.class);
    }

    /**
     * Sets information about the distributor.
     *
     * @param newValues The new distributors.
     */
    public synchronized void setDistributors(final Collection<? extends Distributor> newValues) {
        distributors = copyCollection(newValues, distributors, Distributor.class);
    }

    /**
     * Provides information about technical means and media by which a resource is obtained
     * from the distributor.
     */
    @Override
    @XmlElement(name = "transferOptions")
    public synchronized Collection<DigitalTransferOptions> getTransferOptions() {
        return transferOptions = nonNullCollection(transferOptions, DigitalTransferOptions.class);
    }

    /**
     * Sets information about technical means and media by which a resource is obtained
     * from the distributor.
     *
     * @param newValues The new transfer options.
     */
    public synchronized void setTransferOptions(final Collection<? extends DigitalTransferOptions> newValues) {
        transferOptions = copyCollection(newValues, transferOptions, DigitalTransferOptions.class);
    }
}
