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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import org.apache.sis.internal.gui.FontGlyphs;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;

/**
 * Tree viewer displaying a {@link Resource} hierarchy.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class ResourceTree extends TreeTableView<Resource> {

    private static final Image ICON_VECTOR = SwingFXUtils.toFXImage(FontGlyphs.createImage("\uE922",18,Color.GRAY),null);
    private static final Image ICON_FOLDER = SwingFXUtils.toFXImage(FontGlyphs.createImage("\uE2C8",18,Color.GRAY),null);
    private static final Image ICON_STORE = SwingFXUtils.toFXImage(FontGlyphs.createImage("\uE2C7",18,Color.GRAY),null);
    private static final Image ICON_OTHER = SwingFXUtils.toFXImage(FontGlyphs.createImage("\uE24D",18,Color.GRAY),null);

    public ResourceTree() {
        getColumns().add(new ResourceNameColumn());
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * Get root {@link Resource} of this tree.
     *
     * @return root {@link Resource}, may be null
     */
    public Resource getResource() {
        final TreeItem<Resource> root = getRoot();
        return root==null ? null : root.getValue();
    }

    /**
     * Set root {@link Resource} of this tree.
     *
     * @param resource can be null
     */
    public void setResource(Resource resource) {
        if (resource==null) {
            setRoot(null);
        } else {
            setRoot(new ResourceItem(resource));
        }
    }

    /**
     * Find an appropriate icon for given resource.
     *
     * @param resource resource to test
     * @return Image icon
     */
    private static Image getTypeIcon(Resource resource){
        if (resource instanceof FeatureSet) {
            return ICON_VECTOR;
        } else if (resource instanceof DataStore) {
            return ICON_STORE;
        } else if (resource instanceof Aggregate) {
            return ICON_FOLDER;
        } else {
            //unspecific resource type
            return ICON_OTHER;
        }
    }

    private static String getIdentifier(Metadata metadata) {
        final Collection<? extends Identification> identifications = metadata.getIdentificationInfo();
        for (Identification identification : identifications) {
            final Citation citation = identification.getCitation();
            if (citation != null) {
                for (Identifier identifier : citation.getIdentifiers()) {
                    return identifier.getCode();
                }
            }
        }
        return null;
    }


    private static class ResourceItem extends TreeItem<Resource> {

        private final Resource resource;
        private boolean isFirstTimeChildren = true;

        public ResourceItem(Resource res) {
            super(res);
            this.resource = res;
        }

        public ObservableList<TreeItem<Resource>> getChildren() {
            if (isFirstTimeChildren) {
                isFirstTimeChildren = false;
                super.getChildren().setAll(buildChildren());
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            return !(resource instanceof Aggregate);
        }

        private List<TreeItem<Resource>> buildChildren() {
            if (resource instanceof Aggregate) {
                final List<TreeItem<Resource>> lst = new ArrayList<>();
                try {
                    for (Resource res : ((Aggregate) resource).components()) {
                        lst.add(new ResourceItem(res));
                    }
                } catch (DataStoreException ex) {
                    ex.printStackTrace();
                }
                return lst;
            }
            return Collections.EMPTY_LIST;
        }

    }

    private static class ResourceNameColumn extends TreeTableColumn<Resource,String> {

        public ResourceNameColumn() {
            super("Resource");
            setCellValueFactory(new Callback<CellDataFeatures<Resource, String>, javafx.beans.value.ObservableValue<java.lang.String>>() {
                @Override
                public ObservableValue<String> call(CellDataFeatures<Resource, String> param) {
                    try {
                        return new SimpleObjectProperty<>(getIdentifier(param.getValue().getValue().getMetadata()));
                    } catch (DataStoreException ex) {
                       return new SimpleObjectProperty<>(ex.getMessage());
                    }
                }
            });
            setCellFactory((TreeTableColumn<Resource, String> param) -> new Cell());
            setEditable(true);
            setPrefWidth(200);
            setMinWidth(120);
            setResizable(true);
        }

    }

    private static class Cell extends TreeTableCell<Resource, String> {

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(item);
            setGraphic(null);
            setContentDisplay(ContentDisplay.LEFT);
            setAlignment(Pos.CENTER_LEFT);
            setTextAlignment(TextAlignment.LEFT);
            setWrapText(false);
            if (empty) return;

            final TreeTableRow<Resource> row = getTreeTableRow();
            if (row == null) {
                return;
            }
            final TreeItem<Resource> ti = row.getTreeItem();
            if (ti == null) {
                return;
            }

            final Resource resource = ti.getValue();
            setGraphic(new ImageView(getTypeIcon(resource)));
        }
    }

}
