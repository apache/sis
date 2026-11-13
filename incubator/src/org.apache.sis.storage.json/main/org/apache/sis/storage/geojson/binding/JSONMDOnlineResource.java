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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;

/**
 * Binding of the {@code CI_OnlineResource} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_OnlineResource</a>
 */
@JsonPropertyOrder({
    JSONMDOnlineResource.PROPERTY_LINKAGE,
    JSONMDOnlineResource.PROPERTY_PROTOCOL,
    JSONMDOnlineResource.PROPERTY_APPLICATION_PROFILE,
    JSONMDOnlineResource.PROPERTY_NAME,
    JSONMDOnlineResource.PROPERTY_DESCRIPTION,
    JSONMDOnlineResource.PROPERTY_FUNCTION,
    JSONMDOnlineResource.PROPERTY_PROTOCOL_REQUEST
})
public final class JSONMDOnlineResource extends DataTransferObject {

    public static final String PROPERTY_LINKAGE = "linkage";
    public static final String PROPERTY_PROTOCOL = "protocol";
    public static final String PROPERTY_APPLICATION_PROFILE = "applicationProfile";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_FUNCTION = "function";
    public static final String PROPERTY_PROTOCOL_REQUEST = "protocolRequest";

    @JsonProperty(PROPERTY_LINKAGE)
    private String linkage;
    @JsonProperty(PROPERTY_PROTOCOL)
    private String protocol;
    @JsonProperty(PROPERTY_APPLICATION_PROFILE)
    private String applicationProfile;
    @JsonProperty(PROPERTY_NAME)
    private String name;
    @JsonProperty(PROPERTY_DESCRIPTION)
    private String description;
    @JsonProperty(PROPERTY_FUNCTION)
    private String function;
    @JsonProperty(PROPERTY_PROTOCOL_REQUEST)
    private String protocolRequest;

    public JSONMDOnlineResource() {
    }

    public JSONMDOnlineResource(final OnlineResource source, final JSONMDEncoder encoder) {
        linkage            = encoder.uri(source.getLinkage());
        protocol           = source.getProtocol();
        applicationProfile = source.getApplicationProfile();
        name               = encoder.text(source.getName());
        description        = encoder.text(source.getDescription());
        function           = encoder.code(source.getFunction());
        protocolRequest    = source.getProtocolRequest();
    }

    public String getLinkage() {
        return linkage;
    }

    public void setLinkage(final String linkage) {
        this.linkage = linkage;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String getApplicationProfile() {
        return applicationProfile;
    }

    public void setApplicationProfile(final String applicationProfile) {
        this.applicationProfile = applicationProfile;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(final String function) {
        this.function = function;
    }

    public String getProtocolRequest() {
        return protocolRequest;
    }

    public void setProtocolRequest(final String protocolRequest) {
        this.protocolRequest = protocolRequest;
    }

    public DefaultOnlineResource toMetadata(final JSONMDDecoder decoder) {
        final DefaultOnlineResource target = new DefaultOnlineResource();
        target.setLinkage(decoder.uri(linkage));
        target.setProtocol(protocol);
        target.setApplicationProfile(applicationProfile);
        target.setName(decoder.text(name));
        target.setDescription(decoder.text(description));
        target.setFunction(decoder.code(OnLineFunction.class, function, OnLineFunction::valueOf));
        target.setProtocolRequest(protocolRequest);
        return target;
    }
}
