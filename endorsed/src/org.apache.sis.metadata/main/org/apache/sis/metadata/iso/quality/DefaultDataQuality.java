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

import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.metadata.quality.Scope;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.quality.StandaloneQualityReportInformation;


/**
 * Quality information for the data specified by a data quality scope.
 * The following properties are mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_DataQuality}
 * {@code   ├─scope………………} The specific data to which the data quality information applies.
 * {@code   │   └─level……} Hierarchical level of the data specified by the scope.
 * {@code   └─report……………} Quantitative quality information for the data specified by the scope.</div>
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
@XmlType(name = "DQ_DataQuality_Type", propOrder = {
    "scope",
    "reports",
    "standaloneQualityReport",
    "lineage"
})
@XmlRootElement(name = "DQ_DataQuality")
public class DefaultDataQuality extends ISOMetadata implements DataQuality {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5036527927404894540L;

    /**
     * The specific data to which the data quality information applies.
     */
    @SuppressWarnings("serial")
    private Scope scope;

    /**
     * Quality information for the data specified by the scope.
     */
    @SuppressWarnings("serial")
    private Collection<Element> reports;

    /**
     * Non-quantitative quality information about the lineage of the data specified by the scope.
     *
     * @deprecated Removed from ISO 19157:2013.
     */
    @Deprecated(since="1.3")
    @SuppressWarnings("serial")
    private Lineage lineage;

    /**
     * Reference to an external standalone quality report.
     * Can be used for providing more details than reported as standard metadata.
     */
    @SuppressWarnings("serial")
    private StandaloneQualityReportInformation standaloneQualityReport;

    /**
     * Constructs an initially empty data quality.
     */
    public DefaultDataQuality() {
    }

    /**
     * Creates a data quality initialized to the given scope level.
     * The scope level is, indirectly, a mandatory property in well-formed metadata.
     *
     * @param level  the hierarchical level of the data to which the quality information applies, or {@code null}.
     *
     * @since 0.5
     */
    public DefaultDataQuality(final ScopeCode level) {
        if (level != null) {
            scope = new DefaultScope(level);
        }
    }

    /**
     * Creates a data quality initialized to the given scope.
     *
     * @param scope  the specific data to which the data quality information applies, or {@code null}.
     */
    public DefaultDataQuality(final Scope scope) {
        this.scope = scope;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(DataQuality)
     */
    @SuppressWarnings("deprecation")
    public DefaultDataQuality(final DataQuality object) {
        super(object);
        if (object != null) {
            scope                   = object.getScope();
            reports                 = copyCollection(object.getReports(), Element.class);
            standaloneQualityReport = object.getStandaloneQualityReport();
            lineage                 = object.getLineage();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultDataQuality}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDataQuality} instance is created using the
     *       {@linkplain #DefaultDataQuality(DataQuality) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDataQuality castOrCopy(final DataQuality object) {
        if (object == null || object instanceof DefaultDataQuality) {
            return (DefaultDataQuality) object;
        }
        return new DefaultDataQuality(object);
    }

    /**
     * Returns the specific data to which the data quality information applies.
     *
     * @return the specific data to which the data quality information applies.
     */
    @Override
    @XmlElement(name = "scope", required = true)
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the specific data to which the data quality information applies.
     *
     * @param  newValue  the new scope.
     */
    public void setScope(final Scope newValue) {
        checkWritePermission(scope);
        scope = newValue;
    }

    /**
     * Returns the quality information for the data specified by the scope.
     *
     * @return quality information for the data specified by the scope.
     */
    @Override
    @XmlElement(name = "report", required = true)
    public Collection<Element> getReports() {
        return reports = nonNullCollection(reports, Element.class);
    }

    /**
     * Sets the quality information for the data specified by the scope.
     *
     * @param  newValues  the new reports.
     */
    public void setReports(final Collection<? extends Element> newValues) {
        reports = writeCollection(newValues, reports, Element.class);
    }

    /**
     * Returns the reference to an external standalone quality report.
     * Can be used for providing more details than reported as standard metadata.
     *
     * @return reference to an external standalone quality report, or {@code null} if none.
     *
     * @since 1.3
     */
    @Override
    @XmlElement(name = "standaloneQualityReport")
    public StandaloneQualityReportInformation getStandaloneQualityReport() {
        return standaloneQualityReport;
    }

    /**
     * Sets the quality of the reported information.
     *
     * @param  newValue  the new quality information.
     *
     * @since 1.3
     */
    public void setStandaloneQualityReport(final StandaloneQualityReportInformation newValue) {
        checkWritePermission(standaloneQualityReport);
        standaloneQualityReport = newValue;
    }

    /**
     * Returns non-quantitative quality information about the lineage of the data specified by the scope.
     *
     * @return non-quantitative quality information about the lineage of the data specified, or {@code null}.
     *
     * @deprecated Removed from ISO 19157:2013.
     */
    @Override
    @Deprecated(since="1.3")
    @XmlElement(name = "lineage", namespace = LegacyNamespaces.GMD)
    public Lineage getLineage() {
        return FilterByVersion.LEGACY_METADATA.accept() ? lineage : null;
    }

    /**
     * Sets the non-quantitative quality information about the lineage of the data specified by the scope.
     *
     * @param  newValue  the new lineage.
     *
     * @deprecated Removed from ISO 19157:2013.
     */
    @Deprecated(since="1.3")
    public void setLineage(final Lineage newValue) {
        checkWritePermission(lineage);
        lineage = newValue;
    }
}
