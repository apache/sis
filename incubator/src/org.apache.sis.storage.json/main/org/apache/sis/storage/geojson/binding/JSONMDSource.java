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
import org.apache.sis.metadata.iso.lineage.DefaultSource;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.lineage.Source;

/**
 * Binding of the {@code LI_Source} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#LI_Source</a>
 */
@JsonPropertyOrder({
    JSONMDSource.PROPERTY_DESCRIPTION,
    JSONMDSource.PROPERTY_SOURCE_REFERENCE_SYSTEM,
    JSONMDSource.PROPERTY_SOURCE_CITATION,
    JSONMDSource.PROPERTY_SOURCE_METADATA,
    JSONMDSource.PROPERTY_SCOPE
})
public final class JSONMDSource extends DataTransferObject {

    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_SOURCE_REFERENCE_SYSTEM = "sourceReferenceSystem";
    public static final String PROPERTY_SOURCE_CITATION = "sourceCitation";
    public static final String PROPERTY_SOURCE_METADATA = "sourceMetadata";
    public static final String PROPERTY_SCOPE = "scope";

    @JsonProperty(PROPERTY_DESCRIPTION)
    private String description;
    @JsonProperty(PROPERTY_SOURCE_REFERENCE_SYSTEM)
    private JSONMDReferenceSystem sourceReferenceSystem;
    @JsonProperty(PROPERTY_SOURCE_CITATION)
    private JSONMDCitation sourceCitation;
    @JsonProperty(PROPERTY_SOURCE_METADATA)
    private List<JSONMDCitation> sourceMetadata;
    @JsonProperty(PROPERTY_SCOPE)
    private JSONMDScope scope;

    public JSONMDSource() {
    }

    public JSONMDSource(final Source source, final JSONMDEncoder encoder) {
        description           = encoder.text(source.getDescription());
        sourceReferenceSystem = (source.getSourceReferenceSystem() == null) ? null
                              : new JSONMDReferenceSystem(source.getSourceReferenceSystem(), encoder);
        sourceCitation        = (source.getSourceCitation() == null) ? null : new JSONMDCitation(source.getSourceCitation(), encoder);
        sourceMetadata        = encoder.list(source.getSourceMetadata(), (Citation e) -> new JSONMDCitation(e, encoder));
        scope                 = (source.getScope() == null) ? null : new JSONMDScope(source.getScope(), encoder);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public JSONMDReferenceSystem getSourceReferenceSystem() {
        return sourceReferenceSystem;
    }

    public void setSourceReferenceSystem(final JSONMDReferenceSystem sourceReferenceSystem) {
        this.sourceReferenceSystem = sourceReferenceSystem;
    }

    public JSONMDCitation getSourceCitation() {
        return sourceCitation;
    }

    public void setSourceCitation(final JSONMDCitation sourceCitation) {
        this.sourceCitation = sourceCitation;
    }

    public List<JSONMDCitation> getSourceMetadata() {
        return sourceMetadata;
    }

    public void setSourceMetadata(final List<JSONMDCitation> sourceMetadata) {
        this.sourceMetadata = sourceMetadata;
    }

    public JSONMDScope getScope() {
        return scope;
    }

    public void setScope(final JSONMDScope scope) {
        this.scope = scope;
    }

    public DefaultSource toMetadata(final JSONMDDecoder decoder) {
        final DefaultSource target = new DefaultSource();
        target.setDescription(decoder.text(description));
        if (sourceReferenceSystem != null) {
            target.setSourceReferenceSystem(sourceReferenceSystem.toMetadata(decoder));
        }
        if (sourceCitation != null) {
            target.setSourceCitation(sourceCitation.toMetadata(decoder));
        }
        target.setSourceMetadata(decoder.list(sourceMetadata, (JSONMDCitation e) -> e.toMetadata(decoder)));
        if (scope != null) {
            target.setScope(scope.toMetadata(decoder));
        }
        return target;
    }
}
