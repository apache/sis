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
import javax.measure.quantity.Length;
import org.apache.sis.geometries.internal.shared.DefaultReversedSurface;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.ParametricCurveSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.maths.Vector;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;


/**
 * A 2-dimensional geometric primitive, bounded by a set of simple closed curves called rings.
 *
 * <p>The orientation of a surface picks an <cite>up</cite> direction, given by the
 * {@linkplain #upNormal(DirectPosition) up-normal}: the side from which the exterior boundary
 * appears counter-clockwise. Reversing the orientation reverses every boundary curve and swaps
 * <cite>up</cite> and <cite>down</cite>. When the surface bounds a solid, <cite>up</cite> is
 * also <cite>outward</cite>.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The topological dimension is 2.</li>
 *   <li>A surface is connected: any two of its interior positions can be joined by a curve which
 *       stays entirely inside the surface.</li>
 *   <li>A surface is always bounded: it has a finite envelope and contains no point at infinity.</li>
 *   <li>The surface lies on the left of every one of its rings.</li>
 *   <li>Every ring is simple, neither self-intersecting nor self-tangent, and closed.</li>
 *   <li>Two rings of the same surface may be tangent to each other, but only once per pair,
 *       at a single position, and left side against left side.</li>
 *   <li>A surface is orientable, therefore neither a Möbius strip nor a Klein bottle;
 *       such shapes can only be built as a geometric complex.</li>
 * </ul>
 *
 * <p>In a 2-dimensional coordinate system the interpolation is necessarily planar, each ring
 * splits the space in a bounded and an unbounded region per the Jordan theorem, the ring with the
 * largest envelope is the exterior one and the surface is the intersection of the surfaces defined
 * by its rings. On a bounded reference surface such as a sphere, no ring is exterior.</p>
 *
 * <p>Note: the following describes the OGC Simple Feature Access point of view, where the only
 * instantiable subtypes are {@link Polygon} and {@link PolyhedralSurface}.</p>
 *
 * A simple Surface may consists of a single “patch” that is associated with one “exterior boundary” and 0 or more
 * “interior” boundaries. A single such Surface patch in 3-dimensional space is isometric to planar Surfaces,
 * by a simple affine rotation matrix that rotates the patch onto the plane z = 0. If the patch is not vertical, the
 * projection onto the same plane is an isomorphism, and can be represented as a linear transformation, i.e. an affine.
 *
 * Polyhedral Surfaces are formed by “stitching” together such simple Surfaces patches along their common boundaries.
 * Such polyhedral Surfaces in a 3-dimensional space may not be planar as a whole, depending on the orientation of
 * their planar normals (Reference [1], sections 3.12.9.1, and 3.12.9.3). If all the patches are in alignment
 * (their normals are parallel), then the whole stitched polyhedral surface is co-planar and can be represented
 * as a single patch if it is connected.
 *
 * The boundary of a simple Surface is the set of closed Curves corresponding to its “exterior” and “interior”
 * boundaries (Reference [1], section 3.12.9.4).
 *
 * A Polygon is a simple Surface that is planar. A PolyhedralSurface is a simple surface, consisting of some number
 * of Polygon patches or facets. If a PolyhedralSurface is closed, then it bounds a solid.
 * A MultiSurface containing a set of closed PolyhedralSurfaces can be used to represent a Solid object with holes.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.25
 */
