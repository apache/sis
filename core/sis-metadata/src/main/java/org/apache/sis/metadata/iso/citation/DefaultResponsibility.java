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
package org.apache.sis.metadata.iso.citation;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.extent.Extent;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.FilterByVersion;
import org.apache.sis.internal.jaxb.code.CI_RoleCode;

// Branch-specific imports
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;


/**
 * Information about the party and their role.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code CI_Responsibility}
 * {@code   ├─party……………………} Information about the parties.
 * {@code   │   └─name……………} Name of the party.
 * {@code   └─role………………………} Function performed by the responsible party.</div>
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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.5
 * @module
 */
@SuppressWarnings("deprecation")
@XmlType(name = "CI_Responsibility_Type", propOrder = {
    "role",
    "extent",
    "party"
})
@XmlRootElement(name = "CI_Responsibility")
@XmlSeeAlso({
    DefaultResponsibleParty.class
})
public class DefaultResponsibility extends ISOMetadata implements Responsibility {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8290895980682233572L;

    /**
     * Function performed by the responsible party.
     */
    private Role role;

    /**
     * Spatial or temporal extents of the role.
     */
    private Collection<Extent> extents;

    /**
     * Information about the parties.
     */
    private Collection<Party> parties;

    /**
     * Constructs an initially empty responsible party.
     */
    public DefaultResponsibility() {
    }

    /**
     * Constructs a responsibility initialized to the specified values.
     *
     * @param role    function performed by the responsible party, or {@code null}.
     * @param extent  spatial or temporal extent of the role, or {@code null}.
     * @param party   information about the party, or {@code null}.
     */
    public DefaultResponsibility(final Role role, final Extent extent, final Party party) {
        this.role    = role;
        this.extents = singleton(extent, Extent.class);
        this.parties = singleton(party, Party.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Responsibility)
     */
    public DefaultResponsibility(final Responsibility object) {
        super(object);
        if (object != null) {
            this.role    = object.getRole();
            this.extents = copyCollection(object.getExtents(), Extent.class);
            this.parties = copyCollection(object.getParties(), Party.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultResponsibility}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultResponsibility} instance is created using the
     *       {@linkplain #DefaultResponsibility(Responsibility) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       Responsibility contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultResponsibility castOrCopy(final Responsibility object) {
        if (object == null || object instanceof DefaultResponsibility) {
            return (DefaultResponsibility) object;
        }
        return new DefaultResponsibility(object);
    }

    /**
     * Returns the function performed by the responsible party.
     *
     * @return function performed by the responsible party.
     */
    @Override
    @XmlElement(name = "role", required = true)
    @XmlJavaTypeAdapter(CI_RoleCode.Since2014.class)
    public Role getRole() {
        return role;
    }

    /**
     * Sets the function performed by the responsible party.
     *
     * @param  newValue  the new role, or {@code null} if none.
     */
    public void setRole(final Role newValue) {
        checkWritePermission(role);
        role = newValue;
    }

    /**
     * Returns the spatial or temporal extents of the role.
     *
     * @return the spatial or temporal extents of the role.
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Extent> getExtents() {
        return extents = nonNullCollection(extents, Extent.class);
    }

    /**
     * Sets the spatial and temporal extents of the role.
     *
     * @param  newValues  the new spatial and temporal extents of the role.
     */
    public void setExtents(final Collection<? extends Extent> newValues) {
        extents = writeCollection(newValues, extents, Extent.class);
    }

    /**
     * Returns information about the parties.
     *
     * @return information about the parties.
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Party> getParties() {
        return parties = nonNullCollection(parties, Party.class);
    }

    /**
     * Sets information about the parties.
     *
     * @param  newValues  new information about the parties.
     */
    public void setParties(final Collection<? extends Party> newValues) {
        parties = writeCollection(newValues, parties, Party.class);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "extent")
    private Collection<Extent> getExtent() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getExtents() : null;
    }

    @XmlElement(name = "party", required = true)
    private Collection<Party> getParty() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getParties() : null;
    }
}
