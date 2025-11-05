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
package org.apache.sis.filter.sqlmm;

import java.util.EnumMap;
import java.util.Optional;
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.filter.visitor.FunctionIdentifier;
import static org.apache.sis.geometry.wrapper.GeometryType.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.capability.AvailableFunction;


/**
 * Identification of SQLMM operations. The names or enumeration values are the names
 * defined by SQLMM standard; they do not obey to the usual Java naming conventions.
 * Enumeration values order is the approximated declaration order in SQLMM standard.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://www.iso.org/standard/60343.html">ISO 13249-3 - SQLMM</a>
 */
public enum SQLMM implements FunctionIdentifier {
    /**
     * The number of dimensions in the geometry.
     */
    ST_Dimension(GEOMETRY, Integer.class),

    /**
     * The number of dimensions of coordinate tuples.
     * It depends on whether there is <var>z</var> values.
     *
     * @see #ST_Is3D
     * @see #ST_IsMeasured
     */
    ST_CoordDim(GEOMETRY, Integer.class),

    /**
     * The SQLMM name of the geometry type.
     */
    ST_GeometryType(GEOMETRY, String.class),

    /**
     * The spatial reference system identifier (SRID) of the geometry.
     */
    ST_SRID(GEOMETRY, Integer.class),

    /**
     * Transform a geometry to the specified spatial reference system.
     * The operation should take in account the <var>z</var> and <var>m</var> coordinate values.
     *
     * <h4>Limitation</h4>
     * <ul>
     *   <li>Current implementation ignores the <var>z</var> and <var>m</var> values.</li>
     *   <li>If the SRID is an integer, it is interpreted as an EPSG code.
     *       It should be a primary key in the {@code "spatial_ref_sys"} table instead.</li>
     * </ul>
     */
    ST_Transform(2, 2, GEOMETRY, null, GEOMETRY),

    /**
     * Test if a geometry is empty.
     */
    ST_IsEmpty(GEOMETRY, Boolean.class),

    /**
     * Test if a geometry has no anomalous points such as self intersection.
     * This test ignores the <var>z</var> coordinate values.
     */
    ST_IsSimple(GEOMETRY, Boolean.class),

    /**
     * Test if a geometry is well formed.
     */
    ST_IsValid(GEOMETRY, Boolean.class),

    /**
     * Test if a geometry has <var>z</var> coordinate values.
     *
     * @see #ST_CoordDim
     */
    ST_Is3D(GEOMETRY, Boolean.class),

    /**
     * Test if a geometry has <var>m</var> coordinate values.
     *
     * @see #ST_CoordDim
     */
    ST_IsMeasured(GEOMETRY, Boolean.class),

    /**
     * The boundary of a geometry, ignoring <var>z</var> and <var>m</var> coordinate values.
     */
    ST_Boundary(GEOMETRY, GEOMETRY),

    /**
     * The boundary rectangle of a geometry, ignoring <var>z</var> and <var>m</var> coordinate values.
     */
    ST_Envelope(GEOMETRY, Envelope.class),

    /**
     * The convex hull of a geometry, ignoring <var>z</var> and <var>m</var> coordinate values.
     */
    ST_ConvexHull(GEOMETRY, GEOMETRY),

    /**
     * The geometry that represents all points whose distance from any point of a geometry is less than or equal
     * to a specified distance. This operation ignores <var>z</var> and <var>m</var> coordinate values.
     * This expression expects two arguments: the geometry and the distance.
     * This distance shall be expressed in units of the geometry Coordinate Reference System.
     */
    ST_Buffer(2, 2, GEOMETRY, null, GEOMETRY),

    /**
     * Geometry that represents the intersection of two geometries,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     */
    ST_Intersection(GEOMETRY),

    /**
     * Geometry that represents the union of two geometries,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     */
    ST_Union(GEOMETRY),

    /**
     * Geometry that represents the difference of two geometries,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     */
    ST_Difference(GEOMETRY),

    /**
     * Geometry that represents the symmetric difference of two geometries,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     */
    ST_SymDifference(GEOMETRY),

    /**
     * Distance between two geometries, ignoring <var>z</var> and <var>m</var> coordinate values.
     *
     * @todo Current value type is {@link Number}. Consider using {@link Quantity} instead.
     */
    ST_Distance(Number.class),

