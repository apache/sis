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
import org.opengis.metadata.quality.RelativeInternalPositionalAccuracy;


/**
 * Closeness of the relative positions of features in the scope to their respective
 * relative positions accepted as or being true.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code DQ_RelativeInternalPositionalAccuracy}
 * {@code   └─result……………} Value obtained from applying a data quality measure.</div>
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
 * @version 0.3
 * @since   0.3
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@XmlType(name = "DQ_RelativeInternalPositionalAccuracy_Type")
@XmlRootElement(name = "DQ_RelativeInternalPositionalAccuracy")
public class DefaultRelativeInternalPositionalAccuracy extends AbstractPositionalAccuracy
        implements RelativeInternalPositionalAccuracy
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8385667875833802576L;

    /**
     * Constructs an initially empty relative internal positional accuracy.
     */
    public DefaultRelativeInternalPositionalAccuracy() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(RelativeInternalPositionalAccuracy)
     */
    public DefaultRelativeInternalPositionalAccuracy(final RelativeInternalPositionalAccuracy object) {
        super(object);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultRelativeInternalPositionalAccuracy}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRelativeInternalPositionalAccuracy} instance is created using the
     *       {@linkplain #DefaultRelativeInternalPositionalAccuracy(RelativeInternalPositionalAccuracy) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultRelativeInternalPositionalAccuracy castOrCopy(final RelativeInternalPositionalAccuracy object) {
        if (object == null || object instanceof DefaultRelativeInternalPositionalAccuracy) {
            return (DefaultRelativeInternalPositionalAccuracy) object;
        }
        return new DefaultRelativeInternalPositionalAccuracy(object);
    }
}
