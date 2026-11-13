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
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.MetadataScope;
import org.opengis.metadata.maintenance.ScopeCode;

/**
 * Binding of the {@code MD_MetadataScope} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_MetadataScope</a>
 */
@JsonPropertyOrder({
    JSONMDMetadataScope.PROPERTY_RESOURCE_SCOPE,
    JSONMDMetadataScope.PROPERTY_NAME
})
public final class JSONMDMetadataScope extends DataTransferObject {

    public static final String PROPERTY_RESOURCE_SCOPE = "resourceScope";
    public static final String PROPERTY_NAME = "name";

    @JsonProperty(PROPERTY_RESOURCE_SCOPE)
    private String resourceScope;
    @JsonProperty(PROPERTY_NAME)
    private String name;

    public JSONMDMetadataScope() {
    }

    public JSONMDMetadataScope(final MetadataScope source, final JSONMDEncoder encoder) {
        resourceScope = encoder.code(source.getResourceScope());
        name          = encoder.text(source.getName());
    }

    public String getResourceScope() {
        return resourceScope;
    }

    public void setResourceScope(final String resourceScope) {
        this.resourceScope = resourceScope;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public DefaultMetadataScope toMetadata(final JSONMDDecoder decoder) {
        final DefaultMetadataScope target = new DefaultMetadataScope();
        target.setResourceScope(decoder.code(ScopeCode.class, resourceScope, ScopeCode::valueOf));
        target.setName(decoder.text(name));
        return target;
    }
}
