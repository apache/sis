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
import java.nio.IntBuffer;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Callback;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.map.PlanarCanvas;
import org.apache.sis.internal.util.Numerics;


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
    private final Pane view;

    /**
     * The transform from {@link #data} pixel coordinates to {@link #buffer} (and {@link #image})
     * pixel coordinates. This is the concatenation of {@link GridGeometry#getGridToCRS(PixelInCell)}
     * followed by {@link #getObjectiveToDisplay()}. This transform is updated when the zoom changes
     * or when the viewed area is translated.
     */
    private final AffineTransform dataToImage;

    /**
     * Creates a new two-dimensional canvas for {@link RenderedImage}.
     *
     * @param  locale  the locale to use for labels and some messages, or {@code null} for default.
     */
    public CoverageView(final Locale locale) {
        super(locale);
        coverageProperty    = new SimpleObjectProperty<>(this, "coverage");
        sliceExtentProperty = new SimpleObjectProperty<>(this, "sliceExtent");
        dataToImage = new AffineTransform();
        view = new Pane() {
            @Override protected void layoutChildren() {
                super.layoutChildren();
                repaint();
            }
        };
        image = new ImageView();
        image.setPreserveRatio(true);
        view.getChildren().add(image);
        /*
         * Do not set a preferred size, otherwise `repaint()` is invoked twice: once with the preferred size
         * and once with the actual size of the parent window. Actually the `repaint()` method appears to be
         * invoked twice anyway, but without preferred size the width appears to be 0, in which case nothing
         * is repainted.
         */
        coverageProperty   .addListener(this::onImageSpecified);
        sliceExtentProperty.addListener(this::onImageSpecified);
    }

    /**
     * Returns the region containing the image view.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the region to show.
     */
    public final Region getView() {
        return view;
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
     * Invoked when a new coverage has been specified or when the slice extent changed.
     *
     * @param  property  the {@link #coverageProperty} or {@link #sliceExtentProperty} (ignored).
     * @param  previous  ignored.
     * @param  value     ignored.
     */
    private void onImageSpecified(final ObservableValue<?> property, final Object previous, final Object value) {
        image.setImage(null);
        data   = null;
        buffer = null;
        final GridCoverage coverage = getCoverage();
        if (coverage != null) {
            data = coverage.render(getSliceExtent());     // TODO: background thread.
            repaint();
        }
    }

    /**
     * Invoked when the {@link #data} content needs to be rendered again into {@link #image}.
     * It may be because a new image has been specified, or because the viewed region moved
     * or have been zoomed.
     */
    private void repaint() {
        final int width  = Numerics.clamp(Math.round(view.getWidth()));
        final int height = Numerics.clamp(Math.round(view.getHeight()));
        if (width <= 0 || height <= 0) {
            return;
        }
        PixelBuffer<IntBuffer> wrapper = bufferWrapper;
        BufferedImage drawTo = buffer;
        if (drawTo == null || drawTo.getWidth() != width || drawTo.getHeight() != height) {
            /*
             * TODO: run in background.
             */
            drawTo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            final Graphics2D gr = drawTo.createGraphics();
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
            final WritableImage target = new WritableImage(wrapper);
            image.setImage(target);
            bufferWrapper = wrapper;
            buffer = drawTo;
        } else {
            /*
             * Reuse existing resources (JavaFX image and Java2D buffered image).
             *
             * TODO: start a background task instead of invoking `updateBuffer` now.
             */
            wrapper.updateBuffer(new Updater(drawTo));
        }
    }

    /**
     * The task for updating an image be reusing existing resources (JavaFX image and Java2D buffered image).
     * This task is used when the image size did not changed.
     *
     * @todo Extend {@code Task}, write in a background thread in a {@link VolatileImage} (may be long especially
     *       if {@link RenderedImage} tiles are computed on-the-fly or if the color model is not natively supported),
     *       copy the data to {@link BufferedImage} in the JavaFX thread.
     */
    private final class Updater implements Callback<PixelBuffer<IntBuffer>, Rectangle2D> {
        /**
         * The Java2D image which is sharing data with the JavaFX image.
         * This image needs to be updated in a call to {@link PixelBuffer#updateBuffer(Callback)}.
         */
        private final BufferedImage buffer;

        /**
         * Creates a new updater.
         */
        Updater(final BufferedImage buffer) {
            this.buffer = buffer;
        }

        /**
         * Invoked by {@link PixelBuffer#updateBuffer(Callback)} for updating the {@link #buffer} content.
         *
         * @todo We should render {@link #data} in this method since it may be costly.
         */
        @Override
        public Rectangle2D call(final PixelBuffer<IntBuffer> wrapper) {
            final Graphics2D gr = buffer.createGraphics();
            try {
                gr.setBackground(ColorModelFactory.TRANSPARENT);
                gr.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());
                gr.drawRenderedImage(data, dataToImage);
            } finally {
                gr.dispose();
            }
            return null;
        }
    }
}
