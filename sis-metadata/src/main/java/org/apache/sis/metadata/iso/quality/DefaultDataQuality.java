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
import org.apache.sis.internal.metadata.ExcludedSet;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.apache.sis.internal.jaxb.MarshalContext.isMarshaling;
import static org.apache.sis.util.collection.CollectionsExt.isNullOrEmpty;


/**
 * Quality information for the data specified by a data quality scope.
 *
 * {@section Relationship between properties}
 * According ISO 19115, the {@linkplain #getLineage() lineage} and {@linkplain #getReports() reports}
 * properties are exclusive: setting one of those properties to a non-empty value discard the other one.
 * See the {@linkplain #DefaultDataQuality(DataQuality) constructor javadoc} for information about
 * which property has precedence on copy operations.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @since   0.3 (derived from geotk-2.1)
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
    private static final long serialVersionUID = 7964896551368382214L;

    /**
     * The specific data to which the data quality information applies.
     */
    private Scope scope;

    /**
     * Either the lineage as a {@link Lineage} instance or the reports
     * as a {@code Collection<Element>} instance.
     */
    private Object lineageOrReports;

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
     * <p>If both {@linkplain #getLineage() lineage} and {@linkplain #getReports() reports} are
     * specified, then the reports will have precedence and the lineage is silently discarded.</p>
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(DataQuality)
     */
    public DefaultDataQuality(final DataQuality object) {
        super(object);
        scope = object.getScope();
        lineageOrReports = copyCollection(object.getReports(), Element.class);
        if (lineageOrReports == null) {
            // Give precedence to quantitative information.
            lineageOrReports = object.getLineage();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     */
    @Override
    @XmlElement(name = "scope", required = true)
    public synchronized Scope getScope() {
        return scope;
    }

    /**
     * Sets the specific data to which the data quality information applies.
     *
     * @param newValue The new scope.
     */
    public synchronized void setScope(final Scope newValue) {
        checkWritePermission();
        scope = newValue;
    }

    /**
     * Invoked every time the code needs to decide whether the provided information
     * is lineage or the reports. Defined as a method in order to have a single word
     * to search if we need to revisit the policy.
     */
    private boolean isLineage() {
        return (lineageOrReports instanceof Lineage);
    }

    /**
     * Returns the quantitative quality information for the data specified by the scope.
     *
     * {@section Conditions}
     * This method returns a modifiable collection only if the {@linkplain #getLineage() lineage}
     * is not set. Otherwise, this method returns an unmodifiable empty collection.
     *
     * @return The quantitative quality information.
     */
    @Override
    @XmlElement(name = "report")
    public synchronized Collection<Element> getReports() {
        if (isLineage()) {
            return isMarshaling() ? null : new ExcludedSet<Element>("report", "lineage");
        }
        @SuppressWarnings("unchecked")
        Collection<Element> reports = (Collection<Element>) lineageOrReports;
        reports = nonNullCollection(reports, Element.class);
        lineageOrReports = reports;
        return reports;
    }

    /**
     * Sets the quantitative quality information for the data specified by the scope.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-empty, then this method automatically
     * discards the {@linkplain #setLineage lineage}.
     *
     * @param newValues The new reports.
     */
    public synchronized void setReports(final Collection<? extends Element> newValues) {
        @SuppressWarnings("unchecked")
        final Collection<Element> reports = isLineage() ? null : (Collection<Element>) lineageOrReports;
        if (reports != null || !isNullOrEmpty(newValues)) {
            lineageOrReports = writeCollection(newValues, reports, Element.class);
        }
    }

    /**
     * Returns non-quantitative quality information about the lineage of the data specified
     * by the scope. Note that the lineage and the {@linkplain #getReports() reports} are
     * mutually exclusive properties.
     */
    @Override
    @XmlElement(name = "lineage")
    public synchronized Lineage getLineage() {
        return isLineage() ? (Lineage) lineageOrReports : null;
    }

    /**
     * Sets the non-quantitative quality information about the lineage of the data specified
     * by the scope.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards the {@linkplain #setReports reports} collection.
     *
     * @param newValue The new lineage.
     */
    public synchronized void setLineage(final Lineage newValue) {
        checkWritePermission();
        if (newValue != null || isLineage()) {
            lineageOrReports = newValue;
        }
    }
}
