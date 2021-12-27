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
package org.apache.sis.internal.feature.jts;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.nio.ByteBuffer;
import java.io.ObjectStreamException;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryType;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.math.Vector;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;

// Optional dependencies
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;


/**
 * The factory of geometry objects backed by Java Topology Suite (JTS).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.2
 * @since   0.7
 * @module
 */
public final class Factory extends Geometries<Geometry> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3457343016410620076L;

    /**
     * The singleton instance of this factory.
     *
     * @see #factory(boolean)
     */
    public static final Factory INSTANCE = new Factory();

    /**
     * Invoked at deserialization time for obtaining the unique instance of this {@code Geometries} class.
     *
     * @return {@link #INSTANCE}.
     */
    @Override
    protected Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

    /**
     * The factory to use for creating JTS geometries. Set to a factory using double-precision ({@code factory})
     * or single-precision ({@code fctry32}) floating point numbers and a spatial-reference ID of 0.
     *
     * <p>Not serialized because {@link #readResolve()} will replace by {@link #INSTANCE}.</p>
     *
     * @see #factory(boolean)
     */
    private final transient GeometryFactory factory, fctry32;

    /**
     * Creates the singleton instance.
     */
    private Factory() {
        super(GeometryLibrary.JTS, Geometry.class, Point.class, LineString.class, Polygon.class);
        factory = new GeometryFactory(new PackedCoordinateSequenceFactory(true));
        fctry32 = new GeometryFactory(new PackedCoordinateSequenceFactory(false));
    }

    /**
     * Returns the geometry class of the given instance.
     *
     * @param  type  type of geometry for which the class is desired.
     * @return implementation class for the geometry of the specified type.
     */
    @Override
    public Class<?> getGeometryClass(final GeometryType type) {
        switch (type) {
            default:               return rootClass;
            case POINT:            return pointClass;
            case LINESTRING:       return polylineClass;
            case POLYGON:          return polygonClass;
            case MULTI_POINT:      return MultiPoint.class;
            case MULTI_LINESTRING: return MultiLineString.class;
            case MULTI_POLYGON:    return MultiPolygon.class;
        }
    }

    /**
     * Returns a wrapper for the given {@code <G>} or {@code GeometryWrapper<G>} geometry.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or {@code null}.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     */
    @Override
    public GeometryWrapper<Geometry> castOrWrap(final Object geometry) {
        return (geometry == null || geometry instanceof Wrapper)
                ? (Wrapper) geometry : new Wrapper((Geometry) geometry);
    }

    /**
     * Creates a wrapper for the given geometry instance.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     */
    @Override
    protected GeometryWrapper<Geometry> createWrapper(final Geometry geometry) {
        return new Wrapper(geometry);
    }

    /**
     * Returns {@code true} if the given sequence stores coordinates as single-precision floating point values.
     */
    static boolean isFloat(final CoordinateSequence cs) {
        return (cs instanceof PackedCoordinateSequence.Float) ||
               (cs instanceof org.locationtech.jts.geom.impl.PackedCoordinateSequence.Float);
    }

    /**
     * Returns {@code true} if {@code previous} is {@code true} and the given point uses single-precision.
     * This is a convenience method for less distraction in code using it.
     *
     * @param  previous  previous value of {@code isFloat} boolean.
     * @param  geometry  the geometry to combine with previous value.
     * @return new value of {@code isFloat} boolean.
     */
    static boolean isFloat(final boolean previous, final Point geometry) {
        return previous && isFloat(geometry.getCoordinateSequence());
    }

    /**
     * Returns the geometry factory to use for the given precision.
     *
     * @param  isFloat  {@code true} for single-precision, or {@code false} for double-precision (default).
     * @return the JTS geometry factory for the given precision.
     *
     * @see #INSTANCE
     */
    public final GeometryFactory factory(final boolean isFloat) {
        return isFloat ? fctry32 : factory;
    }

    /**
     * Single-precision variant of {@link #createPoint(double, double)}.
     *
     * @return the point for the given coordinate values.
     */
    @Override
    public Object createPoint(final float x, final float y) {
        return fctry32.createPoint(new CoordinateXY(x, y));
    }

    /**
     * Creates a two-dimensional point from the given coordinates.
     *
     * @return the point for the given coordinate values.
     */
    @Override
    public Object createPoint(final double x, final double y) {
        return factory.createPoint(new CoordinateXY(x, y));
    }

    /**
     * Creates a three-dimensional point from the given coordinates.
     *
     * @return the point for the given coordinate values.
     */
    @Override
    public Object createPoint(final double x, final double y, final double z) {
        return factory.createPoint(new Coordinate(x, y, z));
    }

    /**
     * Creates a polyline from the given coordinate values.
     * Each {@link Double#NaN} coordinate value starts a new path.
     *
     * @param  polygon      whether to return the path as a polygon instead of polyline.
     * @param  dimension    the number of dimensions ({@value #BIDIMENSIONAL} or {@value #TRIDIMENSIONAL}).
     * @param  coordinates  sequence of (x,y) or (x,y,z) tuples.
     * @return the geometric object for the given points.
     * @throws UnsupportedOperationException if this operation is not implemented for the given number of dimensions.
     */
    @Override
    public Geometry createPolyline(final boolean polygon, final int dimension, final Vector... coordinates) {
        final boolean is3D = (dimension == TRIDIMENSIONAL);
        if (!is3D && dimension != BIDIMENSIONAL) {
            throw new UnsupportedOperationException(unsupported(dimension));
        }
        boolean isFloat = true;
        for (final Vector v : coordinates) {
            if (v != null && !v.isSinglePrecision()) {
                isFloat = false;
                break;
            }
        }
        final List<Coordinate> coordList = new ArrayList<>(32);
        final List<Geometry> lines = new ArrayList<>();
        for (final Vector v : coordinates) {
            if (v != null) {
                final int size = v.size();
                for (int i=0; i<size;) {
                    final double x = v.doubleValue(i++);
                    final double y = v.doubleValue(i++);
                    if (!Double.isNaN(x) && !Double.isNaN(y)) {
                        final Coordinate c;
                        if (is3D) {
                            c = new Coordinate(x, y, v.doubleValue(i++));
                        } else {
                            c = new CoordinateXY(x, y);
                        }
                        coordList.add(c);
                    } else {
                        if (is3D) i++;
                        toLineString(coordList, lines, polygon, isFloat);
                        coordList.clear();
                    }
                }
            }
        }
        toLineString(coordList, lines, polygon, isFloat);
        return toGeometry(lines, polygon, isFloat);
    }

    /**
     * Creates a multi-polygon from an array of JTS {@link Polygon} or {@link LinearRing}.
     * If some geometries are actually linear rings, they will be converted to polygons.
     *
     * @param  geometries  the polygons or linear rings to put in a multi-polygons.
     * @throws ClassCastException if an element in the array is not a JTS geometry.
     * @throws IllegalArgumentException if an element is a non-closed linear string.
     */
    @Override
    public GeometryWrapper<Geometry> createMultiPolygon(final Object[] geometries) {
        final Polygon[] polygons = new Polygon[geometries.length];
        boolean isFloat = true;
        for (int i=0; i < geometries.length; i++) {
            final Object polyline = unwrap(geometries[i]);
            final Polygon polygon;
            if (polyline instanceof Polygon) {
                polygon = (Polygon) polyline;
            } else {
                final boolean fs;
                final CoordinateSequence cs;
                if (polyline instanceof LinearRing) {
                    final LinearRing ring = (LinearRing) polyline;
                    cs      = ring.getCoordinateSequence();
                    fs      = isFloat(cs);
                    polygon = factory(fs).createPolygon(ring);
                } else if (polyline instanceof LineString) {
                    // Let JTS throws an exception with its own error message if the ring is not valid.
                    cs      = ((LineString) polyline).getCoordinateSequence();
                    fs      = isFloat(cs);
                    polygon = factory(fs).createPolygon(cs);
                } else {
                    throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_3,
                            Strings.bracket("geometries", i), Polygon.class, Classes.getClass(polyline)));
                }
                JTS.copyMetadata((Geometry) polyline, polygon);
                isFloat &= fs;
            }
            polygons[i] = polygon;
        }
        return new Wrapper(factory(isFloat).createMultiPolygon(polygons));
    }

    /**
     * Makes a line string or linear ring from the given coordinates, and adds the line string to the given list.
     * If the {@code polygon} argument is {@code true}, then this method creates polygons instead of line strings.
     * If the given coordinates list is empty, then this method does nothing.
     * This method does not modify the given coordinates list.
     */
    final void toLineString(final List<Coordinate> coordinates, final List<Geometry> addTo,
                            final boolean polygon, final boolean isFloat)
    {
        final int s = coordinates.size();
        if (s >= 2) {
            final Coordinate[] ca = coordinates.toArray(new Coordinate[s]);
            final GeometryFactory gf = factory(isFloat);
            final Geometry geometry;
            if (polygon) {
                geometry = gf.createPolygon(ca);
            } else if (ca.length > 3 && ca[0].equals2D(ca[s-1])) {
                geometry = gf.createLinearRing(ca);
            } else {
                geometry = gf.createLineString(ca);         // Throws an exception if contains duplicated point.
            }
            addTo.add(geometry);
        }
    }

    /**
     * Returns the given list of polygons or line strings as a single geometry.
     *
     * @param  lines  the polygons or lines strings.
     * @return {@code true} if the given list contains {@link Polygon} instances, or
     *         {@code false} if it contains {@link LineString} instances.
     * @throws ArrayStoreException if the geometries in the given list are not instances
     *         of the type specified by the {@code polygon} argument.
     */
    @SuppressWarnings("SuspiciousToArrayCall")      // Type controlled by `polygon`.
    final Geometry toGeometry(final List<Geometry> lines, final boolean polygon, final boolean isFloat) {
        final int s = lines.size();
        switch (s) {
            case 0:  {
                // Create an empty polygon or linear ring.
                final GeometryFactory gf = factory(isFloat);
                return polygon ? gf.createPolygon   ((Coordinate[]) null)
                               : gf.createLinearRing((Coordinate[]) null);
            }
            case 1: {
                return lines.get(0);
            }
            default: {
                // An ArrayStoreException here would be a bug in our use of `polygon` boolean.
                final GeometryFactory gf = factory(isFloat);
                return polygon ? gf.createMultiPolygon   (lines.toArray(new Polygon   [s]))
                               : gf.createMultiLineString(lines.toArray(new LineString[s]));
            }
        }
    }

    /**
     * Creates a geometry from an array of components, which may be single or double-precision.
     * If all components use single-precision, then the returned geometry will use single-precision too.
     *
     * <p>We have to use generic type because JTS does not provide an
     * {@code Geometry.getCoordinateSequence()} abstract method.</p>
     */
    private <G extends Geometry> Geometry createFromComponents(
            final G[] geometries,
            final Function<G,CoordinateSequence> csGetter,
            final BiFunction<GeometryFactory, G[], Geometry> builder)
    {
        boolean isFloat = true;
        for (final G geometry : geometries) {
            if (!isFloat(csGetter.apply(geometry))) {
                isFloat = false;
                break;
            }
        }
        return builder.apply(factory(isFloat), geometries);
    }

    /**
     * Creates a geometry from components.
     * The expected {@code components} type depend on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components shall be an array of {@link Point},
     *       {@link Geometry}, {@link LineString} or {@link Polygon} elements, depending on the desired
     *       target type.</li>
     *   <li>Otherwise the components shall be an array or collection of {@link Point} or {@link Coordinate}
     *       instances, or a JTS-specific {@link CoordinateSequence}.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws ClassCastException if the given object is not an array or a collection of supported geometry components.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public GeometryWrapper<Geometry> createFromComponents(final GeometryType type, final Object components) {
        final Geometry geometry;
        switch (type) {
            case GEOMETRY_COLLECTION: {
                // The ClassCastException that may happen here is part of method contract.
                geometry = factory.createGeometryCollection((Geometry[]) components);
                break;
            }
            case MULTI_LINESTRING: {
                // The ClassCastException that may happen here is part of method contract.
                geometry = createFromComponents((LineString[]) components,
                            LineString::getCoordinateSequence,
                            GeometryFactory::createMultiLineString);
                break;
            }
            case MULTI_POLYGON: {
                // The ClassCastException that may happen here is part of method contract.
                geometry = createFromComponents((Polygon[]) components,
                            (g) -> g.getExteriorRing().getCoordinateSequence(),
                            GeometryFactory::createMultiPolygon);
                break;
            }
            case MULTI_POINT: {
                if (components instanceof Point[]) {
                    geometry = createFromComponents((Point[]) components,
                                Point::getCoordinateSequence,
                                GeometryFactory::createMultiPoint);
                    break;
                }
                // Else fallthrough
            }
            default: {
                final GeometryFactory gf;
                final CoordinateSequence cs;
                if (components instanceof CoordinateSequence) {
                    cs = (CoordinateSequence) components;
                    gf = factory(isFloat(cs));
                } else {
                    final Coordinate[] coordinates;
                    if (components instanceof Coordinate[]) {
                        coordinates = (Coordinate[]) components;
                        gf = factory;
                    } else {
                        // The ClassCastException that may happen here is part of method contract.
                        final Collection<?> source = (components instanceof Collection<?>)
                                ? (Collection<?>) components : Arrays.asList((Object[]) components);
                        coordinates = new Coordinate[source.size()];
                        boolean isFloat = true;
                        int n = 0;
                        for (final Object obj : source) {
                            final Coordinate c;
                            if (obj instanceof Point) {
                                final Point p = (Point) obj;
                                isFloat = isFloat(isFloat, p);
                                c = p.getCoordinate();
                            } else {
                                // The ClassCastException that may happen here is part of method contract.
                                c = (Coordinate) obj;
                                isFloat = false;
                            }
                            coordinates[n++] = c;
                        }
                        gf = factory(isFloat);
                    }
                    cs = gf.getCoordinateSequenceFactory().create(coordinates);
                }
                switch (type) {
                    case GEOMETRY:    // Default to multi-points for now.
                    case MULTI_POINT: geometry = gf.createMultiPoint(cs); break;
                    case LINESTRING:  geometry = gf.createLineString(cs); break;
                    case POLYGON:     geometry = gf.createPolygon(cs); break;
                    case POINT:       geometry = gf.createMultiPoint(cs).getCentroid(); break;
                    default:          throw new AssertionError(type);
                }
            }
        }
        return new Wrapper(geometry);
    }

    /**
     * Parses the given Well Known Text (WKT).
     *
     * @param  wkt  the Well Known Text to parse.
     * @return the geometry object for the given WKT.
     * @throws ParseException if the WKT can not be parsed.
     */
    @Override
    public GeometryWrapper<Geometry> parseWKT(final String wkt) throws ParseException {
        // WKTReader(GeometryFactory) constructor is cheap.
        final WKTReader reader = new WKTReader(factory);
        reader.setIsOldJtsCoordinateSyntaxAllowed(false);
        return new Wrapper(reader.read(wkt));
    }

    /**
     * Reads the given Well Known Binary (WKB).
     * This implementation does not change the buffer position.
     *
     * @param  data  the sequence of bytes to parse.
     * @return the geometry object for the given WKB.
     * @throws ParseException if the WKB can not be parsed.
     */
    @Override
    public GeometryWrapper<Geometry> parseWKB(final ByteBuffer data) throws ParseException {
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
        // WKBReader(GeometryFactory) constructor is cheap.
        return new Wrapper(new WKBReader(factory).read(array));
    }
}
