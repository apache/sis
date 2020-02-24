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
package org.apache.sis.gui.dataset;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Labeled;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.ToolbarButton;


/**
 * Shows features, sample values, map or coverages in a separated window.
 * The data are initially shown in the "Data" pane of {@link ResourceExplorer},
 * but may be copied in a separated, usually bigger, windows.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class DataWindow extends Stage {
    /**
     * The tools bar. Removed from the pane when going in full screen mode, and reinserted
     * when exiting full screen mode.
     *
     * @see #onFullScreen(boolean)
     */
    private final ToolBar tools;

    /**
     * Creates a new window for the given data selected in the explorer or determined by the active tab.
     * The new window will be positioned in the screen center but not yet shown.
     *
     * @param  home  the window containing the main explorer, to be the target of "home" button.
     * @param  data  the data selected by user, to show in a new window.
     */
    DataWindow(final Stage home, final SelectedData data) {
        final Region content = data.createView();
        /*
         * Build the tools bar. This bar will be hidden in full screen mode. Note that above
         * method assumes that the "home" button created below is the first one in the toolbar.
         */
        final Button mainWindow = new Button("\u2302\uFE0F");               // ⌂ — house
        mainWindow.setTooltip(new Tooltip(data.localized.getString(Resources.Keys.MainWindow)));
        mainWindow.setOnAction((e) -> {home.show(); home.toFront();});

        final Button fullScreen = new Button("\u21F1\uFE0F");               // ⇱ — North West Arrow to Corner
        fullScreen.setTooltip(new Tooltip(data.localized.getString(Resources.Keys.FullScreen)));
        fullScreen.setOnAction((event) -> setFullScreen(true));
        fullScreenProperty().addListener((source, oldValue, newValue) -> onFullScreen(newValue));

        tools = new ToolBar(mainWindow, fullScreen);
        /*
         * Add content-specific buttons. We use the "org.apache.sis.gui.ToolbarButton" property
         * as a way to transfer ToolbarButton accross packages without making this class public.
         */
        tools.getItems().addAll(ToolbarButton.remove(content));
        /*
         * After we finished adding all buttons, set the font of all of them to a larger size.
         */
        final Font font = Font.font(20);
        for (final Node node : tools.getItems()) {
            if (node instanceof Labeled) {
                ((Labeled) node).setFont(font);
            }
        }
        /*
         * Main content. After this constructor returned, caller
         * should set the width and height, then show the window.
         */
        final BorderPane pane = new BorderPane();
        pane.setTop(tools);
        pane.setCenter(content);
        setScene(new Scene(pane));
        /*
         * We use an initial size covering a large fraction of the screen because
         * this window is typically used for showing image or large tabular data.
         */
        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        setWidth (0.8 * bounds.getWidth());
        setHeight(0.8 * bounds.getHeight());
    }

    /**
     * Invoked when entering or existing the full screen mode.
     * Used for hiding/showing the toolbar when entering/exiting full screen mode.
     */
    private void onFullScreen(final boolean entering) {
        final BorderPane pane = (BorderPane) getScene().getRoot();
        pane.setTop(entering ? null : tools);
    }
}
