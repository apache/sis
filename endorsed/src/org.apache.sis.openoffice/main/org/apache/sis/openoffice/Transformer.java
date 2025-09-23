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
package org.apache.sis.openoffice;

import java.util.Arrays;
import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.storage.DataStoreException;


/**
 * Applies a coordinate transformation between two CRS.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class Transformer {
    /**
     * The geographic area of interest. All values are in degrees. Prime meridian is Greenwich.
     * Datum does not need to be specified since area of interest is an approximated information.
     *
     * @see #hasAreaOfInterest()
     */
    private double westBoundLongitude, eastBoundLongitude,
                   southBoundLatitude, northBoundLatitude;

    /**
     * The coordinate operation.
     */
    private CoordinateOperation operation;

    /**
     * The first error that occurred while transforming points, or {@code null} if none.
     */
    TransformException warning;

    /**
     * Creates a new transformer.
     */
    Transformer(final ReferencingFunctions caller, final CoordinateReferenceSystem sourceCRS,
            final String targetCRS, final double[][] points) throws FactoryException, DataStoreException
    {
        /*
         * Computes the area of interest.
         */
        final GeographicCRS domainCRS = ReferencingUtilities.toNormalizedGeographicCRS(sourceCRS, false, false);
        if (domainCRS != null) {
            final MathTransform toDomainOfValidity = CRS.findOperation(sourceCRS, domainCRS, null).getMathTransform();
            final int dimension = toDomainOfValidity.getSourceDimensions();
            final double[] domainCoord = new double[toDomainOfValidity.getTargetDimensions()];
            if (domainCoord.length >= 2) {
                westBoundLongitude = Double.POSITIVE_INFINITY;
                southBoundLatitude = Double.POSITIVE_INFINITY;
                eastBoundLongitude = Double.NEGATIVE_INFINITY;
                northBoundLatitude = Double.NEGATIVE_INFINITY;
                if (points != null) {
                    for (final double[] coord : points) {
                        if (coord != null && coord.length == dimension) {
                            try {
                                toDomainOfValidity.transform(coord, 0, domainCoord, 0, 1);
                            } catch (TransformException e) {
                                if (warning == null) {
                                    warning = e;
                                }
                                continue;
                            }
                            final double x = domainCoord[0];
                            final double y = domainCoord[1];
                            if (x < westBoundLongitude) westBoundLongitude = x;
                            if (x > eastBoundLongitude) eastBoundLongitude = x;
                            if (y < southBoundLatitude) southBoundLatitude = y;
                            if (y > northBoundLatitude) northBoundLatitude = y;
                        }
                    }
                }
            }
        }
        /*
         * Get the coordinate operation from the cache if possible, or compute it otherwise.
         */
        final boolean hasAreaOfInterest = hasAreaOfInterest();
        final CacheKey<CoordinateOperation> key = new CacheKey<>(CoordinateOperation.class, targetCRS, sourceCRS,
                hasAreaOfInterest ? new double[] {westBoundLongitude, eastBoundLongitude,
                                                  southBoundLatitude, northBoundLatitude} : null);
        operation = key.peek();
        if (operation == null) {
            final Cache.Handler<CoordinateOperation> handler = key.lock();
            try {
                operation = handler.peek();
                if (operation == null) {
                    operation = CRS.findOperation(sourceCRS, caller.getCRS(targetCRS),
                            hasAreaOfInterest ? getAreaOfInterest() : null);
                }
            } finally {
                handler.putAndUnlock(operation);
            }
        }
    }

    /**
     * Returns {@code true} if the area of interest is non-empty.
     */
    final boolean hasAreaOfInterest() {
        return (westBoundLongitude < eastBoundLongitude) && (southBoundLatitude < northBoundLatitude);
    }

    /**
     * Returns the area of interest. It is caller's responsibility to verify that
     * {@link #hasAreaOfInterest()} returned {@code true} before to invoke this method.
     */
    final GeographicBoundingBox getAreaOfInterest() {
        return new DefaultGeographicBoundingBox(westBoundLongitude, eastBoundLongitude,
                                                southBoundLatitude, northBoundLatitude);
    }

    /**
     * Returns the accuracy in metres.
     */
    final double getAccuracy() {
        return AbstractCoordinateOperation.castOrCopy(operation).getLinearAccuracy();
    }

    /**
     * Transforms the given points.
     */
    final double[][] transform(final double[][] points) {
        final MathTransform         mt       = operation.getMathTransform();
        final GeneralDirectPosition sourcePt = new GeneralDirectPosition(mt.getSourceDimensions());
        final GeneralDirectPosition targetPt = new GeneralDirectPosition(mt.getTargetDimensions());
        final double[][] result = new double[points.length][];
        for (int j=0; j<points.length; j++) {
            final double[] coords = points[j];
            if (coords != null) {                                               // Paranoiac check.
                for (int i=sourcePt.coordinates.length; --i>=0;) {
                    sourcePt.coordinates[i] = (i < coords.length) ? coords[i] : 0;
                }
                try {
                    result[j] = mt.transform(sourcePt, targetPt).getCoordinate();
                } catch (TransformException exception) {
                    /*
                     * The coordinate operation failed for this particular point. But maybe it will
                     * succeed for another point. Set the values to NaN and continue the loop. Note:
                     * we will report the failure for logging purpose, but only the first one since
                     * all subsequent failures are likely to be the same one.
                     */
                    final double[] pad = new double[mt.getTargetDimensions()];
                    Arrays.fill(pad, Double.NaN);
                    result[j] = pad;
                    if (warning == null) {
                        warning = exception;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Transforms the given envelope.
     */
    final double[][] transformEnvelope(final double[][] points) throws TransformException {
        final double[] min = new double[operation.getMathTransform().getSourceDimensions()];
        final double[] max = new double[min.length];
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        for (final double[] p : points) {
            if (p != null) {                                                    // Paranoiac check.
                for (int i=Math.min(min.length, p.length); --i >= 0;) {
                    final double v = p[i];
                    if (v < min[i]) min[i] = v;
                    if (v > max[i]) max[i] = v;
                }
            }
        }
        final GeneralEnvelope result = Envelopes.transform(operation, new GeneralEnvelope(min, max));
        return new double[][] {
            result.getLowerCorner().getCoordinate(),
            result.getUpperCorner().getCoordinate()
        };
    }
}
