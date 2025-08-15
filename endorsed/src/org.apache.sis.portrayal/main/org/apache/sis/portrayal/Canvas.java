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

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
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
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.privy.DirectPositionView;
import org.apache.sis.referencing.privy.WraparoundApplicator;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.PixelInCell;

// Specific to the main and geoapi-3.1 branches:
import org.apache.sis.geometry.MismatchedReferenceSystemException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coverage.CannotEvaluateException;


/**
 * Common abstraction for implementations that manage the display and user manipulation
 * of spatial graphic elements. This base class makes no assumption about the geometry
 * of the display device (e.g. flat video monitor using Cartesian coordinate system,
 * or planetarium dome using spherical coordinate system).
 *
 * <p>This {@code Canvas} base class does not draw anything by itself.
 * Subclasses are responsible for drawing graphic elements.
 * The visual contents are usually geographic located symbols, features or images,
 * but some implementations can also manage non-geographic elements like a map scale.</p>
 *
 * <p>A {@code Canvas} manages four fundamental properties:</p>
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
 * There are three {@linkplain CoordinateReferenceSystem Coordinate Reference Systems}
 * involved in the rendering of geospatial data:
 *
 * <ol class="verbose">
 *   <li>The <dfn>data CRS</dfn> is specific to the data to be displayed.
 *       It may be anything convertible to the <i>objective CRS</i>.
 *       Different graphic elements may use different data CRS,
 *       potentially with a different number of dimensions.</li>
 *   <li>The {@linkplain #getObjectiveCRS objective CRS} is the common CRS in which all data
 *       are converted before to be displayed. If the objective CRS involves a map projection,
 *       it determines the deformation of shapes that user will see on the display device.
 *       The objective CRS should have the same number of dimensions as the display device
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
 * In addition of above-cited Coordinate Reference Systems, a {@code Canvas} also contains a point of interest.
 * The point of interest is often, but not necessarily, at the center of display area.
 * It defines the position where {@linkplain #getSpatialResolution() resolutions} will be computed,
 * and the position to keep fixed when scales and rotations are applied.
 *
 * <p>The point of interest can be expressed in any CRS;
 * it does not need to be the objective CRS or the CRS of any data.
 * However, the CRS of that point must have enough dimensions for being convertible to the CRS of all data.
 * This rule implies that the number of dimensions of the point of interest is equal or greater than
 * the highest number of dimensions found in data. The purpose is not only to specify which point to show in
 * (typically) the center of the display area, but also to specify which slice to select in all dimensions
 * not shown by the display device.</p>
 *
 * <h3>Example</h3>
 * If some data have (<var>x</var>,<var>y</var>,<var>z</var>) dimensions and
 * other data have (<var>x</var>,<var>y</var>,<var>t</var>) dimensions, then the point of interest shall contain
 * coordinate values for at least all of the (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>) dimensions
 * (i.e. it must be 4-dimensional, even if all data in this example are 3-dimensional). If the display device
 * is a two-dimensional screen showing map in the (<var>x</var>,<var>y</var>) dimensions (horizontal plane),
 * then the point of interest defines the <var>z</var> value (elevation or depth) and the <var>t</var> value
 * (date and time) of the slice to show.
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
 * Another common strategy is to interact with {@code Canvas} from a single thread,
 * for example the Swing or JavaFX event queue.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
public class Canvas extends Observable implements Localized {
    /**
     * The {@value} property name, used for notifications about changes in objective CRS.
     * The objective CRS is the Coordinate Reference System in which all data are transformed before displaying.
     * Its number of dimension is the determined by the display device (two for flat screens).
     * Associated values are instances of {@link CoordinateReferenceSystem}.
     *
     * @see #getObjectiveCRS()
     * @see #setObjectiveCRS(CoordinateReferenceSystem, DirectPosition)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String OBJECTIVE_CRS_PROPERTY = "objectiveCRS";

    /**
     * The {@value} property name, used for notifications about changes in <i>objective to display</i> conversion.
     * This conversion maps coordinates in the {@linkplain #getObjectiveCRS() objective CRS} to coordinates in the
     * {@linkplain #getDisplayCRS() display CRS}. Associated values are instances of {@link LinearTransform}.
     * The event class is the {@link TransformChangeEvent} specialization.
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
     * The point of interest defines the location of a representative point,
     * typically (but not necessarily) in the center of the data bounding box.
     * It defines also the slice coordinate values in all dimensions beyond the ones shown by the device.
     * Associated values are instances of {@link DirectPosition}.
     *
     * @see #getPointOfInterest(boolean)
     * @see #setPointOfInterest(DirectPosition)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String POINT_OF_INTEREST_PROPERTY = "pointOfInterest";

    /**
     * The {@value} property name.
     * The grid geometry is a synthetic property computed from other properties when requested.
     * The computed grid geometry may change every time that a {@value #OBJECTIVE_CRS_PROPERTY},
     * {@value #OBJECTIVE_TO_DISPLAY_PROPERTY}, {@value #DISPLAY_BOUNDS_PROPERTY} or
     * {@value #POINT_OF_INTEREST_PROPERTY} property is changed. We do not (at this time) fire
     * {@value} change events because computing a new grid geometry for every changes of above-cited
     * properties would be costly. An alternative approach could be to fire {@value} event only when
     * {@link #setGridGeometry(GridGeometry)} is explicitly invoked, but it could be misleading if
     * it gives the false impression that the grid geometry did not changed because a listener did
     * not received an {@value} event.
     *
     * @see #getGridGeometry()
     * @see #setGridGeometry(GridGeometry)
     */
    private static final String GRID_GEOMETRY_PROPERTY = "gridGeometry";

    /**
     * The {@value} property name. The geographic area is a synthetic property computed
     * from {@value #DISPLAY_BOUNDS_PROPERTY}, {@value #OBJECTIVE_TO_DISPLAY_PROPERTY}
     * and {@value #OBJECTIVE_CRS_PROPERTY}. There are no events fired for this property.
     *
     * @see #getGeographicArea()
     */
    private static final String GEOGRAPHIC_AREA_PROPERTY = "geographicArea";

    /**
     * The {@value} property name. The resolution is a synthetic property computed from
     * {@value #POINT_OF_INTEREST_PROPERTY}, {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} and
     * {@value #OBJECTIVE_CRS_PROPERTY}. There are no events fired for this property.
     *
     * @see #getSpatialResolution()
     */
    private static final String SPATIAL_RESOLUTION_PROPERTY = "spatialResolution";

    /**
     * The coordinate reference system in which to transform all data before displaying.
     * If {@code null}, then no transformation is applied and data coordinates are used directly
     * as display coordinates, regardless the data CRS (even if different data use different CRS).
     *
     * @see #OBJECTIVE_CRS_PROPERTY
     * @see #getObjectiveCRS()
     * @see #setObjectiveCRS(CoordinateReferenceSystem, DirectPosition)
     * @see #augmentedObjectiveCRS
     */
    private CoordinateReferenceSystem objectiveCRS;

    /**
     * The conversion from {@linkplain #getObjectiveCRS() objective CRS} to the display coordinate system.
     * Conceptually this conversion should never be null (its initial value is the identity conversion).
     * However, subclasses may use a more specialized type such as {@link java.awt.geom.AffineTransform}
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
     * @see #setDisplayBounds(Envelope)
     */
    final GeneralEnvelope displayBounds;

    /**
     * A point (in display coordinates) considered representative of the data.
     * This is the default location where Jacobian matrices are computed when needed.
     * This is typically (but not necessarily) the center of data bounding box.
     * May become outside the viewing area after zooms or translations have been applied.
     *
     * Also used for selecting a slice in all supplemental dimensions.
     * If {@code null}, then calculations that depend on a point of interest are skipped.
     *
     * @see #POINT_OF_INTEREST_PROPERTY
     * @see #getPointOfInterest(boolean)
     * @see #setPointOfInterest(DirectPosition)
     */
    private GeneralDirectPosition pointOfInterest;

    /**
     * The point of interest transformed to the objective CRS, or {@code null} if {@link #pointOfInterest}
     * has not yet been provided. This point shall be updated immediately when {@link #pointOfInterest} is
     * updated, as a way to verify that the point is valid.
     *
     * <p>There is no setter method for this property. It is computed from {@link #pointOfInterest}
     * and {@link #objectiveCRS} (indirectly, through {@link #multidimToObjective}) and should be
     * recomputed when any of those properties changed.</p>
     *
     * @see #getObjectivePOI()
     * @see #getGridGeometry()
     */
    private DirectPosition objectivePOI;

    /**
     * The transform from the multi-dimensional CRS of {@link #pointOfInterest} to the objective CRS.
     * This is the transform used for computing {@link #objectivePOI}. This transform may reduce the
     * number of dimensions.
     *
     * <p>There is no setter method for this property. It is computed from {@link #pointOfInterest}
     * and {@link #objectiveCRS} and should be recomputed when any of those properties changed.</p>
     *
     * <p>In current implementation, this transform may depend on the zoom level and viewed geographic
     * area at the time this transform has been computed. The transform could be slightly different if
     * it has been computed at the time a different geographic area was viewed. Those variations may exist
     * because {@link #findTransform(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)} takes
     * in account the current viewing conditions. We may need to revisit this behavior in the future if
     * it appears to be a problem.</p>
     *
     * @see #getGridGeometry()
     */
    private MathTransform multidimToObjective;

    /**
     * The {@link #objectiveCRS} augmented with additional dimensions found in {@link #pointOfInterest}.
     * This field is initially {@code null} and computed only if needed. It may be reset to {@code null}
     * at any time if the properties used for computing this value changed.
     *
     * <p>If the point of interest has no supplemental dimension, then this CRS is {@link #objectiveCRS}.
     * Otherwise {@linkplain #supplementalDimensions supplemental dimensions} are added on a best effort
     * basis: some supplemental dimensions may be missing if we have not been able to separate components
     * from the Point Of Interest CRS.</p>
     *
     * @see #getGridGeometry()
     */
    private CoordinateReferenceSystem augmentedObjectiveCRS;

    /**
     * The dimensions in the Point Of Interest CRS that are not in the {@link #objectiveCRS}.
     * Those dimensions are encoded as a bitmask: if dimension <var>n</var> is a supplemental
     * dimension, then the bit {@code 1L << n} is set to 1. This encoding implies that we can
     * not handle more than {@value Long#SIZE} dimensions at this time.
     *
     * <p>The value of this field is invalid if {@link #augmentedObjectiveCRS} is {@code null}.
     * Those two fields are computed together.</p>
     *
     * @see #getGridGeometry()
     */
    private long supplementalDimensions;

    /**
     * Type of each grid axis (column, row, vertical, temporal, …) or {@code null} if unspecified.
     * This is only a help for debugging purpose, by providing more information to the developers.
     * Those types should not be used for any "real" work.
     *
     * @see #getGridGeometry()
     */
    private DimensionNameType[] axisTypes;

    /**
     * The grid geometry, computed when first needed and reset to {@code null} when invalidated.
     * This is invalidated when any {@link Canvas} property is modified. In particular, this is
     * invalidated every time that the {@link #objectiveToDisplay} transform changes. Note that
     * "objective to display" changes happen much more often than changes in other properties.
     *
     * <p>The {@link #augmentedObjectiveCRS}, {@link #supplementalDimensions}, {@link #multidimToObjective},
     * {@link #objectivePOI} and {@link #axisTypes} objects are intermediate calculations with typically a
     * longer lifetime than {@code gridGeometry}. They are saved for faster recomputation of grid geometry
     * when only the {@link #objectiveToDisplay} transform has changed.</p>
     */
    private GridGeometry gridGeometry;

    /**
     * The context (geographic area and desired resolution) for selecting a coordinate operation.
     * The information contained in this object can opportunistically be used for providing the
     * geographic area and spatial resolution of this canvas.
     *
     * @see #getGeographicArea()
     * @see #getSpatialResolution()
     * @see #findTransform(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)
     */
    private final CanvasContext operationContext;

    /**
     * The factory to use for creating coordinate operations. This factory allow us to specify the area
     * of interest (the geographic region shown by this {@code Canvas}) and the desired resolution.
     *
     * @see #findTransform(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)
     */
    private final DefaultCoordinateOperationFactory coordinateOperationFactory;

    /**
     * The locale for labels or error messages, or {@code null} for the default.
     */
    private final Locale locale;

    /**
     * Creates a new canvas for a display device using the given coordinate reference system.
     * The display CRS of a canvas cannot be changed after construction.
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
        coordinateOperationFactory = DefaultCoordinateOperationFactory.provider();
        operationContext = new CanvasContext();
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
     * Gets the name of display axes and stores them in the given array. Those display axis names
     * are used for debugging purposes only, as an additional information provided to developers.
     * Those names should not be used for any "real" work. The default implementation does nothing
     * since this base {@link Canvas} class does not know well the geometry of the display device.
     * It is okay to leave elements to {@code null}.
     *
     * @param  axisTypes  where to store the name of display axes. The array length will be
     *                    at least {@link #getDisplayDimensions()} (it will often be longer).
     */
    void getDisplayAxes(final DimensionNameType[] axisTypes) {
    }

    /**
     * Returns the Coordinate Reference System of the display device.
     * The axis units of measurement are typically (but not necessarily) {@link Units#PIXEL}
     * for Cartesian coordinate systems, with {@link Units#DEGREE} in polar, cylindrical or
     * spherical coordinate systems. The coordinate system may have a wraparound axis for
     * some "exotic" display devices (e.g. planetarium dome).
     *
     * <p>Note that the {@link CRS#findOperation CRS.findOperation(…)} static method can generally
     * not handle this display CRS. To apply coordinate operations on display coordinates,
     * {@link #getObjectiveToDisplay()} transform must be inverted and used.</p>
     *
     * <h4>Usage note</h4>
     * Invoking this method is rarely needed.
     * It is sufficient to said that a display CRS exists at least conceptually,
     * and that we define a conversion from the objective CRS to that display CRS.
     * This method may be useful when the subclasses may be something else than {@link PlanarCanvas},
     * in which case the caller may want more information about the geometry of the display device.
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
     * The new CRS must be compatible with the previous CRS, i.e. a coordinate operation between
     * the two CRSs shall exist. If this is not the case (e.g. for rendering completely new data),
     * use {@link #setGridGeometry(GridGeometry)} instead.
     *
     * <p>The given CRS should have a domain of validity wide enough for encompassing all data
     * (the {@link CRS#suggestCommonTarget CRS.suggestCommonTarget(…)} method may be helpful
     * for choosing an objective CRS from a set of data CRS).
     * If the given value is different than the previous value, then a change event is sent to
     * all listeners registered for the {@value #OBJECTIVE_CRS_PROPERTY} property.</p>
     *
     * <p>If the transform between old and new CRS is not identity, then this method recomputes the
     * <i>objective to display</i> conversion in a way preserving the display coordinates of the given anchor,
     * together with the scales and orientations of features in close neighborhood of that point.
     * This calculation may cause {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} property change event
     * with the {@link TransformChangeEvent.Reason#CRS_CHANGE} reason to be sent to listeners.
     * That event is sent after the above-cited {@value #OBJECTIVE_CRS_PROPERTY} event
     * (note that {@value #POINT_OF_INTEREST_PROPERTY} stay unchanged).
     * All those change events are sent only after all property values have been updated to their new values.</p>
     *
     * @param  newValue  the new Coordinate Reference System in which to transform all data before displaying.
     * @param  anchor    the point to keep at fixed display coordinates, expressed in any compatible CRS.
     *                   If {@code null}, defaults to {@linkplain #getPointOfInterest(boolean) point of interest}.
     *                   If non-null, the anchor must be associated to a CRS.
     * @throws NullPointerException if the given CRS is null.
     * @throws MismatchedDimensionException if the given CRS does not have the number of dimensions of the display device.
     * @throws RenderException if the objective CRS cannot be set to the given value for another reason.
     */
    public void setObjectiveCRS(final CoordinateReferenceSystem newValue, DirectPosition anchor) throws RenderException {
        ArgumentChecks.ensureNonNull(OBJECTIVE_CRS_PROPERTY, newValue);
        ArgumentChecks.ensureDimensionMatches(OBJECTIVE_CRS_PROPERTY, getDisplayDimensions(), newValue);
        final CoordinateReferenceSystem oldValue = objectiveCRS;
        if (!newValue.equals(oldValue)) try {
            final CoordinateOperation newToGeo = objectiveToGeographic(newValue);
            LinearTransform oldObjectiveToDisplay = null;
            LinearTransform newObjectiveToDisplay = null;
            if (oldValue != null) {
                /*
                 * Compute the change unconditionally as a way to verify that the new CRS is compatible with
                 * data currently shown. Another reason is that checking identity transform is more reliable
                 * than the `compareIgnoreMetadata(oldValue, newValue)` check.
                 *
                 * Note: we are invoking `findTransform(…)` with a CoordinateOperationContext computed from
                 * the old CRS. But it is okay because the context information are geographic area (degrees)
                 * and approximate resolution (metres), which should not change a lot since we will continue
                 * to view the same area after the CRS change. Those information only need to be approximate
                 * anyway, and in many cases will be totally ignored by `findTransform(…)`.
                 */
                final MathTransform newToOld = findTransform(newValue, oldValue, false);
                if (pointOfInterest != null && !newToOld.isIdentity()) {
                    final CoordinateReferenceSystem poiCRS = pointOfInterest.getCoordinateReferenceSystem();
                    final MathTransform  poiToNew = findTransform(poiCRS, newValue, false);
                    final DirectPosition poiInNew = poiToNew.transform(pointOfInterest, allocatePosition());
                    /*
                     * We need anchor in new CRS. If no anchor was specified, `poiInNew` is already what we need.
                     * Otherwise convert the anchor to coordinates in the new CRS. There is good chances that the
                     * anchor CRS is the objective CRS, so we can reuse `poiToNew`.
                     */
                    if (anchor == null) {
                        anchor = poiInNew;
                    } else {
                        final CoordinateReferenceSystem crs = anchor.getCoordinateReferenceSystem();
                        ArgumentChecks.ensureNonNull("anchor.CRS", crs);
                        if (!Utilities.equalsIgnoreMetadata(crs, newValue)) {
                            MathTransform anchorToNew = poiToNew;
                            if (!Utilities.equalsIgnoreMetadata(crs, poiCRS)) {
                                anchorToNew = findTransform(crs, newValue, true);
                            }
                            anchor = anchorToNew.transform(anchor, allocatePosition());
                        }
                    }
                    /*
                     * We want pixel coordinates of the Point Of Interest (POI) to be unaffected by the change of CRS,
                     * and the Jacobian matrix around POI to be approximately the same. Conceptually, this is as if we
                     * wanted to convert from new CRS to old CRS before to apply the old `objectiveToCRS` transform.
                     * We get this effect by pre-concatenating a linear approximation of "new to old CRS" transform
                     * before `objectiveToCRS`. That approximation contains only uniform scale, rotation or axis flips
                     * in order to preserve pixel ratios (otherwise the map projection would appear deformed).
                     */
                    oldObjectiveToDisplay = getObjectiveToDisplay();
                    final WraparoundApplicator wp = new WraparoundApplicator(null, objectivePOI, oldValue.getCoordinateSystem());
                    final MathTransform change = orthogonalTangent(wp.forDomainOfUse(newToOld), anchor.getCoordinates());
                    final MathTransform result = MathTransforms.concatenate(change, oldObjectiveToDisplay);
                    /*
                     * The result is the new `objectiveToTransform` such as the display is unchanged around POI.
                     * That transform should be an instance of `LinearTransform` because the two concatenated
                     * transforms were linear, but we nevertheless invoke `tangent(…)` as a safety;
                     * normally it should just return the `result` as-is.
                     */
                    newObjectiveToDisplay = MathTransforms.tangent(result, poiInNew);
                    setObjectiveToDisplayImpl(newObjectiveToDisplay);
                    objectivePOI          = poiInNew;               // Set only after everything else succeeded.
                    multidimToObjective   = poiToNew;
                    augmentedObjectiveCRS = null;                   // Will be recomputed when first needed.
                    axisTypes             = null;
                    gridGeometry          = null;
                }
            }
            objectiveCRS = newValue;                                // Set only after everything else succeeded.
            operationContext.setObjectiveToGeographic(newToGeo);
            firePropertyChange(OBJECTIVE_CRS_PROPERTY, oldValue, newValue);
            fireIfChanged(oldObjectiveToDisplay, newObjectiveToDisplay, false);     // Shall be after CRS change event.
        } catch (FactoryException | TransformException e) {
            throw new RenderException(errors().getString(Errors.Keys.CanNotSetPropertyValue_1, OBJECTIVE_CRS_PROPERTY), e);
        }
    }

    /**
     * Computes the approximate change from a new {@link #objectiveToDisplay} to the old one for keeping the
     * Point Of Interest (POI) at the same location. The given {@code newToOld} argument is the change as a
     * potentially non-linear transform. The transform returned by this method is a linear approximation of
     * {@code newToOld} {@linkplain MathTransforms#tangent tangent} at the POI, but with orthogonal vectors.
     * In other words, the returned transform may apply a uniform scale, a rotation or flip axes, but no shear.
     *
     * @param  newToOld  the change as a potentially non-linear transform.
     * @param  poiInNew  point of interest in the coordinates of the new objective CRS.
     * @return an approximation of {@code newToOld} with only uniform scale, rotation and axis flips.
     *
     * @see MathTransforms#tangent(MathTransform, DirectPosition)
     */
    private static MathTransform orthogonalTangent(final MathTransform newToOld, final double[] poiInNew)
            throws TransformException, RenderException
    {
        final double[]  poiInOld   = new double[newToOld.getTargetDimensions()];
        final MatrixSIS derivative = MatrixSIS.castOrCopy(MathTransforms.derivativeAndTransform(newToOld, poiInNew, 0, poiInOld, 0));
        final MatrixSIS magnitudes = derivative.normalizeColumns();
        final MatrixSIS affine     = Matrices.createAffine(derivative, new DirectPositionView.Double(poiInOld));
        final int       srcDim     = magnitudes.getNumCol();
        DoubleDouble    scale      = DoubleDouble.ZERO;             // Will be set to average magnitude value.
        for (int i=0; i<srcDim; i++) {
            scale = scale.add(magnitudes.getNumber(0, i), false);
        }
        scale = scale.divide(srcDim);
        /*
         * Following code assumes a two-dimensional rotation matrix. We have not yet explored how
         * to generalize to n-dimensional case (Gram–Schmidt process may be a path to explore).
         * We want:
         *           ┌          ┐     ┌                 ┐
         *           │ m₀₀  m₀₁ │  ≈  │ cos(θ)  −sin(θ) │
         *           │ m₁₀  m₁₁ │     │ sin(θ)   cos(θ) │
         *           └          ┘     └                 ┘
         *
         * We want some "average" value for |cos(θ)| (the sign will be adjusted later).
         * The root mean square (RMS) is convenient because of  cos²(θ) = 1 − sin²(θ):
         *
         *     |cos(θ)|  ≈  √((m₀₀² + (1 − m₀₁²) + (1 − m₁₀²) + m₁₁²) / 4)
         */
        if (srcDim == PlanarCanvas.BIDIMENSIONAL && poiInOld.length == PlanarCanvas.BIDIMENSIONAL) {
            final double ms  = Math.max(0, Math.min(1, (cps(affine, 0) + cps(affine, 1) + 2) / 4));
            final double sin = Math.sqrt(1 - ms);
            final double cos = Math.sqrt(    ms);
            for (int row = 0; row <= 1; row++) {
                final int sor = row ^ 1;
                affine.setElement(row, row, Math.copySign(cos, affine.getElement(row, row)));
                affine.setElement(row, sor, Math.copySign(sin, affine.getElement(row, sor)));
            }
        } else {
            throw new RenderException(Errors.format(Errors.Keys.UnsupportedCoordinateSystem_1, "3D"));
        }
        for (int i=0; i<srcDim; i++) {
            affine.convertBefore(i, scale, null);           // Use same scale factor for all coordinates.
            affine.convertBefore(i, null, -poiInNew[i]);
        }
        return MathTransforms.linear(affine);
    }

    /**
     * Computes cos(θ)² − sin²(θ) on the given matrix row. Caller needs to add 1 for getting the sum
     * of squares of cosine values. That addition should be done last for reducing rounding errors.
     */
    private static double cps(final MatrixSIS affine, final int row) {
        final double cos = affine.getElement(row, row);
        final double sin = affine.getElement(row, row ^ 1);
        return cos*cos - sin*sin;
    }

    /**
     * Returns the (usually affine) conversion from objective CRS to display coordinate system.
     * The source coordinates shall be in the CRS given by {@link #getObjectiveCRS()} and the
     * converted coordinates will be in the CRS given by {@link #getDisplayCRS()}.
     *
     * <p>The <i>objective to display</i> conversion changes every time that user zooms
     * or scrolls on viewed data. However, the transform returned by this method is a snapshot
     * taken at the time this method is invoked; subsequent changes in the <i>objective to
     * display</i> conversion are not reflected in the returned transform.</p>
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
            objectiveToDisplay = createObjectiveToDisplay();
        }
        return objectiveToDisplay;
    }

    /**
     * Returns the current <i>objective to display</i> conversion managed by the subclass.
     * This method is invoked only if {@link #objectiveToDisplay} is {@code null}, which may
     * happen either at initialization time or if the subclass uses its own specialized field
     * instead of {@link #objectiveToDisplay} for managing changes in the zooms or viewed area.
     * This method needs to be overridden only by subclasses using such specialization.
     *
     * @return objective to display conversion created from current value managed by subclass.
     *
     * @see #setObjectiveToDisplayImpl(LinearTransform)
     */
    LinearTransform createObjectiveToDisplay() {
        return MathTransforms.identity(getDisplayDimensions());
    }

    /**
     * Sets the conversion from objective CRS to display coordinate system.
     * If the given value is different than the previous value, then a change event is sent
     * to all listeners registered for the {@value #OBJECTIVE_TO_DISPLAY_PROPERTY} property.
     * The event reason is {@link TransformChangeEvent.Reason#ASSIGNMENT}.
     *
     * <p>Invoking this method has the effect of changing the viewed area, the zoom level or the rotation of the map.
     * It does not update the {@value #POINT_OF_INTEREST_PROPERTY} property however. The point of interest may move
     * outside the view area as a result of this method call.</p>
     *
     * @param  newValue  the new <i>objective to display</i> conversion.
     * @throws IllegalArgumentException if given the transform does not have the expected number of dimensions or is not affine.
     * @throws RenderException if the <i>objective to display</i> transform cannot be set to the given value for another reason.
     */
    public void setObjectiveToDisplay(final LinearTransform newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(OBJECTIVE_TO_DISPLAY_PROPERTY, newValue);
        final int expected = getDisplayDimensions();
        int actual = newValue.getSourceDimensions();
        if (actual == expected) {
            actual = newValue.getTargetDimensions();
            if (actual == expected) {
                LinearTransform oldValue = objectiveToDisplay;      // Do not invoke user-overridable method.
                if (oldValue == null) {
                    oldValue = createObjectiveToDisplay();
                }
                if (!oldValue.equals(newValue)) {
                    setObjectiveToDisplayImpl(newValue);
                    firePropertyChange(new TransformChangeEvent(this, oldValue, newValue,
                                           TransformChangeEvent.Reason.ASSIGNMENT));
                }
                return;
            }
        }
        throw new org.opengis.geometry.MismatchedDimensionException(errors().getString(
                Errors.Keys.MismatchedDimension_3, OBJECTIVE_TO_DISPLAY_PROPERTY, expected, actual));
    }

    /**
     * Actually sets the conversion from objective CRS to display coordinate system.
     * Contrarily to other setter methods, this method does not notify listeners about that change;
     * it is caller responsibility to fire a {@link TransformChangeEvent} after all fields are updated.
     * This design choice is because this method is usually invoked as part of a larger set of changes.
     *
     * <p>If the new value is {@code null}, then this method only declares that the {@link #objectiveToDisplay}
     * transform became invalid and will need to be recomputed. It is subclasses responsibility to recompute the
     * transform in their {@link #createObjectiveToDisplay()}.</p>
     *
     * @param  newValue  the new "objective to display" transform, or {@code null} if it will be computed later
     *          by {@link #createObjectiveToDisplay()}. A null value is okay only when invoked by subclasses that
     *          overrode {@link #createObjectiveToDisplay()}.
     *
     * @see #createObjectiveToDisplay()
     */
    void setObjectiveToDisplayImpl(final LinearTransform newValue) {
        objectiveToDisplay = newValue;
        gridGeometry       = null;
        operationContext.clear();
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
     * @see #getGeographicArea()
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
     * @throws RenderException if the display bounds cannot be set to the given value for another reason.
     */
    public void setDisplayBounds(final Envelope newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(DISPLAY_BOUNDS_PROPERTY, newValue);
        final CoordinateReferenceSystem crs = newValue.getCoordinateReferenceSystem();
        if (crs != null && !Utilities.equalsIgnoreMetadata(getDisplayCRS(), crs)) {
            throw new MismatchedReferenceSystemException(errors().getString(
                    Errors.Keys.IllegalCoordinateSystem_1, IdentifiedObjects.getDisplayName(crs, getLocale())));
        }
        final GeneralEnvelope oldValue = new GeneralEnvelope(displayBounds);
        displayBounds.setEnvelope(newValue);
        displayBounds.setCoordinateReferenceSystem(oldValue.getCoordinateReferenceSystem());
        if (displayBounds.isEmpty()) {
            displayBounds.setEnvelope(oldValue);
            throw new IllegalArgumentException(errors().getString(Errors.Keys.EmptyProperty_1, DISPLAY_BOUNDS_PROPERTY));
        }
        if (!oldValue.equals(displayBounds)) {
            gridGeometry = null;
            operationContext.partialClear(false);                               // Resolution is still valid.
            firePropertyChange(DISPLAY_BOUNDS_PROPERTY, oldValue, newValue);    // Do not publish reference to `displayBounds`.
        }
    }

    /**
     * Returns the coordinates of a point considered representative of the data.
     * This is typically (but not necessarily) the center of data bounding box.
     * This point is used for example as the default location where to compute resolution
     * (the resolution may vary at each pixel because of map projection deformations).
     * This position may become outside the viewing area after zooms or translations have been applied.
     *
     * <p>The coordinates can be given in their original CRS or in the {@linkplain #getObjectiveCRS() objective CRS}.
     * If {@code objective} is {@code false}, then the returned position can be expressed in any CRS convertible to
     * data or objective CRS. If that CRS has more dimensions than the {@linkplain #getObjectiveCRS() objective CRS},
     * then the supplemental dimensions specify which slice to show
     * (for example the depth of the horizontal plane to display, or the date of the dynamic phenomenon to display.
     * See {@linkplain Canvas class javadoc} for more discussion.)
     * If {@code objective} is {@code true}, then the position is transformed to the objective CRS.</p>
     *
     * <p>This value is initially {@code null}. A value should be specified either by invoking
     * {@link #setPointOfInterest(DirectPosition)} or {@link #setGridGeometry(GridGeometry)}.</p>
     *
     * @param  objective  whether to return a position transformed to {@linkplain #getObjectiveCRS() objective CRS}.
     * @return coordinates of a representative point, or {@code null} if unspecified.
     *
     * @see #POINT_OF_INTEREST_PROPERTY
     */
    public DirectPosition getPointOfInterest(final boolean objective) {
        final DirectPosition poi = objective ? objectivePOI : pointOfInterest;
        return (poi != null) ? new GeneralDirectPosition(poi) : null;
    }

    /**
     * Sets the coordinates of a representative point inside the data bounding box.
     * If the given value is different than the previous value, then a change event is sent to all listeners
     * registered for the {@value #POINT_OF_INTEREST_PROPERTY} property.
     *
     * @param  newValue  the new coordinates of a representative point.
     * @throws NullPointerException if the given position is null.
     * @throws IllegalArgumentException if the given position does not have a CRS.
     * @throws RenderException if the point of interest cannot be set to the given value.
     */
    public void setPointOfInterest(final DirectPosition newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(POINT_OF_INTEREST_PROPERTY, newValue);
        final GeneralDirectPosition copy = new GeneralDirectPosition(newValue);
        final CoordinateReferenceSystem crs = copy.getCoordinateReferenceSystem();
        if (crs == null) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.UnspecifiedCRS));
        }
        final GeneralDirectPosition oldValue = pointOfInterest;
        if (!copy.equals(oldValue)) try {
            /*
             * If the user has not yet specified an objective CRS, takes the Point Of Interest CRS
             * (only the number of dimensions that the display device can show).
             */
            if (objectiveCRS == null) {
                final CoordinateReferenceSystem newObjectiveCRS = CRS.getComponentAt(crs, 0, getDisplayDimensions());
                if (newObjectiveCRS == null) {
                    throw new IllegalArgumentException("Cannot infer objective CRS.");
                    // Message not localized yet because we should probably try harder.
                }
                operationContext.setObjectiveToGeographic(objectiveToGeographic(newObjectiveCRS));
                objectiveCRS = newObjectiveCRS;                            // Set only on success.
            }
            /*
             * Transform the Point Of Interest to the objective CRS as a way to test its validity.
             * All canvas fields will be updated only if this operation succeeds.
             *
             * Note 1: in the CoordinateOperationContext used for selecting a MathTransform, the geographic area is
             * still the same but the spatial resolution could be slightly different because computed at a new point
             * of interest. But we cannot use the new point of interest now, because we need the MathTransform for
             * computing it. However, in practice the resolution is often ignored, or does not vary a lot in regions
             * where it matter. So we assume it is okay to keep the CoordinateOperationContext with old resolution
             * in the following call to `findTransform(…)` or usage of `multidimToObjective`.
             *
             * Note 2: `oldValue` cannot be null if `multidimToObjective` is non-null.
             */
            MathTransform mt = multidimToObjective;
            if (mt == null || !Utilities.equalsIgnoreMetadata(crs, oldValue.getCoordinateReferenceSystem())) {
                mt = findTransform(crs, objectiveCRS, false);
            }
            objectivePOI          = mt.transform(copy, allocatePosition());
            pointOfInterest       = copy;                                           // Set only after transform succeeded.
            multidimToObjective   = mt;
            augmentedObjectiveCRS = null;                                           // Will be recomputed when first needed.
            axisTypes             = null;
            gridGeometry          = null;
            operationContext.partialClear(true);                                    // Geographic area is still valid.
            firePropertyChange(POINT_OF_INTEREST_PROPERTY, oldValue, newValue);     // Do not publish reference to `copy`.
        } catch (FactoryException | TransformException e) {
            throw new RenderException(errors().getString(Errors.Keys.CanNotSetPropertyValue_1, POINT_OF_INTEREST_PROPERTY), e);
        }
    }

    /**
     * Returns the coordinate values of the Point Of Interest (POI) in objective CRS.
     * The array length should be equal to {@link #getDisplayDimensions()}.
     * May be {@code null} if the point of interest is unknown.
     */
    final double[] getObjectivePOI() {
        return (objectivePOI != null) ? objectivePOI.getCoordinates() : null;
    }

    /**
     * Returns canvas properties (CRS, display bounds, conversion) encapsulated in a grid geometry.
     * This is a convenience method for interoperability with grid coverage API.
     * If {@link #setGridGeometry(GridGeometry)} has been invoked with a non-null value and no other
     * {@code Canvas} property changed since that method call, then this method returns that value.
     * Otherwise this method computes a grid geometry as described below.
     *
     * <p>The set of {@link GridGeometry} dimensions includes all the dimensions of the objective CRS,
     * augmented with all (if possible) or some supplemental dimensions found in the point of interest.
     * For example if the canvas manages only (<var>x</var>,<var>y</var>) coordinates but the point of
     * interest includes also a <var>t</var> coordinate, then a third dimension (which we call the
     * <i>supplemental dimension</i>) for <var>t</var> is added to the CRS, {@link GridExtent}
     * and "grid to CRS" transform of the returned grid geometry.</p>
     *
     * <table class="sis">
     *   <caption>Canvas properties → grid geometry properties</caption>
     *   <tr>
     *     <th>Grid geometry element</th>
     *     <th>Display dimensions</th>
     *     <th>Supplemental dimensions</th>
     *   </tr><tr>
     *     <td>{@link GridGeometry#getCoordinateReferenceSystem()}</td>
     *     <td>{@link #getObjectiveCRS()}.</td>
     *     <td>Some of <code>{@linkplain #getPointOfInterest(boolean)
     *         getPointOfInterest}(false).getCoordinateReferenceSystem()</code></td>
     *   </tr><tr>
     *     <td>{@link GridGeometry#getExtent()}</td>
     *     <td>{@link #getDisplayBounds()} rounded to enclosing (floor and ceil) integers</td>
     *     <td>[0 … 0]</td>
     *   </tr><tr>
     *     <td>{@link GridGeometry#getGridToCRS(PixelInCell)}</td>
     *     <td>Inverse of {@link #getObjectiveToDisplay()}</td>
     *     <td>Some {@linkplain #getPointOfInterest(boolean) point of interest} coordinates as translation terms</td>
     *   </tr>
     * </table>
     *
     * The {@link GridGeometry#getGridToCRS(PixelInCell)} transform built by this method is always a {@link LinearTransform}.
     * This linearity implies that the grid geometry CRS cannot be the Point Of Interest (POI) CRS, unless conversion
     * from POI CRS to objective CRS is linear.
     *
     * @return a grid geometry encapsulating canvas properties, including supplemental dimensions if possible.
     * @throws RenderException if the grid geometry cannot be computed.
     */
    public GridGeometry getGridGeometry() throws RenderException {
        if (gridGeometry == null) try {
            /*
             * If not already done, create a multi-dimensional CRS composed of `objectiveCRS`
             * with supplemental dimensions appended. This CRS needs to be recreated only if
             * the Point of Interest and/or the objective CRS changed since last call.
             */
            if (augmentedObjectiveCRS == null) {
                if (pointOfInterest != null && objectiveCRS != null) {
                    final CoordinateReferenceSystem crs = pointOfInterest.getCoordinateReferenceSystem();
                    final ArrayList<CoordinateReferenceSystem> components = new ArrayList<>(4);
                    components.add(objectiveCRS);
                    /*
                     * `findSupplementalDimensions(…)` tries to complete the `components` list on a best effort basis.
                     * We have no guarantees that all supplemental dimensions will be included. The set of dimensions
                     * actually appended is encoded in `supplementalDimensions` bits.
                     */
                    supplementalDimensions = CanvasExtent.findSupplementalDimensions(crs,
                            multidimToObjective.derivative(pointOfInterest), components);
                    augmentedObjectiveCRS = CRS.compound(components.toArray(CoordinateReferenceSystem[]::new));
                    if (Utilities.equalsIgnoreMetadata(augmentedObjectiveCRS, crs)) {
                        augmentedObjectiveCRS = crs;
                    }
                } else {
                    augmentedObjectiveCRS = objectiveCRS;
                }
                /*
                 * The axis types are for information purposes only, for making debugging easier.
                 * It will typically contains the (column, row) names, maybe completed with up or
                 * time names.
                 */
                axisTypes = CanvasExtent.suggestAxisTypes(augmentedObjectiveCRS, getDisplayDimensions());
                getDisplayAxes(axisTypes);
            }
            /*
             * Create the `gridToCRS` transform using the "display to objective" transform augmented with POI
             * coordinate values in supplemental dimensions. Those coordinate values will be stored in the
             * translation terms of the `gridToCRS` matrix.
             */
            if (objectiveToDisplay == null) {
                objectiveToDisplay = createObjectiveToDisplay();
            }
            LinearTransform gridToCRS = objectiveToDisplay.inverse();
            if (supplementalDimensions != 0) {
                gridToCRS = CanvasExtent.createGridToCRS(gridToCRS.getMatrix(), pointOfInterest, supplementalDimensions);
            }
            /*
             * Create the grid extent with a number of dimensions that include the supplemental dimensions.
             * The cell indices range of all supplemental dimensions is [0 … 0]. If a point of interest is
             * available, the `GridExtent` will contain the grid coordinates of that point.
             */
            final GridExtent extent;
            if (displayBounds.isEmpty()) {
                extent = null;
            } else {
                DirectPosition poi = objectivePOI;
                if (poi != null) {
                    poi = objectiveToDisplay.transform(objectivePOI, null);
                }
                extent = CanvasExtent.create(displayBounds, poi, axisTypes, gridToCRS.getSourceDimensions());
            }
            gridGeometry = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, augmentedObjectiveCRS);
        } catch (FactoryException | TransformException e) {
            throw new RenderException(errors().getString(Errors.Keys.CanNotCompute_1, GRID_GEOMETRY_PROPERTY), e);
        }
        return gridGeometry;
    }

    /**
     * Sets canvas properties from the given grid geometry. This convenience method converts the
     * coordinate reference system, "grid to CRS" transform and extent of the given grid geometry
     * to {@code Canvas} properties. If the given value is different than the previous value, then
     * change events are sent to all listeners registered for the {@value #DISPLAY_BOUNDS_PROPERTY},
     * {@value #OBJECTIVE_CRS_PROPERTY}, {@value #OBJECTIVE_TO_DISPLAY_PROPERTY}
     * (with {@link TransformChangeEvent.Reason#GRID_GEOMETRY_CHANGE} reason),
     * and/or {@value #POINT_OF_INTEREST_PROPERTY} properties, in that order.
     *
     * <p>The value given to this method will be returned by {@link #getGridGeometry()} as long as
     * none of above cited properties is changed. If one of those properties changes (for example
     * if the user zooms or pans the map), then a new grid geometry will be computed. There is no
     * guarantee that the recomputed grid geometry will be similar to the grid geometry specified
     * to this method. For example, the {@link GridExtent} in supplemental dimensions may be different.</p>
     *
     * @param  newValue  the grid geometry from which to get new canvas properties.
     * @throws RenderException if the given grid geometry cannot be converted to canvas properties.
     */
    public void setGridGeometry(final GridGeometry newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(GRID_GEOMETRY_PROPERTY, newValue);
        if (!newValue.equals(gridGeometry)) try {
            /*
             * Do not test grid.isDefined(…) — we consider all elements as mandatory for this method.
             * First, get the dimensions to show in the canvas by searching dimensions having a span
             * larger than 1 grid cell. Those spans will become the sizes of display bounds.
             *
             * Result of this block: DISPLAY_BOUNDS_PROPERTY: newBounds
             */
            final GridExtent extent = newValue.getExtent();
            final int[] displayDimensions = extent.getSubspaceDimensions(getDisplayDimensions());
            final var newBounds = new GeneralEnvelope(getDisplayCRS());
            for (int i=0; i<displayDimensions.length; i++) {
                final int s = displayDimensions[i];
                newBounds.setRange(i, extent.getLow(s), Math.incrementExact(extent.getHigh(s)));
            }
            /*
             * Computes the point of interest in the Coordinate Reference System (CRS) of the given grid geometry.
             * This point will also contain the coordinates in supplemental dimensions (if any), such as vertical
             * and temporal positions of the slice shown in this canvas. Those supplemental coordinates should be
             * computed in cell centers. This suggests that we should use PixelInCell.CELL_CENTER transform, but
             * actually the coordinates returned by `extent.getPointOfInterest()` for [x … x] ranges (span of 1,
             * as required for supplemental dimensions) already includes a 0.5 fraction digit.
             *
             * Result of this block: POINT_OF_INTEREST_PROPERTY: newPOI
             */
            final MathTransform gridToCRS = newValue.getGridToCRS(PixelInCell.CELL_CORNER);
            final CoordinateReferenceSystem crs;
            final GeneralDirectPosition newPOI;
            if (newValue.isDefined(GridGeometry.CRS)) {
                crs = newValue.getCoordinateReferenceSystem();
                newPOI = new GeneralDirectPosition(crs);
            } else {
                crs = null;
                newPOI = new GeneralDirectPosition(gridToCRS.getTargetDimensions());
            }
            gridToCRS.transform(extent.getPointOfInterest(PixelInCell.CELL_CORNER), 0, newPOI.coordinates, 0, 1);
            /*
             * Get the CRS component in the dimensions shown by this canvas.
             *
             * Result of this block: OBJECTIVE_CRS_PROPERTY:        newObjectiveCRS
             *                       OBJECTIVE_TO_DISPLAY_PROPERTY: newObjToDisplay
             */
            final TransformSeparator analyzer = new TransformSeparator(gridToCRS, coordinateOperationFactory.getMathTransformFactory());
            analyzer.addSourceDimensions(displayDimensions);
            final LinearTransform           newObjectiveToDisplay = MathTransforms.tangent(analyzer.separate().inverse(), newPOI);
            final int[]                     objectiveDimensions   = analyzer.getTargetDimensions();
            final CoordinateReferenceSystem newObjectiveCRS       = CRS.selectDimensions(crs, objectiveDimensions);
            final MathTransform             dimensionSelect       = MathTransforms.linear(
                    Matrices.createDimensionSelect(newPOI.getDimension(), objectiveDimensions));
            /*
             * At this point we are ready to commit the new values. Before doing so, copy
             * the current property values in order to provide the old values to listeners.
             */
            final GeneralEnvelope           oldBounds             = new GeneralEnvelope(displayBounds);
            final DirectPosition            oldPOI                = pointOfInterest;
            final LinearTransform           oldObjectiveToDisplay = objectiveToDisplay;
            final CoordinateReferenceSystem oldObjectiveCRS       = objectiveCRS;
            /*
             * Set internal fields only after we successfully computed everything,
             * in order to have a "all or nothing" behavior.
             */
            displayBounds.setEnvelope(newBounds);
            setObjectiveToDisplayImpl(newObjectiveToDisplay);
            pointOfInterest       = newPOI;
            objectivePOI          = newPOI;
            objectiveCRS          = newObjectiveCRS;
            multidimToObjective   = dimensionSelect;
            augmentedObjectiveCRS = null;               // Will be recomputed when first needed.
            axisTypes             = null;
            gridGeometry          = newValue;
            /*
             * Notify listeners only after all properties have been updated. If a listener throws an exception,
             * other listeners will not be notified but this Canvas will not be corrupted since all the work to
             * do in this class is already completed. Order matter, it is documented in this method javadoc.
             */
            fireIfChanged(DISPLAY_BOUNDS_PROPERTY,    oldBounds,             newBounds);
            fireIfChanged(OBJECTIVE_CRS_PROPERTY,     oldObjectiveCRS,       newObjectiveCRS);
            fireIfChanged(/* OBJECTIVE_TO_DISPLAY */  oldObjectiveToDisplay, newObjectiveToDisplay, true);
            fireIfChanged(POINT_OF_INTEREST_PROPERTY, oldPOI,                newPOI);
        } catch (IncompleteGridGeometryException | CannotEvaluateException | FactoryException | TransformException e) {
            throw new RenderException(errors().getString(Errors.Keys.CanNotSetPropertyValue_1, GRID_GEOMETRY_PROPERTY), e);
        }
    }

    /**
     * Fires a property change event if the old and new values are not equal.
     *
     * @param  propertyName  name of the property that changed its value.
     * @param  oldValue      the old property value (may be {@code null}).
     * @param  newValue      the new property value.
     */
    private void fireIfChanged(final String propertyName, final Object oldValue, final Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Fires a property change event if the old and new transforms are not equal.
     *
     * @param  oldValue  the old "objective to display" transform.
     * @param  newValue  the new transform, or {@code null} for lazy computation.
     * @param  grid      {@code true} if the reason is a grid geometry change, or {@code false} if only a CRS change.
     */
    private void fireIfChanged(final LinearTransform oldValue, final LinearTransform newValue, final boolean grid) {
        if (!Objects.equals(oldValue, newValue)) {
            firePropertyChange(new TransformChangeEvent(this, oldValue, newValue,
                    grid ? TransformChangeEvent.Reason.GRID_GEOMETRY_CHANGE
                         : TransformChangeEvent.Reason.CRS_CHANGE));
        }
    }

    /**
     * Returns the geographic bounding box encompassing the area shown on the display device.
     * If the {@linkplain #getObjectiveCRS() objective CRS} is not convertible to a geographic CRS,
     * then this method returns an empty value.
     *
     * @return geographic bounding box encompassing the viewed area.
     * @throws RenderException in an error occurred while computing the geographic area.
     *
     * @see #getDisplayBounds()
     */
    public Optional<GeographicBoundingBox> getGeographicArea() throws RenderException {
        try {
            return operationContext.getGeographicArea(this);
        } catch (TransformException e) {
            throw new RenderException(errors().getString(Errors.Keys.CanNotCompute_1, GEOGRAPHIC_AREA_PROPERTY), e);
        }
    }

    /**
     * Returns an estimation of the resolution (in metres) at the point of interest.
     * If the {@linkplain #getObjectiveCRS() objective CRS} is not convertible to a
     * geographic CRS, then this method returns an empty value.
     *
     * @return estimation of the resolution in metres at current point of interest.
     * @throws RenderException in an error occurred while computing the resolution.
     */
    public OptionalDouble getSpatialResolution() throws RenderException {
        try {
            return operationContext.getSpatialResolution(this);
        } catch (TransformException e) {
            throw new RenderException(errors().getString(Errors.Keys.CanNotCompute_1, SPATIAL_RESOLUTION_PROPERTY), e);
        }
    }

    /**
     * Computes the value for {@link #objectiveToGeographic}. The value is not stored by this method for
     * giving caller a chance to validate other properties before to write them in a "all or nothing" way.
     *
     * @param  crs  the new objective CRS in process of being set by the caller.
     * @return the conversion from given CRS to geographic CRS, or {@code null} if none.
     */
    private CoordinateOperation objectiveToGeographic(final CoordinateReferenceSystem crs) throws FactoryException {
        final GeographicCRS geoCRS = ReferencingUtilities.toNormalizedGeographicCRS(crs, false, false);
        return (geoCRS != null) ? coordinateOperationFactory.createOperation(crs, geoCRS, (CanvasContext) null) : null;
    }

    /**
     * Returns the transform from the given source CRS to the given target CRS with precedence for an operation
     * valid for the geographic area of this canvas. The transform returned by this method for the same pair of
     * CRS may differ depending on which area is currently visible in the canvas. All requests for a coordinate
     * operation should invoke this method instead of {@link CRS#findOperation(CoordinateReferenceSystem,
     * CoordinateReferenceSystem, GeographicBoundingBox)}.
     *
     * @param  allowDisplayCRS  whether the {@code sourceCRS} can be {@link #getDisplayCRS()}.
     */
    private MathTransform findTransform(CoordinateReferenceSystem source,
                                  final CoordinateReferenceSystem target,
                                  boolean allowDisplayCRS)
            throws FactoryException, TransformException, RenderException
    {
        if (allowDisplayCRS) {
            allowDisplayCRS = Utilities.equalsIgnoreMetadata(source, displayBounds.getCoordinateReferenceSystem());
        }
        if (allowDisplayCRS) {
            source = objectiveCRS;
        }
        operationContext.refresh(this);
        MathTransform tr = coordinateOperationFactory.createOperation(source, target, operationContext).getMathTransform();
        if (allowDisplayCRS) {
            tr = MathTransforms.concatenate(getObjectiveToDisplay().inverse(), tr);
        }
        return tr;
    }

    /**
     * Allocates a position which can hold a coordinates in objective CRS.
     * May be overridden by subclasses for a little bit more efficiency.
     */
    DirectPosition allocatePosition() {
        return new GeneralDirectPosition(objectiveCRS);
    }

    /**
     * Returns the resources bundle for error messages in the locale of this canvas.
     */
    private Errors errors() {
        return Errors.forLocale(locale);
    }
}
