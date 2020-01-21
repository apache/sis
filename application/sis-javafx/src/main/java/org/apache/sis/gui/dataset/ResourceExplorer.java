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

import java.util.Collection;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.Region;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.gui.metadata.MetadataSummary;
import org.apache.sis.gui.metadata.MetadataTree;
import org.apache.sis.internal.gui.Resources;


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
     * The tree of resources.
     */
    private final ResourceTree resources;

    /**
     * The data as a table.
     */
    private final FeatureTable features;

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
     * Creates a new panel for exploring resources.
     */
    public ResourceExplorer() {
        resources = new ResourceTree();
        metadata  = new MetadataSummary();
        features  = new FeatureTable();
        content   = new SplitPane();

        final Resources localized = localized();
        final Tab dataTab = new Tab(localized.getString(Resources.Keys.Data), features);
        dataTab.setContextMenu(new ContextMenu(createNewWindowMenu()));
        final TabPane tabs = new TabPane(
            new Tab(localized.getString(Resources.Keys.Summary),  metadata.getView()), dataTab,
            new Tab(localized.getString(Resources.Keys.Metadata), new MetadataTree(metadata)));

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        content.getItems().setAll(resources, tabs);
        resources.getSelectionModel().getSelectedItems().addListener(this::selectResource);
        SplitPane.setResizableWithParent(resources, Boolean.FALSE);
        SplitPane.setResizableWithParent(tabs, Boolean.TRUE);
        content.setDividerPosition(0, 300);
    }

    /**
     * Returns resources for current locale.
     */
    @Override
    final Resources localized() {
        return resources.localized;
    }

    /**
     * Returns the region containing the resource tree, metadata panel or any other control managed
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
        final FeatureSet data = (resource instanceof FeatureSet) ? (FeatureSet) resource : null;
        metadata.setMetadata(resource);
        features.setFeatures(data);
        setNewWindowDisabled(data == null);
    }

    /**
     * Returns the set of currently selected data, or {@code null} if none.
     */
    @Override
    final SelectedData getSelectedData() {
        final FeatureSet data = features.getFeatures();
        if (data != null) {
            final SelectedData selection = new SelectedData();
            selection.title = resources.getTitle(data, false);
            selection.features = features;
            return selection;
        }
        return null;
    }
}
