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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.geometry.Pos;
import javafx.util.Callback;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.text.TextAlignment;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;


/**
 * Tree viewer displaying a {@link Resource} hierarchy.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ResourceTree extends TreeTableView<Resource> {

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
        return root == null ? null : root.getValue();
    }

    /**
     * Set root {@link Resource} of this tree.
     *
     * @param resource can be null
     */
    public void setResource(Resource resource) {
        if (resource == null) {
            setRoot(null);
        } else {
            setRoot(new ResourceItem(resource));
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
                        id = Citations.getIdentifier(identification.getCitation());
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
                    ex.printStackTrace();                   // TODO: need better handling.
                }
                return lst;
            }
            return Collections.emptyList();
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
        }
    }
}
