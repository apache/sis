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
package org.apache.sis.gui.coverage;

import java.util.Collection;
import java.util.Locale;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.opengis.util.GenericName;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.util.collection.Containers;


/**
 * A visual representation of the internal tile matrices defined in a {@link TiledResource}.
 *
 * @todo change the text area to a split pane with a tree view on the left and a description pane on the right
 * @todo if the resource is writable, add tiling modification controls
 *
 * @author Johann Sorel (Geomatys)
 *
 * @sinec 1.7
 */
public class TileMatrixSetPane extends Widget {
    /**
     * The converter for producing string representations of the items shown in {@link #tileMatriceSets}.
     * Used for the combo box which contains the list of all Tile Matrix Sets found in a tiled resource.
     */
    private static final StringConverter<TileMatrixSet> MATRIXSET_TO_STRING = new StringConverter<>() {
        /** Returns the string representation of the identifier of the given item. */
        @Override public String toString(final TileMatrixSet tms) {
            if (tms != null) {
                final GenericName identifier = tms.getIdentifier();
                if (identifier != null) {
                    return identifier.toString();
                }
            }
            return null;
        }

        /** Not used because the combo box is non-editable. */
        @Override public TileMatrixSet fromString(String string) {
            return null;
        }
    };

    /**
     * The locale for texts in controls, or {@code null} for the default locale.
     */
    private final Locale locale;

    /**
     * The data shown in this widget.
     *
     * @see #getContent()
     * @see #setContent(TiledResource)
     */
    public final ObjectProperty<TiledResource> contentProperty;

    /**
     * List of Tile Matrix Sets (<abbr>TMS</abbr>) in the tiled resource.
     */
    private final ComboBox<TileMatrixSet> tileMatriceSets;

    /**
     * The label where to write the name of the Coordinate Reference System.
     */
    private final Label crsName;

    /**
     * Tile matrices for the currently selected item in {@link #tileMatriceSets}.
     */
    private final TableView<Row> tileMatrices;

    /**
     * The columns for the resolution along each <abbr>CRS</abbr> axis.
     * The number of columns is the number of <abbr>CRS</abbr> dimensions.
     */
    private final TableColumn<Row, Double> tileResolutionColumns;

    /**
     * The columns for the number of tiles in each dimension.
     * The number of columns is the number of grid dimensions.
     */
    private final TableColumn<Row, Long> tileCountColumns;

    /**
     * A row in the table of tile matrices shown by {@link #tileMatrices}.
     * The resolution and tile count are group of columns.
     */
    private static final class Row {
        /**
         * Property for the Tile Matrix identifier.
         */
        private final SimpleStringProperty identifier;

        /**
         * Resolution along each <abbr>CRS</abbr> dimension.
         * They are the values to show in the group of {@link #tileResolutionColumns}.
         */
        private final SimpleObjectProperty<Double>[] resolution;

        /**
         * Number of tiles along each grid dimension.
         * They are the values to show in the group {@link #tileCountColumns}.
         */
        private final SimpleObjectProperty<Long>[] tileCount;

        /**
         * Creates a new row for the given tile matrix.
         *
         * @param  matrix  the matrix for which to create a row in the table.
         */
        @SuppressWarnings({"this-escape", "rawtypes", "unchecked"})
        Row(final TileMatrix matrix) {
            identifier = new SimpleStringProperty(this, "identifier");
            final GenericName id = matrix.getIdentifier();
            if (id != null) {
                identifier.setValue(id.toString());
            }
            final double[] r = matrix.getResolution();
            resolution = new SimpleObjectProperty[r.length];
            for (int i=0; i<resolution.length; i++) {
                (resolution[i] = new SimpleObjectProperty<>(this, "resolution")).set(r[i]);
            }
            final GridExtent extent = matrix.getTilingScheme().getExtent();
            tileCount = new SimpleObjectProperty[extent.getDimension()];
            for (int i=0; i<tileCount.length; i++) {
                (tileCount[i] = new SimpleObjectProperty<>(this, "tileCount")).set(extent.getSize(i));
            }
        }

        /**
         * The callback for getting the identifier value of a row.
         */
        static final Callback<TableColumn.CellDataFeatures<Row, String>, ObservableValue<String>>
                IDENTIFIER = (cell) -> cell.getValue().identifier;

        /**
         * The callback for getting resolution values in the specified <abbr>CRS</abbr> dimension.
         *
         * @param  i  a <abbr>CRS</abbr> dimension.
         * @return getter of resolution values in the specified <abbr>CRS</abbr> dimension.
         */
        static Callback<TableColumn.CellDataFeatures<Row, Double>, ObservableValue<Double>> resolution(final int i) {
            return (cell) -> {
                final SimpleObjectProperty<Double>[] resolution = cell.getValue().resolution;
                return (i >= 0 && i < resolution.length) ? resolution[i] : null;
            };
        }

