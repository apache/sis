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
import org.opengis.metadata.quality.StandaloneQualityReportInformation;


/**
 * Reference to an external standalone quality report.
 * See the {@link StandaloneQualityReportInformation} GeoAPI interface for more details.
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
public class DefaultStandaloneQualityReportInformation extends ISOMetadata implements StandaloneQualityReportInformation {
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
    public DefaultStandaloneQualityReportInformation() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <dfn>shallow</dfn> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(StandaloneQualityReportInformation)
     */
    public DefaultStandaloneQualityReportInformation(final StandaloneQualityReportInformation object) {
        super(object);
        if (object != null) {
            reportReference  = object.getReportReference();
            summary          = object.getAbstract();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultStandaloneQualityReportInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultStandaloneQualityReportInformation} instance is created using the
     *       {@linkplain #DefaultStandaloneQualityReportInformation(StandaloneQualityReportInformation) copy constructor}
     *       and returned. Note that this is a <dfn>shallow</dfn> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultStandaloneQualityReportInformation castOrCopy(final StandaloneQualityReportInformation object) {
        if (object instanceof StandaloneQualityReportInformation) {
            return DefaultStandaloneQualityReportInformation.castOrCopy((DefaultStandaloneQualityReportInformation) object);
        }
        return new DefaultStandaloneQualityReportInformation(object);
    }

    /**
     * Returns the reference to the associated standalone quality report.
     *
     * @return reference of the standalone quality report.
     */
    @Override
    @XmlElement(name = "reportReference", required = true)
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
    @Override
    @XmlElement(name = "abstract", required = true)
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
