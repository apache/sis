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
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.measure.Units;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;


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
 * @version 1.1
 * @since   1.1
 * @module
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
     * The display bounds in objective CRS, or {@code null} if this value needs to be recomputed.
     * This value is invalidated every time that {@link #objectiveToDisplay} transform changes.
     */
    private Rectangle2D areaOfInterest;

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
     * Sets the size and location of the display device in pixel coordinates.
     * The given envelope shall be two-dimensional. If the given value is different than the previous value,
     * then a change event is sent to all listeners registered for the {@value #DISPLAY_BOUNDS_PROPERTY} property.
     *
     * @see #getDisplayBounds()
     */
    @Override
    public void setDisplayBounds(final Envelope newValue) throws RenderException {
        areaOfInterest = null;
        super.setDisplayBounds(newValue);
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
     * Returns the bounds of the currently visible area in objective CRS.
     * New Area Of Interests (AOI) are computed when the {@linkplain #getDisplayBounds() display bounds}
     * or the {@linkplain #getObjectiveToDisplay() objective to display transform} change.
     * The AOI can be used as a hint, for example in order to clip data for faster rendering.
     *
     * @return bounds of currently visible area in objective CRS, or {@code null} if unavailable.
     */
    public Rectangle2D getAreaOfInterest() {
        if (areaOfInterest == null) {
            final Envelope2D bounds = getDisplayBounds();
            if (bounds != null) try {
                /*
                 * Following cast is safe because of the way `updateObjectiveToDisplay()` is implemented.
                 * The `inverse()` method is invoked on `LinearTransform` instead than `AffineTransform`
                 * because the former is cached.
                 */
                final AffineTransform displayToObjective = (AffineTransform) super.getObjectiveToDisplay().inverse();
                areaOfInterest = AffineTransforms2D.transform(displayToObjective, bounds, null);
            } catch (NoninvertibleTransformException e) {
                Logging.unexpectedException(Logging.getLogger(Modules.PORTRAYAL), PlanarCanvas.class, "getAreaOfInterest", e);
            }
        }
        return (areaOfInterest != null) ? (Rectangle2D) areaOfInterest.clone() : null;
    }

    /**
     * Returns the affine conversion from objective CRS to display coordinate system.
     * The transform returned by this method is a snapshot taken at the time this method is invoked;
     * subsequent changes in the <cite>objective to display</cite> conversion are not reflected in
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
    final LinearTransform updateObjectiveToDisplay() {
        return new AffineTransform2D(objectiveToDisplay);
    }

    /**
     * Sets the conversion from objective CRS to display coordinate system.
     * Contrarily to other setter methods, this method does not notify listeners about that change;
     * it is caller responsibility to send a {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} change event.
     * This method does not update the {@value #POINT_OF_INTEREST_PROPERTY} property;
     * the point of interest may move outside the view area as a result of this method call.
     *
     * @param  newValue  the new <cite>objective to display</cite> conversion.
     * @throws IllegalArgumentException if the given transform is not two-dimensional or is not affine.
     */
    @Override
    final void updateObjectiveToDisplay(final LinearTransform newValue) {
        areaOfInterest = null;
        objectiveToDisplay.setTransform(AffineTransforms2D.castOrCopy(newValue.getMatrix()));
        super.updateObjectiveToDisplay(newValue);
    }

    /**
     * Updates the <cite>objective to display</cite> transform as if the given transform was applied <em>before</em>
     * the current transform. For example if the given {@code before} transform is a translation, then the translation
     * vector is in units of the {@linkplain #getObjectiveCRS() objective CRS} (typically metres on the map).
     *
     * @param  before  coordinate conversion to apply before the current <cite>objective to display</cite> transform.
     */
    public void transformObjectiveCoordinates(final AffineTransform before) {
        if (!before.isIdentity()) {
            areaOfInterest = null;
            final LinearTransform old = hasListener(OBJECTIVE_TO_DISPLAY_PROPERTY) ? getObjectiveToDisplay() : null;
            objectiveToDisplay.concatenate(before);
            invalidateObjectiveToDisplay(old);
        }
    }

    /**
     * Updates the <cite>objective to display</cite> transform as if the given transform was applied <em>after</em>
     * the current transform. For example if the given {@code after} transform is a translation, then the translation
     * vector is in pixel units.
     *
     * @param  after  coordinate conversion to apply after the current <cite>objective to display</cite> transform.
     */
    public void transformDisplayCoordinates(final AffineTransform after) {
        if (!after.isIdentity()) {
            areaOfInterest = null;
            final LinearTransform old = hasListener(OBJECTIVE_TO_DISPLAY_PROPERTY) ? getObjectiveToDisplay() : null;
            objectiveToDisplay.preConcatenate(after);
            invalidateObjectiveToDisplay(old);
        }
    }
}
