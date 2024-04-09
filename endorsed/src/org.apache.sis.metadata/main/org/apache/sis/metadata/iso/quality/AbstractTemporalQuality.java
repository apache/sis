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
package org.apache.sis.metadata.iso.quality;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import org.opengis.metadata.quality.TemporalValidity;
import org.opengis.metadata.quality.TemporalConsistency;
import org.opengis.metadata.quality.AccuracyOfATimeMeasurement;
import org.opengis.metadata.quality.TemporalAccuracy;

// Specific to the main branch:
import org.opengis.annotation.UML;
import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Accuracy of the temporal attributes and temporal relationships of features.
 * The following property is mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_TemporalQuality}
 * {@code   └─result……………} Value obtained from applying a data quality measure.</div>
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
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Gaillard (Geomatys)
 * @version 1.4
 * @since   1.3
 */
@XmlType(name = "AbstractDQ_TemporalQuality_Type")
@XmlRootElement(name = "AbstractDQ_TemporalQuality")
@XmlSeeAlso({
    AbstractTemporalAccuracy.class,
    DefaultAccuracyOfATimeMeasurement.class,
    DefaultTemporalConsistency.class,
    DefaultTemporalValidity.class
})
@SuppressWarnings("deprecation")
@UML(identifier="DQ_TemporalQuality", specification=UNSPECIFIED)
public class AbstractTemporalQuality extends AbstractElement implements TemporalAccuracy {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -442029273458043017L;

    /**
     * Constructs an initially empty temporal accuracy.
     */
    public AbstractTemporalQuality() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(TemporalAccuracy)
     */
    public AbstractTemporalQuality(final TemporalAccuracy object) {
        super(object);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link AccuracyOfATimeMeasurement},
     *       {@link TemporalConsistency} or {@link TemporalValidity}, then this method delegates to
     *       the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractTemporalQuality}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractTemporalQuality} instance is created using the
     *       {@linkplain #AbstractTemporalQuality(TemporalAccuracy) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractTemporalQuality castOrCopy(final TemporalAccuracy object) {
        if (object instanceof AccuracyOfATimeMeasurement) {
            return DefaultAccuracyOfATimeMeasurement.castOrCopy((AccuracyOfATimeMeasurement) object);
        }
        if (object instanceof TemporalConsistency) {
            return DefaultTemporalConsistency.castOrCopy((TemporalConsistency) object);
        }
        if (object instanceof TemporalValidity) {
            return DefaultTemporalValidity.castOrCopy((TemporalValidity) object);
        }
        if (object == null || object instanceof AbstractTemporalQuality) {
            return (AbstractTemporalQuality) object;
        }
        return new AbstractTemporalQuality(object);
    }
}