        /**
         * The callback for getting tile count values of a row in the specified dimension.
         *
         * @param  i  a grid dimension.
         * @return getter of tile count values in the specified grid dimension.
         */
        static Callback<TableColumn.CellDataFeatures<Row, Long>, ObservableValue<Long>> tileCount(final int i) {
            return (cell) -> {
                final SimpleObjectProperty<Long>[] tileCount = cell.getValue().tileCount;
                return (i >= 0 && i < tileCount.length) ? tileCount[i] : null;
            };
        }
    }

    /**
     * The pane where to layout the controls.
     */
    private final GridPane view;

    /**
     * Some spaces to add on the top, before the first control.
     */
    private static final Insets SPACE_ON_TOP = new Insets(9, 0, 0, 0);

    /**
     * Some spaces to add on the left and right sides of labels and controls.
     */
    private static final Insets HORIZONTAL_SPACE = new Insets(0, 12, 0, 12);

    /**
     * Creates an initially empty pane showing the content of a tile matrix.
     *
     * @param  locale  the locale for texts in controls, or {@code null} for the default locale.
     */
    @SuppressWarnings("this-escape")
    public TileMatrixSetPane(final Locale locale) {
        view = new GridPane();
        view.setPadding(SPACE_ON_TOP);
        view.setVgap(9);
        view.setHgap(12);
        this.locale = locale;
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);
        /*
         * Combox box for choosing the Tile Matrix Set.
         * The combo box usually contains only one element.
         */
        {   // Block for keeping variable locale.
            final Label label = new Label(vocabulary.getLabel(Vocabulary.Keys.TileMatrixSets));
            label.setPadding(HORIZONTAL_SPACE);
            GridPane.setHgrow(label, Priority.NEVER);

            tileMatriceSets = new ComboBox<>();
            tileMatriceSets.setEditable(false);
            tileMatriceSets.setConverter(MATRIXSET_TO_STRING);
            GridPane.setHgrow(tileMatriceSets, Priority.ALWAYS);
            label.setLabelFor(tileMatriceSets);
            view.addRow(0, label, tileMatriceSets);
        }
        /*
         * Name of the Coordinate Reference System used by the currently selected Tile Matrix Set.
         */
        {   // Block for keeping variable locale.
            final Label label = new Label(vocabulary.getLabel(Vocabulary.Keys.ReferenceSystem));
            label.setPadding(HORIZONTAL_SPACE);
            GridPane.setHgrow(label, Priority.NEVER);

            crsName = new Label();
            crsName.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(crsName, Priority.ALWAYS);
            label.setLabelFor(crsName);
            view.addRow(1, label, crsName);
        }
        /*
         * Table of tile matrices for the currently selected Tile Matrix Set.
         */
        {
            final TableColumn<Row, String> identifier;
            identifier            = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Identifier));
            tileResolutionColumns = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Resolution));
            tileCountColumns      = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.TileCount));
            identifier.setCellValueFactory(Row.IDENTIFIER);

            tileMatrices = new TableView<>();
            tileMatrices.setEditable(false);
            tileMatrices.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
            tileMatrices.getColumns().setAll(List.of(identifier, tileResolutionColumns, tileCountColumns));
            GridPane.setHgrow(tileMatrices, Priority.ALWAYS);
            GridPane.setVgrow(tileMatrices, Priority.ALWAYS);
            GridPane.setColumnSpan(tileMatrices, 2);
            view.add(tileMatrices, 0, 2);
        }
        contentProperty = new SimpleObjectProperty<>(this, "content");
        contentProperty.addListener(TileMatrixSetPane::resourceChanged);
        tileMatriceSets.getSelectionModel().selectedItemProperty().addListener((p,o,n) -> tileMatrixSetChanged(n));
    }

    /**
     * Sets the data for which to show the tiling.
     * This is a convenience method for setting {@link #contentProperty} value.
     *
     * @param  data  the data for which to show the tiling, or {@code null} if none.
     */
    public final void setContent(final TiledResource data) {
        contentProperty.setValue(data);
    }

    /**
     * Returns the data for which the tiling is currently shown, or {@code null} if none.
     * This is a convenience method for fetching {@link #contentProperty} value.
     *
     * @return the table for which the tiling is currently shown, or {@code null} if none.
     *
     * @see #contentProperty
     * @see #setContent(TiledResource)
     */
    public final TiledResource getContent() {
        return contentProperty.getValue();
    }

    /**
     * Returns the region containing the visual components managed by this {@code TileMatrixSetPane}.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the JavaFX component to insert in a scene graph.
     */
    @Override
    public Region getView() {
        return view;
    }

    /**
     * Invoked when {@link #contentProperty} value changed.
     * The Tile Matrix Sets are loaded in a background thread.
     *
     * @param  property  the property which has been modified.
     * @param  oldValue  the old resource.
     * @param  newValue  the resource to use for building new content.
     */
    private static void resourceChanged(final ObservableValue<? extends TiledResource> property,
                                        final TiledResource oldValue, final TiledResource newValue)
    {
        final var widget = (TileMatrixSetPane) ((SimpleObjectProperty) property).getBean();
        final ComboBox<TileMatrixSet> tileSets = widget.tileMatriceSets;
        tileSets.getItems().clear();
        if (newValue != null) {
            BackgroundThreads.execute(new Task<TileMatrixSet[]>() {
                /** Fetches the Tile Matrix Sets in a background thread. */
                @Override protected TileMatrixSet[] call() throws DataStoreException {
                    return newValue.getTileMatrixSets().toArray(TileMatrixSet[]::new);
                }

                /** Invoked in JavaFX thread on success. */
                @Override protected void succeeded() {
                    tileSets.getItems().setAll(getValue());
                    tileSets.getSelectionModel().selectFirst();
                }

                /** Invoked in JavaFX thread on failure. */
                @Override protected void failed() {
                    widget.reportError(getException());
                }
            });
        }
    }

    /**
     * Invoked when {@link #tileMatriceSets} selected value changed.
     * The Tile Matrix Set properties are fetched in a background thread.
     *
     * @param  newValue  the Tile Matrix Set to use for building new content.
     */
    private void tileMatrixSetChanged(final TileMatrixSet newValue) {
        tileMatrices.getItems().clear();
        crsName.setText(null);
        if (newValue != null) {
            BackgroundThreads.execute(new Task<TileMatrix[]>() {
                /** The coordinate reference system. */
                private CoordinateReferenceSystem crs;

                /** Name of the Coordinate Reference System. */
                private String crsDisplayName;

                /** The grid extent of the tiling scheme, using the first row as a representative value. */
                private GridExtent tilingScheme;

                /** Fetches the Tile Matrices in a background thread. */
                @Override protected TileMatrix[] call() {
                    crs = newValue.getCoordinateReferenceSystem();
                    if (crs != null) {
                        crsDisplayName = IdentifiedObjects.getDisplayName(crs, locale);
                    }
                    final Collection<? extends TileMatrix> matrices = newValue.getTileMatrices().values();
                    final TileMatrix first = Containers.peekFirst(matrices);
                    if (first != null) {
                        tilingScheme = first.getTilingScheme().getExtent();
                    }
                    return matrices.toArray(TileMatrix[]::new);
                }

                /** Invoked in JavaFX thread on success. */
                @Override protected void succeeded() {
                    crsName.setText(crsDisplayName);
                    final TileMatrix[] matrices = getValue();
                    final Row[] rows = new Row[matrices.length];
                    int crsDimension = 0, gridDimension = 0;
                    for (int i=0; i<rows.length; i++) {
                        final var row = new Row(matrices[i]);
                        crsDimension  = Math.max(crsDimension, row.resolution.length);
                        gridDimension = Math.max(gridDimension, row.tileCount.length);
                        rows[i] = row;
                    }
                    tileMatrices.getItems().setAll(rows);
                    /*
                     * Add or remove columns for the CRS dimensions.
                     */
                    {
                        final CoordinateSystem cs = (crs != null) ? crs.getCoordinateSystem() : null;
                        final ObservableList<TableColumn<Row, ?>> columns = tileResolutionColumns.getColumns();
                        for (int i=0; i<crsDimension; i++) {
                            final String header = (i < cs.getDimension())
                                    ? cs.getAxis(i).getAbbreviation() : String.valueOf(i);
                            if (i < columns.size()) {
                                columns.get(i).setText(header);
                            } else {
                                final var column = new TableColumn<Row, Double>(header);
                                column.setCellValueFactory(Row.resolution(i));
                                columns.add(column);
                            }
                        }
                        final int size = columns.size();
                        if (crsDimension < size) {
                            columns.remove(crsDimension, size);
                        }
                    }
                    /*
                     * Add or remove columns for the grid dimensions.
                     * We use the first row for getting the grid axis identifiers.
                     */
                    final ObservableList<TableColumn<Row, ?>> columns = tileCountColumns.getColumns();
                    if (rows.length != 0) {
                        for (int i=0; i<gridDimension; i++) {
                            String header = null;
                            if (i < tilingScheme.getDimension()) {
                                header = tilingScheme.getAxisType(i).flatMap(DimensionNameType::identifier).orElse(null);
                            }
                            if (header == null) {
                                header = String.valueOf(i);
                            }
                            if (i < columns.size()) {
                                columns.get(i).setText(header);
                            } else {
                                final var column = new TableColumn<Row, Long>(header);
                                column.setCellValueFactory(Row.tileCount(i));
                                columns.add(column);
                            }
                        }
                    }
                    final int size = columns.size();
                    if (crsDimension < size) {
                        columns.remove(crsDimension, size);
                    }
                }

                /** Invoked in JavaFX thread on failure. */
                @Override protected void failed() {
                    reportError(getException());
                }
            });
        }
    }

    /**
     * Invoked when a background task failed.
     */
    private void reportError(final Throwable exception) {
        ExceptionReporter.canNotUseResource(view, exception);
    }
}
