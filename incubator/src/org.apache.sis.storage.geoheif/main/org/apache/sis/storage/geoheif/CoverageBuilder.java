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
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.IOException;
import java.nio.ByteOrder;
import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RasterFormatException;
import javax.imageio.ImageTypeSpecifier;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.image.DataType;
import org.apache.sis.image.internal.shared.ColorModelBuilder;
import org.apache.sis.image.internal.shared.ColorModelFactory;
import org.apache.sis.image.internal.shared.SampleModelBuilder;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.modifier.CoverageModifier;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.base.ItemInfoEntry;
import org.apache.sis.storage.isobmff.base.ItemProperties;
import org.apache.sis.storage.isobmff.gimi.ModelTransformation;
import org.apache.sis.storage.isobmff.gimi.ModelCRS;
import org.apache.sis.storage.isobmff.mpeg.Component;
import org.apache.sis.storage.isobmff.mpeg.ComponentType;
import org.apache.sis.storage.isobmff.mpeg.ComponentPalette;
import org.apache.sis.storage.isobmff.mpeg.ComponentDefinition;
import org.apache.sis.storage.isobmff.mpeg.UncompressedFrameConfig;
import org.apache.sis.storage.isobmff.mpeg.InterleavingMode;
import org.apache.sis.storage.isobmff.image.ImageSpatialExtents;
import org.apache.sis.storage.isobmff.image.PixelInformation;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.pending.jdk.JDK18;


