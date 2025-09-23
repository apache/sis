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
 * A curve polygon is a surface where each ring is a closed line string, circular string, or compound curve.
 * The first ring is the exterior boundary and, all other rings are interior boundaries.
 *
 * @todo is Polygon a subclass of CurvePolygon ?
 * ISO-19107 use the name Polygon for CurvePolygon
 * OGC Features and Geometries JSON separates them
 * In practice most geometry library have only polygon with straight lines
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#curve_polygon
 */
@UML(identifier="Polygon", specification=ISO_19107) // section 8.1.2
public interface CurvePolygon extends Surface {

    public static final String TYPE = "CURVEPOLYGON";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @Override
    public default AttributesType getAttributesType() {
        return getExteriorRing().getAttributesType();
    }

    @UML(identifier="rings", specification=ISO_19107) // section 8.1 figure 28
    List<Curve> getInteriorRings();

    /**
     * Returns the exterior ring of this Polygon.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.11.2
     * @return exterior ring of this Polygon.
     */
    @UML(identifier="exteriorRing", specification=ISO_19107) // section 8.1 figure 28
    Curve getExteriorRing();

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
    default Curve getInteriorRingN(int n) {
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
            AbstractGeometry.toText(sb, getInteriorRingN(i).asLine(null, null).getPoints());
            sb.append(')');
        }
        sb.append(')');
        return sb.toString();
    }

}
