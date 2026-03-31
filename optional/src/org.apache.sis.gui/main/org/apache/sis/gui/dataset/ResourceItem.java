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

import java.nio.file.Path;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumMap;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.aggregate.MergeStrategy;
import org.apache.sis.storage.folder.UnstructuredAggregate;
import org.apache.sis.gui.internal.DataStoreOpener;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.LogHandler;


/**
 * An item of the {@link ResourceTree} completed with additional information.
 * The {@linkplain #getChildren() list of children} is fetched in a background thread when first needed.
 * This node contains only the data. For visual appearance, see {@link ResourceCell}.
 *
 * <p>The initial {@link Resource} value of this item is usually {@code null} and should be set only once.
 * Resource shall be set by a call to {@link #setValue(Resource, String)} instead of {@code setValue(T)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see ResourceCell
 */
final class ResourceItem extends TreeItem<Resource> {
    /**
     * The path to the resource, or {@code null} if none or unknown.
     * This is used by {@link ResourceTree} merely for notifications.
     */
    Path path;

    /**
     * The text of this node, computed when first needed. Computation is done by invoking
     * {@link DataStoreOpener#findLabel(Resource, Locale, boolean)} in a background thread.
     * May contain a temporary text such as "Loading…".
     *
     * @see ResourceTree#fetchLabel(ResourceItem.Completer)
     */
    final StringProperty label = new SimpleStringProperty();

    /**
     * Whether this node is in process of loading data. There are two kinds of loading:
     * <ul>
     *   <li>The {@link Resource} itself, in which case {@link #getValue()} is null until the loading is completed.</li>
     *   <li>The resource {@link #label}, in which case {@link #getValue()} has a valid value.</li>
     * </ul>
     *
     * @see #isLoading()
     */
    private boolean isLoading;

    /**
     * If an error occurred while loading the resource, the cause. The {@link #getValue()} property may
     * be null or non-null, depending if the error occurred while loading the resource or only its label.
     *
     * @see #error()
     */
    private Throwable error;

    /**
     * Whether the resource is a leaf. A resource is a leaf if it is not an
     * instance of {@link Aggregate}, in which case it cannot have children.
     * This information is cached because requested often.
     *
     * @see #isLeaf()
     */
    private boolean isLeaf;

    /**
     * Whether the list of children has been determined or is about to be determined
     * (pending completion of a background task). We use this flag in order to fetch
     * children only when first requested, since this process is costly.
     *
     * @todo Register {@link org.apache.sis.storage.event.StoreListener} and reset
     *       this flag to {@code false} if the resource content or structure changed.
     */
    private boolean isChildrenKnown;

    /**
     * Creates an item with null value for a resource in process of being loaded.
     * The {@linkplain #label} should be "Loading…", but this is not set by this constructor.
     * Instead, it will be set by {@link ResourceCell} the first time that this item will be shown.
     */
    private ResourceItem() {
        isLeaf    = true;
        isLoading = true;
    }

    /**
     * Creates an item for a resource that we failed to load.
     * This constructor is used when all previous items are discarded.
     * It happens when we failed to load the components of an aggregate.
     *
     * <p>The {@linkplain #label} should be the error message, but this is not set by this constructor.
     * Instead, it will be set by {@link ResourceCell} the first time that this item will be shown.</p>
     */
    private ResourceItem(final Throwable failure) {
        isLeaf = true;
        error  = failure;
    }

    /**
     * Creates a new node for the given resource.
     * The {@linkplain #label} should be the resource name, but this is not set by this constructor.
     * Instead, it will be set by {@link ResourceCell} by fetching the name in a background thread.
     *
     * @param resource  the resource to show in the tree.
     */
    ResourceItem(final Resource resource) {
        super(resource);
        isLoading = true;       // Means that the label still need to be fetched.
        configure(resource);
    }