/**
 * Helper class for building the grid geometry and sample dimensions of a grid coverage.
 * Also opportunistically builds the coverage metadata associated to the resource.
 *
 * <p>The call to {@link #buildAndFreeze()} shall be last because metadata are completed
 * as side-effect of other method calls (for building name, grid geometry, <i>etc</i>).</p>
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class CoverageBuilder implements Emptiable {
    /**
     * The data store which is creating a grid coverage.
     */
    final GeoHeifStore store;

    /**
     * An index of the image to build, for information purpose only.
     * This is given to {@link CoverageModifier.Source} constructor.
     */
    private final int imageIndex;

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
     * The size of the reconstructed image in pixels.
     */
    private int width, height;

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
     * Information about the sample model (data type, <i>etc.</i>).
     * May be {@code null} if no such information was found.
     * This is actually a mandatory information according ISO/IEC 23001-17:2024,
     * but this class is nevertheless tolerant to its absence.
     */
    private UncompressedFrameConfig model;

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
     * The type of sample values stored in the raster.
     * Determined as a side effect of {@link #sampleDimensions()}.
     *
     * @see #dataType()
     */
    private DataType dataType;

    /**
     * The color model, created as a side effect of {@link #sampleDimensions()}.
     * May stay null if this builder does not have enough information.
     *
     * @see #colorModel()
     */
    private ColorModel colorModel;

    /**
     * The sample model, created as a side effect of {@link #sampleDimensions()}.
     * May stay null if this builder does not have enough information.
     *
     * @see #sampleModel()
     */
    private SampleModel sampleModel;

    /**
     * The sample dimensions, created when first requested.
     *
     * @see #sampleDimensions()
     */
    private SampleDimension[] sampleDimensions;

    /**
     * The builder of metadata, created when first needed.
     *
     * @see #metadata()
     */
    private MetadataBuilder metadata;

    /**
     * The builder used for building tiles, or {@code null} if none.
     */
    private CoverageBuilder tileBuilder;

    /**
     * Creates a new builder with the given properties.
     *
     * @param  store            the data store which is creating a grid coverage.
     * @param  imageIndex       an index of the image, for information purpose only.
     * @param  properties       source of coverage properties for this coverage item.
     * @param  duplicatedBoxes  names of boxes that were duplicated. Used for logging a warning only once per type of box.
     */
    CoverageBuilder(final GeoHeifStore store, final int imageIndex, final ItemProperties.ForID properties, final Set<String> duplicatedBoxes) {
        this.store = store;
        this.imageIndex = imageIndex;
        unknownBoxes = new LinkedHashMap<>();
        if (properties == null) {
            return;
        }
        if (properties.missingItem) {
            // Some boxes could not be handled, but we don't have their identifiers.
            unknownBoxes.put(null, properties.missingEssential);
        }
        final int count = properties.size();
        for (int i=0; i<count; i++) {
            boolean duplicated;
            final Box property = properties.get(i);
            switch (property.type()) {
                /*
                 * We should have only one instance of `ImageSpatialExtents`. If nevertheless there is many,
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
                 * there is many, maybe a broken writer puts one element per box. If this assumption is wrong,
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
                default: {
                    unknownBoxes.merge(property.typeKey(), properties.essential(i), Boolean::logicalOr);
                    continue;
                }
            }
            if (duplicated) {
                final String type = Box.formatFourCC(property.type());
                if (duplicatedBoxes.add(type)) {
                    store.warning(Errors.Keys.DuplicatedElement_1, type);
                }
            }
        }
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
            store.warning(record);
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
     * @param  tile  the builder used for the tiles.
     */
    final void setTileBuilder(final CoverageBuilder tile) {
        if (tile != null && tileBuilder == null) {
            tileBuilder = tile;
            metadata = tile.metadata();
        }
    }

    /**
     * Builds the grid coverage resource for an untiled image.
     * This builder should not be used anymore after this method call.
     *
     * @param  name   name of the resource.
     * @param  image  the single tile of the image.
     * @return the resource.
     * @throws RasterFormatException if the sample dimensions or sample model cannot be created.
     * @throws DataStoreException if the "grid to <abbr>CRS</abbr>" transform cannot be created.
     */
    final ImageResource build(final String name, final Image.Supplier image) throws DataStoreException {
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
     * @throws RasterFormatException if the sample dimensions or sample model cannot be created.
     * @throws DataStoreException if the "grid to <abbr>CRS</abbr>" transform cannot be created.
     */
    final ImageResource build(final String name, final List<Image> tiles) throws DataStoreException {
        this.name = name;
        return new ImageResource(this, tiles.toArray(Image[]::new), null);
    }

    /**
     * Returns a name for the resource to create and opportunistically adds it to the metadata.
     * This method should be invoked exactly once.
     */
    public final GenericName name() {
        GenericName gn = store.createComponentName(name);
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
     */
    public final int numTiles(final int dimension) {
        if (model != null) {
            switch (dimension) {
                case 0: return model.numTileCols;
                case 1: return model.numTileRows;
            }
        } else if ((width | height) != 0 && sampleModel != null) {
            switch (dimension) {
                case 0: return JDK18.ceilDiv(width,  sampleModel.getWidth());
                case 1: return JDK18.ceilDiv(height, sampleModel.getHeight());
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
     * This case happens when each tile is a JPEG image. In such case, the sample model
     * is not declared in {@code UncompressedFrameConfig} and we have to get it from a tile.
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
        if (sampleModel == null) {
            ImageTypeSpecifier type = image.getImageType(store);
            colorModel  = type.getColorModel();
            sampleModel = type.getSampleModel();
            dataType    = DataType.forDataBufferType(sampleModel.getDataType());
        }
    }

    /**
     * Returns the type of sample values stored in the raster.
     * One of {@link #sampleModel()} or {@link #sampleDimensions()}
     * methods must have been invoked before this method.
     */
    public final DataType dataType() {
        return dataType;
    }

    /**
     * Returns the color model, or {@code null} if unknown.
     * The {@link #sampleDimensions()} method must have been invoked before this method.
     */
    public final ColorModel colorModel() {
        return colorModel;
    }

    /**
     * Returns the sample model with a size equals to the tile size.
     *
     * @return the sample model (never {@code null}).
     * @throws RasterFormatException if the sample dimensions or sample model cannot be created.
     * @throws DataStoreException if an error occurred in {@link CoverageModifier}.
     */
    public final SampleModel sampleModel() throws DataStoreException {
        if (sampleModel == null) {
            sampleDimensions(false);
            if (sampleModel == null) {
                throw new RasterFormatException("Unspecified sample model.");
            }
        }
        return sampleModel;
    }

    /**
     * Implementation of {@link #sampleDimensions()} and {@link #sampleModel()}.
     *
     * @todo Need to add information from the {@code CellPropertyTypeProperty} box.
     *       These information include sample dimension name and unit of measurement.
     *
     * @param  full  {@code true} for creating all objects, or {@code false} for creating only the sample model.
     * @throws RasterFormatException if the sample dimensions or sample model cannot be created.
     * @throws DataStoreException if an error occurred in {@link CoverageModifier}.
     */
    private void sampleDimensions(final boolean full) throws DataStoreException {
        final int nc = (model          == null) ? 0 : model.components.length;
        final int nt = (componentTypes == null) ? 0 : componentTypes.length;
        final int ns = (bitsPerChannel == null) ? 0 : bitsPerChannel.length;
        final int numBands = Math.max(Math.max(nc, nt), ns);
        if (numBands == 0) {
            if (tileBuilder != null) {
                if (tileBuilder.sampleDimensions == null) {
                    tileBuilder.sampleDimensions(full);
                }
                sampleDimensions = tileBuilder.sampleDimensions;
                sampleModel      = tileBuilder.sampleModel;
                colorModel       = tileBuilder.colorModel;
                dataType         = tileBuilder.dataType;
            }
            return;
        }
        final int[] bitsPerSample = new int[numBands];
        final var sd = full ? new SampleDimension[numBands] : null;
        final var sb = full ? new SampleDimension.Builder() : null;
        int numBits=0, redMask=0, greenMask=0, blueMask=0, alphaMask=0;
        int grayBand = -1, indexBand = -1, alphaBand = -1;    // Negative value means none.
        for (int band=0; band<numBands; band++) {
            /*
             * `colorType` can be an enumeration value such as `RED`, `GREEN`, `BLUE`,
             * or an URI, or an Integer value. The same value can appear in two boxes.
             * `bitDepth` can also appear in two boxes. The ISO 23001-17:2024 standard
             * said that when the component type information appears in the two boxes,
             * `ComponentDefinitionBox` shall precede `UncompressedFrameConfigBox`.
             */
            var colorType = (band < nt) ? componentTypes[band] : null;  // From `ComponentDefinition`.
            int bitDepth  = (band < ns) ? Byte.toUnsignedInt(bitsPerChannel[band]) : 0;
            if (band < nc) {
                final Component c = model.components[band];
                if (colorType == null) colorType = c.type;              // From `UncompressedFrameConfig`
                if (dataType == null) {
                    dataType = c.getDataType();
                } else if (dataType != c.getDataType()) {
                    throw new RasterFormatException("All bands shall be of the same data type.");
                }
                bitDepth = Short.toUnsignedInt(c.bitDepth);
                /*
                 * Example: 10-bit unaligned RGB components followed by a 1-byte aligned 7-bit alpha component.
                 * Stored as 30 consecutive bits containing R, G and B, followed by 2 pre-alignment padding bits
                 * for byte alignment, followed by one alignment padding bit then followed by the 7-bit alpha value.
                 */
                final int alignSize = Byte.toUnsignedInt(c.alignSize) * Byte.SIZE;
                if (alignSize != 0) {
                    numBits = Numerics.snapToCeil(numBits,  alignSize)
                            + Numerics.snapToCeil(bitDepth, alignSize) - bitDepth;
                }
            }
            /*
             * Create the sample dimension and derive metadata from it.
             * TODO: parse CellPropertyTypeProperty and CellPropertyCategoriesProperty boxes.
             */
            if (full) {
                if (colorType != null) {
                    sb.setName(colorType.toString());
                } else {
                    sb.setName(band);
                }
                var source = new CoverageModifier.BandSource(store, imageIndex, band, numBands, dataType);
                metadata().addNewBand(sd[band] = store.customizer.customize(source, sb));
                sb.clear();
            }
            if (bitDepth == 0) {
                bitDepth = Component.DEFAULT_BIT_DEPTH;
            } else if (full) {
                metadata().setBitPerSample(bitDepth);
            }
            /*
             * Identify the bands that we can map to RGBA.
             * Will be used for building the color model.
             */
            bitsPerSample[band] = bitDepth;
            numBits += bitDepth;
            if (full && colorType instanceof ComponentType ct) {
                int mask = 0;
                if (numBits < Integer.SIZE) {
                    mask = ((1 << bitDepth) - 1) << (Integer.SIZE - numBits);
                }
                switch (ct) {
                    case RED:        redMask   |= mask; break;
                    case GREEN:      greenMask |= mask; break;
                    case BLUE:       blueMask  |= mask; break;
                    case ALPHA:      alphaMask |= mask; alphaBand = band; break;
                    case MONOCHROME: grayBand   = band; break;
                    case PALETTE:    indexBand  = band; break;
                }
            }
        }
        final boolean isRGB = (redMask | greenMask | blueMask) != 0;
        if (isRGB && numBits < Integer.SIZE) {
            final int n = Integer.SIZE - numBits;   // Number of unused bits.
            redMask   >>>= n;
            greenMask >>>= n;
            blueMask  >>>= n;
            alphaMask >>>= n;
        }
        /*
         * Build a sample model. The `InterleavingMode.COMPONENT` default value is arbitrary,
         * as the `UncompressedFrameConfig` box is mandatory according ISO/IEC 23001-17:2024.
         */
        if (sampleModel == null && dataType != null) {
            InterleavingMode interleaveType = InterleavingMode.COMPONENT;
            final var tileSize = new Dimension(width, height);
            if (model != null) {
                tileSize.width  /= model.numTileCols;
                tileSize.height /= model.numTileRows;
                interleaveType = model.interleaveType;
            }
            final boolean isBanded;
            switch (interleaveType) {
                case COMPONENT: isBanded = true;  break;    // Java2D: BandedSampleModel
                case PIXEL:     isBanded = false; break;    // Java2D: PixelInterleavedSampleModel
                default: throw new RasterFormatException("Unsupported interleave type: " + interleaveType);
            }
            sampleModel = new SampleModelBuilder(dataType, tileSize, bitsPerSample, isBanded).build();
        }
        /*
         * Build a RGB(A) or indexed color model compatible with the sample model.
         * The gray scale is used as a fallback for all unrecognized color models.
         */
        if (full) {
            if (indexBand >= 0 && palette != null) {
                colorModel = palette.toARGB(dataType, bitsPerSample[indexBand], numBands, indexBand);   // May be null.
            }
            if (colorModel == null && sampleModel != null) {
                if (grayBand < 0 && (grayBand = indexBand) < 0) {
                    grayBand = ColorModelFactory.DEFAULT_VISIBLE_BAND;
                }
                final var cb = new ColorModelBuilder(isRGB)
                                .dataType(dataType)
                                .bitsPerSample(bitsPerSample)
                                .alphaBand(alphaBand)
                                .visibleBand(grayBand, sd[grayBand].getSampleRange().orElse(null));
                if (isRGB) {
                    cb.componentMasks(redMask, greenMask, blueMask, alphaMask);
                    // TODO: use another color space if not RGB.
                }
                colorModel = cb.createRGB(sampleModel);
            }
        }
        sampleDimensions = sd;
    }

    /**
     * Returns the sample dimensions for the bands and prepares metadata related to them.
     *
     * @return the sample dimensions for all bands.
     * @throws RasterFormatException if the sample dimensions or sample model cannot be created.
     * @throws DataStoreException if an error occurred in {@link CoverageModifier}.
     */
    public final List<SampleDimension> sampleDimensions() throws DataStoreException {
        if (sampleDimensions == null) {
            sampleDimensions(true);
        }
        return Containers.viewAsUnmodifiableList(sampleDimensions);
    }

    /**
     * Creates the grid geometry and opportunistically prepares metadata related to it.
     * This method should be invoked exactly once, preferably after {@link #sampleDimensions()}.
     * It may be invoked not at all when this object is used for building a tile instead of an image.
     *
     * @todo Need to add information from the {@code ExtraDimensionProperty} box.
     *       These information include name, minimum, maximum and resolution.
     *
     * @return the grid geometry.
     * @throws DataStoreException if the "grid to <abbr>CRS</abbr>" transform cannot be created.
     */
    public final GridGeometry gridGeometry() throws DataStoreException {
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
        var source = new CoverageModifier.Source(store, imageIndex, dataType);
        return store.customizer.customize(source, gridGeometry);
    }
}
