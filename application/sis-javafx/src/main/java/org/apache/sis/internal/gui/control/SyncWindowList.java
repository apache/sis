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
package org.apache.sis.internal.gui.control;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.gui.dataset.WindowHandler;
import org.apache.sis.gui.map.MapCanvas;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * Provides a widget for listing all available windows and selecting the ones to follow
 * on gesture events (zoom, pans, <i>etc</i>).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final class SyncWindowList extends TabularWidget implements ListChangeListener<WindowHandler> {
    /**
     * Window containing a {@link MapCanvas} to follow on gesture events.
     * Gestures are followed only if {@link #linked} is {@code true}.
     */
    private static final class Link {
        /**
         * Whether the "foreigner" {@linkplain #view} should be followed.
         */
        public final BooleanProperty linked;

        /**
         * The "foreigner" view for which to follow the gesture.
         */
        public final WindowHandler view;

        /**
         * Creates a new row for a window to follow.
         *
         * @param  view  the "foreigner" view for which to follow the gesture.
         */
        private Link(final WindowHandler view) {
            this.view = view;
            linked = new SimpleBooleanProperty(this, "linked");
        }

        /**
         * Converts the given list of handled to a list of table rows.
         *
         * @param  added    list of new items to put in the table.
         * @param  exclude  item to exclude (because the referenced window is itself).
         */
        static List<Link> wrap(final List<? extends WindowHandler> added, final WindowHandler exclude) {
            final Link[] items = new Link[added.size()];
            int count = 0;
            for (final WindowHandler view : added) {
                if (view != exclude) {
                    items[count++] = new Link(view);
                }
            }
            return UnmodifiableArrayList.wrap(items, 0, count);
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
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public SyncWindowList(final WindowHandler owner, final Resources resources, final Vocabulary vocabulary) {
        this.owner = owner;
        table = newTable();
        newWindow = new Button(resources.getString(Resources.Keys.NewWindow));
        newWindow.setMaxWidth(Double.MAX_VALUE);
        /*
         * The first column contains a checkbox for choosing whether the window should be followed or not.
         * Header text is 🔗 (link symbol).
         */
        final TableColumn<Link,Boolean> linked = newBooleanColumn("\uD83D\uDD17", (cell) -> cell.getValue().linked);
        final TableColumn<Link,String> name = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Title));
        name.setCellValueFactory((cell) -> cell.getValue().view.title);
        table.getColumns().setAll(linked, name);
        table.getItems().setAll(Link.wrap(owner.manager.windows, owner));
        /*
         * Build all other widget controls.
         */
        newWindow.setOnAction((e) -> owner.duplicate().show());
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(newWindow, Priority.NEVER);
        content = new VBox(9, table, newWindow);
        /*
         * Add listener last when the everything else is successful
         * (because the `this` reference escapes).
         */
        owner.manager.windows.addListener(this);
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
        final ObservableList<Link> items = table.getItems();
        while (change.next()) {
            // Ignore permutations; each table can have its own order.
            if (change.wasRemoved()) {
                // List of removed items usually has a single element.
                for (final WindowHandler item : change.getRemoved()) {
                    for (int i = items.size(); --i >= 0;) {
                        if (items.get(i).view == item) {
                            items.remove(i);
                            break;
                        }
                    }
                }
            }
            if (change.wasAdded()) {
                items.addAll(Link.wrap(change.getAddedSubList(), owner));
            }
        }
    }
}
