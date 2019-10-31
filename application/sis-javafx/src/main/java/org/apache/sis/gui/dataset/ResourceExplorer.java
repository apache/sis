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
import javafx.scene.layout.Region;
import javafx.scene.control.SplitPane;
import org.apache.sis.storage.Resource;


/**
 * A panel showing a {@linkplain ResourceTree tree of resources} together with their metadata.
 *
 * @author  Smaniotto Enzo
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
     * The widget showing metadata about a selected resource.
     */
    private final MetadataOverview metadata;

    /**
     * The control that put everything together.
     * The type of control may change in any future SIS version.
     */
    private final SplitPane pane;

    /**
     * Creates a new panel for exploring resources.
     */
    public ResourceExplorer() {
        resources = new ResourceTree();
        metadata  = new MetadataOverview();
        pane      = new SplitPane();
        pane.getItems().setAll(resources, metadata);
    }

    /**
     * Returns the region containing the resource tree, metadata panel or any other control managed
     * by this {@code ResourceExplorer}.
     *
     * @return the region to show.
     */
    public final Region getPane() {
        return pane;
    }

    /**
     * Adds all the given resources to the resource tree. The given collection typically contains
     * files to load, but may also contain {@link Resource} instances to add directly.
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
}
