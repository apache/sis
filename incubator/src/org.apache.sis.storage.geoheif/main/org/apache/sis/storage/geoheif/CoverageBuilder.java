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
package org.apache.sis.storage.geoheif;

import java.net.URI;
import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.IOException;
import java.nio.ByteOrder;
import java.awt.Dimension;
import java.awt.image.SampleModel;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.metadata.MetadataBuilder;
import org.apache.sis.storage.modifier.CoverageModifier;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.base.ItemInfoEntry;
import org.apache.sis.storage.isobmff.base.ItemProperties;
import org.apache.sis.storage.isobmff.image.TiledImageConfiguration;
import org.apache.sis.storage.isobmff.geo.ModelTransformation;
import org.apache.sis.storage.isobmff.geo.ModelCRS;
import org.apache.sis.storage.isobmff.mpeg.ComponentType;
import org.apache.sis.storage.isobmff.mpeg.ComponentPalette;
import org.apache.sis.storage.isobmff.mpeg.ComponentDefinition;
import org.apache.sis.storage.isobmff.mpeg.CompressedUnitsItemInfo;
import org.apache.sis.storage.isobmff.mpeg.CompressionConfiguration;
import org.apache.sis.storage.isobmff.mpeg.UncompressedFrameConfig;
import org.apache.sis.storage.isobmff.mpeg.UnitType;
import org.apache.sis.storage.isobmff.image.ImageSpatialExtents;
import org.apache.sis.storage.isobmff.image.PixelInformation;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.pending.jdk.JDK18;


