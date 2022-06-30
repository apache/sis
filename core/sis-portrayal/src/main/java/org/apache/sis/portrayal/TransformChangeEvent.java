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
import java.util.logging.Logger;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;


/**
 * A change in the "objective to display" transform that {@code Canvas} uses for rendering data.
 * That transform is updated frequently following gestures events such as zoom, translation or rotation.
 * All events fired by {@link Canvas} for the {@value Canvas#OBJECTIVE_TO_DISPLAY_PROPERTY} property
 * are instances of this class.
 * This specialization provides methods for computing the difference between the old and new state.
 *
 * <h2>Multi-threading</h2>
 * This class is <strong>not</strong> thread-safe.
 * All listeners should process this event in the same thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see Canvas#OBJECTIVE_TO_DISPLAY_PROPERTY
 *
 * @since 1.3
 * @module
 */
public class TransformChangeEvent extends PropertyChangeEvent {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4444752056666264066L;

    /**
     * The reason why the "objective to display" transform changed.
     * It may be because of canvas initialization, or an adjustment for a change of CRS
     * without change in the viewing area, or a navigation for viewing a different area.
     *
     * @see #getReason()
     */
    public enum Reason {
        /**
         * A new value has been assigned as part of a wider set of changes.
         * It typically happens when the canvas is initialized.
         *
         * @see Canvas#setGridGeometry(GridGeometry)
         */
        GRID_GEOMETRY_CHANGE,

        /**
         * A new value has been automatically computed for preserving the viewing area after a change of CRS.
         * It typically happens when the user changes the map projection without moving to a different region.
         *
         * @see Canvas#setObjectiveCRS(CoordinateReferenceSystem, DirectPosition)
         */
        CRS_CHANGE,

        /**
         * A new value has been assigned, overwriting the previous values. The objective CRS has not changed.
         * It can be considered as a kind of navigation, moving to absolute coordinates and zoom levels.
         *
         * @see Canvas#setObjectiveToDisplay(LinearTransform)
         */
        ASSIGNMENT,

        /**
         * A relative change has been applied in units of the objective CRS (for example in metres).
         *
         * @see PlanarCanvas#transformObjectiveCoordinates(AffineTransform)
         */
        OBJECTIVE_NAVIGATION,

        /**
         * A relative change has been applied in units of display device (typically pixel units).
         *
         * @see PlanarCanvas#transformDisplayCoordinates(AffineTransform)
         */
        DISPLAY_NAVIGATION,

        /**
         * A relative interim change has been applied but is not yet reflected in the "objective to display" transform.
         * This kind of change is not fired by {@link PlanarCanvas} but may be fired by subclasses such as
         * {@link org.apache.sis.gui.map.MapCanvas}. That class provides immediate feedback to users
         * with a temporary visual change before to perform more expansive rendering in background.
         */
        INTERIM;

        /**
         * Returns {@code true} if the "objective to display" transform changed because of a change
         * in viewing area, without change in the data themselves or in the map projection.
         */
        final boolean isNavigation() {
            return ordinal() >= ASSIGNMENT.ordinal() && ordinal() < INTERIM.ordinal();
        }
    }

    /**
     * The reason why the "objective to display" transform changed.
     *
     * @see #getReason()
     */
    private final Reason reason;

    /**
     * The change from old coordinates to new coordinates, computed when first needed.
     *
     * @see #getDisplayChange()
     * @see #getObjectiveChange()
     */
    private transient LinearTransform displayChange, objectiveChange;

    /**
     * Value of {@link #displayChange} or {@link #objectiveChange} precomputed by the code that fired this event.
     * If not precomputed, will be computed when first needed.
     */
    private AffineTransform displayChange2D, objectiveChange2D;

    /**
     * Non-null if {@link #canNotCompute(String, NoninvertibleTransformException)} already reported an error.
     * This is used for avoiding to report many times the same error.
     */
    private transient Exception error;

