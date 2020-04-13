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

import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;


/**
 * A central place where to store all colors used by SIS application.
 * This provides a single place to revisit if we learn more about how
 * to make those color more dynamic with JavaFX styling.
 *
 * <p>This class also opportunistically provides a few utility methods
 * related to appearance.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class Styles {
    /**
     * Initial position of divider in split panes. This is used in data windows,
     * but not necessarily in the main window (the resource explorer).
     */
    public static final double INITIAL_SPLIT = 200;

    /**
     * Approximate size of vertical scroll bar.
     */
    public static final int SCROLLBAR_WIDTH = 20;

    /**
     * "Standard" height of table rows. Can be approximate.
     */
    public static final double ROW_HEIGHT = 30;

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
    public static final Color LOADING_TEXT = Color.BLUE;

    /**
     * Color of text for authority codes.
     */
    public static final Color CODE_TEXT = Color.LIGHTSLATEGREY;

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
     * The background for cell having an illegal input value.
     */
    public static final Background ERROR_BACKGROUND =
            new Background(new BackgroundFill(Color.LIGHTPINK, null, null));

    /**
     * The Unicode character to put in a button for requesting more information about an error.
     */
    public static final String ERROR_DETAILS = "ℹ";

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
            Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), caller, method, error);
        }
        return image;
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
