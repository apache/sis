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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
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
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.math.Statistics;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A viewer for property value. The property may be of various class (array, image, <i>etc</i>).
 * If the type is unrecognized, the property is shown as text.
 *
 * <p>This class extends {@link CompoundFormat} for implementation convenience only.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
@SuppressWarnings({"serial","CloneableImplementsClone"})            // Not intended to be serialized.
public final class PropertyView extends CompoundFormat<Object> {
    /**
     * The current property value. This is used for detecting changes.
     */
    private Object value;

    /**
     * The node used for showing {@link #value}.
     */
    private final ObjectProperty<Node> view;

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
     */
    private final Pane imageCanvas;

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
     * If a work is under progress, that work. Otherwise {@code null}.
     */
    private Task<?> runningTask;

    /**
     * Creates a new property view.
     *
     * @param  locale      the locale for numbers formatting.
     * @param  view        the property where to set the node showing the value.
     * @param  background  the image background color, or {@code null} if none.
     */
    public PropertyView(final Locale locale, final ObjectProperty<Node> view, final ObjectProperty<Background> background) {
        super(locale, null);
        this.view = view;
        imageCanvas = new Pane();
        if (background != null) {
            imageCanvas.backgroundProperty().bind(background);
        }
    }

    /**
     * Required by {@link CompoundFormat} but not used.
     *
     * @return the base type of values formatted by this {@code PropertyView} instance.
     */
    @Override
    public Class<? extends Object> getValueType() {
        return Object.class;
    }

    /**
     * Unsupported operation.
     *
     * @param  text ignored.
     * @param  pos  ignored.
     * @return never return.
     * @throws ParseException always thrown.
     */
    @Override
    public Object parse(CharSequence text, ParsePosition pos) throws ParseException {
        throw new ParseException(null, 0);
    }

    /**
     * Formats the given property value. Current implementation requires {@code toAppendTo}
     * to be an instance of {@link StringBuffer}. This method is not intended to be invoked
     * outside internal usage.
     *
     * @param  value       the property value to format.
     * @param  toAppendTo  where to append the property value.
     */
    @Override
    public void format(final Object value, final Appendable toAppendTo) throws IOException {
        final Format f = getFormat(value.getClass());
        if (f != null) {
            f.format(value, (StringBuffer) toAppendTo, new FieldPosition(0));
        } else {
            toAppendTo.append(value.toString());
        }
    }

    /**
     * Formats a single value. This method does the same work than the inherited
     * {@link #format(Object)} final method but in a more efficient way.
     */
    private String formatValue(final Object value) {
        final Format f = getFormat(value.getClass());
        if (f == null) {
            return value.toString();
        } else if (value instanceof Number) {
            return Numerics.useScientificNotationIfNeeded(f, value, Format::format);
        } else {
            return f.format(value);
        }
    }

    /**
     * Formats the given value, using scientific notation if needed.
     */
    private static void format(final Format f, final double value, final StringBuffer buffer, final FieldPosition pos) {
        Numerics.useScientificNotationIfNeeded(f, value, (nf,v) -> {nf.format(v, buffer, pos); return null;});
    }

    /**
     * Sets the view to the given value.
     *
     * @param  newValue       the new value (may be {@code null}).
     * @param  visibleBounds  if the property is an image, currently visible region. Can be {@code null}.
     */
    public void set(final Object newValue, final Rectangle visibleBounds) {
        if (newValue != value || !Objects.equals(visibleBounds, visibleImageBounds)) {
            if (runningTask != null) {
                runningTask.cancel(BackgroundThreads.NO_INTERRUPT_DURING_IO);
                runningTask = null;
            }
            visibleImageBounds = visibleBounds;
            final Node content;
            if (newValue == null) {
                content = null;
            } else if (newValue instanceof RenderedImage) {
                content = setImage((RenderedImage) newValue);
            } else if (newValue instanceof Throwable) {
                content = setText((Throwable) newValue);
            } else if (newValue instanceof Collection<?>) {
                content = setList(((Collection<?>) newValue).toArray());
            } else if (newValue.getClass().isArray()) {
                content = setList(newValue);
            } else {
                content = setText(formatValue(newValue));
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
        final StringWriter out = new StringWriter();
        ex.printStackTrace(new PrintWriter(out));
        return setText(out.toString());
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
            list[i] = formatValue(Array.get(array, i));
        }
        listView.getItems().setAll(list);
        return node;
    }

    /**
     * Sets the property value to the given image.
     */
    private Node setImage(final RenderedImage image) {
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
        final ImageConverter converter = new ImageConverter(image, visibleImageBounds, node);
        converter.setOnSucceeded((e) -> taskCompleted(converter.getValue()));
        converter.setOnFailed((e) -> {
            taskCompleted(null);
            view.set(setText(e.getSource().getException()));
        });
        runningTask = converter;
        BackgroundThreads.execute(converter);
        return imagePane;
    }

    /**
     * Invoked when {@link #runningTask} completed its work, either successfully or with a failure.
     */
    private void taskCompleted(final Statistics[] statistics) {
        runningTask  = null;
        String range = null;
        String mean  = null;
        if (statistics != null && statistics.length != 0) {
            final Statistics s = statistics[0];
            final FieldPosition pos = new FieldPosition(0);
            final StringBuffer buffer = new StringBuffer();
            final Format f = getFormat(Number.class);
            format(f, s.minimum(), buffer, pos); buffer.append(" … ");
            format(f, s.maximum(), buffer, pos);
            range = buffer.toString();

            buffer.setLength(0);
            format(f, s.mean(), buffer, pos); buffer.append(" ± ");
            format(f, s.standardDeviation(false), buffer, pos);

            final Vocabulary vocabulary = Vocabulary.getResources(getLocale());
            buffer.append(" (").append(vocabulary.getString(Vocabulary.Keys.StandardDeviation)).append(')');
            mean = buffer.toString();
        }
        sampleValueRange.setText(range);
        meanValue.setText(mean);
    }

    /**
     * Clears all content. This can be used for giving a chance to the garbage collector to release memory.
     */
    public void clear() {
        value = null;
        view.set(null);
        if (textView  != null) textView .setText (null);
        if (imageView != null) imageView.setImage(null);
    }
}
