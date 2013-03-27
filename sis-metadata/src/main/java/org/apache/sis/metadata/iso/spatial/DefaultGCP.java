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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
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
    private static final long serialVersionUID = -5517470507848931237L;

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
    public static DefaultGCP castOrCopy(final GCP object) {
        if (object == null || object instanceof DefaultGCP) {
            return (DefaultGCP) object;
        }
        final DefaultGCP copy = new DefaultGCP();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the geographic or map position of the control point, in either two or three dimensions.
     *
     * @todo finish the annotation on the referencing module before.
     */
    @Override
    //@XmlElement(name = "geographicCoordinates")
    public synchronized DirectPosition getGeographicCoordinates() {
        return geographicCoordinates;
    }

    /**
     * Sets the geographic or map position of the control point, in either two or three dimensions.
     *
     * @param newValue The new geographic coordinates values.
     */
    public synchronized void setGeographicCoordinates(final DirectPosition newValue) {
        checkWritePermission();
        geographicCoordinates = newValue;
    }

    /**
     * Get the accuracy of a ground control point.
     */
    @Override
    @XmlElement(name = "accuracyReport", namespace = Namespaces.GMI)
    public synchronized Collection<Element> getAccuracyReports() {
        return accuracyReports = nonNullCollection(accuracyReports, Element.class);
    }

    /**
     * Sets the accuracy of a ground control point.
     *
     * @param newValues The new accuracy report values.
     */
    public synchronized void setAccuracyReports(final Collection<? extends Element> newValues) {
        accuracyReports = writeCollection(newValues, accuracyReports, Element.class);
    }
}
