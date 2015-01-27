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
import org.opengis.metadata.quality.Completeness;
import org.opengis.metadata.quality.CompletenessOmission;
import org.opengis.metadata.quality.CompletenessCommission;


/**
 * Presence and absence of features, their attributes and their relationships.
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
@XmlType(name = "AbstractDQ_Completeness_Type")
@XmlRootElement(name = "DQ_Completeness")
@XmlSeeAlso({
    DefaultCompletenessCommission.class,
    DefaultCompletenessOmission.class
})
public class AbstractCompleteness extends AbstractElement implements Completeness {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8839542601508369777L;

    /**
     * Constructs an initially empty completeness.
     */
    public AbstractCompleteness() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Completeness)
     */
    public AbstractCompleteness(final Completeness object) {
        super(object);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link CompletenessCommission} or
     *       {@link CompletenessOmission}, then this method delegates to the {@code castOrCopy(…)}
     *       method of the corresponding SIS subclass. Note that if the given object implements
     *       more than one of the above-cited interfaces, then the {@code castOrCopy(…)} method
     *       to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractCompleteness}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractCompleteness} instance is created using the
     *       {@linkplain #AbstractCompleteness(Completeness) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractCompleteness castOrCopy(final Completeness object) {
        if (object instanceof CompletenessCommission) {
            return DefaultCompletenessCommission.castOrCopy((CompletenessCommission) object);
        }
        if (object instanceof CompletenessOmission) {
            return DefaultCompletenessOmission.castOrCopy((CompletenessOmission) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractCompleteness) {
            return (AbstractCompleteness) object;
        }
        return new AbstractCompleteness(object);
    }
}
