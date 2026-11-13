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
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.opengis.metadata.extent.GeographicDescription;

/**
 * Binding of the {@code EX_GeographicDescription} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#EX_GeographicDescription</a>
 */
@JsonPropertyOrder({
    JSONMDGeographicExtent.PROPERTY_TYPE,
    JSONMDGeographicExtent.PROPERTY_EXTENT_TYPE_CODE,
    JSONMDGeographicDescription.PROPERTY_GEOGRAPHIC_IDENTIFIER
})
public final class JSONMDGeographicDescription extends JSONMDGeographicExtent {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "EX_GeographicDescription";
    public static final String PROPERTY_GEOGRAPHIC_IDENTIFIER = "geographicIdentifier";

    @JsonProperty(PROPERTY_GEOGRAPHIC_IDENTIFIER)
    private JSONMDIdentifier geographicIdentifier;

    public JSONMDGeographicDescription() {
    }

    public JSONMDGeographicDescription(final GeographicDescription source, final JSONMDEncoder encoder) {
        super(source);
        geographicIdentifier = (source.getGeographicIdentifier() == null) ? null
                             : new JSONMDIdentifier(source.getGeographicIdentifier(), encoder);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public JSONMDIdentifier getGeographicIdentifier() {
        return geographicIdentifier;
    }

    public void setGeographicIdentifier(final JSONMDIdentifier geographicIdentifier) {
        this.geographicIdentifier = geographicIdentifier;
    }

    @Override
    public DefaultGeographicDescription toMetadata(final JSONMDDecoder decoder) {
        final DefaultGeographicDescription target = new DefaultGeographicDescription();
        fill(target);
        if (geographicIdentifier != null) {
            target.setGeographicIdentifier(geographicIdentifier.toMetadata(decoder));
        }
        return target;
    }
}
