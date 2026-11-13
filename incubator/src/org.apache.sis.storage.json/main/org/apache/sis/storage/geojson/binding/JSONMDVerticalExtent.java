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
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.VerticalCRS;

/**
 * Binding of the {@code EX_VerticalExtent} type, an extension to the ISO 19115-4 Core profile.
 *
 * <p>The vertical CRS is written as a URI, as for {@code MD_ReferenceSystem.crs}.
 *
 * <p>Constraints:
 * <ul>
 *   <li>when reading, the URI must resolve to a vertical CRS, otherwise the CRS is ignored with a warning.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.5"
 */
@JsonPropertyOrder({
    JSONMDVerticalExtent.PROPERTY_MINIMUM_VALUE,
    JSONMDVerticalExtent.PROPERTY_MAXIMUM_VALUE,
    JSONMDVerticalExtent.PROPERTY_VERTICAL_CRS
})
public final class JSONMDVerticalExtent extends DataTransferObject {

    public static final String PROPERTY_MINIMUM_VALUE = "minimumValue";
    public static final String PROPERTY_MAXIMUM_VALUE = "maximumValue";
    public static final String PROPERTY_VERTICAL_CRS = "verticalCRS";

    @JsonProperty(PROPERTY_MINIMUM_VALUE)
    private Double minimumValue;
    @JsonProperty(PROPERTY_MAXIMUM_VALUE)
    private Double maximumValue;
    @JsonProperty(PROPERTY_VERTICAL_CRS)
    private String verticalCRS;

    public JSONMDVerticalExtent() {
    }

    public JSONMDVerticalExtent(final VerticalExtent source, final JSONMDEncoder encoder) {
        minimumValue = source.getMinimumValue();
        maximumValue = source.getMaximumValue();
        verticalCRS  = encoder.crs(source.getVerticalCRS());
    }

    public Double getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(final Double minimumValue) {
        this.minimumValue = minimumValue;
    }

    public Double getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(final Double maximumValue) {
        this.maximumValue = maximumValue;
    }

    public String getVerticalCRS() {
        return verticalCRS;
    }

    public void setVerticalCRS(final String verticalCRS) {
        this.verticalCRS = verticalCRS;
    }

    public DefaultVerticalExtent toMetadata(final JSONMDDecoder decoder) {
        final DefaultVerticalExtent target = new DefaultVerticalExtent();
        target.setMinimumValue(minimumValue);
        target.setMaximumValue(maximumValue);
        final CoordinateReferenceSystem crs = decoder.crs(verticalCRS);
        if (crs instanceof VerticalCRS vertical) {
            target.setVerticalCRS(vertical);
        } else if (crs != null) {
            decoder.warning("Reference system \"" + verticalCRS + "\" is not a vertical CRS.");
        }
        return target;
    }
}
