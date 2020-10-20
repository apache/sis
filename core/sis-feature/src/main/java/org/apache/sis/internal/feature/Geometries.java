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
package org.apache.sis.internal.feature;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Iterator;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.math.Vector;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;


/**
 * Utility methods on geometric objects defined in libraries outside Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to ESRI's API or to Java Topology Suite (JTS) API.
 * This gives us a single place to review if we want to support different geometry libraries,
 * or if Apache SIS come with its own implementation.
 *
 * @param   <G>  the base class of all geometry objects (except point in some implementations).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   0.7
 * @module
 */
public abstract class Geometries<G> {
    /**
     * The {@value} value, used by subclasses for identifying code that assume two- or three-dimensional objects.
     */
    public static final int BIDIMENSIONAL = 2, TRIDIMENSIONAL = 3;

    /**
     * The enumeration value that identifies which geometry library is used.
     */
    public final GeometryLibrary library;

    /**
     * The root geometry class.
     */
    public final Class<G> rootClass;

    /**
     * The class for points.
     */
    public final Class<?> pointClass;

    /**
     * The class for polylines and polygons.
     */
    public final Class<? extends G> polylineClass, polygonClass;

    /**
     * The fallback implementation to use if the default one is not available.
     * This is set by {@link GeometryFactories} and should not change after initialization.
     * We do not synchronize accesses to this field because we keep it stable after
     * {@link GeometryFactories} class initialization.
     */
    Geometries<?> fallback;

    /**
     * {@code true} if {@link #pointClass} is not a subtype of {@link #rootClass}.
     * This is true for Java2D and false for JTS and ESRI libraries.
     */
    private final boolean isPointClassDistinct;

    /**
     * Creates a new adapter for the given root geometry class.
     *
     * @param  library        the enumeration value that identifies which geometry library is used.
     * @param  rootClass      the root geometry class.
     * @param  pointClass     the class for points.
     * @param  polylineClass  the class for polylines.
     * @param  polygonClass   the class for polygons.
     */
    protected Geometries(final GeometryLibrary library, final Class<G> rootClass, final Class<?> pointClass,
                         final Class<? extends G> polylineClass, final Class<? extends G> polygonClass)
    {
        this.library         = library;
        this.rootClass       = rootClass;
        this.pointClass      = pointClass;
        this.polylineClass   = polylineClass;
        this.polygonClass    = polygonClass;
        isPointClassDistinct = !rootClass.isAssignableFrom(pointClass);
    }

    /**
     * Returns a factory backed by the specified geometry library implementation,
     * of the default implementation if the specified library is {@code null}.
     *
     * @param  library  the desired library, or {@code null} for the default.
     * @return the specified or the default geometry implementation (never {@code null}).
     * @throws IllegalArgumentException if a non-null library is specified by that library is not available.
     */
    public static Geometries<?> implementation(final GeometryLibrary library) {
        Geometries<?> g = GeometryFactories.implementation;
        if (library == null) {
            return g;
        }
        while (g != null) {
            if (g.library == library) return g;
            g = g.fallback;
        }
        throw new IllegalArgumentException(Resources.format(Resources.Keys.UnavailableGeometryLibrary_1, library));
    }

