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
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
     * The locale for this window.
     */
    private final Locale locale;

    /**
     * The tools bar. Removed from the pane when going in full screen mode,
     * and reinserted when exiting full screen mode.
     */
    private final ToolBar tools;

    /**
     * Creates a new window for the given data selected in the explorer or determined by the active tab.
     *
     * @param  home  the window containing the main explorer, to be the target of "home" button.
     * @param  data  the data selected by user, to show in a new window.
     */
    DataWindow(final Stage home, final SelectedData data) {
        this(null, data.createView(), data.localized,
                (event) -> {home.show(); home.toFront();});
        /*
         * We use an initial size covering a large fraction of the screen because
         * this window is typically used for showing image or large tabular data.
         */
        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        setWidth (0.8 * bounds.getWidth());
        setHeight(0.8 * bounds.getHeight());
    }

    /**
     * Creates a new window for the given content. The {@code home} and {@code localized} arguments
     * shall be non-null only if {@code originator} is null.
     *
     * @param  originator  the window from which this window is derived, or {@code null} if none.
     * @param  content     content of the window to create.
     * @param  localized   {@link Resources} instance provided because often know by the caller.
     * @param  home        the action to execute when user clicks on the "home" button.
     */
    private DataWindow(final DataWindow originator, final Region content, Resources localized, EventHandler<ActionEvent> home) {
        if (originator != null) {
            home = ((Button) originator.tools.getItems().get(0)).getOnAction();
            localized = Resources.forLocale(originator.locale);
        }
        locale = localized.getLocale();
        /*
         * Build the tools bar. This bar will be hidden in full screen mode.
         * Note that code above assumes that this button is the first button in the toolbar.
         */
        final Button mainWindow = new Button("\u2302\uFE0F");               // ⌂ — house
        mainWindow.setTooltip(new Tooltip(localized.getString(Resources.Keys.MainWindow)));
        mainWindow.setOnAction(home);

        final Button fullScreen = new Button("\u21F1\uFE0F");               // ⇱ — North West Arrow to Corner
        fullScreen.setTooltip(new Tooltip(localized.getString(Resources.Keys.FullScreen)));
        fullScreen.setOnAction((event) -> setFullScreen(true));
        /*
         * Hide/show the toolbar when entering/exiting full screen mode.
         */
        tools = new ToolBar(mainWindow, fullScreen);
        fullScreenProperty().addListener((source, oldValue, newValue) -> onFullScreen(newValue));

        if (originator != null) {
            final Button related = new Button("\uD83D\uDD22\uFE0F");    // Input symbol for numbers.
            related.setOnAction((event) -> {originator.show(); originator.toFront();});
            tools.getItems().add(related);
        }
        /*
         * Add content-specific buttons. We use the "org.apache.sis.gui.ToolbarButton" property
         * as a way to transfer ToolbarButton accross packages without making this class public.
         */
        final ToolbarButton[] contentButtons = (ToolbarButton[]) content.getProperties().remove(ToolbarButton.PROPERTY_KEY);
        if (contentButtons != null) {
            for (final ToolbarButton tb : contentButtons) {
                final Button b = new Button(tb.getText());
                b.setOnAction(new Related(this, tb));
                tools.getItems().add(b);
            }
        }
        final Font bf = Font.font(20);
        for (final Node node : tools.getItems()) {
            ((Button) node).setFont(bf);
        }
        /*
         * Main content. After this constructor returned, caller
         * should set the width and height, then show the window.
         */
        final BorderPane pane = new BorderPane();
        pane.setTop(tools);
        pane.setCenter(content);
        setScene(new Scene(pane));
    }

    /**
     * Manage the creation and display of another window related to the enclosing {@link DataWindow}.
     * For example is the enclosing window shown the tabular data, the window created by this class
     * may shown the map.
     */
    private static final class Related implements EventHandler<ActionEvent> {
        /**
         * The X and Y location of the new window relative to the original window.
         */
        private static final int LOCATION = 40;

        /**
         * The object that can create the window.
         * This is set to {@code null} when no longer needed.
         */
        private ToolbarButton creator;

        /**
         * The related window. If {@link #creator} is non-null, then this is the original window that
         * created this {@code Related} instance. If {@link #creator} is null, this is the new window
         * that has been created.
         */
        private DataWindow window;

        /**
         * Prepares an action for invoking {@code creator.createView()} when first needed.
         */
        Related(final DataWindow originator, final ToolbarButton creator) {
            this.window  = originator;
            this.creator = creator;
        }

        /**
         * Invoked when the user clicked on the button for showing the window managed by this {@code Related}.
         * On the first click, the related window is created. On subsequent click, that window is brought to front.
         */
        @Override
        public void handle(final ActionEvent event) {
            if (creator != null) {
                final String title = window.getTitle();     // TODO! make the title different.
                final DataWindow rw = new DataWindow(window, creator.createView(), null, null);
                rw.setTitle(title);
                rw.setWidth (window.getWidth());
                rw.setHeight(window.getHeight());
                rw.setX(window.getX() + LOCATION);
                rw.setY(window.getY() + LOCATION);
                window  = rw;                   // Set only on success.
                creator = null;
            }
            window.show();
            window.toFront();
        }
    }

    /**
     * Invoked when entering or existing the full screen mode.
     */
    private void onFullScreen(final boolean entering) {
        final BorderPane pane = (BorderPane) getScene().getRoot();
        pane.setTop(entering ? null : tools);
    }
}
