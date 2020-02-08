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

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.util.FactoryException;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.CoordinateOperationContext;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;


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
 * <p>In addition to the set of {@link MapLayer}s to display,
 * a {@code Canvas} manages four fundamental properties:</p>
 * <ul>
 *   <li>The coordinate reference system to use for displaying data.</li>
 *   <li>The location of data to display in all dimensions, including the dimensions
 *       not shown by the display device (for example time).</li>
 *   <li>The size of the display device, in units of the display coordinate system (typically pixels).</li>
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
 *       The {@link CRS#suggestCommonTarget CRS.suggestCommonTarget(…)} method may be helpful
 *       for choosing an objective CRS from a set of data CRS.</li>
 *   <li>The {@linkplain #getDisplayCRS display CRS} is the coordinate system of the display device.
 *       The {@linkplain #getObjectiveToDisplay() conversion from objective CRS to display CRS}
 *       should be an affine transform with a scale, a translation and optionally a rotation.
 *       This conversion changes every time that the user zooms or scrolls on viewed data.</li>
 * </ol>
 *
 * <h2>Location of data to display</h2>
 * In addition of above-cited Coordinate Reference Systems, a {@code Canvas} contains also a point of interest.
 * The point of interest is often, but not necessarily, at the center of display area.
 * It defines the position where {@linkplain #getResolution() resolutions} will be computed, and where
 * {@linkplain PlanarCanvas#scale(double, double) scales},
 * {@linkplain PlanarCanvas#translate(double, double) translations} and
 * {@linkplain PlanarCanvas#rotate(double) rotations} will be applied.
 *
 * <p>The point of interest can be expressed in any CRS;
 * it does not need to be the objective CRS or the CRS of any data.
 * However the CRS of that point must have enough dimensions for being convertible to the CRS of all data.
 * This rule implies that the number of dimensions of the point of interest is equal or greater than
 * the highest number of dimensions found in data. The purpose is not only to specify which point to show in
 * (typically) the center of the display area, but also to specify which slice to select in all dimensions
 * not shown by the display device.</p>
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
 * the display device. The display bounds is usually given in {@linkplain Units#PIXEL pixel units}, but other
 * units such as {@link Units#POINT} are also authorized.
 * The zoom level is given indirectly by the {@link #getObjectiveToDisplay()} transform.
 * The display device may have a wraparound axis, for example in the spherical coordinate system of a planetarium.
 *
 * <h2>Multi-threading</h2>
 * {@code Canvas} is not thread-safe. Synchronization, if desired, must be done by the caller.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class Canvas extends Observable implements Localized {
    /**
     * Desired resolution in display units (usually pixels). This is used for avoiding
     * the cost of transformations having too much accuracy for the current zoom level.
     *
     * @see #findTransform(CoordinateReferenceSystem, CoordinateReferenceSystem)
     */
    private static final double DISPLAY_RESOLUTION = 1;

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
     * The {@value} property name, used for notifications about changes in <cite>objective to display</cite> conversion.
     * This conversion maps coordinates in the {@linkplain #getObjectiveCRS() objective CRS} to coordinates in the
     * {@linkplain #getDisplayCRS() display CRS}. Associated values are instances of {@link LinearTransform}.
     *
     * @see #getObjectiveToDisplay()
     * @see #setObjectiveToDisplay(LinearTransform)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String OBJECTIVE_TO_DISPLAY_PROPERTY = "objectiveToDisplay";

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
     * The coordinate reference system in which to transform all data before displaying.
     * If {@code null}, then no transformation is applied and data coordinates are used directly
     * as display coordinates, regardless the data CRS (even if different data use different CRS).
     *
     * @see #OBJECTIVE_CRS_PROPERTY
     * @see #getObjectiveCRS()
     * @see #setObjectiveCRS(CoordinateReferenceSystem)
     */
    private CoordinateReferenceSystem objectiveCRS;

    /**
     * The conversion from {@linkplain #getObjectiveCRS() objective CRS} to the display coordinate system.
     * Conceptually this conversion should never be null (its initial value is the identity conversion).
     * However subclasses may use a more specialized type such as {@link java.awt.geom.AffineTransform}
     * and set this field to {@code null} for recomputing it from the specialized type when requested.
     *
     * @see #OBJECTIVE_TO_DISPLAY_PROPERTY
     * @see #getObjectiveToDisplay()
     * @see #setObjectiveToDisplay(LinearTransform)
     */
    private LinearTransform objectiveToDisplay;

    /**
     * The size and location of the output device, modified in-place if the size change.
     * The CRS of this envelope is the display CRS. Coordinate values are initially NaN.
     *
     * @see #DISPLAY_BOUNDS_PROPERTY
     * @see #getDisplayBounds()
     * @see #getDisplayCRS()
     */
    final GeneralEnvelope displayBounds;

    /**
     * The point of interest to show typically (but not necessarily) in the center of display area.
     * Also used for selecting a slice in all supplemental dimensions.
     * If {@code null}, then calculations that depend on a point of interest are skipped.
     *
     * @see #POINT_OF_INTEREST_PROPERTY
     * @see #getPointOfInterest()
     */
    private GeneralDirectPosition pointOfInterest;

    /**
     * The point of interest transformed to the objective CRS, or {@code null} if {@link #pointOfInterest}
     * has not yet been provided. This point shall be updated immediately when {@link #pointOfInterest} is
     * updated, as a way to verify that the point is valid.
     */
    private DirectPosition objectivePOI;

    /**
     * The factory to use for creating coordinate operations. This factory allow us to specify the area
     * of interest (the geographic region shown by this {@code Canvas}) and the desired resolution.
     *
     * @see #findTransform(CoordinateReferenceSystem, CoordinateReferenceSystem)
     */
    private final DefaultCoordinateOperationFactory coordinateOperationFactory;

    /**
     * The locale for labels or error messages.
     */
    private final Locale locale;

    /**
     * Creates a new canvas for a display device using the given coordinate reference system.
     * The display CRS of a canvas can not be changed after construction.
     * Its coordinate system depends on the display device shape
     * (for example a two-dimensional Cartesian coordinate system for flat screens,
     * or a polar or spherical coordinate system for planetarium domes).
     * The axis units of measurement are typically (but not necessarily) {@link Units#PIXEL}
     * for Cartesian coordinate systems, with {@link Units#DEGREE} in polar, cylindrical or
     * spherical coordinate systems.
     *
     * @param  displayCRS  the coordinate system of the display device.
     * @param  locale      the locale to use for labels and some messages, or {@code null} for default.
     */
    protected Canvas(final EngineeringCRS displayCRS, final Locale locale) {
        this.locale = locale;
        ArgumentChecks.ensureNonNull("displayCRS", displayCRS);
        displayBounds = new GeneralEnvelope(displayCRS);
        displayBounds.setToNaN();
        coordinateOperationFactory = CoordinateOperations.factory();
    }

    /**
     * Returns the locale used for texts or for producing some error messages.
     * May be {@code null} if no locale has been specified, in which case
     * the {@linkplain Locale#getDefault() system default} should be used.
     *
     * @return the locale for messages, or {@code null} if not explicitly defined.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the number of dimensions of the display device.
     * Subclasses may override for a little bit more efficiency.
     */
    int getDisplayDimensions() {
        return ReferencingUtilities.getDimension(getDisplayCRS());
    }

    /**
     * Returns name of display axes, or {@code null} if unknown.
     */
    DimensionNameType[] getDisplayAxes() {
        return null;
    }

    /**
     * Returns the Coordinate Reference System of the display device.
     * The axis units of measurement are typically (but not necessarily) {@link Units#PIXEL}
     * for Cartesian coordinate systems, with {@link Units#DEGREE} in polar, cylindrical or
     * spherical coordinate systems. The coordinate system may have a wraparound axis for
     * some "exotic" display devices (e.g. planetarium dome).
     *
     * <div class="note"><b>Usage note:</b> invoking this method is rarely needed. It is sufficient
     * to said that a display CRS exists at least conceptually, and that we define a conversion from
     * the objective CRS to that display CRS. This method may be useful when the subclasses may be
     * something else than {@link PlanarCanvas}, in which case the caller may want more information
     * about the geometry of the display device.</div>
     *
     * <p>Note that the {@link CRS#findOperation CRS.findOperation(…)} static method can generally
     * not handle this display CRS. To apply coordinate operations on display coordinates,
     * {@link #getObjectiveToDisplay()} transform must be inverted and used.</p>
     *
     * @return the Coordinate Reference System of the display device.
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
     * with only a final scale, a translation and optionally a rotation remaining to apply.
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
     * The given CRS should have a domain of validity wide enough for encompassing all data
     * (the {@link CRS#suggestCommonTarget CRS.suggestCommonTarget(…)} method may be helpful
     * for choosing an objective CRS from a set of data CRS).
     * If the given value is different than the previous value, then a change event is sent to
     * all listeners registered for the {@value #OBJECTIVE_CRS_PROPERTY} property.
     *
     * <p>If the transform between old and new CRS is not identity, then this method recomputes
     * the <cite>objective to display</cite> conversion in a way preserving the display coordinates
     * of the {@link #getPointOfInterest() point of interest}, together with the scales, shapes and
     * orientations of features in close neighborhood of that point.
     * This calculation may cause {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} property change event
     * to be sent to listeners, in addition of above-cited {@value #OBJECTIVE_CRS_PROPERTY}
     * (note that {@value #POINT_OF_INTEREST_PROPERTY} stay unchanged).
     * All those change events are sent only after all property values have been updated to their new values.</p>
     *
     * @param  newValue  the new Coordinate Reference System in which to transform all data before displaying.
     * @throws NullPointerException if the given CRS is null.
     * @throws MismatchedDimensionException if the given CRS does not have the number of dimensions of the display device.
     * @throws RenderException if the objective CRS can not be set to the given value for another reason.
     */
    public void setObjectiveCRS(final CoordinateReferenceSystem newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(OBJECTIVE_CRS_PROPERTY, newValue);
        ArgumentChecks.ensureDimensionMatches(OBJECTIVE_CRS_PROPERTY, getDisplayDimensions(), newValue);
        final CoordinateReferenceSystem oldValue = objectiveCRS;
        LinearTransform oldObjectiveToDisplay = null;
        LinearTransform newObjectiveToDisplay = null;
        if (!Objects.equals(oldValue, newValue)) {
            if (oldValue != null) try {
                /*
                 * Compute the change unconditionally as a way to verify that the new CRS is compatible with
                 * data currently shown. Another reason is that checking identity transform is more reliable
                 * than the `compareIgnoreMetadata(oldValue, newValue)` check.
                 */
                final MathTransform newToOld = findTransform(newValue, oldValue);
                if (pointOfInterest != null && !newToOld.isIdentity()) {
                    oldObjectiveToDisplay = getObjectiveToDisplay();
                    /*
                     * Conceptually, we want the coordinates in new CRS to be as they were in the old CRS
                     * (same location, same Jacobian matrix) in the neighborhood of the point of interest,
                     * so that we can apply the old `objectiveToCRS` transform. For achieving that goal,
                     * we apply a local affine transform which cancel the effect of "old CRS → new CRS"
                     * transformation around the point of interest. The effect of CRS change will appear
                     * as we look further from the point of interest.
                     */
                    final MathTransform  poiToNew = findTransform(pointOfInterest.getCoordinateReferenceSystem(), newValue);
                    final DirectPosition poiInNew = poiToNew.transform(pointOfInterest, allocatePosition());
                    final LinearTransform  cancel = MathTransforms.tangent(newToOld, poiInNew);
                    final MathTransform    result = MathTransforms.concatenate(cancel, oldObjectiveToDisplay);
                    /*
                     * The result is the new `objectiveToTransform` such as the display is unchanged around POI.
                     * That transform should be an instance of `LinearTransform` because the two concatenated
                     * transforms were linear, but we nevertheless invoke `tangent(…)` again as a safety;
                     * normally it should just return the `result` as-is.
                     */
                    newObjectiveToDisplay = MathTransforms.tangent(result, poiInNew);
                    updateObjectiveToDisplay(newObjectiveToDisplay);
                    objectivePOI = poiInNew;    // Set only after everything else succeeded.
                }
            } catch (FactoryException | TransformException e) {
                throw new RenderException(errors().getString(Errors.Keys.CanNotSetPropertyValue_1, OBJECTIVE_CRS_PROPERTY), e);
            }
            objectiveCRS = newValue;            // Set only after everything else succeeded.
            firePropertyChange(OBJECTIVE_CRS_PROPERTY, oldValue, newValue);
            if (!Objects.equals(oldObjectiveToDisplay, newObjectiveToDisplay)) {
                firePropertyChange(OBJECTIVE_TO_DISPLAY_PROPERTY, oldObjectiveToDisplay, newObjectiveToDisplay);
            }
        }
    }

    /**
     * Returns the (usually affine) conversion from objective CRS to display coordinate system.
     * The source coordinates shall be in the CRS given by {@link #getObjectiveCRS()} and the
     * converted coordinates will be in the CRS given by {@link #getDisplayCRS()}.
     *
     * <p>The <cite>objective to display</cite> conversion changes every time that user zooms
     * or scrolls on viewed data. However the transform returned by this method is a snapshot
     * taken at the time this method is invoked; subsequent changes in the <cite>objective to
     * display</cite> conversion are not reflected in the returned transform.</p>
     *
     * @return snapshot of the (usually affine) conversion from objective CRS
     *         to display coordinate system (never {@code null}).
     *
     * @see #OBJECTIVE_CRS_PROPERTY
     * @see #getObjectiveCRS()
     * @see #getDisplayCRS()
     */
    public LinearTransform getObjectiveToDisplay() {
        if (objectiveToDisplay == null) {
            objectiveToDisplay = updateObjectiveToDisplay();
        }
        return objectiveToDisplay;
    }

    /**
     * Takes a snapshot of the <cite>objective to display</cite> conversion. This method needs
     * to be overridden only by subclasses that use their own specialized class instead than
     * {@link #objectiveToDisplay} for managing changes in the zooms or viewed area.
     *
     * @see #updateObjectiveToDisplay(LinearTransform)
     */
    LinearTransform updateObjectiveToDisplay() {
        return MathTransforms.identity(getDisplayDimensions());
    }

    /**
     * Sets the conversion from objective CRS to display coordinate system.
     * If the given value is different than the previous value, then a change event is sent
     * to all listeners registered for the {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} property.
     *
     * <p>Invoking this method has the effect of changing the viewed area, the zoom level or the rotation of the map.
     * It does not update the {@value #POINT_OF_INTEREST_PROPERTY} property however. The point of interest may move
     * outside the view area as a result of this method call.</p>
     *
     * @param  newValue  the new <cite>objective to display</cite> conversion.
     * @throws IllegalArgumentException if given the transform does not have the expected number of dimensions or is not affine.
     * @throws RenderException if the <cite>objective to display</cite> transform can not be set to the given value for another reason.
     */
    public void setObjectiveToDisplay(LinearTransform newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(OBJECTIVE_TO_DISPLAY_PROPERTY, newValue);
        final int expected = getDisplayDimensions();
        int actual = newValue.getSourceDimensions();
        if (actual == expected) {
            actual = newValue.getTargetDimensions();
            if (actual == expected) {
                LinearTransform oldValue = objectiveToDisplay;      // Do not invoke user-overridable method.
                if (oldValue == null) {
                    oldValue = updateObjectiveToDisplay();
                }
                if (!Objects.equals(oldValue, newValue)) {
                    updateObjectiveToDisplay(newValue);
                    firePropertyChange(OBJECTIVE_TO_DISPLAY_PROPERTY, oldValue, newValue);
                }
                return;
            }
        }
        throw new MismatchedDimensionException(errors().getString(
                Errors.Keys.MismatchedDimension_3, OBJECTIVE_TO_DISPLAY_PROPERTY, expected, actual));
    }

    /**
     * Sets the conversion from objective CRS to display coordinate system.
     * Contrarily to other setter methods, this method does not notify listeners about that change;
     * it is caller responsibility to send a {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} change event.
     * This design choice is because this method is usually invoked as part of a larger set of changes.
     *
     * @see #updateObjectiveToDisplay()
     */
    void updateObjectiveToDisplay(final LinearTransform newValue) {
        objectiveToDisplay = newValue;
    }

    /**
     * Returns the size and location of the display device.
     * The unit of measurement is typically (but not necessarily) pixels.
     * The coordinate values are often integers, but this is not mandatory.
     * The coordinate reference system is given by {@link #getDisplayCRS()}.
     *
     * <p>This value may be {@code null} on newly created {@code Canvas}, before data are added and canvas
     * is configured. It should not be {@code null} anymore once a {@code Canvas} is ready for displaying.</p>
     *
     * @return size and location of the display device.
     *
     * @see #DISPLAY_BOUNDS_PROPERTY
     */
    public Envelope getDisplayBounds() {
        return displayBounds.isAllNaN() ? null : new GeneralEnvelope(displayBounds);
    }

    /**
     * Sets the size and location of the display device. The envelope CRS shall be either the
     * {@linkplain #getDisplayCRS() display CRS} or unspecified, in which case the display CRS
     * is assumed. Unit of measurement is typically (but not necessarily) {@link Units#PIXEL}.
     * If the given value is different than the previous value, then a change event is sent to
     * all listeners registered for the {@value #DISPLAY_BOUNDS_PROPERTY} property.
     *
     * @param  newValue  the new display bounds.
     * @throws IllegalArgumentException if the given envelope does not have the expected CRS or number of dimensions.
     * @throws RenderException if the display bounds can not be set to the given value for another reason.
     */
    public void setDisplayBounds(final Envelope newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(DISPLAY_BOUNDS_PROPERTY, newValue);
        if (objectiveCRS != null) {
            final CoordinateReferenceSystem crs = newValue.getCoordinateReferenceSystem();
            if (crs != null && !Utilities.equalsIgnoreMetadata(objectiveCRS, crs)) {
                throw new MismatchedReferenceSystemException(errors().getString(
                        Errors.Keys.IllegalCoordinateSystem_1, IdentifiedObjects.getDisplayName(crs, getLocale())));
            }
        }
        final GeneralEnvelope oldValue = new GeneralEnvelope(displayBounds);
        displayBounds.setEnvelope(newValue);
        if (displayBounds.isEmpty()) {
            displayBounds.setEnvelope(oldValue);
            throw new IllegalArgumentException(errors().getString(Errors.Keys.EmptyProperty_1, DISPLAY_BOUNDS_PROPERTY));
        }
        if (!Objects.equals(oldValue, displayBounds)) {
            firePropertyChange(DISPLAY_BOUNDS_PROPERTY, oldValue, newValue);    // Do not publish reference to `displayBounds`.
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
     * If the given value is different than the previous value, then a change event is sent to all listeners
     * registered for the {@value #POINT_OF_INTEREST_PROPERTY} property.
     *
     * @param  newValue  the new coordinates of the point to show typically in the center of display area.
     * @throws NullPointerException if the given position is null.
     * @throws RenderException if the point of interest can not be set to the given value.
     */
    public void setPointOfInterest(final DirectPosition newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(POINT_OF_INTEREST_PROPERTY, newValue);
        final GeneralDirectPosition copy = new GeneralDirectPosition(newValue);
        final GeneralDirectPosition oldValue = pointOfInterest;
        if (!Objects.equals(oldValue, copy)) try {
            final MathTransform mt = findTransform(newValue.getCoordinateReferenceSystem(), objectiveCRS);
            objectivePOI = mt.transform(copy, allocatePosition());
            pointOfInterest = copy;
            firePropertyChange(POINT_OF_INTEREST_PROPERTY, oldValue, newValue);     // Do not publish reference to `copy`.
        } catch (FactoryException | TransformException e) {
            throw new RenderException(errors().getString(Errors.Keys.CanNotSetPropertyValue_1, POINT_OF_INTEREST_PROPERTY), e);
        }
    }

    /**
     * Returns canvas properties (objective CRS, display bounds, conversion) encapsulated in a grid geometry.
     * This is a convenience method for interoperability with grid coverage API. Properties are mapped as below:
     *
     * <table>
     *   <caption>Canvas to grid geometry properties</caption>
     *   <tr>
     *     <th>Grid geometry value</th>
     *     <th>Canvas value</th>
     *   </tr><tr>
     *     <td>{@link GridGeometry#getCoordinateReferenceSystem()}</td>
     *     <td>{@link #getObjectiveCRS()}</td>
     *   </tr><tr>
     *     <td>{@link GridGeometry#getExtent()}</td>
     *     <td>{@link #getDisplayBounds()} rounded to enclosing integers</td>
     *   </tr><tr>
     *     <td>{@link GridGeometry#getGridToCRS(PixelInCell)}</td>
     *     <td>Inverse of {@link #getObjectiveToDisplay()}</td>
     *   </tr>
     * </table>
     *
     * @param  allDimensions  if {@code true}, all dimensions from the point of interest are included in
     *         the returned grid geometry. If {@code false}, only the displayed dimensions are included.
     * @return a grid geometry encapsulating canvas properties.
     * @throws RenderException if the grid geometry can not be computed.
     */
    public GridGeometry getGridGeometry(final boolean allDimensions) throws RenderException {
        final GridExtent extent;
        if (displayBounds.isEmpty()) {
            extent = null;
        } else {
            // TODO: take allDimensions in account.
            final int dimension = displayBounds.getDimension();
            final long[] lower = new long[dimension];
            final long[] upper = new long[dimension];
            for (int i=0; i<dimension; i++) {
                lower[i] = (long) Math.floor(displayBounds.getMinimum(i));
                upper[i] = (long) Math.ceil (displayBounds.getMaximum(i));
            }
            final DimensionNameType[] axisTypes = getDisplayAxes();
            extent = new GridExtent(axisTypes, lower, upper, false);
        }
        try {
            // TODO: take allDimensions in account.
            return new GridGeometry(extent, PixelInCell.CELL_CENTER, objectiveToDisplay.inverse(), objectiveCRS);
        } catch (TransformException e) {
            throw new RenderException(errors().getString(Errors.Keys.CanNotCompute_1, POINT_OF_INTEREST_PROPERTY), e);
        }
    }

    public void setGridGeometry(final GridGeometry geometry) throws RenderException {
        // TODO
    }

    public Optional<GeographicBoundingBox> getGeographicArea() {
        return Optional.empty();        // TODO
    }

    public double[] getResolution() {
        return null;
    }

    /**
     * Allocates a position which can hold a coordinates in objective or display CRS, or
     * returns {@code null} for letting {@link MathTransform} do the allocation themselves.
     * May be overridden by subclasses for a little bit more efficiency.
     */
    DirectPosition allocatePosition() {
        return null;
    }

    /**
     * Returns the transform from the given source CRS to the given target CRS with precedence for an operation
     * valid for the geographic area of this canvas. The transform returned by this method for the same pair of
     * CRS may differ depending on which area is currently visible in the canvas. All requests for a coordinate
     * operation should invoke this method instead than {@link CRS#findOperation(CoordinateReferenceSystem,
     * CoordinateReferenceSystem, GeographicBoundingBox)}.
     */
    private MathTransform findTransform(final CoordinateReferenceSystem source,
                                        final CoordinateReferenceSystem target) throws FactoryException
    {
        final CoordinateOperationContext context = new CoordinateOperationContext();
        final Optional<GeographicBoundingBox> geographicArea = getGeographicArea();
        geographicArea.ifPresent(context::setAreaOfInterest);
        return coordinateOperationFactory.createOperation(source, target, context).getMathTransform();
    }

    /**
     * Returns the resources bundle for error messages in the locale of this canvas.
     */
    private Errors errors() {
        return Errors.getResources(locale);
    }
}
