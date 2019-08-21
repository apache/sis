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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.util.Classes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.math.Vector;


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
 * @version 1.0
 * @since   0.7
 * @module
 */
public abstract class Geometries<G> {
    /*
     * Registers all supported library implementations. Those libraries are optional
     * (users will typically put at most one on their classpath).
     */
    static {
        register("Java2D");
        register("JTS");
        register("ESRI");       // Default implementation if other libraries are also present.
    }

    /**
     * The enumeration that identifies the geometry library used.
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
     * The default geometry implementation to use. Unmodifiable after class initialization.
     */
    private static Geometries<?> implementation;

    /**
     * The fallback implementation to use if the default one is not available.
     */
    private final Geometries<?> fallback;

    /**
     * Creates a new adapter for the given root geometry class.
     */
    Geometries(final GeometryLibrary library, final Class<G> rootClass, final Class<?> pointClass,
            final Class<? extends G> polylineClass, final Class<? extends G> polygonClass)
    {
        this.library       = library;
        this.rootClass     = rootClass;
        this.pointClass    = pointClass;
        this.polylineClass = polylineClass;
        this.polygonClass  = polygonClass;
        this.fallback      = implementation;
    }

    /**
     * Registers the library implementation of the given name (JTS or ESRI) if present; ignore otherwise.
     * The given name shall be the simple name of a {@code Geometries} subclass in the same package.
     * The last registered library will be the default implementation.
     */
    private static void register(final String name) {
        String classname = Geometries.class.getName();
        classname = classname.substring(0, classname.lastIndexOf('.')+1).concat(name);
        try {
            implementation = (Geometries) Class.forName(classname).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            LogRecord record = Resources.forLocale(null).getLogRecord(Level.CONFIG,
                    Resources.Keys.OptionalLibraryNotFound_2, name, e.toString());
            record.setLoggerName(Loggers.GEOMETRY);
            Logging.log(Geometries.class, "register", record);
        }
    }

