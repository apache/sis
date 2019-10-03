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
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.math.Vector;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.logging.Logging;


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

    public static Optional<Geometry> toGeometry(final Envelope env, WrapResolution wraparound) {
        return findStrategy(g -> g.tryConvertToGeometry(env, wraparound))
                .map(result -> new GeometryWrapper(result, env));
    }

    abstract Object tryConvertToGeometry(final Envelope env, WrapResolution wraparound);

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
     * @see #createPoint(double, double)
     */
    public static double[] getCoordinate(final Object point) {
        return findStrategy(g -> g.tryGetCoordinate(point)).orElse(null);
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
        return findStrategy(g -> g.tryGetEnvelope(geometry)).orElse(null);
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
        return findStrategy(g -> g.tryToString(geometry)).orElse(null);
    }

    private String tryToString(Object geometry) {
        String s = tryGetLabel(geometry);
        if (s != null) {
            GeneralEnvelope env = tryGetEnvelope(geometry);
            if (env != null) {
                final String bbox = env.toString();
                s += bbox.substring(bbox.indexOf('('));
            }
        }
        return s;
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
        return findStrategy(g -> g.tryFormatWKT(geometry, flatness))
                .orElse(null);
    }

    public static Optional<?> fromWkt(String wkt) {
        return findStrategy(g -> {
            try {
                return g.parseWKT(wkt);
            } catch (Exception e) {
                throw new BackingStoreException(e);
            }
        });
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
     * @throws ClassCastException if not all elements in the given iterator are instances of the same library.
     */
    public static Object mergePolylines(final Iterator<?> paths) {
        while (paths.hasNext()) {
            final Object first = paths.next();
            if (first != null) {
                return findStrategy(g -> g.tryMergePolylines(first, paths))
                        .orElseThrow(() -> unsupported(2));
            }
        }
        return null;
    }

    /**
     * Returns an error message for an unsupported geometry object.
     *
     * @param  dimension  number of dimensions (2 or 3) requested for the geometry object.
     */
    static UnsupportedOperationException unsupported(final int dimension) {
        return new UnsupportedOperationException(Resources.format(Resources.Keys.UnsupportedGeometryObject_1, dimension));
    }

    private static <T> Optional<T> findStrategy(final Function<Geometries<?>, T> op) {
        for (Geometries<?> g = implementation; g != null; g = g.fallback) {
            final T result = op.apply(g);
            if (result != null) return Optional.of(result);
        }

        return Optional.empty();
    }

    private Object envelope2Polygon(final Envelope env, WrapResolution resolution) {
        double[] ordinates;
        double[] secondEnvelopeIfSplit = null;
        if (WrapResolution.NONE.equals(resolution)) {
            ordinates = new double[] {
                    env.getMinimum(0),
                    env.getMinimum(1),
                    env.getMaximum(0),
                    env.getMaximum(1)
            };
        } else {
            final boolean xWrap = env.getMinimum(0) > env.getMaximum(0);
            final boolean yWrap = env.getMinimum(1) > env.getMaximum(1);

            //TODO
            switch (resolution) {
                case EXPAND:
                case SPLIT:
                case CONTIGUOUS:
                default: throw new IllegalArgumentException("Unknown or unset wrap resolution: "+resolution);
            }

        }


        double minX = ordinates[0];
        double minY = ordinates[1];
        double maxX = ordinates[2];
        double maxY = ordinates[3];
        Vector[] points = {
                Vector.create(new double[]{minX, minY}),
                Vector.create(new double[]{minX, maxY}),
                Vector.create(new double[]{maxX, maxY}),
                Vector.create(new double[]{maxX, minY}),
                Vector.create(new double[]{minX, minY})
        };

        final G mainRect = createPolyline(2, points);
        if (secondEnvelopeIfSplit != null) {
            minX = secondEnvelopeIfSplit[0];
            minY = secondEnvelopeIfSplit[1];
            maxX = secondEnvelopeIfSplit[2];
            maxY = secondEnvelopeIfSplit[3];
            Vector[] points2 = {
                    Vector.create(new double[]{minX, minY}),
                    Vector.create(new double[]{minX, maxY}),
                    Vector.create(new double[]{maxX, maxY}),
                    Vector.create(new double[]{maxX, minY}),
                    Vector.create(new double[]{minX, minY})
            };
            final G secondRect = createPolyline(2, points2);
            // TODO: merge then send back
        }

        return mainRect;
    }
}
