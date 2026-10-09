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

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.curve.Circle;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.Clothoid;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.curve.MultiCurve;
import org.apache.sis.geometries.curve.MultiLineString;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.internal.shared.DefaultPoint;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.MultiPolygon;
import org.apache.sis.geometries.surface.MultiSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.geometries.surface.TIN;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.geometry.wrapper.Capability;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.Classes;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;


/**
 * SIS Geometry factory wrapper.
 * Allows to do common operations on any geometry library.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SIS extends org.apache.sis.geometry.wrapper.Geometries<Geometry> {

    public static SIS INSTANCE = new SIS();

    /**
     * The geometry classes tested by {@link #getGeometryType(Class)}, in the order they are tested.
     * A specialized class is listed before the class it specializes, so that the most specific type
     * is the one which is reported. This is the converse of {@link #getGeometryClass(GeometryType)},
     * therefore both lists shall stay in sync.
     *
     * @see #TYPES
     */
    private static final Class<?>[] CLASSES = {
        Point.class,
        MultiPoint.class,
        Circle.class,
        CircularString.class,
        Clothoid.class,
        CompoundCurve.class,
        LineString.class,
        MultiLineString.class,
        MultiCurve.class,
        Triangle.class,
        Polygon.class,
        CurvePolygon.class,
        TIN.class,
        PolyhedralSurface.class,
        MultiPolygon.class,
        MultiSurface.class,
        GeometryCollection.class,
        Curve.class,
        Surface.class,
        Geometry.class
    };

    /**
     * The implementation-neutral types of the classes listed in the {@link #CLASSES} array.
     */
    private static final GeometryType[] TYPES = {
        GeometryType.POINT,
        GeometryType.MULTIPOINT,
        GeometryType.CIRCLE,
        GeometryType.CIRCULARSTRING,
        GeometryType.CLOTHOID,
        GeometryType.COMPOUNDCURVE,
        GeometryType.LINESTRING,
        GeometryType.MULTILINESTRING,
        GeometryType.MULTICURVE,
        GeometryType.TRIANGLE,
        GeometryType.POLYGON,
        GeometryType.CURVEPOLYGON,
        GeometryType.TIN,
        GeometryType.POLYHEDRALSURFACE,
        GeometryType.MULTIPOLYGON,
        GeometryType.MULTISURFACE,
        GeometryType.GEOMETRYCOLLECTION,
        GeometryType.CURVE,
        GeometryType.SURFACE,
        GeometryType.GEOMETRY
    };

    private SIS(){
        super(GeometryLibrary.SIS, Geometry.class, Point.class);
    }

    @Override
    public Class<?> getGeometryClass(GeometryType type) {
        switch (type) {
            case CIRCLE : return Circle.class;
            case CIRCULARSTRING : return CircularString.class;
            case CLOTHOID : return Clothoid.class;
            case COMPOUNDCURVE : return CompoundCurve.class;
            case CURVE : return Curve.class;
            case CURVEPOLYGON : return CurvePolygon.class;
            case GEOMETRY : return Geometry.class;
            case GEOMETRYCOLLECTION : return GeometryCollection.class;
            case LINESTRING : return LineString.class;
            case MULTICURVE : return MultiCurve.class;
            case MULTILINESTRING : return MultiLineString.class;
            case MULTIPOINT : return MultiPoint.class;
            case MULTIPOLYGON : return MultiPolygon.class;
            case MULTISURFACE : return MultiSurface.class;
            case POINT : return Point.class;
            case POLYGON : return Polygon.class;
            case POLYHEDRALSURFACE : return PolyhedralSurface.class;
            case SURFACE : return Surface.class;
            case TIN : return TIN.class;
            case TRIANGLE : return Triangle.class;
            //todo
            case BREPSOLID :
            case COMPOUNDSURFACE :
            case ELLIPTICALCURVE :
            case GEODESICSTRING :
            case NURBSCURVE :
            case SPIRALCURVE :
            default: return Geometry.class;
        }
    }

    /**
     * Returns the implementation-neutral type of the given geometry class. If the given class is an
     * array, then the collection type containing that kind of geometry is returned when there is one.
     *
     * @param  type  class of geometry for which the implementation-neutral type is desired.
     * @return implementation-neutral type of the given class, or {@link GeometryType#GEOMETRY} if unknown.
     */
    @Override
    public GeometryType getGeometryType(final Class<?> type) {
        Class<?> component = type.getComponentType();
        final boolean isArray = (component != null);
        if (!isArray) {
            component = type;
        }
        for (int i=0; i < CLASSES.length; i++) {
            if (CLASSES[i].isAssignableFrom(component)) {
                final GeometryType found = TYPES[i];
                if (isArray && !found.isCollection) {
                    final GeometryType collection = found.collection();
                    if (collection != null) {
                        return collection;
                    }
                }
                return found;
            }
        }
        return GeometryType.GEOMETRY;
    }

    @Override
    public GeometryWrapper castOrWrap(Object geometry) {
        if (geometry == null || geometry instanceof Wrapper) return (Wrapper) geometry;
        return new Wrapper((Geometry) geometry);
    }

    /**
     * Parses the given Well-Known Text. The text may be in the {@code EWKT} flavor, in which case
     * the coordinate reference system it names is used. Otherwise an
     * {@linkplain org.apache.sis.geometries.Geometries#getUndefinedCRS(int) undefined} system of
     * as many dimensions as the text has ordinates per position is assigned to the geometry.
     *
     * @param  wkt  the Well-Known Text to parse.
     * @return the geometry described by the given text.
     * @throws IllegalArgumentException if the text is malformed or names an unsupported geometry type.
     */
    @Override
    public GeometryWrapper parseWKT(final String wkt) {
        return new Wrapper(new WellKnownText().decode(wkt));
    }

    /**
     * Reads the given bytes as a Well-Known Binary encoded geometry. The bytes may be in the
     * {@code EWKB} flavor, in which case the coordinate reference system they name is used.
     * This implementation does not change the buffer position when the buffer is backed by
     * an accessible array.
     *
     * @param  data  the sequence of bytes to parse.
     * @return the geometry described by the given bytes.
     * @throws IllegalArgumentException if the bytes are malformed or name an unsupported geometry type.
     */
    @Override
    public GeometryWrapper parseWKB(final ByteBuffer data) {
        byte[] array;
        if (data.hasArray()) {
            /*
             * Try to use the underlying array without copy if possible.
             * Copy only if the position or length does not match.
             */
            array = data.array();
            int lower = data.arrayOffset();
            int upper = data.limit() + lower;
            lower += data.position();
            if (lower != 0 || upper != array.length) {
                array = Arrays.copyOfRange(array, lower, upper);
            }
        } else {
            array = new byte[data.remaining()];
            data.get(array);
        }
        return new Wrapper(new WellKnownBinary().decode(array));
    }

    @Override
    public boolean supports(Capability feature) {
        switch (feature) {
            case Z_COORDINATE : return true;
            case M_COORDINATE : return true;
            case SINGLE_PRECISION : return true;
            default: return false;
        }
    }

    @Override
    public Point createPoint(double x, double y) {
        return new DefaultPoint(undefinedSystem(BIDIMENSIONAL), x, y);
    }

    @Override
    public Point createPoint(double x, double y, double z) {
        return new DefaultPoint(undefinedSystem(TRIDIMENSIONAL), x ,y, z);
    }

    @Override
    public Point createPoint(boolean isFloat, Dimensions dimensions, DoubleBuffer coordinates) {
        return new DefaultPoint(readDataPoints(isFloat, dimensions, 1, coordinates));
    }

    /**
     * Creates a collection of points from the given coordinate values.
     * The buffer position is advanced by {@code dimensions.count} × the number of points.
     *
     * @param  isFloat      whether to cast and store numbers to single-precision.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     * @return the collection of points for the given coordinate values.
     */
    @Override
    public MultiPoint<?> createMultiPoint(final boolean isFloat, final Dimensions dimensions, final DoubleBuffer coordinates) {
        final int count = coordinates.remaining() / dimensions.count;
        return GeometryFactory.DEFAULT.createMultiPoint(readDataPoints(isFloat, dimensions, count, coordinates));
    }

    /**
     * Creates a polyline or a polygon from the given coordinate values. Each coordinate tuple having
     * a {@link Double#NaN} <var>x</var> or <var>y</var> coordinate starts a new path, and a path of
     * less than two positions is dropped. If a single path remains, it is returned directly instead
     * of being wrapped in a collection.
     *
     * @param  polygon      whether to return the paths as polygons instead of polylines.
     * @param  isFloat      whether to cast and store numbers to single-precision.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     * @return the geometry for the given coordinate values.
     * @throws IllegalArgumentException if a polygon was requested but a path is not a closed ring.
     */
    @Override
    public Geometry createPolyline(final boolean polygon, final boolean isFloat,
                                   final Dimensions dimensions, final DoubleBuffer... coordinates)
    {
        final int spatial = spatialDimension(dimensions);
        final var paths = new ArrayList<Geometry>();
        final var path  = new ArrayList<double[]>();
        for (final DoubleBuffer buffer : coordinates) {
            if (buffer == null) {
                continue;
            }
            while (buffer.remaining() >= dimensions.count) {
                final var tuple = new double[dimensions.count];
                buffer.get(tuple);
                if (Double.isNaN(tuple[0]) || Double.isNaN(tuple[1])) {
                    addPath(paths, path, polygon, isFloat, dimensions, spatial);
                } else {
                    path.add(tuple);
                }
            }
        }
        addPath(paths, path, polygon, isFloat, dimensions, spatial);
        switch (paths.size()) {
            case 0:  return GeometryFactory.DEFAULT.createEmpty(undefinedCRS(spatial));
            case 1:  return paths.get(0);
            default: {
                // An ArrayStoreException here would be a bug in our use of the `polygon` flag.
                return polygon ? GeometryFactory.DEFAULT.createMultiPolygon(paths.toArray(Polygon[]::new))
                               : GeometryFactory.DEFAULT.createMultiLineString(paths.toArray(LineString[]::new));
            }
        }
    }

    /**
     * Makes a line string or a polygon from the given positions, adds it to the given list, then
     * clears the positions. Less than two positions cannot make a path, in which case they are
     * dropped silently, as the libraries wrapped by the other implementations do.
     *
     * @param  addTo       where to add the created geometry, if such geometry is created.
     * @param  path        positions of the geometry to create. This list is cleared by this method.
     * @param  polygon     whether to create a polygon instead of a line string.
     * @param  isFloat     whether to store the coordinates as single-precision numbers.
     * @param  dimensions  the dimensions of the coordinate tuples.
     * @param  spatial     the number of spatial dimensions of the coordinate tuples.
     * @throws IllegalArgumentException if a polygon is requested but the path is not a closed ring.
     */
    private static void addPath(final List<Geometry> addTo, final List<double[]> path, final boolean polygon,
                                final boolean isFloat, final Dimensions dimensions, final int spatial)
    {
        final int size = path.size();
        if (size >= 2) {
            if (polygon && !Arrays.equals(path.get(0), 0, spatial, path.get(size-1), 0, spatial)) {
                throw new IllegalArgumentException("Coordinates of a polygon shall make a closed ring.");
            }
            final DataPoints points = toDataPoints(path, isFloat, dimensions, spatial);
            addTo.add(polygon ? GeometryFactory.DEFAULT.createPolygon(GeometryFactory.DEFAULT.createLinearRing(points), List.of())
                              : GeometryFactory.DEFAULT.createLineString(points));
        }
        path.clear();
    }

    /**
     * Creates a multi-polygon from an array of polygons, rings or line strings.
     * The line strings which are not already rings are converted to rings.
     *
     * @param  geometries  the polygons, rings or line strings to put in a multi-polygon.
     * @return the multi-polygon.
     * @throws ClassCastException if an element of the array is neither a polygon nor a curve.
     */
    @Override
    public GeometryWrapper createMultiPolygon(final Object[] geometries) {
        final var polygons = new Polygon[geometries.length];
        for (int i=0; i < geometries.length; i++) {
            final Object component = implementation(geometries[i]);
            if (component instanceof Polygon polygon) {
                polygons[i] = polygon;
            } else if (component instanceof LineString line) {
                polygons[i] = GeometryFactory.DEFAULT.createPolygon(toRing(line), List.of());
            } else {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_3,
                        Strings.bracket("geometries", i), Polygon.class, Classes.getClass(component)));
            }
        }
        return new Wrapper(GeometryFactory.DEFAULT.createMultiPolygon(polygons));
    }

    /**
     * Creates a geometry from components.
     * The expected {@code components} type depends on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components shall be a {@link Point}[],
     *       {@link Geometry}[], {@link LineString}[] or {@link Polygon}[] array, depending on the
     *       desired target type.</li>
     *   <li>Otherwise, if {@code type} is {@link GeometryType#POLYGON}, then the components shall be
     *       a {@link LineString}[] with the first ring taken as the shell and all other rings as holes.</li>
     *   <li>Otherwise, the components shall be a {@link DataPoints} sequence, an {@link Array} of
     *       positions, or an array or collection of {@link Point} or {@link Vector} instances.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws IllegalArgumentException if the given geometry type is not supported.
     * @throws ArrayStoreException if {@code components} is an array with invalid component type.
     */
    @Override
    public GeometryWrapper createFromComponents(final GeometryType type, final Object components) {
        final Geometry geometry;
        switch (type) {
            case POINT: {
                final DataPoints points = toDataPoints(components);
                geometry = (points.size() == 1) ? GeometryFactory.DEFAULT.createPoint(points)
                                                : GeometryFactory.DEFAULT.createMultiPoint(points).getCentroid();
                break;
            }
            case LINESTRING: {
                geometry = GeometryFactory.DEFAULT.createLineString(toDataPoints(components));
                break;
            }
            case POLYGON: {
                if (components instanceof LineString[] rings) {
                    if (rings.length == 0) {
                        // A polygon is defined by its exterior ring, which there is none to take here.
                        throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "components"));
                    }
                    LinearRing shell = null;
                    final var holes = new ArrayList<LinearRing>(rings.length - 1);
                    for (int i=0; i < rings.length; i++) {
                        final LinearRing ring = toRing(rings[i]);
                        if (i == 0) shell = ring;
                        else holes.add(ring);
                    }
                    geometry = GeometryFactory.DEFAULT.createPolygon(shell, holes);
                } else {
                    geometry = GeometryFactory.DEFAULT.createPolygon(
                            GeometryFactory.DEFAULT.createLinearRing(toDataPoints(components)), List.of());
                }
                break;
            }
            case MULTIPOINT: {
                geometry = (components instanceof Point[] points)
                        ? GeometryFactory.DEFAULT.createMultiPoint(points)
                        : GeometryFactory.DEFAULT.createMultiPoint(toDataPoints(components));
                break;
            }
            case MULTILINESTRING: {
                geometry = GeometryFactory.DEFAULT.createMultiLineString((LineString[]) components);
                break;
            }
            case MULTIPOLYGON: {
                geometry = GeometryFactory.DEFAULT.createMultiPolygon((Polygon[]) components);
                break;
            }
            case GEOMETRYCOLLECTION: {
                geometry = GeometryFactory.DEFAULT.createGeometryCollection((Geometry[]) components);
                break;
            }
            case GEOMETRY: {
                return createFromComponents(components);
            }
            default: {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, type));
            }
        }
        return new Wrapper(geometry);
    }

    @Override
    protected GeometryWrapper createWrapper(Geometry geometry) {
        return new Wrapper(geometry);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Helper methods /////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Returns the number of dimensions of the <em>positions</em> of tuples having the given dimensions.
     * The <var>m</var> coordinate is not one of them: it is stored as the
     * {@value DataPointsType#ATT_M} attribute of the positions instead of as a coordinate.
     */
    private static int spatialDimension(final Dimensions dimensions) {
        return dimensions.hasZ ? TRIDIMENSIONAL : BIDIMENSIONAL;
    }

    /**
     * Returns the coordinate reference system to assign to coordinates given without one.
     * The returned system cannot be converted to any other system, which makes the absence
     * of a real system explicit instead of silently accepting an arbitrary one.
     */
    private static CoordinateReferenceSystem undefinedCRS(final int dimension) {
        return org.apache.sis.geometries.Geometries.getUndefinedCRS(dimension);
    }

    /**
     * Returns the sample system to assign to positions given without a coordinate reference system.
     */
    private static SampleSystem undefinedSystem(final int dimension) {
        return SampleSystem.of(undefinedCRS(dimension));
    }

    /**
     * Reads the given number of coordinate tuples from the given buffer as a sequence of positions.
     * The buffer position is advanced by {@code dimensions.count} × {@code count}.
     *
     * @param  isFloat      whether to store the coordinates as single-precision numbers.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  count        the number of coordinate tuples to read.
     * @param  coordinates  the buffer to read.
     * @return the positions read from the given buffer.
     */
    private static ArrayDataPoints readDataPoints(final boolean isFloat, final Dimensions dimensions,
                                                  final int count, final DoubleBuffer coordinates)
    {
        final int spatial = spatialDimension(dimensions);
        final var positions = new double[Math.multiplyExact(count, spatial)];
        final double[] measures = dimensions.hasM ? new double[count] : null;
        for (int i=0, p=0; i<count; i++) {
            for (int d=0; d<spatial; d++) {
                positions[p++] = coordinates.get();
            }
            if (measures != null) {
                measures[i] = coordinates.get();
            }
        }
        return toDataPoints(isFloat, spatial, positions, measures);
    }

    /**
     * Returns the given coordinate tuples as a sequence of positions.
     *
     * @param  tuples      the coordinate tuples, each of {@code dimensions.count} values.
     * @param  isFloat     whether to store the coordinates as single-precision numbers.
     * @param  dimensions  the dimensions of the coordinate tuples.
     * @param  spatial     the number of spatial dimensions of the coordinate tuples.
     * @return the positions of the given coordinate tuples.
     */
    private static ArrayDataPoints toDataPoints(final List<double[]> tuples, final boolean isFloat,
                                                final Dimensions dimensions, final int spatial)
    {
        final int count = tuples.size();
        final var positions = new double[Math.multiplyExact(count, spatial)];
        final double[] measures = dimensions.hasM ? new double[count] : null;
        for (int i=0, p=0; i<count; i++, p+=spatial) {
            final double[] tuple = tuples.get(i);
            System.arraycopy(tuple, 0, positions, p, spatial);
            if (measures != null) {
                measures[i] = tuple[spatial];
            }
        }
        return toDataPoints(isFloat, spatial, positions, measures);
    }

    /**
     * Returns the given coordinates as a sequence of positions carrying the measures, if any,
     * as their {@value DataPointsType#ATT_M} attribute.
     *
     * @param  isFloat    whether to store the values as single-precision numbers.
     * @param  spatial    the number of spatial dimensions of the positions.
     * @param  positions  the spatial coordinates, as {@code spatial} values per position.
     * @param  measures   one measure per position, or {@code null} if the positions have no measure.
     * @return the sequence of the given positions.
     */
    private static ArrayDataPoints toDataPoints(final boolean isFloat, final int spatial,
                                                final double[] positions, final double[] measures)
    {
        final SampleSystem ss = undefinedSystem(spatial);
        final var points = new ArrayDataPoints(isFloat ? NDArrays.of(ss, toFloat(positions))
                                                       : NDArrays.of(ss, positions));
        if (measures != null) {
            final SampleSystem ms = SampleSystem.ofSize(1);
            points.setAttribute(DataPointsType.ATT_M, isFloat ? NDArrays.of(ms, toFloat(measures))
                                                              : NDArrays.of(ms, measures));
        }
        return points;
    }

    /**
     * Returns the given values cast to single-precision.
     */
    private static float[] toFloat(final double[] values) {
        final var copy = new float[values.length];
        for (int i=0; i<values.length; i++) {
            copy[i] = (float) values[i];
        }
        return copy;
    }

    /**
     * Returns the given components as a sequence of positions. The components can be a
     * {@link DataPoints} sequence, an {@link Array} of positions, or an array or collection
     * of {@link Point} or {@link Vector} instances.
     *
     * @param  components  the components to read.
     * @return the positions of the given components.
     * @throws ClassCastException if the components are not of one of the expected kinds.
     */
    private static DataPoints toDataPoints(final Object components) {
        if (components instanceof DataPoints points) {
            return points;
        }
        if (components instanceof Array positions) {
            return GeometryFactory.DEFAULT.createDataPoints(positions);
        }
        // The ClassCastException that may happen here is part of method contract.
        final Collection<?> source = (components instanceof Collection<?> c) ? c : Arrays.asList((Object[]) components);
        final var tuples = new ArrayList<Vector<?>>(source.size());
        for (final Object element : source) {
            final Object component = implementation(element);
            // The ClassCastException that may happen here is part of method contract.
            tuples.add((component instanceof Point point) ? point.getPosition() : (Vector<?>) component);
        }
        if (tuples.isEmpty()) {
            /*
             * No position to take a sample system from. The number of dimensions is unknown,
             * so the two-dimensional case is assumed, as everywhere else in the absence of
             * a coordinate reference system.
             */
            return GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(undefinedSystem(BIDIMENSIONAL), DataType.DOUBLE, 0));
        }
        final Vector<?> first = tuples.get(0);
        return GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(tuples, first.getSampleSystem(), first.getDataType()));
    }

    /**
     * Returns the given curve as a ring, converting it if it is not already one.
     * The ring shares the positions of the given curve, no copy is performed.
     */
    private static LinearRing toRing(final LineString line) {
        return (line instanceof LinearRing ring) ? ring : GeometryFactory.DEFAULT.createLinearRing(line.getDataPoints());
    }
}
