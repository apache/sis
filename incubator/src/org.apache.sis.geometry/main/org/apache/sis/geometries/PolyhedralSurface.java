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

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;

/**
 * A PolyhedralSurface is a contiguous collection of polygons, which share common boundary segments.
 *
 * For each pair of polygons that “touch”, the common boundary shall be expressible as a finite collection of LineStrings.
 * Each such LineString shall be part of the boundary of at most 2 Polygon patches.
 * A TIN (triangulated irregular network) is a PolyhedralSurface consisting only of Triangle patches.
 * For any two polygons that share a common boundary, the
 * “top” of the polygon shall be consistent. This means that when two LinearRings from these two Polygons traverse the
 * common boundary segment, they do so in opposite directions. Since the Polyhedral surface is contiguous, all polygons
 * will be thus consistently oriented. This means that a non-oriented surface (such as Möbius band) shall not have
 * single surface representations. They may be represented by a MultiSurface.
 *
 * If each such LineString is the boundary of exactly 2 Polygon patches, then the PolyhedralSurface is a simple, closed
 * polyhedron and is topologically isomorphic to the surface of a sphere. By the Jordan Surface Theorem
 * (Jordan’s Theorem for 2-spheres), such polyhedrons enclose a solid topologically isomorphic to the interior of a
 * sphere; the ball. In this case, the “top” of the surface will either point inward or outward of the enclosed finite
 * solid. If outward, the surface is the exterior boundary of the enclosed surface. If inward, the surface is the
 * interior of the infinite complement of the enclosed solid. A Ball with some number of voids (holes) inside can
 * thus be presented as one exterior boundary shell, and some number in interior boundary shells.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="PolyhedralSurface", specification=ISO_19107) // section 8.1.4 TODO extends geometry collection is ISO 19107
public interface PolyhedralSurface<T extends Polygon> extends /*GeometryCollection<org.apache.sis.geometries.Polygon>,*/ Surface {

    public static final String TYPE = "POLYHEDRALSURFACE";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

//    @UML(identifier="segment", specification=ISO_19107) // section 8.1.4.3
//    @Override
//    public List<Primitive> getSegments();

    /**
     * Returns the number of including polygons
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.12.2
     * @return number of including polygons
     */
    int getNumPatches();

    /**
     * Returns a polygon in this surface, the order is arbitrary
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.12.2
     * @param n patch index
     * @return polygon in this surface, the order is arbitrary
     */
    T getPatchN(int n);

    /**
     * Returns the collection of polygons in this surface that bounds the given polygon “p” for any polygon “p” in the surface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.12.2
     * @param p searched polygons.
     * @return collection of polygons in this surface that bounds the given polygon
     */
    default MultiPolygon getBoundingPolygons(Polygon p) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if the polygon closes on itself, and thus has no boundary and encloses a solid.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.12.2
     * @return true polygon closes on itself
     */
    default boolean isClosed() {
        throw new UnsupportedOperationException();
    }

}
