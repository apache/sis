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

import org.apache.sis.geometries.privy.AbstractGeometry;

/**
 * A MultiPolygon is a MultiSurface whose elements are Polygons.
 *
 * The assertions for MultiPolygons are as follows.
 *
 * a) The interiors of 2 Polygons that are elements of a MultiPolygon may not intersect.
 * b) The boundaries of any 2 Polygons that are elements of a MultiPolygon may not “cross” and may touch at only a
 *    finite number of Points.
 * c) A MultiPolygon is defined as topologically closed.
 * d) A MultiPolygon may not have cut lines, spikes or punctures, a MultiPolygon is a regular closed Point set.
 * e) The interior of a MultiPolygon with more than 1 Polygon is not connected; the number of connected components
 *    of the interior of a MultiPolygon is equal to the number of Polygons in the MultiPolygon.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface MultiPolygon extends MultiSurface<Polygon> {

    public static final String TYPE = "MULTIPOLYGON";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("MULTIPOLYGON (");
        for (int k = 0, kn = getNumGeometries(); k < kn; k++){
            if (k > 0) sb.append(',');
            final Polygon polygon = getGeometryN(k);
            sb.append("((");
            AbstractGeometry.toText(sb,  polygon.getExteriorRing().asLine(null, null).getPoints());
            sb.append(')');
            for (int i = 0, n = polygon.getNumInteriorRing(); i < n; i++) {
                if (i != 0) sb.append(',');
                sb.append('(');
                AbstractGeometry.toText(sb, polygon.getInteriorRingN(i).asLine(null, null).getPoints());
                sb.append(')');
            }
            sb.append(')');
        }
        sb.append(')');
        return sb.toString();
    }
}
