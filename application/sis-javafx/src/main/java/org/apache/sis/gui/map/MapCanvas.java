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
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.Cursor;
import javafx.event.EventType;
import javafx.beans.Observable;
import javafx.concurrent.Task;
import javafx.util.Callback;
import org.opengis.geometry.Envelope;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.map.PlanarCanvas;
import org.apache.sis.internal.map.RenderException;


/**
 * A canvas for maps to be rendered on screen in a JavaFX application.
 * The map is rendered using Java2D in a background thread, then copied in a JavaFX image.
 * Java2D is used for rendering the map because it may contain too many elements for a scene graph.
 * After the map has been rendered, other JavaFX nodes can be put on top of the map, typically for
 * controls interacting with the user.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class MapCanvas extends PlanarCanvas {
    /**
     * A factor for converting deltas from scroll wheel into zoom factor.
     * For positive deltas, the zoom in factor will be {@code delta/MOUSE_WHEEL_ZOOM + 1}.
     * For a typical value {@code delta} = 40, a {@code MOUSE_WHEEL_ZOOM} value of 400
     * results in a zoom factor of 10%.
     */
    private static final double MOUSE_WHEEL_ZOOM = 400;

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
     */
    private GraphicsConfiguration bufferConfiguration;

    /**
     * Wraps {@link #buffer} data array for use by JavaFX images. This is the mechanism used
     * by JavaFX 13+ for allowing {@link #image} to share the same data than {@link #buffer}.
     * The same wrapper can be used for many {@link WritableImage} instances (e.g. thumbnails).
     */
    private PixelBuffer<IntBuffer> bufferWrapper;

    /**
     * The node where the rendered map will be shown.
     */
    protected final ImageView image;

    /**
     * The pane where to put children. This pane uses absolute layout. It contains at least the
     * JavaFX {@linkplain #image} of the map, but can also contain additional nodes for geometric
     * shapes, texts, <i>etc</i>.
     */
    protected final Pane view;

    /**
     * The data bounds to use for computing the initial value of {@link #objectiveToDisplay}.
     * This is reset to {@code null} after the transform has been computed.
     * We differ this recomputation until all parameters are known.
     *
     * @see #setObjectiveBounds(Envelope)
     * @see #invalidObjectiveToDisplay
     */
    private Envelope objectiveBounds;

    /**
     * Incremented when the map needs to be rendered again.
     *
     * @see #renderedContentStamp
     * @see #contentsChanged()
     */
    private int contentChangeCount;

    /**
     * Value of {@link #contentChangeCount} last time the data have been rendered. This is used for deciding
     * if a call to {@link #repaint()} should be done with the next layout operation. We need this check for
     * avoiding never-ending repaint events caused by calls to {@link ImageView#setImage(Image)} causing
     * themselves new layout events. It is okay if this value overflows.
     */
    private int renderedContentStamp;

    /**
     * Whether a rendering task is in progress. Used for avoiding to send too many {@link #repaint()} requests;
     * we will wait for current repaint event to finish before to send another one.
     */
    private boolean isRendering;

    /**
     * Whether the size of this canvas changed.
     */
    private boolean sizeChanged;

    /**
     * Whether {@link #objectiveToDisplay} needs to be recomputed.
     * We differ this recomputation until all parameters are known.
     */
    private boolean invalidObjectiveToDisplay;

    /**
     * The zooms, pans and rotations applied on {@link #view} since last time the image has been painted.
     * This is the identity transform except during the short time between a gesture (zoom, pan, <i>etc.</i>)
     * and the completion of new {@link #repaint()}. This is used for giving immediate feedbacks to the user
     * while waiting for the new image to be ready.
     */
    private final Affine transform;

    /**
     * The {@link #transform} values at the time the {@link #repaint()} method has been invoked.
     * This is a change applied on {@link #objectiveToDisplay} but not yet visible in the image.
     * After the image has been updated, this transform is reset to identity.
     */
    private final Affine changeInProgress;

    /**
     * Cursor position at the time pan event started.
     * This is used for computing the {@linkplain #view} translation to apply during drag events.
     *
     * @see #onDrag(MouseEvent)
     */
    private double xPanStart, yPanStart;

    /**
     * Creates a new canvas for JavaFX application.
     *
     * @param  locale  the locale to use for labels and some messages, or {@code null} for default.
     */
    public MapCanvas(final Locale locale) {
        super(locale);
        transform = new Affine();
        changeInProgress = new Affine();
        view = new Pane() {
            @Override protected void layoutChildren() {
                super.layoutChildren();
                if (contentsChanged()) {
                    repaint();
                }
            }
        };
        image = new ImageView();
        image.setPreserveRatio(true);
        view.getChildren().add(image);
        view.getTransforms().add(transform);
        view.setOnScroll(this::onScroll);
        view.setOnMousePressed(this::onDrag);
        view.setOnMouseDragged(this::onDrag);
        view.setOnMouseReleased(this::onDrag);
        /*
         * Do not set a preferred size, otherwise `repaint()` is invoked twice: once with the preferred size
         * and once with the actual size of the parent window. Actually the `repaint()` method appears to be
         * invoked twice anyway, but without preferred size the width appears to be 0, in which case nothing
         * is repainted.
         */
        view.widthProperty() .addListener(this::onSizeChanged);
        view.heightProperty().addListener(this::onSizeChanged);
        view.setClip(new Rectangle(view.getWidth(), view.getHeight()));
        view.setCursor(Cursor.CROSSHAIR);
    }

    /**
     * Invoked when the size of the {@linkplain #view} has changed.
     * This method requests a new repaint.
     */
    private void onSizeChanged(final Observable property) {
        final Rectangle clip = (Rectangle) view.getClip();
        clip.setWidth (view.getWidth());
        clip.setHeight(view.getHeight());
        contentChangeCount++;
        sizeChanged = true;
        repaint();
    }

    /**
     * Invoked when the user presses the button, drags the map and releases the button.
     * This is interpreted as a translation applied in pixel units on the map.
     */
    private void onDrag(final MouseEvent event) {
        final double x = event.getX();
        final double y = event.getY();
        final EventType<? extends MouseEvent> type = event.getEventType();
        if (type == MouseEvent.MOUSE_PRESSED) {
            view.setCursor(Cursor.CLOSED_HAND);
            xPanStart = x;
            yPanStart = y;
        } else {
            if (type != MouseEvent.MOUSE_DRAGGED) {
                view.setCursor(isRendering ? Cursor.WAIT : Cursor.CROSSHAIR);
            }
            transform.appendTranslation(x - xPanStart, y - yPanStart);
            repaintIfMoved();
        }
        event.consume();
    }

    /**
     * Invoked when the user rotates the mouse wheel.
     * This method performs a zoom-in or zoom-out event.
     */
    private void onScroll(final ScrollEvent event) {
        if (event.getTouchCount() != 0) {
            // Do not interpret scroll events on touch pad as a zoom.
            return;
        }
        final double delta = event.getDeltaY();
        double zoom = Math.abs(delta) / MOUSE_WHEEL_ZOOM + 1;
        if (delta < 0) {
            zoom = 1/zoom;
        }
        transform.appendScale(zoom, zoom, event.getX(), event.getY());
        repaintIfMoved();
        event.consume();
    }

    /**
     * Repaints the view if the given transform is not identity.
     */
    private void repaintIfMoved() {
        if (!transform.isIdentity()) {
            contentChangeCount++;
            repaint();
        }
    }

    /**
     * Returns {@code true} if content changed since the last {@link #repaint()} execution.
     */
    private boolean contentsChanged() {
        return contentChangeCount != renderedContentStamp;
    }

    /**
     * Sets the data bounds to use for computing the initial value of {@link #objectiveToDisplay}.
     * This method should be invoked only when new data have been loaded, or when the caller wants
     * to discard any zoom or translation and reset the view of the given bounds.
     *
     * @param  visibleArea  bounding box in objective CRS of the initial area to show,
     *         or {@code null} if unknown (in which case an identity transform will be set).
     *
     * @see #setObjectiveCRS(CoordinateReferenceSystem)
     */
    protected void setObjectiveBounds(final Envelope visibleArea) {
        ArgumentChecks.ensureDimensionMatches("bounds", BIDIMENSIONAL, visibleArea);
        objectiveBounds = visibleArea;
        invalidObjectiveToDisplay = true;
    }

    /**
     * Starts a background task for any process for loading or rendering the map.
     * This {@code MapCanvas} class invokes this method for rendering the map,
     * but subclasses can also invoke this method for other purposes.
     *
     * <p>Tasks need to be careful to not access any {@code MapCanvas} property in their {@link Task#call()} method.
     * If a canvas property is needed by the task, its value should be copied before the background thread is started.
     * However {@link Task#succeeded()} and similar methods can safety read and write those properties.</p>
     *
     * <p>Subclasses are encouraged to override this method and configure the following properties
     * before to invoke {@code super.execute(task)}:</p>
     * <ul>
     *   <li><code>{@linkplain Task#runningProperty()}.addListener(…)</code></li>
     *   <li><code>{@linkplain Task#setOnFailed Task.setOnFailed}(…)</code></li>
     * </ul>
     *
     * @param  task  the task to execute in a background thread for loading or rendering the map.
     */
    protected void execute(final Task<?> task) {
        BackgroundThreads.execute(task);
    }

    /**
     * Invoked in JavaFX thread for creating a renderer to be executed in a background thread.
     * Subclasses should copy in this method all {@code MapCanvas} properties that the background thread
     * will need for performing the rendering process.
     *
     * @return rendering process to be executed in background thread,
     *         or {@code null} if there is nothing to paint.
     */
    protected abstract Renderer createRenderer();

    /**
     * A snapshot of {@link MapCanvas} state to paint as an image.
     * The snapshot is created in JavaFX thread by the {@link MapCanvas#createRenderer()} method,
     * then the rendering process is executed in a background thread by {@link #paint(Graphics2D)}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.1
     * @since   1.1
     * @module
     */
    protected abstract static class Renderer {
        /**
         * The canvas size.
         */
        private int width, height;

        /**
         * Creates a new renderer. The {@linkplain #getWidth() width} and {@linkplain #getHeight() height}
         * are initially zero; they will get a non-zero values before {@link #paint(Graphics2D)} is invoked.
         */
        protected Renderer() {
        }

        /**
         * Sets the width and height to the size of the given view,
         * then returns {@code true} if the view is non-empty.
         */
        final boolean initialize(final Pane view) {
            width  = Numerics.clamp(Math.round(view.getWidth()));
            height = Numerics.clamp(Math.round(view.getHeight()));
            return width > 0 && height > 0;
        }

        /**
         * Returns whether the given buffer is non-null and have the expected size.
         */
        final boolean isValid(final BufferedImage buffer) {
            return buffer != null && buffer.getWidth() == width && buffer.getHeight() == height;
        }

        /**
         * Returns the width (number of columns) of the view, in pixels.
         *
         * @return number of columns in the image to paint.
         */
        public int getWidth() {
            return width;
        }

        /**
         * Returns the height (number of rows) of the view, in pixels.
         *
         * @return number of rows in the image to paint.
         */
        public int getHeight() {
            return height;
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
    }

    /**
     * Requests the map to be rendered again, possibly with new data. Invoking this
     * method does not necessarily causes the repaint process to start immediately.
     * The request will be queued and executed at an arbitrary time.
     */
    protected void requestRepaint() {
        contentChangeCount++;
        view.requestLayout();
    }

    /**
     * Invoked when the map content needs to be rendered again into the {@link #image}.
     * It may be because the map has new content, or because the viewed region moved or
     * have been zoomed.
     *
     * @see #requestRepaint()
     */
    private void repaint() {
        /*
         * If a rendering is already in progress, do not send a new request now.
         * Wait for current rendering to finish; a new one will be automatically
         * requested if content changes are detected after the rendering.
         */
        if (isRendering) {
            contentChangeCount++;
            return;
        }
        renderedContentStamp = contentChangeCount;
        try {
            /*
             * If a new canvas size is known, inform the parent `PlanarCanvas` about that.
             * It may cause a recomputation of the "objective to display" transform.
             */
            if (sizeChanged) {
                sizeChanged = false;
                Envelope2D bounds = new Envelope2D(null, view.getLayoutX(), view.getLayoutY(), view.getWidth(), view.getHeight());
                if (bounds.isEmpty()) return;
                setDisplayBounds(bounds);
            }
            /*
             * Compute the `objectiveToDisplay` only before the first rendering, because the display
             * bounds may not be known before (it may be zero at the time `MapCanvas` is initialized).
             * This code is executed only once for a new map.
             */
            if (invalidObjectiveToDisplay) {
                invalidObjectiveToDisplay = false;
                LinearTransform tr;
                final Envelope source = objectiveBounds;
                if (objectiveBounds != null) {
                    objectiveBounds = null;
                    final Envelope2D target = getDisplayBounds();
                    final MatrixSIS m = Matrices.createTransform(source, target);
                    Matrices.forceUniformScale(m, 0, new double[] {target.width / 2, target.height / 2});
                    tr = MathTransforms.linear(m);
                } else {
                    tr = MathTransforms.identity(BIDIMENSIONAL);
                }
                setObjectiveToDisplay(tr);
                transform.setToIdentity();
            }
        } catch (RenderException ex) {
            errorOccurred(ex);
            return;
        }
        /*
         * If a temporary zoom, rotation or translation has been applied using JavaFX transform API,
         * replaced that temporary transform by a "permanent" adjustment of the `objectiveToDisplay`
         * transform. It allows SIS to get new data for the new visible area and resolution.
         */
        assert changeInProgress.isIdentity() : changeInProgress;
        changeInProgress.setToTransform(transform);
        if (!transform.isIdentity()) {
            transformDisplayCoordinates(new AffineTransform(
                    transform.getMxx(), transform.getMyx(),
                    transform.getMxy(), transform.getMyy(),
                    transform.getTx(),  transform.getTy()));
        }
        /*
         * Invoke `createRenderer()` only after we finished above configuration, because that method
         * may take a snapshot of current canvas state in preparation for use in background threads.
         */
        final Renderer context = createRenderer();
        if (context == null || !context.initialize(view)) {
            return;
        }
        /*
         * There is two possible situations: if the current buffers are not suitable, we clear everything related
         * to Java2D buffered images and will recreate everything from scratch in the background thread. There is
         * no need for double-buffering in such case since the new `BufferedImage` will not be shared with JavaFX
         * image before the end of this task.
         *
         * The second situation is when the buffer is still valid. In such case we should not update the BufferedImage
         * in a background thread because the internal array of that image is shared with JavaFX image, and that image
         * should be updated only in JavaFX thread through the `PixelBuffer.update(…)` method. For that second case we
         * will use a `VolatileImage` as a temporary buffer.
         *
         * In both cases we need to be careful to not use directly any `MapCanvas` field from the `call()` method.
         * Information needed by `call()` must be copied first.
         */
        final Task<?> worker;
        if (!context.isValid(buffer)) {
            buffer              = null;
            doubleBuffer        = null;
            bufferWrapper       = null;
            bufferConfiguration = null;
            worker = new Task<WritableImage>() {
                /**
                 * The Java2D image where to do the rendering. This image will be created in a background thread
                 * and assigned to the {@link MapCanvas#buffer} field in JavaFX thread if rendering succeed.
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
                 * Any {@link MapCanvas} property needed by this method shall be copied before the
                 * background thread is executed; no direct reference to {@link MapCanvas} here.
                 */
                @Override
                protected WritableImage call() {
                    final int width  = context.getWidth();
                    final int height = context.getHeight();
                    drawTo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
                    final Graphics2D gr = drawTo.createGraphics();
                    configuration = gr.getDeviceConfiguration();
                    try {
                        context.paint(gr);
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
                 * buffers created by this task are saved in {@link MapCanvas} fields for reuse next time that
                 * an image of the same size will be rendered again.
                 */
                @Override
                protected void succeeded() {
                    super.succeeded();
                    image.setImage(getValue());
                    buffer              = drawTo;
                    bufferWrapper       = wrapper;
                    bufferConfiguration = configuration;
                    imageUpdated();
                    if (contentsChanged()) {
                        repaint();
                    }
                }

                @Override protected void failed()    {super.failed();    imageUpdated();}
                @Override protected void cancelled() {super.cancelled(); imageUpdated();}
            };
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
                 * Whether {@link VolatileImage} content became invalid and needs to be recreated.
                 */
                private boolean contentsLost;

                /**
                 * Invoked in background thread for rendering the image (may be slow).
                 * Any {@link MapCanvas} field needed by this method shall be copied before the
                 * background thread is executed; no direct reference to {@link MapCanvas} here.
                 */
                @Override
                protected VolatileImage call() {
                    final int width  = context.getWidth();
                    final int height = context.getHeight();
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
                                context.paint(gr);
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
                 * double buffer created by this task is saved in {@link MapCanvas} fields for reuse
                 * next time that an image of the same size will be rendered again.
                 */
                @Override
                protected void succeeded() {
                    super.succeeded();
                    final VolatileImage drawTo = getValue();
                    doubleBuffer = drawTo;
                    try {
                        bufferWrapper.updateBuffer(this);       // This will invoke the `call(…)` method below.
                    } finally {
                        drawTo.flush();
                    }
                    imageUpdated();
                    if (contentsLost || contentsChanged()) {
                        repaint();
                    }
                }

                @Override protected void failed()    {super.failed();    imageUpdated();}
                @Override protected void cancelled() {super.cancelled(); imageUpdated();}

                /**
                 * Invoked by {@link PixelBuffer#updateBuffer(Callback)} for updating the {@link #buffer} content.
                 */
                @Override
                public Rectangle2D call(final PixelBuffer<IntBuffer> wrapper) {
                    final VolatileImage drawTo = doubleBuffer;
                    final Graphics2D gr = buffer.createGraphics();
                    try {
                        gr.drawImage(drawTo, 0, 0, null);
                        contentsLost = drawTo.contentsLost();
                    } finally {
                        gr.dispose();
                    }
                    return null;
                }
            }
            worker = new Updater();
        }
        view.setCursor(Cursor.WAIT);
        execute(worker);
        isRendering = true;                 // Set last after we know that the task has been scheduled.
    }

    /**
     * Invoked after the background thread created by {@link #repaint()} finished to update image content.
     * The {@link #changeInProgress} is the JavaFX transform at the time the repaint event was trigged and
     * which is now integrated in the image. That transform will be removed from {@link #view} transforms.
     * It may be identity if no zoom, rotation or pan gesture has been applied since last rendering.
     */
    private void imageUpdated() {
        isRendering = false;
        view.setCursor(Cursor.CROSSHAIR);
        if (!changeInProgress.isIdentity()) {
            final Point2D p = changeInProgress.transform(xPanStart, yPanStart);
            xPanStart = p.getX();
            yPanStart = p.getY();
            try {
                changeInProgress.invert();
                transform.prepend(changeInProgress);
            } catch (NonInvertibleTransformException e) {
                // Should not happen. If happens anyway, discard the gestures that happenned after `repaint()` call.
                Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), MapCanvas.class, "imageUpdated", e);
                transform.setToIdentity();
            }
            changeInProgress.setToIdentity();
        }
    }

    /**
     * Clears the image and all intermediate buffer.
     * Invoking this method may help to release memory when the map is no longer shown.
     */
    protected void clear() {
        image.setImage(null);
        buffer              = null;
        bufferWrapper       = null;
        doubleBuffer        = null;
        bufferConfiguration = null;
        transform.setToIdentity();
        changeInProgress.setToIdentity();
    }

    /**
     * Invoked when an error occurred. The default implementation popups a dialog box.
     * Subclasses may override. For example the error messages could be written in a status bar instead.
     *
     * @param  ex  the exception that occurred.
     */
    protected void errorOccurred(final Throwable ex) {
        ExceptionReporter.show(null, null, ex);
    }
}
