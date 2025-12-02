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
package org.apache.sis.gui;

import java.io.File;
import java.util.Set;
import java.util.StringJoiner;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.collections.ObservableList;
import org.apache.sis.gui.dataset.ResourceEvent;
import org.apache.sis.gui.dataset.ResourceExplorer;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.internal.RecentChoices;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.ArraysExt;


/**
 * Manages a list of recently opened files. The list of files is initialized from user preferences.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class RecentFiles implements EventHandler<ActionEvent> {
    /**
     * Maximum number of items to show.
     */
    @Configuration
    static final int MAX_COUNT = 10;

    /**
     * Menu items for each recently opened file.
     * Items will be added and removed when files are opened or closed.
     * This list should have no more than {@value #MAX_COUNT} elements.
     */
    private final ObservableList<MenuItem> items;

    /**
     * The explorer where to open the file.
     * This is used when user selects an "Open recent file" menu item.
     */
    private final ResourceExplorer explorer;

    /**
     * Recent files, regardless if opened or not.
     * May contain {@code null} trailing elements.
     */
    private final File[] allFiles;

    /**
     * Creates a new handler for a list of recently used files in the specified menu.
     */
    private RecentFiles(final ResourceExplorer explorer, final Menu menu) {
        this.explorer = explorer;
        items = menu.getItems();
        allFiles = new File[MAX_COUNT];
        final CharSequence[] paths = RecentChoices.getFiles();
        final MenuItem[] menus = new MenuItem[paths.length];
        final int limit = Math.min(paths.length, MAX_COUNT);
        int count = 0;
        for (int i=0; i<limit; i++) {
            final String path = paths[i].toString();
            if (!path.isBlank()) {
                final File file = new File(path);
                allFiles[count] = file;
                menus[count++]  = createItem(file);
            }
        }
        items.setAll(ArraysExt.resize(menus, count));
    }

    /**
     * Creates a menu for a list of recently used files.
     * The menu is initialized with a list of files fetched from user preferences.
     * The user preferences are updated when {@link #touched(ResourceEvent, boolean)} is invoked.
     */
    static Menu create(final ResourceExplorer explorer, final Resources localized) {
        final Menu menu = new Menu(localized.getString(Resources.Keys.OpenRecentFile));
        final RecentFiles handler = new RecentFiles(explorer, menu);
        explorer.setOnResourceLoaded((e) -> handler.touched(e, false));
        explorer.setOnResourceClosed((e) -> handler.touched(e, true));
        return menu;
    }

    /**
     * Creates a new menu item for the specified file.
     */
    private MenuItem createItem(final File file) {
        final MenuItem item = new MenuItem(file.getName());
        item.setUserData(file);
        item.setOnAction(this);
        return item;
    }

    /**
     * Notifies that a file is opened or closed. If opened, the file item (if any) is temporarily removed.
     * If closed, a new menu items is created for the specified file and is inserted at the beginning of
     * the list of recent files. If the list of recent files has more than {@value #MAX_COUNT} elements,
     * the latest item is discarded.
     */
    private void touched(final ResourceEvent event, final boolean closed) {
        final File file = event.getResourceFile().orElse(null);
        if (file == null) {
            // Recently used URIs are not saved here.
            return;
        }
        final int size = items.size();
        /*
         * Verifies if an item already exists for the given file.
         * If yes, we will remove it (if opening) or move it to the top (if closing).
         */
        MenuItem item = null;
        for (int i=0; i<size; i++) {
            if (file.equals(items.get(i).getUserData())) {
                item = items.remove(i);
                break;
            }
        }
        if (closed) {
            if (item == null) {
                if (size >= MAX_COUNT) {
                    item = items.remove(size-1);                // Recycle existing menu item.
                    item.setText(file.getName());
                    item.setUserData(file);
                } else {
                    item = createItem(file);
                }
            }
            items.add(0, item);     // Reinsert the removed menu item at the head of the list.
        }
        /*
         * At this point the menu items has been updated.
         * Now update the array of files (both opened and closed) and save in user preferences.
         */
        int i = 0;      // Index of file to discard (either because null, last or moved to head).
        do {
            final File f = allFiles[i];
            if (f == null || file.equals(f)) break;
        } while (++i < MAX_COUNT-1);
        System.arraycopy(allFiles, 0, allFiles, 1, i);
        allFiles[0] = file;
        final var s = new StringJoiner(System.lineSeparator());
        for (final File f : allFiles) {
            if (f == null) break;
            s.add(f.getPath());
        }
        RecentChoices.setFiles(s.toString());
    }

    /**
     * Invoked when the user selects a file.
     */
    @Override
    public void handle(final ActionEvent event) {
        final Object file = ((MenuItem) event.getSource()).getUserData();
        explorer.loadResources(Set.of(file));
    }
}
