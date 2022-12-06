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

// Branch-dependent imports
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Reference to an external standalone quality report.
 * The following property is mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_Element}
 * {@code   ├─reportReference……} Reference to the procedure information.
 * {@code   └─abstract………………………} Description of the evaluation method.</div>
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
@XmlType(name = "DQ_StandaloneQualityReportInformation_Type", propOrder = {
    "reportReference",
    "abstract"
})
@XmlRootElement(name = "DQ_StandaloneQualityReportInformation")
@UML(identifier="DQ_StandaloneQualityReportInformation", specification=UNSPECIFIED)
public class DefaultEvaluationReportInformation extends ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6646482698986737797L;

    /**
     * Reference to the associated standalone quality report.
     */
    @SuppressWarnings("serial")
    private Citation reportReference;

    /**
     * Abstract for the associated standalone quality report.
     */
    @SuppressWarnings("serial")
    private InternationalString summary;

    /**
     * Constructs an initially empty standalone quality report information.
     */
    public DefaultEvaluationReportInformation() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     */
    public DefaultEvaluationReportInformation(final DefaultEvaluationReportInformation object) {
        super(object);
        if (object != null) {
            reportReference  = object.getReportReference();
            summary          = object.getAbstract();
        }
    }

    /**
     * Returns the reference to the associated standalone quality report.
     *
     * @return reference of the standalone quality report.
     */
    @XmlElement(name = "reportReference", required = true)
    @UML(identifier="reportReference", obligation=MANDATORY, specification=UNSPECIFIED)
    public Citation getReportReference() {
        return reportReference;
    }

    /**
     * Sets the reference to the associated standalone quality report.
     *
     * @param  newValue  the new reference.
     */
    public void setReportReference(final Citation newValue) {
        checkWritePermission(reportReference);
        reportReference = newValue;
    }

    /**
     * Returns the abstract for the standalone quality report.
     *
     * @return abstract of the standalone quality report.
     */
    @XmlElement(name = "abstract", required = true)
    @UML(identifier="abstract", obligation=MANDATORY, specification=UNSPECIFIED)
    public InternationalString getAbstract() {
        return summary;
    }

    /**
     * Sets the abstract for the associated standalone quality report.
     *
     * @param  newValue  the new abstract.
     */
    public void setAbstract(final InternationalString newValue)  {
        checkWritePermission(summary);
        summary = newValue;
    }
}
