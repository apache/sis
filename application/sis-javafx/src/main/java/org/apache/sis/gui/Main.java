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
package org.apache.sis.gui;

import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.sis.gui.metadata.ResourceView;


/**
 * Entry point for Apache SIS application.
 *
 * @author  Smaniotto Enzo
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class Main extends Application {
    /**
     * The primary stage onto which the application scene is set.
     */
    private Stage window;

    /**
     * The main content of this application. For now this is the metadata viewer.
     * In a future version it will be another component.
     */
    private ResourceView content;

    /**
     * Creates a new Apache SIS application.
     */
    public Main() {
    }

    /**
     * Invoked by JavaFX for starting the application.
     * This method is called on the JavaFX Application Thread.
     *
     * @param window  the primary stage onto which the application scene will be be set.
     */
    @Override
    public void start(final Stage window) {
        this.window = window;
        /*
         * Configure the menu bar.
         */
        final MenuBar menus = new MenuBar();
        final Menu file = new Menu("File");                                 // TODO: localize
        menus.getMenus().add(file);

        final MenuItem open = new MenuItem("Open…");                        // TODO: localize
        open.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        open.setOnAction(e -> open());

        final MenuItem exit = new MenuItem("Exit");                         // TODO: localize
        exit.setOnAction(e -> Platform.exit());
        file.getItems().addAll(open, new SeparatorMenuItem(), exit);
        /*
         *
         */
        content = new ResourceView();
        final BorderPane pane = new BorderPane();
        pane.setTop(menus);
        pane.setCenter(content.pane);
        Scene scene = new Scene(pane);
        window.setTitle("Apache Spatial Information System");
        window.setScene(scene);
        window.setWidth(800);
        window.setHeight(650);
        window.show();
    }

    /**
     * Invoked when the user selected "File / open" menu.
     */
    private void open() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open data file");                         // TODO: localize
        File fileByChooser = fileChooser.showOpenDialog(window);
        if (fileByChooser != null) {
            if (fileByChooser.isDirectory()) {
                content.openDirectory(fileByChooser);
            } else {
                content.openFile(fileByChooser);
            }
        }
    }

    /**
     * Starts the Apache SIS application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {
        launch(args);
    }
}
