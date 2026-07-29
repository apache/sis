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
package org.apache.sis.storage.rs.internal.shared.s2;

import com.google.common.geometry.S2Cap;
import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2LatLngRect;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Region;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2PointRegion;
import com.google.common.geometry.S2Polyline;
import com.google.common.geometry.S2Polygon;
import com.google.common.geometry.S2RegionUnion;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.sis.geometry.wrapper.Capability;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.collection.Containers;

/**
 * The factory of geometry objects backed by S2.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Factory extends Geometries<S2Region> {

    /**
     * The singleton instance of this factory.
     */
    public static final Factory INSTANCE = new Factory();

    /**
     * Creates the singleton instance.
     */
    private Factory() {
        /** TODO this should be S2, not GEOAPI, need a sis update */
        super(GeometryLibrary.GEOAPI, S2Region.class, S2Point.class);
    }

    /**
     * Returns the geometry class of the given type.
     *
     * @param  type  type of geometry for which the class is desired.
     * @return implementation class for the geometry of the specified type.
     */
    @Override
    public Class<?> getGeometryClass(GeometryType type) {
        switch (type) {
            default:            return S2Region.class;
            case POINT:         return S2Point.class;
            case LINESTRING:    return S2Polyline.class;
            case POLYGON:       return S2Polygon.class;
            case GEOMETRYCOLLECTION:
            case MULTIPOINT:
            case MULTILINESTRING:
            case MULTIPOLYGON:  return S2RegionUnion.class;
        }
    }

    /**
     * Returns the implementation-neutral type for the given S2 class.
     * If the given class is not recognized, then {@link GeometryType#GEOMETRY} is returned.
     *
     * @param  type  the S2 class
     * @return the implementation-neutral type for the given class, or {@code GEOMETRY} if not recognized.
     */
    @Override
    public GeometryType getGeometryType(Class<?> type) {
        if (S2Cap        .class.isAssignableFrom(type)) return GeometryType.POLYGON;
        if (S2Cell       .class.isAssignableFrom(type)) return GeometryType.POLYGON;
        if (S2LatLngRect .class.isAssignableFrom(type)) return GeometryType.POLYGON;
        if (S2Loop       .class.isAssignableFrom(type)) return GeometryType.LINESTRING;
        if (S2Point      .class.isAssignableFrom(type)) return GeometryType.POINT;
        if (S2PointRegion.class.isAssignableFrom(type)) return GeometryType.POINT;
        if (S2Polygon    .class.isAssignableFrom(type)) return GeometryType.POLYGON;
        if (S2Polyline   .class.isAssignableFrom(type)) return GeometryType.LINESTRING;
        if (S2RegionUnion.class.isAssignableFrom(type)) return GeometryType.GEOMETRYCOLLECTION;
        return GeometryType.GEOMETRY;
    }

    /**
     * Returns the geometry object to return to the user in public API.
     *
     * @param  wrapper  the wrapper for which to get the geometry, or {@code null}.
     * @return the S2 geometry instance, or {@code null} if the given wrapper was null.
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
            return new Wrapper((S2Region) geometry);
        }
    }

    /**
     * Notifies that this library does not support <var>z</var> and <var>m</var> or single-precision floating point type.
     */
    @Override
    public boolean supports(final Capability feature) {
        return false;
    }

    /**
     * Creates a wrapper for the given geometry instance.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     */
    @Override
    protected GeometryWrapper createWrapper(final S2Region geometry) {
        return new Wrapper(geometry);
    }

    /**
     * Creates a point from the given coordinates.
     */
    @Override
    public S2Point createPoint(double lon, double lat) {
        return S2LatLng.fromDegrees(lat, lon).toPoint();
    }

    /**
     * Raises an UnsupportedOperationException, Z ordinate is not supported by S2.
     */
    @Override
    public S2Point createPoint(double x, double y, double z) {
        throw new UnsupportedOperationException("Z ordinate not supported in S2.");
    }

    /**
     * Creates a single point from the given coordinates with the given dimensions.
     * Only supports 2D coordinates.
     *
     * @param  isFloat      ignored.
     * @param  dimensions   the dimensions of the coordinate tuple.
     * @param  coordinates  a (x,y) coordinate tuple.
     * @return the point for the given coordinate values.
     * @throws UnsupportedOperationException if Z or M ordinate is defined.
     */
    @Override
    public S2Point createPoint(boolean isFloat, Dimensions dimensions, DoubleBuffer coordinates) {
        only2D(dimensions);
        return createPoint(coordinates.get(), coordinates.get());
    }

    /**
     * Creates a collection of points from the given coordinate values.
     * The buffer position is advanced by {@code dimensions.count} × the number of points.
     * Only supports 2D coordinates.
     *
     * @param  isFloat      ignored.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y) coordinate tuples.
     * @return the region union for the given points.
     * @throws UnsupportedOperationException if Z or M ordinate is defined.
     */
    @Override
    public S2Region createMultiPoint(boolean isFloat, Dimensions dimensions, DoubleBuffer coordinates) {
        only2D(dimensions);
        final List<S2Region> regions = new ArrayList<>();
        while (coordinates.hasRemaining()) {
            final double lon = coordinates.get();
            final double lat = coordinates.get();
            regions.add(createPoint(lon, lat));
        }
        return new S2RegionUnion(regions);
    }

    /**
     * Creates a polyline from the given coordinate values.
     * Each {@link Double#NaN} coordinate value starts a new path.
     * Only supports 2D coordinates.
     *
     * @param  polygon      whether to return the path as a polygon instead of polyline.
     * @param  isFloat      ignored.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y) coordinate tuples.
     * @throws UnsupportedOperationException if Z or M ordinate is defined.
     */
    @Override
    public S2Region createPolyline(boolean polygon, boolean isFloat, Dimensions dimensions, DoubleBuffer... coordinates) {
        only2D(dimensions);

        final List<S2Polyline> lines = new ArrayList<>();
        final List<S2Point> path = new ArrayList<>();
        for (final DoubleBuffer v : coordinates) {
            if (v == null) {
                continue;
            }
            while (v.hasRemaining()) {
                final double lon = v.get();
                final double lat = v.get();
                if (Double.isNaN(lon) || Double.isNaN(lat)) {
                    v.position(v.position() + (dimensions.count - BIDIMENSIONAL));
                    //store this line
                    if (!path.isEmpty()) {
                        lines.add(new S2Polyline(path)); //path is copied by the constructor
                        path.clear();
                    }
                } else {
                    path.add(createPoint(lon, lat));
                }
            }
        }

        if (!path.isEmpty()) {
            lines.add(new S2Polyline(path)); //path is copied by the constructor
            path.clear();
        }

        if (polygon) {
            if (lines.size() != 1) {
                throw new IllegalArgumentException("Polyline must contain a single loop to create a polygon");
            }
            return new S2Polygon(new S2Loop(lines.get(0).vertices()));
        } else {
            if (lines.size() == 1) {
                return lines.get(0);
            } else {
                return new S2RegionUnion((List)lines);
            }
        }
    }

    /**
     * Creates a multi-polygon from an array of geometries.
     * Callers must ensure that the given objects are S2 geometries.
     *
     * @param  geometries  the polygons to put in a multi-polygons.
     * @throws ClassCastException if an element in the array is not an S2 geometry.
     */
    @Override
    public GeometryWrapper createMultiPolygon(Object[] geometries) {
        return new Wrapper(new S2RegionUnion((List)List.of(geometries)));
    }

    /**
     * Creates a geometry from components.
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws ClassCastException if the given object is not an array or a collection of supported geometry components.
     */
    @Override
    public GeometryWrapper createFromComponents(GeometryType type, Object components) {
        /*
         * No exhaustive `if (x instanceof y)` checks in this method.
         * `ClassCastException` shall be handled by the caller.
         */
        final Collection<?> data = (components instanceof Collection<?>)
                ? (Collection<?>) components : Arrays.asList((Object[]) components);
        S2Region geometry = (S2Region) Containers.peekIfSingleton(data);
        if (geometry == null) {
            geometry = new S2RegionUnion((Collection)data);
        }
        return new Wrapper(geometry);
    }

    /**
     * Raise an UnsupportedOperationException, S2 do not support WKT.
     */
    @Override
    public GeometryWrapper parseWKT(String wkt) throws Exception {
        throw new UnsupportedOperationException("S2 do not support WKT");
    }

    /**
     * Raise an UnsupportedOperationException, S2 do not support WKB.
     */
    @Override
    public GeometryWrapper parseWKB(ByteBuffer data) throws Exception {
        throw new UnsupportedOperationException("S2 do not support WKB");
    }

    private static void only2D(Dimensions dimensions) throws UnsupportedOperationException {
        if (dimensions.hasZ) throw new UnsupportedOperationException("Z ordinate not supported in S2.");
        if (dimensions.hasM) throw new UnsupportedOperationException("M ordinate not supported in S2.");
    }
}
