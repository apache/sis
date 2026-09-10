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
import java.util.Map;
import javax.measure.quantity.Length;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.operation.GeometryProcessor;
import org.apache.sis.geometries.operation.OperationException;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Parent interface of any geometry, a set of {@link DirectPosition} in a coordinate reference system.
 *
 * <p>A geometry behaves as a possibly infinite set of positions, and therefore supports the usual
 * set-theoretic operations. Since an infinite collection cannot be implemented directly, membership
 * is tested through {@link #contains(DirectPosition)}.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Geometries are metrically closed: any position whose distance to the geometry is zero
 *       belongs to it. Curves contain their end points, surfaces their boundary curves,
 *       and solids their boundary surfaces.</li>
 *   <li>All positions and geometries returned by an accessor use the coordinate reference system
 *       of this geometry, unless the operation explicitly specifies another one.</li>
 *   <li>All elements of a geometry collection use the coordinate reference system of the collection.</li>
 *   <li>An operation computes in the coordinate reference system of the first geometry accessed,
 *       usually the geometry on which the operation is invoked, and returns its result in that
 *       same system unless stated otherwise.</li>
 *   <li>This interface is abstract: no application schema can instantiate it directly.</li>
 * </ul>
 *
 * <p>
 * Based on specification :
 * <ul>
 *  <li>ISO 19107</li>
 *  <li>OGC Simple Feature Access - https://www.ogc.org/standards/sfa</li>
 *  <li>Khronos GLTF-2 - https://github.com/KhronosGroup/glTF/tree/main/specification/2.0</li>
 *  <li>Khronos ANARI-1 - https://www.khronos.org/anari/</li>
 * </ul>
 *
 * <p>
 * Deviation from ISO-19107 :<br>
 * A Geometry should be a sub type of TransfiniteSetOfDirectPositions (section 6.4.2)
 * A TransfiniteSetOfDirectPositions exist to define a Geometry within the <b>Set theory</b>, it is a mathematical conceptual interface.
 * But the interface has only a unique subtype and provide a single additional method contains(DirectPosition),
 * therefor we merged TransfiniteSetOfDirectPositions in Geometry for simplicity state
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.4
 */
@UML(identifier="Geometry", specification=ISO_19107)
public sealed interface Geometry
        permits Primitive,
                GeometryCollection,
                OrientedGeometry,
                Empty,
                Prism,
                MeshPrimitive,
                AbstractGeometry,
                BBox
{

    /**
     * Coordinate reference system of all the coordinates used within this geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>All positions and geometries returned when accessing this geometry use this system,
     *       unless another one is implied or provided by the operation.</li>
     *   <li>A geometry contained in another one shall use a system consistent with the container.</li>
     * </ul>
     *
     * <p>Difference with ISO 19107: in ISO 19107 there may be multiple RSID, the following RSID
     * are used in special kind of curves; for the sake of simplicity we only store the first rsid
     * until implementations of such curves will happen.</p>
     *
     * @return coordinate reference system of this geometry, never null.
     *
     * @see ISO 19107:2019 - 6.4.4.20
     */
    @UML(identifier="rsid", specification=ISO_19107)
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Sets the coordinate system in which the coordinates are declared.
     * This method does not transform the coordinates.
     *
     * <p>Difference with ISO 19107: geometries are immutable in the standard,
     * which offers no such setter.</p>
     *
     * @param  crs  the new coordinate reference system, not null.
     * @throws IllegalArgumentException if the coordinate system is not compatible with the geometry.
     */
    void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException;

    /**
     * Documentation about the implementation of this geometry.
     * When absent, the applicable documentation is ISO 19107 itself.
     *
     * <p>Difference with ISO 19107: we return a single Metadata instead of a list of URI,
     * whose first element would point to the normative document describing this geometry.</p>
     *
     * @return metadata about this geometry.
     *
     * @see ISO 19107:2019 - 6.4.4.18
     */
    @UML(identifier="metadata", specification=ISO_19107)
    default Metadata getMetadata() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get geometry attributes type.
     *
     * <p>Difference with ISO 19107: this accessor has no equivalent in the standard.
     * It describes the attributes carried by the geometry positions in addition to the
     * coordinates, as needed by GLTF or GPU models.</p>
     *
     * @return attributes type, never null
     */
    AttributesType getAttributesType();

    /**
     * Number of axes in the coordinate reference system of this geometry.
     *
     * @return number of dimension
     *
     * @see ISO 19107:2019 - 6.4.4.11
     */
    @UML(identifier="coordinateDimension", specification=ISO_19107)
    default int getDimension() {
        return getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
    }

    /**
     * Returns the inherent topological dimension of this geometry at the given position.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The returned value is smaller than or equal to the {@linkplain #getDimension()
     *       coordinate dimension}.</li>
     *   <li>The dimension is unambiguous only for positions interior to this geometry.</li>
     *   <li>If the given position is null, the largest dimension found in the interior of this
     *       geometry is returned.</li>
     * </ul>
     *
     * @param  point  position where to evaluate the dimension, or null for the whole geometry.
     * @return topological dimension at the given position.
     *
     * @see ISO 19107:2019 - 6.4.4.25
     */
    @UML(identifier="dimension", specification=ISO_19107)
    default int getDimension(DirectPosition point) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether this geometry uses three spatial dimensions.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>If {@code true}, then the {@linkplain #getSpatialDimension() spatial dimension} is 3.</li>
     * </ul>
     *
     * @return {@code true} if this geometry is three-dimensional.
     *
     * @see ISO 19107:2019 - 6.4.4.13
     */
    @UML(identifier="is3D", specification=ISO_19107)
    default boolean is3D() {
        return getDimension() == 3;
    }

    /**
     * Number of spatial axes in the coordinate reference system of this geometry, usually 2 or 3.
     *
     * @return number of spatial dimensions.
     *
     * @see ISO 19107:2019 - 6.4.4.21
     */
    @UML(identifier="spatialDimension", specification=ISO_19107)
    default int getSpatialDimension() {
        return getDimension();
    }

    /**
     * Largest topological dimension among the components of this geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>−1 for an empty geometry, 0 for a point, 1 for a curve, 2 for a surface, 3 for a solid.</li>
     *   <li>For a collection, the maximum of the dimensions of the contained primitives.</li>
     * </ul>
     *
     * @return topological dimension of this geometry.
     *
     * @see ISO 19107:2019 - 6.4.4.22
     */
    @UML(identifier="topologicalDimension", specification=ISO_19107)
    default int getTopologicDimension() {
        //TODO remove this method default when all classes implement it.
        //Empty, Point, Curve, Surface, Solid and GeometryCollection already do.
        throw new UnsupportedOperationException();
    }

    /**
     * Rule used to compute the {@linkplain #boundary() boundary} of this geometry
     * in the ambiguous cases raised by aggregates.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@code METRIC} derives the boundary from distances, as in a metric space.</li>
     *   <li>{@code MOD_2} puts in the boundary the positions lying on an odd number of element
     *       boundaries, and in the interior those lying on an even number.</li>
     *   <li>{@code AT_LEAST_2} puts in the boundary the positions lying on exactly one element
     *       boundary, and in the interior those lying on more than one.</li>
     * </ul>
     *
     * @return rule used to compute the boundary of this geometry.
     *
     * @see ISO 19107:2019 - 6.4.3, 10.8.3
     */
    @UML(identifier="boundaryType", specification=ISO_19107)
    default BoundaryType getBoundaryType() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the name of the instantiable subtype of Geometry of which this geometric object is an instantiable member.<br>
     * The name of the subtype of Geometry is returned as a string.
     *
     * @return geometry subtype name.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @see ISO 19107:2019 - 6.4.4.23
     */
    String getGeometryType();

    /**
     * Interfaces of this standard, or of its extensions, supported by this geometry instance.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>At least one value, taken from the types the implementation declares as supported.</li>
     *   <li>The values are the most specific types applicable to this instance.</li>
     *   <li>An empty geometry is of type {@code EMPTY}.</li>
     * </ul>
     *
     * @return types of this geometry.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @UML(identifier="type", specification=ISO_19107)
    default List<GeometryType> getGeometryType2() {
        //TODO merge with getGeometryType
        throw new UnsupportedOperationException();
    }

    /**
     * The minimum bounding box for this Geometry, returned as a Geometry.<br>
     * The polygon is defined by the corner points of the bounding box [(MINX, MINY), (MAXX, MINY), (MAXX, MAXY), (MINX, MAXY), (MINX, MINY)].<br>
     * Minimums for Z and M may be added.<br>
     * The simplest representation of an Envelope is as two direct positions, one containing all the minimums, and another all the maximums.<br>
     * In some cases, this coordinate will be outside the range of validity for the Spatial Reference System.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Smallest coordinate rectangle containing all the positions of this geometry.</li>
     *   <li>The envelope of an empty geometry is the empty geometry.</li>
     *   <li>If the coordinate system wraps around a singularity, several representations of the
     *       envelope are possible and the choice is left to the implementation.</li>
     * </ul>
     *
     * @return Envelope in geometry coordinate reference system.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @see ISO 19107:2019 - 6.4.4.12
     */
    @UML(identifier="envelope", specification=ISO_19107)
    Envelope getEnvelope();

    /**
     * The mathematical centroid for this Geometry as a Point.
     * The result is not guaranteed to be on this Geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>In a heterogeneous collection, only the components of the largest dimension contribute;
     *       for surfaces the average is weighted by area, and curves having no area are ignored.</li>
     *   <li>The centroid may fall outside the domain of validity of the coordinate reference system.</li>
     *   <li>The centroid of an empty geometry is the empty geometry.</li>
     * </ul>
     *
     * <p>Difference from OGC SFA : this method in declared on Surface and MultiSurface
     * but in ISO 19107 it is on Geometry.</p>
     *
     * @return centroid for this Geometry
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.10.2
     * @see ISO 19107:2019 - 6.4.4.8
     */
    @UML(identifier="centroid", specification=ISO_19107)
    default Point getCentroid() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * A position guaranteed to be interior to this geometry, for example for label placement.
     * The {@linkplain #getCentroid() centroid} is a suitable value when it lies inside this geometry.
     *
     * @return a position interior to this geometry.
     *
     * @see ISO 19107:2019 - 6.4.4.19
     */
    @UML(identifier="representativePoint", specification=ISO_19107)
    default Point getRepresentativePoint() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Union of this geometry and of its {@linkplain #boundary() boundary}.
     *
     * @return closure of this geometry, in the coordinate reference system of this geometry.
     *
     * @see ISO 19107:2019 - 6.4.4.9
     */
    @UML(identifier="closure", specification=ISO_19107)
    default Geometry getClosure() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Smallest geometry containing this geometry within which this geometry is a subset,
     * or {@code null} if the application schema has no notion of complex.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>A geometry is maximal when no larger super-set of it exists in the same dataset.</li>
     *   <li>A primitive usually belongs to a single maximal complex, which makes the relationship
     *       a strong aggregation.</li>
     * </ul>
     *
     * @return maximal complex containing this geometry, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 6.4.4.17
     */
    @UML(identifier="maximalComplex", specification=ISO_19107)
    default Geometry getMaximalComplex() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Exports this geometric object to a specific Well-known Text Representation of Geometry.
     *
     * <p>Difference with ISO 19107 :
     * - this method is located on Encoding sub interface in the standard, it is placed
     *   on Geometry to match OGC SFA.</p>
     *
     * @return this geometry in Well-known Text
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @see ISO 19107:2019 - 6.4.4.5, 6.4.7
     */
    @UML(identifier="asText", specification=ISO_19107)
    default String asText() {
        //TODO remove this method default when all classes implement it.
        return this.getClass().getSimpleName();
    }

    /**
     * Exports this geometric object to a specific Well-known Binary Representation of Geometry.
     *
     * <p>Difference with ISO 19107 :
     * - this method is located on Encoding sub interface in the standard, it is placed
     *   on Geometry to match OGC SFA.</p>
     *
     * @return this geometry in Well-known Binary
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @see ISO 19107:2019 - 6.4.4.3, 6.4.7
     */
    @UML(identifier="asBinary", specification=ISO_19107)
    default byte[] asBinary() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object is the empty Geometry.
     * If true, then this geometric object represents the empty point set ∅ for the coordinate space.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The empty set being unique, all empty geometries are spatially equal.</li>
     *   <li>The {@linkplain #getTopologicDimension() topological dimension} of an empty geometry is −1,
     *       and its {@linkplain #getGeometryType2() type} is {@code EMPTY}.</li>
     *   <li>Every derived geometry is empty as well, and every derived position is null.</li>
     *   <li>{@link #isCycle()}, {@link #isSimple()} and {@link #isValid()} are all {@code true}
     *       for an empty geometry.</li>
     *   <li>The distance from an empty geometry to any geometry is infinite.</li>
     * </ul>
     *
     * @return true if empty.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @see ISO 19107:2019 - 6.4.4.2, 6.4.10
     */
    @UML(identifier="isEmpty", specification=ISO_19107)
    boolean isEmpty();

    /**
     * Returns TRUE if this geometric object has no anomalous geometric points, such as self intersection or self tangency.
     * The description of each instantiable geometric class will include the specific conditions that cause an instance
     * of that class to be classified as not simple.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>{@code false} if and only if this geometry has a point of self intersection or of self tangency.</li>
     *   <li>Equivalently, every interior position has a neighbourhood whose intersection with this
     *       geometry is topologically isomorphic to an <var>n</var>-sphere, where <var>n</var> is the
     *       {@linkplain #getTopologicDimension() topological dimension}.</li>
     * </ul>
     *
     * @return true if geometry is simple
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @see ISO 19107:2019 - 6.4.4.15
     */
    @UML(identifier="isSimple", specification=ISO_19107)
    default boolean isSimple() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether this geometry closes on itself, i.e. whether its
     * {@linkplain #boundary() boundary} is empty.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>A point and the empty geometry are always cycles.</li>
     *   <li>A curve is a cycle if its start point is its end point.</li>
     *   <li>A surface is a cycle if it is isomorphic to a sphere or to a torus.</li>
     *   <li>A solid of finite size in a 3-dimensional coordinate space is never a cycle.</li>
     * </ul>
     *
     * <p>ISO 19107 uses <cite>cycle</cite> rather than <cite>closed</cite>, because the latter has
     * two distinct and incompatible meanings in topology.</p>
     *
     * @return {@code true} if this geometry is a cycle.
     *
     * @see ISO 19107:2019 - 6.4.4.14
     */
    @UML(identifier="isCycle", specification=ISO_19107)
    default boolean isCycle() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether the structure of this geometry is valid as a geometry.
     *
     * @return {@code true} if this geometry is valid.
     *
     * @see ISO 19107:2019 - 6.4.4.16
     */
    @UML(identifier="isValid", specification=ISO_19107)
    default boolean isValid() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the closure of the combinatorial boundary of this geometric object (Reference [1], section 3.12.2).
     * Because the result of this function is a closure, and hence topologically closed, the resulting boundary can be
     * represented using representational Geometry primitives (Reference [1], section 3.12.2).
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The boundary is expressed in the coordinate reference system of this geometry.</li>
     *   <li>Its {@linkplain #getTopologicDimension() topological dimension} is one less than the
     *       dimension of this geometry, unless this geometry is empty or is a
     *       {@linkplain #isCycle() cycle}, in which case the boundary is empty.</li>
     *   <li>The boundary of a point, and of any finite set of points, is empty.</li>
     *   <li>The boundary of a curve is its start point and its end point.</li>
     *   <li>The boundary of a surface is a set of simple curves, each of them a cycle having the
     *       surface on its left but not on its right.</li>
     *   <li>The boundary of a solid is a set of surfaces, each of them a cycle.</li>
     *   <li>How the boundary of an aggregate is computed depends on
     *       {@linkplain #getBoundaryType() the boundary type}.</li>
     * </ul>
     *
     * @return boundary of the geometry
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @see ISO 19107:2019 - 6.4.4.7, 10.8.3
     */
    @UML(identifier="boundary", specification=ISO_19107)
    default Geometry boundary() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Map of properties for user needs.
     * Those informations may be lost in geometry processes.
     *
     * <p>Difference with ISO 19107: this accessor has no equivalent in the standard.</p>
     *
     * @return Map, can be null if the geometry cannot store additional informations.
     */
    default Map<String,Object> userProperties() {
        return null;
    }

    // ////////////////////////////////////////////////////////////////////////
    // ISO 19107 Query2D and Query3D interfaces and merged inside Geometry
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns a geometry containing all the positions closer to this geometry than the given distance,
     * expressed in the units of the coordinate reference system.
     *
     * <p>Difference with ISO 19107, which takes a distance quantity:
     * see {@link #buffer(Length)} for the standard operation.</p>
     *
     * @param  distance  radius of the buffer.
     * @return buffer around this geometry.
     * @throws OperationException if the buffer cannot be computed.
     *
     * @see GeometryProcessor#buffer(org.apache.sis.geometries.Geometry, double)
     */
    default Geometry buffer(double distance) throws OperationException {
        return new GeometryProcessor().buffer(this, distance);
    }

    /**
     * Returns a geometry containing all the positions whose distance to this geometry
     * is smaller than or equal to the given radius.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>A zero radius returns a geometry equal to this geometry.</li>
     *   <li>A negative radius returns the positions inside this geometry whose distance to its
     *       boundary is greater than the absolute value of the radius.</li>
     *   <li>The result is expressed in the coordinate reference system of this geometry, and its
     *       topological dimension is usually the spatial dimension of that system.</li>
     *   <li>The buffer of an empty geometry is the empty geometry.</li>
     * </ul>
     *
     * @param  radius  radius of the buffer.
     * @return buffer around this geometry.
     * @throws OperationException if the buffer cannot be computed.
     *
     * @see GeometryProcessor#buffer(org.apache.sis.geometries.Geometry, javax.measure.quantity.Length)
     * @see ISO 19107:2019 - 6.4.4.24, 6.4.8.3, 6.4.9
     */
    @UML(identifier="buffer", specification=ISO_19107)
    //@UML(identifier="3Dbuffer", specification=ISO_19107)
    default Geometry buffer(Length radius) throws OperationException {
        return new GeometryProcessor().buffer(this, radius);
    }

    /**
     * Returns the smallest convex set containing this geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Convexity depends on the meaning given to a straight line, therefore the result
     *       depends on the coordinate reference system in use.</li>
     *   <li>The convex hull may fall outside the domain of validity of the coordinate reference system.</li>
     *   <li>The convex hull of an empty geometry is the empty geometry.</li>
     * </ul>
     *
     * @return convex hull of this geometry.
     * @throws OperationException if the convex hull cannot be computed.
     *
     * @see GeometryProcessor#convexHull(org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.10, 6.4.9
     */
    @UML(identifier="convexHull", specification=ISO_19107)
    //@UML(identifier="3DconvexHull", specification=ISO_19107)
    default Geometry convexHull() throws OperationException {
        return new GeometryProcessor().convexHull(this);
    }

   /**
     * Returns the set-theoretic difference between this geometry and the given geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The difference of two closed sets is not necessarily closed, therefore the result may
     *       differ from the topological difference along its boundary.</li>
     *   <li>The difference between this geometry and an empty geometry is this geometry.</li>
     * </ul>
     *
     * @param  other  the geometry to subtract from this geometry.
     * @return this geometry minus the given geometry.
     * @throws OperationException if the difference cannot be computed.
     *
     * @see GeometryProcessor#difference(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.30, 6.4.8.5, 6.4.9
     */
    @UML(identifier="difference", specification=ISO_19107)
    //@UML(identifier="3Ddifference", specification=ISO_19107)
    default Geometry difference(Geometry other) throws OperationException {
        return new GeometryProcessor().difference(this, other);
    }

    /**
     * Returns the shortest distance between this geometry and the given geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Zero if the two geometries intersect.</li>
     *   <li>Consistent with the geometric reference surface in use, therefore a map distance,
     *       a geodesic distance or a terrain distance depending on that surface.</li>
     *   <li>Infinite if either geometry is empty.</li>
     * </ul>
     *
     * @param  other  the geometry to measure the distance to.
     * @return distance between the two geometries.
     * @throws OperationException if the distance cannot be computed.
     *
     * @see GeometryProcessor#distance(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.26, 6.4.8.2, 6.4.9
     */
    @UML(identifier="distance", specification=ISO_19107)
    //@UML(identifier="3Ddistance", specification=ISO_19107)
    default Length distance(Geometry other) throws OperationException {
        return new GeometryProcessor().distance(this, other);
    }

    /**
     * Returns the set-theoretic intersection of this geometry and the given geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The intersection of two closed sets being closed, the result matches the topological
     *       intersection within the precision of the geometric representations.</li>
     *   <li>The intersection with an empty geometry is the empty geometry.</li>
     * </ul>
     *
     * @param  other  the geometry to intersect with this geometry.
     * @return intersection of the two geometries.
     * @throws OperationException if the intersection cannot be computed.
     *
     * @see GeometryProcessor#intersection(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.30, 6.4.8.4, 6.4.9
     */
    @UML(identifier="intersection", specification=ISO_19107)
    //@UML(identifier="3Dintersection", specification=ISO_19107)
    default Geometry intersection(Geometry other) throws OperationException {
        return new GeometryProcessor().intersection(this, other);
    }

    /**
     * Returns the set-theoretic symmetric difference of this geometry and the given geometry,
     * that is (A−B) ∪ (B−A).
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The symmetric difference of two closed sets is not necessarily closed, therefore the
     *       result may differ from the topological answer along its boundary.</li>
     *   <li>The symmetric difference between this geometry and an empty geometry is this geometry.</li>
     * </ul>
     *
     * @param  other  the geometry to combine with this geometry.
     * @return symmetric difference of the two geometries.
     * @throws OperationException if the symmetric difference cannot be computed.
     *
     * @see GeometryProcessor#symDifference(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.30, 6.4.8.6, 6.4.9
     */
    @UML(identifier="symDifference", specification=ISO_19107)
    //@UML(identifier="3DsymDifference", specification=ISO_19107)
    default Geometry symDifference(Geometry other) throws OperationException {
        return new GeometryProcessor().symDifference(this, other);
    }

    /**
     * Returns the set-theoretic union of this geometry and the given geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The union of two closed sets being closed, the result matches the topological union
     *       within the precision of the geometric representations.</li>
     *   <li>The union with an empty geometry is this geometry.</li>
     * </ul>
     *
     * @param  other  the geometry to unite with this geometry.
     * @return union of the two geometries.
     * @throws OperationException if the union cannot be computed.
     *
     * @see GeometryProcessor#union(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.30, 6.4.8.7, 6.4.9
     */
    @UML(identifier="union", specification=ISO_19107)
    //@UML(identifier="3Dunion", specification=ISO_19107)
    default Geometry union(Geometry other) throws OperationException {
        return new GeometryProcessor().union(this, other);
    }

    /**
     * Returns this geometry expressed in the given coordinate reference system.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The returned geometry is equal to this geometry within the accuracy of the transformation.</li>
     *   <li>Non-spatial coordinates follow their own logic for converting to a compatible system;
     *       a purely spatial geometry is only submitted to a change of coordinates.</li>
     *   <li>The returned geometry is of the same type as this geometry whenever possible.</li>
     * </ul>
     *
     * <p>Difference with ISO 19107, which identifies the target system by a RSID:
     * the target system is given as a {@link CoordinateReferenceSystem}.</p>
     *
     * @param  crs  the target coordinate reference system.
     * @return this geometry in the given coordinate reference system.
     *
     * @see GeometryProcessor#transform(org.apache.sis.geometries.Geometry, org.opengis.referencing.crs.CoordinateReferenceSystem, org.opengis.referencing.operation.MathTransform)
     * @see ISO 19107:2019 - 6.4.4.28
     */
    @UML(identifier="transform", specification=ISO_19107)
    default Geometry transform(CoordinateReferenceSystem crs) {
        return new GeometryProcessor().transform(this, crs, null);
    }

    /**
     * Returns whether the given position belongs to this geometry.
     * This is the membership test which stands for this geometry being a possibly infinite
     * set of positions.
     *
     * @param  element  the position to test.
     * @return {@code true} if the given position is on this geometry.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#contains(org.apache.sis.geometries.Geometry, org.opengis.geometry.DirectPosition)
     * @see ISO 19107:2019 - 6.4.2, 6.4.4.30, 6.4.9
     */
    @UML(identifier="contains", specification=ISO_19107)
    //@UML(identifier="3Dcontains", specification=ISO_19107)
    default boolean contains(DirectPosition element) throws OperationException {
        return new GeometryProcessor().contains(this, element);
    }

    /**
     * Returns whether the given geometry is a subset of this geometry, that is whether no position
     * of the given geometry lies in the exterior of this geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Equivalent to {@code relate(other, "TNNNNNFFN")}.</li>
     *   <li>{@code a.contains(b)} is equivalent to {@code b.within(a)}.</li>
     * </ul>
     *
     * @param  other  the geometry to test for inclusion.
     * @return {@code true} if this geometry contains the given geometry.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#contains(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.30, 6.4.8.8, 10.8.6.3.2
     */
    @UML(identifier="contains", specification=ISO_19107)
    default boolean contains(Geometry other) throws OperationException {
        return new GeometryProcessor().contains(this, other);
    }

    /**
     * Returns whether the two geometries cross, that is whether their interiors intersect in a set
     * of dimension lower than the largest of their dimensions, without either geometry containing
     * the other.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Applies to the point/curve, point/surface, curve/curve and curve/surface cases.</li>
     * </ul>
     *
     * @param  other  the geometry to test against.
     * @return {@code true} if the two geometries cross.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#crosses(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.8.8, 6.4.9, 10.8.6.3.6
     */
    @UML(identifier="crosses", specification=ISO_19107)
    //@UML(identifier="3Dcrosses", specification=ISO_19107)
    default boolean crosses(Geometry other) throws OperationException {
        return new GeometryProcessor().crosses(this, other);
    }

    /**
     * Returns whether the two geometries have no position in common.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Equivalent to {@code relate(other, "FFNFFNNNN")}, or {@code "FNNN"} on the order 4 matrix.</li>
     *   <li>The negation of {@link #intersects(Geometry)}.</li>
     * </ul>
     *
     * @param  other  the geometry to test against.
     * @return {@code true} if the two geometries are disjoint.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#disjoint(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.8.8, 6.4.9, 10.8.6.3.3
     */
    @UML(identifier="disjoint", specification=ISO_19107)
    //@UML(identifier="3Ddisjoint", specification=ISO_19107)
    default boolean disjoint(Geometry other) throws OperationException {
        return new GeometryProcessor().disjoint(this, other);
    }

    /**
     * Returns whether the two geometries are the same set of positions.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Equivalent to {@code relate(other, "NFFFNFNNF")}.</li>
     *   <li>Only the spatial coordinates are compared, so a spatio-temporal geometry is tested
     *       for spatial equality only.</li>
     *   <li>The given geometry is converted to the coordinate reference system of this geometry
     *       before the comparison.</li>
     *   <li>Any two empty geometries are equal.</li>
     * </ul>
     *
     * @param  other  the geometry to compare with.
     * @return {@code true} if the two geometries are spatially equal.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#equal(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.27, 6.4.4.30, 6.4.8.8, 6.4.9, 10.8.6.3.1
     */
    @UML(identifier="equals", specification=ISO_19107)
    //@UML(identifier="3Dequals", specification=ISO_19107)
    default boolean equal(Geometry other) throws OperationException {
        return new GeometryProcessor().equal(this, other);
    }

    /**
     * Returns whether the two geometries have at least one position in common.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Equivalent to {@code relate(other, "TNNN")} on the order 4 matrix.</li>
     *   <li>The negation of {@link #disjoint(Geometry)}.</li>
     * </ul>
     *
     * @param  other  the geometry to test against.
     * @return {@code true} if the two geometries intersect.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#intersects(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.4.30, 6.4.8.8, 6.4.9, 10.8.6.3.4
     */
    @UML(identifier="intersects", specification=ISO_19107)
    //@UML(identifier="3Dintersects", specification=ISO_19107)
    default boolean intersects(Geometry other) throws OperationException {
        return new GeometryProcessor().intersects(this, other);
    }

    /**
     * Returns the part of this geometry located at the given measure along a linear reference.
     *
     * <p>Difference with ISO 19107: this operation has no equivalent in the standard,
     * which delegates linear referencing to ISO 19148.</p>
     *
     * @param  mValue  measure value at which to locate.
     * @return part of this geometry at the given measure.
     * @throws OperationException if the location cannot be computed.
     *
     * @see GeometryProcessor#locateAlong(org.apache.sis.geometries.Geometry, double)
     */
    default Geometry locateAlong(double mValue) throws OperationException {
        return new GeometryProcessor().locateAlong(this, mValue);
    }

    /**
     * Returns the part of this geometry located between the two given measures along a linear reference.
     *
     * <p>Difference with ISO 19107: this operation has no equivalent in the standard,
     * which delegates linear referencing to ISO 19148.</p>
     *
     * @param  mStart  measure value where the returned geometry begins.
     * @param  mEnd    measure value where the returned geometry ends.
     * @return part of this geometry between the two given measures.
     * @throws OperationException if the location cannot be computed.
     *
     * @see GeometryProcessor#contains(org.apache.sis.geometries.Geometry, double, double)
     */
    default Geometry locateBetween(double mStart, double mEnd) throws OperationException {
        return new GeometryProcessor().locateBetween(this, mStart, mEnd);
    }

    /**
     * Returns whether the two geometries have the same dimension and their interiors intersect,
     * without either geometry containing the other.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Equivalent to {@code relate(other, "TNTNNNTNN")}.</li>
     *   <li>Symmetric: {@code a.overlaps(b)} is equivalent to {@code b.overlaps(a)}.</li>
     * </ul>
     *
     * @param  other  the geometry to test against.
     * @return {@code true} if the two geometries overlap.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#overlaps(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.8.8, 6.4.9, 10.8.6.3.8
     */
    @UML(identifier="overlaps", specification=ISO_19107)
    //@UML(identifier="3Doverlaps", specification=ISO_19107)
    default boolean overlaps(Geometry other) throws OperationException {
        return new GeometryProcessor().overlaps(this, other);
    }

    /**
     * Returns whether the two geometries are related according to the given intersection pattern,
     * encoded as an integer mask.
     *
     * <p>Difference with ISO 19107, which specifies the pattern as a string:
     * see {@link #relate(Geometry, String)} for the standard operation.</p>
     *
     * @param  other   the geometry to test against.
     * @param  matrix  intersection pattern, encoded as an integer mask.
     * @return {@code true} if the two geometries match the given pattern.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#relate(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry, int)
     */
    default boolean relate(Geometry other, int matrix) throws OperationException {
        return new GeometryProcessor().relate(this, other, matrix);
    }

    /**
     * Returns whether the two geometries are related according to the given intersection pattern.
     * This is the reference operation from which all the named topological predicates are derived.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>A pattern of 4 characters tests the intersections between the closures and the exteriors
     *       of the two geometries, in row-major order.</li>
     *   <li>A pattern of 9 characters tests the intersections between the interiors, the boundaries
     *       and the exteriors of the two geometries, in row-major order.</li>
     *   <li>{@code T} requires a non-empty intersection, {@code F} an empty one, and {@code N}
     *       (also written {@code *}) leaves that cell untested.</li>
     *   <li>The digits {@code 0} to {@code 3} additionally require the intersection to be of that
     *       topological dimension at most.</li>
     * </ul>
     *
     * @param  other   the geometry to test against.
     * @param  matrix  intersection pattern of 4 or 9 characters.
     * @return {@code true} if the two geometries match the given pattern.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#relate(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry, java.lang.String)
     * @see ISO 19107:2019 - 6.4.8.8, 6.4.9, 10.8.4, 10.8.5, 10.8.6
     */
    @UML(identifier="relate", specification=ISO_19107)
    //@UML(identifier="3Drelate", specification=ISO_19107)
    default boolean relate(Geometry other, String matrix) throws OperationException {
        return new GeometryProcessor().relate(this, other, matrix);
    }

    /**
     * Returns whether the two geometries meet without their interiors intersecting.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The closures intersect but the interiors are disjoint.</li>
     *   <li>Equivalent to {@code relate(other, "FT*******")}, {@code "F**T*****"}
     *       or {@code "F***T****"}.</li>
     * </ul>
     *
     * @param  other  the geometry to test against.
     * @return {@code true} if the two geometries touch.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#touches(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.8.8, 6.4.9, 10.8.6.3.5
     */
    @UML(identifier="touches", specification=ISO_19107)
    //@UML(identifier="3Dtouches", specification=ISO_19107)
    default boolean touches(Geometry other) throws OperationException {
        return new GeometryProcessor().touches(this, other);
    }

    /**
     * Returns whether this geometry is a subset of the given geometry, that is whether no position
     * of this geometry lies in the exterior of the given geometry.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Equivalent to {@code relate(other, "TNFNNFNNN")}.</li>
     *   <li>{@code a.within(b)} is equivalent to {@code b.contains(a)}.</li>
     * </ul>
     *
     * @param  other  the geometry to test for inclusion in.
     * @return {@code true} if this geometry is within the given geometry.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#within(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry)
     * @see ISO 19107:2019 - 6.4.8.8, 6.4.9, 10.8.6.3.7
     */
    @UML(identifier="within", specification=ISO_19107)
    //@UML(identifier="3Dwithin", specification=ISO_19107)
    default boolean within(Geometry other) throws OperationException {
        return new GeometryProcessor().within(this, other);
    }

    /**
     * Returns whether the given geometry lies closer to this geometry than the given distance.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Equivalent to {@code buffer(distance).intersects(other)}.</li>
     * </ul>
     *
     * @param  other     the geometry to test against.
     * @param  distance  maximal distance between the two geometries.
     * @return {@code true} if the two geometries are within the given distance.
     * @throws OperationException if the test cannot be performed.
     *
     * @see GeometryProcessor#withinDistance(org.apache.sis.geometries.Geometry, org.apache.sis.geometries.Geometry, javax.measure.quantity.Length)
     * @see ISO 19107:2019 - 6.4.8.8, 6.4.9, 10.8.7
     */
    @UML(identifier="withinDistance", specification=ISO_19107)
    //@UML(identifier="3DwithinDistance", specification=ISO_19107)
    default boolean withinDistance(Geometry other, Length distance) throws OperationException {
        return new GeometryProcessor().withinDistance(this, other, distance);
    }

}
