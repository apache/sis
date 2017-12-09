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
import java.util.Collections;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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
import org.apache.sis.internal.util.Citations;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.opengis.util.InternationalString;


/**
 * Tree viewer displaying a {@link Resource} hierarchy.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class ResourceTree extends TreeTableView<Resource> {

    private static final Image ICON_VECTOR = FontGlyphs.createImage("\uE922",18,Color.GRAY);
    private static final Image ICON_FOLDER = FontGlyphs.createImage("\uE2C8",18,Color.GRAY);
    private static final Image ICON_STORE = FontGlyphs.createImage("\uE2C7",18,Color.GRAY);
    private static final Image ICON_OTHER = FontGlyphs.createImage("\uE24D",18,Color.GRAY);

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

    /**
     * Returns a label for a resource. Current implementation builds a string containing the resource title
     * if non-ambiguous, followed by filename in order to resolve ambiguity that may be caused by different
     * files having the same resource identification in their metadata.
     *
     * @param  name      the result of {@link DataStore#getDisplayName()}, or {@code null} if unknown.
     * @param  metadata  the result of {@link DataStore#getMetadata()} (may be {@code null}).
     */
    private static String getTitle(final String name, final Metadata metadata) {
        if (metadata != null) {
            String title = null;
            for (final Identification identification : CollectionsExt.nonNull(metadata.getIdentificationInfo())) {
                final Citation citation = identification.getCitation();
                if (citation != null) {
                    final InternationalString i18n = citation.getTitle();
                    String id;
                    if (i18n != null) {
                        id = i18n.toString();                   // TODO: use display locale.
                    } else {
                        id = Citations.getIdentifier(identification.getCitation(), false);
                    }
                    if (id != null && !(id = id.trim()).isEmpty()) {
                        if (title == null) {
                            title = id;
                        } else if (!title.equals(id)) {
                            return name;                        // Ambiguity - will use the filename instead.
                        }
                    }
                }
            }
            if (title != null) {
                if (name != null) {
                    title += " (" + name + ')';
                }
                return title;
            }
        }
        return name;
    }


    private static class ResourceItem extends TreeItem<Resource> {

        private final Resource resource;
        private boolean isFirstTimeChildren = true;

        public ResourceItem(Resource res) {
            super(res);
            this.resource = res;
        }

        @Override
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
                    final Resource res = param.getValue().getValue();
                    final String name = (res instanceof DataStore) ? ((DataStore) res).getDisplayName() : null;
                    try {
                        return new SimpleObjectProperty<>(getTitle(name, res.getMetadata()));
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