    /**
     * Creates a new event for a change of the "objective to display" property.
     * The old and new transforms should not be null, except on initialization or for lazy computation:
     * a null {@code newValue} means to take the value from {@link Canvas#getObjectiveToDisplay()} when needed.
     *
     * @param  source    the canvas that fired the event.
     * @param  oldValue  the old "objective to display" transform, or {@code null} if none.
     * @param  newValue  the new transform, or {@code null} for lazy computation.
     * @param  reason    the reason why the "objective to display" transform changed..
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     */
    public TransformChangeEvent(final Canvas source, final LinearTransform oldValue, final LinearTransform newValue,
                                final Reason reason)
    {
        super(source, Canvas.OBJECTIVE_TO_DISPLAY_PROPERTY, oldValue, newValue);
        ArgumentChecks.ensureNonNull("reason", reason);
        this.reason = reason;
    }

    /**
     * Creates a new event for an incremental change of the "objective to display" property.
     * The incremental change can be specified by the {@code objective} and/or the {@code display} argument.
     * Usually only one of those two arguments is non-null.
     *
     * @param  source     the canvas that fired the event.
     * @param  oldValue   the old "objective to display" transform, or {@code null} if none.
     * @param  newValue   the new transform, or {@code null} for lazy computation.
     * @param  objective  the incremental change in objective coordinates, or {@code null} for lazy computation.
     * @param  display    the incremental change in display coordinates, or {@code null} for lazy computation.
     * @param  reason     the reason why the "objective to display" transform changed..
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     */
    public TransformChangeEvent(final Canvas source, final LinearTransform oldValue, final LinearTransform newValue,
                                final AffineTransform objective, final AffineTransform display, final Reason reason)
    {
        this(source, oldValue, newValue, reason);
        objectiveChange2D = objective;
        displayChange2D   = display;
    }

    /**
     * Quick and non-overrideable check about whether the specified source is the source of this event.
     */
    final boolean isSameSource(final Canvas source) {
        return super.getSource() == source;
    }

    /**
     * Returns the canvas on which this event initially occurred.
     *
     * @return the canvas on which this event initially occurred.
     */
    @Override
    public Canvas getSource() {
        return (Canvas) source;
    }

    /**
     * Returns the reason why the "objective to display" transform changed.
     * It may be because of canvas initialization, or an adjustment for a change of CRS
     * without change in the viewing area, or a navigation for viewing a different area.
     *
     * @return the reason why the "objective to display" transform changed.
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Gets the old "objective to display" transform.
     *
     * @return the old "objective to display" transform, or {@code null} if none.
     */
    @Override
    public LinearTransform getOldValue() {
        return (LinearTransform) super.getOldValue();
    }

    /**
     * Gets the new "objective to display" transform.
     * It should be the current value of {@link Canvas#getObjectiveToDisplay()}.
     *
     * @return the new "objective to display" transform.
     */
    @Override
    public LinearTransform getNewValue() {
        LinearTransform value = (LinearTransform) super.getNewValue();
        if (value == null) {
            value = getSource().getObjectiveToDisplay();
        }
        return value;
    }

    /**
     * Returns the change from old objective coordinates to new objective coordinates.
     * When the "objective to display" transform changed (e.g. because the user did a zoom, translation or rotation),
     * this method expresses how the "real world" coordinates (typically in metres) of any point on the screen changed.
     *
     * <div class="note"><b>Example:</b>
     * if the map is shifted 10 metres toward the right side of the canvas, then (assuming no rotation or axis flip)
     * the <var>x</var> translation coefficient of the change is +10 (same sign than {@link #getDisplayChange()}).
     * Note that it may correspond to any amount of pixels, depending on the zoom factor.</div>
     *
     * The {@link #getObjectiveChange2D()} method gives the same transform as a Java2D object.
     * That change can be replicated on another canvas by giving the transform to
     * {@link PlanarCanvas#transformObjectiveCoordinates(AffineTransform)}.
     *
     * @return the change in objective coordinates. Usually not {@code null},
     *         unless one of the canvas is initializing or has a non-invertible transform.
     */
    public LinearTransform getObjectiveChange() {
        if (objectiveChange == null) {
            if (objectiveChange2D != null) {
                objectiveChange = AffineTransforms2D.toMathTransform(objectiveChange2D);
            } else {
                final LinearTransform oldValue = getOldValue();
                if (oldValue != null) {
                    final LinearTransform newValue = getNewValue();
                    if (newValue != null) try {
                        objectiveChange = (LinearTransform) MathTransforms.concatenate(newValue, oldValue.inverse());
                    } catch (NoninvertibleTransformException e) {
                        canNotCompute("getObjectiveChange", e);
                    }
                }
            }
        }
        return objectiveChange;
    }

