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
package org.apache.sis.internal.feature.esri;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.nio.ByteBuffer;
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
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryType;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.math.Vector;


/**
 * The factory of geometry objects backed by ESRI.
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
    private static final long serialVersionUID = 7832006589071845318L;

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
     * Creates the singleton instance.
     */
    private Factory() {
        super(GeometryLibrary.ESRI, Geometry.class, Point.class, Polyline.class, Polygon.class);
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
     * Creates a polyline from the given coordinate values.
     * Each {@link Double#NaN} coordinate value starts a new path.
     *
     * @param  polygon      whether to return the path as a polygon instead of polyline.
     * @param  dimension    the number of dimensions ({@value #BIDIMENSIONAL} or {@value #TRIDIMENSIONAL}).
     * @param  coordinates  sequence of (x,y) or (x,y,z) tuples.
     * @throws UnsupportedOperationException if this operation is not implemented for the given number of dimensions.
     */
    @Override
    public Geometry createPolyline(final boolean polygon, final int dimension, final Vector... coordinates) {
        if (dimension != BIDIMENSIONAL) {
            throw new UnsupportedOperationException(unsupported(dimension));
        }
        boolean lineTo = false;
        final Polyline path = new Polyline();
        for (final Vector v : coordinates) {
            if (v != null) {
                final int size = v.size();
                for (int i=0; i<size;) {
                    final double x = v.doubleValue(i++);
                    final double y = v.doubleValue(i++);
                    if (Double.isNaN(x) || Double.isNaN(y)) {
                        lineTo = false;
                    } else if (lineTo) {
                        path.lineTo(x, y);
                    } else {
                        path.startPath(x, y);
                        lineTo = true;
                    }
                }
            }
        }
        if (polygon) {
            final Polygon p = new Polygon();
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
    public GeometryWrapper<Geometry> createMultiPolygon(final Object[] geometries) {
        final Polygon polygon = new Polygon();
        for (final Object geometry : geometries) {
            polygon.add((MultiPath) unwrap(geometry), false);
        }
        return new Wrapper(polygon);
    }

    /**
     * Creates a geometry from components.
     * The expected {@code components} type depend on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components shall be an array of {@link Point},
     *       {@link Geometry}, {@link Polyline} or {@link Polygon} elements, depending on the desired target type.</li>
     *   <li>Otherwise the components shall be an array or collection of {@link Point} instances.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws ClassCastException if the given object is not an array or a collection of supported geometry components.
     */
    @Override
    public GeometryWrapper<Geometry> createFromComponents(final GeometryType type, final Object components) {
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
        Geometry geometry = (Geometry) CollectionsExt.singletonOrNull(data);
        if (geometry == null) {
            boolean isPolygon = false;
            switch (type) {
                case MULTI_LINESTRING:
                case LINESTRING: break;
                case MULTI_POLYGON:
                case POLYGON: isPolygon=true; break;
                case GEOMETRY_COLLECTION: {
                    for (final Object component : data) {
                        isPolygon = (((Geometry) component).getType() == Geometry.Type.Polygon);
                        if (!isPolygon) break;
                    }
                    break;
                }
                case GEOMETRY:      // Default to multi-points for now.
                case POINT:
                case MULTI_POINT: {
                    final MultiPoint points = new MultiPoint();
                    for (final Object p : data) {
                        points.add((Point) p);
                    }
                    geometry = points;
                    if (type == GeometryType.POINT) {
                        geometry = new Point(OperatorCentroid2D.local().execute(geometry, null));
                    }
                    break;
                }
                default: throw new AssertionError(type);
            }
            if (geometry == null) {
                final MultiPath path = isPolygon ? new Polygon() : new Polyline();
                if (type.isCollection()) {
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
    public GeometryWrapper<Geometry> parseWKT(final String wkt) {
        return new Wrapper(OperatorImportFromWkt.local().execute(WktImportFlags.wktImportDefaults, Geometry.Type.Unknown, wkt, null));
    }

    /**
     * Reads the given Well Known Binary (WKB).
     *
     * @param  data  the sequence of bytes to parse.
     * @return the geometry object for the given WKB.
     */
    @Override
    public GeometryWrapper<Geometry> parseWKB(final ByteBuffer data) {
        return new Wrapper(OperatorImportFromWkb.local().execute(WkbImportFlags.wkbImportDefaults, Geometry.Type.Unknown, data, null));
    }
}
