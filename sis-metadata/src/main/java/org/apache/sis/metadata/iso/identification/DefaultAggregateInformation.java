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
 * {@section Relationship between properties}
 * According ISO 19115, the {@linkplain #getAggregateDataSetName() aggregate dataset name} and
 * {@linkplain #getAggregateDataSetIdentifier() aggregate dataset identifier} properties are
 * exclusive: setting one of those properties to a non-null value discard the other one.
 * See the {@linkplain #DefaultAggregateInformation(AggregateInformation) constructor javadoc}
 * for information about which property has precedence on copy operations.
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
     * The {@code aggregateDataSetName} or {@code aggregateDataSetIdentifier} property.
     */
    private Object nameOrIdentifier;

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
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * <p>If both {@linkplain #getAggregateDataSetName() aggregate dataset name} and
     * {@linkplain #getAggregateDataSetIdentifier() aggregate dataset identifier} are
     * specified, then the name will have precedence and the identifier is silently discarded.</p>
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(AggregateInformation)
     */
    public DefaultAggregateInformation(final AggregateInformation object) {
        super(object);
        nameOrIdentifier = object.getAggregateDataSetName();
        if (nameOrIdentifier == null) {
            nameOrIdentifier = object.getAggregateDataSetIdentifier();
        }
        associationType = object.getAssociationType();
        initiativeType  = object.getInitiativeType();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultAggregateInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultAggregateInformation} instance is created using the
     *       {@linkplain #DefaultAggregateInformation(AggregateInformation) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAggregateInformation castOrCopy(final AggregateInformation object) {
        if (object == null || object instanceof DefaultAggregateInformation) {
            return (DefaultAggregateInformation) object;
        }
        return new DefaultAggregateInformation(object);
    }

    /**
     * Invoked every time the code needs to decide whether the provided information
     * is the name or the identifier. Defined as a method in order to have a single
     * word to search if we need to revisit the policy.
     */
    private boolean isName() {
        return (nameOrIdentifier instanceof Citation);
    }

    /**
     * Citation information about the aggregate dataset.
     *
     * @return Citation information about the aggregate dataset, or {@code null}.
     */
    @Override
    @XmlElement(name = "aggregateDataSetName")
    public synchronized Citation getAggregateDataSetName() {
        return isName() ? (Citation) nameOrIdentifier : null;
    }

    /**
     * Sets the citation information about the aggregate dataset.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards the {@linkplain #setAggregateDataSetIdentifier aggregate dataset identifier}.
     *
     * @param newValue The new citation.
     */
    public synchronized void setAggregateDataSetName(final Citation newValue) {
        checkWritePermission();
        if (newValue != null || isName()) {
            nameOrIdentifier = newValue;
        }
    }

    /**
     * Identification information about aggregate dataset.
     *
     * @return Identification information about aggregate dataset, or {@code null}.
     */
    @Override
    @XmlElement(name = "aggregateDataSetIdentifier")
    public synchronized Identifier getAggregateDataSetIdentifier() {
        return isName() ? null : (Identifier) nameOrIdentifier;
    }

    /**
     * Sets the identification information about aggregate dataset.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards the {@linkplain #setAggregateDataSetName aggregate dataset name}.
     *
     * @param newValue The new identifier.
     */
    public synchronized void setAggregateDataSetIdentifier(final Identifier newValue) {
        checkWritePermission();
        if (newValue != null || !isName()) {
            nameOrIdentifier = newValue;
        }
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
        checkWritePermission();
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
        checkWritePermission();
        initiativeType = newValue;
    }
}
