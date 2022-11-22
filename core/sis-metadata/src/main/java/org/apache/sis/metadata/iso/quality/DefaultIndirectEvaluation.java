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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.IndirectEvaluation;
import org.apache.sis.util.iso.Types;


/**
 * Indirect evaluation.
 * See the {@link IndirectEvaluation} GeoAPI interface for more details.
 * The following properties are mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_IndirectEvaluation}
 * {@code   └─deductiveSource……………} Information on which data are used as sources in deductive evaluation method.</div>
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
 * @author  Alexis Gaillard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
@XmlType(name = "DQ_IndirectEvaluation_Type", propOrder = {
    "deductiveSource"
})
@XmlRootElement(name = "DQ_IndirectEvaluation")
public class DefaultIndirectEvaluation extends AbstractDataEvaluation implements IndirectEvaluation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5634950981839012526L;

    /**
     * Information on which data are used as sources in deductive evaluation method.
     */
    @SuppressWarnings("serial")
    private InternationalString deductiveSource;

    /**
     * Constructs an initially empty descriptive result.
     */
    public DefaultIndirectEvaluation() {
    }

    /**
     * Creates a conformance result initialized to the given values.
     *
     * @param  source  information on which data are used as sources, or {@code null}.
     */
    public DefaultIndirectEvaluation(final CharSequence source) {
        deductiveSource = Types.toInternationalString(source);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(IndirectEvaluation)
     */
    public DefaultIndirectEvaluation(final IndirectEvaluation object) {
        super(object);
        if (object != null) {
            deductiveSource = object.getDeductiveSource();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultIndirectEvaluation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultIndirectEvaluation} instance is created using the
     *       {@linkplain #DefaultIndirectEvaluation(IndirectEvaluation) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultIndirectEvaluation castOrCopy(final IndirectEvaluation object) {
        if (object == null || object instanceof DefaultIndirectEvaluation) {
            return (DefaultIndirectEvaluation) object;
        }
        return new DefaultIndirectEvaluation(object);
    }

    /**
     * Returns the information on which data are used as sources in deductive evaluation method.
     *
     * @return information on which data are used.
     */
    @Override
    @XmlElement(name = "deductiveSource", required = true)
    public InternationalString getDeductiveSource() {
        return deductiveSource;
    }

    /**
     * Sets the information on which data are used as sources in deductive evaluation method.
     *
     * @param  newValue  the new information.
     */
    public void setDeductiveSource(final InternationalString newValue) {
        checkWritePermission(deductiveSource);
        deductiveSource = newValue;
    }
}
