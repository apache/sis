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
package org.apache.sis.geometry.wrapper;

import java.io.Serializable;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Optional;
import java.util.logging.Logger;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.system.Loggers;
import org.apache.sis.math.Vector;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.geometry.MismatchedDimensionException;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;


/**
 * Utility methods on geometric objects defined in libraries outside Apache <abbr>SIS</abbr>.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to <abbr>ESRI</abbr>'s <abbr>API</abbr> or to Java Topology Suite (<abbr>JTS</abbr>) API.
 * This gives us a single place to review if we want to support different geometry libraries,
 * or if Apache SIS come with its own implementation.
 *
 * <h2>Serialization</h2>
 * All fields except {@link #library} should be declared {@code transient}.
 * Deserialized {@code Geometries} instances shall be replaced by a unique instance,
 * which is given by {@code readResolve()} methods defined in each subclass.
 *
 * @param   <G>  the base class of all geometry objects (except point in some implementations).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
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
     * The fallback implementation to use if the default one is not available.
     * This is set by {@link GeometryFactories} and should not change after initialization.
     * We do not synchronize the accesses to this field because we keep the field value stable
     * after {@link GeometryFactories} class initialization.
     *
     * <h4>Temporarily permitted change</h4>
     * {@link GeometryFactories#setStandard(Geometries)} temporarily permits a change of this field,
     * but only for {@link GeometryLibrary#GEOAPI}. This is internal API and a temporary flexibility
     * for experimenting different GeoAPI implementations.
     */
    transient Geometries<?> fallback;

    /**
     * {@code true} if {@link #pointClass} is not a subtype of {@link #rootClass}.
     * This is true for Java2D and false for <abbr>JTS</abbr> and <abbr>ESRI</abbr> libraries.
     */
    private final transient boolean isPointClassDistinct;

    /**
     * Creates a new adapter for the given root geometry class.
     *
     * @param  library     the enumeration value that identifies which geometry library is used.
     * @param  rootClass   the root geometry class.
     * @param  pointClass  the class for points.
     */
    protected Geometries(final GeometryLibrary library, final Class<G> rootClass, final Class<?> pointClass) {
        this.library         = library;
        this.rootClass       = rootClass;
        this.pointClass      = pointClass;
        isPointClassDistinct = !rootClass.isAssignableFrom(pointClass);
    }

    /**
     * Returns a factory backed by the specified geometry library implementation,
     * of the default implementation if the specified library is {@code null}.
     *
     * @param  library  the desired library, or {@code null} for the default.
     * @return the specified or the default geometry implementation (never {@code null}).
     * @throws IllegalArgumentException if a non-null library is specified but that library is not available.
     */
    public static Geometries<?> factory(final GeometryLibrary library) {
        Geometries<?> g = GeometryFactories.DEFAULT;
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
     * Returns a factory backed by the same implementation as the given type.
     * If the given type is not recognized, then this method returns {@code null}.
     *
     * @param  type  the type for which to get a geometry factory.
     * @return a geometry factory compatible with the given type if possible, or {@code null} otherwise.
     */
    public static Geometries<?> factory(final Class<?> type) {
        for (Geometries<?> g = GeometryFactories.DEFAULT; g != null; g = g.fallback) {
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
        for (Geometries<?> g = GeometryFactories.DEFAULT; g != null; g = g.fallback) {
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
     * Returns the implementation-specific geometry class for the given implementation-neutral type.
     * This is the type of instances returned by {@link GeometryWrapper#implementation()}.
     * If the type is not recognized, then {@link #rootClass} is returned (never null).
     *
     * @param  type  type of geometry for which the class is desired.
     * @return implementation class for the geometry of the specified type.
     *
     * @see #getGeometry(GeometryWrapper)
     */
    public abstract Class<?> getGeometryClass(GeometryType type);

    /**
     * Returns the implementation-neutral type for the given implementation-specific class.
     * This is the converse of {@link #getGeometryClass(GeometryType)}. The given class can
     * be an array, in which case this method returns some kind of multi-geometry type.
     *
     * @param  type  class of geometry for which the implementation-neutral type is desired.
     * @return implementation-neutral identifier for the given implementation class.
     */
    public abstract GeometryType getGeometryType(Class<?> type);

    /**
     * Returns the geometry object to return to the user in public API.
     * This is the kind of object specified by {@link GeometryLibrary}.
     * It is usually the {@linkplain GeometryWrapper#implementation() implementation},
     * unless the user has requested GeoAPI interfaces in which case this method
     * returns the wrapper directly.
     *
     * @param  wrapper  the wrapper for which to get the geometry, or {@code null}.
     * @return the geometry instance of the library requested by user, or {@code null} if the given wrapper was null.
     * @throws ClassCastException if the given wrapper is not an instance of the class expected by this factory.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     *
     * @see #getGeometryClass(GeometryType)
     * @see #implementation(Object)
     */
    public Object getGeometry(final GeometryWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }
        final Geometries<?> other = wrapper.factory();
        if (other.library != library) {
            throw new ClassCastException(Resources.format(Resources.Keys.MismatchedGeometryLibrary_2, library, other.library));
        }
        return wrapper.implementation();
    }

    /**
     * Returns the coordinate reference system of the given geometry, or {@code null} if none.
     * This is a convenience method for cases where the <abbr>CRS</abbr> is the only desired information.
     * If more information are needed, use {@link #wrap(Object)} instead.
     *
     * @param  geometry  the geometry instance (can be {@code null}).
     * @return the coordinate reference system, or {@code null}.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public static CoordinateReferenceSystem getCoordinateReferenceSystem(final Object geometry) {
        return wrap(geometry).map(GeometryWrapper::getCoordinateReferenceSystem).orElse(null);
    }

    /**
     * Wraps the geometry stored in a property of the given feature. This method should be used
     * instead of {@link #wrap(Object)} when the value come from a feature instance in order to
     * allow <abbr>SIS</abbr> to fetch a default <abbr>CRS</abbr> when the geometry object does
     * not specify the <abbr>CRS</abbr> itself.
     *
     * @param  feature   the feature from which wrap a geometry, or {@code null} if none.
     * @param  property  the name of the property from which to get the default <abbr>CRS</abbr>.
     * @return a wrapper for the geometry implementation of the given feature, or empty value.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public static Optional<GeometryWrapper> wrap(final AbstractFeature feature, final String property) {
        if (feature == null) {
            return Optional.empty();
        }
        final Optional<GeometryWrapper> value = wrap(feature.getPropertyValue(property));
        value.ifPresent((wrapper) -> {
            if (wrapper.crs == null) {
                wrapper.crs = AttributeConvention.getCRSCharacteristic(feature, property);
            }
        });
        return value;
    }

    /**
     * Wraps the default geometry of the given feature. This method should be used instead of {@link #wrap(Object)}
     * when possible because it allows <abbr>SIS</abbr> to fetch a default <abbr>CRS</abbr> when the geometry object
     * does not specify the <abbr>CRS</abbr> itself.
     *
     * @param  feature   the feature from which wrap the default geometry, or {@code null} if none.
     * @return a wrapper for the geometry implementation of the given feature, or empty value.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public static Optional<GeometryWrapper> wrap(final AbstractFeature feature) {
        return wrap(feature, AttributeConvention.GEOMETRY);
    }

    /**
     * Wraps the given geometry implementation if recognized.
     * If the given object is already an instance of {@link GeometryWrapper}, then it is returned as-is.
     * If the given object is not recognized, then this method returns an empty value.
     *
     * <h4>Recommended alternative</h4>
     * Prefers {@link #wrap(AbstractFeature)} for wrapping the default geometry of a feature instance.
     * This is preferred for allowing <abbr>SIS</abbr> to fetch the default <abbr>CRS</abbr>
     * from the feature type when that information was not present in the geometry object.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or empty value.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     *
     * @see #castOrWrap(Object)
     * @see #implementation(Object)
     */
    public static Optional<GeometryWrapper> wrap(final Object geometry) {
        if (geometry != null) {
            if (geometry instanceof GeometryWrapper) {
                return Optional.of((GeometryWrapper) geometry);
            }
            for (Geometries<?> g = GeometryFactories.DEFAULT; g != null; g = g.fallback) {
                if (g.isSupportedType(geometry.getClass())) {
                    return Optional.of(g.castOrWrap(geometry));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a wrapper for the given {@code <G>} or {@code GeometryWrapper} instance.
     * The given object can be one of the following choices:
     *
     * <ul>
     *   <li>{@code null}, in which case this method returns {@code null}.</li>
     *   <li>An instance of {@code GeometryWrapper}, in which case the given object is returned unchanged.
     *       Note that instances of {@code GeometryWrapper} for implementations other than {@code <G>}
     *       will cause a {@link ClassCastException} to be thrown.</li>
     *   <li>An instance of {@link #rootClass} or {@link #pointClass}.</li>
     * </ul>
     *
     * This method can be used as an alternative to {@link #wrap(Object)} when the specified
     * geometry shall be an implementation of the specific {@linkplain #library}.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or {@code null} if the given object was null.
     * @throws ClassCastException if the given object is not a wrapper or a geometry object
     *         of the implementation of the library identified by {@link #library}.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     *
     * @see #wrap(Object)
     */
    public abstract GeometryWrapper castOrWrap(Object geometry);

    /**
     * If the given object is an instance of {@link GeometryWrapper}, returns the wrapped geometry implementation.
     * Otherwise, returns the given geometry unchanged.
     *
     * @param  geometry  the geometry to unwrap (can be {@code null}).
     * @return the geometry implementation, or the given geometry as-is.
     *
     * @see GeometryWrapper#implementation()
     */
    protected static Object implementation(final Object geometry) {
        return (geometry instanceof GeometryWrapper) ? ((GeometryWrapper) geometry).implementation() : geometry;
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
    public abstract GeometryWrapper parseWKT(String wkt) throws Exception;

    /**
     * Reads the given bytes as a Well Known Binary (WKB) encoded geometry.
     * The reading starts from the current buffer position.
     * Whether this method changes the buffer position or not is implementation-dependent.
     *
     * @param  data  the binary data in WKB format. Cannot be null.
     * @return decoded geometry (never {@code null}).
     * @throws Exception if the WKB cannot be parsed. The exception sub-class depends on the implementation.
     */
    public abstract GeometryWrapper parseWKB(ByteBuffer data) throws Exception;

    /**
     * Returns whether the library supports the specified feature.
     * Examples are whether <var>z</var> and/or <var>m</var> coordinate values can be stored.
     *
     * @param  feature  the feature for which to get support information.
     * @return whether the given feature is supported.
     */
    public abstract boolean supports(Capability feature);

    /**
     * Creates and wraps a point from the given position.
     *
     * @param  point  the point to convert to a geometry.
     * @return the given point converted to a geometry.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public final GeometryWrapper createPoint(final DirectPosition point) {
        final Object geometry;
        final int n = point.getDimension();
        switch (n) {
            case BIDIMENSIONAL:  geometry = createPoint(point.getOrdinate(0), point.getOrdinate(1)); break;
            case TRIDIMENSIONAL: geometry = createPoint(point.getOrdinate(0), point.getOrdinate(1), point.getOrdinate(2)); break;
            default: throw new MismatchedDimensionException(
                    Errors.format(Errors.Keys.MismatchedDimension_3, "point",
                    (n <= BIDIMENSIONAL) ? BIDIMENSIONAL : TRIDIMENSIONAL, n));
        }
        final GeometryWrapper wrapper = castOrWrap(geometry);
        if (point.getCoordinateReferenceSystem() != null) {
            wrapper.setCoordinateReferenceSystem(point.getCoordinateReferenceSystem());
        }
        return wrapper;
    }

    /**
     * Single-precision variant of {@link #createPoint(double, double)}.
     * Default implementation delegates to the double-precision variant.
     *
     * @param  x  the first coordinate value.
     * @param  y  the second coordinate value.
     * @return the point for the given coordinate values.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     *
     * @see Capability#SINGLE_PRECISION
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
     * @throws BackingStoreException if the operation failed because of a checked exception.
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
     * @throws BackingStoreException if the operation failed because of a checked exception.
     *
     * @see Capability#Z_COORDINATE
     * @see GeometryWrapper#getPointCoordinates()
     */
    public abstract Object createPoint(double x, double y, double z);

    /**
     * Creates a single point from the given coordinates with the given dimensions.
     * The {@code isFloat} argument may be ignored if the geometry implementations
     * does not support single-precision floating point numbers. The created point
     * may contain less dimensions if the given {@code dimensions} is not supported.
     * However, the buffer position is always advanced by {@code dimensions.count}.
     *
     * @param  isFloat      whether single-precision instead of double-precision floating point numbers.
     * @param  dimensions   the dimensions of the coordinate tuple.
     * @param  coordinates  a (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuple.
     * @return the point for the given coordinate values.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public abstract Object createPoint(boolean isFloat, Dimensions dimensions, DoubleBuffer coordinates);

    /**
     * Creates a collection of points from the given coordinate values.
     * The buffer position is advanced by {@code dimensions.count} × the number of points.
     *
     * <h4>Memory safety</h4>
     * The given buffer may be wrapping native memory, or may be a temporary buffer to be
     * reused for more data after this method call. This method shall copy the coordinate
     * values and shall not keep a reference to the buffer.
     *
     * @param  isFloat      whether to cast and store numbers to single-precision.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     * @return the collection of points for the given points.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public abstract G createMultiPoint(boolean isFloat, Dimensions dimensions, DoubleBuffer coordinates);

    /**
     * Creates a path, polyline or polygon from the given coordinate values.
     * The array of coordinate values will be handled as if all vectors were
     * concatenated in a single vector, ignoring {@code null} array elements.
     * Each {@link Double#NaN} coordinate value in the concatenated buffer starts a new path.
     * The implementation returned by this method is an instance of {@link #rootClass}.
     *
     * <p>If the {@code polygon} argument is {@code true}, then the coordinates should
     * make a closed line (e.g: a linear ring), otherwise an exception is thrown.
     *
     * <h4>Memory safety</h4>
     * The given buffer may be wrapping native memory, or may be a temporary buffer to be
     * reused for more data after this method call. This method shall copy the coordinate
     * values and shall not keep a reference to the buffer.
     *
     * @param  polygon      whether to return the path as a polygon instead of polyline.
     * @param  isFloat      whether to cast and store numbers to single-precision.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     * @return the geometric object for the given points.
     * @throws UnsupportedOperationException if the geometry library cannot create the requested collection.
     * @throws IllegalArgumentException if a polygon was requested but the given coordinates do not make
     *         a closed shape (linear ring).
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public abstract G createPolyline(boolean polygon, boolean isFloat, Dimensions dimensions, DoubleBuffer... coordinates);

    /**
     * Creates a path, polyline or polygon from the given coordinate values as vectors of arbitrary type.
     * This method converts the vectors to {@link DoubleBuffer} (may involve a copy to a temporary array),
     * check if single-precision would be sufficient, then delegates to the abstract method.
     *
     * @param  polygon      whether to return the path as a polygon instead of polyline.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuples.
     * @return the geometric object for the given points.
     * @throws UnsupportedOperationException if the geometry library cannot create the requested collection.
     * @throws IllegalArgumentException if a polygon was requested but the given coordinates do not make
     *         a closed shape (linear ring).
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public final G createPolyline(final boolean polygon, final Dimensions dimensions, final Vector... coordinates) {
        boolean isFloat = true;
        final var buffers = new DoubleBuffer[coordinates.length];
        for (int i=0; i<coordinates.length; i++) {
            final Vector v = coordinates[i];
            if (v != null) {
                final Buffer b = v.buffer().orElse(null);
                if (b instanceof DoubleBuffer) {
                    buffers[i] = (DoubleBuffer) b;
                } else {
                    buffers[i] = DoubleBuffer.wrap(v.doubleValues());
                }
                if (isFloat) {
                    isFloat = v.isSinglePrecision();
                }
            }
        }
        return createPolyline(polygon, isFloat, dimensions, buffers);
    }

    /**
     * Creates a multi-polygon from an array of geometries (polygons or linear rings).
     * Callers must ensure that the given objects are instances of geometric classes
     * of the underlying library.
     *
     * If some geometries are actually line strings, current behavior is not well defined.
     * Some implementations may convert polylines to polygons but this is not guaranteed.
     *
     * @param  geometries  the polygons or linear rings to put in a multi-polygons.
     * @return the multi-polygon.
     * @throws ClassCastException if an element in the array is not an implementation of backing library.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     *
     * @todo Consider a more general method creating a multi-polygon or multi-line depending on object types,
     *       or returning a more primitive geometry type if the given array contains only one element.
     *       We may want to return null if the array is empty (to be decided later).
     */
    public abstract GeometryWrapper createMultiPolygon(Object[] geometries);

    /**
     * Creates a geometry from components.
     * The expected {@code components} type depends on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components shall be implementation-specific
     *       {@code Point[]}, {@code Geometry[]}, {@code LineString[]} or {@code Polygon[]} array,
     *       depending on the desired target type.</li>
     *   <li>Otherwise, if {@code type} is {@link GeometryType#POLYGON}, then the components shall be an
     *       implementation-specific {@link LineString[]} with the first ring taken as the shell and all
     *       other rings (if any) taken as holes.</li>
     *   <li>Otherwise, the components shall be an array or collection of {@code Point} or {@code Coordinate}
     *       instances, or some implementation-specific object such as {@code CoordinateSequence}.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws IllegalArgumentException if the given geometry type is not supported.
     * @throws ClassCastException if {@code components} is not an array or a collection of supported geometry components.
     * @throws ArrayStoreException if {@code components} is an array with invalid component type.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public abstract GeometryWrapper createFromComponents(GeometryType type, Object components);

    /**
     * Creates a geometry from components with a type inferred from the component class.
     *
     * @param  components  the components.
     * @return geometry built from the given components.
     * @throws IllegalArgumentException if the library has no specific type for the given components.
     * @throws ClassCastException if {@code components} is not an array or a collection of supported geometry components.
     * @throws ArrayStoreException if {@code components} is an array with invalid component type.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    protected final GeometryWrapper createFromComponents(final Object components) {
        final Class<?> c = components.getClass();
        final GeometryType type = getGeometryType(c);
        if (type != GeometryType.GEOMETRY) {
            return createFromComponents(type, components);
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, c));
    }

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
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    private GeometryWrapper createGeometry2D(final Envelope envelope, final int xd, final int yd,
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
        return createWrapper(createPolyline(true, false, Dimensions.XY, DoubleBuffer.wrap(coordinates)));
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
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public GeometryWrapper toGeometry2D(final Envelope envelope, final WraparoundMethod strategy) {
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
        final GeometryWrapper result;
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
                final var ge = new GeneralEnvelope(envelope);
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
                final var polygons = new GeometryWrapper[parts.length];
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
     * @throws BackingStoreException if the operation failed because of a checked exception.
     *
     * @see #castOrWrap(Object)
     */
    protected abstract GeometryWrapper createWrapper(G geometry);

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
}
