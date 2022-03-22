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
import java.util.Arrays;
import java.util.Collection;
import java.util.prefs.Preferences;
import javafx.scene.control.ComboBox;
import javafx.collections.ObservableList;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.FrequencySortedSet;


/**
 * Stores recent user choices, for example the last directory opened.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public final class RecentChoices {
    /**
     * Maximum number of reference systems to save in the preferences. Note that this is not necessarily
     * the maximum number of reference systems shown in the GUI (that maximum is lower), because the GUI
     * will filter out the reference systems that are valid outside the domain of interest.
     *
     * @see #useReferenceSystem(String)
     */
    public static final int MAXIMUM_REFERENCE_SYSTEMS = 20;

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
     * The node where to store recently opened files.
     */
    private static final String FILES = "RecentFiles";

    /**
     * The node where to store recently opened URLs.
     */
    private static final String URLS = "RecentURLs";

    /**
     * The node where to store authority (usually EPSG) codes of most recently used coordinate reference systems.
     */
    private static final String CRS = "ReferenceSystems";

    /**
     * The coordinate reference systems used in current JVM run, with most frequently used systems first.
     * The CRS are stored by their authority codes. Access to this set must be synchronized.
     */
    private static final FrequencySortedSet<String> CRS_THIS_RUN = new FrequencySortedSet<>(true);

    /**
     * Do not allow instantiation of this class.
     */
    private RecentChoices() {
    }

    /**
     * Returns the directory to show in "Open" dialog.
     * If the directory does not exist anymore, its parent directory is returned instead.
     *
     * @return the initial open directory, or {@code null} if none.
     */
    public static File getOpenDirectory() {
        final String value = NODE.get(OPEN, null);
        if (value != null) {
            File file = new File(value);
            do {
                if (file.isDirectory()) return file;
                file = file.getParentFile();
            } while (file != null);
        }
        return null;
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
     * Returns recently opened files.
     *
     * @return recently opened files, or an empty array if none.
     */
    public static CharSequence[] getFiles() {
        return CharSequences.splitOnEOL(NODE.get(FILES, null));
    }

    /**
     * Sets the list of recently opened files.
     * The files shall be specified in a EOL-separated string.
     *
     * @param files recently opened files.
     */
    public static void setFiles(final String files) {
        NODE.put(FILES, files);
    }

    /**
     * Returns recently opened URLs.
     *
     * @param  addTo  the list where to add recent URLs.
     */
    public static void getURLs(final Collection<String> addTo) {
        for (CharSequence url : CharSequences.splitOnEOL(NODE.get(URLS, null))) {
            addTo.add(url.toString());
        }
    }

    /**
     * Sets the list of recently opened URLs.
     *
     * @param files recently opened URLs.
     */
    public static void setURLs(final Collection<String> files) {
        NODE.put(URLS, String.join(System.lineSeparator(), files));
    }

    /**
     * Returns the authority codes of most recently used reference systems.
     *
     * @return authority codes, or an empty array if none.
     */
    public static String[] getReferenceSystems() {
        final String[] codes;
        synchronized (CRS_THIS_RUN) {
            final int n = CRS_THIS_RUN.size();
            if (n != 0) {
                codes = CRS_THIS_RUN.toArray(new String[n]);
            } else {
                final String value = NODE.get(CRS, null);
                codes = (String[]) CharSequences.split(value, ',');
                CRS_THIS_RUN.addAll(Arrays.asList(codes));
            }
        }
        return codes;
    }

    /**
     * Notifies the preferences that the CRS identified by the given code has been selected.
     * If the given value is {@code null}, then it is ignored.
     *
     * @param  code  code of the CRS selected by user, or {@code null}.
     */
    public static void useReferenceSystem(final String code) {
        if (code != null) {
            final String[] codes;
            synchronized (CRS_THIS_RUN) {
                if (!CRS_THIS_RUN.add(code.trim())) {
                    return;
                }
                codes = CRS_THIS_RUN.toArray(new String[CRS_THIS_RUN.size()]);
            }
            saveReferenceSystems(codes);
        }
    }

    /**
     * Saves the authority codes of most recently used reference systems.
     * This method should be invoked when the application shutdowns.
     */
    public static void saveReferenceSystems() {
        final String[] codes;
        synchronized (CRS_THIS_RUN) {
            codes = CRS_THIS_RUN.toArray(new String[CRS_THIS_RUN.size()]);
        }
        saveReferenceSystems(codes);
    }

    /**
     * Saves the given list of authority codes.
     * Only the first {@value #MAXIMUM_REFERENCE_SYSTEMS} codes are saved.
     */
    private static void saveReferenceSystems(String[] codes) {
        if (codes.length != 0) {
            if (codes.length > MAXIMUM_REFERENCE_SYSTEMS) {
                codes = Arrays.copyOf(codes, MAXIMUM_REFERENCE_SYSTEMS);
            }
            NODE.put(CRS, String.join(",", codes));
        }
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
