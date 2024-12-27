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
package org.apache.sis.gui.internal;

import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import javafx.concurrent.Task;
import javafx.scene.layout.Region;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.apache.sis.image.Colorizer;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.map.coverage.RenderingWorkaround;
import org.apache.sis.image.privy.ColorModelFactory;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.Statistics;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * Converts a Java2D image to a JavaFX image, then writes the result in a given {@link ImageView}.
 * This task should be used only for small images (thumbnail) because some potentially costly resources
 * are created each time.
 *
 * <p>The {@link ImageView} should not be modified by caller. Instead, the {@link #clear(ImageView)}
 * method should be invoked for setting the image to null, because this class also manages properties
 * associated to the {@link ImageView}.</p>
 *
 * <p>Current implementation returns statistics on sample values as a side-product.
 * The statistics may be null if they were not computed.
 * This policy may change in any future version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ImageConverter extends Task<Statistics[]> {
    /**
     * Colors to apply on the mask image when that image is overlay on top of another image.
     * Current value is a transparent yellow color.
     */
    private static final Colorizer MASK_TRANSPARENCY = Colorizer.forRanges(Map.of(
            NumberRange.create(0, true, 0, true), new Color[] {ColorModelFactory.TRANSPARENT},
            NumberRange.create(1, true, 1, true), new Color[] {new Color(0x30FFFF00, true)}));

    /**
     * The Java2D image to convert.
     */
    private final RenderedImage source;

    /**
     * Pixel coordinates of the region to render, or {@code null} for the whole image.
     */
    private Rectangle sourceAOI;

    /**
     * On input, the canvas size. On output, the actual image size rendered in the canvas.
     * One of the width or height may be modified on output for preserving image proportions.
     */
    private int width, height;

    /**
     * Value to give to {@link ImageView} x/y properties after the image has been created.
     */
    private int xpos, ypos;

    /**
     * Where to write the image. This will be updated in JavaFX thread.
     */
    private final ImageView view;

    /**
     * The ARGB values to be copied in the JavaFX image.
     */
    private int[] data;

    /**
     * Creates a new task for converting the given image.
     */
    ImageConverter(final RenderedImage source, final Rectangle sourceAOI, final ImageView view, final Region canvas) {
        this.source    = source;
        this.sourceAOI = sourceAOI;
        this.view      = view;
        this.width     = Numerics.clamp(Math.round(canvas.getWidth()));
        this.height    = Numerics.clamp(Math.round(canvas.getHeight()));
    }

    /**
     * Sets the image property to {@code null} on the given view.
     * This method also clears other properties managed by {@link ImageConverter}.
     *
     * @param  view  the view on which to set the image property to {@code null}.
     */
    public static void clear(final ImageView view) {
        view.setImage(null);
        view.setUserData(null);
    }

    /**
     * Returns {@code true} if the {@link #call()} method needs to be invoked in a background thread.
     * This method may return {@code false} because the canvas size is zero, or because current content can be reused.
     * The content can be reused if the canvas size did not changed, or changed in a way that does not require repaint
     * (i.e. if the size that changed is the size that was already too wide for fitting the image inside the canvas).
     *
     * @param  newAOI  whether {@link #sourceAOI} had a different value during previous rendering.
     * @return whether this task needs to be run.
     */
    public boolean needsRun(final boolean newAOI) {
        if (width <= 0 || height <= 0) {
            return false;
        }
        if (!newAOI && view.getUserData() == source) {
            final Image image = view.getImage();
            final double dx = width  - image.getWidth();
            final double dy = height - image.getHeight();
            /*
             * Conditions: the image is not larger than the canvas (dx and dy >= 0)
             * and at least one image side has the maximal size (dx or dy == 0).
             */
            if (dx >= 0 && dy >= 0 && (dx == 0 || dy == 0)) {
                view.setX(dx / 2);
                view.setY(dy / 2);
                return false;
            }
        }
        return true;
    }

    /**
     * Prepares the ARGB values to be written in the JavaFX image.
     * The task opportunistically returns statistics on all bands of the source image.
     * They are the statistics used for stretching the color ramp before to paint the JavaFX image.
     *
     * @return statistics on sample value, or {@code null} if not computed.
     */
    @Override
    protected Statistics[] call() {
        if (sourceAOI == null) {
            sourceAOI = ImageUtilities.getBounds(source);
        }
        /*
         * Use a uniform scale. At least one of `width` or `height` will be unchanged.
         * We favor showing fully the image instead of filling all the canvas space.
         */
        final double scale = Math.min(width  / (double) sourceAOI.width,
                                      height / (double) sourceAOI.height);
        xpos = (width  - (width  = Numerics.clamp(Math.round(scale * sourceAOI.width )))) / 2;
        ypos = (height - (height = Numerics.clamp(Math.round(scale * sourceAOI.height)))) / 2;
        if (width <= 0 || height <= 0) {
            return null;
        }
        final AffineTransform toCanvas = AffineTransform.getScaleInstance(scale, scale);
        toCanvas.translate(-sourceAOI.x, -sourceAOI.y);
        /*
         * Stretch color ramp using statistics about source image before to paint on JavaFX image.
         */
        final ImageProcessor processor  = new ImageProcessor();
        final Statistics[]   statistics = processor.valueOfStatistics(source, sourceAOI, (DoubleUnaryOperator[]) null);
        final RenderedImage  image      = processor.stretchColorRamp(source, Map.of("multStdDev", 3, "statistics", statistics));
        final RenderedImage  mask       = getMask(processor);
        final BufferedImage  buffer     = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics2D     graphics   = buffer.createGraphics();
        try {
            graphics.drawRenderedImage(RenderingWorkaround.wrap(image), toCanvas);
            if (mask != null) {
                graphics.drawRenderedImage(RenderingWorkaround.wrap(mask), toCanvas);
            }
        } finally {
            graphics.dispose();
        }
        data = ((DataBufferInt) buffer.getRaster().getDataBuffer()).getData();
        return statistics;
    }

    /**
     * If there is a mask that we can apply on the image, returns that mask. Otherwise returns {@code null}.
     * Current implementation returns the mask as a transparent yellow image.
     */
    private RenderedImage getMask(final ImageProcessor processor) {
        final Object mask = source.getProperty(PlanarImage.MASK_KEY);
        if (mask instanceof RenderedImage) try {
            processor.setColorizer(MASK_TRANSPARENCY);
            return processor.visualize((RenderedImage) mask);
        } catch (IllegalArgumentException e) {
            /*
             * Ignore, we will not apply any mask over the thumbnail image.
             * `PropertyView.setImage(…)` is declared as the public method.
             */
            Logging.recoverableException(LOGGER, PropertyView.class, "setImage", e);
        }
        return null;
    }

    /**
     * Sets the JavaFX image to the ARGB values computed in background thread.
     */
    @Override
    protected void succeeded() {
        WritableImage destination = null;
        if (data != null) {
            destination = (WritableImage) view.getImage();
            if (destination == null || destination.getWidth() != width || destination.getHeight() != height) {
                destination = new WritableImage(width, height);
            }
            final PixelWriter writer = destination.getPixelWriter();
            writer.setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), data, 0, width);
            data = null;
        }
        view.setImage(destination);
        view.setUserData(source);
        view.setX(xpos);
        view.setY(ypos);
    }

    /**
     * Discards ARGB values on failure.
     */
    @Override
    protected void failed() {
        data = null;
        clear(view);
    }

    /**
     * Discards ARGB values on cancellation.
     */
    @Override
    protected void cancelled() {
        data = null;
    }
}
