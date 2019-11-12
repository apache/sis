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
package org.apache.sis.gui.referencing;

import java.util.Locale;
import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.Window;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.gui.IdentityValueFactory;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.CRS;


/**
 * A list of Coordinate Reference Systems (CRS) from which the user can select.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class CRSChooser extends Dialog<CoordinateReferenceSystem> {
    /**
     * The pane where the controls for this CRS chooser will be put.
     * The top part contains the tools bar. The center part contains
     * the table or the WKT and change depending on user actions.
     */
    private final BorderPane content;

    /**
     * The tools bar for this pane. Children are in this order:
     * <ul>
     *   <li>A {@link Label} for the second child.</li>
     *   <li>A text field, combo box or other control.</li>
     *   <li>An arbitrary number of buttons.</li>
     * </ul>
     */
    private final HBox tools;

    /**
     * The text field where user can enter a fragment of the name of the CRS (s)he is looking for.
     */
    private final TextField searchField;

    /**
     * The table showing CRS codes together with their names. Table items are provided by a background thread.
     * Items are initially authority codes as {@link Code} instances without {@link Code#name} value.
     * Names are completed later when needed.
     */
    private final TableView<Code> table;

    /**
     * The pane showing the CRS in Well Known Text format.
     * Created when first needed.
     */
    private WKTPane wktPane;

    /**
     * Creates a chooser proposing all coordinate reference systems from the given factory.
     *
     * @param  factory  the factory to use for creating coordinate reference systems, or {@code null}
     *                  for the {@linkplain CRS#getAuthorityFactory(String) Apache SIS default factory}.
     */
    public CRSChooser(final CRSAuthorityFactory factory) {
        final Locale         locale     = Locale.getDefault();
        final Resources      i18n       = Resources.forLocale(locale);
        final Vocabulary     vocabulary = Vocabulary.getResources(locale);
        final AuthorityCodes codeList   = new AuthorityCodes(factory, locale);
        table = new TableView<>(codeList);
        codeList.owner = table;
        /*
         * Columns to show in CRS table. First column is typically EPSG codes and second
         * column is the CRS descriptions. The content is loaded in a background thread.
         */
        final TableColumn<Code,Code>   codes = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Code));
        final TableColumn<Code,String> names = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Name));
        names.setCellValueFactory(codeList);
        codes.setCellValueFactory(IdentityValueFactory.instance());
        codes.setCellFactory(Code.Cell::new);
        codes.setMinWidth ( 60);            // Will be the initial size of this column.
        codes.setMaxWidth (120);            // Seems to be required for preventing `codes` to be as large as `names`.
        table.setPrefWidth(500);
        table.getColumns().setAll(codes, names);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        final Label clock = new Label("\u23F3");      // Unicode U+23F3: Hourglass With Flowing Sand.
        clock.setFont(Font.font(40));
        table.setPlaceholder(clock);
        /*
         * Text field for filtering the list of CRS codes using keywords.
         * The filtering is applied when the "Enter" key is pressed in that field.
         */
        searchField = new TextField();
        searchField.setOnAction((ActionEvent event) -> {
            CodeFilter.apply(table, searchField.getText());
        });
        HBox.setHgrow(searchField, Priority.ALWAYS);
        final Label label = new Label(i18n.getString(Resources.Keys.Filter));
        label.setLabelFor(searchField);
        /*
         * Button for showing the CRS description in Well Known Text (WKT) format.
         * The button is enabled only if a row in the table is selected.
         */
        final ToggleButton info = new ToggleButton("\uD83D\uDDB9"); // Unicode U+1F5B9: Document With Text.
        table.getSelectionModel().selectedItemProperty().addListener((e,o,n) -> info.setDisable(n == null));
        info.setOnAction((ActionEvent event) -> {
            setTools(info.isSelected());
        });
        info.setDisable(true);
        /*
         * Creates the tools bar to show above the table of codes.
         * The tools bar contains the search field and the button for showing the WKT.
         */
        tools = new HBox(label, searchField, info);
        tools.setSpacing(9);
        tools.setAlignment(Pos.BASELINE_LEFT);
        BorderPane.setMargin(tools, new Insets(0, 0, 9, 0));
        /*
         * Layout table and tools bar inside the dialog content.
         * Configure the dialog buttons.
         */
        final DialogPane pane = getDialogPane();
        content = new BorderPane();
        content.setCenter(table);
        content.setTop(tools);
        pane.setContent(content);
        pane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        setTitle(i18n.getString(Resources.Keys.SelectCRS));
        setResultConverter(this::getSelectedCRS);
        setResizable(true);
    }

    /**
     * Sets the tools bar and content to controls for the given mode.
     * If {@code wkt} is {@code true}, then this method set the controls for showing the WKT.
     * If {@code wkt} is {@code false} (the default), then this method set the controls to the table of CRS codes.
     */
    private void setTools(final boolean wkt) {
        final Locale locale = getAuthorityCodes().locale;
        final short labelText;
        final Control control;
        final Control main;
        if (wkt) {
            if (wktPane == null) {
                wktPane = new WKTPane(locale);
            }
            wktPane.setContent(getAuthorityCodes(), table.getSelectionModel().getSelectedItem().code);
            labelText = Resources.Keys.Format;
            control   = wktPane.convention;
            main      = wktPane.text;
        } else {
            labelText = Resources.Keys.Filter;
            control   = searchField;
            main      = table;
        }
        final ObservableList<Node> children = tools.getChildren();
        final Label label = (Label) children.get(0);
        final Resources i18n = Resources.forLocale(locale);
        label.setText(i18n.getString(labelText));
        label.setLabelFor(control);
        children.set(1, control);
        content.setCenter(main);
    }

    /**
     * Returns the list of all authority codes. The list may not be complete at the
     * time this method returns because codes are loaded in a background thread.
     */
    private AuthorityCodes getAuthorityCodes() {
        ObservableList<?> items = table.getItems();
        if (items instanceof FilteredList<?>) {
            items = ((FilteredList<?>) items).getSource();
        }
        return (AuthorityCodes) items;
    }

    /**
     * Returns the currently selected CRS, or {@code null} if none.
     *
     * @return the currently selected CRS, or {@code null}.
     */
    private CoordinateReferenceSystem getSelectedCRS(final ButtonType button) {
        if (ButtonType.OK.equals(button)) {
            final Code code = table.getSelectionModel().getSelectedItem();
            if (code != null) try {
                return getAuthorityCodes().getFactory().createCoordinateReferenceSystem(code.code);
            } catch (FactoryException e) {
                ExceptionReporter.canNotCreateCRS(code.code, e);
            }
        }
        return null;
    }

    /**
     * Shows a dialog to select a {@link CoordinateReferenceSystem}.
     *
     * @param  parent  parent frame of dialog.
     * @return the selected {@link CoordinateReferenceSystem}, or empty if none.
     */
    public Optional<CoordinateReferenceSystem> showDialog(final Window parent) {
        initOwner(parent);
        return showAndWait();
    }
}
