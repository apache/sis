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
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.VerticalExtent;

/**
 * Binding of the {@code EX_Extent} JSON type.
 *
 * <p>The {@code temporalElement} and {@code verticalElement} properties are extensions to the ISO 19115-4 Core profile.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#EX_Extent</a>
 */
@JsonPropertyOrder({
    JSONMDExtent.PROPERTY_DESCRIPTION,
    JSONMDExtent.PROPERTY_GEOGRAPHIC_ELEMENT,
    JSONMDExtent.PROPERTY_TEMPORAL_ELEMENT,
    JSONMDExtent.PROPERTY_VERTICAL_ELEMENT
})
public final class JSONMDExtent extends DataTransferObject {

    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_GEOGRAPHIC_ELEMENT = "geographicElement";
    public static final String PROPERTY_TEMPORAL_ELEMENT = "temporalElement";
    public static final String PROPERTY_VERTICAL_ELEMENT = "verticalElement";

    @JsonProperty(PROPERTY_DESCRIPTION)
    private String description;
    @JsonProperty(PROPERTY_GEOGRAPHIC_ELEMENT)
    private List<JSONMDGeographicExtent> geographicElement;
    @JsonProperty(PROPERTY_TEMPORAL_ELEMENT)
    private List<JSONMDTemporalExtent> temporalElement;
    @JsonProperty(PROPERTY_VERTICAL_ELEMENT)
    private List<JSONMDVerticalExtent> verticalElement;

    public JSONMDExtent() {
    }

    public JSONMDExtent(final Extent source, final JSONMDEncoder encoder) {
        description       = encoder.text(source.getDescription());
        geographicElement = encoder.list(source.getGeographicElements(), (GeographicExtent e) -> JSONMDGeographicExtent.create(e, encoder));
        temporalElement   = encoder.list(source.getTemporalElements(), (TemporalExtent e) -> {
            final JSONMDTemporalExtent element = new JSONMDTemporalExtent(e, encoder);
            return (element.getExtent() == null) ? null : element;
        });
        verticalElement   = encoder.list(source.getVerticalElements(), (VerticalExtent e) -> new JSONMDVerticalExtent(e, encoder));
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<JSONMDGeographicExtent> getGeographicElement() {
        return geographicElement;
    }

    public void setGeographicElement(final List<JSONMDGeographicExtent> geographicElement) {
        this.geographicElement = geographicElement;
    }

    public List<JSONMDTemporalExtent> getTemporalElement() {
        return temporalElement;
    }

    public void setTemporalElement(final List<JSONMDTemporalExtent> temporalElement) {
        this.temporalElement = temporalElement;
    }

    public List<JSONMDVerticalExtent> getVerticalElement() {
        return verticalElement;
    }

    public void setVerticalElement(final List<JSONMDVerticalExtent> verticalElement) {
        this.verticalElement = verticalElement;
    }

    public DefaultExtent toMetadata(final JSONMDDecoder decoder) {
        final DefaultExtent target = new DefaultExtent();
        target.setDescription(decoder.text(description));
        target.setGeographicElements(decoder.list(geographicElement, (JSONMDGeographicExtent e) -> e.toMetadata(decoder)));
        target.setTemporalElements(decoder.list(temporalElement, (JSONMDTemporalExtent e) -> e.toMetadata(decoder)));
        target.setVerticalElements(decoder.list(verticalElement, (JSONMDVerticalExtent e) -> e.toMetadata(decoder)));
        return target;
    }
}
