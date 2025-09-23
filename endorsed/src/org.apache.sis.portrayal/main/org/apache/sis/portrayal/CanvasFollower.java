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

import java.util.Objects;
import java.util.Optional;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * A listener of displacements in a source canvas which can reproduce the same displacement in a target canvas.
 * For example if a translation of 100 meters is applied in a source canvas, the same translation (in meters)
 * can be applied in the target canvas. This class does automatically the necessary conversions for taking in
 * account the differences in zoom levels and map projections. For example, a translation of 10 pixels in one
 * canvas may map to a translation of 20 pixels in the other canvas for reproducing the same "real world" translation.
 *
 * <h2>Listeners</h2>
 * {@code CanvasFollower} listeners need to be registered explicitly by a call to the {@link #initialize()} method.
 * The {@link #dispose()} convenience method is provided for unregistering all those listeners.
 * The listeners registered by this class implement an unidirectional binding:
 * changes in source are applied on target, but not the converse.
 *
 * <h2>Multi-threading</h2>
 * This class is <strong>not</strong> thread-safe.
 * All events should be processed in the same thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
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
     * Whether listeners have been registered.
     */
    private boolean initialized;

    /**
     * Whether to disable the following of source canvas. Other events such as changes
     * of objective CRS are still listened in order to update the fields of this class.
     */
    private boolean disabled;

    /**
     * Whether to follow the source canvas in "real world" coordinates.
     * If {@code false}, displacements will be followed in pixel coordinates instead.
     */
    private boolean followRealWorld;

    /**
     * Conversions from source display coordinates to target display coordinates.
     * Computed when first needed, and recomputed when the objective CRS or the
     * "display to objective" transform change.
     *
     * @see #getDisplayTransform()
     */
    private MathTransform2D displayTransform;

    /**
     * Conversions from source objective coordinates to target objective coordinates.
     * Computed when first needed, and recomputed when the objective CRS changes.
     * A {@code null} value means that no change is needed or cannot be done.
     *
     * @see #findObjectiveTransform(String)
     */
    private MathTransform objectiveTransform;

    /**
     * Whether an attempt to compute {@link #displayTransform} has already been done.
     * The {@code displayTransform} field may still be null if the attempt failed.
     * Value can be {@link #VALID}, {@link #OUTDATED}, {@link #UNKNOWN} or {@link #ERROR}.
     */
    private byte displayTransformStatus;

    /**
     * Whether an attempt to compute {@link #objectiveTransform} has already been done.
     * Note that the {@link #objectiveTransform} field can be up-to-date and {@code null}.
     * Value can be {@link #VALID}, {@link #OUTDATED}, {@link #UNKNOWN} or {@link #ERROR}.
     *
     * @see #findObjectiveTransform(String)
     */
    private byte objectiveTransformStatus;

    /**
     * Enumeration values for {@link #displayTransformStatus} and {@link #objectiveTransformStatus}.
     */
    private static final byte VALID = 0, OUTDATED = 1, UNKNOWN = 2, ERROR = 3;

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
     * <p>Caller needs to register listeners by a call to the {@link #initialize()} method.
     * This is not done automatically by this constructor for allowing users to control
     * when to start listening to changes.</p>
     *
     * @param  source  the canvas which is the source of zoom, pan or rotation events.
     * @param  target  the canvas on which to apply the changes of zoom, pan or rotation.
     */
    public CanvasFollower(final PlanarCanvas source, final PlanarCanvas target) {
        this.source = Objects.requireNonNull(source);
        this.target = Objects.requireNonNull(target);
        followRealWorld = true;
        displayTransformStatus   = OUTDATED;
        objectiveTransformStatus = OUTDATED;
    }

    /**
     * Registers all listeners needed by this object. This method must be invoked at least
     * once after {@linkplain #CanvasFollower(PlanarCanvas, PlanarCanvas) construction},
     * but not necessarily immediately after (it is okay to defer until first needed).
     * The default implementation registers the following listeners:
     *
     * {@snippet lang="java" :
     *     source.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
     *     target.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
     *     source.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
     *     target.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
     *     }
     *
     * This method is idempotent (it is okay to invoke it twice).
     *
     * @see #dispose()
     */
    public void initialize() {
        if (!initialized) {
            initialized = true;     // Set first in case an exception is thrown below.
            source.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
            target.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
            source.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
            target.addPropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
        }
    }

    /**
     * Returns {@code true} if this object stopped to replicate changes from source canvas to target canvas.
     * If {@code true}, this object continues to listen to changes in order to keep its state consistent,
     * but does not replicate those changes on the target canvas.
     *
     * <p>A non-{@linkplain #initialize() initialized} object is considered disabled.</p>
     *
     * @return whether this object stopped to replicate changes from source canvas to target canvas.
     */
    public boolean isDisabled() {
        return disabled | !initialized;
    }

    /**
     * Sets whether to stop to replicate changes from source canvas to target canvas.
     * It does not stop this object to listen to events, because it is necessary for keeping its state consistent.
     *
     * @param  stop  {@code true} for stopping to replicate changes from source canvas to target canvas.
     */
    public void setDisabled(final boolean stop) {
        disabled = stop;
    }

    /**
     * Returns whether this instance is following changes in "real world" coordinates.
     * If {@code true} (the default value), then changes applied on the {@linkplain #source} canvas
     * and converted into changes to apply on the {@link #target} canvas in such a way that the two
     * canvas got the same translations in real world units. It may result in a different number of
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
            followRealWorld          = real;
            displayTransform         = null;
            objectiveTransform       = null;
            displayTransformStatus   = OUTDATED;
            objectiveTransformStatus = OUTDATED;
        }
    }

    /**
     * Returns the objective coordinates of the Point Of Interest (POI) in source canvas.
     * This information is used when the source and target canvases do not use the same CRS.
     * Changes in "real world" coordinates on the {@linkplain #target} canvas are guaranteed
     * to reflect the changes in "real world" coordinates of the {@linkplain #source} canvas
     * at that location only. At all other locations, the "real world" coordinate changes
     * may differ because of map projection deformations.
     *
     * <p>The default implementation computes the value from {@link #getSourceDisplayPOI()}
     * if present, or fallback on {@code source.getPointOfInterest(true)} otherwise.
     * Subclasses can override this method for using a different point of interest.</p>
     *
     * <p>The CRS associated to the position shall be {@link PlanarCanvas#getObjectiveCRS()}.
     * For performance reason, this is not verified by this {@code CanvasFollower} class.</p>
     *
     * @return objective coordinates in source canvas where displacements, zooms and rotations
     *         applied on the source canvas should be mirrored exactly on the target canvas.
     *
     * @see PlanarCanvas#getPointOfInterest(boolean)
     */
    public DirectPosition getSourceObjectivePOI() {
        final Point2D p = getSourceDisplayPOI().orElse(null);
        if (p != null) try {
            final DirectPosition2D poi = new DirectPosition2D(p);
            source.objectiveToDisplay.inverseTransform(poi, poi);
            return poi;
        } catch (NoninvertibleTransformException e) {
            canNotCompute("getSourceObjectivePOI", e);
        }
        return source.getPointOfInterest(true);
    }

    /**
     * Returns the display coordinates of the Point Of Interest (POI) in source canvas.
     * This method provides the same information as {@link #getSourceObjectivePOI()},
     * but in units that are more convenient for expressing the location of mouse cursor
     * for example.
     *
     * <p>The default implementation returns an empty value.</p>
     *
     * @return display coordinates in source canvas where displacements, zooms and rotations
     *         applied on the source canvas should be mirrored exactly on the target canvas.
     */
    public Optional<Point2D> getSourceDisplayPOI() {
        return Optional.empty();
    }

    /**
     * Returns the transform from source display coordinates to target display coordinates.
     * This transform may change every time that a zoom; translation or rotation is applied
     * on at least one canvas. The transform may be absent if an error prevent to compute it,
     * for example is no coordinate operation has been found between the objective CRS of the
     * source and target canvases.
     *
     * @return transform from source display coordinates to target display coordinates.
     */
    public Optional<MathTransform2D> getDisplayTransform() {
        if (displayTransformStatus != VALID) {
            if (displayTransformStatus != OUTDATED) {
                return Optional.empty();
            }
            displayTransformStatus = ERROR;             // Set now in case an exception is thrown below.
            if (objectiveTransformStatus == VALID || findObjectiveTransform("getDisplayTransform")) try {
                /*
                 * Compute (source display to objective) → (map projection) → (target objective to display).
                 * If we can work directly on `AffineTransform` instances, it should be more efficient than
                 * the generic code. But the two `if … else` branches below compute the same thing
                 * (ignoring rounding errors).
                 */
                final MathTransform objectiveTransform = this.objectiveTransform;
                if (objectiveTransform == null || objectiveTransform instanceof AffineTransform) {
                    AffineTransform tr = source.objectiveToDisplay.createInverse();
                    if (objectiveTransform != null) {
                        tr.preConcatenate((AffineTransform) objectiveTransform);
                    }
                    tr.preConcatenate(target.objectiveToDisplay);
                    displayTransform = new AffineTransform2D(tr);
                } else {
                    displayTransform = MathTransforms.bidimensional(MathTransforms.concatenate(
                            source.getObjectiveToDisplay().inverse(), objectiveTransform,
                            target.getObjectiveToDisplay()));
                }
                displayTransformStatus = VALID;
            } catch (NoninvertibleTransformException | org.opengis.referencing.operation.NoninvertibleTransformException e) {
                canNotCompute("getDisplayTransform", e);
            }
        }
        return Optional.ofNullable(displayTransform);
    }

    /**
     * Invoked when the objective CRS, zoom, translation or rotation changed on a map that we are tracking.
     * If the event is an instance of {@link TransformChangeEvent}, then this method applies the same change
     * on the {@linkplain #target} canvas.
     *
     * <p>This method delegates part of its work to the following methods,
     * which can be overridden for altering the changes:</p>
     *
     * <ul>
     *   <li>{@link #transformObjectiveCoordinates(TransformChangeEvent, AffineTransform)}
     *        if {@linkplain #getFollowRealWorld() following real world coordinates}.</li>
     *   <li>{@link #transformDisplayCoordinates(TransformChangeEvent, AffineTransform)}
     *        if following pixel coordinates instead of real world.</li>
     *   <li>{@link #transformedSource(TransformChangeEvent)} after the change has been applied on {@linkplain #source}.</li>
     *   <li>{@link #transformedTarget(TransformChangeEvent)} after the change has been applied on {@linkplain #target}.</li>
     * </ul>
     *
     * @param  event  a change in the canvas that this listener is tracking.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (!changing && event instanceof TransformChangeEvent) try {
            final TransformChangeEvent te = (TransformChangeEvent) event;
            displayTransformStatus = OUTDATED;
            changing = true;
            if (te.isSameSource(source)) {
                transformedSource(te);
                if (!disabled && filter(te)) {
                    if (followRealWorld && (objectiveTransformStatus == VALID || findObjectiveTransform("propertyChange"))) {
                        AffineTransform before = te.getObjectiveChange2D().orElse(null);
                        if (before != null) try {
                            /*
                             * Converts a change from units of the source CRS to units of the target CRS.
                             * If that change cannot be computed, fallback on a change in display units.
                             * The POI may be null, but this is okay if the transform is linear.
                             */
                            if (objectiveTransform != null) {
                                DirectPosition poi = getSourceObjectivePOI();
                                AffineTransform t = AffineTransforms2D.castOrCopy(MathTransforms.tangent(objectiveTransform, poi));
                                AffineTransform c = t.createInverse();
                                c.preConcatenate(before);
                                c.preConcatenate(t);
                                before = c;
                            }
                            transformObjectiveCoordinates(te, before);
                            return;
                        } catch (NullPointerException | TransformException | NoninvertibleTransformException e) {
                            canNotCompute("propertyChange", e);
                        }
                    }
                    te.getDisplayChange2D().ifPresent((after) -> transformDisplayCoordinates(te, after));
                }
            } else if (te.isSameSource(target)) {
                transformedTarget(te);
            }
        } finally {
            changing = false;
        } else if (PlanarCanvas.OBJECTIVE_CRS_PROPERTY.equals(event.getPropertyName())) {
            displayTransform         = null;
            objectiveTransform       = null;
            displayTransformStatus   = OUTDATED;
            objectiveTransformStatus = OUTDATED;
        }
    }

    /**
     * Returns {@code true} if this listener should replicate the following changes on the target canvas.
     * The default implementation returns {@code true} if the transform reason is
     * {@link TransformChangeEvent.Reason#OBJECTIVE_NAVIGATION} or
     * {@link TransformChangeEvent.Reason#DISPLAY_NAVIGATION}.
     *
     * @param  event  a transform change event that occurred on the {@linkplain #source} canvas.
     * @return  whether to replicate that change on the {@linkplain #target} canvas.
     */
    protected boolean filter(final TransformChangeEvent event) {
        return event.getReason().isNavigation();
    }

    /**
     * Invoked by {@link #propertyChange(PropertyChangeEvent)} for updating the transform of the target canvas
     * in units of the objective CRS. The {@linkplain #target} canvas is updated by this method as if the given
     * transform was applied <em>before</em> its current <i>objective to display</i> transform.
     *
     * <p>The default implementation delegates to {@link PlanarCanvas#transformObjectiveCoordinates(AffineTransform)}.
     * Subclasses can override if they need to transform additional data.</p>
     *
     * @param  event   the change in the {@linkplain #source} canvas.
     * @param  before  the change to apply on the {@linkplain #target} canvas, in unit of objective CRS.
     *
     * @see PlanarCanvas#transformObjectiveCoordinates(AffineTransform)
     */
    protected void transformObjectiveCoordinates(final TransformChangeEvent event, final AffineTransform before) {
        target.transformObjectiveCoordinates(before);
    }

    /**
     * Invoked by {@link #propertyChange(PropertyChangeEvent)} for updating the transform of the target canvas
     * in display units (typically pixels). The {@linkplain #target} canvas is updated by this method as if the
     * given transform was applied <em>after</em> its current <i>objective to display</i> transform.
     *
     * <p>The default implementation delegates to {@link PlanarCanvas#transformDisplayCoordinates(AffineTransform)}.
     * Subclasses can override if they need to transform additional data.</p>
     *
     * @param  event  the change in the {@linkplain #source} canvas.
     * @param  after  the change to apply on the {@linkplain #target} canvas, in display units (typically pixels).
     *
     * @see PlanarCanvas#transformDisplayCoordinates(AffineTransform)
     */
    protected void transformDisplayCoordinates(final TransformChangeEvent event, final AffineTransform after) {
        target.transformDisplayCoordinates(after);
    }

    /**
     * Invoked after the source "objective to display" transform has been updated.
     * This method is invoked automatically by {@link #propertyChange(PropertyChangeEvent)}.
     * The default implementation does nothing.
     * Subclasses can override if they need to transform additional data.
     *
     * @param  event  the change which has been applied on the {@linkplain #source} canvas.
     */
    protected void transformedSource(TransformChangeEvent event) {
    }

    /**
     * Invoked after the target "objective to display" transform has been updated.
     * This method is invoked automatically by {@link #propertyChange(PropertyChangeEvent)}.
     * The default implementation does nothing.
     * Subclasses can override if they need to transform additional data.
     *
     * @param  event  the change which has been applied on the {@linkplain #target} canvas.
     */
    protected void transformedTarget(TransformChangeEvent event) {
    }

    /**
     * Finds the transform to use for converting changes from {@linkplain #source} canvas to {@linkplain #target} canvas.
     * This method should be invoked only if {@link #objectiveTransformStatus} is not {@link #VALID}. After this method
     * returned, {@link #objectiveTransform} contains the transform to use, which may be {@code null} if none.
     *
     * @param  caller  the public method which is invoked this private method. Used only for logging purposes.
     * @return whether a transform has been computed.
     */
    private boolean findObjectiveTransform(final String caller) {
        if (objectiveTransformStatus == OUTDATED) {
            displayTransform         = null;
            objectiveTransform       = null;
            displayTransformStatus   = OUTDATED;
            objectiveTransformStatus = ERROR;      // If an exception occurs, use above setting.
            final CoordinateReferenceSystem sourceCRS = source.getObjectiveCRS();
            final CoordinateReferenceSystem targetCRS = target.getObjectiveCRS();
            if (sourceCRS != null && targetCRS != null) try {
                GeographicBoundingBox aoi;
                try {
                    aoi = target.getGeographicArea().orElse(null);
                } catch (RenderException e) {
                    canNotCompute(caller, e);
                    aoi = null;
                }
                objectiveTransform = CRS.findOperation(sourceCRS, targetCRS, aoi).getMathTransform();
                if (objectiveTransform.isIdentity()) {
                    objectiveTransform = null;
                }
                objectiveTransformStatus = VALID;
                return true;
            } catch (FactoryException e) {
                canNotCompute(caller, e);
                // Stay with "changes in display units" mode.
            } else {
                objectiveTransformStatus = UNKNOWN;
            }
        }
        return false;
    }

    /**
     * Invoked when the {@link #objectiveTransform} transform cannot be computed,
     * or when an optional information required for that transform is missing.
     * This method assumes that the public caller (possibly indirectly) is
     * {@link #propertyChange(PropertyChangeEvent)}.
     *
     * @param  caller  the public method which is invoked this private method. Used only for logging purposes.
     * @param  e  the exception that occurred.
     */
    private static void canNotCompute(final String caller, final Exception e) {
        Logging.recoverableException(Observable.LOGGER, CanvasFollower.class, caller, e);
    }

    /**
     * Removes all listeners documented in the {@link #initialize()} method.
     * This method should be invoked when {@code CanvasFollower} is no longer needed, in order to avoid memory leak.
     *
     * @see #initialize()
     */
    @Override
    public void dispose() {
        source.removePropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
        target.removePropertyChangeListener(PlanarCanvas.OBJECTIVE_TO_DISPLAY_PROPERTY, this);
        source.removePropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
        target.removePropertyChangeListener(PlanarCanvas.OBJECTIVE_CRS_PROPERTY, this);
        initialized = false;
    }
}
