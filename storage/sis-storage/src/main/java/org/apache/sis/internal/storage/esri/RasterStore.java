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
package org.apache.sis.internal.storage.esri;

import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.util.Hashtable;
import java.util.logging.Level;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.PRJDataStore;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.storage.RangeArgument;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.math.Statistics;


/**
 * Base class for the implementation of ASCII Grid or raw binary store.
 * This base class manages the reading of following auxiliary files:
 *
 * <ul>
 *   <li>{@code *.stx} for statistics about bands.</li>
 *   <li>{@code *.clr} for the image color map.</li>
 *   <li>{@code *.prj} for the CRS definition.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
abstract class RasterStore extends PRJDataStore implements GridCoverageResource {
    /**
     * Band to make visible if an image contains many bands
     * but a color map is defined for only one band.
     */
    private static final int VISIBLE_BAND = 0;

    /**
     * Keyword for the number of rows in the image.
     */
    static final String NROWS = "NROWS";

    /**
     * Keyword for the number of columns in the image.
     */
    static final String NCOLS = "NCOLS";

    /**
     * The filename extension of {@code "*.stx"} and {@code "*.clr"} files.
     *
     * @see #getComponentFiles()
     */
    private static final String STX = "stx", CLR = "clr";

    /**
     * The color model, created from the {@code "*.clr"} file content when first needed.
     * The color model and sample dimensions are created together because they depend on
     * the same properties.
     */
    private ColorModel colorModel;

    /**
     * The sample dimensions, created from the {@code "*.stx"} file content when first needed.
     * The sample dimensions and color model are created together because they depend on the same properties.
     * This list is unmodifiable.
     *
     * @see #getSampleDimensions()
     */
    private List<SampleDimension> sampleDimensions;

    /**
     * The value to replace by NaN values, or {@link Double#NaN} if none.
     */
    double nodataValue;

    /**
     * The metadata object, or {@code null} if not yet created.
     */
    Metadata metadata;

    /**
     * Creates a new raster store from the given file, URL or stream.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (file, URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    RasterStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        nodataValue = Double.NaN;
        listeners.useReadOnlyEvents();
    }

    /**
     * Returns the {@linkplain #location} as a {@code Path} component together with auxiliary files.
     *
     * @return the main file and auxiliary files as paths, or an empty array if unknown.
     * @throws DataStoreException if the URI can not be converted to a {@link Path}.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return listComponentFiles(PRJ, STX, CLR);
    }

    /**
     * Returns the spatiotemporal extent of the raster file.
     *
     * @return the spatiotemporal resource extent.
     * @throws DataStoreException if an error occurred while computing the envelope.
     * @hidden
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return Optional.ofNullable(getGridGeometry().getEnvelope());
    }

    /**
     * Builds metadata and assigns the result to the {@link #metadata} field.
     *
     * @param  formatName  name of the raster format.
     * @param  formatKey   key of format description in the {@code SpatialMetadata} database.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    final void createMetadata(final String formatName, final String formatKey) throws DataStoreException {
        final GridGeometry gridGeometry = getGridGeometry();        // May cause parsing of header.
        final MetadataBuilder builder = new MetadataBuilder();
        try {
            builder.setPredefinedFormat(formatKey);
        } catch (MetadataStoreException e) {
            builder.addFormatName(formatName);
            listeners.warning(e);
        }
        builder.addResourceScope(ScopeCode.COVERAGE, null);
        builder.addEncoding(encoding, MetadataBuilder.Scope.METADATA);
        builder.addSpatialRepresentation(null, gridGeometry, true);
        try {
            builder.addExtent(gridGeometry.getEnvelope());
        } catch (TransformException e) {
            throw new DataStoreReferencingException(getLocale(), formatName, getDisplayName(), null).initCause(e);
        }
        /*
         * Do not invoke `getSampleDimensions()` because computing sample dimensions without statistics
         * may cause the loading of the full image. Even if `GridCoverage.getSampleDimensions()` exists
         * and could be used opportunistically, we do not use it in order to keep a deterministic behavior
         * (we do not want the metadata to vary depending on the order in which methods are invoked).
         */
        if (sampleDimensions != null) {
            for (final SampleDimension band : sampleDimensions) {
                builder.addNewBand(band);
            }
        }
        addTitleOrIdentifier(builder);
        builder.setISOStandards(false);
        metadata = builder.buildAndFreeze();
    }

    /**
     * Reads the {@code "*.clr"} auxiliary file. Syntax is as below, with one line per color:
     *
     * <pre>value red green blue</pre>
     *
     * The specification said that lines that do not start with a number shall be ignored as comment.
     * Any characters after the fourth number shall also be ignored and can be used as comment.
     *
     * <h4>Limitations</h4>
     * Current implementation requires the data type to be {@link DataBuffer#TYPE_BYTE} or
     * {@link DataBuffer#TYPE_SHORT}. A future version could create scaled color model for
     * floating point values as well.
     *
     * @param  mapSize   minimal size of index color model map. The actual size may be larger.
     * @param  numBands  number of bands in the sample model. Only one of them will be visible.
     * @return the color model, or {@code null} if the file does not contain enough entries.
     * @throws NoSuchFileException if the auxiliary file has not been found (when opened from path).
     * @throws FileNotFoundException if the auxiliary file has not been found (when opened from URL).
     * @throws IOException if another error occurred while opening the stream.
     * @throws NumberFormatException if a number can not be parsed.
     */
    private ColorModel readColorMap(final int dataType, final int mapSize, final int numBands)
            throws DataStoreException, IOException
    {
        final int maxSize;
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:   maxSize = 0xFF;   break;
            case DataBuffer.TYPE_USHORT: maxSize = 0xFFFF; break;
            default: return null;                           // Limitation documented in above javadoc.
        }
        int count = 0;
        long[] indexAndColors = ArraysExt.EMPTY_LONG;       // Index in highest 32 bits, ARGB in lowest 32 bits.
        for (final CharSequence line : CharSequences.splitOnEOL(readAuxiliaryFile(CLR))) {
            final int end   = CharSequences.skipTrailingWhitespaces(line, 0, line.length());
            final int start = CharSequences.skipLeadingWhitespaces(line, 0, end);
            if (start < end && Character.isDigit(Character.codePointAt(line, start))) {
                int column = 0;
                long code = 0;
                for (final CharSequence item : CharSequences.split(line.subSequence(start, end), ' ')) {
                    if (item.length() != 0) {
                        int value = Integer.parseInt(item.toString());
                        if (column == 0) {
                            code = ((long) value) << Integer.SIZE;
                        } else {
                            value = Math.max(0, Math.min(255, value));
                            code |= value << ((3 - column) * Byte.SIZE);
                        }
                        if (++column >= 4) break;
                    }
                }
                if (count >= indexAndColors.length) {
                    indexAndColors = Arrays.copyOf(indexAndColors, Math.max(count*2, 64));
                }
                indexAndColors[count++] = code | 0xFF000000L;
            }
        }
        if (count <= 1) {
            return null;
        }
        /*
         * Sort the color entries in increasing index order. Because we put the value in the highest bits,
         * we can sort the `long` entries directly. If the file contains more entries than what the color
         * map can contains, the last entries are discarded.
         */
        Arrays.sort(indexAndColors, 0, count);
        int[] ARGB = new int[Math.max(mapSize, Math.toIntExact((indexAndColors[count-1] >>> Integer.SIZE) + 1))];
        final int[] colors = new int[2];
        for (int i=1; i<count; i++) {
            final int lower = (int) (indexAndColors[i-1] >>> Integer.SIZE);
            final int upper = (int) (indexAndColors[i  ] >>> Integer.SIZE);
            if (upper >= lower) {
                colors[0] = (int) indexAndColors[i-1];
                colors[1] = (int) indexAndColors[i  ];
                ColorModelFactory.expand(colors, ARGB, lower, upper + 1);
            }
            if (upper > maxSize) {
                ARGB = Arrays.copyOf(ARGB, maxSize + 1);
                break;
            }
        }
        return ColorModelFactory.createIndexColorModel(numBands, VISIBLE_BAND, ARGB, true, -1);
    }

    /**
     * Reads the {@code "*.stx"} auxiliary file. Syntax is as below, with one line per band.
     * Value between {…} are optional and can be skipped with a # sign in place of the number.
     *
     * <pre>band minimum maximum {mean} {std_deviation} {linear_stretch_min} {linear_stretch_max}</pre>
     *
     * The specification said that lines that do not start with a number shall be ignored as comment.
     *
     * @todo Stretch values are not yet stored.
     *
     * @return statistics for each band. Some elements may be null if not specified in the file.
     * @throws NoSuchFileException if the auxiliary file has not been found (when opened from path).
     * @throws FileNotFoundException if the auxiliary file has not been found (when opened from URL).
     * @throws IOException if another error occurred while opening the stream.
     * @throws NumberFormatException if a number can not be parsed.
     */
    private Statistics[] readStatistics(final String name, final SampleModel sm)
            throws DataStoreException, IOException
    {
        final Statistics[] stats = new Statistics[sm.getNumBands()];
        for (final CharSequence line : CharSequences.splitOnEOL(readAuxiliaryFile(STX))) {
            final int end   = CharSequences.skipTrailingWhitespaces(line, 0, line.length());
            final int start = CharSequences.skipLeadingWhitespaces(line, 0, end);
            if (start < end && Character.isDigit(Character.codePointAt(line, start))) {
                int column     = 0;
                int band       = 0;
                double minimum = Double.NaN;
                double maximum = Double.NaN;
                double mean    = Double.NaN;
                double stdev   = Double.NaN;
                for (final CharSequence item : CharSequences.split(line.subSequence(start, end), ' ')) {
                    if (item.length() != 0) {
                        if (column == 0) {
                            band = Integer.parseInt(item.toString());
                        } else if (item.charAt(0) != '#') {
                            final double value = Double.parseDouble(item.toString());
                            switch (column) {
                                case 1: minimum = value; break;
                                case 2: maximum = value; break;
                                case 3: mean    = value; break;
                                case 4: stdev   = value; break;
                            }
                        }
                        column++;
                    }
                }
                if (band >= 1 && band <= stats.length) {
                    final int count = Math.multiplyExact(sm.getWidth(), sm.getHeight());
                    stats[band - 1] = new Statistics(name, 0, count, minimum, maximum, mean, stdev, false);
                }
            }
        }
        return stats;
    }

    /**
     * Loads {@code "*.stx"} and {@code "*.clr"} files if present then builds {@link #sampleDimensions} and
     * {@link #colorModel} from those information. If no color map is found, a grayscale color model is created.
     *
     * @param  name   name to use for the sample dimension, or {@code null} if untitled.
     * @param  sm     the sample model to use for creating a default color model if no {@code "*.clr"} file is found.
     * @param  stats  if the caller collected statistics by itself, those statistics for each band. Otherwise empty.
     * @throws DataStoreException if an error occurred while loading an auxiliary file.
     */
    final void loadBandDescriptions(String name, final SampleModel sm, Statistics... stats) throws DataStoreException {
        final SampleDimension[] bands = new SampleDimension[sm.getNumBands()];
        /*
         * If the "*.stx" file is found, the statistics read from that file will replace the specified one.
         * Otherwise the `stats` parameter will be left unchanged. We read statistics even if a color map
         * overwrite them because we need the minimum/maximum values for building the sample dimensions.
         */
        try {
            stats = readStatistics(name, sm);
        } catch (IOException | NumberFormatException e) {
            canNotReadAuxiliaryFile(STX, e);
        }
        /*
         * Build the sample dimensions and the color model.
         * Some minimum/maximum values will be used as fallback if no statistics were found.
         */
        final int     dataType   = sm.getDataType();
        final boolean isInteger  = ImageUtilities.isIntegerType(dataType);
        final boolean isUnsigned = isInteger && ImageUtilities.isUnsignedType(sm);
        final boolean isRGB      = isInteger && (bands.length == 3 || bands.length == 4);
        final SampleDimension.Builder builder = new SampleDimension.Builder();
        for (int band=0; band < bands.length; band++) {
            double minimum = Double.NaN;
            double maximum = Double.NaN;
            if (band < stats.length) {
                final Statistics s = stats[band];
                if (s != null) {                    // `readStatistics()` may have left some values to null.
                    minimum = s.minimum();
                    maximum = s.maximum();
                }
            }
            /*
             * If statistics were not specified and the sample type is integer,
             * the minimum and maximum values may change for each band because
             * the sample size (in bits) can vary.
             */
            if (!(minimum <= maximum)) {        // Use `!` for catching NaN.
                minimum = 0;
                maximum = 1;
                if (isInteger) {
                    long max = Numerics.bitmask(sm.getSampleSize(band)) - 1;
                    if (!isUnsigned) {
                        max >>>= 1;
                        minimum = ~max;         // Tild operator, not minus.
                    }
                    maximum = max;
                }
            }
            /*
             * Create the sample dimension for this band. The same "no data" value is used for all bands.
             * The sample dimension is considered "converted" on the assumption that caller will replace
             * all "no data" value by NaN before to return the raster to the user.
             */
            if (isRGB) {
                builder.setName(Vocabulary.formatInternational(RGB_BAND_NAMES[band]));
            } else {
                if (name != null) {
                    builder.setName(name);
                    name = null;                // Use the name only for the first band.
                }
                builder.addQuantitative(null, minimum, maximum, null);
                if (nodataValue < minimum || nodataValue > maximum) {
                    builder.mapQualitative(null, nodataValue, Float.NaN);
                }
            }
            bands[band] = builder.build().forConvertedValues(!isInteger);
            builder.clear();
            /*
             * Create the color model using the statistics of the band that we choose to make visible,
             * or using a RGB color model if the number of bands or the data type is compatible.
             * The color file is optional and will be used if present.
             */
            if (band == VISIBLE_BAND) {
                if (isRGB) {
                    colorModel = ColorModelFactory.createRGB(sm);
                } else {
                    try {
                        colorModel = readColorMap(dataType, (int) (maximum + 1), bands.length);
                    } catch (IOException | NumberFormatException e) {
                        canNotReadAuxiliaryFile(CLR, e);
                    }
                    if (colorModel == null) {
                        colorModel = ColorModelFactory.createGrayScale(dataType, bands.length, band, minimum, maximum);
                    }
                }
            }
        }
        sampleDimensions = UnmodifiableArrayList.wrap(bands);
    }

    /**
     * Sends a warning about a failure to read an optional auxiliary file.
     * This is used for errors that affect only the rendering, not the georeferencing.
     *
     * @param  suffix     suffix of the auxiliary file.
     * @param  exception  error that occurred while reading the auxiliary file.
     */
    private void canNotReadAuxiliaryFile(final String suffix, final Exception exception) {
        Level level = Level.WARNING;
        if (exception instanceof NoSuchFileException || exception instanceof FileNotFoundException) {
            level = Level.FINE;
        }
        listeners.warning(level, Resources.format(Resources.Keys.CanNotReadAuxiliaryFile_1, suffix), exception);
    }

    /**
     * Default names of bands when the color model is RGB or RGBA.
     */
    private static final short[] RGB_BAND_NAMES = {
        Vocabulary.Keys.Red,
        Vocabulary.Keys.Green,
        Vocabulary.Keys.Blue,
        Vocabulary.Keys.Transparency
    };

    /**
     * Creates the grid coverage resulting from a {@link #read(GridGeometry, int...)} operation.
     *
     * @param  domain  the effective domain after intersection and subsampling.
     * @param  range   indices of selected bands.
     * @param  data    the loaded data.
     * @param  stats   statistics to save as a property, or {@code null} if none.
     * @return the grid coverage.
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    final GridCoverage2D createCoverage(final GridGeometry domain, final RangeArgument range,
                                        final WritableRaster data, final Statistics stats)
    {
        Hashtable<String,Object> properties = null;
        if (stats != null) {
            final Statistics[] as = new Statistics[range.getNumBands()];
            Arrays.fill(as, stats);
            properties = new Hashtable<>();
            properties.put(PlanarImage.STATISTICS_KEY, as);
        }
        List<SampleDimension> bands = sampleDimensions;
        ColorModel cm = colorModel;
        if (!range.isIdentity()) {
            bands = Arrays.asList(range.select(sampleDimensions));
            cm = range.select(cm).orElse(null);
            if (cm == null) {
                final SampleDimension band = bands.get(VISIBLE_BAND);
                cm = ColorModelFactory.createGrayScale(data.getSampleModel(), VISIBLE_BAND, band.getSampleRange().orElse(null));
            }
        }
        return new GridCoverage2D(domain, bands, new BufferedImage(cm, data, false, properties));
    }

    /**
     * Returns the sample dimensions computed by {@code loadBandDescriptions(…)}.
     * Shall be overridden by subclasses in a synchronized method. The subclass
     * must ensure that {@code loadBandDescriptions(…)} has been invoked once.
     *
     * @return the sample dimensions, or {@code null} if not yet computed.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return sampleDimensions;
    }

    /**
     * Closes this data store and releases any underlying resources.
     * Shall be overridden by subclasses in a synchronized method.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public void close() throws DataStoreException {
        // `listeners.close()` should be invoked by sub-classes.
        metadata = null;
    }
}
