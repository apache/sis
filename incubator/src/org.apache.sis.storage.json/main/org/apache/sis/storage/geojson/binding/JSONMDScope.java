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
import java.util.List;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.maintenance.Scope;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.maintenance.ScopeDescription;

/**
 * Binding of the {@code MD_Scope} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Scope</a>
 */
@JsonPropertyOrder({
    JSONMDScope.PROPERTY_LEVEL,
    JSONMDScope.PROPERTY_EXTENT,
    JSONMDScope.PROPERTY_LEVEL_DESCRIPTION
})
public final class JSONMDScope extends DataTransferObject {

    public static final String PROPERTY_LEVEL = "level";
    public static final String PROPERTY_EXTENT = "extent";
    public static final String PROPERTY_LEVEL_DESCRIPTION = "levelDescription";

    @JsonProperty(PROPERTY_LEVEL)
    private String level;
    @JsonProperty(PROPERTY_EXTENT)
    private List<JSONMDExtent> extent;
    @JsonProperty(PROPERTY_LEVEL_DESCRIPTION)
    private List<JSONMDScopeDescription> levelDescription;

    public JSONMDScope() {
    }

    public JSONMDScope(final Scope source, final JSONMDEncoder encoder) {
        level            = encoder.code(source.getLevel());
        extent           = encoder.list(source.getExtents(), (Extent e) -> new JSONMDExtent(e, encoder));
        levelDescription = encoder.list(source.getLevelDescription(), (ScopeDescription e) -> new JSONMDScopeDescription(e, encoder));
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(final String level) {
        this.level = level;
    }

    public List<JSONMDExtent> getExtent() {
        return extent;
    }

    public void setExtent(final List<JSONMDExtent> extent) {
        this.extent = extent;
    }

    public List<JSONMDScopeDescription> getLevelDescription() {
        return levelDescription;
    }

    public void setLevelDescription(final List<JSONMDScopeDescription> levelDescription) {
        this.levelDescription = levelDescription;
    }

    public DefaultScope toMetadata(final JSONMDDecoder decoder) {
        final DefaultScope target = new DefaultScope();
        target.setLevel(decoder.code(ScopeCode.class, level, ScopeCode::valueOf));
        target.setExtents(decoder.list(extent, (JSONMDExtent e) -> e.toMetadata(decoder)));
        target.setLevelDescription(decoder.list(levelDescription, (JSONMDScopeDescription e) -> e.toMetadata(decoder)));
        return target;
    }
}
