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
package org.apache.sis.gui.controls;

import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.gui.dataset.WindowHandler;
import org.apache.sis.gui.map.MapCanvas;
import org.apache.sis.gui.map.GestureFollower;
import org.apache.sis.gui.internal.Resources;


/**
 * Provides a widget for listing all available windows and selecting the ones
 * on which to replicate gesture events (zoom, pans, <i>etc</i>).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SyncWindowList extends TabularWidget implements ListChangeListener<WindowHandler> {
    /**
     * Window containing a {@link MapCanvas} on which to replicate gesture events.
     * Gestures are replicated only if {@link #transformEnabled} is {@code true}.
     */
    private static final class Link extends GestureFollower {
        /**
         * The "foreigner" view on which to replicate the gestures.
         */
        public final WindowHandler view;

        /**
         * Creates a new row for a window on which to replicate gestures.
         *
         * @param  view    the "foreigner" view on which to replicate the gesture events.
         * @param  source  the canvas which is the source of zoom, pan or rotation events.
         * @param  target  the canvas on which to apply the changes of zoom, pan or rotation.
         */
        private Link(final WindowHandler view, final MapCanvas source, final MapCanvas target) {
            super(source, target);
            this.view = view;
        }

        /**
         * Converts the given list of handlers to a list of table rows.
         *
         * @param  added   list of new items to put in the table.
         * @param  addTo   where to add the converted items.
         * @param  owner   item to exclude (because the referenced window is itself).
         * @param  source  the canvas for which to replicate the changes of zoom, pan or rotation.
         */
        static void wrap(final List<? extends WindowHandler> added, final List<Link> addTo,
                         final WindowHandler owner, final MapCanvas source)
        {
            final Link[] items = new Link[added.size()];
            int count = 0;
            try {
                for (final WindowHandler view : added) {
                    if (view != owner) {
                        final MapCanvas target = view.getCanvas().orElse(null);
                        if (target != null) {
                            final Link item = new Link(view, source, target);;
                            items[count++] = item;          // Add now for disposing if an exception is thrown.
                            item.initialize();              // Invoked outside constructor for allowing disposal.
                            item.cursorEnabled.set(true);
                        }
                    }
                }
                addTo.addAll(Containers.viewAsUnmodifiableList(items, 0, count));
            } catch (Throwable e) {
                while (--count >= 0) {
                    items[--count].dispose();               // Remove listeners for avoiding memory leak.
                }
                throw e;
            }
        }
    }

    /**
     * The table showing values associated to colors.
     */
    private final TableView<Link> table;

    /**
     * The button for creating a new window.
     */
    private final Button newWindow;

    /**
     * The view for which to create a list of synchronized windows.
     */
    private final WindowHandler owner;

    /**
     * The canvas for which to replicate the changes of zoom, pan or rotation.
     * Needs to be fetched only when first needed (not at construction time)
     * for avoiding a stack overflow.
     */
    private MapCanvas source;

    /**
     * The component to be returned by {@link #getView()}.
     */
    private final VBox content;

    /**
     * Creates a new "synchronized windows" widget.
     *
     * @param  owner       the view for which to create a list of synchronized windows.
     * @param  resources   localized resources, given because already known by the caller.
     * @param  vocabulary  localized resources, given because already known by the caller
     *                     (those arguments would be removed if this constructor was public API).
     */
    @SuppressWarnings("unchecked")
    public SyncWindowList(final WindowHandler owner, final Resources resources, final Vocabulary vocabulary) {
        this.owner = owner;
        table = newTable();
        newWindow = new Button(resources.getString(Resources.Keys.NewWindow));
        newWindow.setMaxWidth(Double.MAX_VALUE);
        /*
         * The first column contains a checkbox for choosing whether the window should be followed or not.
         */
        table.getColumns().setAll(
                newBooleanColumn(/* 🔗 (link symbol) */  "\uD83D\uDD17",      (cell) -> cell.getValue().transformEnabled),
                newStringColumn (vocabulary.getString(Vocabulary.Keys.Title), (cell) -> cell.getValue().view.title));
        table.setRowFactory(SyncWindowList::newRow);
        /*
         * Build all other widget controls.
         */
        newWindow.setOnAction((e) -> owner.duplicate().show());
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(newWindow, Priority.NEVER);
        content = new VBox(9, table, newWindow);
        /*
         * Add listener last when the everything else is successful (because the `this` reference escapes).
         * The creation of table item list needs to be done after the caller finished its initialization,
         * not now, because invoking`owner.getCanvas()` here create an infinite loop.
         */
        Platform.runLater(() -> addAll(owner.manager.windows));
        owner.manager.windows.addListener(this);
    }

    /**
     * Invoked when `TableView` needs to create a new row for rendering purpose.
     * This is the place where to configure each {@code TableRow} instances.
     *
     * @param  table  the table which needs a new row.
     * @return the new row.
     */
    private static TableRow<Link> newRow(final TableView<Link> table) {
        TableRow<Link> row = new TableRow<>();
        row.addEventFilter(MouseEvent.MOUSE_CLICKED, SyncWindowList::onMouseClicked);
        return row;
    }

    /**
     * Invoked when the user clicked on a cell of the row. We register this listener as a filter,
     * e.g. this is invoked before the table row see the event, so we can prevent the event to be
     * forwarded to the cell if desired.
     */
    private static void onMouseClicked(final MouseEvent event) {
        if (event.getClickCount() == 2) {
            final TableRow<?> row = (TableRow<?>) event.getSource();
            final Link item = (Link) row.getItem();
            item.view.show();
            event.consume();
            /*
             * If the cell was already selected, the first click caused the cell to enter in editing mode
             * before the second click is detected. We want to cancel the edition since we moved focus to
             * another window. The javadoc for the following method said: "Note: This method will cancel
             * editing if the given row value is less than zero and the given column is null."
             */
            row.getTableView().edit(-1, null);
        }
    }

    /**
     * Returns the encapsulated JavaFX component to add in a scene graph for making the table visible.
     * The {@code Region} subclass is implementation dependent and may change in any future SIS version.
     *
     * @return the JavaFX component to insert in a scene graph.
     */
    @Override
    public Region getView() {
        return content;
    }

    /**
     * Invoked when new items are added or removed in the list of windows.
     *
     * @param  change  a description of changes in the list of windows.
     */
    @Override
    public void onChanged(final Change<? extends WindowHandler> change) {
        while (change.next()) {
            // Ignore permutations; each table can have its own order.
            if (change.wasRemoved()) {
                // List of removed items usually has a single element.
                final ObservableList<Link> items = table.getItems();
                for (final WindowHandler view : change.getRemoved()) {
                    for (int i = items.size(); --i >= 0;) {
                        final Link item = items.get(i);
                        if (item.view == view) {
                            items.remove(i);
                            item.dispose();
                            break;
                        }
                    }
                }
            }
            if (change.wasAdded()) {
                addAll(change.getAddedSubList());
            }
        }
    }

    /**
     * Adds the given window handlers and items in {@link #table}.
     */
    private void addAll(final List<? extends WindowHandler> windows) {
        if (source == null) {
            source = owner.getCanvas().get();
        }
        Link.wrap(windows, table.getItems(), owner, source);
    }
}
