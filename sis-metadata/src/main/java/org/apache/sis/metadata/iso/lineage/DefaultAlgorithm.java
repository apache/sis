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
package org.apache.sis.metadata.iso.lineage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.lineage.Algorithm;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.Namespaces;


/**
 * Details of the methodology by which geographic information was derived from the instrument readings.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
@XmlType(name = "LE_Algorithm_Type", propOrder = {
    "citation",
    "description"
})
@XmlRootElement(name = "LE_Algorithm", namespace = Namespaces.GMI)
public class DefaultAlgorithm extends ISOMetadata implements Algorithm {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6343760610092069341L;

    /**
     * Information identifying the algorithm and version or date.
     */
    private Citation citation;

    /**
     * Information describing the algorithm used to generate the data.
     */
    private InternationalString description;

    /**
     * Constructs an initially empty algorithm.
     */
    public DefaultAlgorithm() {
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
    public static DefaultAlgorithm castOrCopy(final Algorithm object) {
        if (object == null || object instanceof DefaultAlgorithm) {
            return (DefaultAlgorithm) object;
        }
        final DefaultAlgorithm copy = new DefaultAlgorithm();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the information identifying the algorithm and version or date.
     */
    @Override
    @XmlElement(name = "citation", namespace = Namespaces.GMI, required = true)
    public synchronized Citation getCitation() {
        return citation;
    }

    /**
     * Sets the information identifying the algorithm and version or date.
     *
     * @param newValue The new citation value.
     */
    public synchronized void setCitation(final Citation newValue) {
        checkWritePermission();
        citation = newValue;
    }

    /**
     * Returns the information describing the algorithm used to generate the data.
     */
    @Override
    @XmlElement(name = "description", namespace = Namespaces.GMI, required = true)
    public synchronized InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the information describing the algorithm used to generate the data.
     *
     * @param newValue The new description value.
     */
    public synchronized void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }
}
