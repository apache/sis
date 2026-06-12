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
import java.util.List;
import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import javax.imageio.ImageTypeSpecifier;
import org.opengis.util.GenericName;
import org.apache.sis.image.DataType;
import org.apache.sis.image.internal.shared.ColorModelBuilder;
import org.apache.sis.image.internal.shared.ColorModelFactory;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.internal.shared.SampleModelBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.modifier.CoverageModifier;
import org.apache.sis.storage.isobmff.mpeg.Component;
import org.apache.sis.storage.isobmff.mpeg.ComponentType;
import org.apache.sis.storage.isobmff.mpeg.ComponentPalette;
import org.apache.sis.storage.isobmff.mpeg.InterleavingMode;
import org.apache.sis.storage.isobmff.mpeg.UncompressedFrameConfig;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * Color model, sample model and sample dimensions computed from <abbr>HEIF</abbr> boxes.
 *
 * @todo The same {@code ImageModel} may be duplicated in many tiles of the same image.
 * This class is intended to be used for avoiding to recompute the same information for every tiles.
 * This sharing has not yet been implemented.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ImageModel {
    /**
     * The type of sample values stored in the raster.
     */
    final DataType dataType;

    /**
     * The color model, or {@code null} if this builder did not have enough information.
     */
    final ColorModel colorModel;

    /**
     * The sample model, {@code null} if this builder did not have enough information.
     */
    final SampleModel sampleModel;

    /**
     * Default name of each band.
     */
    private final GenericName[] defaultBandNames;

    /**
     * The sample dimensions for each band.
     */
    private final SampleDimension[] sampleDimensions;

    /**
     * Creates a new image model from the given Image I/O specifier.
     *
     * @param  type  the image specifier from the Image I/O <abbr>API</abbr>.
     */
    ImageModel(final ImageTypeSpecifier type) {
        colorModel  = type.getColorModel();
        sampleModel = type.getSampleModel();
        dataType    = DataType.forDataBufferType(sampleModel.getDataType());
        sampleDimensions = null;
        defaultBandNames = null;
    }

    /**
     * Computes date type, color model, sample model, and sample dimensions.
     *
     * @todo Need to add information from the {@code CellPropertyTypeProperty} box.
     *       These information include sample dimension name and unit of measurement.
     *
     * @param  width           the width  (in pixels) of the reconstructed image.
     * @param  height          the height (in pixels) of the reconstructed image.
     * @param  model           information about the sample model (data type, <i>etc.</i>), or {@code null}.
     * @param  componentTypes  pixel data displaying as {@link ComponentType}, {@link URI} or {@link Integer}.
     * @param  bitsPerChannel  number of bits per channel in the reconstructed image, or {@code null}.
     * @param  palette         the color palette of an indexed color model, or {@code null}.
     * @param  builder         the builder which is creating a grid coverage.
     * @throws DataStoreContentException if the sample model or sample dimensions cannot be created.
     * @throws DataStoreException if the reading failed for another reason.
     */
    ImageModel(final int width,
               final int height,
               final UncompressedFrameConfig model,
               final Object[] componentTypes,
               final byte[] bitsPerChannel,
               final ComponentPalette palette,
               final CoverageBuilder builder)
            throws DataStoreException
    {
        final int nc = (model          == null) ? 0 : model.components.length;
        final int nt = (componentTypes == null) ? 0 : componentTypes.length;
        final int ns = (bitsPerChannel == null) ? 0 : bitsPerChannel.length;
        final int numBands = Math.max(Math.max(nc, nt), ns);
        final var bitsPerSample = new int[numBands];
        final var sb = new SampleDimension.Builder();
        sampleDimensions = new SampleDimension[numBands];
        defaultBandNames = new GenericName[numBands];

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        DataType dataType = null;
        int numBits=0, redMask=0, greenMask=0, blueMask=0, alphaMask=0;
        int grayBand = -1, indexBand = -1, alphaBand = -1;    // Negative value means none.
        for (int band = 0; band < numBands; band++) {
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
                if (colorType == null) {
                    colorType = c.type;         // From `UncompressedFrameConfig`
                }
                if (dataType == null) {
                    dataType = c.getDataType();
                } else if (dataType != c.getDataType()) {
                    throw new DataStoreContentException("All bands shall be of the same data type.");
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
            if (colorType != null) {
                sb.setName(colorType.toString());
            } else {
                sb.setName(band);
            }
            defaultBandNames[band] = sb.getName();
            var source = new CoverageModifier.BandSource(builder.store, builder.imageIndex, band, numBands, dataType);
            builder.metadata().addNewBand(sampleDimensions[band] = builder.store.customizer.customize(source, sb));
            sb.clear();
            if (bitDepth == 0) {
                bitDepth = Component.DEFAULT_BIT_DEPTH;
            } else {
                builder.metadata().setBitPerSample(bitDepth);
            }
            /*
             * Identify the bands that we can map to RGBA.
             * Will be used for building the color model.
             */
            bitsPerSample[band] = bitDepth;
            numBits += bitDepth;
            if (colorType instanceof ComponentType ct) {
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
        this.dataType = dataType;
        if (dataType != null) {
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
                default: throw new UnsupportedEncodingException("Unsupported interleave type: " + interleaveType);
            }
            sampleModel = new SampleModelBuilder(dataType, tileSize, bitsPerSample, isBanded).build();
        } else {
            sampleModel = null;
        }
        /*
         * Build a RGB(A) or indexed color model compatible with the sample model.
         * The gray scale is used as a fallback for all unrecognized color models.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        ColorModel colorModel = null;
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
                            .visibleBand(grayBand, sampleDimensions[grayBand].getSampleRange().orElse(null));
            if (isRGB) {
                cb.componentMasks(redMask, greenMask, blueMask, alphaMask);
                // TODO: use another color space if not RGB.
            }
            colorModel = cb.createRGB(sampleModel);
        }
        this.colorModel = colorModel;
    }

    /**
     * Returns the sample dimensions.
     * If the {@code coverage} argument is null, it is assumed the same as at construction time.
     *
     * @param  builder  builder of the coverage for which to create sample dimensions, or {@code null}.
     * @return the sample dimensions.
     * @throws DataStoreContentException if the sample dimensions cannot be created.
     */
    final List<SampleDimension> sampleDimensions(final CoverageBuilder builder) throws DataStoreException {
        SampleDimension[] bands = sampleDimensions;
        boolean share = true;
        if (builder != null) {
            bands = bands.clone();
            final int numBands = bands.length;
            final var sb = new SampleDimension.Builder();
            for (int band = 0; band < numBands; band++) {
                sb.setName(defaultBandNames[band]);
                var source = new CoverageModifier.BandSource(builder.store, builder.imageIndex, band, numBands, dataType);
                final SampleDimension sd = builder.store.customizer.customize(source, sb);
                if (!sd.equals(bands[band])) {
                    bands[band] = sd;
                    share = false;
                }
                sb.clear();
            }
            if (share) {
                bands = sampleDimensions;
            }
        }
        return List.of(bands);
    }
}
