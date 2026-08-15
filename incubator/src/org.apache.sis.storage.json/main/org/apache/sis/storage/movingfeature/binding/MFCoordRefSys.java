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
package org.apache.sis.storage.movingfeature.binding;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * Value of a Moving Features JSON (MF-JSON, OGC 16-140r1) 'crs' or 'trs' member.
 *
 * <p>Both members share this exact {@code {"type": ..., "properties": ...}} shape: a
 * "Name" type refers to a named CRS or TRS (e.g. {@code {"type":"Name","properties":{"name":"urn:ogc:def:crs:OGC:1.3:CRS84"}}}),
 * a "Link" type refers to one by URI (e.g. {@code {"type":"Link","properties":{"href":"...","type":"..."}}}).
 * The formal schema requires 'properties' to be an object, but the specification's own examples
 * sometimes use a bare URI string instead (e.g. {@code "properties": "urn:ogc:def:crs:OGC:1.3:CRS84"}),
 * so 'properties' is typed as {@link Object} here to accept both real-world forms.</p>
 */
@JsonPropertyOrder({
    MFCoordRefSys.PROPERTY_TYPE,
    MFCoordRefSys.PROPERTY_PROPERTIES
})
public class MFCoordRefSys extends DataTransferObject {

    public static final String TYPE_NAME = "Name";
    public static final String TYPE_LINK = "Link";

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_PROPERTIES = "properties";

    private String type;

    private Object properties;

    public MFCoordRefSys() {
    }

    /**
     * Get type
     *
     * @return type
     */
    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getType() {
        return type;
    }

    @JsonProperty(PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get properties, either a plain name/URI {@link String}, or an object such as
     * {@code {"name": "..."}} or {@code {"href": "...", "type": "..."}}.
     *
     * @return properties
     */
    @JsonProperty(PROPERTY_PROPERTIES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Object getProperties() {
        return properties;
    }

    @JsonProperty(PROPERTY_PROPERTIES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setProperties(Object properties) {
        this.properties = properties;
    }

    /**
     * Return true if this MFCoordRefSys object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFCoordRefSys other = (MFCoordRefSys) o;
        return Objects.equals(this.type, other.type)
                && Objects.equals(this.properties, other.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, properties);
    }

}
