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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.sis.metadata.iso.extent.AbstractGeographicExtent;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicDescription;
import org.opengis.metadata.extent.GeographicExtent;

/**
 * Binding of the {@code EX_GeographicExtentUnion} JSON type, the parent of bounding boxes and geographic descriptions.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 a)"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#EX_GeographicExtentUnion</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
              property = JSONMDGeographicExtent.PROPERTY_TYPE)
@JsonSubTypes({
    @JsonSubTypes.Type(value = JSONMDGeographicBoundingBox.class, name = JSONMDGeographicBoundingBox.TYPE),
    @JsonSubTypes.Type(value = JSONMDGeographicDescription.class, name = JSONMDGeographicDescription.TYPE)
})
@JsonPropertyOrder({
    JSONMDGeographicExtent.PROPERTY_TYPE,
    JSONMDGeographicExtent.PROPERTY_EXTENT_TYPE_CODE
})
public abstract class JSONMDGeographicExtent extends DataTransferObject {

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_EXTENT_TYPE_CODE = "extentTypeCode";

    @JsonProperty(PROPERTY_EXTENT_TYPE_CODE)
    private Boolean extentTypeCode;

    protected JSONMDGeographicExtent() {
    }

    protected JSONMDGeographicExtent(final GeographicExtent source) {
        extentTypeCode = source.getInclusion();
    }

    /**
     * Creates the binding matching the geographic extent type.
     *
     * <p>Returns {@code null} with a warning for other geographic extent types, such as bounding polygons.
     */
    public static JSONMDGeographicExtent create(final GeographicExtent source, final JSONMDEncoder encoder) {
        if (source instanceof GeographicBoundingBox box) {
            return new JSONMDGeographicBoundingBox(box);
        } else if (source instanceof GeographicDescription description) {
            return new JSONMDGeographicDescription(description, encoder);
        }
        encoder.warning("Geographic extent type is not supported: " + source.getClass().getName());
        return null;
    }

    /**
     * Returns the name of the geographic extent type in the JSON schema.
     */
    @JsonProperty(PROPERTY_TYPE)
    public abstract String getType();

    /**
     * Returns whether the extent is included ({@code true}) or excluded ({@code false}) from the resource extent.
     */
    public Boolean getExtentTypeCode() {
        return extentTypeCode;
    }

    public void setExtentTypeCode(final Boolean extentTypeCode) {
        this.extentTypeCode = extentTypeCode;
    }

    public abstract AbstractGeographicExtent toMetadata(JSONMDDecoder decoder);

    /**
     * Copies the properties common to all geographic extents.
     */
    protected final void fill(final AbstractGeographicExtent target) {
        target.setInclusion(extentTypeCode);
    }
}