    /**
     * Test if a geometry is spatially equal to another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     * This operation is similar to Filter Encoding "Equals" operation except in the way the CRS is selected
     * (the SQLMM standard requires that we use the CRS of the first geometry) and in the return value when
     * an error occurred (this implementation returns {@code null}).
     *
     * @see SpatialOperatorName#EQUALS
     */
    ST_Equals(Boolean.class),

    /**
     * Test if a geometry is spatially related to another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     */
    ST_Relate(2, 3, GEOMETRY, GEOMETRY, Boolean.class),

    /**
     * Test if a geometry value is spatially disjoint from another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     * This operation is similar to Filter Encoding "Disjoint" operation except in the way the CRS is selected
     * (the SQLMM standard requires that we use the CRS of the first geometry) and in the return value when an
     * error occurred (this implementation returns {@code null}).
     *
     * @see SpatialOperatorName#DISJOINT
     */
    ST_Disjoint(Boolean.class),

    /**
     * Test if a geometry value spatially intersects another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     * This operation is similar to Filter Encoding "Intersects" operation except in the way the CRS is selected
     * (the SQLMM standard requires that we use the CRS of the first geometry) and in the return value when an
     * error occurred (this implementation returns {@code null}).
     *
     * @see SpatialOperatorName#INTERSECTS
     */
    ST_Intersects(Boolean.class),

    /**
     * Test if a geometry value spatially touches another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     * This operation is similar to Filter Encoding "Touches" operation except in the way the CRS is selected
     * (the SQLMM standard requires that we use the CRS of the first geometry) and in the return value when
     * an error occurred (this implementation returns {@code null}).
     *
     * @see SpatialOperatorName#TOUCHES
     */
    ST_Touches(Boolean.class),

    /**
     * Test if a geometry value spatially crosses another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     * This operation is similar to Filter Encoding "Crosses" operation except in the way the CRS is selected
     * (the SQLMM standard requires that we use the CRS of the first geometry) and in the return value when
     * an error occurred (this implementation returns {@code null}).
     *
     * @see SpatialOperatorName#CROSSES
     */
    ST_Crosses(Boolean.class),

    /**
     * Test if a geometry value is spatially within another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     * This operation is similar to Filter Encoding "Within" operation except in the way the CRS is selected
     * (the SQLMM standard requires that we use the CRS of the first geometry) and in the return value when
     * an error occurred (this implementation returns {@code null}).
     *
     * @see SpatialOperatorName#WITHIN
     */
    ST_Within(Boolean.class),

    /**
     * Test if a geometry value spatially contains another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     * This operation is similar to Filter Encoding "Contains" operation except in the way the CRS is selected
     * (the SQLMM standard requires that we use the CRS of the first geometry) and in the return value when an
     * error occurred (this implementation returns {@code null}).
     *
     * @see SpatialOperatorName#CONTAINS
     */
    ST_Contains(Boolean.class),

    /**
     * Test if a geometry value spatially overlaps another geometry value,
     * ignoring <var>z</var> and <var>m</var> coordinate values.
     * This operation is similar to Filter Encoding "Overlaps" operation except in the way the CRS is selected
     * (the SQLMM standard requires that we use the CRS of the first geometry) and in the return value when an
     * error occurred (this implementation returns {@code null}).
     *
     * @see SpatialOperatorName#OVERLAPS
     */
    ST_Overlaps(Boolean.class),

    /**
     * The Well-Known Text (WKT) representation of a geometry.
     */
    ST_AsText(GEOMETRY, String.class),

    /**
     * The Well-Known Binary (WKB) representation of a geometry.
     */
    ST_AsBinary(GEOMETRY, byte[].class),

    /**
     * Constructor for a geometry which is transformed from a Well-Known Text (WKT) representation.
     *
     * @see #ST_PointFromText
     * @see #ST_LineFromText
     * @see #ST_PolyFromText
     * @see #ST_GeomCollFromText
     * @see #ST_MPointFromText
     * @see #ST_MLineFromText
     * @see #ST_MPolyFromText
     */
    ST_GeomFromText(1, 2, null, null, GEOMETRY),

