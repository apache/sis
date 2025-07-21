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
package org.apache.sis.geometry.wrapper.jts;

import java.util.Map;
import java.util.Objects;
import java.awt.Shape;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.GeometryCollection;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.privy.Constants;
import static org.apache.sis.geometry.wrapper.Geometries.LOGGER;


/**
 * Utilities for Java Topology Suite (JTS) objects.
 * We use this class for functionalities not supported by Apache SIS with other libraries.
 * For library-agnostic functionalities, see {@link org.apache.sis.geometry.wrapper.Geometries} instead.
 *
 * <p>This method may be modified or removed in any future version.
 * For example, we may replace it by a more general mechanism working also on other geometry libraries.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class JTS extends Static {
    /**
     * Key used in {@linkplain Geometry#getUserData() user data} map for storing an instance of {@link CoordinateReferenceSystem}.
     *
     * @see #getCoordinateReferenceSystem(Geometry)
     */
    public static final String CRS_KEY = "CRS";

    /**
     * Do not allow instantiation of this class.
     */
    private JTS() {
    }

    /**
     * Gets the Coordinate Reference System (CRS) from the given geometry.
     * This method expects the CRS to be stored in one the following ways:
     *
     * <ul>
     *   <li>Geometry {@linkplain Geometry#getUserData() user data} is an instance of {@code CoordinateReferenceSystem}.</li>
     *   <li>{@linkplain Geometry#getUserData() user data} is a (@link Map} with a value for the {@value #CRS_KEY} key.</li>
     *   <li>Geometry SRID is strictly positive, in which case it is interpreted as an EPSG code.</li>
     * </ul>
     *
     * If none of the above is valid, {@code null} is returned.
     *
     * @param  source  the geometry from which to get the CRS, or {@code null}.
     * @return the coordinate reference system, or {@code null} if none.
     * @throws FactoryException if the CRS cannot be created from the SRID code.
     */
    public static CoordinateReferenceSystem getCoordinateReferenceSystem(final Geometry source) throws FactoryException {
        if (source != null) {
            final Object userData = source.getUserData();
            if (userData instanceof CoordinateReferenceSystem) {
                return (CoordinateReferenceSystem) userData;
            } else if (userData instanceof Map<?,?>) {
                final var map = (Map<?,?>) userData;
                final Object value = map.get(CRS_KEY);
                if (value instanceof CoordinateReferenceSystem) {
                    return (CoordinateReferenceSystem) value;
                }
            }
            /*
             * Fallback on SRID with the assumption that they are EPSG codes except for axis order which
             * is (longitude, latitude). This is the order used in popular spatial databases and is also
             * the order frequently used by other libraries that use JTS.
             *
             * TODO: This is not necessarily EPSG code. We need a plugin mechanism for specifying the authority.
             * It may be for example the "spatial_ref_sys" table of a spatial database.
             */
            final int srid = source.getSRID();
            if (srid > 0) {
                CoordinateReferenceSystem crs = CRS.forCode(Constants.EPSG + ':' + srid);
                crs = AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
                return crs;
            }
        }
        return null;
    }

    /**
     * Sets the Coordinate Reference System (CRS) in the specified geometry. This method overwrites any previous
     * user data; it should be invoked only when the geometry is known to not store any other information.
     * In current Apache SIS usage, this method is invoked only for newly created geometries.
     *
     * <p>This method also sets the JTS SRID to EPSG code if such code can be found. For performance reasons
     * this method does not perform a full scan of EPSG database if the CRS does not provide an EPSG code.</p>
     *
     * @param  target  the geometry where to store coordinate reference system information.
     * @param  crs     the CRS to store, or {@code null}.
     */
    public static void setCoordinateReferenceSystem(final Geometry target, final CoordinateReferenceSystem crs) {
        target.setUserData(crs);
        int epsg = 0;
        final Identifier id = IdentifiedObjects.getIdentifier(crs, Citations.EPSG);
        if (id != null) try {
            epsg = Integer.parseInt(id.getCode());
        } catch (NumberFormatException e) {
            // Ignore. Note: this is also the exception if id.getCode() is null.
        }
        target.setSRID(epsg);
    }

    /**
     * Copies coordinate reference system information from the given source geometry to the target geometry.
     * Current implementation copies only CRS information, but future implementations could copy some other
     * values if they may apply to the target geometry as well.
     */
    static void copyMetadata(final Geometry source, final Geometry target) {
        target.setSRID(source.getSRID());
        Object crs = source.getUserData();
        if (!(crs instanceof CoordinateReferenceSystem)) {
            if (!(crs instanceof Map<?,?>)) {
                return;
            }
            crs = ((Map<?,?>) crs).get(CRS_KEY);
            if (!(crs instanceof CoordinateReferenceSystem)) {
                return;
            }
        }
        target.setUserData(crs);
    }

    /**
     * Finds an operation between the given CRS valid in the given area of interest.
     * This method does not verify the <abbr>CRS</abbr> of the given geometry.
     *
     * @param  sourceCRS       the CRS of source coordinates.
     * @param  targetCRS       the CRS of target coordinates.
     * @param  areaOfInterest  the area of interest.
     * @return the mathematical operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation cannot be created.
     */
    private static CoordinateOperation findOperation(final CoordinateReferenceSystem sourceCRS,
                                                     final CoordinateReferenceSystem targetCRS,
                                                     final Geometry areaOfInterest)
            throws FactoryException
    {
        DefaultGeographicBoundingBox bbox = null;
        if (!areaOfInterest.isEmpty()) {
            bbox = new DefaultGeographicBoundingBox();
            try {
                final Envelope e = areaOfInterest.getEnvelopeInternal();
                final var env = new GeneralEnvelope(sourceCRS);     // May be 3- or 4-dimensional.
                env.setRange(0, e.getMinX(), e.getMaxX());
                env.setRange(1, e.getMinY(), e.getMaxY());
                bbox.setBounds(env);
            } catch (TransformException ex) {
                bbox = null;
                Logging.ignorableException(LOGGER, JTS.class, "transform", ex);
            }
        }
        return CRS.findOperation(sourceCRS, targetCRS, bbox);
    }

    /**
     * Transforms the given geometry to the specified Coordinate Reference System (<abbr>CRS</abbr>).
     * If the given geometry is {@code null}, or if the given <abbr>CRS</abbr> is {@code null} or the
     * same as the current geometry <abbr>CRS</abbr>, then the given geometry is returned unchanged.
     * If the geometry has no Coordinate Reference System, then the geometry is returned unchanged.
     *
     * <p><b>This operation may be slow!</b>
     * If many geometries need to be transformed, it is better to fetch the {@link CoordinateOperation} only once,
     * then invoke {@link #transform(Geometry, CoordinateOperation, boolean)} for each geometry.
     * Alternatively, the geometries can be stored in a single geometry collection
     * in order to invoke this method only once.</p>
     *
     * @param  geometry   the geometry to transform, or {@code null}.
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     * @throws FactoryException if transformation to the target CRS cannot be constructed.
     * @throws TransformException if the given geometry cannot be transformed.
     */
    public static Geometry transform(Geometry geometry, final CoordinateReferenceSystem targetCRS)
            throws FactoryException, TransformException
    {
        if (geometry != null && targetCRS != null) {
            final CoordinateReferenceSystem sourceCRS = getCoordinateReferenceSystem(geometry);
            if (sourceCRS != null) {
                if (!Utilities.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                    geometry = transform(geometry, findOperation(sourceCRS, targetCRS, geometry), false);
                }
            }
        }
        return geometry;
    }

    /**
     * Transforms the given geometry using the given coordinate operation.
     * If the geometry or the operation is null, then the geometry is returned unchanged.
     * If the source CRS is not equals to the geometry CRS and {@code validate} is {@code true},
     * then a new operation is inferred.
     *
     * @todo Handle antimeridian case.
     *
     * @param  geometry   the geometry to transform, or {@code null}.
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @param  validate   whether to validate the operation source CRS.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     * @throws FactoryException if transformation to the target CRS cannot be found.
     * @throws TransformException if the given geometry cannot be transformed.
     */
    public static Geometry transform(Geometry geometry, CoordinateOperation operation, final boolean validate)
            throws FactoryException, TransformException
    {
        if (geometry != null && operation != null) {
            if (validate) {
                final CoordinateReferenceSystem sourceCRS = operation.getSourceCRS();
                if (sourceCRS != null) {
                    final CoordinateReferenceSystem crs = getCoordinateReferenceSystem(geometry);
                    if (crs != null && !Utilities.equalsIgnoreMetadata(sourceCRS, crs)) {
                        operation = findOperation(crs, operation.getTargetCRS(), geometry);
                    }
                }
            }
            geometry = transform(geometry, operation.getMathTransform());
            geometry.setUserData(operation.getTargetCRS());
        }
        return geometry;
    }

    /**
     * Transforms the given geometry using the given math transform.
     * If the geometry or the transform is null or identity, then the geometry is returned unchanged.
     *
     * @param  geometry   the geometry to transform, or {@code null}.
     * @param  transform  the transform to apply, or {@code null}.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     * @throws TransformException if the given geometry cannot be transformed.
     */
    public static Geometry transform(Geometry geometry, final MathTransform transform) throws TransformException {
        if (geometry != null && transform != null && !transform.isIdentity()) {
            final var gct = new GeometryCoordinateTransform(transform, geometry.getFactory());
            geometry = gct.transform(geometry);
        }
        return geometry;
    }

    /**
     * Returns a view of the given JTS geometry as a Java2D shape.
     *
     * @param  geometry  the geometry to view as a shape, not {@code null}.
     * @return the Java2D shape view.
     */
    public static Shape asShape(final Geometry geometry) {
        // Null value check in the invoked constructor.
        return new ShapeAdapter(geometry);
    }

    /**
     * Converts a Java2D shape to a JTS geometry. If the given shape is a view created by {@link #asShape(Geometry)},
     * then the original geometry is returned. Otherwise a new geometry is created with a copy (not a view) of the
     * shape coordinates.
     *
     * @param  factory   factory to use for creating the geometry, or {@code null} for the default.
     * @param  shape     the Java2D shape to convert. Cannot be {@code null}.
     * @param  flatness  the maximum distance that line segments are allowed to deviate from curves.
     * @return JTS geometry with shape coordinates. Never null but can be empty.
     */
    public static Geometry fromAWT(final GeometryFactory factory, final Shape shape, final double flatness) {
        return ShapeConverter.create(factory, Objects.requireNonNull(shape), flatness);
    }

    /**
     * Makes the given geometry clockwise or counterclockwise.
     * This method ensures that shells are in the requested winding and holes are in reverse-winding.
     * If the given geometry already has the desired orientation, then it is returned unchanged.
     *
     * <h4>Limitations</h4>
     * The current implementation handles only the {@link GeometryCollection}, {@link MultiPolygon},
     * {@link Polygon} and {@link LinearRing} geometry types. Other geometry types are returned unchanged.
     *
     * @param  geometry  the geometry to make clockwise or counterclockwise.
     * @param  clockwise {@code true} for making the exterior ring clockwise, or {@code false} for counterclockwise.
     * @return the given geometry with the requested winding. May be the {@code geometry} returned directly.
     */
    public static Geometry ensureWinding(final Geometry geometry, final boolean clockwise) {
        if (geometry instanceof MultiPolygon) {
            final var collection = (MultiPolygon) geometry;
            final var components = new Polygon[collection.getNumGeometries()];
            boolean changed = false;
            for (int i=0; i<components.length; i++) {
                final var c = (Polygon) geometry.getGeometryN(i);
                changed |= (c != (components[i] = ensureWinding(c, clockwise)));
            }
            if (changed) {
                return geometry.getFactory().createMultiPolygon(components);
            }
        } else if (geometry instanceof GeometryCollection) {
            final var collection = (GeometryCollection) geometry;
            final var components = new Geometry[collection.getNumGeometries()];
            boolean changed = false;
            for (int i=0; i<components.length; i++) {
                final var c = geometry.getGeometryN(i);
                changed |= (c != (components[i] = ensureWinding(c, clockwise)));    // Recursive method invocation.
            }
            if (changed) {
                return geometry.getFactory().createGeometryCollection(components);
            }
        } else if (geometry instanceof Polygon) {
            return ensureWinding((Polygon) geometry, clockwise);
        } else if (geometry instanceof LinearRing) {
            return ensureWinding((LinearRing) geometry, clockwise);
        }
        return geometry;
    }

    /**
     * Makes the given polygon clockwise or counterclockwise.
     * This method ensures that the shell is in the requested winding and holes are in reverse-winding.
     * If the given polygon already has the desired orientation, then it is returned unchanged.
     *
     * @param  geometry  the polygon to make clockwise or counterclockwise.
     * @param  clockwise {@code true} for making the polygon clockwise, or {@code false} for counterclockwise.
     * @return the given polygon with the requested winding. May be the {@code geometry} returned directly.
     */
    public static Polygon ensureWinding(final Polygon geometry, final boolean clockwise) {
        LinearRing outer = geometry.getExteriorRing();
        boolean same = (outer == (outer = ensureWinding(outer, clockwise)));
        final var holes = new LinearRing[geometry.getNumInteriorRing()];
        for (int i=0; i<holes.length; i++) {
            final LinearRing inner = geometry.getInteriorRingN(i);
            same &= (inner == (holes[i] = ensureWinding(inner, !clockwise)));
        }
        if (same) {
            return geometry;
        }
        final Polygon reversed = geometry.getFactory().createPolygon(outer, holes);
        copyMetadata(geometry, reversed);
        return reversed;
    }

    /**
     * Makes the given ring clockwise or counterclockwise.
     * If the given ring already has the desired orientation, then it is returned unchanged.
     *
     * @param  geometry  the ring to make clockwise or counterclockwise.
     * @param  clockwise {@code true} for making the ring clockwise, or {@code false} for counterclockwise.
     * @return the given ring with the requested winding. May be the {@code geometry} returned directly.
     */
    public static LinearRing ensureWinding(final LinearRing geometry, final boolean clockwise) {
        CoordinateSequence cs = geometry.getCoordinateSequence();
        if (Orientation.isCCW(cs) != clockwise) {
            return geometry;
        }
        cs = cs.copy();
        CoordinateSequences.reverse(cs);
        final LinearRing reversed = geometry.getFactory().createLinearRing(cs);
        copyMetadata(geometry, reversed);
        return reversed;
    }
}