/**
 * Helper class for building the grid geometry and sample dimensions of a grid coverage.
 * Also opportunistically builds the coverage metadata associated to the resource.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class CoverageBuilder implements Emptiable {
    /**
     * The resource which is creating a grid coverage.
     *
     * @todo A future version could store a map of {@link ImageModel} in the resource builder
     *       for the case where an image is tiled and each tile has the same image description.
     *       It would avoid recreating the same color model and sample model many times.
     */
    private final ResourceBuilder owner;

    /**
     * An index of the image to build, for information purpose only.
     * This is given to {@link CoverageModifier.Source} constructor.
     */
    final int imageIndex;

    /**
     * Name or identifier of the resource. This is taken from {@link ItemInfoEntry#itemName}
     * if available, or {@link ItemInfoEntry#itemID} otherwise. The item name is documented
     * as "symbolic name of the item (source file for file delivery transmissions)" and this
     * class assumes that the name is unique.
     *
     * @todo Verify if {@link ItemInfoEntry#itemName} is really unique.
     *
     * @see #name()
     */
    private String name;

    /**
     * The size (in pixels) of the reconstructed image.
     */
    private int width, height;

    /**
     * Information about where to find the tiles when the image type is {@code "tili"}.
     * May be {@code null} if no such information was found.
     *
     * @see #tiling()
     */
    private TiledImageConfiguration tiling;

    /**
     * Coefficients of the matrix that defines the "grid to <abbr>CRS</abbr>" coordinate conversion.
     * May be {@code null} if no such information was found.
     */
    private ModelTransformation affine;

    /**
     * The Well Known Text representation of the coordinate reference system.
     * May be {@code null} if no such information was found.
     */
    private ModelCRS crsDefinition;

    /**
     * Identification of the compression algorithm.
     */
    private CompressionConfiguration compression;

    /**
     * Locations in the file where to find compressed data.
     */
    private CompressedUnitsItemInfo compressedUnits;

    /**
     * Information about the sample model (data type, <i>etc.</i>).
     * May be {@code null} if no such information was found.
     * This is actually a mandatory information according ISO/IEC 23001-17:2024,
     * but this class is nevertheless tolerant to its absence.
     */
    private UncompressedFrameConfig model;

    /**
     * How pixel data should be displayed.
     * Values are instance of {@link ComponentType}, {@link URI} or {@link Integer}, in preference order.
     * In the latter case, the integer value is the user-defined component type.
     * May be {@code null} if no such information was found.
     */
    private Object[] componentTypes;

    /**
     * Number of bits per channel for the pixels of the reconstructed image.
     * The array length is the number of channels. Values are unsigned.
     * May be {@code null} if no such information was found.
     */
    private byte[] bitsPerChannel;

    /**
     * The color palette of an indexed color model.
     * May be {@code null} if no such information was found.
     */
    private ComponentPalette palette;

    /**
     * Boxes that are unknown to this class.
     * Keys are either {@link Integer} for a four-characters code, or {@link UUID}.
     * The map values specify whether the associated box was considered essential.
     * The null key means that some missing boxes are unidentified.
     */
    private final Map<Object, Boolean> unknownBoxes;

    /**
     * The color model, sample model and sample dimensions.
     * This is created when first needed. The same instance may be shared by many tiles.
     *
     * @see #imageModel()
     */
    private ImageModel imageModel;

    /**
     * The builder of metadata, created when first needed.
     *
     * @see #metadata()
     */
    private MetadataBuilder metadata;

    /**
     * The builder used for building tiles, or {@code null} if none.
     *
     * @see #setTileBuilder(CoverageBuilder)
     */
    private CoverageBuilder tileBuilder;

    /**
     * Creates a new builder with the given properties.
     *
     * @param  owner            the resource which is creating a grid coverage.
     * @param  imageIndex       an index of the image, for information purpose only.
     * @param  properties       source of coverage properties for this coverage item, or {@code null} if none.
     * @param  duplicatedBoxes  names of boxes that were duplicated. Used for logging a warning only once per type of box.
     */
    CoverageBuilder(final ResourceBuilder owner,
                    final int imageIndex,
                    final ItemProperties.ForID properties,
                    final Set<String> duplicatedBoxes)
    {
        this.owner = owner;
        this.imageIndex = imageIndex;
        unknownBoxes = new LinkedHashMap<>();
        if (properties != null) {
            if (properties.missingItem) {
                // Some boxes could not be handled, but we don't have their identifiers.
                unknownBoxes.put(null, properties.missingEssential);
            }
            load(properties, duplicatedBoxes);
        }
    }

    /**
     * Collects from the given boxes the data that this builder will need.
     * This method may invoke itself recursively if a box is a container for other boxes.
     *
     * @param  properties       source of coverage properties for this coverage item.
     * @param  duplicatedBoxes  names of boxes that were duplicated. Used for logging a warning only once per type of box.
     */
    private void load(final List<Box> properties, final Set<String> duplicatedBoxes) {
        final int count = properties.size();
        for (int i=0; i<count; i++) {
            final boolean duplicated;
            final Box property = properties.get(i);
            switch (property.type()) {
                /*
                 * We should have only one instance of `ImageSpatialExtents`. If nevertheless there are many,
                 * the use of the maximum values increases the chances that we detect inconsistency because
                 * it may result in a length larger than the size of the box which contains the pixels.
                 */
                case ImageSpatialExtents.BOXTYPE: {
                    var c = (ImageSpatialExtents) property;
                    duplicated = (width | height) != 0;
                    width  = Math.max(width,  c.imageWidth);
                    height = Math.max(height, c.imageHeight);
                    break;
                }
                /*
                 * We should have one instance of `ComponentDefinition` and `PixelInformation`. If nevertheless
                 * there are many, maybe a broken writer puts one element per box. If this assumption is wrong,
                 * the total length should be greater than the pixels box size, thus allowing error detection.
                 */
                case ComponentDefinition.BOXTYPE: {
                    var c = (ComponentDefinition) property;
                    duplicated = (componentTypes != null);
                    componentTypes = ArraysExt.concatenate(componentTypes, c.componentTypes);
                    break;
                }
                case PixelInformation.BOXTYPE: {
                    var c = (PixelInformation) property;
                    duplicated = (bitsPerChannel != null);
                    bitsPerChannel = ArraysExt.concatenate(bitsPerChannel, c.bitsPerChannel);
                    break;
                }
                /*
                 * For the next types of box, we keep only the first occurrence.
                 */
                case ModelTransformation.BOXTYPE: {
                    var c = (ModelTransformation) property;
                    duplicated = (affine != null);
                    if (!duplicated) affine = c;
                    break;
                }
                case ModelCRS.BOXTYPE: {
                    var c = (ModelCRS) property;
                    duplicated = (crsDefinition != null);
                    if (!duplicated) crsDefinition = c;
                    break;
                }
                case CompressionConfiguration.BOXTYPE: {
                    var c = (CompressionConfiguration) property;
                    duplicated = (compression != null);
                    if (!duplicated) compression = c;
                    break;
                }
                case CompressedUnitsItemInfo.BOXTYPE: {
                    var c = (CompressedUnitsItemInfo) property;
                    duplicated = (compressedUnits != null);
                    if (!duplicated) compressedUnits = c;
                    break;
                }
                case UncompressedFrameConfig.BOXTYPE: {
                    var c = (UncompressedFrameConfig) property;
                    duplicated = (model != null);
                    if (!duplicated) model = c;
                    break;
                }
                case ComponentPalette.BOXTYPE: {
                    var c = (ComponentPalette) property;
                    duplicated = (palette != null);
                    if (!duplicated) palette = c;
                    break;
                }
                case TiledImageConfiguration.BOXTYPE: {
                    var c = (TiledImageConfiguration) property;
                    duplicated = (tiling != null);
                    if (!duplicated) {
                        tiling = c;     // Before `load(…)` for safety against nested tiling.
                        load(Arrays.asList(c.tileImageProperties), duplicatedBoxes);
                    }
                    break;
                }
                default: {
                    boolean essential = (properties instanceof ItemProperties.ForID)
                                && ((ItemProperties.ForID) properties).essential(i);
                    unknownBoxes.merge(property.typeKey(), essential, Boolean::logicalOr);
                    continue;
                }
            }
            if (duplicated) {
                final String type = Box.formatFourCC(property.type());
                if (duplicatedBoxes.add(type)) {
                    store().warning(Errors.Keys.DuplicatedElement_1, type);
                }
            }
        }
    }

    /**
     * Returns information about where to find the tiles when the image type is {@code "tili"}.
     * This is used for reading the positions of tiles in the file and the number of bytes to read.
     */
    final TiledImageConfiguration tiling() {
        return tiling;
    }

    /**
     * Returns the compression method, or 0 if none.
     * The returned value should be one of the {@code CompressionConfiguration.COMPRESSION_*} constants.
     */
    final int compressionType() {
        return (compression != null) ? compression.compressionType : 0;
    }

    /**
     * Returns the compression units which contains all image data, or {@code null} if the image is uncompressed.
     * This method requires that the compression unit is {@link UnitType#IMAGE_TILE}.
     *
     * @return the compression units for the whole image, or {@code null}.
     * @throws UnsupportedEncodingException if the compression configuration is unsupported.
     * @throws DataStoreContentException if the compression cannot be decoded for another reason.
     */
    final CompressedUnitsItemInfo.Unit compressedImageUnit() throws DataStoreContentException {
        if (compression == null) {
            return null;
        }
        if (compression.unitType == UnitType.IMAGE_TILE) {
            if (compressedUnits == null) {
                return null;
            }
            final CompressedUnitsItemInfo.Unit[] units = compressedUnits.units;
            if (units.length == 1) {
                return units[0];
            }
        }
        throw new UnsupportedEncodingException("Unsupported compression.");
    }

    /**
     * If any boxes were unrecognized, reports these boxes in a warning.
     * If at least one ignored box was flagged as essential, then this method returns {@code true}
     * and the caller should not create the resource. Otherwise, this method returns {@code false}
     * and the caller can proceed.
     *
     * @param  name  name of the resource to create.
     * @return whether at least one ignored box was flagged as essential.
     */
    boolean reportUnknownBoxes(final String name) {
        boolean essential = false;
        if (!unknownBoxes.isEmpty()) {
            final Level level;
            final var message = new StringBuilder();
            final Collection<Boolean> essentials = unknownBoxes.values();
            if (essentials.contains(Boolean.TRUE)) {
                essentials.removeIf((e) -> !e);  // Remove all non-essential boxes.
                message.append("Cannot create a resource for \"").append(name)
                        .append("\" because the following essential boxes are not handled: ");
                level = Level.WARNING;
                essential = true;
            } else {
                message.append("The \"").append(name)
                        .append("\" resource has been read but the following boxes have been ignored: ");
                level = Level.FINE;
            }
            final var sj = new StringJoiner(", ");
            for (Object id : unknownBoxes.keySet()) {
                if (id == null) {
                    id = "unidentified boxes";
                } else if (id instanceof Integer fourCC) {
                    id = Box.formatFourCC(fourCC);
                }
                sj.add(id.toString());
            }
            final var record = new LogRecord(level, message.append(sj).append('.').toString());
            store().warning(record);
        }
        return essential;
    }

    /**
     * Sets the builder which is used for the tiles. This method is invoked when this builder is used for a grid view,
     * and that grid is made of many images interpreted as tiles. In such cases, there is no box specifying the color
     * and sample model of the view. This builder will need to get these models from the tile builder.
     *
     * <p>The two builders will share the same {@link MetadataBuilder} instance. This is because we need to redirect
     * the metadata information collected by the tile builder to this builder, otherwise they will be lost.
     * This method needs to be invoked before {@link #gridGeometry()} and {@link #sampleDimensions()}.</p>
     *
     * <p>If this method is invoked many times with a non-null {@code firstBuilder} argument, only the first
     * occurrence is retained. It does not necessarily cause a lost of data because the caller already saved
     * the image in the {@link ResourceBuilder#resources} list, and the builder is used for fetching
     * information that should be the same for every tiles.</p>
     *
     * @param  firstBuilder  the builder used for building the first tile, or {@code null} if none.
     */
    final void setTileBuilder(final CoverageBuilder firstBuilder) {
        if (firstBuilder != null && tileBuilder == null) {
            tileBuilder = firstBuilder;
            metadata = firstBuilder.metadata();
        }
    }

    /**
     * Builds the grid coverage resource for an untiled image.
     * This builder should not be used anymore after this method call.
     *
     * @param  name   name of the resource.
     * @param  image  the single tile of the image.
     * @return the resource.
     * @throws DataStoreContentException if the "grid to <abbr>CRS</abbr>" transform or the sample dimensions cannot be created.
     * @throws DataStoreException if the construction failed for another reason.
     */
    final ImageResource build(final String name, final Image image) throws DataStoreException {
        this.name = name;
        return new ImageResource(this, null, image);
    }

    /**
     * Builds the grid coverage resource for a tiled image.
     * This builder should not be used anymore after this method call.
     *
     * @param  name   name of the resource.
     * @param  tiles  all tiles of the image.
     * @return the resource.
     * @throws DataStoreContentException if the "grid to <abbr>CRS</abbr>" transform or the sample dimensions cannot be created.
     * @throws DataStoreException if the construction failed for another reason.
     */
    final ImageResource build(final String name, final List<Image> tiles) throws DataStoreException {
        this.name = name;
        return new ImageResource(this, tiles.toArray(Image[]::new), null);
    }

    /**
     * Returns the data store which is creating a grid coverage.
     *
     * @return the data store which is creating a grid coverage.
     */
    public final GeoHeifStore store() {
        return owner.store;
    }

    /**
     * Returns a name for the resource to create and opportunistically adds it to the metadata.
     * This method should be invoked exactly once.
     */
    public final GenericName name() {
        GenericName gn = store().createComponentName(name);
        metadata().addIdentifier(gn, MetadataBuilder.Scope.RESOURCE);
        return gn;
    }

    /**
     * Returns the builder of metadata.
     */
    public final MetadataBuilder metadata() {
        if (metadata == null) {
            if (tileBuilder == null || (metadata = tileBuilder.metadata) == null) {
                metadata = new MetadataBuilder();
            }
        }
        return metadata;
    }

    /**
     * Returns whether the image has no width or no height.
     */
    @Override
    public final boolean isEmpty() {
        return (width <= 0 || height <= 0);
    }

    /**
     * Returns the number of tiles in the given dimension.
     *
     * @param  dimension  the dimension: 0 or 1.
     * @return number of tiles in the requested dimension.
     * @throws DataStoreContentException if the sample model cannot be created.
     * @throws DataStoreException if the reading failed for another reason.
     */
    public final int numTiles(final int dimension) throws DataStoreException {
        if (model != null) {
            switch (dimension) {
                case 0: return model.numTileCols;
                case 1: return model.numTileRows;
            }
        } else if (!isEmpty()) {
            if (tiling != null) {
                switch (dimension) {
                    case 0: return JDK18.ceilDiv(width,  tiling.tileWidth);
                    case 1: return JDK18.ceilDiv(height, tiling.tileHeight);
                }
            } else {
                final SampleModel sampleModel = imageModel().sampleModel;
                if (sampleModel != null) {
                    switch (dimension) {
                        case 0: return JDK18.ceilDiv(width,  sampleModel.getWidth());
                        case 1: return JDK18.ceilDiv(height, sampleModel.getHeight());
                    }
                }
            }
        }
        return 1;
    }

    /**
     * Returns the byte order to use for reading the sample values of the image.
     */
    public final ByteOrder byteOrder() {
        return (model != null && model.componentsLittleEndian) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    /**
     * Fetches the color model and sample model from a sample tile (usually the first).
     * This case happens when each tile is a <abbr>JPEG</abbr> image.
     * In such case, the sample model is not declared in {@code UncompressedFrameConfig}
     * and we have to get it from a tile.
     *
     * <p>If this sample model has already been initialized, then this method does nothing.
     * This is desirable because this method is invoked in a loop for every tiles, while
     * fetching the sample model of the first tile is sufficient.</p>
     *
     * @param  image  a tile from which to get the sample and color models.
     * @throws DataStoreException if a problem occurred with the content of the <abbr>HEIF</abbr> file.
     * @throws IOException if an I/O error occurred.
     */
    final void setImageLayout(final Image image) throws DataStoreException, IOException {
        if (imageModel == null) {
            imageModel = new ImageModel(image.getImageType(store()), this);
        }
    }

    /**
     * Returns color model, sample model and sample dimensions computed from <abbr>HEIF</abbr> boxes.
     *
     * @throws DataStoreContentException if the sample model or sample dimensions cannot be created.
     * @throws DataStoreException if the reading failed for another reason.
     */
    final ImageModel imageModel() throws DataStoreException {
        if (imageModel == null) {
            Dimension tileSize = null;
            if (tiling != null) {
                tileSize = new Dimension(tiling.tileWidth, tiling.tileHeight);
            }
            imageModel = new ImageModel(width, height, tileSize, model, componentTypes, bitsPerChannel, palette, this);
            if (imageModel.sampleModel == null && tileBuilder != null) {
                imageModel = tileBuilder.imageModel();
            }
        }
        return imageModel;
    }

    /**
     * Returns the sample model with a size equals to the tile size.
     *
     * @return the sample model (never {@code null}).
     * @throws DataStoreContentException if the sample model or sample dimensions cannot be created.
     * @throws DataStoreException if the reading failed for another reason.
     */
    public final SampleModel sampleModel() throws DataStoreException {
        final SampleModel sampleModel = imageModel().sampleModel;
        if (sampleModel != null) {
            return sampleModel;
        }
        throw new DataStoreContentException("Unspecified sample model.");
    }

    /**
     * Creates the grid geometry and opportunistically prepares metadata related to it.
     * This method should be invoked at most once.
     * It may be invoked not at all when this object is used for building a tile instead of an image.
     *
     * @todo Need to add information from the {@code ExtraDimensionProperty} (edim) box.
     *       These information include name, minimum, maximum and resolution.
     *       See https://docs.ogc.org/per/24-038r1.html
     *
     * @return the grid geometry.
     * @throws DataStoreException if the "grid to <abbr>CRS</abbr>" transform cannot be created.
     */
    public final GridGeometry gridGeometry() throws DataStoreException {
        final GeoHeifStore store = store();
        final var extent = new GridExtent(width, height);
        MathTransform gridToCRS = null;
        if (affine != null) {
            gridToCRS = affine.toMathTransform();
        }
        CoordinateReferenceSystem crs = null;
        if (crsDefinition != null) {
            crs = crsDefinition.toCRS(store.listeners());     // May stil be null.
        }
        var gridGeometry = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs);
        metadata().addSpatialRepresentation(null, gridGeometry, false);
        if (gridGeometry.isDefined(GridGeometry.ENVELOPE)) {
            metadata.addExtent(gridGeometry.getEnvelope(), store.listeners());
        }
        var source = new CoverageModifier.Source(store, imageIndex, imageModel().dataType);
        return store.customizer.customize(source, gridGeometry);
    }
}
