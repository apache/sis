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
import java.util.Locale;
import java.util.Queue;
import java.util.List;
import java.util.LinkedList;
import java.util.Collection;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.gui.DataStoreOpener;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.system.Modules;
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
 * @version 1.3
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
     * @see #fetchLabel(ResourceItem.Completer)
     */
    private final Queue<ResourceItem.Completer> pendingItems;

    /**
     * Creates a new tree of resources with initially no resource to show.
     * For showing a resource, invoke {@link #setResource(Resource)} after construction.
     */
    public ResourceTree() {
        locale = Locale.getDefault();
        pendingItems = new LinkedList<>();
        setCellFactory((v) -> new ResourceCell());
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
        return (item == null) ? null : item.getValue();
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
        setRoot(resource == null ? null : new ResourceItem(resource));
        setShowRoot(!(resource instanceof RootResource));
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
        RootResource addTo = null;
        final TreeItem<Resource> item = getRoot();
        if (item != null) {
            final Resource root = item.getValue();
            if (root == resource) {
                return false;
            }
            if (root instanceof RootResource) {
                addTo = (RootResource) root;
            }
        }
        /*
         * We create the `RootResource` pseudo-resource even if there is only one resource.
         * A previous version created `RootResource` only if there was two or more ressources,
         * but it was causing confusing events when the second resource was added.
         */
        if (addTo == null) {
            final TreeItem<Resource> group = new TreeItem<>();
            setShowRoot(false);
            setRoot(group);                                 // Also detach `item` from the TreeView root.
            addTo = new RootResource(group, item);          // Pseudo-resource for a group of data stores.
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
     *
     * @param  store   the data store which has been loaded.
     * @param  source  the user-supplied object which was the input of the store.
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
                    ((ResourceItem) findOrRemove(store, false)).path = path;
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
     * Children of {@link Aggregate} resource are not scanned.
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
    public void removeAndClose(Resource resource) {
        final TreeItem<Resource> item = findOrRemove(resource, true);
        if (item instanceof ResourceItem) {
            resource = ((ResourceItem) item).getSource();
        }
        if (resource instanceof DataStore) {
            final DataStore store = (DataStore) resource;
            DataStoreOpener.removeAndClose(store, this);
            final EventHandler<ResourceEvent> handler = onResourceClosed.get();
            if (handler != null) {
                Path path = null;
                if (item instanceof ResourceItem) {
                    path = ((ResourceItem) item).path;
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
     * If {@code remove} is {@code true}, then it is caller's responsibility to close
     * the resource.
     *
     * @param  resource  the resource to search of remove, or {@code null}.
     * @param  remove    {@code true} for removing the resource, or {@code false} for checking only.
     * @return the item wrapping the resource, or {@code null} if the resource has not been found in the roots.
     */
    final TreeItem<Resource> findOrRemove(final Resource resource, final boolean remove) {
        assert Platform.isFxApplicationThread();
        if (resource != null) {
            /*
             * If the item to remove is selected, unselect it before to remove it.
             * The `ResourceExplorer` will be notified by a change event.
             */
            if (remove) {
                final ObservableList<TreeItem<Resource>> items = getSelectionModel().getSelectedItems();
                for (int i=items.size(); --i >= 0;) {
                    if (((ResourceItem) items.get(i)).contains(resource)) {
                        getSelectionModel().clearSelection(i);
                    }
                }
            }
            /*
             * Search for the resource from the root, and optionally remove it.
             * Intentionally use identity comparison, not `Object.equals(…)`
             * (should be consistent in whole `ResourceTree` implementation).
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
                    if (root instanceof RootResource) {
                        return ((RootResource) root).contains(resource, remove);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Updates {@link ResourceItem#label} with the resource label fetched in background thread.
     * Caller should invoke this method only if {@link ResourceItem#isLoading} is {@code true}.
     */
    final void fetchLabel(final ResourceItem.Completer item) {
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
                    final ResourceItem.Completer c;
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
}
