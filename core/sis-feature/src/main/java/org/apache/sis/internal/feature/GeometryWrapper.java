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
import java.util.OptionalInt;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.IncommensurableException;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.Boundary;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.TransfiniteSet;
import org.opengis.geometry.complex.Complex;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.filter.sqlmm.SQLMM;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.InvalidFilterValueException;


/**
 * Wraps a JTS, ESRI or Java2D geometry behind a {@code Geometry} interface.
 * This is a temporary class to be refactored later as a more complete geometry framework.
 * The methods provided in this class are not committed API, and often not even clean API.
 * They are only utilities added for very specific Apache SIS needs and will certainly
 * change without warning in future Apache SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <G>  root class of geometry instances of the underlying library (i.e. {@link Geometries#rootClass}).
 *              This is not necessarily the class of the wrapped geometry returned by {@link #implementation()}.
 *
 * @see Geometries#wrap(Object)
 *
 * @since 0.8
 */
public abstract class GeometryWrapper<G> extends AbstractGeometry implements Geometry {
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
     * Returns the Spatial Reference System Identifier (SRID) if available.
     * The SRID is used in database such as PostGIS and is generally database-dependent.
     * This is <em>not</em> necessarily an EPSG code, even it is common practice to use
     * the same numerical values than EPSG. Note that the absence of SRID does not mean
     * that {@link #getCoordinateReferenceSystem()} would return no CRS.
     *
     * <p>Users should invoke the {@link #getCoordinateReferenceSystem()} method instead.
     * This {@code getSRID()} method is provided for classes such as {@code DataStore} backed by an SQL database.
     * Those classes have a connection to a {@code "spatial_ref_sys} table providing the mapping from SRID codes
     * to authority codes such as EPSG. Those {@code DataStore} will typically get the SRID soon after geometry
     * creation, resolves its CRS and invoke {@link #setCoordinateReferenceSystem(CoordinateReferenceSystem)}.</p>
     *
     * @return the Spatial Reference System Identifier of the geometry.
     */
    public OptionalInt getSRID() {
        return OptionalInt.empty();
    }

    /**
     * Gets the Coordinate Reference System (CRS) of this geometry. In some libraries (for example JTS) the CRS
     * is stored in the {@link Geometries#rootClass} instances of that library. In other libraries (e.g. Java2D)
     * the CRS is stored only in this {@code GeometryWrapper} instance.
     *
     * @return the geometry CRS, or {@code null} if unknown.
     * @throws BackingStoreException if the CRS is defined by a SRID code and that code cannot be used.
     */
    @Override
    public abstract CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Sets the coordinate reference system.
     * This method should be invoked only for newly created geometries. If the geometry library supports
     * user objects (e.g. JTS), there is no guarantee that this method will not overwrite user setting.
     *
     * @param  crs  the coordinate reference system to set.
     *
     * @see #transform(CoordinateReferenceSystem)
     */
    public abstract void setCoordinateReferenceSystem(CoordinateReferenceSystem crs);

    /**
     * Returns the geometry bounding box, together with its coordinate reference system.
     *
     * @return the geometry envelope. Should never be {@code null}.
     *         Note though that for an empty geometry or a single point, the returned envelope will be empty.
     */
    @Override
    public abstract GeneralEnvelope getEnvelope();

    /**
     * Returns the mathematical centroid (if possible) or center (as a fallback) as a direct position.
     *
     * @return the centroid of the wrapped geometry.
     *
     * @todo Consider a {@code getCentroid2D()} method avoiding the cost of fetching the CRS.
     */
    @Override
    public abstract DirectPosition getCentroid();

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
     *         or {@code null} if they cannot be obtained.
     *
     * @todo Replace by a {@code toJava2D()} method returning a {@link java.awt.Shape},
     *       so we can use the path iterator instead of this array.
     */
    @Debug
    public abstract double[] getAllCoordinates();

    /**
     * Appends a sequence of points or polylines after this geometry.
     * Each previous polyline will be a separated path in the new polyline instance.
     *
     * <p>The given iterator shall return instances of {@link Geometries#rootClass} or
     * {@link Geometries#pointClass}, not <strong>not</strong> {@link GeometryWrapper}
     * (it is caller responsibility to unwrap if needed).</p>
     *
     * @param  paths  the points or polylines to merge in a single polyline instance.
     * @return the merged polyline (may be the underlying geometry of {@code this} but never {@code null}).
     * @throws ClassCastException if collection elements are not instances of the point or geometry class.
     */
    public abstract G mergePolylines(final Iterator<?> paths);

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method transforms the two geometries to the same CRS if needed.
     *
     * @param  type      the predicate operation to apply.
     * @param  other     the other geometry to test with this geometry.
     * @param  distance  the buffer distance around the geometry of the second expression.
     * @param  context   the preferred CRS and other context to use if geometry transformations are needed.
     * @return result of applying the specified predicate.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws InvalidFilterValueException if an error occurred while executing the operation on given geometries.
     */
    public final boolean predicate(final DistanceOperatorName type, final GeometryWrapper<G> other,
                                   final Quantity<Length> distance, final SpatialOperationContext context)
    {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final GeometryWrapper<G>[] geometries = new GeometryWrapper[] {this, other};
        try {
            if (context.transform(geometries)) {
                double dv = distance.getValue().doubleValue();
                final Unit<?> unit = ReferencingUtilities.getUnit(context.commonCRS);
                if (unit != null) {
                    dv = distance.getUnit().getConverterToAny(unit).convert(dv);
                }
                return geometries[0].predicateSameCRS(type, geometries[1], dv);
            }
        } catch (FactoryException | TransformException | IncommensurableException e) {
            throw new InvalidFilterValueException(e);
        }
        /*
         * No common CRS. Consider that we have no intersection, no overlap, etc.
         * since the two geometries are existing in different coordinate spaces.
         */
        return SpatialOperationContext.negativeResult(type);
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method transforms the two geometries to the same CRS if needed.
     *
     * <p><b>Note:</b> {@link SpatialOperatorName#BBOX} is implemented by {@code NOT DISJOINT}.
     * It is caller's responsibility to ensure that one of the geometries is rectangular.</p>
     *
     * @param  type     the predicate operation to apply.
     * @param  other    the other geometry to test with this geometry.
     * @param  context  the preferred CRS and other context to use if geometry transformations are needed.
     * @return result of applying the specified predicate.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws InvalidFilterValueException if an error occurred while executing the operation on given geometries.
     */
    public final boolean predicate(final SpatialOperatorName type, final GeometryWrapper<G> other,
                                   final SpatialOperationContext context)
    {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final GeometryWrapper<G>[] geometries = new GeometryWrapper[] {this, other};
        try {
            if (context.transform(geometries)) {
                return geometries[0].predicateSameCRS(type, geometries[1]);
            }
        } catch (FactoryException | TransformException | IncommensurableException e) {
            throw new InvalidFilterValueException(e);
        }
        /*
         * No common CRS. Consider that we have no intersection, no overlap, etc.
         * since the two geometries are existing in different coordinate spaces.
         */
        return SpatialOperationContext.negativeResult(type);
    }

    /**
     * Applies a SQLMM operation on this geometry.
     * This method shall be invoked only for operations without non-geometric parameters.
     *
     * @param  operation  the SQLMM operation to apply.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     */
    public final Object operation(final SQLMM operation) {
        assert operation.geometryCount() == 1 && operation.maxParamCount == 1 : operation;
        final Object result = operationSameCRS(operation, null, null);
        assert isInstance(operation, result) : result;
        return result;
    }

    /**
     * Applies a SQLMM operation on two geometries.
     * This method shall be invoked only for operations without non-geometric parameters.
     * The second geometry is transformed to the same CRS than this geometry for conformance with SQLMM standard.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry. It is caller's responsibility to check that this value is non-null.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     * @throws TransformException if it was necessary to transform the other geometry and that transformation failed.
     */
    public final Object operation(final SQLMM operation, final GeometryWrapper<G> other)
            throws TransformException
    {
        assert operation.geometryCount() == 2 && operation.maxParamCount == 2 : operation;
        final Object result = operationSameCRS(operation, toSameCRS(other), null);
        assert isInstance(operation, result) : result;
        return result;
    }

    /**
     * Applies a SQLMM operation on this geometry with one operation-specific argument.
     * The argument shall be non-null, unless the argument is optional.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  argument   an operation-specific argument.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     */
    public final Object operationWithArgument(final SQLMM operation, final Object argument) {
        assert operation.geometryCount() == 1 && operation.maxParamCount == 2 : operation;
        if (argument == null && operation.minParamCount > 1) {
            // TODO: fetch argument name.
            throw new NullArgumentException(Errors.format(Errors.Keys.NullArgument_1, "arg1"));
        }
        final Object result = operationSameCRS(operation, null, argument);
        assert isInstance(operation, result) : result;
        return result;
    }

    /**
     * Applies a SQLMM operation on two geometries with one operation-specific argument.
     * The argument shall be non-null, unless the argument is optional.
     * The second geometry is transformed to the same CRS than this geometry for conformance with SQLMM standard.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry. It is caller's responsibility to check that this value is non-null.
     * @param  argument   an operation-specific argument.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     * @throws TransformException if it was necessary to transform the other geometry and that transformation failed.
     */
    public final Object operationWithArgument(final SQLMM operation, final GeometryWrapper<G> other, final Object argument)
            throws TransformException
    {
        assert operation.geometryCount() == 2 && operation.maxParamCount == 3 : operation;
        if (argument == null && operation.minParamCount > 2) {
            // TODO: fetch argument name.
            throw new NullArgumentException(Errors.format(Errors.Keys.NullArgument_1, "arg2"));
        }
        final Object result = operationSameCRS(operation, toSameCRS(other), argument);
        assert isInstance(operation, result) : result;
        return result;
    }

    /**
     * Transforms the {@code other} geometry to the same CRS than this geometry.
     * This method should be cheap for the common case where the other geometry
     * already uses the CRS, in which case it is returned unchanged.
     *
     * <p>If this geometry does not define a CRS, then current implementation
     * returns the other geometry unchanged.</p>
     *
     * @param  other  the other geometry.
     * @return the other geometry in the same CRS than this geometry.
     * @throws TransformException if the other geometry cannot be transformed.
     *         If may be because the other geometry does not define its CRS.
     */
    private GeometryWrapper<G> toSameCRS(final GeometryWrapper<G> other) throws TransformException {
        if (isSameCRS(other)) {
            return other;
        }
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        return (crs != null) ? other.transform(crs) : this;
    }

    /**
     * Returns {@code true} if a result is of the expected type.
     * This is used for assertion purposes only.
     */
    private boolean isInstance(final SQLMM operation, final Object result) {
        return (result == null) || operation.getReturnType(factory()).isInstance(result);
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method assumes that the two geometries are in the same CRS (this is not verified).
     *
     * <p><b>Note:</b> {@link SpatialOperatorName#BBOX} is implemented by {@code NOT DISJOINT}.
     * It is caller's responsibility to ensure that one of the geometries is rectangular.</p>
     *
     * @param  type   the predicate operation to apply.
     * @param  other  the other geometry to test with this geometry.
     * @return result of applying the specified predicate.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     */
    protected boolean predicateSameCRS(final SpatialOperatorName type, final GeometryWrapper<G> other) {
        throw new UnsupportedOperationException(Geometries.unsupported(type.name()));
    }

    /**
     * Applies a filter predicate between this geometry and another geometry within a given distance.
     * This method assumes that the two geometries are in the same CRS and that the unit of measurement
     * is the same for {@code distance} than for axes (this is not verified).
     *
     * @param  type      the predicate operation to apply.
     * @param  other     the other geometry to test with this geometry.
     * @param  distance  distance to test between the geometries.
     * @return result of applying the specified predicate.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     */
    protected boolean predicateSameCRS(final DistanceOperatorName type, final GeometryWrapper<G> other, final double distance) {
        throw new UnsupportedOperationException(Geometries.unsupported(type.name()));
    }

    /**
     * Applies a SQLMM operation on this geometry.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry, or {@code null} if the operation requires only one geometry.
     * @param  argument   an operation-specific argument, or {@code null} if not applicable.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     */
    protected Object operationSameCRS(final SQLMM operation, final GeometryWrapper<G> other, final Object argument) {
        throw new UnsupportedOperationException(Geometries.unsupported(operation.name()));
    }

    /**
     * Converts the given geometry to the specified type.
     * If the geometry is already of that type, it is returned unchanged.
     * Otherwise coordinates are copied in a new geometry of the requested type.
     *
     * <p>The following conversions are illegal and will cause an {@link IllegalArgumentException} to be thrown:</p>
     * <ul>
     *   <li>From point to polyline or polygon.</li>
     *   <li>From geometry collection (except multi-point) to polyline.</li>
     *   <li>From geometry collection (except multi-point and multi-line string) to polygon.</li>
     *   <li>From geometry collection containing nested collections.</li>
     * </ul>
     *
     * The conversion from {@code MultiLineString} to {@code Polygon} is defined as following:
     * the first {@code LineString} is taken as the exterior {@code LinearRing} and all others
     * {@code LineString}s are interior {@code LinearRing}s.
     * This rule is defined by some SQLMM operations.
     *
     * @param  target  the desired type.
     * @return the converted geometry.
     * @throws IllegalArgumentException if the geometry cannot be converted to the specified type.
     */
    public GeometryWrapper<G> toGeometryType(GeometryType target) {
        final Class<?> type = factory().getGeometryClass(target);
        final Object geometry = implementation();
        if (type.isInstance(geometry)) {
            return this;
        }
        throw new UnconvertibleObjectException(Errors.format(Errors.Keys.CanNotConvertFromType_2, geometry.getClass(), type));
    }

    /**
     * Transforms this geometry using the given coordinate operation.
     * If the operation is {@code null}, then the geometry is returned unchanged.
     * If the geometry uses a different CRS than the source CRS of the given operation
     * and {@code validate} is {@code true},
     * then a new operation to the target CRS will be automatically computed.
     *
     * <p>This method is preferred to {@link #transform(CoordinateReferenceSystem)}
     * when possible because not all geometry libraries store the CRS of their objects.</p>
     *
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @param  validate   whether to validate the operation source CRS.
     * @return the transformed geometry (may be the same geometry instance, but never {@code null}).
     * @throws UnsupportedOperationException if this operation is not supported for current implementation.
     * @throws FactoryException if transformation to the target CRS cannot be found.
     * @throws TransformException if the geometry cannot be transformed.
     */
    public GeometryWrapper<G> transform(final CoordinateOperation operation, final boolean validate)
            throws FactoryException, TransformException
    {
        throw new UnsupportedOperationException(Geometries.unsupported("transform"));
    }

    /**
     * Transforms this geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS is null, then the geometry is returned unchanged.
     * If this geometry has no Coordinate Reference System, a {@link TransformException} is thrown.
     *
     * <p>Consider using {@link #transform(CoordinateOperation)} instead of this method as much as possible,
     * both for performance reasons and because not all geometry libraries provide information about the CRS
     * of their geometries.</p>
     *
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry but never {@code null}).
     * @throws UnsupportedOperationException if this operation is not supported for current implementation.
     * @throws TransformException if the given geometry has no CRS or cannot be transformed.
     *
     * @see #getCoordinateReferenceSystem()
     */
    @Override
    public GeometryWrapper<G> transform(final CoordinateReferenceSystem targetCRS) throws TransformException {
        if (targetCRS == null) {
            return this;
        }
        throw new UnsupportedOperationException(Geometries.unsupported("transform"));
    }

    /**
     * Returns {@code true} if the given geometry use the same CRS than this geometry, or conservatively
     * returns {@code false} in case of doubt. This method should perform only a cheap test; it is used
     * as a way to filter rapidly if {@link #transform(CoordinateReferenceSystem)} needs to be invoked.
     * If this method wrongly returned {@code false}, the {@code transform(…)} method will return the
     * geometry unchanged anyway.
     *
     * <p>If both CRS are undefined (null), then they are considered the same.</p>
     *
     * @param  other  the second geometry.
     * @return {@code true} if the two geometries use equivalent CRS or if the CRS is undefined on both side,
     *         or {@code false} in case of doubt.
     */
    public abstract boolean isSameCRS(GeometryWrapper<G> other);

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
    @Deprecated public final Geometry       clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
    @Deprecated public final boolean        contains(TransfiniteSet pointSet)         {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        contains(DirectPosition point)            {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        intersects(TransfiniteSet pointSet)       {throw new UnsupportedOperationException();}
    @Deprecated public final boolean        equals(TransfiniteSet pointSet)           {throw new UnsupportedOperationException();}
    @Deprecated public final TransfiniteSet union(TransfiniteSet pointSet)            {throw new UnsupportedOperationException();}
    @Deprecated public final TransfiniteSet intersection(TransfiniteSet pointSet)     {throw new UnsupportedOperationException();}
    @Deprecated public final TransfiniteSet difference(TransfiniteSet pointSet)       {throw new UnsupportedOperationException();}
    @Deprecated public final TransfiniteSet symmetricDifference(TransfiniteSet ps)    {throw new UnsupportedOperationException();}

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