    /**
     * Returns the change from old display coordinates to new display coordinates.
     * When the "objective to display" transform changed (e.g. because the user did a zoom, translation or rotation),
     * this method expresses how the display coordinates (typically pixels) of any given point on the map changed.
     *
     * <div class="note"><b>Example:</b>
     * if the map is shifted 10 pixels toward the right side of the canvas, then (assuming no rotation or axis flip)
     * the <var>x</var> translation coefficient of the change is +10: the points on the map which were located at
     * <var>x</var>=0 pixel before the change are now located at <var>x</var>=10 pixels after the change.</div>
     *
     * The {@link #getDisplayChange2D()} method gives the same transform as a Java2D object.
     * That change can be replicated on another canvas by giving the transform to
     * {@link PlanarCanvas#transformDisplayCoordinates(AffineTransform)}.
     *
     * @return the change in display coordinates. Usually not {@code null},
     *         unless one of the canvas is initializing or has a non-invertible transform.
     */
    public LinearTransform getDisplayChange() {
        if (displayChange == null) {
            if (displayChange2D != null) {
                displayChange = AffineTransforms2D.toMathTransform(displayChange2D);
            } else {
                final LinearTransform oldValue = getOldValue();
                if (oldValue != null) {
                    final LinearTransform newValue = getNewValue();
                    if (newValue != null) try {
                        displayChange = (LinearTransform) MathTransforms.concatenate(oldValue.inverse(), newValue);
                    } catch (NoninvertibleTransformException e) {
                        canNotCompute("getDisplayChange", e);
                    }
                }
            }
        }
        return displayChange;
    }

    /**
     * Returns the change in objective coordinates as a Java2D affine transform.
     * This method is suitable for two-dimensional canvas only.
     * For performance reason, it does not clone the returned transform.
     *
     * @return the change in objective coordinates. <strong>Do not modify.</strong>
     *
     * @see #getObjectiveChange()
     */
    public Optional<AffineTransform> getObjectiveChange2D() {
        if (objectiveChange2D == null) try {
            final Object oldValue = super.getOldValue();
            final Object newValue = super.getNewValue();
            if (oldValue instanceof AffineTransform && newValue instanceof AffineTransform) {
                // Equivalent to the `else` branch, but more efficient.
                objectiveChange2D = ((AffineTransform) oldValue).createInverse();
                objectiveChange2D.concatenate((AffineTransform) newValue);
            } else {
                objectiveChange2D = AffineTransforms2D.castOrCopy(getObjectiveChange());
            }
        } catch (java.awt.geom.NoninvertibleTransformException | IllegalArgumentException e) {
            canNotCompute("getObjectiveChange2D", e);
        }
        return Optional.ofNullable(objectiveChange2D);
    }

    /**
     * Returns the change in display coordinates as a Java2D affine transform.
     * This method is suitable for two-dimensional canvas only.
     * For performance reason, it does not clone the returned transform.
     *
     * @return the change in display coordinates. <strong>Do not modify.</strong>
     *
     * @see #getDisplayChange()
     */
    public Optional<AffineTransform> getDisplayChange2D() {
        if (displayChange2D == null) try {
            final Object oldValue = super.getOldValue();
            final Object newValue = super.getNewValue();
            if (oldValue instanceof AffineTransform && newValue instanceof AffineTransform) {
                // Equivalent to the `else` branch, but more efficient.
                displayChange2D = ((AffineTransform) oldValue).createInverse();
                displayChange2D.preConcatenate((AffineTransform) newValue);
            } else {
                displayChange2D = AffineTransforms2D.castOrCopy(getDisplayChange());
            }
        } catch (java.awt.geom.NoninvertibleTransformException | IllegalArgumentException e) {
            canNotCompute("getDisplayChange2D", e);
        }
        return Optional.ofNullable(displayChange2D);
    }

    /**
     * Invoked when a change can not be computed. It should never happen because "objective to display"
     * transforms should always be invertible. If this error nevertheless happens, consider the change
     * as a missing optional information.
     */
    private void canNotCompute(final String method, final Exception e) {
        if (error == null) {
            error = e;
            Logging.recoverableException(Logger.getLogger(Modules.PORTRAYAL), TransformChangeEvent.class, method, e);
        } else {
            error.addSuppressed(e);
        }
    }
}