    /**
     * Returns an accessor to the default geometry library implementation in use.
     *
     * @param  library  the required library, or {@code null} for the default.
     * @return the default geometry implementation.
     * @throws IllegalArgumentException if the given library is non-null but not available.
     */
    public static Geometries<?> implementation(final GeometryLibrary library) {
        if (library == null) {
            return implementation;
        }
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            if (g.library == library) return g;
        }
        throw new IllegalArgumentException(Resources.format(Resources.Keys.UnavailableGeometryLibrary_1, library));
    }

    /**
     * Returns an accessor to the library implementation for a geometry of the given type.
     * If the given type is not recognized, then this method returns the default library.
     *
     * @param  type  the type to verify.
     * @return a geometry implementation compatible with the given type.
     */
    public static Geometries<?> implementation(final Class<?> type) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            if (g.rootClass.isAssignableFrom(type)) return g;
        }
        return implementation;
    }

    /**
     * Returns {@code true} if the given type is one of the types known to Apache SIS.
     *
     * @param  type  the type to verify.
     * @return {@code true} if the given type is one of the geometry type known to SIS.
     */
    public static boolean isKnownType(final Class<?> type) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            if (g.rootClass.isAssignableFrom(type)) return true;
        }
        return false;
    }

    /**
     * If the given geometry is an implementation of this library, returns its coordinate reference system.
     * Otherwise returns {@code null}. The default implementation returns {@code null} because only a few
     * geometry implementations can store CRS information.
     *
     * @see #tryTransform(Object, CoordinateOperation, CoordinateReferenceSystem)
     */
    CoordinateReferenceSystem tryGetCoordinateReferenceSystem(Object point) throws FactoryException {
        return null;
    }

    /**
     * Gets the Coordinate Reference System (CRS) from the given geometry. If no CRS information is found or
     * if the geometry implementation can not store this information, then this method returns {@code null}.
     *
     * @param  geometry the geometry from which to get the CRS, or {@code null}.
     * @return the coordinate reference system, or {@code null}.
     * @throws FactoryException if the CRS is defined by a SRID code and that code can not be used.
     *
     * @see #transform(Object, CoordinateReferenceSystem)
     */
    public static CoordinateReferenceSystem getCoordinateReferenceSystem(final Object geometry) throws FactoryException {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            CoordinateReferenceSystem crs = g.tryGetCoordinateReferenceSystem(geometry);
            if (crs != null) return crs;
        }
        return null;
    }

    /**
     * If the given point is an implementation of this library, returns its coordinate.
     * Otherwise returns {@code null}.
     */
    abstract double[] tryGetCoordinate(Object point);

    /**
     * If the given object is one of the recognized point implementation, returns its coordinate.
     * Otherwise returns {@code null}. If non-null, the returned array may have a length of 2 or 3.
     * If the CRS is geographic, then the (x,y) values should be (longitude, latitude) for compliance
     * with usage in ESRI and JTS libraries.
     *
     * @param  point  the point from which to get the coordinate, or {@code null}.
     * @return the coordinate of the given point as an array of length 2 or 3,
     *         or {@code null} if the given object is not a recognized implementation.
     *
     * @see #getCoordinateReferenceSystem(Object)
     * @see #createPoint(double, double)
     */
    public static double[] getCoordinate(final Object point) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            double[] coord = g.tryGetCoordinate(point);
            if (coord != null) return coord;
        }
        return null;
    }

    /**
     * If the given geometry is the type supported by this {@code Geometries} instance,
     * returns its envelope if non-empty. Otherwise returns {@code null}. We currently
     * do not distinguish the reasons why this method may return null.
     */
    abstract GeneralEnvelope tryGetEnvelope(Object geometry);

    /**
     * If the given object is one of the recognized types and its envelope is non-empty,
     * returns that envelope as an Apache SIS implementation. Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get the envelope, or {@code null}.
     * @return the envelope of the given geometry, or {@code null} if the given object
     *         is not a recognized geometry or its envelope is empty.
     */
    public static GeneralEnvelope getEnvelope(final Object geometry) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            GeneralEnvelope env = g.tryGetEnvelope(geometry);
            if (env != null) return env;
        }
        return null;
    }

    /**
     * If the given geometry is the type supported by this {@code Geometries} instance, returns its
     * centroid or center as a point instance of the same library. Otherwise returns {@code null}.
     */
    abstract Object tryGetCentroid(Object geometry);

    /**
     * If the given object is one of the recognized types, returns its mathematical centroid
     * (if possible) or center (as a fallback) as a point instance of the same library.
     * Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get the centroid, or {@code null}.
     * @return the centroid of the given geometry, or {@code null} if the given object
     *         is not a recognized geometry.
     */
    public static Object getCentroid(final Object geometry) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            Object center = g.tryGetCentroid(geometry);
            if (center != null) return center;
        }
        return null;
    }

    /**
     * If the given geometry is the type supported by this {@code Geometries} instance,
     * returns a short string representation of the class name. Otherwise returns {@code null}.
     */
    abstract String tryGetLabel(Object geometry);

    /**
     * If the given object is one of the recognized types, returns a short string representation
     * (typically the class name and the bounds). Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get a string representation, or {@code null}.
     * @return a short string representation of the given geometry, or {@code null} if the given
     *         object is not a recognized geometry.
     */
    public static String toString(final Object geometry) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            String s = g.tryGetLabel(geometry);
            if (s != null) {
                GeneralEnvelope env = g.tryGetEnvelope(geometry);
                if (env != null) {
                    final String bbox = env.toString();
                    s += bbox.substring(bbox.indexOf('('));
                }
                return s;
            }
        }
        return null;
    }

    /**
     * If the given object is one of the recognized types, formats that object in Well Known Text (WKT).
     * Otherwise returns {@code null}. If the geometry contains curves, then the {@code flatness} parameter
     * specifies the maximum distance that the line segments used in the Well Known Text are allowed to deviate
     * from any point on the original curve. This parameter is ignored if the geometry does not contain curves.
     *
     * @param  geometry  the geometry to format in Well Known Text.
     * @param  flatness  maximal distance between the approximated WKT and any point on the curve.
     * @return the Well Known Text for the given geometry, or {@code null} if the given object is unrecognized.
     */
    public static String formatWKT(Object geometry, double flatness) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            String wkt = g.tryFormatWKT(geometry, flatness);
            if (wkt != null) return wkt;
        }
        return null;
    }

    /**
     * If the given geometry is the type supported by this {@code Geometries} instance,
     * returns its WKT representation. Otherwise returns {@code null}.
     */
    abstract String tryFormatWKT(Object geometry, double flatness);

    /**
     * Parses the given WKT.
     *
     * @param  wkt  the WKT to parse.
     * @return the geometry object for the given WKT.
     * @throws Exception if the WKT can not be parsed. The exception sub-class depends on the implementation.
     */
    public abstract Object parseWKT(String wkt) throws Exception;

    /**
     * Creates a two-dimensional point from the given coordinate. If the CRS is geographic, then the
     * (x,y) values should be (longitude, latitude) for compliance with usage in ESRI and JTS libraries.
     *
     * @param  x  the first coordinate value.
     * @param  y  the second coordinate value.
     * @return the point for the given coordinate values.
     *
     * @see #getCoordinate(Object)
     */
    public abstract Object createPoint(double x, double y);

    /**
     * Creates a path or polyline from the given coordinate values.
     * The array of coordinate values will be handled as if all vectors were concatenated in a single vector,
     * ignoring {@code null} array elements.
     * Each {@link Double#NaN} coordinate value in the concatenated vector starts a new path.
     * The implementation returned by this method is an instance of {@link #rootClass}.
     *
     * @param  dimension    the number of dimensions (2 or 3).
     * @param  coordinates  sequence of (x,y) or (x,y,z) tuples.
     * @return the geometric object for the given points.
     * @throws UnsupportedOperationException if the geometry library can not create the requested path.
     */
    public abstract G createPolyline(int dimension, Vector... coordinates);

    /**
     * Merges a sequence of polyline instances if the first instance is an implementation of this library.
     *
     * @param  first      the first instance to merge.
     * @param  polylines  the second and subsequent instances to merge.
     * @return the merged polyline, or {@code null} if the first instance is not an implementation of this library.
     * @throws ClassCastException if an element in the iterator is not an implementation of this library.
     */
    abstract G tryMergePolylines(Object first, Iterator<?> polylines);

    /**
     * Merges a sequence of points or polylines into a single polyline instances.
     * Each previous polyline will be a separated path in the new polyline instances.
     * The implementation returned by this method is an instance of {@link #rootClass}.
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
                for (Geometries<?> g = implementation; g != null; g = g.fallback) {
                    final Object merged = g.tryMergePolylines(first, paths);
                    if (merged != null) {
                        return merged;
                    }
                }
                /*
                 * Use the same exception type than `tryMergePolylines(…)` implementations.
                 * Also the same type than exception occurring elsewhere in the code of the
                 * caller (GroupAsPolylineOperation).
                 */
                throw new ClassCastException(unsupportedImplementation(first));
            }
        }
        return null;
    }

    /**
     * If the given geometry is the type supported by this {@code Geometries} instance, computes
     * its buffer as a geometry instance of the same library. Otherwise returns {@code null}.
     */
    Object tryBuffer(Object geometry, double distance) {
        if (rootClass.isInstance(geometry)) {
            throw new UnsupportedImplementationException(unsupported("buffer"));
        }
        return null;
    }

    /**
     * If the given object is one of the recognized types, computes its buffer as a geometry instance
     * of the same library. Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to compute a buffer, or {@code null}.
     * @param  distance  the buffer distance in the CRS of the geometry object.
     * @return the buffer of the given geometry, or {@code null} if the given object is not recognized.
     */
    public static Object buffer(final Object geometry, final double distance) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            Object center = g.tryBuffer(geometry, distance);
            if (center != null) return center;
        }
        return null;
    }

    /**
     * Tries to transforms the given geometry to the specified Coordinate Reference System (CRS),
     * or returns {@code null} if this method can not perform this operation on the given object.
     * Exactly one of {@code operation} and {@code targetCRS} shall be non-null. If operation is
     * null and geometry has no Coordinate Reference System, a {@link TransformException} is thrown.
     *
     * <p>Default implementation throws {@link UnsupportedImplementationException} because current
     * Apache SIS implementation supports geometry transformations only with JTS geometries.</p>
     *
     * @param  geometry   the geometry to transform.
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     *
     * @see #tryGetCoordinateReferenceSystem(Object)
     */
    Object tryTransform(Object geometry, CoordinateOperation operation, CoordinateReferenceSystem targetCRS)
            throws FactoryException, TransformException
    {
        if (rootClass.isInstance(geometry)) {
            throw new UnsupportedImplementationException(unsupported("transform"));
        }
        return null;
    }

    /**
     * Transforms the given geometry using the given coordinate operation.
     * If the geometry or the operation is null, then the geometry is returned unchanged.
     * If the geometry uses a different CRS than the source CRS of the given operation,
     * then a new operation to the target CRS will be used.
     * If the given object is not a known implementation, then this method returns {@code null}.
     *
     * <p>This method is preferred to {@link #transform(Object, CoordinateReferenceSystem)}
     * when possible because not all geometry libraries store the CRS of their objects.</p>
     *
     * @param  geometry   the geometry to transform, or {@code null}.
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance), or {@code null}.
     * @throws UnsupportedImplementationException if this operation is not supported for the given geometry.
     * @throws FactoryException if transformation to the target CRS can not be constructed.
     * @throws TransformException if the given geometry can not be transformed.
     */
    public static Object transform(final Object geometry, final CoordinateOperation operation)
            throws FactoryException, TransformException
    {
        /*
         * Do NOT check MathTransform.isIdentity() below because the source CRS may not match
         * the geometry CRS. This verification will be done by tryTransform(…) implementation.
         */
        if (geometry != null && operation != null) {
            for (Geometries<?> g = implementation; g != null; g = g.fallback) {
                final Object result = g.tryTransform(geometry, operation, null);
                if (result != null) {
                    return result;
                }
            }
            if (!operation.getMathTransform().isIdentity()) {
                return null;
            }
        }
        return geometry;
    }

    /**
     * Transforms the given geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS or the given geometry is null, the geometry is returned unchanged.
     * If the geometry has no Coordinate Reference System, a {@link TransformException} is thrown.
     * If the given object is not a known implementation, then this method returns {@code null}.
     *
     * <p>Consider using {@link #transform(Object, CoordinateOperation)} instead of this method
     * as much as possible, both for performance reasons and because not all geometry libraries
     * provide information about the CRS of their geometries.</p>
     *
     * @param  geometry   the geometry to transform, or {@code null}.
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance), or {@code null}.
     * @throws UnsupportedImplementationException if this operation is not supported for the given geometry.
     * @throws FactoryException if transformation to the target CRS can not be constructed.
     * @throws TransformException if the given geometry has no CRS or can not be transformed.
     *
     * @see #getCoordinateReferenceSystem(Object)
     */
    public static Object transform(final Object geometry, final CoordinateReferenceSystem targetCRS)
            throws FactoryException, TransformException
    {
        if (geometry != null && targetCRS != null) {
            for (Geometries<?> g = implementation; g != null; g = g.fallback) {
                final Object result = g.tryTransform(geometry, null, targetCRS);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
        return geometry;
    }

    /**
     * Returns an error message for an unsupported operation. This error message is used by non-abstract methods
     * in {@code Geometries} subclasses, after we identified the geometry library implementation to use but that
     * library does not provided the required functionality.
     */
    static String unsupported(final String operation) {
        return Errors.format(Errors.Keys.UnsupportedOperation_1, operation);
    }

    /**
     * Returns an error message for an unsupported number of dimensions in a geometry object.
     *
     * @param  dimension  number of dimensions (2 or 3) requested for the geometry object.
     */
    static String unsupported(final int dimension) {
        return Resources.format(Resources.Keys.UnsupportedGeometryObject_1, dimension);
    }

    /**
     * Returns an error message for an unsupported geometry object implementation.
     *
     * @param  geometry  the unsupported object.
     */
    private static String unsupportedImplementation(final Object geometry) {
        return Errors.format(Errors.Keys.UnsupportedType_1, Classes.getClass(geometry));
    }
}
