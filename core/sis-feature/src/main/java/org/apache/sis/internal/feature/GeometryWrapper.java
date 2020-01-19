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

import java.util.Set;
import java.util.Objects;
import java.util.Iterator;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.Boundary;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.TransfiniteSet;
import org.opengis.geometry.complex.Complex;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;


/**
 * Wraps a JTS, ESRI or Java2D geometry behind a {@code Geometry} interface.
 * This is a temporary class to be refactored later as a more complete geometry framework.
 * The methods provided in this class are not committed API, and often not even clean API.
 * They are only utilities added for very specific Apache SIS needs and will certainly
 * change without warning in future Apache SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <G>  root class of geometry instances of the underlying library (i.e. {@link Geometries#rootClass}).
 *              This is not necessarily the class of the wrapped geometry returned by {@link #implementation()}.
 *
 * @see Geometries#wrap(Object)
 *
 * @since 0.8
 * @module
 */
public abstract class GeometryWrapper<G> implements Geometry {
    /**
     * Creates a new geometry object.
     */
    protected GeometryWrapper() {
    }

    /**
     * Returns the implementation-dependent factory of geometric objects.
     * This is typically a system-wide factory shared by all geometry instances.
     *
     * @return the factory of implementation-dependent geometric objects (never {@code null}).
     */
    public abstract Geometries<G> factory();

    /**
     * Returns the JTS, ESRI or Java2D geometry implementation wrapped by this {@code GeometryWrapper} instance.
     * This returned object will be an instance of {@link Geometries#rootClass} or {@link Geometries#pointClass}
     * (note that {@code pointClass} is not necessarily a subtype of {@code rootClass}).
     *
     * @return the geometry implementation wrapped by this instance (never {@code null}).
     */
    public abstract Object implementation();

    /**
     * Wraps the given geometry in a wrapper of the same type than this {@code GeometryWrapper}.
     * This method is the converse of {@link #implementation()}.
     *
     * @param  geometry  the geometry to wrap, or {@code null}.
     * @return the wrapped geometry, or {@code null} if the given value was null.
     * @throws ClassCastException if the given object is not an instance of expected type.
     */
    public final GeometryWrapper<G> wrap(final Object geometry) {
        return (geometry != null) ? factory().createWrapper(geometry) : null;
    }

    /**
     * Gets the Coordinate Reference System (CRS) of this geometry. In some libraries (for example JTS) the CRS
     * is stored in the {@link Geometries#rootClass} instances of that library. In other libraries (e.g. Java2D)
     * the CRS is stored only in this {@code GeometryWrapper} instance.
     *
     * @return the geometry CRS, or {@code null} if unknown.
     * @throws BackingStoreException if the CRS is defined by a SRID code and that code can not be used.
     */
    @Override
    public abstract CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Sets the coordinate reference system.
     * This method should be invoked only for newly created geometries. If the geometry library supports
     * user objects (e.g. JTS), there is no guarantees that this method will not overwrite user setting.
     *
     * @param  crs  the coordinate reference system to set.
     *
     * @see #transform(CoordinateReferenceSystem)
     */
    public abstract void setCoordinateReferenceSystem(CoordinateReferenceSystem crs);

    /**
     * Returns the geometry bounding box, together with its coordinate reference system.
     *
     * @return the geometry envelope. Should never be {@code null} except for an empty geometry or a single point.
     */
    @Override
    public abstract GeneralEnvelope getEnvelope();

    /**
     * Returns the mathematical centroid (if possible) or center (as a fallback) as a direct position.
     *
     * @return the centroid of the wrapped geometry.
     */
    @Override
    public abstract DirectPosition getCentroid();

    /**
     * Returns the mathematical centroid (if possible) or center (as a fallback) as an implementation-dependent
     * point instance. The returned object shall be an instance of {@link Geometries#pointClass}.
     *
     * @return the centroid of the wrapped geometry.
     */
    public abstract Object getCentroidImpl();

