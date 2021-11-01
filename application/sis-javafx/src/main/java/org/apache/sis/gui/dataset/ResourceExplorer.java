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

import java.util.Objects;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.gui.metadata.MetadataSummary;
import org.apache.sis.gui.metadata.MetadataTree;
import org.apache.sis.gui.metadata.StandardMetadataTree;
import org.apache.sis.gui.coverage.ImageRequest;
import org.apache.sis.gui.coverage.CoverageExplorer;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.LogHandler;


/**
 * A panel showing a {@linkplain ResourceTree tree of resources} together with their metadata and data views.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public class ResourceExplorer extends WindowManager {
    /**
     * The tree of resources.
     */
    private final ResourceTree resources;

    /**
     * The currently selected resource.
     *
     * @see #selectedResourceProperty()
     */
    private final ReadOnlyObjectWrapper<Resource> selectedResource;

    /**
     * The widget showing metadata about a selected resource.
     * Its content will be updated only when the tab is visible.
     */
    private final MetadataSummary metadata;

    /**
     * The widget showing native metadata about a selected resource.
     * Its content will be updated only when the tab is visible.
     */
    private final MetadataTree nativeMetadata;

    /**
     * The tab containing {@link #nativeMetadata}.
     * The table title will change depending on the selected resource.
     */
    private final Tab nativeMetadataTab;

    /**
     * Default label for {@link #nativeMetadataTab} when no resource is selected.
     */
    private final String defaultNativeTabLabel;

    /**
     * The gridded data as an image or as a table, created when first needed.
     */
    private CoverageExplorer coverage;

    /**
     * The vector data as a table, created when first needed.
     */
    private FeatureTable features;

    /**
     * Controls for the image or tabular data. This is a vertical split pane.
     * The upper part contains the {@link #resources} tree and the lower part
     * contains the resource-dependent controls.
     */
    private final SplitPane controls;

    /**
     * The control that put everything together.
     * The type of control may change in any future SIS version.
     *
     * @see #getView()
     */
    private final SplitPane content;

    /**
     * The tab where to show {@link #features} or {@link #coverage}, depending on the kind of resource.
     * The {@code viewTab} and {@code tableTab} are collectively identified in this class as "data tab".
     * The {@link #features} and {@link #coverage} data will be set only if a data tab is visible,
     * because the data loading may be costly.
     *
     * @see #isDataTabSet
     * @see #isDataTabSelected()
     * @see #updateDataTab(Resource, boolean)
     */
    private final Tab viewTab, tableTab;

    /**
     * Whether the setting of new values in {@link #viewTab} or {@link #tableTab} has been done.
     * The new values are set only if a data tab is visible, and otherwise are delayed until one
     * of data tab become visible.
     *
     * @see #updateDataTab(Resource, boolean)
     */
    private boolean isDataTabSet;

    /**
     * Whether one of the standard metadata tab (either "summary" or "metadata") is selected.
     */
    private final BooleanBinding metadataShown;

    /**
     * Last divider position as a fraction between 0 and 1, or {@code NaN} if undefined.
     * This is used for keeping the position constant when adding and removing controls.
     */
    private double dividerPosition;

    /**
     * Creates a new panel for exploring resources.
     */
    public ResourceExplorer() {
        /*
         * Build the resource explorer. Must be first because `localized()` depends on it.
         */
        resources = new ResourceTree();
        resources.getSelectionModel().getSelectedItems().addListener(this::onResourceSelected);
        resources.setPrefWidth(400);
        selectedResource = new ReadOnlyObjectWrapper<>(this, "selectedResource");
        final Vocabulary vocabulary = Vocabulary.getResources(resources.locale);
        /*
         * "Summary" tab showing a summary of resource metadata.
         */
        metadata = new MetadataSummary();
        final Tab summaryTab = new Tab(vocabulary.getString(Vocabulary.Keys.Summary),  metadata.getView());
        /*
         * "Visual" tab showing the raster data as an image.
         *
         * TODO: add contextual menu for creating a window showing directly the visual.
         */
        viewTab = new Tab(vocabulary.getString(Vocabulary.Keys.Visual));
        /*
         * "Data" tab showing raster data as a table.
         */
        tableTab = new Tab(vocabulary.getString(Vocabulary.Keys.Data));
        tableTab.setContextMenu(new ContextMenu(SelectedData.setTabularView(createNewWindowMenu())));
        /*
         * "Metadata" tab showing ISO 19115 metadata as a tree.
         */
        final Tab metadataTab = new Tab(vocabulary.getString(Vocabulary.Keys.Metadata), new StandardMetadataTree(metadata));
        /*
         * "Native metadata" tab showing metadata in their "raw" form (specific to the format).
         */
        nativeMetadata = new MetadataTree(metadata);
        defaultNativeTabLabel = vocabulary.getString(Vocabulary.Keys.Format);
        nativeMetadataTab = new Tab(defaultNativeTabLabel, nativeMetadata);
        nativeMetadataTab.setDisable(true);
        /*
         * "Logging" tab showing log records specific to the selected resource
         * (as opposed to the application menu showing all loggings regardless their source).
         */
        final LogViewer logging = new LogViewer(vocabulary);
        logging.source.bind(selectedResource);
        final Tab loggingTab = new Tab(vocabulary.getString(Vocabulary.Keys.Logs), logging.getView());
        loggingTab.disableProperty().bind(logging.isEmptyProperty());
        /*
         * Build the main pane which put everything together.
         */
        final TabPane tabs = new TabPane(summaryTab, viewTab, tableTab, metadataTab, nativeMetadataTab, loggingTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        controls = new SplitPane(resources);
        controls.setOrientation(Orientation.VERTICAL);
        content = new SplitPane(controls, tabs);
        content.setDividerPosition(0, 0.2);
        dividerPosition = Double.NaN;
        SplitPane.setResizableWithParent(resources, Boolean.FALSE);
        SplitPane.setResizableWithParent(tabs, Boolean.TRUE);
        /*
         * Register listeners last, for making sure we don't have undesired event.
         * Those listeners trig loading of various objects (data, standard metadata,
         * native metadata) when the corresponding tab become visible.
         */
        viewTab .selectedProperty().addListener((p,o,n) -> dataTabShown(n, true));
        tableTab.selectedProperty().addListener((p,o,n) -> dataTabShown(n, false));
        metadataShown = summaryTab.selectedProperty().or(metadataTab.selectedProperty());
        metadataShown.addListener((p,o,n) -> {
            if (Boolean.FALSE.equals(o) && Boolean.TRUE.equals(n)) {
                metadata.setMetadata(getSelectedResource());
            }
        });
        nativeMetadataTab.selectedProperty().addListener((p,o,n) -> {
            if (Boolean.FALSE.equals(o) && Boolean.TRUE.equals(n)) {
                loadNativeMetadata();
            }
        });
    }

    /**
     * Returns resources for current locale.
     */
    @Override
    final Resources localized() {
        return resources.localized();
    }

    /**
     * Returns the region containing the resource tree, metadata panel and any other control managed
     * by this {@code ResourceExplorer}. The subclass is implementation dependent and may change in
     * any future version.
     *
     * @return the region to show.
     */
    @Override
    public final Region getView() {
        return content;
    }

    /**
     * Returns the function to be called after a resource has been loaded from a file or URL.
     * This is an accessor for the {@link ResourceTree#onResourceLoaded} property value.
     *
     * @return current function to be called after a resource has been loaded, or {@code null} if none.
     */
    public EventHandler<LoadEvent> getOnResourceLoaded() {
        return resources.onResourceLoaded.get();
    }

    /**
     * Specifies a function to be called after a resource has been loaded from a file or URL.
     * This is a setter for the {@link ResourceTree#onResourceLoaded} property value.
     * If this method is never invoked, then the default value is {@code null}.
     *
     * @param  handler  new function to be called after a resource has been loaded, or {@code null} if none.
     */
    public void setOnResourceLoaded(final EventHandler<LoadEvent> handler) {
        resources.onResourceLoaded.set(handler);
    }

    /**
     * Loads all given sources in background threads and add them to the resource tree.
     * The given collection typically contains files to load,
     * but may also contain {@link Resource} instances to add directly.
     * This method forwards the files to {@link ResourceTree#loadResource(Object)},
     * which will allocate a background thread for each resource to load.
     *
     * @param  files  the source of the resource to load. They are usually
     *                {@link java.io.File} or {@link java.nio.file.Path} instances.
     *
     * @see ResourceTree#loadResource(Object)
     */
    public void loadResources(final Collection<?> files) {
        for (final Object file : files) {
            resources.loadResource(file);
        }
    }

    /**
     * Removes the given resource from the tree and eventually closes it.
     * If the given resource is not in this tree explorer or can not be removed,
     * then this method does nothing.
     *
     * @param  resource  the resource to remove, or {@code null}.
     *
     * @see ResourceTree#removeAndClose(Resource)
     */
    public void removeAndClose(final Resource resource) {
        resources.removeAndClose(resource);
    }

    /**
     * Invoked in JavaFX thread when a new item is selected in the resource tree.
     * Normally, only one resource is selected since we use a single selection model.
     * We nevertheless loop over the items as a paranoiac check and take the first non-null resource.
     *
     * @param  change  a change event with the new resource to show.
     */
    private void onResourceSelected(final ListChangeListener.Change<? extends TreeItem<Resource>> change) {
        Resource resource = null;
        for (final TreeItem<Resource> item : change.getList()) {
            if (item != null) {
                resource = item.getValue();
                if (resource != null) break;
            }
        }
        /*
         * Fetch metadata immediately if one of the two ISO 19115 metadata tabs is selected.
         * Otherwise metadata will be fetched when one of those tabs will become selected
         * (listener registered in the constructor). A similar policy is applied for data.
         */
        selectedResource.set(resource);
        metadata.setMetadata(metadataShown.get() ? resource : null);
        isDataTabSet = viewTab.isSelected() || tableTab.isSelected();
        updateDataTab(isDataTabSet ? resource : null, true);
        if (!isDataTabSet) {
            setNewWindowDisabled(!(resource instanceof GridCoverageResource || resource instanceof FeatureSet));
        }
        /*
         * Update the label is disabled state of the native metadata tab. We do not have a reliable way
         * to know if metadata are present without trying to fetch them, so current implementation only
         * checks if the data store implementation override the `getNativeMetadata()` method.
         */
        String  label    = null;
        boolean disabled = true;
        if (resource instanceof DataStore) {
            final DataStore store = (DataStore) resource;
            final DataStoreProvider provider = store.getProvider();
            if (provider != null) {
                label = provider.getShortName();
            }
            try {
                disabled = resource.getClass().getMethod("getNativeMetadata").getDeclaringClass() == DataStore.class;
            } catch (NoSuchMethodException e) {
                // Should never happen.
            }
        }
        nativeMetadataTab.setText(Objects.toString(label, defaultNativeTabLabel));
        nativeMetadataTab.setDisable(disabled);
        nativeMetadata.setPlaceholder(null);
        nativeMetadata.setContent(null);
        if (nativeMetadataTab.isSelected()) {
            loadNativeMetadata();
        }
    }

    /**
     * Loads native metadata in a background thread and shows them in the "native metadata" tab.
     */
    private final void loadNativeMetadata() {
        final Resource resource = getSelectedResource();
        if (resource instanceof DataStore) {
            final DataStore store = (DataStore) resource;
            BackgroundThreads.execute(new Task<TreeTable>() {
                /** Invoked in a background thread for fetching metadata. */
                @Override protected TreeTable call() throws DataStoreException {
                    return store.getNativeMetadata().orElse(null);
                }

                /** Shows the result in JavaFX thread. */
                @Override protected void succeeded() {
                    if (resource == getSelectedResource()) {
                        nativeMetadata.setContent(getValue());
                    }
                }

                /** Invoked in JavaFX thread if metadata loading failed. */
                @Override protected void failed() {
                    nativeMetadata.setPlaceholder(new ExceptionReporter(getException()).getView());
                }
            });
        }
    }

    /**
     * Assigns the given resource into the {@link #viewTab} and {@link #tableTab}. Should be invoked only
     * if a data tab is visible because data loading may be costly. It is caller responsibility to invoke
     * {@link #setNewWindowDisabled(boolean)} after this method.
     *
     * <p>The {@link #isDataTabSet} flag should be set before to invoke this method. If {@code true}, then
     * the given resource is the final content and window menus will be updated accordingly by this method.
     * If {@code false}, then the given resource is temporarily null and window menus should be updated by
     * the caller instead of this method.</p>
     *
     * @param  resource  the resource to set, or {@code null} if none.
     * @param  fallback  whether to allow the search for a default component to show
     *                   if the given resource is an aggregate.
     */
    private void updateDataTab(final Resource resource, boolean fallback) {
        Region       image = null;
        Region       table = null;
        FeatureSet   data  = null;
        ImageRequest grid  = null;
        CoverageExplorer.View type = null;
        if (resource instanceof GridCoverageResource) {
            if (coverage == null) {
                coverage = new CoverageExplorer();
            }
            grid  = new ImageRequest((GridCoverageResource) resource, null, null);
            image = coverage.getDataView(CoverageExplorer.View.IMAGE);
            table = coverage.getDataView(CoverageExplorer.View.TABLE);
            type  = viewTab.isSelected() ? CoverageExplorer.View.IMAGE : CoverageExplorer.View.TABLE;
            fallback = false;
        } else if (resource instanceof FeatureSet) {
            data = (FeatureSet) resource;
            if (features == null) {
                features = new FeatureTable();
            }
            table = features;
            fallback = false;
        }
        /*
         * At least one of `grid` or `data` will be null. Invoking the following
         * setter methods with a null argument will release memory.
         */
        if (coverage != null) coverage.setCoverage(grid);
        if (features != null) features.setFeatures(data);
        if (image    != null) viewTab .setContent(image);
        if (table    != null) tableTab.setContent(table);
        if (isDataTabSet) {
            setNewWindowDisabled(image == null && table == null);
            updateControls(type);
        }
        if (fallback) {
            defaultIfNotViewable(resource);
        }
    }

    /**
     * Invoked when a data tab become selected or unselected.
     * This method sets the current resource in the {@link #viewTab}
     * or {@link #tableTab} if it has not been already set.
     *
     * @param  selected  whether the tab became the selected one.
     * @param  visual    {@code true} for visual, or {@code false} for tabular data.
     */
    private void dataTabShown(final Boolean selected, final boolean visual) {
        CoverageExplorer.View type = null;
        if (selected) {
            if (!isDataTabSet) {
                isDataTabSet = true;                    // Must be set before to invoke `updateDataTab(…)`.
                updateDataTab(getSelectedResource(), true);
            }
            if (coverage != null) {                     // May still be null if the selected resource is not a coverage.
                type = visual ? CoverageExplorer.View.IMAGE : CoverageExplorer.View.TABLE;
            }
        }
        updateControls(type);
    }

    /**
     * Adds or removes controls for the given view.
     *
     * @param  type  the view for which to provide controls, or {@code null} if none.
     */
    private void updateControls(final CoverageExplorer.View type) {
        final Region controlPanel = (type != null) ? coverage.getControls(type) : null;
        final ObservableList<Node> items = controls.getItems();
        if (items.size() >= 2) {
            if (controlPanel != null) {
                items.set(1, controlPanel);
            } else {
                dividerPosition = controls.getDividerPositions()[0];
                items.remove(1);
            }
        } else if (controlPanel != null) {
            items.add(controlPanel);
            if (dividerPosition >= 0) {
                controls.setDividerPosition(0, dividerPosition);
            }
        }
    }

    /**
     * Returns the set of currently selected data, or {@code null} if none.
     */
    @Override
    final SelectedData getSelectedData() {
        final Resource resource = getSelectedResource();
        if (resource == null) {
            return null;
        }
        ImageRequest grid  = null;
        FeatureTable table = null;
        if (resource instanceof GridCoverageResource) {
            /*
             * Want the full coverage in all bands (sample dimensions).
             */
            grid = new ImageRequest((GridCoverageResource) resource, null, null);
        } else if (resource instanceof FeatureSet) {
            /*
             * We will not set features in an initially empty `FeatureTable` (to be newly created),
             * but instead share the `FeatureLoader` created by the feature table of this explorer.
             * We do that even if the feature table is not currently visible. This will not cause
             * useless data loading since they share the same `FeatureLoader`.
             */
            if (features == null || !isDataTabSet) {
                isDataTabSet = true;                    // Must be set before to invoke `updateDataTab(…)`.
                updateDataTab(resource, true);          // For forcing creation of FeatureTable.
            }
            table = features;
        } else {
            return null;
        }
        String text;
        try {
            text = ResourceTree.findLabel(resource, resources.locale);
        } catch (DataStoreException | RuntimeException e) {
            text = Vocabulary.getResources(resources.locale).getString(Vocabulary.Keys.Unnamed);
        }
        return new SelectedData(text, table, grid, localized());
    }

    /**
     * Returns the currently selected resource.
     *
     * @return the currently selected resource, or {@code null} if none.
     */
    public final Resource getSelectedResource() {
        return selectedResource.get();
    }

    /**
     * Returns the property for currently selected resource.
     *
     * @return property for currently selected resource.
     */
    public final ReadOnlyProperty<Resource> selectedResourceProperty() {
        return selectedResource.getReadOnlyProperty();
    }

    /**
     * If the given resource is not one of the resource that {@link #updateDataTab(Resource, boolean)}
     * can handle, searches in a background thread for a default resource to show. The purpose of this
     * method is to make navigation easier by allowing users to click on the root node of a resource,
     * without requerying them to expand the tree node before to select a resource.
     *
     * @param  resource  the selected resource.
     */
    private void defaultIfNotViewable(final Resource resource) {
        if (resource instanceof Aggregate && !(resource instanceof DataSet)) {
            BackgroundThreads.execute(new Task<Resource>() {
                /** Invoked in background thread for fetching the first resource. */
                @Override protected Resource call() throws DataStoreException {
                    final Long id = LogHandler.loadingStart(resource);
                    try {
                        for (final Resource component : ((Aggregate) resource).components()) {
                            if (component instanceof DataSet) {
                                return component;
                            }
                        }
                    } finally {
                        LogHandler.loadingStop(id);
                    }
                    return null;
                }

                /** Invoked in JavaFX thread for showing the resource. */
                @Override protected void succeeded() {
                    if (getSelectedResource() == resource) {
                        updateDataTab(getValue(), false);
                    }
                }

                /** Invoked in JavaFX thread if children can not be loaded. */
                @Override protected void failed() {
                    final ObservableList<LogRecord> records = LogHandler.getRecords(resource);
                    if (records != null) {
                        final Throwable e = getException();
                        final LogRecord record = new LogRecord(Level.WARNING, e.getLocalizedMessage());
                        record.setSourceClassName(ResourceExplorer.class.getName());
                        record.setSourceMethodName("defaultIfNotViewable");
                        record.setThrown(e);
                        records.add(record);
                    }
                }
            });
        }
    }
}
