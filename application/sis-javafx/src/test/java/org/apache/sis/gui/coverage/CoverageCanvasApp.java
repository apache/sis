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
import java.awt.Point;
import java.awt.image.DataBuffer;
import javafx.application.Application;
import javafx.scene.layout.BorderPane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.image.TiledImageMock;
import org.apache.sis.image.WritablePixelIterator;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * Shows {@link CoverageCanvas} with random data. The image will have small tiles of size
 * {@value #TILE_WIDTH}×{@value #TILE_HEIGHT}. The image will artificially fails to provide
 * some tiles in order to test error controls.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class CoverageCanvasApp extends Application {
    /**
     * Size of the artificial tiles. Should be small enough so we can have many of them.
     * Width and height should be different in order to increase the chance to see bugs
     * if some code confuse them.
     */
    private static final int TILE_WIDTH = 200, TILE_HEIGHT = 300;


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
        final CoverageCanvas canvas = new CoverageCanvas();
        final StatusBar statusBar = new StatusBar(null);
        statusBar.track(canvas);
        canvas.setCoverage(createImage());
        final BorderPane pane = new BorderPane(canvas.getView());
        pane.setBottom(statusBar.getView());
        window.setTitle("CoverageCanvas Test");
        window.setScene(new Scene(pane));
        window.setWidth (800);
        window.setHeight(600);
        window.show();
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
    private static GridCoverage2D createImage() {
        final Random random = new Random();
        final int width  = TILE_WIDTH  * 4;
        final int height = TILE_HEIGHT * 2;
        final TiledImageMock image = new TiledImageMock(
                DataBuffer.TYPE_BYTE, 1,
                random.nextInt(50) - 25,            // minX
                random.nextInt(50) - 25,            // minY
                width, height,
                TILE_WIDTH, TILE_HEIGHT,
                random.nextInt(10) - 5,             // minTileX
                random.nextInt(10) - 5,             // minTileY
                false);
        image.validate();
        final double sc = 500d / Math.max(width, height);
        final WritablePixelIterator it = WritablePixelIterator.create(image);
        while (it.next()) {
            final Point p = it.getPosition();
            final double d = Math.hypot(p.x - width/2, p.y - height/2);
            int value = 0;
            if ((Math.round(d) & 16) == 0) {
                value = Math.max(0, 255 - (int) (d * sc));
            }
            it.setSample(0, value);
        }
        image.failRandomly(random, false);
        return new GridCoverage2D(new GridGeometry(null, PixelInCell.CELL_CORNER,
                MathTransforms.identity(2), CommonCRS.Engineering.DISPLAY.crs()), null, image);
    }
}
