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

import java.util.List;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.coordinate.GriddedSurface;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;


/**
 * A Polygon is a planar Surface defined by 1 exterior boundary and 0 or more interior boundaries.
 * Each interior boundary defines a hole in the Polygon. A Triangle is a polygon with 3 distinct, non-collinear
 * vertices and no interior boundary.
 *
 * The exterior boundary LinearRing defines the “top” of the surface which is the side of the surface from which
 * the exterior boundary appears to traverse the boundary in a counter clockwise direction.
 * The interior LinearRings will have the opposite orientation, and appear as clockwise when viewed from the “top”,
 *
 * The assertions for Polygons (the rules that define valid Polygons) are as follows:
 *
 * a) Polygons are topologically closed;
 * b) The boundary of a Polygon consists of a set of LinearRings that make up its exterior and interior boundaries;
 * c) No two Rings in the boundary cross and the Rings in the boundary of a Polygon may intersect at a Point but only as a tangent.
 * d) A Polygon may not have cut lines, spikes or punctures
 * e) The interior of every Polygon is a connected point set;
 * f) The exterior of a Polygon with 1 or more holes is not connected. Each hole defines a connected component of the exterior.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Polygon", specification=ISO_19107) // section 8.1.2
public interface Polygon extends Surface {

    public static final String TYPE = "POLYGON";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @Override
    public default AttributesType getAttributesType() {
        return getExteriorRing().getAttributesType();
    }

    @UML(identifier="rings", specification=ISO_19107) // section 8.1 figure 28
    List<LinearRing> getInteriorRings();

    /**
     * Returns the exterior ring of this Polygon.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.11.2
     * @return exterior ring of this Polygon.
     */
    @UML(identifier="exteriorRing", specification=ISO_19107) // section 8.1 figure 28
    LinearRing getExteriorRing();

    /**
     * Returns the number of interior rings in this Polygon.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.11.2
     * @return number of interior rings in this Polygon.
     */
    @UML(identifier="numInteriorRing", specification=ISO_19107) // section 8.1 figure 28
    default int getNumInteriorRing() {
        return getInteriorRings().size();
    }

    /**
     * Returns the Nth interior ring for this Polygon as a LineString.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.11.2
     * @param n ring index
     * @return interior ring for this Polygon.
     */
    @UML(identifier="interiorRingN", specification=ISO_19107) // section 8.1 figure 28
    default LinearRing getInteriorRingN(int n) {
        return getInteriorRings().get(n);
    }

    @UML(identifier="spanningSurface", specification=ISO_19107) // section 8.1.2.3
    default GriddedSurface getSpanningSurface() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("POLYGON ((");
        AbstractGeometry.toText(sb,  getExteriorRing().asLine(null, null).getPoints());
        sb.append(')');
        for (int i = 0, n = getNumInteriorRing(); i < n; i++) {
            if (i != 0) sb.append(',');
            sb.append('(');
            AbstractGeometry.toText(sb, getInteriorRingN(i).getPoints());
            sb.append(')');
        }
        sb.append(')');
        return sb.toString();
    }

}
