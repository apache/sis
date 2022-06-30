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

import java.util.Locale;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.geometry.Rectangle2D;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.ToolbarButton;


/**
 * A list of windows showing resources managed by {@link ResourceExplorer}.
 * Windows are created when user clicks on the "New window" button.
 * Many windows can be created for the same resource.
 * Each window can apply different styles or map projections.
 * Gestures such as zooms, pans and rotations can be applied independently
 * or synchronized between windows, at user's choice.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final class WindowManager {          // Not designed for subclassing.
    /**
     * The handler of the main window. This handler shall never be disposed.
     */
    final WindowHandler main;

    /**
     * The language of texts to show to the user.
     */
    final Locale locale;

    /**
     * Read-only list of windows showing resources managed by {@link ResourceExplorer}.
     * Items are added in this list when the user clicks on "New window" button and are
     * removed from this list when the user closes the window.
     */
    public final ObservableList<WindowHandler> windows;

    /**
     * Modifiable list where to append new windows when they are created.
     * This is the source of {@link #windows} list.
     */
    final ObservableList<WindowHandler> modifiableWindowList;

    /**
     * Creates a new window manager.
     *
     * @param  main    handler of the main window. This handler shall never be disposed.
     * @param  locale  the language of texts to show to the user.
     */
    WindowManager(final WindowHandler main, final Locale locale) {
        this.main   = main;
        this.locale = locale;
        modifiableWindowList = FXCollections.observableArrayList();
        windows = FXCollections.unmodifiableObservableList(modifiableWindowList);
    }

    /**
     * Creates a new window for the specified widget together with its toolbar.
     * The new window will be positioned in the screen center but not yet shown.
     * The window will have initially no title (title should be set by caller).
     *
     * @param  content  control that contains the data to show in a new window.
     * @return window for showing the resource. Untitled and not yet visible.
     */
    final Stage newWindow(final Region content) {
        final Stage     stage      = new Stage();
        final Button    mainWindow = new Button("\u2302\uFE0F");        // ⌂ — house
        final Button    fullScreen = new Button("\u21F1\uFE0F");        // ⇱ — North West Arrow to Corner
        final Resources localized  = Resources.forLocale(locale);
        mainWindow.setTooltip(new Tooltip(localized.getString(Resources.Keys.MainWindow)));
        fullScreen.setTooltip(new Tooltip(localized.getString(Resources.Keys.FullScreen)));
        mainWindow.setOnAction((e) -> main.show());
        fullScreen.setOnAction((e) -> stage.setFullScreen(true));
        /*
         * Add content-specific buttons. We use the "org.apache.sis.gui.ToolbarButton" property
         * as a way to transfer ToolbarButton accross packages without making this class public.
         */
        final ToolBar tools = new ToolBar(mainWindow, fullScreen);
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
         * Main content. The tools bar will be hidden in full screen mode.
         */
        final BorderPane pane = new BorderPane();
        stage.fullScreenProperty().addListener((p,o,entering) -> pane.setTop(entering ? null : tools));
        pane.setTop(tools);
        pane.setCenter(content);
        stage.setScene(new Scene(pane));
        /*
         * We use an initial size covering a large fraction of the screen because
         * this window is typically used for showing image or large tabular data.
         */
        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth (0.8 * bounds.getWidth());
        stage.setHeight(0.8 * bounds.getHeight());
        return stage;
    }
}
