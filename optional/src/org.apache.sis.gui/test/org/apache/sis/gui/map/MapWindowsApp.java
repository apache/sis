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
package org.apache.sis.gui.map;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.apache.sis.gui.coverage.CoverageCanvasApp;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.util.iso.Names;


/**
 * Shows {@link MapWindows} with random data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class MapWindowsApp extends Application implements EventHandler<ActionEvent> {
    /**
     * The instance to test.
     */
    private MapWindows windows;

    /**
     * Number of windows created, used for creating an identifier.
     */
    private int count;

    /**
     * Creates a widget viewer.
     */
    public MapWindowsApp() {
    }

    /**
     * Starts the test application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {
        launch(args);
    }

    /**
     * Creates and starts the test application.
     *
     * @param  window  where to show the application.
     */
    @Override
    public void start(final Stage window) {
        final var canvas = new Button("Add a canvas");
        final var label = new Label("Close this window for ending the application.");
        final var box = new VBox(24, canvas, label);
        box.setAlignment(Pos.CENTER);
        window.setTitle("MapWindows Test");
        window.setScene(new Scene(box));
        window.setWidth (400);
        window.setHeight(200);
        window.setX(0);
        window.setY(0);
        window.show();
        windows = new MapWindows(window);
        canvas.setOnAction(this);
    }

    /**
     * Invoked when the button is pressed.
     *
     * @param  event  the event sent by the button.
     */
    @Override
    public void handle(final ActionEvent event) {
        windows.addResource(new MemoryGridCoverageResource(
                null,       // Parent resource.
                Names.createLocalName(null, null, "Image #" + ++count),
                CoverageCanvasApp.createImage(false),
                null));     // Grid coverage processor.
    }

    /**
     * Stops background threads for allowing <abbr>JVM</abbr> to exit.
     *
     * @throws Exception if an error occurred while stopping the threads.
     */
    @Override
    public void stop() throws Exception {
        BackgroundThreads.stop();
        super.stop();
    }
}
