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

import java.text.Format;
import java.text.ParsePosition;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.util.CharSequences;


/**
 * A table cell where values are parsed and formatted using {@link java.text} API.
 * This is equivalent to using {@link javafx.scene.control.cell.TextFieldTableCell}
 * together with {@link javafx.util.converter.FormatStringConverter}, but with more
 * control on parsing errors.
 *
 * <p>Subclasses may need to register a listener with {@code TableColumn.setOnEditCommit(…)}
 * for copying the value in the object stored by {@link javafx.scene.control.TableView}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <S>  the type of elements contained in {@link javafx.scene.control.TableView}.
 * @param  <T>  the type of elements contained in {@link javafx.scene.control.TableColumn}.
 *
 * @since 1.1
 * @module
 */
public final class FormatTableCell<S,T> extends TableCell<S,T> {
    /**
     * The type of objects expected and returned by {@link #format}.
     */
    private final Class<T> valueType;

    /**
     * The format to use for parsing and formatting {@code <T>} values.
     */
    private final Format format;

    /**
     * The control to use during edition.
     */
    private TextField editor;

    /**
     * The {@link #editor} background to restore after successful parsing or cancellation.
     * This is non-null only if the background has been changed for signaling a parsing error.
     */
    private Background backgroundToRestore;

    /**
     * Creates a new table cell for parsing and formatting values of the given type.
     *
     * @param valueType  the type of objects expected and returned by {@code format}.
     * @param format     the format to use for parsing and formatting {@code <T>} values.
     */
    public FormatTableCell(final Class<T> valueType, final Format format) {
        this.valueType = valueType;
        this.format    = format;
        setAlignment(Pos.CENTER_LEFT);
    }

    /**
     * Returns {@code true} if the given item is null or {@link Double#NaN}.
     * Future version may give some control on the values to filter, if there is a need.
     */
    private static boolean isNil(final Object item) {
        return (item == null) || ((item instanceof Double) && ((Double) item).isNaN());
    }

    /**
     * Returns the given item as text, or {@code null} if none. The text will be given
     * to different control depending on whether this cell is in editing state or not.
     *
     * <p>Current implementation does not format {@link Double#NaN} values.
     * Future version may give some control on the values to filter, if there is a need.</p>
     */
    private String format(final T item) {
        return isNil(item) ? null : format.format(item);
    }

    /**
     * Parses the current editor content and, if the parsing is successful, commit.
     * If the parsing failed, the cell background color is changed and the caret is
     * moved to the error position.
     */
    private void parseAndCommit() {
        String text = editor.getText();
        if (text != null) {
            final int end = CharSequences.skipTrailingWhitespaces(text, 0, text.length());
            final int start = CharSequences.skipLeadingWhitespaces(text, 0, end);
            if (start < end) {
                final ParsePosition pos = new ParsePosition(start);
                final T value = valueType.cast(format.parseObject(text, pos));
                final int stop = pos.getIndex();
                if (stop >= end && !isNil(value)) {
                    commitEdit(value);
                    return;
                }
                editor.positionCaret(value != null ? stop : pos.getErrorIndex());
            }
        }
        /*
         * If `format` did not used all characters, either we have a parsing error
         * or the last characters have been ignored (which we consider as an error).
         * The 2 cases can be distinguished by `value` being null or not.
         */
        if (backgroundToRestore == null) {
            backgroundToRestore = editor.getBackground();
            if (backgroundToRestore == null) {
                backgroundToRestore = Background.EMPTY;
            }
        }
        editor.setBackground(Styles.ERROR_BACKGROUND);
    }

    /**
     * Invoked when a new value needs to be shown in the cell. The new value will be formatted in either
     * the {@linkplain #editor} or in the label, depending if this cell is in editing state or not.
     *
     * @param  item   the new value to show, or {@code null} if none.
     * @param  empty  whether the cell is used for rendering an empty row.
     */
    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        String text = null;
        TextField g = null;
        if (!empty) {
            if (isEditing()) {
                g = editor;
                if (g != null) {
                    g.setText(format(item));
                }
            } else if (item != null) {
                text = format(item);
            }
        }
        setText(text);
        setGraphic(g);
    }

    /**
     * Transitions to editing state. If the {@linkplain #editor} has not yet been created, it is created now.
     * Then the editor is given the current text and is shown in temporary replacement of the cell text.
     * The full text is selected for allowing easy replacement.
     */
    @Override
    public void startEdit() {
        super.startEdit();
        if (editor != null) {
            /*
             * If the editor background color has been changed because of an error,
             * restores the normal background.
             */
            if (backgroundToRestore != null) {
                editor.setBackground(backgroundToRestore);
                backgroundToRestore = null;
            }
            editor.setText(getText());
        } else {
            editor = new TextField(getText());
            editor.setOnAction((event) -> {
                event.consume();
                parseAndCommit();
            });
            editor.setOnKeyReleased((event) -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    event.consume();
                    cancelEdit();
                }
            });
        }
        setText(null);
        setGraphic(editor);
        editor.selectAll();
        editor.requestFocus();
    }

    /**
     * Invoked when edition has been cancelled.
     */
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        setText(format(getItem()));
    }
}
