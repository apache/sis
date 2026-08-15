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
 * Value of a Moving Features JSON (MF-JSON, OGC 16-140r1) 'base' member of a
 * {@link MFTemporalPrimitiveGeometry}: a reference to an external 3D model asset
 * (e.g. a glTF file) that {@link MFOrientation} entries are applied to.
 */
@JsonPropertyOrder({
    MFMovingGeometryBase.PROPERTY_TYPE,
    MFMovingGeometryBase.PROPERTY_HREF
})
public class MFMovingGeometryBase extends DataTransferObject {

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_HREF = "href";

    private String type;

    private String href;

    public MFMovingGeometryBase() {
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
     * Get href
     *
     * @return href
     */
    @JsonProperty(PROPERTY_HREF)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getHref() {
        return href;
    }

    @JsonProperty(PROPERTY_HREF)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * Return true if this MFMovingGeometryBase object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MFMovingGeometryBase other = (MFMovingGeometryBase) o;
        return Objects.equals(this.type, other.type)
                && Objects.equals(this.href, other.href);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, href);
    }

}
