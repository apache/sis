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
import org.opengis.metadata.citation.Individual;
import org.opengis.metadata.citation.Organisation;
import org.opengis.metadata.identification.BrowseGraphic;


/**
 * Information about the party if the party is an organization.
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
public class DefaultOrganisation extends AbstractParty implements Organisation {
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
    private Collection<Individual> individual;

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
                               final Individual individual,
                               final Contact contactInfo)
    {
        super(name, contactInfo);
        this.logo       = singleton(logo, BrowseGraphic.class);
        this.individual = singleton(individual, Individual.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Organisation)
     */
    public DefaultOrganisation(final Organisation object) {
        super(object);
        if (object != null) {
            logo       = copyCollection(object.getLogo(), BrowseGraphic.class);
            individual = copyCollection(object.getIndividual(), Individual.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultOrganisation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultOrganisation} instance is created using the
     *       {@linkplain #DefaultOrganisation(Organisation) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultOrganisation castOrCopy(final Organisation object) {
        if (object == null || object instanceof DefaultOrganisation) {
            return (DefaultOrganisation) object;
        }
        return new DefaultOrganisation(object);
    }

    /**
     * Returns the graphics identifying organization.
     *
     * @return Graphics identifying organization, or an empty collection if there is none.
     */
    @Override
    @XmlElement(name = "logo")
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
     * @return Individuals in the named organization, or an empty collection.
     */
    @Override
    @XmlElement(name = "individual")
    public Collection<Individual> getIndividual() {
        return individual = nonNullCollection(individual, Individual.class);
    }

    /**
     * Sets the individuals in the named organization.
     *
     * @param newValues The new individuals in the named organization.
     */
    public void setIndividual(final Collection<? extends Individual> newValues) {
        individual = writeCollection(newValues, individual, Individual.class);
    }
}
