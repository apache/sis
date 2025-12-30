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
package org.apache.sis.geometry.wrapper.esri;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.io.ObjectStreamException;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Line;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.OperatorCentroid2D;
import com.esri.core.geometry.OperatorImportFromWkb;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.WkbImportFlags;
import com.esri.core.geometry.WktImportFlags;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.wrapper.Capability;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;


/**
 * The factory of geometry objects backed by ESRI.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class Factory extends Geometries<Geometry> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7832006589071845318L;

    /**
     * The singleton instance of this factory.
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
     * Creates the singleton instance.
     */
    private Factory() {
        super(GeometryLibrary.ESRI, Geometry.class, Point.class);
    }

    /**
     * Returns the geometry class of the given type.
     *
     * @param  type  type of geometry for which the class is desired.
     * @return implementation class for the geometry of the specified type.
     */
    @Override
    public Class<?> getGeometryClass(final GeometryType type) {
        switch (type) {
            default:            return Geometry.class;
            case POINT:         return Point.class;
            case MULTIPOINT:    return MultiPoint.class;
            case MULTILINESTRING:
            case LINESTRING:    return Polyline.class;
            case MULTIPOLYGON:
            case POLYGON:       return Polygon.class;
        }
    }

    /**
     * Returns the implementation-neutral type for the given ESRI class or array class.
     * If the given class is not recognized, then {@link GeometryType#GEOMETRY} is returned.
     *
     * @param  type  the ESRI class, or an array type having a ESRI class as components.
     * @return the implementation-neutral type for the given class, or {@code GEOMETRY} if not recognized.
     */
    @Override
    public GeometryType getGeometryType(Class<?> type) {
        final Class<?> component = type.getComponentType();
        if (component != null) type = component;    // ESRI does not distinguish single components and collections.
        if (Point     .class.isAssignableFrom(type)) return (component != null) ? GeometryType.MULTIPOINT      : GeometryType.POINT;
        if (Polygon   .class.isAssignableFrom(type)) return (component != null) ? GeometryType.MULTIPOLYGON    : GeometryType.POLYGON;
        if (Polyline  .class.isAssignableFrom(type)) return (component != null) ? GeometryType.MULTILINESTRING : GeometryType.LINESTRING;
        if (MultiPoint.class.isAssignableFrom(type)) return GeometryType.MULTIPOINT;
        return GeometryType.GEOMETRY;
    }

    /**
     * Returns the geometry object to return to the user in public API.
     *
     * @param  wrapper  the wrapper for which to get the geometry, or {@code null}.
     * @return the ESRI geometry instance, or {@code null} if the given wrapper was null.
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
     * Returns a wrapper for the given {@code <G>} or {@code GeometryWrapper} geometry.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or {@code null}.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     */
    @Override
    public GeometryWrapper castOrWrap(final Object geometry) {
        if (geometry == null || geometry instanceof Wrapper) {
            return (Wrapper) geometry;
        } else {
            return new Wrapper((Geometry) geometry);
        }
    }

    /**
     * Notifies that this library supports <var>z</var> and <var>m</var> coordinates.
     * Notifies that this library does not support single-precision floating point type.
     */
    @Override
    public boolean supports(final Capability feature) {
        return feature != Capability.SINGLE_PRECISION;
    }

    /**
     * Creates a wrapper for the given geometry instance.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     */
    @Override
    protected GeometryWrapper createWrapper(final Geometry geometry) {
        return new Wrapper(geometry);
    }

    /**
     * Creates a two-dimensional point from the given coordinates.
     */
    @Override
    public Object createPoint(final double x, final double y) {
        return new Point(x, y);
    }

    /**
     * Creates a three-dimensional point from the given coordinates.
     */
    @Override
    public Object createPoint(final double x, final double y, final double z) {
        return new Point(x, y, z);
    }

    /**
     * Creates a single point from the given coordinates with the given dimensions.
     *
     * @param  isFloat      ignored.
     * @param  dimensions   the dimensions of the coordinate tuple.
     * @param  coordinates  a (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuple.
     * @return the point for the given coordinate values.
     */
    @Override
    public Object createPoint(final boolean isFloat, final Dimensions dimensions, final DoubleBuffer coordinates) {
        final var point = new Point(coordinates.get(), coordinates.get());
        if (dimensions.hasZ) point.setM(coordinates.get());
        if (dimensions.hasM) point.setM(coordinates.get());
        return point;
    }

    /**
     * Creates a collection of points from the given coordinate values.
     * The buffer position is advanced by {@code dimensions.count} × the number of points.
     *
     * @param  isFloat      ignored.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     * @return the collection of points for the given points.
     */
    @Override
    public Geometry createMultiPoint(final boolean isFloat, final Dimensions dimensions, final DoubleBuffer coordinates) {
        final boolean is2D = (dimensions == Dimensions.XY);
        final var points = new MultiPoint();
        while (coordinates.hasRemaining()) {
            final double x = coordinates.get();
            final double y = coordinates.get();
            if (is2D) {     // Optimization for a common case.
                points.add(x, y);
            } else {
                final var point = new Point(x, y);
                if (dimensions.hasZ) point.setZ(coordinates.get());
                if (dimensions.hasM) point.setM(coordinates.get());
                points.add(point);
            }
        }
        return points;
    }

    /**
     * Creates a polyline from the given coordinate values.
     * Each {@link Double#NaN} coordinate value starts a new path.
     *
     * @param  polygon      whether to return the path as a polygon instead of polyline.
     * @param  isFloat      ignored.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     */
    @Override
    public Geometry createPolyline(final boolean polygon, final boolean isFloat,
            final Dimensions dimensions, final DoubleBuffer... coordinates)
    {
        final boolean is2D = (dimensions == Dimensions.XY);
        boolean lineTo = false;
        final var path = new Polyline();
        for (final DoubleBuffer v : coordinates) {
            if (v == null) {
                continue;
            }
            while (v.hasRemaining()) {
                final double x = v.get();
                final double y = v.get();
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    v.position(v.position() + (dimensions.count - BIDIMENSIONAL));
                    lineTo = false;
                } else if (is2D) {
                    if (lineTo) {
                        path.lineTo(x, y);
                    } else {
                        path.startPath(x, y);
                        lineTo = true;
                    }
                } else {
                    final var point = new Point(x, y);
                    if (dimensions.hasZ) point.setZ(v.get());
                    if (dimensions.hasM) point.setM(v.get());
                    if (lineTo) {
                        path.lineTo(point);
                    } else {
                        path.startPath(point);
                        lineTo = true;
                    }
                }
            }
        }
        if (polygon) {
            final var p = new Polygon();
            p.add(path, false);
            return p;
        }
        return path;
    }

    /**
     * Creates a multi-polygon from an array of geometries.
     * Callers must ensure that the given objects are ESRI geometries.
     *
     * @param  geometries  the polygons or linear rings to put in a multi-polygons.
     * @throws ClassCastException if an element in the array is not an ESRI geometry.
     */
    @Override
    public GeometryWrapper createMultiPolygon(final Object[] geometries) {
        final Polygon polygon = new Polygon();
        for (final Object geometry : geometries) {
            polygon.add((MultiPath) implementation(geometry), false);
        }
        return new Wrapper(polygon);
    }

    /**
     * Creates a geometry from components.
     * The expected {@code components} type depends on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components shall be an array of {@link Point},
     *       {@link Geometry}, {@link Polyline} or {@link Polygon} elements, depending on the desired target type.</li>
     *   <li>Otherwise, the components shall be an array or collection of {@link Point} instances.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws IllegalArgumentException if the given geometry type is not supported.
     * @throws ClassCastException if the given object is not an array or a collection of supported geometry components.
     */
    @Override
    public GeometryWrapper createFromComponents(final GeometryType type, final Object components) {
        /*
         * No exhaustive `if (x instanceof y)` checks in this method.
         * `ClassCastException` shall be handled by the caller.
         */
        final Collection<?> data = (components instanceof Collection<?>)
                ? (Collection<?>) components : Arrays.asList((Object[]) components);
        /*
         * ESRI API does not distinguish between single geometry and geometry collection, except MultiPoint.
         * So if the number of components is 1, there is no reason to create a new geometry object.
         */
        Geometry geometry = (Geometry) Containers.peekIfSingleton(data);
multi:  if (geometry == null) {
            boolean isPolygon = false;
            switch (type) {
                default: {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, type));
                }
                case GEOMETRY: {
                    return createFromComponents(components);
                }
                case LINESTRING:
                case MULTILINESTRING: {
                    break;
                }
                case POLYGON:
                case MULTIPOLYGON: {
                    isPolygon = true;
                    break;
                }
                case GEOMETRYCOLLECTION: {
                    for (final Object component : data) {
                        isPolygon = (((Geometry) component).getType() == Geometry.Type.Polygon);
                        if (!isPolygon) break;
                    }
                    break;
                }
                case POINT:
                case MULTIPOINT: {
                    final var points = new MultiPoint();
                    for (final Object p : data) {
                        points.add((Point) p);
                    }
                    geometry = points;
                    if (type == GeometryType.POINT) {
                        geometry = new Point(OperatorCentroid2D.local().execute(geometry, null));
                    }
                    break multi;
                }
            }
            /*
             * All types other than point and multi-points.
             */
            final MultiPath path = isPolygon ? new Polygon() : new Polyline();
            if (type.isCollection) {
                for (final Object component : data) {
                    path.add((MultiPath) component, false);
                }
            } else {
                final Iterator<?> it = data.iterator();
                if (it.hasNext()) {
                    final Line segment = new Line();
                    segment.setEnd((Point) it.next());
                    while (it.hasNext()) {
                        segment.setStartXY(segment.getEndX(), segment.getEndY());
                        segment.setEnd((Point) it.next());
                        path.addSegment(segment, false);
                    }
                }
            }
            geometry = path;
        }
        return new Wrapper(geometry);
    }

    /**
     * Parses the given Well Known Text (WKT).
     *
     * @param  wkt  the Well Known Text to parse.
     * @return the geometry object for the given WKT.
     */
    @Override
    public GeometryWrapper parseWKT(final String wkt) {
        return new Wrapper(OperatorImportFromWkt.local().execute(WktImportFlags.wktImportDefaults, Geometry.Type.Unknown, wkt, null));
    }

    /**
     * Reads the given Well Known Binary (WKB).
     *
     * @param  data  the sequence of bytes to parse.
     * @return the geometry object for the given WKB.
     */
    @Override
    public GeometryWrapper parseWKB(ByteBuffer data) {
        if (data.position() != 0) {
            data = data.slice();    // ESRI implementation seems to ignore the position and always starts reading at 0.
        }
        return new Wrapper(OperatorImportFromWkb.local().execute(WkbImportFlags.wkbImportDefaults, Geometry.Type.Unknown, data, null));
    }
}
