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
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.internal.shared.DefaultPolygon;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A surface defined only by its boundary rings and by the surface those rings adhere to.
 *
 * <p>The exterior boundary defines the <cite>top</cite> of the surface, which is the side from which
 * that boundary appears to be traversed counter-clockwise. The interior rings have the opposite
 * orientation and appear clockwise when viewed from the <cite>top</cite>. A {@link Triangle} is a
 * polygon with 3 distinct, non-collinear vertices and no interior boundary.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>A polygon is topologically closed and its interior is a connected point set.</li>
 *   <li>The boundary consists of one exterior ring and zero or more interior rings, each interior
 *       ring defining a hole.</li>
 *   <li>Every ring is oriented so that its left side faces the interior of the polygon.</li>
 *   <li>No two rings cross; they may intersect at a position, but only as a tangent.</li>
 *   <li>A polygon has no cut line, spike or puncture.</li>
 *   <li>The exterior of a polygon with one or more holes is not connected: each hole defines a
 *       connected component of the exterior.</li>
 *   <li>Where the geometric reference surface is unbounded, the exterior ring comes first, as
 *       measured by its minimum bounding region, and the smaller rings follow in any order.</li>
 *   <li>Where the geometric reference surface is bounded and closed, such as a sphere, the
 *       distinction between exterior and interior rings does not apply.</li>
 * </ul>
 *
 * <p>Note: a position not on the boundary is interior to the polygon if a curve drawn from it, which
 * touches a boundary ring without crossing any, always arrives on the left side of that ring.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.11
 * @see ISO 19107:2019 - 8.1.1, 8.1.2
 */
@UML(identifier="Polygon", specification=ISO_19107)
public sealed interface Polygon extends Surface
        permits Triangle,
                DefaultPolygon
{

    /**
     * Well-known text keyword of this geometry type.
     */
    public static final String TYPE = "POLYGON";

    /**
     * Returns {@value #TYPE}.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns the attributes of the exterior ring, which are the attributes of this polygon.
     */
    @Override
    public default AttributesType getAttributesType() {
        return getExteriorRing().getAttributesType();
    }

    /**
     * Rings bounding the holes of this polygon, each of them oriented clockwise
     * when viewed from the <cite>top</cite> of this polygon.
     *
     * @return interior rings of this polygon, possibly empty.
     *
     * @see ISO 19107:2019 - 8.1.2.2
     */
    @UML(identifier="rings", specification=ISO_19107)
    List<LinearRing> getInteriorRings();

    /**
     * Returns the exterior ring of this Polygon.
     * It defines the <cite>top</cite> of this polygon and is oriented counter-clockwise
     * when viewed from that side.
     *
     * @return exterior ring of this Polygon.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.11.2
     * @see ISO 19107:2019 - 8.1.2.2
     */
    @UML(identifier="exteriorRing", specification=ISO_19107)
    LinearRing getExteriorRing();

    /**
     * Returns the number of interior rings in this Polygon.
     *
     * @return number of interior rings in this Polygon.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.11.2
     * @see ISO 19107:2019 - 8.1.2.2
     */
    @UML(identifier="numInteriorRing", specification=ISO_19107)
    default int getNumInteriorRing() {
        return getInteriorRings().size();
    }

    /**
     * Returns the Nth interior ring for this Polygon as a LineString.
     *
     * @param n ring index
     * @return interior ring for this Polygon.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.11.2
     * @see ISO 19107:2019 - 8.1.2.2
     */
    @UML(identifier="interiorRingN", specification=ISO_19107)
    default LinearRing getInteriorRingN(int n) {
        return getInteriorRings().get(n);
    }

    /**
     * Surface which the rings of this polygon adhere to, extending its interior in 3 dimensions.
     * A common spanning surface is an elevation model.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>No boundary component of the spanning surface crosses the boundary of this polygon,
     *       so that the portion of the surface enclosed by the rings is unambiguous.</li>
     *   <li>When a spanning surface is used, the coordinate system of this polygon may be the
     *       parameter space of that surface rather than a more classical reference system.</li>
     * </ul>
     *
     * @return spanning surface of this polygon, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 8.1.2.3
     */
    @UML(identifier="spanningSurface", specification=ISO_19107)
    default ParametricCurveSurface getSpanningSurface() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("POLYGON ((");
        AbstractGeometry.toText(sb,  getExteriorRing().asLine(null, null).getDataPoints());
        sb.append(')');
        for (int i = 0, n = getNumInteriorRing(); i < n; i++) {
            if (i != 0) sb.append(',');
            sb.append('(');
            AbstractGeometry.toText(sb, getInteriorRingN(i).getDataPoints());
            sb.append(')');
        }
        sb.append(')');
        return sb.toString();
    }

}
