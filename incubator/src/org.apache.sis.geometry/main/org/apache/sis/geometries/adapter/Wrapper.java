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
package org.apache.sis.geometries.adapter;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.filter.sqlmm.SQLMM;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DE9IM;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.curve.MultiLineString;
import org.apache.sis.geometries.operation.GeometryProcessor;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.geometries.surface.MultiPolygon;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Tuple;
import org.apache.sis.maths.Vectors;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Debug;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;


/**
 * The wrapper of SIS geometries.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class Wrapper extends GeometryWrapper {


    /**
     * The types of SIS geometries to be recognized by the SQLMM {@code ST_GeometryType} operation.
     * The collections are tested before {@link GeometryCollection} itself, and the specialized
     * geometries before the type they specialize.
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
     * All predicates recognized by {@link #predicateSameCRS(SpatialOperatorName, GeometryWrapper)}.
     * Array indices are {@link SpatialOperatorName#ordinal()} values.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static final BiPredicate<Geometry,Geometry>[] PREDICATES =
            new BiPredicate[SpatialOperatorName.OVERLAPS.ordinal() + 1];
    static {
        PREDICATES[SpatialOperatorName.BBOX      .ordinal()] = (a,b) -> !a.disjoint(b);
        PREDICATES[SpatialOperatorName.EQUALS    .ordinal()] = Geometry::equal;
        PREDICATES[SpatialOperatorName.DISJOINT  .ordinal()] = Geometry::disjoint;
        PREDICATES[SpatialOperatorName.INTERSECTS.ordinal()] = Geometry::intersects;
        PREDICATES[SpatialOperatorName.TOUCHES   .ordinal()] = Geometry::touches;
        PREDICATES[SpatialOperatorName.CROSSES   .ordinal()] = Geometry::crosses;
        PREDICATES[SpatialOperatorName.WITHIN    .ordinal()] = Geometry::within;
        PREDICATES[SpatialOperatorName.CONTAINS  .ordinal()] = Geometry::contains;
        PREDICATES[SpatialOperatorName.OVERLAPS  .ordinal()] = Geometry::overlaps;
    }

    /**
     * The wrapped implementation.
     */
    private final Geometry geometry;

    /**
     * Creates a new wrapper around the given geometry.
     *
     * @param  geometry  the geometry to wrap.
     */
    Wrapper(final Geometry geometry) {
        this.geometry = geometry;
        crs = geometry.getCoordinateReferenceSystem();
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
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    protected Geometries<Geometry> factory() {
        return SIS.INSTANCE;
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
        return OptionalInt.empty();
    }

    /**
     * Sets the coordinate reference system. This method overwrites any previous user object.
     * This is okay for the context in which Apache SIS uses this method, which is only for
     * newly created geometries.
     */
    @Override
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        super.setCoordinateReferenceSystem(crs);
        geometry.setCoordinateReferenceSystem(crs);
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
     *
     * @param  geometry  the geometry for which to get <em>vertex</em> (not topological) dimension.
     * @return vertex dimension of the given geometry.
     */
    private static int getCoordinatesDimension(final Geometry geometry) {
        return geometry.getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
    }

    /**
     * Returns the envelope of SIS geometry. Never null, but may be empty.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        return new GeneralEnvelope(geometry.getEnvelope());
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        final Point centroid = geometry.getCentroid();
        if (centroid == null) {
            // The centroid of an empty geometry is the empty geometry, which is no position.
            return null;
        }
        return new GeneralDirectPosition(Vectors.asDirectPostion(centroid.getPosition()));
    }

    /**
     * If the wrapped geometry is a point, returns its coordinates. Otherwise returns {@code null}.
     * If non-null, the returned array may have a length of 2 or 3.
     */
    @Override
    public double[] getPointCoordinates() {
        if (!(geometry instanceof Point point)) {
            return null;
        }
        return point.getPosition().toArrayDouble();
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    public double[] getAllCoordinates() {
        final DataPoints points = geometry.getDataPoints();
        final int dimension = points.getDimension();
        final int size = points.size();
        final var coordinates = new double[Math.multiplyExact(size, dimension)];
        for (int i=0; i<size; i++) {
            points.getPosition(i).toArrayDouble(coordinates, i * dimension);
        }
        return coordinates;
    }

    /**
     * Merges a sequence of points or paths after the wrapped geometry.
     *
     * @throws ClassCastException if an element in the iterator is not a SIS geometry.
     */
    @Override
    public Geometry mergePolylines(final Iterator<?> polylines) {
        final var lines = new ArrayList<LineString>();
        final var coordinates = new ArrayList<Tuple<?>>();
add:    for (Geometry next = geometry;;) {
            if (next instanceof Point point) {
                final Tuple<?> position = point.getPosition();
                if (isDefined(position)) {
                    coordinates.add(position);
                } else {
                    addPolyline(lines, coordinates);
                }
            } else if (next instanceof GeometryCollection<?> collection) {
                for (int i=0, n=collection.getNumGeometries(); i<n; i++) {
                    addPolyline(lines, coordinates, (LineString) collection.getGeometryN(i));
                }
            } else {
                addPolyline(lines, coordinates, (LineString) next);
            }
            /*
             * `polylines.hasNext()` check is conceptually part of `for` instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = (Geometry) polylines.next()) == null);
        }
        addPolyline(lines, coordinates);
        switch (lines.size()) {
            case 0:  return GeometryFactory.createEmpty(crs);
            case 1:  return lines.get(0);
            default: return GeometryFactory.createMultiLineString(crs, lines.toArray(LineString[]::new));
        }
    }

    /**
     * Returns whether all the coordinates of the given position are defined. A position having at
     * least one {@link Double#NaN} coordinate separates two paths in {@link #mergePolylines(Iterator)}.
     */
    private static boolean isDefined(final Tuple<?> position) {
        for (int i=0, n=position.getDimension(); i<n; i++) {
            if (Double.isNaN(position.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Makes a line string from the given positions and adds it to the given list, then clears the
     * positions. Less than two positions cannot make a line string, in which case they are dropped.
     *
     * @param  lines        where to add the line string, if such object is created.
     * @param  coordinates  positions of the line string to create. This list is cleared by this method.
     */
    private static void addPolyline(final List<LineString> lines, final List<Tuple<?>> coordinates) {
        if (coordinates.size() >= 2) {
            final Tuple<?> first = coordinates.get(0);
            final Array positions = NDArrays.of(coordinates, first.getSampleSystem(), first.getDataType());
            lines.add(GeometryFactory.createLineString(GeometryFactory.createSequence(positions)));
        }
        coordinates.clear();
    }

    /**
     * Appends the given line string to the polyline under construction. If no position has been
     * accumulated yet, the given line string is added as-is. Otherwise its positions continue the
     * accumulated ones and the whole makes a single line string.
     *
     * @param  lines        where to add the line strings.
     * @param  coordinates  positions accumulated so far, cleared by this method if they are used.
     * @param  line         the line string to append.
     */
    private static void addPolyline(final List<LineString> lines, final List<Tuple<?>> coordinates, final LineString line) {
        if (coordinates.isEmpty()) {
            lines.add(line);
        } else {
            final DataPoints points = line.getDataPoints();
            for (int i=0, n=points.size(); i<n; i++) {
                coordinates.add(points.getPosition(i));
            }
            addPolyline(lines, coordinates);
        }
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method assumes that the two geometries are in the same CRS (this is not verified).
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
        final boolean reverse = (type != DistanceOperatorName.WITHIN);
        if (reverse && type != DistanceOperatorName.BEYOND) {
            return super.predicateSameCRS(type, other, distance);
        }
        /*
         * The distance is already expressed in the units of the coordinate system axes,
         * which the dimensionless quantity tells the operation to take as-is.
         */
        return geometry.withinDistance(((Wrapper) other).geometry,
                Quantities.create(distance, Units.UNITY)) ^ reverse;
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
        switch (operation) {
            case ST_Dimension:        return geometry.getTopologicDimension();
            case ST_CoordDim:         return geometry.getDimension();
            case ST_Is3D:             return geometry.is3D();
            case ST_SRID:             return getSRID().orElse(0);
            case ST_IsEmpty:          return geometry.isEmpty();
            case ST_IsSimple:         return geometry.isSimple();
            case ST_IsValid:          return geometry.isValid();
            case ST_Envelope:         return getEnvelope();
            case ST_Boundary:         return geometry.getBoundary();
            case ST_ConvexHull:       return geometry.convexHull();
            case ST_Buffer:           return geometry.buffer(((Number) argument).doubleValue());
            case ST_Intersection:     return geometry.intersection (((Wrapper) other).geometry);
            case ST_Union:            return geometry.union        (((Wrapper) other).geometry);
            case ST_Difference:       return geometry.difference   (((Wrapper) other).geometry);
            case ST_SymDifference:    return geometry.symDifference(((Wrapper) other).geometry);
            case ST_Distance:         return geometry.distance     (((Wrapper) other).geometry).getValue();
            case ST_Equals:           return geometry.equal        (((Wrapper) other).geometry);
            case ST_Disjoint:         return geometry.disjoint     (((Wrapper) other).geometry);
            case ST_Intersects:       return geometry.intersects   (((Wrapper) other).geometry);
            case ST_Touches:          return geometry.touches      (((Wrapper) other).geometry);
            case ST_Crosses:          return geometry.crosses      (((Wrapper) other).geometry);
            case ST_Within:           return geometry.within       (((Wrapper) other).geometry);
            case ST_Contains:         return geometry.contains     (((Wrapper) other).geometry);
            case ST_Overlaps:         return geometry.overlaps     (((Wrapper) other).geometry);
            case ST_Relate:           return geometry.relate(((Wrapper) other).geometry, DE9IM.valueOf(argument.toString()));
            case ST_AsText:           return geometry.asText();
            case ST_AsBinary:         return geometry.asBinary();
            case ST_Centroid:         return geometry.getCentroid();
            case ST_PointOnSurface:   return geometry.getRepresentativePoint();
            case ST_X:                return ((Point) geometry).getPosition().get(0);
            case ST_Y:                return ((Point) geometry).getPosition().get(1);
            case ST_ExplicitPoint:    return ((Point) geometry).getPosition().toArrayDouble();
            case ST_Length:           return ((Curve) geometry).getLength().getValue();
            case ST_StartPoint:       return ((Curve) geometry).getStartPoint();
            case ST_EndPoint:         return ((Curve) geometry).getEndPoint();
            case ST_IsClosed:         return ((Curve) geometry).isClosed();
            case ST_IsRing:           return ((Curve) geometry).isRing();
            case ST_NumPoints:        return ((LineString) geometry).getNumPoints();
            case ST_PointN:           return ((LineString) geometry).getPointN(toIndex(argument));
            case ST_Area:             return ((Surface) geometry).getArea().getValue();
            case ST_Perimeter:        return ((Surface) geometry).getPerimeter().getValue();
            case ST_ExteriorRing:     return ((Polygon) geometry).getExteriorRing();
            case ST_InteriorRingN:    return ((Polygon) geometry).getInteriorRingN(toIndex(argument));
            case ST_NumInteriorRings: return ((Polygon) geometry).getNumInteriorRing();
            case ST_NumGeometries:    return ((GeometryCollection<?>) geometry).getNumGeometries();
            case ST_GeometryN:        return ((GeometryCollection<?>) geometry).getGeometryN(toIndex(argument));
            case ST_Z: {
                // A geometry of a two-dimensional system has no z value to report.
                final Tuple<?> position = ((Point) geometry).getPosition();
                return (position.getDimension() > Geometries.BIDIMENSIONAL) ? position.get(2) : Double.NaN;
            }
            case ST_IsMeasured: {
                final DataPointsType type = geometry.getDataPointsType();
                return (type != null) && type.getAttributeNames().contains(DataPointsType.ATT_M);
            }
            case ST_GeometryType: {
                for (int i=0; i < TYPES.length; i++) {
                    if (TYPES[i].isInstance(geometry)) {
                        return SQLMM_NAMES[i];
                    }
                }
                return null;
            }
            case ST_ToLineString:
            case ST_ToPoint:
            case ST_ToPolygon:
            case ST_ToMultiPoint:
            case ST_ToMultiLine:
            case ST_ToMultiPolygon:
            case ST_ToGeomColl: {
                final GeometryType target = operation.getGeometryType().get();
                if (factory().getGeometryClass(target).isInstance(geometry)) {
                    return geometry;
                }
                return convert(target);
            }
            case ST_Simplify: {
                final double distance = ((Number) argument).doubleValue();
                return fromJTS(DouglasPeuckerSimplifier.simplify(asJTS(null), distance));
            }
            case ST_SimplifyPreserveTopology: {
                final double distance = ((Number) argument).doubleValue();
                return fromJTS(TopologyPreservingSimplifier.simplify(asJTS(null), distance));
            }
            default: return super.operationSameCRS(operation, other, argument);
        }
    }

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
     *   <li>From point to polyline or polygon.</li>
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
        if (!factory().getGeometryClass(target).isInstance(geometry)) {
            final Geometry result = convert(target);
            if (result != geometry) {
                return new Wrapper(this, result);
            }
        }
        return this;
    }

    /**
     * Converts the wrapped geometry to the specified type without wrapper.
     * This is the implementation of {@link #toGeometryType(GeometryType)}.
     *
     * @param  target  the desired type.
     * @return the converted geometry.
     * @throws IllegalArgumentException if the geometry cannot be converted to the specified type.
     */
    private Geometry convert(final GeometryType target) {
        switch (target) {
            case POINT: {
                return geometry.getCentroid();
            }
            case LINESTRING: {
                if (isCollection(geometry)) break;
                return GeometryFactory.createLineString(GeometryFactory.copy(geometry.getDataPoints()));
            }
            case POLYGON: {
                if (!geometry.isEmpty() && geometry instanceof MultiLineString lines) {
                    // SQLMM `ST_BdMPolyFromText` and `ST_BdMPolyFromWKB` behavior.
                    final int count = lines.getNumGeometries();
                    final LinearRing exterior = toRing(lines.getGeometryN(0));
                    final var interiors = new ArrayList<LinearRing>(count - 1);
                    for (int i=1; i<count; i++) {
                        interiors.add(toRing(lines.getGeometryN(i)));
                    }
                    return GeometryFactory.createPolygon(exterior, interiors);
                }
                if (isCollection(geometry)) break;
                return GeometryFactory.createPolygon(toRing(geometry), List.of());
            }
            case MULTIPOINT: {
                if (geometry instanceof Point point) {
                    return GeometryFactory.createMultiPoint(point);
                }
                return GeometryFactory.createMultiPoint(GeometryFactory.copy(geometry.getDataPoints()));
            }
            case MULTILINESTRING: {
                return GeometryFactory.createMultiLineString(crs,
                        components(LineString.class, LineString[]::new, GeometryFactory::createLineString));
            }
            case MULTIPOLYGON: {
                return GeometryFactory.createMultiPolygon(crs,
                        components(Polygon.class, Polygon[]::new,
                                   (points) -> GeometryFactory.createPolygon(GeometryFactory.createLinearRing(points), List.of())));
            }
            case GEOMETRYCOLLECTION: {
                if (geometry instanceof Point point) {
                    return GeometryFactory.createMultiPoint(point);
                } else if (geometry instanceof LineString line) {
                    return GeometryFactory.createMultiLineString(line);
                } else if (geometry instanceof Polygon polygon) {
                    return GeometryFactory.createMultiPolygon(polygon);
                }
                break;
            }
        }
        throw new UnconvertibleObjectException(Errors.format(Errors.Keys.CanNotConvertFromType_2,
                geometry.getClass(), factory().getGeometryClass(target)));
    }

    /**
     * Returns the components of the wrapped geometry as geometries of the given type, for building
     * a collection of that type. A geometry which is not a collection provides a single component.
     *
     * @param  <T>           the compile-time value of {@code type}.
     * @param  type          the type of geometry components to put in a collection.
     * @param  newArray      constructor for a new array of given {@code type}.
     * @param  newComponent  constructor for a geometry component of given {@code type}.
     * @return the components to put in a geometry collection.
     * @throws IllegalArgumentException if a geometry collection contains nested collection.
     */
    private <T extends Geometry> T[] components(final Class<T> type, final IntFunction<T[]> newArray,
                                                final Function<DataPoints,T> newComponent)
    {
        final GeometryCollection<?> source = (geometry instanceof GeometryCollection<?> c) ? c : null;
        final T[] components = newArray.apply((source != null) ? source.getNumGeometries() : 1);
        for (int i=0; i<components.length; i++) {
            final Geometry element = (source != null) ? source.getGeometryN(i) : geometry;
            if (type.isInstance(element)) {
                components[i] = type.cast(element);
            } else if (isCollection(element)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NestedElementNotAllowed_1, GeometryCollection.class));
            } else {
                components[i] = newComponent.apply(GeometryFactory.copy(element.getDataPoints()));
            }
        }
        return components;
    }

    /**
     * Returns {@code true} if the given geometry is a collection other than {@link MultiPoint}.
     * A multi-point is excluded because its positions make a polyline or a ring as well as the
     * positions of a single geometry do.
     */
    private static boolean isCollection(final Geometry candidate) {
        return (candidate instanceof GeometryCollection<?> collection)
                && !(candidate instanceof MultiPoint<?>)
                && collection.getNumGeometries() >= 2;
    }

    /**
     * Returns a ring having the positions of the given geometry. The positions are copied,
     * so that the returned ring does not share its data with the given geometry.
     */
    private static LinearRing toRing(final Geometry source) {
        return GeometryFactory.createLinearRing(GeometryFactory.copy(source.getDataPoints()));
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
            throws FactoryException, TransformException {
        if (operation == null) {
            return this;
        }
        MathTransform mt = operation.getMathTransform();
        if (validate && crs != null) {
            final CoordinateOperation step = CRS.findOperation(crs, operation.getSourceCRS(), null);
            mt = MathTransforms.concatenate(step.getMathTransform(), mt);
        }
        return new Wrapper(new GeometryProcessor().transform(geometry, operation.getTargetCRS(), mt));
    }

    /**
     * Transforms this geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS is null or is the same CRS as current one, the geometry is returned unchanged.
     *
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance), or {@code null}.
     * @throws TransformException if this geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper transform(final CoordinateReferenceSystem targetCRS) throws TransformException {
        if (targetCRS == null || targetCRS == crs || crs == null) {
            return this;
        }
        try {
            return transform(CRS.findOperation(crs, targetCRS, null), false);
        } catch (FactoryException e) {
            throw new TransformException(e);
        }
    }

    /**
     * Transforms this geometry using the given transform.
     * If the transform is {@code null}, then the geometry is returned unchanged.
     *
     * @param  transform  the math transform to apply, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance, but never {@code null}).
     * @throws TransformException if the geometry cannot be transformed.
     */
    @Override
    public GeometryWrapper transform(final MathTransform transform) throws FactoryException, TransformException {
        if (transform == null || transform.isIdentity()) {
            return this;
        }
        return new Wrapper(this, new GeometryProcessor().transform(geometry, null, transform));
    }

    /**
     * Returns a view over the SIS geometry as a Java2D shape. Changes in the SIS geometry
     * after this method call may be reflected in the returned shape in an unspecified way.
     *
     * @return a view over the geometry as a Java2D shape.
     */
    @Override
    public Shape toJava2D() {
        return new ShapeAdapter(geometry);
    }

    /**
     * Returns the WKT representation of the wrapped geometry.
     */
    @Override
    public String formatWKT(final double flatness) {
        return geometry.asText();
    }

    /**
     * View SIS Geometry as a JTS Geometry.
     * Only the matching JTS geometry types are supported.
     * The created geometry references the original geometry DataPoints, so modifications
     * are forwarded to the original but all metadata change, like the CRS, will not be preserved if changed
     * after the JTS view has been made.
     *
     * @param gf optional creation factory.
     * @return JTS geometry view of the given geometry
     */
    public org.locationtech.jts.geom.Geometry asJTS(org.locationtech.jts.geom.GeometryFactory gf) {
        return asJTS(geometry, gf);
    }

    /**
     * View SIS Geometry as a JTS Geometry.
     * Only the matching JTS geometry types are supported.
     * The created geometry references the original geometry DataPoints, so modifications
     * are forwarded to the original but all metadata change, like the CRS, will not be preserved if changed
     * after the JTS view has been made.
     *
     * @param geometry to convert
     * @param gf optional creation factory.
     * @return JTS geometry view of the given geometry
     */
    public static org.locationtech.jts.geom.Geometry asJTS(Geometry geometry, org.locationtech.jts.geom.GeometryFactory gf) {
        if (gf == null) gf = new org.locationtech.jts.geom.GeometryFactory();
        return JTSAdapter.asJTS(geometry, false, gf);
    }

    private Geometry fromJTS(final org.locationtech.jts.geom.Geometry result) {
        result.setUserData(crs);
        return JTSAdapter.fromJTS(result, true);
    }
}
