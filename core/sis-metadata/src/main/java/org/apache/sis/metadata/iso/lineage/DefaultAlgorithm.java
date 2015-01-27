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
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
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
    private static final long serialVersionUID = 5718445163047946957L;

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
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Algorithm)
     */
    public DefaultAlgorithm(final Algorithm object) {
        super(object);
        if (object != null) {
            citation    = object.getCitation();
            description = object.getDescription();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultAlgorithm}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultAlgorithm} instance is created using the
     *       {@linkplain #DefaultAlgorithm(Algorithm) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAlgorithm castOrCopy(final Algorithm object) {
        if (object == null || object instanceof DefaultAlgorithm) {
            return (DefaultAlgorithm) object;
        }
        return new DefaultAlgorithm(object);
    }

    /**
     * Returns the information identifying the algorithm and version or date.
     *
     * @return Algorithm and version or date, or {@code null}.
     */
    @Override
    @XmlElement(name = "citation", namespace = Namespaces.GMI, required = true)
    public Citation getCitation() {
        return citation;
    }

    /**
     * Sets the information identifying the algorithm and version or date.
     *
     * @param newValue The new citation value.
     */
    public void setCitation(final Citation newValue) {
        checkWritePermission();
        citation = newValue;
    }

    /**
     * Returns the information describing the algorithm used to generate the data.
     *
     * @return Algorithm used to generate the data, or {@code null}.
     */
    @Override
    @XmlElement(name = "description", namespace = Namespaces.GMI, required = true)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the information describing the algorithm used to generate the data.
     *
     * @param newValue The new description value.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }
}
