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
package org.apache.sis.gui.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.sis.internal.storage.folder.FolderStoreProvider;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;


/**
 * Show resources available in data files.
 *
 * @author Smaniotto Enzo
 */
public class ResourceView {

    private final TreeItem<Label> root = new TreeItem<>(new Label("Files"));
    public final SplitPane pane = new SplitPane();
    private final Map<Label, TreeItem<?>> labelToItem = new HashMap<>();

    // TODO: What is that?
    private final List<String> LABINTRV = new ArrayList<>();

    private Label sauvLabel, parent;

    public ResourceView() {
        pane.setStyle("-fx-background-color: linear-gradient(to bottom right, #aeb7c4, #fafafa);");
        final VBox dragTarget = new VBox();

        root.setExpanded(true);
        final TreeView<Label> resources = new TreeView<>(root);
        resources.setStyle("-fx-background-color: rgba(77, 201, 68, 0.4);");
        resources.setShowRoot(false);

        resources.setOnDragOver(event -> {
            if (event.getGestureSource() != dragTarget && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        resources.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            File firstFile = db.getFiles().get(0);
            if (firstFile.isDirectory()) {
                openDirectory(firstFile);
                success = true;
            } else {
                if (db.hasFiles()) {
                    success = true;
                }
                List<?> fileList = (List<?>) db.getContent(DataFormat.FILES);
                for (Object item : fileList) {
                    File f = (File) item;
                    openFile(f);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
        pane.getItems().add(resources);
    }

    private MetadataOverview getContent() {
        final ObservableList<Node> items = pane.getItems();
        if (items.size() >= 2) {
            return (MetadataOverview) items.get(1);
        } else {
            return null;
        }
    }

    private void setContent(final MetadataOverview content) {
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
            root.getChildren().remove(labelToItem.get(lab));
            labelToItem.remove(lab);
            LABINTRV.remove(lab.getId());
            final MetadataOverview content = getContent();
            if (content != null) {
                if (lab.getId().equals(content.getFromFile())) {
                    setContent(null);
                }
            }
        });
        ContextMenu cm = new ContextMenu(close);
        lab.setContextMenu(cm);
    }

    private void setOnClick(Label lab) {
        addContextMenu(lab);
        lab.setOnMouseClicked(click -> {
            if (click.getButton() == MouseButton.PRIMARY) {
                if (sauvLabel != null) {
                    sauvLabel.setTextFill(Color.BLACK);
                }
                addMetadatPanel(lab.getId());
                sauvLabel = lab;
                lab.setTextFill(Color.RED);
            }
        });
    }

    private boolean checkMetaPanel(String filePath) {
        MetadataOverview meta;
        try {
            DataStore ds = DataStores.open(filePath);
            meta = new MetadataOverview(new DefaultMetadata(ds.getMetadata()), filePath);
            return true;
        } catch (DataStoreException e) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("An error was occur");
            Label lab = new Label(e.getMessage());
            lab.setWrapText(true);
            lab.setMaxWidth(650);
            VBox vb = new VBox();
            vb.getChildren().add(lab);
            alert.getDialogPane().setContent(vb);
            alert.show();
            return false;
        }
    }

    private void addMetadatPanel(String filePath) {
        MetadataOverview meta;
        try {
            DataStore ds = DataStores.open(filePath);
            meta = new MetadataOverview(new DefaultMetadata(ds.getMetadata()), filePath);
            setContent(meta);
        } catch (DataStoreException e) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("An error was occur");
            Label lab = new Label(e.getMessage());
            lab.setWrapText(true);
            lab.setMaxWidth(650);
            VBox vb = new VBox();
            vb.getChildren().add(lab);
            alert.getDialogPane().setContent(vb);
            alert.show();
        }
    }

    private void addMetadatPanel(Resource res, String filePath) {
        MetadataOverview meta;
        try {
            meta = new MetadataOverview(((DefaultMetadata) res.getMetadata()), filePath);
            setContent(meta);
        } catch (DataStoreException e) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("An error was occur");
            Label lab = new Label(e.getMessage());
            lab.setWrapText(true);
            lab.setMaxWidth(650);
            VBox vb = new VBox();
            vb.getChildren().add(lab);
            alert.getDialogPane().setContent(vb);
            alert.show();
        }
    }