    /**
     * Constructor for a geometry which is transformed from a Well-Known Binary (WKB) representation.
     *
     * @see #ST_PointFromWKB
     * @see #ST_LineFromWKB
     * @see #ST_PolyFromWKB
     * @see #ST_GeomCollFromWKB
     * @see #ST_MPointFromWKB
     * @see #ST_MLineFromWKB
     * @see #ST_MPolyFromWKB
     */
    ST_GeomFromWKB(1, 2, null, null, GEOMETRY),

    /**
     * Point geometry created from coordinate values.
     * In current implementation, the parameters can be:
     * <ul>
     *   <li>WKT|WKB</li>
     *   <li>WKT|WKB, CoordinateReferenceSystem</li>
     *   <li>X, Y</li>
     *   <li>X, Y, CoordinateReferenceSystem</li>
     *   <li>X, Y, Z</li>
     *   <li>X, Y, Z, CoordinateReferenceSystem</li>
     * </ul>
     */
    ST_Point(2, 3, null, null, POINT),

    /**
     * The <var>x</var> coordinate value of a point.
     */
    ST_X(POINT, Number.class),

    /**
     * The <var>y</var> coordinate value of a point.
     */
    ST_Y(POINT, Number.class),

    /**
     * The <var>z</var> coordinate value of a point.
     */
    ST_Z(POINT, Number.class),

    /**
     * The coordinate values as a {@code DOUBLE PRECISION ARRAY} value.
     */
    ST_ExplicitPoint(POINT, double[].class),

    /**
     * Constructor for a point which is transformed from a Well-Known Text (WKT) representation.
     *
     * @see #ST_GeomFromText
     * @see #ST_LineFromText
     * @see #ST_PolyFromText
     * @see #ST_GeomCollFromText
     * @see #ST_MPointFromText
     * @see #ST_MLineFromText
     * @see #ST_MPolyFromText
     */
    ST_PointFromText(1, 2, null, null, POINT),

    /**
     * Constructor for a point which is transformed from a Well-Known Binary (WKB) representation.
     *
     * @see #ST_GeomFromWKB
     * @see #ST_LineFromWKB
     * @see #ST_PolyFromWKB
     * @see #ST_GeomCollFromWKB
     * @see #ST_MPointFromWKB
     * @see #ST_MLineFromWKB
     * @see #ST_MPolyFromWKB
     */
    ST_PointFromWKB(1, 2, null, null, POINT),

    /**
     * The length measurement of a curve, ignoring <var>z</var> and <var>m</var> coordinate values in the calculations.
     *
     * @todo Current value type is {@link Number}. Consider using {@link Quantity} instead.
     *
     * @see #ST_Perimeter
     */
    ST_Length(GEOMETRY, Number.class),

    /**
     * The point value that is the start point of a curve,
     * including existing <var>z</var> and <var>m</var> coordinates.
     */
    ST_StartPoint(LINESTRING, POINT),

    /**
     * The point value that is the end point of a curve,
     * including existing <var>z</var> and <var>m</var> coordinates.
     */
    ST_EndPoint(LINESTRING, POINT),

    /**
     * Test if a curve is closed, ignoring <var>z</var> and <var>m</var> coordinates.
     */
    ST_IsClosed(LINESTRING, Boolean.class),

    /**
     * Test if a curve is a ring, ignoring <var>z</var> and <var>m</var> coordinates.
     */
    ST_IsRing(LINESTRING, Boolean.class),

    /**
     * Line-string representation of a curve, including the <var>z</var> values.
     */
    ST_ToLineString(GEOMETRY, LINESTRING),

    /**
     * {@code LineString} constructed from either a Well-Known Text (WKT) representation,
     * a Well-Known Binary (WKB) representation, or an array of points.
     *
     * @see #ST_Polygon
     * @see #ST_GeomCollection
     * @see #ST_MultiPoint
     * @see #ST_MultiLineString
     * @see #ST_MultiPolygon
     */
    ST_LineString(1, 2, null, null, LINESTRING),

    /**
     * The cardinality of the points of a {@code LineString}.
     */
    ST_NumPoints(LINESTRING, Integer.class),

