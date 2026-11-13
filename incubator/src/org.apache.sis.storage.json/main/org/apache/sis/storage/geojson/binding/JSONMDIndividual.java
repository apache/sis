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
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.opengis.metadata.citation.Individual;

/**
 * Binding of the {@code CI_Individual} JSON type.
 *
 * <p>The {@code type} member may be omitted when the declared type is already {@code CI_Individual},
 * as in {@code CI_Organisation.individual}.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Individual</a>
 */
@JsonPropertyOrder({
    JSONMDParty.PROPERTY_TYPE,
    JSONMDParty.PROPERTY_NAME,
    JSONMDParty.PROPERTY_CONTACT_INFO,
    JSONMDParty.PROPERTY_PARTY_IDENTIFIER,
    JSONMDIndividual.PROPERTY_POSITION_NAME
})
public final class JSONMDIndividual extends JSONMDParty {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "CI_Individual";
    public static final String PROPERTY_POSITION_NAME = "positionName";

    @JsonProperty(PROPERTY_POSITION_NAME)
    private String positionName;

    public JSONMDIndividual() {
    }

    public JSONMDIndividual(final Individual source, final JSONMDEncoder encoder) {
        super(source, encoder);
        positionName = encoder.text(source.getPositionName());
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(final String positionName) {
        this.positionName = positionName;
    }

    @Override
    public DefaultIndividual toMetadata(final JSONMDDecoder decoder) {
        final DefaultIndividual target = new DefaultIndividual();
        fill(target, decoder);
        target.setPositionName(decoder.text(positionName));
        return target;
    }
}
