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
     * The tools bar. Removed from the pane when going in full screen mode, and reinserted
     * when exiting full screen mode. The first button in this toolbar shall be the "home"
     * button (for showing the main window on front) — if a different position is desired,
     * revisit {@link #getHomeButton()}.
     *
     * @see #getHomeButton()
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
        this(data.createView(), data.localized);
        getHomeButton().setOnAction((e) -> {home.show(); home.toFront();});
        /*
         * We use an initial size covering a large fraction of the screen because
         * this window is typically used for showing image or large tabular data.
         */
        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        setWidth (0.8 * bounds.getWidth());
        setHeight(0.8 * bounds.getHeight());
    }

    /**
     * Returns the "home" button. This implementation assumes that "home" is the first button on the toolbar.
     * This method is kept close to the following constructor for making easier to verify its assumption.
     */
    private Button getHomeButton() {
        return ((Button) tools.getItems().get(0));
    }

    /**
     * Creates a new window for the given content. After this constructor returned,
     * caller should set an action on {@link #getHomeButton()} and set the window size.
     *
     * @param  content    content of the window to create.
     * @param  localized  {@link Resources} instance, provided because often known by the caller.
     */
    private DataWindow(final Region content, final Resources localized) {
        /*
         * Build the tools bar. This bar will be hidden in full screen mode. Note that above
         * method assumes that the "home" button created below is the first one in the toolbar.
         */
        final Button mainWindow = new Button("\u2302\uFE0F");               // ⌂ — house
        mainWindow.setTooltip(new Tooltip(localized.getString(Resources.Keys.MainWindow)));

        final Button fullScreen = new Button("\u21F1\uFE0F");               // ⇱ — North West Arrow to Corner
        fullScreen.setTooltip(new Tooltip(localized.getString(Resources.Keys.FullScreen)));
        fullScreen.setOnAction((event) -> setFullScreen(true));
        fullScreenProperty().addListener((source, oldValue, newValue) -> onFullScreen(newValue));

        tools = new ToolBar(mainWindow, fullScreen);
        /*
         * Add content-specific buttons. We use the "org.apache.sis.gui.ToolbarButton" property
         * as a way to transfer ToolbarButton accross packages without making this class public.
         */
        for (final ToolbarButton specialized : ToolbarButton.remove(content)) {
            final Node button = specialized.createButton(localized);
            if (specialized instanceof ToolbarButton.RelatedWindow) {
                ((Button) button).setOnAction(new Related(this, (ToolbarButton.RelatedWindow) specialized));
            }
            tools.getItems().add(button);
        }
        /*
         * After we finished adding all buttons, set the font of all of them to a larger size.
         */
        final Font font = Font.font(20);
        for (final Node node : tools.getItems()) {
            if (node instanceof Button) {
                ((Button) node).setFont(font);
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
    }

    /**
     * Manage the creation and display of another window related to the enclosing {@link DataWindow}.
     * For example is the enclosing window shows the tabular data, the window created by this class
     * may show the map.
     */
    private static final class Related implements EventHandler<ActionEvent> {
        /**
         * The X and Y location of the new window relative to the original window.
         */
        private static final int LOCATION = 40;

        /**
         * The object for creating the window on the first time that the user clicks on the button.
         * This is set to {@code null} when no longer needed, in which case {@link #window} should
         * be a reference to the window that we created.
         */
        private ToolbarButton.RelatedWindow creator;

        /**
         * The related window. If {@link #creator} is non-null, then this is the original window that
         * created this {@code Related} instance. If {@link #creator} is null, this is the new window
         * that this class has created.
         */
        private DataWindow window;

        /**
         * Prepares an action for invoking {@code creator.createView()} when first needed.
         * The given {@code originator} window will be the target of the "back" button.
         *
         * @param  originator  the original window that created this {@code Related} instance.
         * @param  creator     a factory for the new window to create when the user request it.
         */
        Related(final DataWindow originator, final ToolbarButton.RelatedWindow creator) {
            this.window  = originator;
            this.creator = creator;
        }

        /**
         * Invoked when the user clicked on the button for showing the window managed by this {@code Related} object.
         * On the first click, the related window is created. On subsequent clicks, that window is brought to front.
         */
        @Override
        public void handle(final ActionEvent event) {
            if (creator != null) {
                final DataWindow originator = window;
                final Resources  localized  = Resources.forLocale(null);
                final Region     content    = creator.createView();
                final Button     backButton = creator.createBackButton(localized);
                backButton.setOnAction((e) -> {originator.show(); originator.toFront();});
                ToolbarButton.insert(content, new ToolbarButton() {
                    @Override public Node createButton(Resources localized) {
                        return backButton;
                    }
                });
                final DataWindow rw = new DataWindow(content, localized);
                rw.getHomeButton().setOnAction(originator.getHomeButton().getOnAction());
                rw.setTitle (originator.getTitle());
                rw.setWidth (originator.getWidth());
                rw.setHeight(originator.getHeight());
                rw.setX(originator.getX() + LOCATION);
                rw.setY(originator.getY() + LOCATION);
                window  = rw;                                       // Set only on success.
                creator = null;
            }
            window.show();
            window.toFront();
        }
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