    /**
     * The specified element in the points of a {@code LineString}.
     */
    ST_PointN(2, 2, LINESTRING, null, POINT),

    /**
     * Constructor for a {@code LineString} which is transformed from a Well-Known Text (WKT) representation.
     *
     * @see #ST_GeomFromText
     * @see #ST_PointFromText
     * @see #ST_PolyFromText
     * @see #ST_GeomCollFromText
     * @see #ST_MPointFromText
     * @see #ST_MLineFromText
     * @see #ST_MPolyFromText
     */
    ST_LineFromText(1, 2, null, null, LINESTRING),

    /**
     * Constructor for a {@code LineString} which is transformed from a Well-Known Binary (WKB) representation.
     *
     * @see #ST_GeomFromWKB
     * @see #ST_PointFromWKB
     * @see #ST_PolyFromWKB
     * @see #ST_GeomCollFromWKB
     * @see #ST_MPointFromWKB
     * @see #ST_MLineFromWKB
     * @see #ST_MPolyFromWKB
     */
    ST_LineFromWKB(1, 2, null, null, LINESTRING),

    /**
     * The area measurement of a surface, ignoring <var>z</var> and <var>m</var> coordinates.
     *
     * <h4>Limitation</h4>
     * The SQLMM standard allows an optional {@code CHARACTER VARYING} argument for specifying
     * the desired unit of measurement, which must be linear. This is not yet implemented.
     *
     * @todo Current value type is {@link Number}. Consider using {@link Quantity} instead.
     */
    ST_Area(POLYGON, Number.class),

    /**
     * The length measurement of the boundary of a surface,
     * ignoring <var>z</var> and <var>m</var> coordinates.
     *
     * @todo Current value type is {@link Number}. Consider using {@link Quantity} instead.
     *
     * @see #ST_Length
     */
    ST_Perimeter(POLYGON, Number.class),

    /**
     * The 2D point value that is the mathematical centroid of the surface,
     * ignoring <var>z</var> and <var>m</var> coordinates.
     */
    ST_Centroid(GEOMETRY, POINT),

    /**
     * A point guaranteed to spatially intersect the surface,
     * ignoring <var>z</var> and <var>m</var> coordinates.
     */
    ST_PointOnSurface(GEOMETRY, POINT),

    /**
     * Polygon constructed from either a Well-Known Text (WKT) representation,
     * a Well-Known Binary (WKB) representation, or an array of points.
     *
     * @see #ST_LineString
     * @see #ST_GeomCollection
     * @see #ST_MultiPoint
     * @see #ST_MultiLineString
     * @see #ST_MultiPolygon
     */
    ST_Polygon(1, 2, null, null, POLYGON),

    /**
     * The exterior ring of a polygon.
     */
    ST_ExteriorRing(POLYGON, LINESTRING),

    /**
     * The cardinality of the interior rings a polygon.
     */
    ST_NumInteriorRings(POLYGON, Integer.class),

    /**
     * The specified element in the interior rings of a polygon.
     */
    ST_InteriorRingN(2, 2, POLYGON, null, LINESTRING),

    /**
     * Constructor for a polygon which is transformed from a Well-Known Text (WKT) representation.
     *
     * @see #ST_GeomFromText
     * @see #ST_PointFromText
     * @see #ST_LineFromText
     * @see #ST_GeomCollFromText
     * @see #ST_MPointFromText
     * @see #ST_MLineFromText
     * @see #ST_MPolyFromText
     */
    ST_PolyFromText(1, 2, null, null, POLYGON),

    /**
     * Constructor for a polygon which is transformed from a Well-Known Binary (WKB) representation.
     *
     * @see #ST_GeomFromWKB
     * @see #ST_PointFromWKB
     * @see #ST_LineFromWKB
     * @see #ST_GeomCollFromWKB
     * @see #ST_MPointFromWKB
     * @see #ST_MLineFromWKB
     * @see #ST_MPolyFromWKB
     */
    ST_PolyFromWKB(1, 2, null, null, POLYGON),

    /**
     * Constructor for a polygon which is transformed from a Well-Known Text (WKT) representation of polylines.
     * The first polyline defines the exterior ring and all additional polylines define interior rings.
     * If the WKT defines directly a polygon, then it is used as-is (this is an Apache SIS extension).
     *
     * @see #ST_BdMPolyFromText
     */
    ST_BdPolyFromText(1, 2, null, null, POLYGON),

