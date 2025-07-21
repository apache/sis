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

import java.awt.Shape;
import java.util.Objects;
import java.util.Iterator;
import java.util.OptionalInt;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Length;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.filter.sqlmm.SQLMM;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Set;
import org.opengis.geometry.Boundary;
import org.opengis.geometry.TransfiniteSet;
import org.opengis.geometry.complex.Complex;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * Wraps a <abbr>JTS</abbr>, <abbr>ESRI</abbr> or Java2D geometry behind a {@code Geometry} interface.
 *
 * <h4>Future plans</h4>
 * This is a temporary class to be refactored later as a more complete geometry framework.
 * The methods provided in this class are not committed API, and often not even clean API.
 * They are only utilities added for very specific Apache <abbr>SIS</abbr> needs and will
 * certainly change without warning in future Apache <abbr>SIS</abbr> versions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Geometries#wrap(Object)
 */
public abstract class GeometryWrapper extends AbstractGeometry implements Geometry {
    /**
     * The coordinate reference system, or {@code null} if unspecified.
     * The value of this field should be set by subclass constructors.
     *
     * <h4>Where this value come from</h4>
     * This information is sometime redundant with information stored in the geometry itself.
     * However, even when the geometry library supports some kind of <abbr>SRID</abbr> field,
     * the value in geometry instances is sometime 0 or null. In such case, this {@code crs}
     * field may come from other sources such as characteristics of the {@code FeatureType}.
     *
     * @see #getCoordinateDimension()
     * @see #getCoordinateReferenceSystem()
     * @see #setCoordinateReferenceSystem(CoordinateReferenceSystem)
     */
    protected CoordinateReferenceSystem crs;

    /**
     * Creates a new geometry object.
     * Subclasses should set the {@link #crs} field if known.
     */
    protected GeometryWrapper() {
    }

    /**
     * Returns the implementation-dependent factory of geometric objects.
     * This is typically a system-wide factory shared by all geometry instances.
     *
     * @return the factory of implementation-dependent geometric objects (never {@code null}).
     */
    protected abstract Geometries<?> factory();

    /**
     * Returns the JTS, ESRI or Java2D geometry implementation wrapped by this {@code GeometryWrapper} instance.
     * The return type should be {@code <G>}, except for points which may be of unrelated type in some libraries.
     * For runtime check, the base class is either {@link Geometries#rootClass} or {@link Geometries#pointClass}.
     *
     * @return the geometry implementation wrapped by this instance (never {@code this} or {@code null}).
     *
     * @see Geometries#implementation(Object)
     * @see Geometries#getGeometry(GeometryWrapper)
     */
    protected abstract Object implementation();

    /**
     * Returns the Spatial Reference System Identifier (<abbr>SRID</abbr>) if available.
     * The <abbr>SRID</abbr> is used in databases such as PostGIS and is generally database-dependent.
     * This is <em>not</em> necessarily an <abbr>EPSG</abbr> code, even if it is a common practice to
     * use the same numerical values as <abbr>EPSG</abbr>. Note that the absence of <abbr>SRID</abbr>
     * does not mean that {@link #getCoordinateReferenceSystem()} would return no <abbr>CRS</abbr>.
     *
     * <p>Users should invoke the {@link #getCoordinateReferenceSystem()} method instead.
     * This {@code getSRID()} method is provided for classes such as {@code DataStore} backed by an SQL database.
     * Those classes have a connection to a {@code spatial_ref_sys} table providing the mapping from SRID codes
     * to authority codes such as EPSG. Those {@code DataStore}s will typically get the SRID soon after geometry
     * creation, resolve its CRS and invoke {@link #setCoordinateReferenceSystem(CoordinateReferenceSystem)}.</p>
     *
     * @return the Spatial Reference System Identifier of the geometry.
     */
    public OptionalInt getSRID() {
        return OptionalInt.empty();
    }

