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

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.util.Callback;
import org.apache.sis.internal.gui.FontGlyphs;
import org.apache.sis.internal.gui.FXUtilities;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.resources.Vocabulary;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.ConicProjection;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.operation.PlanarProjection;
import org.opengis.referencing.operation.Projection;
import org.opengis.util.FactoryException;

/**
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class CRSTable extends ScrollPane {

    private static final Color COLOR = new Color(30, 150, 250);
    private static final Image ICON_GEO, ICON_SQUARE, ICON_STEREO, ICON_UTM, ICON_CONIC;
    private static final Image ICON_UNKNOWN = FontGlyphs.createImage("\uE22F",16,COLOR);
    static {
        final Class<?> c = CRSTable.class;
        final Dimension dim = new Dimension(16, 16);
        try {
            ICON_GEO    = FXUtilities.getImage(c, "proj_geo.png",    dim);
            ICON_SQUARE = FXUtilities.getImage(c, "proj_square.png", dim);
            ICON_STEREO = FXUtilities.getImage(c, "proj_stereo.png", dim);
            ICON_UTM    = FXUtilities.getImage(c, "proj_utm.png",    dim);
            ICON_CONIC  = FXUtilities.getImage(c, "proj_conic.png",  dim);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final ObjectProperty<CoordinateReferenceSystem> crsProperty = new SimpleObjectProperty<>();
    private final TableView<Code> uiTable = new TableView<>();

    private List<Code> allValues;

    public CRSTable(){
        uiTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setContent(uiTable);
        setFitToHeight(true);
        setFitToWidth(true);

        //add a loader while we load datas
        final ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxWidth(60);
        loading.setMaxHeight(60);
        loading.setBackground(new Background(new BackgroundFill(new javafx.scene.paint.Color(0, 0, 0, 0), CornerRadii.EMPTY, Insets.EMPTY)));
        loading.setProgress(-1);
        uiTable.setPlaceholder(loading);
        uiTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        uiTable.setTableMenuButtonVisible(false);

        uiTable.getSelectionModel().getSelectedCells().addListener(new ListChangeListener<TablePosition>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TablePosition> c) {
                final ObservableList<TablePosition> cells = uiTable.getSelectionModel().getSelectedCells();
                if (!cells.isEmpty()) {
                    final TablePosition cell = cells.get(0);
                    final Code code = uiTable.getItems().get(cell.getRow());
                    try {
                        crsProperty.set((CoordinateReferenceSystem) code.createObject());
                    } catch (FactoryException ex) {
                        error(ex);
                    }
                }
            }
        });

        uiTable.getColumns().add(new TypeColumn());
        uiTable.getColumns().add(new CodeColumn());
        uiTable.getColumns().add(new DescColumn());
        uiTable.getColumns().add(new WKTColumn());


        //load list
        new Thread(){
            @Override
            public void run() {
                try {
                    allValues = getCodes();
                    Platform.runLater(() -> {
                        uiTable.setItems(FXCollections.observableArrayList(allValues));
                        uiTable.setPlaceholder(new Label(""));
                    });
                } catch (FactoryException ex) {
                    error(ex);
                }
            }
        }.start();
    }

    public ObjectProperty<CoordinateReferenceSystem> crsProperty(){
        return crsProperty;
    }

    public void searchCRS(final String searchword){
        filter(searchword);
    }

    private static void error(final Exception e) {
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
        List<Code> model = allValues;
        if (keywords != null) {
            final Locale locale = Locale.getDefault();
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
        uiTable.getItems().setAll(model);
    }

    /**
     * Returns a collection containing only the factories of the specified authority.
     */
    private static Collection<CRSAuthorityFactory> filter(
            final Collection<? extends CRSAuthorityFactory> factories, final String authority){
        final List<CRSAuthorityFactory> filtered = new ArrayList<>();
        for (final CRSAuthorityFactory factory : factories) {
            if (Citations.identifierMatches(factory.getAuthority(), authority)) {
                filtered.add(factory);
            }
        }
        return filtered;
    }

    private List<Code> getCodes() throws FactoryException{
        final CRSAuthorityFactory factory = org.apache.sis.referencing.CRS.getAuthorityFactory(null);
        final Set<String> strs = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
        final List<Code> codes = new ArrayList<>();
        for (String str : strs) {
            codes.add(new Code(factory, str));
        }
        return codes;
    }

    static Image getIcon(IdentifiedObject obj) {
        Image icon = ICON_UNKNOWN;
        if (obj instanceof GeographicCRS) {
            icon = ICON_GEO;
        } else if (obj instanceof ProjectedCRS) {
            final ProjectedCRS pcrs = (ProjectedCRS) obj;
            final Projection proj = pcrs.getConversionFromBase();

            if (String.valueOf(proj.getName()).toLowerCase().contains("utm")) {
                icon = ICON_UTM;
            } else if (proj instanceof ConicProjection) {
                icon = ICON_CONIC;
            } else if (proj instanceof CylindricalProjection) {
                icon = ICON_SQUARE;
            } else if (proj instanceof PlanarProjection) {
                icon = ICON_STEREO;
            } else {
                icon = ICON_SQUARE;
            }
        } else {
            icon = ICON_SQUARE;
        }
        return icon;
    }

    private static class TypeColumn extends TableColumn<Code, Code> {

        public TypeColumn() {
            setEditable(false);
            setPrefWidth(30);
            setMinWidth(30);
            setMaxWidth(30);
            setCellValueFactory((CellDataFeatures<Code, Code> param) -> new SimpleObjectProperty<>(param.getValue()));
            setCellFactory(new Callback<TableColumn<Code, Code>, TableCell<Code, Code>>() {

                @Override
                public TableCell<Code, Code> call(TableColumn<Code, Code> param) {
                    return new TableCell<Code,Code>(){
                        @Override
                        protected void updateItem(Code item, boolean empty) {
                            super.updateItem(item, empty);
                            setGraphic(null);
                            if (item!=null){
                                Image icon = ICON_UNKNOWN;
                                try {
                                    final IdentifiedObject obj = item.createObject();
                                    icon = getIcon(obj);
                                } catch (FactoryException ex) {
                                    error(ex);
                                }
                                setGraphic(new ImageView(icon));
                            }
                        }
                    };
                }
            });
        }

    }

    private static class CodeColumn extends TableColumn<Code, String> {

        public CodeColumn() {
            super(Vocabulary.format(Vocabulary.Keys.Code));
            setEditable(false);
            setPrefWidth(150);
            setCellValueFactory((TableColumn.CellDataFeatures<Code, String> param) -> new SimpleObjectProperty<>(param.getValue().code));
        }

    }

    private static class DescColumn extends TableColumn<Code, String> {

        public DescColumn() {
            super(Vocabulary.format(Vocabulary.Keys.Description));
            setEditable(false);
            setCellValueFactory((TableColumn.CellDataFeatures<Code, String> param) ->
                    new SimpleObjectProperty<>(param.getValue().getDescription()));
        }

    }

    private static class WKTColumn extends TableColumn<Code, Code> {

        private static final Image ICON = FontGlyphs.createImage("\uE873",16,COLOR);

        public WKTColumn() {
            super("");
            setEditable(false);
            setPrefWidth(26);
            setMinWidth(26);
            setMaxWidth(26);
            setCellValueFactory((CellDataFeatures<Code, Code> param) -> new SimpleObjectProperty<>(param.getValue()));
            setCellFactory(new Callback<TableColumn<Code, Code>, TableCell<Code, Code>>() {

                @Override
                public TableCell<Code, Code> call(TableColumn<Code, Code> param) {
                    return new TableCell<Code,Code>() {
                        {
                            setOnMouseClicked(new EventHandler<MouseEvent>() {
                                @Override
                                public void handle(MouseEvent event) {
                                    final Code item = getItem();
                                    if (item!=null) {
                                        try {
                                            final IdentifiedObject obj = getItem().createObject();
                                            if (obj instanceof FormattableObject) {
                                                WKTPane.showDialog(this, (FormattableObject) obj);
                                            }
                                        } catch (FactoryException ex) {
                                            error(ex);
                                        }
                                    }
                                }
                            });
                        }

                        @Override
                        protected void updateItem(Code item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item !=null && !empty) {
                                setGraphic(new ImageView(ICON));
                            } else {
                                setGraphic(null);
                            }
                        }
                    };
                }
            });
        }
    }
}
