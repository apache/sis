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
package org.apache.sis.geometries.point;

import javax.measure.quantity.Length;
import org.apache.sis.geometries.Geometry;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;


/**
 * A very large collection of data points, all in the same coordinate system.
 *
 * <p>The coordinate system may be augmented by parameters carrying some kind of attribute value at
 * the spatial projection of each point. The sheer size of a point cloud calls for compact encodings
 * and for a complete index.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>All the points share the coordinate system of the cloud.</li>
 *   <li>The number of points may exceed 2³², therefore indices are not limited to 32 bits.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 11.2.1, 11.2.2
 */
@UML(identifier="PointCloud", specification=ISO_19107)
public interface PointCloud {

    /**
     * Minimal region containing all the points of this cloud.
     *
     * <p>Difference with ISO 19107, which declares a {@code Range}: the extent is returned as an
     * {@link Envelope}, which is the geometric object containing all the points of the cloud.</p>
     *
     * @return extent of this cloud.
     *
     * @see ISO 19107:2019 - 11.2.2.2
     */
    @UML(identifier="range", specification=ISO_19107)
    Envelope getRange();

    /**
     * Number of points in this cloud.
     *
     * <p>Difference with ISO 19107, which declares the points as an array: the count is returned as
     * a {@code long} because the number of points may exceed 2³².</p>
     *
     * @return number of points in this cloud.
     *
     * @see ISO 19107:2019 - 11.2.2.3
     */
    long getNumPoints();

    /**
     * Returns the point at the given index.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>An implementation may derive the points instead of storing them, possibly through the
     *       {@linkplain #subCloud(Length, Envelope) sub-cloud} association.</li>
     * </ul>
     *
     * @param  index  index of the point, from 0 inclusive to {@link #getNumPoints()} exclusive.
     * @return point at the given index.
     *
     * @see ISO 19107:2019 - 11.2.2.3
     */
    @UML(identifier="points", specification=ISO_19107)
    DirectPosition getPoint(long index);

    /**
     * Returns a subset of this cloud, limited to the given extent and thinned to the given
     * average distance between points.
     *
     * <p>Difference with ISO 19107, which also takes a list of {@code Constraint}: no such type is
     * defined by this implementation, so additional constraints cannot be expressed yet.</p>
     *
     * @param  density  average distance between the points of the returned cloud.
     * @param  range    extent to which the returned cloud is limited.
     * @return a sub-cloud of this cloud.
     *
     * @see ISO 19107:2019 - 11.2.2.4
     */
    @UML(identifier="SubCloud", specification=ISO_19107)
    PointCloud subCloud(Length density, Envelope range);

    /**
     * Builds a simplicial complex from this cloud: a triangulated surface in 2 dimensions,
     * a tetrahedral solid in 3 dimensions, or a similar complex of any dimension.
     *
     * <p>Difference with ISO 19107, which returns a {@code SimplicialComplex} and also takes a list
     * of {@code Constraint}: neither type is defined by this implementation, so the result is
     * returned as a {@link Geometry} and additional constraints cannot be expressed yet.</p>
     *
     * @param  dimension  topological dimension of the simplices to build.
     * @param  density    average distance between the points to use.
     * @param  range      extent to which the computation is limited.
     * @return a simplicial complex built from this cloud.
     *
     * @see ISO 19107:2019 - 11.2.2.5
     */
    @UML(identifier="SimplicialComplex", specification=ISO_19107)
    Geometry simplicialComplex(int dimension, Length density, Envelope range);
}
