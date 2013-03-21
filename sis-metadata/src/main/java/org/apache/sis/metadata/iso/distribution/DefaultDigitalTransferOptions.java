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
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Medium;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.gco.GO_Real;


/**
 * Technical means and media by which a resource is obtained from the distributor.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_DigitalTransferOptions_Type", propOrder = {
    "unitsOfDistribution",
    "transferSize",
    "onLines",
    "offLine"
})
@XmlRootElement(name = "MD_DigitalTransferOptions")
public class DefaultDigitalTransferOptions extends ISOMetadata implements DigitalTransferOptions {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1533064478468754337L;

    /**
     * Tiles, layers, geographic areas, etc., in which data is available.
     */
    private InternationalString unitsOfDistribution;

    /**
     * Estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer size shall be greater than 0.
     */
    private Double transferSize;

    /**
     * Information about online sources from which the resource can be obtained.
     */
    private Collection<OnlineResource> onLines;

    /**
     * Information about offline media on which the resource can be obtained.
     */
    private Medium offLines;

    /**
     * Constructs an initially empty digital transfer options.
     */
    public DefaultDigitalTransferOptions() {
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
    public static DefaultDigitalTransferOptions castOrCopy(final DigitalTransferOptions object) {
        if (object == null || object instanceof DefaultDigitalTransferOptions) {
            return (DefaultDigitalTransferOptions) object;
        }
        final DefaultDigitalTransferOptions copy = new DefaultDigitalTransferOptions();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns tiles, layers, geographic areas, <i>etc.</i>, in which data is available.
     */
    @Override
    @XmlElement(name = "unitsOfDistribution")
    public synchronized InternationalString getUnitsOfDistribution() {
        return unitsOfDistribution;
    }

    /**
     * Sets tiles, layers, geographic areas, <i>etc.</i>, in which data is available.
     *
     * @param newValue The new units of distribution.
     */
    public synchronized void setUnitsOfDistribution(final InternationalString newValue) {
        checkWritePermission();
        unitsOfDistribution = newValue;
    }

    /**
     * Returns an estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer size is greater than zero.
     */
    @Override
    @XmlElement(name = "transferSize")
    @XmlJavaTypeAdapter(GO_Real.class)
    @ValueRange(minimum=0, isMinIncluded=false)
    public synchronized Double getTransferSize() {
        return transferSize;
    }

    /**
     * Sets an estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer shall be greater than zero.
     *
     * @param newValue The new transfer size.
     */
    public synchronized void setTransferSize(final Double newValue) {
        checkWritePermission();
        transferSize = newValue;
    }

    /**
     * Returns information about online sources from which the resource can be obtained.
     */
    @Override
    @XmlElement(name = "onLine")
    public synchronized Collection<OnlineResource> getOnLines() {
        return onLines = nonNullCollection(onLines, OnlineResource.class);
    }

    /**
     * Sets information about online sources from which the resource can be obtained.
     *
     * @param newValues The new online sources.
     */
    public synchronized void setOnLines(final Collection<? extends OnlineResource> newValues) {
        onLines = copyCollection(newValues, onLines, OnlineResource.class);
    }

    /**
     * Returns information about offline media on which the resource can be obtained.
     */
    @Override
    @XmlElement(name = "offLine")
    public synchronized Medium getOffLine() {
        return offLines;
    }

    /**
     * Sets information about offline media on which the resource can be obtained.
     *
     * @param newValue The new offline media.
     */
    public synchronized void setOffLine(final Medium newValue) {
        checkWritePermission();
        offLines = newValue;
    }
}
