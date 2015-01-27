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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opengis.metadata.quality.TemporalAccuracy;
import org.opengis.metadata.quality.TemporalValidity;
import org.opengis.metadata.quality.TemporalConsistency;
import org.opengis.metadata.quality.AccuracyOfATimeMeasurement;


/**
 * Accuracy of the temporal attributes and temporal relationships of features.
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
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "AbstractDQ_TemporalAccuracy_Type")
@XmlRootElement(name = "DQ_TemporalAccuracy")
@XmlSeeAlso({
    DefaultAccuracyOfATimeMeasurement.class,
    DefaultTemporalConsistency.class,
    DefaultTemporalValidity.class
})
public class AbstractTemporalAccuracy extends AbstractElement implements TemporalAccuracy {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6273519060177989201L;

    /**
     * Constructs an initially empty temporal accuracy.
     */
    public AbstractTemporalAccuracy() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(TemporalAccuracy)
     */
    public AbstractTemporalAccuracy(final TemporalAccuracy object) {
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
     *       {@code AbstractTemporalAccuracy}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractTemporalAccuracy} instance is created using the
     *       {@linkplain #AbstractTemporalAccuracy(TemporalAccuracy) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractTemporalAccuracy castOrCopy(final TemporalAccuracy object) {
        if (object instanceof AccuracyOfATimeMeasurement) {
            return DefaultAccuracyOfATimeMeasurement.castOrCopy((AccuracyOfATimeMeasurement) object);
        }
        if (object instanceof TemporalConsistency) {
            return DefaultTemporalConsistency.castOrCopy((TemporalConsistency) object);
        }
        if (object instanceof TemporalValidity) {
            return DefaultTemporalValidity.castOrCopy((TemporalValidity) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractTemporalAccuracy) {
            return (AbstractTemporalAccuracy) object;
        }
        return new AbstractTemporalAccuracy(object);
    }
}
