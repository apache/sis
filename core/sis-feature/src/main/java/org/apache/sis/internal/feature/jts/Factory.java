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
 * @version 1.1
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
     * The factory to use for creating JTS geometries. Currently set to a factory using
     * double-precision floating point numbers and a spatial-reference ID of 0.
     */
    private final transient GeometryFactory factory;

    /**
     * Creates the singleton instance.
     */
    private Factory() {
        super(GeometryLibrary.JTS, Geometry.class, Point.class, LineString.class, Polygon.class);
        factory = new GeometryFactory();            // Default to double precision and SRID of 0.
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
     * Creates a two-dimensional point from the given coordinates.
     *
     * @return the point for the given coordinate values.
     */
    @Override
    public Object createPoint(final double x, final double y) {
        return factory.createPoint(new Coordinate(x, y));
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
     * @param  polygon      whether to return the path as a polygon instead than polyline.
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
                            c = new Coordinate(x, y);
                        }
                        coordList.add(c);
                    } else {
                        if (is3D) i++;
                        toLineString(coordList, lines, polygon);
                        coordList.clear();
                    }
                }
            }
        }
        toLineString(coordList, lines, polygon);
        return toGeometry(lines, polygon);
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
        for (int i=0; i<geometries.length; i++) {
            final Object polyline = unwrap(geometries[i]);
            final Polygon polygon;
            if (polyline instanceof Polygon) {
                polygon = (Polygon) polyline;
            } else if (polyline instanceof LinearRing) {
                polygon = factory.createPolygon((LinearRing) polyline);
                JTS.copyMetadata((Geometry) polyline, polygon);
            } else if (polyline instanceof LineString) {
                // Let JTS throws an exception with its own error message if the ring is not valid.
                polygon = factory.createPolygon(((LineString) polyline).getCoordinateSequence());
                JTS.copyMetadata((Geometry) polyline, polygon);
            } else {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalArgumentClass_3,
                        Strings.bracket("geometries", i), Polygon.class, Classes.getClass(polyline)));
            }
            polygons[i] = polygon;
        }
        return new Wrapper(factory.createMultiPolygon(polygons));
    }

    /**
     * Makes a line string or linear ring from the given coordinates, and adds the line string to the given list.
     * If the {@code polygon} argument is {@code true}, then this method creates polygons instead of line strings.
     * If the given coordinates array is empty, then this method does nothing.
     * This method does not modify the given coordinates list.
     */
    final void toLineString(final List<Coordinate> coordinates, final List<Geometry> addTo, final boolean polygon) {
        final int s = coordinates.size();
        if (s >= 2) {
            final Coordinate[] ca = coordinates.toArray(new Coordinate[s]);
            final Geometry geom;
            if (polygon) {
                geom = factory.createPolygon(ca);
            } else if (ca.length > 3 && ca[0].equals2D(ca[s-1])) {
                geom = factory.createLinearRing(ca);
            } else {
                geom = factory.createLineString(ca);        // Throws an exception if contains duplicated point.
            }
            addTo.add(geom);
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
    final Geometry toGeometry(final List<Geometry> lines, final boolean polygon) {
        final int s = lines.size();
        switch (s) {
            case 0:  {
                // Create an empty polygon or linear ring.
                return polygon ? factory.createPolygon   ((Coordinate[]) null)
                               : factory.createLinearRing((Coordinate[]) null);
            }
            case 1: {
                return lines.get(0);
            }
            default: {
                // An ArrayStoreException here would be a bug in our use of `polygon` boolean.
                return polygon ? factory.createMultiPolygon   (lines.toArray(new Polygon   [s]))
                               : factory.createMultiLineString(lines.toArray(new LineString[s]));
            }
        }
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
            // The ClassCastException that may happen here is part of method contract.
            case GEOMETRY_COLLECTION: geometry = factory.createGeometryCollection((Geometry[]) components); break;
            case MULTI_LINESTRING:    geometry = factory.createMultiLineString((LineString[]) components); break;
            case MULTI_POLYGON:       geometry = factory.createMultiPolygon((Polygon[]) components); break;
            case MULTI_POINT: {
                if (components instanceof Point[]) {
                    geometry = factory.createMultiPoint((Point[]) components);
                    break;
                }
                // Else fallthrough
            }
            default: {
                final CoordinateSequence cs;
                if (components instanceof CoordinateSequence) {
                    cs = (CoordinateSequence) components;
                } else {
                    final Coordinate[] coordinates;
                    if (components instanceof Coordinate[]) {
                        coordinates = (Coordinate[]) components;
                    } else {
                        // The ClassCastException that may happen here is part of method contract.
                        final Collection<?> source = (components instanceof Collection<?>)
                                ? (Collection<?>) components : Arrays.asList((Object[]) components);
                        coordinates = new Coordinate[source.size()];
                        int n = 0;
                        for (final Object obj : source) {
                            // The ClassCastException that may happen here is part of method contract.
                            coordinates[n++] = (obj instanceof Point) ? ((Point) obj).getCoordinate() : (Coordinate) obj;
                        }
                    }
                    cs = factory.getCoordinateSequenceFactory().create(coordinates);
                }
                switch (type) {
                    case GEOMETRY:    // Default to multi-points for now.
                    case MULTI_POINT: geometry = factory.createMultiPoint(cs); break;
                    case LINESTRING:  geometry = factory.createLineString(cs); break;
                    case POLYGON:     geometry = factory.createPolygon(cs); break;
                    case POINT:       geometry = factory.createMultiPoint(cs).getCentroid(); break;
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
        return new Wrapper(new WKTReader(factory).read(wkt));
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