    /**
     * Updates the internal fields of this item for a new resource.
     * Also redirects logging messages emitted by the resource.
     */
    private void configure(final Resource resource) {
        isLeaf = !(resource instanceof Aggregate);
        LogHandler.installListener(resource);
    }

    /**
     * Sets the resource after the loading in a background thread completed successfully.
     *
     * @param resource  the resource to show in the tree.
     * @param text      the text to show as the resource's label.
     */
    public void setValue(final Resource resource, final String text) {
        isLoading = false;
        label.setValue(text);
        configure(resource);
        setValue(resource);
    }

    /**
     * Update {@link #label} with the resource label fetched in background thread.
     * Caller should use this task only if {@link #isLoading()} is {@code true}.
     */
    final class Completer implements Runnable {
        /** The resource for which to fetch a label. */
        private final Resource resource;

        /** The cell where the resource will be shown in the tree. */
        private final ResourceCell cell;

        /** Result of fetching the label of a resource. */
        private String result;

        /** Error that occurred while fetching the label. */
        private Throwable failure;

        /** Creates a new task for fetching the label of a resource. */
        Completer(final Resource resource, final ResourceCell cell) {
            this.resource = resource;
            this.cell = cell;
        }

        /** Invoked in a background thread for fetching the label. */
        @SuppressWarnings("UseSpecificCatch")
        final void fetch(final Locale locale) {
            try {
                result = DataStoreOpener.findLabel(resource, locale, false);
            } catch (Throwable e) {
                failure = e;
            }
            Platform.runLater(this);
        }

        /** Invoked in JavaFX thread after the label has been fetched or failed to be fetched. */
        @Override public void run() {
            isLoading = false;
            label.setValue(result);
            if (failure != null) error = failure;
            cell.completed(ResourceItem.this, failure);
        }
    }

    /**
     * If an error occurred while loading the resource, the exception which was thrown.
     */
    final Throwable error() {
        return error;
    }

    /**
     * Returns whether this node is in process of loading data.
     */
    final boolean isLoading() {
        return isLoading;
    }