@UML(identifier="Surface", specification=ISO_19107)
public sealed interface Surface extends Orientable
        permits CurvePolygon,
                ParametricCurveSurface,
                Polygon,
                PolyhedralSurface,
                DefaultReversedSurface
{

    /**
     * The area of this Surface, as measured in the spatial reference system of this Surface.
     *
     * <p>TODO / Limitation: implementations label the returned quantity in square metres, but its
     * magnitude is computed in the units of the coordinate system axes. On a geographic coordinate
     * reference system that magnitude is therefore an amount of square degrees reported as square
     * metres. Computing a true area on the reference surface, as required by ISO 19107 REQ. 11,
     * remains to be done.</p>
     *
     * @return area of the surface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.10.2
     * @see ISO 19107:2019 - 6.4.25.7
     */
    @UML(identifier="area", specification=ISO_19107)
    Area getArea();

    /**
     * The mathematical centroid for this Surface as a Point.
     * The result is not guaranteed to be on this Surface.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The average is weighted by area.</li>
     *   <li>The centroid may fall outside the domain of validity of the coordinate reference system.</li>
     * </ul>
     *
     * @return centroid for this Surface
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.10.2
     * @see ISO 19107:2019 - 6.4.4.8
     */
    @Override
    default Point getCentroid() {
        throw new UnsupportedOperationException();
    }

    /**
     * A Point guaranteed to be on this Surface.
     *
     * <p>Difference with ISO 19107, which declares this operation on {@code Geometry}:
     * see {@link Geometry#getRepresentativePoint()}.</p>
     *
     * @return point guaranteed to be on this Surface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.10.2
     * @see ISO 19107:2019 - 6.4.4.19
     */
    default Point getPointOnSurface() {
        throw new UnsupportedOperationException();
    }

    /**
     * Rings bounding this surface, each of them a simple closed curve having this surface on its left.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>At least one ring.</li>
     *   <li>Except on a plane, no ring is the exterior one, since the notion of unbounded exterior
     *       does not apply.</li>
     *   <li>In a 2-dimensional coordinate system the rings determine the surface completely,
     *       and may therefore be used to build it.</li>
     * </ul>
     *
     * @return boundary of this surface.
     *
     * @see ISO 19107:2019 - 6.4.25.2
     */
    @UML(identifier="boundary", specification=ISO_19107)
    default Geometry getBoundary() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Interpolation mechanisms used between the data points of this surface.
     * The default is a polygonal interpolation.
     *
     * <p>The interpolation combines the {@linkplain #getDataPoints() data points},
     * {@linkplain #getControlPoints() control points} and {@linkplain #getKnots() knots}
     * to determine the positions of this surface.</p>
     *
     * @return interpolations used by this surface.
     *
     * @see ISO 19107:2019 - 6.4.25.3
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    default List<SurfaceInterpolation> getInterpolation() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Number of continuous derivatives guaranteed across the {@linkplain #getBoundary() boundary},
     * therefore the continuity between this surface and the neighbours sharing a boundary curve with it.
     * The default value 0 means simple continuity (C⁰), while a value <var>n</var> means that this
     * surface and its <var>n</var> first derivatives are continuous (Cⁿ).
     *
     * @return number of continuous derivatives on the boundary, or {@code null} if unspecified.
     *
     * @see ISO 19107:2019 - 6.4.25.4
     */
    @UML(identifier="numDerivativesBoundary", specification=ISO_19107)
    default Integer getNumDerivativesBoundary() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Minimal level of continuity guaranteed within the interior of this surface.
     * The default value 0 means simple continuity (C⁰).
     *
     * @return number of continuous derivatives in the interior, or {@code null} if unspecified.
     *
     * @see ISO 19107:2019 - 6.4.25.5
     */
    @UML(identifier="numDerivativeInterior", specification=ISO_19107)
    default Integer getNumDerivativeInterior() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Sum of the lengths of all the boundary curves of this surface.
     *
     * @return perimeter of this surface.
     *
     * @see ISO 19107:2019 - 6.4.25.6
     */
    @UML(identifier="perimeter", specification=ISO_19107)
    default Length getPerimeter() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Points lying on this surface.
     *
     * <p>Difference with ISO 19107: the type has been changed from a list of direct positions
     * to {@link DataPoints}, in order to accommodate additional attributes like in GLTF or GPU models.</p>
     *
     * @return surface data points.
     *
     * @see ISO 19107:2019 - 6.4.25.8
     */
    @UML(identifier="dataPoint", specification=ISO_19107)
    default DataPoints getDataPoints() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Positions used to build the geometry of this surface, the way they are used depending on the interpolation.
     * Control points do not necessarily lie on the surface.
     *
     * @return surface control points, possibly empty.
     *
     * @see ISO 19107:2019 - 6.4.25.9
     */
    @UML(identifier="controlPoint", specification=ISO_19107)
    default List<DirectPosition> getControlPoints() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Construction parameter values matching the {@linkplain #getDataPoints() data points},
     * one sequence for each of the two surface parameters.
     * Needed only when the construction parameter space is not the default one.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Two sequences, the first one for the horizontal parameter and the second one for
     *       the vertical parameter.</li>
     *   <li>Values in each sequence are monotonic.</li>
     * </ul>
     *
     * @return knot values in the construction (knot) space, one sequence per surface parameter.
     *
     * @see ISO 19107:2019 - 6.4.25.10
     */
    @UML(identifier="knot", specification=ISO_19107)
    default List<double[]> getKnots() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the vector perpendicular to this surface at the given position, pointing upward.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Normals are consistent over the whole surface, which therefore has two faces.</li>
     *   <li>In a 3-dimensional coordinate system, the returned vector is perpendicular to the
     *       tangent plane of this surface at the given position.</li>
     *   <li>In a 2-dimensional coordinate system, the returned vector lies in the enclosing
     *       3-dimensional Cartesian space of the geometric reference surface.</li>
     *   <li>When this surface bounds a solid, the returned vector points away from that solid.</li>
     * </ul>
     *
     * @param  point  position on this surface where to evaluate the normal.
     * @return upward normal at the given position.
     *
     * @see ISO 19107:2019 - 6.4.25.11
     */
    @UML(identifier="upNormal", specification=ISO_19107)
    default Vector upNormal(DirectPosition point) {
        //TODO
        throw new UnsupportedOperationException();
    }
}
