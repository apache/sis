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
package org.apache.sis.geometries;

import org.opengis.referencing.crs.SingleCRS;
import org.apache.sis.geometries.internal.shared.DefaultPrism;
import org.apache.sis.measure.NumberRange;


/**
 * A prism is defined by a base shape (e.g. Polygon or Circle) that is then extruded from some optional lower limit to an upper limit.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#prism
 */
public sealed interface Prism extends Geometry
        permits DefaultPrism {

    static final String TYPE = "PRISM";

    @Override
    default GeometryType getGeometryType() {
        return GeometryType.PRISM;
    }

    @Override
    default DataPointsType getDataPointsType() {
        return getBase().getDataPointsType();
    }

    /**
     * Returns the topological dimension of this prism, which is one more than the dimension of
     * the base shape it extrudes: extruding a point gives a curve, extruding a curve gives a
     * surface, and extruding a surface gives a solid.
     */
    @Override
    default int getTopologicDimension() {
        return getBase().getTopologicDimension() + 1;
    }

    /**
     * Returns the positions of the geometry this prism is the extrusion of. The positions the
     * extrusion adds are not materialized, so they are not part of the returned {@code DataPoints}.
     */
    @Override
    default DataPoints getDataPoints() {
        return getBase().getDataPoints();
    }

    /**
     * @return base shape of the prism.
     */
    Geometry getBase();

    /**
     * @return lower and upper limits of the extrusion
     */
    NumberRange<?> getExtrusionRange();

    /**
     * @return the extrusion crs
     */
    SingleCRS getExtrusionCrs();

    /**
     * Returns the geometry covered by the extrusion of the base, which depends on the type of the base:
     *
     * <ul>
     *   <li>A point becomes the line string going from the lower limit to the upper limit.</li>
     *   <li>A line string becomes a polyhedral surface made of one quadrilateral for each segment.</li>
     *   <li>A polygon becomes a polyhedron whose exterior shell is made of the polygon at the lower limit,
     *       the polygon at the upper limit, and one quadrilateral for each segment of its rings.
     *       All the faces are oriented outward.</li>
     *   <li>A geometry collection becomes the collection of the extrusions of its elements,
     *       which shall be of the above types or collections of them.</li>
     * </ul>
     *
     * <p>If the reference system of the base contains the extrusion reference system, the extrusion
     * replaces the coordinate of the corresponding axis. Otherwise the extrusion adds a dimension,
     * and the result is in the compound of the two reference systems.</p>
     *
     * <p>Each position of the result is a copy of a position of the base, with the extrusion
     * coordinate set to one of the limits. The other attributes of that position are copied unchanged.</p>
     *
     * @return the extrusion of the base.
     * @throws UnsupportedOperationException if the base or one of its elements is of another type,
     *         if the extrusion reference system has more than one dimension,
     *         or if the extrusion of a polygon would not be 3-dimensional.
     */
    Geometry toExtrusion();

}
