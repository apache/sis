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
import org.opengis.temporal.PeriodDuration;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.Medium;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;

// Branch-specific imports
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Technical means and media by which a resource is obtained from the distributor.
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
    private static final long serialVersionUID = -2901375920581273330L;

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
    private Collection<Medium> offLines;

    /**
     * Rate of occurrence of distribution.
     */
    private PeriodDuration transferFrequency;

    /**
     * Formats of distribution.
     */
    private Collection<Format> distributionFormats;

    /**
     * Constructs an initially empty digital transfer options.
     */
    public DefaultDigitalTransferOptions() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * <div class="note"><b>Note on properties validation:</b>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     * </div>
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(DigitalTransferOptions)
     */
    public DefaultDigitalTransferOptions(final DigitalTransferOptions object) {
        super(object);
        if (object != null) {
            unitsOfDistribution = object.getUnitsOfDistribution();
            transferSize        = object.getTransferSize();
            onLines             = copyCollection(object.getOnLines(), OnlineResource.class);
            if (object instanceof DefaultDigitalTransferOptions) {
                final DefaultDigitalTransferOptions c = (DefaultDigitalTransferOptions) object;
                offLines            = copyCollection(c.getOffLines(), Medium.class);
                transferFrequency   = c.getTransferFrequency();
                distributionFormats = copyCollection(c.getDistributionFormats(), Format.class);
            } else {
                offLines = singleton(object.getOffLine(), Medium.class);
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
     *       {@code DefaultDigitalTransferOptions}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDigitalTransferOptions} instance is created using the
     *       {@linkplain #DefaultDigitalTransferOptions(DigitalTransferOptions) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDigitalTransferOptions castOrCopy(final DigitalTransferOptions object) {
        if (object == null || object instanceof DefaultDigitalTransferOptions) {
            return (DefaultDigitalTransferOptions) object;
        }
        return new DefaultDigitalTransferOptions(object);
    }

    /**
     * Returns tiles, layers, geographic areas, <i>etc.</i>, in which data is available.
     *
     * @return Tiles, layers, geographic areas, <cite>etc.</cite> in which data is available, or {@code null}.
     */
    @Override
    @XmlElement(name = "unitsOfDistribution")
    public InternationalString getUnitsOfDistribution() {
        return unitsOfDistribution;
    }

    /**
     * Sets tiles, layers, geographic areas, <i>etc.</i>, in which data is available.
     *
     * @param newValue The new units of distribution.
     */
    public void setUnitsOfDistribution(final InternationalString newValue) {
        checkWritePermission();
        unitsOfDistribution = newValue;
    }

    /**
     * Returns an estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer size is greater than zero.
     *
     * @return Estimated size of a unit in the specified transfer format in megabytes, or {@code null}.
     */
    @Override
    @XmlElement(name = "transferSize")
    @ValueRange(minimum = 0, isMinIncluded = false)
    public Double getTransferSize() {
        return transferSize;
    }

    /**
     * Sets an estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer shall be greater than zero.
     *
     * @param newValue The new transfer size, or {@code null}.
     * @throws IllegalArgumentException if the given value is NaN or negative.
     */
    public void setTransferSize(final Double newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultDigitalTransferOptions.class, "transferSize", true, newValue)) {
            transferSize = newValue;
        }
    }

    /**
     * Returns information about online sources from which the resource can be obtained.
     *
     * @return Online sources from which the resource can be obtained.
     */
    @Override
    @XmlElement(name = "onLine")
    public Collection<OnlineResource> getOnLines() {
        return onLines = nonNullCollection(onLines, OnlineResource.class);
    }

    /**
     * Sets information about online sources from which the resource can be obtained.
     *
     * @param newValues The new online sources.
     */
    public void setOnLines(final Collection<? extends OnlineResource> newValues) {
        onLines = writeCollection(newValues, onLines, OnlineResource.class);
    }

    /**
     * Returns information about offline media on which the resource can be obtained.
     *
     * @return Offline media on which the resource can be obtained.
     *
     * @since 0.5
     */
    @UML(identifier="offLine", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Medium> getOffLines() {
        return offLines = nonNullCollection(offLines, Medium.class);
    }

    /**
     * Sets information about offline media on which the resource can be obtained.
     *
     * @param newValues The new offline media.
     *
     * @since 0.5
     */
    public void setOffLines(final Collection<? extends Medium> newValues) {
        offLines = writeCollection(newValues, offLines, Medium.class);
    }

    /**
     * Returns information about offline media on which the resource can be obtained.
     *
     * @return Offline media on which the resource can be obtained, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getOffLines()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "offLine")
    public Medium getOffLine() {
        return LegacyPropertyAdapter.getSingleton(getOffLines(), Medium.class, null, DefaultDigitalTransferOptions.class, "getOffLine");
    }

    /**
     * Sets information about offline media on which the resource can be obtained.
     *
     * @param newValue The new offline media.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setOffLines(Collection)}.
     */
    @Deprecated
    public void setOffLine(final Medium newValue) {
        setOffLines(LegacyPropertyAdapter.asCollection(newValue));
    }

    /**
     * Returns the rate of occurrence of distribution.
     *
     * @return Rate of occurrence of distribution, or {@code null} if none.
     *
     * @since 0.5
     */
    @UML(identifier="transferFrequency", obligation=OPTIONAL, specification=ISO_19115)
    public PeriodDuration getTransferFrequency() {
        return transferFrequency;
    }

    /**
     * Sets the rate of occurrence of distribution.
     *
     * @param newValue The new rate of occurrence of distribution.
     *
     * @since 0.5
     */
    public void setTransferFrequency(final PeriodDuration newValue) {
        checkWritePermission();
        transferFrequency = newValue;
    }

    /**
     * Returns the formats of distribution.
     *
     * @return Formats of distribution.
     *
     * @since 0.5
     */
    @UML(identifier="distributionFormat", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Format> getDistributionFormats() {
        return distributionFormats = nonNullCollection(distributionFormats, Format.class);
    }

    /**
     * Sets the formats of distribution.
     *
     * @param newValues The new formats of distribution.
     *
     * @since 0.5
     */
    public void setDistributionFormats(final Collection<? extends Format> newValues) {
        distributionFormats = writeCollection(newValues, distributionFormats, Format.class);
    }
}
