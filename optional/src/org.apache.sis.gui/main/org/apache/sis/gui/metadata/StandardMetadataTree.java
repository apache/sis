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

import java.util.Map;
import java.io.StringWriter;
import javax.xml.transform.stream.StreamResult;
import javafx.event.ActionEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.privy.LegacyNamespaces;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.gui.internal.DataFormats;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.io.wkt.WKTFormat;


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
 * @version 1.2
 * @since   1.1
 */
public class StandardMetadataTree extends MetadataTree {
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
    @SuppressWarnings("this-escape")        // `this` appears in a cyclic graph.
    public StandardMetadataTree(final MetadataSummary controller) {
        super(controller, true);
        setRowFactory(Row::new);
        if (controller != null) {
            controller.metadataProperty.addListener((p,o,n) -> setContent(n));
        }
    }

    /**
     * Sets the metadata to show in this tree table. This method gets a {@link TreeTable} view
     * of the given metadata, then delegates to {@link #setContent(TreeTable)}.
     *
     * @param  metadata  the metadata to show in this tree table view, or {@code null} if none.
     */
    public void setContent(final Metadata metadata) {
        final TreeTable tree;
        if (metadata == null) {
            tree = null;
        } else if (metadata instanceof AbstractMetadata) {
            tree = ((AbstractMetadata) metadata).asTreeTable();
        } else {
            // `COMPACT` is the default policy of `AbstractMetadata.asTreeTable()`.
            tree = MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.COMPACT);
        }
        setContent(tree);
    }

    /**
     * A row in a metadata tree view, used for adding contextual menu on a row-by-row basis.
     */
    private static final class Row extends MetadataTree.Row {
        /**
         * The menu items for XML or WKT formats.
         */
        private final MenuItem copyAsXML, copyAsLegacy, copyAsWKT;

        /**
         * The group of menu items for copying in various formats, to be disabled if we cannot do this export.
         */
        private final Menu copyAs;

        /**
         * Creates a new row for the given tree table.
         */
        Row(final TreeTableView<TreeTable.Node> view) {
            super(view);
            final var md = (StandardMetadataTree) view;
            final Resources localized = Resources.forLocale(md.getLocale());
            copyAsXML    = new MenuItem();
            copyAsWKT    = new MenuItem("WKT — Well Known Text");
            copyAsLegacy = new MenuItem("XML — Metadata (2007)");
            copyAs       = new Menu(localized.getString(Resources.Keys.CopyAs), null, copyAsWKT, copyAsXML, copyAsLegacy);
            menu        .getItems().add(copyAs);
            copyAsLegacy.setOnAction(this);
            copyAsXML   .setOnAction(this);
            copyAsWKT   .setOnAction(this);
        }

        /**
         * Invoked when a new row is selected. This method enables or disables the "copy as" menu
         * depending on whether or not we can format XML document for currently selected row.
         */
        @Override
        protected void updateItem(final TreeTable.Node item, final boolean empty) {
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
                if (obj == null) {
                    super.handle(event);
                } else {
                    final Object source = event.getSource();
                    final var content = new ClipboardContent();
                    final String text;
                    try {
                        if (source == copyAsWKT) {                              // Well Known Text.
                            final var f = new WKTFormat();
                            text = f.format(obj);
                        } else if (source == copyAsXML) {                       // GML or ISO 19115-3:2016.
                            text = XML.marshal(obj);
                            content.put(DataFormats.XML, text);
                        } else if (source == copyAsLegacy) {                    // ISO 19139:2007.
                            final var output = new StringWriter();
                            XML.marshal(obj, new StreamResult(output), Map.of(
                                        XML.METADATA_VERSION, LegacyNamespaces.VERSION_2007));
                            text = output.toString();
                            content.put(DataFormats.ISO_19139, text);
                        } else {
                            text = toString(obj);
                        }
                        content.putString(text);
                        Clipboard.getSystemClipboard().setContent(content);
                    } catch (Exception e) {
                        final Resources localized = Resources.forLocale(((MetadataTree) getTreeTableView()).getLocale());
                        ExceptionReporter.show(this, localized.getString(Resources.Keys.ErrorExportingData),
                                                     localized.getString(Resources.Keys.CanNotCreateXML), e);
                    }
                }
            }
        }
    }
}
