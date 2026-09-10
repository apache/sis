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

import java.util.Set;
import javax.measure.quantity.Area;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.internal.shared.DefaultMultiSurface;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;


/**
 * A MultiSurface is a 2-dimensional GeometryCollection whose elements are Surfaces, all using coordinates from
 * the same coordinate reference system. The geometric interiors of any two Surfaces in a MultiSurface may not intersect
 * in the full coordinate system. The boundaries of any two coplanar elements in a MultiSurface may intersect, at most,
 * at a finite number of Points. If they were to meet along a curve, they could be merged into a single surface.
 *
 * MultiSurface is an instantiable class in this Standard, and may be used to represent heterogeneous surfaces
 * collections of polygons and polyhedral surfaces. It defines a set of methods for its subclasses. The subclass of
 * MultiSurface is MultiPolygon corresponding to a collection of Polygons only. Other collections shall use MultiSurface.
 *
 * NOTE: The geometric relationships and sets are the common geometric ones in the full coordinate systems.
 * The use of the 2D map operations defined Clause 6.1.15 may classify the elements of a valid 3D MultiSurface as
 * having overlapping interiors in their 2D projections.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#multi_surface
 */
public sealed interface MultiSurface<T extends Surface> extends GeometryCollection<T>
        permits MultiPolygon,
                DefaultMultiSurface
{

    static final String TYPE = "MULTISURFACE";

    @Override
    default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns {@link GeometryType#SURFACE}: all the elements of this collection are surfaces.
     *
     * @see ISO 19107:2019 - 6.4.31.2
     */
    @Override
    default Set<GeometryType> getElementType() {
        return Set.of(GeometryType.SURFACE);
    }

    /**
     * The area of this MultiSurface, as measured in the spatial reference system of this MultiSurface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.13.2
     * @return area of the surface.
     */
    default Area getArea() {
        final int n = getNumGeometries();
        if (n == 0) {
            return Quantities.create(0, Units.SQUARE_METRE);
        }
        Area area = getGeometryN(0).getArea();
        for (int i = 1; i < n; i++) {
            area = Quantities.castOrCopy(area.add(getGeometryN(i).getArea()));
        }
        return area;
    }

    /**
     * The mathematical centroid for this MultiSurface.
     * The result is not guaranteed to be on this MultiSurface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.13.2
     * @return centroid for this MultiSurface
     */
    @Override
    default Point getCentroid() {
        throw new UnsupportedOperationException();
    }

    /**
     * A Point guaranteed to be on this MultiSurface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.13.2
     * @return point guaranteed to be on this MultiSurface.
     */
    default Point getPointOnSurface() {
        throw new UnsupportedOperationException();
    }
}
