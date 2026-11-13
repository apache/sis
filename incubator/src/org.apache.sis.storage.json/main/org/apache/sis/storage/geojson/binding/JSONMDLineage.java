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
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.lineage.Source;

/**
 * Binding of the {@code LI_Lineage} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#LI_Lineage</a>
 */
@JsonPropertyOrder({
    JSONMDLineage.PROPERTY_STATEMENT,
    JSONMDLineage.PROPERTY_SCOPE,
    JSONMDLineage.PROPERTY_SOURCE,
    JSONMDLineage.PROPERTY_ADDITIONAL_DOCUMENTATION
})
public final class JSONMDLineage extends DataTransferObject {

    public static final String PROPERTY_STATEMENT = "statement";
    public static final String PROPERTY_SCOPE = "scope";
    public static final String PROPERTY_SOURCE = "source";
    public static final String PROPERTY_ADDITIONAL_DOCUMENTATION = "additionalDocumentation";

    @JsonProperty(PROPERTY_STATEMENT)
    private String statement;
    @JsonProperty(PROPERTY_SCOPE)
    private JSONMDScope scope;
    @JsonProperty(PROPERTY_SOURCE)
    private List<JSONMDSource> source;
    @JsonProperty(PROPERTY_ADDITIONAL_DOCUMENTATION)
    private List<JSONMDCitation> additionalDocumentation;

    public JSONMDLineage() {
    }

    public JSONMDLineage(final Lineage source, final JSONMDEncoder encoder) {
        statement               = encoder.text(source.getStatement());
        scope                   = (source.getScope() == null) ? null : new JSONMDScope(source.getScope(), encoder);
        this.source             = encoder.list(source.getSources(), (Source e) -> new JSONMDSource(e, encoder));
        additionalDocumentation = encoder.list(source.getAdditionalDocumentation(), (Citation e) -> new JSONMDCitation(e, encoder));
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(final String statement) {
        this.statement = statement;
    }

    public JSONMDScope getScope() {
        return scope;
    }

    public void setScope(final JSONMDScope scope) {
        this.scope = scope;
    }

    public List<JSONMDSource> getSource() {
        return source;
    }

    public void setSource(final List<JSONMDSource> source) {
        this.source = source;
    }

    public List<JSONMDCitation> getAdditionalDocumentation() {
        return additionalDocumentation;
    }

    public void setAdditionalDocumentation(final List<JSONMDCitation> additionalDocumentation) {
        this.additionalDocumentation = additionalDocumentation;
    }

    public DefaultLineage toMetadata(final JSONMDDecoder decoder) {
        final DefaultLineage target = new DefaultLineage();
        target.setStatement(decoder.text(statement));
        if (scope != null) {
            target.setScope(scope.toMetadata(decoder));
        }
        target.setSources(decoder.list(source, (JSONMDSource e) -> e.toMetadata(decoder)));
        target.setAdditionalDocumentation(decoder.list(additionalDocumentation, (JSONMDCitation e) -> e.toMetadata(decoder)));
        return target;
    }
}
