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
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.metadata.iso.ISOMetadata;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the party and their role.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code Responsibility} interface.
 * </div>
 *
 * <p><b>Limitations:</b></p>
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
@XmlType(name = "CI_Responsibility_Type", propOrder = {
/// "role",
/// "extents",
/// "parties"
})
@XmlRootElement(name = "CI_Responsibility")
@XmlSeeAlso({
    DefaultResponsibleParty.class
})
@UML(identifier="CI_Responsibility", specification=ISO_19115)
public class DefaultResponsibility extends ISOMetadata {
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
    private Collection<AbstractParty> parties;

    /**
     * Constructs an initially empty responsible party.
     */
    public DefaultResponsibility() {
    }

    /**
     * Constructs a responsibility initialized to the specified values.
     *
     * @param role   Function performed by the responsible party, or {@code null}.
     * @param extent Spatial or temporal extent of the role, or {@code null}.
     * @param party  Information about the party, or {@code null}.
     */
    public DefaultResponsibility(final Role role, final Extent extent, final AbstractParty party) {
        this.role    = role;
        this.extents = singleton(extent, Extent.class);
        this.parties = singleton(party, AbstractParty.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    public DefaultResponsibility(final DefaultResponsibility object) {
        super(object);
        if (object != null) {
            this.role    = object.getRole();
            this.extents = copyCollection(object.getExtents(), Extent.class);
            this.parties = copyCollection(object.getParties(), AbstractParty.class);
        }
    }

    /**
     * Bridge constructor for {@link DefaultResponsibleParty#DefaultResponsibleParty(ResponsibleParty)}.
     */
    DefaultResponsibility(final ResponsibleParty object) {
        super(object);
        if (object != null) {
            this.role = object.getRole();
            if (object instanceof DefaultResponsibility) {
                final DefaultResponsibility c = (DefaultResponsibility) object;
                this.extents = copyCollection(c.getExtents(), Extent.class);
                this.parties = copyCollection(c.getParties(), AbstractParty.class);
            }
        }
    }

    /**
     * Returns the function performed by the responsible party.
     *
     * @return Function performed by the responsible party.
     */
/// @XmlElement(name = "role", required = true)
    @UML(identifier="role", obligation=MANDATORY, specification=ISO_19115)
    public Role getRole() {
        return role;
    }

    /**
     * Sets the function performed by the responsible party.
     *
     * @param newValue The new role, or {@code null} if none.
     */
    public void setRole(final Role newValue) {
        checkWritePermission();
        role = newValue;
    }

    /**
     * Returns the spatial or temporal extents of the role.
     *
     * @return The spatial or temporal extents of the role.
     */
/// @XmlElement(name = "extent")
    @UML(identifier="extent", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Extent> getExtents() {
        return extents = nonNullCollection(extents, Extent.class);
    }

    /**
     * Sets the spatial and temporal extents of the role.
     *
     * @param newValues The new spatial and temporal extents of the role.
     */
    public void setExtents(final Collection<? extends Extent> newValues) {
        extents = writeCollection(newValues, extents, Extent.class);
    }

    /**
     * Returns information about the parties.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code Party} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Information about the parties.
     */
/// @XmlElement(name = "party", required = true)
    @UML(identifier="party", obligation=MANDATORY, specification=ISO_19115)
    public Collection<AbstractParty> getParties() {
        return parties = nonNullCollection(parties, AbstractParty.class);
    }

    /**
     * Sets information about the parties.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code Party} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues New information about the parties.
     */
    public void setParties(final Collection<? extends AbstractParty> newValues) {
        parties = writeCollection(newValues, parties, AbstractParty.class);
    }
}
