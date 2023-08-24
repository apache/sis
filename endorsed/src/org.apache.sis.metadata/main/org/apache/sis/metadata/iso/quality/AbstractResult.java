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

import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.CoverageResult;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.QuantitativeResult;
import org.apache.sis.xml.bind.metadata.MD_Scope;
import org.apache.sis.xml.bind.gco.GO_Temporal;

// Branch-dependent imports
import org.opengis.metadata.quality.Scope;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Base class of more specific result classes.
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
 * @author  Alexis Gaillard (Geomatys)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "AbstractDQ_Result_Type", propOrder = {
    "resultScope",
    "dateTime"
})
@XmlRootElement(name = "AbstractDQ_Result")
@XmlSeeAlso({
    DefaultConformanceResult.class,
    DefaultQuantitativeResult.class,
    DefaultDescriptiveResult.class,
    DefaultCoverageResult.class
})
public class AbstractResult extends ISOMetadata implements Result {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3510023908820052467L;

    /**
     * Scope of the result.
     */
    @SuppressWarnings("serial")
    private Scope resultScope;

    /**
     * Date when the result was generated, or {@code null} if none.
     */
    @SuppressWarnings("serial")
    private Temporal dateTime;

    /**
     * Constructs an initially empty result.
     */
    public AbstractResult() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Result)
     */
    public AbstractResult(final Result object) {
        super(object);
        if (object instanceof AbstractResult) {
            final AbstractResult impl = (AbstractResult) object;
            resultScope = impl.getResultScope();
            dateTime    = impl.getDateTime();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link ConformanceResult},
     *       {@link QuantitativeResult} or {@link CoverageResult},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractResult}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractResult} instance is created using the
     *       {@linkplain #AbstractResult(Result) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractResult castOrCopy(final Result object) {
        if (object instanceof QuantitativeResult) {
            return DefaultQuantitativeResult.castOrCopy((QuantitativeResult) object);
        }
        if (object instanceof ConformanceResult) {
            return DefaultConformanceResult.castOrCopy((ConformanceResult) object);
        }
        if (object instanceof CoverageResult) {
            return DefaultCoverageResult.castOrCopy((CoverageResult) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractResult) {
            return (AbstractResult) object;
        }
        return new AbstractResult(object);
    }

    /**
     * Returns the scope of the result.
     *
     * @return scope of the result, or {@code null} if unspecified.
     *
     * @since 1.3
     */
    @XmlElement(name = "resultScope")
    @XmlJavaTypeAdapter(MD_Scope.Since2014.class)
    @UML(identifier="resultScope", obligation=OPTIONAL, specification=UNSPECIFIED)
    public Scope getResultScope() {
        return resultScope;
    }

    /**
     * Sets the scope of the result.
     *
     * @param  newValue  the new evaluation procedure.
     *
     * @since 1.3
     */
    public void setResultScope(final Scope newValue) {
        resultScope = newValue;
    }

    /**
     * Returns the date when the result was generated.
     * This is typically a {@link java.time.LocalDate}, {@link java.time.LocalDateTime}
     * or {@link java.time.ZonedDateTime} depending on whether the hour of the day and
     * the time zone are provided.
     *
     * @return date of the result, or {@code null} if none.
     *
     * @since 1.3
     */
    @XmlElement(name = "dateTime")
    @XmlJavaTypeAdapter(GO_Temporal.Since2014.class)
    @UML(identifier="dateTime", obligation=OPTIONAL, specification=UNSPECIFIED)
    public Temporal getDateTime() {
        return dateTime;
    }

    /**
     * Sets the date when the result was generated.
     *
     * @param  newValue  the new date, or {@code null}.
     *
     * @since 1.3
     */
    public void setDateTime(final Temporal newValue) {
        dateTime = newValue;
    }
}
