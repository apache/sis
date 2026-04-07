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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ConstraintsBase;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.coverage.CoverageCanvas;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.DataStoreOpener;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.io.TableAppender;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.CloseEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.FrequencySortedSet;
import org.apache.sis.util.logging.Logging;


/**
 * A grid of {@link MapCanvas} instances shown side-by-side with the same visual size.
 * User's navigation can optionally by synchronized so that panning or zooming
 * in one map causes the same pan or zoom to be applied in the other maps.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MultiCanvas extends Widget implements Observable {
    /**
     * The border around each canvas. This border is applied only on {@link BorderPane}.
     * Therefore, it does not appear when {@code MultiCanvas} is showing only one canvas,
     * because the canvas view is shown directly (without {@link BorderPane}) in such case.
     */
    private static final Border CANVAS_BORDER = Border.stroke(Color.LIGHTBLUE);

    /**
     * Handles the {@link javafx.scene.control.ChoiceBox} and menu items for selecting a <abbr>CRS</abbr>.
     */
    private final RecentReferenceSystems referenceSystems;

    /**
     * The view where canvases are shown. All children of this pane shall be either {@link MapCanvas#fixedPane}
     * values, or instances of {@link BorderPane} wrapping the above-cited values as their center component.
     *
     * @see #getCanvasView(Node)
     * @see #addCanvasView(MapCanvas)
     */
    private final GridPane canvasGrid;

    /**
     * The constraint applied on all rows. Contains the height as a percentage.
     */
    private final RowConstraints rowSize;

    /**
     * The constraint applied on all columns. Contains the width as a percentage.
     */
    private final ColumnConstraints colSize;

    /**
     * The combination of canvases and status bar.
     * The status bar shall be the last element.
     */
    private final VBox view;

    /**
     * All created canvases, visible or not, associated to their title and status bar.
     *
     * @see #addResource(Resource)
     * @see #removeResource(Resource)
     * @see #createOrReuseCanvas(Resource, Collection)
     */
    private final Map<MapCanvas, Controls> canvasPool;

    /**
     * Controls associated to each map canvas.
     */
    private static final class Controls {
        /**
         * The title of the associated map canvas. This label is not necessarily shown.
         * If the enclosing {@link MultiCanvas} contains only one {@link MapCanvas},
         * the text of this label may be shown in the window title instead.
         */
        final Label title;

        /**
         * The status bar of the associated map canvas. Only one status bar will be shown at any given time,
         * but it is easier to nevertheless create a separated instance for each canvas for avoiding the need
         * to unregister and register again numerous listeners every time that the mouse moves over a new canvas.
         * Also because the warning message, coordinate format, and the set of sample dimensions to show depend
         * on the {@link MapCanvas} content.
         */
        final StatusBar status;

        /**
         * Listeners of mouse displacements and navigation actions such as zooms and pans.
         */
        final List<GestureFollower> followers;

        /**
         * Creates new controls for a new map canvas, then registers the listeners.
         * Note that these listeners create cyclic references: most references are from {@code canvas} to {@code owner}
         * (therefore could be garbage-collected), but there are also some references from {@link #referenceSystems} to
         * {@linkplain #status} bar, which itself has a reference to the canvas, therefore preventing garbage collection.
         *
         * @param  owner   the enclosing {@code MultiCanvas}.
         * @param  canvas  the new canvas.
         */
        Controls(final MultiCanvas owner, final MapCanvas canvas) {
            followers = new ArrayList<>();
            title  = new Label();
            status = new StatusBar(owner.referenceSystems);
            status.track(canvas);
            final var menu = new MapMenu(canvas);
            menu.addReferenceSystems(owner.referenceSystems);
            menu.addCopyOptions(status);
            getView(canvas).addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> owner.showStatusBar(canvas));
        }

        /**
         * Creates listeners of navigation events in the source canvas, for replicating them in the target canvas.
         * The {@code source} argument <em>shall</em> be the {@code canvas} argument given to the constructor.
         *
         * @param  source  the {@code canvas} argument given to the constructor.
         * @param  target  the canvas where to replicate the navigation events applied on {@code source}.
         */
        final void addGestureFollower(final MapCanvas source, final MapCanvas target) {
            final var follower = new GestureFollower(source, target);
            follower.initialize();
            follower.cursorEnabled.set(true);
            followers.add(follower);
        }

        /**
         * Removes the listeners which were replicating navigation events to the specified target.
         *
         * @param  target  the canvas where the navigation events of {@code source} were replicated.
         */
        final void removeGestureFollower(final MapCanvas target) {
            for (int i = followers.size(); --i >= 0;) {
                if (followers.get(i).target == target) {
                    followers.remove(i).dispose();
                }
            }
        }

        /**
         * Unregisters the listeners which were following the mouse displacements and navigation events.
         * Also clears the title for preventing it to contribute to {@link #getCanvasTitles()}.
         * Does not unregister the listener for showing the status bar, as this listener does not depend
         * on which data are shown in the canvas. This {@code Controls} may be reused for new data.
         */
        final void clear() {
            title.setText(null);
            followers.forEach(GestureFollower::dispose);
            followers.clear();
        }
    }

    /**
     * Whether the status bar is visible. If {@code true}, the status bar
     * shall be the last element of the {@linkplain #view} children.
     */
    private boolean isStatusBarVisible;

    /**
     * The action to execute when any resource contained in this {@code MultiCanvas} is closed.
     * This listener delegates to {@link #removeResource(Resource)}.
     *
     * @see #removeResource(Resource)
     */
    private final OnClose closer;

    /**
     * The listeners to notify when the state of this {@code MultiCanvas} changed.
     * This is a copy-on-change array: a new array is created every time that a listener is added or removed.
     *
     * @see #addListener(InvalidationListener)
     * @see #removeListener(InvalidationListener)
     * @see #invalidate()
     */
    private InvalidationListener[] listeners;

    /**
     * Creates an initially empty grid of map canvases.
     */
    public MultiCanvas() {
        canvasPool = new LinkedHashMap<>();
        canvasGrid = new GridPane();
        VBox.setVgrow(canvasGrid, Priority.ALWAYS);
        rowSize = new RowConstraints();
        colSize = new ColumnConstraints();
        rowSize.setVgrow(Priority.ALWAYS);
        colSize.setHgrow(Priority.ALWAYS);
        referenceSystems = new RecentReferenceSystems();
        referenceSystems.addUserPreferences();
        referenceSystems.addAlternatives("EPSG:4326", "EPSG:3395", "MGRS");     // WGS 84 / World Mercator
        view = new VBox(canvasGrid);
        closer = new OnClose();
    }

    /**
     * Returns the encapsulated JavaFX component to add in a scene graph for making the canvases visible.
     * The {@code Region} subclass is implementation dependent and may change in any future SIS version.
     *
     * @return the JavaFX component to insert in a scene graph.
     */
    @Override
    public Region getView() {
        return view;
    }

    /**
     * Returns the JavaFX node to show for the given canvas. This method is defined
     * for having a central place where this choice is made, for ensuring consistency.
     */
    private static Region getView(final MapCanvas canvas) {
        return canvas.fixedPane;
    }

    /**
     * Returns the view of the map canvas which is contained in the given node of the map canvas grid.
     * The given {@code child} argument shall be one of the children of {@link #canvasGrid}.
     *
     * @param  child  an element of the {@code canvasGrid.getChildren()} list.
     * @return the {@link MapCanvas#fixedPane} value in the given node.
     * @throws ClassCastException if {@code child} does not comply with the conditions documented in {@link #canvasGrid}.
     */
    private static Region getCanvasView(final Node child) {
        return (Region) ((child instanceof BorderPane pane) ? pane.getCenter() : child);
    }

    /**
     * Returns the views of all map canvases that are currently shown in this {@code MultiCanvas}.
     * The returned set is modifiable (callers may remove elements) and elements are in no particular order.
     *
     * @param  children  value of {@code canvasGrid.getChildren()}.
     * @return all currently visible map canvases views.
     */
    private static Set<Region> getCanvasViews(final List<Node> children) {
        final Set<Region> views = HashSet.newHashSet(children.size());
        for (final Node child : children) {
            views.add(getCanvasView(child));
        }
        return views;
    }

    /**
     * Returns the controls of a canvas when only its view is known.
     * This method is inefficient, but is invoked in contexts where the map has only one element.
     *
     * @param  canvasView  the result of a call to {@link #getCanvasView(Node)}.
     * @return the controls for the canvas having the given view.
     * @throws NoSuchElementException if no canvas with the given view has been found.
     */
    private Controls getControls(final Region canvasView) {
        for (final Map.Entry<MapCanvas, Controls> entry : canvasPool.entrySet()) {
            if (getView(entry.getKey()) == canvasView) {
                return entry.getValue();
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Adds a view for the given map canvas. If the given {@code canvas} is the only map canvas which is shown,
     * then its view will be added directly. Otherwise, the {@code canvas} view will be wrapped as the center of
     * a {@link BorderPane} with a title on top of it. In the latter case, this method may update previous child
     * for adding also a title bar to them.
     *
     * <p>Callers should start a background thread for setting the text of the returned label to a map canvas title.
     * It may be, for example, the label of the resource which is shown. That label will not necessarily be visible
     * now (it depends how many canvases are shown). Its text may be shown in the window title bar instead.</p>
     *
     * @param  canvas  the canvas for which to add a view.
     * @return the label on which the caller should set the map canvas title.
     */
    @SuppressWarnings("fallthrough")
    private Label addCanvasView(final MapCanvas canvas) {
        final Controls controls = canvasPool.computeIfAbsent(canvas, (key) -> new Controls(this, key));
        /*
         * If the canvas will be alone in the window, show the view directly without title and border.
         * If more than one canvas will exist, wrap the canvas view in a pane with a title and a border
         * for separating this view from other views. It may require adding the title bar and border to
         * a previously existing canvas view if the latter was alone before this method call.
         */
        Region canvasView = getView(canvas);
        final List<Node> children = canvasGrid.getChildren();
        switch (children.size()) {
            case 0:  break;
            case 1:  var previous = (Region) children.removeLast();
                     previous = addTitleBar(previous, getControls(previous).title);
                     children.add(previous);
                     // Fall through
            default: canvasView = addTitleBar(canvasView, controls.title);
        }
        /*
         * Add listeners for replicating navigation events of `canvas` into all other visible canvases,
         * and conversely. Shall be done before the canvas view is added to the children list.
         */
        final Set<Region> visibles = getCanvasViews(children);
        for (final Map.Entry<MapCanvas, Controls> entry : canvasPool.entrySet()) {
            final MapCanvas other = entry.getKey();
            if (visibles.contains(getView(other))) {
                controls.addGestureFollower(canvas, other);
                entry.getValue().addGestureFollower(other, canvas);
            }
        }
        children.add(canvasView);
        return controls.title;
    }

    /**
     * Removes the view of the given map canvas.
     * Because of bidirectional references between {@link MapCanvas} and {@link MultiCanvas} through listeners,
     * instead of trying to remove entries from the {@link #canvasPool} map, current implementation rather just
     * hides unused canvases and may reuse them later.
     *
     * @param  canvas    the canvas for which to remove the view.
     * @param  controls  value of {@code canvasPool.get(canvas)} (not necessarily obtained by that call).
     * @return whether the view has been found and removed.
     */
    private boolean removeCanvasView(final MapCanvas canvas, final Controls controls) {
        canvasPool.values().forEach((other) -> other.removeGestureFollower(canvas));
        boolean changed = false;
        final Region canvasView = getView(canvas);
        final List<Node> children = canvasGrid.getChildren();
        for (int i = children.size(); --i >= 0;) {
            if (getCanvasView(children.get(i)) == canvasView) {
                children.remove(i);
                changed = true;
            }
        }
        if (children.size() == 1) {
            Node previous = children.removeLast();
            previous = getCanvasView(previous);
            children.add(previous);                 // Hide the title bar and the border.
        }
        clear(canvas, controls);
        return changed;
    }

    /**
     * Removes the specified map canvas view.
     * This method is invoked when the user clicks on the close button.
     * The {@link MapCanvas} instance is kept in the pool for potential reuse.
     *
     * @param  canvasView  view of the canvas to close.
     */
    private void closeCanvasView(final Region canvasView) {
        boolean changed = false;
        for (final Map.Entry<MapCanvas, Controls> entry : canvasPool.entrySet()) {
            final MapCanvas canvas = entry.getKey();
            if (getView(canvas) == canvasView) {
                changed |= removeCanvasView(canvas, entry.getValue());
                // Should have only one instance, but continue the loop by paranoia.
            }
        }
        if (changed) {
            layoutGrid();
            invalidate();
        }
    }

    /**
     * Wraps the given {@code MapCanvas} view into a pane with a title.
     *
     * @param  canvasView  the {@link MapCanvas} view for which to add a title bar.
     * @return a wrapper of {@code canvasView} with the addition of a title bar.
     */
    private BorderPane addTitleBar(final Region canvasView, final Label title) {
        final var close = new Button("❌");
        close.setOnAction((event) -> closeCanvasView(canvasView));
        HBox.setHgrow(title, Priority.ALWAYS);
        HBox.setHgrow(close, Priority.NEVER);
        final var bar = new HBox(title, close);
        bar.setAlignment(Pos.CENTER);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        final var pane = new BorderPane(canvasView);
        pane.setBorder(CANVAS_BORDER);
        pane.setTop(bar);
        return pane;
    }

    /**
     * Returns whether the given resource is supported.
     * This is currently a static method because {@link #createOrReuseCanvas(Resource, Collection)} is static.
     * But this method would become public and non-static of {@code createOrReuseCanvas(…)} become public and
     * non-static.
     *
     * @param  resource   the resource to show, or {@code null}.
     * @return whether the given resource can be shown.
     */
    static boolean isSupported(final Resource resource) {
        return (resource instanceof GridCoverageResource);
    }

    /**
     * Returns a canvas showing the given resource, or {@code null} if none.
     * If one of the canvases in the {@code available} collection is suitable,
     * this method should configure it for showing the given resource.
     * Otherwise, this method should create a new canvas.
     *
     * <p>There is currently no mechanism for removing a canvas because it is tedious to remove all listeners.
     * However, {@code MultiCanvas} may hide a canvas by setting its resource to null and reuse that canvas later
     * if a new resource is specified.</p>
     *
     * @param  resource   the resource to show.
     * @param  available  previously created canvases that may be reused.
     * @return a canvas showing the given resource, or {@code null} if none.
     */
    private static MapCanvas createOrReuseCanvas(final Resource resource, final Collection<MapCanvas> available) {
        if (resource instanceof GridCoverageResource c) {
            for (final MapCanvas canvas : available) {
                if (canvas instanceof CoverageCanvas cc) {
                    cc.setResource(c);
                    return cc;
                }
            }
            final var canvas = new CoverageCanvas();
            canvas.setResource(c);
            return canvas;
        }
        return null;
    }

    /**
     * Returns the resource shown by the given canvas, or {@code null} if none.
     * Note: there is no {@code MapCanvas.getResource()} method because the base
     * {@link MapCanvas} class is not about a single resource.
     * It may be about a map context.
     */
    private static Resource getResource(final MapCanvas canvas) {
        if (canvas instanceof CoverageCanvas c) {
            return c.getResource();
        }
        return null;
    }

    /**
     * Tries to allocate a canvas for the given resource and to show it in this multi-canvas view.
     * The grid is reorganized for accommodating the new canvas,
     * potentially with the addition of a new row or a new column.
     * The same resource may be shown many times. Null resources are ignored.
     *
     * @param  resource  the resource to add, or {@code null}.
     * @return whether the given resource has been accepted.
     */
    public boolean addResource(final Resource resource) {
        if (resource == null) {
            return false;
        }
        /*
         * Get the collection of `MapCanvas` instances which are not currently used.
         * These instances may be recycled for the given resource.
         */
        final Map<Node, MapCanvas> available = LinkedHashMap.newLinkedHashMap(canvasPool.size());
        canvasPool.keySet().forEach((canvas) -> available.put(getView(canvas), canvas));
        canvasGrid.getChildren().forEach((child) -> available.remove(getCanvasView(child)));
        final MapCanvas canvas = createOrReuseCanvas(resource, available.values());
        if (canvas == null) {
            return false;
        }
        final Locale locale = canvas.getLocale();
        final var title = addCanvasView(canvas);
        BackgroundThreads.execute(() -> {
            String label;
            try {
                label = DataStoreOpener.findLabel(resource, locale, true);
            } catch (DataStoreException | RuntimeException e) {
                Logging.recoverableException(LOGGER, MultiCanvas.class, "addResource", e);
                label = DataStoreOpener.fallbackLabel(resource, locale);
            }
            final String text = label;      // Because lambda functions want final variables.
            Platform.runLater(() -> {
                title.setText(text);
                invalidate();
            });
        });
        resource.addListener(CloseEvent.class, closer);
        layoutGrid();
        return true;
    }

    /**
     * Tries to remove all canvases associated to the given resource.
     * The grid is reorganized for accommodating the remaining canvases,
     * potentially with the removal of rows or columns.
     *
     * <p>This method is invoked automatically if the resource added with
     * {@link #addResource(Resource)} fired a {@link CloseEvent}.</p>
     *
     * @param  resource  the resource to remove, or {@code null}.
     * @return whether the given resource has been found and removed.
     */
    public boolean removeResource(final Resource resource) {
        boolean changed = false;
        if (resource != null) {
            resource.removeListener(CloseEvent.class, closer);
            for (final Map.Entry<MapCanvas, Controls> entry : canvasPool.entrySet()) {
                final MapCanvas canvas = entry.getKey();
                if (getResource(canvas) == resource) {
                    changed |= removeCanvasView(canvas, entry.getValue());
                }
            }
            if (changed) {
                layoutGrid();
                invalidate();
            }
        }
        return changed;
    }

    /**
     * Sets the grid row index, column index and span on all map canvases.
     * The grid size and the indexes of each child depend on the number of children.
     */
    private void layoutGrid() {
        final List<Node> children = canvasGrid.getChildren();
        final int size = children.size();
        if (size != 0) {
            final int numRow = (int) Math.rint(Math.sqrt(size));
            final int numCol = Math.ceilDiv(size, numRow);
            rowSize.setPercentHeight(resize(rowSize, canvasGrid.getRowConstraints(),    numRow));
            colSize.setPercentWidth (resize(colSize, canvasGrid.getColumnConstraints(), numCol));
            int span = GridPane.REMAINING;  // Allocated to the last canvas.
            for (int i = size; --i >= 0;) {
                final int row = i / numCol;
                final int col = i % numCol;
                GridPane.setConstraints(children.get(i), col, row, span, 1);
                span = 1;
            }
        }
    }

    /**
     * Ensures that the given list has the expected size.
     * Opportunistically computes the percentage to set for the column width or height.
     *
     * @param  <C>       {@link RowConstraints} or {@link ColumnConstraints}.
     * @param  toAdd     the constraint to add if the list is too short.
     * @param  declared  the current list of constraints.
     * @param  expected  the expected size of the list.
     * @return the percentage to set for the column width or row height.
     */
    private static <C extends ConstraintsBase> double resize(final C toAdd, final List<C> declared, final int expected) {
        final int size = declared.size();
        if (size > expected) {
            declared.subList(expected, size).clear();
        } else if (size != expected) do {
            declared.add(toAdd);
        } while (declared.size() < expected);
        return 100d / expected;
    }

    /**
     * Shows the status bar of the given canvas.
     * This method is invoked when the mouse enter in a new canvas.
     *
     * @param  canvas  the canvas for which to show the status bar.
     */
    private void showStatusBar(final MapCanvas canvas) {
        final Controls controls = canvasPool.get(canvas);
        if (controls != null) {
            final Region status = controls.status.getView();
            final List<Node> children = view.getChildren();
            if (isStatusBarVisible) {
                children.set(children.size() - 1, status);
            } else {
                children.add(status);
                isStatusBarVisible = true;
            }
        }
    }

    /**
     * Returns the titles of all canvases shown in this {@code MultiCanvas}.
     * If many canvases have the same title, the most frequently used title will be first.
     */
    final FrequencySortedSet<String> getCanvasTitles() {
        final var titles = new FrequencySortedSet<String>(true);
        for (final Controls controls : canvasPool.values()) {
            final String text = controls.title.getText();
            if (text != null) titles.add(text);
        }
        return titles;
    }

    /**
     * Adds a listener which will be notified when a map canvas is added or removed.
     * The listener is also invoked for a change in the title of a map canvas,
     * for example after completion of the background thread fetching the title.
     *
     * @param  listener  the listener to add.
     */
    @Override
    public void addListener(final InvalidationListener listener) {
        Objects.requireNonNull(listener);
        if (listeners == null) {
            listeners = new InvalidationListener[] {listener};
        } else {
            listeners = ArraysExt.append(listeners, listener);
        }
    }

    /**
     * Removes a listener which is not longer interested to map canvas addition or removal.
     *
     * @param  listener  the listener to remove.
     */
    @Override
    public void removeListener(final InvalidationListener listener) {
        Objects.requireNonNull(listener);
        final InvalidationListener[] snapshot = listeners;
        if (snapshot != null) {
            for (int i=0; i<snapshot.length; i++) {
                if (snapshot[i] == listener) {
                    listeners = ArraysExt.remove(snapshot, i, 1);
                    break;
                }
            }
        }
    }

    /**
     * Notifies all listeners that a map canvas has been added or removed.
     * May also be invoked if a title changed.
     */
    protected void invalidate() {
        if (listeners != null) {
            for (final InvalidationListener listener : listeners) {
                listener.invalidated(this);
            }
        }
    }

    /**
     * Clears the content of the given map canvas.
     * It is better to remove the canvas from {@link #canvasGrid} before to invoke this method.
     *
     * @param  canvas    the map canvas to clear.
     * @param  controls  value of {@code canvasPool.get(canvas)} (not necessarily obtained by that call).
     */
    private void clear(final MapCanvas canvas, Controls controls) {
        final Resource resource = getResource(canvas);
        if (resource != null) {
            resource.removeListener(CloseEvent.class, closer);
        }
        canvas.clearLater();
        controls.clear();
    }

    /**
     * Releases resources in an attempt to help the garbage collector.
     * This is invoked when the window containing this {@code MultiCanvas} is closed.
     */
    final void dispose() {
        canvasGrid.getChildren().clear();
        canvasPool.forEach(this::clear);
        canvasPool.clear();
    }

    /**
     * The action to execute when a resource is closed.
     * A single instance of this listener can be shared by all resources.
     */
    private final class OnClose implements StoreListener<CloseEvent> {
        /**
         * Invoked when a resource is closing. This method can be invoked from any thread,
         * but delegation to {@link #removeResource(Resource)} is done in the JavaFX thread.
         * This method blocks until the JavaFX thread finished to execute {@code removeResource(…)}
         * for avoiding that the background thread closes the resource before we removed its usages.
         */
        @Override public void eventOccured(final CloseEvent event) {
            final Resource resource = event.getSource();
            if (Platform.isFxApplicationThread()) {
                removeResource(resource);
            } else BackgroundThreads.runAndWaitDialog(() -> {
                removeResource(resource);
                return null;
            });
        }
    }

    /**
     * Returns a string representation of the state of this {@code MultiCanvas} for debugging purposes.
     * The returned string representation contains a consistency check in the last column.
     *
     * @return a string representation of the state of this {@code MultiCanvas}.
     */
    @Override
    public String toString() {
        final Set<Region> views = getCanvasViews(canvasGrid.getChildren());
        final var table = new TableAppender(" │ ");
        table.appendHorizontalSeparator();
        table.append("Title").nextColumn();
        table.append("Shown").nextColumn();
        table.append("Error").appendHorizontalSeparator();
        for (final Map.Entry<MapCanvas, Controls> entry : canvasPool.entrySet()) {
            final MapCanvas canvas = entry.getKey();
            final Controls controls = entry.getValue();
            table.append(controls.title.getText()).nextColumn();
            table.append(Boolean.toString(views.remove(getView(canvas)))).nextColumn();
            String error = "";
            for (final GestureFollower follower : controls.followers) {
                if (follower.source != canvas) {
                    error = "follower.source";
                    break;
                }
                @SuppressWarnings("element-type-mismatch")
                final Controls target = canvasPool.get(follower.target);
                if (target == null) {
                    error = "follower.target";
                    break;
                }
            }
            table.append(error).nextLine();
        }
        for (final Region orphan : views) {
            table.append(String.valueOf(orphan)).nextColumn();
            table.append("true").nextColumn();
            table.append("orphan").nextLine();
        }
        table.appendHorizontalSeparator();
        return table.toString();
    }
}