    /**
     * Returns a factory backed by the same implementation than the given type.
     * If the given type is not recognized, then this method returns the default library.
     *
     * @param  type  the type for which to get a geometry factory.
     * @return a geometry factory compatible with the given type if possible, or the default factory otherwise.
     */
    public static Geometries<?> implementation(final Class<?> type) {
        final Geometries<?> implementation = GeometryFactories.implementation;
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            if (g.isSupportedType(type)) return g;
        }
        return implementation;
    }

    /**
     * Returns {@code true} if the given type is one of the geometry types known to Apache SIS.
     *
     * @param  type  the type to verify.
     * @return {@code true} if the given type is one of the geometry types known to SIS.
     */
    public static boolean isKnownType(final Class<?> type) {
        for (Geometries<?> g = GeometryFactories.implementation; g != null; g = g.fallback) {
            if (g.isSupportedType(type)) return true;
        }
        return GeometryWrapper.class.isAssignableFrom(type);
    }

    /**
     * Returns {@code true} if the given class is a geometry type supported by the underlying library.
     */
    private boolean isSupportedType(final Class<?> type) {
        return rootClass.isAssignableFrom(type) || (isPointClassDistinct && pointClass.isAssignableFrom(type));
    }

    /**
     * Wraps the given geometry implementation if recognized.
     * If the given object is already an instance of {@link GeometryWrapper}, then it is returned as-is.
     * If the given object is not recognized, then this method returns an empty value.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or empty value.
     */
    public static Optional<GeometryWrapper<?>> wrap(final Object geometry) {
        if (geometry != null) {
            if (geometry instanceof GeometryWrapper<?>) {
                return Optional.of((GeometryWrapper<?>) geometry);
            }
            for (Geometries<?> g = GeometryFactories.implementation; g != null; g = g.fallback) {
                if (g.isSupportedType(geometry.getClass())) {
                    return Optional.of(g.createWrapper(geometry));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * If the given object is an instance of {@link GeometryWrapper}, returns the wrapped geometry implementation.
     * Otherwise returns the given geometry unchanged.
     *
     * @param  geometry  the geometry to unwrap (can be {@code null}).
     * @return the geometry implementation, or the given geometry as-is.
     */
    protected static Object unwrap(final Object geometry) {
        return (geometry instanceof GeometryWrapper<?>) ? ((GeometryWrapper<?>) geometry).implementation() : geometry;
    }

    /**
     * If the given object is one of the recognized point implementations, returns its coordinates.
     * Otherwise returns empty optional. If non-empty, the returned array may have a length of 2 or 3.
     * If the CRS is geographic, then the (x,y) values should be (longitude, latitude) for compliance
     * with usage in ESRI and JTS libraries.
     *
     * @param  point  the point from which to get the coordinate, or {@code null}.
     * @return the coordinate of the given point as an array of length 2 or 3,
     *         or empty if the given object is not a recognized implementation.
     *
     * @see #createPoint(double, double)
     */
    public static Optional<double[]> getPointCoordinates(final Object point) {
        return wrap(point).map(GeometryWrapper::getPointCoordinates);
    }

    /**
     * If the given object is one of the recognized types and its envelope is non-empty,
     * returns that envelope as an Apache SIS implementation. Otherwise returns empty optional.
     *
     * @param  geometry  the geometry from which to get the envelope, or {@code null}.
     * @return the envelope of the given geometry, or empty if the given object
     *         is not a recognized geometry or its envelope is empty.
     *
     * @see #toGeometry2D(Envelope, WraparoundMethod)
     * @see GeometryWrapper#getEnvelope()
     */
    public static Optional<GeneralEnvelope> getEnvelope(final Object geometry) {
        return wrap(geometry).map(GeometryWrapper::getEnvelope);
    }

    /**
     * If the given object is one of the recognized types, returns a short string representation
     * (typically the class name and the bounds). Otherwise returns empty optional.
     *
     * @param  geometry  the geometry from which to get a string representation, or {@code null}.
     * @return a short string representation of the given geometry, or empty if the given object
     *         is not a recognized geometry.
     *
     * @see GeometryWrapper#toString()
     */
    public static Optional<String> toString(final Object geometry) {
        return wrap(geometry).map(GeometryWrapper::toString);
    }

    /**
     * Parses the given Well Known Text (WKT).
     *
     * @param  wkt  the WKT to parse. Can not be null.
     * @return the geometry object for the given WKT (never {@code null}).
     * @throws Exception if the WKT can not be parsed. The exception sub-class depends on the implementation.
     *
     * @see GeometryWrapper#formatWKT(double)
     */
    public abstract GeometryWrapper<G> parseWKT(String wkt) throws Exception;

    /**
     * Reads the given bytes as a Well Known Binary (WKB) encoded geometry.
     * Whether this method changes the buffer position or not is implementation-dependent.
     *
     * @param  data  the binary data in WKB format. Can not be null.
     * @return decoded geometry (never {@code null}).
     * @throws Exception if the WKB can not be parsed. The exception sub-class depends on the implementation.
     */
    public abstract GeometryWrapper<G> parseWKB(ByteBuffer data) throws Exception;

    /**
     * Returns whether this library can produce geometry backed by the {@code float} primitive type
     * instead than the {@code double} primitive type. If single-precision mode is supported, using
     * that mode may reduce memory usage.
     *
     * @return whether the library support single-precision values.
     */
    public boolean supportSinglePrecision() {
        return false;
    }

    /**
     * Creates a two-dimensional point from the given coordinate. If the CRS is geographic, then the
     * (x,y) values should be (longitude, latitude) for compliance with usage in ESRI and JTS libraries.
     * The returned object will be an instance of {@link #pointClass}.
     *
     * @param  x  the first coordinate value.
     * @param  y  the second coordinate value.
     * @return the point for the given coordinate values.
     *
     * @see GeometryWrapper#getPointCoordinates()
     */
    public abstract Object createPoint(double x, double y);

    /**
     * Creates a path, polyline or polygon from the given coordinate values.
     * The array of coordinate values will be handled as if all vectors were
     * concatenated in a single vector, ignoring {@code null} array elements.
     * Each {@link Double#NaN} coordinate value in the concatenated vector starts a new path.
     * The implementation returned by this method is an instance of {@link #rootClass}.
     *
     * <p>If the {@code polygon} argument is {@code true}, then the coordinates should
     * make a closed line (e.g: a linear ring), otherwise an exception is thrown.
     *
     * @param  polygon      whether to return the path as a polygon instead than polyline.
     * @param  dimension    the number of dimensions ({@value #BIDIMENSIONAL} or {@value #TRIDIMENSIONAL}).
     * @param  coordinates  sequence of (x,y) or (x,y,z) tuples.
     * @return the geometric object for the given points.
     * @throws UnsupportedOperationException if the geometry library can not create the requested path.
     * @throws IllegalArgumentException if a polygon was requested but the given coordinates do not make
     *         a closed shape (linear ring).
     */
    public abstract G createPolyline(final boolean polygon, int dimension, Vector... coordinates);

    /**
     * Creates a multi-polygon from an array of geometries (polygons or linear rings).
     * Callers must ensure that the given objects are instances of geometric classes
     * of the underlying library.
     *
     * If some geometries are actually linear rings, current behavior is not well defined.
     * Some implementations may convert polylines to polygons but this is not guaranteed.
     *
     * @param  geometries  the polygons or linear rings to put in a multi-polygons.
     * @return the multi-polygon.
     * @throws ClassCastException if an element in the array is not an implementation of backing library.
     *
     * @todo Consider a more general method creating a multi-polygon or multi-line depending on object types,
     *       or returning a more primitive geometry type if the given array contains only one element.
     *       We may want to return null if the array is empty (to be decided later).
     */
    public abstract GeometryWrapper<G> createMultiPolygon(final Object[] geometries);

    /**
     * Creates a polyline made of points describing a rectangle whose start point is the lower left corner.
     * The sequence of points describes each corner, going in clockwise direction and repeating the starting
     * point to properly close the ring.
     *
     * @param  x  dimension of first axis.
     * @param  y  dimension of second axis.
     * @return a polyline made of a sequence of 5 points describing the given rectangle.
     */
    private GeometryWrapper<G> createGeometry2D(final Envelope envelope, final int x, final int y) {
        final DirectPosition lc = envelope.getLowerCorner();
        final DirectPosition uc = envelope.getUpperCorner();
        final double xmin = lc.getOrdinate(x);
        final double ymin = lc.getOrdinate(y);
        final double xmax = uc.getOrdinate(x);
        final double ymax = uc.getOrdinate(y);
        return createWrapper(createPolyline(true, BIDIMENSIONAL, Vector.create(new double[] {
                             xmin, ymin,  xmin, ymax,  xmax, ymax,  xmax, ymin,  xmin, ymin})));
    }

    /**
     * Transforms an envelope to a two-dimensional polygon whose start point is lower corner
     * and other points are the envelope corners in clockwise order. The specified envelope
     * should be two-dimensional (see for example {@link GeneralEnvelope#horizontal()}) but
     * the coordinates does not need to be in (longitude, latitude) order; this method will
     * reorder coordinates as (x,y) on a best-effort basis.
     *
     * @param  envelope  the envelope to convert.
     * @param  strategy  how to resolve wrap-around ambiguities on the envelope.
     * @return the envelope as a polygon, or potentially as two polygons in {@link WraparoundMethod#SPLIT} case.
     *
     * @see #getEnvelope(Object)
     */
    public GeometryWrapper<G> toGeometry2D(final Envelope envelope, final WraparoundMethod strategy) {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        int x = 0, y = 1;
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            int cx = AxisDirections.indexOfColinear(cs, AxisDirection.EAST);
            int cy = AxisDirections.indexOfColinear(cs, AxisDirection.NORTH);
            if (cx >= 0 || cy >= 0) {
                if ((cx < 0 && (cx = cy - 1) < 0 && (cx = cy + 1) >= cs.getDimension()) ||
                    (cy < 0 && (cy = cx + 1) >= cs.getDimension() && (cy = cx - 1) < 0))
                {
                    // May happen if the CRS has only one dimension.
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyEnvelope2D));
                }
                x = cx;
                y = cy;
            }
        }
        switch (strategy) {
            case NORMALIZE: {
                throw new IllegalArgumentException();
            }
            case NONE: {
                return createGeometry2D(envelope, x, y);
            }
            default: {
                final GeneralEnvelope ge = new GeneralEnvelope(envelope);
                ge.normalize();
                ge.wraparound(strategy);
                return createGeometry2D(ge, x, y);
            }
            case SPLIT: {
                final Envelope[] parts = AbstractEnvelope.castOrCopy(envelope).toSimpleEnvelopes();
                if (parts.length == 1) {
                    return createGeometry2D(parts[0], x, y);
                }
                final Object[] polygons = new Object[parts.length];
                for (int i=0; i<parts.length; i++) {
                    polygons[i] = createGeometry2D(parts[i], x, y);
                }
                return createMultiPolygon(polygons);
            }
        }
    }

    /**
     * Merges a sequence of points or polylines into a single polyline instances.
     * Each previous polyline will be a separated path in the new polyline instances.
     * The implementation returned by this method is an instance of {@link #rootClass}.
     *
     * <p>Contrarily to other methods in this class, this method does <strong>not</strong> unwrap
     * the geometries contained in {@link GeometryWrapper}. It is caller responsibility to do so
     * if needed.</p>
     *
     * @param  paths  the points or polylines to merge in a single polyline object.
     * @return the merged polyline, or {@code null} if the given iterator has no element.
     * @throws ClassCastException if collection elements are not instances of a supported library,
     *         or not all elements are instances of the same library.
     */
    public static Object mergePolylines(final Iterator<?> paths) {
        while (paths.hasNext()) {
            final Object first = paths.next();
            if (first != null) {
                final Optional<GeometryWrapper<?>> w = wrap(first);
                if (w.isPresent()) return w.get().mergePolylines(paths);
                /*
                 * Use the same exception type than `mergePolylines(…)` implementations.
                 * Also the same type than exception occurring elsewhere in the code of
                 * the caller (GroupAsPolylineOperation).
                 */
                throw new ClassCastException(Errors.format(Errors.Keys.UnsupportedType_1, Classes.getClass(first)));
            }
        }
        return null;
    }

    /**
     * Creates a wrapper for the given geometry instance.
     * The given object shall be an instance of {@link #rootClass} or {@link #pointClass}.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     *
     * @see #wrap(Object)
     */
    protected abstract GeometryWrapper<G> createWrapper(Object geometry);

    /**
     * Returns an error message for an unsupported operation. This error message is used by non-abstract methods
     * in {@code Geometries} subclasses, after we identified the geometry library implementation to use but that
     * library does not provided the required functionality.
     *
     * @param  operation  name of the unsupported operation.
     * @return error message to put in the exception to be thrown.
     */
    protected static String unsupported(final String operation) {
        return Errors.format(Errors.Keys.UnsupportedOperation_1, operation);
    }

    /**
     * Returns an error message for an unsupported number of dimensions in a geometry object.
     *
     * @param  dimension  number of dimensions (2 or 3) requested for the geometry object.
     * @return error message to put in the exception to be thrown.
     */
    protected static String unsupported(final int dimension) {
        return Resources.format(Resources.Keys.UnsupportedGeometryObject_1, dimension);
    }
}
