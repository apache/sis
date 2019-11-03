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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.opengis.referencing.ReferenceSystem;


/**
 *
 * @author Siddhesh Rane (GSoC)
 */
final class MetadataTree extends TreeTableView<TreeTable.Node> {

    private static final Predicate<TreeTable.Node> HIDE_EMPTY_LEAF = t -> !t.isLeaf() || t.getValue(TableColumn.VALUE) != null;
    private static final Predicate<TreeTable.Node> EXPAND_SINGLE_CHILD = node -> node.getChildren().size() == 1 || node.getParent() == null;

    private TreeTable treeTable;
    private TreeTableColumn<TreeTable.Node, String> nameColumn;
    private TreeTableColumn<TreeTable.Node, TreeTable.Node> valueColumn;
    private TreeTableColumn<TreeTable.Node, String> textColumn;

    private final ObjectProperty<Predicate<TreeTable.Node>> createTreeItemForNodeProperty = new SimpleObjectProperty<>(HIDE_EMPTY_LEAF);

    /**
     * A property containing predicate that returns true if the given
     * {@link TreeTable.Node} must be shown as a {@link TreeItem} in the
     * {@link TreeTableView}. By default return true for all nodes unless
     * preferences are loaded
     *
     * @return The property containing the expansion predicate
     */
    private ObjectProperty<Predicate<TreeTable.Node>> createTreeItemForNodeProperty() {
        return createTreeItemForNodeProperty;
    }

    private Predicate<TreeTable.Node> getCreateTreeItemForNode() {
        return createTreeItemForNodeProperty.get();
    }

    private void setCreateTreeItemForNode(Predicate<TreeTable.Node> createTreeItemForNode) {
        createTreeItemForNodeProperty.set(createTreeItemForNode);
    }

    private final SimpleObjectProperty<Predicate<TreeTable.Node>> expandNodeProperty = new SimpleObjectProperty<>(EXPAND_SINGLE_CHILD);

    /**
     * A property containing predicate that returns true if the given
     * {@link TreeTable.Node} must be expanded in the {@link TreeTableView} to
     * show its children by default.
     *
     * @return The property containing the expansion predicate
     */
    private ObjectProperty<Predicate<TreeTable.Node>> expandNodeProperty() {
        return expandNodeProperty;
    }

    private Predicate<TreeTable.Node> getExpandNode() {
        return expandNodeProperty.get();
    }

    private void setExpandNode(Predicate<TreeTable.Node> expandNode) {
        expandNodeProperty.set(expandNode);
    }

    private void expandNodes(TreeItem<TreeTable.Node> root) {
        if (root == null || root.isLeaf()) {
            return;
        }
        root.setExpanded(getExpandNode().test(root.getValue()));
        for (TreeItem<TreeTable.Node> child : root.getChildren()) {
            expandNodes(child);
        }
    }

    ObjectProperty<Comparator<TreeTable.Node>> order = new SimpleObjectProperty<>(Comparator.comparingInt(n -> 0));

    /**
     * the default order of sorting for rows of the tree table.
     *
     * @return
     */
    private ObjectProperty<Comparator<TreeTable.Node>> orderProperty() {
        return order;
    }

    private void setOrder(Comparator c) {
        order.set(c);
    }

    private Comparator<TreeTable.Node> getOrder() {
        return order.get();
    }

    MetadataTree(TreeTable treeTable) {
        this();
        setTreeTable(treeTable);
    }

    private MetadataTree() {
        createTableColumns();
        setShowRoot(false);
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        expandNodeProperty.addListener(ob -> expandNodes(getRoot()));
    }

    /**
     * Creates {@link TableColumn}s for name, identifier, value valueastext, but
     * doesnt add them to the table. These columns can later be added according
     * what all is contained by the TreeTable.
     */
    private void createTableColumns() {
        //NAME column
        nameColumn = new TreeTableColumn<>(TableColumn.NAME.getHeader().toString());
        nameColumn.setCellValueFactory((param) -> {
            CharSequence value = param.getValue().getValue().getValue(TableColumn.NAME);
            return new SimpleStringProperty(Objects.toString(value, ""));
        });

        //TEXT column
        textColumn = new TreeTableColumn<>(TableColumn.VALUE_AS_TEXT.getHeader().toString());
        textColumn.setCellValueFactory((param) -> {
            CharSequence value = param.getValue().getValue().getValue(TableColumn.VALUE_AS_TEXT);
            return new SimpleStringProperty(Objects.toString(value, ""));
        });

        //VALUE column
        valueColumn = new TreeTableColumn<>(TableColumn.VALUE.getHeader().toString());
        valueColumn.setCellValueFactory((param) -> {
            TreeTable.Node value = param.getValue().getValue();
            SimpleObjectProperty ret = new SimpleObjectProperty(value.getValue(TableColumn.VALUE));
            if (value.getValue(TableColumn.NAME).toString().equals("Reference system info")) {
                ReferenceSystem newVal = (ReferenceSystem) ret.getValue();
                ret.setValue(newVal.getName());
            }
            return ret;
        });
    }

    private TreeTable getTreeTable() {
        return treeTable;
    }

    private void setTreeTable(TreeTable treeTable) {
        this.treeTable = treeTable;
        if (treeTable == null) {
            setRoot(null);
            return;
        }

        List<TableColumn<?>> columns = treeTable.getColumns();
        if (columns.contains(TableColumn.NAME)) {
            if (nameColumn.getTreeTableView() == null) {
                getColumns().add(nameColumn);
            }
        } else {
            getColumns().remove(nameColumn);
        }
        if (columns.contains(TableColumn.VALUE)) {
            if (valueColumn.getTreeTableView() == null) {
                getColumns().add(valueColumn);
            }
        } else {
            getColumns().remove(valueColumn);
        }
        if (columns.contains(TableColumn.VALUE_AS_TEXT)) {
            if (textColumn.getTreeTableView() == null) {
                getColumns().add(textColumn);
            }
        } else {
            getColumns().remove(textColumn);
        }
        TreeItem<TreeTable.Node> rootItem = new TreeItem<>(treeTable.getRoot());
        setRoot(rootItem);
        rootItem.setExpanded(true);
        updateRoot();
    }

    private void updateRoot() {
        getRoot().getChildren().clear();
        createTreeItems(getRoot(), treeTable.getRoot().getChildren());
    }

    private void createTreeItems(TreeItem<TreeTable.Node> rootItem, Collection<TreeTable.Node> children) {
        for (TreeTable.Node node : children) {
            TreeItem parent = rootItem;
            //include this node in the tree table view?
            if (getCreateTreeItemForNode().test(node)) {
                parent = new TreeItem(node);
                parent.setExpanded(getExpandNode().test(node));
                rootItem.getChildren().add(parent);
            }
            if (!node.isLeaf()) {
                createTreeItems(parent, node.getChildren());
            }
            if (parent.getChildren().size() > 1) {
                Comparator<TreeItem<TreeTable.Node>> cp = Comparator.comparing(TreeItem<TreeTable.Node>::getValue, getOrder());
                parent.getChildren().sort(cp);
            }
        }
    }
}
