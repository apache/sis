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
import java.util.Map;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Binding of the {@code MD_ReferenceSystem} JSON type.
 *
 * <p>When reading, the {@code crs} URI is resolved to a coordinate reference system.
 * If there is no URI, or if it cannot be resolved, a reference system holding only the identifier is created,
 * as the SIS XML reader does.
 *
 * <p>Constraints:
 * <ul>
 *   <li>the {@code crs} URI is written only for coordinate reference systems having a known URN,</li>
 *   <li>{@code coordinateEpoch} is not supported and is ignored with a warning,</li>
 *   <li>{@code referenceSystemType} is lost when the reference system is not resolved.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.5"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_ReferenceSystem</a>
 */
@JsonPropertyOrder({
    JSONMDReferenceSystem.PROPERTY_REFERENCE_SYSTEM_IDENTIFIER,
    JSONMDReferenceSystem.PROPERTY_CRS,
    JSONMDReferenceSystem.PROPERTY_COORDINATE_EPOCH,
    JSONMDReferenceSystem.PROPERTY_REFERENCE_SYSTEM_TYPE
})
public final class JSONMDReferenceSystem extends DataTransferObject {

    public static final String PROPERTY_REFERENCE_SYSTEM_IDENTIFIER = "referenceSystemIdentifier";
    public static final String PROPERTY_CRS = "crs";
    public static final String PROPERTY_COORDINATE_EPOCH = "coordinateEpoch";
    public static final String PROPERTY_REFERENCE_SYSTEM_TYPE = "referenceSystemType";

    @JsonProperty(PROPERTY_REFERENCE_SYSTEM_IDENTIFIER)
    private JSONMDIdentifier referenceSystemIdentifier;
    @JsonProperty(PROPERTY_CRS)
    private String crs;
    @JsonProperty(PROPERTY_COORDINATE_EPOCH)
    private Double coordinateEpoch;
    @JsonProperty(PROPERTY_REFERENCE_SYSTEM_TYPE)
    private String referenceSystemType;

    public JSONMDReferenceSystem() {
    }

    public JSONMDReferenceSystem(final ReferenceSystem source, final JSONMDEncoder encoder) {
        referenceSystemIdentifier = (source.getName() == null) ? null : new JSONMDIdentifier(source.getName(), encoder);
        crs                       = (source instanceof CoordinateReferenceSystem) ? encoder.crs(source) : null;
        referenceSystemType       = encoder.code(source.getReferenceSystemType().orElse(null));
    }

    public JSONMDIdentifier getReferenceSystemIdentifier() {
        return referenceSystemIdentifier;
    }

    public void setReferenceSystemIdentifier(final JSONMDIdentifier referenceSystemIdentifier) {
        this.referenceSystemIdentifier = referenceSystemIdentifier;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(final String crs) {
        this.crs = crs;
    }

    public Double getCoordinateEpoch() {
        return coordinateEpoch;
    }

    public void setCoordinateEpoch(final Double coordinateEpoch) {
        this.coordinateEpoch = coordinateEpoch;
    }

    public String getReferenceSystemType() {
        return referenceSystemType;
    }

    public void setReferenceSystemType(final String referenceSystemType) {
        this.referenceSystemType = referenceSystemType;
    }

    /**
     * Returns the reference system, or {@code null} with a warning if there is neither URI nor identifier.
     */
    public ReferenceSystem toMetadata(final JSONMDDecoder decoder) {
        if (coordinateEpoch != null) {
            decoder.warning("Coordinate epoch of reference system is not supported and is ignored.");
        }
        if (crs != null) {
            final CoordinateReferenceSystem resolved = decoder.crs(crs);
            if (resolved != null) {
                return resolved;
            }
        }
        final Object name;
        if (referenceSystemIdentifier != null) {
            name = referenceSystemIdentifier.toMetadata(decoder);
        } else if (crs != null) {
            name = crs;
        } else {
            decoder.warning("Reference system without identifier is ignored.");
            return null;
        }
        if (referenceSystemType != null) {
            decoder.warning("Reference system type \"" + referenceSystemType + "\" is lost for unresolved reference systems.");
        }
        try {
            return new AbstractReferenceSystem(Map.of(ReferenceSystem.NAME_KEY, name));
        } catch (IllegalArgumentException e) {
            decoder.warning("Invalid reference system identifier: " + e.getMessage());
            return null;
        }
    }
}
