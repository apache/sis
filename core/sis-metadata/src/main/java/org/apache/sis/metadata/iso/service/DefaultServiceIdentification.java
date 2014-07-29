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
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.distribution.StandardOrderProcess;
import org.opengis.metadata.service.ServiceIdentification;
import org.opengis.metadata.service.CoupledResource;
import org.opengis.metadata.service.CouplingType;
import org.opengis.metadata.service.OperationChainMetadata;
import org.opengis.metadata.service.OperationMetadata;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;


/**
 * Identification of capabilities which a service provider makes available to a service user
 * through a set of interfaces that define a behaviour.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_ServiceIdentification_Type", propOrder = { // ISO 19139 still use the old prefix.
    "serviceType",
    "serviceTypeVersions",
/// "accessProperties",
    "coupledResources",
    "couplingType",
/// "operatedDatasets",
/// "profiles",
/// "serviceStandards",
    "containsOperations",
    "operatesOn",
/// "containsChain"
})
@XmlRootElement(name = "SV_ServiceIdentification")
public class DefaultServiceIdentification extends AbstractIdentification implements ServiceIdentification {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 7700836694236616300L;

    /**
     * A service type name.
     */
    private GenericName serviceType;

    /**
     * The version of the service, supports searching based on the version of serviceType.
     */
    private Collection<String> serviceTypeVersions;

    /**
     * Information about the availability of the service.
     */
    private StandardOrderProcess accessProperties;

    /**
     * Type of coupling between service and associated data (if exist).
     */
    private CouplingType couplingType;

    /**
     * Further description of the data coupling in the case of tightly coupled services.
     */
    private Collection<CoupledResource> coupledResources;

    /**
     * References to the resource on which the service operates.
     */
    private Collection<Citation> operatedDatasets;

    /**
     * Profiles to which the service adheres.
     */
    private Collection<Citation> profiles;

    /**
     * Standards to which the service adheres.
     */
    private Collection<Citation> serviceStandards;

    /**
     * Information about the operations that comprise the service.
     */
    private Collection<OperationMetadata> containsOperations;

    /**
     * Information on the resources that the service operates on.
     */
    private Collection<DataIdentification> operatesOn;

    /**
     * Information about the chain applied by the service.
     */
    private Collection<OperationChainMetadata> containsChain;

    /**
     * Constructs an initially empty service identification.
     */
    public DefaultServiceIdentification() {
    }

    /**
     * Constructs a service identification initialized to the specified values.
     *
     * @param serviceType Service type name.
     * @param citation    Citation data for the resource(s).
     * @param abstracts   Brief narrative summary of the content of the resource(s).
     */
    public DefaultServiceIdentification(final GenericName  serviceType,
                                        final Citation     citation,
                                        final CharSequence abstracts)
    {
        super(citation, abstracts);
        this.serviceType = serviceType;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ServiceIdentification)
     */
    public DefaultServiceIdentification(final ServiceIdentification object) {
        super(object);
        if (object != null) {
            serviceType         = object.getServiceType();
            serviceTypeVersions = copyCollection(object.getServiceTypeVersions(), String.class);
            accessProperties    = object.getAccessProperties();
            couplingType        = object.getCouplingType();
            coupledResources    = copyCollection(object.getCoupledResources(), CoupledResource.class);
            operatedDatasets    = copyCollection(object.getOperatedDatasets(), Citation.class);
            profiles            = copyCollection(object.getProfiles(), Citation.class);
            serviceStandards    = copyCollection(object.getServiceStandards(), Citation.class);
            containsOperations  = copyCollection(object.getContainsOperations(), OperationMetadata.class);
            operatesOn          = copyCollection(object.getOperatesOn(), DataIdentification.class);
            containsChain       = copyCollection(object.getContainsChain(), OperationChainMetadata.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultServiceIdentification}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultServiceIdentification} instance is created using the
     *       {@linkplain #DefaultServiceIdentification(ServiceIdentification) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultServiceIdentification castOrCopy(final ServiceIdentification object) {
        if (object == null || object instanceof DefaultServiceIdentification) {
            return (DefaultServiceIdentification) object;
        }
        return new DefaultServiceIdentification(object);
    }

    /**
     * Returns a service type name.
     *
     * <div class="note"><b>Examples:</b> "discovery", "view", "download", "transformation", or "invoke"</div>
     *
     * @return A service type name.
     */
    @Override
    @XmlElement(name = "serviceType", required = true)
    public GenericName getServiceType() {
        return serviceType;
    }

    /**
     * Sets the service type name.
     *
     * @param newValue The new service type name.
     */
    public void setServiceType(final GenericName newValue) {
        checkWritePermission();
        serviceType = newValue;
    }

    /**
     * Returns the versions of the service.
     *
     * @return The versions of the service.
     */
    @Override
    @XmlElement(name = "serviceTypeVersion")
    public Collection<String> getServiceTypeVersions() {
        return serviceTypeVersions = nonNullCollection(serviceTypeVersions, String.class);
    }

    /**
     * Sets the versions of the service.
     *
     * @param newValues The new versions of the service.
     */
    public void setServiceTypeVersions(final Collection<? extends String> newValues) {
        serviceTypeVersions = writeCollection(newValues, serviceTypeVersions, String.class);
    }

    /**
     * Returns information about the availability of the service.
     *
     * @return Information about the availability of the service, or {@code null} if none.
     */
    @Override
/// @XmlElement(name = "accessProperties")
    public StandardOrderProcess getAccessProperties() {
        return accessProperties;

    }

    /**
     * Sets information about the availability of the service.
     *
     * @param newValue The new information about the availability of the service.
     */
    public void setAccessProperties(final StandardOrderProcess newValue) {
        checkWritePermission();
        accessProperties = newValue;
    }

    /**
     * Returns type of coupling between service and associated data (if exist).
     *
     * @return Type of coupling between service and associated data, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "couplingType")
    public CouplingType getCouplingType() {
        return couplingType;
    }

    /**
     * Sets the type of coupling between service and associated data.
     *
     * @param newValue The new type of coupling between service and associated data.
     */
    public void setCouplingType(final CouplingType newValue) {
        checkWritePermission();
        couplingType = newValue;
    }

    /**
     * Returns further description(s) of the data coupling in the case of tightly coupled services.
     *
     * @return Further description(s) of the data coupling in the case of tightly coupled services.
     */
    @Override
    @XmlElement(name = "coupledResource")
    public Collection<CoupledResource> getCoupledResources() {
        return coupledResources = nonNullCollection(coupledResources, CoupledResource.class);
    }

    /**
     * Sets further description(s) of the data coupling in the case of tightly coupled services.
     *
     * @param newValues The new further description(s) of the data coupling.
     */
    public void setCoupledResources(final Collection<? extends CoupledResource> newValues) {
        coupledResources = writeCollection(newValues, coupledResources, CoupledResource.class);
    }

    /**
     * Returns the reference(s) to the resource on which the service operates.
     *
     * @return Reference(s) to the resource on which the service operates.
     */
    @Override
/// @XmlElement(name = "operatedDataset")
    public Collection<Citation> getOperatedDatasets() {
        return operatedDatasets = nonNullCollection(operatedDatasets, Citation.class);
    }

    /**
     * Sets the reference(s) to the resource on which the service operates.
     *
     * @param newValues The new reference(s) to the resource on which the service operates.
     */
    public void setOperatedDatasets(final Collection<? extends Citation> newValues) {
        operatedDatasets = writeCollection(newValues, operatedDatasets, Citation.class);
    }

    /**
     * Returns the profile(s) to which the service adheres.
     *
     * @return Profile(s) to which the service adheres.
     */
    @Override
/// @XmlElement(name = "profile")
    public Collection<Citation> getProfiles() {
        return profiles = nonNullCollection(profiles, Citation.class);
    }

    /**
     * Sets the profile(s) to which the service adheres.
     *
     * @param newValues The new profile(s) to which the service adheres.
     */
    public void setProfiles(final Collection<? extends Citation> newValues) {
        profiles = writeCollection(newValues, profiles, Citation.class);
    }

    /**
     * Returns the standard(s) to which the service adheres.
     *
     * @return Standard(s) to which the service adheres.
     */
    @Override
/// @XmlElement(name = "serviceStandard")
    public Collection<Citation> getServiceStandards() {
        return serviceStandards = nonNullCollection(serviceStandards, Citation.class);
    }

    /**
     * Sets the standard(s) to which the service adheres.
     *
     * @param newValues The new standard(s) to which the service adheres.
     */
    public void setServiceStandards(final Collection<? extends Citation> newValues) {
        serviceStandards = writeCollection(newValues, serviceStandards, Citation.class);
    }

    /**
     * Provides information about the operations that comprise the service.
     *
     * @return Information about the operations that comprise the service.
     */
    @Override
    @XmlElement(name = "containsOperations")
    public Collection<OperationMetadata> getContainsOperations() {
        return containsOperations = nonNullCollection(containsOperations, OperationMetadata.class);
    }

    /**
     * Sets information(s) about the operations that comprise the service.
     *
     * @param newValues The new information(s) about the operations that comprise the service.
     */
    public void setContainsOperations(final Collection<? extends OperationMetadata> newValues) {
        containsOperations = writeCollection(newValues, containsOperations, OperationMetadata.class);
    }

    /**
     * Provides information on the resources that the service operates on.
     *
     * @return Information on the resources that the service operates on.
     */
    @Override
    @XmlElement(name = "operatesOn")
    public Collection<DataIdentification> getOperatesOn() {
        return operatesOn = nonNullCollection(operatesOn, DataIdentification.class);
    }

    /**
     * Sets the information on the resources that the service operates on.
     *
     * @param newValues The new information on the resources that the service operates on.
     */
    public void setOperatesOn(final Collection<? extends DataIdentification> newValues) {
        operatesOn = writeCollection(newValues, operatesOn, DataIdentification.class);
    }

    /**
     * Provides information about the chain applied by the service.
     *
     * @return Information about the chain applied by the service.
     */
    @Override
/// @XmlElement(name = "containsChain")
    public Collection<OperationChainMetadata> getContainsChain() {
        return containsChain = nonNullCollection(containsChain, OperationChainMetadata.class);
    }

    /**
     * Sets the information about the chain applied by the service.
     *
     * @param newValues The new information about the chain applied by the service.
     */
    public void setContainsChain(final Collection<? extends OperationChainMetadata>  newValues) {
        containsChain = writeCollection(newValues, containsChain, OperationChainMetadata.class);
    }

    /**
     * Invoked after JAXB has unmarshalled this object.
     */
    private void afterUnmarshal(final Unmarshaller u, final Object parent) {
        if (containsOperations != null && coupledResources != null) {
            OperationName.resolve(containsOperations, coupledResources);
        }
    }
}
