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
package org.apache.sis.geometry.wrapper.jts;

import java.awt.Shape;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.CRS;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.filter.sqlmm.SQLMM;
import static org.apache.sis.geometry.wrapper.GeometryType.POINT;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.DistanceOperatorName;


/**
 * The wrapper of Java Topology Suite (JTS) geometries.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
final class Wrapper extends GeometryWrapper {
    /**
     * The wrapped implementation.
     */
    private final Geometry geometry;

    /**
     * Creates a new wrapper around the given geometry.
     *
     * @param  geometry  the geometry to wrap.
     * @throws FactoryException if the <abbr>CRS</abbr> cannot be created from the <abbr>SRID</abbr> code.
     */
    Wrapper(final Geometry geometry) throws FactoryException {
        this.geometry = geometry;
        crs = JTS.getCoordinateReferenceSystem(geometry);
    }

    /**
     * Creates a new wrapper with the same <abbr>CRS</abbr> than the given wrapper.
     *
     * @param  source    the source wrapper from which is derived the geometry.
     * @param  geometry  the geometry to wrap.
     */
    private Wrapper(final Wrapper source, final Geometry geometry) {
        this.geometry = geometry;
        this.crs = source.crs;
        JTS.copyMetadata(source.geometry, geometry);
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    protected Geometries<Geometry> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Returns the geometry specified at construction time.
     */
    @Override
    protected Object implementation() {
        return geometry;
    }

    /**
     * Returns the Spatial Reference System Identifier (SRID) if available.
     * This is <em>not</em> necessarily an EPSG code, even it is common practice to use
     * the same numerical values as EPSG. Note that the absence of SRID does not mean
     * that {@link #getCoordinateReferenceSystem()} would return no CRS.
     */
    @Override
    public OptionalInt getSRID() {
        final int srid = geometry.getSRID();
        return (srid != 0) ? OptionalInt.of(srid) : OptionalInt.empty();
    }

    /**
     * Sets the coordinate reference system. This method overwrites any previous user object.
     * This is okay for the context in which Apache SIS uses this method, which is only for
     * newly created geometries.
     */
    @Override
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        super.setCoordinateReferenceSystem(crs);
        JTS.setCoordinateReferenceSystem(geometry, crs);
    }

    /**
     * Returns the dimension of the coordinates that define this geometry.
     */
    @Override
    public int getCoordinateDimension() {
        return getCoordinatesDimension(geometry);
    }

    /**
     * Gets the number of dimensions of geometry vertex (sequence of coordinate tuples), which can be 2 or 3.
     * Note that this is different than the {@linkplain Geometry#getDimension() geometry topological dimension},
     * which can be 0, 1 or 2.
     *
     * @param  geometry  the geometry for which to get <em>vertex</em> (not topological) dimension.
     * @return vertex dimension of the given geometry.
     * @throws IllegalArgumentException if the type of the given geometry is not recognized.
     */
    private static int getCoordinatesDimension(final Geometry geometry) {
        final CoordinateSequence cs;
        if (geometry instanceof Point) {
            // Most efficient method (no allocation) in JTS 1.18.
            cs = ((Point) geometry).getCoordinateSequence();
        } else if (geometry instanceof LineString) {
            // Most efficient method (no allocation) in JTS 1.18.
            cs = ((LineString) geometry).getCoordinateSequence();
        } else if (geometry instanceof Polygon) {
            return getCoordinatesDimension(((Polygon) geometry).getExteriorRing());
        } else if (geometry instanceof GeometryCollection) {
            final GeometryCollection gc = (GeometryCollection) geometry;
            final int n = gc.getNumGeometries();
            if (n == 0) {
                return Factory.TRIDIMENSIONAL;      // Undefined coordinates, JTS assumes 3 for empty geometries.
            }
            for (int i=0; i<n; i++) {
                // If at least one geometry is 3D, consider the whole geometry as 3D.
                final int d = getCoordinatesDimension(gc.getGeometryN(i));
                if (d > Factory.BIDIMENSIONAL) return d;
            }
            return Factory.BIDIMENSIONAL;
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, geometry.getGeometryType()));
        }
        return cs.getDimension();
    }

    /**
     * Returns the envelope of the wrapped JTS geometry. Never null, but may be empty.
     * In current implementation, <var>z</var> values of three-dimensional envelopes
     * are {@link Double#NaN}. It may change in a future version if we have a way to
     * get those <var>z</var> values from a JTS object.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        final Envelope bounds = geometry.getEnvelopeInternal();
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        final var env = (crs != null) ? new GeneralEnvelope(crs) : new GeneralEnvelope(Factory.BIDIMENSIONAL);
        env.setToNaN();
        if (!bounds.isNull()) {
            env.setRange(0, bounds.getMinX(), bounds.getMaxX());
            env.setRange(1, bounds.getMinY(), bounds.getMaxY());
        }
        return env;
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        final Coordinate c = geometry.getCentroid().getCoordinate();
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        if (crs == null) {
            final double z = c.getZ();
            if (!Double.isNaN(z)) {
                return new GeneralDirectPosition(c.x, c.y, z);
            }
        } else if (CRS.getDimensionOrZero(crs) != Factory.BIDIMENSIONAL) {
            final var point = new GeneralDirectPosition(crs);
            point.setCoordinate(0, c.x);
            point.setCoordinate(1, c.y);
            point.setCoordinate(2, c.getZ());
            return point;
        }
        return new DirectPosition2D(crs, c.x, c.y);
    }

    /**
     * If the wrapped geometry is a point, returns its coordinates. Otherwise returns {@code null}.
     * If non-null, the returned array may have a length of 2 or 3.
     */
    @Override
    public double[] getPointCoordinates() {
        if (!(geometry instanceof Point)) {
            return null;
        }
        final Coordinate pt = ((Point) geometry).getCoordinate();
        final double z = pt.getZ();
        final double[] coord;
        if (Double.isNaN(z)) {
            coord = new double[Factory.BIDIMENSIONAL];
        } else {
            coord = new double[Factory.TRIDIMENSIONAL];
            coord[2] = z;
        }
        coord[1] = pt.y;
        coord[0] = pt.x;
        return coord;
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    public double[] getAllCoordinates() {
        final Coordinate[] points = geometry.getCoordinates();
        final var coordinates = new double[points.length * Factory.BIDIMENSIONAL];
        int i = 0;
        for (final Coordinate p : points) {
            coordinates[i++] = p.x;
            coordinates[i++] = p.y;
        }
        return coordinates;
    }

    /**
     * Merges a sequence of points or paths after the wrapped geometry.
     *
     * @throws ClassCastException if an element in the iterator is not a JTS geometry.
     */
    @Override
    public Geometry mergePolylines(final Iterator<?> polylines) {
        final var coordinates = new ArrayList<Coordinate>();
        final var lines = new ArrayList<Geometry>();
        boolean isFloat = true;
add:    for (Geometry next = geometry;;) {
            if (next instanceof Point) {
                final Coordinate pt = ((Point) next).getCoordinate();
                if (!Double.isNaN(pt.x) && !Double.isNaN(pt.y)) {
                    isFloat = Factory.isFloat(isFloat, (Point) next);
                    coordinates.add(pt);
                } else {
                    Factory.INSTANCE.addPolyline(coordinates, lines, false, isFloat);
                    coordinates.clear();
                    isFloat = true;
                }
            } else {
                final int n = next.getNumGeometries();
                for (int i=0; i<n; i++) {
                    final var ls = (LineString) next.getGeometryN(i);
                    if (coordinates.isEmpty()) {
                        lines.add(ls);
                    } else {
                        if (isFloat) isFloat = Factory.isFloat(ls.getCoordinateSequence());
                        coordinates.addAll(Arrays.asList(ls.getCoordinates()));
                        Factory.INSTANCE.addPolyline(coordinates, lines, false, isFloat);
                        coordinates.clear();
                        isFloat = true;
                    }
                }
            }
            /*
             * `polylines.hasNext()` check is conceptually part of `for` instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = (Geometry) polylines.next()) == null);
        }
        Factory.INSTANCE.addPolyline(coordinates, lines, false, isFloat);
        return Factory.INSTANCE.groupPolylines(lines, false, isFloat);
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method assumes that the two geometries are in the same CRS (this is not verified).
     *
     * <p><b>Note:</b> {@link SpatialOperatorName#BBOX} is implemented by {@code NOT DISJOINT}.
     * It is caller's responsibility to ensure that one of the geometries is rectangular,
     * for example by a call to {@link Geometry#getEnvelope()}.</p>
     *
     * @throws ClassCastException if the given wrapper is not for the same geometry library.
     */
    @Override
    protected boolean predicateSameCRS(final SpatialOperatorName type, final GeometryWrapper other) {
        final int ordinal = type.ordinal();
        if (ordinal >= 0 && ordinal < PREDICATES.length) {
            final BiPredicate<Geometry,Geometry> op = PREDICATES[ordinal];
            if (op != null) {
                return op.test(geometry, ((Wrapper) other).geometry);
            }
        }
        return super.predicateSameCRS(type, other);
    }

    /**
     * All predicates recognized by {@link #predicate(SpatialOperatorName, Geometry)}.
     * Array indices are {@link SpatialOperatorName#ordinal()} values.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static final BiPredicate<Geometry,Geometry>[] PREDICATES =
            new BiPredicate[SpatialOperatorName.OVERLAPS.ordinal() + 1];
    static {
        PREDICATES[SpatialOperatorName.BBOX      .ordinal()] = (a,b) -> !a.disjoint(b);
        PREDICATES[SpatialOperatorName.EQUALS    .ordinal()] = Geometry::equalsTopo;
        PREDICATES[SpatialOperatorName.DISJOINT  .ordinal()] = Geometry::disjoint;
        PREDICATES[SpatialOperatorName.INTERSECTS.ordinal()] = Geometry::intersects;
        PREDICATES[SpatialOperatorName.TOUCHES   .ordinal()] = Geometry::touches;
        PREDICATES[SpatialOperatorName.CROSSES   .ordinal()] = Geometry::crosses;
        PREDICATES[SpatialOperatorName.WITHIN    .ordinal()] = Geometry::within;
        PREDICATES[SpatialOperatorName.CONTAINS  .ordinal()] = Geometry::contains;
        PREDICATES[SpatialOperatorName.OVERLAPS  .ordinal()] = Geometry::overlaps;
    }

    /**
     * Applies a filter predicate between this geometry and another geometry within a given distance.
     * This method assumes that the two geometries are in the same CRS and that the unit of measurement
     * is the same for {@code distance} than for axes (this is not verified).
     *
     * @throws ClassCastException if the given wrapper is not for the same geometry library.
     */
    @Override
    protected boolean predicateSameCRS(final DistanceOperatorName type,
                    final GeometryWrapper other, final double distance)
    {
        boolean reverse = (type != DistanceOperatorName.WITHIN);
        if (reverse && type != DistanceOperatorName.BEYOND) {
            return super.predicateSameCRS(type, other, distance);
        }
        return geometry.isWithinDistance(((Wrapper) other).geometry, distance) ^ reverse;
    }

    /**
     * Applies a SQLMM operation on this geometry.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry, or {@code null} if the operation requires only one geometry.
     * @param  argument   an operation-specific argument, or {@code null} if not applicable.
     * @return result of the specified operation.
     * @throws ClassCastException if the operation can only be executed on some specific argument types
     *         (for example geometries that are polylines) and one of the argument is not of that type.
     */
    @Override
    protected Object operationSameCRS(final SQLMM operation, final GeometryWrapper other, final Object argument) {
        /*
         * For all operation producing a geometry, the result is collected for post-processing.
         * For all other kinds of value, the result is returned directly in the switch statement.
         */
        final Geometry result;
        switch (operation) {
            case ST_IsMeasured:       return Boolean.FALSE;
            case ST_Dimension:        return geometry.getDimension();
            case ST_SRID:             return geometry.getSRID();
            case ST_IsEmpty:          return geometry.isEmpty();
            case ST_IsSimple:         return geometry.isSimple();
            case ST_IsValid:          return geometry.isValid();
            case ST_Envelope:         return getEnvelope();
            case ST_Boundary:         result = geometry.getBoundary(); break;
            case ST_ConvexHull:       result = geometry.convexHull(); break;
            case ST_Buffer:           result = geometry.buffer(((Number) argument).doubleValue()); break;
            case ST_Intersection:     result = geometry.intersection (((Wrapper) other).geometry); break;
            case ST_Union:            result = geometry.union        (((Wrapper) other).geometry); break;
            case ST_Difference:       result = geometry.difference   (((Wrapper) other).geometry); break;
            case ST_SymDifference:    result = geometry.symDifference(((Wrapper) other).geometry); break;
            case ST_Distance:         return   geometry.distance     (((Wrapper) other).geometry);
            case ST_Equals:           return   geometry.equalsTopo   (((Wrapper) other).geometry);
            case ST_Relate:           return   geometry.relate       (((Wrapper) other).geometry, argument.toString());
            case ST_Disjoint:         return   geometry.disjoint     (((Wrapper) other).geometry);
            case ST_Intersects:       return   geometry.intersects   (((Wrapper) other).geometry);
            case ST_Touches:          return   geometry.touches      (((Wrapper) other).geometry);
            case ST_Crosses:          return   geometry.crosses      (((Wrapper) other).geometry);
            case ST_Within:           return   geometry.within       (((Wrapper) other).geometry);
            case ST_Contains:         return   geometry.contains     (((Wrapper) other).geometry);
            case ST_Overlaps:         return   geometry.overlaps     (((Wrapper) other).geometry);
            case ST_AsText:           return new WKTWriter().write(geometry);   // WKTWriter() constructor is cheap.
            case ST_AsBinary:         return FilteringContext.writeWKB(geometry);
            case ST_X:                return ((Point) geometry).getX();
            case ST_Y:                return ((Point) geometry).getY();
            case ST_Z:                return ((Point) geometry).getCoordinate().getZ();
            case ST_ToLineString:     return geometry;                          // JTS does not have curves.
            case ST_NumGeometries:    return geometry.getNumGeometries();
            case ST_NumPoints:        return geometry.getNumPoints();
            case ST_PointN:           result = ((LineString) geometry).getPointN(toIndex(argument)); break;
            case ST_StartPoint:       result = ((LineString) geometry).getStartPoint(); break;
            case ST_EndPoint:         result = ((LineString) geometry).getEndPoint(); break;
            case ST_IsClosed:         return   ((LineString) geometry).isClosed();
            case ST_IsRing:           return   ((LineString) geometry).isRing();
            case ST_Perimeter:        // Fallthrough: length is the perimeter for polygons.
            case ST_Length:           return   geometry.getLength();
            case ST_Area:             return   geometry.getArea();
            case ST_Centroid:         result = geometry.getCentroid(); break;
            case ST_PointOnSurface:   result = geometry.getInteriorPoint(); break;
            case ST_ExteriorRing:     result = ((Polygon) geometry).getExteriorRing(); break;
            case ST_InteriorRingN:    result = ((Polygon) geometry).getInteriorRingN(toIndex(argument)); break;
            case ST_NumInteriorRings: return   ((Polygon) geometry).getNumInteriorRing();
            case ST_GeometryN:        result = geometry.getGeometryN(toIndex(argument)); break;
            case ST_ToPoint:
            case ST_ToPolygon:
            case ST_ToMultiPoint:
            case ST_ToMultiLine:
            case ST_ToMultiPolygon:
            case ST_ToGeomColl: {
                final GeometryType target = operation.getGeometryType().get();
                final Class<?> type = getGeometryClass(target);
                if (type.isInstance(geometry)) {
                    return geometry;
                }
                result = convert(target);
                break;
            }
            case ST_Is3D: {
                final Coordinate c = geometry.getCoordinate();
                return (c != null) ? !Double.isNaN(c.z) : null;
            }
            case ST_CoordDim: {
                final Coordinate c = geometry.getCoordinate();
                return (c != null) ? Double.isNaN(c.z) ? Geometries.BIDIMENSIONAL : Geometries.TRIDIMENSIONAL : null;
            }
            case ST_GeometryType: {
                for (int i=0; i < TYPES.length; i++) {
                    if (TYPES[i].isInstance(geometry)) {
                        return SQLMM_NAMES[i];
                    }
                }
                return null;
            }
            case ST_ExplicitPoint: {
                final Coordinate c = ((Point) geometry).getCoordinate();
                if (c == null) return ArraysExt.EMPTY_DOUBLE;
                final double x = c.getX();
                final double y = c.getY();
                final double z = c.getZ();
                return Double.isNaN(z) ? new double[] {x, y} : new double[] {x, y, z};
            }
            case ST_Simplify: {
                final double distance = ((Number) argument).doubleValue();
                result = DouglasPeuckerSimplifier.simplify(geometry, distance);
                break;
            }
            case ST_SimplifyPreserveTopology: {
                final double distance = ((Number) argument).doubleValue();
                result = TopologyPreservingSimplifier.simplify(geometry, distance);
                break;
            }
            default: return super.operationSameCRS(operation, other, argument);
        }
        JTS.copyMetadata(geometry, result);
        return result;
    }

    /**
     * The types of JTS objects to be recognized by the SQLMM {@code ST_GeometryType} operation.
     */
    private static final Class<?>[] TYPES = {
        Point.class, LineString.class, Polygon.class,
        MultiPoint.class, MultiLineString.class, MultiPolygon.class,
        GeometryCollection.class, Geometry.class,
    };

    /**
     * The SQLMM names for the types listed in the {@link #TYPES} array.
     */
    private static final String[] SQLMM_NAMES = {
        "ST_Point", "ST_LineString", "ST_Polygon",
        "ST_MultiPoint", "ST_MultiLineString", "ST_MultiPolygon",
        "ST_GeomCollection", "ST_Geometry"
    };

    /**
     * Converts the given argument to a zero-based index.
     *
     * @throws ClassCastException if the argument is not a string or a number.
     * @throws NumberFormatException if the argument is an unparseable string.
     * @throws IllegalArgumentException if the argument is zero or negative.
     */
    private static int toIndex(final Object argument) {
        final int i = (argument instanceof CharSequence)
                ? Integer.parseInt(argument.toString())
                : ((Number) argument).intValue();           // ClassCastException is part of this method contract.
        ArgumentChecks.ensureStrictlyPositive("index", i);
        return i - 1;
    }

    /**
     * Converts the wrapped geometry to the specified type.
     * If the geometry is already of that type, it is returned unchanged.
     * Otherwise coordinates are copied in a new geometry of the requested type.
     *
     * <p>The following conversions are illegal and will cause an {@link IllegalArgumentException} to be thrown:</p>
     * <ul>
     *   <li>From point to polyline or polygon (exception thrown by JTS itself).</li>
     *   <li>From geometry collection (except multi-point) to polyline.</li>
     *   <li>From geometry collection (except multi-point and multi-line string) to polygon.</li>
     *   <li>From geometry collection containing nested collections.</li>
     * </ul>
     *
     * The conversion from {@link MultiLineString} to {@link Polygon} is defined as following:
     * the first {@link LineString} is taken as the exterior {@link LinearRing} and all others
     * {@link LineString}s are interior {@link LinearRing}s.
     * This rule is defined by some SQLMM operations.
     *
     * @param  target  the desired type.
     * @return the converted geometry.
     * @throws IllegalArgumentException if the geometry cannot be converted to the specified type.
     */
    @Override
    public GeometryWrapper toGeometryType(final GeometryType target) {
        if (!getGeometryClass(target).isInstance(geometry)) {
            final Geometry result = convert(target);
            if (result != geometry) {
                return new Wrapper(this, result);
            }
        }
        return this;
    }

    /**
     * Mapping from implementation-neutral geometry types to their JTS classes.
     */
    private static final EnumMap<GeometryType, Class<?>> GEOMETRY_CLASSES = new EnumMap<>(GeometryType.class);
    static {
        final var m = GEOMETRY_CLASSES;
        /*
         * Following types are intentionally omitted:
         *   - GEOMETRY  because it is the first enumeration value, which is too early for `isAssignableFrom(Class)` checks.
         *   - TRIANGLE  because it is not a geometry in JTS class hierarchy.
         */
        m.put(GeometryType.POINT,              Point.class);
        m.put(GeometryType.LINESTRING,         LineString.class);
        m.put(GeometryType.POLYGON,            Polygon.class);
        m.put(GeometryType.MULTIPOINT,         MultiPoint.class);
        m.put(GeometryType.MULTILINESTRING,    MultiLineString.class);
        m.put(GeometryType.MULTIPOLYGON,       MultiPolygon.class);
        m.put(GeometryType.GEOMETRYCOLLECTION, GeometryCollection.class);
        // If other collection types are added, verify if they are enumerated before `GEOMETRYCOLLECTION`.
    }

    /**
     * Returns the JTS geometry class for the given implementation-neutral type.
     * If the given type has no specific JTS classes, then the generic {@link Geometry} is returned.
     *
     * @param  type  type of geometry for which the class is desired.
     * @return implementation class for the geometry of the specified type.
     */
    static Class<?> getGeometryClass(final GeometryType type) {
        return GEOMETRY_CLASSES.getOrDefault(type, Geometry.class);
    }

    /**
     * Returns the implementation-neutral type for the given JTS class or array class.
     * If the given class is not recognized, then {@link GeometryType#GEOMETRY} is returned.
     *
     * @param  type  the JTS class, or an array type having a JTS class as components.
     * @return the implementation-neutral type for the given class, or {@code GEOMETRY} if not recognized.
     */
    static GeometryType getGeometryType(final Class<?> type) {
        Class<?> componentType = type.getComponentType();
        if (componentType == null) componentType = type;
        for (final EnumMap.Entry<GeometryType, Class<?>> entry : GEOMETRY_CLASSES.entrySet()) {
            if (entry.getValue().isAssignableFrom(componentType)) {
                final GeometryType gt = entry.getKey();
                if (!gt.isCollection && componentType != type) {
                    var ct = gt.collection();   // If the given type was an array, return a geometry collection type.
                    if (ct != null) return ct;
                }
                return gt;
            }
        }
        return GeometryType.GEOMETRY;
    }

    /**
     * Converts the given geometry to the specified type without wrapper.
     * This is the implementation of {@link #toGeometryType(GeometryType)}.
     * Caller should invoke {@link JTS#copyMetadata(Geometry, Geometry)} after this method.
     *
     * @param  target  the desired type.
     * @return the converted geometry.
     * @throws IllegalArgumentException if the geometry cannot be converted to the specified type.
     */
    private Geometry convert(final GeometryType target) {
        final GeometryFactory factory = geometry.getFactory();
        switch (target) {
            case POINT: {
                return geometry.getCentroid();
            }
            case LINESTRING: {
                if (isCollection(geometry)) break;
                return factory.createLineString(geometry.getCoordinates());
            }
            case POLYGON: {
                if (!geometry.isEmpty() && geometry instanceof MultiLineString) {
                    // SQLMM `ST_BdMPolyFromText` and `ST_BdMPolyFromWKB` behavior.
                    final MultiLineString lines  = (MultiLineString) geometry;
                    final LinearRing   exterior  = factory.createLinearRing(lines.getGeometryN(0).getCoordinates());
                    final LinearRing[] interiors = new LinearRing[lines.getNumGeometries() - 1];
                    for (int i=0; i < interiors.length;) {
                        interiors[i] = factory.createLinearRing(lines.getGeometryN(++i).getCoordinates());
                    }
                    return factory.createPolygon(exterior, interiors);
                }
                if (isCollection(geometry)) break;
                return factory.createPolygon(geometry.getCoordinates());
            }
            case MULTIPOINT: {
                return (geometry instanceof Point)
                        ? factory.createMultiPoint(new Point[] {(Point) geometry})
                        : factory.createMultiPointFromCoords(geometry.getCoordinates());
            }
            case MULTILINESTRING: {
                return toCollection(factory,
                        LineString.class, LineString[]::new,
                        GeometryFactory::createLineString,
                        GeometryFactory::createMultiLineString);
            }
            case MULTIPOLYGON: {
                return toCollection(factory,
                        Polygon.class, Polygon[]::new,
                        GeometryFactory::createPolygon,
                        GeometryFactory::createMultiPolygon);
            }
            case GEOMETRYCOLLECTION: {
                if (geometry instanceof Point) {
                    return factory.createMultiPoint(new Point[] {(Point) geometry});
                } else if (geometry instanceof LineString) {
                    return factory.createMultiLineString(new LineString[] {(LineString) geometry});
                } else if (geometry instanceof Polygon) {
                    return factory.createMultiPolygon(new Polygon[] {(Polygon) geometry});
                }
                break;
            }
        }
        throw new UnconvertibleObjectException(Errors.format(Errors.Keys.CanNotConvertFromType_2,
                geometry.getClass(), getGeometryClass(target)));
    }

    /**
     * Converts a single geometry or a geometry collection to a collection of another type.
     * This is a helper method for {@link #toGeometryType(GeometryType)}.
     *
     * @param  <T>             the compile-time value of {@code type}.
     * @param  factory         the factory to use for creating new geometries.
     * @param  type            the type of geometry components to put in a collection.
     * @param  newArray        constructor for a new array of given {@code type}.
     * @param  newComponent    constructor for a geometry component of given {@code type}.
     * @param  newCollection   constructor for a geometry collection from an array of components/
     * @return the geometry collection created from the given type.
     * @throws IllegalArgumentException if a geometry collection contains nested collection.
     */
    private <T extends Geometry> GeometryCollection toCollection(
            final GeometryFactory factory,
            final Class<T> type, final IntFunction<T[]> newArray,
            final BiFunction<GeometryFactory,Coordinate[],T> newComponent,
            final BiFunction<GeometryFactory,T[],GeometryCollection> newCollection)
    {
        final T[] components = newArray.apply(geometry.getNumGeometries());
        for (int i=0; i<components.length; i++) {
            final Geometry c = geometry.getGeometryN(i);
            if (type.isInstance(c)) {
                components[i] = type.cast(c);
            } else if (isCollection(c)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NestedElementNotAllowed_1, GeometryCollection.class));
            } else {
                components[i] = newComponent.apply(factory, c.getCoordinates());
            }
        }
        return newCollection.apply(factory, components);
    }

    /**
     * Returns {@code true} if the given geometry is a collection other than {@link MultiPoint}.
     * Collections are handled recursively by {@code getLineStrings(…)} and {@code getPolygons(…)}.
     */
    private static boolean isCollection(final Geometry geometry) {
        return (geometry.getNumGeometries() >= 2) && !(geometry instanceof MultiPoint);
    }

    /**
     * Transforms this geometry using the given coordinate operation.
     * If the operation is {@code null}, then the geometry is returned unchanged.
     * If the geometry uses a different CRS than the source CRS of the given operation
     * and {@code validate} is {@code true},
     * then a new operation to the target CRS will be automatically computed.
     *
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @param  validate   whether to validate the operation source CRS.
     * @throws FactoryException if transformation to the target CRS cannot be found.
     * @throws TransformException if the geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper transform(final CoordinateOperation operation, final boolean validate)
            throws FactoryException, TransformException
    {
        return rewrap(JTS.transform(geometry, operation, validate));
    }

    /**
     * Transforms this geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS is null or is the same CRS as current one, the geometry is returned unchanged.
     * If the geometry has no Coordinate Reference System, then the geometry is returned unchanged.
     *
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance), or {@code null}.
     * @throws TransformException if this geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper transform(final CoordinateReferenceSystem targetCRS) throws TransformException {
        try {
            return rewrap(JTS.transform(geometry, targetCRS));
        } catch (FactoryException e) {
            /*
             * We wrap that exception because `Geometry.transform(…)` does not declare `FactoryException`.
             * We may revisit in a future version if `Geometry.transform(…)` method declaration is updated.
             */
            throw new TransformException(e);
        }
    }

    /**
     * Transforms this geometry using the given transform.
     * If the transform is {@code null}, then the geometry is returned unchanged.
     * Otherwise, a new geometry is returned without CRS.
     *
     * @param  transform  the math transform to apply, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance, but never {@code null}).
     * @throws TransformException if the geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper transform(final MathTransform transform) throws FactoryException, TransformException {
        return rewrap(JTS.transform(geometry, transform));
    }

    /**
     * Returns the given geometry in a new wrapper, or {@code this} if {@code result} is same as current geometry.
     * If a new wrapper is created, then its <abbr>CRS</abbr> will be inferred from the geometry <abbr>SRID</abbr>
     * or user properties.
     *
     * @param  result  the geometry computed by a JTS operation.
     * @return wrapper for the given geometry. May be {@code this}.
     * @throws FactoryException if the <abbr>CRS</abbr> cannot be created from the <abbr>SRID</abbr> code.
     */
    private Wrapper rewrap(final Geometry result) throws FactoryException {
        return (result == geometry) ? this : new Wrapper(result);
    }

    /**
     * Returns a view over the JTS geometry as a Java2D shape. Changes in the JTS geometry
     * after this method call may be reflected in the returned shape in an unspecified way.
     *
     * @return a view over the geometry as a Java2D shape.
     */
    @Override
    public Shape toJava2D() {
        return JTS.asShape(geometry);
    }

    /**
     * Returns the WKT representation of the wrapped geometry.
     */
    @Override
    public String formatWKT(final double flatness) {
        return geometry.toText();
    }
}
