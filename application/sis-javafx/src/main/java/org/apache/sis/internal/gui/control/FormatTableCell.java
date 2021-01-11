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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javafx.geometry.Pos;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
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
     * The same instance can be shared by all cells in a table.
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
     * A listener for enabling automatic transition to insertion state when a digit is pressed,
     * or {@code null} if none. The same instance is shared by all cells in the same column.
     */
    private final Trigger<S> trigger;

    /**
     * Creates a new table cell for parsing and formatting values of the given type.
     *
     * @param valueType  the type of objects expected and returned by {@code format}.
     * @param format     the format to use for parsing and formatting {@code <T>} values.
     * @param trigger    listener for automatic transition to insertion state when a digit is pressed,
     *                   or {@code null} if none.
     */
    public FormatTableCell(final Class<T> valueType, final Format format, final Trigger<S> trigger) {
        this.valueType = valueType;
        this.format    = format;
        this.trigger   = trigger;
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
        String text = (trigger != null) ? trigger.initialText : null;
        if (text == null) text = getText();
        if (editor != null) {
            /*
             * If the editor background color has been changed because of an error,
             * restores the normal background.
             */
            if (backgroundToRestore != null) {
                editor.setBackground(backgroundToRestore);
                backgroundToRestore = null;
            }
            editor.setText(text);
        } else {
            editor = new TextField(text);
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
        editor.requestFocus();
        if (trigger.initialText == null) {
            editor.selectAll();
        } else {
            editor.deselect();
            editor.end();
        }
    }

    /**
     * Invoked when edition has been cancelled.
     * The current item value is reformatted.
     */
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        setText(format(getItem()));
    }

    /**
     * A key event handler that can be registered on the table for transitioning automatically to edition state
     * in the insertion row when a digit is pressed. This trigger frees user from the need to select the cell
     * before editing the value. Current implementation reacts to digit keys, which is okay for number format.
     * Future version may be extended to more keys if there is a need for that.
     *
     * <p>Note: for making easier to edit current row instead than insertion row, it is recommended to register
     * also a listener for the F2 key (same key than Excel and OpenOffice). The {@link #registerTo(TableView)}
     * convenience method does that.</p>
     *
     * @param  <S>  the type of elements contained in {@link javafx.scene.control.TableView}.
     */
    public static final class Trigger<S> implements EventHandler<KeyEvent> {
        /**
         * The column containing the cells to transition to edition state.
         */
        private final TableColumn<S,?> column;

        /**
         * A few special character to recognize in addition to digits.
         */
        private char minusSign, zeroDigit;

        /**
         * The text to initially show in the editor, or {@code null} if none.
         * If a key has been pressed, then it should be that key.
         */
        String initialText;

        /**
         * Creates a new trigger for transitioning cells in the specified column.
         *
         * @param  column  the column containing the cells to transition to edition state.
         * @param  format  the format used for formatting values.
         */
        public Trigger(final TableColumn<S,?> column, final Format format) {
            this.column = column;
            if (format instanceof DecimalFormat) {
                final DecimalFormatSymbols symbols = ((DecimalFormat) format).getDecimalFormatSymbols();
                minusSign = symbols.getMinusSign();
                zeroDigit = symbols.getZeroDigit();
            }
        }

        /**
         * Registers this trigger to the given table. This method registers also a listener on the
         * F2 key for editing cell on the current row instead than cell in the insertion row.
         * It assumes that only one column should get the focus when F2 is pressed,
         * and that column is the one given in constructor to this {@code Trigger}.
         *
         * @param  target  table where to register listeners.
         */
        public void registerTo(final TableView<S> target) {
            target.addEventHandler(KeyEvent.KEY_TYPED, this);
            target.addEventHandler(KeyEvent.KEY_PRESSED, (event) -> {
                if (event.getCode() == KeyCode.F2) {
                    final TableView<S> table = column.getTableView();
                    if (table.getEditingCell() == null) {
                        final TablePosition<?,?> cell = table.getFocusModel().getFocusedCell();
                        if (cell != null) {
                            table.edit(cell.getRow(), column);
                        }
                    }
                    event.consume();
                }
            });
        }

        /**
         * Invoked when user typed a key. If the key is one of the keys used for entering numbers
         * and if no edition is already under way, transition to edition state on the last row.
         * This method is public as an implementation side-effect and should not be invoked directly.
         *
         * @param  event  event that describe the key typed.
         */
        @Override
        public void handle(final KeyEvent event) {
            final TableView<S> table = column.getTableView();
            if (table.getEditingCell() == null) {
                final String t = event.getCharacter();
                if (t.length() == 1) {
                    final char c = t.charAt(0);
                    if ((c >= '0' && c <= '9') || c == minusSign || c == zeroDigit) {
                        final int row = table.getItems().size() - 1;
                        try {
                            initialText = t;
                            table.edit(row, column);
                        } finally {
                            initialText = null;
                        }
                        event.consume();
                    }
                }
            }
        }
    }
}