    /**
     * Constructor for a polygon which is transformed from a Well-Known Binary (WKB) representation of polylines.
     * The first polyline defines the exterior ring and all additional polylines define interior rings.
     * If the WKB defines directly a polygon, then it is used as-is (this is an Apache SIS extension).
     *
     * @see #ST_BdMPolyFromWKB
     */
    ST_BdPolyFromWKB(1, 2, null, null, POLYGON),

    /**
     * Collection of geometries constructed from either a Well-Known Text (WKT) representation,
     * a Well-Known Binary (WKB) representation, or an array of geometry components.
     *
     * @see #ST_LineString
     * @see #ST_Polygon
     * @see #ST_MultiPoint
     * @see #ST_MultiLineString
     * @see #ST_MultiPolygon
     */
    ST_GeomCollection(1, 2, null, null, GEOMETRYCOLLECTION),

    /**
     * The cardinality of the geometries of a geometry collection.
     */
    ST_NumGeometries(GEOMETRYCOLLECTION, Integer.class),

    /**
     * The specified element in a geometry collection.
     */
    ST_GeometryN(2, 2, GEOMETRYCOLLECTION, null, GEOMETRY),

    /**
     * Constructor for a geometry collection which is transformed from a Well-Known Text (WKT) representation.
     *
     * @see #ST_GeomFromText
     * @see #ST_PointFromText
     * @see #ST_LineFromText
     * @see #ST_PolyFromText
     * @see #ST_MPointFromText
     * @see #ST_MLineFromText
     * @see #ST_MPolyFromText
     */
    ST_GeomCollFromText(1, 2, null, null, GEOMETRYCOLLECTION),

    /**
     * Constructor for a geometry collection which is transformed from a Well-Known Binary (WKB) representation.
     *
     * @see #ST_GeomFromWKB
     * @see #ST_PointFromWKB
     * @see #ST_LineFromWKB
     * @see #ST_PolyFromWKB
     * @see #ST_MPointFromWKB
     * @see #ST_MLineFromWKB
     * @see #ST_MPolyFromWKB
     */
    ST_GeomCollFromWKB(1, 2, null, null, GEOMETRYCOLLECTION),

    /**
     * {@code MultiPoint} constructed from either a Well-Known Text (WKT) representation,
     * a Well-Known Binary (WKB) representation, or an array of points.
     *
     * @see #ST_LineString
     * @see #ST_Polygon
     * @see #ST_GeomCollection
     * @see #ST_MultiLineString
     * @see #ST_MultiPolygon
     */
    ST_MultiPoint(1, 2, null, null, MULTIPOINT),

    /**
     * Constructor for a multi-point which is transformed from a Well-Known Text (WKT) representation.
     *
     * @see #ST_GeomFromText
     * @see #ST_PointFromText
     * @see #ST_LineFromText
     * @see #ST_PolyFromText
     * @see #ST_GeomCollFromText
     * @see #ST_MLineFromText
     * @see #ST_MPolyFromText
     */
    ST_MPointFromText(1, 2, null, null, MULTIPOINT),

    /**
     * Constructor for a multi-point which is transformed from a Well-Known Binary (WKB) representation.
     *
     * @see #ST_GeomFromWKB
     * @see #ST_PointFromWKB
     * @see #ST_LineFromWKB
     * @see #ST_PolyFromWKB
     * @see #ST_GeomCollFromWKB
     * @see #ST_MLineFromWKB
     * @see #ST_MPolyFromWKB
     */
    ST_MPointFromWKB(1, 2, null, null, MULTIPOINT),

    /**
     * {@code MultiLineString} constructed from either a Well-Known Text (WKT) representation,
     * a Well-Known Binary (WKB) representation, or an array of points.
     *
     * @see #ST_LineString
     * @see #ST_Polygon
     * @see #ST_GeomCollection
     * @see #ST_MultiPoint
     * @see #ST_MultiPolygon
     */
    ST_MultiLineString(1, 2, null, null, MULTILINESTRING),

