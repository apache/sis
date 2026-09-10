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
import javax.measure.quantity.Area;
import javax.measure.quantity.Volume;
import org.apache.sis.geometries.solid.ParametricCurveSolid;
import org.apache.sis.geometries.solid.Polyhedron;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;


/**
 * A 3-dimensional geometric primitive, bounded by a set of simple closed surfaces called shells.
 *
 * <p>In a 3-dimensional space a solid is fully determined by its boundary, which is the usual
 * boundary representation (B-REP). An internal {@linkplain #getInterpolation() interpolation} is
 * only needed to describe a non-homogeneous interior, as found in coverages or in solid modelling.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The topological dimension is 3, therefore a solid only exists in a coordinate system
 *       having three spatial dimensions.</li>
 *   <li>A solid is connected: any two of its interior positions can be joined by a curve which
 *       stays entirely inside the solid.</li>
 *   <li>A solid is always bounded: it has a finite envelope and contains no point at infinity.</li>
 *   <li>The solid lies below every one of its shells, as given by the upward surface normal.</li>
 *   <li>Every shell is simple, neither self-intersecting nor self-tangent, and closed.</li>
 *   <li>Two shells of the same solid may be tangent to each other at a single position or along a
 *       simple non-closed curve, lower side against lower side, meaning their upward normals point
 *       in opposite directions at the contact.</li>
 * </ul>
 *
 * <p>In a 3-dimensional coordinate system each shell splits the space in a bounded and an unbounded
 * region per the Jordan-Schönflies theorem, so the shell with the largest envelope is the exterior
 * one and the others are interior. Each shell defines the volume opposite to its upward normal.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.28
 */
@UML(identifier="Solid", specification=ISO_19107)
public sealed interface Solid extends Primitive
        permits ParametricCurveSolid,
                Polyhedron
{

    /**
     * Returns 3: a solid bounds a volume.
     *
     * @see ISO 19107:2019 - 6.4.4.22
     */
    @Override
    default int getTopologicDimension() {
        return 3;
    }

    /**
     * Shells bounding this solid, each of them a closed surface without boundary.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>At least one shell.</li>
     *   <li>Every shell is a cycle, therefore a closed composite surface having an empty boundary.</li>
     *   <li>The shells are oriented outward: the upward normal of each of them faces away from the
     *       interior of this solid.</li>
     *   <li>The shell with the largest envelope is the exterior one, which is well defined only
     *       because the enclosing coordinate space is a 3-dimensional Euclidean space.</li>
     * </ul>
     *
     * @return boundary of this solid.
     *
     * @see ISO 19107:2019 - 6.4.28.2
     */
    @UML(identifier="boundary", specification=ISO_19107)
    default Geometry getBoundary() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sum of the areas of all the boundary surfaces of this solid.
     *
     * @return area of the boundary of this solid.
     *
     * @see ISO 19107:2019 - 6.4.28.3
     */
    @UML(identifier="area", specification=ISO_19107)
    default Area getArea() {
        throw new UnsupportedOperationException();
    }

    /**
     * Volume enclosed by this solid, that is the volume interior to the exterior shell
     * and exterior to any interior shell.
     *
     * @return volume of this solid.
     *
     * @see ISO 19107:2019 - 6.4.28.4
     */
    @UML(identifier="volume", specification=ISO_19107)
    default Volume getVolume() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sample positions on the interior and on the boundary of this solid, as a point cloud.
     * Subtypes may add requirements on this collection.
     *
     * <p>Difference with ISO 19107: the type has been changed from a list of direct positions
     * to {@link DataPoints}, in order to accommodate additional attributes like in GLTF or GPU models.</p>
     *
     * @return solid data points, possibly empty.
     *
     * @see ISO 19107:2019 - 6.4.28.5
     */
    @UML(identifier="dataPoint", specification=ISO_19107)
    default DataPoints getDataPoints() {
        throw new UnsupportedOperationException();
    }

    /**
     * Positions used to build the geometry of this solid, the way they are used depending on the interpolation.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Control points need not lie inside this solid.</li>
     *   <li>Interpolated values are only valid for the control points enclosed by the
     *       {@linkplain #getBoundary() boundary} of this solid.</li>
     * </ul>
     *
     * @return solid control points, possibly empty.
     *
     * @see ISO 19107:2019 - 6.4.28.6
     */
    @UML(identifier="controlPoint", specification=ISO_19107)
    default List<DirectPosition> getControlPoints() {
        throw new UnsupportedOperationException();
    }

    /**
     * Interpolation mechanism defining the internal parameterization of this solid.
     * The default is a boundary representation, which leaves the interior unspecified.
     *
     * @return interpolation used by this solid.
     *
     * @see ISO 19107:2019 - 6.4.28.7, 6.4.30
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    default SolidInterpolation getInterpolation() {
        throw new UnsupportedOperationException();
    }

    /**
     * Construction parameter values matching the {@linkplain #getDataPoints() data points},
     * one sequence for each of the three solid parameters.
     * Needed only by parametric solids, such as 3-dimensional b-splines.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Three sequences, in the horizontal, vertical and depth parameter order.</li>
     *   <li>Values in each sequence are monotonic.</li>
     * </ul>
     *
     * @return knot values in the construction (knot) space, one sequence per solid parameter.
     *
     * @see ISO 19107:2019 - 6.4.28.8
     */
    @UML(identifier="knot", specification=ISO_19107)
    default List<double[]> getKnots() {
        throw new UnsupportedOperationException();
    }

}
