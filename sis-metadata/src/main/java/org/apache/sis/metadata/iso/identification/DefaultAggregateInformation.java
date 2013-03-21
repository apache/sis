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
package org.apache.sis.metadata.iso.identification;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.AggregateInformation;
import org.opengis.metadata.identification.AssociationType;
import org.opengis.metadata.identification.InitiativeType;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Aggregate dataset information.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_AggregateInformation_Type", propOrder = {
    "aggregateDataSetName",
    "aggregateDataSetIdentifier",
    "associationType",
    "initiativeType"
})
@XmlRootElement(name = "MD_AggregateInformation")
public class DefaultAggregateInformation extends ISOMetadata implements AggregateInformation {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 5520234916010871192L;

    /**
     * Citation information about the aggregate dataset.
     */
    private Citation aggregateDataSetName;

    /**
     * Identification information about aggregate dataset.
     */
    private Identifier aggregateDataSetIdentifier;

    /**
     * Association type of the aggregate dataset.
     */
    private  AssociationType associationType;

    /**
     * Type of initiative under which the aggregate dataset was produced.
     */
    private InitiativeType initiativeType;

    /**
     * Constructs an initially empty Aggregate dataset information.
     */
    public DefaultAggregateInformation() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAggregateInformation castOrCopy(final AggregateInformation object) {
        if (object == null || object instanceof DefaultAggregateInformation) {
            return (DefaultAggregateInformation) object;
        }
        final DefaultAggregateInformation copy = new DefaultAggregateInformation();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Citation information about the aggregate dataset.
     *
     * @return Citation information about the aggregate dataset, or {@code null}.
     */
    @Override
    @XmlElement(name = "aggregateDataSetName")
    public synchronized Citation getAggregateDataSetName() {
        return aggregateDataSetName;
    }

    /**
     * Sets the citation information about the aggregate dataset.
     *
     * @param newValue The new citation.
     */
    public synchronized void setAggregateDataSetName(final Citation newValue) {
        checkWritePermission();
        aggregateDataSetName = newValue;
    }

    /**
     * Identification information about aggregate dataset.
     *
     * @return Identification information about aggregate dataset, or {@code null}.
     */
    @Override
    @XmlElement(name = "aggregateDataSetIdentifier")
    public synchronized Identifier getAggregateDataSetIdentifier() {
        return aggregateDataSetIdentifier;
    }

    /**
     * Sets the identification information about aggregate dataset.
     *
     * @param newValue The new identifier.
     */
    public synchronized void setAggregateDataSetIdentifier(final Identifier newValue) {
        aggregateDataSetIdentifier = newValue;
    }

    /**
     * Association type of the aggregate dataset.
     *
     * @return Association type of the aggregate dataset.
     */
    @Override
    @XmlElement(name = "associationType", required = true)
    public synchronized AssociationType getAssociationType() {
        return associationType;
    }

    /**
     * Sets the association type of the aggregate dataset.
     *
     * @param newValue The new association type.
     */
    public synchronized void setAssociationType(final AssociationType newValue) {
        associationType = newValue;
    }

    /**
     * Type of initiative under which the aggregate dataset was produced.
     *
     * @return Type of initiative under which the aggregate dataset was produced, or {@code null}.
     */
    @Override
    @XmlElement(name = "initiativeType")
    public synchronized InitiativeType getInitiativeType() {
        return initiativeType;
    }

    /**
     * Sets the type of initiative under which the aggregate dataset was produced.
     *
     * @param newValue The new initiative.
     */
    public synchronized void setInitiativeType(final InitiativeType newValue) {
        initiativeType = newValue;
    }
}
