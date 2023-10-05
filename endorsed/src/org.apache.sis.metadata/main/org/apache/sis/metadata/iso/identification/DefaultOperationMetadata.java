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
import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.Namespaces;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.identification.DistributedComputingPlatform;
import org.opengis.metadata.identification.OperationMetadata;


/**
 * Parameter information.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code SV_OperationMetadata}
 * {@code   ├─operationName……………………………………………} A unique identifier for this interface.
 * {@code   ├─distributedComputingPlatform……} Distributed computing platforms on which the operation has been implemented.
 * {@code   └─connectPoint………………………………………………} Handle for accessing the service interface.
 * {@code       └─linkage…………………………………………………} Location for on-line access using a URL address or similar addressing scheme.</div>
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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.5
 */
@TitleProperty(name = "operationName")
@XmlType(name = "SV_OperationMetadata_Type", namespace = Namespaces.SRV, propOrder = {
    "operationName",
    "distributedComputingPlatforms",    // Singular form used in ISO 19115:2014, was "DCP" in ISO 19115:2003.
    "operationDescription",
    "invocationName",
    "connectPoints",                    // Was after "parameters" in ISO 19115:2003.
    "parameters",                       // Actually "parameter" in ISO 19115:2014, was "parameters" in ISO 19115:2003.
    "dependsOn"
})
@XmlRootElement(name = "SV_OperationMetadata", namespace = Namespaces.SRV)
public class DefaultOperationMetadata extends ISOMetadata implements OperationMetadata {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -6120853428175790473L;

    /**
     * An unique identifier for this interface.
     */
    private String operationName;

    /**
     * Distributed computing platforms on which the operation has been implemented.
     */
    @SuppressWarnings("serial")
    private Collection<DistributedComputingPlatform> distributedComputingPlatforms;

    /**
     * Free text description of the intent of the operation and the results of the operation.
     */
    @SuppressWarnings("serial")
    private InternationalString operationDescription;

    /**
     * The name used to invoke this interface within the context of the DCP.
     */
    @SuppressWarnings("serial")
    private InternationalString invocationName;

    /**
     * Handle for accessing the service interface.
     */
    @SuppressWarnings("serial")
    private Collection<OnlineResource> connectPoints;

    /**
     * The parameters that are required for this interface.
     */
    @SuppressWarnings("serial")
    private Collection<ParameterDescriptor<?>> parameters;

    /**
     * List of operation that must be completed immediately.
     */
    @SuppressWarnings("serial")
    private List<OperationMetadata> dependsOn;

    /**
     * Constructs an initially empty operation metadata.
     */
    public DefaultOperationMetadata() {
    }

