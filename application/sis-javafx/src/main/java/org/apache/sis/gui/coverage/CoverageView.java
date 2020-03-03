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

import java.util.Locale;
import java.util.EnumMap;
import java.util.Objects;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.input.MouseEvent;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.gui.ImageRenderings;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.gui.map.MapCanvas;


/**
 * Shows a {@link RenderedImage} produced by a {@link GridCoverage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CoverageView extends MapCanvas {
    /**
     * The data shown in this view. Note that setting this property to a non-null value may not
     * modify the view content immediately. Instead, a background process will request the tiles.
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
     * May be {@code null} if this grid coverage has only two dimensions with a size greater than 1 cell.
     *
     * @see #getSliceExtent()
     * @see #setSliceExtent(GridExtent)
     * @see GridCoverage#render(GridExtent)
     */
    public final ObjectProperty<GridExtent> sliceExtentProperty;

    /**
     * Different ways to represent the data. The {@link #data} field shall be one value from this map.
     *
     * @see #setImage(RangeType, RenderedImage)
     */
    private final EnumMap<RangeType,RenderedImage> dataAlternatives;

    /**
     * Key of the currently selected alternative in {@link #dataAlternatives} map.
     *
     * @see #setImage(RangeType, RenderedImage)
     */
    private RangeType currentDataAlternative;

    /**
     * The data to shown, or {@code null} if not yet specified. This image may be tiled,
     * and fetching tiles may require computations to be performed in background thread.
     * The size of this image is not necessarily {@link #buffer} or {@link #image} size.
     * In particular this image way cover a larger area.
     */
    private RenderedImage data;

    /**
     * The image together with the status bar.
     */
    private final BorderPane imageAndStatus;

    /**
     * The transform from {@link #data} pixel coordinates to {@link #buffer} (and {@link #image})
     * pixel coordinates. This is the concatenation of {@link GridGeometry#getGridToCRS(PixelInCell)}
     * followed by {@link #getObjectiveToDisplay()}. This transform is updated when the zoom changes
     * or when the viewed area is translated.
     */
    private final AffineTransform dataToImage;

    /**
     * The bar where to format the coordinates below mouse cursor.
     */
    private final StatusBar statusBar;

    /**
     * Creates a new two-dimensional canvas for {@link RenderedImage}.
     */
    public CoverageView() {
        super(Locale.getDefault());
        coverageProperty       = new SimpleObjectProperty<>(this, "coverage");
        sliceExtentProperty    = new SimpleObjectProperty<>(this, "sliceExtent");
        dataAlternatives       = new EnumMap<>(RangeType.class);
        dataToImage            = new AffineTransform();
        currentDataAlternative = RangeType.DECLARED;
        statusBar              = new StatusBar(this::toImageCoordinates);
        imageAndStatus         = new BorderPane(view);
        imageAndStatus.setBottom(statusBar);
        coverageProperty   .addListener(this::onImageSpecified);
        sliceExtentProperty.addListener(this::onImageSpecified);
        view.setOnMouseMoved(this::onMouveMoved);
        view.setOnMouseEntered(statusBar);
        view.setOnMouseExited (statusBar);
    }

    /**
     * Returns the data which are the source of all alternative images that may be stored in the
     * {@link #dataAlternatives} map. All alternative images are computed from this source.
     */
    private RenderedImage getSourceData() {
        return dataAlternatives.get(RangeType.DECLARED);
    }

    /**
     * Returns the region containing the image view.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the region to show.
     */
    public final Region getView() {
        return imageAndStatus;
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
     * Starts a background task for loading data, computing slice or rendering the data in a {@link CoverageView}.
     *
     * <p>Tasks need to be careful to not use any {@link CoverageView} field in their {@link Task#call()}
     * method (needed fields shall be copied in the JavaFX thread before the background thread is started).
     * But {@link Task#succeeded()} and similar methods can read and write those fields.</p>
     */
    @Override
    protected final void execute(final Task<?> task) {
        statusBar.setErrorMessage(null);
        task.runningProperty().addListener(statusBar::setRunningState);
        task.setOnFailed((e) -> errorOccurred(e.getSource().getException()));
        super.execute(task);
    }

    /**
     * Invoked when a new coverage has been specified or when the slice extent changed.
     *
     * @param  property  the {@link #coverageProperty} or {@link #sliceExtentProperty} (ignored).
     * @param  previous  ignored.
     * @param  value     ignored.
     */
    private void onImageSpecified(final ObservableValue<?> property, final Object previous, final Object value) {
        data = null;
        dataAlternatives.clear();
        final GridCoverage coverage = getCoverage();
        if (coverage == null) {
            clear();
        } else {
            final GridExtent sliceExtent = getSliceExtent();
            statusBar.setCoordinateConversion(coverage.getGridGeometry(), sliceExtent);
            execute(new Task<RenderedImage>() {
                /** Invoked in background thread for fetching the image. */
                @Override protected RenderedImage call() {
                    return coverage.render(sliceExtent);
                }

                /** Invoked in JavaFX thread on success. */
                @Override protected void succeeded() {
                    super.succeeded();
                    if (coverage.equals(getCoverage()) && Objects.equals(sliceExtent, getSliceExtent())) {
                        setImage(RangeType.DECLARED, getValue());
                        setRangeType(currentDataAlternative);
                    }
                }
            });
        }
    }

    /**
     * Invoked when the user selected a new range of values to scale. Also invoked {@linkplain #onImageSpecified after
     * loading a new image or a new slice} for switching the new image to the same type of range as previously selected.
     * If the image for the specified type is not already available, then this method computes the image in a background
     * thread and refreshes the view after the computation completed.
     */
    final void setRangeType(final RangeType rangeType) {
        currentDataAlternative = rangeType;
        final RenderedImage alt = dataAlternatives.get(rangeType);
        if (alt != null) {
            setImage(rangeType, alt);
        } else {
            final RenderedImage source = getSourceData();
            if (source != null) {
                execute(new Task<RenderedImage>() {
                    /** Invoked in background thread for fetching the image. */
                    @Override protected RenderedImage call() {
                        switch (rangeType) {
                            case AUTOMATIC: return ImageRenderings.automaticScale(source);
                            default:        return source;
                        }
                    }

                    /** Invoked in JavaFX thread on success. */
                    @Override protected void succeeded() {
                        super.succeeded();
                        if (source.equals(getSourceData())) {
                            setImage(rangeType, getValue());
                        }
                    }
                });
            }
        }
    }

    /**
     * Invoked in JavaFX thread for setting the image to show. The given image should be a slice
     * produced by current value of {@link #coverageProperty} (should be verified by the caller).
     *
     * @param  type  the type of range used for scaling the color ramp of given image.
     * @param  alt   the image or alternative image to show (can be {@code null}).
     */
    private void setImage(final RangeType type, RenderedImage alt) {
        /*
         * Store the result but do not necessarily show it because maybe the user changed the
         * `RangeType` during the time the background thread was working. If the user did not
         * changed the type, then the `alt` variable below will stay unchanged.
         */
        dataAlternatives.put(type, alt);
        alt = dataAlternatives.get(currentDataAlternative);
        if (!Objects.equals(alt, data)) {
            data = alt;
            requestRepaint();
        }
    }

    /**
     * Sets the background, as a color for now but more patterns my be allowed in a future version.
     */
    final void setBackground(final Color color) {
        view.setBackground(new Background(new BackgroundFill(color, null, null)));
    }

    /**
     * Invoked in JavaFX thread for creating a renderer to be executed in a background thread.
     */
    @Override
    protected Renderer createRenderer(){
        final RenderedImage data = this.data;       // Need to copy this reference here before background tasks.
        if (data == null) {
            return null;
        }
        final AffineTransform dataToImage = new AffineTransform(this.dataToImage);
        return new Renderer() {
            @Override protected void paint(final Graphics2D gr) {
                gr.drawRenderedImage(data, dataToImage);
            }
        };
    }

    /**
     * Invoked when an error occurred.
     *
     * @param  ex  the exception that occurred.
     *
     * @todo Should provide a button for getting more details.
     */
    private void errorOccurred(final Throwable ex) {
        String message = ex.getMessage();
        if (message == null) {
            message = ex.toString();
        }
        statusBar.setErrorMessage(message);
    }

    /**
     * Invoked when the mouse moved. This method update the coordinates below mouse cursor.
     */
    private void onMouveMoved(final MouseEvent event) {
        statusBar.setCoordinates((int) Math.round(event.getX()),
                                 (int) Math.round(event.getY()));
    }

    /**
     * Converts pixel indices in the window to pixel indices in the image.
     */
    private void toImageCoordinates(final double[] indices) {
        try {
            dataToImage.inverseTransform(indices, 0, indices, 0, 1);
        } catch (NoninvertibleTransformException e) {
            throw new BackingStoreException(e);         // Will be unwrapped by the caller
        }
    }
}
