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

import java.util.List;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.Namespaces;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Operation chain information.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code OperationChainMetadata} interface.
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
@XmlType(name = "SV_OperationChainMetadata_Type", namespace = Namespaces.SRV, propOrder = {
    "name",
    "description",
    "operations"
})
@XmlRootElement(name = "SV_OperationChainMetadata", namespace = Namespaces.SRV)
@UML(identifier="SV_OperationChainMetadata", specification=ISO_19115)
public class DefaultOperationChainMetadata extends ISOMetadata {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 4132508877114835286L;

    /**
     * The name as used by the service for this chain.
     */
    private InternationalString name;

    /**
     * A narrative explanation of the services in the chain and resulting output.
     */
    private InternationalString description;

    /**
     * Information about the operations applied by the chain.
     */
    private List<DefaultOperationMetadata> operations;

    /**
     * Constructs an initially empty operation chain metadata.
     */
    public DefaultOperationChainMetadata() {
    }

    /**
     * Constructs a new operation chain metadata initialized to the specified name.
     *
     * @param name The name as used by the service for this chain.
     */
    public DefaultOperationChainMetadata(final CharSequence name) {
        this.name = Types.toInternationalString(name);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(OperationChainMetadata)
     */
    public DefaultOperationChainMetadata(final DefaultOperationChainMetadata object) {
        super(object);
        if (object != null) {
            this.name        = object.getName();
            this.description = object.getDescription();
            this.operations  = copyList(object.getOperations(), DefaultOperationMetadata.class);
        }
    }

    /**
     * Returns the name as used by the service for this chain.
     *
     * @return Name as used by the service for this chain.
     */
    @XmlElement(name = "name", namespace = Namespaces.SRV, required = true)
    @UML(identifier="name", obligation=MANDATORY, specification=ISO_19115)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name used by the service for this chain.
     *
     * @param newValue The new name used by the service for this chain.
     */
    public void setName(final InternationalString newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns a narrative explanation of the services in the chain and resulting output.
     *
     * @return Narrative explanation of the services in the chain and resulting output, or {@code null} if none.
     */
    @XmlElement(name = "description", namespace = Namespaces.SRV)
    @UML(identifier="description", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the narrative explanation of the services in the chain and resulting output.
     *
     * @param newValue The new a narrative explanation of the services in the chain and resulting output
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns information about the operations applied by the chain.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code OperationMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Information about the operations applied by the chain.
     */
    @XmlElement(name = "operation", namespace = Namespaces.SRV, required = true)
    @UML(identifier="operation", obligation=MANDATORY, specification=ISO_19115)
    public List<DefaultOperationMetadata> getOperations() {
        return operations = nonNullList(operations, DefaultOperationMetadata.class);
    }

    /**
     * Sets the information about the operations applied by the chain.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code OperationMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new information about the operations applied by the chain.
     */
    public void setOperations(final List<? extends DefaultOperationMetadata> newValues) {
        operations = writeList(newValues, operations, DefaultOperationMetadata.class);
    }
}