    /**
     * If the geometry implementation is a point, returns its coordinates. Otherwise returns {@code null}.
     * If non-null, the returned array may have a length of 2 or 3. If the CRS is geographic, then the (x,y)
     * values should be in (longitude, latitude) order for compliance with usage in ESRI and JTS libraries.
     *
     * @return the coordinate of the point as an array of length 2 or 3,
     *         or {@code null} if the geometry is not a point.
     *
     * @see #getCoordinateReferenceSystem()
     * @see Geometries#createPoint(double, double)
     */
    public abstract double[] getPointCoordinates();

    /**
     * Returns all geometry coordinate tuples. This method is currently used for testing purpose only because
     * it does not separate the sequence of coordinates for different polygons in a multi-polygon.
     *
     * @return the sequence of all coordinate values in the wrapped geometry,
     *         or {@code null} if they can not be obtained.
     */
    @Debug
    protected abstract double[] getAllCoordinates();

    /**
     * Appends a sequence of points or polylines after this geometry.
     * Each previous polyline will be a separated path in the new polyline instance.
     *
     * <p>The given iterator shall return instances of {@link Geometries#rootClass} or
     * {@link Geometries#pointClass}, not <strong>not</strong> {@link GeometryWrapper}
     * (it is caller responsibility to unwrap if needed).</p>
     *
     * @param  paths  the points or polylines to merge in a single polyline instance.
     * @return the merged polyline (may be the wrapper geometry but never {@code null}).
     * @throws ClassCastException if collection elements are not instances of the point or geometry class.
     */
    protected abstract G mergePolylines(final Iterator<?> paths);

    /**
     * Computes geometry buffer as a geometry instance of the same library.
     *
     * @param  distance  the buffer distance in the CRS of the geometry object.
     * @return the buffer of the given geometry (never {@code null}).
     * @throws UnsupportedOperationException if this operation can not be performed with current implementation.
     */
    public G buffer(final double distance) {
        throw new UnsupportedOperationException(Geometries.unsupported("buffer"));
    }

    /**
     * Transforms this geometry using the given coordinate operation.
     * If the operation is null, then the geometry is returned unchanged.
     * If the geometry uses a different CRS than the source CRS of the given operation,
     * then a new operation to the target CRS will be automatically computed.
     *
     * <p>This method is preferred to {@link #transform(CoordinateReferenceSystem)}
     * when possible because not all geometry libraries store the CRS of their objects.</p>
     *
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance, but never {@code null}).
     * @throws UnsupportedOperationException if this operation is not supported for current implementation.
     * @throws FactoryException if transformation to the target CRS can not be found.
     * @throws TransformException if the geometry can not be transformed.
     */
    public G transform(final CoordinateOperation operation) throws FactoryException, TransformException {
        throw new UnsupportedOperationException(Geometries.unsupported("transform"));
    }

    /**
     * Transforms this geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS is null, then the geometry is returned unchanged.
     * If the geometry has no Coordinate Reference System, a {@link TransformException} is thrown.
     *
     * <p>Consider using {@link #transform(CoordinateOperation)} instead of this method as much as possible,
     * both for performance reasons and because not all geometry libraries provide information about the CRS
     * of their geometries.</p>
     *
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry but never {@code null}).
     * @throws UnsupportedOperationException if this operation is not supported for current implementation.
     * @throws TransformException if the given geometry has no CRS or can not be transformed.
     *
     * @see #getCoordinateReferenceSystem()
     */
    @Override
    public GeometryWrapper<G> transform(final CoordinateReferenceSystem targetCRS) throws TransformException {
        throw new UnsupportedOperationException(Geometries.unsupported("transform"));
    }

    /**
     * Formats the wrapped geometry in Well Known Text (WKT).
     * If the geometry contains curves, then the {@code flatness} parameter specifies the maximum distance that
     * the line segments used in the Well Known Text are allowed to deviate from any point on the original curve.
     * This parameter is ignored if the geometry does not contain curves.
     *
     * @param  flatness  maximal distance between the approximated WKT and any point on the curve.
     * @return the Well Known Text for the wrapped geometry (never {@code null}).
     *
     * @see Geometries#parseWKT(String)
     */
    public abstract String formatWKT(double flatness);

