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
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.identification.BrowseGraphic;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the party if the party is an organization.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code Organisation} interface.
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
 * @version 0.5
 * @since   0.5
 * @module
 */
@XmlType(name = "CI_Organisation_Type", propOrder = {
    "logo",
    "individual"
})
@XmlRootElement(name = "CI_Organisation")
@UML(identifier="CI_Organisation", specification=ISO_19115)
public class DefaultOrganisation extends AbstractParty {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5113656476176154532L;

    /**
     * Graphic identifying organization.
     */
    private Collection<BrowseGraphic> logo;

    /**
     * Individuals in the named organization.
     */
    private Collection<DefaultIndividual> individual;

    /**
     * Constructs an initially empty organization.
     */
    public DefaultOrganisation() {
    }

    /**
     * Constructs an organization initialized to the specified values.
     *
     * @param name        Name of the organization, or {@code null} if none.
     * @param logo        Graphic identifying the organization, or {@code null} if none.
     * @param individual  Position of the individual in an organization, or {@code null} if none.
     * @param contactInfo Contact information for the organization, or {@code null} if none.
     */
    public DefaultOrganisation(final CharSequence name,
                               final BrowseGraphic logo,
                               final DefaultIndividual individual,
                               final Contact contactInfo)
    {
        super(name, contactInfo);
        this.logo       = singleton(logo, BrowseGraphic.class);
        this.individual = singleton(individual, DefaultIndividual.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    public DefaultOrganisation(final DefaultOrganisation object) {
        super(object);
        if (object != null) {
            logo       = copyCollection(object.getLogo(), BrowseGraphic.class);
            individual = copyCollection(object.getIndividual(), DefaultIndividual.class);
        }
    }

    /**
     * Returns the graphics identifying organization.
     *
     * @return Graphics identifying organization, or an empty collection if there is none.
     */
    @XmlElement(name = "logo")
    @UML(identifier="logo", obligation=CONDITIONAL, specification=ISO_19115)
    public Collection<BrowseGraphic> getLogo() {
        return logo = nonNullCollection(logo, BrowseGraphic.class);
    }

    /**
     * Sets the graphics identifying organisation.
     *
     * @param newValues The new graphics identifying organization.
     */
    public void setLogo(final Collection<? extends BrowseGraphic> newValues) {
        logo = writeCollection(newValues, logo, BrowseGraphic.class);
    }

    /**
     * Returns the individuals in the named organization.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code Individual} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Individuals in the named organization, or an empty collection.
     */
    @XmlElement(name = "individual")
    @UML(identifier="individual", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DefaultIndividual> getIndividual() {
        return individual = nonNullCollection(individual, DefaultIndividual.class);
    }

    /**
     * Sets the individuals in the named organization.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code Individual} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new individuals in the named organization.
     */
    public void setIndividual(final Collection<? extends DefaultIndividual> newValues) {
        individual = writeCollection(newValues, individual, DefaultIndividual.class);
    }
}
