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
package org.apache.sis.internal.referencing;

import org.apache.sis.internal.metadata.WKTKeywords;
import java.util.Iterator;
import java.util.Collection;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.geometry.Envelope;

import org.apache.sis.geometry.Envelopes;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Utilities;


/**
 * Implements the referencing services needed by the {@code "sis-metadata"} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public final class ServicesForMetadata extends ReferencingServices {
    /**
     * Creates a new instance. This constructor is invoked by reflection only.
     */
    public ServicesForMetadata() {
    }

    /**
     * Returns a fully implemented parameter descriptor.
     *
     * @param  parameter A partially implemented parameter descriptor, or {@code null}.
     * @return A fully implemented parameter descriptor, or {@code null} if the given argument was null.
     */
    @Override
    public ParameterDescriptor<?> toImplementation(final ParameterDescriptor<?> parameter) {
        return DefaultParameterDescriptor.castOrCopy(parameter);
    }

    /**
     * Converts the given object in a {@code FormattableObject} instance.
     *
     * @param  object The object to wrap.
     * @return The given object converted to a {@code FormattableObject} instance.
     */
    @Override
    public FormattableObject toFormattableObject(final IdentifiedObject object) {
        return AbstractIdentifiedObject.castOrCopy(object);
    }

    /**
     * Converts the given object in a {@code FormattableObject} instance. Callers should verify that the given
     * object is not already an instance of {@code FormattableObject} before to invoke this method. This method
     * returns {@code null} if it can not convert the object.
     *
     * @param  object The object to wrap.
     * @param  internal {@code true} if the formatting convention is {@code Convention.INTERNAL}.
     * @return The given object converted to a {@code FormattableObject} instance, or {@code null}.
     *
     * @since 0.6
     */
    @Override
    public FormattableObject toFormattableObject(final MathTransform object, boolean internal) {
        Matrix matrix;
        final ParameterValueGroup parameters;
        if (internal && (matrix = MathTransforms.getMatrix(object)) != null) {
            parameters = Affine.parameters(matrix);
        } else if (object instanceof Parameterized) {
            parameters = ((Parameterized) object).getParameterValues();
        } else {
            matrix = MathTransforms.getMatrix(object);
            if (matrix == null) {
                return null;
            }
            parameters = Affine.parameters(matrix);
        }
        return new FormattableObject() {
            @Override
            protected String formatTo(final Formatter formatter) {
                WKTUtilities.appendParamMT(parameters, formatter);
                return WKTKeywords.Param_MT;
            }
        };
    }

    /**
     * Returns the coordinate operation factory to be used for transforming the envelope.
     * We will fetch a lenient factory because {@link GeographicBoundingBox} are usually for approximative
     * bounds (e.g. the area of validity of some CRS). If a user wants accurate bounds, he should probably
     * use an {@link Envelope} with the appropriate CRS.
     */
    private static CoordinateOperationFactory getFactory() throws TransformException {
        // TODO: specify a lenient factory when the API will allow that.
        final CoordinateOperationFactory factory = DefaultFactories.forClass(CoordinateOperationFactory.class);
        if (factory != null) {
            return factory;
        }
        throw new TransformException(Errors.format(Errors.Keys.MissingRequiredModule_1, "geotk-referencing")); // This is temporary.
    }

    /**
     * Creates an exception message for a spatial, vertical or temporal dimension not found.
     */
    private static String dimensionNotFound(final short errorKey, final CoordinateReferenceSystem crs) {
        if (crs == null) {
            return Errors.format(Errors.Keys.UnspecifiedCRS);
        } else {
            return Errors.format(errorKey, crs.getName());
        }
    }

    /**
     * Implementation of the public {@code setBounds(…, DefaultGeographicBoundingBox, …)} methods for
     * the horizontal extent. If the {@code crs} argument is null, then it is caller's responsibility
     * to ensure that the given envelope is two-dimensional.
     *
     * @param  envelope The source envelope.
     * @param  target The target bounding box.
     * @param  crs The envelope CRS, or {@code null} if unknown.
     * @param  normalizedCRS The horizontal component of the given CRS, or null if the {@code crs} argument is null.
     * @throws TransformException If the given envelope can not be transformed.
     */
    private void setGeographicExtent(Envelope envelope, final DefaultGeographicBoundingBox target,
            final CoordinateReferenceSystem crs, final GeographicCRS normalizedCRS) throws TransformException
    {
        if (normalizedCRS != null) {
            // No need to check for dimension, since GeodeticCRS can not have less than 2.
            final CoordinateSystem cs1 = crs.getCoordinateSystem();
            final CoordinateSystem cs2 = normalizedCRS.getCoordinateSystem();
            if (!Utilities.equalsIgnoreMetadata(cs2.getAxis(0), cs1.getAxis(0)) ||
                !Utilities.equalsIgnoreMetadata(cs2.getAxis(1), cs1.getAxis(1)))
            {
                final CoordinateOperation operation;
                try {
                    operation = getFactory().createOperation(crs, normalizedCRS);
                } catch (FactoryException e) {
                    throw new TransformException(Errors.format(Errors.Keys.CanNotTransformEnvelopeToGeodetic), e);
                }
                envelope = Envelopes.transform(operation, envelope);
            }
        }
        /*
         * At this point, the envelope should use (longitude, latitude) coordinates in degrees.
         * However the prime meridian is not necessarily Greenwich.
         */
        double westBoundLongitude = envelope.getMinimum(0);
        double eastBoundLongitude = envelope.getMaximum(0);
        double southBoundLatitude = envelope.getMinimum(1);
        double northBoundLatitude = envelope.getMaximum(1);
        if (normalizedCRS != null) {
            final double rotation = CRS.getGreenwichLongitude(normalizedCRS);
            westBoundLongitude += rotation;
            eastBoundLongitude += rotation;
        }
        target.setBounds(westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude);
        target.setInclusion(Boolean.TRUE);
    }

    /**
     * Implementation of the public {@code setBounds} methods for the vertical extent.
     * If the {@code crs} argument is null, then it is caller's responsibility to ensure
     * that the given envelope is one-dimensional.
     *
     * @param  envelope    The source envelope.
     * @param  target      The target vertical extent.
     * @param  crs         The envelope CRS, or {@code null} if unknown.
     * @param  verticalCRS The vertical component of the given CRS, or null if the {@code crs} argument is null.
     */
    private static void setVerticalExtent(final Envelope envelope, final DefaultVerticalExtent target,
            final CoordinateReferenceSystem crs, final VerticalCRS verticalCRS)
    {
        final int dim;
        if (verticalCRS == null) {
            dim = 0;
        } else {
            dim = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), verticalCRS.getCoordinateSystem());
            assert dim >= 0 : crs; // Should not fail since 'verticalCRS' has been extracted from 'crs' by the caller.
        }
        target.setMinimumValue(envelope.getMinimum(dim));
        target.setMaximumValue(envelope.getMaximum(dim));
        target.setVerticalCRS(verticalCRS);
    }

    /**
     * Implementation of the public {@code setBounds} methods for the temporal extent.
     *
     * @param  envelope    The source envelope.
     * @param  target      The target temporal extent.
     * @param  crs         The envelope CRS (mandatory, can not be {@code null}).
     * @param  verticalCRS The temporal component of the given CRS (mandatory).
     * @throws UnsupportedOperationException If no implementation of {@code TemporalFactory} has been found
     *         on the classpath.
     */
    private static void setTemporalExtent(final Envelope envelope, final DefaultTemporalExtent target,
            final CoordinateReferenceSystem crs, final TemporalCRS temporalCRS)
    {
        final int dim = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), temporalCRS.getCoordinateSystem());
        assert dim >= 0 : crs; // Should not fail since 'temporalCRS' has been extracted from 'crs' by the caller.
        final DefaultTemporalCRS converter = DefaultTemporalCRS.castOrCopy(temporalCRS);
        target.setBounds(converter.toDate(envelope.getMinimum(dim)),
                         converter.toDate(envelope.getMaximum(dim)));
    }

    /**
     * Sets a geographic bounding box from the specified envelope.
     * If the envelope contains a CRS which is not geographic, then the bounding box will be transformed
     * to a geographic CRS (without datum shift if possible). Otherwise, the envelope is assumed already
     * in a geographic CRS using (<var>longitude</var>, <var>latitude</var>) axis order.
     *
     * @param  envelope The source envelope.
     * @param  target The target bounding box where to store envelope information.
     * @throws TransformException If the given envelope can not be transformed.
     */
    @Override
    public void setBounds(Envelope envelope, final DefaultGeographicBoundingBox target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        GeographicCRS normalizedCRS = ReferencingUtilities.toNormalizedGeographicCRS(crs);
        if (normalizedCRS == null) {
            if (crs != null) {
                normalizedCRS = CommonCRS.defaultGeographic();
            } else if (envelope.getDimension() != 2) {
                throw new TransformException(dimensionNotFound(Errors.Keys.MissingHorizontalDimension_1, crs));
            }
        }
        setGeographicExtent(envelope, target, crs, normalizedCRS);
    }

    /**
     * Sets a vertical extent with the value inferred from the given envelope.
     * Only the vertical ordinates are extracted; all other ordinates are ignored.
     *
     * @param  envelope The source envelope.
     * @param  target The target vertical extent where to store envelope information.
     * @throws TransformException If no vertical component can be extracted from the given envelope.
     */
    @Override
    public void setBounds(final Envelope envelope, final DefaultVerticalExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        if (verticalCRS == null && envelope.getDimension() != 1) {
            throw new TransformException(dimensionNotFound(Errors.Keys.MissingVerticalDimension_1, crs));
        }
        setVerticalExtent(envelope, target, crs, verticalCRS);
    }

    /**
     * Sets a temporal extent with the value inferred from the given envelope.
     * Only the vertical ordinates are extracted; all other ordinates are ignored.
     *
     * @param  envelope The source envelope.
     * @param  target The target temporal extent where to store envelope information.
     * @throws TransformException If no temporal component can be extracted from the given envelope.
     */
    @Override
    public void setBounds(final Envelope envelope, final DefaultTemporalExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final TemporalCRS temporalCRS = CRS.getTemporalComponent(crs);
        if (temporalCRS == null) { // Mandatory for the conversion from numbers to dates.
            throw new TransformException(dimensionNotFound(Errors.Keys.MissingTemporalDimension_1, crs));
        }
        setTemporalExtent(envelope, target, crs, temporalCRS);
    }

    /**
     * Sets the geographic, vertical and temporal extents with the values inferred from the given envelope.
     * If the given {@code target} has more geographic or vertical extents than needed (0 or 1), then the
     * extraneous extents are removed.
     *
     * @param  envelope The source envelope.
     * @param  target The target spatio-temporal extent where to store envelope information.
     * @throws TransformException If no temporal component can be extracted from the given envelope.
     */
    @Override
    public void setBounds(final Envelope envelope, final DefaultSpatialTemporalExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final SingleCRS horizontalCRS = CRS.getHorizontalComponent(crs);
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        final TemporalCRS temporalCRS = CRS.getTemporalComponent(crs);
        if (horizontalCRS == null && verticalCRS == null && temporalCRS == null) {
            throw new TransformException(dimensionNotFound(Errors.Keys.MissingSpatioTemporalDimension_1, crs));
        }
        /*
         * Try to set the geographic bounding box first, because this operation may fail with a
         * TransformException while the other operations (vertical and temporal) should not fail.
         * So doing the geographic part first help us to get a "all or nothing" behavior.
         */
        DefaultGeographicBoundingBox box = null;
        boolean useExistingBox = (horizontalCRS != null);
        final Collection<GeographicExtent> spatialExtents = target.getSpatialExtent();
        final Iterator<GeographicExtent> it = spatialExtents.iterator();
        while (it.hasNext()) {
            final GeographicExtent extent = it.next();
            if (extent instanceof GeographicBoundingBox) {
                if (useExistingBox && (extent instanceof DefaultGeographicBoundingBox)) {
                    box = (DefaultGeographicBoundingBox) extent;
                    useExistingBox = false;
                } else {
                    it.remove();
                }
            }
        }
        if (horizontalCRS != null) {
            if (box == null) {
                box = new DefaultGeographicBoundingBox();
                spatialExtents.add(box);
            }
            GeographicCRS normalizedCRS = ReferencingUtilities.toNormalizedGeographicCRS(crs);
            if (normalizedCRS == null) {
                normalizedCRS = CommonCRS.defaultGeographic();
            }
            setGeographicExtent(envelope, box, crs, normalizedCRS);
        }
        /*
         * Other dimensions (vertical and temporal).
         */
        if (verticalCRS != null) {
            VerticalExtent e = target.getVerticalExtent();
            if (!(e instanceof DefaultVerticalExtent)) {
                e = new DefaultVerticalExtent();
                target.setVerticalExtent(e);
            }
            setVerticalExtent(envelope, (DefaultVerticalExtent) e, crs, verticalCRS);
        } else {
            target.setVerticalExtent(null);
        }
        if (temporalCRS != null) {
            setTemporalExtent(envelope, target, crs, temporalCRS);
        } else {
            target.setExtent(null);
        }
    }

    /**
     * Initializes a horizontal, vertical and temporal extent with the values inferred from the given envelope.
     *
     * @param  envelope The source envelope.
     * @param  target The target extent where to store envelope information.
     * @throws TransformException If a coordinate transformation was required and failed.
     */
    @Override
    public void addElements(final Envelope envelope, final DefaultExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final SingleCRS horizontalCRS = CRS.getHorizontalComponent(crs);
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        final TemporalCRS temporalCRS = CRS.getTemporalComponent(crs);
        if (horizontalCRS == null && verticalCRS == null && temporalCRS == null) {
            throw new TransformException(dimensionNotFound(Errors.Keys.MissingSpatioTemporalDimension_1, crs));
        }
        if (horizontalCRS != null) {
            final DefaultGeographicBoundingBox extent = new DefaultGeographicBoundingBox();
            extent.setInclusion(Boolean.TRUE);
            setBounds(envelope, extent);
            target.getGeographicElements().add(extent);
        }
        if (verticalCRS != null) {
            final DefaultVerticalExtent extent = new DefaultVerticalExtent();
            setVerticalExtent(envelope, extent, crs, verticalCRS);
            target.getVerticalElements().add(extent);
        }
        if (temporalCRS != null) {
            final DefaultTemporalExtent extent = new DefaultTemporalExtent();
            setTemporalExtent(envelope, extent, crs, temporalCRS);
            target.getTemporalElements().add(extent);
        }
    }
}