    /**
     * Methods from the {@link Geometry} interface. The {@link Override} annotation is intentionally omitted
     * for reducing the risk of compilation failures during the upcoming revision of GeoAPI interfaces since
     * some of those methods will be removed.
     */
    @Deprecated public final Geometry       getMbRegion()                             {throw new UnsupportedOperationException();}
    @Deprecated public final DirectPosition getRepresentativePoint()                  {throw new UnsupportedOperationException();}
    @Deprecated public final Boundary       getBoundary()                             {throw new UnsupportedOperationException();}
    @Deprecated public final Complex        getClosure()                              {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        isSimple()                                {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        isCycle()                                 {throw new UnsupportedOperationException();}
    @Deprecated public final double         distance(Geometry geometry)               {throw new UnsupportedOperationException();}
    @Deprecated public final int            getDimension(DirectPosition point)        {throw new UnsupportedOperationException();}
    @Deprecated public final int            getCoordinateDimension()                  {throw new UnsupportedOperationException();}
    @Deprecated public final Set<Complex>   getMaximalComplex()                       {throw new UnsupportedOperationException();}
    @Deprecated public final Geometry       getConvexHull()                           {throw new UnsupportedOperationException();}
    @Deprecated public final Geometry       getBuffer(double distance)                {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        isMutable()                               {throw new UnsupportedOperationException();}
    @Deprecated public final Geometry       toImmutable()                             {throw new UnsupportedOperationException();}
    @Deprecated public final Geometry       clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
    @Deprecated public final boolean        contains(TransfiniteSet pointSet)         {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        contains(DirectPosition point)            {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        intersects(TransfiniteSet pointSet)       {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        equals(TransfiniteSet pointSet)           {throw new UnsupportedOperationException();}
    @Deprecated public final TransfiniteSet union(TransfiniteSet pointSet)            {throw new UnsupportedOperationException();}
    @Deprecated public final TransfiniteSet intersection(TransfiniteSet pointSet)     {throw new UnsupportedOperationException();}
    @Deprecated public final TransfiniteSet difference(TransfiniteSet pointSet)       {throw new UnsupportedOperationException();}
    @Deprecated public final TransfiniteSet symmetricDifference(TransfiniteSet ps)    {throw new UnsupportedOperationException();}
    @Deprecated public final Geometry       transform(CoordinateReferenceSystem crs, MathTransform tr) {throw new UnsupportedOperationException();}

    /**
     * Returns {@code true} if the given object is a wrapper of the same class
     * and the wrapped geometry implementations are equal.
     *
     * @param  obj  the object to compare with this wrapper.
     * @return whether the two objects are wrapping geometry implementations that are themselves equal.
     */
    @Override
    public final boolean equals(final Object obj) {
        return (obj != null) && obj.getClass().equals(getClass()) &&
                Objects.equals(((GeometryWrapper) obj).implementation(), implementation());
    }

    /**
     * Returns a hash code value based on the wrapped geometry.
     */
    @Override
    public final int hashCode() {
        return ~Objects.hashCode(implementation());
    }

    /**
     * Returns the string representation of the wrapped geometry
     * (typically the class name and the bounds).
     */
    @Override
    public final String toString() {
        /*
         * Get a short string representation of the class name, ignoring the primitive type specialization.
         * For example if the geometry class is `Rectangle2D.Float`, then get the "Rectangle2D" class name.
         */
        final Class<?> c = implementation().getClass();
        final Class<?> e = c.getEnclosingClass();
        String s = Classes.getShortName(e != null ? e : c);
        final GeneralEnvelope envelope = getEnvelope();
        if (envelope != null) {
            final String bbox = envelope.toString();
            s += bbox.substring(bbox.indexOf('('));
        }
        return s;
    }
}
