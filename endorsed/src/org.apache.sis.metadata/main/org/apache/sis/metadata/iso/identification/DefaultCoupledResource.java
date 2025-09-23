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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.util.ScopedName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.DataIdentification;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.metadata.SV_OperationMetadata;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.gco.GO_GenericName;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.iso.Names;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.identification.CoupledResource;
import org.opengis.metadata.identification.OperationMetadata;


/**
 * Links a given operation name with a resource identified by an "identifier".
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
@XmlType(name = "SV_CoupledResource_Type", namespace = Namespaces.SRV, propOrder = {
    "scopedName",               // ISO 19115-3:2016 way to write scoped name
    "resourceReference",        // New in ISO 19115:2014
    "resource",                 // Ibid.
    "operation",                // Ibid.
    "operationName",            // Legacy ISO 19139:2007
    "id",                       // Ibid.
    "legacyName"                // Legacy ISO 19139:2007 way to write scoped name
})
@XmlRootElement(name = "SV_CoupledResource", namespace = Namespaces.SRV)
public class DefaultCoupledResource extends ISOMetadata implements CoupledResource {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 154704781596732747L;

    /**
     * Scoped identifier of the resource in the context of the given service instance.
     */
    @SuppressWarnings("serial")
    private ScopedName scopedName;

    /**
     * References to the resource on which the services operates.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> resourceReferences;

    /**
     * The tightly coupled resources.
     */
    @SuppressWarnings("serial")
    private Collection<DataIdentification> resources;

    /**
     * The service operation.
     */
    @SuppressWarnings("serial")
    private OperationMetadata operation;

    /**
     * Constructs an initially empty coupled resource.
     */
    public DefaultCoupledResource() {
    }

    /**
     * Constructs a new coupled resource initialized to the specified values.
     *
     * @param name        scoped identifier of the resource in the context of the given service instance.
     * @param reference   reference to the reference to the resource on which the services operates.
     * @param resource    the tightly coupled resource.
     * @param operation   the service operation.
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
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
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
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
    @XmlJavaTypeAdapter(GO_GenericName.Since2014.class)
    public ScopedName getScopedName() {
        return scopedName;
    }

    /**
     * Sets the identifier of the resource in the context of the given service instance.
     *
     * @param  newValue  the new identifier of the resource.
     */
    public void setScopedName(final ScopedName newValue) {
        checkWritePermission(scopedName);
        scopedName = newValue;
    }

    /**
     * Returns references to the resource on which the services operates.
     *
     * @return references to the resource on which the services operates.
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Citation> getResourceReferences() {
        return resourceReferences = nonNullCollection(resourceReferences, Citation.class);
    }

    /**
     * Sets references to the resource on which the services operates.
     *
     * @param  newValues  the new references to the resource on which the services operates.
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
    // @XmlElement at the end of this class.
    public Collection<DataIdentification> getResources() {
        return resources = nonNullCollection(resources, DataIdentification.class);
    }

    /**
     * Sets the tightly coupled resources.
     *
     * @param  newValues  the new tightly coupled resources.
     */
    public void setResources(final Collection<? extends DataIdentification> newValues) {
        resources = writeCollection(newValues, resources, DataIdentification.class);
    }

    /**
     * Returns the service operation.
     *
     * @return the service operation, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "operation")
    @XmlJavaTypeAdapter(SV_OperationMetadata.Since2014.class)
    public OperationMetadata getOperation() {
        return operation;
    }

    /**
     * Sets a new service operation.
     *
     * @param  newValue  the new service operation.
     */
    public void setOperation(final OperationMetadata newValue) {
        checkWritePermission(operation);
        operation = newValue;
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * For JAXB marshalling of ISO 19139:2007 document only (XML based on legacy ISO 19115:2003 model).
     */
    @XmlElement(name = "operationName", namespace = LegacyNamespaces.SRV)
    private String getOperationName() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final OperationMetadata operation = getOperation();
            if (operation != null) {
                return operation.getOperationName();
            }
        }
        return null;
    }

    /**
     * For JAXB unmarhalling of ISO 19139:2007 document only. Sets {@link #operation} to a temporary
     * {@link OperationName} placeholder. That temporary instance will be replaced by the real one
     * when the enclosing {@link DefaultServiceIdentification} is unmarshalled.
     */
    @SuppressWarnings("unused")
    private void setOperationName(final String name) {
        if (operation == null) {
            operation = new OperationName(name);
        }
    }

    /**
     * Returns the resource identifier, which is assumed to be the name as a string.
     * Used in legacy ISO 19139:2007 documents. There is no setter method; we expect
     * the XML to declare {@code <srv:operationName>} instead.
     */
    @XmlElement(name = "identifier", namespace = LegacyNamespaces.SRV)
    private String getId() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final ScopedName name = getScopedName();
            if (name != null) {
                return name.tip().toString();
            }
        }
        return null;
    }

    /**
     * Returns the {@code <gco:ScopedName>} element to marshal in legacy ISO 19139:2007 element.
     * The {@code <srv:scopedName>} element wrapper (note the lower-case "s") was missing in that
     * legacy specification. This departure from ISO patterns has been fixed in ISO 19115-3:2016.
     *
     * <p>Note that the namespace is {@value Namespaces#GCO} rather than {@value LegacyNamespaces#GCO}
     * because this is the namespace of the {@link ScopedName} type, not the namespace of a property
     * in {@code SV_CoupledResource}.</p>
     */
    @XmlElement(name = "ScopedName", namespace = Namespaces.GCO)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String getLegacyName() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final ScopedName name = getScopedName();
            if (name != null) return name.toString();
        }
        return null;
    }

    /**
     * Invoked by JAXB when unmarshalling a legacy ISO 19139:2007 documents.
     */
    @SuppressWarnings("unused")
    private void setLegacyName(String value) {
        if (value != null && !value.isEmpty()) {
            /*
             * If the given name does not have a namespace, add an arbitrary namespace
             * in order to get an instanceof ScopedName instead of LocalName after parsing.
             */
            if (value.indexOf(Constants.DEFAULT_SEPARATOR) < 0) {
                value = "global" + Constants.DEFAULT_SEPARATOR + value;
            }
            setScopedName((ScopedName) Names.parseGenericName(null, null, value));
        }
    }

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "resourceReference")
    private Collection<Citation> getResourceReference() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getResourceReferences() : null;
    }

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "resource")
    private Collection<DataIdentification> getResource() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getResources() : null;
    }
}
