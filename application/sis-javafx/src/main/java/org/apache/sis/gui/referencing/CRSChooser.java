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

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.gui.BackgroundThreads;
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
public class CRSChooser {
    /**
     * The preferred locale of CRS descriptions.
     */
    private final Locale locale;

    /**
     * The pane where the controls for this CRS chooser will be put.
     */
    final BorderPane content;

    /**
     * The text field where user can enter a fragment of the name of the CRS (s)he is looking for.
     */
    private final TextField searchField;

    /**
     * The table showing CRS names together with their codes.
     */
    private final TableView<Code> table;

    /**
     * Creates chooser proposing all coordinate reference systems from the given factory.
     *
     * @param  factory  the factory to use for creating coordinate reference systems, or {@code null}
     *                  for the {@linkplain CRS#getAuthorityFactory(String) Apache SIS default factory}.
     */
    public CRSChooser(final CRSAuthorityFactory factory) {
        locale = Locale.getDefault();
        table  = new TableView<>();
        /*
         * Loading of all CRS codes may take about one second.
         * Following put an animation will the CRS are loading.
         *
         * TODO: use deferred loading instead.
         */
        final ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxWidth(60);
        loading.setMaxHeight(60);
        loading.setProgress(-1);
        table.setPlaceholder(loading);

        final Vocabulary vocabulary = Vocabulary.getResources(locale);
        final TableColumn<Code,String> names = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Name));
        final TableColumn<Code,String> codes = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Code));
        codes.setPrefWidth(150);
        codes.setCellValueFactory((TableColumn.CellDataFeatures<Code,String> p) -> new SimpleObjectProperty<>(p.getValue().code));
        names.setCellValueFactory((TableColumn.CellDataFeatures<Code,String> p) -> new SimpleObjectProperty<>(p.getValue().name(locale)));

        table.getColumns().setAll(names, codes);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setTableMenuButtonVisible(false);

        content = new BorderPane();
        searchField = new TextField();
        searchField.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            searchCRS(searchField.getText());
        });
        content.setCenter(table);

        BackgroundThreads.execute(new Loader(factory));
    }

    private final class Loader extends Task<List<Code>> {
        private CRSAuthorityFactory factory;

        Loader(final CRSAuthorityFactory factory) {
            this.factory = factory;
        }

        @Override
        protected List<Code> call() throws Exception {
            if (factory == null) {
                factory = CRS.getAuthorityFactory(null);
            }
            final Set<String> strs = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
            final List<Code> codes = new ArrayList<>();
            for (final String code : strs) {
                codes.add(new Code(factory, code));
            }
            return codes;
        }

        @Override
        protected void succeeded() {
            table.setItems(FXCollections.observableArrayList(getValue()));
            table.setPlaceholder(new Label(""));
        }

        @Override
        protected void failed() {
            error(getException());
        }
    }

    public void searchCRS(final String searchword){
        filter(searchword);
    }

    private static void error(final Throwable e) {
        // TODO
    }

    /**
     * Display only the CRS name that contains the specified keywords. The {@code keywords}
     * argument is a space-separated list, usually provided by the user after he pressed the
     * "Search" button.
     *
     * @param keywords space-separated list of keywords to look for.
     */
    private void filter(String keywords) {
        final List<Code> allValues = table.getItems();
        List<Code> model = allValues;
        if (keywords != null) {
            keywords = keywords.toLowerCase(locale).trim();
            final String[] tokens = keywords.split("\\s+");
            if (tokens.length != 0) {
                model = new ArrayList<>();
                scan:
                for (Code code : allValues) {
                    final String name = code.toString().toLowerCase(locale);
                    for (int j=0; j<tokens.length; j++) {
                        if (!name.contains(tokens[j])) {
                            continue scan;
                        }
                    }
                    model.add(code);
                }
            }
        }
        table.getItems().setAll(model);
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
