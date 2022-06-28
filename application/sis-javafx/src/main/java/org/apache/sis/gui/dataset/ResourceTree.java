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

import java.io.File;
import java.nio.file.Path;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.file.FileSystemNotFoundException;
import java.util.AbstractList;
import java.util.Locale;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Optional;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.gui.DataStoreOpener;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.LogHandler;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;

import static java.util.logging.Logger.getLogger;


/**
 * A view of data {@link Resource}s organized as a tree.
 * This view can be used for showing the content of one or many {@link DataStore}s.
 * A resource can be added by a call to {@link #addResource(Resource)} or loaded from
 * a file by {@link #loadResource(Object)}.
 *
 * <p>{@code ResourceTree} registers the necessarily handlers for making this view a target
 * of "drag and drop" events. Users can drop files or URLs for opening data files.</p>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The {@link #rootProperty() rootProperty} should be considered read-only.
 *       For changing content, use the {@link #setResource(Resource)} instead.</li>
 *   <li>If the user selects "close" in the contextual menu, the resource is unconditionally closed
 *       (if it is an instance of {@link DataStore}). There is not yet a mechanism for keeping it open
 *       if the resource is shared by another {@link ResourceTree} instance.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public class ResourceTree extends TreeView<Resource> {
    /**
     * The locale to use for titles, messages, labels, contextual menu, <i>etc</i>.
     */
    final Locale locale;

    /**
     * Function to be called after a resource has been loaded from a file or URL.
     * The default value is {@code null}.
     *
     * @see #loadResource(Object)
     * @see ResourceEvent#LOADED
     */
    public final ObjectProperty<EventHandler<ResourceEvent>> onResourceLoaded;

    /**
     * Function to be called after a resource has been closed from a file or URL.
     * The default value is {@code null}.
     *
     * @see #removeAndClose(Resource)
     * @see ResourceEvent#CLOSED
     *
     * @since 1.2
     */
    public final ObjectProperty<EventHandler<ResourceEvent>> onResourceClosed;

    /**
     * Tree node items (wrappers around {@link Resource} instances) waiting to be
     * completed with the item labels. Those labels are fetched in a background thread.
     * All accesses to this list must be synchronized on {@code pendingItems}.
     *
     * <h4>Design note</h4>
     * We use a list instead of creating a {@link Task} for each item because the latter can create a lot
     * of threads, which are likely to be blocked anyway because of {@link DataStore} synchronization.
     * Furthermore those threads of overkill in the common case where labels are very quick to fetch.
     *
     * @see #fetchLabel(Item.Completer)
     */
    private final Queue<Item.Completer> pendingItems;

    /**
     * Creates a new tree of resources with initially no resource to show.
     * For showing a resource, invoke {@link #setResource(Resource)} after construction.
     */
    public ResourceTree() {
        locale = Locale.getDefault();
        pendingItems = new LinkedList<>();
        setCellFactory((v) -> new Cell());
        setOnDragOver(ResourceTree::onDragOver);
        setOnDragDropped(this::onDragDropped);
        onResourceLoaded = new SimpleObjectProperty<>(this, "onResourceLoaded");
        onResourceClosed = new SimpleObjectProperty<>(this, "onResourceClosed");
    }

    /**
     * Returns the root {@link Resource} of this tree.
     * The returned value depends on how the resource was set:
     *
     * <ul>
     *   <li>If the resource was specified by {@link #setResource(Resource)},
     *       then this method returns that resource.
     *       This is often (but not necessarily) a {@link DataStore}.</li>
     *   <li>If one or more resources were specified by {@link #addResource(Resource)},
     *       then this method returns an {@link Aggregate} of all added resources.</li>
     * </ul>
     *
     * @return root {@link Resource}, or {@code null} if none.
     */
    public Resource getResource() {
        final TreeItem<Resource> item = getRoot();
        return item == null ? null : item.getValue();
    }

    /**
     * Sets the root {@link Resource} of this tree.
     * The root resource is typically, but not necessarily, a {@link DataStore} instance.
     * If another root resource existed before this method call, it is discarded without being closed.
     * Closing the previous resource is caller's responsibility.
     *
     * <h4>Modified tree view properties</h4>
     * This method updates the {@link #setRoot root} and {@link #setShowRoot showRoot}
     * properties of {@link TreeView} in an implementation-dependent way.
     *
     * @param  resource  the root resource to show, or {@code null} if none.
     *
     * @see #addResource(Resource)
     * @see #removeAndClose(Resource)
     */
    public void setResource(final Resource resource) {
        setRoot(resource == null ? null : new Item(resource));
        setShowRoot(!(resource instanceof Root));
    }

    /**
     * Adds a resource in this tree below previously added resources.
     * This method does nothing if the given resource is already present in this tree.
     *
     * <h4>Modified tree view properties</h4>
     * This method updates the {@link #setRoot root} and {@link #setShowRoot showRoot}
     * properties of {@link TreeView} in an implementation-dependent way.
     *
     * @param  resource  the root resource to add, or {@code null} if none.
     * @return {@code true} if the given resource has been added, or {@code false}
     *         if it was already presents or if the given resource is {@code null}.
     *
     * @see #setResource(Resource)
     * @see #removeAndClose(Resource)
     */
    public boolean addResource(final Resource resource) {
        assert Platform.isFxApplicationThread();
        if (resource == null) {
            return false;
        }
        Root addTo = null;
        final TreeItem<Resource> item = getRoot();
        if (item != null) {
            final Resource root = item.getValue();
            if (root == resource) {
                return false;
            }
            if (root instanceof Root) {
                addTo = (Root) root;
            }
        }
        /*
         * We create the `Root` pseudo-resource even if there is only one resource.
         * A previous version created `Root` only if there was two or more ressources,
         * but it was causing confusing events when the second resource was added.
         */
        if (addTo == null) {
            final TreeItem<Resource> group = new TreeItem<>();
            setShowRoot(false);
            setRoot(group);                                 // Also detach `item` from the TreeView root.
            addTo = new Root(group, item);                  // Pseudo-resource for a group of data stores.
            group.setValue(addTo);
        }
        return addTo.add(resource);
    }

    /**
     * Loads in a background thread the resources from the given source,
     * then {@linkplain #addResource(Resource) adds the resource} to this tree.
     * If the resource has already been loaded, then this method will use the
     * existing instance instead of loading the data again.
     *
     * <h4>Notifications</h4>
     * If {@link #onResourceLoaded} has a non-null value, the {@link EventHandler} will be
     * notified in JavaFX thread after the background thread finished to open the resource.
     * If an exception occurs while opening the resource, then {@link EventHandler} is not
     * notified and the error is reported in a dialog box instead.
     *
     * @param  source  the source of the resource to load. This is usually
     *                 a {@link java.io.File} or {@link java.nio.file.Path}.
     *
     * @see ResourceExplorer#loadResources(Collection)
     */
    public void loadResource(final Object source) {
        if (source != null) {
            if (source instanceof Resource) {
                addResource((Resource) source);
            } else {
                final DataStoreOpener opener = new DataStoreOpener(source);
                final DataStore existing = opener.fromCache();
                if (existing != null) {
                    addResource(existing);
                } else {
                    opener.setOnSucceeded((event) -> {
                        addLoadedResource((DataStore) event.getSource().getValue(), source);
                    });
                    opener.setOnFailed((event) -> ExceptionReporter.show(this, event));
                    BackgroundThreads.execute(opener);
                }
            }
        }
    }

    /**
     * Adds the given store as a resource, then notifies {@link #onResourceLoaded}
     * handler that a resource at the given path has been loaded.
     * This method is invoked from JavaFX thread.
     */
    private void addLoadedResource(final DataStore store, final Object source) {
        final boolean added = addResource(store);
        final EventHandler<ResourceEvent> handler = onResourceLoaded.getValue();
        if (handler != null) {
            final Path path;
            try {
                path = IOUtilities.toPathOrNull(source);
            } catch (IllegalArgumentException | FileSystemNotFoundException e) {
                recoverableException("loadResource", e);
                return;
            }
            if (path != null) {
                /*
                 * Following call should be quick because it starts the search from last item.
                 * A `NullPointerException` or `ClassCastException` here would be a bug in our
                 * wrapping of resources.
                 */
                if (added) {
                    ((Item) findOrRemove(store, false)).path = path;
                }
                handler.handle(new ResourceEvent(this, path, ResourceEvent.LOADED));
            }
        }
    }

    /**
     * Invoked when the user drops files or a URL on this resource tree.
     * This method starts the loading processes in a background thread.
     * The loading is started by calls to {@link #loadResource(Object)}.
     *
     * @param  event  the "drag and drop" event.
     */
    private void onDragDropped(final DragEvent event) {
        final Dragboard db = event.getDragboard();
        final List<File> files = db.getFiles();
        boolean success = false;
        if (files != null) {
            for (final File file : files) {
                loadResource(file);
            }
            success = true;
        } else {
            final String url = db.getUrl();
            if (url != null) try {
                loadResource(new URL(url));
                success = true;
            } catch (MalformedURLException e) {
                /*
                 * Try to take only the filename, taken as the text after last '/' ignoring
                 * the very last character (this is the purpose of the `length - 2` part).
                 * The resulting `start` will be 0 if no '/' is found.
                 */
                final int start = url.lastIndexOf('/', url.length() - 2) + 1;
                int stop = url.indexOf('?', start);
                if (stop <= 0) stop = url.length();
                ExceptionReporter.canNotReadFile(this, url.substring(start, stop), e);
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * Invoked when the user drags something over the resource tree but has not yet dropped them.
     * This method determines if the {@link ResourceTree} accepts this drag.
     */
    private static void onDragOver(final DragEvent event) {
        final Dragboard db = event.getDragboard();
        if (db.hasFiles() || db.hasUrl()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    /**
     * Removes the given resource from this tree and closes the resource if it is a {@link DataStore} instance.
     * It is caller's responsibility to ensure that the given resource is not used anymore.
     *
     * <p>Only the "root" resources (such as the resources given to {@link #setResource(Resource)} or
     * {@link #addResource(Resource)} methods) can be removed.
     * Children of {@link Aggregate} resource and not scanned.
     * If the given resource can not be removed, then this method does nothing.</p>
     *
     * <h4>Notifications</h4>
     * If {@link #onResourceClosed} has a non-null value, the {@link EventHandler} will be notified.
     * The notification may happen in same time that the resource is closing in a background thread.
     * If an exception occurs while closing the resource, the error is reported in a dialog box.
     *
     * @param  resource  the resource to remove. Null values are ignored.
     *
     * @see #setResource(Resource)
     * @see #addResource(Resource)
     * @see ResourceExplorer#removeAndClose(Resource)
     */
    public void removeAndClose(final Resource resource) {
        final TreeItem<Resource> item = findOrRemove(resource, true);
        if (item != null && resource instanceof DataStore) {
            final DataStore store = (DataStore) resource;
            DataStoreOpener.removeAndClose(store, this);
            final EventHandler<ResourceEvent> handler = onResourceClosed.get();
            if (handler != null) {
                Path path = null;
                if (item instanceof Item) {
                    path = ((Item) item).path;
                }
                if (path == null) try {
                    path = store.getOpenParameters()
                            .map((p) -> IOUtilities.toPathOrNull(p.parameter(DataStoreProvider.LOCATION).getValue()))
                            .orElse(null);
                } catch (IllegalArgumentException | FileSystemNotFoundException e) {
                    // Ignore because the location parameter is optional.
                    recoverableException("removeAndClose", e);
                }
                if (path != null) {
                    handler.handle(new ResourceEvent(this, path, ResourceEvent.CLOSED));
                }
            }
        }
    }

    /**
     * Verifies if the given resource is one of the roots, and optionally removes it.
     *
     * @param  resource  the resource to search of remove, or {@code null}.
     * @param  remove    {@code true} for removing the resource, or {@code false} for checking only.
     * @return the item wrapping the resource, or {@code null} if the resource has not been found in the roots.
     */
    private TreeItem<Resource> findOrRemove(final Resource resource, final boolean remove) {
        assert Platform.isFxApplicationThread();
        if (resource != null) {
            /*
             * If the item to remove is selected, unselect it before to remove it.
             * The `ResourceExplorer` will be notified by a change event.
             */
            if (remove) {
                final ObservableList<TreeItem<Resource>> items = getSelectionModel().getSelectedItems();
                for (int i=items.size(); --i >= 0;) {
                    if (items.get(i).getValue() == resource) {
                        getSelectionModel().clearSelection(i);
                    }
                }
            }
            /*
             * Search for the resource from the root, and optionally remove it.
             */
            final TreeItem<Resource> item = getRoot();
            if (item != null) {
                final Resource root = item.getValue();
                if (root != null) {
                    if (root == resource) {
                        if (remove) {
                            setRoot(null);
                        }
                        return item;
                    }
                    if (root instanceof Root) {
                        return ((Root) root).contains(resource, remove);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Updates {@link Item#label} with the resource label fetched in background thread.
     * Caller should invoke this method only if {@link Item#isLoading} is {@code true}.
     */
    private void fetchLabel(final Item.Completer item) {
        final boolean isEmpty;
        synchronized (pendingItems) {
            // The two operations below must be atomic (this is why we do not use ConcurrentLinkedQueue).
            isEmpty = pendingItems.isEmpty();
            pendingItems.add(item);
        }
        if (isEmpty) {
            // Not a problem if 2 tasks are launched in parallel.
            BackgroundThreads.execute(() -> {
                for (;;) {
                    final Item.Completer c;
                    synchronized (pendingItems) {
                        c = pendingItems.poll();
                    }
                    if (c == null) break;
                    c.fetch(locale);
                }
            });
        }
    }

    /**
     * Returns resources for current locale.
     */
    final Resources localized() {
        return Resources.forLocale(locale);
    }

    /**
     * Returns a localized (if possible) string representation of the given exception.
     * This method returns the message if one exist, or the exception class name otherwise.
     */
    private static String string(final Throwable failure, final Locale locale) {
        String text = Strings.trimOrNull(Exceptions.getLocalizedMessage(failure, locale));
        if (text == null) {
            text = Classes.getShortClassName(failure);
        }
        return text;
    }

    /**
     * Reports an ignorable exception in the given method.
     */
    private static void recoverableException(final String method, final Exception e) {
        Logging.recoverableException(getLogger(Modules.APPLICATION), ResourceTree.class, method, e);
    }

    /**
     * Reports an unexpected but non-fatal exception in the given method.
     */
    static void unexpectedException(final String method, final Exception e) {
        Logging.unexpectedException(getLogger(Modules.APPLICATION), ResourceTree.class, method, e);
    }




    /**
     * The visual appearance of an {@link Item} in a tree. Cells are initially empty;
     * their content will be specified by {@link TreeView} after construction.
     * This class gets the cell text from a resource by a call to
     * {@link DataStoreOpener#findLabel(Resource, Locale, boolean)} in a background thread.
     * The same call may be recycled many times for different {@link Item} data.
     *
     * @see Item
     */
    private static final class Cell extends TreeCell<Resource> {
        /**
         * Creates a new cell with initially no data.
         */
        Cell() {
        }

        /**
         * Invoked when a new resource needs to be shown in the tree view.
         * This method sets the text to a label that describe the resource.
         *
         * @param resource  the resource to show.
         * @param empty     whether this cell is used to fill out space.
         */
        @Override
        protected void updateItem(final Resource resource, boolean empty) {
            super.updateItem(resource, empty);          // Mandatory according JavaFX documentation.
            Color       color = Styles.NORMAL_TEXT;
            String      text  = null;
            Button      more  = null;
            ContextMenu menu  = null;
            final TreeItem<Resource> t;
            if (!empty && (t = getTreeItem()) instanceof Item) {
                final ResourceTree tree = (ResourceTree) getTreeView();
                final Item item = (Item) t;
                final Throwable error;
                text = item.label;
                if (item.isLoading) {
                    /*
                     * If the resource is in process of being loaded in a background thread, show "Loading…"
                     * with a different color. Item with null resource will be replaced by a collection of new
                     * items by a call to `CellItem.getChildren().setAll(…)` after loading process finished.
                     * Item with non-null resource only need to have their name updated.
                     */
                    color = Styles.LOADING_TEXT;
                    if (text == null) {
                        text = item.label = tree.localized().getString(Resources.Keys.Loading);
                        if (resource != null) {
                            tree.fetchLabel(item.new Completer(resource));      // Start a background thread.
                        }
                    }
                } else if ((error = item.error) != null) {
                    /*
                     * If an error occurred, show the exception message with a button for more details.
                     * The list of resource children may or may not be available, depending if the error
                     * occurred while fetching the children list or only their labels.
                     */
                    color = Styles.ERROR_TEXT;
                    if (text == null) {
                        if (resource != null) {
                            // We have the resource, we only failed to fetch its name.
                            text = Vocabulary.getResources(tree.locale).getString(Vocabulary.Keys.Unnamed);
                        } else {
                            // More serious error (no resource), show exception message.
                            text = string(error, tree.locale);
                        }
                        item.label = text;
                    }
                    more = (Button) getGraphic();
                    if (more == null) {
                        more = new Button(Styles.ERROR_DETAILS_ICON);
                    }
                    more.setOnAction((e) -> {
                        final Resources localized = tree.localized();
                        ExceptionReporter.show(tree,
                                localized.getString(Resources.Keys.ErrorDetails),
                                localized.getString(Resources.Keys.CanNotReadResource), error);
                    });
                }
                /*
                 * If the resource is one of the "root" resources, add a menu for removing it.
                 * If we find that the cell already has a menu, we do not need to build it again.
                 */
                if (tree.findOrRemove(resource, false) != null) {
                    menu = getContextMenu();
                    if (menu == null) {
                        menu = new ContextMenu();
                        final Resources localized = tree.localized();
                        final MenuItem[] items = new MenuItem[CLOSE + 1];
                        items[COPY_PATH]   = localized.menu(Resources.Keys.CopyFilePath, new PathAction(this, false));
                        items[OPEN_FOLDER] = localized.menu(Resources.Keys.OpenContainingFolder, new PathAction(this, true));
                        items[CLOSE]       = localized.menu(Resources.Keys.Close, (e) -> {
                            ((ResourceTree) getTreeView()).removeAndClose(getItem());
                        });
                        menu.getItems().setAll(items);
                    }
                    /*
                     * "Copy file path" menu item should be enabled only if we can
                     * get some kind of file path or URI from the specified resource.
                     */
                    Object path;
                    try {
                        path = URIDataStore.location(resource);
                    } catch (DataStoreException e) {
                        path = null;
                        unexpectedException("updateItem", e);
                    }
                    menu.getItems().get(COPY_PATH).setDisable(!IOUtilities.isKindOfPath(path));
                    menu.getItems().get(OPEN_FOLDER).setDisable(PathAction.isBrowseDisabled || IOUtilities.toFile(path) == null);
                }
            }
            setText(text);
            setTextFill(isSelected() ? Styles.SELECTED_TEXT : color);
            setGraphic(more);
            setContextMenu(menu);
        }

        /**
         * Position of menu items in the contextual menu built by {@link #updateItem(Resource, boolean)}.
         * Above method assumes that {@link #CLOSE} is the last menu item.
         */
        private static final int COPY_PATH = 0, OPEN_FOLDER = 1, CLOSE = 2;
    }




    /**
     * An item of the {@link Resource} tree completed with additional information.
     * The list of children is fetched in a background thread when first needed.
     * This node contains only the data; for visual appearance, see {@link Cell}.
     *
     * @see Cell
     */
    private static final class Item extends TreeItem<Resource> {
        /**
         * The path to the resource, or {@code null} if none or unknown. This is used for notifications only;
         * this information does not play an important role for {@link ResourceTree} itself.
         */
        Path path;

        /**
         * The text of this node, computed and cached when first needed. Computation is done by invoking
         * {@link DataStoreOpener#findLabel(Resource, Locale, boolean)} in a background thread.
         *
         * @see #fetchLabel(Item.Completer)
         */
        String label;

        /**
         * Whether this node is in process of loading data. There is two kinds of loading:
         * <ul>
         *   <li>The {@link Resource} itself, in which case {@link #getValue()} is null.</li>
         *   <li>The resource {@link #title}, in which case {@link #getValue()} has a valid value.</li>
         * </ul>
         */
        boolean isLoading;

        /**
         * If an error occurred while loading the resource, the cause. The {@link #getValue()} property may
         * be null or non-null, depending if the error occurred while loading the resource or only its title.
         */
        Throwable error;

        /**
         * Whether the resource is a leaf. A resource is a leaf if it is not an
         * instance of {@link Aggregate}, in which case it can not have children.
         * This information is cached because requested often.
         */
        private final boolean isLeaf;

        /**
         * Whether the list of children has been determined. We use this flag in order
         * to fetch children only when first requested, since this process is costly.
         *
         * @todo Register {@link org.apache.sis.storage.event.StoreListener} and reset
         *       this flag to {@code false} if the resource content or structure changed.
         */
        private boolean isChildrenKnown;

        /**
         * Creates a temporary item with null value for a resource in process of being loaded.
         * This item will be replaced (not updated) by a fresh {@code Item} instance when the
         * resource will become available.
         */
        Item() {
            isLeaf    = true;
            isLoading = true;
        }

        /**
         * Creates an item for a resource that we failed to load.
         */
        Item(final Throwable exception) {
            isLeaf = true;
            error  = exception;
        }

        /**
         * Creates a new node for the given resource.
         *
         * @param resource  the resource to show in the tree.
         */
        Item(final Resource resource) {
            super(resource);
            isLoading = true;       // Means that the label still need to be fetched.
            isLeaf = !(resource instanceof Aggregate);
            LogHandler.installListener(resource);
        }

        /**
         * Update {@link #label} with the resource label fetched in background thread.
         * Caller should invoke this method only if {@link #isLoading} is {@code true}.
         */
        final class Completer implements Runnable {
            /** The resource for which to fetch a label. */
            private final Resource resource;

            /** Result of fetching the label of a resource. */
            private String result;

            /** Error that occurred while fetching the label. */
            private Throwable failure;

            /** Creates a new container for the label of a resource. */
            Completer(final Resource resource) {
                this.resource = resource;
            }

            /** Invoked in a background thread for fetching the label. */
            final void fetch(final Locale locale) {
                try {
                    result = DataStoreOpener.findLabel(resource, locale, false);
                } catch (Throwable e) {
                    failure = e;
                }
                Platform.runLater(this);
            }

            /** Invoked in JavaFX thread after the label has been fetched. */
            public void run() {
                isLoading = false;
                label     = result;
                error     = failure;
                GUIUtilities.forceCellUpdate(Item.this);
            }
        }

        /**
         * Returns whether the resource can not have children.
         */
        @Override
        public boolean isLeaf() {
            return isLeaf;
        }

        /**
         * Returns the items for all sub-resources contained in this resource.
         * The list is empty if the resource is not an aggregate.
         */
        @Override
        public ObservableList<TreeItem<Resource>> getChildren() {
            final ObservableList<TreeItem<Resource>> children = super.getChildren();
            if (!isChildrenKnown) {
                isChildrenKnown = true;                 // Set first for avoiding to repeat in case of failure.
                final Resource resource = getValue();
                if (resource instanceof Aggregate) {
                    BackgroundThreads.execute(new GetChildren((Aggregate) resource));
                    children.add(new Item());
                }
            }
            return children;
        }

        /**
         * The task to execute in a background thread for fetching the children.
         */
        private final class GetChildren extends Task<List<TreeItem<Resource>>> {
            /**
             * The aggregate from which to get the children.
             */
            private final Aggregate resource;

            /**
             * Creates a new background task for fetching the children from the given resource.
             */
            GetChildren(final Aggregate resource) {
                this.resource = resource;
            }

            /**
             * Invoked in a background thread for fetching the children of the resource
             * specified at construction time.
             */
            @Override
            protected List<TreeItem<Resource>> call() throws DataStoreException {
                final List<TreeItem<Resource>> items = new ArrayList<>();
                final Long id = LogHandler.loadingStart(resource);
                try {
                    for (final Resource component : resource.components()) {
                        items.add(new Item(component));
                    }
                } finally {
                    LogHandler.loadingStop(id);
                }
                return items;
            }

            /**
             * Invoked in JavaFX thread if children have been loaded successfully.
             * The previous node, which was showing "Loading…", is replaced by all
             * nodes loaded in the background thread.
             */
            @Override
            protected void succeeded() {
                Item.super.getChildren().setAll(getValue());
            }

            /**
             * Invoked in JavaFX thread if children can not be loaded.
             */
            @Override
            @SuppressWarnings("unchecked")
            protected void failed() {
                Item.super.getChildren().setAll(new Item(getException()));
            }
        }
    }




    /**
     * The root pseudo-resource for allowing the tree to contain more than one resource.
     * This root node should be hidden in the {@link ResourceTree}.
     */
    private static final class Root implements Aggregate {
        /**
         * The children to expose as an unmodifiable list of components.
         */
        private final List<TreeItem<Resource>> components;

        /**
         * Creates a new aggregate which is going to be wrapped in the given node.
         * Caller shall invoke {@code group.setValue(root)} after this constructor.
         *
         * @param  group     the new tree root which will contain "real" resources.
         * @param  previous  the previous root, to be added in the new group.
         */
        Root(final TreeItem<Resource> group, final TreeItem<Resource> previous) {
            components = group.getChildren();
            if (previous != null) {
                components.add(previous);
            }
        }

        /**
         * Checks whether this root contains the given resource as a direct child.
         * This method does not search recursively in sub-trees.
         *
         * @param  resource  the resource to search.
         * @param  remove    whether to remove the resource if found.
         * @return the resource wrapper, or {@code null} if not found.
         */
        TreeItem<Resource> contains(final Resource resource, final boolean remove) {
            for (int i=components.size(); --i >= 0;) {
                final TreeItem<Resource> item = components.get(i);
                if (item.getValue() == resource) {
                    return remove ? components.remove(i) : item;
                }
            }
            return null;
        }

        /**
         * Adds the given resource if not already present.
         *
         * @param  resource  the resource to add.
         * @return whether the given resource has been added.
         */
        boolean add(final Resource resource) {
            for (int i = components.size(); --i >= 0;) {
                if (components.get(i).getValue() == resource) {
                    return false;
                }
            }
            return components.add(new Item(resource));
        }

        /**
         * Returns a read-only view of the components. This method is not used directly by {@link ResourceTree}
         * but is defined in case a user invoke {@link ResourceTree#getResource()}. For this reason, it is not
         * worth to cache the list created in this method.
         */
        @Override
        public Collection<Resource> components() {
            return new AbstractList<Resource>() {
                @Override public int size() {
                    return components.size();
                }

                @Override public Resource get(final int index) {
                    return components.get(index).getValue();
                }
            };
        }

        /**
         * Returns empty optional since this resource has no identifier.
         */
        @Override
        public Optional<GenericName> getIdentifier() {
            return Optional.empty();
        }

        /**
         * Returns null since this resource has no metadata. Returning null is normally
         * not allowed for this method, but {@link ResourceTree} is robust to this case.
         */
        @Override
        public Metadata getMetadata() {
            return null;
        }

        /** Ignored since this class does not emit any event. */
        @Override public <T extends StoreEvent> void    addListener(Class<T> eventType, StoreListener<? super T> listener) {}
        @Override public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {}
    }
}
