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
import java.nio.IntBuffer;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ImageRenderings;
import org.apache.sis.internal.map.PlanarCanvas;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Shows a {@link RenderedImage} produced by a {@link GridCoverage}.
 *
 * This class should not be put in public API yet.
 * It may be refactored to a {@code MapView} after we have a renderer in SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CoverageView extends PlanarCanvas {
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
     * A buffer where to draw the {@link RenderedImage} for the region to be displayed.
     * This buffer uses ARGB color model, contrarily to {@link #data} which may have any
     * color model. This buffered image will contain only the visible region of the data;
     * it may be a zoom over a small region.
     */
    private BufferedImage buffer;

    /**
     * A temporary buffer where to draw the {@link RenderedImage} in a background thread.
     * We use this double-buffering when the {@link #buffer} is already wrapped by JavaFX.
     * After creating the image in background, its content is copied to {@link #buffer} in
     * JavaFX thread.
     */
    private VolatileImage doubleBuffer;

    /**
     * The graphic configuration at the time {@link #buffer} has been rendered.
     * This will be used for creating compatible {@link VolatileImage} for updating.
     */
    private GraphicsConfiguration bufferConfiguration;

    /**
     * Wraps {@link #buffer} data array for use by JavaFX images. This is the mechanism used
     * by JavaFX 13+ for allowing {@link #image} to share the same data than {@link #buffer}.
     * The same wrapper can be used for many {@link WritableImage} instances (e.g. thumbnails).
     */
    private PixelBuffer<IntBuffer> bufferWrapper;

    /**
     * The node where the image will be shown. The image of this view contains the same
     * data than {@link #buffer}. They will share the same data array (no copy) and the
     * same coordinate system.
     */
    private final ImageView image;

    /**
     * The pane where to put children. This pane uses absolute layout. It contains at least the
     * {@linkplain #image} to show, but can also contain additional nodes for geometric shapes,
     * texts, <i>etc</i>.
     */
    private final Pane imageRegion;

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
     * Incremented when {@link #data} or {@link #dataToImage} changed.
     *
     * @see #renderedDataStamp
     * @see #isDataChanged()
     */
    private int dataChangeCount;

    /**
     * Value of {@link #dataChangeCount} last time the data have been rendered. This is used for deciding
     * if a call to {@link #repaint()} should be done with the next layout operation. We need this check
     * for avoiding never-ending repaint events caused by calls to {@link ImageView#setImage(Image)}
     * causing themselves new layout events. It is okay if this value overflows.
     */
    private int renderedDataStamp;

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
        imageRegion = new Pane() {
            @Override protected void layoutChildren() {
                super.layoutChildren();
                if (isDataChanged()) {
                    repaint();
                }
            }
        };
        image = new ImageView();
        image.setPreserveRatio(true);
        imageRegion.getChildren().add(image);
        imageAndStatus = new BorderPane(imageRegion);
        statusBar = new StatusBar(this::toImageCoordinates);
        imageAndStatus.setBottom(statusBar);
        /*
         * Do not set a preferred size, otherwise `repaint()` is invoked twice: once with the preferred size
         * and once with the actual size of the parent window. Actually the `repaint()` method appears to be
         * invoked twice anyway, but without preferred size the width appears to be 0, in which case nothing
         * is repainted.
         */
        coverageProperty   .addListener(this::onImageSpecified);
        sliceExtentProperty.addListener(this::onImageSpecified);
        imageRegion.setOnMouseMoved(this::onMouveMoved);
        imageRegion.setOnMouseEntered(statusBar);
        imageRegion.setOnMouseExited (statusBar);
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
     * <p>Tasks need to be careful to not use any {@link CoverageView} field in their {@link Task#call()} method
     * (needed fields shall be copied in the JavaFX thread before the background thread is started).
     * But {@link Task#succeeded()} and similar methods can read and write those fields.</p>
     */
    private void execute(final Task<?> task) {
        statusBar.setErrorMessage(null);
        task.runningProperty().addListener(statusBar::setRunningState);
        task.setOnFailed((e) -> errorOccurred(e.getSource().getException()));
        BackgroundThreads.execute(task);
    }

    /**
     * Invoked when a new coverage has been specified or when the slice extent changed.
     *
     * @param  property  the {@link #coverageProperty} or {@link #sliceExtentProperty} (ignored).
     * @param  previous  ignored.
     * @param  value     ignored.
     */
    private void onImageSpecified(final ObservableValue<?> property, final Object previous, final Object value) {
        image.setImage(null);
        data = null;
        dataAlternatives.clear();
        final GridCoverage coverage = getCoverage();
        if (coverage == null) {
            buffer        = null;           // Free memory.
            bufferWrapper = null;
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
            dataChangeCount++;
            imageRegion.requestLayout();
        }
    }

    /**
     * Sets the background, as a color for now but more patterns my be allowed in a future version.
     */
    final void setBackground(final Color color) {
        imageRegion.setBackground(new Background(new BackgroundFill(color, null, null)));
    }

    /**
     * Returns {@code true} if data changed since the last {@link #repaint()} execution.
     */
    private boolean isDataChanged() {
        return dataChangeCount != renderedDataStamp;
    }

    /**
     * Invoked when the {@link #data} content needs to be rendered again into {@link #image}.
     * It may be because a new image has been specified, or because the viewed region moved
     * or have been zoomed.
     */
    private void repaint() {
        renderedDataStamp = dataChangeCount;
        final RenderedImage data = this.data;       // Need to copy this reference here before background tasks.
        if (data == null) {
            return;
        }
        final int width  = Numerics.clamp(Math.round(imageRegion.getWidth()));
        final int height = Numerics.clamp(Math.round(imageRegion.getHeight()));
        if (width <= 0 || height <= 0) {
            return;
        }
        /*
         * There is two possible situations: if the current buffers are not suitable, we clear everything related
         * to Java2D buffered images and will recreate everything from scratch in the background thread. There is
         * no need for double-buffering in such case since the new `BufferedImage` will not be shared with JavaFX
         * image before the end of this task.
         *
         * The second situation is if the buffers are still valid. In such case we should not update the BufferedImage
         * in a background thread because the internal array of that image is shared with JavaFX image, and that image
         * should be updated only in JavaFX thread through the `PixelBuffer.update(…)` method. For that second case we
         * will use a `VolatileImage` as a temporary buffer.
         *
         * In both cases we need to be careful to not use directly any `CoverageView` field from the `call()` method.
         * Information needed by `call()` must be copied first. This is the case of `dataToImage` below among others.
         */
        final AffineTransform dataToImage = new AffineTransform(this.dataToImage);
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            buffer              = null;
            doubleBuffer        = null;
            bufferWrapper       = null;
            bufferConfiguration = null;
            execute(new Task<WritableImage>() {
                /**
                 * The Java2D image where to do the rendering. This image will be created in a background thread
                 * and assigned to the {@link CoverageView#buffer} field in JavaFX thread if rendering succeed.
                 */
                private BufferedImage drawTo;

                /**
                 * Wrapper around {@link #buffer} internal array for interoperability between Java2D and JavaFX.
                 * Created only if {@link #drawTo} have been successfully painted.
                 */
                private PixelBuffer<IntBuffer> wrapper;

                /**
                 * The graphic configuration at the time {@link #drawTo} has been rendered.
                 * This will be used for creating {@link VolatileImage} when updating the image.
                 */
                private GraphicsConfiguration configuration;

                /**
                 * Invoked in background thread for creating and rendering the image (may be slow).
                 * Any {@link CoverageView} field needed by this method shall be copied before the
                 * background thread is executed; no direct reference to {@link CoverageView} here.
                 */
                @Override
                protected WritableImage call() {
                    drawTo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
                    final Graphics2D gr = drawTo.createGraphics();
                    configuration = gr.getDeviceConfiguration();
                    try {
                        gr.drawRenderedImage(data, dataToImage);
                    } finally {
                        gr.dispose();
                    }
                    /*
                     * The call to `array.getData()` below should be after we finished drawing in the new
                     * BufferedImage, because this direct access to data array disables GPU accelerations.
                     */
                    final DataBufferInt array = (DataBufferInt) drawTo.getRaster().getDataBuffer();
                    IntBuffer ib = IntBuffer.wrap(array.getData(), array.getOffset(), array.getSize());
                    wrapper = new PixelBuffer<>(width, height, ib, PixelFormat.getIntArgbPreInstance());
                    return new WritableImage(wrapper);
                }

                /**
                 * Invoked in JavaFX thread on success. The JavaFX image is set to the result, then intermediate
                 * buffers created by this task are saved in {@link CoverageView} fields for reuse next time that
                 * an image of the same size will be rendered again.
                 */
                @Override
                protected void succeeded() {
                    super.succeeded();
                    image.setImage(getValue());
                    buffer              = drawTo;
                    bufferWrapper       = wrapper;
                    bufferConfiguration = configuration;
                }
            });
        } else {
            /*
             * This is the second case described in the block comment at the beginning of this method:
             * The existing resources (JavaFX image and Java2D volatile/buffered image) can be reused.
             * The Java2D volatile image will be rendered in background thread, then its content will
             * be transferred to JavaFX image (through BufferedImage shared array) in JavaFX thread.
             */
            final VolatileImage         previousBuffer = doubleBuffer;
            final GraphicsConfiguration configuration  = bufferConfiguration;
            final class Updater extends Task<VolatileImage> implements Callback<PixelBuffer<IntBuffer>, Rectangle2D> {
                /**
                 * Invoked in background thread for rendering the image (may be slow).
                 * Any {@link CoverageView} field needed by this method shall be copied before the
                 * background thread is executed; no direct reference to {@link CoverageView} here.
                 */
                @Override
                protected VolatileImage call() {
                    VolatileImage drawTo = previousBuffer;
                    if (drawTo == null) {
                        drawTo = configuration.createCompatibleVolatileImage(width, height);
                    }
                    boolean invalid = true;
                    try {
                        do {
                            if (drawTo.validate(configuration) == VolatileImage.IMAGE_INCOMPATIBLE) {
                                drawTo = configuration.createCompatibleVolatileImage(width, height);
                            }
                            final Graphics2D gr = drawTo.createGraphics();
                            try {
                                gr.setBackground(ColorModelFactory.TRANSPARENT);
                                gr.clearRect(0, 0, drawTo.getWidth(), drawTo.getHeight());
                                gr.drawRenderedImage(data, dataToImage);
                            } finally {
                                gr.dispose();
                            }
                            invalid = drawTo.contentsLost();
                        } while (invalid && !isCancelled());
                    } finally {
                        if (invalid) {
                            drawTo.flush();         // Release native resources.
                        }
                    }
                    return drawTo;
                }

                /**
                 * Invoked in JavaFX thread on success. The JavaFX image is set to the result, then the
                 * double buffer created by this task is saved in {@link CoverageView} fields for reuse
                 * next time that an image of the same size will be rendered again.
                 */
                @Override
                protected void succeeded() {
                    final VolatileImage drawTo = getValue();
                    doubleBuffer = drawTo;
                    try {
                        bufferWrapper.updateBuffer(this);       // This will invoke the `call(…)` method below.
                    } finally {
                        drawTo.flush();
                    }
                    super.succeeded();
                }

                /**
                 * Invoked by {@link PixelBuffer#updateBuffer(Callback)} for updating the {@link #buffer} content.
                 */
                @Override
                public Rectangle2D call(final PixelBuffer<IntBuffer> wrapper) {
                    final VolatileImage drawTo = doubleBuffer;
                    final Graphics2D gr = buffer.createGraphics();
                    final boolean contentsLost;
                    try {
                        gr.drawImage(drawTo, 0, 0, null);
                        contentsLost = drawTo.contentsLost();
                    } finally {
                        gr.dispose();
                    }
                    if (contentsLost) {
                        repaint();
                    }
                    return null;
                }
            }
            execute(new Updater());
        }
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
