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

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.ScopedName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.DataIdentification;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.metadata.direct.GO_ScopedName;
import org.apache.sis.xml.Namespaces;

import static org.apache.sis.internal.jaxb.gco.PropertyType.LEGACY_XML;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Links a given operation name with a resource identified by an "identifier".
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code CoupledResource} interface.
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
@XmlType(name = "SV_CoupledResource_Type", namespace = Namespaces.SRV, propOrder = {
    "operationName",
    "identifier",
    "scopedName" /*,
    "resourceReferences",
    "resources",
    "operation" */
})
@XmlRootElement(name = "SV_CoupledResource", namespace = Namespaces.SRV)
@UML(identifier="SV_CoupledResource", specification=ISO_19115)
public class DefaultCoupledResource extends ISOMetadata {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 154704781596732747L;

    /**
     * Scoped identifier of the resource in the context of the given service instance.
     */
    private ScopedName scopedName;

    /**
     * References to the resource on which the services operates.
     */
    private Collection<Citation> resourceReferences;

    /**
     * The tightly coupled resources.
     */
    private Collection<DataIdentification> resources;

    /**
     * The service operation.
     */
    private DefaultOperationMetadata operation;

    /**
     * Constructs an initially empty coupled resource.
     */
    public DefaultCoupledResource() {
    }

    /**
     * Constructs a new coupled resource initialized to the specified values.
     *
     * @param name       Scoped identifier of the resource in the context of the given service instance.
     * @param reference  Reference to the reference to the resource on which the services operates.
     * @param resource   The tightly coupled resource.
     * @param operation  The service operation.
     */
    public DefaultCoupledResource(final ScopedName name,
                                  final Citation reference,
                                  final DataIdentification resource,
                                  final DefaultOperationMetadata operation)
    {
        this.scopedName         = name;
        this.resourceReferences = singleton(reference, Citation.class);
        this.resources          = singleton(resource, DataIdentification.class);
        this.operation          = operation;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(CoupledResource)
     */
    public DefaultCoupledResource(final DefaultCoupledResource object) {
        super(object);
        if (object != null) {
            this.scopedName         = object.getScopedName();
            this.resourceReferences = copyCollection(object.getResourceReferences(), Citation.class);
            this.resources          = copyCollection(object.getResources(), DataIdentification.class);
            this.operation          = object.getOperation();
        }
    }

    /**
     * Returns scoped identifier of the resource in the context of the given service instance.
     *
     * @return identifier of the resource, or {@code null} if none.
     */
    @XmlElementRef
    @XmlJavaTypeAdapter(GO_ScopedName.class)
    @UML(identifier="scopedName", obligation=OPTIONAL, specification=ISO_19115)
    public ScopedName getScopedName() {
        return scopedName;
    }

    /**
     * Sets the identifier of the resource in the context of the given service instance.
     *
     * @param newValue The new identifier of the resource.
     */
    public void setScopedName(final ScopedName newValue) {
        checkWritePermission();
        this.scopedName = newValue;
    }

    /**
     * Returns references to the resource on which the services operates.
     *
     * @return References to the resource on which the services operates.
     */
/// @XmlElement(name = "resourceReference", namespace = Namespaces.SRV)
    @UML(identifier="resourceReference", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getResourceReferences() {
        return resourceReferences = nonNullCollection(resourceReferences, Citation.class);
    }

    /**
     * Sets references to the resource on which the services operates.
     *
     * @param newValues The new references to the resource on which the services operates.
     */
    public void setResourceReferences(final Collection<? extends Citation> newValues) {
        resourceReferences = writeCollection(newValues, resourceReferences, Citation.class);
    }

    /**
     * Returns the tightly coupled resources.
     *
     * @return tightly coupled resources.
     */
/// @XmlElement(name = "resource", namespace = Namespaces.SRV)
    @UML(identifier="resource", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DataIdentification> getResources() {
        return resources = nonNullCollection(resources, DataIdentification.class);
    }

    /**
     * Sets the tightly coupled resources.
     *
     * @param newValues The new tightly coupled resources.
     */
    public void setResources(final Collection<? extends DataIdentification> newValues) {
        resources = writeCollection(newValues, resources, DataIdentification.class);
    }

    /**
     * Returns the service operation.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The return type will be changed to the {@code OperationMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return The service operation, or {@code null} if none.
     */
/// @XmlElement(name = "operation", namespace = Namespaces.SRV)
    @UML(identifier="operation", obligation=OPTIONAL, specification=ISO_19115)
    public DefaultOperationMetadata getOperation() {
        return operation;
    }

    /**
     * Sets a new service operation.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The argument type will be changed to the {@code OperationMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValue The new service operation.
     */
    public void setOperation(final DefaultOperationMetadata newValue) {
        checkWritePermission();
        this.operation = newValue;
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
     * For JAXB marhalling of ISO 19119 document only.
     */
    @XmlElement(name = "operationName", namespace = Namespaces.SRV)
    private String getOperationName() {
        if (LEGACY_XML) {
            final DefaultOperationMetadata operation = getOperation();
            if (operation != null) {
                return operation.getOperationName();
            }
        }
        return null;
    }

    /**
     * For JAXB unmarhalling of ISO 19119 document only. Sets {@link #operation} to a temporary
     * {@link OperationName} placeholder. That temporary instance will be replaced by the real
     * one when the enclosing {@link DefaultServiceIdentification} is unmarshalled.
     */
    private void setOperationName(final String name) {
        if (operation == null) {
            operation = new OperationName(name);
        }
    }

    /**
     * Returns the resource identifier, which is assumed to be the name as a string.
     */
    @XmlElement(name = "identifier", namespace = Namespaces.SRV)
    private String getIdentifier() {
        if (LEGACY_XML) {
            final ScopedName name = getScopedName();
            if (name != null) {
                return name.tip().toString();
            }
        }
        return null;
    }
}
