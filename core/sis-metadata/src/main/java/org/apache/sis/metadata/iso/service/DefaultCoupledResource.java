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
package org.apache.sis.metadata.iso.service;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.ScopedName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.service.CoupledResource;
import org.opengis.metadata.service.OperationMetadata;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Links a given operation name with a resource identified by an "identifier".
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
@XmlType(name = "SV_CoupledResource_Type", propOrder = {
    "scopedName",
    "resourceReferences",
    "resources",
    "operation"
})
@XmlRootElement(name = "SV_CoupledResource")
public class DefaultCoupledResource extends ISOMetadata implements CoupledResource {
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
    private OperationMetadata operation;

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
                                  final OperationMetadata operation)
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
    public DefaultCoupledResource(final CoupledResource object) {
        super(object);
        if (object != null) {
            this.scopedName         = object.getScopedName();
            this.resourceReferences = copyCollection(object.getResourceReferences(), Citation.class);
            this.resources          = copyCollection(object.getResources(), DataIdentification.class);
            this.operation          = object.getOperation();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultCoupledResource}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultCoupledResource} instance is created using the
     *       {@linkplain #DefaultCoupledResource(CoupledResource) copy constructor} and returned.
     *       Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCoupledResource castOrCopy(final CoupledResource object) {
        if (object == null || object instanceof DefaultCoupledResource) {
            return (DefaultCoupledResource) object;
        }
        return new DefaultCoupledResource(object);
    }

    /**
     * Returns scoped identifier of the resource in the context of the given service instance.
     *
     * @return identifier of the resource, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "scopedName")
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
    @Override
    @XmlElement(name = "resourceReference")
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
    @Override
    @XmlElement(name = "resource")
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
     * @return The service operation, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "operation")
    public OperationMetadata getOperation() {
        return operation;
    }

    /**
     * Sets a new service operation.
     *
     * @param newValue The new service operation.
     */
    public void setOperation(final OperationMetadata newValue) {
        checkWritePermission();
        this.operation = newValue;
    }
}
