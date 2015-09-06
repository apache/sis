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
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.annotation.UML;
import org.opengis.util.CodeList;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.distribution.StandardOrderProcess;
import org.opengis.metadata.identification.ServiceIdentification;
import org.apache.sis.internal.jaxb.code.SV_CouplingType;
import org.apache.sis.xml.Namespaces;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Identification of capabilities which a service provider makes available to a service user
 * through a set of interfaces that define a behaviour.
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
@XmlRootElement(name = "SV_ServiceIdentification", namespace = Namespaces.SRV)
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
    private CodeList<?> couplingType;

    /**
     * Further description of the data coupling in the case of tightly coupled services.
     */
    private Collection<DefaultCoupledResource> coupledResources;

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
    private Collection<DefaultOperationMetadata> containsOperations;

    /**
     * Information on the resources that the service operates on.
     */
    private Collection<DataIdentification> operatesOn;

    /**
     * Information about the chain applied by the service.
     */
    private Collection<DefaultOperationChainMetadata> containsChain;

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
        if (object instanceof DefaultServiceIdentification) {
            final DefaultServiceIdentification c = (DefaultServiceIdentification) object;
            serviceType         = c.getServiceType();
            serviceTypeVersions = copyCollection(c.getServiceTypeVersions(), String.class);
            accessProperties    = c.getAccessProperties();
            couplingType        = c.getCouplingType();
            coupledResources    = copyCollection(c.getCoupledResources(), DefaultCoupledResource.class);
            operatedDatasets    = copyCollection(c.getOperatedDatasets(), Citation.class);
            profiles            = copyCollection(c.getProfiles(), Citation.class);
            serviceStandards    = copyCollection(c.getServiceStandards(), Citation.class);
            containsOperations  = copyCollection(c.getContainsOperations(), DefaultOperationMetadata.class);
            operatesOn          = copyCollection(c.getOperatesOn(), DataIdentification.class);
            containsChain       = copyCollection(c.getContainsChain(), DefaultOperationChainMetadata.class);
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
    @XmlElement(name = "serviceType", namespace = Namespaces.SRV, required = true)
    @UML(identifier="serviceType", obligation=MANDATORY, specification=ISO_19115)
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
    @XmlElement(name = "serviceTypeVersion", namespace = Namespaces.SRV)
    @UML(identifier="serviceTypeVersion", obligation=OPTIONAL, specification=ISO_19115)
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
     *
     * @since 0.5
     */
/// @XmlElement(name = "accessProperties", namespace = Namespaces.SRV)
    @UML(identifier="accessProperties", obligation=OPTIONAL, specification=ISO_19115)
    public StandardOrderProcess getAccessProperties() {
        return accessProperties;

    }

    /**
     * Sets information about the availability of the service.
     *
     * @param newValue The new information about the availability of the service.
     *
     * @since 0.5
     */
    public void setAccessProperties(final StandardOrderProcess newValue) {
        checkWritePermission();
        accessProperties = newValue;
    }

    /**
     * Returns type of coupling between service and associated data (if exist).
     *
     * <div class="warning"><b>Upcoming API change — specialization</b><br>
     * The return type will be changed to the {@code CouplingType} code list
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Type of coupling between service and associated data, or {@code null} if none.
     */
    @XmlJavaTypeAdapter(SV_CouplingType.class)
    @XmlElement(name = "couplingType", namespace = Namespaces.SRV)
    @UML(identifier="couplingType", obligation=CONDITIONAL, specification=ISO_19115)
    public CodeList<?> getCouplingType() {
        return couplingType;
    }

    /**
     * Sets the type of coupling between service and associated data.
     *
     * <div class="warning"><b>Upcoming API change — specialization</b><br>
     * The argument type will be changed to the {@code CouplingType} code list when GeoAPI will provide it
     * (tentatively in GeoAPI 3.1). In the meantime, users can define their own code list class as below:
     *
     * {@preformat java
     *   final class UnsupportedCodeList extends CodeList<UnsupportedCodeList> {
     *       private static final List<UnsupportedCodeList> VALUES = new ArrayList<UnsupportedCodeList>();
     *
     *       // Need to declare at least one code list element.
     *       public static final UnsupportedCodeList MY_CODE_LIST = new UnsupportedCodeList("MY_CODE_LIST");
     *
     *       private UnsupportedCodeList(String name) {
     *           super(name, VALUES);
     *       }
     *
     *       public static UnsupportedCodeList valueOf(String code) {
     *           return valueOf(UnsupportedCodeList.class, code);
     *       }
     *
     *       &#64;Override
     *       public UnsupportedCodeList[] family() {
     *           synchronized (VALUES) {
     *               return VALUES.toArray(new UnsupportedCodeList[VALUES.size()]);
     *           }
     *       }
     *   }
     * }
     * </div>
     *
     * @param newValue The new type of coupling between service and associated data.
     */
    public void setCouplingType(final CodeList<?> newValue) {
        checkWritePermission();
        couplingType = newValue;
    }

    /**
     * Returns further description(s) of the data coupling in the case of tightly coupled services.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code CoupledResource} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Further description(s) of the data coupling in the case of tightly coupled services.
     */
    @XmlElement(name = "coupledResource", namespace = Namespaces.SRV)
    @UML(identifier="coupledResource", obligation=CONDITIONAL, specification=ISO_19115)
    public Collection<DefaultCoupledResource> getCoupledResources() {
        return coupledResources = nonNullCollection(coupledResources, DefaultCoupledResource.class);
    }

    /**
     * Sets further description(s) of the data coupling in the case of tightly coupled services.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code CoupledResource} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new further description(s) of the data coupling.
     */
    public void setCoupledResources(final Collection<? extends DefaultCoupledResource> newValues) {
        coupledResources = writeCollection(newValues, coupledResources, DefaultCoupledResource.class);
    }

    /**
     * Returns the reference(s) to the resource on which the service operates.
     *
     * @return Reference(s) to the resource on which the service operates.
     *
     * @since 0.5
     */
/// @XmlElement(name = "operatedDataset", namespace = Namespaces.SRV)
    @UML(identifier="operatedDataset", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getOperatedDatasets() {
        return operatedDatasets = nonNullCollection(operatedDatasets, Citation.class);
    }

    /**
     * Sets the reference(s) to the resource on which the service operates.
     *
     * @param newValues The new reference(s) to the resource on which the service operates.
     *
     * @since 0.5
     */
    public void setOperatedDatasets(final Collection<? extends Citation> newValues) {
        operatedDatasets = writeCollection(newValues, operatedDatasets, Citation.class);
    }

    /**
     * Returns the profile(s) to which the service adheres.
     *
     * @return Profile(s) to which the service adheres.
     *
     * @since 0.5
     */
/// @XmlElement(name = "profile", namespace = Namespaces.SRV)
    @UML(identifier="profile", obligation=OPTIONAL, specification=ISO_19115)
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
     *
     * @since 0.5
     */
/// @XmlElement(name = "serviceStandard", namespace = Namespaces.SRV)
    @UML(identifier="serviceStandard", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getServiceStandards() {
        return serviceStandards = nonNullCollection(serviceStandards, Citation.class);
    }

    /**
     * Sets the standard(s) to which the service adheres.
     *
     * @param newValues The new standard(s) to which the service adheres.
     *
     * @since 0.5
     */
    public void setServiceStandards(final Collection<? extends Citation> newValues) {
        serviceStandards = writeCollection(newValues, serviceStandards, Citation.class);
    }

    /**
     * Provides information about the operations that comprise the service.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code OperationMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Information about the operations that comprise the service.
     */
    @XmlElement(name = "containsOperations", namespace = Namespaces.SRV)
    @UML(identifier="containsOperations", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DefaultOperationMetadata> getContainsOperations() {
        return containsOperations = nonNullCollection(containsOperations, DefaultOperationMetadata.class);
    }

    /**
     * Sets information(s) about the operations that comprise the service.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code OperationMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new information(s) about the operations that comprise the service.
     */
    public void setContainsOperations(final Collection<? extends DefaultOperationMetadata> newValues) {
        containsOperations = writeCollection(newValues, containsOperations, DefaultOperationMetadata.class);
    }

    /**
     * Provides information on the resources that the service operates on.
     *
     * @return Information on the resources that the service operates on.
     */
    @XmlElement(name = "operatesOn", namespace = Namespaces.SRV)
    @UML(identifier="operatesOn", obligation=OPTIONAL, specification=ISO_19115)
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
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code OperationChainMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Information about the chain applied by the service.
     *
     * @since 0.5
     */
/// @XmlElement(name = "containsChain", namespace = Namespaces.SRV)
    @UML(identifier="containsChain", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DefaultOperationChainMetadata> getContainsChain() {
        return containsChain = nonNullCollection(containsChain, DefaultOperationChainMetadata.class);
    }

    /**
     * Sets the information about the chain applied by the service.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code OperationChainMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new information about the chain applied by the service.
     *
     * @since 0.5
     */
    public void setContainsChain(final Collection<? extends DefaultOperationChainMetadata>  newValues) {
        containsChain = writeCollection(newValues, containsChain, DefaultOperationChainMetadata.class);
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
     * Invoked after JAXB has unmarshalled this object.
     */
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if (containsOperations != null && coupledResources != null) {
            OperationName.resolve(containsOperations, coupledResources);
        }
    }
}
