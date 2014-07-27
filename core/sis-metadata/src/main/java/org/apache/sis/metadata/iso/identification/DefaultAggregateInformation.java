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

import java.util.Iterator;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.AggregateInformation;
import org.opengis.metadata.identification.AssociatedResource;
import org.opengis.metadata.identification.AssociationType;
import org.opengis.metadata.identification.InitiativeType;
import org.apache.sis.metadata.iso.citation.DefaultCitation;


/**
 * Aggregate dataset information.
 *
 * {@section Relationship between properties}
 * According ISO 19115, at least one of {@linkplain #getAggregateDataSetName() aggregate dataset
 * name} and {@linkplain #getAggregateDataSetIdentifier() aggregate dataset identifier} shall be
 * provided.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.5
 * @module
 *
 * @deprecated Replaced by {@link DefaultAssociatedResource} as of ISO 19115:2014.
 */
@Deprecated
@XmlType(name = "MD_AggregateInformation_Type", propOrder = {
    "aggregateDataSetName",
    "aggregateDataSetIdentifier",
    "associationType",
    "initiativeType"
})
@XmlRootElement(name = "MD_AggregateInformation")
public class DefaultAggregateInformation extends DefaultAssociatedResource implements AggregateInformation {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -8769840909779188495L;

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
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(AggregateInformation)
     */
    public DefaultAggregateInformation(final AssociatedResource object) {
        super(object);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultAggregateInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultAggregateInformation} instance is created using the
     *       {@linkplain #DefaultAggregateInformation(AssociatedResource) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAggregateInformation castOrCopy(final AssociatedResource object) {
        if (object == null || object instanceof DefaultAggregateInformation) {
            return (DefaultAggregateInformation) object;
        }
        return new DefaultAggregateInformation(object);
    }

    /**
     * Citation information about the aggregate dataset.
     *
     * @return Citation information about the aggregate dataset, or {@code null}.
     *
     * @deprecated Replaced by {@link #getName()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "aggregateDataSetName")
    public Citation getAggregateDataSetName() {
        return getName();
    }

    /**
     * Sets the citation information about the aggregate dataset.
     *
     * @param newValue The new citation.
     *
     * @deprecated Replaced by {@link #setName(Citation)}.
     */
    @Deprecated
    public void setAggregateDataSetName(final Citation newValue) {
        setName(newValue);
    }

    /**
     * Identification information about aggregate dataset.
     *
     * @return Identification information about aggregate dataset, or {@code null}.
     *
     * @deprecated Replaced by the first identifier of {@link #getAggregateDataSetName()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "aggregateDataSetIdentifier")
    public Identifier getAggregateDataSetIdentifier() {
        return getAggregateDataSetIdentifier(getAggregateDataSetName());
    }

    /**
     * Returns the first identifier of the given citation.
     */
    static Identifier getAggregateDataSetIdentifier(final Citation name) {
        if (name != null) {
            final Iterator<? extends Identifier> it = name.getIdentifiers().iterator();
            if (it.hasNext()) {
                return it.next();
            }
        }
        return null;
    }

    /**
     * Sets the identification information about aggregate dataset.
     *
     * @param newValue The new identifier.
     *
     * @deprecated Replaced by an identifier of {@link #getAggregateDataSetName()}.
     */
    public void setAggregateDataSetIdentifier(final Identifier newValue) {
        checkWritePermission();
        Citation name = getAggregateDataSetName();
        if (newValue != null) {
            if (!(name instanceof DefaultCitation)) {
                name = new DefaultCitation(name);
                setAggregateDataSetName(name);
            }
            /*
             * If there is more than one value, replace only the first one and keep all other ones unchanged.
             * The intend is to be consistent with the getter method, which returns the first element.
             */
            final ArrayList<Identifier> identifiers = new ArrayList<Identifier>(name.getIdentifiers());
            if (identifiers.isEmpty()) {
                identifiers.add(newValue);
            } else {
                identifiers.set(0, newValue);
            }
            ((DefaultCitation) name).setIdentifiers(identifiers);
        } else if (name != null) {
            final Iterator<? extends Identifier> it = name.getIdentifiers().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    /**
     * Association type of the aggregate dataset.
     *
     * @return Association type of the aggregate dataset.
     */
    @Override
    @XmlElement(name = "associationType", required = true)
    public AssociationType getAssociationType() {
        return super.getAssociationType();
    }

    /**
     * Sets the association type of the aggregate dataset.
     *
     * @param newValue The new association type.
     */
    @Override
    public void setAssociationType(final AssociationType newValue) {
        super.setAssociationType(newValue);
    }

    /**
     * Type of initiative under which the aggregate dataset was produced.
     *
     * @return Type of initiative under which the aggregate dataset was produced, or {@code null}.
     */
    @Override
    @XmlElement(name = "initiativeType")
    public InitiativeType getInitiativeType() {
        return super.getInitiativeType();
    }

    /**
     * Sets the type of initiative under which the aggregate dataset was produced.
     *
     * @param newValue The new initiative.
     */
    @Override
    public void setInitiativeType(final InitiativeType newValue) {
        super.setInitiativeType(newValue);
    }
}
