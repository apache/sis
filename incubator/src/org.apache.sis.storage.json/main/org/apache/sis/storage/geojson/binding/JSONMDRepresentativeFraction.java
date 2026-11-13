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
import org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.identification.RepresentativeFraction;

/**
 * Binding of the {@code MD_RepresentativeFraction} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_RepresentativeFraction</a>
 */
@JsonPropertyOrder({
    JSONMDRepresentativeFraction.PROPERTY_DENOMINATOR
})
public final class JSONMDRepresentativeFraction extends DataTransferObject {

    public static final String PROPERTY_DENOMINATOR = "denominator";

    @JsonProperty(PROPERTY_DENOMINATOR)
    private Long denominator;

    public JSONMDRepresentativeFraction() {
    }

    public JSONMDRepresentativeFraction(final RepresentativeFraction source) {
        denominator = source.getDenominator();
    }

    public Long getDenominator() {
        return denominator;
    }

    public void setDenominator(final Long denominator) {
        this.denominator = denominator;
    }

    /**
     * Returns the representative fraction, or {@code null} with a warning if the denominator is missing.
     */
    public DefaultRepresentativeFraction toMetadata(final JSONMDDecoder decoder) {
        if (denominator == null) {
            decoder.warning("Representative fraction without denominator is ignored.");
            return null;
        }
        return new DefaultRepresentativeFraction(denominator);
    }
}
