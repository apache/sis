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
import org.opengis.metadata.quality.DescriptiveResult;


/**
 * Data quality descriptive result.
 * See the {@link DescriptiveResult} GeoAPI interface for more details.
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
 * @version 1.3
 * @since   1.3
 * @module
 */
@XmlType(name = "DQ_DescriptiveResult_Type", propOrder = {
    "statement"
})
@XmlRootElement(name = "DQ_DescriptiveResult")
public class DefaultDescriptiveResult extends AbstractResult implements DescriptiveResult {
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
     * @param statement  statement against which data is being evaluated, or {@code null}.
     */
    public DefaultDescriptiveResult(final InternationalString statement) {
        this.statement = statement;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(DescriptiveResult)
     */
    public DefaultDescriptiveResult(final DescriptiveResult object) {
        super(object);
        if (object != null) {
            statement = object.getStatement();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultDescriptiveResult}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDescriptiveResult} instance is created using the
     *       {@linkplain #DefaultDescriptiveResult(DescriptiveResult) copy constructor} and returned.
     *       Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDescriptiveResult castOrCopy(final DescriptiveResult object) {
        if (object == null || object instanceof DefaultDescriptiveResult) {
            return (DefaultDescriptiveResult) object;
        }
        return new DefaultDescriptiveResult(object);
    }

    /**
     * Returns the textual expression of the descriptive result.
     *
     * @return textual expression of the result.
     */
    @Override
    @XmlElement(name = "statement", required = true)
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
