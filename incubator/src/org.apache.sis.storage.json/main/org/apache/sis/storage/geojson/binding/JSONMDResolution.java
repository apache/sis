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
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.identification.DefaultResolution;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.identification.Resolution;

/**
 * Binding of the {@code MD_Resolution} type, an extension to the ISO 19115-4 Core profile.
 *
 * <p>This is a union where exactly one property is set.
 * Distances are written with their unit of measurement, see {@link JSONMDQuantity}.
 *
 * <p>Constraints:
 * <ul>
 *   <li>when reading an object with more than one property, only the first one in the following order is kept:
 *       {@code equivalentScale}, {@code distance}, {@code vertical}, {@code angularDistance}, {@code levelOfDetail}.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3"
 */
@JsonPropertyOrder({
    JSONMDResolution.PROPERTY_EQUIVALENT_SCALE,
    JSONMDResolution.PROPERTY_DISTANCE,
    JSONMDResolution.PROPERTY_VERTICAL,
    JSONMDResolution.PROPERTY_ANGULAR_DISTANCE,
    JSONMDResolution.PROPERTY_LEVEL_OF_DETAIL
})
public final class JSONMDResolution extends DataTransferObject {

    public static final String PROPERTY_EQUIVALENT_SCALE = "equivalentScale";
    public static final String PROPERTY_DISTANCE = "distance";
    public static final String PROPERTY_VERTICAL = "vertical";
    public static final String PROPERTY_ANGULAR_DISTANCE = "angularDistance";
    public static final String PROPERTY_LEVEL_OF_DETAIL = "levelOfDetail";

    @JsonProperty(PROPERTY_EQUIVALENT_SCALE)
    private JSONMDRepresentativeFraction equivalentScale;
    @JsonProperty(PROPERTY_DISTANCE)
    private JSONMDQuantity distance;
    @JsonProperty(PROPERTY_VERTICAL)
    private JSONMDQuantity vertical;
    @JsonProperty(PROPERTY_ANGULAR_DISTANCE)
    private JSONMDQuantity angularDistance;
    @JsonProperty(PROPERTY_LEVEL_OF_DETAIL)
    private String levelOfDetail;

    public JSONMDResolution() {
    }

    public JSONMDResolution(final Resolution source, final JSONMDEncoder encoder) {
        equivalentScale = (source.getEquivalentScale() == null) ? null : new JSONMDRepresentativeFraction(source.getEquivalentScale());
        distance        = quantity(source.getDistance(), encoder.unit(Units.METRE));
        vertical        = quantity(source.getVertical(), encoder.unit(Units.METRE));
        angularDistance = quantity(source.getAngularDistance(), encoder.unit(Units.DEGREE));
        levelOfDetail   = encoder.text(source.getLevelOfDetail());
    }

    private static JSONMDQuantity quantity(final Double value, final String uom) {
        return (value == null) ? null : new JSONMDQuantity(value, uom);
    }

    public JSONMDRepresentativeFraction getEquivalentScale() {
        return equivalentScale;
    }

    public void setEquivalentScale(final JSONMDRepresentativeFraction equivalentScale) {
        this.equivalentScale = equivalentScale;
    }

    public JSONMDQuantity getDistance() {
        return distance;
    }

    public void setDistance(final JSONMDQuantity distance) {
        this.distance = distance;
    }

    public JSONMDQuantity getVertical() {
        return vertical;
    }

    public void setVertical(final JSONMDQuantity vertical) {
        this.vertical = vertical;
    }

    public JSONMDQuantity getAngularDistance() {
        return angularDistance;
    }

    public void setAngularDistance(final JSONMDQuantity angularDistance) {
        this.angularDistance = angularDistance;
    }

    public String getLevelOfDetail() {
        return levelOfDetail;
    }

    public void setLevelOfDetail(final String levelOfDetail) {
        this.levelOfDetail = levelOfDetail;
    }

    /**
     * Returns the resolution, or {@code null} with a warning if no property is set.
     */
    public DefaultResolution toMetadata(final JSONMDDecoder decoder) {
        final DefaultResolution target = new DefaultResolution();
        if (equivalentScale != null) {
            target.setEquivalentScale(equivalentScale.toMetadata(decoder));
        } else if (distance != null) {
            target.setDistance(distance.toValue(Units.METRE, decoder));
        } else if (vertical != null) {
            target.setVertical(vertical.toValue(Units.METRE, decoder));
        } else if (angularDistance != null) {
            target.setAngularDistance(angularDistance.toValue(Units.DEGREE, decoder));
        } else if (levelOfDetail != null) {
            target.setLevelOfDetail(decoder.text(levelOfDetail));
        } else {
            decoder.warning("Resolution without value is ignored.");
            return null;
        }
        return target;
    }
}
