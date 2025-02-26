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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import static java.lang.Math.multiplyExact;
import org.opengis.metadata.Metadata;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.privy.RangeArgument;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.AuxiliaryContent;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.image.DataType;
import static org.apache.sis.util.privy.Numerics.wholeDiv;
import static org.apache.sis.pending.jdk.JDK18.ceilDiv;


/**
 * Data store implementation for BIL, BIP, and BSQ raster files.
 * Sample values are provided in a raw binary files, without compression.
 * Information about image layout is provided in a separated text files.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class RawRasterStore extends RasterStore {
    /**
     * Keyword for the number of bands.
     * Default value is 1.
     */
    private static final String NBANDS = "NBANDS";

    /**
     * Keyword for the number of bits per sample: 1, 4, 8, 16, 32.
     * Default value is {@value Byte#SIZE}.
     */
    private static final String NBITS = "NBITS";

    /**
     * Keyword for the type of integers (signed or unsigned).
     * Value can be {@code SIGNEDINT} for signed integers.
     * Default value is unsigned integers.
     */
    private static final String PIXELTYPE = "PIXELTYPE";

    /**
     * Keyword for the byte order: I = Intel; M = Motorola.
     * Default value is the byte order of host machine.
     */
    private static final String BYTEORDER = "BYTEORDER";

    /**
     * Keyword for the sample model: BIL, BIP or BSQ.
     * Default value is {@link RawRasterLayout#BIL}.
     */
    private static final String LAYOUT = "LAYOUT";

    /**
     * Keyword for the offset in the stream of the first byte to read.
     * Default value is 0.
     */
    private static final String SKIPBYTES = "SKIPBYTES";

    /**
     * Keyword for the number of bytes per band per row.
     * This is used only with {@link RawRasterLayout#BIL}.
     * Default value is (NCOLS x NBITS) / 8 rounded up.
     */
    private static final String BANDROWBYTES = "BANDROWBYTES";

    /**
     * Keyword for the total number of bytes in a row.
     * Default value depends on the layout:
     *
     * <ul>
     *   <li>{@link RawRasterLayout#BIL}: (NBANDS x BANDROWBYTES)</li>
     *   <li>{@link RawRasterLayout#BIP}: (BANDS x NCOLS x NBITS) / 8 rounded up</li>
     * </ul>
     */
    private static final String TOTALROWBYTES = "TOTALROWBYTES";

    /**
     * Number of bytes to skip between band.
     * This is used only with {@link RawRasterLayout#BSQ}.
     * Default value is 0.
     *
     * @see RawRasterReader#bandGapBytes
     */
    private static final String BANDGAPBYTES = "BANDGAPBYTES";

    /**
     * Keyword for the x-axis coordinate of the center of the upper left pixel.
     * Default value is 0.
     */
    private static final String ULXMAP = "ULXMAP";

    /**
     * Keyword for the y-axis coordinate of the center of the upper left pixel.
     * Default value is NROWS - 1.
     */
    private static final String ULYMAP = "ULYMAP";

    /**
     * Keyword for the pixel size in the x-axis dimension.
     * Default value is 1.
     */
    static final String XDIM = "XDIM";

    /**
     * Keyword for the pixel size in the y-axis dimension.
     * Default value is 1.
     */
    static final String YDIM = "YDIM";

    /**
     * Keyword for the value to replace by NaN.
     * This is not documented in the ESRI specification but used in practice.
     *
     * @see #nodataValue
     */
    private static final String NODATA = "NODATA";

    /**
     * The "cell center" versus "cell corner" interpretation of translation coefficients.
     * The ESRI specification said that the coefficients map to pixel center.
     */
    private static final PixelInCell CELL_ANCHOR = PixelInCell.CELL_CENTER;

    /**
     * The object to use for reading data, or {@code null} if the channel has been closed.
     */
    private volatile ChannelDataInput input;

    /**
     * Helper method for reading a rectangular region from the {@linkplain #input} stream.
     * This is created when the header is parsed, because its depends on the type of data.
     * A non-null value is used as a sentinel value meaning that the header has been read.
     */
    private RawRasterReader reader;

    /**
     * Creates a new raw raster store from the given file or URL.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (file, URL, <i>etc</i>).
     * @throws DataStoreException if an error occurred while closing unused streams.
     */
    RawRasterStore(final RawRasterStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        input = connector.commit(ChannelDataInput.class, RawRasterStoreProvider.NAME);
    }

    /**
     * Returns the {@linkplain #location} as a {@code Path} component together with auxiliary files.
     *
     * @return the main file and auxiliary files as paths, or an empty value if unknown.
     * @throws DataStoreException if the URI cannot be converted to a {@link Path}.
     */
    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return listComponentFiles(RawRasterStoreProvider.HDR, PRJ, STX, CLR);
    }

    /**
     * Returns the metadata associated to the raw binary file.
     *
     * @return the metadata associated to the raw binary.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            getSampleDimensions();      // Force loading of statistics file if not already done.
            createMetadata(RawRasterStoreProvider.NAME, "RAWGRD");
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
        if (reader == null) try {
            readHeader();
        } catch (URISyntaxException | IOException e) {
            throw new DataStoreException(canNotRead(), e);
        } catch (RuntimeException e) {
            throw new DataStoreContentException(canNotRead(), e);
        }
        return reader.gridGeometry;
    }

    /**
     * Returns the ranges of sample values.
     *
     * @return ranges of sample values.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public synchronized List<SampleDimension> getSampleDimensions() throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ChannelDataInput input = this.input;
        List<SampleDimension> sampleDimensions = super.getSampleDimensions();
        if (sampleDimensions == null) try {
            if (reader == null) {
                readHeader();
            }
            loadBandDescriptions(input.filename, reader.layout);
            sampleDimensions = super.getSampleDimensions();
        } catch (URISyntaxException | IOException e) {
            throw new DataStoreException(canNotRead(), e);
        } catch (RuntimeException e) {
            throw new DataStoreContentException(canNotRead(), e);
        }
        return sampleDimensions;
    }

    /**
     * Returns localized resources for warnings an error messages.
     */
    private Errors errors() {
        return Errors.forLocale(getLocale());
    }

    /**
     * Returns the exception to throw for a missing property in the header file.
     *
     * @param  header   the header to parse.
     * @param  keyword  the missing keyword.
     * @return the exception to throw.
     */
    private DataStoreContentException missingProperty(final AuxiliaryContent header, final String keyword) {
        return new DataStoreContentException(errors().getString(
                Errors.Keys.MissingValueForProperty_2, header.getFilename(), keyword));
    }

    /**
     * Sends a warning if a property was specified in the header file but has been ignored by this data store.
     *
     * @param  keyword  keyword of the potentially ignored property.
     * @param  value    the specified value, or 0 if it is the default value.
     */
    private void ignoredProperty(final String keyword, final int value) {
        if (value != 0) {
            listeners.warning(Messages.forLocale(getLocale()).getString(Messages.Keys.IgnoredPropertyValue_1, keyword));
        }
    }

    /**
     * Returns the index of {@code value} in the {@code alternatives} array, or -1 if not found.
     * The comparison ignore cases. If the value is not found in the array, a warning message is emitted.
     *
     * @param  keyword       the keyword (used in case a warning message is emitted).
     * @param  value         the value to search.
     * @param  alternatives  valid values.
     * @return index of {@code value} in the {@code alternatives} array, or -1 if not found.
     */
    private int indexOf(final String keyword, final String value, final String... alternatives) {
        for (int i=0; i < alternatives.length; i++) {
            if (value.equalsIgnoreCase(alternatives[i])) {
                return i;
            }
        }
        listeners.warning(errors().getString(Errors.Keys.IllegalPropertyValue_2, keyword, value));
        return -1;
    }

    /**
     * Parses the given string as a strictly positive integer.
     *
     * @param  keyword  the keyword (used in case a warning message is emitted).
     * @param  value    the value to parse as an unsigned integer.
     * @return the parsed value, guaranteed greater than zero.
     */
    private int parseStrictlyPositive(final String keyword, final String value) throws DataStoreContentException {
        final int n = Integer.parseInt(value);
        if (n > 0) return n;
        throw new DataStoreContentException(errors().getString(Errors.Keys.ValueNotGreaterThanZero_2, keyword, value));
    }

    /**
     * Reads the {@code "*.hdr"} and {@code "*.prj"} files.
     * After a successful return, {@link #reader} is guaranteed non-null.
     *
     * <p>Note: we don't do this initialization in the constructor
     * for giving a chance for users to register listeners first.</p>
     *
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     * @throws IOException if the auxiliary file cannot be found or read.
     * @throws DataStoreException if the auxiliary file cannot be parsed.
     * @throws RasterFormatException if the number of bits or the signed/unsigned property is invalid.
     * @throws ArithmeticException if image size of pixel/line/band stride is too large.
     * @throws IllegalArgumentException if {@link SampleModel} constructor rejects some argument values.
     */
    private void readHeader() throws URISyntaxException, IOException, DataStoreException {
        assert Thread.holdsLock(this);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ChannelDataInput input = this.input;
        if (input == null) {
            throw new DataStoreClosedException(canNotRead());
        }
        int     nrows          = 0;
        int     ncols          = 0;
        int     nbands         = 1;
        int     nbits          = Byte.SIZE;
        boolean signed         = false;
        long    skipBytes      = 0;
        int     bandRowBytes   = 0;
        int     totalRowBytes  = 0;
        int     bandGapBytes   = 0;
        double  ulxmap         = 0;
        double  ulymap         = 0;
        double  xdim           = 1;
        double  ydim           = 1;
        int     geomask        = 0;   // Mask telling whether ulxmap, ulymap, xdim, ydim were specified (in that order).
        RawRasterLayout layout = RawRasterLayout.BIL;
        ByteOrder byteOrder    = ByteOrder.nativeOrder();
        final AuxiliaryContent header = readAuxiliaryFile(RawRasterStoreProvider.HDR, false);
        if (header == null) {
            throw new DataStoreException(cannotReadAuxiliaryFile(RawRasterStoreProvider.HDR));
        }
        for (CharSequence line : CharSequences.splitOnEOL(header)) {
            final int length   = line.length();
            final int keyStart = CharSequences.skipLeadingWhitespaces(line, 0, length);
            final int keyEnd   = CharSequences.indexOf(line, ' ', keyStart, length);
            if (keyStart >= 0) {
                // Note: text after value is considered comment according ESRI specification.
                int valStart = CharSequences.skipLeadingWhitespaces(line, keyEnd, length);
                int valEnd   = CharSequences.indexOf(line, ' ', valStart, length);
                if (valEnd < 0) {
                    valEnd = CharSequences.skipTrailingWhitespaces(line, valStart, length);
                    if (valEnd <= valStart) continue;
                }
                final String keyword = line.subSequence(keyStart, keyEnd).toString();
                final String value   = line.subSequence(valStart, valEnd).toString();
                try {
                    switch (keyword.toUpperCase(Locale.US)) {
                        case NROWS:         nrows         = parseStrictlyPositive(keyword, value); break;
                        case NCOLS:         ncols         = parseStrictlyPositive(keyword, value); break;
                        case NBANDS:        nbands        = parseStrictlyPositive(keyword, value); break;
                        case NBITS:         nbits         = parseStrictlyPositive(keyword, value); break;
                        case BANDROWBYTES:  bandRowBytes  = parseStrictlyPositive(keyword, value); break;
                        case TOTALROWBYTES: totalRowBytes = parseStrictlyPositive(keyword, value); break;
                        case BANDGAPBYTES:  bandGapBytes  = Integer.parseInt(value); break;
                        case SKIPBYTES:     skipBytes     = Long.parseLong(value); break;
                        case ULXMAP:        ulxmap        = Double.parseDouble(value); geomask |= 1; break;
                        case ULYMAP:        ulymap        = Double.parseDouble(value); geomask |= 2; break;
                        case XDIM:          xdim          = Double.parseDouble(value); geomask |= 4; break;
                        case YDIM:          ydim          = Double.parseDouble(value); geomask |= 8; break;
                        case NODATA:        nodataValue   = Double.parseDouble(value); break;
                        case PIXELTYPE:     signed        = indexOf(keyword, value, "SIGNED", "SIGNEDINT") >= 0; break;
                        case LAYOUT:        layout        = RawRasterLayout.valueOf(value.toUpperCase(Locale.US)); break;
                        case BYTEORDER: {
                            switch (indexOf(keyword, value, "I", "M")) {
                                case 0:  byteOrder = ByteOrder.LITTLE_ENDIAN; break;
                                case 1:  byteOrder = ByteOrder.BIG_ENDIAN; break;
                                default: throw new DataStoreContentException(errors().getString(
                                            Errors.Keys.IllegalPropertyValue_2, keyword, value));
                            }
                        }
                        /*
                         * No default. The specification said that any line in the file that
                         * does not begin with a keyword is treated as a comment and ignored.
                         */
                    }
                } catch (IllegalArgumentException e) {      // Include NumberFormatException.
                    throw new DataStoreContentException(errors().getString(
                            Errors.Keys.IllegalPropertyValue_2, keyword, value), e);
                }
            }
        }
        input.buffer.order(byteOrder);
        /*
         * Validate parameters, compute default values then create the grid geometry.
         * If one of ULXMAP or ULYMAP is specified, then both of them shall be specified.
         * If one of XDIM or YDIM is specified, then all of ULXMAP, ULYMAP, XDIM and YDIM shall be specified.
         */
        if (nrows == 0 || ncols == 0) {
            throw missingProperty(header, (nrows == 0) ? NROWS : NCOLS);
        }
        // Invoke following method now because it does argument validation.
        final var dataType = DataType.forNumberOfBits(nbits, false, signed);
        final int bytesPerSample = dataType.bytes();
        switch (geomask) {
            case 0:  ulymap = ncols - 1; break;     // No property specified.
            case 3:  break;                         // ULXMAP and ULYMAP specified.
            case 15: break;                         // ULXMAP, ULYMAP, XDIM and YDIM specified.
            default: {
                final String keyword;
                switch (Integer.lowestOneBit(~geomask)) {
                    case 1:  keyword = ULXMAP; break;
                    case 2:  keyword = ULYMAP; break;
                    case 4:  keyword = XDIM;   break;
                    case 8:  keyword = YDIM;   break;
                    default: keyword = "?";    break;       // Should not happen.
                }
                throw missingProperty(header, keyword);
            }
        }
        readPRJ(RawRasterStore.class, "getGridGeometry");
        final GridGeometry gg = new GridGeometry(new GridExtent(ncols, nrows), CELL_ANCHOR,
                new AffineTransform2D(xdim, 0, 0, -ydim, ulxmap, ulymap), crs);
        /*
         * Create a sample model for the data layout. This block encapsulates all layout information
         * except `skipBytes` and `bandGapBytes`, which need to be taken in account at reading time.
         * Note that there is many ways to create a sample model. For example, a `BandedSampleModel`
         * could store 3 bands in the same array or in 3 different arrays. The choices made in this
         * block must be consistent with the expectations of `read(…)` method implementation.
         */
        SampleModel sampleModel = null;
        final int bt = dataType.toDataBufferType();
        switch (layout) {
            case BIL: {
                ignoredProperty(BANDGAPBYTES, bandGapBytes);
                if (bandRowBytes  == 0)  bandRowBytes = ceilDiv(multiplyExact(ncols, nbits), Byte.SIZE);
                if (totalRowBytes == 0) totalRowBytes = multiplyExact(nbands, bandRowBytes);
                if (bytesPerSample != 0) {
                    final int   bandStride     = wholeDiv(bandRowBytes,  bytesPerSample);
                    final int   scanlineStride = wholeDiv(totalRowBytes, bytesPerSample);
                    final int[] bankIndices    = new int[nbands];
                    final int[] bandOffsets    = new int[nbands];
                    for (int i=1; i<nbands; i++) {
                        bandOffsets[i] = multiplyExact(bandStride, i);
                    }
                    sampleModel = new ComponentSampleModel(bt, ncols, nrows, 1, scanlineStride, bankIndices, bandOffsets);
                }
                break;
            }
            case BIP: {
                ignoredProperty(BANDGAPBYTES, bandGapBytes);
                ignoredProperty(BANDROWBYTES, bandRowBytes);
                if (totalRowBytes == 0) {
                    totalRowBytes = ceilDiv(multiplyExact(multiplyExact(ncols, nbands), nbits), Byte.SIZE);
                }
                if (bytesPerSample != 0) {
                    final int   scanlineStride = wholeDiv(totalRowBytes, bytesPerSample);
                    final int[] bandOffsets    = ArraysExt.range(0, nbands);
                    sampleModel = new PixelInterleavedSampleModel(bt, ncols, nrows, nbands, scanlineStride, bandOffsets);
                }
                break;
            }
            case BSQ: {
                ignoredProperty(BANDROWBYTES, bandRowBytes);
                if (totalRowBytes == 0) {
                    totalRowBytes = ncols;
                }
                if (bytesPerSample != 0) {
                    final int   scanlineStride = wholeDiv(totalRowBytes, bytesPerSample);
                    final int[] bankIndices    = ArraysExt.range(0, nbands);
                    final int[] bandOffsets    = new int[nbands];
                    sampleModel = new BandedSampleModel(bt, ncols, nrows, scanlineStride, bankIndices, bandOffsets);
                }
                break;
            }
            default: throw new AssertionError(layout);
        }
        if (bytesPerSample == 0) {
            if (nbands != 1) {
                throw new DataStoreContentException(errors().getString(Errors.Keys.InconsistentAttribute_2, nbits, NBITS));
            }
            sampleModel = new MultiPixelPackedSampleModel(bt, ncols, nrows, nbits, totalRowBytes, 0);
        }
        /*
         * Prepare the reader as the last step because non-null `reader` field is used
         * as a sentinel value meaning that the initialization has been completed.
         */
        reader = new RawRasterReader(gg, dataType, sampleModel, bandGapBytes, input);
        reader.setOrigin(skipBytes);
    }

    /**
     * Loads the data.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  indices of bands to load.
     * @return the grid coverage for the specified domain.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public synchronized GridCoverage read(GridGeometry domain, final int... ranges) throws DataStoreException {
        try {
            getSampleDimensions();      // Force reading the header and building the list of sample dimensions.
            final RangeArgument  bands  = RangeArgument.validate(reader.layout.getNumBands(), ranges, listeners);
            final WritableRaster raster = reader.read(domain, bands);
            return createCoverage(reader.getEffectiveDomain(), bands, raster, null);
        } catch (IOException e) {
            throw new DataStoreException(canNotRead(), e);
        } catch (RuntimeException e) {
            throw new DataStoreContentException(canNotRead(), e);
        }
    }

    /**
     * Returns an error message saying that the file cannot be read.
     */
    private String canNotRead() {
        return Resources.forLocale(getLocale())
                .getString(Resources.Keys.CanNotReadFile_2, RawRasterStoreProvider.NAME, getDisplayName());
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
            listeners.close();                      // Should never fail.
            final ChannelDataInput input = this.input;
            if (input != null) try {
                input.channel.close();
            } catch (IOException e) {
                throw new DataStoreException(e);
            }
        } finally {
            synchronized (this) {
                input  = null;                      // Cleared first in case of failure.
                reader = null;
                super.close();                      // Clear more fields. Never fail.
            }
        }
    }
}
