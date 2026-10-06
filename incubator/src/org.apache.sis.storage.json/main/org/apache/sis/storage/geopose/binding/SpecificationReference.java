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
package org.apache.sis.storage.geopose.binding;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * Reference to a specification identified by an authority, an identifier and a parameter string.
 *
 * <p>GeoPose uses this triple for two distinct purposes, modelled by the two subclasses
 * {@link FrameSpecification} and {@link TransitionModel}.</p>
 *
 * <ul>
 *   <li>The three members are mandatory.</li>
 *   <li>The interpretation of {@code parameters} is defined by the {@code authority} and
 *       {@code id} pair; it is not parsed by this class.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definitions FrameSpecification and TransitionModel"
 */
public abstract class SpecificationReference extends DataTransferObject {

    public static final String JSON_PROPERTY_AUTHORITY = "authority";
    private String authority;

    public static final String JSON_PROPERTY_ID = "id";
    private String id;

    public static final String JSON_PROPERTY_PARAMETERS = "parameters";
    private String parameters;

    protected SpecificationReference() {
    }

    protected SpecificationReference(final String authority, final String id, final String parameters) {
        this.authority = authority;
        this.id = id;
        this.parameters = parameters;
    }

    /**
     * Returns the organization which defines the referenced specification.
     *
     * @return the authority, such as {@code "/geopose/1.0"}
     */
    @JsonProperty(JSON_PROPERTY_AUTHORITY)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public String getAuthority() {
        return authority;
    }

    @JsonProperty(JSON_PROPERTY_AUTHORITY)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setAuthority(final String authority) {
        this.authority = authority;
    }

    /**
     * Returns the identifier of the referenced specification within its authority.
     *
     * @return the identifier, such as {@code "LTP-ENU"}
     */
    @JsonProperty(JSON_PROPERTY_ID)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public String getId() {
        return id;
    }

    @JsonProperty(JSON_PROPERTY_ID)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the parameters completing the specification reference.
     *
     * @return the parameter string, whose syntax depends on the authority and identifier
     */
    @JsonProperty(JSON_PROPERTY_PARAMETERS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public String getParameters() {
        return parameters;
    }

    @JsonProperty(JSON_PROPERTY_PARAMETERS)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setParameters(final String parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SpecificationReference other = (SpecificationReference) o;
        return Objects.equals(this.authority,  other.authority)
            && Objects.equals(this.id,         other.id)
            && Objects.equals(this.parameters, other.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authority, id, parameters);
    }
}
