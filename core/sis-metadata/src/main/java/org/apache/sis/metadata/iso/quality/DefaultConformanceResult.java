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
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.ConformanceResult;
import org.apache.sis.util.iso.Types;


/**
 * Information about the outcome of evaluating the obtained value (or set of values) against
 * a specified acceptable conformance quality level.
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
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "DQ_ConformanceResult_Type", propOrder = {
    "specification",
    "explanation",
    "pass"
})
@XmlRootElement(name = "DQ_ConformanceResult")
public class DefaultConformanceResult extends AbstractResult implements ConformanceResult {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -2958690684356371311L;

    /**
     * Citation of product specification or user requirement against which data is being evaluated.
     */
    private Citation specification;

    /**
     * Explanation of the meaning of conformance for this result.
     */
    private InternationalString explanation;

    /**
     * Indication of the conformance result.
     *
     * <p>The field is directly annotated here, because the getter method is called {@link #pass()},
     * and JAXB does not recognize it. The method should have been called getPass() or isPass().</p>
     */
    @XmlElement(name = "pass", required = true)
    private Boolean pass;

    /**
     * Constructs an initially empty conformance result.
     */
    public DefaultConformanceResult() {
    }

    /**
     * Creates a conformance result initialized to the given values.
     *
     * @param specification Specification or requirement against which data is being evaluated, or {@code null}.
     * @param explanation The meaning of conformance for this result, or {@code null}.
     * @param pass Indication of the conformance result, or {@code null}.
     */
    public DefaultConformanceResult(final Citation specification,
                                    final CharSequence explanation,
                                    final boolean pass)
    {
        this.specification = specification;
        this.explanation = Types.toInternationalString(explanation);
        this.pass = pass;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ConformanceResult)
     */
    public DefaultConformanceResult(final ConformanceResult object) {
        super(object);
        if (object != null) {
            specification = object.getSpecification();
            explanation   = object.getExplanation();
            pass          = object.pass();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultConformanceResult}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultConformanceResult} instance is created using the
     *       {@linkplain #DefaultConformanceResult(ConformanceResult) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultConformanceResult castOrCopy(final ConformanceResult object) {
        if (object == null || object instanceof DefaultConformanceResult) {
            return (DefaultConformanceResult) object;
        }
        return new DefaultConformanceResult(object);
    }

    /**
     * Returns the citation of product specification or user requirement against which data is being evaluated.
     *
     * @return Citation of product specification or user requirement, or {@code null}.
     */
    @Override
    @XmlElement(name = "specification", required = true)
    public Citation getSpecification() {
        return specification;
    }

    /**
     * Sets the citation of product specification or user requirement against which data is being evaluated.
     *
     * @param newValue The new specification.
     */
    public void setSpecification(final Citation newValue) {
        checkWritePermission();
        specification = newValue;
    }

    /**
     * Returns the explanation of the meaning of conformance for this result.
     *
     * @return Explanation of the meaning of conformance, or {@code null}.
     */
    @Override
    @XmlElement(name = "explanation", required = true)
    public InternationalString getExplanation() {
        return explanation;
    }

    /**
     * Sets the explanation of the meaning of conformance for this result.
     *
     * @param newValue The new explanation.
     */
    public void setExplanation(final InternationalString newValue) {
        checkWritePermission();
        explanation = newValue;
    }

    /**
     * Returns an indication of the conformance result.
     *
     * @return Indication of the conformance result, or {@code null}.
     */
    @Override
    public Boolean pass() {
        return pass;
    }

    /**
     * Sets the indication of the conformance result.
     *
     * @param newValue {@code true} if the test pass.
     */
    public void setPass(final Boolean newValue) {
        checkWritePermission();
        pass = newValue;
    }
}
