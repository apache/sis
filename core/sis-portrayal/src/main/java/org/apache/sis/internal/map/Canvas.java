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

import java.util.Objects;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArgumentChecks;


/**
 * Common abstraction for implementations that manage the display and user manipulation
 * of {@link MapLayer} instances. This base class makes no assumption about the geometry
 * of the display device (e.g. flat video monitor using Cartesian coordinate system, or
 * planetarium dome using spherical coordinate system).
 *
 * <p>A newly constructed {@code Canvas} is initially empty. To make something appears, at least one
 * {@link MapLayer} must be added. The visual content depends on the {@link MapLayer} data and associated style.
 * The contents are usually symbols, features or images, but some implementations can also manage non-geographic
 * elements like a map scale.</p>
 *
 * <p>In addition of the set of {@link MapLayer} to display,
 * a {@code Canvas} manages four fundamental properties:</p>
 * <ul>
 *   <li>The coordinate reference system to use for displaying data.</li>
 *   <li>The location of data to display in all dimensions, including the dimensions
 *       not shown by the display device (for example time).</li>
 *   <li>The size of the display device, in units of the display coordinate system.</li>
 *   <li>The conversion from the Coordinate Reference System to the display coordinate system.</li>
 * </ul>
 *
 * Those properties are explained in more details below. Other information, for example the
 * geographic bounding box of the data shown on screen, are inferred from above properties.
 *
 * <h2>Coordinate Reference Systems</h2>
 * There is three {@linkplain CoordinateReferenceSystem Coordinate Reference Systems}
 * involved in the rendering of geospatial data:
 *
 * <ol class="verbose">
 *   <li>The <cite>data CRS</cite> is specific to the data to be displayed.
 *       It may be anything convertible to the <cite>objective CRS</cite>.
 *       Different {@link MapItem} instances may use different data CRS,
 *       potentially with a different number of dimensions.</li>
 *   <li>The {@linkplain #getObjectiveCRS objective CRS} is the common CRS in which all data
 *       are converted before to be displayed. If the objective CRS involves a map projection,
 *       it determines the deformation of shapes that user will see on the display device.
 *       The objective CRS should have the same number of dimensions than the display device
 *       (often 2). Its domain of validity should be wide enough for encompassing all data.
 *       The {@link org.apache.sis.referencing.CRS#suggestCommonTarget CRS.suggestCommonTarget(…)}
 *       method may be helpful for choosing an objective CRS from a set of data CRS.</li>
 *   <li>The <cite>display CRS</cite> is the coordinate system of the display device.
 *       The conversion from <cite>objective CRS</cite> to <cite>display CRS</cite> should
 *       be an affine transform with a scale, a translation and optionally a rotation.
 *       This conversion changes every time that the user zooms or scrolls on viewed data.</li>
 * </ol>
 *
 * <h2>Location of data to display</h2>
 * In addition of above-cited Coordinate Reference Systems, a {@code Canvas} contains also a point of interest.
 * The point of interest is often, but not necessarily, at the center of display area.
 * It can be expressed in any CRS; it does not need to be the objective CRS or the CRS of any data.
 * However the point of interest CRS must have enough dimensions for being convertible to the CRS of all data.
 * In other words the number of dimensions of the point of interest is equal or greater than the highest
 * number of dimensions found in data. The point of interest is used not only for defining which point to show
 * in (typically) the center of the display area, but also for defining which slice to select in all dimensions
 * not shown by the display device.
 *
 * <div class="note"><b>Example:</b> if some data have (<var>x</var>,<var>y</var>,<var>z</var>) dimensions and
 * other data have (<var>x</var>,<var>y</var>,<var>t</var>) dimensions, then the point of interest shall contain
 * coordinate values for at least all of the (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>) dimensions
 * (i.e. it must be 4-dimensional, even if all data in this example are 3-dimensional). If the display device
 * is a two-dimensional screen showing map in the (<var>x</var>,<var>y</var>) dimensions (horizontal plane),
 * then the point of interest defines the <var>z</var> value (elevation or depth) and the <var>t</var> value
 * (date and time) of the slice to show.</div>
 *
 * <h2>Display device size</h2>
 * The geographic extent of data to be rendered is constrained by the zoom level and the display device size.
 * The display size is given by {@link #getDisplayBounds()} as an envelope having the number of dimensions of
 * the display device. The zoom level is given indirectly by the {@link #getObjectiveToDisplay()} transform.
 * The display device may have a wraparound axis, for example in the spherical coordinate system of a planetarium.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class Canvas extends Observable {
    /**
     * The {@value} property name, used for notifications about changes in objective CRS.
     * The objective CRS is the Coordinate Reference System in which all data are transformed before displaying.
     * Its number of dimension is the determined by the display device (two for flat screens).
     * Associated values are instances of {@link CoordinateReferenceSystem}.
     *
     * @see #getObjectiveCRS()
     * @see #setObjectiveCRS(CoordinateReferenceSystem)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String OBJECTIVE_CRS_PROPERTY = "objectiveCRS";

    /**
     * The {@value} property name, used for notifications about changes in point of interest.
     * The point of interest defines the location to show typically (but not necessarily) in
     * the center of the display device. But it defines also the slice coordinate values
     * in all dimensions beyond the ones shown by the device.
     * Associated values are instances of {@link DirectPosition}.
     *
     * @see #getPointOfInterest()
     * @see #setPointOfInterest(DirectPosition)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String POINT_OF_INTEREST_PROPERTY = "pointOfInterest";

    /**
     * The {@value} property name, used for notifications about changes in bounds of display device.
     * It may be for example changes in the size of the window were data are shown.
     * Associated values are instances of {@link Envelope}.
     *
     * @see #getDisplayBounds()
     * @see #setDisplayBounds(Envelope)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String DISPLAY_BOUNDS_PROPERTY = "displayBounds";

    /**
     * The coordinate reference system in which to transform all data before displaying.
     * If {@code null}, then no transformation is applied and data coordinates are used directly
     * as display coordinates, regardless the data CRS (even if different data use different CRS).
     *
     * @see #OBJECTIVE_CRS_PROPERTY
     * @see #getObjectiveCRS()
     */
    private CoordinateReferenceSystem objectiveCRS;

    /**
     * The point of interest to show typically (but not necessarily) in the center of display area.
     * Also used for selecting a slice in all supplemental dimensions.
     * If {@code null}, then the 0 coordinate value is assumed in all dimensions.
     *
     * @see #POINT_OF_INTEREST_PROPERTY
     * @see #getPointOfInterest()
     */
    private GeneralDirectPosition pointOfInterest;

    /**
     * The size and location of the output device, modified in-place if the size change.
     * The CRS of this envelope is the display CRS.
     *
     * @see #DISPLAY_BOUNDS_PROPERTY
     * @see #getDisplayBounds()
     * @see #getDisplayCRS()
     */
    private final GeneralEnvelope displayBounds;

    /**
     * Creates a new canvas for an output device using the given coordinate reference system.
     * The display CRS of a canvas can not be changed after construction.
     * Its coordinate system depends on the display device shape
     * (for example a two-dimensional Cartesian coordinate system for flat screens,
     * or a spherical coordinate system for planetarium domes).
     * The axis units of measurement are typically (but not necessarily) {@link Units#PIXEL}
     * for Cartesian coordinate systems, with {@link Units#DEGREE} in polar, cylindrical or
     * spherical coordinate systems.
     *
     * @param  displayCRS  the coordinate system of the display device.
     */
    protected Canvas(final EngineeringCRS displayCRS) {
        ArgumentChecks.ensureNonNull("displayCRS", displayCRS);
        displayBounds = new GeneralEnvelope(displayCRS);
    }

    /**
     * Returns the Coordinate Reference System of the display device.
     * The axis units of measurement are typically (but not necessarily) {@link Units#PIXEL}
     * for Cartesian coordinate systems, with {@link Units#DEGREE} in polar, cylindrical or
     * spherical coordinate systems. The coordinate system may have a wraparound axis for
     * some "exotic" display devices (e.g. planetarium dome).
     *
     * <div class="note"><b>Note:</b> invoking this method is rarely needed. It is sufficient to
     * said that a display CRS exists at least conceptually, and that we define a conversion from
     * the objective CRS to that display CRS.</div>
     *
     * @return the coordinate reference system of the display device.
     *
     * @see #getObjectiveCRS()
     * @see #getObjectiveToDisplay()
     */
    public final EngineeringCRS getDisplayCRS() {
        return (EngineeringCRS) displayBounds.getCoordinateReferenceSystem();
    }

    /**
     * Returns the Coordinate Reference System in which all data are transformed before displaying.
     * After conversion to this CRS, coordinates should be related to the display device coordinates
     * with only a final scale, a translation and optionally a rotation to add.
     *
     * <p>This value may be {@code null} on newly created {@code Canvas}, before data are added and canvas
     * is configured. It should not be {@code null} anymore once a {@code Canvas} is ready for displaying.</p>
     *
     * @return the Coordinate Reference System in which to transform all data before displaying.
     *
     * @see #OBJECTIVE_CRS_PROPERTY
     * @see #getDisplayCRS()
     * @see #getObjectiveToDisplay()
     */
    public CoordinateReferenceSystem getObjectiveCRS() {
        return objectiveCRS;
    }

    /**
     * Sets the Coordinate Reference System in which all data are transformed before displaying.
     * If the given value is different than the previous value, then a change event is sent to
     * all listeners registered for the {@value #OBJECTIVE_CRS_PROPERTY} property.
     *
     * <p>The domain of validity of the given CRS should be wide enough for encompassing all data.
     * The {@link org.apache.sis.referencing.CRS#suggestCommonTarget CRS.suggestCommonTarget(…)}
     * method may be helpful for choosing an objective CRS from a set of data CRS.</p>
     *
     * @param  newValue  the new Coordinate Reference System in which to transform all data before displaying.
     * @throws NullPointerException if the given CRS is null.
     * @throws MismatchedDimensionException if the given CRS does not have the number of dimensions of the display device.
     * @throws RenderException if the objective CRS can not be set to the given value for another reason.
     */
    public void setObjectiveCRS(final CoordinateReferenceSystem newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(OBJECTIVE_CRS_PROPERTY, newValue);
        ArgumentChecks.ensureDimensionMatches(OBJECTIVE_CRS_PROPERTY, getObjectiveToDisplay().getSourceDimensions(), newValue);
        final CoordinateReferenceSystem oldValue = objectiveCRS;
        if (!Objects.equals(oldValue, newValue)) {
            objectiveCRS = newValue;
            firePropertyChange(OBJECTIVE_CRS_PROPERTY, oldValue, newValue);
        }
    }

    /**
     * Returns the coordinates of the point to show typically (but not necessarily) in the center of display area.
     * This position may be expressed in any CRS, not necessarily the {@linkplain #getObjectiveCRS() objective CRS}.
     * The number of dimensions is equal or greater than the highest number of dimensions found in data CRS.
     * The coordinate values in dimensions beyond the {@linkplain #getObjectiveCRS() objective CRS} dimensions
     * specifies which slice to show (for example the depth of the horizontal plane to display, or the date of
     * the dynamic phenomenon to display). See {@linkplain Canvas class javadoc} for more discussion.
     *
     * <p>This value may be {@code null} on newly created {@code Canvas}, before data are added and canvas
     * is configured. It should not be {@code null} anymore once a {@code Canvas} is ready for displaying.</p>
     *
     * @return coordinates of the point to show typically (but not necessarily) in the center of display area.
     *
     * @see #POINT_OF_INTEREST_PROPERTY
     */
    public DirectPosition getPointOfInterest() {
        return (pointOfInterest != null) ? pointOfInterest.clone() : null;
    }

    /**
     * Sets the coordinates of the point to show typically (but not necessarily) in the center of display area.
     * If the given value is different than the previous value, then a change event
     * is sent to all listeners registered for the {@value #POINT_OF_INTEREST_PROPERTY} property.
     *
     * @param  newValue  the new coordinates of the point to show typically in the center of display area.
     * @throws NullPointerException if the given position is null.
     * @throws RenderException if the point of interest can not be set to the given value.
     */
    public void setPointOfInterest(final DirectPosition newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(POINT_OF_INTEREST_PROPERTY, newValue);
        final GeneralDirectPosition copy = new GeneralDirectPosition(newValue);
        final GeneralDirectPosition oldValue = pointOfInterest;
        if (!Objects.equals(oldValue, copy)) {
            pointOfInterest = copy;
            firePropertyChange(POINT_OF_INTEREST_PROPERTY, oldValue, newValue);        // Really `newValue`, not `copy`.
        }
    }

    /**
     * Returns the size and location of the display device.
     *
     * @return size and location of the display device.
     */
    public Envelope getDisplayBounds() {
        return new ImmutableEnvelope(displayBounds);
    }

    public void setDisplayBounds(final Envelope newValue) {
        ArgumentChecks.ensureNonNull(DISPLAY_BOUNDS_PROPERTY, newValue);
        displayBounds.setEnvelope(newValue);
    }

    /**
     * Returns the (usually affine) conversion from objective CRS to display coordinate system.
     * The number of source dimensions shall be the number of dimensions of the {@linkplain #getObjectiveCRS() objective CRS}.
     * The number of target dimensions shall be the number of dimensions of the display device.
     * That conversion will change every time that the user zooms or scrolls on viewed data.
     * This method shall never return {@code null}.
     *
     * @return conversion (usually affine) from objective CRS to display coordinate system.
     *
     * @see #getObjectiveCRS()
     * @see #getDisplayCRS()
     */
    public abstract LinearTransform getObjectiveToDisplay();
}
