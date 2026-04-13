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
package org.apache.sis.gui.map.style;

import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Region;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.internal.Resources;


/**
 * Tree of portrayal objects such as map layers, together with controls for configuring their appearance.
 * When an item in the tree is selected, controls for configuring that item are shown.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class MapContextView extends Widget {
    /**
     * The method to invoke for creating tree cells for a map item.
     */
    private static final class CellFactory extends StringConverter<TreeItem<MapItem>>
            implements Callback<TreeView<MapItem>, TreeCell<MapItem>>
    {
        /** The unique instance. */
        static final CellFactory INSTANCE = new CellFactory();

        /** The function to invoke for getting the property describing whether the item is selected. */
        private final Callback<TreeItem<MapItem>, ObservableValue<Boolean>> selectedProperty;

        /** Creates the unique instance. */
        private CellFactory() {
            selectedProperty = (item) -> item instanceof CheckBoxTreeItem<?> c ? c.selectedProperty() : null;
        }

        /** Creates an initially empty tree cell for the given tree view. */
        @Override public TreeCell<MapItem> call(final TreeView<MapItem> item) {
            return new CheckBoxTreeCell<>(selectedProperty, this);
        }

        /** Returns the text to show in the tree node. */
        @Override public String toString(final TreeItem<MapItem> item) {
            if (item != null) {
                final MapItem value = item.getValue();
                if (value != null) return value.title;
            }
            return null;
        }

        /** Returns a new item with the given text. Defined as a matter of principle, but should not be invoked. */
        @Override public TreeItem<MapItem> fromString(final String text) {
            return new ItemController(new MapItem(text));
        }
    }

    /**
     * The map items to show in a tree.
     */
    private final TreeView<MapItem> items;

    /**
     * The tree of map items in the top part,
     * together with configuration options for the selected item in the bottom part.
     */
    private final SplitPane itemsAndConfiguration;

    /**
     * The node to show when no map item is selected.
     */
    private final Region noSelection;

    /**
     * Creates an initially empty tree.
     *
     * @param  resources  the resource to use for localized labels.
     *         Should not be in public API, appears in argument for now only for convenience.
     */
    public MapContextView(final Resources resources) {
        items = new TreeView<>();
        items.setCellFactory(CellFactory.INSTANCE);
        noSelection = ItemController.createLabel(Resources.Keys.NoSelectedItem);
        itemsAndConfiguration = new SplitPane(items, noSelection);
        itemsAndConfiguration.setOrientation(Orientation.VERTICAL);
        items.getSelectionModel().selectedItemProperty().addListener((p,o,n) -> onSelected(n));
    }

    /**
     * Returns the encapsulated JavaFX component to add in a scene graph for making the tree visible.
     * The {@code Region} subclass is implementation dependent and may change in any future SIS version.
     *
     * @return the JavaFX component to insert in a scene graph.
     */
    @Override
    public Region getView() {
        return itemsAndConfiguration;
    }

    /**
     * Sets the root item.
     *
     * @param  root  the new root item, or {@code null} if none.
     */
    public void setRootItem(final ItemController root) {
        items.setRoot(root);
    }

    /**
     * Invoked when a new map item is selected.
     * This method shows the configuration panel of the selected item.
     *
     * @param  item  the selected map item, or {@code null} if none.
     */
    private void onSelected(final TreeItem<MapItem> item) {
        Region config = noSelection;
        if (item instanceof ItemController c) {
            config = c.getConfigurationPanel();
        }
        itemsAndConfiguration.getItems().set(1, config);
    }
}
