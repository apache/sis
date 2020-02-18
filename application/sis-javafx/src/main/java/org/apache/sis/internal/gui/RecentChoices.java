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
package org.apache.sis.internal.gui;

import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.scene.control.ComboBox;
import javafx.collections.ObservableList;


/**
 * Stores recent user choices, for example the last directory opened.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class RecentChoices {
    /**
     * The nodes where to store user information (for example last directory opened).
     * We want node for the {@code "org.apache.sis.gui"} package, which is the public one.
     */
    private static final Preferences NODE = Preferences.userNodeForPackage​(org.apache.sis.gui.DataViewer.class);

    /**
     * The node where to store the directory containing last data loaded.
     */
    private static final String OPEN = "Open";

    /**
     * Do not allow instantiation of this class.
     */
    private RecentChoices() {
    }

    /**
     * Returns the directory to show in "Open" dialog.
     *
     * @return the initial open directory, or {@code null} if none.
     */
    public static File getOpenDirectory() {
        final String value = NODE.get(OPEN, null);
        return (value != null) ? new File(value) : null;
    }

    /**
     * Saves the directory to show next time that an "Open" dialog will be shown.
     *
     * @param  files  files selected by user.
     */
    public static void setOpenDirectory(final List<File> files) {
        final File parent = getCommonParent(files);
        NODE.put(OPEN, parent != null ? parent.getAbsolutePath() : null);
    }

    /**
     * Returns the common parent of a list of files.
     * This is used for selecting which directory to remember for the next open or save dialog box.
     *
     * @param  files  files for which to get a common parent.
     * @return the common parent, or {@code null} if none.
     */
    private static File getCommonParent(final List<File> files) {
        File parent = null;
        for (final File file : files) {
            final File otherParent = file.getParentFile();
            if (otherParent != null && !otherParent.equals(parent)) {
                if (parent == null) {
                    parent = otherParent;
                } else {
                    final String path = otherParent.getAbsolutePath();
                    while (!path.startsWith(parent.getAbsolutePath())) {
                        parent = parent.getParentFile();
                        if (parent == null) return null;
                    }
                }
            }
        }
        return parent;
    }

    /**
     * Sets a value in a list of choices provided by an editable combo box.
     * The combo box will remember the last choices, up to an arbitrary limit.
     * The most recently used choices appear firsT.
     *
     * @param  <T>       type of values in the combo box.
     * @param  choices   the list of choices to update.
     * @param  newValue  the new choice.
     */
    public static <T> void setInList(final ComboBox<T> choices, final T newValue) {
        final ObservableList<T> items = choices.getItems();
        final int p = items.indexOf(newValue);
        if (p != 0) {
            if (p > 0) {
                items.remove(p);
            } else {
                final int count = items.size();
                if (count >= 20) {
                    items.remove(count - 1);
                }
            }
            items.add(0, newValue);
        }
        choices.getSelectionModel().selectFirst();
    }
}
