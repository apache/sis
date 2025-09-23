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
import org.opengis.metadata.quality.NonQuantitativeAttributeAccuracy;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;


/**
 * Accuracy of non-quantitative attributes.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.3
 *
 * @deprecated Renamed {@link DefaultNonQuantitativeAttributeCorrectness} for following a renaming in ISO 19157:2013.
 */
@Deprecated(since="1.3")
@XmlType(name = "DQ_NonQuantitativeAttributeAccuracy_Type", namespace = LegacyNamespaces.GMD)
@XmlRootElement(name = "DQ_NonQuantitativeAttributeAccuracy", namespace = LegacyNamespaces.GMD)
public class DefaultNonQuantitativeAttributeAccuracy extends DefaultNonQuantitativeAttributeCorrectness
        implements NonQuantitativeAttributeAccuracy
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8777909940058192886L;

    /**
     * Constructs an initially empty non quantitative attribute accuracy.
     */
    public DefaultNonQuantitativeAttributeAccuracy() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(NonQuantitativeAttributeAccuracy)
     */
    public DefaultNonQuantitativeAttributeAccuracy(final NonQuantitativeAttributeAccuracy object) {
        super(object);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultNonQuantitativeAttributeAccuracy}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultNonQuantitativeAttributeAccuracy} instance is created using the
     *       {@linkplain #DefaultNonQuantitativeAttributeAccuracy(NonQuantitativeAttributeAccuracy) copy constructor}
     *       and returned. Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultNonQuantitativeAttributeAccuracy castOrCopy(final NonQuantitativeAttributeAccuracy object) {
        if (object == null || object instanceof DefaultNonQuantitativeAttributeAccuracy) {
            return (DefaultNonQuantitativeAttributeAccuracy) object;
        }
        return new DefaultNonQuantitativeAttributeAccuracy(object);
    }
}
