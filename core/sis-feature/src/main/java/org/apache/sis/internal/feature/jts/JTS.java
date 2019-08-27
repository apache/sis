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
package org.apache.sis.internal.feature.jts;

import java.util.Map;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.internal.system.Loggers;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;


/**
 * Utilities for Java Topology Suite (JTS) objects.
 * We use this class for functionalities not supported by Apache SIS with other libraries.
 * For library-agnostic functionalities, see {@link org.apache.sis.internal.feature.Geometries} instead.
 *
 * <p>This method may be modified or removed in any future version.
 * For example we may replace it by a more general mechanism working also on other geometry libraries.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
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
     * @param  geometry the geometry from which to get the CRS, or {@code null}.
     * @return the coordinate reference system, or {@code null} if none.
     * @throws FactoryException if the CRS can not be created from the SRID code.
     */
    public static CoordinateReferenceSystem getCoordinateReferenceSystem(final Geometry geometry) throws FactoryException {
        if (geometry != null) {
            final Object userData = geometry.getUserData();
            if (userData instanceof CoordinateReferenceSystem) {
                return (CoordinateReferenceSystem) userData;
            } else if (userData instanceof Map<?,?>) {
                final Map<?,?> map = (Map<?,?>) userData;
                final Object value = map.get(CRS_KEY);
                if (value instanceof CoordinateReferenceSystem) {
                    return (CoordinateReferenceSystem) value;
                }
            }
            /*
             * Fallback on SRID with the assumption that they are EPSG codes.
             */
            final int srid = geometry.getSRID();
            if (srid > 0) {
                return CRS.forCode(Constants.EPSG + ':' + srid);
            }
        }
        return null;
    }

    /**
     * Finds an operation between the given CRS valid in the given area of interest.
     * This method does not verify the CRS of the given geometry.
     *
     * @param  sourceCRS       the CRS of source coordinates.
     * @param  targetCRS       the CRS of target coordinates.
     * @param  areaOfInterest  the area of interest.
     * @return the mathematical operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation can not be created.
     */
    private static CoordinateOperation findOperation(final CoordinateReferenceSystem sourceCRS,
                                                     final CoordinateReferenceSystem targetCRS,
                                                     final Geometry areaOfInterest)
            throws FactoryException
    {
        DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox();
        try {
            final Envelope e = areaOfInterest.getEnvelopeInternal();
            bbox.setBounds(new Envelope2D(sourceCRS, e.getMinX(), e.getMinY(), e.getWidth(), e.getHeight()));
        } catch (TransformException ex) {
            bbox = null;
            Logging.ignorableException(Logging.getLogger(Loggers.GEOMETRY), JTS.class, "transform", ex);
        }
        return CRS.findOperation(sourceCRS, targetCRS, bbox);
    }

    /**
     * Transforms the given geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS or the given geometry is null, the geometry is returned unchanged.
     * If the geometry has no Coordinate Reference System, a {@link TransformException} is thrown.
     *
     * <p><b>This operation may be slow!</b>
     * If many geometries need to be transformed, it is better to fetch the {@link CoordinateOperation} only once,
     * then invoke {@link #transform(Geometry, CoordinateOperation)} for each geometry. Alternatively the geometries
     * can be stored in a single geometry collection in order to invoke this method only once.</p>
     *
     * @param  geometry   the geometry to transform, or {@code null}.
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     * @throws FactoryException if transformation to the target CRS can not be constructed.
     * @throws TransformException if the given geometry has no CRS or can not be transformed.
     */
    public static Geometry transform(Geometry geometry, final CoordinateReferenceSystem targetCRS)
            throws FactoryException, TransformException
    {
        if (geometry != null && targetCRS != null) {
            final CoordinateReferenceSystem sourceCRS = getCoordinateReferenceSystem(geometry);
            if (sourceCRS == null) {
                throw new TransformException(Errors.format(Errors.Keys.UnspecifiedCRS));
            }
            if (!Utilities.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                geometry = transform(geometry, findOperation(sourceCRS, targetCRS, geometry));
            }
        }
        return geometry;
    }

    /**
     * Transforms the given geometry using the given coordinate operation.
     * If the geometry or the operation is null, then the geometry is returned unchanged.
     * If the source CRS is not equals to the geometry CRS, a new operation is inferred.
     *
     * @todo Handle antimeridian case.
     *
     * @param  geometry   the geometry to transform, or {@code null}.
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     * @throws FactoryException if transformation to the target CRS can not be constructed.
     * @throws TransformException if the given geometry can not be transformed.
     */
    public static Geometry transform(Geometry geometry, CoordinateOperation operation)
            throws FactoryException, TransformException
    {
        if (geometry != null && operation != null) {
            final CoordinateReferenceSystem sourceCRS = operation.getSourceCRS();
            if (sourceCRS != null) {
                final CoordinateReferenceSystem crs = getCoordinateReferenceSystem(geometry);
                if (crs != null && !Utilities.equalsIgnoreMetadata(sourceCRS, crs)) {
                    operation = findOperation(crs, operation.getTargetCRS(), geometry);
                }
            }
            geometry = transform(geometry, operation.getMathTransform());
            geometry.setUserData(operation.getTargetCRS());
        }
        return geometry;
    }

    /**
     * Transform the given geometry using the given math transform.
     * If the geometry or the transform is null or identity, then the geometry is returned unchanged.
     *
     * @param  geometry   the geometry to transform, or {@code null}.
     * @param  transform  the transform to apply, or {@code null}.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     * @throws TransformException if the given geometry can not be transformed.
     */
    public static Geometry transform(Geometry geometry, MathTransform transform) throws TransformException {
        if (geometry != null && transform != null && !transform.isIdentity()) {
            final GeometryCoordinateTransform gct = new GeometryCoordinateTransform(transform, geometry.getFactory());
            geometry = gct.transform(geometry);
        }
        return geometry;
    }
}
