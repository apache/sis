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
import java.net.URL;
import java.net.MalformedURLException;
import java.util.AbstractList;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.internal.gui.ResourceLoader;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.util.Strings;


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
 * If the user selects "close" in the contextual menu, the resource if closed (if it is an instance
 * of {@link DataStore}). There is not yet a mechanism for keeping it open if the resource is shared
 * by another {@link ResourceTree} instance.
 *
 * @todo Listen to warnings and save log records in a separated collection for each data store.
 *       Add to the contextual menu an option for viewing the log records of selected data store.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ResourceTree extends TreeView<Resource> {
    /**
     * The resources for localized strings. Stored because needed often
     * (when loading data, when building a contextual menu, etc.).
     */
    final Resources localized;

    /**
     * Creates a new tree of resources with initially no resource to show.
     * For showing a resource, invoke {@link #setResource(Resource)} after construction.
     */
    public ResourceTree() {
        localized = Resources.forLocale(Locale.getDefault(Locale.Category.DISPLAY));
        setCellFactory(ResourceTree::newCell);
        setOnDragOver(ResourceTree::onDragOver);
        setOnDragDropped(this::load);
    }

    /**
     * Returns the root {@link Resource} of this tree.
     * This is often (but not necessarily) a {@link DataStore}.
     *
     * @return root {@link Resource}, or {@code null} if none.
     */
    public Resource getResource() {
        final TreeItem<Resource> item = getRoot();
        return item == null ? null : item.getValue();
    }

    /**
     * Sets the root {@link Resource} of this tree. The root resource is typically,
     * but not necessarily, a {@link DataStore} instance. If other resources existed
     * before this method call, they are discarded.
     *
     * <p>This method updates the {@link #setRoot root} and {@link #setShowRoot showRoot}
     * properties of {@link TreeView}.</p>
     *
     * @param  resource  the root resource to show, or {@code null} if none.
     */
    public void setResource(final Resource resource) {
        setRoot(resource == null ? null : new Item(resource));
        setShowRoot(!(resource instanceof Root));
    }

    /**
     * Adds a resource to this tree. If this tree is empty, then invoking this method
     * has the same effect than invoking {@link #setResource(Resource)}. Otherwise this
     * method add the new resource below previously added resources if not already present.
     *
     * <p>This method updates the {@link #setRoot root} and {@link #setShowRoot showRoot}
     * properties of {@link TreeView}.</p>
     *
     * @param  resource  the root resource to add, or {@code null} if none.
     * @return {@code true} if the given resource has been added, or {@code false}
     *         if it was already presents or if the given resource is {@code null}.
     */
    public boolean addResource(final Resource resource) {
        assert Platform.isFxApplicationThread();
        if (resource == null) {
            return false;
        }
        final TreeItem<Resource> item = getRoot();
        if (item != null) {
            final Resource root = item.getValue();
            if (root != null) {
                if (root == resource) {
                    return false;
                }
                final Root addTo;
                if (root instanceof Root) {
                    addTo = (Root) root;
                } else {
                    final TreeItem<Resource> group = new TreeItem<>();
                    addTo = new Root(group, root);
                    group.setValue(addTo);
                    setRoot(group);
                    setShowRoot(false);
                }
                return addTo.add(resource);
            }
        }
        setRoot(new Item(resource));
        setShowRoot(true);
        return true;
    }

    /**
     * Loads in a background thread the resources from the given source,
     * then {@linkplain #addResource(Resource) adds the resource} to this tree.
     * If the resource has already been loaded, then this method will use the
     * existing instance instead than loading the data again.
     *
     * @param  source  the source of the resource to load. This is usually
     *                 a {@link java.io.File} or {@link java.nio.file.Path}.
     */
    public void loadResource(final Object source) {
        if (source != null) {
            if (source instanceof Resource) {
                addResource((Resource) source);
            } else {
                final ResourceLoader loader = new ResourceLoader(source);
                final Resource existing = loader.fromCache();
                if (existing != null) {
                    addResource(existing);
                } else {
                    loader.setOnSucceeded((event) -> addResource((Resource) event.getSource().getValue()));
                    loader.setOnFailed(ExceptionReporter::show);
                    BackgroundThreads.execute(loader);
                }
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
    private void load(final DragEvent event) {
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
                ExceptionReporter.canNotReadFile(url.substring(start, stop), e);
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
     * Removes the given resource from the tree and closes it if it is a {@link DataStore}.
     * It is caller's responsibility to ensure that the given resource is not used anymore.
     * A resource can be removed only if it is a root. If the given resource is not in this
     * tree view or is not a root resource, then this method does nothing.
     *
     * @param  resource  the resource to remove, or {@code null}.
     */
    public void removeAndClose(final Resource resource) {
        if (findOrRemove(resource, true)) {
            if (resource instanceof DataStore) {
                ResourceLoader.removeAndClose((DataStore) resource);
            }
        }
    }

    /**
     * Verifies if the given resource is one of the roots, and optionally removes it.
     *
     * @param  resource  the resource to search of remove, or {@code null}.
     * @param  remove    {@code true} for removing the resource, or {@code false} for checking only.
     * @return whether the resource has been found in the roots.
     */
    private boolean findOrRemove(final Resource resource, final boolean remove) {
        assert Platform.isFxApplicationThread();
        if (resource != null) {
            final TreeItem<Resource> item = getRoot();
            if (item != null) {
                final Resource root = item.getValue();
                if (root != null) {
                    if (root == resource) {
                        if (remove) {
                            setRoot(null);
                        }
                        return true;
                    }
                    if (root instanceof Root) {
                        if (remove) {
                            return ((Root) root).remove(resource);
                        } else {
                            return ((Root) root).contains(resource);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Invoked when a new cell needs to be created. This method creates a specialized instance
     * which will get the cell text from a resource by a call to {@link #getTitle(Resource)}.
     *
     * @param  tree  the {@link ResourceTree} for which to create a cell.
     * @return a new cell renderer for the given tree.
     */
    private static TreeCell<Resource> newCell(final TreeView<Resource> tree) {
        return new Cell();
    }

    /**
     * Returns the locale to use for titles, messages, labels, etc.
     */
    final Locale getLocale() {
        return localized.getLocale();
    }

    /**
     * Returns a label for a resource. Current implementation returns the
     * {@linkplain DataStore#getDisplayName() data store display name} if available,
     * or the title found in {@linkplain Resource#getMetadata() metadata} otherwise.
     *
     * @param  resource  the resource for which to get a label, or {@code null}.
     * @return the resource display name or the citation title, never null.
     */
    private String getTitle(final Resource resource) {
        Throwable failure = null;
        if (resource != null) try {
            /*
             * The data store display name is typically the file name. We give precedence to that name
             * instead than the citation title because the citation may be the same for many files of
             * the same product, while the display name have better chances to be distinct for each file.
             */
            if (resource instanceof DataStore) {
                final String name = Strings.trimOrNull(((DataStore) resource).getDisplayName());
                if (name != null) return name;
            }
            /*
             * Search for a title in metadata first because it has better chances
             * to be human-readable compared to the resource identifier.
             */
            Collection<? extends Identification> identifications = null;
            final Metadata metadata = resource.getMetadata();
            if (metadata != null) {
                identifications = metadata.getIdentificationInfo();
                if (identifications != null) {
                    for (final Identification identification : identifications) {
                        final Citation citation = identification.getCitation();
                        if (citation != null) {
                            final String t = string(citation.getTitle());
                            if (t != null) return t;
                        }
                    }
                }
            }
            /*
             * If we find no title in the metadata, use the resource identifier.
             * We search of explicitly declared identifier first before to fallback
             * on metadata, because the later is more subject to interpretation.
             */
            final Optional<GenericName> id = resource.getIdentifier();
            if (id.isPresent()) {
                final String t = string(id.get().toInternationalString());
                if (t != null) return t;
            }
            if (identifications != null) {
                for (final Identification identification : identifications) {
                    final String t = Citations.getIdentifier(identification.getCitation());
                    if (t != null) return t;
                }
            }
        } catch (DataStoreException | RuntimeException e) {
            failure = e;
        }
        /*
         * If we failed to get the name, use "unnamed" with the exception message.
         * It may still be possible to select this resource, view it or expand the children nodes.
         */
        String text = Vocabulary.getResources(getLocale()).getString(Vocabulary.Keys.Unnamed);
        if (failure != null) {
            text = text + " — " + string(failure);
        }
        return text;
    }

    /**
     * Returns the given international string as a non-empty localized string, or {@code null} if none.
     */
    private String string(final InternationalString i18n) {
        return (i18n != null) ? Strings.trimOrNull(i18n.toString(getLocale())) : null;
    }

    /**
     * Returns a localized (if possible) string representation of the given exception.
     * This method returns the message if one exist, or the exception class name otherwise.
     */
    private String string(final Throwable failure) {
        String text = Strings.trimOrNull(Exceptions.getLocalizedMessage(failure, getLocale()));
        if (text == null) {
            text = Classes.getShortClassName(failure);
        }
        return text;
    }

    /**
     * The visual appearance of an {@link Item} in a tree. Cells are initially empty;
     * their content will be specified by {@link TreeView} after construction.
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
         * Invoked when a new resource need to be shown in the tree view.
         * This method sets the text to a title that describe the resource.
         *
         * @param resource  the resource to show.
         * @param empty     whether this cell is used to fill out space.
         */
        @Override
        protected void updateItem(final Resource resource, boolean empty) {
            /*
             * This method is sometime invoked even if the resource is the same. It may be for example
             * because the selected state changed. In such case, we do not need to construct again the
             * title, contextual menu, etc. Only the color may change. More generally we don't need to
             * fetch data from enclosing ResourceTree if the resource is the same, so we mark this case
             * by setting `tree` to null.
             */
            final ResourceTree tree = (getItem() != resource) ? (ResourceTree) getTreeView() : null;
            super.updateItem(resource, empty);          // Mandatory according JavaFX documentation.
            Color color = Styles.NORMAL_TEXT;
            String text = null;
            if (!empty) {
                if (resource == PseudoResource.LOADING) {
                    color = Styles.LOADING_TEXT;
                    if (tree != null) text = tree.localized.getString(Resources.Keys.Loading);
                } else if (resource instanceof Unloadable) {
                    color = Styles.ERROR_TEXT;
                    if (tree != null) text = tree.string(((Unloadable) resource).failure);
                } else {
                    if (tree != null) text = tree.getTitle(resource);
                }
            }
            setTextFill(isSelected() ? Styles.SELECTED_TEXT : color);
            /*
             * If the resource is at the root, add a menu for removing it.
             * If we find that the cell already has a menu, we do not need
             * to build it again (it may change in a future SIS version if
             * the menu is not always the same).
             */
            if (tree != null) {
                setText(text);
                ContextMenu menu = null;
                if (tree.findOrRemove(resource, false)) {
                    menu = getContextMenu();
                    if (menu == null) {
                        final Resources localized = tree.localized;
                        menu = new ContextMenu();
                        menu.getItems().add(localized.menu(Resources.Keys.Close, this::close));
                    }
                }
                setContextMenu(menu);
            }
        }

        /**
         * Invoked when user selected the "close" action in the contextual menu.
         */
        private void close(final ActionEvent event) {
            ((ResourceTree) getTreeView()).removeAndClose(getItem());
        }
    }

    /**
     * A simple node encapsulating a {@link Resource} in a view.
     * The list of children is fetched when first needed.
     * This node contains only the data; for visual appearance, see {@link Cell}.
     *
     * @see Cell
     */
    private static final class Item extends TreeItem<Resource> {
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
         * Creates a new node for the given resource.
         *
         * @param resource  the resource to show in the tree.
         */
        Item(final Resource resource) {
            super(resource);
            isLeaf = !(resource instanceof Aggregate);
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
         * The list is empty is the resource is not an aggregate.
         */
        @Override
        public ObservableList<TreeItem<Resource>> getChildren() {
            final ObservableList<TreeItem<Resource>> children = super.getChildren();
            if (!isChildrenKnown) {
                isChildrenKnown = true;                 // Set first for avoiding to repeat in case of failure.
                final Resource resource = getValue();
                if (resource instanceof Aggregate) {
                    final GetChildren task = new GetChildren((Aggregate) resource);
                    task.setOnSucceeded((event) -> setResources(((GetChildren) event.getSource()).getValue()));
                    task.setOnFailed((event) -> setResources(((GetChildren) event.getSource()).unloadable()));
                    BackgroundThreads.execute(task);
                    children.add(new Item(PseudoResource.LOADING));     // Temporary node with "loading" text.
                }
            }
            return children;
        }

        /**
         * Sets the resources after the background task completed.
         * This method must be invoked in the JavaFX thread.
         */
        private void setResources(final List<TreeItem<Resource>> result) {
            super.getChildren().setAll(result);
        }
    }

    /**
     * The task to execute in a background thread for fetching the children.
     *
     * @todo Draw a progress bar.
     */
    private static final class GetChildren extends Task<List<TreeItem<Resource>>> {
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
         * Invoked in a background thread for fetching the children of the resource specified
         * at construction time.
         */
        @Override
        protected List<TreeItem<Resource>> call() throws DataStoreException {
            final List<TreeItem<Resource>> items = new ArrayList<>();
            for (final Resource component : resource.components()) {
                items.add(new Item(component));
            }
            return items;
        }

        /**
         * Returns an item to set instead of the result when the operation failed.
         */
        final List<TreeItem<Resource>> unloadable() {
            return Collections.singletonList(new Item(new Unloadable(getException())));
        }
    }

    /**
     * Placeholder for a resource that we failed to load.
     */
    private static final class Unloadable extends PseudoResource {
        /**
         * The reason why we can not load the resource.
         */
        final Throwable failure;

        /**
         * Creates a new place-holder for a resource that we failed to load for the given reason.
         */
        Unloadable(final Throwable failure) {
            this.failure = failure;
        }
    }

    /**
     * The root resource when there is more than one resources to display.
     * This root node should be hidden in the {@link ResourceTree}.
     */
    private static final class Root extends PseudoResource implements Aggregate {
        /**
         * The children to expose as an unmodifiable list of components.
         */
        private final List<TreeItem<Resource>> components;

        /**
         * Creates a new aggregate which is going to be wrapped in the given item.
         * Caller should invoke {@code group.setValue(root)} after this constructor.
         *
         * @param  group     the new tree root which will contain "real" resources.
         * @param  previous  the previous root, to be added in the new group.
         */
        Root(final TreeItem<Resource> group, final Resource previous) {
            components = group.getChildren();
            add(previous);
        }

        /**
         * Returns whether this root contains the given resource as a direct child.
         * This method does not search recursively in sub-trees.
         *
         * @param  resource  the resource to search.
         * @return whether the given resource is present.
         */
        boolean contains(final Resource resource) {
            for (int i=components.size(); --i >= 0;) {
                if (components.get(i).getValue() == resource) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Adds the given resource if not already present.
         *
         * @param  resource  the resource to add.
         * @return whether the given resource has been added.
         */
        boolean add(final Resource resource) {
            for (int i=components.size(); --i >= 0;) {
                if (components.get(i).getValue() == resource) {
                    return false;
                }
            }
            return components.add(new Item(resource));
        }

        /**
         * Removes the given resource if presents.
         *
         * @param  resource  the resource to remove.
         * @return whether the resource has been removed.
         */
        boolean remove(final Resource resource) {
            return components.removeIf((i) -> i.getValue() == resource);
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
    }

    /**
     * A pseudo-resource with no identifier and no metadata.
     * This is used as a placeholder for a node while loading
     * is in progress, or for reporting a failure to load a node.
     */
    private static class PseudoResource implements Resource {
        /**
         * Place holder for a resource in process of being loaded.
         */
        static final PseudoResource LOADING = new PseudoResource();

        /**
         * Creates a new pseudo-resource.
         */
        PseudoResource() {
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
