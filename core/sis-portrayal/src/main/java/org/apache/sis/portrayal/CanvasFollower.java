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

import java.util.logging.Logger;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * A listener of displacements in a source canvas which can reproduce the same displacement in a target canvas.
 * For example if a translation of 100 meters is applied in a source canvas, the same translation (in meters)
 * can be applied in the target canvas. This class does automatically the necessary conversions for taking in
 * account the differences in zoom levels and map projections. For example a translation of 10 pixels in one
 * canvas may map to a translation of 20 pixels in the other canvas for reproducing the same "real world" translation.
 *
 * <h2>Listeners</h2>
 * This class implements an unidirectional binding: changes in source are applied on target, but not the converse.
 * {@code CanvasFollower} listener needs to be registered explicitly as below. This is not done automatically for
 * allowing users to control when to listen to changes:
 *
 * {@preformat java
 *     source.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
 *     source.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
 *     target.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
 * }
 *
 * The {@link #dispose()} convenience method is provided for unregistering all the above.
 *
 * <h2>Multi-threading</h2>
 * This class is <strong>not</strong> thread-safe.
 * All events should be processed in the same thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public class CanvasFollower implements PropertyChangeListener, Disposable {
    /**
     * The canvas which is the source of zoom, translation or rotation events.
     */
    protected final PlanarCanvas source;

    /**
     * The canvas on which to apply the change of zoom, translation or rotation.
     */
    protected final PlanarCanvas target;

    /**
     * Whether to follow the source canvas in "real world" coordinates.
     * If {@code false}, displacements will be followed in pixel coordinates instead.
     */
    private boolean followRealWorld;

    /**
     * The effective value of {@link #followRealWorld}.
     * May be temporarily set to {@code false} if {@link #sourceToTarget} can not be computed.
     */
    private boolean effectiveRealWorld;

    /**
     * The transform from a change in source canvas to a change in target canvas.
     * Computed when first needed, and recomputed when the objective CRS changes.
     * A {@code null} value means that no change is needed or can not be done.
     *
     * @see #findSourceToTarget()
     */
    private MathTransform sourceToTarget;

    /**
     * Whether {@link #sourceToTarget} field is up to date.
     * Note that the field can be up-to-date and {@code null}.
     *
     * @see #findSourceToTarget()
     */
    private boolean isTransformUpdated;

    /**
     * Whether a change is in progress. This is for avoiding never-ending loop
     * if a bidirectional mapping or a cycle exists (A → B → C → A).
     */
    private boolean changing;

    /**
     * Creates a new listener for synchronizing "objective to display" transform changes
     * between the specified canvas. This is a unidirectional binding: changes in source
     * are applied on target, but not the converse.
     *
     * <p>Caller needs to register this listener explicitly as below
     * (this is not done automatically by this constructor):</p>
     *
     * {@preformat java
     *     source.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
     *     source.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
     *     target.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
     * }
     *
     * @param  source  the canvas which is the source of zoom, pan or rotation events.
     * @param  target  the canvas on which to apply the changes of zoom, pan or rotation.
     */
    public CanvasFollower(final PlanarCanvas source, final PlanarCanvas target) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        this.source = source;
        this.target = target;
        effectiveRealWorld = followRealWorld = true;
    }

    /**
     * Returns whether this instance is following changes in "real world" coordinates.
     * If {@code true} (the default value), then changes applied on the {@linkplain #source} canvas
     * and converted into changes to apply on the {@link #target} canvas in such a way that the two
     * canvas got the same translations in real world units. It may result in a different amount of
     * pixels is the two canvas have different zoom level, or a different direction if a canvas is
     * rotated relatively to the other canvas.
     *
     * @return whether this instance is following changes in "real world" coordinates.
     */
    public boolean getFollowRealWorld() {
        return followRealWorld;
    }

    /**
     * Sets whether this instance should following changes in "real world" coordinates.
     * The default value is {@code true}. If this is set to {@code false}, then the same changes
     * in pixel coordinates will be applied on canvas regardless the difference in rotation or zoom level.
     *
     * @param  real  whether this instance should following changes in "real world" coordinates.
     */
    public void setFollowRealWorld(final boolean real) {
        if (real != followRealWorld) {
            effectiveRealWorld = followRealWorld = real;
            isTransformUpdated = false;
            sourceToTarget     = null;
        }
    }

    /**
     * Invoked when the objective CRS, zoom, translation or rotation changed on a map that we are tracking.
     * If the event is an instance of {@link TransformChangeEvent}, then this method applies the same change
     * on the {@linkplain #target} canvas.
     *
     * @param  event  a change in the canvas that this listener is tracking.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (event instanceof TransformChangeEvent) {
            final TransformChangeEvent te = (TransformChangeEvent) event;
            if (!changing && te.getReason().isNavigation()) try {
                changing = true;
                if (!isTransformUpdated) {
                    findSourceToTarget();               // May update the `effectiveRealWorld` field.
                }
                if (effectiveRealWorld) {
                    AffineTransform before = convertObjectiveChange(te.getObjectiveChange2D().orElse(null));
                    if (before != null) {
                        target.transformObjectiveCoordinates(before);
                        return;
                    }
                }
                te.getDisplayChange2D().ifPresent(target::transformDisplayCoordinates);
            } finally {
                changing = false;
            }
        } else if (PlanarCanvas.OBJECTIVE_CRS_PROPERTY.equals(event.getPropertyName())) {
            isTransformUpdated = false;
            sourceToTarget = null;
        }
    }

    /**
     * Finds the transform to use for converting changes from {@linkplain #source} canvas to {@linkplain #target} canvas.
     * This method should be invoked only if {@link #isTransformUpdated} is {@code false}. After this method returned,
     * {@link #sourceToTarget} contains the transform to use, which may be {@code null} if none.
     */
    private void findSourceToTarget() {
        sourceToTarget     = null;
        effectiveRealWorld = false;
        isTransformUpdated = true;      // If an exception occurs, use above setting.
        if (followRealWorld) {
            final CoordinateReferenceSystem sourceCRS = source.getObjectiveCRS();
            final CoordinateReferenceSystem targetCRS = target.getObjectiveCRS();
            if (sourceCRS != null && targetCRS != null) try {
                GeographicBoundingBox aoi;
                try {
                    aoi = target.getGeographicArea().orElse(null);
                } catch (RenderException e) {
                    canNotCompute(e);
                    aoi = null;
                }
                sourceToTarget = CRS.findOperation(sourceCRS, targetCRS, aoi).getMathTransform();
                if (sourceToTarget.isIdentity()) {
                    sourceToTarget = null;
                }
                effectiveRealWorld = true;
            } catch (FactoryException e) {
                canNotCompute(e);
                // Stay with "changes in display units" mode.
            }
        }
    }

    /**
     * Converts a change from units of the source CRS to units of the target CRS.
     * If that change can not be computed, the caller will fallback on a change
     * in display units (typically pixels).
     *
     * @param  before  the change in units of the {@linkplain #source} objective CRS.
     * @return  the same change but in units of the {@linkplain #target} objective CRS,
     *          or {@code null} if it can not be computed.
     */
    private AffineTransform convertObjectiveChange(final AffineTransform before) {
        if (sourceToTarget == null) {
            return before;
        }
        if (before != null) {
            final DirectPosition poi = target.getPointOfInterest(true);
            if (poi != null) try {
                AffineTransform t = AffineTransforms2D.castOrCopy(MathTransforms.linear(sourceToTarget, poi));
                AffineTransform c = t.createInverse();
                c.preConcatenate(before);
                c.preConcatenate(t);
                return c;
            } catch (TransformException | NoninvertibleTransformException e) {
                canNotCompute(e);
            }
        }
        return null;
    }

    /**
     * Invoked when the {@link #sourceToTarget} transform can not be computed,
     * or when an optional information required for that transform is missing.
     * This method assumes that the public caller (possibly indirectly) is
     * {@link #propertyChange(PropertyChangeEvent)}.
     */
    private static void canNotCompute(final Exception e) {
        Logging.recoverableException(Logger.getLogger(Modules.PORTRAYAL), CanvasFollower.class, "propertyChange", e);
    }

    /**
     * Removes all listeners documented in {@linkplain CanvasFollower class javadoc}.
     * This method should be invoked when {@code CanvasFollower} is no longer needed
     * for avoiding memory leak.
     */
    @Override
    public void dispose() {
        source.removePropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
        source.removePropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
        target.removePropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
    }
}
