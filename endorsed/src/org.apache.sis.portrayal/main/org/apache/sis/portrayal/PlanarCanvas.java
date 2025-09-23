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

import java.util.Locale;
import java.awt.geom.AffineTransform;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.measure.Units;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;


/**
 * A canvas for two-dimensional display device using a Cartesian coordinate system.
 * Data are reduced to a two-dimensional slice before to be displayed.
 *
 * <h2>Multi-threading</h2>
 * {@code PlanarCanvas} is not thread-safe. Synchronization, if desired, must be done by the caller.
 * Another common strategy is to interact with {@code PlanarCanvas} from a single thread,
 * for example the Swing or JavaFX event queue.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 */
public abstract class PlanarCanvas extends Canvas {
    /**
     * The {@value} constant for identifying code specific to bi-dimensional case.
     */
    protected static final int BIDIMENSIONAL = 2;

    /**
     * The conversion from {@linkplain #getObjectiveCRS() objective CRS} to the display coordinate system as a
     * Java2D affine transform. This transform will be modified in-place when user applies zoom, translation or
     * rotation on the view area. Subclasses should generally not modify this affine transform directly; invoke
     * one of the <code>transform<var>Foo</var>Coordinates(AffineTransform)</code> methods instead.
     *
     * @see #getObjectiveToDisplay()
     * @see #transformObjectiveCoordinates(AffineTransform)
     * @see #transformDisplayCoordinates(AffineTransform)
     */
    protected final AffineTransform objectiveToDisplay;

    /**
     * Creates a new two-dimensional canvas.
     *
     * @param  locale  the locale to use for labels and some messages, or {@code null} for default.
     */
    protected PlanarCanvas(final Locale locale) {
        super(CommonCRS.Engineering.DISPLAY.crs(), locale);
        objectiveToDisplay = new AffineTransform();
    }

    /**
     * Returns the number of dimensions of the display device.
     */
    @Override
    final int getDisplayDimensions() {
        return BIDIMENSIONAL;
    }

    /**
     * Gets the name of display axes and stores them in the given array. Those display axis names
     * are used for debugging purposes only, as an additional information provided to developers.
     * Those names should not be used for any "real" work.
     *
     * @param  axisTypes  where to store the name of display axes. The array length will
     *                    be at least {@link #BIDIMENSIONAL} (it will often be longer).
     */
    @Override
    final void getDisplayAxes(final DimensionNameType[] axisTypes) {
        axisTypes[0] = DimensionNameType.COLUMN;
        axisTypes[1] = DimensionNameType.ROW;
    }

    /**
     * Allocates a position which can hold a coordinates in objective or display CRS.
     */
    @Override
    final DirectPosition allocatePosition() {
        return new DirectPosition2D(super.getObjectiveCRS());
    }

    /**
     * Returns the size and location of the display device. The unit of measurement is
     * {@link Units#PIXEL} and coordinate values are usually (but not necessarily) integers.
     *
     * <p>This value may be {@code null} on newly created {@code Canvas}, before data are added and canvas
     * is configured. It should not be {@code null} anymore once a {@code Canvas} is ready for displaying.
     * The returned envelope is a copy; display changes happening after this method invocation will not be
     * reflected in the returned envelope.</p>
     *
     * @return size and location of the display device in pixel coordinates.
     *
     * @see #setDisplayBounds(Envelope)
     */
    @Override
    public Envelope2D getDisplayBounds() {
        return displayBounds.isAllNaN() ? null : new Envelope2D(displayBounds);
    }

