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
package org.apache.sis.gui.map;

import java.util.Locale;
import java.nio.IntBuffer;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.concurrent.Task;
import javafx.util.Callback;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;


/**
 * A canvas for maps to be rendered using Java2D from Abstract Window Toolkit.
 * The map is rendered using Java2D in a background thread, then copied in a JavaFX image.
 * Java2D is used for rendering the map because it may contain too many elements for a scene graph.
 * After the map has been rendered, other JavaFX nodes can be put on top of the map, typically for
 * controls by the user.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class MapCanvasAWT extends MapCanvas {
    /**
     * Whether to try to get native acceleration in the {@link VolatileImage} used for painting the map.
     * Native acceleration is of limited interested here because even if painting occurs in video card
     * memory, it is copied to Java heap before to be transferred to JavaFX image, which may itself copy
     * back to video card memory. I'm not aware of a way to perform direct transfer from AWT to JavaFX.
     * Consequently before to enable this acceleration, we should benchmark to see if it is worth.
     */
    private static final boolean NATIVE_ACCELERATION = false;

    /**
     * A buffer where to draw the content of the map for the region to be displayed.
     * This buffer uses ARGB color model, contrarily to the {@link RenderedImage} of
     * {@link org.apache.sis.coverage.grid.GridCoverage} which may have any color model.
     * This buffered image will contain only the visible region of the map;
     * it may be a zoom over a small region.
     *
     * <p>This buffered image contains the same data than the {@linkplain #image} of this canvas.
     * Those two images will share the same data array (no copy) and the same coordinate system.</p>
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
     * This configuration determines whether native acceleration will be enabled or not.
     *
     * @see #NATIVE_ACCELERATION
     */
    private GraphicsConfiguration bufferConfiguration;

    /**
     * Wraps {@link #buffer} data array for use by JavaFX images. This is the mechanism used
     * by JavaFX 13+ for allowing {@link #image} to share the same data than {@link #buffer}.
     * The same wrapper can be used for many {@link WritableImage} instances (e.g. thumbnails).
     */
    private PixelBuffer<IntBuffer> bufferWrapper;

    /**
     * The node where the rendered map will be shown. Its content is prepared in a background thread
     * by {@link Renderer#paint(Graphics2D)}. Subclasses should not set the image content directly.
     */
    protected final ImageView image;

    /**
     * Creates a new canvas for JavaFX application.
     *
     * @param  locale  the locale to use for labels and some messages, or {@code null} for default.
     */
    public MapCanvasAWT(final Locale locale) {
        super(locale);
        image = new ImageView();
        image.setPreserveRatio(true);
        floatingPane.getChildren().add(image);
    }

    /**
     * Invoked in JavaFX thread for creating a renderer to be executed in a background thread.
     * Subclasses should copy in this method all {@code MapCanvas} properties that the background thread
     * will need for performing the rendering process.
     *
     * @return rendering process to be executed in background thread,
     *         or {@code null} if there is nothing to paint.
     */
    @Override
    protected abstract Renderer createRenderer();

    /**
     * A snapshot of {@link MapCanvasAWT} state to paint as an image.
     * The snapshot is created in JavaFX thread by the {@link MapCanvasAWT#createRenderer()} method,
     * then the rendering process is executed in a background thread by {@link #paint(Graphics2D)}.
     * Methods are invoked in the following order:
     *
     * <table class="sis">
     *   <caption>Methods invoked during a map rendering process</caption>
     *   <tr><th>Method</th>                     <th>Thread</th>            <th>Remarks</th></tr>
     *   <tr><td>{@link #createRenderer()}</td>  <td>JavaFX thread</td>     <td></td></tr>
     *   <tr><td>{@link #render()}</td>          <td>Background thread</td> <td></td></tr>
     *   <tr><td>{@link #paint(Graphics2D)}</td> <td>Background thread</td> <td>May be invoked many times.</td></tr>
     *   <tr><td>{@link #commit(MapCanvas)}</td> <td>JavaFX thread</td>     <td></td></tr>
     * </table>
     *
     * This class should not access any {@link MapCanvasAWT} property from a method invoked in background thread
     * ({@link #render()} and {@link #paint(Graphics2D)}). It may access {@link MapCanvasAWT} properties from the
     * {@link #commit(MapCanvas)} method.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.1
     * @since   1.1
     * @module
     */
    protected abstract static class Renderer extends MapCanvas.Renderer {
        /**
         * Creates a new renderer. The {@linkplain #getWidth() width} and {@linkplain #getHeight() height}
         * are initially zero; they will get a non-zero values before {@link #paint(Graphics2D)} is invoked.
         */
        protected Renderer() {
        }

        /**
         * Returns whether the given buffer is non-null and have the expected size.
         */
        final boolean isValid(final BufferedImage buffer) {
            return (buffer != null)
                    && buffer.getWidth()  == super.getWidth()
                    && buffer.getHeight() == super.getHeight();
        }

        /**
         * Invoked in a background thread before {@link #paint(Graphics2D)}. Subclasses can override
         * this method if some rendering steps do not need {@link Graphics2D} handler. Doing work in
         * advance allow to hold the {@link Graphics2D} handler for a shorter time.
         *
         * <p>The default implementation does nothing.</p>
         *
         * @throws TransformException if the rendering required coordinate transformation and that
         *         operation failed.
         */
        @Override
        protected void render() throws TransformException {
        }

        /**
         * Invoked in a background thread for rendering the map. This method should not access any
         * {@link MapCanvas} property; if some canvas properties are needed, they should have been
         * copied at construction time. This method may be invoked many times if the rendering is
         * done in a {@link VolatileImage}.
         *
         * @param  gr  the Java2D handler to use for rendering the map.
         */
        protected abstract void paint(Graphics2D gr);

        /**
         * Invoked in JavaFX thread after successful {@link #paint(Graphics2D)} completion. This method can update the
         * {@link #floatingPane} children with the nodes (images, shaped, <i>etc.</i>) created by {@link #render()}.
         * If this method detects that data has changed during the time {@code Renderer} was working in background,
         * then this method can return {@code true} for requesting a new repaint. In such case that repaint will use
         * a new {@link Renderer} instance; the current instance will not be reused.
         *
         * <p>The default implementation does nothing and returns {@code true}.</p>
         *
         * @param  canvas  the canvas where drawing has been done. It will be a {@link MapCanvasAWT} instance.
         * @return {@code true} on success, or {@code false} if the rendering should be redone
         *         (for example because a change has been detected in the data).
         */
        @Override
        protected boolean commit(MapCanvas canvas) {
            return true;
        }
    }

    /**
     * Invoked when the map content needs to be rendered again into the {@link #image}.
     * It may be because the map has new content, or because the viewed region moved or
     * has been zoomed.
     *
     * <p>There is two possible situations:</p>
     * <ul class="verbose">
     *   <li>If the current buffers are not suitable, then we clear everything related to Java2D buffered images.
     *     Those resources will recreated from scratch in background thread. There is no need for double-buffering
     *     in such case because the new {@link BufferedImage} will not be shared with JavaFX image before the end
     *     of this task.</li>
     *   <li>If the current buffer are still valid, then we should not update {@link BufferedImage} in background
     *     thread because the internal array of that image is shared with JavaFX image. That image can be updated
     *     only in JavaFX thread through the {@code PixelBuffer.update(…)} method. In this case we will use a
     *     {@link VolatileImage} as a temporary buffer.</li>
     * </ul>
     *
     * In both cases we need to be careful to not use directly any {@link MapCanvas} field from the {@code call()}
     * methods. Information needed by {@code call()} must be copied first.
     *
     * @see #requestRepaint()
     */
    @Override
    final Task<?> createWorker(final MapCanvas.Renderer mc) {
        final Renderer context = (Renderer) mc;
        if (!context.isValid(buffer)) {
            buffer              = null;
            doubleBuffer        = null;
            bufferWrapper       = null;
            bufferConfiguration = null;
            return new Creator(context);
        } else {
            return new Updater(context);
        }
    }

    /**
     * Background tasks for creating a new {@link BufferedImage}. This task is invoked when there is no
     * previous resources that we can recycle, either because they have never been created yet or because
     * they are not suitable anymore (for example because the image size changed).
     */
    private final class Creator extends Task<WritableImage> {
        /**
         * The user-provided object which will perform the actual rendering.
         * Its {@link Renderer#paint(Graphics2D)} method will be invoked.
         */
        private final Renderer renderer;

        /**
         * The Java2D image where to do the rendering. This image will be created in a background thread
         * and assigned to the {@link MapCanvasAWT#buffer} field in JavaFX thread if rendering succeed.
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
         * Creates a new task for painting without resource recycling.
         */
        Creator(final Renderer context) {
            renderer = context;
        }

        /**
         * Invoked in background thread for creating and rendering the image (may be slow).
         * Any {@link MapCanvas} property needed by this method shall be copied before the
         * background thread is executed; no direct reference to {@link MapCanvas} here.
         */
        @Override
        protected WritableImage call() throws TransformException {
            renderer.render();
            final int width  = renderer.getWidth();
            final int height = renderer.getHeight();
            drawTo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            final Graphics2D gr = drawTo.createGraphics();
            try {
                configuration = gr.getDeviceConfiguration();
                renderer.paint(gr);
            } finally {
                gr.dispose();
            }
            if (NATIVE_ACCELERATION) {
                if (!configuration.getImageCapabilities().isAccelerated()) {
                    configuration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                    .getDefaultScreenDevice().getDefaultConfiguration();
                }
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
         * buffers created by this task are saved in {@link MapCanvas} fields for reuse next time that
         * an image of the same size will be rendered again.
         */
        @Override
        protected void succeeded() {
            image.setImage(getValue());
            buffer              = drawTo;
            bufferWrapper       = wrapper;
            bufferConfiguration = configuration;
            final boolean done  = renderer.commit(MapCanvasAWT.this);
            renderingCompleted(this);
            if (!done || contentsChanged()) {
                repaint();
            }
        }

        @Override protected void failed()    {renderingCompleted(this);}
        @Override protected void cancelled() {renderingCompleted(this);}
    }

    /**
     * Background tasks for painting in an existing {@link BufferedImage}. This task is invoked
     * when previous resources (JavaFX image and Java2D volatile/buffered image) can be reused.
     * The Java2D volatile image will be rendered in background thread, then its content will be
     * transferred to JavaFX image (through {@link BufferedImage} shared array) in JavaFX thread.
     */
    private final class Updater extends Task<VolatileImage> implements Callback<PixelBuffer<IntBuffer>, Rectangle2D> {
        /**
         * The user-provided object which will perform the actual rendering.
         * Its {@link Renderer#paint(Graphics2D)} method will be invoked.
         */
        private final Renderer renderer;

        /**
         * The buffer during last paint operation. This buffer will be reused if possible,
         * but may become invalid and in need to be recreated. May be {@code null}.
         */
        private VolatileImage previousBuffer;

        /**
         * The configuration to use for creating a new {@link VolatileImage}
         * if {@link #previousBuffer} is invalid.
         */
        private final GraphicsConfiguration configuration;

        /**
         * Whether {@link VolatileImage} content became invalid and needs to be recreated.
         */
        private boolean contentsLost;

        /**
         * Creates a new task for painting with resource recycling.
         */
        Updater(final Renderer context) {
            renderer       = context;
            previousBuffer = doubleBuffer;
            configuration  = bufferConfiguration;
        }

        /**
         * Invoked in background thread for rendering the image (may be slow).
         * Any {@link MapCanvas} field needed by this method shall be copied before the
         * background thread is executed; no direct reference to {@link MapCanvas} here.
         */
        @Override
        protected VolatileImage call() throws TransformException {
            renderer.render();
            final int width  = renderer.getWidth();
            final int height = renderer.getHeight();
            VolatileImage drawTo = previousBuffer;
            previousBuffer = null;                      // For letting GC do its work.
            if (drawTo == null) {
                drawTo = configuration.createCompatibleVolatileImage(width, height, VolatileImage.TRANSLUCENT);
            }
            boolean invalid = true;
            try {
                do {
                    if (drawTo.validate(configuration) == VolatileImage.IMAGE_INCOMPATIBLE) {
                        drawTo = configuration.createCompatibleVolatileImage(width, height, VolatileImage.TRANSLUCENT);
                    }
                    final Graphics2D gr = drawTo.createGraphics();
                    try {
                        gr.setBackground(ColorModelFactory.TRANSPARENT);
                        gr.clearRect(0, 0, drawTo.getWidth(), drawTo.getHeight());
                        renderer.paint(gr);
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
         * Invoked by {@link PixelBuffer#updateBuffer(Callback)} for updating the {@link #buffer} content.
         */
        @Override
        public Rectangle2D call(final PixelBuffer<IntBuffer> wrapper) {
            final VolatileImage drawTo = doubleBuffer;
            final Graphics2D gr = buffer.createGraphics();
            try {
                gr.setComposite(AlphaComposite.Src);        // Copy source (previous destination is discarded).
                gr.drawImage(drawTo, 0, 0, null);
                contentsLost = drawTo.contentsLost();
            } finally {
                gr.dispose();
            }
            return null;
        }

        /**
         * Invoked in JavaFX thread on success. The JavaFX image is set to the result, then the double buffer
         * created by this task is saved in {@link MapCanvas} fields for reuse next time that an image of the
         * same size will be rendered again.
         */
        @Override
        protected void succeeded() {
            final VolatileImage drawTo = getValue();
            doubleBuffer = drawTo;
            try {
                bufferWrapper.updateBuffer(this);   // This will invoke the `call(PixelBuffer)` method above.
            } finally {
                drawTo.flush();                     // Release native resources.
            }
            final boolean done = renderer.commit(MapCanvasAWT.this);
            renderingCompleted(this);
            if (!done || contentsLost || contentsChanged()) {
                repaint();
            }
        }

        @Override protected void failed()    {renderingCompleted(this);}
        @Override protected void cancelled() {renderingCompleted(this);}
    }

    /**
     * Clears the image and all intermediate buffer.
     * Invoking this method may help to release memory when the map is no longer shown.
     */
    @Override
    protected void clear() {
        image.setImage(null);
        buffer              = null;
        bufferWrapper       = null;
        doubleBuffer        = null;
        bufferConfiguration = null;
        super.clear();
    }
}
