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
package org.apache.sis.metadata.iso.constraint;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.constraint.Restriction;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about resource release constraints.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code Releasability} interface.
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
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_Releasability_Type", propOrder = {
    "addressees",
    "statement",
    "disseminationConstraints"
})
@XmlRootElement(name = "MD_Releasability")
@UML(identifier="MD_Releasability", specification=ISO_19115)
public class DefaultReleasability extends ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4449531804328254887L;

    /**
     * Party to which the release statement applies.
     */
    private Collection<DefaultResponsibility> addressees;

    /**
     * Release statement.
     */
    private InternationalString statement;

    /**
     * Component in determining releasability.
     */
    private Collection<Restriction> disseminationConstraints;

    /**
     * Creates an initially empty releasability.
     */
    public DefaultReleasability() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Releasability)
     */
    public DefaultReleasability(final DefaultReleasability object) {
        super(object);
        if (object != null) {
            addressees                = copyCollection(object.getAddressees(), DefaultResponsibility.class);
            statement                 = object.getStatement();
            disseminationConstraints  = copyCollection(object.getDisseminationConstraints(), Restriction.class);
        }
    }

    /**
     * Returns the parties to which the release statement applies.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code Responsibility} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Parties to which the release statement applies.
     */
    @XmlElement(name = "addressee")
    @UML(identifier="addressee", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DefaultResponsibility> getAddressees() {
        return addressees = nonNullCollection(addressees, DefaultResponsibility.class);
    }

    /**
     * Sets the parties to which the release statement applies.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code Responsibility} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new parties.
     */
    public void getAddressees(final Collection<? extends DefaultResponsibility> newValues) {
        addressees = writeCollection(newValues, addressees, DefaultResponsibility.class);
    }

    /**
     * Returns the release statement.
     *
     * @return Release statement, or {@code null} if none.
     */
    @XmlElement(name = "statement")
    @UML(identifier="statement", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getStatement() {
        return statement;
    }

    /**
     * Sets the release statement.
     *
     * @param newValue The new release statement.
     */
    public void setStatement(final InternationalString newValue) {
        checkWritePermission();
        statement = newValue;
    }

    /**
     * Components in determining releasability.
     *
     * @return Components in determining releasability.
     */
    @XmlElement(name = "disseminationConstraints")
    @UML(identifier="disseminationConstraints", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Restriction> getDisseminationConstraints() {
        return disseminationConstraints = nonNullCollection(disseminationConstraints, Restriction.class);
    }

    /**
     * Sets the components in determining releasability.
     *
     * @param newValues The new components.
     */
    public void getDisseminationConstraints(final Collection<? extends Restriction> newValues) {
        disseminationConstraints = writeCollection(newValues, disseminationConstraints, Restriction.class);
    }
}
