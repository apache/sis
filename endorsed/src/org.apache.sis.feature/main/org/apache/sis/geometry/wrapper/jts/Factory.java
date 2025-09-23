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

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.io.ObjectStreamException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateXYM;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import org.opengis.util.FactoryException;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.wrapper.Capability;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.util.Classes;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * The factory of geometry objects backed by Java Topology Suite (JTS).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
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
     * Invoked at deserialization time for obtaining the unique instance of this {@code Factory} class.
     *
     * @return {@link #INSTANCE}.
     * @throws ObjectStreamException if the object state is invalid.
     */
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
        super(GeometryLibrary.JTS, Geometry.class, Point.class);
        factory = new GeometryFactory(new PackedCoordinateSequenceFactory(true));
        fctry32 = new GeometryFactory(new PackedCoordinateSequenceFactory(false));
    }

    /**
     * Returns the geometry object to return to the user in public API.
     *
     * @param  wrapper  the wrapper for which to get the geometry, or {@code null}.
     * @return the JTS geometry instance, or {@code null} if the given wrapper was null.
     * @throws ClassCastException if the given wrapper is not an instance of the class expected by this factory.
     */
    @Override
    public Object getGeometry(final GeometryWrapper wrapper) {
        if (wrapper instanceof Wrapper) {
            // Intentionally stronger cast than needed.
            return ((Wrapper) wrapper).implementation();
        } else {
            return super.getGeometry(wrapper);
        }
    }

    /**
     * Returns the geometry class of the given type.
     *
     * @param  type  type of geometry for which the class is desired.
     * @return implementation class for the geometry of the specified type.
     */
    @Override
    public Class<?> getGeometryClass(final GeometryType type) {
        return Wrapper.getGeometryClass(type);
    }

    /**
     * Returns the geometry type of the given implementation class.
     *
     * @param  type  class of geometry for which the type is desired.
     * @return implementation-neutral type for the geometry of the specified class.
     */
    @Override
    public GeometryType getGeometryType(Class<?> type) {
        return Wrapper.getGeometryType(type);
    }

    /**
     * Returns a wrapper for the given {@code <G>} or {@code GeometryWrapper} geometry.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or {@code null}.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     * @throws BackingStoreException if the <abbr>CRS</abbr> cannot be created from the <abbr>SRID</abbr> code.
     */
    @Override
    public GeometryWrapper castOrWrap(final Object geometry) {
        if (geometry == null || geometry instanceof Wrapper) {
            return (Wrapper) geometry;
        } else {
            return createWrapper((Geometry) geometry);
        }
    }

    /**
     * Creates a wrapper for the given geometry instance.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     * @throws BackingStoreException if the <abbr>CRS</abbr> cannot be created from the <abbr>SRID</abbr> code.
     */
    @Override
    protected GeometryWrapper createWrapper(final Geometry geometry) {
        try {
            return new Wrapper(geometry);
        } catch (FactoryException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Notifies that this library supports <var>z</var> and <var>m</var> coordinates.
     * Notifies that single-precision floating point type is also supported.
     */
    @Override
    public boolean supports(Capability feature) {
        return true;
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
     * Creates a single point from the given coordinates with the given dimensions.
     * Note that when the number of dimensions is known at compile-time, the other
     * {@code createPoint(…)} methods should be more efficient.
     *
     * @param  isFloat      whether single-precision instead of double-precision floating point numbers.
     * @param  dimensions   the dimensions of the coordinate tuple.
     * @param  coordinates  a (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuple.
     * @return the point for the given coordinate values.
     */
    @Override
    public Object createPoint(final boolean isFloat, final Dimensions dimensions, final DoubleBuffer coordinates) {
        return factory(isFloat).createPoint(createCoordinateTuple(dimensions, coordinates));
    }

    /**
     * Creates a coordinate tuple from the given coordinates with the given dimensions.
     *
     * @param  dimensions   the dimensions of the coordinate tuple.
     * @param  coordinates  a (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuple.
     * @return the single coordinate tuple for the given coordinate values.
     */
    private static Coordinate createCoordinateTuple(final Dimensions dimensions, final DoubleBuffer coordinates) {
        final double x = coordinates.get();
        final double y = coordinates.get();
        if (dimensions.hasZ) {
            final double z = coordinates.get();
            if (dimensions.hasM) {
                return new CoordinateXYZM(x, y, z, coordinates.get());
            } else {
                return new Coordinate(x, y, z);
            }
        } else if (dimensions.hasM) {
            return new CoordinateXYM(x, y, coordinates.get());
        } else {
            return new CoordinateXY(x, y);
        }
    }

    /**
     * Creates a collection of points from the given coordinate values.
     * The buffer position is advanced by {@code dimensions.count} × the number of points.
     *
     * @param  isFloat      whether single-precision instead of double-precision floating point numbers.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     * @return the collection of points for the given points.
     */
    @Override
    public Geometry createMultiPoint(final boolean isFloat, final Dimensions dimensions, final DoubleBuffer coordinates) {
        final var coordList = new Coordinate[coordinates.remaining() / dimensions.count];
        int n = 0;
        while (coordinates.hasRemaining()) {
            coordList[n++] = createCoordinateTuple(dimensions, coordinates);
        }
        return factory(isFloat).createMultiPointFromCoords(coordList);
    }

    /**
     * Creates a polyline from the given coordinate values.
     * Each {@link Double#NaN} coordinate value starts a new path.
     *
     * @param  polygon      whether to return the path as a polygon instead of polyline.
     * @param  isFloat      whether to cast and store numbers to single-precision.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     * @return the geometric object for the given points.
     */
    @Override
    public Geometry createPolyline(final boolean polygon, final boolean isFloat,
            final Dimensions dimensions, final DoubleBuffer... coordinates)
    {
        final var coordList = new ArrayList<Coordinate>(32);
        final var lines = new ArrayList<Geometry>();
        for (final DoubleBuffer v : coordinates) {
            if (v == null) {
                continue;
            }
            while (v.hasRemaining()) {
                final Coordinate c = createCoordinateTuple(dimensions, v);
                if (!Double.isNaN(c.x) && !Double.isNaN(c.y)) {
                    coordList.add(c);
                } else {
                    addPolyline(coordList, lines, polygon, isFloat);
                    coordList.clear();
                }
            }
        }
        addPolyline(coordList, lines, polygon, isFloat);
        return groupPolylines(lines, polygon, isFloat);
    }

    /**
     * Makes a line string or linear ring from the given coordinates, and adds the line string to the given list.
     * If the {@code polygon} argument is {@code true}, then this method creates polygons instead of line strings.
     * If the given coordinates list is empty, then this method does nothing.
     * This method does not modify the given coordinates list.
     *
     * @param  coordinates  coordinates of the line string or polygon to create.
     * @param  addTo        where to add the created line string or polygon, if such object is created.
     * @param  polygon      {@code true} for creating a polygon, or {@code false} for creating a line string.
     * @param  isFloat      whether to use single-precision instead of double-precision coordinate numbers.
     */
    final void addPolyline(final List<Coordinate> coordinates, final List<Geometry> addTo,
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
     * If the given list contains more than one geometry, then some kind of multi-geometry is returned.
     *
     * @param  polylines  the polygons or lines strings.
     * @return {@code true} if the given list contains {@link Polygon} instances, or
     *         {@code false} if it contains {@link LineString} instances.
     * @throws ArrayStoreException if the geometries in the given list are not instances
     *         of the type specified by the {@code polygon} argument.
     */
    @SuppressWarnings("SuspiciousToArrayCall")      // Type controlled by `polygon`.
    final Geometry groupPolylines(final List<Geometry> polylines, final boolean polygon, final boolean isFloat) {
        final int s = polylines.size();
        switch (s) {
            case 0:  {
                // Create an empty polygon or linear ring.
                final GeometryFactory gf = factory(isFloat);
                return polygon ? gf.createPolygon   ((Coordinate[]) null)
                               : gf.createLinearRing((Coordinate[]) null);
            }
            case 1: {
                return polylines.get(0);
            }
            default: {
                // An ArrayStoreException here would be a bug in our use of `polygon` boolean.
                final GeometryFactory gf = factory(isFloat);
                return polygon ? gf.createMultiPolygon   (polylines.toArray(new Polygon   [s]))
                               : gf.createMultiLineString(polylines.toArray(new LineString[s]));
            }
        }
    }

    /**
     * Creates a polygon from an array of rings. The first ring is taken as the shell,
     * and all other rings are taken as the holes.
     *
     * <h4>API note</h4>
     * The {@code rings} argument is not of type {@code LinearRing[]} because {@code LinearRing}
     * is a JTS-specific type not in the {@link GeometryType} enumeration.
     *
     * @param  factory  the factory to use for creating the polygon.
     * @param  rings    the shell followed by the holes.
     * @return the polygon.
     * @throws ClassCastException if the first array element is not a {@link LinearRing}.
     * @throws ArrayStoreException if one of the holes is not a {@link LinearRing}.
     */
    @SuppressWarnings("fallthrough")
    private static Polygon createPolygon(final GeometryFactory factory, final LineString[] rings) {
        LinearRing   shell = null;
        LinearRing[] holes = null;
        switch (rings.length) {
            default: holes = Arrays.copyOfRange(rings, 1, rings.length, LinearRing[].class);  // Fall through
            case 1:  shell = (LinearRing) rings[0];  // Fall through
            case 0:  break;
        }
        return factory.createPolygon(shell, holes);
    }

    /**
     * Creates a multi-polygon from an array of JTS {@link Polygon} or {@link LineString} instances.
     * If some geometries are actually {@link LinearRing}s, they will be converted to polygons.
     *
     * @param  geometries  the polygons or linear rings to put in a multi-polygons.
     * @throws ClassCastException if an element in the array is not a JTS geometry.
     * @throws IllegalArgumentException if an element is a non-closed linear string.
     * @throws BackingStoreException if the <abbr>CRS</abbr> cannot be created from the <abbr>SRID</abbr> code.
     */
    @Override
    public GeometryWrapper createMultiPolygon(final Object[] geometries) {
        final var polygons = new Polygon[geometries.length];
        boolean isFloat = true;
        for (int i=0; i < geometries.length; i++) {
            final Object polyline = implementation(geometries[i]);
            final Polygon polygon;
            if (polyline instanceof Polygon) {
                polygon = (Polygon) polyline;
            } else {
                final boolean fs;
                final CoordinateSequence cs;
                if (polyline instanceof LinearRing) {
                    final var ring = (LinearRing) polyline;
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
        return createWrapper(factory(isFloat).createMultiPolygon(polygons));
    }

    /**
     * Creates a geometry from components.
     * The expected {@code components} type depends on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components shall be {@code Point[]}, {@code Geometry[]},
     *       {@code LineString[]} or {@code Polygon[]} array, depending on the desired target type.</li>
     *   <li>Otherwise, if {@code type} is {@link GeometryType#POLYGON}, then the components shall be a
     *       {@link LineString[]} with the first ring taken as the shell and all other rings as holes.</li>
     *   <li>Otherwise, the components shall be an array or collection of {@link Point} or {@link Coordinate}
     *       instances, or a JTS-specific {@link CoordinateSequence}.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws IllegalArgumentException if the given geometry type is not supported.
     * @throws ClassCastException if {@code components} is not an array or a collection of supported geometry components.
     * @throws ArrayStoreException if {@code components} is an array with invalid component type.
     * @throws BackingStoreException if the <abbr>CRS</abbr> cannot be created from the <abbr>SRID</abbr> code.
     */
    @Override
    public GeometryWrapper createFromComponents(final GeometryType type, final Object components) {
        final Geometry geometry;
        switch (type) {
            case POINT: {
                geometry = createFromCoordinateSequence(components, GeometryFactory::createMultiPoint).getCentroid();
                break;
            }
            case LINESTRING: {
                geometry = createFromCoordinateSequence(components, GeometryFactory::createLineString);
                break;
            }
            case POLYGON: {
                if (components instanceof LineString[]) {
                    geometry = createFromComponentArray((LineString[]) components,
                                LineString::getCoordinateSequence,
                                Factory::createPolygon);
                } else {
                    geometry = createFromCoordinateSequence(components, GeometryFactory::createPolygon);
                }
                break;
            }
            case MULTIPOINT: {
                if (components instanceof Point[]) {
                    geometry = createFromComponentArray((Point[]) components,
                                Point::getCoordinateSequence,
                                GeometryFactory::createMultiPoint);
                } else {
                    geometry = createFromCoordinateSequence(components, GeometryFactory::createMultiPoint);
                }
                break;
            }
            case MULTILINESTRING: {
                // The ClassCastException that may happen here is part of method contract.
                geometry = createFromComponentArray((LineString[]) components,
                            LineString::getCoordinateSequence,
                            GeometryFactory::createMultiLineString);
                break;
            }
            case MULTIPOLYGON: {
                // The ClassCastException that may happen here is part of method contract.
                geometry = createFromComponentArray((Polygon[]) components,
                            (g) -> g.getExteriorRing().getCoordinateSequence(),
                            GeometryFactory::createMultiPolygon);
                break;
            }
            case GEOMETRYCOLLECTION: {
                // The ClassCastException that may happen here is part of method contract.
                geometry = factory.createGeometryCollection((Geometry[]) components);
                break;
            }
            case GEOMETRY: {
                return createFromComponents(components);
            }
            default: {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, type));
            }
        }
        return createWrapper(geometry);
    }

    /**
     * Creates a geometry from an array of components, which may be single or double-precision.
     * If all components use single-precision, then the returned geometry will use single-precision too.
     *
     * <p>We have to use generic type because JTS does not provide an
     * {@code Geometry.getCoordinateSequence()} abstract method.</p>
     *
     * @param  <G>          the type of components in the geometry to create.
     * @param  components   the array of components to give to the geometry constructor.
     * @param  cseqGetter   the method returning the coordinate sequence of a component.
     * @param  constructor  the factory method which will create a geometry from the given components.
     */
    private <G extends Geometry> Geometry createFromComponentArray(
            final G[] components,
            final Function<G, CoordinateSequence> cseqGetter,
            final BiFunction<GeometryFactory, G[], Geometry> constructor)
    {
        boolean isFloat = true;
        for (final G component : components) {
            if (!isFloat(cseqGetter.apply(component))) {
                isFloat = false;
                break;
            }
        }
        return constructor.apply(factory(isFloat), components);
    }

    /**
     * Creates a geometry from a coordinate sequence.
     * Coordinate arrays or collections are also accepted and converted to coordinate sequence.
     *
     * @param  sequence     the coordinate sequences, or array, or collection.
     * @param  constructor  the factory method which will create a geometry from the given coordinate sequences.
     * @return geometry built from the given coordinate sequence.
     */
    private Geometry createFromCoordinateSequence(final Object sequence,
            final BiFunction<GeometryFactory, CoordinateSequence, Geometry> constructor)
    {
        final GeometryFactory gf;
        final CoordinateSequence cs;
        if (sequence instanceof CoordinateSequence) {
            cs = (CoordinateSequence) sequence;
            gf = factory(isFloat(cs));
        } else {
            final Coordinate[] coordinates;
            if (sequence instanceof Coordinate[]) {
                coordinates = (Coordinate[]) sequence;
                gf = factory;
            } else {
                // The ClassCastException that may happen here is part of method contract.
                final Collection<?> source = (sequence instanceof Collection<?>)
                        ? (Collection<?>) sequence : Arrays.asList((Object[]) sequence);
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
        return constructor.apply(gf, cs);
    }

    /**
     * Parses the given Well Known Text (WKT).
     *
     * @param  wkt  the Well Known Text to parse.
     * @return the geometry object for the given WKT.
     * @throws ParseException if the <abbr>WKT</abbr> cannot be parsed.
     * @throws FactoryException if the <abbr>CRS</abbr> cannot be created from the <abbr>SRID</abbr> code.
     */
    @Override
    public GeometryWrapper parseWKT(final String wkt) throws ParseException, FactoryException {
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
     * @throws ParseException if the <abbr>WKB</abbr> cannot be parsed.
     * @throws FactoryException if the <abbr>CRS</abbr> cannot be created from the <abbr>SRID</abbr> code.
     */
    @Override
    public GeometryWrapper parseWKB(final ByteBuffer data) throws ParseException, FactoryException {
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
