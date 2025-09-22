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
package org.apache.sis.gui.coverage;

import java.util.Random;
import java.awt.image.DataBuffer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.sis.gui.internal.BackgroundThreads;

// Test dependencies
import org.apache.sis.image.TiledImageMock;


/**
 * Shows {@link GridView} with random data. The image will have small tiles of size
 * {@value #TILE_WIDTH}×{@value #TILE_HEIGHT}. The image will artificially fails to
 * provide some tiles in order to test error controls.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GridViewApp extends Application {
    /**
     * Size of the artificial tiles. Should be small enough so we can have many of them.
     * Width and height should be different in order to increase the chance to see bugs
     * if some code confuse them.
     */
    private static final int TILE_WIDTH = 10, TILE_HEIGHT = 15;

    /**
     * Creates a widget viewer.
     */
    public GridViewApp() {
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
        final var view = new GridView();
        final var pane = new BorderPane(view);
        window.setTitle("GridView Test");
        window.setScene(new Scene(pane));
        window.setWidth (400);
        window.setHeight(500);
        window.show();
        view.setImage(createImage());
    }

    /**
     * Stops background threads for allowing JVM to exit.
     *
     * @throws Exception if an error occurred while stopping the threads.
     */
    @Override
    public void stop() throws Exception {
        BackgroundThreads.stop();
        super.stop();
    }

    /**
     * Creates a dummy image for testing purpose. Some tiles will
     * have artificial errors in order to see the error controls.
     */
    private static TiledImageMock createImage() {
        final var image = new TiledImageMock(
                DataBuffer.TYPE_USHORT, 1,
                -50,                            // minX
                 70,                            // minY
                TILE_WIDTH  * 30,               // width
                TILE_HEIGHT * 25,               // height
                TILE_WIDTH,
                TILE_HEIGHT,
                3,                              // minTileX
                -5,                             // minTileY
                false);
        image.validate();
        image.initializeAllTiles(0);
        image.failRandomly(new Random(), true);
        return image;
    }
}
