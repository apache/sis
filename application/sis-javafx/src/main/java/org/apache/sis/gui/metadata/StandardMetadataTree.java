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

import java.util.Collections;
import java.io.StringWriter;
import javax.xml.transform.stream.StreamResult;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.DataFormats;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.xml.XML;


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
 * This class still supports the {@link #setContent(TreeTable)} method,
 * but {@link #setContent(Metadata)} should be used instead.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The {@link #rootProperty() rootProperty} should be considered read-only.
 *       For changing content, use the {@link #contentProperty} instead.</li>
 * </ul>
 *
 * @todo Add contextual menu for showing a node in the summary pane (we would store in memory the path,
 *       including sequence number for multi-values property, and apply it to all opened resources).
 *
 * @author  Siddhesh Rane (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class StandardMetadataTree extends MetadataTree {
    /**
     * The "copy" and "copy as" localized string, used for contextual menus.
     */
    private final String copy, copyAs;

    /**
     * Creates a new initially empty metadata tree.
     */
    public StandardMetadataTree() {
        this(null);
    }

    /**
     * Creates a new initially empty metadata tree which will be automatically updated
     * when the given widget shows new metadata. This constructor registers a listener
     * to {@link MetadataSummary#metadataProperty} which forwards the metadata changes
     * to {@link #setContent(Metadata)}.
     *
     * @param  controller  the widget to watch, or {@code null} if none.
     */
    public StandardMetadataTree(final MetadataSummary controller) {
        super(controller, true);
        final Resources localized = Resources.forLocale(getLocale());
        copy   = localized.getString(Resources.Keys.Copy);
        copyAs = localized.getString(Resources.Keys.CopyAs);
        setRowFactory(Row::new);
        if (controller != null) {
            controller.metadataProperty.addListener((p,o,n) -> setContent(n));
        }
    }

    /**
     * Returns the given metadata as a tree table.
     */
    private static TreeTable toTree(final Object metadata) {
        if (metadata instanceof AbstractMetadata) {
            return ((AbstractMetadata) metadata).asTreeTable();
        } else {
            return MetadataStandard.ISO_19115.asTreeTable(metadata, null, ValueExistencePolicy.COMPACT);
        }
    }

    /**
     * Sets the metadata to show in this tree table. This method gets a {@link TreeTable} view
     * of the given metadata, then delegates to {@link #setContent(TreeTable)}.
     *
     * @param  metadata  the metadata to show in this tree table view, or {@code null} if none.
     */
    public void setContent(final Metadata metadata) {
        setContent(metadata == null ? null : toTree(metadata));
    }

    /**
     * A row in a metadata tree view, used for adding contextual menu on a row-by-row basis.
     */
    private static final class Row extends TreeTableRow<TreeTable.Node> implements EventHandler<ActionEvent> {
        /**
         * The context menu, to be added only if this row is non-empty.
         */
        private final ContextMenu menu;

        /**
         * The menu items for XML or WKT formats.
         */
        private final MenuItem copyAsXML, copyAsLegacy, copyAsWKT;

        /**
         * The menu items for copying in XML formats, to be disabled if we can not do this export.
         */
        private final Menu copyAs;

        /**
         * Creates a new row for the given tree table.
         */
        @SuppressWarnings("ThisEscapedInObjectConstruction")
        Row(final TreeTableView<TreeTable.Node> view) {
            final StandardMetadataTree md = (StandardMetadataTree) view;
            final MenuItem copy;
            copy         = new MenuItem(md.copy);
            copyAsXML    = new MenuItem();
            copyAsWKT    = new MenuItem("WKT — Well Known Text");
            copyAsLegacy = new MenuItem("XML — Metadata (2007)");
            copyAs       = new Menu(md.copyAs, null, copyAsWKT, copyAsXML, copyAsLegacy);
            menu         = new ContextMenu(copy, copyAs);
            copyAsLegacy.setOnAction(this);
            copyAsXML   .setOnAction(this);
            copy        .setOnAction(this);
        }

        /**
         * Invoked when a new row is selected. This method enable or disable the "copy as" menu
         * depending on whether or not we can format XML document for currently selected row.
         */
        @Override
        protected void updateItem​(final TreeTable.Node item, final boolean empty) {
            super.updateItem(item, empty);
            if (!empty && copyAs != null) {
                boolean disabled = true;
                final TreeTable.Node node = getItem();
                if (node != null) {
                    final Object obj = node.getUserObject();
                    if (obj != null) {
                        if (MetadataStandard.ISO_19115.isMetadata(obj.getClass())) {
                            copyAsXML.setText("XML — Metadata (2016)");
                            copyAsWKT.setDisable(true);
                            copyAsLegacy.setDisable(false);
                            disabled = false;
                        } else if (obj instanceof IdentifiedObject) {
                            copyAsXML.setText("XML — Geographic Markup Language");
                            copyAsWKT.setDisable(false);
                            copyAsLegacy.setDisable(true);
                            disabled = false;
                        }
                    }
                }
                copyAs.setDisable(disabled);
            }
            setContextMenu(empty ? null : menu);
        }

        /**
         * Invoked when user requested to copy metadata. The requested format (ISO 19115 versus ISO 19139)
         * will be determined by comparing the event source with {@link #copyAsLegacy} and {@link #copyAsXML}
         * menu items.
         */
        @Override
        public void handle(final ActionEvent event) {
            final TreeTable.Node node = getItem();
            if (node != null) {
                final Object obj = node.getUserObject();
                if (obj != null) {
                    final Object source = event.getSource();
                    final ClipboardContent content = new ClipboardContent();
                    final String text;
                    try {
                        if (source == copyAsWKT) {                              // Well Known Text.
                            final WKTFormat f = new WKTFormat(null, null);
                            text = f.format(obj);
                        } else if (source == copyAsXML) {                       // GML or ISO 19115-3:2016.
                            text = XML.marshal(obj);
                            content.put(DataFormats.XML, text);
                        } else if (source == copyAsLegacy) {                    // ISO 19139:2007.
                            final StringWriter output = new StringWriter();
                            XML.marshal(obj, new StreamResult(output),
                                    Collections.singletonMap(XML.METADATA_VERSION, LegacyNamespaces.VERSION_2007));
                            text = output.toString();
                            content.put(DataFormats.ISO_19139, text);
                        } else if (MetadataStandard.ISO_19115.isMetadata(obj.getClass())) {
                            text = toTree(obj).toString();
                        } else {
                            final Object value = node.getValue(((MetadataTree) getTreeTableView()).valueSourceColumn);
                            if (value == null) return;
                            text = value.toString();
                        }
                    } catch (Exception e) {
                        final Resources localized = Resources.forLocale(((MetadataTree) getTreeTableView()).getLocale());
                        ExceptionReporter.show(this, localized.getString(Resources.Keys.ErrorExportingData),
                                                     localized.getString(Resources.Keys.CanNotCreateXML), e);
                        return;
                    }
                    content.putString(text);
                    Clipboard.getSystemClipboard().setContent(content);
                }
            }
        }
    }
}
