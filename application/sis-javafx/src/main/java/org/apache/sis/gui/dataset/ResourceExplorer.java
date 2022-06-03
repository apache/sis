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

import java.util.EnumMap;
import java.util.Objects;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.concurrent.Task;
import javafx.scene.layout.Region;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.metadata.MetadataSummary;
import org.apache.sis.gui.metadata.MetadataTree;
import org.apache.sis.gui.metadata.StandardMetadataTree;
import org.apache.sis.gui.coverage.ImageRequest;
import org.apache.sis.gui.coverage.CoverageExplorer;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.LogHandler;


/**
 * A panel showing a {@linkplain ResourceTree tree of resources} together with their metadata and data views.
 * This panel contains also a "new window" button for creating new windows showing the same data but potentially
 * a different locations and times. {@code ResourceExplorer} contains a list of windows created by this widget.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
public class ResourceExplorer extends Widget {
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
     * The type of view (image or table) for the coverage, or {@code null} if the coverage is not currently shown.
     * We save this information because we need to know what was the previous view when a new view is selected.
     *
     * @see #getCoverageView()
     */
    private CoverageExplorer.View coverageView;

    /**
     * The pane which is expanded for a given type of view.
     * This is used for restoring the expanded tab when user switch tab.
     */
    private final EnumMap<CoverageExplorer.View, TitledPane> expandedPane;

    /**
     * Controls for the image or tabular data. The first titled pane on top contains the
     * {@link #resources} tree and all other panes below are resource-dependent controls.
     *
     * @see #updateControls(Region)
     */
    private final Accordion controls;

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
     * @see #updateDataTab(Resource)
     */
    private final Tab viewTab, tableTab;

    /**
     * Whether one of the "view" or "table" tab is shown. They are the tabs requiring data loading.
     *
     * @see #getCoverageView()
     */
    private final BooleanBinding dataShown;

    /**
     * Whether one of the standard metadata tab (either "summary" or "metadata") is selected.
     */
    private final BooleanBinding metadataShown;

    /**
     * Creates a new panel for exploring resources.
     */
    public ResourceExplorer() {
        /*
         * Build the controls on the left side, which will initially contain only the resource explorer.
         * The various tabs will be next (on the right side).
         */
        resources  = new ResourceTree();
        resources.getSelectionModel().getSelectedItems().addListener(this::onResourceSelected);
        resources.setPrefWidth(400);
        final Vocabulary vocabulary = Vocabulary.getResources(resources.locale);
        final TitledPane resourcesPane = new TitledPane(vocabulary.getString(Vocabulary.Keys.Resources), resources);
        controls = new Accordion(resourcesPane);
        controls.setExpandedPane(resourcesPane);
        expandedPane = new EnumMap<>(CoverageExplorer.View.class);
        /*
         * Prepare content of tab panes.
         * "Native metadata" tab will show metadata in their "raw" form (specific to the format).
         * "Logging" tab will show log records specific to the selected resource
         * (as opposed to the application menu showing all loggings regardless their source).
         */
        metadata = new MetadataSummary();
        nativeMetadata = new MetadataTree(metadata);
        final LogViewer logging = new LogViewer(vocabulary);
        selectedResource = new ReadOnlyObjectWrapper<>(this, "selectedResource");
        logging.source.bind(selectedResource);
        final Tab summaryTab, metadataTab, loggingTab;
        final TabPane tabs = new TabPane(
            summaryTab        = new Tab(vocabulary.getString(Vocabulary.Keys.Summary),  metadata.getView()),
            viewTab           = new Tab(vocabulary.getString(Vocabulary.Keys.Visual)),
            tableTab          = new Tab(vocabulary.getString(Vocabulary.Keys.Data)),
            metadataTab       = new Tab(vocabulary.getString(Vocabulary.Keys.Metadata), new StandardMetadataTree(metadata)),
            nativeMetadataTab = new Tab(vocabulary.getString(Vocabulary.Keys.Format),   nativeMetadata),
            loggingTab        = new Tab(vocabulary.getString(Vocabulary.Keys.Logs),     logging.getView()));

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        defaultNativeTabLabel = nativeMetadataTab.getText();
        nativeMetadataTab.setDisable(true);
        /*
         * Build the main pane which put everything together.
         */
        content = new SplitPane(controls, tabs);
        content.setDividerPosition(0, 1./3);
        SplitPane.setResizableWithParent(controls, Boolean.FALSE);
        SplitPane.setResizableWithParent(tabs,     Boolean.TRUE);
        /*
         * Register listeners last, for making sure we do not have undesired events.
         * Those listeners trig loading of various objects (data, standard metadata,
         * native metadata) when the corresponding tab become visible.
         */
        loggingTab.disableProperty().bind(logging.isEmptyProperty());
        dataShown = viewTab.selectedProperty().or(tableTab.selectedProperty());
        dataShown.addListener((p,o,n) -> {
            if (Boolean.FALSE.equals(o) && Boolean.TRUE.equals(n)) {
                updateDataTabWithDefault(getSelectedResource());
            } else {
                updateDataTab(null);
            }
        });
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
     * Returns the locale for controls and messages.
     */
    @Override
    public final Locale getLocale() {
        return resources.locale;
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
     *
     * @see ResourceTree#onResourceLoaded
     */
    public EventHandler<ResourceEvent> getOnResourceLoaded() {
        return resources.onResourceLoaded.get();
    }

    /**
     * Specifies a function to be called after a resource has been loaded from a file or URL.
     * This is a setter for the {@link ResourceTree#onResourceLoaded} property value.
     * If this method is never invoked, then the default value is {@code null}.
     *
     * @param  handler  new function to be called after a resource has been loaded, or {@code null} if none.
     *
     * @see ResourceTree#onResourceLoaded
     */
    public void setOnResourceLoaded(final EventHandler<ResourceEvent> handler) {
        resources.onResourceLoaded.set(handler);
    }

    /**
     * Returns the function to be called when a resource is closed.
     * This is an accessor for the {@link ResourceTree#onResourceClosed} property value.
     *
     * @return current function to be called when a resource is closed, or {@code null} if none.
     *
     * @see ResourceTree#onResourceClosed
     *
     * @since 1.2
     */
    public EventHandler<ResourceEvent> getOnResourceClosed() {
        return resources.onResourceClosed.get();
    }

    /**
     * Specifies a function to be called when a resource is closed.
     * This is a setter for the {@link ResourceTree#onResourceClosed} property value.
     * If this method is never invoked, then the default value is {@code null}.
     *
     * @param  handler  new function to be called when a resource is closed, or {@code null} if none.
     *
     * @see ResourceTree#onResourceClosed
     *
     * @since 1.2
     */
    public void setOnResourceClosed(final EventHandler<ResourceEvent> handler) {
        resources.onResourceClosed.set(handler);
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
        updateDataTabWithDefault(dataShown.get() ? resource : null);
        /*
         * Update the label and disabled state of the native metadata tab. We do not have a reliable way
         * to know if metadata are present without trying to fetch them, so current implementation only
         * checks if the data store implementation overrides the `getNativeMetadata()` method.
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
                warning("onResourceSelected", resource, e);         // Should never happen.
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
     * This method is invoked when the tab become visible, or when a new resource is loaded.
     */
    private void loadNativeMetadata() {
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
     * Returns the enumeration value that describe the kind of content to show in {@link CoverageExplorer}.
     * The type depends on which tab is visible. If no coverage data tab is visible, then returns null.
     *
     * @see #coverageView
     * @see #dataShown
     */
    private CoverageExplorer.View getCoverageView() {
        if  (viewTab.isSelected()) return CoverageExplorer.View.IMAGE;
        if (tableTab.isSelected()) return CoverageExplorer.View.TABLE;
        return null;
    }

    /**
     * Assigns the given resource into the {@link #viewTab} or {@link #tableTab}, depending which one is visible.
     * Shall be invoked with a non-null resource only if a data tab is visible because data loading may be costly.
     *
     * @param  resource  the resource to set, or {@code null} if none.
     * @return {@code true} if the resource has been recognized.
     *
     * @see #dataShown
     * @see #updateDataTabWithDefault(Resource)
     */
    private boolean updateDataTab(final Resource resource) {
        Region       image  = null;
        Region       table  = null;
        FeatureSet   data   = null;
        ImageRequest grid   = null;
        TitledPane[] cpanes = null;
        final CoverageExplorer.View type = getCoverageView();
        if (resource instanceof GridCoverageResource) {
            // A null `type` value here would be a violation of method contract.
            if (coverage == null) {
                coverage = new CoverageExplorer(type);
            } else {
                coverage.setViewType(type);
            }
            final Region view = coverage.getDataView(type);
            switch (type) {
                case IMAGE: image = view; break;
                case TABLE: table = view; break;
            }
            grid = new ImageRequest((GridCoverageResource) resource, null, null);
            cpanes = coverage.getControls(type);
        } else if (resource instanceof FeatureSet) {
            data = (FeatureSet) resource;
            if (features == null) {
                features = new FeatureTable();
            }
            table = features;
        }
        /*
         * At least one of `grid` or `data` will be null. Invoking the following
         * setter methods with a null argument will release memory.
         */
        if (coverage != null) coverage.setCoverage(grid);
        if (features != null) features.setFeatures(data);
        if (image    != null) viewTab .setContent(image);
        if (table    != null) tableTab.setContent(table);
        final boolean isEmpty = (image == null & table == null);
        /*
         * Add or remove controls for the selected view.
         * Information about the expanded pane needs to be saved before to remove controls,
         * and restored (for a potentially different view) after new controls have been added.
         */
        TitledPane expanded = controls.getExpandedPane();
        if (expanded != null && coverageView != null) {
            expandedPane.put(coverageView, expanded);
        }
        final ObservableList<TitledPane> items = controls.getPanes();
        final int size = items.size();
        items.remove(1, size);
        if (cpanes != null) {
            items.addAll(cpanes);
            if (!items.contains(expanded)) {
                expanded = expandedPane.get(type);
                if (expanded != null) {
                    controls.setExpandedPane(expanded);
                }
            }
        }
        coverageView = type;
        return !isEmpty | (resource == null);
    }

    /**
     * If the given resource is not one of the resource that {@link #updateDataTab(Resource)} can handle,
     * searches in a background thread for a default resource to show. The purpose of this method is to
     * make navigation easier by allowing users to click on the root node of a resource,
     * without requerying them to expand the tree node before to select a resource.
     *
     * @param  resource  the selected resource.
     *
     * @see #updateDataTab(Resource)
     */
    private void updateDataTabWithDefault(final Resource resource) {
        if (updateDataTab(resource)) {
            return;
        }
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
                        updateDataTab(getValue());
                    }
                }

                /** Invoked in JavaFX thread if children can not be loaded. */
                @Override protected void failed() {
                    warning("updateDataTabWithDefault", resource, getException());
                }
            });
        }
    }

    /**
     * Adds a warning to the logger associated to the resource.
     *
     * @param caller    the method to declare as the source of the warning.
     * @param resource  the resource for which an exception occurred.
     * @param error     the exception to log.
     */
    private static void warning(final String caller, final Resource resource, final Throwable error) {
        final LogHandler.Destination records = LogHandler.getRecords(resource);
        if (records != null) {
            final LogRecord record = new LogRecord(Level.WARNING, error.getLocalizedMessage());
            record.setSourceClassName(ResourceExplorer.class.getName());
            record.setSourceMethodName(caller);
            record.setThrown(error);
            records.add(record);
        }
    }
}
