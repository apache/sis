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
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.xml.Namespaces;

// Branch-specific imports
import org.opengis.util.CodeList;
import org.opengis.annotation.UML;
import org.apache.sis.internal.jaxb.code.DCPList;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Parameter information.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code OperationMetadata} interface.
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
@UML(identifier="SV_OperationMetadata", specification=ISO_19115)
public class DefaultOperationMetadata extends ISOMetadata {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -3513177609655567627L;

    /**
     * An unique identifier for this interface.
     */
    private String operationName;

    /**
     * Distributed computing platforms on which the operation has been implemented.
     */
    private Collection<CodeList<?>> distributedComputingPlatforms;

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
    private List<DefaultOperationMetadata> dependsOn;

    /**
     * Constructs an initially empty operation metadata.
     */
    public DefaultOperationMetadata() {
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
    public DefaultOperationMetadata(final DefaultOperationMetadata object) {
        super(object);
        if (object != null) {
            this.operationName                 = object.getOperationName();
            this.distributedComputingPlatforms = copyCollection(object.getDistributedComputingPlatforms(), (Class) CodeList.class);
            this.operationDescription          = object.getOperationDescription();
            this.invocationName                = object.getInvocationName();
            this.connectPoints                 = copyCollection(object.getConnectPoints(), OnlineResource.class);
            this.parameters                    = copySet(object.getParameters(), (Class) ParameterDescriptor.class);
            this.dependsOn                     = copyList(object.getDependsOn(), DefaultOperationMetadata.class);
        }
    }

    /**
     * Returns an unique identifier for this interface.
     *
     * @return An unique identifier for this interface.
     */
    @XmlElement(name = "operationName", namespace = Namespaces.SRV, required = true)
    @UML(identifier="operationName", obligation=MANDATORY, specification=ISO_19115)
    public String getOperationName() {
        return operationName;
    }

    /**
     * Sets the unique identifier for this interface.
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
     * <div class="warning"><b>Upcoming API change — specialization</b><br>
     * The element type will be changed to the {@code DistributedComputingPlatform} code list
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return Distributed computing platforms on which the operation has been implemented.
     */
    @XmlJavaTypeAdapter(DCPList.class)
    @XmlElement(name = "DCP", namespace = Namespaces.SRV, required = true)
    @UML(identifier="distributedComputingPlatform", obligation=MANDATORY, specification=ISO_19115)
    public Collection<CodeList<?>> getDistributedComputingPlatforms() {
        return distributedComputingPlatforms = nonNullCollection(distributedComputingPlatforms, (Class) CodeList.class);
    }

    /**
     * Sets the distributed computing platforms on which the operation has been implemented.
     *
     * <div class="warning"><b>Upcoming API change — specialization</b><br>
     * The element type will be changed to the {@code DistributedComputingPlatform} code list when GeoAPI will provide
     * it (tentatively in GeoAPI 3.1). In the meantime, users can define their own code list class as below:
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
     * @param newValues The new distributed computing platforms on which the operation has been implemented.
     */
    public void setDistributedComputingPlatforms(final Collection<? extends CodeList<?>> newValues) {
        distributedComputingPlatforms = writeCollection(newValues, distributedComputingPlatforms, (Class) CodeList.class);
    }

    /**
     * Returns free text description of the intent of the operation and the results of the operation.
     *
     * @return Free text description of the intent of the operation and the results of the operation, or {@code null} if none.
     */
    @XmlElement(name = "operationDescription", namespace = Namespaces.SRV)
    @UML(identifier="operationDescription", obligation=OPTIONAL, specification=ISO_19115)
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
    @XmlElement(name = "invocationName", namespace = Namespaces.SRV)
    @UML(identifier="invocationName", obligation=OPTIONAL, specification=ISO_19115)
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
    @XmlElement(name = "connectPoint", namespace = Namespaces.SRV, required = true)
    @UML(identifier="connectPoint", obligation=MANDATORY, specification=ISO_19115)
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
    @SuppressWarnings("unchecked")
    @XmlElement(name = "parameters", namespace = Namespaces.SRV)
    @UML(identifier="parameters", obligation=OPTIONAL, specification=ISO_19115)
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
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code OperationMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return List of operation that must be completed immediately, or an empty list if none.
     */
    @XmlElement(name = "dependsOn", namespace = Namespaces.SRV)
    @UML(identifier="dependsOn", obligation=OPTIONAL, specification=ISO_19115)
    public List<DefaultOperationMetadata> getDependsOn() {
        return dependsOn = nonNullList(dependsOn, DefaultOperationMetadata.class);
    }

    /**
     * Sets the list of operation that must be completed before current operation is invoked.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code OperationMetadata} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param newValues The new list of operation.
     */
    public void setDependsOn(final List<? extends DefaultOperationMetadata> newValues) {
        dependsOn = writeList(newValues, dependsOn, DefaultOperationMetadata.class);
    }
}