    /**
     * Constructor for a multi-line string which is transformed from a Well-Known Text (WKT) representation.
     *
     * @see #ST_GeomFromText
     * @see #ST_PointFromText
     * @see #ST_LineFromText
     * @see #ST_PolyFromText
     * @see #ST_GeomCollFromText
     * @see #ST_MPointFromText
     * @see #ST_MPolyFromText
     */
    ST_MLineFromText(1, 2, null, null, MULTILINESTRING),

    /**
     * Constructor for a multi-line string which is transformed from a Well-Known Binary (WKB) representation.
     *
     * @see #ST_GeomFromWKB
     * @see #ST_PointFromWKB
     * @see #ST_LineFromWKB
     * @see #ST_PolyFromWKB
     * @see #ST_GeomCollFromWKB
     * @see #ST_MPointFromWKB
     * @see #ST_MPolyFromWKB
     */
    ST_MLineFromWKB(1, 2, null, null, MULTILINESTRING),

    /**
     * {@code MultiPolygon} constructed from either a Well-Known Text (WKT) representation,
     * a Well-Known Binary (WKB) representation, or an array of points.
     *
     * @see #ST_LineString
     * @see #ST_Polygon
     * @see #ST_GeomCollection
     * @see #ST_MultiPoint
     * @see #ST_MultiLineString
     */
    ST_MultiPolygon(1, 2, null, null, MULTIPOLYGON),

    /**
     * Constructor for a multi-polygon which is transformed from a Well-Known Text (WKT) representation.
     *
     * @see #ST_GeomFromText
     * @see #ST_PointFromText
     * @see #ST_LineFromText
     * @see #ST_PolyFromText
     * @see #ST_GeomCollFromText
     * @see #ST_MPointFromText
     * @see #ST_MLineFromText
     */
    ST_MPolyFromText(1, 2, null, null, MULTIPOLYGON),

    /**
     * Constructor for a multi-polygon which is transformed from a Well-Known Binary (WKB) representation.
     *
     * @see #ST_GeomFromWKB
     * @see #ST_PointFromWKB
     * @see #ST_LineFromWKB
     * @see #ST_PolyFromWKB
     * @see #ST_GeomCollFromWKB
     * @see #ST_MPointFromWKB
     * @see #ST_MLineFromWKB
     */
    ST_MPolyFromWKB(1, 2, null, null, MULTIPOLYGON),

    /**
     * Constructor for a multi-polygon which is transformed from a Well-Known Text (WKT) representation
     * of multi line string. There is one polygon for each line-string.
     *
     * @see #ST_BdPolyFromText
     */
    ST_BdMPolyFromText(1, 2, null, null, MULTIPOLYGON),

    /**
     * Constructor for a multi-polygon which is transformed from a Well-Known Binary (WKB) representation
     * of multi line string. There is one polygon for each line-string.
     */
    ST_BdMPolyFromWKB(1, 2, null, null, MULTIPOLYGON),

    /**
     * Cast a geometry to a specific instantiable subtype of geometry.
     */
    ST_ToPoint(GEOMETRY, POINT),

    /**
     * Cast a geometry to a specific instantiable subtype of geometry.
     */
    ST_ToPolygon(GEOMETRY, POLYGON),

    /**
     * Cast a geometry to a specific instantiable subtype of geometry.
     */
    ST_ToMultiPoint(GEOMETRY, MULTIPOINT),

    /**
     * Cast a geometry to a specific instantiable subtype of geometry.
     */
    ST_ToMultiLine(GEOMETRY, MULTILINESTRING),

    /**
     * Cast a geometry to a specific instantiable subtype of geometry.
     */
    ST_ToMultiPolygon(GEOMETRY, MULTIPOLYGON),

    /**
     * Cast a geometry to a specific instantiable subtype of geometry.
     */
    ST_ToGeomColl(GEOMETRY, GEOMETRYCOLLECTION),

    /**
     * Computes a geometry simplification.
     * This function expects a geometry and a distance tolerance.
     * The distance shall be in units of the geometry Coordinate Reference System
     *
     * <p>Note: this function is defined in PostGIS and H2GIS but not in SQL/MM 13249-3 2011.</p>
     */
    ST_Simplify(2, 2, GEOMETRY, null, GEOMETRY),