    /**
     * Returns the affine conversion from objective CRS to display coordinate system.
     * The transform returned by this method is a snapshot taken at the time this method is invoked;
     * subsequent changes in the <i>objective to display</i> conversion are not reflected in
     * the returned transform.
     *
     * <p>The {@link Canvas#objectiveToDisplay} transform in parent class is used as an immutable snapshot of
     * the {@link #objectiveToDisplay} transform in this class. That snapshot is created when needed and reset
     * to {@code null} when {@link #objectiveToDisplay} is modified.</p>
     *
     * @return snapshot of the affine conversion from objective CRS
     *         to display coordinate system (never {@code null}).
     *
     * @see Canvas#objectiveToDisplay
     */
    @Override
    final LinearTransform createObjectiveToDisplay() {
        return new AffineTransform2D(objectiveToDisplay);
    }

    /**
     * Sets the conversion from objective CRS to display coordinate system.
     * Contrarily to other setter methods, this method does not notify listeners about that change;
     * it is caller responsibility to send a {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} change event.
     * This method does not update the {@value #POINT_OF_INTEREST_PROPERTY} property;
     * the point of interest may move outside the view area as a result of this method call.
     *
     * @param  newValue  the new <i>objective to display</i> conversion.
     * @throws IllegalArgumentException if the given transform is not two-dimensional or is not affine.
     */
    @Override
    final void setObjectiveToDisplayImpl(final LinearTransform newValue) {
        objectiveToDisplay.setTransform(AffineTransforms2D.castOrCopy(newValue.getMatrix()));
        super.setObjectiveToDisplayImpl(newValue);
    }

    /**
     * Updates the <i>objective to display</i> transform as if the given transform was applied <em>before</em>
     * the current transform. For example if the given {@code before} transform is a translation, then the translation
     * vector is in units of the {@linkplain #getObjectiveCRS() objective CRS} (typically metres on the map).
     *
     * <p>This method does nothing if the given transform is identity.
     * Otherwise an {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} property change event will be sent with the
     * {@link TransformChangeEvent.Reason#OBJECTIVE_NAVIGATION} reason after the change became effective.
     * Depending on the implementation, the change may not take effect immediately.
     * For example, subclasses may do the rendering in a background thread.</p>
     *
     * @param  before  coordinate conversion to apply before the current <i>objective to display</i> transform.
     *
     * @see TransformChangeEvent#getObjectiveChange()
     */
    public void transformObjectiveCoordinates(final AffineTransform before) {
        if (!before.isIdentity()) {
            final LinearTransform old = hasPropertyChangeListener(OBJECTIVE_TO_DISPLAY_PROPERTY) ? getObjectiveToDisplay() : null;
            objectiveToDisplay.concatenate(before);
            super.setObjectiveToDisplayImpl(null);
            if (old != null) {
                firePropertyChange(new TransformChangeEvent(this, old, null, before, null,
                                       TransformChangeEvent.Reason.OBJECTIVE_NAVIGATION));
            }
        }
    }

    /**
     * Updates the <i>objective to display</i> transform as if the given transform was applied <em>after</em>
     * the current transform. For example if the given {@code after} transform is a translation, then the translation
     * vector is in pixel units.
     *
     * <p>This method does nothing if the given transform is identity.
     * Otherwise an {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} property change event will be sent with the
     * {@link TransformChangeEvent.Reason#DISPLAY_NAVIGATION} reason after the change became effective.
     * Depending on the implementation, the change may not take effect immediately.
     * For example, subclasses may do the rendering in a background thread.</p>
     *
     * @param  after  coordinate conversion to apply after the current <i>objective to display</i> transform.
     *
     * @see TransformChangeEvent#getDisplayChange()
     */
    public void transformDisplayCoordinates(final AffineTransform after) {
        if (!after.isIdentity()) {
            final LinearTransform old = hasPropertyChangeListener(OBJECTIVE_TO_DISPLAY_PROPERTY) ? getObjectiveToDisplay() : null;
            objectiveToDisplay.preConcatenate(after);
            super.setObjectiveToDisplayImpl(null);
            if (old != null) {
                firePropertyChange(new TransformChangeEvent(this, old, null, null, after,
                                       TransformChangeEvent.Reason.DISPLAY_NAVIGATION));
            }
        }
    }
}
