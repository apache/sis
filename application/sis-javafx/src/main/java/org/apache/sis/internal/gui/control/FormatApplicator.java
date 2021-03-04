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
package org.apache.sis.internal.gui.control;

import java.math.BigDecimal;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import javafx.util.StringConverter;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.InvalidationListener;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.util.CharSequences;


/**
 * Parses and formats {@link TextField} content with a {@link Format}.
 * The same {@code FormatApplicator} can be used for many {@link TextField} instances.
 *
 * <p>The interfaces implemented by this classes are for registering listeners on
 * {@link TextField} instances. The set of interfaces may change in any future version.
 * Registrations should be done by calls to {@link #setListenersOn(TextField)} only.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <T>  the type of objects expected and returned by {@link #format}.
 *
 * @since 1.1
 * @module
 */
final class FormatApplicator<T> extends StringConverter<T>
        implements EventHandler<ActionEvent>, ChangeListener<Boolean>
{
    /**
     * The type of objects expected and returned by {@link #format}.
     */
    private final Class<T> valueType;

    /**
     * The format to use for parsing and formatting {@code <T>} values.
     * The same instance can be shared by all cells in a table.
     */
    final Format format;

    /**
     * Listener to notify when a {@link TextField} value changed.
     * We track only the {@link TextField} instances given to {@link #setListenersOn(TextField)}.
     */
    InvalidationListener listener;

    /**
     * Creates a new handler for parsing and formatting values of the given type.
     *
     * @param valueType  the type of objects expected and returned by {@code format}.
     * @param format     the format to use for parsing and formatting {@code <T>} values.
     */
    public FormatApplicator(final Class<T> valueType, final Format format) {
        this.valueType = valueType;
        this.format    = format;
    }

    /**
     * Creates an instance using {@link NumberFormat}. If the {@linkplain DecimalFormat format is decimal},
     * then it will parse {@link BigDecimal} values. The intent is to allow arithmetic operations without
     * rounding errors that may surprise the user, for example if we need to compute {@code n * scale}
     * where <var>scale</var> has been specified by user as 0.1.
     *
     * @return an instance for parsing and formatting numbers.
     */
    public static FormatApplicator<Number> createNumberFormat() {
        final FormatApplicator<Number> f = new FormatApplicator<>(Number.class, NumberFormat.getInstance());
        if (f.format instanceof DecimalFormat) {
            ((DecimalFormat) f.format).setParseBigDecimal(true);
        }
        return f;
    }

    /**
     * Sets listeners on the given editor. The text will be parsed when the field lost focus or when
     * user presses "Enter" and the result will be stored using {@link TextField#setUserData(Object)}.
     *
     * @param  editor  the editor on which to set listeners.
     */
    public final void setListenersOn(final TextField editor) {
        editor.focusedProperty().addListener(this);
        editor.setOnAction(this);
    }

    /**
     * Returns {@code true} if the given item is null or {@link Double#NaN}.
     * Future version may give some control on the values to filter, if there is a need.
     */
    private static boolean isNil(final Object item) {
        return (item == null) || ((item instanceof Double) && ((Double) item).isNaN());
    }

    /**
     * Returns the given item as text, or {@code null} if none.
     * Current implementation does not format {@link Double#NaN} values.
     * Future version may give some control on the values to filter, if there is a need.
     *
     * @param  item    the value to format, or {@code null}.
     * @return formatted value, or {@code null} if the given item is null or NaN.
     */
    @Override
    public final String toString(final T item) {
        return isNil(item) ? null : format.format(item);
    }

    /**
     * Formats the given item as text and write the result in the given editor.
     *
     * @param  editor  the editor where to write the formatted value.
     * @param  item    the value to format, or {@code null}.
     */
    public final void format(final TextField editor, final T item) {
        editor.setText(toString(item));
        setErrorFlag(editor, false);
    }

    /**
     * Parses the given text. This method is defined for compliance with {@link StringConverter}
     * contract, but {@link #parse(TextField)} should be used instead.
     *
     * @param  text  the text to parse, or {@code null}.
     * @return the parsed value, or {@code null} if the given text was null.
     * @throws IllegalArgumentException if the given text can not be parsed.
     */
    @Override
    public final T fromString(String text) {
        if (text == null || (text = text.trim()).isEmpty()) {
            return null;
        }
        try {
            return valueType.cast(format.parseObject(text));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Parses the given editor content and, if the parsing is successful, returns the value.
     * If the parsing failed, the cell background color is changed and the caret is moved to
     * the error position.
     *
     * @param   editor  the editor containing the text to parse.
     * @return  the parsed value, or {@code null} if parsing failed.
     */
    public final T parse(final TextField editor) {
        String text = editor.getText();
        if (text != null) {
            final int end = CharSequences.skipTrailingWhitespaces(text, 0, text.length());
            final int start = CharSequences.skipLeadingWhitespaces(text, 0, end);
            if (start < end) {
                final ParsePosition pos = new ParsePosition(start);
                final T value = valueType.cast(format.parseObject(text, pos));
                final int stop = pos.getIndex();
                if (stop >= end && !isNil(value)) {
                    setErrorFlag(editor, false);
                    return value;
                }
                editor.positionCaret(value != null ? stop : pos.getErrorIndex());
            }
        }
        /*
         * If `format` did not used all characters, either we have a parsing error
         * or the last characters have been ignored (which we consider as an error).
         * The 2 cases can be distinguished by `value` being null or not.
         */
        if (text != null && !text.isEmpty()) {
            setErrorFlag(editor, true);
        }
        return null;
    }

    /**
     * Parses the given editor content and stores the result as a user object.
     */
    private void parseAndStore(final TextField editor) {
        final T newValue = parse(editor);
        editor.setUserData(newValue);
        if (listener != null) {
            listener.invalidated(editor.getProperties());
        }
    }

    /**
     * Invoked when user presses {@code Enter} in a {@link TextField}.
     * This method is public as a listener implementation side effect
     * and should not be invoked directly.
     *
     * @param  event  information about the event, such as the source text field.
     *
     * @see #setListenersOn(TextField)
     */
    @Override
    public void handle(final ActionEvent event) {
        parseAndStore((TextField) event.getSource());
    }

    /**
     * Invoked when a {@link TextField} get or lost focus.
     * This method is public as a listener implementation side effect
     * and should not be invoked directly.
     *
     * @param  property  the {@link TextField#focusedProperty()}.
     * @param  oldValue  the old "is focused" value.
     * @param  newValue  the new "is focused" value.
     *
     * @see #setListenersOn(TextField)
     */
    @Override
    public void changed(final ObservableValue<? extends Boolean> property, final Boolean oldValue, final Boolean newValue) {
        final TextField editor = (TextField) ((ReadOnlyProperty<?>) property).getBean();
        if (newValue) {
            setErrorFlag(editor, false);
        } else {
            parseAndStore(editor);
        }
    }

    /**
     * Declares whether content of given editor has an error.
     *
     * @param  editor  the editor on which to set the error flag.
     * @param  flag    {@code true} if editor content has an error, or {@code false} if valid.?
     */
    private static void setErrorFlag(final TextField editor, final boolean flag) {
        editor.pseudoClassStateChanged(Styles.ERROR, flag);
    }
}