    /**
     * Computes a geometry simplification preserving topology.
     * This function expects a geometry and a distance tolerance.
     * The distance shall be in units of the geometry Coordinate Reference System
     *
     * <p>Note: this function is defined in PostGIS and H2GIS but not in SQL/MM 13249-3 2011.</p>
     */
    ST_SimplifyPreserveTopology(2, 2, GEOMETRY, null, GEOMETRY);

    /**
     * The minimum number of parameters that the function expects.
     */
    public final byte minParamCount;

    /**
     * The maximum number of parameters that the function expects.
     */
    public final byte maxParamCount;

    /**
     * The geometry type of the two first parameters,
     * or {@code null} if the parameter is not a geometry.
     */
    final GeometryType geometryType1, geometryType2;

    /**
     * Type of value returned by the method as a {@link Class} or a {@link GeometryType}.
     *
     * @see #getReturnType(Geometries)
     */
    private final Object returnType;

    /**
     * Description of this SQLMM function, created when first needed.
     * The associated Java type depends on the geometry library.
     *
     * @see #description(Geometries)
     */
    private transient EnumMap<GeometryLibrary,AvailableFunction> descriptions;

    /**
     * Creates a new enumeration value for an operation expecting exactly one geometry object
     * and no other argument.
     */
    private SQLMM(final GeometryType geometryType1, final Object returnType) {
        this.minParamCount = 1;
        this.maxParamCount = 1;
        this.geometryType1 = geometryType1;
        this.geometryType2 = null;
        this.returnType    = returnType;
    }

    /**
     * Creates a new enumeration value for an operation expecting exactly two geometry objects
     * and no other argument.
     */
    private SQLMM(final Object returnType) {
        this.minParamCount = 2;
        this.maxParamCount = 2;
        this.geometryType1 = GEOMETRY;
        this.geometryType2 = GEOMETRY;
        this.returnType    = returnType;
    }

    /**
     * Creates a new enumeration value.
     */
    private SQLMM(final int minParamCount,
                  final int maxParamCount,
                  final GeometryType geometryType1,
                  final GeometryType geometryType2,
                  final Object       returnType)
    {
        this.minParamCount = (byte) minParamCount;
        this.maxParamCount = (byte) maxParamCount;
        this.geometryType1 = geometryType1;
        this.geometryType2 = geometryType2;
        this.returnType    = returnType;
    }

    /**
     * Returns a description of this SQLMM function.
     * The Java types associated to arguments and return value depend on which geometry library is used.
     *
     * @param  library  the geometry library implementation to use.
     * @return description of this SQLMM function.
     */
    public final synchronized AvailableFunction description(final Geometries<?> library) {
        if (descriptions == null) {
            descriptions = new EnumMap<>(GeometryLibrary.class);
        }
        return descriptions.computeIfAbsent(library.library, (key) -> new FunctionDescription(this, library));
    }

    /**
     * Returns the number of parameters that are geometry objects. Those parameters shall be first.
     * This value shall be between {@link #minParamCount} and {@link #maxParamCount}.
     *
     * @return number of parameters that are geometry objects.
     */
    public final int geometryCount() {
        return (geometryType1 == null) ? 0 :
               (geometryType2 == null) ? 1 : 2;
    }

    /**
     * Returns {@code true} if the operation has at least one geometry argument
     * and the return type is also a geometry.
     */
    final boolean isGeometryInOut() {
        return (geometryType1 != null) && (returnType instanceof GeometryType);
    }

    /**
     * Returns the type of geometry returned by the SQLMM function.
     *
     * @return the type of geometry returned by the SQLMM function.
     */
    public final Optional<GeometryType> getGeometryType() {
        return (returnType instanceof GeometryType) ? Optional.of((GeometryType) returnType) : Optional.empty();
    }

    /**
     * Returns the type of value returned by the SQLMM function.
     *
     * @param  library  the handler used for wrapping geometry implementations.
     * @return type of value returned the SQLMM function.
     */
    public final Class<?> getReturnType(final Geometries<?> library) {
        return (returnType instanceof Class<?>) ? (Class<?>) returnType
                : library.getGeometryClass((GeometryType) returnType);
    }
}
