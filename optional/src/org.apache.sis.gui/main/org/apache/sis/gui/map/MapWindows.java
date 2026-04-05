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
package org.apache.sis.gui.map;

import java.util.Map;
import java.util.LinkedHashMap;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Rectangle2D;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.FrequencySortedSet;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A group of windows showing map canvases.
 * Each window can show a mosaic of {@link MapCanvas} instances of the same size and a single {@link StatusBar}.
 * User's navigation can optionally by synchronized so that panning or zooming in one map causes the same pan or
 * zoom to be applied in the other maps.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 * @since   1.7
 */
public class MapWindows implements AutoCloseable {
    /**
     * The owner, or {@code null} if none.
     */
    private final Window owner;

    /**
     * All windows created by this {@code MapWindows} instance.
     * Values may be {@code null} before the window is created.
     * Entries are removed when windows are closed.
     */
    private final Map<MultiCanvas, Stage> windows;

    /**
     * Creates an initially empty group of windows.
     * If {@code owner} is non-null, then the windows created by this class will always be on top of the owner
     * and closing or minimizing {@code owner} will also close or minimize the windows created by this class.
     *
     * @param  owner  the parent stage, or {@code null} if none.
     */
    public MapWindows(final Window owner) {
        this.owner = owner;
        windows = new LinkedHashMap<>();
    }

    /**
     * Creates a new window for the specified {@code MultiCanvas}.
     * This method shall be invoked at most once per {@code canvas}.
     */
    private Stage newWindow(final MultiCanvas canvas) {
        final Stage window = new Stage();
        if (owner != null) window.initOwner(owner);
        window.setScene(new Scene(canvas.getView()));
        /*
         * We use an initial size covering a large fraction of the screen because
         * this window is typically used for showing image or large tabular data.
         */
        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        window.setWidth (0.8 * bounds.getWidth());
        window.setHeight(0.8 * bounds.getHeight());
        window.setOnHidden((event) -> {
            windows.remove(canvas);
            canvas.dispose();
        });
        return window;
    }

    /**
     * Returns whether the given resource is supported.
     *
     * @param  resource   the resource to show, or {@code null}.
     * @return whether the given resource can be shown.
     */
    public boolean isSupported(final Resource resource) {
        return MultiCanvas.isSupported(resource);
    }

    /**
     * Tries to allocate a canvas for the given resource and to show it.
     * The first time that this method is invoked, it will show a new window.
     * On next invocations, the resources are added in the existing window.
     * The same resource may be shown many times. Null resources are ignored.
     *
     * @param  resource  the resource to add, or {@code null}.
     * @return whether the given resource has been accepted.
     */
    public boolean addResource(final Resource resource) {
        if (windows.isEmpty()) {
            final var canvas = new MultiCanvas();
            canvas.addListener((c) -> updateWindowTitles());
            windows.put(canvas, null);
        }
        for (final Map.Entry<MultiCanvas, Stage> entry : windows.entrySet()) {
            final MultiCanvas canvas = entry.getKey();
            if (canvas.addResource(resource)) {
                Stage window = entry.getValue();
                if (window == null) {
                    window = newWindow(canvas);
                    entry.setValue(window);
                }
                window.show();
                window.toFront();
                return true;
            }
        }
        return false;
    }

    /**
     * Recomputes the titles of all windows by using the title of one of the canvas.
     * This method prefers titles that are not shared by canvases in different windows,
     * in order to use a distinct title for each window.
     */
    private void updateWindowTitles() {
        final var allTitles = new FrequencySortedSet<String>();
        final var perWindow = new LinkedHashMap<Stage, FrequencySortedSet<String>>();
        for (final Map.Entry<MultiCanvas, Stage> entry : windows.entrySet()) {
            FrequencySortedSet<String> titles = entry.getKey().getCanvasTitles();
            perWindow.put(entry.getValue(), titles);
            allTitles.addAll(titles);
        }
        /*
         * Iterate over the less frequently used titles first. In the common case where a
         * title is used only once, this strategy gives a distinct title for each window.
         */
        for (final String title : allTitles) {
            int frequency = 0;
            Stage window = null;    // Window where to set the title.
            for (final Map.Entry<Stage, FrequencySortedSet<String>> entry : perWindow.entrySet()) {
                final FrequencySortedSet<String> titles = entry.getValue();
                final int n = titles.frequency(title);
                if (n > frequency) {
                    frequency = n;
                    window = entry.getKey();
                }
            }
            if (window != null) {
                window.setTitle(title.concat(" — Apache SIS"));
                perWindow.remove(window);
            }
        }
        /*
         * If any window got no name, assigne a fallback name.
         */
        for (final Map.Entry<Stage, FrequencySortedSet<String>> entry : perWindow.entrySet()) {
            String title = Containers.peekFirst(entry.getValue());
            if (title == null) title = Vocabulary.format(Vocabulary.Keys.Unnamed);
            entry.getKey().setTitle(title);
        }
    }

    /**
     * Hides all windows and releases resources. Invoking this method is not strictly necessary
     * (waiting for the garbage-collector is sufficient), but may help to release memory faster.
     */
    @Override
    public void close() {
        for (final Map.Entry<MultiCanvas, Stage> entry : windows.entrySet()) {
            entry.getKey().dispose();
            entry.getValue().close();
        }
        windows.clear();
    }
}
