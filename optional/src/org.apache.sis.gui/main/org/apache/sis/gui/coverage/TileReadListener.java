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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.layout.Pane;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
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
    private static final int MIN_TILE_SIZE = 10;

    /**
     * Delay in milliseconds before to request an update (in JavaFX thread) of the children list.
     * This delay is for grouping some children additions or removals in a single change event.
     */
    private static final int DELAY_BEFORE_UPDATE = 100;

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
     * Children waiting to be removed. Stored in a separated queue for removing many children together.
     * This is necessary because removing children one-by-one can cause slow invalidation of JavaFX scene.
     * This map shall be used in the JavaFX thread only.
     */
    private final Map<Node, ObservableList<Node>> childrenToRemove;

    /**
     * Whether an update (to be done in JavaFX thread) of the list of children has already been requested.
     * This is used for waiting a little bit before to perform an update in order to produce less change events.
     *
     * @see #DELAY_BEFORE_UPDATE
     */
    private boolean childrenUpdateRequested;

    /**
     * Creates a new listener of tile read events.
     * This constructor must be invoked from the JavaFX thread.
     */
    TileReadListener(final CoverageCanvas canvas) {
        tileShapes = new ConcurrentLinkedQueue<>();
        childrenToRemove = new IdentityHashMap<>();
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
        final boolean pending = childrenUpdateRequested;
        childrenUpdateRequested = true;
        BackgroundThreads.EXECUTOR.execute(() -> {
            /*
             * `TileReadListener.snapshot` may change at any time. We can take any value,
             * but it must stay constant for the rest of this method for consistency.
             * It is not really useful to take the value at the time when the event occurred,
             * because that event is itself sent after an arbitrarily long background thread.
             */
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final CoverageCanvas.StaticGraphics snapshot = TileReadListener.this.snapshot;
            if (snapshot.objectiveToDisplay instanceof AffineTransform objectiveToDisplay) try {
                final Shape tile = ShapeConverter.convert(event.outline(snapshot.objectiveCRS), objectiveToDisplay);
                final int ic = event.getPyramidLevel() % TILE_COLORS.length;
                final Dimension tileSize = event.getTileSize();
                if (tileSize.width < MIN_TILE_SIZE || tileSize.height < MIN_TILE_SIZE) {
                    /*
                     * If the tiles are very thin, there is a risk of adding too many nodes.
                     * Tries to reduce the number of transitions by merging adjacent tile shapes.
                     * We do that only if there is no stroke, otherwise some lines would disappear.
                     * Note that the `tileShapes` queue should be small, because it contains only the
                     * transitions not yet processed by an execution of `Platform.runLater(…)` below.
                     */
                    if (tile instanceof Rectangle r) {
                        final var merger = new RectangleMerger(snapshot, r);
                        while (tileShapes.removeIf(merger)) {}
                        merger.copyTo(r);
                    }
                } else {
                    tile.setStroke(TILE_COLORS[ic]);
                    tile.setStrokeType(StrokeType.INSIDE);
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
            /*
             * The addition of the tiles in the scene graph needs to be done in the JavaFX thread.
             * Wait a little bit for improving the chance to group many tiles in a single event.
             */
            if (!pending) {
                try {
                    Thread.sleep(DELAY_BEFORE_UPDATE);
                } catch (InterruptedException e) {
                    // Ignore.
                }
                Platform.runLater(this);
            }
        });
    }

    /**
     * Invoked in the JavaFX thread for updating the children lists and playing the animations that have been prepared.
     * The animation are taken from the {@link #tileShapes} queue, which often contains exactly one element.
     * But more elements may be present if tiles have been read quickly between two executions of this method.
     */
    @Override
    public void run() {
        childrenUpdateRequested = false;
        removeFinishedTransitions();
        final var group = new ArrayList<FadeTransition>();
        CoverageCanvas.StaticGraphics target = null;
        FadeTransition transition;
        while ((transition = tileShapes.poll()) != null) {
            final Node node = transition.getNode();
            /*
             * We need to use the snapshot at the time when the rectangle was created.
             * This is not necessarily the same snapshot as when this method is executed.
             */
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final var snapshot = (CoverageCanvas.StaticGraphics) node.getUserData();
            node.setUserData(null);             // Not needed anymore.
            if (target != snapshot) {
                addAndPlay(group, target);
                target = snapshot;
                group.clear();
            }
            group.add(transition);
        }
        addAndPlay(group, target);
    }

    /**
     * Adds the given tiles to the JavaFX scene graph and play them.
     * This method is used for trying to add nodes in bulk, because adding a
     * list of nodes causes less change events than adding nodes one by one.
     *
     * @param tiles   the tiles to add as (usually) rectangles that will fade away.
     * @param target  where to add the tiles. May be {@code null} if {@code tiles} is empty.
     */
    private static void addAndPlay(final List<FadeTransition> tiles, final CoverageCanvas.StaticGraphics target) {
        final int n = tiles.size();
        if (n != 0) {
            final ObservableList<Node> children = target.getChildren();
            if (n == 1) {
                // Shortcut for a very common case.
                FadeTransition transition = tiles.get(0);
                children.add(transition.getNode());
                transition.play();
            } else {
                // JavaFX is faster with bulk changes.
                final var shapes = new Node[n];
                for (int i=0; i<shapes.length; i++) {
                    shapes[i] = tiles.get(i).getNode();
                }
                children.addAll(shapes);
                tiles.forEach(FadeTransition::play);
            }
        }
    }

    /**
     * Invoked in the JavaFX thread when the animation of a tile is finished.
     * The JavaFX geometry object that represented the tile outline is added
     * to a list of nodes to be removed a little bit later.
     * The removal is not done immediately for having a chance to group them,
     * because removing nodes one-by-one appears to be sometime very slow.
     */
    @Override
    public void handle(final ActionEvent event) {
        final var transition = (FadeTransition) event.getSource();
        final Node node = transition.getNode();
        if (node.getParent() instanceof Pane parent) {
            childrenToRemove.put(node, parent.getChildren());
        }
        if (childrenToRemove.size() <= 1 && !childrenUpdateRequested) {
            BackgroundThreads.EXECUTOR.execute(() -> {
                try {
                    Thread.sleep(DELAY_BEFORE_UPDATE * 10);     // Can wait longer because the effect is not visible.
                } catch (InterruptedException e) {
                    // Ignore.
                }
                Platform.runLater(() -> removeFinishedTransitions());
            });
        }
    }

    /**
     * Removes children which were waiting to be removed.
     * This method tries to remove children by groups.
     */
    private void removeFinishedTransitions() {
        Iterator<ObservableList<Node>> it;
        while ((it = childrenToRemove.values().iterator()).hasNext()) {
            final ObservableList<Node> children = it.next();
            int upper = children.size();
            int lower = upper;
            while (lower != 0) {
                if (childrenToRemove.containsKey(children.get(lower - 1))) {
                    // Include in the range of nodes to remove.
                    lower--;
                } else {
                    // Found a node to not remove. Remove the range found before.
                    if (lower != upper) {
                        children.remove(lower, upper);
                    }
                    upper = --lower;
                }
            }
            children.remove(lower, upper);
            /*
             * Removes all map entries which were removing elements from the same list.
             * They should have been removed already by above loop. The map will often
             * become empty, but we verify by reexecuted the loop for other lists.
             */
            it.remove();
            while (it.hasNext()) {
                if (it.next() == children) {
                    it.remove();
                }
            }
        }
    }
}
