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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.identification.DistributedComputingPlatform;
import org.opengis.metadata.identification.OperationMetadata;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.xml.Namespaces;


/**
 * Parameter information.
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
@XmlType(name = "SV_OperationMetadata_Type", namespace = Namespaces.SRV, propOrder = {
    "operationName",
    "distributedComputingPlatforms",
    "operationDescription",
    "invocationName",
    "parameters",
    "connectPoints",
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
    private Collection<DistributedComputingPlatform> distributedComputingPlatforms;

    /**
     * Free text description of the intent of the operation and the results of the operation.
     */
    private InternationalString operationDescription;

    /**
     * The name used to invoke this interface within the context of the DCP.
     */
    private InternationalString invocationName;

    /**
     * Handle for accessing the service interface.
     */
    private Collection<OnlineResource> connectPoints;

    /**
     * The parameters that are required for this interface.
     */
    private Collection<ParameterDescriptor<?>> parameters;

    /**
     * List of operation that must be completed immediately.
     */
    private List<OperationMetadata> dependsOn;

    /**
     * Constructs an initially empty operation metadata.
     */
    public DefaultOperationMetadata() {
    }

    /**
     * Constructs a new operation metadata initialized to the specified values.
     *
     * @param operationName An unique identifier for this interface.
     * @param platform      Distributed computing platforms on which the operation has been implemented.
     * @param connectPoint  Handle for accessing the service interface.
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
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(OperationMetadata)
     */
    @SuppressWarnings("unchecked")
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
     *       {@linkplain #DefaultOperationMetadata(OperationMetadata) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return An unique identifier for this interface.
     */
    @Override
    @XmlElement(name = "operationName", namespace = Namespaces.SRV, required = true)
    public String getOperationName() {
        return operationName;
    }

    /**
     * Set the unique identifier for this interface.
     *
     * @param newValue The new unique identifier for this interface.
     */
    public void setOperationName(final String newValue) {
        checkWritePermission();
        operationName = newValue;
    }

    /**
     * Returns the distributed computing platforms (DCPs) on which the operation has been implemented.
     *
     * @return Distributed computing platforms on which the operation has been implemented.
     */
    @Override
    @XmlElement(name = "DCP", namespace = Namespaces.SRV, required = true)
    public Collection<DistributedComputingPlatform> getDistributedComputingPlatforms() {
        return distributedComputingPlatforms = nonNullCollection(distributedComputingPlatforms, DistributedComputingPlatform.class);
    }

    /**
     * Sets the distributed computing platforms on which the operation has been implemented.
     *
     * @param newValues The new distributed computing platforms on which the operation has been implemented.
     */
    public void setDistributedComputingPlatforms(final Collection<? extends DistributedComputingPlatform> newValues) {
        distributedComputingPlatforms = writeCollection(newValues, distributedComputingPlatforms, DistributedComputingPlatform.class);
    }

    /**
     * Returns free text description of the intent of the operation and the results of the operation.
     *
     * @return Free text description of the intent of the operation and the results of the operation, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "operationDescription", namespace = Namespaces.SRV)
    public InternationalString getOperationDescription() {
        return operationDescription;
    }

    /**
     * Sets free text description of the intent of the operation and the results of the operation.
     *
     * @param newValue The new free text description of the intent of the operation and the results of the operation.
     */
    public void setOperationDescription(final InternationalString newValue) {
        checkWritePermission();
        operationDescription = newValue;
    }

    /**
     * Returns the name used to invoke this interface within the context of the DCP.
     *
     * @return The name used to invoke this interface within the context of the distributed computing platforms,
     *         or {@code null} if none.
     */
    @Override
    @XmlElement(name = "invocationName", namespace = Namespaces.SRV)
    public InternationalString getInvocationName() {
        return invocationName;
    }

    /**
     * Sets the name used to invoke this interface within the context of the DCP.
     *
     * @param newValue The new name used to invoke this interface within the context of the DCP.
     */
    public void setInvocationName(final InternationalString newValue) {
        checkWritePermission();
        invocationName = newValue;
    }

    /**
     * Returns the handle for accessing the service interface.
     *
     * @return Handle for accessing the service interface.
     */
    @Override
    @XmlElement(name = "connectPoint", namespace = Namespaces.SRV, required = true)
    public Collection<OnlineResource> getConnectPoints() {
        return connectPoints = nonNullCollection(connectPoints, OnlineResource.class);
    }

    /**
     * Sets the handle for accessing the service interface.
     *
     * @param newValue The new handle for accessing the service interface.
     */
    public void setConnectPoints(final Collection<? extends OnlineResource> newValue) {
        connectPoints = writeCollection(newValue, connectPoints, OnlineResource.class);
    }

    /**
     * Returns the parameters that are required for this interface.
     *
     * @return The parameters that are required for this interface, or an empty collection if none.
     */
    @Override
    @SuppressWarnings("unchecked")
    @XmlElement(name = "parameters", namespace = Namespaces.SRV)
    public Collection<ParameterDescriptor<?>> getParameters() {
        return parameters = nonNullCollection(parameters, (Class) ParameterDescriptor.class);
    }

    /**
     * Sets the parameters that are required for this interface.
     *
     * @param newValues The new set of parameters that are required for this interface.
     */
    @SuppressWarnings("unchecked")
    public void setParameters(final Collection<? extends ParameterDescriptor<?>> newValues) {
        parameters = writeCollection(newValues, parameters, (Class) ParameterDescriptor.class);
    }

    /**
     * Returns the list of operation that must be completed immediately before current operation is invoked.
     *
     * @return List of operation that must be completed immediately, or an empty list if none.
     */
    @Override
    @XmlElement(name = "dependsOn", namespace = Namespaces.SRV)
    public List<OperationMetadata> getDependsOn() {
        return dependsOn = nonNullList(dependsOn, OperationMetadata.class);
    }

    /**
     * Set the list of operation that must be completed before current operation is invoked.
     *
     * @param newValues The new list of operation.
     */
    public void setDependsOn(final List<? extends OperationMetadata> newValues) {
        dependsOn = writeList(newValues, dependsOn, OperationMetadata.class);
    }
}
