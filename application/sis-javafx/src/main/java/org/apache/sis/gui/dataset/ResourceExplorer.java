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
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.Region;
import javafx.scene.control.Control;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.gui.metadata.MetadataSummary;
import org.apache.sis.gui.metadata.MetadataTree;
import org.apache.sis.gui.metadata.StandardMetadataTree;
import org.apache.sis.gui.coverage.GridView;
import org.apache.sis.gui.coverage.ImageRequest;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A panel showing a {@linkplain ResourceTree tree of resources} together with their metadata.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ResourceExplorer extends WindowManager {
    /**
     * Approximate overview width and height (averaged) in number of pixels.
     */
    private static final int OVERVIEW_SIZE = 10000;

    /**
     * The tree of resources.
     */
    private final ResourceTree resources;

    /**
     * The data as a table, created when first needed.
     */
    private FeatureTable features;

    /**
     * The data as a grid coverage, created when first needed.
     */
    private GridView coverage;

    /**
     * The widget showing metadata about a selected resource.
     */
    private final MetadataSummary metadata;

    /**
     * The control that put everything together.
     * The type of control may change in any future SIS version.
     *
     * @see #getView()
     */
    private final SplitPane content;

    /**
     * The tab where to show {@link #features} or {@link #coverage}, depending on the kind of resource.
     * The data will be set only if this tab is visible, because their loading may be costly.
     */
    private final Tab dataTab;

    /**
     * The currently selected resource.
     */
    public final ReadOnlyProperty<Resource> selectedResourceProperty;

    /**
     * Whether the setting of new values in {@link #dataTab} has been done.
     * The new values are set only if the tab is visible, and otherwise are
     * delayed until the tab become visible.
     */
    private boolean isDataTabSet;

    /**
     * Creates a new panel for exploring resources.
     */
    public ResourceExplorer() {
        resources = new ResourceTree();
        metadata  = new MetadataSummary();
        content   = new SplitPane();

        final Resources localized = localized();
        dataTab = new Tab(localized.getString(Resources.Keys.Data));
        dataTab.setContextMenu(new ContextMenu(createNewWindowMenu()));

        final String nativeTabText = Vocabulary.getResources(localized.getLocale()).getString(Vocabulary.Keys.Format);
        final MetadataTree nativeMetadata = new MetadataTree(metadata);
        final Tab nativeTab = new Tab(nativeTabText, nativeMetadata);
        nativeTab.setDisable(true);
        nativeMetadata.contentProperty.addListener((p,o,n) -> {
            nativeTab.setDisable(n == null);
            Object label = (n != null) ? n.getRoot().getValue(TableColumn.NAME) : null;
            nativeTab.setText(Objects.toString(label, nativeTabText));
        });

        final TabPane tabs = new TabPane(
            new Tab(localized.getString(Resources.Keys.Summary),  metadata.getView()), dataTab,
            new Tab(localized.getString(Resources.Keys.Metadata), new StandardMetadataTree(metadata)),
            nativeTab);

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        content.getItems().setAll(resources, tabs);
        resources.getSelectionModel().getSelectedItems().addListener(this::selectResource);
        SplitPane.setResizableWithParent(resources, Boolean.FALSE);
        SplitPane.setResizableWithParent(tabs, Boolean.TRUE);
        resources.setPrefWidth(400);

        selectedResourceProperty = new SimpleObjectProperty<>(this, "selectedResource");
        dataTab.selectedProperty().addListener(this::dataTabShown);
    }

    /**
     * Returns resources for current locale.
     */
    @Override
    final Resources localized() {
        return resources.localized;
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
     * Invoked in JavaFX thread when a new item is selected in the resource tree.
     * Normally, only one resource is selected since we use a single selection model.
     * We nevertheless loop over the items as a paranoiac check and take the first non-null resource.
     *
     * @param  change  a change event with the new resource to show.
     */
    private void selectResource(final ListChangeListener.Change<? extends TreeItem<Resource>> change) {
        Resource resource = null;
        for (final TreeItem<Resource> item : change.getList()) {
            if (item != null) {
                resource = item.getValue();
                if (resource != null) break;
            }
        }
        ((SimpleObjectProperty<Resource>) selectedResourceProperty).set(resource);
        metadata.setMetadata(resource);
        isDataTabSet = dataTab.isSelected();
        updateDataTab(isDataTabSet ? resource : null);
        if (!isDataTabSet) {
            setNewWindowDisabled(!(resource instanceof GridCoverageResource || resource instanceof FeatureSet));
        }
    }

    /**
     * Sets the given resource to the {@link #dataTab}. Should be invoked only if the tab is visible, since data
     * loading may be costly. It is caller responsibility to invoke {@link #setNewWindowDisabled(boolean)} after
     * this method.
     *
     * <p>The {@link #isDataTabSet} flag should be set before to invoke this method. If {@code true}, then
     * the given resource is the final content and window menus will be updated accordingly by this method.
     * If {@code false}, then the given resource is temporarily null and window menus should be updated by
     * the caller instead than this method.</p>
     *
     * @param  resource  the resource to set, or {@code null} if none.
     */
    private void updateDataTab(final Resource resource) {
        Control      view  = null;
        FeatureSet   table = null;
        ImageRequest grid  = null;
        if (resource instanceof GridCoverageResource) {
            grid = new ImageRequest((GridCoverageResource) resource, null, 0);
            grid.setOverviewSize(OVERVIEW_SIZE);
            if (coverage == null) {
                coverage = new GridView();
            }
            view = coverage;
        } else if (resource instanceof FeatureSet) {
            table = (FeatureSet) resource;
            if (features == null) {
                features = new FeatureTable();
            }
            view = features;
        }
        /*
         * At least one of `grid` or `table` will be null. Invoking the following
         * setter methods with a null argument will release memory.
         */
        if (coverage != null) coverage.setImage(grid);
        if (features != null) features.setFeatures(table);
        if (view     != null) dataTab .setContent(view);
        if (isDataTabSet) {
            setNewWindowDisabled(view == null);
        }
    }

    /**
     * Invoked when the data tab become selected or unselected.
     * This method sets the current resource in the {@link #dataTab} if it has not been already set.
     *
     * @param  property  ignored.
     * @param  previous  ignored.
     * @param  selected  whether the tab became the selected one.
     */
    private void dataTabShown(final ObservableValue<? extends Boolean> property,
                              final Boolean previous, final Boolean selected)
    {
        if (selected && !isDataTabSet) {
            isDataTabSet = true;                // Must be set before to invoke `updateDataTab(…)`.
            updateDataTab(selectedResourceProperty.getValue());
        }
    }

    /**
     * Returns the set of currently selected data, or {@code null} if none.
     */
    @Override
    final SelectedData getSelectedData() {
        final Resource resource = selectedResourceProperty.getValue();
        if (resource == null) {
            return null;
        }
        ImageRequest grid  = null;
        FeatureTable table = null;
        if (resource instanceof GridCoverageResource) {
            /*
             * Want the full coverage in all bands (sample dimensions). This is different than
             * the ImageRequest created by `updateDataTab(…)` which requested only an overview
             * (i.e. potentially with subsamplings) and only the first band.
             *
             * TODO: check if we can still share the coverage in some situations.
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
                updateDataTab(resource);                // For forcing creation of FeatureTable.
            }
            table = features;
        } else {
            return null;
        }
        return new SelectedData(resources.getTitle(resource, false), table, grid, resources.localized);
    }
}
