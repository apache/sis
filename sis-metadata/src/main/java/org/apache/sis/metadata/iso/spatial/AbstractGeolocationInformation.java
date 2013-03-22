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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.spatial.GCPCollection;
import org.opengis.metadata.spatial.GeolocationInformation;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.Namespaces;


/**
 * Information used to determine geographic location corresponding to image location.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
@XmlType(name = "AbstractMI_GeolocationInformation_Type")
@XmlRootElement(name = "MI_GeolocationInformation", namespace = Namespaces.GMI)
@XmlSeeAlso(DefaultGCPCollection.class)
public class AbstractGeolocationInformation extends ISOMetadata implements GeolocationInformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2929163425440282342L;

    /**
     * Provides an overall assessment of quality of geolocation information.
     */
    private Collection<DataQuality> qualityInfo;

    /**
     * Constructs an initially empty geolocation information.
     */
    public AbstractGeolocationInformation() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * <p>This method checks for the {@link GCPCollection} sub-interface. If that interface is
     * found, then this method delegates to the corresponding {@code castOrCopy} static method.</p>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractGeolocationInformation castOrCopy(final GeolocationInformation object) {
        if (object instanceof GCPCollection) {
            return DefaultGCPCollection.castOrCopy((GCPCollection) object);
        }
        if (object == null || object instanceof AbstractGeolocationInformation) {
            return (AbstractGeolocationInformation) object;
        }
        final AbstractGeolocationInformation copy = new AbstractGeolocationInformation();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns an overall assessment of quality of geolocation information.
     */
    @Override
    @XmlElement(name = "qualityInfo", namespace = Namespaces.GMI)
    public synchronized Collection<DataQuality> getQualityInfo() {
        return qualityInfo = nonNullCollection(qualityInfo, DataQuality.class);
    }

    /**
     * Sets an overall assessment of quality of geolocation information.
     *
     * @param newValues The new quality information values.
     */
    public synchronized void setQualityInfo(Collection<? extends DataQuality> newValues) {
        qualityInfo = copyCollection(newValues, qualityInfo, DataQuality.class);
    }
}