    /**
     * Returns whether the resource cannot have children.
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
                children.add(new ResourceItem());
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
            final var items = new ArrayList<TreeItem<Resource>>();
            final Long id = LogHandler.loadingStart(resource);
            try {
                for (final Resource component : resource.components()) {
                    items.add(new ResourceItem(component));
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
            ResourceItem.super.getChildren().setAll(getValue());
        }

        /**
         * Invoked in JavaFX thread if children cannot be loaded.
         * This method replaces all children (which are unknown) by
         * a single node which represents a failure to load the data.
         */
        @Override
        @SuppressWarnings("unchecked")
        protected void failed() {
            ResourceItem.super.getChildren().setAll(new ResourceItem(getException()));
        }
    }




    // ┌──────────────────────────────────────────────────────────────────────────────────────────┐
    // │ Management of different Views of the resoure (for example aggregations of folder conent) │
    // └──────────────────────────────────────────────────────────────────────────────────────────┘

    /**
     * If derived resources (aggregation, etc.) are created, the derived resource for each view.
     * Otherwise {@code null}. This is used for switching view without recomputing the resource.
     * All {@link ResourceItem} derived from the same source will share the same map of views.
     */
    private EnumMap<TreeViewType, ResourceItem> views;

    /**
     * Returns the resource which is the source of this item.
     */
    final Resource getSource() {
        return (views != null ? views.get(TreeViewType.SOURCE) : this).getValue();
    }

    /**
     * Returns {@code true} if the value, or the value of one of the views, is the given resource.
     * This method should be used instead of {@code getValue() == resource} for locating the item
     * that represents a resource.
     */
    static boolean isWrapperOf(final TreeItem<Resource> item, final Resource resource) {
        if (item.getValue() == resource) {
            return true;
        }
        if (item instanceof ResourceItem r && r.views != null) {
            for (final ResourceItem view : r.views.values()) {
                if (view.getValue() == resource) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether this item is for the specified view.
     * This is used for deciding whether the corresponding menu item should be checked.
     *
     * @param  type  the view to test.
     * @return whether this item is for the specified view.
     */
    final boolean isView(final TreeViewType type) {
        return (views != null) && views.get(type) == this;
    }

    /**
     * Returns whether the specified type of view can be used with the given resource.
     *
     * @param  resource  the resource on which different types of views may apply.
     * @param  type      the desired type of view.
     * @return whether the specified type of view can be used.
     */
    final boolean isViewSelectable(final Resource resource, final TreeViewType type) {
        if (views != null && views.containsKey(type)) {
            return true;
        }
        if (getParent() != null) {      // Views can be changed only if a parent exists.
            switch (type) {
                case AGGREGATION: return (resource instanceof UnstructuredAggregate);
                // More views may be added in the future.
            }
        }
        return false;
    }

    /**
     * Replaces this resource item by the specified view.
     * The replacement is performed in the list of children of the parent.
     *
     * @param  view  the view to select as the active view.
     */
    private void selectView(final ResourceItem view) {
        final TreeItem<Resource> parent = getParent();
        final List<TreeItem<Resource>> siblings;
        if (parent != null) {
            siblings = parent.getChildren();
            final int i = siblings.indexOf(this);
            if (i >= 0) {
                siblings.set(i, view);
                return;
            }
            // Should never happen, otherwise the `parent` information would be wrong.
        } else {
            siblings = super.getChildren();
        }
        /*
         * Following fallback should never happen. If it happen anyway, add the view as a sibling
         * for avoiding the complete lost of the resource. It is possible only if a parent exists.
         * A parent may not exist if the resource was declared by `ResourceTree.setResource(…)`,
         * in which case we do not want to change the resource specified by user.
         */
        siblings.add(view);
    }

    /**
     * Enables or disables the aggregated view. This functionality is used mostly when the resource is a folder,
     * for example added by a drag-and-drop action. It usually do not apply to individual files.
     *
     * @param  cell    the cell which is requesting a view.
     * @param  type    the type of view to show.
     * @param  locale  the locale to use for fetching resource label.
     */
    final void setView(final ResourceCell cell, final TreeViewType type, final Locale locale) {
        if (views == null) {
            views = new EnumMap<>(TreeViewType.class);
            views.put(TreeViewType.SOURCE, this);
        }
        final ResourceItem existing = views.get(type);
        if (existing != null) {
            selectView(existing);
            return;
        }
        /*
         * Replaces this resource item by a newly created view.
         * The new item will initially show only "Loading…".
         * The actual label is fetched in a background thread.
         */
        final Resource resource = getSource();
        final var loading = new ResourceItem();
        loading.views = views;
        views.put(type, loading);
        selectView(loading);
        BackgroundThreads.execute(new Task<Resource>() {
            /** Value to assign to the label property. */
            private String text;

            /** Fetch in a background thread the view selected by user. */
            @Override protected Resource call() throws DataStoreException {
                Resource result = resource;
                switch (type) {
                    case AGGREGATION: {
                        if (resource instanceof UnstructuredAggregate) {
                            result = ((UnstructuredAggregate) resource).getStructuredView();
                            result = MergeStrategy.selectByTimeThenArea(null).apply(result);
                        }
                        break;
                    }
                    // More cases may be added in the future.
                }
                LogHandler.redirect(result, resource);
                text = DataStoreOpener.findLabel(resource, locale, false);
                return result;
            }

            /** Invoked in JavaFX thread after the requested view has been obtained. */
            @Override protected void succeeded() {
                loading.setValue(getValue(), text);
            }

            /** Invoked in JavaFX thread if an exception occurred while fetching the view. */
            @Override protected void failed() {
                loading.isLoading = false;
                loading.isLeaf = true;
                cell.completed(loading, getException());
            }
        });
    }
}
