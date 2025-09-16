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

import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import org.apache.sis.util.logging.Logging;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * A central place where to store some appearance choices such as colors used by SIS application.
 * This provides a single place to revisit if we learn more about how to make those choices more
 * configurable with JavaFX styling.
 *
 * <p>This class also opportunistically provides a few utility methods related to appearance.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Styles {
    /**
     * Approximate size of vertical scroll bar.
     */
    public static final int SCROLLBAR_WIDTH = 20;

    /**
     * "Standard" height of table rows. Can be approximate.
     */
    public static final int ROW_HEIGHT = 30;

    /**
     * Usual color of text.
     */
    public static final Color NORMAL_TEXT = Color.BLACK;

    /**
     * Color of text in a selection.
     */
    public static final Color SELECTED_TEXT = Color.WHITE;

    /**
     * Color of the text saying that data are in process of being loaded.
     */
    public static final Color LOADING_TEXT = Color.STEELBLUE;

    /**
     * Color of text for authority codes.
     */
    public static final Color CODE_TEXT = Color.LIGHTSLATEGREY;

    /**
     * Color of text used for outdated information while a background thread is refreshing data.
     */
    public static final Color OUTDATED_TEXT = Color.GRAY;

    /**
     * Color of text shown in place of data that we failed to load.
     */
    public static final Color ERROR_TEXT = Color.RED;

    /**
     * Color for header of expanded rows in {@link org.apache.sis.gui.dataset.FeatureTable}.
     */
    public static final Color EXPANDED_ROW = Color.GAINSBORO;

    /**
     * Color for border grouping some controls together.
     */
    public static final Color GROUP_BORDER = Color.SILVER;

    /**
     * Color for background of a selection.
     */
    public static final Color SELECTION_BACKGROUND = Color.LIGHTBLUE;

    /**
     * Identifies the CSS pseudo-class from {@code "org/apache/sis/gui/stylesheet.css"}
     * to apply if a {@link javafx.scene.control.TextInputControl} has an invalid value.
     */
    public static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    /**
     * The Unicode character to put in a button for requesting more information about an error.
     * The symbol is {@value}.
     */
    public static final String ERROR_DETAILS_ICON = "\u2139\uFE0F";     // ℹ

    /**
     * The Unicode character to put in a label for representing a warning.
     * The symbol is {@value}.
     */
    public static final String WARNING_ICON = "\u26A0\uFE0F";           // ⚠

    /**
     * Do not allow instantiation of this class.
     */
    private Styles() {
    }

    /**
     * Loads an image of the given name.
     * This method should be used only for relatively small images.
     *
     * @param  caller  class to use for fetching resource. Also used for logging.
     * @param  method  the method invoking this method. Used only in case of logging.
     * @param  file    filename of the image to load.
     * @return the image, or {@code null} if the operation failed.
     */
    public static Image loadIcon(final Class<?> caller, final String method, final String file) {
        Image image;
        Exception error;
        try (InputStream in = caller.getResourceAsStream(file)) {
            image = new Image(in);
            error = image.getException();
        } catch (IOException e) {
            image = null;
            error = e;
        }
        if (error != null) {
            Logging.unexpectedException(LOGGER, caller, method, error);
        }
        return image;
    }

    /**
     * Space between a group of controls and the border encompassing the group.
     */
    public static final Insets FORM_INSETS = new Insets(12);

    /**
     * Creates a grid pane of two columns and an arbitrary number of rows.
     * Each row contains a (label, control) pair, with all growths and shrinks applied on the second column.
     * The controls must be associated to the given labels by {@link Label#getLabelFor()}.
     * If a label is {@code null}, then no row is created for that label.
     *
     * @param  row       index of the first row. If different than 0, then it is caller responsibility
     *                   to provide controls for all rows before the specified index.
     * @param  controls  (label, control) pairs to layout in rows.
     * @return a pane with each (label, control) pair on a row.
     */
    public static GridPane createControlGrid(int row, final Label... controls) {
        final GridPane gp = new GridPane();
        final ColumnConstraints labelColumn   = new ColumnConstraints();
        final ColumnConstraints controlColumn = new ColumnConstraints();
        labelColumn  .setHgrow(Priority.NEVER);
        controlColumn.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().setAll(labelColumn, controlColumn);
        gp.setPadding(FORM_INSETS);
        gp.setVgap(9);
        gp.setHgap(9);
        for (final Label label : controls) {
            if (label != null) {
                final Node control = label.getLabelFor();
                GridPane.setConstraints(label,   0, row);
                GridPane.setConstraints(control, 1, row);
                gp.getChildren().addAll(label, control);
                label.setMinWidth(Label.USE_PREF_SIZE);
                row++;
            }
        }
        return gp;
    }

    /**
     * Sets all rows in the given grid pane to the same height.
     *
     * @param  gp  the grid pane in which to set row constraints.
     */
    public static void setAllRowToSameHeight(final GridPane gp) {
        final RowConstraints[] constraints = new RowConstraints[gp.getRowCount()];
        final RowConstraints c = new RowConstraints();
        c.setPercentHeight(100.0 / constraints.length);
        Arrays.fill(constraints, c);
        gp.getRowConstraints().setAll(constraints);
    }
}
