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
import java.util.function.Predicate;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.gui.IdentityValueFactory;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.CharSequences;
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
public class CRSChooser {
    /**
     * The pane where the controls for this CRS chooser will be put.
     */
    final BorderPane content;

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
     * Creates a chooser proposing all coordinate reference systems from the given factory.
     *
     * @param  factory  the factory to use for creating coordinate reference systems, or {@code null}
     *                  for the {@linkplain CRS#getAuthorityFactory(String) Apache SIS default factory}.
     */
    public CRSChooser(final CRSAuthorityFactory factory) {
        final Locale locale = Locale.getDefault();
        final AuthorityCodes codeList = new AuthorityCodes(factory, locale);
        table = new TableView<>(codeList);

        final Vocabulary vocabulary = Vocabulary.getResources(locale);
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

        searchField = new TextField();
        searchField.setOnAction((ActionEvent event) -> {
            filter(searchField.getText());
        });
        final Resources i18n = Resources.forLocale(locale);
        final Label label = new Label(i18n.getString(Resources.Keys.Filter));
        final Button info = new Button("\uD83D\uDEC8");         // Unicode U+1F6C8: Circled Information Source
        label.setLabelFor(searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        final HBox bar = new HBox(label, searchField, info);
        bar.setSpacing(9);
        bar.setAlignment(Pos.BASELINE_LEFT);
        BorderPane.setMargin(bar, new Insets(0, 0, 9, 0));

        content = new BorderPane();
        content.setCenter(table);
        content.setTop(bar);
    }

    /**
     * Displays only the CRS whose names contains the specified keywords. The {@code keywords}
     * argument is a space-separated list provided by the user after he pressed "Enter" key.
     *
     * @param  keywords  space-separated list of keywords to look for.
     */
    private void filter(String keywords) {
        final ObservableList<Code> items = table.getItems();
        final AuthorityCodes allCodes;
        FilteredList<Code> filtered;
        if (items instanceof AuthorityCodes) {
            allCodes = (AuthorityCodes) items;
            filtered = null;
        } else {
            filtered = (FilteredList<Code>) items;
            allCodes = (AuthorityCodes) filtered.getSource();
        }
        keywords = Strings.trimOrNull(keywords);
        if (keywords != null) {
            keywords = keywords.toLowerCase(allCodes.locale);
            final String[] tokens = (String[]) CharSequences.split(keywords, ' ');
            if (tokens.length != 0) {
                final Predicate<Code> p = (code) -> {
                    String name = allCodes.getName(code).getValue();
                    if (name == null) {
                        return false;
                    }
                    name = name.toLowerCase(allCodes.locale);
                    for (final String token : tokens) {
                        if (!name.contains(token)) {
                            return false;
                        }
                    }
                    return true;
                };
                if (filtered == null) {
                    filtered = new FilteredList<>(allCodes, p);
                    table.setItems(filtered);
                } else {
                    filtered.setPredicate(p);
                }
                return;
            }
        }
        table.setItems(allCodes);
    }

    /**
     * Show a modal dialog to select a {@link CoordinateReferenceSystem}.
     *
     * @param parent parent frame of widget.
     * @param crs {@link CoordinateReferenceSystem} to edit.
     * @return modified {@link CoordinateReferenceSystem}.
     */
    public static CoordinateReferenceSystem showDialog(Object parent, CoordinateReferenceSystem crs) {
        final CRSChooser chooser = new CRSChooser(null);
//        chooser.crsProperty.set(crs);
        final Alert alert = new Alert(Alert.AlertType.NONE);
        final DialogPane pane = alert.getDialogPane();
        pane.setContent(chooser.content);
        alert.getButtonTypes().setAll(ButtonType.OK,ButtonType.CANCEL);
        alert.setResizable(true);
        final ButtonType res = alert.showAndWait().orElse(ButtonType.CANCEL);
        return null;//res == ButtonType.CANCEL ? null : chooser.crsProperty.get();
    }
}
