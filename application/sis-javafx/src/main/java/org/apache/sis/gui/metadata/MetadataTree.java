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

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import org.opengis.metadata.Metadata;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;


/**
 * A view of {@link Metadata} properties organized as a tree table.
 * The content of each row in this tree table is represented by a {@link TreeTable.Node}.
 * The tree table shows the following columns:
 *
 * <ul>
 *   <li>{@link TableColumn#NAME}  — a name for the metadata property, e.g. "Title".</li>
 *   <li>{@link TableColumn#VALUE} — the property value typically as a string, number or date.</li>
 * </ul>
 *
 * <p>While this view is designed mostly for metadata, it can actually be used
 * for other kinds of data provided by the {@link TreeTable} interface.</p>
 *
 * @author  Siddhesh Rane (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DefaultProperty("content")
public class MetadataTree extends TreeTableView<TreeTable.Node> {
    /**
     * The column for metadata property name.
     */
    private final TreeTableColumn<TreeTable.Node, String> nameColumn;

    /**
     * The column for metadata property value.
     * Values are typically {@link InternationalString}, {@link Number} or dates.
     */
    private final TreeTableColumn<TreeTable.Node, Object> valueColumn;

    /**
     * The data shown in this tree table. The {@link ObjectProperty#set(Object)} method requires
     * that the given value obey to the constraints documented in {@link #setContent(TreeTable)}.
     *
     * @see #getContent()
     * @see #setContent(TreeTable)
     */
    public final ObjectProperty<TreeTable> contentProperty;

    /**
     * Implementation of {@link MetadataTree#contentProperty} as a named class for more readable stack trace.
     * This class verifies the constraints documented in {@link MetadataTree#setContent(TreeTable)}.
     */
    private static final class ContentProperty extends SimpleObjectProperty<TreeTable> {
        /** Creates a new property. */
        ContentProperty(final MetadataTree bean) {
            super(bean, "content");
        }

        /** Invoked when the user wants to set new data. */
        @Override public void set(final TreeTable data) {
            if (data != null) {
                final List<TableColumn<?>> columns = data.getColumns();
                if (!(columns.contains(TableColumn.NAME) && columns.contains(TableColumn.VALUE))) {
                    throw new IllegalArgumentException();
                }
            }
            super.set(data);
        }
    }

    /**
     * The locale to use for texts.
     */
    private final Locale textLocale;

    /**
     * The locale to use for dates/numbers.
     * This is often the same than {@link #textLocale}.
     */
    private final Locale dataLocale;

    /**
     * Creates a new initially empty metadata tree.
     */
    public MetadataTree() {
        textLocale      = Locale.getDefault(Locale.Category.DISPLAY);
        dataLocale      = Locale.getDefault(Locale.Category.FORMAT);
        contentProperty = new ContentProperty(this);
        nameColumn      = new TreeTableColumn<>(TableColumn.NAME .getHeader().toString(textLocale));
        valueColumn     = new TreeTableColumn<>(TableColumn.VALUE.getHeader().toString(textLocale));
        nameColumn .setCellValueFactory(MetadataTree::getPropertyName);
        valueColumn.setCellValueFactory(MetadataTree::getPropertyValue);

        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        getColumns().setAll(nameColumn, valueColumn);
        contentProperty.addListener(MetadataTree::applyChange);
    }

    /**
     * Sets the metadata to show in this tree table. This method gets a {@link TreeTable} view
     * of the given metadata, then delegates to {@link #setContent(TreeTable)}.
     *
     * @param  metadata  the metadata to show in this tree table view, or {@code null} if none.
     */
    public void setContent(final Metadata metadata) {
        TreeTable content = null;
        if (metadata != null) {
            if (metadata instanceof AbstractMetadata) {
                content = ((AbstractMetadata) metadata).asTreeTable();
            } else {
                content = MetadataStandard.ISO_19115.asTreeTable(metadata, null, ValueExistencePolicy.COMPACT);
            }
        }
        setContent(content);
    }

    /**
     * Sets the data to show.
     * This is a convenience method for setting {@link #contentProperty} value.
     * The given {@link TreeTable} shall contain at least the following columns:
     *
     * <ul>
     *   <li>{@link TableColumn#NAME}</li>
     *   <li>{@link TableColumn#VALUE}</li>
     * </ul>
     *
     * @param  data  the data to show, or {@code null} if none.
     * @throws IllegalArgumentException if the data is non-null but does not contains the required columns.
     */
    public final void setContent(final TreeTable data) {
        contentProperty.setValue(data);
    }

    /**
     * Returns the data currently shown, or {@code null} if none.
     * This is a convenience method for fetching {@link #contentProperty} value.
     *
     * @return the table currently shown, or {@code null} if none.
     *
     * @see #contentProperty
     * @see #setContent(TreeTable)
     */
    public final TreeTable getContent() {
        return contentProperty.getValue();
    }

    /**
     * Invoked when {@link #contentProperty} value changed.
     *
     * @param  property  the property which has been modified.
     * @param  oldValue  the old tree table.
     * @param  content   the tree table to use for building new content.
     */
    private static void applyChange(final ObservableValue<? extends TreeTable> property,
                                    final TreeTable oldValue, final TreeTable  content)
    {
        final MetadataTree s = (MetadataTree) ((ContentProperty) property).getBean();
        TreeItem<TreeTable.Node> root = null;
        if (content != null) {
            root = new Item(content.getRoot());
            root.setExpanded(true);
        }
        s.setRoot(root);
    }

    /**
     * A simple node encapsulating a {@link TreeTable.Node} in a view.
     * The list of children is fetched when first needed.
     */
    private static final class Item extends TreeItem<TreeTable.Node> {
        /**
         * +1 if this item is a leaf, +2 if it has children, or 0 if not yet determined.
         */
        private byte isLeaf;

        /**
         * Creates a new node.
         */
        Item(final TreeTable.Node node) {
            super(node);
        }

        /**
         * Returns whether the node can not have children.
         */
        @Override
        public boolean isLeaf() {
            if (isLeaf == 0) {
                final TreeTable.Node node = getValue();
                isLeaf = (node.isLeaf() || node.getChildren().isEmpty()) ? (byte) 1 : 2;
            }
            return isLeaf == 1;
        }

        /**
         * Returns the items for all sub-nodes contained in this node.
         */
        @Override
        public ObservableList<TreeItem<TreeTable.Node>> getChildren() {
            final ObservableList<TreeItem<TreeTable.Node>> children = super.getChildren();
            if (children.isEmpty()) {
                final Collection<TreeTable.Node> data = getValue().getChildren();
                final List<Item> wrappers = new ArrayList<>(data.size());
                for (final TreeTable.Node child : data) {
                    wrappers.add(new Item(child));
                }
                children.setAll(wrappers);      // Fire a single event instead of multiple `add`.
            }
            return children;
        }
    }

    /**
     * Returns the value for the specified column.
     * The generic type in this method signature reduces the risk that we confuse columns.
     *
     * @param  <T>      the type of values in the column.
     * @param  cell     a wrapper around the {@link TreeTable.Node} from which to get the value.
     * @param  column   column of the desired value.
     * @return value in the specified column. May be {@code null}.
     */
    private static <T> T getValue(final CellDataFeatures<TreeTable.Node, ? extends T> cell, final TableColumn<T> column) {
        final TreeTable.Node node = cell.getValue().getValue();
        return node.getValue(column);
    }

    /**
     * Returns the name of the metadata property wrapped by the given argument.
     * This method is invoked by JavaFX when a new cell needs to be rendered.
     */
    private static ObservableValue<String> getPropertyName(final CellDataFeatures<TreeTable.Node, String> cell) {
        final CharSequence value = getValue(cell, TableColumn.NAME);
        final String text;
        if (value instanceof InternationalString) {
            final MetadataTree view = (MetadataTree) cell.getTreeTableView();
            text = ((InternationalString) value).toString(view.textLocale);
        } else {
            text = (value != null) ? value.toString() : null;
        }
        return new ReadOnlyStringWrapper(text);
    }

    /**
     * Returns the value of the metadata property wrapped by the given argument.
     * This method is invoked by JavaFX when a new cell needs to be rendered.
     */
    private static ObservableValue<Object> getPropertyValue(final CellDataFeatures<TreeTable.Node, Object> cell) {
        Object value = getValue(cell, TableColumn.VALUE);
        if (value instanceof IdentifiedObject) {
            String name = IdentifiedObjects.getName((IdentifiedObject) value, null);
            if (name == null) {
                name = IdentifiedObjects.getIdentifierOrName((IdentifiedObject) value);
            }
            value = name;
        }
        if (value instanceof InternationalString) {
            final MetadataTree view = (MetadataTree) cell.getTreeTableView();
            value = ((InternationalString) value).toString(view.textLocale);
        }
        return new ReadOnlyObjectWrapper<>(value);
    }
}
