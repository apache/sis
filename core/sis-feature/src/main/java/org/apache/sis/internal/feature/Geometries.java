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

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CoordinateSystemAxis;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.math.Vector;
import org.apache.sis.referencing.CRS;
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

    /**
     * Transforms an envelope to a polygon whose start point is lower corner, and points composing result are the
     * envelope corners in clockwise order.
     * @param env The envelope to convert.
     * @param wraparound How to resolve wrap-around ambiguities on the envelope.
     * @return If any geometric implementation is installed, return a polygon (or two polygons in case of
     * {@link WrapResolution#SPLIT split handling of wrap-around}.
     */
    public static Optional<Geometry> toGeometry(final Envelope env, WrapResolution wraparound) {
        return findStrategy(g -> g.tryConvertToGeometry(env, wraparound))
                .map(result -> new GeometryWrapper(result, env));
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

    /**
     * See {@link Geometries#toGeometry(Envelope, WrapResolution)}.
     */
    Object tryConvertToGeometry(final Envelope env, WrapResolution resolution) {
        // Ensure that we can isolate an horizontal part in the given envelope.
        final int x;
        if (env.getDimension() == 2) {
            x = 0;
        } else {
            final CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
            if (crs == null) throw new IllegalArgumentException("Envelope with more than 2 dimensions, but without CRS: cannot isolate horizontal part.");
            final SingleCRS hCrs = CRS.getHorizontalComponent(crs);
            if (hCrs == null) throw new IllegalArgumentException("Cannot find an horizontal part in given CRS");
            x = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), hCrs.getCoordinateSystem());
        }

        final int y = x+1;

        final DirectPosition lc = env.getLowerCorner();
        final DirectPosition uc = env.getUpperCorner();
        double minX = lc.getOrdinate(x);
        double minY = lc.getOrdinate(y);
        double maxX = uc.getOrdinate(x);
        double maxY = uc.getOrdinate(y);
        double[] splittedLeft = null;
        // We start by short-circuiting simplest case for minor simplicity/performance reason.
        if (!WrapResolution.NONE.equals(resolution)) {
            // ensure the envelope is correctly defined, by forcing non-authorized wrapped axes to take entire crs span.
            final GeneralEnvelope fixedEnv = new GeneralEnvelope(env);
            fixedEnv.normalize();
            int wrapAxis = -1;
            for (int i = x ; i <= y && wrapAxis < x ; i++) {
                if (fixedEnv.getLower(i) > fixedEnv.getUpper(i)) wrapAxis = i;
            }
            if (wrapAxis >= x) {
                final CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                if (crs == null) throw new IllegalArgumentException("Cannot resolve wrap-around for an envelope without any system defined");
                final CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(wrapAxis);
                final double wrapRange = axis.getMaximumValue() - axis.getMinimumValue();
                switch (resolution) {
                    case EXPAND:
                        // simpler and more performant than a call to GeneralEnvelope.simplify()
                        if (wrapAxis == x) {
                            minX = axis.getMinimumValue();
                            maxX = axis.getMaximumValue();
                        } else {
                            minY = axis.getMinimumValue();
                            maxY = axis.getMaximumValue();
                        }
                        break;
                    case SPLIT:
                        if (wrapAxis == x) {
                            splittedLeft = new double[]{axis.getMinimumValue(), minY, maxX, maxY};
                            maxX = axis.getMaximumValue();
                        }
                        else {
                            splittedLeft = new double[] {minX, axis.getMinimumValue(), maxX, maxY};
                            maxY = axis.getMaximumValue();
                        }
                        break;
                    case CONTIGUOUS:
                        if (wrapAxis == x) maxX += wrapRange;
                        else maxY += wrapRange;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown or unset wrap resolution: " + resolution);
                }
            }
        }

        Vector[] points = clockwiseRing(minX, minY, maxX, maxY);

        final G mainRect = createPolyline(2, points);
        if (splittedLeft != null) {
            minX = splittedLeft[0];
            minY = splittedLeft[1];
            maxX = splittedLeft[2];
            maxY = splittedLeft[3];
            Vector[] points2 = clockwiseRing(minX, minY, maxX, maxY);
            final G secondRect = createPolyline(2, points2);
            return createMultiPolygonImpl(mainRect, secondRect);
        }

        /* Geotk original method had an option to insert a median point on wrappped around axis, but we have not ported
         * it, because in an orthonormal space, I don't see any case where it could be useful. However, in case it
         * have to be added, we can do it here by amending created ring(s).
         */
        return mainRect;
    }

    /**
     * Create a sequence of points describing a rectangle whose start point is the lower left one. The sequence of
     * points describe each corner, going in clockwise order and repeating starting point to properly close the ring.
     *
     * @param minX Lower coordinate of first axis.
     * @param minY Lower coordinate of second axis.
     * @param maxX Upper coordinate of first axis.
     * @param maxY Upper coordinate of second axis.
     *
     * @return A set of 5 points describing given rectangle.
     */
    private static Vector[] clockwiseRing(final double minX, final double minY, final double maxX, final double maxY) {
        return new Vector[]{
                Vector.create(new double[]{minX, minY}),
                Vector.create(new double[]{minX, maxY}),
                Vector.create(new double[]{maxX, maxY}),
                Vector.create(new double[]{maxX, minY}),
                Vector.create(new double[]{minX, minY})
        };
    }

    /**
     * Extract all points from input geometry, and return them as a contiguous set of ordinates.
     * For rings, point order (clockwise/counter-clockwise) is implementation dependant.
     *
     * @param geometry The geometry to extract point from.
     */
    public static Optional<double[]> getOrdinates(Geometry geometry) {
        return findStrategy(g -> g.getPoints(geometry));
    }

    abstract double[] getPoints(Object geometry);

    abstract Object createMultiPolygonImpl(final Object... polygonsOrLinearRings);

    public static Object createMultiPolygon(final Object... polygonsOrLinearRings) {
        return findStrategy(g -> g.createMultiPolygonImpl(polygonsOrLinearRings));
    }
}
