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

import java.net.URI;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information about on-line sources from which the dataset, specification, or
 * community profile name and extended metadata elements can be obtained.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "CI_OnlineResource_Type", propOrder = {
    "linkage",
    "protocol",
    "applicationProfile",
    "name",
    "description",
    "function"
})
@XmlRootElement(name = "CI_OnlineResource")
public class DefaultOnlineResource extends ISOMetadata implements OnlineResource {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1413613911128890864L;

    /**
     * Location (address) for on-line access using a Uniform Resource Locator address or
     * similar addressing scheme such as "{@code http://www.statkart.no/isotc211}".
     */
    private URI linkage;

    /**
     * The connection protocol to be used.
     */
    private String protocol;

    /**
     * Name of an application profile that can be used with the online resource.
     */
    private String applicationProfile;

    /**
     * Name of the online resources.
     */
    private String name;

    /**
     * Detailed text description of what the online resource is/does.
     */
    private InternationalString description;

    /**
     * Code for function performed by the online resource.
     */
    private OnLineFunction function;

    /**
     * Creates an initially empty on line resource.
     */
    public DefaultOnlineResource() {
    }

    /**
     * Creates an on line resource initialized to the given URI.
     *
     * @param linkage The location for on-line access using a Uniform Resource Locator address,
     *        or {@code null} if none.
     */
    public DefaultOnlineResource(final URI linkage) {
        this.linkage = linkage;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(OnlineResource)
     */
    public DefaultOnlineResource(final OnlineResource object) {
        super(object);
        linkage            = object.getLinkage();
        protocol           = object.getProtocol();
        applicationProfile = object.getApplicationProfile();
        name               = object.getName();
        description        = object.getDescription();
        function           = object.getFunction();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultOnlineResource}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultOnlineResource} instance is created using the
     *       {@linkplain #DefaultOnlineResource(OnlineResource) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultOnlineResource castOrCopy(final OnlineResource object) {
        if (object == null || object instanceof DefaultOnlineResource) {
            return (DefaultOnlineResource) object;
        }
        return new DefaultOnlineResource(object);
    }

    /**
     * Returns the name of an application profile that can be used with the online resource.
     * Returns {@code null} if none.
     */
    @Override
    @XmlElement(name = "applicationProfile")
    public String getApplicationProfile() {
        return applicationProfile;
    }

    /**
     * Sets the name of an application profile that can be used with the online resource.
     *
     * @param newValue The new application profile.
     */
    public void setApplicationProfile(final String newValue) {
        checkWritePermission();
        applicationProfile = newValue;
    }

    /**
     * Name of the online resource. Returns {@code null} if none.
     */
    @Override
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the online resource.
     *
     * @param newValue The new name, or {@code null} if none.
     */
    public void setName(final String newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns the detailed text description of what the online resource is/does.
     */
    @Override
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the detailed text description of what the online resource is/does.
     *
     * @param newValue The new description, or {@code null} if none.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the code for function performed by the online resource.
     */
    @Override
    @XmlElement(name = "function")
    public OnLineFunction getFunction() {
        return function;
    }

    /**
     * Sets the code for function performed by the online resource.
     *
     * @param newValue The new function, or {@code null} if none.
     */
    public void setFunction(final OnLineFunction newValue) {
        checkWritePermission();
        function = newValue;
    }

    /**
     * Returns the location (address) for on-line access using a Uniform Resource Locator address or
     * similar addressing scheme such as "{@code http://www.statkart.no/isotc211}".
     */
    @Override
    @XmlElement(name = "linkage", required = true)
    public URI getLinkage() {
        return linkage;
    }

    /**
     * Sets the location (address) for on-line access using a Uniform Resource Locator address or
     * similar addressing scheme such as "{@code http://www.statkart.no/isotc211}".
     *
     * @param newValue The new linkage, or {@code null} if none.
     */
    public void setLinkage(final URI newValue) {
        checkWritePermission();
        linkage = newValue;
    }

    /**
     * Returns the connection protocol to be used. If no protocol has been {@linkplain #setProtocol(String)
     * explicitely set}, then this method returns the {@linkplain #getLinkage() linkage}
     * {@linkplain URI#getScheme() scheme} (if any).
     */
    @Override
    @XmlElement(name = "protocol")
    public String getProtocol() {
        if (protocol == null && linkage != null) {
            return linkage.getScheme();
        }
        return protocol;
    }

    /**
     * Returns the connection protocol to be used.
     *
     * @param newValue The new protocol, or {@code null} if none.
     */
    public void setProtocol(final String newValue) {
        checkWritePermission();
        protocol = newValue;
    }
}
