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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.AggregateInformation;
import org.opengis.metadata.identification.AssociationType;
import org.opengis.metadata.identification.InitiativeType;
import org.apache.sis.metadata.iso.ISOMetadata;

// Branch-specific imports.
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Associated resource information.
 *
 * <div class="section">Relationship between properties</div>
 * According ISO 19115, at least one of {@linkplain #getName() name} and
 * {@linkplain #getMetadataReference() metadata reference} shall be provided.
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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_AssociatedResource_Type" /*, propOrder = {
    "name",
    "associationType",
    "initiativeType",
    "metadataReference"
}*/)
@XmlRootElement(name = "MD_AssociatedResource")
@UML(identifier="MD_AssociatedResource", specification=ISO_19115)
public class DefaultAssociatedResource extends ISOMetadata {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -803259032236939135L;

    /**
     * Citation information about the associated resource.
     */
    private Citation name;

    /**
     * Type of relation between the resources.
     */
    private AssociationType associationType;

    /**
     * Type of initiative under which the associated resource was produced.
     */
    private InitiativeType initiativeType;

    /**
     * Reference to the metadata of the associated resource.
     */
    private Citation metadataReference;

    /**
     * Constructs an initially empty associated resource.
     */
    public DefaultAssociatedResource() {
    }

    /**
     * Constructs an associated resource initialized to the specified values.
     *
     * @param name            Citation information about the associated resource.
     * @param associationType Type of relation between the resources.
     */
    public DefaultAssociatedResource(final Citation name, final AssociationType associationType) {
        this.name            = name;
        this.associationType = associationType;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a constructor for {@link DefaultAggregateInformation} constructor only.
     *
     * @param object The metadata to copy values from.
     */
    DefaultAssociatedResource(final DefaultAssociatedResource object) {
        this.associationType   = object.associationType;
        this.initiativeType    = object.initiativeType;
        this.name              = object.name;
        this.metadataReference = object.metadataReference;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    DefaultAssociatedResource(final AggregateInformation object) {
        if (object != null) {
            this.associationType   = object.getAssociationType();
            this.initiativeType    = object.getInitiativeType();
            if (object instanceof DefaultAssociatedResource) {
                this.name              = ((DefaultAssociatedResource) object).getName();
                this.metadataReference = ((DefaultAssociatedResource) object).getMetadataReference();
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    static DefaultAssociatedResource castOrCopy(final AggregateInformation object) {
        if (object == null || object instanceof DefaultAssociatedResource) {
            return (DefaultAssociatedResource) object;
        }
        return new DefaultAssociatedResource(object);
    }

    /**
     * Returns citation information about the associated resource, or {@code null} if none.
     *
     * @return Citation information about the associated resource, or {@code null} if none.
     */
/// @XmlElement(name = "name")
    @UML(identifier="name", obligation=CONDITIONAL, specification=ISO_19115)
    public Citation getName() {
        return name;
    }

    /**
     * Sets citation information about the associated resource.
     *
     * @param newValue The new citation information, or {@code null}.
     */
    public void setName(final Citation newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns the type of relation between the resources.
     *
     * @return Type of relation between the resources.
     */
/// @XmlElement(name = "associationType", required = true)
    @UML(identifier="associationType", obligation=MANDATORY, specification=ISO_19115)
    public AssociationType getAssociationType() {
        return associationType;
    }

    /**
     * Sets the type of relation between the resources.
     *
     * @param newValue The new type of relation.
     */
    public void setAssociationType(final AssociationType newValue) {
        checkWritePermission();
        associationType = newValue;
    }

    /**
     * Returns the type of initiative under which the associated resource was produced, or {@code null} if none.
     *
     * @return The type of initiative under which the associated resource was produced, or {@code null} if none.
     */
/// @XmlElement(name = "initiativeType")
    @UML(identifier="initiativeType", obligation=OPTIONAL, specification=ISO_19115)
    public InitiativeType getInitiativeType() {
        return initiativeType;
    }

    /**
     * Sets a new type of initiative under which the associated resource was produced.
     *
     * @param newValue The new type of initiative.
     */
    public void setInitiativeType(final InitiativeType newValue) {
        checkWritePermission();
        initiativeType = newValue;
    }

    /**
     * Return a reference to the metadata of the associated resource, or {@code null} if none.
     *
     * @return Reference to the metadata of the associated resource, or {@code null} if none.
     */
/// @XmlElement(name = "metadataReference")
    @UML(identifier="metadataReference", obligation=CONDITIONAL, specification=ISO_19115)
    public Citation getMetadataReference() {
        return metadataReference;
    }

    /**
     * Sets the reference to the metadata of the associated resource.
     *
     * @param newValue The new reference to the metadata.
     */
    public void setMetadataReference(final Citation newValue) {
        checkWritePermission();
        metadataReference = newValue;
    }
}
