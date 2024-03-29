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
package org.apache.sis.metadata.iso.spatial;

import java.util.Collection;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.spatial.GCPCollection;
import org.opengis.metadata.spatial.GeolocationInformation;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information used to determine geographic location corresponding to image location.
 *
 * <h2>Limitations</h2>
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
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "AbstractMI_GeolocationInformation_Type")
@XmlRootElement(name = "AbstractMI_GeolocationInformation")
@XmlSeeAlso(DefaultGCPCollection.class)
public class AbstractGeolocationInformation extends ISOMetadata implements GeolocationInformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2234791083092464542L;

    /**
     * Provides an overall assessment of quality of geolocation information.
     */
    @SuppressWarnings("serial")
    private Collection<DataQuality> qualityInfo;

    /**
     * Constructs an initially empty geolocation information.
     */
    public AbstractGeolocationInformation() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(GeolocationInformation)
     */
    public AbstractGeolocationInformation(final GeolocationInformation object) {
        super(object);
        if (object != null) {
            qualityInfo = copyCollection(object.getQualityInfo(), DataQuality.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link GCPCollection}, then this method
     *       delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractGeolocationInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractGeolocationInformation} instance is created using the
     *       {@linkplain #AbstractGeolocationInformation(GeolocationInformation) copy constructor}
     *       and returned. Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractGeolocationInformation castOrCopy(final GeolocationInformation object) {
        if (object instanceof GCPCollection) {
            return DefaultGCPCollection.castOrCopy((GCPCollection) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractGeolocationInformation) {
            return (AbstractGeolocationInformation) object;
        }
        return new AbstractGeolocationInformation(object);
    }

    /**
     * Returns an overall assessment of quality of geolocation information.
     *
     * @return an overall assessment of quality of geolocation information.
     */
    @Override
    @XmlElement(name = "qualityInfo")
    public Collection<DataQuality> getQualityInfo() {
        return qualityInfo = nonNullCollection(qualityInfo, DataQuality.class);
    }

    /**
     * Sets an overall assessment of quality of geolocation information.
     *
     * @param  newValues  the new quality information values.
     */
    public void setQualityInfo(Collection<? extends DataQuality> newValues) {
        qualityInfo = writeCollection(newValues, qualityInfo, DataQuality.class);
    }
}
