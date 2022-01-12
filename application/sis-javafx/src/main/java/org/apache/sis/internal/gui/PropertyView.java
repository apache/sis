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

import java.util.Locale;
import java.util.Objects;
import java.util.Collection;
import java.lang.reflect.Array;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.Localized;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A viewer for property value. The property may be of various class (array, image, <i>etc</i>).
 * If the type is unrecognized, the property is shown as text.
 *
 * <p>This class implements {@code ChangeListener} for implementation convenience only.
 * Users should not rely on this implementation details.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public final class PropertyView implements Localized, ChangeListener<Number> {
    /**
     * Provider for {@link java.text.NumberFormat}, {@link java.text.DateFormat}, <i>etc</i>.
     */
    private final TextFormats formats;

    /**
     * The current property value. This is used for detecting changes.
     */
    private Object value;

    /**
     * The node used for showing {@link #value}.
     * The node is created by {@link #set(Object, Rectangle)}.
     */
    public final ObjectProperty<Node> view;

    /**
     * Shows the {@linkplain #value} as plain text.
     * This is built only when first needed.
     */
    private TextArea textView;

    /**
     * Shows the {@linkplain #value} as a list.
     * This is built only when first needed.
     */
    private ListView<String> listView;

    /**
     * Shows the {@linkplain #value} as an image.
     * This is built only when first needed.
     */
    private ImageView imageView;

    /**
     * The pane containing {@link #imageView}. We use that pane for allowing a background color to be specified.
     * A future version may also use that pane for putting more visual components on top or below the image.
     *
     * @see #getImageCanvas()
     */
    private Pane imageCanvas;

    /**
     * The group of all components related to image, created when first needed.
     * This includes {@link #imageView} and {@link #sampleValueRange}.
     */
    private GridPane imagePane;

    /**
     * Minimum and maximum values found in the image wrapped by {@link #imageView}.
     * Created when first needed.
     */
    private Label sampleValueRange;

    /**
     * Mean value and standard deviation found in the image wrapped by {@link #imageView}.
     * Created when first needed.
     */
    private Label meanValue;

    /**
     * If the property is an image, bounds of currently visible region. May be {@code null} is unknown,
     * in which case the whole image bounds is taken.
     */
    private Rectangle visibleImageBounds;

    /**
     * The work which is under progress in a background thread (running) or
     * which is waiting for execution (pending), or {@code null} if none.
     */
    private ImageConverter runningTask, pendingTask;

    /**
     * Creates a new property view which will use the given formatter for formatting values.
     *
     * @param  formatter  the formatter to use for formatting values.
     */
    public PropertyView(final PropertyValueFormatter formatter) {
        formats = formatter.formats;
        view = new SimpleObjectProperty<>(this, "view");
    }

    /**
     * Creates a new property view which will set the node on the given property.
     *
     * @param  locale      the locale for numbers formatting.
     * @param  view        the property where to set the node showing the value.
     * @param  background  the image background color, or {@code null} if none.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public PropertyView(final Locale locale, final ObjectProperty<Node> view, final ObjectProperty<Background> background) {
        formats = new TextFormats(locale);
        this.view = view;
        if (background != null) {
            getImageCanvas().backgroundProperty().bind(background);
        }
    }

    /**
     * Returns the locale for formatting messages and values.
     *
     * @return the locale used by formats.
     */
    @Override
    public Locale getLocale() {
        return formats.getLocale();
    }

    /**
     * Sets the view to the given value.
     *
     * @param  newValue       the new value (may be {@code null}).
     * @param  visibleBounds  if the property is an image, currently visible region. Can be {@code null}.
     */
    public void set(final Object newValue, final Rectangle visibleBounds) {
        final boolean boundsChanged = !Objects.equals(visibleBounds, visibleImageBounds);
        if (newValue != value || boundsChanged) {
            visibleImageBounds = visibleBounds;
            final Node content;
            if (newValue instanceof RenderedImage) {
                content = setImage((RenderedImage) newValue, boundsChanged);
            } else {
                /*
                 * The `setImage(…)` method manages itself the `runningTaks` and `pendingTask` fields.
                 * But if the new property requires a different method, we need to cancel everything.
                 */
                final ImageConverter task = runningTask;
                if (task != null) {
                    runningTask = null;
                    pendingTask = null;
                    task.cancel(BackgroundThreads.NO_INTERRUPT_DURING_IO);
                }
                if (newValue == null) {
                    content = null;
                } else if (newValue instanceof Throwable) {
                    content = setText((Throwable) newValue);
                } else if (newValue instanceof IdentifiedObject) {
                    content = setCRS((IdentifiedObject) newValue);
                } else if (newValue instanceof Collection<?>) {
                    content = setList(((Collection<?>) newValue).toArray());
                } else if (newValue.getClass().isArray()) {
                    content = setList(newValue);
                } else {
                    content = setText(formats.formatValue(newValue, true));
                }
            }
            view.set(content);
            value = newValue;           // Assign only on success.
        }
    }

    /**
     * Sets the property value to the given text.
     */
    private Node setText(final String text) {
        TextArea node = textView;
        if (node == null) {
            node = new TextArea();
            node.setEditable(false);
            node.setFont(Font.font("Monospaced"));
            textView = node;
        }
        node.setText(text);
        return node;
    }

    /**
     * Sets the text to the stack trace of given exception.
     */
    private Node setText(final Throwable ex) {
        return setText(ExceptionReporter.getStackTrace(ex));
    }

    /**
     * Sets the property value to the given array.
     */
    private Node setList(final Object array) {
        ListView<String> node = listView;
        if (node == null) {
            node = new ListView<>();
            listView = node;
        }
        final String[] list = new String[Array.getLength(array)];
        for (int i=0; i<list.length; i++) {
            list[i] = formats.formatValue(Array.get(array, i), true);
        }
        listView.getItems().setAll(list);
        return node;
    }

    /**
     * Sets the viewer for a coordinate reference system. Shown as Well-Known Text (WKT) for now,
     * but a future version may provide a more sophisticated viewer.
     */
    private Node setCRS(final IdentifiedObject crs) {
        return setText(crs.toString());
    }

    /**
     * Returns the pane containing {@link #imageView}.
     */
    private Pane getImageCanvas() {
        if (imageCanvas == null) {
            imageCanvas = new Pane();
            imageCanvas.widthProperty() .addListener(this);
            imageCanvas.heightProperty().addListener(this);
        }
        return imageCanvas;
    }

    /**
     * Sets the property value to the given image.
     *
     * @param  image          the property value to set, or {@code null}.
     * @param  boundsChanged  whether {@link #visibleImageBounds} changed since last call.
     */
    private Node setImage(final RenderedImage image, final boolean boundsChanged) {
        final Pane imageCanvas = getImageCanvas();
        ImageView node = imageView;
        if (node == null) {
            node = new ImageView();
            node.setPreserveRatio(true);
            imageCanvas.getChildren().setAll(node);
            GUIUtilities.setClipToBounds(imageCanvas);
            GridPane.setConstraints(imageCanvas, 0, 0, 2, 1);
            GridPane.setHgrow(imageCanvas, Priority.ALWAYS);
            GridPane.setVgrow(imageCanvas, Priority.ALWAYS);

            final Vocabulary vocabulary = Vocabulary.getResources(getLocale());
            final Label rangeLabel = new Label(vocabulary.getLabel(Vocabulary.Keys.ValueRange));
            final Label meanLabel  = new Label(vocabulary.getLabel(Vocabulary.Keys.MeanValue));
            rangeLabel.setLabelFor(sampleValueRange = new Label());
            meanLabel .setLabelFor(meanValue = new Label());

            Insets insets = new Insets(9, 0, 0, 9);
            rangeLabel.setPadding(insets);
            sampleValueRange.setPadding(insets);
            insets = new Insets(3, 0, 9, 9);
            meanLabel.setPadding(insets);
            meanValue.setPadding(insets);

            imagePane = Styles.createControlGrid(1, rangeLabel, meanLabel);
            imagePane.getChildren().add(imageCanvas);
            imagePane.setPadding(Insets.EMPTY);
            imagePane.setVgap(0);
            imagePane.setHgap(0);
            imageView = node;
        }
        final ImageConverter converter = new ImageConverter(image, visibleImageBounds, node, imageCanvas);
        if (converter.needsRun(boundsChanged)) {
            converter.setOnSucceeded((e) -> taskCompleted(converter.getValue()));
            converter.setOnFailed((e) -> {
                taskCompleted(null);
                view.set(setText(e.getSource().getException()));
            });
            /*
             * If an image rendering is already in progress, we will wait for its completion before to start
             * a new background thread. That way, if many `setImage(…)` invocations happen before the current
             * rendering finished, we will start only one new rendering process instead of as many processes
             * as they were `setImage(…)` invocations.
             */
            if (runningTask != null) {
                pendingTask = converter;
            } else {
                runningTask = converter;
                BackgroundThreads.execute(converter);
            }
        }
        return imagePane;
    }

    /**
     * Invoked when {@link #runningTask} completed its work, either successfully or with a failure.
     * This method updates the text containing statistic values in the status bar below the image.
     * If new rendering request happened during the time the task was running, start the last request.
     *
     * @param  statistics  statistics for each band of the source image (before conversion to JavaFX).
     */
    private void taskCompleted(final Statistics[] statistics) {
        runningTask = pendingTask;
        pendingTask = null;
        if (runningTask != null) {
            BackgroundThreads.execute(runningTask);
        }
        /*
         * Update the status bar with statistics computed by background thread.
         */
        String range = null;
        String mean  = null;
        if (statistics != null && statistics.length != 0) {
            final Statistics s = statistics[0];
            final StringBuffer buffer = new StringBuffer();
            formats.formatPair(s.minimum(), " … ", s.maximum(), buffer);
            range = buffer.toString();

            buffer.setLength(0);
            formats.formatPair(s.mean(), " ± ", s.standardDeviation(false), buffer);

            final Vocabulary vocabulary = Vocabulary.getResources(getLocale());
            buffer.append(" (").append(vocabulary.getString(Vocabulary.Keys.StandardDeviation)).append(')');
            mean = buffer.toString();
        }
        sampleValueRange.setText(range);
        meanValue.setText(mean);
    }

    /**
     * Invoked when the image canvas size changed. If the previous canvas size was 0, the image could not be rendered
     * during the previous call to {@link #setImage(RenderedImage, boolean)}, so we need to call that method again now
     * that the image size is known. In addition we also need to move the image to canvas center.
     *
     * @param  property  the image canvas property that changed (width or height).
     * @param  oldValue  the old width or height value.
     * @param  newValue  the new width or height value.
     */
    @Override
    public void changed(final ObservableValue<? extends Number> property, final Number oldValue, final Number newValue) {
        if (value instanceof RenderedImage) {
            setImage((RenderedImage) value, false);
        }
    }

    /**
     * Clears all content. This can be used for giving a chance to the garbage collector to release memory.
     */
    public void clear() {
        value = null;
        view.set(null);
        if (textView != null) {
            textView.setText(null);
        }
        if (listView != null) {
            listView.getItems().clear();
        }
        if (imageView != null) {
            ImageConverter.clear(imageView);
        }
    }
}
