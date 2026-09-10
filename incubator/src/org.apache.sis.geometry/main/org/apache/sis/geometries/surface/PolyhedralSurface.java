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
package org.apache.sis.geometries.surface;

import java.util.List;
import javax.measure.quantity.Area;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.internal.shared.DefaultPolyhedralSurface;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A surface made of polygons connected along their common boundary curves.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>For each pair of polygons that touch, the common boundary is expressible as a finite
 *       collection of line strings.</li>
 *   <li>Each such line string is part of the boundary of at most 2 polygon patches.</li>
 *   <li>The <cite>top</cite> of two polygons sharing a common boundary is consistent: when their
 *       rings traverse the common boundary segment, they do so in opposite directions. Since the
 *       surface is contiguous, all its polygons are therefore consistently oriented.</li>
 *   <li>A non-orientable surface such as a Möbius band has no single surface representation; it can
 *       only be represented by a {@link MultiSurface}.</li>
 * </ul>
 *
 * <p>If each of those line strings is the boundary of exactly 2 polygon patches, then the surface is
 * a simple closed polyhedron, topologically isomorphic to the surface of a sphere. By the Jordan
 * surface theorem such a polyhedron encloses a solid topologically isomorphic to a ball, and the
 * <cite>top</cite> of the surface points either outward, in which case the surface is the exterior
 * boundary of that solid, or inward, in which case it bounds the infinite complement of that solid.
 * A ball with voids inside is therefore presented as one exterior shell and a number of interior
 * shells.</p>
 *
 * <p>Note: a {@link TIN} (triangulated irregular network) is a polyhedral surface consisting only of
 * {@link Triangle} patches.</p>
 *
 * @param  <T>  type of the polygonal patches of this surface.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.12
 * @see ISO 19107:2019 - 8.1.4
 */
@UML(identifier="PolyhedralSurface", specification=ISO_19107) // TODO extends geometry collection is ISO 19107
public sealed interface PolyhedralSurface<T extends Polygon> extends /*GeometryCollection<org.apache.sis.geometries.Polygon>,*/ Surface
        permits TriangulatedSurface,
                DefaultPolyhedralSurface
{

    /**
     * Well-known text keyword of this geometry type.
     */
    static final String TYPE = "POLYHEDRALSURFACE";

    /**
     * Returns {@value #TYPE}.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @Override
    default String getGeometryType() {
        return TYPE;
    }

//    @UML(identifier="segment", specification=ISO_19107)
//    @Override
//    public List<Primitive> getSegments();

    /**
     * Returns {@link SurfaceInterpolation#PLANAR}: every patch of this surface is a planar polygon.
     *
     * @see ISO 19107:2019 - 8.1.4
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.PLANAR);
    }

    /**
     * Returns the number of including polygons
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>At least one patch, since the patches are the segments of this surface.</li>
     * </ul>
     *
     * @return number of including polygons
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.12.2
     * @see ISO 19107:2019 - 8.1.4.3
     */
    int getNumPatches();

    /**
     * Returns a polygon in this surface, the order is arbitrary
     *
     * @param n patch index
     * @return polygon in this surface, the order is arbitrary
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.12.2
     * @see ISO 19107:2019 - 8.1.4.3
     */
    T getPatchN(int n);

    /**
     * Returns the sum of the areas of the patches.
     * The unit of measurement is the one of the first patch,
     * or square metres if this surface has no patch.
     *
     * @see ISO 19107:2019 - 6.4.25.7
     */
    @Override
    default Area getArea() {
        final int n = getNumPatches();
        if (n == 0) {
            return Quantities.create(0, Units.SQUARE_METRE);
        }
        Area area = getPatchN(0).getArea();
        for (int i = 1; i < n; i++) {
            area = Quantities.castOrCopy(area.add(getPatchN(i).getArea()));
        }
        return area;
    }

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
