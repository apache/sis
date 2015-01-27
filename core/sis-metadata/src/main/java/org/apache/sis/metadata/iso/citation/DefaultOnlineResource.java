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
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about on-line sources from which the dataset, specification, or
 * community profile name and extended metadata elements can be obtained.
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
 * @since   0.3
 * @version 0.5
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
     * Request used to access the resource depending on the protocol.
     * This is used mainly for POST requests.
     */
    private String protocolRequest;

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
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(OnlineResource)
     */
    public DefaultOnlineResource(final OnlineResource object) {
        super(object);
        if (object != null) {
            linkage            = object.getLinkage();
            protocol           = object.getProtocol();
            applicationProfile = object.getApplicationProfile();
            name               = object.getName();
            description        = object.getDescription();
            function           = object.getFunction();
            if (object instanceof DefaultOnlineResource) {
                protocolRequest = ((DefaultOnlineResource) object).getProtocolRequest();
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
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
     *
     * @return Application profile that can be used with the online resource, or {@code null}.
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
     *
     * <div class="warning"><b>Upcoming API change — internationalization</b><br>
     * The return type may be changed from {@code String} to {@code InternationalString} in GeoAPI 4.0.
     * </div>
     *
     * @return Name of the online resource, or {@code null}.
     */
    @Override
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the online resource.
     *
     * <div class="warning"><b>Upcoming API change — internationalization</b><br>
     * The argument type may be changed from {@code String} to {@code InternationalString} in GeoAPI 4.0.
     * </div>
     *
     * @param newValue The new name, or {@code null} if none.
     */
    public void setName(final String newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns the detailed text description of what the online resource is/does.
     *
     * @return Text description of what the online resource is/does, or {@code null}.
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
     *
     * @return Function performed by the online resource, or {@code null}.
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
     * similar addressing scheme.
     *
     * @return Location for on-line access using a Uniform Resource Locator address or similar scheme, or {@code null}.
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
     * Returns the connection protocol to be used.
     *
     * <div class="note"><b>Example:</b>
     * ftp, http get KVP, http POST, <i>etc</i>.
     * </div>
     *
     * @return Connection protocol to be used, or {@code null}.
     */
    @Override
    @XmlElement(name = "protocol")
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the connection protocol to be used.
     *
     * @param newValue The new protocol, or {@code null} if none.
     */
    public void setProtocol(final String newValue) {
        checkWritePermission();
        protocol = newValue;
    }

    /**
     * Returns the request used to access the resource depending on the protocol.
     * This is used mainly for POST requests.
     *
     * <div class="note"><b>Example:</b>
     * {@preformat xml
     *     <GetFeature service="WFS" version="2.0.0"
     *                 outputFormat="application/gml+xml;verson=3.2"
     *                 xmlns="(…snip…)">
     *         <Query typeNames="Roads"/>
     *     </GetFeature>
     * }
     * </div>
     *
     * @return Request used to access the resource.
     *
     * @since 0.5
     */
    @UML(identifier="protocolRequest", obligation=OPTIONAL, specification=ISO_19115)
    public String getProtocolRequest() {
        return protocolRequest;
    }

    /**
     * Sets the request to be used.
     *
     * @param newValue The new request, or {@code null} if none.
     *
     * @since 0.5
     */
    public void setProtocolRequest(final String newValue) {
        checkWritePermission();
        protocolRequest = newValue;
    }
}
