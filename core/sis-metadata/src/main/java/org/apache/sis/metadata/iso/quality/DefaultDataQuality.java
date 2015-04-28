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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Scope;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Quality information for the data specified by a data quality scope.
 *
 * <div class="section">Relationship between properties</div>
 * According ISO 19115, at least one of {@linkplain #getLineage() lineage} and
 * {@linkplain #getReports() reports} shall be provided.
 *
 * <div class="section">Limitations</div>
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
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "DQ_DataQuality_Type", propOrder = {
    "scope",
    "reports",
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
    private Scope scope;

    /**
     * Quantitative quality information for the data specified by the scope.
     * Should be provided only if {@linkplain Scope#getLevel scope level} is
     * {@linkplain org.opengis.metadata.maintenance.ScopeCode#DATASET dataset}.
     */
    private Collection<Element> reports;

    /**
     * Non-quantitative quality information about the lineage of the data specified by the scope.
     * Should be provided only if {@linkplain Scope#getLevel scope level} is
     * {@linkplain org.opengis.metadata.maintenance.ScopeCode#DATASET dataset}.
     */
    private Lineage lineage;

    /**
     * Constructs an initially empty data quality.
     */
    public DefaultDataQuality() {
    }

    /**
     * Creates a data quality initialized to the given scope.
     *
     * @param scope The specific data to which the data quality information applies, or {@code null}.
     */
    public DefaultDataQuality(final Scope scope) {
        this.scope = scope;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(DataQuality)
     */
    public DefaultDataQuality(final DataQuality object) {
        super(object);
        if (object != null) {
            scope   = object.getScope();
            reports = copyCollection(object.getReports(), Element.class);
            lineage = object.getLineage();
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
     *       {@linkplain #DefaultDataQuality(DataQuality) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return The specific data to which the data quality information applies, or {@code null}.
     */
    @Override
    @XmlElement(name = "scope", required = true)
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the specific data to which the data quality information applies.
     *
     * @param newValue The new scope.
     */
    public void setScope(final Scope newValue) {
        checkWritePermission();
        scope = newValue;
    }

    /**
     * Returns the quantitative quality information for the data specified by the scope.
     *
     * @return Quantitative quality information for the data.
     */
    @Override
    @XmlElement(name = "report")
    public Collection<Element> getReports() {
        return reports = nonNullCollection(reports, Element.class);
    }

    /**
     * Sets the quantitative quality information for the data specified by the scope.
     *
     * @param newValues The new reports.
     */
    public void setReports(final Collection<? extends Element> newValues) {
        reports = writeCollection(newValues, reports, Element.class);
    }

    /**
     * Returns non-quantitative quality information about the lineage of the data specified by the scope.
     *
     * @return Non-quantitative quality information about the lineage of the data specified, or {@code null}.
     */
    @Override
    @XmlElement(name = "lineage")
    public Lineage getLineage() {
        return lineage;
    }

    /**
     * Sets the non-quantitative quality information about the lineage of the data specified by the scope.
     *
     * @param newValue The new lineage.
     */
    public void setLineage(final Lineage newValue) {
        checkWritePermission();
        lineage = newValue;
    }
}
