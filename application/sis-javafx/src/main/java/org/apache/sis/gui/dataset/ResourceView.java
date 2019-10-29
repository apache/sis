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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
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
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.TaskOnFile;
import org.apache.sis.internal.storage.folder.FolderStoreProvider;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;


/**
 * Shows resources available in data files.
 *
 * @author  Smaniotto Enzo
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ResourceView {

    private final TreeItem<Label> root = new TreeItem<>(new Label("Files"));

    // Those variables are used to temporary fix a bug while the FeatureTable is open (the root item erase his childrens so their disappear of the TreeView). this will have to be corrected later.
    private final ObservableList<TreeItem<Label>> temporarySauv;

    private List<TreeItem<Label>> temp = new ArrayList<>();

    public final SplitPane pane = new SplitPane();

    //This map link the respective Tree Items to their own Label in order to keep the informations of the document's name and of it location (stocked in the Label) to the item in the TreeView. Usefull to know which element is selected.
    private final Map<Label, TreeItem<?>> labelToItem = new HashMap<>();

    // This List contain all the elements present in the TreeView, referenced by their id (set to the specific location of each file). Used to know if a file is already open.
    private final List<String> labToTrv = new ArrayList<>();

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
                List<?> fileList = (List<?>) db.getContent(DataFormat.FILES); // To open multiple files in one drop.
                for (Object item : fileList) {
                    File f = (File) item;
                    openFile(f);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Link to the temporary varaible see above. This is a temporary part.
        temporarySauv = root.getChildren();
        temporarySauv.addListener((ListChangeListener.Change<? extends TreeItem<Label>> c) -> {
            c.next();
            if (c.wasRemoved()) {
                temp.addAll(c.getRemoved());
            }
        });

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
            root.getChildren().remove(labelToItem.get(lab));
            labelToItem.remove(lab);
            labToTrv.remove(lab.getId());
            final Object content = getContent();
            if (content != null && content instanceof FeatureTable) {
                pane.getItems().remove(1);
            }
            if (content != null && content instanceof MetadataOverview) {
                if (lab.getId().equals(((MetadataOverview) content).fromFile)) {
                    setContent(new Label("   Please choose a file to open   "));
               }
            }
        });
        MenuItem feature = new MenuItem("Open Feature");
        feature.setOnAction(ac -> {
            try {
                DataStore ds = DataStores.open(lab.getId());
                if (ds instanceof FeatureSet) {
                    root.getChildren().remove(labelToItem.get(lab));
                    addFeaturePanel(lab.getId());
                }
            } catch (DataStoreException ex) {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("An error has occurred");
                lab.setWrapText(true);
                lab.setMaxWidth(650);
                VBox vb = new VBox();
                vb.getChildren().add(lab);
                alert.getDialogPane().setContent(vb);
                alert.show();
            }
        });
        ContextMenu cm = new ContextMenu(feature, close);
        lab.setContextMenu(cm);
    }

    // Define what happens when an element of the TreeView is clicked.
    private void setOnClick(Label lab) {
        addContextMenu(lab);
        lab.setOnMouseClicked(click -> {
            if (click.getButton() == MouseButton.PRIMARY) {
                if (sauvLabel != null) {
                    sauvLabel.setTextFill(Color.BLACK);
                }
                addMetadatPanel(null, lab.getId());
                sauvLabel = lab;
                lab.setTextFill(Color.RED);
            }
        });
    }

    private void addFeaturePanel(String filePath) {
        try {
            DataStore ds = DataStores.open(filePath);
            setContent(new FeatureTable(ds, 18));
            root.getChildren().addAll(temp);
            temp.clear();
        } catch (DataStoreException e) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("An error has occurred");
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
        Task<MetadataOverview> task = new TaskOnFile<MetadataOverview>(Paths.get(filePath)) {
            @Override
            protected MetadataOverview call() throws DataStoreException {
                MetadataOverview meta;
                if (res != null) {
                    meta = new MetadataOverview(((DefaultMetadata) res.getMetadata()), filePath);
                } else {
                    DataStore ds = DataStores.open(file);
                    meta = new MetadataOverview(new DefaultMetadata(ds.getMetadata()), filePath);
                }
                return meta;
            }
        };
        task.setOnSucceeded(wse -> {
            final MetadataOverview meta = task.getValue();
            setContent(meta);
        });
        task.setOnRunning(wse -> {
            ProgressIndicator p = new ProgressIndicator(-1);
            p.setPrefSize(17, 18);
            setContent(p);
        });
        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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
        Task<Void> t = new TaskOnFile<Void>(f.toPath()) {
            @Override
            protected Void call() throws DataStoreException {
                DataStore ds = DataStores.open(file);
                MetadataOverview meta;
                meta = new MetadataOverview(new DefaultMetadata(ds.getMetadata()), file.toString());
                return null;
            }
        };
        t.setOnSucceeded(ac -> {
            Label label = new Label(f.getName());
            label.setId(f.getAbsolutePath());
            if (labToTrv.contains(f.getAbsolutePath())) {
                for (Label elem : labelToItem.keySet()) {
                    if (elem.getId().equals(f.getAbsolutePath())) {
                        Event.fireEvent(elem, new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY,
                                1, true, true, true, true, true, true, true, true, true, true, null));
                    }
                }
            } else {
                setOnClick(label);
                TreeItem<Label> tItem = new TreeItem<>(label);
                labelToItem.put(label, tItem);
                labToTrv.add(f.getAbsolutePath());
                root.getChildren().add(tItem);
            }
        });
        final Thread thread = new Thread(t);
        thread.setDaemon(true);
        thread.start();
    }

    // For the directory, the methods "setOnClick" and "addContextMenu" are redefine inside.
    private void openDirectory(File firstFile) {
        if (!labToTrv.contains("[Aggregate] " + firstFile.getName())) {
            DataStore resource = null;
            try {
                resource = FolderStoreProvider.INSTANCE.open(new StorageConnector(firstFile));
            } catch (DataStoreException ex) {
                ex.getMessage();                    // TODO: needs better handling.
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
                    labToTrv.remove(parent.getText());
                    final Object content = getContent();
                    if (content != null && content instanceof FeatureTable) {
                        pane.getItems().remove(1);
                    }
                    if (content != null && content instanceof MetadataOverview) {
                        if (parent.getId().equals(((MetadataOverview) content).fromFile)) {
                            setContent(new Label("Please choose a file to open"));
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
                    ExceptionReporter.canNotReadFile(firstFile.toPath(), ex);
                }
                root.getChildren().add(ti);
                labToTrv.add(parent.getText());
                labelToItem.put(parent, ti);
            }
        } else {                                                // If the file is already open.
            if (sauvLabel != null) {
                sauvLabel.setTextFill(Color.BLACK);
            }
            root.getChildren().forEach(elem -> {
                if (elem.getValue().getText().equals("[Aggregate] " + firstFile.getName())) {
                    sauvLabel = elem.getValue();
                    elem.getValue().setTextFill(Color.RED);
                }
            });
            setContent(new Label("Please choose a file to open"));
        }
    }
}
