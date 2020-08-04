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
package org.apache.sis.internal.gui;

import java.util.Map;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.jdk9.JDK9;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.Statistics;


/**
 * Converts a Java2D image to a JavaFX image, then writes the result in a given {@link ImageView}.
 * This task should be used only for small images (thumbnail) because some potentially costly resources
 * are created each time.
 *
 * <p>Current implementation returns statistics on sample values as a side-product.
 * It may change in any future version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ImageConverter extends Task<Statistics[]> {
    /**
     * The maximal image width and height, as an arbitrary value that may change in any future version.
     * This size should be a typical size expected for the JavaFX control showing the image.
     * If the {@link ImageView} is larger than this size, the image will be rescaled by JavaFX.
     * The quality is lower than if we had let the "resample" operation do its work at full resolution,
     * but {@link PropertyView} should be used only of overview anyway.
     */
    private static final int MAX_SIZE = 600;

    /**
     * Colors to apply on the mask image when that image is overlay on top of another image.
     */
    private static final Map<NumberRange<?>,Color[]> MASK_TRANSPARENCY = JDK9.mapOf(
            NumberRange.create(0, true, 0, true), new Color[] {ColorModelFactory.TRANSPARENT},
            NumberRange.create(1, true, 1, true), new Color[] {new Color(0x20FFFF00, true)});

    /**
     * The Java2D image to convert.
     */
    private final RenderedImage source;

    /**
     * Pixel coordinates of the region to render, or {@code null} for the whole image.
     */
    private Rectangle bounds;

    /**
     * Size of the image actually rendered. This is the {@link #bounds} size,
     * unless that size is greater than {@link #MAX_SIZE} in which case it is scaled down.
     */
    private int width, height;

    /**
     * Where to write the image. This will be updated in JavaFX thread.
     */
    private final ImageView canvas;

    /**
     * The ARGB values to be copied in the JavaFX image.
     */
    private int[] data;

    /**
     * Creates a new task for converting the given image.
     */
    ImageConverter(final RenderedImage source, final Rectangle bounds, final ImageView canvas) {
        this.source = source;
        this.bounds = bounds;
        this.canvas = canvas;
    }

    /**
     * Prepares the ARGB values to be written in the JavaFX image.
     */
    @Override
    protected Statistics[] call() {
        if (bounds == null) {
            bounds = ImageUtilities.getBounds(source);
        }
        width  = Math.min(bounds.width,  MAX_SIZE);
        height = Math.min(bounds.height, MAX_SIZE);
        final double scale = Math.max(width  / (double) bounds.width,
                                      height / (double) bounds.height);
        /*
         * Use a uniform scale. At least one of `width` or `height` will be unchanged.
         */
        width  = (int) Math.round(scale * bounds.width);
        height = (int) Math.round(scale * bounds.height);
        final AffineTransform toCanvas = AffineTransform.getScaleInstance(scale, scale);
        toCanvas.translate(-bounds.x, -bounds.y);

        final ImageProcessor processor  = new ImageProcessor();
        final Statistics[]   statistics = processor.getStatistics(source, bounds);
        final RenderedImage  image      = processor.stretchColorRamp(source, JDK9.mapOf("multStdDev", 3, "statistics", statistics));
        final RenderedImage  mask       = getMask(processor);
        final BufferedImage  buffer     = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics2D     graphics   = buffer.createGraphics();
        try {
            graphics.drawRenderedImage(image, toCanvas);
            if (mask != null) {
                graphics.drawRenderedImage(mask, toCanvas);
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
            return processor.visualize((RenderedImage) mask, MASK_TRANSPARENCY);
        } catch (IllegalArgumentException e) {
            // Ignore, we will not apply any mask. Declare PropertyView.setImage(…) as the public method.
            Logging.recoverableException(Logging.getLogger(Loggers.APPLICATION), PropertyView.class, "setImage", e);
        }
        return null;
    }

    /**
     * Sets the JavaFX image to the ARGB values computed in background thread.
     */
    @Override
    protected void succeeded() {
        WritableImage destination = (WritableImage) canvas.getImage();
        if (destination == null || destination.getWidth() != width || destination.getHeight() != height) {
            destination = new WritableImage(width, height);
        }
        final PixelWriter writer = destination.getPixelWriter();
        writer.setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), data, 0, width);
        data = null;
        canvas.setImage(destination);
        canvas.setFitWidth (bounds.width);
        canvas.setFitHeight(bounds.height);
    }

    /**
     * Discards ARGB values on failure.
     */
    @Override
    protected void failed() {
        data = null;
    }

    /**
     * Discards ARGB values on cancellation.
     */
    @Override
    protected void cancelled() {
        data = null;
    }
}
