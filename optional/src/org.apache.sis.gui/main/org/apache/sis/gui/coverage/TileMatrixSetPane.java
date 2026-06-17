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

import java.util.Arrays;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.storage.tiling.TileMatrixSetFormat;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.internal.AlignedTableCell;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.ExceptionReporter;


/**
 * A visual representation of the internal tile matrices defined in a {@link TiledResource}.
 * Each {@link TileMatrix} instance is presented as a row in a table. The columns are the tile
 * matrix identifier, the resolution, the number of tiles and (if applicable) the tile size.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 *
 * @see TileMatrixSetFormat
 *
 * @since 1.7
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
     * The object to use for formatting Tile Matrix Set properties.
     */
    private final TileMatrixSetFormat formatter;

    /**
     * The resource for which this widget is showing the Tile Matrix Sets (<abbr>TMS</abbr>).
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
     * The label where to write the name of the Coordinate Reference System of the selected <abbr>TMS</abbr>.
     */
    private final Label crsName;

    /**
     * Tile matrices for the currently selected item in {@link #tileMatriceSets}.
     */
    private final TableView<Row> tileMatrices;

    /**
     * The columns for the resolution along each <abbr>CRS</abbr> axis.
     * The number of columns is the number of <abbr>CRS</abbr> dimensions.
     * Values are {@link Double} numbers, for formatted as {@link String}s
     * by {@link TileMatrixSetFormat}.
     */
    private final TableColumn<Row, String> tileResolutionColumns;

    /**
     * The columns for the number of tiles in each dimension.
     * The number of columns is the number of grid dimensions.
     * Values are {@link Long}s, for formatted as {@link String}s by {@link TileMatrixSetFormat}.
     */
    private final TableColumn<Row, String> tileCountColumns;

    /**
     * The columns for the tile size in each dimension.
     * The number of columns is the number of grid dimensions.
     * Values are {@link Integer}s, for formatted as {@link String}s by {@link TileMatrixSetFormat}.
     */
    private final TableColumn<Row, String> tileSizeColumns;

    /**
     * The columns for the image size in each dimension.
     * The number of columns is the number of grid dimensions.
     * Values are {@link Integer}s, for formatted as {@link String}s by {@link TileMatrixSetFormat}.
     */
    private final TableColumn<Row, String> imageSizeColumns;

    /**
     * A row in the table of tile matrices shown by {@link #tileMatrices}.
     * The resolution, tile count and tile size are groups of columns.
     */
    private static final class Row {
        /**
         * Property for the Tile Matrix identifier.
         */
        final StringProperty identifier;

        /**
         * Resolution along each <abbr>CRS</abbr> dimension.
         * They are the values to show in the group of {@link #tileResolutionColumns}.
         */
        final StringProperty[] resolution;

        /**
         * Number of tiles along each grid dimension.
         * They are the values to show in the group {@link #tileCountColumns}.
         */
        final StringProperty[] tileCount;

        /**
         * Tile size along each grid dimension of the tiles at this level.
         * They are the values to show in the group {@link #tileSizeColumns}.
         */
        final StringProperty[] tileSize;

        /**
         * Tile size along each grid dimension of the image at this level.
         * They are the values to show in the group {@link #imageSizeColumns}.
         */
        final StringProperty[] imageSize;

        /**
         * Creates a new row for the given properties at the specified row index.
         */
        @SuppressWarnings("this-escape")
        Row(final int row,
            final String[]   identifiers,
            final String[][] resolutions,
            final String[][] tileCounts,
            final String[][] tileSizes,
            final String[][] imageSizes)
        {
            identifier = new SimpleStringProperty(this, "identifier", identifiers == null ? null : identifiers[row]);
            resolution = new SimpleStringProperty[resolutions != null ? resolutions.length : 0];
            tileCount  = new SimpleStringProperty[tileCounts  != null ?  tileCounts.length : 0];
            tileSize   = new SimpleStringProperty[tileSizes   != null ?   tileSizes.length : 0];
            imageSize  = new SimpleStringProperty[imageSizes  != null ?  imageSizes.length : 0];
            Arrays.setAll(resolution, (i) -> new SimpleStringProperty(this, "resolution", resolutions[i][row]));
            Arrays.setAll(tileCount,  (i) -> new SimpleStringProperty(this, "tileCount",  tileCounts [i][row]));
            Arrays.setAll(tileSize,   (i) -> new SimpleStringProperty(this, "tileSize",   tileSizes  [i][row]));
            Arrays.setAll(imageSize,  (i) -> new SimpleStringProperty(this, "imageSize",  imageSizes [i][row]));
        }

        /**
         * Returns the group of columns identified by the given index.
         * Value 0 is reserved for identifier (not accepted by this method).
         *
         * @param  groupIndex  1 for resolution, 2 for tile count, 3 for tile size, 4 for image size.
         * @return group of columns at the given index.
         */
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        final StringProperty[] group(final int groupIndex) {
            switch (groupIndex) {
                case 1: return resolution;
                case 2: return tileCount;
                case 3: return tileSize;
                case 4: return imageSize;
                default: throw new AssertionError(groupIndex);
            }
        }

        /**
         * The callback for getting the value of the "identifier" column of a row.
         */
        static final Callback<TableColumn.CellDataFeatures<Row, String>, ObservableValue<String>>
                IDENTIFIER_GETTER = (cell) -> cell.getValue().identifier;

        /**
         * The callback for getting the value of a column other than "identifier".
         *
         * @param  groupIndex   1 for resolution, 2 for tile count or 3 for tile size. 0 is reserved for identifier.
         * @param  columnIndex  the column index in the specified group. This is a <abbr>CRS</abbr> or grid dimension.
         * @return getter of values in the specified column of the specified group of columns.
         */
        static Callback<TableColumn.CellDataFeatures<Row, String>, ObservableValue<String>>
                getter(final int groupIndex, final int columnIndex)
        {
            // Index should never be out of bounds because of the way that we built the arrays.
            return (cell) -> cell.getValue().group(groupIndex)[columnIndex];
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
        formatter = new TileMatrixSetFormat(locale, null);
        view = new GridPane();
        view.setPadding(SPACE_ON_TOP);
        view.setVgap(9);
        view.setHgap(12);
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
            tileSizeColumns       = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.TileSize));
            imageSizeColumns      = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.ImageSize));
            identifier.setCellValueFactory(Row.IDENTIFIER_GETTER);

            tileMatrices = new TableView<>();
            tileMatrices.setEditable(false);
            tileMatrices.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
            tileMatrices.getColumns().setAll(List.of(identifier, tileResolutionColumns, tileCountColumns, tileSizeColumns, imageSizeColumns));
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
     * Adds or removes columns in the "resolution", "tile count" or "tile size" group of columns.
     * After this method calls, the number of columns of the group will be equal to {@code numCols}.
     * If that number is zero, the element is removed from the table.
     *
     * @param row         any row that can be used for determining {@code numCols}, or {@code null} if none.
     * @param groupIndex  1 for resolution, 2 for tile count or 3 for tile size. 0 is reserved for identifier.
     * @param group       group of columns identified by {@code groupIndex}.
     * @param headerText  provider of labels for the column header. The function can return {@code null}.
     */
    private void addOrRemoveColumns(final Row row, final int groupIndex,
                                    final TableColumn<Row, String> group,
                                    final IntFunction<String> headerText)
    {
        final int numCols = (row != null) ? row.group(groupIndex).length : 0;
        final ObservableList<TableColumn<Row, ?>> columns = group.getColumns();
        for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
            String header = headerText.apply(columnIndex);
            if (header == null) {
                header = String.valueOf(columnIndex);
            }
            if (columnIndex < columns.size()) {
                columns.get(columnIndex).setText(header);
            } else {
                final var column = new TableColumn<Row, String>(header);
                column.setCellFactory(AlignedTableCell.baselineRight());
                column.setCellValueFactory(Row.getter(groupIndex, columnIndex));
                columns.add(column);
            }
        }
        columns.remove(numCols, columns.size());
        final ObservableList<TableColumn<Row, ?>> table = tileMatrices.getColumns();
        if (columns.isEmpty()) {
            table.remove(group);
        } else if (!table.contains(group)) {
            table.add(group);
        }
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
     *
     * @todo Defer the execution of the background task if this pane is not currently visible.
     */
    private void tileMatrixSetChanged(final TileMatrixSet newValue) {
        tileMatrices.getItems().clear();
        crsName.setText(null);
        if (newValue != null) {
            BackgroundThreads.execute(new Task<Map<?,?>>() {
                /** The number of rows. */
                private int numRows;

                /** The warning, or {@code null} if none. */
                private Throwable warning;

                /** Fetches the Tile Matrix properties in a background thread. */
                @Override protected Map<?,?> call() {
                    final Map<String, Object> properties;
                    synchronized (formatter) {
                        try {
                            properties = formatter.formatHeader(newValue);
                            numRows = formatter.formatTable(newValue.getTileMatrices().values(), properties);
                            warning = formatter.getError().orElse(null);
                        } finally {
                            formatter.clear();
                        }
                    }
                    return properties;
                }

                /** Invoked in JavaFX thread on success. */
                @Override protected void succeeded() {
                    final Map<?,?> properties = getValue();
                    crsName.setText((String) properties.get("referencing"));
                    final var identifiers = (String[])   properties.get("identifiers");
                    final var resolutions = (String[][]) properties.get("resolutions");
                    final var tileCounts  = (String[][]) properties.get("tileCounts");
                    final var tileSizes   = (String[][]) properties.get("tileSizes");
                    final var imageSizes  = (String[][]) properties.get("imageSizes");
                    final var rows        = new Row[numRows];
                    for (int i = 0; i < rows.length; i++) {
                        rows[i] = new Row(i, identifiers, resolutions, tileCounts, tileSizes, imageSizes);
                    }
                    tileMatrices.getItems().setAll(rows);
                    final Row first = (rows.length != 0) ? rows[0] : null;
                    final var crs = (CoordinateReferenceSystem) properties.get("crs");
                    final CoordinateSystem cs = (crs != null) ? crs.getCoordinateSystem() : null;
                    addOrRemoveColumns(first, 1, tileResolutionColumns, (columnIndex) -> {
                        if (cs == null || columnIndex >= cs.getDimension()) return null;
                        return cs.getAxis(columnIndex).getAbbreviation();
                    });
                    final var gridAxes = (String[]) properties.get("gridAxes");
                    final IntFunction<String> headerText = (columnIndex) -> {
                        if (gridAxes == null || columnIndex >= gridAxes.length) return null;
                        return gridAxes[columnIndex];
                    };
                    addOrRemoveColumns(first, 2, tileCountColumns, headerText);
                    addOrRemoveColumns(first, 3, tileSizeColumns,  headerText);
                    addOrRemoveColumns(first, 4, imageSizeColumns, headerText);
                    if (warning != null) {
                        reportError(warning);
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
     *
     * @todo Should write the message somewhere instead of a window popup.
     */
    private void reportError(final Throwable exception) {
        ExceptionReporter.canNotUseResource(view, exception);
    }
}
