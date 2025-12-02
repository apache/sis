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
package org.apache.sis.storage.esri;

import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.awt.image.RenderedImage;
import java.awt.image.DataBufferFloat;
import java.awt.image.BandedSampleModel;
import java.awt.image.WritableRaster;
import org.opengis.metadata.Metadata;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.math.Statistics;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.coverage.internal.shared.RangeArgument;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.resources.Errors;


/**
 * Data store implementation for ESRI ASCII grid format.
 * This is a very simple format for reading and writing single-banded raster data.
 * As the "ASCII" name implies, files are text files in US-ASCII character encoding
 * no matter what the {@link org.apache.sis.setup.OptionKey#ENCODING} value is,
 * and numbers are parsed or formatted according the US locale no matter
 * what the {@link org.apache.sis.setup.OptionKey#LOCALE} value is.
 *
 * <p>ASCII grid files contains a header before the actual data.
 * The header contains (<var>key</var> <var>value</var>) pairs,
 * one pair per line and using spaces as separator between keys and values.
 * The valid keys are listed in the table below
 * (note that some of them are extensions to the ESRI ASCII Grid format).</p>
 *
 * <table class="sis">
 *   <caption>Recognized keywords in ASCII Grid header</caption>
 *   <tr>
 *     <th>Keyword</th>
 *     <th>Value type</th>
 *     <th>Obligation</th>
 *   </tr><tr>
 *     <td>{@code NCOLS}</td>
 *     <td>{@link java.lang.Integer}</td>
 *     <td>Mandatory</td>
 *   </tr><tr>
 *     <td>{@code NROWS}</td>
 *     <td>{@link java.lang.Integer}</td>
 *     <td>Mandatory</td>
 *   </tr><tr>
 *     <td>{@code XLLCORNER} or {@code XLLCENTER}</td>
 *     <td>{@link java.lang.Double}</td>
 *     <td>Mandatory</td>
 *   </tr><tr>
 *     <td>{@code YLLCORNER} or {@code YLLCENTER}</td>
 *     <td>{@link java.lang.Double}</td>
 *     <td>Mandatory</td>
 *   </tr><tr>
 *     <td>{@code CELLSIZE}</td>
 *     <td>{@link java.lang.Double}</td>
 *     <td>Mandatory, unless an alternative below is present</td>
 *   </tr><tr>
 *     <td>{@code XCELLSIZE} and {@code YCELLSIZE}</td>
 *     <td>{@link java.lang.Double}</td>
 *     <td>Non-standard alternative to {@code CELLSIZE}</td>
 *   </tr><tr>
 *     <td>{@code XDIM} and {@code YDIM}</td>
 *     <td>{@link java.lang.Double}</td>
 *     <td>Non-standard alternative to {@code CELLSIZE}</td>
 *   </tr><tr>
 *     <td>{@code DX} and {@code DY}</td>
 *     <td>{@link java.lang.Double}</td>
 *     <td>Non-standard alternative to {@code CELLSIZE}</td>
 *   </tr><tr>
 *     <td>{@code NODATA_VALUE}</td>
 *     <td>{@link java.lang.Double}</td>
 *     <td>Optional</td>
 *   </tr>
 * </table>
 *
 * <h2>Extensions</h2>
 * The implementation in this package adds the following extensions
 * (some of them are taken from GDAL):
 *
 * <ul class="verbose">
 *   <li>Coordinate reference system specified by auxiliary {@code *.prj} file.
 *       If the format is WKT 1, the GDAL variant is used (that variant differs from
 *       the OGC 01-009 standard in their interpretation of units of measurement).</li>
 *   <li>{@code XCELLSIZE} and {@code YCELLSIZE} parameters in the header are used
 *       instead of {@code CELLSIZE} if the pixels are non-square.</li>
 *   <li>Lines in the header starting with {@code '#'} are ignored as comment lines.</li>
 * </ul>
 *
 * <h2>Possible evolutions</h2>
 * If we allow subclasses in a future version, we could add a {@code processHeader(Map)} method
 * that subclasses can override for processing their own (<var>key</var>, <var>value</var>) pairs
 * or for modifying the values of existing pairs.
 *
 * <h2>Limitations</h2>
 * Current implementation loads and caches the full image no matter the subregion or subsampling
 * specified to the {@code read(…)} method. The image is loaded by {@link #getSampleDimensions()}
 * call too, because there is no other way to build a reliable sample dimension.
 * Even the data type cannot be determined for sure without loading the full image.
 * Loading the full image is reasonable if ASCII Grid files contain only small images,
 * which is usually the case given how inefficient this format is.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class AsciiGridStore extends RasterStore {
    /**
     * Keys of elements expected in the header. Must be in upper-case letters.
     */
    static final String
            XLLCORNER = "XLLCORNER",  YLLCORNER    = "YLLCORNER",
            XLLCENTER = "XLLCENTER",  YLLCENTER    = "YLLCENTER",
            CELLSIZE  = "CELLSIZE",   NODATA_VALUE = "NODATA_VALUE";

    /**
     * Alternatives names for {@value #CELLSIZE} when the pixels are not squares.
     * Those names are not part of the format defined by ESRI.
     * Various implementations use different names.
     *
     * <p>Names at even indices are for the <var>x</var> axis
     * and names at odd indices are for the <var>y</var> axis.</p>
     */
    static final String[] CELLSIZES = {
        "XCELLSIZE", "YCELLSIZE",
        RawRasterStore.XDIM, RawRasterStore.YDIM,
        "DX", "DY"
    };

    /**
     * The default no-data value. This is part of the ASCII Grid format specification.
     */
    private static final double DEFAULT_NODATA = -9999;

    /**
     * The object to use for reading data, or {@code null} if the channel has been closed.
     * Note that a null value does not necessarily means that the store is closed, because
     * it may have finished to read fully the {@linkplain #coverage}.
     */
    private volatile CharactersView input;

    /**
     * The {@code NCOLS} and {@code NROWS} attributes read from the header.
     * Those values are valid only if {@link #gridGeometry} is non-null.
     */
    private int width, height;

    /**
     * The {@link #nodataValue} as a text. This is useful when the fill value
     * cannot be parsed as a {@code double} value, for example {@code "NULL"},
     * {@code "N/A"}, {@code "NA"}, {@code "mv"}, {@code "!"} or {@code "-"}.
     */
    private String nodataText;

    /**
     * The image size together with the "grid to CRS" transform.
     * This is also used as a flag for checking whether the {@code "*.prj"} file and the header have been read.
     */
    private GridGeometry gridGeometry;

    /**
     * The full coverage, read when first requested then cached.
     * We cache the full coverage on the assumption that the
     * ASCII Grid format is not used for very large images.
     */
    private GridCoverage coverage;

    /**
     * Creates a new ASCII Grid store from the given file, URL or stream.
     * This constructor opens the file, possibly creating it if the {@code connector} contains an
     * option like {@link java.nio.file.StandardOpenOption#CREATE}, but does not try to read it now.
     * It is possible to open an empty file and have {@link WritableStore} to write in it later.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @param  readOnly   whether to fail if the channel cannot be opened at least in read mode.
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    AsciiGridStore(final AsciiGridStoreProvider provider, final StorageConnector connector, final boolean readOnly)
            throws DataStoreException
    {
        super(provider, connector);
        final ChannelDataInput channel;
        if (readOnly) {
            channel = connector.commit(ChannelDataInput.class, AsciiGridStoreProvider.NAME);
        } else {
            channel = connector.getStorageAs(ChannelDataInput.class);
            if (channel != null) {
                connector.closeAllExcept(channel);
            }
        }
        if (channel != null) {
            input = new CharactersView(channel, channel.buffer);
        }
    }

    /**
     * Returns whether this store can read or write. If this store cannot write,
     * then we can close the {@linkplain #input} channel as soon as the coverage
     * has been fully read. Otherwise we need to keep it open.
     *
     * @param  write  {@code false} for testing read capability, or {@code true} for testing write capability.
     */
    boolean canReadOrWrite(final boolean write) {
        return !write && (input != null);
    }

    /**
     * Reads the {@code "*.prj"} file and the header if not already done.
     * This method does nothing if the data store is already initialized.
     * After a successful return, {@link #gridGeometry} is guaranteed non-null.
     *
     * <p>Note: we don't do this initialization in the constructor
     * for giving a chance for users to register listeners first.</p>
     */
    private void readHeader() throws DataStoreException {
        if (gridGeometry == null) try {
            final Map<String,String> header = input().readHeader();
            final Matrix3 gridToCRS = new Matrix3();
            PixelInCell anchor = PixelInCell.CELL_CORNER;
            String key = null;      // Used for error message if an exception is thrown.
            try {
                width  = Integer.parseInt(getHeaderValue(header, key = NCOLS));
                height = Integer.parseInt(getHeaderValue(header, key = NROWS));
                /*
                 * The ESRI ASCII Grid format has only a "CELLSIZE" property for both axes.
                 * The "DX" and "DY" properties are GDAL extensions and considered optional.
                 * If the de-facto standard "CELLSIZE" property exists, "DX" and "DY" will
                 * be considered unexpected.
                 */
                String value = header.remove(key = CELLSIZE);
cellsize:       if (value != null) {
                    gridToCRS.m11 = -(gridToCRS.m00 = Double.parseDouble(value));
                } else {
                    int def = 0;
                    for (int i=0; i < CELLSIZES.length;) {
                        value = header.remove(key = CELLSIZES[i++]); if (value != null) {gridToCRS.m00 =  Double.parseDouble(value); def |= 1;}
                        value = header.remove(key = CELLSIZES[i++]); if (value != null) {gridToCRS.m11 = -Double.parseDouble(value); def |= 2;}
                        if (def == 3) break cellsize;
                    }
                    // Report "CELLSIZE" as the missing property because it is the de-facto standard one.
                    throw new DataStoreContentException(messageForProperty(Errors.Keys.MissingValueForProperty_2, CELLSIZE));
                }
                /*
                 * Lower-left coordinates is specified either by CENTER or CORNER property.
                 * If both are missing, the error message reports that CORNER is missing.
                 */
                value = header.remove(key = XLLCENTER);
                final boolean xCenter = (value != null);
                if (!xCenter) {
                    value = getHeaderValue(header, key = XLLCORNER);
                }
                gridToCRS.m02 = Double.parseDouble(value);
                value = header.remove(key = YLLCENTER);
                final boolean yCenter = (value != null);
                if (!yCenter) {
                    value = getHeaderValue(header, key = YLLCORNER);
                }
                gridToCRS.m12 = Double.parseDouble(value) - gridToCRS.m11 * height;
                if (xCenter & yCenter) {
                    anchor = PixelInCell.CELL_CENTER;
                } else if (xCenter != yCenter) {
                    gridToCRS.convertBefore(xCenter ? 0 : 1, null, -0.5);
                }
                /*
                 * "No data" value is an optional property. Default value is NaN.
                 * This reader accepts a value both as text and as a floating point.
                 * The intent is to accept unparsable texts such as "NULL".
                 */
                nodataText = header.remove(key = NODATA_VALUE);
                if (nodataText != null) try {
                    nodataValue = Double.parseDouble(nodataText);
                } catch (NumberFormatException e) {
                    nodataValue = Double.NaN;
                    listeners.warning(messageForProperty(Errors.Keys.IllegalPropertyValue_2, key), e);
                } else {
                    nodataValue = DEFAULT_NODATA;
                    nodataText  = "null";         // "NaN" is already understood by `parseDouble(String)`.
                }
            } catch (NumberFormatException e) {
                throw new DataStoreContentException(messageForProperty(Errors.Keys.IllegalPropertyValue_2, key), e);
            }
            /*
             * Read the auxiliary PRJ file after we finished parsing the header file.
             * A future version could skip this step if we add a non-standard "CRS" property in the header.
             */
            readPRJ(AsciiGridStore.class, "getGridGeometry");
            gridGeometry = new GridGeometry(new GridExtent(width, height), anchor, MathTransforms.linear(gridToCRS), crs);
            /*
             * If there is any unprocessed properties, log a warning about them.
             * We list all properties in a single message.
             */
            if (!header.isEmpty()) {
                listeners.warning(messageForProperty(Errors.Keys.UnexpectedProperty_2, String.join(", ", header.keySet())));
            }
        } catch (DataStoreException e) {
            closeOnError(e);
            throw e;
        } catch (Exception e) {
            closeOnError(e);
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns the error message for an exception or log record.
     * Invoke only in contexts where {@link #input} is known to be non-null.
     *
     * @param  rk   {@link Errors.Keys#IllegalPropertyValue_2} or {@link Errors.Keys#MissingValueForProperty_2}.
     * @param  key  key of the header property which was requested.
     * @return the message to use in the exception to be thrown or the warning to be logged.
     */
    private String messageForProperty(final short rk, final String key) {
        return Errors.forLocale(getLocale()).getString(rk, input.input.filename, key);
    }

    /**
     * Gets a value from the header map and ensures that it is non-null.
     * The entry is removed from the {@code header} map for making easy
     * to see if there is any unknown key left.
     *
     * @param  header  map of (key, value) pairs from the header.
     * @param  key     the name of the properties to get.
     * @return the value, guaranteed to be non-null.
     * @throws DataStoreException if the value was null.
     */
    private String getHeaderValue(final Map<String,String> header, final String key) throws DataStoreException {
        final String value = header.remove(key);
        if (value == null) {
            throw new DataStoreContentException(messageForProperty(Errors.Keys.MissingValueForProperty_2, key));
        }
        return value;
    }

    /**
     * Returns the metadata associated to the ASII grid file.
     * The returned object contains only the metadata that can be computed without reading the whole image.
     *
     * @return the metadata associated to the ASCII grid file.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            createMetadata(AsciiGridStoreProvider.NAME, "ASCGRD");
        }
        return metadata;
    }

    /**
     * Returns the valid extent of grid coordinates together with the conversion from those grid
     * coordinates to real world coordinates.
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public synchronized GridGeometry getGridGeometry() throws DataStoreException {
        readHeader();
        return gridGeometry;
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     * ASCII Grid files always contain a single band.
     *
     * <p>In current implementation, fetching the sample dimension requires loading the full coverage because
     * the ASCII Grid format provides no way to infer a reasonable {@code SampleDimension} from only the header.
     * Even determining the type (integer or floating point values) requires parsing all values.</p>
     *
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return read(null, null).getSampleDimensions();
    }

    /**
     * Loads the data if not already done and closes the channel if read-only.
     * In current implementation the image is always fully loaded and cached.
     * The given domain is ignored. We do that in order to have determinist
     * and stable values for the range of sample values and for the data type.
     * Loading the full image is reasonable if ASCII Grid files contain only small images,
     * which is usually the case given how inefficient this format is.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  shall be either {@code null} or an array containing only 0.
     * @return the grid coverage for the specified domain.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public synchronized GridCoverage read(final GridGeometry domain, final int... ranges) throws DataStoreException {
        final RangeArgument bands = RangeArgument.validate(1, ranges, listeners);
        if (coverage == null) try {
            readHeader();
            final CharactersView view = input();
            final String filename = view.input.filename;
            final Statistics stats = new Statistics(filename);
            final float[] data = new float[width * height];
            for (int i=0; i < data.length; i++) {
                final String token = view.readToken();
                double value;
                try {
                    value = Double.parseDouble(token);
                    if (value == nodataValue) {
                        value = Double.NaN;
                    }
                } catch (NumberFormatException e) {
                    if (token.equalsIgnoreCase(nodataText)) {
                        value = Double.NaN;
                    } else {
                        throw new DataStoreContentException(Resources.forLocale(getLocale()).getString(
                                Resources.Keys.CanNotReadPixel_3, i % width, i / width, filename), e);
                    }
                }
                data[i] = (float) value;
                stats.accept(value);        // Need to invoke even for NaN values (because we count them).
            }
            /*
             * At this point we finished to read the full image. Close the channel now and build the sample dimension.
             * We add a category for the NODATA_VALUE even if this value does not appear anymore in the `data` array
             * (since we replaced it by NaN on-the-fly) because this information is needed by `WritableStore`.
             *
             * TODO: a future version could try to convert the image to integer values.
             * In this case only we may need to declare the NODATA_VALUE.
             */
            if (!canReadOrWrite(true)) {
                input = null;
                view.input.channel.close();
            }
            final BandedSampleModel sm = new BandedSampleModel(DataBufferFloat.TYPE_FLOAT, width, height, 1);
            loadBandDescriptions(filename, sm, stats);
            /*
             * Build the coverage last, because a non-null `coverage` field
             * is used for meaning that everything succeed.
             */
            final DataBufferFloat buffer = new DataBufferFloat(data, data.length);
            final WritableRaster  raster = WritableRaster.createWritableRaster(sm, buffer, null);
            coverage = createCoverage(gridGeometry, bands, raster, stats);
        } catch (DataStoreException e) {
            closeOnError(e);
            throw e;
        } catch (Exception e) {
            closeOnError(e);
            throw new DataStoreException(e);
        }
        return coverage;
    }

    /**
     * Replaces all data by the given coverage.
     * This is used for write operations only.
     *
     * @param  replacement  the new coverage.
     * @param  data         the image wrapped by the given coverage.
     * @param  band         index of the band to write (usually 0).
     * @return the "no data" value, or {@link Double#NaN} if none.
     */
    final Number setCoverage(final GridCoverage replacement, final RenderedImage data, final int band) {
        coverage     = replacement;
        gridGeometry = replacement.getGridGeometry();
        crs          = gridGeometry.isDefined(GridGeometry.CRS) ? gridGeometry.getCoordinateReferenceSystem() : null;
        width        = data.getWidth();
        height       = data.getHeight();
        metadata     = null;
        nodataText   = "null";
        nodataValue  = Double.NaN;
        final SampleDimension sd = replacement.getSampleDimensions().get(band);
        final NumberRange<?> range = sd.getSampleRange().orElse(null);
        if (range != null) {
            try {
                for (final Number nodata : sd.forConvertedValues(false).getNoDataValues()) {
                    if (!range.containsAny(nodata)) {
                        nodataValue = nodata.doubleValue();
                        return nodata;
                    }
                }
            } catch (IllegalStateException e) {
                listeners.warning(e);
            }
            if (range.containsAny(DEFAULT_NODATA)) {
                nodataValue = DEFAULT_NODATA;
            }
        }
        return nodataValue;
    }

    /**
     * Returns the input if it has not been closed.
     */
    final CharactersView input() throws DataStoreException {
        final CharactersView in = input;
        if (in == null) {
            throw new DataStoreClosedException(getLocale(), AsciiGridStoreProvider.NAME, StandardOpenOption.READ);
        }
        return in;
    }

    /**
     * Closes this data store and releases any underlying resources.
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public void close() throws DataStoreException {
        try {
            listeners.close();                  // Should never fail.
            final CharactersView view = input;
            if (view != null) {
                view.input.channel.close();
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        } finally {
            synchronized (this) {
                super.close();                  // Clear more fields. Never fail.
                gridGeometry = null;
                coverage     = null;
                input        = null;
            }
        }
    }

    /**
     * Closes this data store after an unrecoverable error occurred.
     * The caller is expected to throw the given exception after this method call.
     */
    final void closeOnError(final Throwable e) {
        try {
            close();
        } catch (Throwable s) {
            e.addSuppressed(s);
        }
    }
}
