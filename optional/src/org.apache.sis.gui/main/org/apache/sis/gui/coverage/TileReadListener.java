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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Pane;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.tiling.TileReadEvent;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.ShapeConverter;
import org.apache.sis.util.logging.Logging;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * Object notified when a tile is about to be read. The notifications can be sent from any thread,
 * typically a background thread which is reading the data. The tiles are enqueued for processing
 * in another background thread for avoiding to slow down the thread that read the data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TileReadListener implements StoreListener<TileReadEvent>, EventHandler<ActionEvent>, Runnable {
    /**
     * Colors of the tiles, using different colors for different resolutions (pyramid levels).
     */
    private static final Color[] TILE_COLORS = {
        Color.VIOLET, Color.RED, Color.YELLOW, Color.CYAN, Color.PALEGREEN
    };

    /**
     * Same colors, but with transparency.
     */
    private static final Color[] FILL_COLORS = new Color[TILE_COLORS.length];
    static {
        for (int i = 0; i < FILL_COLORS.length; i++) {
            final Color c = TILE_COLORS[i];
            FILL_COLORS[i] = Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.5);
        }
    }

    /**
     * Minimal size in pixels for showing the stroke. A minimal size is needed because if, for example,
     * the tile height is 1 pixel (as in <abbr>TIFF</abbr> stripped images), the stroke fills all the
     * surface and the tile appears opaque.
     */
    private static final int MIN_SIZE = 10;

    /**
     * Time that tiles are visible before they fade away.
     */
    private static final Duration DURATION = new Duration(4000);

    /**
     * The JavaFX shapes (usually rectangles) for highlighting the tiles.
     * This queue shall be thread-safe as it is read and written from different threads.
     */
    private final Queue<FadeTransition> tileShapes;

    /**
     * The transform from objective <abbr>CRS</abbr> to the display coordinate system of the canvas.
     * This information is updated in the JavaFX thread after each rendering, so that creations of
     * JavaFX shapes will use the information that reflects the image shown in the canvas.
     */
    private volatile CoverageCanvas.StaticGraphics snapshot;

    /**
     * Creates a new listener of tile read events.
     * This constructor must be invoked from the JavaFX thread.
     */
    TileReadListener(final CoverageCanvas canvas) {
        tileShapes = new ConcurrentLinkedQueue<>();
        newStaticGraphics(canvas);
    }

    /**
     * Takes a snapshot of the objective <abbr>CRS</abbr> and transform to display coordinate system.
     * This method should be invoked after each rendering, so that creations of JavaFX shapes will use
     * the information that reflects the image shown in the canvas.
     */
    final void newStaticGraphics(final CoverageCanvas canvas) {
        snapshot = canvas.usingFixedTransform();
    }

    /**
     * Invoked when a tile has been read. This method computes the JavaFX shape in a background thread.
     * One thread is used for each shape (we do not collect the shapes in a queue) because that thread
     * is likely to finish before the next tile has been read anyway.
     */
    @Override
    @SuppressWarnings("UseSpecificCatch")
    public void eventOccured(final TileReadEvent event) {
        BackgroundThreads.EXECUTOR.execute(() -> {
            /*
             * `TileReadListener.snapshot` may change at any time. We can take any value,
             * but it must stay constant for the rest of this method for consistency.
             */
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final CoverageCanvas.StaticGraphics snapshot = TileReadListener.this.snapshot;
            if (snapshot.objectiveToDisplay instanceof AffineTransform objectiveToDisplay) try {
                final Shape tile = ShapeConverter.convert(event.outline(snapshot.objectiveCRS), objectiveToDisplay);
                final int ic = event.getPyramidLevel() % TILE_COLORS.length;
                final Dimension tileSize = event.getTileSize();
                if (tileSize.width < MIN_SIZE || tileSize.height < MIN_SIZE) {
                    /*
                     * If the tiles are very thin, there is a risk of adding too many nodes.
                     * Tries to reduce the number of transitions by merging adjacent tile shapes.
                     * We do that only if there is no stroke, otherwise some lines would disappear.
                     * Note that the `tileShapes` list should be small, because it contains only the
                     * transitions not yet processed by an execution of `Platform.runLater(…)` below.
                     */
                    if (tile instanceof Rectangle r) {
                        final var merger = new RectangleMerger(r);
                        while (tileShapes.removeIf(merger)) {}
                        merger.copyTo(r);
                    }
                } else {
                    tile.setStroke(TILE_COLORS[ic]);
                }
                tile.setFill(FILL_COLORS[ic]);
                tile.setOpacity(0.5);
                tile.setUserData(snapshot);
                final var transition = new FadeTransition(DURATION, tile);
                transition.setFromValue(0.5);
                transition.setToValue(0);
                transition.setOnFinished(this);
                tileShapes.add(transition);
            } catch (Exception e) {
                Logging.recoverableException(LOGGER, TileReadListener.class, "eventOccured", e);
            }
            Platform.runLater(this);
        });
    }

    /**
     * Invoked in the JavaFX thread for playing the animations that have been prepared.
     * The animation are taken from the {@link #tileShapes} queue, which usually contains
     * exactly one element. But more elements may be present if tiles have been read quickly
     * between two executions of this method by the JavaFX thread.
     */
    @Override
    public void run() {
        FadeTransition transition;
        while ((transition = tileShapes.poll()) != null) {
            final Node node = transition.getNode();
            /*
             * We need to use the snapshot at the time when the rectangle was created.
             * This is not necessarily the same snapshot as when this method is executed.
             */
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final var snapshot = (CoverageCanvas.StaticGraphics) node.getUserData();
            snapshot.getChildren().add(node);
            node.setUserData(null);             // Not needed anymore.
            transition.play();
        }
    }

    /**
     * Invoked in the JavaFX thread when the animation of a tile is finished.
     * This method removes the JavaFX geometry object that represented the tile outline.
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public void handle(final ActionEvent event) {
        final var transition = (FadeTransition) event.getSource();
        final Node node = transition.getNode();
        final Pane parent = (Pane) node.getParent();
        if (parent != null && parent.getChildren().remove(node) && CoverageCanvas.TRACE) {
            CoverageCanvas.trace("TileReadListener.removeChild");
        }
    }
}
