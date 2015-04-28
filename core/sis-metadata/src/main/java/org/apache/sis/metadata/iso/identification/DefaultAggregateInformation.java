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
import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.AggregateInformation;
import org.opengis.metadata.identification.AssociationType;
import org.opengis.metadata.identification.InitiativeType;
import org.apache.sis.metadata.iso.citation.DefaultCitation;


/**
 * Associated resource information.
 *
 * <div class="section">Relationship between properties</div>
 * According ISO 19115, at least one of {@linkplain #getName() name} and
 * {@linkplain #getMetadataReference() metadata reference} shall be provided.
 *
 * <div class="warning"><b>Upcoming API change — renaming</b><br>
 * As of ISO 19115:2014, {@code AggregateInformation} has been renamed {@code AssociatedResource}.
 * This class will be replaced by {@link DefaultAssociatedResource} when GeoAPI will provide the
 * {@code AssociatedResource} interface (tentatively in GeoAPI 3.1 or 4.0).
 * </div>
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
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
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
     *
     * @param object The metadata to copy values from.
     */
    DefaultAggregateInformation(final DefaultAssociatedResource object) {
        super(object);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(AssociatedResource)
     */
    public DefaultAggregateInformation(final AggregateInformation object) {
        super(object);
        if (object != null && !(object instanceof DefaultAssociatedResource)) {
            setAggregateDataSetName(object.getAggregateDataSetName());
            setAggregateDataSetIdentifier(object.getAggregateDataSetIdentifier());
        }
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
    public static DefaultAggregateInformation castOrCopy(final AggregateInformation object) {
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
     * @deprecated As of ISO 19115:2014, replaced by {@link #getName()}.
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
     * @deprecated As of ISO 19115:2014, replaced by {@link #setName(Citation)}.
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
     * @deprecated As of ISO 19115:2014, replaced by the first identifier of {@link #getAggregateDataSetName()}.
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
            final Collection<? extends Identifier> names = name.getIdentifiers();
            if (names != null) { // May be null on XML marshalling.
                final Iterator<? extends Identifier> it = names.iterator();
                if (it.hasNext()) {
                    return it.next();
                }
            }
        }
        return null;
    }

    /**
     * Sets the identification information about aggregate dataset.
     *
     * @param newValue The new identifier.
     *
     * @deprecated As of ISO 19115:2014, replaced by an identifier of {@link #getAggregateDataSetName()}.
     */
    @Deprecated
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
     * Returns the type of relation between the resources.
     *
     * @return Type of relation between the resources.
     */
    @Override
    @XmlElement(name = "associationType", required = true)
    public AssociationType getAssociationType() {
        return super.getAssociationType();
    }

    /**
     * Sets the type of relation between the resources.
     *
     * @param newValue The new type of relation.
     */
    @Override
    public void setAssociationType(final AssociationType newValue) {
        super.setAssociationType(newValue);
    }

    /**
     * Returns the type of initiative under which the associated resource was produced, or {@code null} if none.
     *
     * @return The type of initiative under which the associated resource was produced, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "initiativeType")
    public InitiativeType getInitiativeType() {
        return super.getInitiativeType();
    }

    /**
     * Sets a new type of initiative under which the associated resource was produced.
     *
     * @param newValue The new type of initiative.
     */
    @Override
    public void setInitiativeType(final InitiativeType newValue) {
        super.setInitiativeType(newValue);
    }
}
