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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.Types;

// Specific to the main branch:
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Data quality descriptive result.
 * The following properties are mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_DescriptiveResult}
 * {@code   └─statement……………} textual expression of the descriptive result.</div>
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
 * @version 1.4
 * @since   1.3
 */
@XmlType(name = "DQ_DescriptiveResult_Type", propOrder = {
    "statement"
})
@XmlRootElement(name = "DQ_DescriptiveResult")
@UML(identifier="DQ_DescriptiveResult", specification=UNSPECIFIED)
public class DefaultDescriptiveResult extends AbstractResult {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5786649528259918304L;

    /**
     * Textual expression of the descriptive result.
     */
    @SuppressWarnings("serial")
    private InternationalString statement;

    /**
     * Constructs an initially empty descriptive result.
     */
    public DefaultDescriptiveResult() {
    }

    /**
     * Creates a conformance result initialized to the given values.
     *
     * @param text  statement against which data is being evaluated, or {@code null}.
     */
    public DefaultDescriptiveResult(final CharSequence text) {
        statement = Types.toInternationalString(text);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     */
    public DefaultDescriptiveResult(final DefaultDescriptiveResult object) {
        super(object);
        if (object != null) {
            statement = object.getStatement();
        }
    }

    /**
     * Returns the textual expression of the descriptive result.
     *
     * @return textual expression of the result.
     */
    @XmlElement(name = "statement", required = true)
    @UML(identifier="statement", obligation=MANDATORY, specification=UNSPECIFIED)
    public InternationalString getStatement() {
        return statement;
    }

    /**
     * Sets the textual expression of the descriptive result.
     *
     * @param  newValue  the new expression.
     */
    public void setStatement(final InternationalString newValue) {
        checkWritePermission(statement);
        statement = newValue;
    }
}
