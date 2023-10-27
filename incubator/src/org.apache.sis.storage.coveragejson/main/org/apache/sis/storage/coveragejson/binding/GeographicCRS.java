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
package org.apache.sis.storage.coveragejson.binding;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.Objects;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * Geographic CRSs anchor coordinate values to an ellipsoidal approximation of
 * the Earth. They have coordinate axes of geodetic longitude and geodetic
 * latitude, and perhaps height above the ellipsoid (i.e. they can be two- or three-dimensional).
 * The origin of the CRS is on the surface of the ellipsoid.
 *
 * Note that sometimes (e.g. for numerical model data) the exact CRS may not be
 * known or may be undefined. In this case the "id" may be omitted, but the "type"
 * still indicates that this is a geographic CRS. Therefore clients can still use
 * geodetic longitude, geodetic latitude (and maybe height) axes, even if they
 * cannot accurately georeference the information.
 * If a Coverage conforms to one of the defined domain types then the coordinate
 * identifier "x" is used to denote geodetic longitude, "y" is used for geodetic
 * latitude and "z" for ellipsoidal height.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","id","description"})
public final class GeographicCRS extends CoverageJsonObject {
    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The object MAY have an "id" member, whose value MUST be a string and
     * SHOULD be a common identifier for the reference system.
     */
    public String id;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The object MAY have a "description" member, where the value MUST be an
     * i18n object, but no standardized content is interpreted from this description.
     */
    public I18N description;

    public GeographicCRS() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof GeographicCRS)) return false;

        final GeographicCRS cdt = ((GeographicCRS) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(description, cdt.description);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                description);
    }
}
