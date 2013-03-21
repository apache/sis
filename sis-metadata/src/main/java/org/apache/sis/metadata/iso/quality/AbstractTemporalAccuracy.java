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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @since   0.3 (derived from geotk-2.1)
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
    private static final long serialVersionUID = 4525353962603986621L;

    /**
     * Constructs an initially empty temporal accuracy.
     */
    public AbstractTemporalAccuracy() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * <p>This method checks for the {@link AccuracyOfATimeMeasurement}, {@link TemporalConsistency}
     * and {@link TemporalValidity} sub-interfaces. If one of those interfaces is found, then this
     * method delegates to the corresponding {@code castOrCopy} static method. If the given object
     * implements more than one of the above-cited interfaces, then the {@code castOrCopy} method to
     * be used is unspecified.</p>
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
        if (object == null || object instanceof AbstractTemporalAccuracy) {
            return (AbstractTemporalAccuracy) object;
        }
        final AbstractTemporalAccuracy copy = new AbstractTemporalAccuracy();
        copy.shallowCopy(object);
        return copy;
    }
}
