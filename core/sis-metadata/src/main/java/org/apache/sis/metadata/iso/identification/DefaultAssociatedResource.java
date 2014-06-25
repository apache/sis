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
import org.opengis.metadata.identification.AssociatedResource;
import org.opengis.metadata.identification.AssociationType;
import org.opengis.metadata.identification.InitiativeType;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Associated resource information.
 *
 * @author  Remi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_AssociatedResource_Type", propOrder = {
/// "name",
/// "associationType",
/// "initiativeType",
/// "metadataReference"
})
@XmlRootElement(name = "MD_AssociatedResource")
public class DefaultAssociatedResource extends ISOMetadata implements AssociatedResource {
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
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(AssociatedResource)
     */
    public DefaultAssociatedResource(final AssociatedResource object) {
        if (object != null) {
            this.name              = object.getName();
            this.associationType   = object.getAssociationType();
            this.initiativeType    = object.getInitiativeType();
            this.metadataReference = object.getMetadataReference();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultAssociatedResource}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultAssociatedResource} instance is created using the
     *       {@linkplain #DefaultAssociatedResource(AssociatedResource) copy constructor} and returned.
     *       Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAssociatedResource castOrCopy(final AssociatedResource object) {
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
    @Override
/// @XmlElement(name = "name")
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
    @Override
/// @XmlElement(name = "associationType", required = true)
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
    @Override
/// @XmlElement(name = "initiativeType")
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
    @Override
/// @XmlElement(name = "metadataReference")
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
