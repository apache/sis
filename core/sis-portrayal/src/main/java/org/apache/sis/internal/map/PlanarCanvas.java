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
package org.apache.sis.internal.map;

import java.util.Map;
import java.util.Locale;
import java.awt.geom.AffineTransform;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.measure.Units;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;


/**
 * A canvas for two-dimensional display device using a Cartesian coordinate system.
 * Data are reduced to a two-dimensional slice before to be displayed.
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
    private static final int BIDIMENSIONAL = 2;

    /**
     * The display Coordinate Reference System used by all {@code PlanarCanvas} instances.
     */
    private static final DefaultEngineeringCRS DISPLAY_CRS;
    static {
        Map<String,?> property = singletonMap(NAME_KEY, "Display on two-dimensional Cartesian coordinate system");
        DefaultCartesianCS cs = new DefaultCartesianCS(property,
                new DefaultCoordinateSystemAxis(singletonMap(NAME_KEY, "Column"), "x", AxisDirection.DISPLAY_RIGHT, Units.PIXEL),
                new DefaultCoordinateSystemAxis(singletonMap(NAME_KEY, "Row"),    "y", AxisDirection.DISPLAY_DOWN,  Units.PIXEL));
        property = singletonMap(NAME_KEY, cs.getName());        // Reuse the same Identifier instance.
        DISPLAY_CRS = new DefaultEngineeringCRS(property, new DefaultEngineeringDatum(property), cs);
    }

    /**
     * The conversion from {@linkplain #getObjectiveCRS() objective CRS} to the display coordinate system.
     * This transform will be modified in-place when user applies zoom, translation or rotation on the view area.
     * The {@link #objectiveToDisplay} transform inherited from parent class is used as an immutable snapshot of
     * this {@link #toDisplayAsAffine} transform. That snapshot is created when needed and reset to {@code null}
     * when {@link #toDisplayAsAffine} is modified.
     *
     * @see #getObjectiveToDisplay()
     * @see #objectiveToDisplay
     */
    private final AffineTransform toDisplayAsAffine;

    /**
     * Creates a new two-dimensional canvas.
     *
     * @param  locale  the locale to use for labels and some messages, or {@code null} for default.
     */
    protected PlanarCanvas(final Locale locale) {
        super(DISPLAY_CRS, locale);
        toDisplayAsAffine = new AffineTransform();
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
        return new DirectPosition2D();
    }

    /**
     * Returns the size and location of the display device. The unit of measurement is
     * {@link Units#PIXEL} and coordinate values are usually (but not necessarily) integers.
     *
     * <p>This value may be {@code null} on newly created {@code Canvas}, before data are added and canvas
     * is configured. It should not be {@code null} anymore once a {@code Canvas} is ready for displaying.</p>
     *
     * @return size and location of the display device.
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
     * subsequent changes in the <cite>objective to display</cite> conversion are not reflected in
     * the returned transform.
     *
     * @return snapshot of the affine conversion from objective CRS
     *         to display coordinate system (never {@code null}).
     */
    @Override
    final LinearTransform updateObjectiveToDisplay() {
        return new AffineTransform2D(toDisplayAsAffine);
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
        toDisplayAsAffine.setTransform(AffineTransforms2D.castOrCopy(newValue.getMatrix()));
        super.updateObjectiveToDisplay(newValue);
    }

    public void scale(final double sx, final double sy) {
        // TODO
    }

    public void translate(final double tx, final double ty) {
        // TODO
    }

    public void rotate(final double angle) {
        // TODO
    }
}
