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
import java.io.IOException;
import javafx.util.Callback;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.internal.PropertyView;
import org.apache.sis.gui.internal.PropertyValueFormatter;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A view of metadata represented by a {@link TreeTable}. This base class can represent
 * {@linkplain org.apache.sis.storage.DataStore#getNativeMetadata() native metadata} or
 * {@linkplain org.apache.sis.storage.DataStore#getMetadata() standard metadata}.
 * The tree table shows the following columns:
 *
 * <ul>
 *   <li>{@link TableColumn#NAME}  — a name for the metadata property, e.g. "Title".</li>
 *   <li>{@link TableColumn#VALUE} — the property value typically as a string, number or date.</li>
 * </ul>
 *
 * In the particular case of metadata from ISO 19115 standard,
 * the {@link StandardMetadataTree} specialization should be used instead of this base class.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The {@link #rootProperty() rootProperty} should be considered read-only.
 *       For changing content, use the {@link #contentProperty} instead.</li>
 * </ul>
 *
 * @todo Add menu items for controlling the number/date/angle format pattern.
 *
 * @author  Siddhesh Rane (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
@DefaultProperty("content")
public class MetadataTree extends TreeTableView<TreeTable.Node> {
    /**
     * The column for metadata property name in the view.
     */
    private final TreeTableColumn<TreeTable.Node, String> nameColumn;

    /**
     * The column for metadata property value in the view.
     * Values are typically {@link InternationalString}, {@link Number} or dates.
     */
    private final TreeTableColumn<TreeTable.Node, Object> valueColumn;

    /**
     * The column for metadata property value in the model.
     * This is usually {@link TableColumn#VALUE} or {@link TableColumn#VALUE_AS_TEXT}.
     */
    private TableColumn<?> valueSourceColumn;

    /**
     * The data shown in this tree table. The {@link ObjectProperty#set(Object)} method requires
     * that the given value obey to the constraints documented in {@link #setContent(TreeTable)}.
     *
     * @see #getContent()
     * @see #setContent(TreeTable)
     */
    public final ObjectProperty<TreeTable> contentProperty;

    /**
     * Content of a dialog showing the value of selected row.
     *
     * @see #showPropertyValue(Object)
     */
    private PropertyView propertyViewer;

    /**
     * The dialog showing the property value. We keep the instance for preserving
     * its size and position if the user modifies them.
     */
    private Dialog<Void> propertyDialog;

    /**
     * Implementation of {@link MetadataTree#contentProperty} as a named class for more readable stack trace.
     * This class verifies the constraints documented in {@link MetadataTree#setContent(TreeTable)}.
     */
    private static final class ContentProperty extends SimpleObjectProperty<TreeTable> {
        /**
         * The columns where to search for values, in preference order.
         */
        private static final TableColumn<?>[] VALUES_COLUMNS = {
            TableColumn.VALUE,
            TableColumn.VALUE_AS_NUMBER,
            TableColumn.VALUE_AS_TEXT
        };

        /** Creates a new property. */
        ContentProperty(final MetadataTree bean) {
            super(bean, "content");
        }

        /** Invoked when the user wants to set new data. */
        @Override public void set(final TreeTable data) {
check:      if (data != null) {
                final List<TableColumn<?>> columns = data.getColumns();
                if (columns.contains(TableColumn.NAME)) {
                    for (final TableColumn<?> value : VALUES_COLUMNS) {
                        if (columns.contains(value)) {
                            ((MetadataTree) getBean()).valueSourceColumn = value;
                            break check;
                        }
                    }
                }
                throw new IllegalArgumentException();
            }
            super.set(data);
        }
    }

    /**
     * The object to use for formatting property values.
     */
    private final Formatter formatter;

    /**
     * Creates a new initially empty metadata tree.
     */
    public MetadataTree() {
        this(null);
    }

    /**
     * Creates a new initially empty metadata tree which will be automatically updated
     * when the given widget is given a {@linkplain org.apache.sis.storage.DataStore}.
     *
     * @param  controller  the widget to watch, or {@code null} if none.
     */
    @SuppressWarnings("this-escape")
    public MetadataTree(final MetadataSummary controller) {
        this(controller, false);
        setRowFactory(Row::new);
        setShowRoot(false);
    }

    /**
     * For {@link MetadataTree} and {@link StandardMetadataTree} constructors.
     *
     * @param  controller  the widget to watch, or {@code null} if none.
     * @param  standard    {@code true} for showing standard metadata, or {@code false} for native metadata.
     */
    @SuppressWarnings({"this-escape", "unchecked"})
    MetadataTree(final MetadataSummary controller, final boolean standard) {
        final Vocabulary vocabulary;
        if (controller != null) {
            vocabulary = controller.vocabulary;
        } else {
            vocabulary = Vocabulary.forLocale(null);
        }
        formatter       = new Formatter(vocabulary.getLocale());
        contentProperty = new ContentProperty(this);
        nameColumn      = new TreeTableColumn<>(vocabulary.getString(Vocabulary.Keys.Property));
        valueColumn     = new TreeTableColumn<>(vocabulary.getString(Vocabulary.Keys.Value));
        nameColumn .setCellValueFactory(MetadataTree::getPropertyName);
        valueColumn.setCellValueFactory(formatter);

        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().setAll(nameColumn, valueColumn);
        contentProperty.addListener(MetadataTree::applyChange);
    }

    /**
     * The locale to use for texts. This is usually {@link Locale#getDefault()}.
     * This value is given to {@link InternationalString#toString(Locale)} calls.
     */
    final Locale getLocale() {
        return formatter.getLocale();
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
     * This method invokes {@link TreeTable#getRoot()} and
     * wraps the value as the root node of this control.
     *
     * @param  property  the property which has been modified.
     * @param  oldValue  the old tree table.
     * @param  newValue  the tree table to use for building new content.
     */
    private static void applyChange(final ObservableValue<? extends TreeTable> property,
                                    final TreeTable oldValue, final TreeTable  newValue)
    {
        final var s = (MetadataTree) ((ContentProperty) property).getBean();
        if (s.propertyViewer != null) {
            s.propertyViewer.clear();
        }
        TreeItem<TreeTable.Node> root = null;
        if (newValue != null) {
            root = new Item(newValue.getRoot());
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
         * Whether this node is a leaf.
         */
        private final boolean isLeaf;

        /**
         * Creates a new node.
         */
        Item(final TreeTable.Node node) {
            super(node);
            isLeaf = node.isLeaf() || node.getChildren().isEmpty();
        }

        /**
         * Returns whether the node cannot have children.
         */
        @Override
        public boolean isLeaf() {
            return isLeaf;
        }

        /**
         * Returns the items for all sub-nodes contained in this node.
         */
        @Override
        public ObservableList<TreeItem<TreeTable.Node>> getChildren() {
            final var children = super.getChildren();
            if (children.isEmpty()) {
                // Fire a single event instead of multiple `add`.
                children.setAll(getValue().getChildren().stream().map(Item::new).toList());
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
    private static <T> T getValue(final CellDataFeatures<TreeTable.Node, ?> cell, final TableColumn<T> column) {
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
            final var view = (MetadataTree) cell.getTreeTableView();
            text = ((InternationalString) value).toString(view.getLocale());
        } else {
            text = (value != null) ? value.toString() : null;
        }
        return new ReadOnlyStringWrapper(text);
    }

    /**
     * Formatter for metadata property value in a tree cell. This formatter handles in a special way
     * many object classes like {@link InternationalString}, <i>etc</i>.
     */
    private static final class Formatter extends PropertyValueFormatter
            implements Callback<CellDataFeatures<TreeTable.Node, Object>, ObservableValue<Object>>
    {
        /**
         * Creates a new formatter for the given locale.
         */
        Formatter(final Locale locale) {
            super(new StringBuilder(), locale);
        }

        /**
         * Returns the value of the metadata property wrapped by the given argument.
         * This method is invoked by JavaFX when a new cell needs to be rendered.
         */
        @Override
        public ObservableValue<Object> call(final CellDataFeatures<TreeTable.Node, Object> cell) {
            final var view = (MetadataTree) cell.getTreeTableView();
            Object value = getValue(cell, view.valueSourceColumn);
            if (value instanceof IdentifiedObject) {
                value = IdentifiedObjects.getDisplayName((IdentifiedObject) value, getLocale());
            }
            try {
                clear();
                final var buffer = (StringBuilder) out;
                buffer.setLength(0);
                appendValue(value);
                flush();
                value = buffer.toString();
            } catch (IOException e) {               // Should never happen because we append in a StringBuilder.
                throw new AssertionError(e);
            }
            return new ReadOnlyObjectWrapper<>(value);
        }
    }

    /**
     * A row in a metadata tree view, used for adding contextual menu on a row-by-row basis.
     */
    static class Row extends TreeTableRow<TreeTable.Node> implements EventHandler<ActionEvent> {
        /**
         * The context menu, to be added only if this row is non-empty.
         */
        protected final ContextMenu menu;

        /**
         * The menu item for copying current row or viewing in a dialog box.
         */
        private final MenuItem view, copy;

        /**
         * Creates a new row for the given tree table.
         */
        Row(final TreeTableView<TreeTable.Node> owner) {
            final var md = (MetadataTree) owner;
            final Resources localized = Resources.forLocale(md.getLocale());
            view = new MenuItem(localized.getString(Resources.Keys.View));
            copy = new MenuItem(localized.getString(Resources.Keys.Copy));
            menu = new ContextMenu(view, copy);
            copy.setOnAction(this);
            view.setOnAction((h) -> ((MetadataTree) getTreeTableView()).showPropertyValue(getValue()));
        }

        /**
         * Invoked when a new row is selected.
         * This method sets the contextual menu on the row and updates the disabled state.
         */
        @Override
        protected void updateItem(final TreeTable.Node item, final boolean empty) {
            super.updateItem(item, empty);
            setContextMenu(empty ? null : menu);
            final boolean disabled = empty || getValue() == null;
            view.setDisable(disabled);
            copy.setDisable(disabled);
        }

        /**
         * Returns the object in the "value" column of current row, or {@code null} if none.
         */
        private Object getValue() {
            final TreeTable.Node node = getItem();
            if (node != null) {
                final Object obj = node.getUserObject();
                return (obj != null) ? obj : node.getValue(((MetadataTree) getTreeTableView()).valueSourceColumn);
            }
            return null;
        }

        /**
         * Invoked when user selected a "Copy" or "Copy as" menu item.
         * The default implementation handles the "Copy" action,
         * but subclasses can override for handling other copy variants.
         */
        @Override
        public void handle(final ActionEvent event) {
            final Object value = getValue();
            if (value != null) {
                final ClipboardContent content = new ClipboardContent();
                content.putString(toString(value));
                Clipboard.getSystemClipboard().setContent(content);
            }
        }

        /**
         * Returns a string representation of the given object
         * for the purpose of a "copy to clipboard" operation.
         */
        static String toString(final Object obj) {
            if (obj instanceof Throwable) {
                return ExceptionReporter.getStackTrace((Throwable) obj);
            }
            return obj.toString();
        }
    }

    /**
     * Shows the given value in a dialog box.
     */
    private void showPropertyValue(final Object value) {
        if (propertyViewer == null) {
            propertyViewer = new PropertyView(formatter);
            propertyDialog = new Dialog<>();
            propertyDialog.setResizable(true);
            propertyDialog.setTitle(Resources.forLocale(getLocale()).getString(Resources.Keys.PropertyValue));
            propertyDialog.initOwner(getScene().getWindow());
            final DialogPane pane = propertyDialog.getDialogPane();
            pane.contentProperty().bind(propertyViewer.view);
            pane.getButtonTypes().add(ButtonType.CLOSE);
        }
        propertyViewer.set(value, null);
        propertyDialog.show();
    }
}
