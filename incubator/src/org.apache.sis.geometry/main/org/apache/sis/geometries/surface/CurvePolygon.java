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
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.internal.shared.DefaultCurvePolygon;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.coordinate.GriddedSurface;


/**
 * A surface whose rings may use any curve interpolation, such as a closed line string,
 * a circular string or a compound curve.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>A curve polygon is topologically closed and its interior is a connected point set.</li>
 *   <li>Every ring is oriented so that its left side faces the interior of this surface.</li>
 *   <li>No two rings cross; they may intersect at a position, but only as a tangent.</li>
 *   <li>Where the geometric reference surface is unbounded, the first ring is the exterior boundary
 *       and all the other rings are interior boundaries.</li>
 *   <li>Where the geometric reference surface is bounded and closed, such as a sphere, the
 *       distinction between exterior and interior rings does not apply.</li>
 * </ul>
 *
 * @todo is Polygon a subclass of CurvePolygon ?
 * ISO-19107 use the name Polygon for CurvePolygon
 * OGC Features and Geometries JSON separates them
 * In practice most geometry library have only polygon with straight lines
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#curve_polygon
 * @see ISO 19107:2019 - 8.1.1, 8.1.2
 */
@UML(identifier="Polygon", specification=ISO_19107)
public sealed interface CurvePolygon extends Surface
        permits DefaultCurvePolygon
{

    /**
     * Well-known text keyword of this geometry type.
     */
    public static final String TYPE = "CURVEPOLYGON";

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
     * Returns the attributes of the exterior ring, which are the attributes of this surface.
     */
    @Override
    public default AttributesType getAttributesType() {
        return getExteriorRing().getAttributesType();
    }

    /**
     * Rings bounding the holes of this surface, each of them oriented clockwise
     * when viewed from the <cite>top</cite> of this surface.
     *
     * @return interior rings of this surface, possibly empty.
     *
     * @see ISO 19107:2019 - 8.1.2.2
     */
    @UML(identifier="rings", specification=ISO_19107)
    List<Curve> getInteriorRings();

    /**
     * Returns the exterior ring of this Polygon.
     * It defines the <cite>top</cite> of this surface and is oriented counter-clockwise
     * when viewed from that side.
     *
     * @return exterior ring of this Polygon.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.11.2
     * @see ISO 19107:2019 - 8.1.2.2
     */
    @UML(identifier="exteriorRing", specification=ISO_19107)
    Curve getExteriorRing();

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
    default Curve getInteriorRingN(int n) {
        return getInteriorRings().get(n);
    }

    /**
     * Surface which the rings of this surface adhere to, extending its interior in 3 dimensions.
     * A common spanning surface is an elevation model.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>No boundary component of the spanning surface crosses the boundary of this surface,
     *       so that the portion of it enclosed by the rings is unambiguous.</li>
     *   <li>When a spanning surface is used, the coordinate system of this surface may be the
     *       parameter space of that surface rather than a more classical reference system.</li>
     * </ul>
     *
     * @return spanning surface of this surface, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 8.1.2.3
     */
    @UML(identifier="spanningSurface", specification=ISO_19107)
    default GriddedSurface getSpanningSurface() {
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
            AbstractGeometry.toText(sb, getInteriorRingN(i).asLine(null, null).getDataPoints());
            sb.append(')');
        }
        sb.append(')');
        return sb.toString();
    }

}
