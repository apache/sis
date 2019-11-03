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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.beans.property.SimpleObjectProperty;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyType;
import org.opengis.geometry.Geometry;
import org.apache.sis.internal.util.CheckedArrayList;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;


/**
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Smaniotto Enzo (GSoC)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class FeatureTable extends BorderPane {
    /**
     * Contains ResourceBundles indexed by table names.
     */
    private final Map<String, ResourceBundle> bundles = new HashMap<>();

    private String bundlePrefix;

    private String generateFinalColumnName(final PropertyType prop) {
        Map<String, Map.Entry<String, String>> labelInfo = (Map) prop.getDesignation();
        final String labelName = prop.getName().toString();
        String columnName = labelName;
        String tableName = null;
        /*
         * If exists, explore labelInfo to retrive table and column respect to this label.
         */
        if (labelInfo != null) {
            final Map.Entry<String, String> entry = labelInfo.get(labelName);
            if (entry != null) {
                if (entry.getKey() != null) {
                    tableName = entry.getKey();
                } else {
                    tableName = null;
                }
                if (entry.getValue() != null) {
                    columnName = entry.getValue();
                } else {
                    columnName = labelName;
                }
            }
        }
        /*
         * If table name is not null, try to found resourcebundle for this table.
         */
        if (tableName != null) {
            /*
             * If there isn't resource bundles (or not for the curruen table), try to generate.
             */
            if (bundles.get(tableName) == null) {
                if (bundlePrefix != null) {
                    bundles.put(tableName, ResourceBundle.getBundle(bundlePrefix + tableName));
                }
            }
        }
        final ResourceBundle bundle = bundles.get(tableName);
        String finalColumnName;
        if (labelName == null) {
            finalColumnName = "";
        } else if (bundle == null) {
            if (!labelName.equals(columnName)) {
                finalColumnName = columnName + " as " + labelName;
            } else {
                finalColumnName = columnName;
            }
        } else {
            try {
                if (!labelName.equals(columnName)) {
                    finalColumnName = bundle.getString(columnName) + " as " + labelName;
                } else {
                    finalColumnName = bundle.getString(columnName);
                }
            } catch (MissingResourceException ex) {
                if (!labelName.equals(columnName)) {
                    finalColumnName = columnName + " as " + labelName;
                } else {
                    finalColumnName = columnName;
                }
            }
        }
        return finalColumnName;
    }

    public FeatureTable(Resource res, int i) throws DataStoreException {
        TableView<Feature> ttv = new TableView<>();
        final ScrollPane scroll = new ScrollPane(ttv);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        ttv.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        ttv.setTableMenuButtonVisible(true);
        ttv.setFixedCellSize(100);
        scroll.setPrefSize(600, 400);
        scroll.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setCenter(scroll);
        final List<Feature> list;
        if (res instanceof FeatureSet) {
            try (Stream<Feature> stream = ((FeatureSet) res).features(false)) {
                list = stream.collect(Collectors.toList());
                ttv.setItems(FXCollections.observableArrayList(list));
                for (PropertyType pt : list.get(0).getType().getProperties(false)) {
                    final TableColumn<Feature, BorderPane> column = new TableColumn<>(generateFinalColumnName(pt));
                    column.setCellValueFactory((TableColumn.CellDataFeatures<Feature, BorderPane> param) -> {
                        final Object val = param.getValue().getPropertyValue(pt.getName().toString());
                        if (val instanceof Geometry) {
                            return new SimpleObjectProperty<>(new BorderPane(new Label("{geometry}")));
                        } else {
                            SimpleObjectProperty<BorderPane> sop = new SimpleObjectProperty<>();
                            if (val instanceof CheckedArrayList<?>) {
                                Iterator<String> it = ((CheckedArrayList<String>) val).iterator();
                                TreeItem<String> ti = new TreeItem<>(it.next());
                                while (it.hasNext()) {
                                    ti.getChildren().add(new TreeItem<>(it.next()));
                                }
                                BorderPane bp = new BorderPane(new TreeView<>(ti));
                                sop.setValue(bp);
                                return sop;
                            } else {
                                sop.setValue(new BorderPane(new Label(String.valueOf(val))));
                                return sop;
                            }
                        }
                    });
                    ttv.getColumns().add(column);
                }
            }
        }
    }
}
