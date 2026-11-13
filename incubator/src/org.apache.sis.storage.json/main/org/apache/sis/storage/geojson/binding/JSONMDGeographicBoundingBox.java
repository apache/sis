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
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.opengis.metadata.extent.GeographicBoundingBox;

/**
 * Binding of the {@code EX_GeographicBoundingBox} JSON type.
 *
 * <p>Constraints:
 * <ul>
 *   <li>the four bounds are mandatory, a missing bound is read as {@code NaN}.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#EX_GeographicBoundingBox</a>
 */
@JsonPropertyOrder({
    JSONMDGeographicExtent.PROPERTY_TYPE,
    JSONMDGeographicExtent.PROPERTY_EXTENT_TYPE_CODE,
    JSONMDGeographicBoundingBox.PROPERTY_WEST_BOUND_LONGITUDE,
    JSONMDGeographicBoundingBox.PROPERTY_EAST_BOUND_LONGITUDE,
    JSONMDGeographicBoundingBox.PROPERTY_SOUTH_BOUND_LATITUDE,
    JSONMDGeographicBoundingBox.PROPERTY_NORTH_BOUND_LATITUDE
})
public final class JSONMDGeographicBoundingBox extends JSONMDGeographicExtent {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "EX_GeographicBoundingBox";
    public static final String PROPERTY_WEST_BOUND_LONGITUDE = "westBoundLongitude";
    public static final String PROPERTY_EAST_BOUND_LONGITUDE = "eastBoundLongitude";
    public static final String PROPERTY_SOUTH_BOUND_LATITUDE = "southBoundLatitude";
    public static final String PROPERTY_NORTH_BOUND_LATITUDE = "northBoundLatitude";

    @JsonProperty(PROPERTY_WEST_BOUND_LONGITUDE)
    private Double westBoundLongitude;
    @JsonProperty(PROPERTY_EAST_BOUND_LONGITUDE)
    private Double eastBoundLongitude;
    @JsonProperty(PROPERTY_SOUTH_BOUND_LATITUDE)
    private Double southBoundLatitude;
    @JsonProperty(PROPERTY_NORTH_BOUND_LATITUDE)
    private Double northBoundLatitude;

    public JSONMDGeographicBoundingBox() {
    }

    public JSONMDGeographicBoundingBox(final GeographicBoundingBox source) {
        super(source);
        westBoundLongitude = source.getWestBoundLongitude();
        eastBoundLongitude = source.getEastBoundLongitude();
        southBoundLatitude = source.getSouthBoundLatitude();
        northBoundLatitude = source.getNorthBoundLatitude();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public Double getWestBoundLongitude() {
        return westBoundLongitude;
    }

    public void setWestBoundLongitude(final Double westBoundLongitude) {
        this.westBoundLongitude = westBoundLongitude;
    }

    public Double getEastBoundLongitude() {
        return eastBoundLongitude;
    }

    public void setEastBoundLongitude(final Double eastBoundLongitude) {
        this.eastBoundLongitude = eastBoundLongitude;
    }

    public Double getSouthBoundLatitude() {
        return southBoundLatitude;
    }

    public void setSouthBoundLatitude(final Double southBoundLatitude) {
        this.southBoundLatitude = southBoundLatitude;
    }

    public Double getNorthBoundLatitude() {
        return northBoundLatitude;
    }

    public void setNorthBoundLatitude(final Double northBoundLatitude) {
        this.northBoundLatitude = northBoundLatitude;
    }

    @Override
    public DefaultGeographicBoundingBox toMetadata(final JSONMDDecoder decoder) {
        final DefaultGeographicBoundingBox target = new DefaultGeographicBoundingBox();
        fill(target);
        target.setWestBoundLongitude(value(westBoundLongitude, PROPERTY_WEST_BOUND_LONGITUDE, decoder));
        target.setEastBoundLongitude(value(eastBoundLongitude, PROPERTY_EAST_BOUND_LONGITUDE, decoder));
        target.setSouthBoundLatitude(value(southBoundLatitude, PROPERTY_SOUTH_BOUND_LATITUDE, decoder));
        target.setNorthBoundLatitude(value(northBoundLatitude, PROPERTY_NORTH_BOUND_LATITUDE, decoder));
        return target;
    }

    private static double value(final Double bound, final String property, final JSONMDDecoder decoder) {
        if (bound == null) {
            decoder.warning("Missing bounding box property \"" + property + "\".");
            return Double.NaN;
        }
        return bound;
    }
}
