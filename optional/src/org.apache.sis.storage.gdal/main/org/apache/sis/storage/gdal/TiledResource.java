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
package org.apache.sis.storage.gdal;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.MemorySegment;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.referencing.privy.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.image.ImageLayout;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.image.privy.ColorModelBuilder;
import org.apache.sis.image.privy.ColorModelFactory;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.TiledGridResource;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A {@code DataSet} which has been identified to be a raster.
 * This class is named {@code Raster} in <abbr>GDAL</abbr>, but we avoid that
 * name here for avoiding confusion with Java2D {@link java.awt.image.Raster}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TiledResource extends TiledGridResource {
    /**
     * Arbitrary number of pixels for considering a tile as too large.
     * Tile larger than this size will be divided in smaller tiles.
     */
    @Configuration
    private static final int LARGE_TILE_SIZE = 1024 * 1024 * 16;

    /**
     * The data set that contains this raster.
     */
    final GDALStore parent;

    /**
     * Index of this image, with numbers starting at 1. This is the tip of the identifier.
     */
    private final int imageIndex;

    /**
     * The identifier of this resource, or {@code null} if none.
     *
     * @see #getIdentifier()
     */
    private final GenericName identifier;

    /**
     * The grid geometry of the raster, or {@code null} if not yet computed.
     */
    private GridGeometry geometry;

    /**
     * The image size in pixels. Considered as unsigned integers.
     */
    private final int width, height;

    /**
     * The tile size in pixels. This is the <abbr>GDAL</abbr> block size,
     * unless the block size was too large in which case <abbr>SIS</abbr>
     * selects a smaller size.
     *
     * @see #getTileSize()
     */
    private final int tileWidth, tileHeight;

    /**
     * The pixel data type. It shall be the same for all bands in this raster.
     */
    final DataType dataType;

    /**
     * Description of all bands.
     * Those objects should never escape {@code TiledResource} in order to avoid the risk
     * that {@code GDALRasterBandH} exist after {@link #parent} has been garbage collected.
     */
    private final Band[] bands;

    /**
     * The sample dimensions, created when first requested.
     */
    private List<SampleDimension> sampleDimensions;

    /**
     * Indices of the {@linkplain #bands} selected for creating {@link #colorModel} and {@link #sampleModel}.
     * A null value means that all bands are used.
     */
    private int[] selectedBandIndices;

    /**
     * The color model for the bands specified by {@link #selectedBandIndices}, created when first requested.
     * This model is recreated when {@link #selectedBandIndices} changed.
     *
     * @see #getColorModel(int[])
     */
    private ColorModel colorModel;

    /**
     * The sample model for the bands specified by {@link #selectedBandIndices}, created when first requested.
     * All sample models shall have the size of the tile size with no sub-sampling.
     * This model is recreated when {@link #selectedBandIndices} changed.
     *
     * @see #getSampleModel(int[])
     */
    private SampleModel sampleModel;

    /**
     * The fill values, fetched when first requested. An array of length 0 is used as a sentinel
     * value meaning that the array has been computed and the result is {@code null}.
     *
     * @see #getFillValues(int[])
     */
    private Number[] fillValues;

    /**
     * Creates a new instance as a child of the given data set.
     *
     * @param  parent      the parent data set.
     * @param  imageIndex  index of this image, with numbers starting at 1.
     * @param  size        the raster width, height and data type.
     * @param  bands       description of all bands as an array of {@link Band}.
     * @param  name        an identifier for this band.
     */
    private TiledResource(final GDALStore parent, final int imageIndex, final SizeAndType size, final List<Band> bands)
            throws DataStoreException
    {
        super(parent);
        final Dimension t = size.tileSize();
        this.tileWidth  = t.width;
        this.tileHeight = t.height;
        this.parent     = parent;
        this.imageIndex = imageIndex;
        this.identifier = parent.factory.createLocalName(parent.namespace, String.valueOf(imageIndex)).toFullyQualifiedName();
        this.width      = size.width();
        this.height     = size.height();
        this.dataType   = DataType.valueOf(size.type());
        this.bands      = bands.toArray(Band[]::new);
    }

    /**
     * The raster width, height and data type. This is a short-lived object used only
     * at construction time, for grouping compatible bands in the same rasters.
     */
    private record SizeAndType(int width, int height, int type, int tileWidth, int tileHeight) {
        /**
         * Returns the suggested tile size. This is the block size given by <abbr>GDAL</abbr>,
         * unless that size is the image's width and a height of 1. The latter is GDAL default
         * when the file format is not tiled. But that default can consume a lot of memory.
         */
        Dimension tileSize() {
            int w, h;
            if ((w = tileWidth)  < 1 || Integer.compareUnsigned(w, width)  >= 0 ||
                (h = tileHeight) < 1 || Integer.compareUnsigned(h, height) >= 0)
            {
                /*
                 * If the tile size seems invalid or overflows the capacity of 32 bits integers,
                 * we will derive a tile size from the image size. If the image size overflows,
                 * use a default tile size.
                 */
                if ((w = width)  < 0) w = ImageLayout.DEFAULT_TILE_SIZE;
                if ((h = height) < 0) h = ImageLayout.DEFAULT_TILE_SIZE;
            } else if (Math.multiplyFull(w, h) <= LARGE_TILE_SIZE) {
                return new Dimension(w, h);
            }
            return ImageLayout.DEFAULT.suggestTileSize(w, h);
        }
    }

    /**
     * Returns rasters where each instance have bands of the same size and data type.
     * The raster having the same size as the {@code GDALDataset} is first.
     * Rasters having a different size may be lower-resolution images.
     *
     * @param  parent  wrapper for the {@code GDALDatasetH} of <abbr>GDAL</abbr> C/C++ <abbr>API</abbr>.
     * @param  gdal    set of <abbr>GDAL</abbr> native functions.
     * @return pointers to the band ({@code GDALRasterBandH}).
     * @throws DataStoreException if an error occurred.
     */
    static TiledResource[] groupBySizeAndType(final GDALStore parent, final GDAL gdal) throws DataStoreException {
        final MemorySegment dataset = parent.handle();
        final var bands = new LinkedHashMap<SizeAndType, ArrayList<Band>>();
        final int mainWidth, mainHeight;
        try (Arena arena = Arena.ofConfined()) {
            final var layout = ValueLayout.JAVA_INT;
            final MemorySegment pnXSize = arena.allocate(layout, 2);
            final MemorySegment pnYSize = pnXSize.asSlice(layout.byteSize());
            final int count = (int) gdal.getRasterCount.invokeExact(dataset);
            for (int i=0; i<count; i++) {
                final var band = (MemorySegment) gdal.getRasterBand.invokeExact(dataset, i+1);
                if (GDAL.isNull(band)) continue;       // Paranoiac check (should not happen).
                /*
                 * The following properties may differ in each band and define a `SizeAndType` key.
                 * Those keys and their associated bands are stored in the order they are found.
                 */
                final int width  = (int) gdal.getRasterBandXSize.invokeExact(band);
                final int height = (int) gdal.getRasterBandYSize.invokeExact(band);
                final int type   = (int) gdal.getRasterDataType .invokeExact(band);
                gdal.getBlockSize.invokeExact(band, pnXSize, pnYSize);
                int tileWidth  = pnXSize.get(layout, 0);
                int tileHeight = pnYSize.get(layout, 0);
                var key = new SizeAndType(width, height, type, tileWidth, tileHeight);
                bands.computeIfAbsent(key, (_) -> new ArrayList<Band>()).add(new Band(band));
            }
            mainWidth  = (int) gdal.getRasterXSize.invokeExact(dataset);
            mainHeight = (int) gdal.getRasterYSize.invokeExact(dataset);
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        /*
         * Replaces all list of bands by a single raster instance.
         * Then, makes sure that the raster having the size of the dataset is first.
         */
        final var rasters = new TiledResource[bands.size()];
        int count = 0;
        for (Map.Entry<SizeAndType, ArrayList<Band>> entry : bands.entrySet()) {
            rasters[count++] = new TiledResource(parent, count, entry.getKey(), entry.getValue());
        }
        // Search for the main image and, if found, move it first.
        for (int i=0; i<count; i++) {
            final TiledResource main = rasters[i];
            if (main.width == mainWidth && main.height == mainHeight) {
                System.arraycopy(rasters, 0, rasters, 1, i);
                rasters[0] = main;
                break;
            }
        }
        return ArraysExt.resize(rasters, count);
    }

    /**
     * Returns the object on which to perform synchronizations for thread-safety.
     * We need a single lock per <abbr>GDAL</abbr> {@code GDALStore} for all operations.
     */
    @Override
    protected final Object getSynchronizationLock() {
        return parent;
    }

    /**
     * Returns the resource persistent identifier.
     * This identifier can be used to uniquely identify a raster in the containing {@link GDALStore}.
     *
     * @return a persistent identifier unique within the data set.
     */
    @Override
    public final Optional<GenericName> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Builds the metadata.
     * This method is invoked only if the user requested the ISO 19115 metadata.
     *
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final var builder = new MetadataBuilder();
        parent.addFormatInfo(builder);
        builder.addTitle(Vocabulary.formatInternational(Vocabulary.Keys.Image_1, imageIndex));
        builder.addDefaultMetadata(this, listeners);
        return builder.build();
    }

    /**
     * Returns the extent of grid coordinates together with the conversion to real world coordinates.
     *
     * @throws DataStoreException if an error occurred during the construction of the grid geometry.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (geometry == null) try {
                final MemorySegment handle = parent.handle();          // Handle to the GDAL dataset.
                final GDAL gdal = parent.getProvider().GDAL();
                final var  srs  = SpatialRef.create(parent, gdal, handle);
                final CoordinateReferenceSystem crs;
                if (srs != null) {
                    crs = srs.parseCRS("getGridGeometry");
                } else {
                    crs = null;
                }
                /*
                 * Note that the CRS may be null. Now get the "grid to CRS" transform,
                 * which may also be null if GDAL reported an error. We do not use the
                 * GDAL default, which is the identity transform. Instead, we keep the
                 * information that the transform is missing (null).
                 */
                MathTransform gridToCRS = null;
                try (final Arena arena = Arena.ofConfined()) {
                    final var layout = ValueLayout.JAVA_DOUBLE;
                    final MemorySegment m = arena.allocate(layout, 6);
                    int err;
                    try {
                        err = (int) gdal.getGeoTransform.invokeExact(handle, m);
                    } catch (Throwable e) {
                        throw GDAL.propagate(e);
                    }
                    if (ErrorHandler.checkCPLErr(err)) {
                        gridToCRS = new AffineTransform2D(
                                m.getAtIndex(layout, 1),
                                m.getAtIndex(layout, 4),
                                m.getAtIndex(layout, 2),
                                m.getAtIndex(layout, 5),
                                m.getAtIndex(layout, 0),
                                m.getAtIndex(layout, 3));
                    }
                    // TODO: if above is not available, we could fallback on `GDALGCPsToGeoTransform`.
                }
                /*
                 * The axis order used by GDAL is not the axis order in the CRS definition.
                 * GDAL provides a separated method for specifying the axis swapping.
                 */
                if (gridToCRS != null && srs != null) {
                    int dimension = (crs != null) ? crs.getCoordinateSystem().getDimension() : SpatialRef.BIDIMENSIONAL;
                    final Matrix swap = srs.getDataToCRS(dimension);
                    if (swap != null) {
                        gridToCRS = MathTransforms.concatenate(gridToCRS, MathTransforms.linear(swap));
                    }
                }
                /*
                 * According GDAL documentation, the upper left corner of the upper left pixel
                 * is at position (m[0], m[3]). Therefore, we have a "cell corner" convention.
                 */
                var extent = new GridExtent(Integer.toUnsignedLong(width),
                                            Integer.toUnsignedLong(height));
                try {
                    geometry = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs);
                } catch (NullPointerException | IllegalArgumentException e) {
                    throw new DataStoreReferencingException(e);
                }
            } finally {
                ErrorHandler.report(parent, "getGridGeometry");
            }
            return geometry;
        }
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     *
     * @throws DataStoreException if an error occurred during the construction of sample dimensions.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")   // Because unmodifable.
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (sampleDimensions == null) try {
                final GDAL gdal = parent.getProvider().GDAL();
                final var sd = new SampleDimension[bands.length];
                try (Arena arena = Arena.ofConfined()) {
                    final MemorySegment flag = arena.allocate(ValueLayout.JAVA_INT);
                    for (int i=0; i<sd.length; i++) {
                        sd[i] = bands[i].createSampleDimension(parent, gdal, flag);
                    }
                }
                sampleDimensions = List.of(sd);
            } finally {
                ErrorHandler.report(parent, "getSampleDimensions");
            }
            return sampleDimensions;
        }
    }

    /**
     * Always return {@code true} because <abbr>GDAL</abbr> provides a band-oriented <abbr>API</abbr>,
     * where each band can always be accessed separately from other bands.
     */
    @Override
    protected final boolean canSeparateBands() {
        return true;
    }

    /**
     * Always returns 1, because the complexity of reading only a sub-region is handled by <abbr>GDAL</abbr>.
     */
    @Override
    protected final int getAtomSize(int dim) {
        return 1;
    }

    /**
     * Returns the bands in the given indices.
     *
     * @param  bandIndices  indices of the selected bands, or {@code null} for all bands.
     * @return specified bands. May be a reference to internal array: do not modify.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Band[] bands(final int[] bandIndices) {
        if (bandIndices == null) {
            return bands;
        }
        var selectedBands = new Band[bandIndices.length];
        for (int i=0; i<bandIndices.length; i++) {
            selectedBands[i] = bands[bandIndices[i]];
        }
        return selectedBands;
    }

    /**
     * Creates the color model and sample model.
     * This method stores the results in {@link #sampleModel} and {@link #colorModel},
     * which are used by the callers as a cache when the {@code bandIndices} argument
     * is equal to the values given the last time that this method has been invoked.
     *
     * All calls to this method should be indirect calls from {@link #read read(…)}
     * through the {@link Subset} constructor. Therefore, this method relies on the
     * error handling setup by {@code read(…)}.
     *
     * @param  bandIndices  indices of the selected bands, or {@code null} for all bands.
     */
    private void createColorAndSampleModel(final int[] bandIndices) throws DataStoreException {
        final Band[] selectedBands = bands(bandIndices);
        final GDAL gdal = parent.getProvider().GDAL();
        int[] palette = null;
        int paletteIndex = 0;
        int alpha = -1, red = -1, green = -1, blue = -1, gray = -1;
        for (int i=0; i < selectedBands.length; i++) {
            final Band band = selectedBands[i];
            switch (band.getColorInterpretation(gdal)) {
                case ALPHA:     if (alpha < 0) alpha = i; break;
                case RED:       if (red   < 0) red   = i; break;
                case GREEN:     if (green < 0) green = i; break;
                case BLUE:      if (blue  < 0) blue  = i; break;
                case GRAYSCALE: if (gray  < 0) gray  = i; break;
                case PALETTE: {
                    if (palette == null) {
                        paletteIndex = i;
                        palette = band.getARGB(gdal);
                    }
                    break;
                }
            }
            /*
             * TODO: check if all bands have the same `colorSpaceType`. Use that information for creating a
             * generic color space (potentially just rendering gray scale with the average value of all bands)
             * which can return that type in the `getType()` method. Note that there is generic color space types
             * for all number of bands from 2 to 15.
             */
        }
        if ((red | green | blue) >= 0) {
            colorModel = new ColorModelBuilder().bitsPerSample(dataType.numBits).alphaBand(alpha).createBandedRGB();
            // TODO: needs custom color model if too many bands, or if order is not (A)RGB.
        } else if (palette != null) {
            colorModel = ColorModelFactory.createIndexColorModel(selectedBands.length, paletteIndex, palette, true, -1);
        } else {
            gray = Math.max(gray, 0);
            final Band band = selectedBands[gray];
            final double min = band.getValue(gdal.getRasterMinimum, MemorySegment.NULL);
            final double max = band.getValue(gdal.getRasterMaximum, MemorySegment.NULL);
            colorModel = ColorModelFactory.createGrayScale(dataType.imageType, selectedBands.length, gray, min, max);
        }
        sampleModel = new BandedSampleModel(dataType.imageType, tileWidth, tileHeight, selectedBands.length);
        selectedBandIndices = bandIndices;
    }

    /**
     * Returns the Java2D color model for rendering images.
     * All calls to this method should be indirect calls from {@link #read read(…)}
     * through the {@link Subset} constructor. Therefore, this method relies on the
     * error handling setup by {@code read(…)}.
     */
    @Override
    protected ColorModel getColorModel(final int[] bandIndices) throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (colorModel == null || !Arrays.equals(bandIndices, selectedBandIndices)) {
                createColorAndSampleModel(bandIndices);
            }
            return colorModel;
        }
    }

    /**
     * Returns the sample model for tiles at full resolution with all their bands.
     * The raster size is the {@linkplain #getTileSize() tile size} as stored in the resource.
     *
     * All calls to this method should be indirect calls from {@link #read read(…)} through the
     * {@link Subset} constructor. Therefore, this method relies on the error handling setup by
     * {@code read(…)}.
     */
    @Override
    protected SampleModel getSampleModel(final int[] bandIndices) throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (sampleModel == null || !Arrays.equals(bandIndices, selectedBandIndices)) {
                createColorAndSampleModel(bandIndices);
            }
            return sampleModel;
        }
    }

    /**
     * Returns the values to use for filling empty spaces in rasters, with one value per band.
     * The returned array can be {@code null} if the fill values are not different than zero.
     * The zero value is excluded because tiles are already initialized to zero by default.
     *
     * All calls to this method should be indirect calls from {@link #read read(…)} through
     * the {@link Subset} constructor. Therefore, this method relies on the error handling
     * setup by {@code read(…)}.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    protected Number[] getFillValues(final int[] bandIndices) throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            if (fillValues == null) {
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final var fillValues = new Number[] {(bandIndices != null) ? bandIndices.length : bands.length};
                final GDAL gdal = parent.getProvider().GDAL();
                boolean hasNonZero = false;
                try (final Arena arena = Arena.ofConfined()) {
                    final MemorySegment flag = arena.allocate(ValueLayout.JAVA_INT);
                    for (int i=0; i<fillValues.length; i++) {
                        final int b = (bandIndices != null) ? bandIndices[i] : i;
                        final double value = bands[b].getValue(gdal.getRasterNoDataValue, flag);
                        hasNonZero |= (value != 0);
                        if (!Band.isTrue(flag)) {
                            hasNonZero = false;
                            break;
                        }
                    }
                }
                // Use `ExtendedPrecisionMatrix.CREATE_ZERO` as an arbitrary zero-length array.
                this.fillValues = hasNonZero ? fillValues : ExtendedPrecisionMatrix.CREATE_ZERO;
            }
            return (fillValues.length != 0) ? fillValues : null;
        }
    }

    /**
     * Allows the reading of truncated tiles in all dimensions. In <abbr>GDAL</abbr> case, truncating is actually
     * mandatory because <abbr>GDAL</abbr> requires the read requests to be fully contained inside the valid area.
     */
    @Override
    protected boolean canReadTruncatedTiles(int dim, boolean suggested) {
        return true;
    }

    /**
     * Returns the size of tiles (in pixels) in this resource.
     * The length of the returned array is the number of dimensions.
     */
    @Override
    protected final int[] getTileSize() {
        return new int[] {tileWidth, tileHeight};
    }

    /**
     * Returns virtual tile size to use during read operations. From the point of view of this class,
     * pretending that tiles have a size different than their real size is easy because <abbr>GDAL</abbr>
     * does the hard work of reading only the relevant parts of the real tiles. Therefore, we compensate
     * subsampling with larger virtual size for avoiding that subsampled tiles become too small.
     * This is useful in particular with pyramided images read with potentially high subsampling values.
     */
    @Override
    protected long[] getVirtualTileSize(final long[] subsampling) {
        return new long[] {
            Math.multiplyExact(subsampling[0], tileWidth),
            Math.multiplyExact(subsampling[1], tileHeight)
        };
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     * The actual loading may be deferred until a tile is requested for the first time.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     */
    @Override
    public GridCoverage read(final GridGeometry domain, final int... ranges) throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            try {
                final var subset = new Subset(domain, ranges);
                final var result = new TiledCoverage(this, subset);
                return preload(result);
            } finally {
                ErrorHandler.report(parent, "read");
            }
        }
    }
}
