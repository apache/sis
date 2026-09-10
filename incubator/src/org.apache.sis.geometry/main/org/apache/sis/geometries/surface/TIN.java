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
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.internal.shared.DefaultTriangulatedSurface;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.maths.Tuple;


/**
 * A TIN (triangulated irregular network) is a PolyhedralSurface consisting only of Triangle patches.
 *
 * @author Johann Sorel (Geomatys)
 */
public sealed interface TIN extends TriangulatedSurface<Triangle>
        permits PreparedTIN,
                DefaultTriangulatedSurface,
                MeshPrimitive.Triangles,
                MeshPrimitive.TriangleFan,
                MeshPrimitive.TriangleStrip
{

    static final String TYPE = "TIN";

    @Override
    default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns {@link SurfaceInterpolation#TIN}: the triangulation of this surface is derived
     * from its own data points.
     *
     * @see ISO 19107:2019 - 6.4.27
     */
    @Override
    default List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.TIN);
    }

    /**
     * Produce a Well Known Text representation of this TIN.
     *
     * @return WKT string
     */
    default String asText() {
        final StringBuilder sb = new StringBuilder("TIN(");
        boolean first = true;
        Tuple corner;
        for (int i = 0, n = getNumPatches(); i < n; i++) {
            Triangle triangle = getPatchN(i);
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append("((");
            final DataPoints points = triangle.getExteriorRing().getDataPoints();
            corner = points.getPosition(0);
            AbstractGeometry.toText(sb, corner);

            sb.append(',');
            corner = points.getPosition(1);
            AbstractGeometry.toText(sb, corner);

            sb.append(',');
            corner = points.getPosition(2);
            AbstractGeometry.toText(sb, corner);

            sb.append(',');
            corner = points.getPosition(0);
            AbstractGeometry.toText(sb, corner);

            sb.append("))");
        }
        sb.append(')');
        return sb.toString();
    }
}
