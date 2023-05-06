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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Logger;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.math.Vector;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.resources.Errors;


/**
 * Utility methods on geometric objects defined in libraries outside Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to ESRI's API or to Java Topology Suite (JTS) API.
 * This gives us a single place to review if we want to support different geometry libraries,
 * or if Apache SIS come with its own implementation.
 *
 * <h2>Serialization</h2>
 * All fields except {@link #library} should be declared {@code transient}.
 * Deserialized {@code Geometries} instances shall be replaced by a unique instance,
 * which is given by {@link #readResolve()}.
 *
 * @param   <G>  the base class of all geometry objects (except point in some implementations).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.4
 * @since   0.7
 */
public abstract class Geometries<G> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1856503921463395122L;

    /**
     * The logger for operations on geometries.
     */
    public static final Logger LOGGER = Logger.getLogger(Loggers.GEOMETRY);

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
    public final transient Class<G> rootClass;

    /**
     * The class for points.
     * This is often a subclass of {@link #rootClass} but not necessarily.
     */
    public final transient Class<?> pointClass;

    /**
     * The class for polylines and polygons.
     */
    public final transient Class<? extends G> polylineClass, polygonClass;

    /**
     * The fallback implementation to use if the default one is not available.
     * This is set by {@link GeometryFactories} and should not change after initialization.
     * We do not synchronize accesses to this field because we keep it stable after
     * {@link GeometryFactories} class initialization.
     */
    transient Geometries<?> fallback;

    /**
     * {@code true} if {@link #pointClass} is not a subtype of {@link #rootClass}.
     * This is true for Java2D and false for JTS and ESRI libraries.
     */
    private final transient boolean isPointClassDistinct;

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
     * If the given type is not recognized, then this method returns {@code null}.
     *
     * @param  type  the type for which to get a geometry factory.
     * @return a geometry factory compatible with the given type if possible, or {@code null} otherwise.
     */
    public static Geometries<?> implementation(final Class<?> type) {
        for (Geometries<?> g = GeometryFactories.implementation; g != null; g = g.fallback) {
            if (g.isSupportedType(type)) return g;
        }
        return null;
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
     * Returns the geometry class of the given instance.
     *
     * @param  type  type of geometry for which the class is desired.
     * @return implementation class for the geometry of the specified type.
     */
    public Class<?> getGeometryClass(final GeometryType type) {
        switch (type) {
            default:         return rootClass;
            case POINT:      return pointClass;
            case LINESTRING: return polylineClass;
            case POLYGON:    return polygonClass;
        }
    }

    /**
     * Wraps the given geometry implementation if recognized.
     * If the given object is already an instance of {@link GeometryWrapper}, then it is returned as-is.
     * If the given object is not recognized, then this method returns an empty value.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or empty value.
     *
     * @see #castOrWrap(Object)
     */
    public static Optional<GeometryWrapper<?>> wrap(final Object geometry) {
        if (geometry != null) {
            if (geometry instanceof GeometryWrapper<?>) {
                return Optional.of((GeometryWrapper<?>) geometry);
            }
            for (Geometries<?> g = GeometryFactories.implementation; g != null; g = g.fallback) {
                if (g.isSupportedType(geometry.getClass())) {
                    return Optional.of(g.castOrWrap(geometry));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a wrapper for the given {@code <G>} or {@code GeometryWrapper<G>} instance.
     * The given object can be one of the following choices:
     *
     * <ul>
     *   <li>{@code null}, in which case this method returns {@code null}.</li>
     *   <li>An instance of {@code GeometryWrapper<G>}, in which case the given object is returned unchanged.
     *       Note that instances of {@code GeometryWrapper<?>} for implementations other than {@code <G>}
     *       will cause a {@link ClassCastException} to be thrown.</li>
     *   <li>An instance of {@link #rootClass} or {@link #pointClass}.</li>
     * </ul>
     *
     * This method can be used as an alternative to {@link #wrap(Object)} when the specified
     * geometry shall be an implementation of the specific {@linkplain #library}.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or {@code null} if the given object was null.
     * @throws ClassCastException if the the given object is not a wrapper or a geometry object
     *         of the implementation of the library identified by {@link #library}.
     *
     * @see #wrap(Object)
     */
    public abstract GeometryWrapper<G> castOrWrap(Object geometry);

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
     * Parses the given Well Known Text (WKT).
     *
     * @param  wkt  the WKT to parse. Cannot be null.
     * @return the geometry object for the given WKT (never {@code null}).
     * @throws Exception if the WKT cannot be parsed. The exception sub-class depends on the implementation.
     *
     * @see GeometryWrapper#formatWKT(double)
     */
    public abstract GeometryWrapper<G> parseWKT(String wkt) throws Exception;

    /**
     * Reads the given bytes as a Well Known Binary (WKB) encoded geometry.
     * Whether this method changes the buffer position or not is implementation-dependent.
     *
     * @param  data  the binary data in WKB format. Cannot be null.
     * @return decoded geometry (never {@code null}).
     * @throws Exception if the WKB cannot be parsed. The exception sub-class depends on the implementation.
     */
    public abstract GeometryWrapper<G> parseWKB(ByteBuffer data) throws Exception;

    /**
     * Creates and wraps a point from the given position.
     *
     * @param  point  the point to convert to a geometry.
     * @return the given point converted to a geometry.
     */
    public final GeometryWrapper<G> createPoint(final DirectPosition point) {
        final Object geometry;
        final int n = point.getDimension();
        switch (n) {
            case 2: geometry = createPoint(point.getOrdinate(0), point.getOrdinate(1)); break;
            case 3: geometry = createPoint(point.getOrdinate(0), point.getOrdinate(1), point.getOrdinate(2)); break;
            default: throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3, "point", (n <= 2) ? 2 : 3, n));
        }
        final GeometryWrapper<G> wrapper = castOrWrap(geometry);
        if (point.getCoordinateReferenceSystem() != null) {
            wrapper.setCoordinateReferenceSystem(point.getCoordinateReferenceSystem());
        }
        return wrapper;
    }

    /**
     * Returns whether this library can produce geometry backed by the {@code float} primitive type
     * instead of the {@code double} primitive type. If single-precision mode is supported, using
     * that mode may reduce memory usage. This method is used for checking whether it is worth to
     * invoke {@link Vector#isSinglePrecision()} for example.
     *
     * @return whether the library support single-precision values.
     *
     * @see Vector#isSinglePrecision()
     */
    public boolean supportSinglePrecision() {
        return false;
    }

    /**
     * Single-precision variant of {@link #createPoint(double, double)}.
     * Default implementation delegates to the double-precision variant.
     *
     * @param  x  the first coordinate value.
     * @param  y  the second coordinate value.
     * @return the point for the given coordinate values.
     *
     * @see #supportSinglePrecision()
     */
    public Object createPoint(float x, float y) {
        return createPoint((double) x, (double) y);
    }

    /**
     * Creates a two-dimensional point from the given coordinates. If the CRS is geographic, then the
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
     * Creates a three-dimensional point from the given coordinates. If the CRS is geographic, then the
     * (x,y) values should be (longitude, latitude) for compliance with usage in ESRI and JTS libraries.
     * The returned object will be an instance of {@link #pointClass}.
     *
     * @param  x  the first coordinate value.
     * @param  y  the second coordinate value.
     * @param  z  the third coordinate value.
     * @return the point for the given coordinate values.
     *
     * @see GeometryWrapper#getPointCoordinates()
     */
    public abstract Object createPoint(double x, double y, double z);

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
     * @param  polygon      whether to return the path as a polygon instead of polyline.
     * @param  dimension    the number of dimensions ({@value #BIDIMENSIONAL} or {@value #TRIDIMENSIONAL}).
     * @param  coordinates  sequence of (x,y) or (x,y,z) tuples.
     * @return the geometric object for the given points.
     * @throws UnsupportedOperationException if the geometry library cannot create the requested path.
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
     * Creates a geometry from components.
     * The expected {@code components} type depend on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components should be implementation-specific
     *       {@code Point[]}, {@code Geometry[]}, {@code LineString[]} or {@code Polygon[]},
     *       depending on the desired target type.</li>
     *   <li>Otherwise the components should be an array or collection of {@code Point} or {@code Coordinate}
     *       instances, or some implementation-specific object such as {@code CoordinateSequence}.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws ClassCastException if the given object is not an array or a collection of supported geometry components.
     */
    public abstract GeometryWrapper<G> createFromComponents(GeometryType type, Object components);

    /**
     * Creates a polyline made of points describing a rectangle whose start point is the lower left corner.
     * The sequence of points describes each corner, going in clockwise direction and repeating the starting
     * point to properly close the ring. If wraparound may happen on at least one axis, then this method may
     * add intermediate points on the axes where the envelope crosses the axis limit.
     *
     * @param  xd      dimension of first axis.
     * @param  yd      dimension of second axis.
     * @param  expand  whether to expand the envelope to full axis range if there is a wraparound.
     * @param  addPts  whether to allow insertion of intermediate points on edges of axis domains.
     * @return a polyline made of a sequence of at least 5 points describing the given rectangle.
     */
    private GeometryWrapper<G> createGeometry2D(final Envelope envelope, final int xd, final int yd,
                                                final boolean expand, final boolean addPts)
    {
        final double  xmin, ymin, xmax, ymax;
        if (expand) {
            xmin = envelope.getMinimum(xd);
            ymin = envelope.getMinimum(yd);
            xmax = envelope.getMaximum(xd);
            ymax = envelope.getMaximum(yd);
        } else {
            final DirectPosition lc = envelope.getLowerCorner();
            final DirectPosition uc = envelope.getUpperCorner();
            xmin = lc.getOrdinate(xd);
            ymin = lc.getOrdinate(yd);
            xmax = uc.getOrdinate(xd);
            ymax = uc.getOrdinate(yd);
        }
        final double[] coordinates;
        /*
         * Find if some intermediate points need to be added. We add points only at the edges of axis domain,
         * for example at 180°E or 180°W. Furthermore, we add points only on axes having increasing values,
         * i.e. we do not add points on axes using the "end point < start point" convention.
         */
        final CoordinateReferenceSystem crs;
        if (addPts && (crs  = envelope.getCoordinateReferenceSystem()) != null) {
            final double  xminIn,  yminIn,  xmaxIn,  ymaxIn;        // Intermediate min/max.
            final boolean addXmin, addYmin, addXmax, addYmax;       // Whether to add intermediate min/max.
            int n = 5*BIDIMENSIONAL;                                // Number of coordinate values.

            final CoordinateSystem cs = crs.getCoordinateSystem();
            CoordinateSystemAxis axis = cs.getAxis(xd);
            xminIn = axis.getMinimumValue();
            xmaxIn = axis.getMaximumValue();
            axis   = cs.getAxis(yd);
            yminIn = axis.getMinimumValue();
            ymaxIn = axis.getMaximumValue();
            final boolean addX = xmin <= xmax;      // Whether we can add intermediates X/Y.
            final boolean addY = ymin <= ymax;
            if (addXmin = (addX && xminIn > xmin)) n += 2*BIDIMENSIONAL;
            if (addYmin = (addY && yminIn > ymin)) n += 2*BIDIMENSIONAL;
            if (addXmax = (addX && xmaxIn < xmax)) n += 2*BIDIMENSIONAL;
            if (addYmax = (addY && ymaxIn < ymax)) n += 2*BIDIMENSIONAL;

            int i = 0;
            coordinates = new double[n];
            /*Envelope*/ {coordinates[i++] = xmin;    coordinates[i++] = ymin;}
            if (addYmin) {coordinates[i++] = xmin;    coordinates[i++] = yminIn;}
            if (addYmax) {coordinates[i++] = xmin;    coordinates[i++] = ymaxIn;}
            /*Envelope*/ {coordinates[i++] = xmin;    coordinates[i++] = ymax;}
            if (addXmin) {coordinates[i++] = xminIn;  coordinates[i++] = ymax;}
            if (addXmax) {coordinates[i++] = xmaxIn;  coordinates[i++] = ymax;}
            /*Envelope*/ {coordinates[i++] = xmax;    coordinates[i++] = ymax;}
            if (addYmax) {coordinates[i++] = xmax;    coordinates[i++] = ymaxIn;}
            if (addYmin) {coordinates[i++] = xmax;    coordinates[i++] = yminIn;}
            /*Envelope*/ {coordinates[i++] = xmax;    coordinates[i++] = ymin;}
            if (addXmax) {coordinates[i++] = xmaxIn;  coordinates[i++] = ymin;}
            if (addXmin) {coordinates[i++] = xminIn;  coordinates[i++] = ymin;}
            /*Envelope*/ {coordinates[i++] = xmin;    coordinates[i++] = ymin;}
            assert i == n : i;
        } else {
            coordinates = new double[] {xmin, ymin,  xmin, ymax,  xmax, ymax,  xmax, ymin,  xmin, ymin};
        }
        return createWrapper(createPolyline(true, BIDIMENSIONAL, Vector.create(coordinates)));
    }

    /**
     * Transforms an envelope to a two-dimensional polygon whose start point is lower corner
     * and other points are the envelope corners in clockwise order. The specified envelope
     * should be two-dimensional (see for example {@link GeneralEnvelope#horizontal()}) but
     * the coordinates does not need to be in (longitude, latitude) order; this method will
     * preserve envelope horizontal axis order. It means that any non-2D axis will be ignored,
     * and the first horizontal axis in the envelope will be the first axis (x) in the resulting geometry.
     * To force {@link AxesConvention#RIGHT_HANDED}, should transform the bounding box before calling this method.
     *
     * @param  envelope  the envelope to convert.
     * @param  strategy  how to resolve wrap-around ambiguities on the envelope.
     * @return the envelope as a polygon, or potentially as two polygons in {@link WraparoundMethod#SPLIT} case.
     */
    public GeometryWrapper<G> toGeometry2D(final Envelope envelope, final WraparoundMethod strategy) {
        int xd = 0, yd = 1;
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final int dimension = envelope.getDimension();
        if (dimension != BIDIMENSIONAL) {
            if (dimension < BIDIMENSIONAL) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyEnvelope2D));
            }
            final CoordinateReferenceSystem crsND = crs;
            crs = CRS.getHorizontalComponent(crsND);
            if (crs == null) {
                crs = CRS.getComponentAt(crsND, 0, BIDIMENSIONAL);
            } else if (crs != crsND) {
                final CoordinateSystem csND = crsND.getCoordinateSystem();
                final CoordinateSystem cs   = crs  .getCoordinateSystem();
                xd = AxisDirections.indexOfColinear(csND, cs.getAxis(0).getDirection());
                yd = AxisDirections.indexOfColinear(csND, cs.getAxis(1).getDirection());
                if (xd == yd) yd++;    // Paranoiac check (e.g. CS with 2 temporal axes).
                /*
                 * `indexOfColinear` returns -1 if the axis has not been found, but it should never
                 * happen here because we ask for axis directions that are known to exist in the CRS.
                 */
            }
        }
        final GeometryWrapper<G> result;
        switch (strategy) {
            case NORMALIZE: {
                throw new IllegalArgumentException();
            }
            /*
             * TODO: `addPts` is `false` in all cases. We have not yet determined
             *       what could be a public API for enabling this option.
             */
            case NONE: {
                result = createGeometry2D(envelope, xd, yd, false, false);
                break;
            }
            default: {
                final GeneralEnvelope ge = new GeneralEnvelope(envelope);
                ge.normalize();
                ge.wraparound(strategy);
                result = createGeometry2D(ge, xd, yd, true, false);
                break;
            }
            case SPLIT: {
                final Envelope[] parts = AbstractEnvelope.castOrCopy(envelope).toSimpleEnvelopes();
                if (parts.length == 1) {
                    result = createGeometry2D(parts[0], xd, yd, true, false);
                    break;
                }
                @SuppressWarnings({"unchecked", "rawtypes"})
                final GeometryWrapper<G>[] polygons = new GeometryWrapper[parts.length];
                for (int i=0; i<parts.length; i++) {
                    polygons[i] = createGeometry2D(parts[i], xd, yd, true, false);
                    polygons[i].setCoordinateReferenceSystem(crs);
                }
                result = createMultiPolygon(polygons);
                break;
            }
        }
        result.setCoordinateReferenceSystem(crs);
        return result;
    }

    /**
     * Creates a wrapper for the given geometry instance.
     * The given object shall be an instance of {@link #rootClass}.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     *
     * @see #castOrWrap(Object)
     */
    protected abstract GeometryWrapper<G> createWrapper(G geometry);

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
