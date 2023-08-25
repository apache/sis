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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.constraint.Restriction;
import org.apache.sis.metadata.iso.ISOMetadata;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.constraint.Releasability;


/**
 * Information about resource release constraints.
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
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.5
 */
@XmlType(name = "MD_Releasability_Type", propOrder = {
    "addressees",
    "statement",
    "disseminationConstraints"
})
@XmlRootElement(name = "MD_Releasability")
public class DefaultReleasability extends ISOMetadata implements Releasability {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4449531804328254887L;

    /**
     * Party to which the release statement applies.
     */
    @SuppressWarnings("serial")
    private Collection<Responsibility> addressees;

    /**
     * Release statement.
     */
    @SuppressWarnings("serial")
    private InternationalString statement;

    /**
     * Component in determining releasability.
     */
    @SuppressWarnings("serial")
    private Collection<Restriction> disseminationConstraints;

    /**
     * Creates an initially empty releasability.
     */
    public DefaultReleasability() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Releasability)
     */
    public DefaultReleasability(final Releasability object) {
        super(object);
        if (object != null) {
            addressees                = copyCollection(object.getAddressees(), Responsibility.class);
            statement                 = object.getStatement();
            disseminationConstraints  = copyCollection(object.getDisseminationConstraints(), Restriction.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultReleasability}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultReleasability} instance is created using the
     *       {@linkplain #DefaultReleasability(Releasability) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultReleasability castOrCopy(final Releasability object) {
        if (object == null || object instanceof DefaultReleasability) {
            return (DefaultReleasability) object;
        }
        return new DefaultReleasability(object);
    }

    /**
     * Returns the parties to which the release statement applies.
     *
     * @return parties to which the release statement applies.
     */
    @Override
    @XmlElement(name = "addressee")
    public Collection<Responsibility> getAddressees() {
        return addressees = nonNullCollection(addressees, Responsibility.class);
    }

    /**
     * Sets the parties to which the release statement applies.
     *
     * @param  newValues  the new parties.
     */
    public void setAddressees(final Collection<? extends Responsibility> newValues) {
        addressees = writeCollection(newValues, addressees, Responsibility.class);
    }

    /**
     * Returns the release statement.
     *
     * @return release statement, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "statement")
    public InternationalString getStatement() {
        return statement;
    }

    /**
     * Sets the release statement.
     *
     * @param  newValue  the new release statement.
     */
    public void setStatement(final InternationalString newValue) {
        checkWritePermission(statement);
        statement = newValue;
    }

    /**
     * Components in determining releasability.
     *
     * @return components in determining releasability.
     */
    @Override
    @XmlElement(name = "disseminationConstraints")
    public Collection<Restriction> getDisseminationConstraints() {
        return disseminationConstraints = nonNullCollection(disseminationConstraints, Restriction.class);
    }

    /**
     * Sets the components in determining releasability.
     *
     * @param  newValues  the new components.
     */
    public void setDisseminationConstraints(final Collection<? extends Restriction> newValues) {
        disseminationConstraints = writeCollection(newValues, disseminationConstraints, Restriction.class);
    }
}
