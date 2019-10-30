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

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.internal.gui.ResourceLoader;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * Tree viewer displaying a {@link Resource} hierarchy.
 * This viewer can be used for showing the content of a {@link DataStore}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ResourceTree extends TreeView<Resource> {
    /**
     * The locale for resource title.
     */
    private final Locale locale;

    /**
     * The paint to use for the text of resources that we failed to load.
     */
    private final Paint errorTextFill;

    /**
     * Creates a new tree of resources with initially no resource to show.
     * For showing a resource, invoke {@link #setResource(Resource)} after construction.
     */
    public ResourceTree() {
        errorTextFill = Color.RED;
        locale = Locale.getDefault(Locale.Category.DISPLAY);
        setCellFactory(ResourceTree::newCell);
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
     * method add the new resource below previously added resources.
     *
     * <p>This method updates the {@link #setRoot root} and {@link #setShowRoot showRoot}
     * properties of {@link TreeView}.</p>
     *
     * @param  resource  the root resource to add, or {@code null} if none.
     */
    public void addResource(final Resource resource) {
        if (resource != null) {
            final Item child = new Item(resource);
            final TreeItem<Resource> item = getRoot();
            if (item != null) {
                Resource root = item.getValue();
                if (root != null) {
                    if (!(root instanceof Root)) {
                        root = new Root(root);
                        setRoot(new Item(root));
                        setShowRoot(false);
                    }
                    ((Root) root).components.add(resource);
                    item.getChildren().add(child);
                    return;
                }
            }
            setRoot(child);
            setShowRoot(true);
        }
    }

    /**
     * Loads in a background thread the resources from the given source,
     * then {@linkplain #addResource(Resource)} add the resource to this tree.
     *
     * @param  source  the source of the resource to load. This is usually
     *                 a {@link java.io.File} or {@link java.nio.file.Path}.
     */
    public void loadResource(final Object source) {
        final ResourceLoader loader = new ResourceLoader(source);
        loader.setOnSucceeded((event) -> addResource((Resource) event.getSource().getValue()));
        BackgroundThreads.execute(loader);
    }

    /**
     * Invoked when a new cell need to be created. This method creates a specialized instance
     * which will get the cell text from a resource by a call to {@link #getTitle(Resource)}.
     *
     * @param  tree  the {@link ResourceTree} for which to create a cell.
     * @return a new cell renderer for the given tree.
     */
    private static TreeCell<Resource> newCell(final TreeView<Resource> tree) {
        return new Cell();
    }

    /**
     * Returns a label for a resource. Current implementation returns the
     * {@linkplain DataStore#getDisplayName() data store display name} if available,
     * or the title found in {@linkplain Resource#getMetadata() metadata} otherwise.
     *
     * @param  resource  the resource for which to get a label.
     * @return the resource display name of title, never null.
     * @throws DataStoreException if an error occurred while fetching metadata.
     */
    private String getTitle(final Resource resource) throws DataStoreException {
        if (resource != null) {
            if (resource instanceof DataStore) {
                String name = ((DataStore) resource).getDisplayName();
                if (name != null && !(name = name.trim()).isEmpty()) {
                    return name;
                }
            }
            final Metadata metadata = resource.getMetadata();
            if (metadata != null) {
                for (final Identification identification : CollectionsExt.nonNull(metadata.getIdentificationInfo())) {
                    final Citation citation = identification.getCitation();
                    if (citation != null) {
                        final InternationalString i18n = citation.getTitle();
                        String id;
                        if (i18n != null) {
                            id = i18n.toString(locale);
                        } else {
                            id = Citations.getIdentifier(identification.getCitation());
                        }
                        if (id != null && !(id = id.trim()).isEmpty()) {
                            return id;
                        }
                    }
                }
            }
        }
        return Vocabulary.getResources(locale).getString(Vocabulary.Keys.Unnamed);
    }

    /**
     * The visual appearance of an {@link Item} in a tree. Cells are initially empty;
     * their content will be specified by {@link TreeView} after construction.
     * The same call may be recycled many times for different {@link Item} data.
     */
    private static final class Cell extends TreeCell<Resource> {
        /**
         * If an exception occurred while loading the resource, the exception.
         *
         * @todo Provides a button for showing the details about this failure.
         */
        private Throwable failure;

        /**
         * The default text fill which was used before an error occurred.
         * We use this information for restoring the default fill after the error is cleared.
         */
        private Paint defaultTextFill;

        /**
         * Creates a new cell with initially no data.
         */
        Cell() {
        }

        /**
         * Invoked when a new resource need to be shown in the tree view.
         * This method sets the text to a title that describe the resource.
         *
         * @param item   the resource to show.
         * @param empty  whether this cell is used to fill out space.
         */
        @Override
        protected void updateItem(final Resource item, boolean empty) {
            super.updateItem(item, empty);      // Mandatory according JavaFX documentation.
            String text;
            if (empty) {
                text    = "";
                failure = null;
            } else if (item instanceof Unloadable) {
                failure = ((Unloadable) item).failure;
                text    = failure.toString();
            } else try {
                text    = ((ResourceTree) getTreeView()).getTitle(item);
                failure = null;
            } catch (ClassCastException | DataStoreException ex) {
                failure = ex;
                text    = ex.toString();
            }
            setText(text);
            if (failure != null) {
                if (defaultTextFill == null) {
                    defaultTextFill = getTextFill();
                    setTextFill(((ResourceTree) getTreeView()).errorTextFill);
                }
            } else if (defaultTextFill != null) {
                setTextFill(defaultTextFill);
                defaultTextFill = null;
            }
        }
    }

    /**
     * A simple node encapsulating a {@link Resource} in a view.
     * The list of children is fetched when first needed.
     * This node contains only the data; for visual appearance, see {@link Cell}.
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
            if (!isChildrenKnown) {
                isChildrenKnown = true;                 // Set first for avoiding to repeat in case of failure.
                final Resource resource = getValue();
                if (resource instanceof Aggregate) {
                    final GetChildren task = new GetChildren((Aggregate) resource);
                    task.setOnSucceeded((event) -> super.getChildren().setAll(((GetChildren) event.getSource()).getValue()));
                    task.setOnFailed((event) -> super.getChildren().add(((GetChildren) event.getSource()).unloadable()));
                    BackgroundThreads.execute(task);
                }
            }
            return super.getChildren();
        }
    }

    /**
     * The task to execute in a background thread for fetching the children.
     *
     * @todo Wait a short time (e.g. 0.1 second) in the JavaFX thread.
     *       If the task did not finished, draw a progress bar.
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
        final Item unloadable() {
            return new Item(new Unloadable(getException()));
        }
    }

    /**
     * Placeholder for a resource that we failed to load.
     */
    private static final class Unloadable implements Resource {
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

    /**
     * The root resource when there is more than one resources to display.
     * This root node should be hidden in the {@link ResourceTree}.
     */
    private static final class Root implements Aggregate {
        /**
         * The writable list of resources in this aggregate.
         * Shall be modified only in the JavaFX thread.
         */
        final List<Resource> components;

        /**
         * Creates a new aggregate initialized to a singleton of the given value.
         */
        Root(final Resource singleton) {
            components = new ArrayList<>();
            components.add(singleton);
        }

        /**
         * Returns a read-only view of the components.
         */
        @Override
        public Collection<Resource> components() {
            return Collections.unmodifiableList(components);
        }

        /**
         * Returns empty optional since this aggregate has no identifier.
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
