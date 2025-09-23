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
package org.apache.sis.portrayal;

import java.util.Optional;
import java.util.OptionalDouble;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.operation.CoordinateOperationContext;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.Units;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.system.Configuration;


/**
 * Contextual information for allowing {@link Canvas} to select the most appropriate
 * coordinate operation for the viewed area and resolution.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")   // Not intended to be serialized.
final class CanvasContext extends CoordinateOperationContext {
    /**
     * Desired resolution in display units (usually pixels). This is used for avoiding
     * the cost of transformations having too much accuracy for the current zoom level.
     *
     * @see Canvas#findTransform(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)
     */
    @Configuration
    private static final double DISPLAY_RESOLUTION = 1;

    /**
     * Transformation from {@linkplain Canvas#getObjectiveCRS() objective CRS} to a geographic CRS, or {@code null} if
     * none can be found. The geographic CRS (operation target) has (longitude, latitude) axes in degrees but the same
     * geodetic datum than the objective CRS, so the prime meridian is not necessarily Greenwich. This is recomputed
     * immediately after a change of {@link Canvas#objectiveCRS} or {@link Canvas#pointOfInterest} because it will be
     * needed anyway for {@link Canvas#findTransform(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)}.
     *
     * @see Canvas#getGeographicArea()
     * @see Canvas#objectiveToGeographic(CoordinateReferenceSystem)
     */
    private CoordinateOperation objectiveToGeographic;

    /**
     * The geographic area, computed when first requested and saved for reuse. This is reset to {@code null} every time
     * that {@link Canvas#objectiveCRS}, {@link Canvas#objectiveToDisplay} or {@link Canvas#displayBounds} is modified.
     *
     * @see Canvas#GEOGRAPHIC_AREA_PROPERTY
     * @see #getGeographicArea(Canvas)
     */
    private GeographicBoundingBox geographicArea;

    /**
     * The resolution in metres, computed when first requested and saved for reuse. This is reset to {@code null}
     * every time {@link Canvas#objectiveCRS}, {@link Canvas#objectiveToDisplay} or {@link Canvas#pointOfInterest}
     * is modified. Value 0 means that the value has not yet been computed.
     *
     * @see Canvas#SPATIAL_RESOLUTION_PROPERTY
     * @see #getSpatialResolution(Canvas)
     */
    private double resolution;

    /**
     * Creates a new context.
     */
    CanvasContext() {
    }

    /**
     * Sets the operation from {@link Canvas#objectiveCRS} to geographic CRS.
     *
     * @param  op  the conversion from objective CRS to geographic CRS, or {@code null} if none.
     */
    final void setObjectiveToGeographic(final CoordinateOperation op) {
        objectiveToGeographic = op;
        clear();
    }

    /**
     * Clears information that depends on {@link Canvas#objectiveToDisplay}.
     * This method assumes that {@link Canvas#objectiveCRS} is still valid.
     */
    final void clear() {
        geographicArea = null;
        resolution     = 0;
    }

    /**
     * Clears only some information, depending on whether the modified property is point of interest
     * or the display bounds.
     *
     * @param  poi  {@code true} if the modified property is the point of interest,
     *              {@code false} if the modified property is the display bounds.
     */
    final void partialClear(final boolean poi) {
        if (poi) resolution = 0;
        else geographicArea = null;
    }

    /**
     * Returns the geographic area, or an empty value if none.
     *
     * @see Canvas#getGeographicArea()
     */
    final Optional<GeographicBoundingBox> getGeographicArea(final Canvas canvas) throws TransformException {
        if (geographicArea != null) {
            return Optional.of(geographicArea);
        }
        recompute(canvas);
        return Optional.ofNullable(geographicArea);
    }

    /**
     * Returns the spatial resolution, or an empty value if none.
     *
     * @see Canvas#getSpatialResolution()
     */
    final OptionalDouble getSpatialResolution(final Canvas canvas) throws TransformException {
        if (!(resolution > 0)) {
            recompute(canvas);
            if (!(resolution > 0)) {
                return OptionalDouble.empty();
            }
        }
        return OptionalDouble.of(resolution);
    }

    /**
     * Recomputes {@link #geographicArea} and {@link #resolution} fields that are not valid.
     * This method assumes that {@link #objectiveToGeographic} is {@code null} or valid.
     */
    @SuppressWarnings("fallthrough")
    private void recompute(final Canvas canvas) throws TransformException {
        if (objectiveToGeographic == null) {
            return;
        }
        final LinearTransform objectiveToDisplay = canvas.getObjectiveToDisplay();
        final MathTransform displayToGeographic = MathTransforms.concatenate(
                            objectiveToDisplay.inverse(),
                            objectiveToGeographic.getMathTransform());
        /*
         * Compute geographic area using an operation going directly from display CRS
         * to geographic CRS (do not go to objective CRS as an intermediate step,
         * because doing 2 envelope transformations increases the errors).
         */
        if (geographicArea == null && !canvas.displayBounds.isAllNaN()) {
            final GeneralEnvelope bounds = Envelopes.transform(displayToGeographic, canvas.displayBounds);
            bounds.setCoordinateReferenceSystem(objectiveToGeographic.getTargetCRS());
            final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox();
            bbox.setBounds(bounds);     // Will perform longitude rotation to Greenwich if needed.
            bbox.transitionTo(DefaultGeographicBoundingBox.State.FINAL);
            geographicArea = bbox;
        }
        /*
         * Estimate spatial resolution at the point of interest. The calculation is done in
         * (longitude, latitude, height) space where the height is optional. The angles are
         * converted to meters using the radius of conformal sphere.
         */
        if (!(resolution > 0)) {
            final double[] poi = canvas.getObjectivePOI();
            if (poi != null) {
                objectiveToDisplay.transform(poi, 0, poi, 0, 1);
                final Matrix derivative = MathTransforms.derivativeAndTransform(displayToGeographic, poi, 0, poi, 0);
                final double[] vector   = new double[derivative.getNumCol()];
                final double[] combined = new double[derivative.getNumRow()];
                for (int j=combined.length; --j>=0;) {                      // Process latitude (1) before longitude (0).
                    for (int i=0; i<vector.length; i++) {
                        vector[i] = derivative.getElement(j,i);
                    }
                    double m = MathFunctions.magnitude(vector);
                    switch (j) {
                        case 0: m *= Math.cos(combined[1]);                 // Adjust longitude, then fall through.
                        case 1: m  = Math.toRadians(m); break;              // Latitude (this case) or longitude (case 0).
                        // Other cases: assume value already in metres.
                    }
                    combined[j] = m;
                }
                DatumOrEnsemble.getEllipsoid(objectiveToGeographic.getTargetCRS()).ifPresent((ellipsoid) -> {
                    double radius = Formulas.radiusOfConformalSphere(ellipsoid, combined[1]);
                    radius = ellipsoid.getAxisUnit().getConverterTo(Units.METRE).convert(radius);
                    resolution = MathFunctions.magnitude(combined) * radius;
                });
            }
        }
    }

    /**
     * Sets the {@link CoordinateOperationContext} object to the desired area and accuracy
     * of the coordinate operation to obtain.
     */
    final void refresh(final Canvas canvas) throws TransformException {
        recompute(canvas);
        setAreaOfInterest(geographicArea);                          // null for default behavior.
        setDesiredAccuracy(resolution * DISPLAY_RESOLUTION);        // 0 for default behavior.
    }
}