    /**
     * Constructs a new operation metadata initialized to the specified values.
     *
     * @param operationName  an unique identifier for this interface.
     * @param platform       distributed computing platforms on which the operation has been implemented.
     * @param connectPoint   handle for accessing the service interface.
     */
    public DefaultOperationMetadata(final String operationName,
                                    final DistributedComputingPlatform platform,
                                    final OnlineResource connectPoint)
    {
        this.operationName                 = operationName;
        this.distributedComputingPlatforms = singleton(platform, DistributedComputingPlatform.class);
        this.connectPoints                 = singleton(connectPoint, OnlineResource.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(OperationMetadata)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultOperationMetadata(final OperationMetadata object) {
        super(object);
        if (object != null) {
            this.operationName                 = object.getOperationName();
            this.distributedComputingPlatforms = copyCollection(object.getDistributedComputingPlatforms(), DistributedComputingPlatform.class);
            this.operationDescription          = object.getOperationDescription();
            this.invocationName                = object.getInvocationName();
            this.connectPoints                 = copyCollection(object.getConnectPoints(), OnlineResource.class);
            this.parameters                    = copySet(object.getParameters(), (Class) ParameterDescriptor.class);
            this.dependsOn                     = copyList(object.getDependsOn(), OperationMetadata.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultOperationMetadata}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultOperationMetadata} instance is created using the
     *       {@linkplain #DefaultOperationMetadata(OperationMetadata) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultOperationMetadata castOrCopy(final OperationMetadata object) {
        if (object == null || object instanceof DefaultOperationMetadata) {
            return (DefaultOperationMetadata) object;
        }
        return new DefaultOperationMetadata(object);
    }

    /**
     * Returns an unique identifier for this interface.
     *
     * @return an unique identifier for this interface.
     */
    @Override
    @XmlElement(name = "operationName", required = true)
    public String getOperationName() {
        return operationName;
    }

    /**
     * Sets the unique identifier for this interface.
     *
     * @param  newValue  the new unique identifier for this interface.
     */
    public void setOperationName(final String newValue) {
        checkWritePermission(operationName);
        operationName = newValue;
    }

    /**
     * Returns the distributed computing platforms (DCPs) on which the operation has been implemented.
     *
     * @return distributed computing platforms on which the operation has been implemented.
     */
    @Override
    @XmlElement(name = "distributedComputingPlatform", required = true)
    public Collection<DistributedComputingPlatform> getDistributedComputingPlatforms() {
        return distributedComputingPlatforms = nonNullCollection(distributedComputingPlatforms, DistributedComputingPlatform.class);
    }

    /**
     * Sets the distributed computing platforms on which the operation has been implemented.
     *
     * @param  newValues  the new distributed computing platforms on which the operation has been implemented.
     */
    public void setDistributedComputingPlatforms(final Collection<? extends DistributedComputingPlatform> newValues) {
        distributedComputingPlatforms = writeCollection(newValues, distributedComputingPlatforms, DistributedComputingPlatform.class);
    }

    /**
     * Returns free text description of the intent of the operation and the results of the operation.
     *
     * @return free text description of the intent of the operation and the results of the operation, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "operationDescription")
    public InternationalString getOperationDescription() {
        return operationDescription;
    }

    /**
     * Sets free text description of the intent of the operation and the results of the operation.
     *
     * @param  newValue  the new free text description of the intent of the operation and the results of the operation.
     */
    public void setOperationDescription(final InternationalString newValue) {
        checkWritePermission(operationDescription);
        operationDescription = newValue;
    }

    /**
     * Returns the name used to invoke this interface within the context of the DCP.
     *
     * @return the name used to invoke this interface within the context of the distributed computing platforms,
     *         or {@code null} if none.
     */
    @Override
    @XmlElement(name = "invocationName")
    public InternationalString getInvocationName() {
        return invocationName;
    }

    /**
     * Sets the name used to invoke this interface within the context of the DCP.
     *
     * @param  newValue  the new name used to invoke this interface within the context of the DCP.
     */
    public void setInvocationName(final InternationalString newValue) {
        checkWritePermission(invocationName);
        invocationName = newValue;
    }

    /**
     * Returns the handle for accessing the service interface.
     *
     * @return handle for accessing the service interface.
     */
    @Override
    @XmlElement(name = "connectPoint", required = true)
    public Collection<OnlineResource> getConnectPoints() {
        return connectPoints = nonNullCollection(connectPoints, OnlineResource.class);
    }

    /**
     * Sets the handle for accessing the service interface.
     *
     * @param  newValue  the new handle for accessing the service interface.
     */
    public void setConnectPoints(final Collection<? extends OnlineResource> newValue) {
        connectPoints = writeCollection(newValue, connectPoints, OnlineResource.class);
    }

    /**
     * Returns the parameters that are required for this interface.
     *
     * <h4>Unified parameter API</h4>
     * In GeoAPI, the {@code SV_Parameter} type defined by ISO 19115 is replaced by {@code ParameterDescriptor}
     * in order to provide a single parameter API. See {@link org.opengis.parameter} for more information.
     *
     * @return the parameters that are required for this interface, or an empty collection if none.
     */
    @Override
    @XmlElement(name = "parameter")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Collection<ParameterDescriptor<?>> getParameters() {
        return parameters = nonNullCollection(parameters, (Class) ParameterDescriptor.class);
    }

    /**
     * Sets the parameters that are required for this interface.
     *
     * @param  newValues  the new set of parameters that are required for this interface.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setParameters(final Collection<? extends ParameterDescriptor<?>> newValues) {
        parameters = writeCollection(newValues, parameters, (Class) ParameterDescriptor.class);
    }

    /**
     * Returns the list of operation that must be completed immediately before current operation is invoked.
     *
     * @return list of operation that must be completed immediately, or an empty list if none.
     */
    @Override
    @XmlElement(name = "dependsOn")
    public List<OperationMetadata> getDependsOn() {
        return dependsOn = nonNullList(dependsOn, OperationMetadata.class);
    }

    /**
     * Sets the list of operation that must be completed before current operation is invoked.
     *
     * @param  newValues  the new list of operation.
     */
    public void setDependsOn(final List<? extends OperationMetadata> newValues) {
        dependsOn = writeList(newValues, dependsOn, OperationMetadata.class);
    }
}