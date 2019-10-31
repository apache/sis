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

import java.io.File;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;


/**
 * Shows resources available in data files.
 *
 * @author  Smaniotto Enzo
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ResourceView {
    private final ResourceTree resources;

    public final SplitPane pane = new SplitPane();

    public ResourceView() {
        resources = new ResourceTree();
        resources.makeDropTarget();
        pane.getItems().add(resources);
    }

    // Get the view of the opened element, return null otherwise.
    private Object getContent() {
        final ObservableList<Node> items = pane.getItems();
        if (items.size() >= 2) {
            return items.get(1);
        } else {
            return null;
        }
    }

    // Set the view of the opened element.
    private void setContent(final Node content) {
        final ObservableList<Node> items = pane.getItems();
        if (items.size() >= 2) {
            items.set(1, content);
        } else {
            items.add(content);
        }
    }

    private void addContextMenu(Label lab) {
        MenuItem close = new MenuItem("Close");
        close.setOnAction(ac -> {
            final Object content = getContent();
            if (content != null && content instanceof FeatureTable) {
                pane.getItems().remove(1);
            }
        });
        MenuItem feature = new MenuItem("Open Feature");
        feature.setOnAction(ac -> {
            try {
                DataStore ds = DataStores.open(lab.getId());
                if (ds instanceof FeatureSet) {
                    addFeaturePanel(lab.getId());
                }
            } catch (DataStoreException ex) {
                ExceptionReporter.canNotReadFile(lab.getId(), ex);
            }
        });
        ContextMenu cm = new ContextMenu(feature, close);
        lab.setContextMenu(cm);
    }

    private void addFeaturePanel(String filePath) {
        try {
            DataStore ds = DataStores.open(filePath);
            setContent(new FeatureTable(ds, 18));
        } catch (DataStoreException e) {
            ExceptionReporter.canNotReadFile(filePath, e);
        }
    }

    private void addMetadatPanel(Resource res) {
        try {
            MetadataOverview meta = new MetadataOverview(((DefaultMetadata) res.getMetadata()));
            setContent(meta);
        } catch (DataStoreException e) {
            ExceptionReporter.canNotReadFile("?", e);
        }
    }

    public void open(final List<File> files) {
        if (!files.isEmpty()) {
            resources.loadResource(files.get(0));
        }
    }
}
