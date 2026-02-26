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
package org.apache.sis.storage.tiling;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Format;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Objects;
import java.util.Optional;
import java.util.Arrays;
import java.util.StringJoiner;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.io.TableAppender;


/**
 * Formats Tile Matrix Sets (<abbr>TMS</abbr>) in a tabular format.
 * This format assumes a monospaced font and a character encoding which supports the drawing of box characters,
 * such as <abbr>UTF</abbr>-8.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The current implementation can only format tile matrices — parsing is supported.</li>
 *   <li>{@code TileMatrixSetFormat}, like most {@code java.text.Format} subclasses, is not thread-safe.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 * @since   1.7
 */
public class TileMatrixSetFormat extends CompoundFormat<TileMatrixSet> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2612127094991996016L;

    /**
     * Helper class for string representation of an image pyramid as a table.
     * Each instance describes one table row for one {@link ImageTileMatrix}.
     */
    private static final class Row {
        /** The tile matrix identifier. */
        final String identifier;

        /** The tile matrix resolution in each <abbr>CRS</abbr> dimensions. */
        final double[] resolution;

        /** The string representations of the tile matrix resolution. */
        final String[] formattedResolution;

        /** The number of tiles in each grid dimension. */
        final String[] tileCount;

        /** The tile sizes in pixels. */
        final String[] tileSize;

        /** The extent of the tiling scheme. */
        private final GridExtent tilingScheme;

        /**
         * Creates one row in the table for the given matrix.
         *
         * @param  matrix         the matrix for which to store information.
         * @param  integerFormat  a number format configured for integer values.
         * @throws BackingStoreException if an error occurred while extracting information.
         * @throws IncompleteGridGeometryException if the tiling scheme has not extent or resolution.
         *         Tile matrices with such tiling scheme should not have been constructed in first place.
         */
        Row(final TileMatrix matrix, final NumberFormat integerFormat) {
            final GenericName id = matrix.getIdentifier();
            identifier = (id != null) ? id.toString() : "";
            resolution = matrix.getResolution();
            formattedResolution = new String[resolution.length];
            tilingScheme = matrix.getTilingScheme().getExtent();
            tileCount = new String[tilingScheme.getDimension()];
            for (int i=0; i<tileCount.length; i++) {
                tileCount[i] = integerFormat.format(tilingScheme.getSize(i));
            }
            final int[] ts = ImageTileMatrix.getTileSize(matrix);
            tileSize = new String[ts != null ? ts.length : 0];
            for (int i=0; i<tileSize.length; i++) {
                tileSize[i] = integerFormat.format(ts[i]);
            }
        }

        /**
         * Updates the axis name in the dimension <var>i</var> of the grid extent.
         * This method verifies that grid dimensions in all rows have the same axis names.
         */
        private void searchCommonAxisName(final String[] gridAxes, final int i) {
            tilingScheme.getAxisType(i).ifPresent((axis) -> {
                final String name = axis.identifier().orElseGet(() -> axis.name().toLowerCase(Locale.US));
                final String current = gridAxes[i];
                if (current == null) {
                    gridAxes[i] = name;
                } else if (!current.equals(name)) {
                    gridAxes[i] = "";
                }
            });
        }

        /**
         * Creates the string representation of the resolutions in all resolution columns of all rows.
         * This method computes the number of fraction digits based on the resolution values of all rows.
         *
         * @param  rows    the rows in which to format the resolutions.
         * @param  format  the number formats to use. Its number of fraction digits will be updated.
         * @return the maximal number of resolution values found in all rows.
         *         This value should be equal to the number of dimensions in the <abbr>CRS</abbr>.
         */
        static int formatResolutions(final List<Row> rows, final NumberFormat format) {
            final var values = new double[rows.size()];
            for (int i=0; ; i++) {
                int count = 0;
                for (final Row row : rows) {
                    if (i < row.resolution.length) {
                        values[count++] = row.resolution[i];
                    }
                }
                if (count == 0) return i;
                final int n = Numerics.suggestFractionDigits(ArraysExt.resize(values, count));
                format.setMinimumFractionDigits(n);
                format.setMaximumFractionDigits(n);
                final int column = i;   // Because lambda requires final values.
                final String[] formatted = Numerics.formatAndTrimTrailingZeros(format, values.length, (j) -> {
                    final double[] resolution = rows.get(j).resolution;
                    return (column < resolution.length) ? resolution[column] : Double.NaN;
                });
                for (int j=0; j<values.length; j++) {
                    final String[] resolution = rows.get(j).formattedResolution;
                    if (i < resolution.length) {
                        resolution[i] = formatted[j];
                    }
                }
            }
        }
    }

    /**
     * The error that occurred while formatting a Tile Matrix Set.
     */
    private transient Throwable error;

    /**
     * Creates a new formatter using the given locale and timezone.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     */
    public TileMatrixSetFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
    }

    /**
     * Returns the type of values formatted by this {@code Format} instance.
     *
     * @return the type of values formatted by this {@code Format} instance.
     */
    @Override
    public final Class<TileMatrixSet> getValueType() {
        return TileMatrixSet.class;
    }

    /**
     * Formats the properties of the given Tile Matrix Set for presentation before the main table.
     * The properties are formatted as {@link String}s using the locale given at construction time.
     * The returned map contains the following entries if the corresponding properties were found:
     *
     * <table class="sis">
     *   <caption>Tile Matrix Set (<abbr>TMS</abbr>) formatted properties</caption>
     *   <tr><th>Key</th>                  <th>Value type</th>                           <th>Description</th></tr>
     *   <tr><td>{@code "identifier"}</td> <td>{@link String}</td>                       <td>Identifier of the <abbr>TMS</abbr>.</td></tr>
     *   <tr><td>{@code "crsName"}</td>    <td>{@link String}</td>                       <td>Name of the <abbr>CRS</abbr>.</td></tr>
     *   <tr><td>{@code "crs"}</td>        <td>{@link CoordinateReferenceSystem}</td>    <td>The <abbr>CRS</abbr>.</td></tr>
     *   <tr><td>{@code "bbox"}</td>       <td>{@link DefaultGeographicBoundingBox}</td> <td>Bounding box of the <abbr>TMS</abbr>.</td></tr>
     * </table>
     *
     * The returned properties can be completed by a call to {@link #formatTable(Iterable, Map)}.
     *
     * @param  matrices  the tile matrices to format.
     * @return properties of the header of the given tile matrix set.
     */
    public Map<String, Object> formatHeader(final TileMatrixSet matrices) {
        final var addTo = new HashMap<String, Object>();
        addTo.put("identifier", matrices.getIdentifier());
        try {
            final CoordinateReferenceSystem crs = matrices.getCoordinateReferenceSystem();
            addTo.put("crsName", IdentifiedObjects.getDisplayName(crs, getLocale()));
            addTo.put("crs", crs);
            matrices.getEnvelope().ifPresent((envelope) -> {
                try {
                    final var bbox = new DefaultGeographicBoundingBox();
                    bbox.setBounds(envelope);
                    bbox.setInclusion(null);
                    addTo.put("bbox", bbox);
                } catch (TransformException e) {
                    // Ignore because this exception may be normal if the envelope has no spatial component.
                    Logging.ignorableException(ImageTileMatrix.LOGGER, TileMatrixSetFormat.class, "format", e);
                }
            });
        } catch (BackingStoreException e) {
            // The CRS or envelope may be missing in the header, but we may still be able to format the table.
            addError(e.getCause());
        }
        addTo.values().removeIf(Objects::isNull);
        return addTo;
    }

    /**
     * Formats the properties of the given Tile Matrices in a way suitable to a tabular format.
     * The properties are formatted as {@link String}s using the locale given at construction time.
     * The returned map contains the following entries if the corresponding properties were found:
     *
     * <table class="sis">
     *   <caption>Tile Matrices formatted properties</caption>
     *   <tr><th>Key</th>                   <th>Value type</th>         <th>Description</th></tr>
     *   <tr><td>{@code "identifiers"}</td> <td>{@code String[]}</td>   <td>Column of the identifier of each Tile Matrix.</td></tr>
     *   <tr><td>{@code "resolutions"}</td> <td>{@code String[][]}</td> <td>Columns of the resolution of each Tile Matrix.</td></tr>
     *   <tr><td>{@code "tileCounts"}</td>  <td>{@code String[][]}</td> <td>Columns of the number of tiles of each Tile Matrix.</td></tr>
     *   <tr><td>{@code "tileSizes"}</td>   <td>{@code String[][]}</td> <td>Columns of the tile size of each Tile Matrix.</td></tr>
     *   <tr><td>{@code "gridAxes"}</td>    <td>{@code String[]}</td>   <td>Name of grid axes.</td></tr>
     * </table>
     *
     * The {@code "tileSizes"} property may be absent if not applicable.
     * Implementations other than the default implementation may also choose to add or remove more properties.
     *
     * <p>This method returns the number of rows in the table to format.
     * The length of the {@code "identifiers"} array is that number of rows.
     * All other arrays have a length equal to the number of columns in a group of columns,
     * which is 2 in the usual case of two-dimensional Tile Matrix Sets.
     * The {@code "resolutions[i]"}, {@code "tileCounts[i]"} and {@code "tileSizes[i]"} arrays,
     * where <var>i</var> is a column index, all have a length equal to the number of rows.
     * Some element may be {@code null} if the corresponding data could not be extracted.</p>
     *
     * @param  matrices  the tile matrices to format.
     * @param  addTo     where to put the properties of the given tile matrix set.
     * @return number of rows in the table to format.
     */
    public int formatTable(final Iterable<? extends TileMatrix> matrices, final Map<String, Object> addTo) {
        final var rows = new ArrayList<Row>();
        int gridDimension = 0, sizeDimension = 0;
        final var integerFormat = (NumberFormat) getFormat(Long.class);
        for (final TileMatrix matrix : matrices) try {
            final var row = new Row(matrix, integerFormat);
            gridDimension = Math.max(gridDimension, row.tileCount.length);
            sizeDimension = Math.max(sizeDimension, row.tileSize.length);
            rows.add(row);  // Add only on success.
        } catch (RuntimeException e) {
            addError(e);    // Skip the row. Next row will be tried.
        }
        final int crsDimension = Row.formatResolutions(rows, (NumberFormat) getFormat(Double.class));
        final int numRows     = rows.size();
        final var identifiers = new String[numRows];
        final var resolutions = new String[ crsDimension][numRows];
        final var tileCounts  = new String[gridDimension][numRows];
        final var tileSizes   = new String[sizeDimension][numRows];
        final var gridAxes    = new String[gridDimension];
        for (int j=0; j<numRows; j++) {
            final Row row = rows.get(j);
            identifiers[j] = row.identifier;
            for (int i = Math.min(crsDimension, row.resolution.length); --i >= 0;) {
                resolutions[i][j] = row.formattedResolution[i];
            }
            for (int i = Math.min(gridDimension, row.tileCount.length); --i >= 0;) {
                tileCounts[i][j] = row.tileCount[i];
                row.searchCommonAxisName(gridAxes, i);
            }
            for (int i = Math.min(sizeDimension, row.tileSize.length); --i >= 0;) {
                tileSizes[i][j] = row.tileSize[i];
            }
        }
        if (numRows       != 0) addTo.put("identifiers", identifiers);
        if (crsDimension  != 0) addTo.put("resolutions", resolutions);
        if (gridDimension != 0) addTo.put("tileCounts",  tileCounts);
        if (sizeDimension != 0) addTo.put("tileSizes",   tileSizes);
        for (int i=0; i<gridAxes.length; i++) {
            final String name = gridAxes[i];
            if (name != null && name.isBlank()) {
                gridAxes[i] = null;
            }
        }
        if (!ArraysExt.allEquals(gridAxes, null)) {
            addTo.put("gridAxes", gridAxes);
        }
        return numRows;
    }

    /**
     * Returns localized resources for the table header.
     */
    private Vocabulary vocabulary() {
        return Vocabulary.forLocale(getLocale());
    }

    /**
     * Formats a textual representation of the given tile matrices.
     * This method delegates to {@link #format(TileMatrixSet, Appendable)}
     * with a temporary buffer.
     *
     * @param  matrices  the tile matrices to format.
     * @return a string representation of the given Tile Matrix Set.
     */
    public String format(TileMatrixSet matrices) {
        return format(matrices, false);
    }

    /**
     * Formats the given tile matrices, optionally with a report of exceptions.
     *
     * @param  matrices     the tile matrices to format.
     * @param  reportError  whether to report exceptions.
     * @return a string representation of the given Tile Matrix Set.
     */
    final String format(final TileMatrixSet matrices, final boolean reportError) {
        final var buffer = new StringBuilder(1000);
        try {
            format(matrices, buffer);
            if (!reportError || error == null) {
                return buffer.toString();
            }
            final var writer = new StringWriter(buffer.length()).append(buffer);
            vocabulary().appendLabel(Vocabulary.Keys.Warnings, writer);
            error.printStackTrace(new PrintWriter(writer.append(' ')));
            return writer.append(System.lineSeparator()).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Formats a textual representation of the given tile matrices.
     * First, a header is formatted with the <abbr>TMS</abbr> identifier,
     * the Coordinate Reference System and the geographic bounding box.
     * Then, each tile matrix is formatted as a row in a table.
     *
     * @param  matrices    the tile matrices to format.
     * @param  toAppendTo  where to format the object.
     * @throws IOException if an error occurred while writing to the given appendable.
     */
    @Override
    public void format(final TileMatrixSet matrices, final Appendable toAppendTo) throws IOException {
        final Vocabulary vocabulary = vocabulary();
        final Map<String, Object> properties = formatHeader(matrices);
        final int numRows = formatTable(matrices.getTileMatrices().values(), properties);
        Object value = properties.get("identifier");
        if (value != null) {
            toAppendTo.append(vocabulary.getString(Vocabulary.Keys.TileMatrixSets)).append(' ')
                      .append(vocabulary.getString(Vocabulary.Keys.Quoted_1, value))
                      .append(System.lineSeparator());
        }
        value = properties.get("crsName");
        if (value != null) {
            vocabulary.appendLabel(Vocabulary.Keys.ReferenceSystem, toAppendTo);
            toAppendTo.append(' ').append(value.toString()).append(System.lineSeparator());
        }
        value = properties.get("bbox");
        if (value != null) {
            final var bbox = (DefaultGeographicBoundingBox) value;
            final var mf = (TreeTableFormat) getFormat(GeographicBoundingBox.class);
            mf.setColumns(TableColumn.NAME, TableColumn.VALUE);
            toAppendTo.append(mf.format(bbox.asTreeTable()));
        }
        /*
         * The header was formatted by above code. The following code formats the main table.
         * First, we get the values to format in all columns and prepend the number of spaces
         * needed for right alignment.
         */
        final var identifiers = (String[])   properties.get("identifiers");
        final var resolutions = (String[][]) properties.get("resolutions");
        final var tileCounts  = (String[][]) properties.get("tileCounts");
        final var tileSizes   = (String[][]) properties.get("tileSizes");
        String[][][] columnGroups = {resolutions, tileCounts, tileSizes};
        columnGroups = ArraysExt.resize(columnGroups, ArraysExt.removeNulls(columnGroups));
        for (final String[][] columns : columnGroups) {
            for (final String[] column : columns) {
                Arrays.stream(column).mapToInt(String::length).max().ifPresent((length) -> {
                    for (int j=0; j<column.length; j++) {
                        String e  = column[j];
                        column[j] = CharSequences.spaces(length - e.length()) + e;
                    }
                });
            }
        }
        final var table = new TableAppender(toAppendTo);
        table.appendHorizontalSeparator();
        if (identifiers != null) table.append(vocabulary.getString(Vocabulary.Keys.Identifier)).nextColumn();
        if (resolutions != null) table.append(vocabulary.getString(Vocabulary.Keys.Resolution)).nextColumn();
        if (tileCounts  != null) table.append(vocabulary.getString(Vocabulary.Keys.TileCount)) .nextColumn();
        if (tileSizes   != null) table.append(vocabulary.getString(Vocabulary.Keys.TileSize))  .nextColumn();
        table.appendHorizontalSeparator();
        for (int j=0; j<numRows; j++) {
            if (identifiers != null) {
                table.setCellAlignment(TableAppender.ALIGN_LEFT);
                table.append(identifiers[j]).nextColumn();
                table.setCellAlignment(TableAppender.ALIGN_RIGHT);
            }
            for (final String[][] columns : columnGroups) {
                final var joiner = new StringJoiner(" × ");
                for (String[] column : columns) {
                    joiner.add(column[j]);
                }
                table.append(joiner.toString()).nextColumn();
            }
            table.nextLine();
        }
        table.appendHorizontalSeparator();
        table.flush();
    }

    /**
     * Not supported.
     *
     * @return currently never return.
     * @throws ParseException currently always thrown.
     * @hidden Not implemented.
     */
    @Override
    public TileMatrixSet parse(CharSequence text, ParsePosition pos) throws ParseException {
        throw new ParseException(Errors.forLocale(getLocale())
                .getString(Errors.Keys.UnsupportedOperation_1, "parse"), pos.getIndex());
    }

    /**
     * Creates a new format to use for formatting values of the given type.
     * This method adds the creation of {@link TreeTableFormat}.
     *
     * @param  valueType  the base type of values to parse or format.
     * @return the format to use for parsing of formatting values of the given type, or {@code null} if none.
     * @hidden the addition compared to parent class are implementation details.
     */
    @Override
    protected Format createFormat(final Class<?> valueType) {
        if (valueType == GeographicBoundingBox.class) {
            return new TreeTableFormat(getLocale(), getTimeZone());
        }
        return super.createFormat(valueType);
    }

    /**
     * Records that an error occurred.
     *
     * @param  e  the error that occurred.
     */
    private void addError(final Throwable e) {
        if (error == null) {
            error = e;
        } else {
            error.addSuppressed(e);
        }
    }

    /**
     * If an error occurred while formatting the Tile Matrix Set, returns that error.
     *
     * @return the error that occurred during formatting.
     */
    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    /**
     * Clears the error status. This method should be invoked if the same {@code TileMatrixSetFormat}
     * instance is used for formatting many {@code TileMatrixSet} instances.
     */
    public void clear() {
        error = null;
    }
}
