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
package org.apache.sis.gui.dataset;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.paint.Color;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.internal.Styles;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Vocabulary;


/**
 * The visual appearance of an {@link ResourceItem} in a tree.
 * Cells are initially empty; their content will be specified by {@link TreeView} after construction.
 * The text is initially "Loading…" and the actual text is obtained from the resource in a background thread.
 * The same {@code ResourceCell} instance may be recycled many times for different {@link ResourceItem} data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see ResourceItem
 */
final class ResourceCell extends TreeCell<Resource> {
    /**
     * Position of menu items in the contextual menu built by {@link #updateItem(Resource, boolean)}.
     * Above method assumes that {@link #CLOSE} is the last menu item.
     */
    private static final int COPY_PATH = 0, OPEN_FOLDER = 1, AGGREGATED = 2, CLOSE = 3;

    /**
     * Creates a new cell with initially no data.
     *
     * @param  tree  the tree which will contain this new cell
     *         (ignored, defined for matching method signature of call factory).
     */
    @SuppressWarnings("unused")
    ResourceCell(final TreeView<Resource> tree) {
    }

    /**
     * Invoked when a new resource needs to be shown in the tree view.
     * This method sets the text to a label that describes the resource,
     * possibly starting a background thread for fetching that label.
     * This method also sets a contextual menu.
     *
     * @param resource  the resource to show.
     * @param empty     whether this cell is used to fill out space.
     */
    @Override
    protected void updateItem(final Resource resource, boolean empty) {
        super.updateItem(resource, empty);          // Mandatory according JavaFX documentation.
        Color       color = Styles.NORMAL_TEXT;
        Button      more  = null;
        ContextMenu menu  = null;
        if (!empty && getTreeItem() instanceof ResourceItem item) {
            final var tree = (ResourceTree) getTreeView();
            textProperty().bind(item.label);
            if (item.isLoading()) {
                /*
                 * If the resource is in process of being loaded in a background thread, show "Loading…"
                 * with a different color. Item with null resource will be replaced by a collection of new
                 * items by a call to `CellItem.getChildren().setAll(…)` after loading process finished.
                 * Item with non-null resource only need to have their name updated.
                 */
                color = Styles.LOADING_TEXT;
                if (item.label.getValue() == null) {
                    item.label.setValue(tree.localized().getString(Resources.Keys.Loading));
                    if (resource != null) {
                        tree.fetchLabel(item.new Completer(resource, this));  // Start a background thread.
                    }
                }
            } else {
                /*
                 * If an error occurred, show the exception message with a button for more details.
                 * The list of resource children may or may not be available, depending if the error
                 * occurred while fetching the children list or only their labels.
                 */
                final Throwable error = item.error();
                if (error != null) {
                    color = Styles.ERROR_TEXT;
                    more = createErrorDetails(tree, item, error);
                }
            }
            /*
             * Following block is for the contextual menu. In current version,
             * we provide menu only for "root" resources (usually data stores).
             */
            if (tree.findOrRemove(resource, false) != null) {
                /*
                 * "Copy file path" menu item should be enabled only if we can
                 * get some kind of file path or URI from the specified resource.
                 * "Aggregated view" should be enabled only on supported resources.
                 */
                Object path;
                try {
                    path = URIDataStoreProvider.location(resource);
                } catch (DataStoreException e) {
                    path = null;
                    ResourceTree.unexpectedException("updateItem", e);
                }
                final boolean aggregatable = item.isViewSelectable(resource, TreeViewType.AGGREGATION);
                /*
                 * Create (if not already done) and configure contextual menu using above information.
                 */
                menu = getContextMenu();
                if (menu == null) {
                    menu = new ContextMenu();
                    final Resources localized = tree.localized();
                    final MenuItem[] items = new MenuItem[CLOSE + 1];
                    items[COPY_PATH]   = localized.menu(Resources.Keys.CopyFilePath, new PathAction(this, false));
                    items[OPEN_FOLDER] = localized.menu(Resources.Keys.OpenContainingFolder, new PathAction(this, true));
                    items[AGGREGATED]  = localized.menu(Resources.Keys.AggregatedView, false, (p,o,n) -> {
                        setView(n ? TreeViewType.AGGREGATION : TreeViewType.SOURCE);
                    });
                    items[CLOSE] = localized.menu(Resources.Keys.Close, (e) -> {
                        ((ResourceTree) getTreeView()).removeAndClose(getItem());
                    });
                    menu.getItems().setAll(items);
                }
                final ObservableList<MenuItem> items = menu.getItems();
                items.get(COPY_PATH).setDisable(!IOUtilities.isKindOfPath(path));
                items.get(OPEN_FOLDER).setDisable(PathAction.isBrowseDisabled(path));
                final CheckMenuItem aggregated = (CheckMenuItem) items.get(AGGREGATED);
                aggregated.setDisable(!aggregatable);
                aggregated.setSelected(aggregatable && item.isView(TreeViewType.AGGREGATION));
            }
        } else {
            textProperty().unbind();
            setText(null);
        }
        setTextFill(isSelected() ? Styles.SELECTED_TEXT : color);
        setGraphic(more);
        setContextMenu(menu);
    }

    /**
     * Invoked after a loading has been completed, either successfully or with an error.
     * If this cell view is no longer showing the given item (for example if the content
     * changed concurrently), then this method does nothing.
     *
     * @param  item   the item in which the error occurred.
     * @param  error  the error that occurred, or {@code null} if the operation was successful.
     */
    final void completed(final ResourceItem item, final Throwable error) {
        if (item == getTreeItem()) {
            Color color = Styles.NORMAL_TEXT;
            if (error != null) {
                color = Styles.ERROR_TEXT;
                setGraphic(createErrorDetails((ResourceTree) getTreeView(), item, error));
            }
            setTextFill(isSelected() ? Styles.SELECTED_TEXT : color);
        }
    }

    /**
     * Creates a button for providing details about an exception that occurred while loading the data.
     * This method also updates the label of the given item with a text that summarizes the error.
     *
     * @param  tree   value of {@link #getTreeView()}.
     * @param  item   the item in which the error occurred.
     * @param  error  the error that occurred.
     */
    private Button createErrorDetails(final ResourceTree tree, final ResourceItem item, final Throwable error) {
        if (item.label.getValue() == null) {
            String text;
            if (item.getValue() != null) {
                // We have the resource, we only failed to fetch its name.
                text = Vocabulary.forLocale(tree.locale).getString(Vocabulary.Keys.Unnamed);
            } else {
                // More serious error (no resource), show exception message.
                text = Strings.trimOrNull(Exceptions.getLocalizedMessage(error, tree.locale));
                if (text == null) text = Classes.getShortClassName(error);
            }
            item.label.setValue(text);
        }
        Button more = (Button) getGraphic();
        if (more == null) {
            more = new Button(Styles.ERROR_DETAILS_ICON);
        }
        more.setOnAction((e) -> {
            final Resources localized = tree.localized();
            ExceptionReporter.show(tree,
                    localized.getString(Resources.Keys.ErrorDetails),
                    localized.getString(Resources.Keys.CanNotReadResource), error);
        });
        return more;
    }

    /**
     * Sets the view of the resource to show in this node.
     * For example, instead of showing the components as given by the data store,
     * we can create an aggregated view of all components.
     */
    private void setView(final TreeViewType type) {
        ((ResourceItem) getTreeItem()).setView(this, type, ((ResourceTree) getTreeView()).locale);
    }
}
