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
import java.awt.image.ColorModel;
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
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Vocabulary;
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
        listeners.useWarningEventsOnly();
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
         * Do not add the sample dimensions because in current version computing the sample dimensions without
         * statistics requires loading the full image. Even if `GridCoverage.getSampleDimensions()` exists and
         * could be used opportunistically, we do not use it in order to keep a deterministic behavior
         * (we do not want the metadata to vary depending on the order in which methods are invoked).
         */
        addTitleOrIdentifier(builder);
        builder.setISOStandards(false);
        metadata = builder.buildAndFreeze();
    }

    /**
     * Loads {@code "*.stx"} and {@code "*.clr"} files if present then builds {@link #sampleDimensions} and
     * {@link #colorModel} from those information. If no color map is found, a grayscale color model is created.
     *
     * @param  name   name to use for the sample dimension, or {@code null} if untitled.
     * @param  sm     the sample model to use for creating a default color model if no {@code "*.clr"} file is found.
     * @param  stats  if the caller collected statistics by itself, those statistics. Otherwise {@code null}.
     */
    final void loadBandDescriptions(String name, final SampleModel sm, final Statistics stats) {
        final SampleDimension[] bands = new SampleDimension[sm.getNumBands()];
        final int dataType = sm.getDataType();
        /*
         * TODO: read color map and statistics.
         *
         * Fallback when no statistics auxiliary file was found.
         * Try to infer the minimum and maximum from data type.
         */
        double  minimum = 0;
        double  maximum = 1;
        boolean computeForEachBand = false;
        final boolean isInteger  = ImageUtilities.isIntegerType(dataType);
        final boolean isUnsigned = isInteger && ImageUtilities.isUnsignedType(sm);
        final boolean isRGB      = isInteger && (bands.length == 3 || bands.length == 4);
        if (stats != null && stats.count() != 0) {
            minimum = stats.minimum();
            maximum = stats.maximum();
        } else {
            computeForEachBand = isInteger && !isRGB;
        }
        final SampleDimension.Builder builder = new SampleDimension.Builder();
        for (int band=0; band < bands.length; band++) {
            /*
             * If statistics were not specified and the sample type is integer,
             * the minimum and maximum values may change for each band because
             * the sample size (in bits) can vary.
             */
            if (computeForEachBand) {
                minimum = 0;
                long max = Numerics.bitmask(sm.getSampleSize(band)) - 1;
                if (!isUnsigned) {
                    max >>>= 1;
                    minimum = ~max;         // Tild operator, not minus.
                }
                maximum = max;
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
             * or using a RGB color model if the number of bands and the data type are compatible.
             */
            if (band == VISIBLE_BAND) {
                if (isRGB) {
                    colorModel = ColorModelFactory.createRGB(sm);
                } else {
                    colorModel = ColorModelFactory.createGrayScale(dataType, bands.length, band, minimum, maximum);
                }
            }
        }
        sampleDimensions = UnmodifiableArrayList.wrap(bands);
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
            cm = range.select(colorModel).orElse(null);
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
        metadata = null;
    }
}
