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
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.internal.shared.DefaultMultiPolygon;


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
public sealed interface MultiPolygon extends MultiSurface<Polygon>
        permits DefaultMultiPolygon
{

    static final String TYPE = "MULTIPOLYGON";

    @Override
    default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns {@link GeometryType#POLYGON}: all the elements of this collection are polygons.
     *
     * @see ISO 19107:2019 - 6.4.31.2
     */
    @Override
    default Set<GeometryType> getElementType() {
        return Set.of(GeometryType.POLYGON);
    }

}
