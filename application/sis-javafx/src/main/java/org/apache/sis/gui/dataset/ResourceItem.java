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
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.gui.DataStoreOpener;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.LogHandler;


/**
 * An item of the {@link Resource} tree completed with additional information.
 * The list of children is fetched in a background thread when first needed.
 * This node contains only the data; for visual appearance, see {@link Cell}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see Cell
 *
 * @since 1.3
 * @module
 */
final class ResourceItem extends TreeItem<Resource> {
    /**
     * The path to the resource, or {@code null} if none or unknown. This is used for notifications only;
     * this information does not play an important role for {@link ResourceTree} itself.
     */
    Path path;

    /**
     * The text of this node, computed and cached when first needed. Computation is done by invoking
     * {@link DataStoreOpener#findLabel(Resource, Locale, boolean)} in a background thread.
     *
     * @see ResourceTree#fetchLabel(ResourceItem.Completer)
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
     * This item will be replaced (not updated) by a fresh {@code ResourceItem} instance
     * when the resource will become available.
     */
    ResourceItem() {
        isLeaf    = true;
        isLoading = true;
    }

    /**
     * Creates an item for a resource that we failed to load.
     */
    ResourceItem(final Throwable exception) {
        isLeaf = true;
        error  = exception;
    }

    /**
     * Creates a new node for the given resource.
     *
     * @param resource  the resource to show in the tree.
     */
    ResourceItem(final Resource resource) {
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
            GUIUtilities.forceCellUpdate(ResourceItem.this);
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
            final List<TreeItem<Resource>> items = new ArrayList<>();
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
         * Invoked in JavaFX thread if children can not be loaded.
         */
        @Override
        @SuppressWarnings("unchecked")
        protected void failed() {
            ResourceItem.super.getChildren().setAll(new ResourceItem(getException()));
        }
    }
}
