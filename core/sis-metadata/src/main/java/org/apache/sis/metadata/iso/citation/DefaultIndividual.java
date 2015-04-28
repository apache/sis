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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Contact;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.Types;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the party if the party is an individual.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code Individual} interface.
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
@XmlType(name = "CI_Individual_Type", propOrder = {
    "positionName"
})
@XmlRootElement(name = "CI_Individual")
@UML(identifier="CI_Individual", specification=ISO_19115)
public class DefaultIndividual extends AbstractParty {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5116802681895278739L;

    /**
     * Position of the individual in an organization.
     */
    private InternationalString positionName;

    /**
     * Constructs an initially empty individual.
     */
    public DefaultIndividual() {
    }

    /**
     * Constructs an individual initialized to the specified values.
     *
     * @param name         Name of the individual.
     * @param positionName Position of the individual in an organization.
     * @param contactInfo  Contact information for the individual.
     */
    public DefaultIndividual(final CharSequence name,
                             final CharSequence positionName,
                             final Contact contactInfo)
    {
        super(name, contactInfo);
        this.positionName = Types.toInternationalString(positionName);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    public DefaultIndividual(final DefaultIndividual object) {
        super(object);
        if (object != null) {
            positionName = object.getPositionName();
        }
    }

    /**
     * Returns position of the individual in an organization, or {@code null} if none.
     *
     * @return Position of the individual in an organization, or {@code null} if none.
     */
    @XmlElement(name = "positionName")
    @UML(identifier="positionName", obligation=CONDITIONAL, specification=ISO_19115)
    public InternationalString getPositionName() {
        return positionName;
    }

    /**
     * Sets a new position of the individual in an organization.
     *
     * @param newValue The new position of the individual in an organization.
     */
    public void setPositionName(final InternationalString newValue) {
        checkWritePermission();
        positionName = newValue;
    }
}
