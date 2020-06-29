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
package org.apache.sis.gui.coverage;

import java.util.Map;
import java.util.EnumMap;
import java.util.Locale;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javafx.scene.paint.Color;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.event.EventHandler;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.beans.value.ChangeListener;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.FactoryException;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.Interpolation;
import org.apache.sis.gui.map.MapCanvas;
import org.apache.sis.gui.map.MapCanvasAWT;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.gui.referencing.PositionableProjection;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.measure.Units;


/**
 * A canvas for {@link RenderedImage} provided by a {@link GridCoverage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see CoverageExplorer
 *
 * @since 1.1
 * @module
 */
@DefaultProperty("coverage")
public class CoverageCanvas extends MapCanvasAWT {
    /**
     * The data shown in this canvas. Note that setting this property to a non-null value may not
     * modify the canvas content immediately. Instead, a background process will request the tiles.
     *
     * <p>Current implementation is restricted to {@link GridCoverage} instances, but a future
     * implementation may generalize to {@link org.opengis.coverage.Coverage} instances.</p>
     *
     * @see #getCoverage()
     * @see #setCoverage(GridCoverage)
     */
    public final ObjectProperty<GridCoverage> coverageProperty;

    /**
     * A subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     * May be {@code null} if the grid coverage has only two dimensions with a size greater than 1 cell.
     *
     * @see #getSliceExtent()
     * @see #setSliceExtent(GridExtent)
     * @see GridCoverage#render(GridExtent)
     */
    public final ObjectProperty<GridExtent> sliceExtentProperty;

    /**
     * The interpolation method to use for resampling the image.
     *
     * @see #getInterpolation()
     * @see #setInterpolation(Interpolation)
     */
    public final ObjectProperty<Interpolation> interpolationProperty;

    /**
     * The {@code RenderedImage} to draw together with transform from pixel coordinates to display coordinates.
     * Shall never be {@code null} but may be {@linkplain RenderingData#isEmpty() empty}. This instance shall be
     * read and modified in JavaFX thread only and cloned if those data are needed by a background thread.
     *
     * @see Worker
     */
    private RenderingData data;

    /**
     * The {@link #data} resampled to a CRS which can easily be mapped to {@linkplain #getDisplayCRS() display CRS}.
     * The different values are variants with color ramp changed.
     */
    private final Map<Stretching,RenderedImage> resampledImages;

    /**
     * The explorer to notify when the image shown in this canvas has changed.
     * This is non-null only if this {@link CoverageCanvas} is used together with {@link CoverageControls}.
     *
     * <p>Consider as final after {@link #createPropertyExplorer()} invocation.
     * This field may be removed in a future version if we revisit this API before making public.</p>
     *
     * @see #createPropertyExplorer()
     */
    private ImagePropertyExplorer imageProperty;

    /**
     * The status bar associated to this {@code MapCanvas}.
     * This is non-null only if this {@link CoverageCanvas} is used together with {@link CoverageControls}.
     */
    StatusBar statusBar;

    /**
     * Creates a new two-dimensional canvas for {@link RenderedImage}.
     */
    public CoverageCanvas() {
        super(Locale.getDefault());
        data                  = new RenderingData();
        resampledImages       = new EnumMap<>(Stretching.class);
        coverageProperty      = new SimpleObjectProperty<>(this, "coverage");
        sliceExtentProperty   = new SimpleObjectProperty<>(this, "sliceExtent");
        interpolationProperty = new SimpleObjectProperty<>(this, "interpolation", data.processor.getInterpolation());
        coverageProperty     .addListener((p,o,n) -> onImageSpecified());
        sliceExtentProperty  .addListener((p,o,n) -> onImageSpecified());
        interpolationProperty.addListener((p,o,n) -> onInterpolationSpecified(n));
    }

    /**
     * Completes initialization of this canvas for use with the returned property explorer.
     * The intent is to be notified when the image used for showing the coverage changed.
     * This method may be removed in a future SIS version if we revisit this API before
     * to make public.
     */
    final ImagePropertyExplorer createPropertyExplorer() {
        imageProperty = new ImagePropertyExplorer(getLocale(), fixedPane.backgroundProperty());
        imageProperty.setImage(resampledImages.get(data.selectedDerivative), getVisibleImageBounds());
        return imageProperty;
    }

