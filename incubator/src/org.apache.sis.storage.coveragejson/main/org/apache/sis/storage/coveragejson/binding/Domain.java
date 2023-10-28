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

import java.util.List;
import java.util.Objects;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * A domain object is a CoverageJSON object which defines a set of positions and
 * their extent in one or more referencing systems.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","domainType","axes","referencing"})
public final class Domain extends CoverageJsonObject {

    public static final String DOMAINTYPE_GRID = "Grid";
    public static final String DOMAINTYPE_VERTICALPROFILE = "VerticalProfile";
    public static final String DOMAINTYPE_POINTSERIES = "PointSeries";
    public static final String DOMAINTYPE_POINT = "Point";
    public static final String DOMAINTYPE_MULTIPOINTSERIES = "MultiPointSeries";
    public static final String DOMAINTYPE_MULTIPOINT = "MultiPoint";
    public static final String DOMAINTYPE_POLYGONSERIES = "PolygonSeries";
    public static final String DOMAINTYPE_POLYGON = "Polygon";
    public static final String DOMAINTYPE_MULTIPOLYGONSERIES = "MultiPolygonSeries";
    public static final String DOMAINTYPE_MULTIPOLYGON = "MultiPolygon";
    public static final String DOMAINTYPE_TRAJECTORY = "Trajectory";
    public static final String DOMAINTYPE_SECTION = "Section";

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * For interoperability reasons it is RECOMMENDED that a domain object has
     * the member "domainType" with a string value to indicate that the domain
     * follows a certain structure (e.g. a time series, a vertical profile, a
     * spatio-temporal 4D grid). See the section Common Domain Types for details.
     * Custom domain types may be used as recommended in the section Extensions.
     */
    public String domainType;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A domain object MUST have the member "axes" which has as value an object
     * where each key is an axis identifier and each value an axis object as defined below.
     */
    public Axes axes;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A domain object MAY have the member "referencing" where the value is an
     * array of reference system connection objects as defined below.
     *
     * A domain object MUST have a "referencing" member if the domain object is
     * not part of a coverage collection or if the coverage collection does not
     * have a "referencing" member.
     */
    public List<ReferenceSystemConnection> referencing;

    public Domain() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Domain)) return false;

        final Domain cdt = ((Domain) other);
        return super.equals(other)
            && Objects.equals(domainType, cdt.domainType)
            && Objects.equals(axes, cdt.axes)
            && Objects.equals(referencing, cdt.referencing);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                domainType,
                axes,
                referencing);
    }
}
