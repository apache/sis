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
import org.opengis.metadata.quality.CompletenessOmission;


/**
 * Data absent from the dataset, as described by the scope.
 * See the {@link CompletenessOmission} GeoAPI interface for more details.
 * The following property is mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_CompletenessOmission}
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "DQ_CompletenessOmission_Type")
@XmlRootElement(name = "DQ_CompletenessOmission")
public class DefaultCompletenessOmission extends AbstractCompleteness implements CompletenessOmission {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6782225824097039360L;

    /**
     * Constructs an initially empty completeness omission.
     */
    public DefaultCompletenessOmission() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(CompletenessOmission)
     */
    public DefaultCompletenessOmission(final CompletenessOmission object) {
        super(object);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultCompletenessOmission}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultCompletenessOmission} instance is created using the
     *       {@linkplain #DefaultCompletenessOmission(CompletenessOmission) copy constructor}
     *       and returned. Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCompletenessOmission castOrCopy(final CompletenessOmission object) {
        if (object == null || object instanceof DefaultCompletenessOmission) {
            return (DefaultCompletenessOmission) object;
        }
        return new DefaultCompletenessOmission(object);
    }
}