    public void open(final List<File> files) {
        for (final File file : files) {
            if (file.isDirectory()) {
                openDirectory(file);
            } else {
                openFile(file);
            }
        }
    }

    private void openFile(File f) {
        if (checkMetaPanel(f.getAbsolutePath())) {
            Label label = new Label(f.getName());
            label.setId(f.getAbsolutePath());
            if (LABINTRV.contains(f.getAbsolutePath())) {
                for (Label elem : labelToItem.keySet()) {
                    if (elem.getId().equals(f.getAbsolutePath())) {
                        Event.fireEvent(elem, new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1, true, true, true, true, true, true, true, true, true, true, null));
                    }
                }
            } else {
                setOnClick(label);
                TreeItem<Label> tItem = new TreeItem<>(label);
                labelToItem.put(label, tItem);
                LABINTRV.add(f.getAbsolutePath());
                root.getChildren().add(tItem);
            }
        }
    }

    private void openDirectory(File firstFile) {
        if (!LABINTRV.contains("[Aggregate] " + firstFile.getName())) {
            DataStore resource = null;
            try {
                resource = FolderStoreProvider.INSTANCE.open(new StorageConnector(firstFile));
            } catch (DataStoreException ex) {
                ex.getMessage();
            }
            if (resource instanceof Aggregate) {
                parent = new Label("[Aggregate] " + firstFile.getName());
                parent.setId(firstFile.getAbsolutePath());
                Label cl = parent;
                parent.setOnMouseClicked(click -> {
                    if (click.getButton() == MouseButton.SECONDARY) {
                        parent = cl;
                    }
                });
                TreeItem<Label> ti = new TreeItem<>(parent);

                MenuItem close = new MenuItem("Close");
                close.setOnAction(ac -> {
                    root.getChildren().remove(labelToItem.get(cl));
                    labelToItem.remove(cl);
                    LABINTRV.remove(parent.getText());
                    final MetadataOverview content = getContent();
                    if (content != null) {
                        if (parent.getId().equals(content.getFromFile())) {
                            setContent(null);
                        }
                    }
                });
                ContextMenu cm = new ContextMenu(close);
                parent.setContextMenu(cm);

                try {
                    for (Resource res : ((Aggregate) resource).components()) {
                        Label lab = new Label(res.toString());
                        lab.setOnMouseClicked(click -> {
                            if (click.getButton() == MouseButton.PRIMARY) {
                                if (sauvLabel != null) {
                                    sauvLabel.setTextFill(Color.BLACK);
                                }
                                addMetadatPanel(res, cl.getId());
                                sauvLabel = lab;
                                lab.setTextFill(Color.RED);
                            }
                        });
                        TreeItem<Label> tiChild = new TreeItem<>(lab);
                        ti.getChildren().add(tiChild);
                    }
                } catch (DataStoreException ex) {
                    System.out.println(ex.getMessage());        // TODO: NO!!!!!
                }
                root.getChildren().add(ti);
                LABINTRV.add(parent.getText());
                labelToItem.put(parent, ti);
            }
        } else { // If the file is already open.
            if (sauvLabel != null) {
                sauvLabel.setTextFill(Color.BLACK);
            }
            root.getChildren().forEach(elem -> {
                if (elem.getValue().getText().equals("[Aggregate] " + firstFile.getName())) {
                    sauvLabel = elem.getValue();
                    elem.getValue().setTextFill(Color.RED);
                }
            });
            setContent(null);
        }
    }
}