    /**
     * Gets the Coordinate Reference System (<abbr>CRS</abbr>) of the geometry.
     * In some libraries such as <abbr>JTS</abbr>, some <abbr>CRS</abbr> information can be stored in the
     * geometry object of that library. In other libraries such as Java2D, the <abbr>CRS</abbr> is stored
     * only in this {@code GeometryWrapper} instance.
     *
     * @return the geometry <abbr>CRS</abbr>, or {@code null} if unknown.
     */
    @Override
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Sets the coordinate reference system.
     * This method should be invoked only for newly created geometries. If the geometry library supports
     * user objects (e.g. JTS), there is no guarantee that this method will not overwrite user's setting.
     *
     * @param  crs  the coordinate reference system to set.
     * @throws MismatchedDimensionException if the <abbr>CRS</abbr> does not have the expected number of dimensions.
     *
     * @see #transform(CoordinateReferenceSystem)
     */
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureDimensionMatches("crs", getCoordinateDimension(), crs);
        this.crs = crs;
    }

    /**
     * Returns the dimension of the coordinates that define this geometry.
     * It must be the same as the dimension of the coordinate reference system for this geometry.
     *
     * @return the coordinate dimension.
     */
    @Override
    public int getCoordinateDimension() {
        return Geometries.BIDIMENSIONAL;
    }

    /**
     * Creates an initially empty envelope with the <abbr>CRS</abbr> of this geometry.
     * If this geometry has no <abbr>CRS</abbr>, then a two- or three-dimensional envelope is created.
     * This is a convenience method for {@link #getEnvelope()} implementations.
     *
     * @return an initially empty envelope.
     */
    protected final GeneralEnvelope createEnvelope() {
        return (crs != null) ? new GeneralEnvelope(crs) : new GeneralEnvelope(getCoordinateDimension());
    }

    /**
     * Returns the geometry bounding box, together with its coordinate reference system.
     * For an empty geometry or a single point, the returned envelope will be empty.
     *
     * @return the geometry envelope. Should never be {@code null}.
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
     * <p>The given iterator shall return instances of the underlying library assignable to
     * {@link Geometries#rootClass} or {@link Geometries#pointClass}, <em>not</em> instances
     * of {@link GeometryWrapper}. It is caller responsibility to unwrap if needed.</p>
     *
     * @param  paths  the points or polylines to merge in a single polyline instance.
     * @return the merged polyline (may be the underlying geometry of {@code this} but never {@code null}).
     * @throws ClassCastException if collection elements are not instances of the point or geometry class.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public abstract Object mergePolylines(final Iterator<?> paths);

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
     * @throws FactoryException if transformation to the target <abbr>CRS</abbr> cannot be found.
     * @throws TransformException if a geometry cannot be transformed.
     * @throws IncommensurableException if a unit conversion was necessary but failed.
     * @throws BackingStoreException if the operation failed because of another checked exception.
     */
    public final boolean predicate(final DistanceOperatorName type, final GeometryWrapper other,
                                   final Quantity<Length> distance, final SpatialOperationContext context)
            throws FactoryException, TransformException, IncommensurableException
    {
        final var geometries = new GeometryWrapper[] {this, other};
        if (context.transform(geometries)) {
            double dv = distance.getValue().doubleValue();
            final Unit<?> unit = ReferencingUtilities.getUnit(context.commonCRS);
            if (unit != null) {
                dv = distance.getUnit().getConverterToAny(unit).convert(dv);
            }
            return geometries[0].predicateSameCRS(type, geometries[1], dv);
        }
        /*
         * No common CRS. Consider that we have no intersection, no overlap, etc.
         * since the two geometries are existing in different coordinate spaces.
         */
        return SpatialOperationContext.emptyResult(type);
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
     * @throws FactoryException if transformation to the target <abbr>CRS</abbr> cannot be found.
     * @throws TransformException if a geometry cannot be transformed.
     * @throws IncommensurableException if a unit conversion was necessary but failed.
     * @throws BackingStoreException if the operation failed because of another checked exception.
     */
    public final boolean predicate(final SpatialOperatorName type, final GeometryWrapper other,
                                   final SpatialOperationContext context)
            throws FactoryException, TransformException, IncommensurableException
    {
        final var geometries = new GeometryWrapper[] {this, other};
        if (context.transform(geometries)) {
            return geometries[0].predicateSameCRS(type, geometries[1]);
        }
        /*
         * No common CRS. Consider that we have no intersection, no overlap, etc.
         * since the two geometries are existing in different coordinate spaces.
         */
        return SpatialOperationContext.emptyResult(type);
    }

    /**
     * Applies a <abbr>SQLMM</abbr> operation on this geometry.
     * This method shall be invoked only for operations without non-geometric parameters.
     *
     * @param  operation  the SQLMM operation to apply.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public final Object operation(final SQLMM operation) {
        assert operation.geometryCount() == 1 && operation.maxParamCount == 1 : operation;
        final Object result = operationSameCRS(operation, null, null);
        assert isInstance(operation, result) : result;
        return result;
    }

    /**
     * Applies a <abbr>SQLMM</abbr> operation on two geometries.
     * This method shall be invoked only for operations without non-geometric parameters.
     * The second geometry is transformed to the same CRS as this geometry for conformance with SQLMM standard.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry. It is caller's responsibility to check that this value is non-null.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     * @throws TransformException if it was necessary to transform the other geometry and that transformation failed.
     * @throws BackingStoreException if the operation failed because of another checked exception.
     */
    public final Object operation(final SQLMM operation, final GeometryWrapper other) throws TransformException {
        assert operation.geometryCount() == 2 && operation.maxParamCount == 2 : operation;
        final Object result = operationSameCRS(operation, other.transform(crs), null);
        assert isInstance(operation, result) : result;
        return result;
    }

    /**
     * Applies a <abbr>SQLMM</abbr> operation on this geometry with one operation-specific argument.
     * The argument shall be non-null, unless the argument is optional.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  argument   an operation-specific argument.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public final Object operationWithArgument(final SQLMM operation, final Object argument) {
        assert operation.geometryCount() == 1 && operation.maxParamCount == 2 : operation;
        if (operation.minParamCount > 1) {
            // TODO: fetch argument name.
            ArgumentChecks.ensureNonNull("arg1", argument);
        }
        final Object result = operationSameCRS(operation, null, argument);
        assert isInstance(operation, result) : result;
        return result;
    }

    /**
     * Applies a <abbr>SQLMM</abbr> operation on two geometries with one operation-specific argument.
     * The argument shall be non-null, unless the argument is optional.
     * The second geometry is transformed to the same CRS as this geometry for conformance with SQLMM standard.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry. It is caller's responsibility to check that this value is non-null.
     * @param  argument   an operation-specific argument.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     * @throws TransformException if it was necessary to transform the other geometry and that transformation failed.
     * @throws BackingStoreException if the operation failed because of another checked exception.
     */
    public final Object operationWithArgument(final SQLMM operation, final GeometryWrapper other, final Object argument)
            throws TransformException
    {
        assert operation.geometryCount() == 2 && operation.maxParamCount == 3 : operation;
        if (argument == null && operation.minParamCount > 2) {
            // TODO: fetch argument name.
            throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, "arg2"));
        }
        final Object result = operationSameCRS(operation, other.transform(crs), argument);
        assert isInstance(operation, result) : result;
        return result;
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
     * This method assumes that the two geometries are in the same <abbr>CRS</abbr> (this is not verified).
     * Conversions, if needed, shall be done by the caller.
     *
     * <p><b>Note:</b> {@link SpatialOperatorName#BBOX} is implemented by {@code NOT DISJOINT}.
     * It is caller's responsibility to ensure that one of the geometries is rectangular.</p>
     *
     * @param  type   the predicate operation to apply.
     * @param  other  the other geometry to test with this geometry.
     * @return result of applying the specified predicate.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    protected boolean predicateSameCRS(SpatialOperatorName type, GeometryWrapper other) {
        throw new UnsupportedOperationException(Geometries.unsupported(type.name()));
    }

    /**
     * Applies a filter predicate between this geometry and another geometry within a given distance.
     * This method assumes that the two geometries are in the same <abbr>CRS</abbr> and that the unit
     * of measurement is the same for {@code distance} than for axes (this is not verified).
     * Conversions, if needed, shall be done by the caller.
     *
     * @param  type      the predicate operation to apply.
     * @param  other     the other geometry to test with this geometry.
     * @param  distance  distance to test between the geometries.
     * @return result of applying the specified predicate.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    protected boolean predicateSameCRS(DistanceOperatorName type, GeometryWrapper other, double distance) {
        throw new UnsupportedOperationException(Geometries.unsupported(type.name()));
    }

    /**
     * Applies a <abbr>SQLMM</abbr> operation on this geometry.
     * This method assumes that the two geometries are in the same <abbr>CRS</abbr> (this is not verified).
     * Conversions, if needed, shall be done by the caller.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry, or {@code null} if the operation requires only one geometry.
     * @param  argument   an operation-specific argument, or {@code null} if not applicable.
     * @return result of the specified operation.
     * @throws UnsupportedOperationException if the operation cannot be performed with current implementation.
     * @throws ClassCastException if the operation can only be executed on some specific geometry subclasses
     *         (for example polylines) and the wrapped geometry is not of that class.
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    protected Object operationSameCRS(SQLMM operation, GeometryWrapper other, Object argument) {
        throw new UnsupportedOperationException(Geometries.unsupported(operation.name()));
    }

    /**
     * Converts the wrapped geometry to the specified type.
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
     * @throws BackingStoreException if the operation failed because of a checked exception.
     */
    public GeometryWrapper toGeometryType(final GeometryType target) {
        final Class<?> type = factory().getGeometryClass(target);
        final Object geometry = implementation();
        if (type.isInstance(geometry)) {
            return this;
        }
        throw new UnconvertibleObjectException(Errors.format(Errors.Keys.CanNotConvertFromType_2, geometry.getClass(), type));
    }

    /**
     * Transforms this geometry using the given coordinate operation.
     * If the geometry uses a different <abbr>CRS</abbr> than the source <abbr>CRS</abbr>
     * of the given {@code operation} and if the {@code validate} argument is {@code true},
     * then a new operation to the target <abbr>CRS</abbr> will be automatically computed.
     *
     * <p>This method is preferred to the {@link #transform(CoordinateReferenceSystem)} method
     * because not all geometry libraries store the <abbr>CRS</abbr> of their objects.</p>
     *
     * @param  operation  the coordinate operation to apply.
     * @param  validate   whether to validate the operation source <abbr>CRS</abbr>.
     * @return the transformed geometry (may be the same geometry instance, but never {@code null}).
     * @throws UnsupportedOperationException if this operation is not supported for current implementation.
     * @throws FactoryException if transformation to the target <abbr>CRS</abbr> cannot be found.
     * @throws TransformException if the geometry cannot be transformed.
     * @throws BackingStoreException if the operation failed because of another checked exception.
     */
    public GeometryWrapper transform(final CoordinateOperation operation, boolean validate)
            throws FactoryException, TransformException
    {
        MathTransform transform = operation.getMathTransform();
        if (validate && crs != null) {
            CoordinateOperation step = CRS.findOperation(crs, operation.getSourceCRS(), null);
            transform = MathTransforms.concatenate(step.getMathTransform(), transform);
        }
        final GeometryWrapper wrapper = transform(transform);
        wrapper.setCoordinateReferenceSystem(operation.getTargetCRS());
        return wrapper;
    }

    /**
     * Transforms this geometry using the given transform.
     * If the transform is identity, then the geometry is returned unchanged.
     * Otherwise, a new geometry is returned without <abbr>CRS</abbr>.
     *
     * @param  transform  the math transform to apply.
     * @return the transformed geometry (may be the same geometry instance, but never {@code null}).
     * @throws UnsupportedOperationException if this operation is not supported for current implementation.
     * @throws TransformException if the geometry cannot be transformed.
     * @throws FactoryException if a problem happened while setting the <abbr>CRS</abbr>.
     * @throws BackingStoreException if the operation failed because of another checked exception.
     */
    public GeometryWrapper transform(final MathTransform transform)
            throws FactoryException, TransformException
    {
        if (transform.isIdentity()) {
            return this;
        }
        throw new UnsupportedOperationException(Geometries.unsupported("transform"));
    }

    /**
     * Transforms this geometry to the specified Coordinate Reference System (<abbr>CRS</abbr>).
     * If the given <abbr>CRS</abbr> is {@code null} or the same as the current <abbr>CRS</abbr>,
     * or if the geometry has no <abbr>CRS</abbr>, then this wrapper is returned unchanged.
     *
     * <p>Consider using {@link #transform(CoordinateOperation, boolean)} instead of this method,
     * for performance reasons and because not all geometry libraries associate <abbr>CRS</abbr>
     * with their geometric objects.</p>
     *
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry but never {@code null}).
     * @throws UnsupportedOperationException if this operation is not supported for current implementation.
     * @throws TransformException if the given geometry has no CRS or cannot be transformed.
     * @throws BackingStoreException if the operation failed because of another checked exception.
     *
     * @see #getCoordinateReferenceSystem()
     */
    @Override
    public GeometryWrapper transform(final CoordinateReferenceSystem targetCRS) throws TransformException {
        if (targetCRS == null || targetCRS == crs || crs == null) {
            return this;
        }
        try {
            return transform(CRS.findOperation(crs, targetCRS, null), false);
        } catch (FactoryException e) {
            /*
             * We wrap that exception because `Geometry.transform(…)` does not declare `FactoryException`.
             * We may revisit in a future version if `Geometry.transform(…)` method declaration is updated.
             */
            throw new TransformException(e);
        }
    }

    /**
     * Formats the wrapped geometry in Well Known Text (<abbr>WKT</abbr>) format.
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
     * Returns a Java2D shape made from this geometry.
     * The returned shape may be a view backed by the {@linkplain #implementation() geometry implementation},
     * or may be an internal object returned directly. Caller should not attempt to modify the returned shape.
     * Changes in the geometry implementation may or may not be reflected in the returned Java2D shape.
     *
     * @return a view, copy or direct reference to the geometry as a Java2D shape.
     * @throws UnsupportedOperationException if this operation is not supported for current implementation.
     */
    public Shape toJava2D() {
        throw new UnsupportedOperationException(Geometries.unsupported("toJava2D"));
    }

    /**
     * Methods from the {@link Geometry} interface. The {@link Override} annotation is intentionally omitted
     * for reducing the risk of compilation failures during the upcoming revision of GeoAPI interfaces since
     * some of those methods will be removed.
     */
    @Deprecated @Override public final Geometry       getMbRegion()                             {throw new UnsupportedOperationException();}
    @Deprecated @Override public final DirectPosition getRepresentativePoint()                  {throw new UnsupportedOperationException();}
    @Deprecated @Override public final Boundary       getBoundary()                             {throw new UnsupportedOperationException();}
    @Deprecated @Override public final Complex        getClosure()                              {throw new UnsupportedOperationException();}
    @Deprecated @Override public final boolean        isSimple()                                {throw new UnsupportedOperationException();}
    @Deprecated @Override public final boolean        isCycle()                                 {throw new UnsupportedOperationException();}
    @Deprecated @Override public final double         distance(Geometry geometry)               {throw new UnsupportedOperationException();}
    @Deprecated @Override public final int            getDimension(DirectPosition point)        {throw new UnsupportedOperationException();}
    @Deprecated @Override public final Set<Complex>   getMaximalComplex()                       {throw new UnsupportedOperationException();}
    @Deprecated @Override public final Geometry       getConvexHull()                           {throw new UnsupportedOperationException();}
    @Deprecated @Override public final Geometry       getBuffer(double distance)                {throw new UnsupportedOperationException();}
    @Deprecated @Override public final Geometry       clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
    @Deprecated @Override public final boolean        contains(TransfiniteSet pointSet)         {throw new UnsupportedOperationException();}
    @Deprecated @Override public final boolean        contains(DirectPosition point)            {throw new UnsupportedOperationException();}
    @Deprecated @Override public final boolean        intersects(TransfiniteSet pointSet)       {throw new UnsupportedOperationException();}
    @Deprecated @Override public final boolean        equals(TransfiniteSet pointSet)           {throw new UnsupportedOperationException();}
    @Deprecated @Override public final TransfiniteSet union(TransfiniteSet pointSet)            {throw new UnsupportedOperationException();}
    @Deprecated @Override public final TransfiniteSet intersection(TransfiniteSet pointSet)     {throw new UnsupportedOperationException();}
    @Deprecated @Override public final TransfiniteSet difference(TransfiniteSet pointSet)       {throw new UnsupportedOperationException();}
    @Deprecated @Override public final TransfiniteSet symmetricDifference(TransfiniteSet ps)    {throw new UnsupportedOperationException();}

    /**
     * Returns {@code true} if the given object is a wrapper of the same class
     * and the wrapped geometry implementations are equal.
     *
     * @param  obj  the object to compare with this wrapper.
     * @return whether the two objects are wrapping geometry implementations that are themselves equal.
     */
    @Override
    public final boolean equals(final Object obj) {
        return (obj != null) && obj.getClass() == getClass() &&
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
