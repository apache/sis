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

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.layout.Region;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
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
public class ResourceExplorer {
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
     * The contextual menu items for showing data in a new window.
     * This is disabled if there is no data to show.
     */
    private final List<MenuItem> windowMenus;

    /**
     * The menu items for navigating to different windows. {@code ResourceExplorer} will automatically
     * add or remove elements in this list when new windows are created or closed.
     *
     * @see #setWindowsItems(ObservableList)
     */
    private ObservableList<MenuItem> windowsMenuItems;

    /**
     * A property telling whether at least one data window created by this {@code ResourceExplorer} is
     * still visible.
     *
     * @see #createNewWindowMenu()
     * @see #setWindowsItems(ObservableList)
     */
    public final ReadOnlyBooleanProperty hasWindowsProperty;

    /**
     * The {@link ResourceExplorer#hasWindowsProperty} property implementation.
     */
    private final class WindowsProperty extends ReadOnlyBooleanPropertyBase {
        /** The property value. */
        private boolean hasWindows;

        /** Sets this property to the given value. */
        final void set(final boolean value) {
            hasWindows = value;
            fireValueChangedEvent();
        }

        /** Returns the current property value. */
        @Override public boolean get()    {return hasWindows;}
        @Override public Object getBean() {return ResourceExplorer.this;}
        @Override public String getName() {return "hasWindows";}
    }

    /**
     * Creates a new panel for exploring resources.
     */
    public ResourceExplorer() {
        resources   = new ResourceTree();
        metadata    = new MetadataSummary();
        features    = new FeatureTable();
        content     = new SplitPane();
        windowMenus = new ArrayList<>(2);
        hasWindowsProperty = new WindowsProperty();

        final Resources localized = resources.localized;
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
     * Returns the region containing the resource tree, metadata panel or any other control managed
     * by this {@code ResourceExplorer}. The subclass is implementation dependent and may change in
     * any future version.
     *
     * @return the region to show.
     */
    public final Region getView() {
        return content;
    }

    /**
     * Creates a menu item for creating new windows for the currently selected resource.
     * The new menu item is initially disabled. Its will become enabled automatically when
     * a resource is selected.
     *
     * <p>Note: current implementation keeps a strong reference to created menu.
     * Use this method only for menus that are expected to exist for application lifetime.</p>
     *
     * @return a "new window" menu item.
     *
     * @see #hasWindowsProperty
     */
    public final MenuItem createNewWindowMenu() {
        final MenuItem menu = new MenuItem(resources.localized.getString(Resources.Keys.NewWindow));
        menu.setOnAction(this::newDataWindow);
        menu.setDisable(true);
        windowMenus.add(menu);
        return menu;
    }

    /**
     * Sets the list where to add or remove the name of data windows. New data windows are created when
     * user selects a menu item given by {@link #createNewWindowMenu()}. {@code ResourceExplorer} will
     * automatically add or remove elements in the given list. The position of the new menu item will
     * be just before the last {@link SeparatorMenuItem} instance. If no {@code SeparatorMenuItem} is
     * found, then one will be inserted at the beginning of the given list when needed.
     *
     * @param  items  the list where to add and remove the name of windows.
     *
     * @see #hasWindowsProperty
     * @see #createNewWindowMenu()
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setWindowsItems(final ObservableList<MenuItem> items) {
        windowsMenuItems = items;
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
        for (final MenuItem m : windowMenus) {
            m.setDisable(data == null);
        }
    }

    /**
     * Invoked when user asked to show the data in a new window. This method may be invoked from various sources:
     * contextual menu on the tab, contextual menu in the explorer tree, or from the "new window" menu item.
     *
     * @param  event  ignored (can be {@code null}).
     */
    private void newDataWindow(final ActionEvent event) {
        final FeatureSet data = features.getFeatures();
        if (data != null) {
            final String title = resources.getTitle(data, false);
            final DataWindow window = new DataWindow((Stage) content.getScene().getWindow(), features);
            window.setTitle(title + " — Apache SIS");
            window.show();
            if (windowsMenuItems != null) {
                /*
                 * Search for insertion point just before the menu separator.
                 * If no menu separator is found, add one.
                 */
                int insertAt = windowsMenuItems.size();
                do if (--insertAt < 0) {
                    windowsMenuItems.add(insertAt = 0, new SeparatorMenuItem());
                    ((WindowsProperty) hasWindowsProperty).set(true);
                    break;
                } while (!(windowsMenuItems.get(insertAt) instanceof SeparatorMenuItem));
                final MenuItem menu = new MenuItem(title);
                menu.setOnAction((e) -> window.toFront());
                windowsMenuItems.add(insertAt, menu);
                window.setOnHidden((e) -> removeDataWindow(menu));
            }
        }
    }

    /**
     * Invoked when a window has been hidden. This method removes the window title from the "Windows" menu.
     * The hidden window will be garbage collected at some later time.
     */
    private void removeDataWindow(final MenuItem menu) {
        final ObservableList<MenuItem> items = windowsMenuItems;
        if (items != null) {
            for (int i = items.size(); --i >= 0;) {
                if (items.get(i) == menu) {
                    items.remove(i);
                    if (i == 0) {
                        if (!items.isEmpty()) {
                            if (items.get(0) instanceof SeparatorMenuItem) {
                                items.remove(0);
                            } else {
                                break;      // Some other windows are still present.
                            }
                        }
                        ((WindowsProperty) hasWindowsProperty).set(false);
                    }
                    break;
                }
            }
        }
    }
}
