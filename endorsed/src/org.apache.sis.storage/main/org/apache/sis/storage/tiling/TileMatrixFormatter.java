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
import java.util.Locale;
import java.util.ArrayList;
import java.text.NumberFormat;
import org.opengis.util.GenericName;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.io.TableAppender;


/**
 * Formatter of tile matrix sets.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TileMatrixFormatter {
    /**
     * Helper class for string representation of an image pyramids as a table.
     * Each instance describes one table row for one {@link ImageTileMatrix}.
     */
    private static final class Row {
        /** The tile matrix identifier. */
        private final String identifier;

        /** The tile matrix resolution in each <abbr>CRS</abbr> dimensions. */
        private final double[] resolution;

        /** The string representations of the tile matrix resolution. */
        private final String[] formattedResolution;

        /** The number of tiles in each grid dimension. */
        private final String[] tileCount;

        /** The tile sizes in pixels. */
        private final String[] tileSize;

        /**
         * Creates one row in the table for the given matrix.
         *
         * @param  matrix  the matrix for which to store information.
         * @param  integerFormat  the format to use for integer values.
         * @throws BackingStoreException if an error occurred while extracting information.
         * @throws IncompleteGridGeometryException if the tiling scheme has not extent or resolution.
         *         Tile matrices with such tiling scheme should not have been constructed in first place.
         */
        private Row(final TileMatrix matrix, final NumberFormat integerFormat) {
            final GenericName id = matrix.getIdentifier();
            identifier = (id != null) ? id.toString() : "";
            resolution = matrix.getResolution();
            formattedResolution = new String[resolution.length];
            final GridExtent ge = matrix.getTilingScheme().getExtent();
            tileCount = new String[ge.getDimension()];
            for (int i=0; i<tileCount.length; i++) {
                tileCount[i] = integerFormat.format(ge.getSize(i));
            }
            final int[] ts = ImageTileMatrix.getTileSize(matrix);
            tileSize = new String[ts != null ? ts.length : 0];
            for (int i=0; i<tileSize.length; i++) {
                tileSize[i] = integerFormat.format(ts[i]);
            }
        }

        /**
         * Creates the string representation of resolutions.
         */
        final void formatResolutions(final NumberFormat[] formats) {
            for (int i=0; i<resolution.length; i++) {
                formattedResolution[i] = formats[i].format(resolution[i]);
            }
        }
    }

    /**
     * The locale specified at construction time. May be {@code null}.
     */
    private final Locale locale;

    /**
     * Resources for table header.
     */
    private final Vocabulary vocabulary;

    /**
     * The object to use for formatting integer values.
     */
    private final NumberFormat integerFormat;

    /**
     * The error that occurred while formatting a value.
     */
    private Throwable error;

    /**
     * Creates a new formatter using the given locale.
     */
    TileMatrixFormatter(final Locale locale) {
        this.locale = locale;
        vocabulary = Vocabulary.forLocale(locale);
        integerFormat = (locale != null)
                ? NumberFormat.getIntegerInstance(locale)
                : NumberFormat.getIntegerInstance();
    }

    /**
     * Returns a string representation of the given tile matrices.
     * Each tile matrix is formatted as a row in a table.
     *
     * @param  matrices  the tile matrices to format.
     * @return the string representation of the table of tile matrices.
     */
    final String format(final TileMatrixSet matrices) {
        final var buffer = new StringBuilder(1000);
        try {
            formatHeader(matrices, buffer);
            formatTable(matrices, buffer);
            if (error == null) {
                return buffer.toString();
            }
            final var writer = new StringWriter(buffer.length()).append(buffer);
            vocabulary.appendLabel(Vocabulary.Keys.Warnings, writer);
            error.printStackTrace(new PrintWriter(writer.append(' ')));
            return writer.append(System.lineSeparator()).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Formats the header of the Tile Matrix Set (<abbr>TMS</abbr>) representation.
     * The header contains the <abbr>TMS</abbr> identifier, the Coordinate Reference System
     * and the geographic bounding box.
     *
     * @param  matrices  the Tile Matrix Set (<abbr>TMS</abbr>) to format.
     * @param  buffer    where to format the header.
     * @throws IOException should never happen but handled by the caller for convenience.
     */
    private void formatHeader(final TileMatrixSet matrices, final StringBuilder buffer) throws IOException {
        buffer.append(vocabulary.getString(Vocabulary.Keys.TileMatrixSets)).append(' ')
              .append(vocabulary.getString(Vocabulary.Keys.Quoted_1, matrices.getIdentifier()))
              .append(System.lineSeparator());
        try {
            final String crs = IdentifiedObjects.getDisplayName(matrices.getCoordinateReferenceSystem(), locale);
            if (crs != null) {
                vocabulary.appendLabel(Vocabulary.Keys.ReferenceSystem, buffer);
                buffer.append(' ').append(crs).append(System.lineSeparator());
            }
            matrices.getEnvelope().ifPresent((envelope) -> {
                try {
                    final var bbox = new DefaultGeographicBoundingBox();
                    bbox.setBounds(envelope);
                    bbox.setInclusion(null);
                    final var mf = new TreeTableFormat(locale, null);
                    mf.setColumns(TableColumn.NAME, TableColumn.VALUE);
                    buffer.append(mf.format(bbox.asTreeTable()));
                } catch (TransformException e) {
                    addError(e);
                }
            });
        } catch (BackingStoreException e) {
            addError(e.getCause());
        }
    }

    /**
     * Updates an array of maximal length of string representations in the given columns.
     * The {@code lenghts} array is updated in-place.
     */
    private static void updateMaximalLengths(final int[] lengths, final String[] columns) {
        for (int i = Math.min(lengths.length, columns.length); --i >= 0;) {
            final int length = columns[i].length();
            if (length > lengths[i]) {
                lengths[i] = length;
            }
        }
    }

    /**
     * Appends spaces in front of the given columns in order to have the specified lengths.
     */
    private static void rightAlign(final int[] lengths, final String[] columns) {
        for (int i = Math.min(lengths.length, columns.length); --i >= 0;) {
            final String column = columns[i];
            final int more = lengths[i] - column.length();
            if (more > 0) {
                columns[i] = CharSequences.spaces(more) + columns[i];
            }
        }
    }

    /**
     * Formats the main body of the Tile Matrix Set (<abbr>TMS</abbr>) representation.
     * This is formatted as a table.
     *
     * @param  matrices  the Tile Matrix Set (<abbr>TMS</abbr>) to format.
     * @param  buffer    where to format the main body.
     * @throws IOException should never happen but handled by the caller for convenience.
     */
    private void formatTable(final TileMatrixSet matrices, final StringBuilder buffer) throws IOException {
        final var rows = new ArrayList<Row>();
        int crsDimension = 0, gridDimension = 0, sizeDimension = 0;
        for (final TileMatrix matrix : matrices.getTileMatrices().values()) try {
            final var row = new Row(matrix, integerFormat);
            crsDimension  = Math.max(crsDimension,  row.formattedResolution.length);
            gridDimension = Math.max(gridDimension, row.tileCount.length);
            sizeDimension = Math.max(sizeDimension, row.tileSize.length);
            rows.add(row);  // Add only on success.
        } catch (RuntimeException e) {
            addError(e);
        }
        /*
         * Find the number of fraction digits to use for showing the resolution.
         */
        final var values  = new double[rows.size()];
        final var formats = new NumberFormat[crsDimension];
        for (int i=0; i<crsDimension; i++) {
            for (int j=0; j<values.length; j++) {
                values[j] = rows.get(j).resolution[i];
            }
            final NumberFormat format = (locale != null)
                    ? NumberFormat.getNumberInstance(locale)
                    : NumberFormat.getNumberInstance();
            final int n = Numerics.suggestFractionDigits(values);
            format.setMinimumFractionDigits(n);
            format.setMaximumFractionDigits(n);
            formats[i] = format;
        }
        /*
         * At this point, all values have been formatted as character strings.
         * Compute the maximum lengths in each column in order to align the values.
         */
        final int[] resolutionLengths = new int[crsDimension];
        final int[]  tileCountLengths = new int[gridDimension];
        final int[]   tileSizeLengths = new int[sizeDimension];
        for (final Row row : rows) {
            row.formatResolutions(formats);
            updateMaximalLengths(resolutionLengths, row.formattedResolution);
            updateMaximalLengths(tileCountLengths,  row.tileCount);
            updateMaximalLengths(tileSizeLengths,   row.tileSize);
        }
        for (final Row row : rows) {
            rightAlign(resolutionLengths, row.formattedResolution);
            rightAlign(tileCountLengths,  row.tileCount);
            rightAlign(tileSizeLengths,   row.tileSize);
        }
        /*
         * All data are prepared. Write the table.
         */
        final var table = new TableAppender(buffer);
        table.appendHorizontalSeparator();
        table.append(vocabulary.getString(Vocabulary.Keys.Identifier)).nextColumn();
        if  (crsDimension != 0) table.append(vocabulary.getString(Vocabulary.Keys.Resolution)).nextColumn();
        if (gridDimension != 0) table.append(vocabulary.getString(Vocabulary.Keys.TileCount)) .nextColumn();
        if (sizeDimension != 0) table.append(vocabulary.getString(Vocabulary.Keys.TileSize))  .nextColumn();
        table.appendHorizontalSeparator();
        for (final Row row : rows) {
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
            table.append(row.identifier).nextColumn();
            table.setCellAlignment(TableAppender.ALIGN_RIGHT);
            if  (crsDimension != 0) table.append(String.join(" × ", row.formattedResolution)).nextColumn();
            if (gridDimension != 0) table.append(String.join(" × ", row.tileCount)) .nextColumn();
            if (sizeDimension != 0) table.append(String.join(" × ", row.tileSize))  .nextColumn();
            table.nextLine();
        }
        table.appendHorizontalSeparator();
        table.flush();
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
}