    /**
     * Creates and register a contextual menu.
     *
     * @todo Consider moving to {@link org.apache.sis.gui.map.MapCanvas}.
     */
    final ObjectProperty<ReferenceSystem> createContextMenu(final RecentReferenceSystems referenceSystems) {
        final Resources resources = Resources.forLocale(getLocale());
        final MenuHandler handler = new MenuHandler();
        final Menu  systemChoices = referenceSystems.createMenuItems(handler);
        final Menu   localSystems = new Menu(resources.getString(Resources.Keys.CenteredProjection));
        for (final PositionableProjection projection : PositionableProjection.values()) {
            final RadioMenuItem item = new RadioMenuItem(projection.toString());
            item.setToggleGroup(handler.positionables);
            item.setOnAction((e) -> handler.createProjectedCRS(projection));
            localSystems.getItems().add(item);
        }
        handler.menu.getItems().setAll(systemChoices, localSystems);
        addPropertyChangeListener(OBJECTIVE_CRS_PROPERTY, handler);
        fixedPane.setOnMousePressed(handler);
        return handler.selectedProperty = RecentReferenceSystems.getSelectedProperty(systemChoices);
    }

    /**
     * Shows or hides the contextual menu when the right mouse button is clicked. This handler can determine
     * the geographic location where the click occurred. This information is used for changing the projection
     * while preserving approximately the location, scale and rotation of pixels around the mouse cursor.
     */
    @SuppressWarnings("serial")                                         // Not intended to be serialized.
    private final class MenuHandler extends DirectPosition2D
            implements EventHandler<MouseEvent>, ChangeListener<ReferenceSystem>, PropertyChangeListener
    {
        /**
         * The property to update if a change of CRS occurs in the enclosing canvas. This property is provided
         * by {@link RecentReferenceSystems}, which listen to changes. Setting this property to a new value
         * causes the "Referencing systems" radio menus to change the item where the check mark appear.
         *
         * <p>This field is initialized by {@link #createContextMenu(RecentReferenceSystems)} and should be
         * considered final after initialization.</p>
         */
        ObjectProperty<ReferenceSystem> selectedProperty;

        /**
         * The group of {@link PositionableProjection} items for projections created on-the-fly at mouse position.
         * Those items are not managed by {@link RecentReferenceSystems} so they need to be handled there.
         */
        final ToggleGroup positionables;

        /**
         * The contextual menu to show or hide when mouse button is clicked on the canvas.
         */
        final ContextMenu menu;

        /**
         * {@code true} if we are in the process of setting a CRS generated by {@link PositionableProjection}.
         */
        private boolean isPositionableProjection;

        /**
         * Creates a new handler for contextual menu in enclosing canvas.
         */
        MenuHandler() {
            super(getDisplayCRS());
            menu = new ContextMenu();
            positionables = new ToggleGroup();
        }

        /**
         * Invoked when the user click on the canvas.
         * Shows the menu on right mouse click, hide otherwise.
         */
        @Override
        public void handle(final MouseEvent event) {
            if (event.isSecondaryButtonDown()) {
                x = event.getX();
                y = event.getY();
                menu.show((Pane) event.getSource(), event.getScreenX(), event.getScreenY());
                event.consume();
            } else {
                menu.hide();
            }
        }

        /**
         * Invoked when user selected a new coordinate reference system among the choices of predefined CRS.
         * Those CRS are the ones managed by {@link RecentReferenceSystems}, not the ones created on-the-fly.
         */
        @Override
        public void changed(final ObservableValue<? extends ReferenceSystem> property,
                            final ReferenceSystem oldValue, final ReferenceSystem newValue)
        {
            if (newValue instanceof CoordinateReferenceSystem) {
                setObjectiveCRS((CoordinateReferenceSystem) newValue, this, property);
            }
        }

        /**
         * Invoked when user selected a projection centered on mouse position. Those CRS are generated on-the-fly
         * and are generally not on the list of CRS managed by {@link RecentReferenceSystems}.
         */
        final void createProjectedCRS(final PositionableProjection projection) {
            try {
                DirectPosition2D center = new DirectPosition2D();
                center = (DirectPosition2D) objectiveToDisplay.inverseTransform(this, center);
                center.setCoordinateReferenceSystem(getObjectiveCRS());
                CoordinateReferenceSystem crs = projection.createProjectedCRS(center);
                try {
                    isPositionableProjection = true;
                    setObjectiveCRS(crs, this, null);
                } finally {
                    isPositionableProjection = false;
                }
            } catch (NoninvertibleTransformException | FactoryException | TransformException e) {
                ExceptionReporter.show(null, null, e);
            }
        }

        /**
         * Invoked when a canvas property changed, typically after a new coverage has been selected.
         * The property of interest is {@value CoverageCanvas#OBJECTIVE_CRS_PROPERTY}.
         * This method updates the CRS selected in the contextual menu.
         */
        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            final Object value = event.getNewValue();
            if (value instanceof CoordinateReferenceSystem) {
                selectedProperty.set((CoordinateReferenceSystem) value);
            }
            if (!isPositionableProjection) {
                positionables.selectToggle(null);
            }
        }
    }

    /**
     * Returns the region containing the image view.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the region to show.
     */
    final Region getView() {
        return fixedPane;
    }

    /**
     * Returns the source of image for this viewer.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
     *
     * @return the coverage shown in this explorer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final GridCoverage getCoverage() {
        return coverageProperty.get();
    }

    /**
     * Sets the coverage to show in this viewer.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The new data are loaded in a background thread and will appear after an
     * undetermined amount of time.
     *
     * @param  coverage  the data to show in this viewer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final void setCoverage(final GridCoverage coverage) {
        assert Platform.isFxApplicationThread();
        coverageProperty.set(coverage);
    }

    /**
     * Returns a subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @return subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @see #sliceExtentProperty
     * @see GridCoverage#render(GridExtent)
     */
    public final GridExtent getSliceExtent() {
        return sliceExtentProperty.get();
    }

    /**
     * Sets a subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @param  sliceExtent  subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @see #sliceExtentProperty
     * @see GridCoverage#render(GridExtent)
     */
    public final void setSliceExtent(final GridExtent sliceExtent) {
        sliceExtentProperty.set(sliceExtent);
    }

    /**
     * Gets the interpolation method used during resample operations.
     *
     * @return the current interpolation method.
     *
     * @see #interpolationProperty
     */
    public final Interpolation getInterpolation() {
        return interpolationProperty.get();
    }

    /**
     * Sets the interpolation method to use during resample operations.
     *
     * @param interpolation the new interpolation method.
     *
     * @see #interpolationProperty
     */
    public final void setInterpolation(final Interpolation interpolation) {
        interpolationProperty.set(interpolation);
    }

    /**
     * Sets the background, as a color for now but more patterns may be allowed in a future version.
     */
    final void setBackground(final Color color) {
        fixedPane.setBackground(new Background(new BackgroundFill(color, null, null)));
    }

    /**
     * Invoked when a new coverage has been specified or when the slice extent changed.
     * This method fetches the image (which may imply data loading) in a background thread.
     */
    private void onImageSpecified() {
        final GridCoverage coverage = getCoverage();
        if (coverage == null) {
            clear();
        } else {
            final GridExtent sliceExtent = getSliceExtent();
            execute(new Task<RenderedImage>() {
                /**
                 * The coverage geometry reduced to two dimensions and with a translation taking in account
                 * the {@code sliceExtent}. That value will be stored in {@link CoverageCanvas#dataGeometry}.
                 */
                private GridGeometry imageGeometry;

                /**
                 * Invoked in a background thread for fetching the image and computing its geometry. The image
                 * geometry should be provided by {@value PlanarImage#GRID_GEOMETRY_KEY} property. But if that
                 * property is not provided, {@link ImageRenderer} is used as a fallback for computing it.
                 */
                @Override protected RenderedImage call() throws FactoryException {
                    final RenderedImage image = coverage.render(sliceExtent);
                    final Object value = image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
                    imageGeometry = (value instanceof GridGeometry) ? (GridGeometry) value
                                  : new ImageRenderer(coverage, sliceExtent).getImageGeometry(BIDIMENSIONAL);
                    return image;
                }

                /**
                 * Invoked when an error occurred while loading an image or processing it.
                 * This method popups the dialog box immediately because it is considered
                 * an important error.
                 */
                @Override protected void failed() {
                    final Throwable ex = getException();
                    errorOccurred(ex);
                    ExceptionReporter.canNotUseResource(ex);
                }

                /**
                 * Invoked in JavaFX thread for setting the image to the instance we just fetched.
                 */
                @Override protected void succeeded() {
                    setRawImage(getValue(), imageGeometry);
                }
            });
        }
    }

    /**
     * Invoked when a new image has been successfully loaded. The given image must be the "raw" image,
     * without resampling and without color ramp stretching. The call to this method is followed by a
     * a repaint event, which will cause the image to be resampled in a background thread.
     */
    private void setRawImage(final RenderedImage image, final GridGeometry imageGeometry) {
        resampledImages.clear();
        data.setImage(image, imageGeometry);
        Envelope bounds = null;
        if (imageGeometry != null && imageGeometry.isDefined(GridGeometry.ENVELOPE)) {
            bounds = imageGeometry.getEnvelope();
        }
        setObjectiveBounds(bounds);
        requestRepaint();                       // Cause `Worker` class to be executed.
    }

    /**
     * Invoked when a new interpolation has been specified.
     */
    private void onInterpolationSpecified(final Interpolation newValue) {
        data.processor.setInterpolation(newValue);
        resampledImages.clear();
        requestRepaint();
    }

    /**
     * Invoked in JavaFX thread for creating a renderer to be executed in a background thread.
     * This method prepares the information needed but does not start the rendering itself.
     * The rendering will be done later by a call to {@link Renderer#paint(Graphics2D)}.
     */
    @Override
    protected Renderer createRenderer() {
        return data.isEmpty() ? null : new Worker(this);
    }

    /**
     * Resample and paint image in the canvas. This class performs some or all of the following tasks, in order.
     * It is possible to skip the first tasks if they are already done, but after the work started at some point
     * all remaining points are executed:
     *
     * <ol>
     *   <li>Compute statistics on sample values (if needed).</li>
     *   <li>Resample the image (if needed).</li>
     *   <li>Paint the image.</li>
     * </ol>
     */
    private static final class Worker extends Renderer {
        /**
         * Value of {@link CoverageCanvas#data} at the time this worker has been initialized.
         */
        private final RenderingData data;

        /**
         * The coordinate reference system in which to reproject the data.
         */
        private final CoordinateReferenceSystem objectiveCRS;

        /**
         * The conversion from {@link #objectiveCRS} to the canvas display CRS.
         */
        private final LinearTransform objectiveToDisplay;

        /**
         * The source image after resampling.
         */
        private RenderedImage resampledImage;

        /**
         * The resampled image after color ramp stretching or other operation applied.
         */
        private RenderedImage filteredImage;

        /**
         * The filtered image with tiles computed in advance. The set of prefetched
         * tiles may differ at each rendering event. This image should not be cached
         * after rendering operation is completed.
         */
        private RenderedImage prefetchedImage;

        /**
         * Conversion from {@link #resampledImage} (also {@link #prefetchedImage})
         * pixel coordinates to display coordinates.
         */
        private AffineTransform resampledToDisplay;

        /**
         * Size and location of the display device, in pixel units.
         */
        private final Envelope2D displayBounds;

        /**
         * Creates a new renderer.
         */
        Worker(final CoverageCanvas canvas) {
            data               = canvas.data.clone();
            objectiveCRS       = canvas.getObjectiveCRS();
            objectiveToDisplay = canvas.getObjectiveToDisplay();
            displayBounds      = canvas.getDisplayBounds();
            if (data.validateCRS(objectiveCRS)) {
                resampledImage = canvas.resampledImages.get(Stretching.NONE);
                filteredImage  = canvas.resampledImages.get(data.selectedDerivative);
            }
        }

        /**
         * Returns the bounds of the image part which is currently shown. This method can be invoked
         * only after {@link #render()}. It returns {@code null} if the visible bounds are unknown.
         *
         * @see CoverageCanvas#getVisibleImageBounds()
         */
        final Rectangle getVisibleImageBounds() {
            try {
                return (Rectangle) AffineTransforms2D.inverseTransform(resampledToDisplay, displayBounds, new Rectangle());
            } catch (NoninvertibleTransformException e) {
                unexpectedException(e);                     // Should never happen.
            }
            return null;
        }

        /**
         * Invoked in background thread for resampling the image or stretching the color ramp.
         * This method performs some of the steps documented in class Javadoc, with possibility
         * to skip the first step if the required source image is already resampled.
         */
        @Override
        @SuppressWarnings("PointlessBitwiseExpression")
        protected void render() throws TransformException {
            boolean isResampled = (resampledImage != null);
            if (isResampled) {
                resampledToDisplay = data.getTransform(objectiveToDisplay);
                // Recompute if anything else than identity or translation.
                isResampled = (resampledToDisplay.getType()
                        & ~(AffineTransform.TYPE_IDENTITY | AffineTransform.TYPE_TRANSLATION)) == 0;
            }
            if (!isResampled) {
                filteredImage = null;
                resampledImage = data.resample(objectiveCRS, objectiveToDisplay);
                resampledToDisplay = data.getTransform(objectiveToDisplay);
            }
            if (filteredImage == null) {
                filteredImage = data.filter(resampledImage, displayBounds);
            }
            prefetchedImage = data.prefetch(filteredImage, resampledToDisplay, displayBounds);
        }

        /**
         * Draws the image in a background buffer after {@link #render()} finished to prepare data.
         */
        @Override
        protected void paint(final Graphics2D gr) {
            gr.drawRenderedImage(prefetchedImage, resampledToDisplay);
        }

        /**
         * Invoked in JavaFX thread after successful {@link #paint(Graphics2D)} completion.
         * This method stores the computation results.
         */
        @Override
        protected boolean commit(final MapCanvas canvas) {
            ((CoverageCanvas) canvas).cacheRenderingData(this);
            return super.commit(canvas);
        }
    }

    /**
     * Invoked after a paint event for caching rendering data.
     * If the resampled image changed, all previously cached images are discarded.
     */
    private void cacheRenderingData(final Worker worker) {
        data = worker.data;
        final RenderedImage newValue = worker.resampledImage;
        final RenderedImage oldValue = resampledImages.put(Stretching.NONE, newValue);
        if (oldValue != newValue && oldValue != null) {
            /*
             * If resampled image changed, then all derivative images (with stretched color ramp
             * or other operation applied) are not valid anymore. We need to empty the cache.
             */
            resampledImages.clear();
            resampledImages.put(Stretching.NONE, newValue);
        }
        resampledImages.put(data.selectedDerivative, worker.filteredImage);
        /*
         * Notify the "Image properties" tab that the image changed.
         */
        if (imageProperty != null) {
            imageProperty.setImage(worker.filteredImage, worker.getVisibleImageBounds());
        }
        if (statusBar != null) {
            final Object value = worker.filteredImage.getProperty(PlanarImage.POSITIONAL_ACCURACY_KEY);
            Quantity<Length> accuracy = null;
            if (value instanceof Quantity<?>[]) {
                for (final Quantity<?> q : (Quantity<?>[]) value) {
                    if (Units.isLinear(q.getUnit())) {
                        accuracy = q.asType(Length.class);
                        accuracy = GUIUtilities.shorter(accuracy, accuracy.getUnit().getConverterTo(Units.METRE)
                                                                    .convert(accuracy.getValue().doubleValue()));
                        break;
                    }
                }
            }
            statusBar.setLowestAccuracy(accuracy);
        }
    }

    /**
     * Returns the bounds of the image part which is currently shown. This method performs the same work
     * than {@link Worker#getVisibleImageBounds()} is a less efficient way. It is used when no worker is
     * available.
     *
     * @see Worker#getVisibleImageBounds()
     */
    private Rectangle getVisibleImageBounds() {
        final Envelope2D displayBounds = getDisplayBounds();
        final AffineTransform resampledToDisplay = data.getTransform(getObjectiveToDisplay());
        try {
            return (Rectangle) AffineTransforms2D.inverseTransform(resampledToDisplay, displayBounds, new Rectangle());
        } catch (NoninvertibleTransformException e) {
            unexpectedException(e);                     // Should never happen.
        }
        return null;
    }

    /**
     * Invoked by {@link CoverageControls} when the user selected a new color stretching mode.
     * The sample values are assumed the same; only the image appearance is modified.
     */
    final void setStyling(final Stretching selection) {
        if (data.selectedDerivative != selection) {
            data.selectedDerivative = selection;
            requestRepaint();
        }
    }

    /**
     * Invoked when the user changed the CRS from a JavaFX control. If the CRS can not be set to the specified
     * value, then an error message is shown in the status bar and the property is reset to its previous value.
     *
     * @param  crs       the new Coordinate Reference System in which to transform all data before displaying.
     * @param  anchor    the point to keep at fixed display coordinates, or {@code null} for default value.
     * @param  property  the property to reset if the operation fails.
     */
    private void setObjectiveCRS(final CoordinateReferenceSystem crs, DirectPosition anchor,
                                 final ObservableValue<? extends ReferenceSystem> property)
    {
        final CoordinateReferenceSystem previous = getObjectiveCRS();
        if (crs != previous) try {
            if (anchor == null) {
                final Envelope2D bounds = getDisplayBounds();
                if (bounds != null) {
                    anchor = AbstractEnvelope.castOrCopy(bounds).getMedian();
                }
            }
            setObjectiveCRS(crs, anchor);
            requestRepaint();
        } catch (Exception e) {
            if (property instanceof WritableValue<?>) {
                ((WritableValue<ReferenceSystem>) property).setValue(previous);
            }
            errorOccurred(e);
            final Locale locale = getLocale();
            final Resources i18n = Resources.forLocale(locale);
            ExceptionReporter.show(null, i18n.getString(Resources.Keys.CanNotUseRefSys_1,
                                   IdentifiedObjects.getDisplayName(crs, locale)), e);
        }
    }

    /**
     * Invoked when an exception occurred while computing a transform but the painting process can continue.
     */
    private static void unexpectedException(final Exception e) {
        Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), CoverageCanvas.class, "render", e);
    }

    /**
     * Removes the image shown and releases memory.
     */
    @Override
    protected void clear() {
        setRawImage(null, null);
        super.clear();
    }
}
