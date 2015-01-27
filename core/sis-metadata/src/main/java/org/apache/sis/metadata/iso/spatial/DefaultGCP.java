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
import javax.xml.bind.annotation.XmlType;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.spatial.GCP;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.Namespaces;


/**
 * Information on ground control point.
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
@XmlType(name = "MI_GCP_Type", propOrder = {
    //"geographicCoordinates",
    "accuracyReports"
})
@XmlRootElement(name = "MI_GCP", namespace = Namespaces.GMI)
public class DefaultGCP extends ISOMetadata implements GCP {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2021981491910341192L;

    /**
     * Geographic or map position of the control point, in either two or three dimensions.
     */
    private DirectPosition geographicCoordinates;

    /**
     * Accuracy of a ground control point.
     */
    private Collection<Element> accuracyReports;

    /**
     * Constructs an initially empty ground control point.
     */
    public DefaultGCP() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(GCP)
     */
    public DefaultGCP(final GCP object) {
        super(object);
        if (object != null) {
            accuracyReports       = copyCollection(object.getAccuracyReports(), Element.class);
            geographicCoordinates = object.getGeographicCoordinates();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultGCP}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultGCP} instance is created using the
     *       {@linkplain #DefaultGCP(GCP) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGCP castOrCopy(final GCP object) {
        if (object == null || object instanceof DefaultGCP) {
            return (DefaultGCP) object;
        }
        return new DefaultGCP(object);
    }

    /**
     * Returns the geographic or map position of the control point, in either two or three dimensions.
     *
     * @return Geographic or map position of the control point, or {@code null}.
     *
     * @todo finish the annotation on the referencing module before.
     */
    @Override
    //@XmlElement(name = "geographicCoordinates", required = true)
    public DirectPosition getGeographicCoordinates() {
        return geographicCoordinates;
    }

    /**
     * Sets the geographic or map position of the control point, in either two or three dimensions.
     *
     * @param newValue The new geographic coordinates values.
     */
    public void setGeographicCoordinates(final DirectPosition newValue) {
        checkWritePermission();
        geographicCoordinates = newValue;
    }

    /**
     * Returns the accuracy of a ground control point.
     *
     * @return Accuracy of a ground control point.
     */
    @Override
    @XmlElement(name = "accuracyReport", namespace = Namespaces.GMI)
    public Collection<Element> getAccuracyReports() {
        return accuracyReports = nonNullCollection(accuracyReports, Element.class);
    }

    /**
     * Sets the accuracy of a ground control point.
     *
     * @param newValues The new accuracy report values.
     */
    public void setAccuracyReports(final Collection<? extends Element> newValues) {
        accuracyReports = writeCollection(newValues, accuracyReports, Element.class);
    }
}
